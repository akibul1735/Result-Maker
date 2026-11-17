import React, { useMemo, useState } from "react";
import {
  FileDown,
  Pencil,
  Plus,
  Search,
  Volume2,
  FileText,
  Loader2,
} from "lucide-react";
import {
  CLSN,
  CLS_SHORT,
  Settings,
  Student,
  bn,
  fmt,
  gradeLabelBn,
  gradeOf,
  hasTerm,
  overallPct,
} from "../lib/models";

interface StudentRosterProps {
  students: Student[];
  settings: Settings;
  selectedClass: number;
  onSelectClass: (cls: number) => void;
  onOpenStudent: (student: Student) => void;
  onNewStudent: () => void;
  onPreviewPdf: (student: Student) => void;
  onExportClassPdf: (cls: number) => void;
  onSpeakSummary: (student: Student) => Promise<void>;
  speakingId: string | null;
  exportingPdf: boolean;
}

export const StudentRoster: React.FC<StudentRosterProps> = ({
  students,
  settings,
  selectedClass,
  onSelectClass,
  onOpenStudent,
  onNewStudent,
  onPreviewPdf,
  onExportClassPdf,
  onSpeakSummary,
  speakingId,
  exportingPdf,
}) => {
  const [query, setQuery] = useState("");

  const classStudents = useMemo(() => {
    const q = query.trim().toLowerCase();
    return students
      .filter((s) => s.cls === selectedClass)
      .filter((s) => {
        if (!q) return true;
        return (
          s.name.toLowerCase().includes(q) ||
          String(s.roll).includes(q) ||
          bn(String(s.roll)).includes(q) ||
          s.guardian.toLowerCase().includes(q)
        );
      })
      .sort((a, b) => a.roll - b.roll);
  }, [students, selectedClass, query]);

  const classMetrics = useMemo(() => {
    const allInClass = students.filter((s) => s.cls === selectedClass);
    const evaluated = allInClass.filter((s) =>
      [1, 2, 3].some((t) => hasTerm(s, t))
    );
    const avgPct =
      evaluated.length > 0
        ? evaluated.reduce((acc, s) => acc + overallPct(s), 0) / evaluated.length
        : 0;
    const gradeACount = evaluated.filter(
      (s) => gradeOf(overallPct(s)) === "A"
    ).length;
    return {
      total: allInClass.length,
      evaluated: evaluated.length,
      avgPct,
      gradeACount,
    };
  }, [students, selectedClass]);

  const classTeacher = settings.teachers[selectedClass] || "অনির্ধারিত";

  return (
    <div className="space-y-6">
      {/* Class Segmented Selector & Toolbar */}
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div
          className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-slate-100 p-1 overflow-x-auto"
          role="tablist"
          aria-label="Class selector"
        >
          {[1, 2, 3, 4, 5].map((c) => {
            const active = selectedClass === c;
            const count = students.filter((s) => s.cls === c).length;
            return (
              <button
                key={c}
                role="tab"
                aria-selected={active}
                onClick={() => onSelectClass(c)}
                className={`px-3.5 py-2 text-sm font-medium rounded-md transition-colors whitespace-nowrap shrink-0 tabular-nums ${
                  active
                    ? "bg-white text-slate-900 shadow-xs border border-slate-200/60"
                    : "text-slate-600 hover:text-slate-900"
                }`}
              >
                {CLS_SHORT[c - 1]} শ্রেণি ({bn(String(count))})
              </button>
            );
          })}
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="নাম বা রোল খুঁজুন..."
              className="pl-9 pr-3 py-2 text-sm bg-white border border-slate-200 rounded-lg text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-teal-700/20 focus:border-teal-700 w-56"
            />
          </div>
          <button
            onClick={() => onExportClassPdf(selectedClass)}
            disabled={classStudents.length === 0 || exportingPdf}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors whitespace-nowrap shrink-0"
          >
            {exportingPdf ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <FileDown className="w-4 h-4" />
            )}
            <span>শ্রেণির সব PDF</span>
          </button>
        </div>
      </div>

      {/* Summary Strip */}
      <div className="grid grid-cols-2 lg:grid-cols-4 border border-slate-200 bg-white rounded-xl divide-y lg:divide-y-0 lg:divide-x divide-slate-200">
        <div className="p-4">
          <div className="text-xs font-medium text-slate-500">শ্রেণি ও শিক্ষক</div>
          <div className="mt-1 text-base font-semibold text-slate-900 truncate">
            {CLSN[selectedClass - 1]} শ্রেণি
          </div>
          <div className="mt-0.5 text-xs text-slate-500 truncate">
            {classTeacher}
          </div>
        </div>
        <div className="p-4">
          <div className="text-xs font-medium text-slate-500">মোট শিক্ষার্থী</div>
          <div className="mt-1 text-2xl font-semibold text-slate-900 tabular-nums">
            {bn(String(classMetrics.total))} জন
          </div>
          <div className="mt-0.5 text-xs text-slate-500 tabular-nums">
            মূল্যায়িত {bn(String(classMetrics.evaluated))} জন
          </div>
        </div>
        <div className="p-4">
          <div className="text-xs font-medium text-slate-500">শ্রেণির গড় অর্জন</div>
          <div className="mt-1 text-2xl font-semibold text-slate-900 tabular-nums">
            {classMetrics.evaluated > 0 ? `${fmt(classMetrics.avgPct)}%` : "—"}
          </div>
          <div className="mt-0.5 text-xs text-slate-500">
            {classMetrics.evaluated > 0
              ? `গ্রেড ${gradeOf(classMetrics.avgPct)} · ${gradeLabelBn(gradeOf(classMetrics.avgPct))}`
              : "নম্বর যুক্ত হলে হিসাব হবে"}
          </div>
        </div>
        <div className="p-4">
          <div className="text-xs font-medium text-slate-500">অতি উত্তম (Grade A)</div>
          <div className="mt-1 text-2xl font-semibold text-teal-700 tabular-nums">
            {bn(String(classMetrics.gradeACount))} জন
          </div>
          <div className="mt-0.5 text-xs text-slate-500">৮০% বা তদূর্ধ্ব নম্বর</div>
        </div>
      </div>

      {/* Student Roster Table */}
      <div className="border border-slate-200 bg-white rounded-xl overflow-hidden">
        {classStudents.length === 0 ? (
          <div className="py-16 px-6 text-center">
            <div className="mx-auto w-11 h-11 rounded-full bg-slate-100 flex items-center justify-center text-slate-500 mb-3">
              <FileText className="w-5 h-5" />
            </div>
            <h3 className="text-base font-semibold text-slate-900">
              {query
                ? "অনুসন্ধানে কোনো শিক্ষার্থী পাওয়া যায়নি"
                : `${CLSN[selectedClass - 1]} শ্রেণিতে এখনো কোনো শিক্ষার্থী নেই`}
            </h3>
            <p className="mt-1 text-sm text-slate-500 max-w-md mx-auto">
              নতুন শিক্ষার্থী যোগ করুন অথবা সেটিংস থেকে CSV / JSON ফাইল ইমপোর্ট করুন।
            </p>
            <div className="mt-5">
              <button
                onClick={onNewStudent}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg bg-teal-700 text-white hover:bg-teal-800 transition-colors whitespace-nowrap"
              >
                <Plus className="w-4 h-4" />
                <span>নতুন শিক্ষার্থী যোগ করুন</span>
              </button>
            </div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/70 text-xs font-medium text-slate-500">
                  <th className="py-3 px-4 w-20">রোল</th>
                  <th className="py-3 px-4">শিক্ষার্থীর নাম ও অভিভাবক</th>
                  <th className="py-3 px-4 hidden md:table-cell">ধর্ম ও ঠিকানা</th>
                  <th className="py-3 px-4 text-right">গড় শতাংশ</th>
                  <th className="py-3 px-4">সমন্বিত গ্রেড</th>
                  <th className="py-3 px-4 text-right">কার্যক্রম</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 text-sm">
                {classStudents.map((s) => {
                  const evaluated = [1, 2, 3].some((t) => hasTerm(s, t));
                  const pct = overallPct(s);
                  const grade = gradeOf(pct);
                  const gradeColor =
                    grade === "A"
                      ? "text-emerald-700"
                      : grade === "B"
                        ? "text-teal-700"
                        : grade === "C"
                          ? "text-amber-700"
                          : "text-rose-700";

                  return (
                    <tr
                      key={s.id}
                      className="hover:bg-slate-50/80 transition-colors"
                    >
                      <td className="py-3.5 px-4 font-mono font-semibold text-slate-900 tabular-nums">
                        {bn(String(s.roll).padStart(2, "0"))}
                      </td>
                      <td className="py-3.5 px-4">
                        <button
                          onClick={() => onOpenStudent(s)}
                          className="text-left group focus:outline-none"
                        >
                          <div className="font-semibold text-slate-900 group-hover:text-teal-700 transition-colors">
                            {s.name || "(নাম নেই)"}
                          </div>
                          <div className="text-xs text-slate-500 mt-0.5">
                            {s.guardian ? `অভিভাবক: ${s.guardian}` : "অভিভাবকের তথ্য নেই"}
                            {s.mobile ? ` · ${bn(s.mobile)}` : ""}
                          </div>
                        </button>
                      </td>
                      <td className="py-3.5 px-4 hidden md:table-cell text-xs text-slate-600">
                        <div>{s.religion}</div>
                        <div className="text-slate-400 mt-0.5 truncate max-w-[220px]">
                          {[s.village, s.upazila, s.district]
                            .filter(Boolean)
                            .join(", ") || "ঠিকানা অনির্ধারিত"}
                        </div>
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono font-medium text-slate-900 tabular-nums">
                        {evaluated ? `${fmt(pct)}%` : "—"}
                      </td>
                      <td className="py-3.5 px-4">
                        {evaluated ? (
                          <div className={`font-medium ${gradeColor}`}>
                            <span className="font-mono font-bold">{grade}</span>
                            <span className="text-slate-400 mx-1.5">·</span>
                            <span className="text-xs">{gradeLabelBn(grade)}</span>
                          </div>
                        ) : (
                          <span className="text-xs text-slate-400">নম্বর নেই</span>
                        )}
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            onClick={() => onSpeakSummary(s)}
                            disabled={speakingId === s.id}
                            title="ফলাফল সারসংক্ষেপ শুনুন (TTS)"
                            className="p-2 rounded-lg text-slate-600 hover:text-teal-700 hover:bg-slate-100 transition-colors disabled:opacity-50"
                          >
                            {speakingId === s.id ? (
                              <Loader2 className="w-4 h-4 animate-spin" />
                            ) : (
                              <Volume2 className="w-4 h-4" />
                            )}
                          </button>
                          <button
                            onClick={() => onPreviewPdf(s)}
                            title="প্রতিবেদন PDF প্রিভিউ"
                            className="p-2 rounded-lg text-slate-600 hover:text-teal-700 hover:bg-slate-100 transition-colors"
                          >
                            <FileText className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => onOpenStudent(s)}
                            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 transition-colors whitespace-nowrap shrink-0"
                          >
                            <Pencil className="w-3.5 h-3.5" />
                            <span>সম্পাদনা</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
