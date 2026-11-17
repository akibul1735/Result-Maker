import React, { useState } from "react";
import {
  ArrowLeft,
  FileDown,
  Loader2,
  Sparkles,
  Trash2,
  Volume2,
} from "lucide-react";
import {
  CLSN,
  RELIGIONS,
  Student,
  bn,
  cont,
  fmt,
  fmtEn,
  fromBn,
  gradeLabelBn,
  gradeOf,
  mk,
  overallPct,
  studentSubs,
  subTotal,
  wr,
} from "../lib/models";

interface StudentEditorProps {
  student: Student;
  onSave: (student: Student) => void;
  onDelete: (id: string) => void;
  onBack: () => void;
  onExportPdf: (student: Student) => void;
  onSpeakText: (text: string, id: string) => Promise<void>;
  speakingId: string | null;
}

export const StudentEditor: React.FC<StudentEditorProps> = ({
  student,
  onSave,
  onDelete,
  onBack,
  onExportPdf,
  onSpeakText,
  speakingId,
}) => {
  const [draft, setDraft] = useState<Student>(student);
  const [tab, setTab] = useState<"info" | "marks" | "traits">("info");
  const [term, setTerm] = useState<number>(1);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [draftingTerm, setDraftingTerm] = useState<number | null>(null);
  const [aiError, setAiError] = useState<string | null>(null);

  const updateDraft = (updater: (prev: Student) => Student) => {
    setDraft((prev) => {
      const next = updater(prev);
      if (next.name.trim()) {
        onSave(next);
      }
      return next;
    });
  };

  const setMark = (key: string, raw: string) => {
    const trimmed = raw.trim();
    updateDraft((prev) => {
      const m = { ...prev.m };
      if (!trimmed) {
        delete m[key];
      } else {
        const val = Number.parseFloat(fromBn(trimmed));
        if (!Number.isNaN(val)) {
          m[key] = val;
        }
      }
      return { ...prev, m };
    });
  };

  const setExtra = (key: string, val: string) => {
    updateDraft((prev) => {
      const x = { ...prev.x };
      if (!val.trim()) delete x[key];
      else x[key] = val;
      return { ...prev, x };
    });
  };

  const handleDraftRemark = async (t: number) => {
    setAiError(null);
    setDraftingTerm(t);
    try {
      const subs = studentSubs(draft);
      const subjectLines = subs
        .map((sub) => `${sub.bn}: ${fmt(subTotal(draft, sub, t))}/${fmt(sub.max)}`)
        .join(", ");
      const summary = [
        `শিক্ষার্থীর নাম: ${draft.name || "শিক্ষার্থী"}`,
        `শ্রেণি: ${CLSN[draft.cls - 1]}`,
        `প্রান্তিক: ${t}`,
        `বিষয়ভিত্তিক নম্বর: ${subjectLines}`,
        `সমন্বিত গড়: ${fmt(overallPct(draft))}% (গ্রেড ${gradeOf(overallPct(draft))})`,
        `উপস্থিতি: ${draft.x[`PRES_${t}`] || "অনির্ধারিত"} / ${draft.x[`DAYS_${t}`] || "অনির্ধারিত"}`,
        `সক্রিয়তা: ${draft.x[`ACT_${t}`] || "-"}, শৃঙ্খলা: ${draft.x[`DISC_${t}`] || "-"}, আচরণ: ${draft.x[`COND_${t}`] || "-"}`,
      ].join("\n");

      const res = await fetch("/api/gemini/draft-remark", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ studentSummary: summary, term: t }),
      });
      const data = (await res.json()) as { remark?: string; error?: string };
      if (!res.ok || !data.remark) {
        throw new Error(data.error || "Failed to generate remark");
      }
      setExtra(`REM_${t}`, data.remark);
    } catch (err) {
      setAiError((err as Error).message);
    } finally {
      setDraftingTerm(null);
    }
  };

  const pct = overallPct(draft);
  const grade = gradeOf(pct);
  const subs = studentSubs(draft);

  return (
    <div className="space-y-6">
      {/* Top Action Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-slate-200 pb-4">
        <div className="flex items-center gap-3">
          <button
            onClick={() => {
              if (draft.name.trim()) onSave(draft);
              onBack();
            }}
            className="inline-flex items-center gap-1.5 px-3 py-2 text-sm font-medium rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 transition-colors whitespace-nowrap shrink-0"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>তালিকায় ফিরুন</span>
          </button>
          <div>
            <h2 className="text-lg font-semibold text-slate-900">
              {draft.name || "নতুন শিক্ষার্থী"}
            </h2>
            <p className="text-xs text-slate-500 tabular-nums">
              {CLSN[draft.cls - 1]} শ্রেণি · রোল {bn(String(draft.roll))} ·{" "}
              {draft.religion}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => {
              if (!draft.name.trim()) return;
              onSave(draft);
              onExportPdf(draft);
            }}
            disabled={!draft.name.trim()}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg bg-teal-700 text-white hover:bg-teal-800 disabled:opacity-50 transition-colors whitespace-nowrap shrink-0"
          >
            <FileDown className="w-4 h-4" />
            <span>PDF তৈরি করুন</span>
          </button>
          <button
            onClick={() => setConfirmDelete(true)}
            className="inline-flex items-center gap-1.5 px-3 py-2 text-sm font-medium rounded-lg border border-rose-200 bg-white text-rose-700 hover:bg-rose-50 transition-colors whitespace-nowrap shrink-0"
          >
            <Trash2 className="w-4 h-4" />
            <span>মুছুন</span>
          </button>
        </div>
      </div>

      {/* Sub-Tabs */}
      <div className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-slate-100 p-1">
        {(
          [
            ["info", "১. শিক্ষার্থীর তথ্য"],
            ["marks", "২. প্রান্তিক নম্বর"],
            ["traits", "৩. উপস্থিতি ও গুণাবলি"],
          ] as const
        ).map(([id, label]) => (
          <button
            key={id}
            onClick={() => setTab(id)}
            className={`px-4 py-2 text-sm font-medium rounded-md transition-colors whitespace-nowrap shrink-0 ${
              tab === id
                ? "bg-white text-slate-900 shadow-xs"
                : "text-slate-600 hover:text-slate-900"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {aiError && (
        <div className="p-3 rounded-lg border border-rose-200 bg-rose-50 text-sm text-rose-800">
          {aiError}
        </div>
      )}

      {/* Tab 1: Info */}
      {tab === "info" && (
        <div className="border border-slate-200 bg-white rounded-xl p-6 space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1.5">
                শিক্ষার্থীর নাম *
              </label>
              <input
                type="text"
                value={draft.name}
                onChange={(e) =>
                  updateDraft((p) => ({ ...p, name: e.target.value }))
                }
                placeholder="যেমন: তানভীর আহমেদ"
                className="w-full px-3.5 py-2.5 text-sm rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-teal-700/20 focus:border-teal-700"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1.5">
                রোল নম্বর *
              </label>
              <input
                type="text"
                value={draft.roll === 0 ? "" : String(draft.roll)}
                onChange={(e) =>
                  updateDraft((p) => ({
                    ...p,
                    roll: Number.parseInt(fromBn(e.target.value), 10) || 0,
                  }))
                }
                placeholder="1"
                className="w-full px-3.5 py-2.5 text-sm font-mono tabular-nums rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-teal-700/20 focus:border-teal-700"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1.5">
                শ্রেণি
              </label>
              <div className="flex flex-wrap gap-1.5">
                {[1, 2, 3, 4, 5].map((c) => (
                  <button
                    key={c}
                    type="button"
                    onClick={() => updateDraft((p) => ({ ...p, cls: c }))}
                    className={`px-3.5 py-2 text-sm font-medium rounded-lg border transition-colors whitespace-nowrap ${
                      draft.cls === c
                        ? "bg-teal-700 text-white border-teal-700"
                        : "bg-white text-slate-700 border-slate-200 hover:bg-slate-50"
                    }`}
                  >
                    {CLSN[c - 1]} ({bn(String(c))})
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1.5">
                ধর্ম (ধর্মশিক্ষা বিষয়ের জন্য)
              </label>
              <div className="flex flex-wrap gap-1.5">
                {RELIGIONS.map((r) => (
                  <button
                    key={r}
                    type="button"
                    onClick={() =>
                      updateDraft((p) => ({ ...p, religion: r }))
                    }
                    className={`px-3.5 py-2 text-sm font-medium rounded-lg border transition-colors whitespace-nowrap ${
                      draft.religion === r
                        ? "bg-teal-700 text-white border-teal-700"
                        : "bg-white text-slate-700 border-slate-200 hover:bg-slate-50"
                    }`}
                  >
                    {r}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1.5">
                অভিভাবকের নাম
              </label>
              <input
                type="text"
                value={draft.guardian}
                onChange={(e) =>
                  updateDraft((p) => ({ ...p, guardian: e.target.value }))
                }
                className="w-full px-3.5 py-2.5 text-sm rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-teal-700/20 focus:border-teal-700"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1.5">
                মোবাইল নম্বর
              </label>
              <input
                type="text"
                value={draft.mobile}
                onChange={(e) =>
                  updateDraft((p) => ({ ...p, mobile: e.target.value }))
                }
                className="w-full px-3.5 py-2.5 text-sm font-mono tabular-nums rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-teal-700/20 focus:border-teal-700"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1.5">
                গ্রাম / মহল্লা
              </label>
              <input
                type="text"
                value={draft.village}
                onChange={(e) =>
                  updateDraft((p) => ({ ...p, village: e.target.value }))
                }
                className="w-full px-3.5 py-2.5 text-sm rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-teal-700/20 focus:border-teal-700"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1.5">
                  উপজেলা
                </label>
                <input
                  type="text"
                  value={draft.upazila}
                  onChange={(e) =>
                    updateDraft((p) => ({ ...p, upazila: e.target.value }))
                  }
                  className="w-full px-3.5 py-2.5 text-sm rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-teal-700/20 focus:border-teal-700"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1.5">
                  জেলা
                </label>
                <input
                  type="text"
                  value={draft.district}
                  onChange={(e) =>
                    updateDraft((p) => ({ ...p, district: e.target.value }))
                  }
                  className="w-full px-3.5 py-2.5 text-sm rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-teal-700/20 focus:border-teal-700"
                />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Tab 2: Marks */}
      {tab === "marks" && (
        <div className="space-y-5">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border border-slate-200 bg-white rounded-xl p-4">
            <div className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-slate-100 p-1">
              {[1, 2, 3].map((t) => (
                <button
                  key={t}
                  onClick={() => setTerm(t)}
                  className={`px-4 py-1.5 text-sm font-medium rounded-md transition-colors whitespace-nowrap ${
                    term === t
                      ? "bg-white text-slate-900 shadow-xs"
                      : "text-slate-600 hover:text-slate-900"
                  }`}
                >
                  {bn(String(t))} প্রান্তিক
                </button>
              ))}
            </div>
            <div className="text-sm text-slate-700 tabular-nums">
              সমন্বিত গড়:{" "}
              <span className="font-mono font-bold text-slate-900">
                {fmt(pct)}%
              </span>
              <span className="text-slate-400 mx-2">·</span>
              গ্রেড{" "}
              <span className="font-mono font-bold text-teal-700">
                {grade}
              </span>{" "}
              ({gradeLabelBn(grade)})
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {subs.map((sub) => {
              const tTotal = subTotal(draft, sub, term);
              const tCont = cont(draft, sub, term);
              const tWr = wr(draft, sub, term);
              return (
                <div
                  key={sub.key}
                  className="border border-slate-200 bg-white rounded-xl p-5 space-y-4"
                >
                  <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                    <div>
                      <h3 className="text-base font-semibold text-slate-900">
                        {sub.bn}
                      </h3>
                      <p className="text-xs text-slate-500 tabular-nums">
                        পূর্ণমান {bn(String(sub.max))} · ধারাবাহিক{" "}
                        {fmt(tCont)}
                        {sub.written > 0 ? ` · সামষ্টিক ${fmt(tWr)}` : ""}
                      </p>
                    </div>
                    <div className="text-right font-mono tabular-nums">
                      <div className="text-lg font-bold text-slate-900">
                        {fmt(tTotal)}
                      </div>
                      <div className="text-xs text-slate-400">
                        / {bn(String(sub.max))}
                      </div>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {sub.comps.map((c) => {
                      const k = mk(sub.key, term, c.key);
                      const val = draft.m[k];
                      const invalid =
                        val !== undefined && (val < 0 || val > c.max);
                      return (
                        <div key={k}>
                          <label className="block text-xs text-slate-600 mb-1 truncate">
                            {c.bn} ({bn(String(c.max))})
                          </label>
                          <input
                            type="number"
                            min={0}
                            max={c.max}
                            step="0.5"
                            value={val !== undefined ? fmtEn(val) : ""}
                            onChange={(e) => setMark(k, e.target.value)}
                            className={`w-full px-3 py-2 text-sm font-mono tabular-nums rounded-lg border focus:outline-none focus:ring-2 ${
                              invalid
                                ? "border-rose-400 bg-rose-50 focus:ring-rose-500/20"
                                : "border-slate-200 focus:ring-teal-700/20 focus:border-teal-700"
                            }`}
                          />
                        </div>
                      );
                    })}

                    {sub.written > 0 && (
                      <div>
                        <label className="block text-xs text-slate-600 mb-1 truncate">
                          সামষ্টিক (লিখিত) ({bn(String(sub.written))})
                        </label>
                        <input
                          type="number"
                          min={0}
                          max={sub.written}
                          step="0.5"
                          value={
                            draft.m[mk(sub.key, term, "WR")] !== undefined
                              ? fmtEn(draft.m[mk(sub.key, term, "WR")])
                              : ""
                          }
                          onChange={(e) =>
                            setMark(mk(sub.key, term, "WR"), e.target.value)
                          }
                          className="w-full px-3 py-2 text-sm font-mono tabular-nums rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-teal-700/20 focus:border-teal-700"
                        />
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Tab 3: Traits & Attendance */}
      {tab === "traits" && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          {[1, 2, 3].map((t) => {
            const daysVal = Number.parseFloat(
              fromBn(draft.x[`DAYS_${t}`] || "")
            );
            const presVal = Number.parseFloat(
              fromBn(draft.x[`PRES_${t}`] || "")
            );
            const attPct =
              !Number.isNaN(daysVal) &&
              !Number.isNaN(presVal) &&
              daysVal > 0
                ? `${fmt((presVal / daysVal) * 100)}%`
                : "—";
            const remKey = `REM_${t}`;
            const remText = draft.x[remKey] || "";

            return (
              <div
                key={t}
                className="border border-slate-200 bg-white rounded-xl p-5 space-y-4"
              >
                <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                  <h3 className="text-base font-semibold text-slate-900">
                    {bn(String(t))}ম প্রান্তিক
                  </h3>
                  <span className="text-xs font-mono text-slate-500 tabular-nums">
                    উপস্থিতির হার: {attPct}
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs text-slate-600 mb-1">
                      মোট কার্যদিবস
                    </label>
                    <input
                      type="text"
                      value={draft.x[`DAYS_${t}`] || ""}
                      onChange={(e) => setExtra(`DAYS_${t}`, e.target.value)}
                      placeholder="62"
                      className="w-full px-3 py-2 text-sm font-mono tabular-nums rounded-lg border border-slate-200 focus:outline-none focus:border-teal-700"
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-slate-600 mb-1">
                      উপস্থিত দিন
                    </label>
                    <input
                      type="text"
                      value={draft.x[`PRES_${t}`] || ""}
                      onChange={(e) => setExtra(`PRES_${t}`, e.target.value)}
                      placeholder="59"
                      className="w-full px-3 py-2 text-sm font-mono tabular-nums rounded-lg border border-slate-200 focus:outline-none focus:border-teal-700"
                    />
                  </div>
                </div>

                <div className="space-y-2.5 pt-1">
                  {(
                    [
                      ["সক্রিয়তা", "ACT"],
                      ["শৃঙ্খলা", "DISC"],
                      ["আচরণ", "COND"],
                    ] as const
                  ).map(([label, key]) => (
                    <div
                      key={key}
                      className="flex items-center justify-between"
                    >
                      <span className="text-sm text-slate-700">{label}</span>
                      <div className="inline-flex gap-1">
                        {["ক", "খ", "গ"].map((g) => {
                          const selected = draft.x[`${key}_${t}`] === g;
                          return (
                            <button
                              key={g}
                              type="button"
                              onClick={() =>
                                setExtra(`${key}_${t}`, selected ? "" : g)
                              }
                              className={`w-9 h-8 text-sm font-medium rounded-md border transition-colors ${
                                selected
                                  ? "bg-teal-700 text-white border-teal-700"
                                  : "bg-white text-slate-700 border-slate-200 hover:bg-slate-50"
                              }`}
                            >
                              {g}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  ))}
                </div>

                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <label className="text-xs font-medium text-slate-600">
                      শ্রেণি শিক্ষকের সার্বিক মতামত
                    </label>
                    <div className="flex items-center gap-1">
                      {remText.trim() && (
                        <button
                          type="button"
                          onClick={() =>
                            onSpeakText(remText, `rem-${draft.id}-${t}`)
                          }
                          disabled={speakingId === `rem-${draft.id}-${t}`}
                          className="inline-flex items-center gap-1 px-2 py-1 text-xs text-slate-600 hover:text-teal-700 rounded border border-slate-200 bg-slate-50"
                        >
                          {speakingId === `rem-${draft.id}-${t}` ? (
                            <Loader2 className="w-3 h-3 animate-spin" />
                          ) : (
                            <Volume2 className="w-3 h-3" />
                          )}
                          <span>শুনুন</span>
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => handleDraftRemark(t)}
                        disabled={draftingTerm === t}
                        className="inline-flex items-center gap-1 px-2 py-1 text-xs font-medium text-teal-700 hover:bg-teal-50 rounded border border-teal-200 bg-white"
                      >
                        {draftingTerm === t ? (
                          <Loader2 className="w-3 h-3 animate-spin" />
                        ) : (
                          <Sparkles className="w-3 h-3" />
                        )}
                        <span>AI খসড়া</span>
                      </button>
                    </div>
                  </div>
                  <textarea
                    rows={3}
                    value={remText}
                    onChange={(e) => setExtra(remKey, e.target.value)}
                    placeholder="পাঠ্যবিষয়ে অগ্রগতি ও মতামত লিখুন..."
                    className="w-full px-3 py-2 text-sm rounded-lg border border-slate-200 focus:outline-none focus:ring-2 focus:ring-teal-700/20 focus:border-teal-700"
                  />
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {confirmDelete && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 flex items-center justify-center p-4">
          <div className="bg-white border border-slate-200 rounded-xl max-w-sm w-full p-6 space-y-4">
            <h3 className="text-base font-semibold text-slate-900">
              শিক্ষার্থী মুছে ফেলবেন?
            </h3>
            <p className="text-sm text-slate-600">
              {draft.name || "এই শিক্ষার্থীর"} সব তথ্য, নম্বর এবং মতামত স্থায়ীভাবে মুছে যাবে।
            </p>
            <div className="flex justify-end gap-2">
              <button
                onClick={() => setConfirmDelete(false)}
                className="px-4 py-2 text-sm font-medium rounded-lg border border-slate-200 text-slate-700 hover:bg-slate-50"
              >
                বাতিল
              </button>
              <button
                onClick={() => {
                  setConfirmDelete(false);
                  onDelete(draft.id);
                }}
                className="px-4 py-2 text-sm font-medium rounded-lg bg-rose-600 text-white hover:bg-rose-700"
              >
                হ্যাঁ, মুছুন
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
