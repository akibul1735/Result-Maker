import React, { useEffect, useMemo, useRef, useState } from "react";
import {
  ArrowDown,
  ArrowRight,
  FileDown,
  Grid2X2,
  Plus,
  UserPlus,
} from "lucide-react";
import {
  CLSN,
  RELIGIONS,
  Student,
  bn,
  cont,
  createEmptyStudent,
  fmt,
  fmtEn,
  fromBn,
  gradeOf,
  mk,
  subTotal,
  subjectsFor,
} from "../lib/models";

type SheetMode = "subject" | "all_subjects" | "attendance";
type AllSubjectField = "PRIMARY" | "TB" | "ACT" | "LANG" | "CT";

interface SheetColDef {
  id: string;
  label: string;
  subLabel?: string;
  max: number;
  kind: "mark" | "text_num" | "trait";
  resolveKey: (student: Student) => {
    bucket: "m" | "x";
    key: string;
    max: number;
  } | null;
}

interface BulkSheetEntryProps {
  students: Student[];
  selectedClass: number;
  onSelectClass: (cls: number) => void;
  onUpsertStudent: (s: Student) => void;
  onBatchUpdateStudents: (updated: Student[]) => void;
  onOpenStudent: (s: Student) => void;
  onExportClassPdf: (cls: number) => void;
  onShowToast: (msg: string) => void;
  exportingPdf: boolean;
}

interface CellProps {
  value: string;
  placeholder?: string;
  max?: number;
  isText?: boolean;
  onCommit: (val: string) => void;
  registerRef: (el: HTMLInputElement | null) => void;
  onNavigate: (dr: number, dc: number) => void;
  onPasteGrid: (text: string) => void;
  enterMovesDown: boolean;
  align?: "center" | "left";
}

const SheetCellInput: React.FC<CellProps> = ({
  value,
  placeholder,
  max,
  isText = false,
  onCommit,
  registerRef,
  onNavigate,
  onPasteGrid,
  enterMovesDown,
  align = "center",
}) => {
  const [focused, setFocused] = useState(false);
  const [draft, setDraft] = useState(value);

  useEffect(() => {
    if (!focused) {
      setDraft(value);
    }
  }, [value, focused]);

  const numericVal = !isText && draft !== "" ? Number.parseFloat(fromBn(draft)) : Number.NaN;
  const isInvalid =
    !isText &&
    draft !== "" &&
    (Number.isNaN(numericVal) || numericVal < 0 || (max !== undefined && numericVal > max));

  const commit = (raw: string) => {
    if (isText) {
      onCommit(raw.trim());
      return;
    }
    const clean = fromBn(raw).trim();
    if (clean === "") {
      onCommit("");
      return;
    }
    const num = Number.parseFloat(clean);
    if (!Number.isNaN(num)) {
      const clamped =
        max !== undefined ? Math.max(0, Math.min(max, num)) : Math.max(0, num);
      onCommit(fmtEn(clamped));
    }
  };

  return (
    <input
      ref={registerRef}
      type="text"
      inputMode={isText ? "text" : "decimal"}
      value={focused ? draft : value}
      placeholder={placeholder}
      onFocus={(e) => {
        setFocused(true);
        setDraft(value);
        e.currentTarget.select();
      }}
      onChange={(e) => {
        const next = e.target.value;
        setDraft(next);
        // Immediate live sync when valid number or empty
        if (!isText) {
          const clean = fromBn(next).trim();
          if (clean === "") {
            onCommit("");
          } else {
            const num = Number.parseFloat(clean);
            if (!Number.isNaN(num) && num >= 0 && (max === undefined || num <= max)) {
              onCommit(fmtEn(num));
            }
          }
        }
      }}
      onBlur={() => {
        setFocused(false);
        commit(draft);
      }}
      onPaste={(e) => {
        const text = e.clipboardData.getData("text");
        if (text.includes("\n") || text.includes("\t")) {
          e.preventDefault();
          onPasteGrid(text);
        }
      }}
      onKeyDown={(e) => {
        if (e.key === "Enter") {
          e.preventDefault();
          commit(draft);
          if (e.shiftKey) {
            onNavigate(enterMovesDown ? -1 : 0, enterMovesDown ? 0 : -1);
          } else {
            onNavigate(enterMovesDown ? 1 : 0, enterMovesDown ? 0 : 1);
          }
        } else if (e.key === "ArrowDown") {
          e.preventDefault();
          commit(draft);
          onNavigate(1, 0);
        } else if (e.key === "ArrowUp") {
          e.preventDefault();
          commit(draft);
          onNavigate(-1, 0);
        } else if (
          e.key === "ArrowRight" &&
          e.currentTarget.selectionStart === e.currentTarget.value.length
        ) {
          e.preventDefault();
          commit(draft);
          onNavigate(0, 1);
        } else if (
          e.key === "ArrowLeft" &&
          e.currentTarget.selectionStart === 0
        ) {
          e.preventDefault();
          commit(draft);
          onNavigate(0, -1);
        }
      }}
      className={`w-full h-9 px-1.5 text-sm font-mono tabular-nums bg-transparent focus:outline-none focus:bg-teal-50/80 focus:ring-2 focus:ring-inset focus:ring-teal-700 transition-colors ${
        align === "center" ? "text-center" : "text-left"
      } ${isInvalid ? "bg-rose-50 text-rose-700 font-semibold" : "text-slate-900"}`}
    />
  );
};

export const BulkSheetEntry: React.FC<BulkSheetEntryProps> = ({
  students,
  selectedClass,
  onSelectClass,
  onUpsertStudent,
  onBatchUpdateStudents,
  onOpenStudent,
  onExportClassPdf,
  onShowToast,
  exportingPdf,
}) => {
  const [term, setTerm] = useState<number>(1);
  const [mode, setMode] = useState<SheetMode>("subject");
  const [allSubField, setAllSubField] = useState<AllSubjectField>("PRIMARY");
  const [enterMovesDown, setEnterMovesDown] = useState<boolean>(true);

  const classSubs = useMemo(
    () => subjectsFor(selectedClass, "ইসলাম"),
    [selectedClass]
  );
  const [selectedSubKey, setSelectedSubKey] = useState<string>("BAN");

  useEffect(() => {
    if (!classSubs.some((sub) => sub.key === selectedSubKey)) {
      setSelectedSubKey(classSubs[0]?.key || "BAN");
    }
  }, [classSubs, selectedSubKey]);

  const classStudents = useMemo(
    () =>
      students
        .filter((s) => s.cls === selectedClass)
        .sort((a, b) => a.roll - b.roll || a.name.localeCompare(b.name)),
    [students, selectedClass]
  );

  // Quick Add student state
  const suggestedRoll = useMemo(
    () =>
      classStudents.reduce((max, s) => Math.max(max, s.roll), 0) + 1,
    [classStudents]
  );
  const [newRoll, setNewRoll] = useState<string>("");
  const [newName, setNewName] = useState<string>("");
  const [newReligion, setNewReligion] = useState<string>(RELIGIONS[0]);
  const newNameInputRef = useRef<HTMLInputElement | null>(null);

  const activeSub =
    classSubs.find((s) => s.key === selectedSubKey) || classSubs[0];

  const columns: SheetColDef[] = useMemo(() => {
    if (mode === "subject" && activeSub) {
      const cols: SheetColDef[] = activeSub.comps.map((c) => ({
        id: `${activeSub.key}_${c.key}`,
        label:
          c.key === "TB"
            ? "পাঠ্যবই"
            : c.key === "ACT"
              ? "সক্রিয়তা"
              : c.key === "LANG"
                ? "ভাষাদক্ষতা"
                : c.key === "CT"
                  ? "ক্লাস টেস্ট"
                  : "মোট নম্বর",
        subLabel: `সর্বোচ্চ ${bn(String(c.max))}`,
        max: c.max,
        kind: "mark",
        resolveKey: () => ({
          bucket: "m",
          key: mk(activeSub.key, term, c.key),
          max: c.max,
        }),
      }));
      if (activeSub.written > 0) {
        cols.push({
          id: `${activeSub.key}_WR`,
          label: "সামষ্টিক",
          subLabel: `সর্বোচ্চ ${bn(String(activeSub.written))}`,
          max: activeSub.written,
          kind: "mark",
          resolveKey: () => ({
            bucket: "m",
            key: mk(activeSub.key, term, "WR"),
            max: activeSub.written,
          }),
        });
      }
      return cols;
    }

    if (mode === "all_subjects") {
      return classSubs.map((sub) => {
        if (allSubField === "PRIMARY") {
          const targetComp =
            sub.written > 0
              ? { key: "WR", label: "সামষ্টিক", max: sub.written }
              : sub.key === "ART"
                ? { key: "TOTAL", label: "মোট", max: sub.max }
                : { key: "TB", label: "পাঠ্যবই", max: sub.comps[0]?.max ?? 20 };

          return {
            id: `${sub.key}_${targetComp.key}`,
            label: sub.key === "REL" ? "ধর্মশিক্ষা" : sub.bn,
            subLabel: `${targetComp.label} (${bn(String(targetComp.max))})`,
            max: targetComp.max,
            kind: "mark",
            resolveKey: () => ({
              bucket: "m",
              key: mk(sub.key, term, targetComp.key),
              max: targetComp.max,
            }),
          };
        }

        const comp =
          sub.key === "ART"
            ? sub.comps[0]
            : sub.comps.find((c) => c.key === allSubField) || sub.comps[0];
        return {
          id: `${sub.key}_${comp.key}`,
          label: sub.key === "REL" ? "ধর্মশিক্ষা" : sub.bn,
          subLabel: `${comp.key === "TOTAL" ? "মোট" : comp.key} (${bn(String(comp.max))})`,
          max: comp.max,
          kind: "mark",
          resolveKey: () => ({
            bucket: "m",
            key: mk(sub.key, term, comp.key),
            max: comp.max,
          }),
        };
      });
    }

    // attendance & traits mode
    return [
      {
        id: `DAYS_${term}`,
        label: "কার্যদিবস",
        subLabel: "দিন",
        max: 365,
        kind: "text_num",
        resolveKey: () => ({ bucket: "x", key: `DAYS_${term}`, max: 365 }),
      },
      {
        id: `PRES_${term}`,
        label: "উপস্থিতি",
        subLabel: "দিন",
        max: 365,
        kind: "text_num",
        resolveKey: () => ({ bucket: "x", key: `PRES_${term}`, max: 365 }),
      },
      {
        id: `ACT_${term}`,
        label: "সক্রিয়তা",
        subLabel: "ক / খ / গ",
        max: 1,
        kind: "trait",
        resolveKey: () => ({ bucket: "x", key: `ACT_${term}`, max: 1 }),
      },
      {
        id: `DISC_${term}`,
        label: "শৃঙ্খলা",
        subLabel: "ক / খ / গ",
        max: 1,
        kind: "trait",
        resolveKey: () => ({ bucket: "x", key: `DISC_${term}`, max: 1 }),
      },
      {
        id: `COND_${term}`,
        label: "আচরণ",
        subLabel: "ক / খ / গ",
        max: 1,
        kind: "trait",
        resolveKey: () => ({ bucket: "x", key: `COND_${term}`, max: 1 }),
      },
    ];
  }, [mode, activeSub, classSubs, allSubField, term]);

  // Ref grid [row][col] for instant spreadsheet cursor jumps
  const inputRefs = useRef<Record<string, HTMLInputElement | null>>({});

  const focusCell = (r: number, c: number) => {
    if (r < 0 || r >= classStudents.length || c < 0 || c >= columns.length) {
      return;
    }
    const el = inputRefs.current[`${r}:${c}`];
    if (el) {
      el.focus();
      el.select();
    }
  };

  const navigateFrom = (r: number, c: number, dr: number, dc: number) => {
    let nr = r + dr;
    let nc = c + dc;
    if (nr >= classStudents.length) {
      // Wrap smoothly to top of next column when reaching bottom with Enter
      if (dc === 0 && dr > 0 && nc + 1 < columns.length) {
        nr = 0;
        nc += 1;
      } else {
        nr = classStudents.length - 1;
      }
    }
    if (nr < 0) {
      nr = 0;
    }
    if (nc >= columns.length) {
      nc = columns.length - 1;
    }
    if (nc < 0) {
      nc = 0;
    }
    focusCell(nr, nc);
  };

  const getCellDisplay = (s: Student, col: SheetColDef): string => {
    const resolved = col.resolveKey(s);
    if (!resolved) return "";
    if (resolved.bucket === "m") {
      const val = s.m[resolved.key];
      return val !== undefined ? fmtEn(val) : "";
    }
    return s.x[resolved.key] ?? "";
  };

  const applyCellValue = (s: Student, col: SheetColDef, rawVal: string): Student => {
    const resolved = col.resolveKey(s);
    if (!resolved) return s;

    if (resolved.bucket === "m") {
      const nextM = { ...s.m };
      if (rawVal === "") {
        delete nextM[resolved.key];
      } else {
        const parsed = Number.parseFloat(fromBn(rawVal));
        if (!Number.isNaN(parsed)) {
          nextM[resolved.key] = Math.max(0, Math.min(resolved.max, parsed));
        }
      }
      return { ...s, m: nextM };
    }

    const nextX = { ...s.x };
    if (col.kind === "text_num") {
      const clean = fromBn(rawVal).trim();
      if (clean === "") {
        delete nextX[resolved.key];
      } else {
        const parsed = Number.parseFloat(clean);
        if (!Number.isNaN(parsed)) {
          nextX[resolved.key] = String(Math.max(0, Math.round(parsed)));
        }
      }
    } else {
      const clean = rawVal.trim();
      if (clean === "") delete nextX[resolved.key];
      else nextX[resolved.key] = clean;
    }
    return { ...s, x: nextX };
  };

  const handleCellCommit = (student: Student, col: SheetColDef, rawVal: string) => {
    const prevVal = getCellDisplay(student, col);
    if (prevVal === rawVal) return;
    onUpsertStudent(applyCellValue(student, col, rawVal));
  };

  const handlePasteGrid = (startRow: number, startCol: number, text: string) => {
    const rowLines = text.replace(/\r\n/g, "\n").replace(/\r/g, "\n").split("\n");
    if (rowLines.length > 0 && rowLines[rowLines.length - 1] === "") {
      rowLines.pop();
    }
    if (rowLines.length === 0) return;

    const updatedMap = new Map<string, Student>();
    let cellsApplied = 0;

    rowLines.forEach((line, rOff) => {
      const rIdx = startRow + rOff;
      if (rIdx >= classStudents.length) return;
      const baseStudent = updatedMap.get(classStudents[rIdx].id) || classStudents[rIdx];
      let working = baseStudent;

      line.split("\t").forEach((cellVal, cOff) => {
        const cIdx = startCol + cOff;
        if (cIdx >= columns.length) return;
        working = applyCellValue(working, columns[cIdx], cellVal.trim());
        cellsApplied++;
      });

      updatedMap.set(working.id, working);
    });

    if (updatedMap.size > 0) {
      const merged = students.map((st) => updatedMap.get(st.id) || st);
      onBatchUpdateStudents(merged);
      onShowToast(`${bn(String(cellsApplied))}টি ঘরের তথ্য পেস্ট করা হয়েছে।`);
    }
  };

  const handleQuickAddStudent = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmedName = newName.trim();
    if (!trimmedName) return;
    const parsedRoll =
      Number.parseInt(fromBn(newRoll.trim()), 10) || suggestedRoll;
    const fresh: Student = {
      ...createEmptyStudent(selectedClass, parsedRoll),
      name: trimmedName,
      religion: newReligion,
    };
    onUpsertStudent(fresh);
    setNewName("");
    setNewRoll("");
    onShowToast(`${trimmedName} (রোল ${bn(String(parsedRoll))}) যোগ হয়েছে।`);
    newNameInputRef.current?.focus();
  };

  return (
    <div className="space-y-5">
      {/* Header & Top Controls */}
      <div className="border border-slate-200 bg-white rounded-xl p-4 sm:p-5 space-y-4">
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <Grid2X2 className="w-5 h-5 text-teal-700" />
              <h1 className="text-lg font-bold text-slate-900">
                দ্রুত শিট এন্ট্রি (Sheet / Bulk Marks Entry)
              </h1>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              বামে রোল ও নাম স্থির রেখে ডানদিকে একসাথে নম্বর এন্ট্রি করুন। Enter চাপলে স্বয়ংক্রিয়ভাবে নিচের ঘরে চলে যাবে।
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={() => setEnterMovesDown((v) => !v)}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 bg-slate-50 text-slate-700 hover:bg-slate-100 cursor-pointer"
            >
              {enterMovesDown ? (
                <>
                  <ArrowDown className="w-3.5 h-3.5 text-teal-700" />
                  <span>Enter: নিচের ঘর (↓)</span>
                </>
              ) : (
                <>
                  <ArrowRight className="w-3.5 h-3.5 text-teal-700" />
                  <span>Enter: ডানের ঘর (→)</span>
                </>
              )}
            </button>

            <button
              type="button"
              onClick={() => onExportClassPdf(selectedClass)}
              disabled={classStudents.length === 0 || exportingPdf}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 text-xs font-medium rounded-lg bg-teal-700 text-white hover:bg-teal-800 disabled:opacity-50 cursor-pointer"
            >
              <FileDown className="w-3.5 h-3.5" />
              <span>{CLSN[selectedClass - 1]} শ্রেণির PDF</span>
            </button>
          </div>
        </div>

        {/* Class & Term Row */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 pt-2 border-t border-slate-100">
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="text-xs font-medium text-slate-500 mr-1">শ্রেণি:</span>
            {[1, 2, 3, 4, 5].map((cls) => {
              const active = selectedClass === cls;
              const cnt = students.filter((s) => s.cls === cls).length;
              return (
                <button
                  key={cls}
                  type="button"
                  onClick={() => onSelectClass(cls)}
                  className={`px-3 py-1.5 text-xs font-medium rounded-lg border transition-colors cursor-pointer ${
                    active
                      ? "border-teal-700 bg-teal-700 text-white"
                      : "border-slate-200 bg-white text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  {CLSN[cls - 1]} ({bn(String(cnt))})
                </button>
              );
            })}
          </div>

          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="text-xs font-medium text-slate-500 mr-1">প্রান্তিক:</span>
            {[1, 2, 3].map((t) => {
              const active = term === t;
              return (
                <button
                  key={t}
                  type="button"
                  onClick={() => setTerm(t)}
                  className={`px-3 py-1.5 text-xs font-medium rounded-lg border transition-colors cursor-pointer ${
                    active
                      ? "border-slate-900 bg-slate-900 text-white"
                      : "border-slate-200 bg-white text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  {bn(String(t))} প্রান্তিক
                </button>
              );
            })}
          </div>
        </div>

        {/* Sheet Mode & Subject / Field Selector */}
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3 pt-2 border-t border-slate-100">
          <div className="flex items-center gap-1.5 flex-wrap">
            {(
              [
                ["subject", "বিষয়ভিত্তিক শিট (সব অংশ)"],
                ["all_subjects", "সকল বিষয় (একসাথে)"],
                ["attendance", "উপস্থিতি ও গুণাবলি"],
              ] as const
            ).map(([mId, mLabel]) => {
              const active = mode === mId;
              return (
                <button
                  key={mId}
                  type="button"
                  onClick={() => setMode(mId)}
                  className={`px-3 py-1.5 text-xs font-semibold rounded-lg border transition-colors cursor-pointer ${
                    active
                      ? "border-teal-700 bg-teal-50 text-teal-800"
                      : "border-slate-200 bg-white text-slate-600 hover:bg-slate-50"
                  }`}
                >
                  {mLabel}
                </button>
              );
            })}
          </div>

          {mode === "subject" && (
            <div className="flex items-center gap-1.5 flex-wrap">
              {classSubs.map((sub) => {
                const active = activeSub?.key === sub.key;
                const shortName =
                  sub.key === "REL"
                    ? "ধর্মশিক্ষা"
                    : sub.key === "SOC"
                      ? "সামাজিক ও বিজ্ঞান"
                      : sub.key === "PE"
                        ? "শারীরিক শিক্ষা"
                        : sub.bn;
                return (
                  <button
                    key={sub.key}
                    type="button"
                    onClick={() => setSelectedSubKey(sub.key)}
                    className={`px-2.5 py-1 text-xs font-medium rounded-md border transition-colors cursor-pointer ${
                      active
                        ? "border-teal-700 bg-teal-700 text-white"
                        : "border-slate-200 bg-slate-50 text-slate-700 hover:bg-slate-100"
                    }`}
                  >
                    {shortName}
                  </button>
                );
              })}
            </div>
          )}

          {mode === "all_subjects" && (
            <div className="flex items-center gap-1.5 flex-wrap">
              <span className="text-xs text-slate-500 mr-1">কলামের ধরন:</span>
              {(
                [
                  ["PRIMARY", "সামষ্টিক / মূল নম্বর"],
                  ["TB", "পাঠ্যবই (TB)"],
                  ["CT", "ক্লাস টেস্ট (CT)"],
                  ["ACT", "সক্রিয়তা (ACT)"],
                  ["LANG", "ভাষাদক্ষতা (LANG)"],
                ] as const
              ).map(([fId, fLabel]) => {
                const active = allSubField === fId;
                return (
                  <button
                    key={fId}
                    type="button"
                    onClick={() => setAllSubField(fId)}
                    className={`px-2.5 py-1 text-xs font-medium rounded-md border transition-colors cursor-pointer ${
                      active
                        ? "border-teal-700 bg-teal-700 text-white"
                        : "border-slate-200 bg-slate-50 text-slate-700 hover:bg-slate-100"
                    }`}
                  >
                    {fLabel}
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Spreadsheet Grid Table */}
      <div className="border border-slate-200 bg-white rounded-xl overflow-hidden">
        <div className="overflow-x-auto max-h-[62vh] overflow-y-auto">
          <table className="w-full border-collapse text-sm">
            <thead className="sticky top-0 z-20 bg-slate-100 border-b border-slate-300">
              <tr>
                <th className="sticky left-0 z-30 bg-slate-100 px-2.5 py-2 text-center text-xs font-bold text-slate-700 border-r border-slate-200 w-16 min-w-[64px]">
                  রোল
                </th>
                <th className="sticky left-16 z-30 bg-slate-100 px-3 py-2 text-left text-xs font-bold text-slate-700 border-r-2 border-slate-300 min-w-[170px] max-w-[210px]">
                  শিক্ষার্থীর নাম
                </th>

                {columns.map((col) => (
                  <th
                    key={col.id}
                    className="px-2 py-2 text-center text-xs font-semibold text-slate-800 border-r border-slate-200 min-w-[96px]"
                  >
                    <div className="truncate">{col.label}</div>
                    {col.subLabel && (
                      <div className="text-[11px] font-normal text-slate-500 mt-0.5">
                        {col.subLabel}
                      </div>
                    )}
                  </th>
                ))}

                {mode === "subject" && activeSub && (
                  <>
                    <th className="px-2.5 py-2 text-center text-xs font-semibold text-slate-700 bg-slate-50 border-r border-slate-200 min-w-[78px]">
                      <div>ধারাবাহিক</div>
                      <div className="text-[11px] font-normal text-slate-500">
                        ({bn(String(activeSub.max - activeSub.written))})
                      </div>
                    </th>
                    <th className="px-2.5 py-2 text-center text-xs font-bold text-teal-800 bg-teal-50/70 border-r border-slate-200 min-w-[78px]">
                      <div>মোট</div>
                      <div className="text-[11px] font-normal text-teal-700">
                        ({bn(String(activeSub.max))})
                      </div>
                    </th>
                    <th className="px-2.5 py-2 text-center text-xs font-bold text-slate-700 bg-slate-50 min-w-[60px]">
                      গ্রেড
                    </th>
                  </>
                )}

                {mode === "attendance" && (
                  <th className="px-2.5 py-2 text-center text-xs font-semibold text-teal-800 bg-teal-50/70 min-w-[82px]">
                    উপস্থিতির হার
                  </th>
                )}
              </tr>
            </thead>

            <tbody className="divide-y divide-slate-200">
              {classStudents.length === 0 ? (
                <tr>
                  <td
                    colSpan={columns.length + (mode === "subject" ? 5 : mode === "attendance" ? 3 : 2)}
                    className="py-10 text-center text-sm text-slate-500"
                  >
                    {CLSN[selectedClass - 1]} শ্রেণিতে এখনো কোনো শিক্ষার্থী নেই। নিচে থেকে নতুন শিক্ষার্থী যোগ করুন।
                  </td>
                </tr>
              ) : (
                classStudents.map((s, rIdx) => {
                  const subForStudent =
                    subjectsFor(s.cls, s.religion).find(
                      (sub) => sub.key === activeSub?.key
                    ) || activeSub;

                  const cScore = subForStudent ? cont(s, subForStudent, term) : 0;
                  const tScore = subForStudent ? subTotal(s, subForStudent, term) : 0;
                  const gScore =
                    subForStudent && subForStudent.max > 0
                      ? gradeOf((tScore / subForStudent.max) * 100)
                      : "-";

                  const dRaw = s.x[`DAYS_${term}`];
                  const pRaw = s.x[`PRES_${term}`];
                  const dNum = dRaw ? Number.parseFloat(fromBn(dRaw)) : Number.NaN;
                  const pNum = pRaw ? Number.parseFloat(fromBn(pRaw)) : Number.NaN;
                  const attRate =
                    !Number.isNaN(dNum) && !Number.isNaN(pNum) && dNum > 0
                      ? `${fmt((pNum / dNum) * 100)}%`
                      : "-";

                  return (
                    <tr key={s.id} className="hover:bg-slate-50/70 group">
                      <td className="sticky left-0 z-10 bg-white group-hover:bg-slate-50 px-2 py-0 text-center font-mono tabular-nums text-xs font-semibold text-slate-700 border-r border-slate-200">
                        {bn(String(s.roll))}
                      </td>
                      <td className="sticky left-16 z-10 bg-white group-hover:bg-slate-50 px-3 py-0 border-r-2 border-slate-300">
                        <button
                          type="button"
                          onClick={() => onOpenStudent(s)}
                          title="বিস্তারিত সম্পাদনা করুন"
                          className="w-full text-left py-2 text-sm font-medium text-slate-900 hover:text-teal-700 truncate cursor-pointer"
                        >
                          {s.name || "(নাম নেই)"}
                        </button>
                      </td>

                      {columns.map((col, cIdx) => {
                        const cellValue = getCellDisplay(s, col);
                        return (
                          <td
                            key={col.id}
                            className="p-0 border-r border-slate-200"
                          >
                            {col.kind === "trait" ? (
                              <select
                                ref={(el) => {
                                  // Select elements don't attach HTMLInputElement; handled via button/keyboard
                                  void el;
                                }}
                                value={cellValue}
                                onChange={(e) =>
                                  handleCellCommit(s, col, e.target.value)
                                }
                                className="w-full h-9 px-1.5 text-center text-xs font-semibold bg-transparent focus:outline-none focus:bg-teal-50/80 text-slate-800 cursor-pointer"
                              >
                                <option value="">-</option>
                                <option value="ক">ক (উত্তম)</option>
                                <option value="খ">খ (সন্তোষজনক)</option>
                                <option value="গ">গ (উন্নতি প্রয়োজন)</option>
                              </select>
                            ) : (
                              <SheetCellInput
                                value={cellValue}
                                max={col.max}
                                onCommit={(val) => handleCellCommit(s, col, val)}
                                registerRef={(el) => {
                                  inputRefs.current[`${rIdx}:${cIdx}`] = el;
                                }}
                                onNavigate={(dr, dc) =>
                                  navigateFrom(rIdx, cIdx, dr, dc)
                                }
                                onPasteGrid={(txt) =>
                                  handlePasteGrid(rIdx, cIdx, txt)
                                }
                                enterMovesDown={enterMovesDown}
                              />
                            )}
                          </td>
                        );
                      })}

                      {mode === "subject" && subForStudent && (
                        <>
                          <td className="px-2 py-2 text-center font-mono tabular-nums text-xs text-slate-700 bg-slate-50/70 border-r border-slate-200">
                            {fmt(cScore)}
                          </td>
                          <td className="px-2 py-2 text-center font-mono tabular-nums text-xs font-bold text-teal-800 bg-teal-50/40 border-r border-slate-200">
                            {fmt(tScore)}
                          </td>
                          <td className="px-2 py-2 text-center text-xs font-bold text-slate-800 bg-slate-50/70">
                            {gScore}
                          </td>
                        </>
                      )}

                      {mode === "attendance" && (
                        <td className="px-2 py-2 text-center font-mono tabular-nums text-xs font-semibold text-teal-800 bg-teal-50/40">
                          {attRate}
                        </td>
                      )}
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Inline Quick Add Student Row + Keyboard Guide */}
        <div className="border-t border-slate-200 bg-slate-50/80 px-4 py-3 flex flex-col lg:flex-row lg:items-center lg:justify-between gap-3">
          <form
            onSubmit={handleQuickAddStudent}
            className="flex flex-wrap items-center gap-2"
          >
            <span className="inline-flex items-center gap-1 text-xs font-semibold text-slate-700">
              <UserPlus className="w-3.5 h-3.5 text-teal-700" />
              দ্রুত নতুন শিক্ষার্থী:
            </span>
            <input
              type="text"
              inputMode="numeric"
              value={newRoll}
              onChange={(e) => setNewRoll(e.target.value)}
              placeholder={`রোল (${bn(String(suggestedRoll))})`}
              className="w-20 px-2.5 py-1.5 text-xs font-mono rounded-lg border border-slate-200 bg-white focus:outline-none focus:border-teal-700"
            />
            <input
              ref={newNameInputRef}
              type="text"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              placeholder="শিক্ষার্থীর নাম লিখুন..."
              className="w-48 sm:w-56 px-2.5 py-1.5 text-xs rounded-lg border border-slate-200 bg-white focus:outline-none focus:border-teal-700"
            />
            <select
              value={newReligion}
              onChange={(e) => setNewReligion(e.target.value)}
              className="px-2 py-1.5 text-xs rounded-lg border border-slate-200 bg-white focus:outline-none focus:border-teal-700"
            >
              {RELIGIONS.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
            <button
              type="submit"
              disabled={!newName.trim()}
              className="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-medium rounded-lg bg-slate-900 text-white hover:bg-slate-800 disabled:opacity-40 cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>যোগ করুন</span>
            </button>
          </form>

          <div className="text-[11px] text-slate-500">
            টিপস: Enter = নিচের ঘর · Shift+Enter / ↑↓ = উপরে-নিচে · Tab / ←→ = ডানে-বামে · Excel থেকে কপি-পেস্ট সাপোর্ট করে
          </div>
        </div>
      </div>
    </div>
  );
};
