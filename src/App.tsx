/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect, useRef, useState } from "react";
import {
  Check,
  ChevronDown,
  FileDown,
  Menu,
  Plus,
  Settings as SettingsIcon,
  Type,
  Volume2,
  X,
} from "lucide-react";
import {
  CLSN,
  DEFAULT_SETTINGS,
  DPE_LOGO_OPTIONS,
  DpeLogoStyle,
  PDF_FONT_OPTIONS,
  PdfFontId,
  SAMPLE_STUDENTS,
  Settings,
  Student,
  bn,
  createEmptyStudent,
  fmt,
  getPdfFontOption,
  gradeLabelBn,
  gradeOf,
  overallPct,
} from "./lib/models";
import { exportStudentsToPdf } from "./lib/pdfRenderer";
import { requestTtsAudio } from "./lib/tts";
import { StudentRoster } from "./components/StudentRoster";
import { BulkSheetEntry } from "./components/BulkSheetEntry";
import { StudentEditor } from "./components/StudentEditor";
import { ReportStudio } from "./components/ReportStudio";
import { AiAssistantPanel } from "./components/AiAssistantPanel";
import { SettingsAndCicd } from "./components/SettingsAndCicd";
import { SmartCsvImportModal } from "./components/SmartCsvImportModal";

const STORAGE_STUDENTS_KEY = "result_maker_students_v1";
const STORAGE_SETTINGS_KEY = "result_maker_settings_v1";

type NavSection = "roster" | "sheet" | "studio" | "assistant" | "settings";

function loadInitialStudents(): Student[] {
  try {
    const raw = localStorage.getItem(STORAGE_STUDENTS_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) return parsed as Student[];
    }
  } catch {
    // ignore corrupted cache
  }
  return SAMPLE_STUDENTS;
}

function loadInitialSettings(): Settings {
  try {
    const raw = localStorage.getItem(STORAGE_SETTINGS_KEY);
    if (raw) {
      return JSON.parse(raw) as Settings;
    }
  } catch {
    // ignore corrupted cache
  }
  return DEFAULT_SETTINGS;
}

export default function App() {
  const [students, setStudents] = useState<Student[]>(loadInitialStudents);
  const [settings, setSettings] = useState<Settings>(loadInitialSettings);
  const [section, setSection] = useState<NavSection>("roster");
  const [selectedClass, setSelectedClass] = useState<number>(1);
  const [editingStudent, setEditingStudent] = useState<Student | null>(null);
  const [studioStudentId, setStudioStudentId] = useState<string | null>(null);
  const [exportingPdf, setExportingPdf] = useState(false);
  const [speakingId, setSpeakingId] = useState<string | null>(null);
  const [toastMsg, setToastMsg] = useState<string | null>(null);
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [csvModal, setCsvModal] = useState<{
    open: boolean;
    initialText?: string;
    initialFileName?: string;
  }>({ open: false });
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!menuOpen) return;
    const handleOutsideClick = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false);
      }
    };
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === "Escape") setMenuOpen(false);
    };
    document.addEventListener("mousedown", handleOutsideClick);
    document.addEventListener("keydown", handleEsc);
    return () => {
      document.removeEventListener("mousedown", handleOutsideClick);
      document.removeEventListener("keydown", handleEsc);
    };
  }, [menuOpen]);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_STUDENTS_KEY, JSON.stringify(students));
    } catch {
      // ignore storage quota errors
    }
  }, [students]);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_SETTINGS_KEY, JSON.stringify(settings));
    } catch {
      // ignore storage quota errors
    }
  }, [settings]);

  const showToast = (msg: string) => {
    setToastMsg(msg);
    setTimeout(() => {
      setToastMsg((cur) => (cur === msg ? null : cur));
    }, 3500);
  };

  const upsertStudent = (s: Student) => {
    setStudents((prev) => {
      const exists = prev.some((item) => item.id === s.id);
      return exists
        ? prev.map((item) => (item.id === s.id ? s : item))
        : [...prev, s];
    });
  };

  const deleteStudent = (id: string) => {
    setStudents((prev) => prev.filter((item) => item.id !== id));
    setEditingStudent(null);
    showToast("শিক্ষার্থীর তথ্য মুছে ফেলা হয়েছে।");
  };

  const handleNewStudent = () => {
    const nextRoll =
      students
        .filter((s) => s.cls === selectedClass)
        .reduce((max, s) => Math.max(max, s.roll), 0) + 1;
    const fresh = createEmptyStudent(selectedClass, nextRoll);
    setEditingStudent(fresh);
    setSection("roster");
  };

  const handleExportPdf = async (list: Student[], filename: string) => {
    if (list.length === 0) {
      showToast("PDF তৈরি করার জন্য কোনো শিক্ষার্থী নেই।");
      return;
    }
    setExportingPdf(true);
    try {
      await exportStudentsToPdf(list, settings, filename);
      showToast(`${filename} সংরক্ষিত হয়েছে।`);
    } catch (err) {
      showToast(`PDF তৈরি করা যায়নি: ${(err as Error).message}`);
    } finally {
      setExportingPdf(false);
    }
  };

  const handleSpeakText = async (text: string, id: string) => {
    setSpeakingId(id);
    try {
      const dataUrl = await requestTtsAudio(text);
      setAudioUrl(dataUrl);
      setTimeout(() => {
        audioRef.current?.play().catch(() => {
          // autoplay fallback: user can click play on visible player
        });
      }, 60);
    } catch (err) {
      showToast(`অডিও তৈরি করা যায়নি: ${(err as Error).message}`);
    } finally {
      setSpeakingId(null);
    }
  };

  const handleSpeakStudentSummary = async (s: Student) => {
    const pct = overallPct(s);
    const grade = gradeOf(pct);
    const remark = s.x["REM_3"] || s.x["REM_2"] || s.x["REM_1"] || "";
    const script = [
      `শিক্ষার্থীর নাম ${s.name || "অনির্ধারিত"}।`,
      `শ্রেণি ${CLSN[s.cls - 1]}, রোল ${bn(String(s.roll))}।`,
      `সমন্বিত গড় প্রাপ্ত নম্বর ${fmt(pct)} শতাংশ, গ্রেড ${grade}, অর্থাৎ ${gradeLabelBn(grade)}।`,
      remark ? `শ্রেণি শিক্ষকের মতামত: ${remark}` : "",
    ]
      .filter(Boolean)
      .join(" ");

    await handleSpeakText(script, s.id);
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-slate-900">
      {/* Three-Zone Top Bar Contract */}
      <header className="sticky top-0 z-30 bg-white/95 backdrop-blur-xs border-b border-slate-200 print:hidden">
        <div className="max-w-[1280px] mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-8">
          {/* Zone 1: Brand wordmark + app icon */}
          <button
            onClick={() => {
              setEditingStudent(null);
              setSection("roster");
            }}
            className="inline-flex items-center gap-2.5 text-lg font-bold tracking-tight text-slate-900 whitespace-nowrap shrink-0 cursor-pointer"
          >
            <img
              src="/app-icon.png"
              alt="Result Maker icon"
              className="w-8 h-8 rounded-lg object-contain"
            />
            <span>Result Maker</span>
          </button>

          {/* Zone 2: Single-line navigation links */}
          <nav
            className="flex items-center gap-5 sm:gap-7 text-sm font-medium text-slate-600 overflow-x-auto"
            aria-label="Primary navigation"
          >
            {(
              [
                ["roster", "শিক্ষার্থী তালিকা"],
                ["sheet", "শিট এন্ট্রি"],
                ["studio", "রিপোর্ট স্টুডিও"],
                ["assistant", "AI সহকারী"],
                ["settings", "সেটিংস"],
              ] as const
            ).map(([key, label]) => {
              const active = section === key && !editingStudent;
              return (
                <button
                  key={key}
                  onClick={() => {
                    setEditingStudent(null);
                    setSection(key);
                  }}
                  className={`py-1.5 border-b-2 transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                    active
                      ? "border-teal-700 text-slate-900 font-semibold"
                      : "border-transparent hover:text-slate-900"
                  }`}
                >
                  {label}
                </button>
              );
            })}
          </nav>

          {/* Zone 3: Settings/Menu Dropdown + Primary CTA */}
          <div className="flex items-center gap-2 shrink-0">
            <div className="relative" ref={menuRef}>
              <button
                type="button"
                onClick={() => setMenuOpen((prev) => !prev)}
                aria-expanded={menuOpen}
                aria-haspopup="menu"
                className="inline-flex items-center gap-1.5 px-3 py-2 text-xs sm:text-sm font-medium rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 transition-colors whitespace-nowrap shrink-0 cursor-pointer"
              >
                <Menu className="w-4 h-4 text-slate-600" />
                <span>মেনু ও সেটিংস</span>
                <ChevronDown
                  className={`w-3.5 h-3.5 text-slate-400 transition-transform ${
                    menuOpen ? "rotate-180" : ""
                  }`}
                />
              </button>

              {menuOpen && (
                <div
                  role="menu"
                  className="absolute right-0 mt-2 w-80 max-w-[calc(100vw-2rem)] rounded-xl border border-slate-200 bg-white shadow-lg py-2 z-50"
                >
                  <div className="px-3.5 py-1.5 text-[11px] font-semibold uppercase tracking-wide text-slate-400">
                    মেনু নেভিগেশন
                  </div>
                  {(
                    [
                      ["roster", "শিক্ষার্থী তালিকা"],
                      ["sheet", "দ্রুত শিট এন্ট্রি (Bulk Marks)"],
                      ["studio", "রিপোর্ট স্টুডিও (PDF প্রিভিউ)"],
                      ["assistant", "AI সহকারী"],
                      ["settings", "সেটিংস, ব্যাকআপ ও CI/CD"],
                    ] as const
                  ).map(([key, label]) => {
                    const active = section === key && !editingStudent;
                    return (
                      <button
                        key={key}
                        role="menuitem"
                        onClick={() => {
                          setEditingStudent(null);
                          setSection(key);
                          setMenuOpen(false);
                        }}
                        className={`w-full px-3.5 py-2 text-left text-sm flex items-center justify-between transition-colors cursor-pointer ${
                          active
                            ? "bg-teal-50/70 text-teal-800 font-semibold"
                            : "text-slate-700 hover:bg-slate-50"
                        }`}
                      >
                        <span>{label}</span>
                        {active && <Check className="w-4 h-4 text-teal-700" />}
                      </button>
                    );
                  })}

                  <div className="my-2 border-t border-slate-200" />

                  <div className="px-3.5 py-1.5 flex items-center justify-between">
                    <span className="inline-flex items-center gap-1.5 text-[11px] font-semibold text-slate-500">
                      <Type className="w-3.5 h-3.5 text-teal-700" />
                      অফিসিয়াল বাংলা ফন্ট (PDF)
                    </span>
                    <span className="text-[11px] text-teal-700 font-medium">
                      {getPdfFontOption(settings.pdfFont).primaryFace.split(" ")[0]}
                    </span>
                  </div>
                  <div className="px-2 space-y-0.5">
                    {PDF_FONT_OPTIONS.map((font) => {
                      const selected =
                        (settings.pdfFont || "noto_serif") === font.id;
                      return (
                        <button
                          key={font.id}
                          role="menuitemradio"
                          aria-checked={selected}
                          onClick={() => {
                            setSettings((prev) => ({
                              ...prev,
                              pdfFont: font.id as PdfFontId,
                            }));
                            showToast(`ফন্ট নির্বাচিত হয়েছে: ${font.labelBn}`);
                            setMenuOpen(false);
                          }}
                          className={`w-full px-2.5 py-2 rounded-lg text-left flex items-center justify-between gap-2 transition-colors cursor-pointer ${
                            selected
                              ? "bg-teal-50 text-teal-900"
                              : "text-slate-700 hover:bg-slate-50"
                          }`}
                        >
                          <div className="min-w-0">
                            <div className="text-xs font-semibold truncate">
                              {font.labelBn}
                            </div>
                            <div
                              className="text-xs text-slate-500 truncate mt-0.5"
                              style={{ fontFamily: font.cssFamily }}
                            >
                              শিখন অগ্রগতি প্রতিবেদন — ২০২৬
                            </div>
                          </div>
                          {selected && (
                            <Check className="w-4 h-4 text-teal-700 shrink-0" />
                          )}
                        </button>
                      );
                    })}
                  </div>

                  <div className="my-2 border-t border-slate-200" />

                  <div className="px-3.5 py-1 flex items-center justify-between">
                    <span className="text-[11px] font-semibold text-slate-500">
                      কভার লোগো (অধিদপ্তর)
                    </span>
                    <span className="text-[11px] text-teal-700 font-medium">
                      {(settings.dpeLogo || "simple") === "color"
                        ? "রঙিন"
                        : "সাদাকালো"}
                    </span>
                  </div>
                  <div className="px-2 grid grid-cols-2 gap-1.5 pb-1">
                    {DPE_LOGO_OPTIONS.map((opt) => {
                      const selected =
                        (settings.dpeLogo || "simple") === opt.id;
                      return (
                        <button
                          key={opt.id}
                          role="menuitemradio"
                          aria-checked={selected}
                          onClick={() => {
                            setSettings((prev) => ({
                              ...prev,
                              dpeLogo: opt.id as DpeLogoStyle,
                            }));
                            showToast(`লোগো নির্বাচিত হয়েছে: ${opt.labelBn}`);
                            setMenuOpen(false);
                          }}
                          className={`px-2 py-1.5 rounded-lg border text-xs font-medium flex items-center gap-2 transition-colors cursor-pointer ${
                            selected
                              ? "border-teal-700 bg-teal-50 text-teal-900"
                              : "border-slate-200 bg-white text-slate-700 hover:bg-slate-50"
                          }`}
                        >
                          <img
                            src={opt.src}
                            alt={opt.labelBn}
                            className="w-6 h-7 object-contain shrink-0"
                          />
                          <span className="truncate">
                            {opt.id === "color" ? "রঙিন" : "সাদাকালো"}
                          </span>
                        </button>
                      );
                    })}
                  </div>

                  <div className="my-2 border-t border-slate-200" />

                  <div className="px-2 space-y-0.5">
                    <button
                      role="menuitem"
                      onClick={() => {
                        setMenuOpen(false);
                        const sorted = [...students].sort(
                          (a, b) => a.cls - b.cls || a.roll - b.roll
                        );
                        handleExportPdf(sorted, "সব-শ্রেণির-প্রতিবেদন.pdf");
                      }}
                      disabled={students.length === 0 || exportingPdf}
                      className="w-full px-2.5 py-2 rounded-lg text-left text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50 flex items-center gap-2 cursor-pointer"
                    >
                      <FileDown className="w-3.5 h-3.5 text-teal-700" />
                      <span>সব শ্রেণির সব শিক্ষার্থীর PDF ডাউনলোড</span>
                    </button>
                    <button
                      role="menuitem"
                      onClick={() => {
                        setEditingStudent(null);
                        setSection("settings");
                        setMenuOpen(false);
                      }}
                      className="w-full px-2.5 py-2 rounded-lg text-left text-xs font-medium text-slate-700 hover:bg-slate-50 flex items-center gap-2 cursor-pointer"
                    >
                      <SettingsIcon className="w-3.5 h-3.5 text-slate-500" />
                      <span>বিদ্যালয় ও শ্রেণি শিক্ষক সেটিংস</span>
                    </button>
                  </div>
                </div>
              )}
            </div>

            <button
              onClick={handleNewStudent}
              className="inline-flex items-center gap-1.5 px-3.5 sm:px-4 py-2 text-xs sm:text-sm font-medium rounded-lg bg-teal-700 text-white hover:bg-teal-800 transition-colors whitespace-nowrap shrink-0 cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              <span>নতুন শিক্ষার্থী</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 w-full max-w-[1280px] mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {editingStudent ? (
          <StudentEditor
            student={editingStudent}
            settings={settings}
            onUpdateSettings={setSettings}
            onSave={upsertStudent}
            onDelete={deleteStudent}
            onBack={() => setEditingStudent(null)}
            onExportPdf={(s) =>
              handleExportPdf([s], `${s.name || "student"}-report.pdf`)
            }
            onSpeakText={handleSpeakText}
            speakingId={speakingId}
          />
        ) : section === "roster" ? (
          <StudentRoster
            students={students}
            settings={settings}
            selectedClass={selectedClass}
            onSelectClass={setSelectedClass}
            onOpenStudent={(s) => setEditingStudent(s)}
            onNewStudent={handleNewStudent}
            onOpenCsvImport={(initialText, initialFileName) =>
              setCsvModal({ open: true, initialText, initialFileName })
            }
            onPreviewPdf={(s) => {
              setStudioStudentId(s.id);
              setSection("studio");
            }}
            onExportClassPdf={(cls) => {
              const list = students
                .filter((s) => s.cls === cls)
                .sort((a, b) => a.roll - b.roll);
              handleExportPdf(list, `শ্রেণি-${cls}-প্রতিবেদন.pdf`);
            }}
            onSpeakSummary={handleSpeakStudentSummary}
            speakingId={speakingId}
            exportingPdf={exportingPdf}
          />
        ) : section === "sheet" ? (
          <BulkSheetEntry
            students={students}
            customFields={settings.customFields ?? []}
            selectedClass={selectedClass}
            onSelectClass={setSelectedClass}
            onUpsertStudent={upsertStudent}
            onBatchUpdateStudents={(updatedList) => setStudents(updatedList)}
            onOpenStudent={(s) => setEditingStudent(s)}
            onOpenCsvImport={() => setCsvModal({ open: true })}
            onExportClassPdf={(cls) => {
              const list = students
                .filter((s) => s.cls === cls)
                .sort((a, b) => a.roll - b.roll);
              handleExportPdf(list, `শ্রেণি-${cls}-প্রতিবেদন.pdf`);
            }}
            onShowToast={showToast}
            exportingPdf={exportingPdf}
          />
        ) : section === "studio" ? (
          <ReportStudio
            students={students}
            settings={settings}
            onUpdateSettings={setSettings}
            selectedStudentId={studioStudentId}
            onSelectStudentId={setStudioStudentId}
            onExportPdf={handleExportPdf}
            onSpeakSummary={handleSpeakStudentSummary}
            speakingId={speakingId}
            exportingPdf={exportingPdf}
          />
        ) : section === "assistant" ? (
          <AiAssistantPanel
            students={students}
            onApplyExtractedStudent={(s) => {
              upsertStudent(s);
              setSelectedClass(s.cls);
            }}
            onSpeakText={handleSpeakText}
            speakingId={speakingId}
          />
        ) : (
          <SettingsAndCicd
            students={students}
            settings={settings}
            onUpdateSettings={setSettings}
            onReplaceAll={(list, st) => {
              setStudents(list);
              setSettings(st);
            }}
            onOpenCsvImport={(initialText, initialFileName) =>
              setCsvModal({ open: true, initialText, initialFileName })
            }
            onExportAllPdf={() => {
              const sorted = [...students].sort(
                (a, b) => a.cls - b.cls || a.roll - b.roll
              );
              return handleExportPdf(sorted, "সব-শ্রেণির-প্রতিবেদন.pdf");
            }}
            onResetSampleData={() => {
              setStudents(SAMPLE_STUDENTS);
              setSettings(DEFAULT_SETTINGS);
              showToast("নমুনা শিক্ষার্থী তথ্য পুনরুদ্ধার হয়েছে।");
            }}
            exportingPdf={exportingPdf}
          />
        )}
      </main>

      <SmartCsvImportModal
        isOpen={csvModal.open}
        initialCsvText={csvModal.initialText}
        initialFileName={csvModal.initialFileName}
        students={students}
        settings={settings}
        defaultClass={selectedClass}
        onClose={() => setCsvModal({ open: false })}
        onImportComplete={(nextStudents, nextSettings, msg) => {
          setStudents(nextStudents);
          setSettings(nextSettings);
          setCsvModal({ open: false });
          showToast(msg);
        }}
      />

      {/* Audio Player Drawer (when TTS audio is generated) */}
      {audioUrl && (
        <div className="fixed bottom-4 right-4 z-40 bg-white border border-slate-200 rounded-xl shadow-lg p-3 flex items-center gap-3 max-w-sm print:hidden">
          <Volume2 className="w-4 h-4 text-teal-700 shrink-0" />
          <audio
            ref={audioRef}
            controls
            src={audioUrl}
            className="h-8 w-56"
          />
          <button
            onClick={() => setAudioUrl(null)}
            aria-label="Close audio player"
            className="p-1 text-slate-400 hover:text-slate-700"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Toast Notification */}
      {toastMsg && (
        <div className="fixed bottom-4 left-4 z-40 bg-slate-900 text-white text-xs sm:text-sm px-4 py-2.5 rounded-lg shadow-lg print:hidden">
          {toastMsg}
        </div>
      )}
    </div>
  );
}
