import React, { useEffect, useState } from "react";
import {
  Check,
  Copy,
  Download,
  FileCode2,
  FileDown,
  FileUp,
  RotateCcw,
  ShieldCheck,
} from "lucide-react";
import {
  CLSN,
  DPE_LOGO_OPTIONS,
  DpeLogoStyle,
  PDF_FONT_OPTIONS,
  PdfFontId,
  Settings,
  Student,
  bn,
  importCsv,
  toCsv,
  toJson,
  fromJson,
} from "../lib/models";

interface ProjectFileEntry {
  label: string;
  path: string;
  content: string;
}

interface SettingsAndCicdProps {
  students: Student[];
  settings: Settings;
  onUpdateSettings: (settings: Settings) => void;
  onReplaceAll: (students: Student[], settings: Settings) => void;
  onExportAllPdf: () => Promise<void>;
  onResetSampleData: () => void;
  exportingPdf: boolean;
}

function downloadTextFile(filename: string, content: string, mime: string) {
  const blob = new Blob([content], { type: mime });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

export const SettingsAndCicd: React.FC<SettingsAndCicdProps> = ({
  students,
  settings,
  onUpdateSettings,
  onReplaceAll,
  onExportAllPdf,
  onResetSampleData,
  exportingPdf,
}) => {
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [projectFiles, setProjectFiles] = useState<ProjectFileEntry[]>([]);
  const [selectedFilePath, setSelectedFilePath] = useState<string>("");
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    fetch("/api/project-files")
      .then((r) => r.json())
      .then((data: { files?: ProjectFileEntry[] }) => {
        if (data.files && data.files.length > 0) {
          setProjectFiles(data.files);
          setSelectedFilePath(data.files[0].path);
        }
      })
      .catch(() => {
        // Optional inspector fetch
      });
  }, []);

  const handleCsvImport = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setError(null);
    setNotice(null);
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const text = String(reader.result || "");
        const before = students.length;
        const merged = importCsv(text, students);
        onReplaceAll(merged, settings);
        const added = merged.length - before;
        setNotice(
          `CSV ইমপোর্ট সম্পন্ন হয়েছে (নতুন ${bn(String(Math.max(0, added)))} জন, মোট ${bn(String(merged.length))} জন)।`
        );
      } catch (err) {
        setError(`CSV ইমপোর্ট ব্যর্থ: ${(err as Error).message}`);
      }
    };
    reader.readAsText(file, "UTF-8");
    e.target.value = "";
  };

  const handleJsonRestore = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setError(null);
    setNotice(null);
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const parsed = fromJson(String(reader.result || ""));
        onReplaceAll(parsed.students, parsed.settings);
        setNotice(
          `JSON ব্যাকআপ থেকে ${bn(String(parsed.students.length))} জন শিক্ষার্থীর তথ্য রিস্টোর হয়েছে।`
        );
      } catch (err) {
        setError(`ব্যাকআপ ফাইল সঠিক নয়: ${(err as Error).message}`);
      }
    };
    reader.readAsText(file, "UTF-8");
    e.target.value = "";
  };

  const activeFile =
    projectFiles.find((f) => f.path === selectedFilePath) || projectFiles[0];

  const handleCopyCode = async () => {
    if (!activeFile) return;
    await navigator.clipboard.writeText(activeFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 1800);
  };

  return (
    <div className="space-y-6">
      {(notice || error) && (
        <div
          className={`p-3.5 rounded-xl border text-sm ${
            error
              ? "border-rose-200 bg-rose-50 text-rose-800"
              : "border-emerald-200 bg-emerald-50 text-emerald-800"
          }`}
        >
          {error || notice}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* School & Teachers Settings */}
        <div className="lg:col-span-7 border border-slate-200 bg-white rounded-xl p-6 space-y-5">
          <div>
            <h2 className="text-base font-semibold text-slate-900">
              বিদ্যালয় ও শ্রেণি শিক্ষক সেটিংস
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              এই তথ্য প্রতিবেদন PDF-এর ফ্রন্ট ও ব্যাক কভারে স্বয়ংক্রিয়ভাবে যুক্ত হয়।
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="sm:col-span-2">
              <label className="block text-xs font-medium text-slate-600 mb-1">
                বিদ্যালয়ের নাম
              </label>
              <input
                type="text"
                value={settings.school}
                onChange={(e) =>
                  onUpdateSettings({ ...settings, school: e.target.value })
                }
                className="w-full px-3.5 py-2 text-sm rounded-lg border border-slate-200 focus:outline-none focus:border-teal-700"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">
                শিক্ষাবর্ষ
              </label>
              <input
                type="text"
                value={settings.year}
                onChange={(e) =>
                  onUpdateSettings({ ...settings, year: e.target.value })
                }
                className="w-full px-3.5 py-2 text-sm font-mono tabular-nums rounded-lg border border-slate-200 focus:outline-none focus:border-teal-700"
              />
            </div>
            <div className="sm:col-span-3">
              <label className="block text-xs font-medium text-slate-600 mb-1">
                বিদ্যালয়ের ঠিকানা (ব্যাক কভারে প্রদর্শিত হবে)
              </label>
              <input
                type="text"
                value={settings.schoolAddress ?? ""}
                placeholder="যেমন: কামারখালী, মধুখালী, ফরিদপুর"
                onChange={(e) =>
                  onUpdateSettings({
                    ...settings,
                    schoolAddress: e.target.value,
                  })
                }
                className="w-full px-3.5 py-2 text-sm rounded-lg border border-slate-200 focus:outline-none focus:border-teal-700"
              />
            </div>
          </div>

          <div className="border-t border-slate-200 pt-4 space-y-2.5">
            <div className="flex items-center justify-between">
              <div className="text-xs font-semibold text-slate-700">
                প্রতিবেদনের বাংলা ফন্ট (প্রাতিষ্ঠানিক ব্যবহারের জন্য)
              </div>
              <span className="text-[11px] text-slate-400">
                PDF ও প্রিভিউতে প্রযোজ্য
              </span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              {PDF_FONT_OPTIONS.map((opt) => {
                const selected = (settings.pdfFont || "noto_serif") === opt.id;
                return (
                  <button
                    key={opt.id}
                    type="button"
                    onClick={() =>
                      onUpdateSettings({
                        ...settings,
                        pdfFont: opt.id as PdfFontId,
                      })
                    }
                    className={`text-left p-3 rounded-lg border transition-colors ${
                      selected
                        ? "border-teal-700 bg-teal-50/50"
                        : "border-slate-200 bg-white hover:bg-slate-50"
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-xs font-semibold text-slate-900">
                        {opt.labelBn}
                      </span>
                      {selected && (
                        <Check className="w-3.5 h-3.5 text-teal-700 shrink-0" />
                      )}
                    </div>
                    <div
                      className="mt-1.5 text-sm text-slate-800"
                      style={{ fontFamily: opt.cssFamily }}
                    >
                      শিখন অগ্রগতি প্রতিবেদন — ২০২৬
                    </div>
                    <div className="mt-1 text-[11px] text-slate-500">
                      {opt.descriptionBn}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          <div className="border-t border-slate-200 pt-4 space-y-2.5">
            <div className="flex items-center justify-between">
              <div className="text-xs font-semibold text-slate-700">
                প্রাথমিক শিক্ষা অধিদপ্তরের লোগো (কভার পৃষ্ঠা)
              </div>
              <span className="text-[11px] text-slate-400">
                রঙিন অথবা সাদাকালো
              </span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              {DPE_LOGO_OPTIONS.map((opt) => {
                const selected = (settings.dpeLogo || "simple") === opt.id;
                return (
                  <button
                    key={opt.id}
                    type="button"
                    onClick={() =>
                      onUpdateSettings({
                        ...settings,
                        dpeLogo: opt.id as DpeLogoStyle,
                      })
                    }
                    className={`text-left p-3 rounded-lg border transition-colors flex items-center gap-3 cursor-pointer ${
                      selected
                        ? "border-teal-700 bg-teal-50/50"
                        : "border-slate-200 bg-white hover:bg-slate-50"
                    }`}
                  >
                    <img
                      src={opt.src}
                      alt={opt.labelBn}
                      className="w-12 h-14 object-contain rounded border border-slate-200 bg-white p-1 shrink-0"
                    />
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-xs font-semibold text-slate-900">
                          {opt.labelBn}
                        </span>
                        {selected && (
                          <Check className="w-3.5 h-3.5 text-teal-700 shrink-0" />
                        )}
                      </div>
                      <div className="mt-1 text-[11px] text-slate-500 leading-snug">
                        {opt.descriptionBn}
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          <div className="border-t border-slate-200 pt-4 space-y-3">
            <div className="text-xs font-semibold text-slate-700">
              শ্রেণিভিত্তিক শ্রেণি শিক্ষক
            </div>
            {[1, 2, 3, 4, 5].map((c) => (
              <div
                key={c}
                className="grid grid-cols-1 sm:grid-cols-12 gap-2.5 items-center"
              >
                <div className="sm:col-span-3 text-sm font-medium text-slate-700">
                  {CLSN[c - 1]} শ্রেণি
                </div>
                <div className="sm:col-span-5">
                  <input
                    type="text"
                    value={settings.teachers[c] || ""}
                    onChange={(e) =>
                      onUpdateSettings({
                        ...settings,
                        teachers: { ...settings.teachers, [c]: e.target.value },
                      })
                    }
                    placeholder="শিক্ষকের নাম"
                    className="w-full px-3 py-1.5 text-sm rounded-lg border border-slate-200 focus:outline-none focus:border-teal-700"
                  />
                </div>
                <div className="sm:col-span-4">
                  <input
                    type="text"
                    value={settings.teacherMobiles[c] || ""}
                    onChange={(e) =>
                      onUpdateSettings({
                        ...settings,
                        teacherMobiles: {
                          ...settings.teacherMobiles,
                          [c]: e.target.value,
                        },
                      })
                    }
                    placeholder="মোবাইল নম্বর"
                    className="w-full px-3 py-1.5 text-sm font-mono tabular-nums rounded-lg border border-slate-200 focus:outline-none focus:border-teal-700"
                  />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* CSV / JSON / Bulk PDF */}
        <div className="lg:col-span-5 border border-slate-200 bg-white rounded-xl p-6 space-y-5">
          <div>
            <h2 className="text-base font-semibold text-slate-900">
              ডাটা ব্যাকআপ, CSV ও PDF এক্সপোর্ট
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Android অ্যাপের Repo.kt ফরম্যাটের সাথে ১০০% সামঞ্জস্যপূর্ণ।
            </p>
          </div>

          <div className="space-y-2.5">
            <button
              onClick={onExportAllPdf}
              disabled={students.length === 0 || exportingPdf}
              className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm font-medium rounded-lg bg-teal-700 text-white hover:bg-teal-800 disabled:opacity-50"
            >
              <FileDown className="w-4 h-4" />
              <span>সব শ্রেণির সব শিক্ষার্থীর PDF ({bn(String(students.length))} জন)</span>
            </button>
          </div>

          <div className="border-t border-slate-200 pt-4 space-y-2.5">
            <div className="text-xs font-semibold text-slate-700">
              CSV (Excel) ইমপোর্ট ও এক্সপোর্ট
            </div>
            <div className="grid grid-cols-2 gap-2.5">
              <button
                onClick={() =>
                  downloadTextFile(
                    "শিক্ষার্থী-তথ্য.csv",
                    toCsv(students),
                    "text/csv;charset=utf-8;"
                  )
                }
                className="inline-flex items-center justify-center gap-1.5 px-3 py-2 text-xs font-medium rounded-lg border border-slate-200 text-slate-700 hover:bg-slate-50"
              >
                <Download className="w-3.5 h-3.5" />
                <span>CSV এক্সপোর্ট</span>
              </button>
              <button
                onClick={() =>
                  downloadTextFile(
                    "খালি-টেমপ্লেট.csv",
                    toCsv([]),
                    "text/csv;charset=utf-8;"
                  )
                }
                className="inline-flex items-center justify-center gap-1.5 px-3 py-2 text-xs font-medium rounded-lg border border-slate-200 text-slate-700 hover:bg-slate-50"
              >
                <Download className="w-3.5 h-3.5" />
                <span>খালি টেমপ্লেট</span>
              </button>
            </div>
            <label className="w-full inline-flex items-center justify-center gap-2 px-3 py-2 text-xs font-medium rounded-lg border border-slate-200 text-slate-700 hover:bg-slate-50 cursor-pointer">
              <FileUp className="w-3.5 h-3.5" />
              <span>CSV ইমপোর্ট (শ্রেণি+রোল মিললে হালনাগাদ)</span>
              <input
                type="file"
                accept=".csv,text/csv"
                onChange={handleCsvImport}
                className="hidden"
              />
            </label>
          </div>

          <div className="border-t border-slate-200 pt-4 space-y-2.5">
            <div className="text-xs font-semibold text-slate-700">
              JSON ব্যাকআপ / রিস্টোর
            </div>
            <div className="grid grid-cols-2 gap-2.5">
              <button
                onClick={() =>
                  downloadTextFile(
                    "ব্যাকআপ.json",
                    toJson(students, settings),
                    "application/json;charset=utf-8;"
                  )
                }
                className="inline-flex items-center justify-center gap-1.5 px-3 py-2 text-xs font-medium rounded-lg border border-slate-200 text-slate-700 hover:bg-slate-50"
              >
                <Download className="w-3.5 h-3.5" />
                <span>ব্যাকআপ এক্সপোর্ট</span>
              </button>
              <label className="inline-flex items-center justify-center gap-1.5 px-3 py-2 text-xs font-medium rounded-lg border border-slate-200 text-slate-700 hover:bg-slate-50 cursor-pointer">
                <FileUp className="w-3.5 h-3.5" />
                <span>রিস্টোর JSON</span>
                <input
                  type="file"
                  accept=".json,application/json"
                  onChange={handleJsonRestore}
                  className="hidden"
                />
              </label>
            </div>
            <button
              onClick={onResetSampleData}
              className="w-full inline-flex items-center justify-center gap-1.5 px-3 py-2 text-xs font-medium rounded-lg border border-slate-200 text-slate-500 hover:text-slate-800 hover:bg-slate-50"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>নমুনা ডাটা রিসেট করুন</span>
            </button>
          </div>
        </div>
      </div>

      {/* Android Codebase & GitHub Actions CI/CD Inspector */}
      <div className="border border-slate-200 bg-white rounded-xl overflow-hidden">
        <div className="p-6 border-b border-slate-200 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="flex items-center gap-2">
              <ShieldCheck className="w-5 h-5 text-teal-700" />
              <h2 className="text-base font-semibold text-slate-900">
                Android প্রকল্প (com.kgps.result) ও GitHub Actions CI/CD
              </h2>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              অ্যাপের নাম: Result Maker · প্যাকেজ: com.kgps.result · কী আলিয়াস: release · স্বয়ংক্রিয় Build, Sign ও Deploy ওয়ার্কফ্লো
            </p>
          </div>

          <div className="text-xs font-mono text-slate-600 bg-slate-50 border border-slate-200 rounded-lg px-3 py-2 space-y-0.5">
            <div>SHA-1: 83:13:11:6B:F3:4D:0C:CA:44:8F:1A:51:EF:63:79:8D:4E:0B:DA:CA</div>
            <div>SHA-256: CD:8A:75:D6:9F:53:16:15:57:C8:19:1D:2C:57:CF:7E:FC:2D:75:A9:44:AE:A2:78:43:FB:38:D6:FC:1C:68:A9</div>
          </div>
        </div>

        <div className="p-6 border-b border-slate-200 bg-slate-50/50 text-xs text-slate-600 grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <div className="font-semibold text-slate-800 mb-1">
              GitHub Repository Secrets (Release Workflow-এর জন্য)
            </div>
            <ul className="space-y-1 font-mono text-[11px] text-slate-600">
              <li>RELEASE_KEYSTORE_BASE64 — Base64-encoded release keystore</li>
              <li>RELEASE_KEYSTORE_PASSWORD — Keystore storePassword</li>
              <li>RELEASE_KEY_ALIAS — release</li>
              <li>RELEASE_KEY_PASSWORD — Key password</li>
              <li>PLAY_SERVICE_ACCOUNT_JSON — Optional Google Play deploy key</li>
            </ul>
          </div>
          <div>
            <div className="font-semibold text-slate-800 mb-1">
              স্বয়ংক্রিয় ওয়ার্কফ্লো ট্রিগারসমূহ
            </div>
            <ul className="space-y-1 text-slate-600">
              <li>1. <span className="font-mono">build-and-release.yml</span>: প্রতিটি পুশে একক ওয়ার্কফ্লো — Web যাচাই ও Signed <span className="font-mono">result_maker_release.apk</span> বিল্ড</li>
              <li>2. শুধুমাত্র <span className="font-mono">result_maker_release.apk</span> ফাইলটি সরাসরি <span className="font-mono">GitHub Releases</span>-এ প্রকাশ হয়</li>
            </ul>
          </div>
        </div>

        {projectFiles.length > 0 && (
          <div className="grid grid-cols-1 lg:grid-cols-12">
            <div className="lg:col-span-4 border-b lg:border-b-0 lg:border-r border-slate-200 divide-y divide-slate-100">
              {projectFiles.map((f) => {
                const active = activeFile?.path === f.path;
                return (
                  <button
                    key={f.path}
                    onClick={() => setSelectedFilePath(f.path)}
                    className={`w-full text-left px-4 py-3 flex items-center gap-2.5 text-sm transition-colors ${
                      active
                        ? "bg-teal-50/70 text-teal-800 font-medium"
                        : "text-slate-700 hover:bg-slate-50"
                    }`}
                  >
                    <FileCode2 className="w-4 h-4 shrink-0" />
                    <div className="min-w-0">
                      <div className="truncate">{f.label}</div>
                      <div className="text-[11px] font-mono text-slate-400 truncate">
                        {f.path}
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>

            <div className="lg:col-span-8 flex flex-col">
              {activeFile && (
                <>
                  <div className="px-4 py-2.5 border-b border-slate-200 bg-slate-50 flex items-center justify-between gap-2">
                    <span className="text-xs font-mono text-slate-600 truncate">
                      {activeFile.path}
                    </span>
                    <div className="flex items-center gap-2 shrink-0">
                      <button
                        onClick={handleCopyCode}
                        className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded border border-slate-200 bg-white text-slate-700 hover:bg-slate-50"
                      >
                        {copied ? (
                          <Check className="w-3.5 h-3.5 text-emerald-600" />
                        ) : (
                          <Copy className="w-3.5 h-3.5" />
                        )}
                        <span>{copied ? "কপি হয়েছে" : "কপি"}</span>
                      </button>
                      <button
                        onClick={() =>
                          downloadTextFile(
                            activeFile.label,
                            activeFile.content,
                            "text/plain;charset=utf-8;"
                          )
                        }
                        className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded border border-slate-200 bg-white text-slate-700 hover:bg-slate-50"
                      >
                        <Download className="w-3.5 h-3.5" />
                        <span>ডাউনলোড</span>
                      </button>
                    </div>
                  </div>
                  <pre className="p-4 text-xs font-mono text-slate-800 bg-slate-950/2 overflow-x-auto max-h-[420px] leading-relaxed">
                    <code>{activeFile.content}</code>
                  </pre>
                </>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
