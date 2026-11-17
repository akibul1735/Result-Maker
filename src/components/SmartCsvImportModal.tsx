import React, { useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  CheckCircle2,
  ClipboardPaste,
  Database,
  FileSpreadsheet,
  Plus,
  Sparkles,
  Upload,
  X,
} from "lucide-react";
import {
  CLSN,
  DetectedCsvColumn,
  MARK_KEYS,
  STANDARD_STUDENT_FIELDS,
  Settings,
  StandardStudentFieldKey,
  Student,
  TEXT_KEYS,
  applyMappedCsvImport,
  bn,
  detectCsvColumns,
  parseCsv,
} from "../lib/models";

interface SmartCsvImportModalProps {
  isOpen: boolean;
  initialCsvText?: string;
  initialFileName?: string;
  students: Student[];
  settings: Settings;
  defaultClass: number;
  onClose: () => void;
  onImportComplete: (
    nextStudents: Student[],
    nextSettings: Settings,
    summaryMessage: string
  ) => void;
}

export const SmartCsvImportModal: React.FC<SmartCsvImportModalProps> = ({
  isOpen,
  initialCsvText = "",
  initialFileName = "",
  students,
  settings,
  defaultClass,
  onClose,
  onImportComplete,
}) => {
  const [rawText, setRawText] = useState(initialCsvText);
  const [fileName, setFileName] = useState(initialFileName);
  const [fallbackCls, setFallbackCls] = useState<number>(defaultClass);
  const [columns, setColumns] = useState<DetectedCsvColumn[]>([]);

  const existingCustomFields = settings.customFields ?? [];

  const parsedRows = useMemo(() => {
    if (!rawText.trim()) return [];
    return parseCsv(rawText);
  }, [rawText]);

  useEffect(() => {
    if (!isOpen) return;
    setRawText(initialCsvText);
    setFileName(initialFileName);
    setFallbackCls(defaultClass);
  }, [isOpen, initialCsvText, initialFileName, defaultClass]);

  useEffect(() => {
    if (parsedRows.length === 0) {
      setColumns([]);
      return;
    }
    setColumns(detectCsvColumns(parsedRows, existingCustomFields));
  }, [parsedRows, existingCustomFields]);

  const previewResult = useMemo(() => {
    if (parsedRows.length < 2 || columns.length === 0) return null;
    return applyMappedCsvImport(
      parsedRows,
      columns,
      students,
      settings,
      fallbackCls
    );
  }, [parsedRows, columns, students, settings, fallbackCls]);

  if (!isOpen) return null;

  const newColsCount = columns.filter((c) => c.isNew).length;
  const matchedColsCount = columns.length - newColsCount;
  const hasRollOrNameMapped = columns.some(
    (c) =>
      c.mapping.type === "standard" &&
      (c.mapping.field === "roll" || c.mapping.field === "name")
  );
  const dataRowCount = Math.max(0, parsedRows.length - 1);

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setFileName(file.name);
    const reader = new FileReader();
    reader.onload = () => {
      setRawText(String(reader.result || ""));
    };
    reader.readAsText(file, "UTF-8");
    e.target.value = "";
  };

  const selectValueForCol = (col: DetectedCsvColumn): string => {
    switch (col.mapping.type) {
      case "standard":
        return `std:${col.mapping.field}`;
      case "custom":
        return `cf:${col.mapping.key}`;
      case "mark":
        return `mark:${col.mapping.key}`;
      case "text":
        return `text:${col.mapping.key}`;
      case "create_new":
        return "__CREATE_NEW__";
      case "skip":
        return "__SKIP__";
    }
  };

  const updateColMapping = (colIndex: number, selectVal: string) => {
    setColumns((prev) =>
      prev.map((col) => {
        if (col.colIndex !== colIndex) return col;
        if (selectVal === "__CREATE_NEW__") {
          return {
            ...col,
            mapping: { type: "create_new", newLabel: col.header },
          };
        }
        if (selectVal === "__SKIP__") {
          return { ...col, mapping: { type: "skip" } };
        }
        if (selectVal.startsWith("std:")) {
          const field = selectVal.slice(4) as StandardStudentFieldKey;
          return { ...col, mapping: { type: "standard", field } };
        }
        if (selectVal.startsWith("cf:")) {
          const key = selectVal.slice(3);
          const found = existingCustomFields.find((cf) => cf.key === key);
          return {
            ...col,
            mapping: { type: "custom", key, label: found?.label || key },
          };
        }
        if (selectVal.startsWith("mark:")) {
          return {
            ...col,
            mapping: { type: "mark", key: selectVal.slice(5) },
          };
        }
        if (selectVal.startsWith("text:")) {
          return {
            ...col,
            mapping: { type: "text", key: selectVal.slice(5) },
          };
        }
        return col;
      })
    );
  };

  const updateNewFieldLabel = (colIndex: number, label: string) => {
    setColumns((prev) =>
      prev.map((col) =>
        col.colIndex === colIndex && col.mapping.type === "create_new"
          ? { ...col, mapping: { type: "create_new", newLabel: label } }
          : col
      )
    );
  };

  const setAllNewColumnsMode = (mode: "create" | "skip") => {
    setColumns((prev) =>
      prev.map((col) => {
        if (!col.isNew) return col;
        return {
          ...col,
          mapping:
            mode === "create"
              ? { type: "create_new", newLabel: col.header }
              : { type: "skip" },
        };
      })
    );
  };

  const handleConfirm = () => {
    if (!previewResult) return;
    const parts = [
      `নতুন ${bn(String(previewResult.addedCount))} জন`,
      `হালনাগাদ ${bn(String(previewResult.updatedCount))} জন`,
    ];
    if (previewResult.createdFields.length > 0) {
      parts.push(
        `নতুন ফিল্ড ${bn(String(previewResult.createdFields.length))}টি যুক্ত`
      );
    }
    onImportComplete(
      previewResult.students,
      previewResult.settings,
      `CSV ইমপোর্ট সম্পন্ন: ${parts.join(" · ")}।`
    );
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-[1px] flex items-center justify-center p-3 sm:p-5">
      <div className="bg-white border border-slate-200 rounded-2xl shadow-xl w-full max-w-5xl max-h-[92vh] flex flex-col overflow-hidden">
        {/* Header */}
        <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between gap-4 bg-slate-50/70">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-9 h-9 rounded-lg bg-teal-700 text-white flex items-center justify-center shrink-0">
              <FileSpreadsheet className="w-5 h-5" />
            </div>
            <div className="min-w-0">
              <h2 className="text-base font-bold text-slate-900 truncate">
                স্মার্ট CSV / Excel ইমপোর্ট ও কলাম ম্যাপিং
              </h2>
              <p className="text-xs text-slate-500 truncate">
                কলামের নাম স্বয়ংক্রিয়ভাবে শনাক্ত হবে · নতুন কলাম থাকলে নতুন ডাটাবেস ফিল্ড তৈরি বা বিদ্যমান ফিল্ড বেছে নিন
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Close modal"
            className="p-2 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto p-5 space-y-5">
          {/* Source Input Bar: File Upload or Direct Paste */}
          <div className="border border-slate-200 rounded-xl p-4 bg-slate-50/50 space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                <ClipboardPaste className="w-4 h-4 text-teal-700" />
                <span>
                  {fileName
                    ? `নির্বাচিত ফাইল: ${fileName}`
                    : "CSV / TSV ফাইল নির্বাচন করুন অথবা Excel থেকে কপি করে নিচে পেস্ট করুন"}
                </span>
              </div>
              <label className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-medium rounded-lg bg-slate-900 text-white hover:bg-slate-800 cursor-pointer shrink-0">
                <Upload className="w-3.5 h-3.5" />
                <span>CSV / TSV ফাইল বেছে নিন</span>
                <input
                  type="file"
                  accept=".csv,.tsv,.txt,text/csv,text/tab-separated-values,text/plain"
                  onChange={handleFileSelect}
                  className="hidden"
                />
              </label>
            </div>

            <textarea
              rows={parsedRows.length > 0 ? 3 : 5}
              value={rawText}
              onChange={(e) => {
                setFileName("");
                setRawText(e.target.value);
              }}
              placeholder={
                "এখানে সরাসরি Excel / Google Sheet বা CSV-এর ডাটা পেস্ট করতে পারেন...\nউদাহরণ: আইডি\tশ্রেণি\tরোল নম্বর\tনাম\tপিতার নাম\tমাতার নাম\tজন্মতারিখ\tমোবাইল নম্বর\tগ্রাম..."
              }
              className="w-full px-3 py-2 text-xs font-mono rounded-lg border border-slate-200 bg-white focus:outline-none focus:border-teal-700"
            />
          </div>

          {parsedRows.length > 0 && (
            <>
              {/* Detection Summary + Default Class */}
              <div className="grid grid-cols-1 md:grid-cols-12 gap-3 items-center border border-slate-200 rounded-xl p-4 bg-white">
                <div className="md:col-span-7 flex flex-wrap items-center gap-4 text-xs">
                  <div>
                    <span className="text-slate-500">মোট কলাম:</span>{" "}
                    <span className="font-bold text-slate-900">
                      {bn(String(columns.length))}টি
                    </span>
                  </div>
                  <div className="flex items-center gap-1 text-emerald-700 font-semibold">
                    <CheckCircle2 className="w-4 h-4" />
                    <span>শনাক্তকৃত: {bn(String(matchedColsCount))}টি</span>
                  </div>
                  {newColsCount > 0 && (
                    <div className="flex items-center gap-1 text-amber-700 font-semibold">
                      <Sparkles className="w-4 h-4" />
                      <span>নতুন কলাম: {bn(String(newColsCount))}টি</span>
                    </div>
                  )}
                  <div>
                    <span className="text-slate-500">ডাটা সারি:</span>{" "}
                    <span className="font-bold text-slate-900">
                      {bn(String(dataRowCount))} জন
                    </span>
                  </div>
                </div>

                <div className="md:col-span-5 flex flex-wrap items-center justify-start md:justify-end gap-2">
                  <label className="text-xs font-medium text-slate-600">
                    ডিফল্ট শ্রেণি (না থাকলে):
                  </label>
                  <select
                    value={fallbackCls}
                    onChange={(e) => setFallbackCls(Number(e.target.value))}
                    className="px-2.5 py-1.5 text-xs font-medium rounded-lg border border-slate-200 bg-white focus:outline-none focus:border-teal-700"
                  >
                    {[1, 2, 3, 4, 5].map((c) => (
                      <option key={c} value={c}>
                        {CLSN[c - 1]} ({bn(String(c))})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Bulk action for new columns if any */}
              {newColsCount > 0 && (
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 px-4 py-3 rounded-xl border border-amber-200 bg-amber-50/70 text-xs text-amber-900">
                  <div className="flex items-start sm:items-center gap-2">
                    <AlertCircle className="w-4 h-4 text-amber-700 shrink-0 mt-0.5 sm:mt-0" />
                    <span>
                      <strong>{bn(String(newColsCount))}টি নতুন কলাম</strong> পাওয়া গেছে যা মূল ডাটাবেসে নেই। প্রতিটি কলামের জন্য{" "}
                      <strong>নতুন ফিল্ড তৈরি</strong> করতে পারেন অথবা বিদ্যমান কোনো ফিল্ডে যুক্ত করতে পারেন।
                    </span>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <button
                      type="button"
                      onClick={() => setAllNewColumnsMode("create")}
                      className="px-2.5 py-1.5 rounded-md bg-amber-700 text-white font-medium hover:bg-amber-800 cursor-pointer"
                    >
                      সবগুলো নতুন ফিল্ড করুন
                    </button>
                    <button
                      type="button"
                      onClick={() => setAllNewColumnsMode("skip")}
                      className="px-2.5 py-1.5 rounded-md border border-amber-300 bg-white text-amber-900 font-medium hover:bg-amber-100 cursor-pointer"
                    >
                      নতুনগুলো বাদ দিন
                    </button>
                  </div>
                </div>
              )}

              {/* Column Mapping Table */}
              <div className="border border-slate-200 rounded-xl overflow-hidden">
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse text-sm">
                    <thead>
                      <tr className="bg-slate-100 border-b border-slate-200 text-xs font-semibold text-slate-600">
                        <th className="py-2.5 px-3.5 w-10 text-center">#</th>
                        <th className="py-2.5 px-3.5">CSV কলামের নাম ও নমুনা</th>
                        <th className="py-2.5 px-3.5 w-36">অবস্থা</th>
                        <th className="py-2.5 px-3.5 min-w-[300px]">
                          ডাটাবেসে যেভাবে সংরক্ষিত হবে (ফিল্ড নির্বাচন / নতুন ফিল্ড)
                        </th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200">
                      {columns.map((col, idx) => {
                        const selVal = selectValueForCol(col);
                        const isCreateNew = col.mapping.type === "create_new";
                        const isSkipped = col.mapping.type === "skip";

                        return (
                          <tr
                            key={col.colIndex}
                            className={
                              col.isNew
                                ? "bg-amber-50/25 hover:bg-amber-50/50"
                                : "bg-white hover:bg-slate-50/70"
                            }
                          >
                            <td className="py-3 px-3.5 text-center text-xs font-mono text-slate-400">
                              {bn(String(idx + 1))}
                            </td>
                            <td className="py-3 px-3.5">
                              <div className="font-semibold text-slate-900">
                                {col.header}
                              </div>
                              <div className="text-[11px] text-slate-500 mt-0.5 truncate max-w-[240px]">
                                {col.samples.length > 0
                                  ? `নমুনা: ${col.samples.join(" · ")}`
                                  : "নমুনা ডাটা নেই"}
                              </div>
                            </td>
                            <td className="py-3 px-3.5">
                              {col.isNew ? (
                                <span className="inline-flex items-center gap-1 px-2 py-1 text-[11px] font-semibold rounded-md bg-amber-100 text-amber-800">
                                  <Plus className="w-3 h-3" />
                                  নতুন কলাম
                                </span>
                              ) : (
                                <span className="inline-flex items-center gap-1 px-2 py-1 text-[11px] font-semibold rounded-md bg-emerald-100 text-emerald-800">
                                  <CheckCircle2 className="w-3 h-3" />
                                  স্বয়ংক্রিয় মিলেছে
                                </span>
                              )}
                            </td>
                            <td className="py-3 px-3.5">
                              <div className="flex flex-col sm:flex-row sm:items-center gap-2">
                                <select
                                  value={selVal}
                                  onChange={(e) =>
                                    updateColMapping(
                                      col.colIndex,
                                      e.target.value
                                    )
                                  }
                                  className={`px-2.5 py-1.5 text-xs font-medium rounded-lg border focus:outline-none focus:border-teal-700 ${
                                    isSkipped
                                      ? "border-slate-200 bg-slate-100 text-slate-500"
                                      : isCreateNew
                                        ? "border-amber-300 bg-amber-50 text-amber-900"
                                        : "border-teal-200 bg-teal-50/40 text-slate-900"
                                  }`}
                                >
                                  <option value="__CREATE_NEW__">
                                    + নতুন ফিল্ড তৈরি করে ডাটাবেসে যোগ করুন
                                  </option>
                                  <option value="__SKIP__">
                                    ✕ বাদ দিন (ইমপোর্ট করবেন না)
                                  </option>
                                  <optgroup label="মূল শিক্ষার্থী ফিল্ড">
                                    {STANDARD_STUDENT_FIELDS.map((sf) => (
                                      <option
                                        key={sf.key}
                                        value={`std:${sf.key}`}
                                      >
                                        {sf.labelBn}
                                      </option>
                                    ))}
                                  </optgroup>
                                  {existingCustomFields.length > 0 && (
                                    <optgroup label="ডাটাবেসের কাস্টম ফিল্ড">
                                      {existingCustomFields.map((cf) => (
                                        <option
                                          key={cf.key}
                                          value={`cf:${cf.key}`}
                                        >
                                          {cf.label}
                                        </option>
                                      ))}
                                    </optgroup>
                                  )}
                                  {(col.mapping.type === "mark" ||
                                    MARK_KEYS.includes(col.header)) && (
                                    <optgroup label="নম্বর কলাম">
                                      <option value={`mark:${col.header}`}>
                                        নম্বর ({col.header})
                                      </option>
                                    </optgroup>
                                  )}
                                  {(col.mapping.type === "text" ||
                                    TEXT_KEYS.includes(col.header)) && (
                                    <optgroup label="উপস্থিতি / মতামত">
                                      <option value={`text:${col.header}`}>
                                        উপস্থিতি/মতামত ({col.header})
                                      </option>
                                    </optgroup>
                                  )}
                                </select>

                                {col.mapping.type === "create_new" && (
                                  <input
                                    type="text"
                                    value={col.mapping.newLabel}
                                    onChange={(e) =>
                                      updateNewFieldLabel(
                                        col.colIndex,
                                        e.target.value
                                      )
                                    }
                                    placeholder="নতুন ফিল্ডের নাম..."
                                    className="flex-1 min-w-[150px] px-2.5 py-1.5 text-xs rounded-lg border border-amber-300 bg-white focus:outline-none focus:border-teal-700"
                                  />
                                )}
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Validation warning if no roll/name */}
              {!hasRollOrNameMapped && dataRowCount > 0 && (
                <div className="p-3 rounded-lg border border-rose-200 bg-rose-50 text-xs text-rose-800">
                  কমপক্ষে <strong>শিক্ষার্থীর নাম</strong> অথবা{" "}
                  <strong>রোল নম্বর</strong> কলাম নির্ধারণ করুন যাতে সারিগুলো শনাক্ত করা যায়।
                </div>
              )}

              {/* Resulting Summary */}
              {previewResult && dataRowCount > 0 && (
                <div className="border border-slate-200 rounded-xl p-4 bg-slate-50/70 space-y-2">
                  <div className="flex items-center gap-2 text-xs font-semibold text-slate-800">
                    <Database className="w-4 h-4 text-teal-700" />
                    <span>ইমপোর্ট সারাংশ:</span>
                    <span className="text-emerald-700">
                      নতুন শিক্ষার্থী {bn(String(previewResult.addedCount))} জন
                    </span>
                    <span className="text-slate-300">·</span>
                    <span className="text-teal-700">
                      তথ্য হালনাগাদ {bn(String(previewResult.updatedCount))} জন
                    </span>
                    {previewResult.createdFields.length > 0 && (
                      <>
                        <span className="text-slate-300">·</span>
                        <span className="text-amber-700">
                          নতুন ডাটাবেস ফিল্ড{" "}
                          {bn(String(previewResult.createdFields.length))}টি (
                          {previewResult.createdFields
                            .map((cf) => cf.label)
                            .join(", ")}
                          )
                        </span>
                      </>
                    )}
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        {/* Footer */}
        <div className="px-5 py-3.5 border-t border-slate-200 bg-slate-50/80 flex items-center justify-between gap-3">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 cursor-pointer"
          >
            বাতিল
          </button>

          <button
            type="button"
            onClick={handleConfirm}
            disabled={
              !previewResult ||
              dataRowCount === 0 ||
              !hasRollOrNameMapped ||
              previewResult.addedCount + previewResult.updatedCount === 0
            }
            className="inline-flex items-center gap-2 px-5 py-2 text-sm font-semibold rounded-lg bg-teal-700 text-white hover:bg-teal-800 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
          >
            <Database className="w-4 h-4" />
            <span>
              ডাটাবেসে ইমপোর্ট করুন
              {previewResult &&
              previewResult.addedCount + previewResult.updatedCount > 0
                ? ` (${bn(String(previewResult.addedCount + previewResult.updatedCount))} জন)`
                : ""}
            </span>
          </button>
        </div>
      </div>
    </div>
  );
};
