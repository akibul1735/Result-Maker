export interface Student {
  id: string;
  cls: number;
  roll: number;
  name: string;
  guardian: string;
  village: string;
  post?: string;
  upazila: string;
  district: string;
  mobile: string;
  religion: string;
  m: Record<string, number>;
  x: Record<string, string>;
}

export type PdfFontId =
  | "noto_serif"
  | "tiro_bangla"
  | "noto_sans"
  | "hind_siliguri";

export interface PdfFontOption {
  id: PdfFontId;
  name: string;
  labelBn: string;
  descriptionBn: string;
  cssFamily: string;
  primaryFace: string;
}

export const PDF_FONT_OPTIONS: PdfFontOption[] = [
  {
    id: "noto_serif",
    name: "Noto Serif Bengali",
    labelBn: "নোতো সেরিফ বাংলা (প্রাতিষ্ঠানিক)",
    descriptionBn: "সরকারি নথি ও আনুষ্ঠানিক প্রিন্টের জন্য আদর্শ সেরিফ ফন্ট",
    cssFamily: '"Noto Serif Bengali", "Plus Jakarta Sans", serif',
    primaryFace: "Noto Serif Bengali",
  },
  {
    id: "tiro_bangla",
    name: "Tiro Bangla",
    labelBn: "তিরো বাংলা (বই ও সনদপত্র)",
    descriptionBn: "মুদ্রিত বই, সনদপত্র ও মার্জিত কভার শিরোনামের জন্য উপযোগী",
    cssFamily: '"Tiro Bangla", "Noto Serif Bengali", serif',
    primaryFace: "Tiro Bangla",
  },
  {
    id: "noto_sans",
    name: "Noto Sans Bengali",
    labelBn: "নোতো স্যান্স বাংলা (স্ট্যান্ডার্ড)",
    descriptionBn: "Android সিস্টেম ও NCTB ডিজিটাল ফরম্যাটের সাথে সামঞ্জস্যপূর্ণ",
    cssFamily: '"Noto Sans Bengali", "Plus Jakarta Sans", sans-serif',
    primaryFace: "Noto Sans Bengali",
  },
  {
    id: "hind_siliguri",
    name: "Hind Siliguri",
    labelBn: "হিন্দ শিলিগুড়ি (পরিচ্ছন্ন টেবিল)",
    descriptionBn: "নম্বর টেবিল ও সংখ্যার স্পষ্ট পাঠযোগ্যতার জন্য আধুনিক স্যান্স ফন্ট",
    cssFamily: '"Hind Siliguri", "Plus Jakarta Sans", sans-serif',
    primaryFace: "Hind Siliguri",
  },
];

export function getPdfFontOption(id?: string): PdfFontOption {
  return (
    PDF_FONT_OPTIONS.find((opt) => opt.id === id) || PDF_FONT_OPTIONS[0]
  );
}

export type DpeLogoStyle = "simple" | "color";

export interface DpeLogoOption {
  id: DpeLogoStyle;
  labelBn: string;
  descriptionBn: string;
  src: string;
}

export const DPE_LOGO_OPTIONS: DpeLogoOption[] = [
  {
    id: "simple",
    labelBn: "সাদাকালো লোগো (Simple)",
    descriptionBn: "সাদাকালো ফটোকপি ও সাধারণ প্রিন্টের জন্য স্পষ্ট লাইন-আর্ট লোগো",
    src: "/dpe-logo-simple.png",
  },
  {
    id: "color",
    labelBn: "রঙিন লোগো (Color)",
    descriptionBn: "রঙিন মুদ্রণ ও ডিজিটাল প্রদর্শনের জন্য মূল রঙিন লোগো",
    src: "/dpe-logo-color.png",
  },
];

export interface Settings {
  school: string;
  year: string;
  pdfFont?: PdfFontId;
  dpeLogo?: DpeLogoStyle;
  teachers: Record<number, string>;
  teacherMobiles: Record<number, string>;
}

export interface Comp {
  key: string;
  bn: string;
  max: number;
}

export interface Sub {
  key: string;
  bn: string;
  max: number;
  comps: Comp[];
  written: number;
}

export const RELIGIONS = ["ইসলাম", "হিন্দু", "বৌদ্ধ", "খ্রীষ্ট"] as const;
export const CLSN = ["প্রথম", "দ্বিতীয়", "তৃতীয়", "চতুর্থ", "পঞ্চম"] as const;
export const CLS_SHORT = ["১ম", "২য়", "৩য়", "৪র্থ", "৫ম"] as const;

function comps(tb: number, ct: number): Comp[] {
  return [
    { key: "TB", bn: "পাঠ্যপুস্তক/শিক্ষক সহায়িকার কাজ", max: tb },
    { key: "ACT", bn: "শ্রেণিকাজে সক্রিয়তা", max: 5 },
    { key: "LANG", bn: "ভাষাদক্ষতা/বোধগম্যতা", max: 5 },
    { key: "CT", bn: "ক্লাস টেস্ট", max: ct },
  ];
}

export function subjectsFor(cls: number, religion: string): Sub[] {
  const low = cls <= 2;
  const full = (key: string, bnName: string): Sub =>
    low
      ? { key, bn: bnName, max: 100, comps: comps(20, 20), written: 50 }
      : { key, bn: bnName, max: 100, comps: comps(10, 10), written: 70 };
  const half = (key: string, bnName: string): Sub => ({
    key,
    bn: bnName,
    max: 50,
    comps: comps(20, 20),
    written: 0,
  });
  const art: Sub = {
    key: "ART",
    bn: "শিল্পকলা",
    max: 50,
    comps: [
      {
        key: "TOTAL",
        bn: "মোট নম্বর (চারু ও কারুকলা, সংগীত, নৃত্য, নাট্যকলা)",
        max: 50,
      },
    ],
    written: 0,
  };
  const rel = `ধর্মশিক্ষা (${religion})`;

  return low
    ? [
        full("BAN", "বাংলা"),
        full("ENG", "ইংরেজি"),
        full("MATH", "গণিত"),
        half("SOC", "সামাজিক বিজ্ঞান ও বিজ্ঞান (সমন্বিত)"),
        half("REL", rel),
        half("PE", "শারীরিক ও মানসিক স্বাস্থ্য শিক্ষা"),
        art,
      ]
    : [
        full("BAN", "বাংলা"),
        full("ENG", "ইংরেজি"),
        full("MATH", "গণিত"),
        full("SCI", "বিজ্ঞান"),
        full("BGW", "বাংলাদেশ ও বিশ্বপরিচয়"),
        full("REL", rel),
        half("PE", "শারীরিক ও মানসিক স্বাস্থ্য শিক্ষা"),
        art,
      ];
}

export function mk(sub: string, t: number, c: string): string {
  return `${sub}_${t}_${c}`;
}

const BN_DIGITS = ["০", "১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯"];

export function bn(s: string): string {
  return s.replace(/[0-9]/g, (d) => BN_DIGITS[Number(d)]);
}

export function fromBn(s: string): string {
  return s.replace(/[০-৯]/g, (d) => String(BN_DIGITS.indexOf(d)));
}

export function fmtEn(d: number): string {
  const r = Math.round(d * 10) / 10;
  return Number.isInteger(r) ? String(r) : r.toFixed(1);
}

export function fmt(d: number): string {
  return bn(fmtEn(d));
}

export function studentSubs(s: Student): Sub[] {
  return subjectsFor(s.cls, s.religion);
}

export function hasTerm(s: Student, t: number): boolean {
  return Object.keys(s.m).some((k) => k.includes(`_${t}_`));
}

export function hasSub(s: Student, sub: Sub, t: number): boolean {
  return Object.keys(s.m).some((k) => k.startsWith(`${sub.key}_${t}_`));
}

export function cont(s: Student, sub: Sub, t: number): number {
  return sub.comps.reduce((acc, c) => acc + (s.m[mk(sub.key, t, c.key)] ?? 0), 0);
}

export function wr(s: Student, sub: Sub, t: number): number {
  return sub.written > 0 ? s.m[mk(sub.key, t, "WR")] ?? 0 : 0;
}

export function subTotal(s: Student, sub: Sub, t: number): number {
  return cont(s, sub, t) + wr(s, sub, t);
}

export function subAvg(s: Student, sub: Sub): number {
  return (subTotal(s, sub, 1) + subTotal(s, sub, 2) + subTotal(s, sub, 3)) / 3;
}

export function gradeOf(p: number): "A" | "B" | "C" | "D" {
  if (p >= 80) return "A";
  if (p >= 60) return "B";
  if (p >= 40) return "C";
  return "D";
}

export function gradeLabelBn(g: string): string {
  switch (g) {
    case "A":
      return "অতি উত্তম";
    case "B":
      return "উত্তম";
    case "C":
      return "সন্তোষজনক";
    default:
      return "সহায়তা প্রয়োজন";
  }
}

export function overallPct(s: Student): number {
  const ss = studentSubs(s);
  const maxSum = ss.reduce((acc, sub) => acc + sub.max, 0);
  if (maxSum === 0) return 0;
  const avgSum = ss.reduce((acc, sub) => acc + subAvg(s, sub), 0);
  return (avgSum / maxSum) * 100;
}

export function createEmptyStudent(cls: number, roll: number): Student {
  return {
    id: crypto.randomUUID ? crypto.randomUUID() : `st-${Date.now()}-${Math.random()}`,
    cls,
    roll,
    name: "",
    guardian: "",
    village: "",
    post: "",
    upazila: "",
    district: "",
    mobile: "",
    religion: "ইসলাম",
    m: {},
    x: {},
  };
}

// ---------- Repo (CSV / JSON compatible with Repo.kt) ----------
const BASE_COLS = [
  "cls",
  "roll",
  "name",
  "guardian",
  "village",
  "post",
  "upazila",
  "district",
  "mobile",
  "religion",
];

export const MARK_KEYS: string[] = [
  "BAN",
  "ENG",
  "MATH",
  "SOC",
  "SCI",
  "BGW",
  "REL",
  "PE",
  "ART",
].flatMap((s) =>
  [1, 2, 3].flatMap((t) =>
    ["TB", "ACT", "LANG", "CT", "WR", "TOTAL"].map((c) => mk(s, t, c))
  )
);

export const TEXT_KEYS: string[] = [1, 2, 3].flatMap((t) =>
  ["DAYS", "PRES", "ACT", "DISC", "COND", "REM"].map((k) => `${k}_${t}`)
);

export function toJson(list: Student[], st: Settings): string {
  return JSON.stringify(
    {
      version: 1,
      school: st.school,
      year: st.year,
      pdfFont: st.pdfFont || "noto_serif",
      dpeLogo: st.dpeLogo || "simple",
      teachers: st.teachers,
      teacherMobiles: st.teacherMobiles,
      students: list,
    },
    null,
    2
  );
}

export function fromJson(txt: string): { students: Student[]; settings: Settings } {
  const o = JSON.parse(txt);
  const teachers: Record<number, string> = {};
  if (o.teachers && typeof o.teachers === "object") {
    for (const [k, v] of Object.entries(o.teachers)) {
      const n = Number(k);
      if (!Number.isNaN(n) && typeof v === "string") teachers[n] = v;
    }
  }
  const teacherMobiles: Record<number, string> = {};
  if (o.teacherMobiles && typeof o.teacherMobiles === "object") {
    for (const [k, v] of Object.entries(o.teacherMobiles)) {
      const n = Number(k);
      if (!Number.isNaN(n) && typeof v === "string") teacherMobiles[n] = v;
    }
  }
  const rawFont = typeof o.pdfFont === "string" ? o.pdfFont : "noto_serif";
  const pdfFont = PDF_FONT_OPTIONS.some((f) => f.id === rawFont)
    ? (rawFont as PdfFontId)
    : "noto_serif";
  const dpeLogo: DpeLogoStyle = o.dpeLogo === "color" ? "color" : "simple";
  const settings: Settings = {
    school: typeof o.school === "string" ? o.school : "",
    year: typeof o.year === "string" ? o.year : "২০২৬",
    pdfFont,
    dpeLogo,
    teachers,
    teacherMobiles,
  };
  const arr = Array.isArray(o.students) ? o.students : [];
  const students: Student[] = arr.map((j: Record<string, unknown>) => ({
    id: typeof j.id === "string" ? j.id : crypto.randomUUID(),
    cls: typeof j.cls === "number" ? j.cls : 1,
    roll: typeof j.roll === "number" ? j.roll : 0,
    name: typeof j.name === "string" ? j.name : "",
    guardian: typeof j.guardian === "string" ? j.guardian : "",
    village: typeof j.village === "string" ? j.village : "",
    post: typeof j.post === "string" ? j.post : "",
    upazila: typeof j.upazila === "string" ? j.upazila : "",
    district: typeof j.district === "string" ? j.district : "",
    mobile: typeof j.mobile === "string" ? j.mobile : "",
    religion: typeof j.religion === "string" ? j.religion : "ইসলাম",
    m: j.m && typeof j.m === "object" ? (j.m as Record<string, number>) : {},
    x: j.x && typeof j.x === "object" ? (j.x as Record<string, string>) : {},
  }));
  return { students, settings };
}

function csvEscape(s: string): string {
  if (/[,"\n\r]/.test(s)) {
    return `"${s.replace(/"/g, '""')}"`;
  }
  return s;
}

export function toCsv(list: Student[]): string {
  const header = [...BASE_COLS, ...MARK_KEYS, ...TEXT_KEYS];
  const lines = [header.join(",")];
  const sorted = [...list].sort((a, b) => a.cls - b.cls || a.roll - b.roll);
  for (const s of sorted) {
    const row = [
      String(s.cls),
      String(s.roll),
      s.name,
      s.guardian,
      s.village,
      s.post ?? "",
      s.upazila,
      s.district,
      s.mobile,
      s.religion,
      ...MARK_KEYS.map((k) => (s.m[k] !== undefined ? fmtEn(s.m[k]) : "")),
      ...TEXT_KEYS.map((k) => s.x[k] ?? ""),
    ];
    lines.push(row.map(csvEscape).join(","));
  }
  return "\uFEFF" + lines.join("\n") + "\n";
}

export function parseCsv(t0: string): string[][] {
  const t = t0.replace(/^\uFEFF/, "");
  const rows: string[][] = [];
  let row: string[] = [];
  let cur = "";
  let inQ = false;
  for (let i = 0; i < t.length; i++) {
    const c = t[i];
    if (inQ) {
      if (c === '"') {
        if (i + 1 < t.length && t[i + 1] === '"') {
          cur += '"';
          i++;
        } else {
          inQ = false;
        }
      } else {
        cur += c;
      }
    } else if (c === '"') {
      inQ = true;
    } else if (c === ",") {
      row.push(cur);
      cur = "";
    } else if (c === "\n") {
      row.push(cur);
      cur = "";
      rows.push(row);
      row = [];
    } else if (c === "\r") {
      // skip
    } else {
      cur += c;
    }
  }
  if (cur.length > 0 || row.length > 0) {
    row.push(cur);
    rows.push(row);
  }
  return rows.filter((r) => r.some((cell) => cell.trim().length > 0));
}

export function importCsv(txt: string, existing: Student[]): Student[] {
  const rows = parseCsv(txt);
  if (rows.length < 2) return existing;
  const idx = new Map<string, number>();
  rows[0].forEach((h, i) => idx.set(h.trim(), i));
  const out = [...existing];

  for (const r of rows.slice(1)) {
    const g = (k: string) => {
      const col = idx.get(k);
      return col !== undefined ? (r[col] ?? "").trim() : "";
    };
    const cls = Number.parseInt(fromBn(g("cls")), 10);
    if (!Number.isInteger(cls) || cls < 1 || cls > 5) continue;
    const roll = Number.parseInt(fromBn(g("roll")), 10);
    if (!Number.isInteger(roll)) continue;

    const m: Record<string, number> = {};
    const x: Record<string, string> = {};
    for (const k of MARK_KEYS) {
      const val = g(k);
      if (val) {
        const num = Number.parseFloat(fromBn(val));
        if (!Number.isNaN(num)) m[k] = num;
      }
    }
    for (const k of TEXT_KEYS) {
      const val = g(k);
      if (val) x[k] = val;
    }

    const pos = out.findIndex((s) => s.cls === cls && s.roll === roll);
    const base = pos >= 0 ? out[pos] : createEmptyStudent(cls, roll);
    const upd: Student = {
      ...base,
      name: g("name") || base.name,
      guardian: g("guardian") || base.guardian,
      village: g("village") || base.village,
      post: g("post") || base.post || "",
      upazila: g("upazila") || base.upazila,
      district: g("district") || base.district,
      mobile: g("mobile") || base.mobile,
      religion: g("religion") || base.religion,
      m: { ...base.m, ...m },
      x: { ...base.x, ...x },
    };
    if (pos >= 0) out[pos] = upd;
    else out.push(upd);
  }
  return out;
}

export const DEFAULT_SETTINGS: Settings = {
  school: "কামারখালী সরকারি প্রাথমিক বিদ্যালয়",
  year: "২০২৬",
  pdfFont: "noto_serif",
  dpeLogo: "simple",
  teachers: {
    1: "মোসাঃ তাসলিমা খাতুন",
    2: "মোঃ রফিকুল ইসলাম",
    3: "শারমিন আক্তার",
    4: "মোঃ আকিবুল ইসলাম",
    5: "নাসরিন সুলতানা",
  },
  teacherMobiles: {
    1: "01712345601",
    2: "01712345602",
    3: "01712345603",
    4: "01712345604",
    5: "01712345605",
  },
};

export const SAMPLE_STUDENTS: Student[] = [
  {
    id: "sample-1-1",
    cls: 1,
    roll: 1,
    name: "তানভীর আহমেদ",
    guardian: "মোঃ করিম উদ্দিন",
    village: "কামারখালী",
    post: "কামারখালী",
    upazila: "মধুখালী",
    district: "ফরিদপুর",
    mobile: "01711223344",
    religion: "ইসলাম",
    m: {
      BAN_1_TB: 18, BAN_1_ACT: 5, BAN_1_LANG: 4, BAN_1_CT: 17, BAN_1_WR: 44,
      ENG_1_TB: 17, ENG_1_ACT: 4, ENG_1_LANG: 4, ENG_1_CT: 16, ENG_1_WR: 41,
      MATH_1_TB: 19, MATH_1_ACT: 5, MATH_1_LANG: 5, MATH_1_CT: 18, MATH_1_WR: 46,
      SOC_1_TB: 18, SOC_1_ACT: 4, SOC_1_LANG: 5, SOC_1_CT: 17,
      REL_1_TB: 19, REL_1_ACT: 5, REL_1_LANG: 4, REL_1_CT: 18,
      PE_1_TB: 18, PE_1_ACT: 5, PE_1_LANG: 4, PE_1_CT: 18,
      ART_1_TOTAL: 45,
      BAN_2_TB: 19, BAN_2_ACT: 5, BAN_2_LANG: 5, BAN_2_CT: 18, BAN_2_WR: 45,
      ENG_2_TB: 18, ENG_2_ACT: 4, ENG_2_LANG: 5, ENG_2_CT: 17, ENG_2_WR: 43,
      MATH_2_TB: 19, MATH_2_ACT: 5, MATH_2_LANG: 5, MATH_2_CT: 19, MATH_2_WR: 47,
      SOC_2_TB: 18, SOC_2_ACT: 5, SOC_2_LANG: 4, SOC_2_CT: 18,
      REL_2_TB: 19, REL_2_ACT: 5, REL_2_LANG: 5, REL_2_CT: 19,
      PE_2_TB: 19, PE_2_ACT: 5, PE_2_LANG: 5, PE_2_CT: 18,
      ART_2_TOTAL: 47,
      BAN_3_TB: 19, BAN_3_ACT: 5, BAN_3_LANG: 5, BAN_3_CT: 19, BAN_3_WR: 46,
      ENG_3_TB: 18, ENG_3_ACT: 5, ENG_3_LANG: 5, ENG_3_CT: 18, ENG_3_WR: 44,
      MATH_3_TB: 20, MATH_3_ACT: 5, MATH_3_LANG: 5, MATH_3_CT: 19, MATH_3_WR: 48,
      SOC_3_TB: 19, SOC_3_ACT: 5, SOC_3_LANG: 5, SOC_3_CT: 18,
      REL_3_TB: 19, REL_3_ACT: 5, REL_3_LANG: 5, REL_3_CT: 19,
      PE_3_TB: 19, PE_3_ACT: 5, PE_3_LANG: 5, PE_3_CT: 19,
      ART_3_TOTAL: 48,
    },
    x: {
      DAYS_1: "62", PRES_1: "59", ACT_1: "ক", DISC_1: "ক", COND_1: "ক",
      REM_1: "পাঠ্যবিষয়ে মনোযোগী ও নিয়মিত। বাংলা ও গণিতে চমৎকার অগ্রগতি দেখিয়েছে।",
      DAYS_2: "65", PRES_2: "62", ACT_2: "ক", DISC_2: "ক", COND_2: "ক",
      REM_2: "দলগত কাজে সক্রিয় অংশগ্রহণ প্রশংসনীয়। ইংরেজি পঠনে আরও অনুশীলন ফলপ্রসূ হবে।",
      DAYS_3: "64", PRES_3: "63", ACT_3: "ক", DISC_3: "ক", COND_3: "ক",
      REM_3: "সকল বিষয়ে ধারাবাহিক উৎকর্ষ অর্জন করেছে। পরবর্তী শ্রেণিতে উত্তীর্ণ হওয়ার যোগ্য।",
    },
  },
  {
    id: "sample-3-1",
    cls: 3,
    roll: 1,
    name: "নুসরাত জাহান মিম",
    guardian: "মোঃ শফিকুল ইসলাম",
    village: "গোয়ালপাড়া",
    post: "কামারখালী",
    upazila: "মধুখালী",
    district: "ফরিদপুর",
    mobile: "01819445566",
    religion: "ইসলাম",
    m: {
      BAN_1_TB: 9, BAN_1_ACT: 5, BAN_1_LANG: 4, BAN_1_CT: 9, BAN_1_WR: 60,
      ENG_1_TB: 9, ENG_1_ACT: 4, ENG_1_LANG: 4, ENG_1_CT: 8, ENG_1_WR: 56,
      MATH_1_TB: 10, MATH_1_ACT: 5, MATH_1_LANG: 5, MATH_1_CT: 9, MATH_1_WR: 64,
      SCI_1_TB: 9, SCI_1_ACT: 5, SCI_1_LANG: 4, SCI_1_CT: 9, SCI_1_WR: 58,
      BGW_1_TB: 9, BGW_1_ACT: 4, BGW_1_LANG: 4, BGW_1_CT: 8, BGW_1_WR: 57,
      REL_1_TB: 9, REL_1_ACT: 5, REL_1_LANG: 5, REL_1_CT: 9, REL_1_WR: 62,
      PE_1_TB: 18, PE_1_ACT: 5, PE_1_LANG: 4, PE_1_CT: 17,
      ART_1_TOTAL: 44,
      BAN_2_TB: 9, BAN_2_ACT: 5, BAN_2_LANG: 5, BAN_2_CT: 9, BAN_2_WR: 62,
      ENG_2_TB: 9, ENG_2_ACT: 5, ENG_2_LANG: 4, ENG_2_CT: 9, ENG_2_WR: 59,
      MATH_2_TB: 10, MATH_2_ACT: 5, MATH_2_LANG: 5, MATH_2_CT: 10, MATH_2_WR: 65,
      SCI_2_TB: 9, SCI_2_ACT: 5, SCI_2_LANG: 5, SCI_2_CT: 9, SCI_2_WR: 61,
      BGW_2_TB: 9, BGW_2_ACT: 5, BGW_2_LANG: 4, BGW_2_CT: 9, BGW_2_WR: 60,
      REL_2_TB: 10, REL_2_ACT: 5, REL_2_LANG: 5, REL_2_CT: 9, REL_2_WR: 64,
      PE_2_TB: 19, PE_2_ACT: 5, PE_2_LANG: 5, PE_2_CT: 18,
      ART_2_TOTAL: 46,
      BAN_3_TB: 10, BAN_3_ACT: 5, BAN_3_LANG: 5, BAN_3_CT: 9, BAN_3_WR: 63,
      ENG_3_TB: 9, ENG_3_ACT: 5, ENG_3_LANG: 5, ENG_3_CT: 9, ENG_3_WR: 61,
      MATH_3_TB: 10, MATH_3_ACT: 5, MATH_3_LANG: 5, MATH_3_CT: 10, MATH_3_WR: 67,
      SCI_3_TB: 9, SCI_3_ACT: 5, SCI_3_LANG: 5, SCI_3_CT: 9, SCI_3_WR: 63,
      BGW_3_TB: 9, BGW_3_ACT: 5, BGW_3_LANG: 5, BGW_3_CT: 9, BGW_3_WR: 62,
      REL_3_TB: 10, REL_3_ACT: 5, REL_3_LANG: 5, REL_3_CT: 10, REL_3_WR: 65,
      PE_3_TB: 19, PE_3_ACT: 5, PE_3_LANG: 5, PE_3_CT: 19,
      ART_3_TOTAL: 47,
    },
    x: {
      DAYS_1: "62", PRES_1: "58", ACT_1: "ক", DISC_1: "ক", COND_1: "ক",
      REM_1: "শ্রেণিকক্ষে অত্যন্ত শৃঙ্খলাপরায়ণ এবং পাঠদানে মনোযোগী।",
      DAYS_2: "65", PRES_2: "63", ACT_2: "ক", DISC_2: "ক", COND_2: "ক",
      REM_2: "বিজ্ঞান ও গণিতে বিশ্লেষণী দক্ষতা বেড়েছে। ধারাবাহিকতা বজায় রাখুক।",
      DAYS_3: "64", PRES_3: "62", ACT_3: "ক", DISC_3: "ক", COND_3: "ক",
      REM_3: "বার্ষিক মূল্যায়নে অতি উত্তম ফলাফল অর্জন করেছে। অভিনন্দন।",
    },
  },
  {
    id: "sample-5-2",
    cls: 5,
    roll: 2,
    name: "অনিক চক্রবর্তী",
    guardian: "সুমন চক্রবর্তী",
    village: "বাগাট",
    post: "বাগাট",
    upazila: "মধুখালী",
    district: "ফরিদপুর",
    mobile: "01915667788",
    religion: "হিন্দু",
    m: {
      BAN_1_TB: 8, BAN_1_ACT: 4, BAN_1_LANG: 4, BAN_1_CT: 8, BAN_1_WR: 52,
      ENG_1_TB: 8, ENG_1_ACT: 4, ENG_1_LANG: 4, ENG_1_CT: 7, ENG_1_WR: 48,
      MATH_1_TB: 9, MATH_1_ACT: 4, MATH_1_LANG: 4, MATH_1_CT: 8, MATH_1_WR: 54,
      SCI_1_TB: 8, SCI_1_ACT: 4, SCI_1_LANG: 4, SCI_1_CT: 8, SCI_1_WR: 51,
      BGW_1_TB: 8, BGW_1_ACT: 4, BGW_1_LANG: 4, BGW_1_CT: 7, BGW_1_WR: 49,
      REL_1_TB: 9, REL_1_ACT: 5, REL_1_LANG: 4, REL_1_CT: 8, REL_1_WR: 56,
      PE_1_TB: 17, PE_1_ACT: 4, PE_1_LANG: 4, PE_1_CT: 16,
      ART_1_TOTAL: 41,
      BAN_2_TB: 9, BAN_2_ACT: 4, BAN_2_LANG: 4, BAN_2_CT: 8, BAN_2_WR: 55,
      ENG_2_TB: 8, ENG_2_ACT: 4, ENG_2_LANG: 4, ENG_2_CT: 8, ENG_2_WR: 50,
      MATH_2_TB: 9, MATH_2_ACT: 5, MATH_2_LANG: 4, MATH_2_CT: 9, MATH_2_WR: 56,
      SCI_2_TB: 8, SCI_2_ACT: 4, SCI_2_LANG: 4, SCI_2_CT: 8, SCI_2_WR: 53,
      BGW_2_TB: 8, BGW_2_ACT: 4, BGW_2_LANG: 4, BGW_2_CT: 8, BGW_2_WR: 52,
      REL_2_TB: 9, REL_2_ACT: 5, REL_2_LANG: 4, REL_2_CT: 9, REL_2_WR: 58,
      PE_2_TB: 18, PE_2_ACT: 4, PE_2_LANG: 4, PE_2_CT: 17,
      ART_2_TOTAL: 43,
    },
    x: {
      DAYS_1: "62", PRES_1: "54", ACT_1: "খ", DISC_1: "ক", COND_1: "ক",
      REM_1: "নিয়মিত উপস্থিত থাকলে এবং ইংরেজি অনুশীলন বাড়ালে আরও ভালো করবে।",
      DAYS_2: "65", PRES_2: "59", ACT_2: "ক", DISC_2: "ক", COND_2: "ক",
      REM_2: "২য় প্রান্তিকে উল্লেখযোগ্য উন্নতি হয়েছে। গণিতে আগ্রহ প্রশংসনীয়।",
    },
  },
];
