import React, { useEffect, useRef, useState } from "react";
import {
  BookOpen,
  FileDown,
  Loader2,
  Printer,
  Volume2,
} from "lucide-react";
import {
  CLSN,
  PDF_FONT_OPTIONS,
  PdfFontId,
  Settings,
  Student,
  bn,
  fmt,
  gradeLabelBn,
  gradeOf,
  overallPct,
} from "../lib/models";
import {
  PAGE_H,
  PAGE_W,
  ensurePdfFontLoaded,
  renderCoverPage,
  renderMarksPage,
} from "../lib/pdfRenderer";

interface ReportStudioProps {
  students: Student[];
  settings: Settings;
  onUpdateSettings: (settings: Settings) => void;
  selectedStudentId: string | null;
  onSelectStudentId: (id: string) => void;
  onExportPdf: (list: Student[], filename: string) => Promise<void>;
  onSpeakSummary: (student: Student) => Promise<void>;
  speakingId: string | null;
  exportingPdf: boolean;
}

export const ReportStudio: React.FC<ReportStudioProps> = ({
  students,
  settings,
  onUpdateSettings,
  selectedStudentId,
  onSelectStudentId,
  onExportPdf,
  onSpeakSummary,
  speakingId,
  exportingPdf,
}) => {
  const [pageView, setPageView] = useState<"both" | "cover" | "marks">("both");
  const [showFoldGuide, setShowFoldGuide] = useState(true);
  const coverCanvasRef = useRef<HTMLCanvasElement | null>(null);
  const marksCanvasRef = useRef<HTMLCanvasElement | null>(null);

  const sortedStudents = [...students].sort(
    (a, b) => a.cls - b.cls || a.roll - b.roll
  );
  const activeStudent =
    sortedStudents.find((s) => s.id === selectedStudentId) ||
    sortedStudents[0] ||
    null;

  useEffect(() => {
    if (!activeStudent) return;
    let cancelled = false;

    const paint = async () => {
      await ensurePdfFontLoaded(settings.pdfFont);
      if (cancelled) return;

      const dpr = Math.min(window.devicePixelRatio || 2, 2.5);

      if (coverCanvasRef.current) {
        const canvas = coverCanvasRef.current;
        canvas.width = Math.round(PAGE_W * dpr);
        canvas.height = Math.round(PAGE_H * dpr);
        const ctx = canvas.getContext("2d");
        if (ctx) {
          ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
          renderCoverPage(ctx, activeStudent, settings, showFoldGuide);
        }
      }

      if (marksCanvasRef.current) {
        const canvas = marksCanvasRef.current;
        canvas.width = Math.round(PAGE_W * dpr);
        canvas.height = Math.round(PAGE_H * dpr);
        const ctx = canvas.getContext("2d");
        if (ctx) {
          ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
          renderMarksPage(ctx, activeStudent, settings.pdfFont);
        }
      }
    };

    paint();
    return () => {
      cancelled = true;
    };
  }, [activeStudent, settings, showFoldGuide, pageView]);

  if (!activeStudent) {
    return (
      <div className="border border-slate-200 bg-white rounded-xl py-16 px-6 text-center">
        <div className="mx-auto w-11 h-11 rounded-full bg-slate-100 flex items-center justify-center text-slate-500 mb-3">
          <BookOpen className="w-5 h-5" />
        </div>
        <h3 className="text-base font-semibold text-slate-900">
          প্রিভিউ দেখানোর মতো কোনো শিক্ষার্থী নেই
        </h3>
        <p className="mt-1 text-sm text-slate-500">
          প্রথমে শিক্ষার্থীর তালিকায় অন্তত একজন শিক্ষার্থী যোগ করুন।
        </p>
      </div>
    );
  }

  const pct = overallPct(activeStudent);
  const grade = gradeOf(pct);

  return (
    <div className="space-y-6">
      {/* Studio Toolbar */}
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between border border-slate-200 bg-white rounded-xl p-4 print:hidden">
        <div className="flex flex-wrap items-center gap-3">
          <div>
            <label className="block text-xs font-medium text-slate-500 mb-1">
              শিক্ষার্থী নির্বাচন
            </label>
            <select
              value={activeStudent.id}
              onChange={(e) => onSelectStudentId(e.target.value)}
              className="px-3 py-2 text-sm rounded-lg border border-slate-200 bg-white text-slate-900 focus:outline-none focus:border-teal-700"
            >
              {sortedStudents.map((s) => (
                <option key={s.id} value={s.id}>
                  {CLSN[s.cls - 1]} · রোল {bn(String(s.roll))} —{" "}
                  {s.name || "(নাম নেই)"}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-500 mb-1">
              পাতা বিন্যাস (A4 Landscape)
            </label>
            <div className="inline-flex items-center gap-1 rounded-lg border border-slate-200 bg-slate-100 p-1">
              {(
                [
                  ["both", "দুই পাতা"],
                  ["cover", "পাতা-১ (কভার)"],
                  ["marks", "পাতা-২ (নম্বর)"],
                ] as const
              ).map(([mode, label]) => (
                <button
                  key={mode}
                  onClick={() => setPageView(mode)}
                  className={`px-3 py-1 text-xs font-medium rounded-md transition-colors whitespace-nowrap ${
                    pageView === mode
                      ? "bg-white text-slate-900 shadow-xs"
                      : "text-slate-600 hover:text-slate-900"
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-500 mb-1">
              প্রতিবেদনের ফন্ট (প্রাতিষ্ঠানিক)
            </label>
            <select
              value={settings.pdfFont || "noto_serif"}
              onChange={(e) =>
                onUpdateSettings({
                  ...settings,
                  pdfFont: e.target.value as PdfFontId,
                })
              }
              className="px-3 py-2 text-sm rounded-lg border border-slate-200 bg-white text-slate-900 focus:outline-none focus:border-teal-700"
            >
              {PDF_FONT_OPTIONS.map((font) => (
                <option key={font.id} value={font.id}>
                  {font.labelBn}
                </option>
              ))}
            </select>
          </div>

          <label className="inline-flex items-center gap-2 text-xs text-slate-600 pt-5 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={showFoldGuide}
              onChange={(e) => setShowFoldGuide(e.target.checked)}
              className="rounded border-slate-300 text-teal-700 focus:ring-teal-700"
            />
            <span>মাঝখানের ভাঁজ-রেখা দেখান</span>
          </label>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => onSpeakSummary(activeStudent)}
            disabled={speakingId === activeStudent.id}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 text-sm font-medium rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-50 whitespace-nowrap shrink-0"
          >
            {speakingId === activeStudent.id ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Volume2 className="w-4 h-4" />
            )}
            <span>ফলাফল শুনুন</span>
          </button>

          <button
            onClick={() => window.print()}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 text-sm font-medium rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 whitespace-nowrap shrink-0"
          >
            <Printer className="w-4 h-4" />
            <span>প্রিন্ট</span>
          </button>

          <button
            onClick={() =>
              onExportPdf(
                [activeStudent],
                `${activeStudent.name || "student"}-report.pdf`
              )
            }
            disabled={exportingPdf}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg bg-teal-700 text-white hover:bg-teal-800 disabled:opacity-50 whitespace-nowrap shrink-0"
          >
            {exportingPdf ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <FileDown className="w-4 h-4" />
            )}
            <span>PDF ডাউনলোড</span>
          </button>
        </div>
      </div>

      {/* Metadata Strip */}
      <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-slate-500 px-1 print:hidden">
        <div>
          বাইরের পাশ: বাম অর্ধেক ব্যাক কভার · ডান অর্ধেক ফ্রন্ট কভার · ভেতরের পাশ: নম্বর, গুণাবলি ও উপস্থিতি (মাঝখানে ভাঁজ করলে A5 বই)
        </div>
        <div className="font-mono tabular-nums text-slate-700">
          সমন্বিত গড় {fmt(pct)}% · গ্রেড {grade} ({gradeLabelBn(grade)})
        </div>
      </div>

      {/* Canvas Pages */}
      <div className="space-y-6">
        {(pageView === "both" || pageView === "cover") && (
          <div className="border border-slate-200 bg-white rounded-xl p-4 sm:p-6 overflow-x-auto print:border-0 print:p-0">
            <div className="text-xs font-medium text-slate-500 mb-3 print:hidden">
              পাতা-১ (বাইরের পাশ): বাম = ব্যাক কভার ও স্বাক্ষর · ডান = ফ্রন্ট কভার
            </div>
            <div className="min-w-[700px] max-w-[1040px] mx-auto border border-slate-300 shadow-xs bg-white print:border-0 print:shadow-none">
              <canvas
                ref={coverCanvasRef}
                className="w-full h-auto block"
                style={{ aspectRatio: `${PAGE_W} / ${PAGE_H}` }}
              />
            </div>
          </div>
        )}

        {(pageView === "both" || pageView === "marks") && (
          <div className="border border-slate-200 bg-white rounded-xl p-4 sm:p-6 overflow-x-auto print:border-0 print:p-0">
            <div className="text-xs font-medium text-slate-500 mb-3 print:hidden">
              পাতা-২ (ভেতরের পাশ): ৩ প্রান্তিকের নম্বর, চূড়ান্ত গ্রেড, গুণাবলি ও উপস্থিতি
            </div>
            <div className="min-w-[700px] max-w-[1040px] mx-auto border border-slate-300 shadow-xs bg-white print:border-0 print:shadow-none">
              <canvas
                ref={marksCanvasRef}
                className="w-full h-auto block"
                style={{ aspectRatio: `${PAGE_W} / ${PAGE_H}` }}
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
