import { jsPDF } from "jspdf";
import {
  CLSN,
  PdfFontId,
  Settings,
  Student,
  bn,
  cont,
  fmt,
  fromBn,
  getPdfFontOption,
  gradeOf,
  hasSub,
  hasTerm,
  overallPct,
  studentSubs,
  subAvg,
  subTotal,
  wr,
} from "./models";

export const PAGE_W = 842;
export const PAGE_H = 595;

let activeFontFamily = getPdfFontOption("noto_serif").cssFamily;

export async function ensurePdfFontLoaded(fontId?: PdfFontId): Promise<void> {
  const opt = getPdfFontOption(fontId);
  if (document.fonts?.load) {
    await Promise.allSettled([
      document.fonts.load(`400 16px "${opt.primaryFace}"`, "শিখন অগ্রগতি প্রতিবেদন ০১২৩"),
      document.fonts.load(`700 16px "${opt.primaryFace}"`, "শিখন অগ্রগতি প্রতিবেদন ০১২৩"),
    ]);
  }
  if (document.fonts?.ready) {
    await document.fonts.ready;
  }
}

function fontStr(size: number, bold = false): string {
  return `${bold ? "700" : "400"} ${size}px ${activeFontFamily}`;
}

function wrapText(
  ctx: CanvasRenderingContext2D,
  text: string,
  maxWidth: number
): string[] {
  const out: string[] = [];
  for (const para of text.split("\n")) {
    let cur = "";
    for (const word of para.split(" ")) {
      const test = cur ? `${cur} ${word}` : word;
      if (ctx.measureText(test).width <= maxWidth || !cur) {
        cur = test;
      } else {
        out.push(cur);
        cur = word;
      }
    }
    out.push(cur);
  }
  return out;
}

function drawCell(
  ctx: CanvasRenderingContext2D,
  text: string,
  l: number,
  top: number,
  w: number,
  h: number,
  size = 9,
  bold = false,
  fill = false
) {
  if (fill) {
    ctx.fillStyle = "#EAEAEA";
    ctx.fillRect(l, top, w, h);
  }
  ctx.strokeStyle = "#0F172A";
  ctx.lineWidth = 0.8;
  ctx.strokeRect(l, top, w, h);

  if (!text) return;

  let curSize = size;
  const lines = text.split("\n");
  ctx.font = fontStr(curSize, bold);
  while (
    lines.some((ln) => ctx.measureText(ln).width > w - 4) &&
    curSize > 5
  ) {
    curSize -= 0.5;
    ctx.font = fontStr(curSize, bold);
  }

  ctx.fillStyle = "#0F172A";
  ctx.textAlign = "center";
  ctx.textBaseline = "alphabetic";
  const lh = curSize * 1.25;
  let y = top + (h - lh * lines.length) / 2 + curSize * 0.95;
  for (const ln of lines) {
    ctx.fillText(ln, l + w / 2, y);
    y += lh;
  }
}

function drawBox(
  ctx: CanvasRenderingContext2D,
  text: string,
  l: number,
  top: number,
  w: number,
  h: number,
  size = 8
) {
  ctx.strokeStyle = "#0F172A";
  ctx.lineWidth = 0.8;
  ctx.strokeRect(l, top, w, h);
  if (!text) return;

  ctx.font = fontStr(size, false);
  ctx.fillStyle = "#0F172A";
  ctx.textAlign = "left";
  ctx.textBaseline = "alphabetic";
  let y = top + size + 3;
  for (const line of wrapText(ctx, text, w - 8)) {
    if (y < top + h - 2) {
      ctx.fillText(line, l + 4, y);
    }
    y += size * 1.3;
  }
}

function drawCenter(
  ctx: CanvasRenderingContext2D,
  text: string,
  cx: number,
  y: number,
  size: number,
  bold = false,
  maxW = 340
) {
  let curSize = size;
  ctx.font = fontStr(curSize, bold);
  while (ctx.measureText(text).width > maxW && curSize > 6) {
    curSize -= 0.5;
    ctx.font = fontStr(curSize, bold);
  }
  ctx.fillStyle = "#0F172A";
  ctx.textAlign = "center";
  ctx.textBaseline = "alphabetic";
  ctx.fillText(text, cx, y);
}

export function renderCoverPage(
  ctx: CanvasRenderingContext2D,
  s: Student,
  st: Settings,
  showFoldGuide = false
) {
  activeFontFamily = getPdfFontOption(st.pdfFont).cssFamily;
  ctx.fillStyle = "#FFFFFF";
  ctx.fillRect(0, 0, PAGE_W, PAGE_H);

  // Optional subtle fold guide down the center (421pt) for screen preview
  if (showFoldGuide) {
    ctx.save();
    ctx.setLineDash([4, 4]);
    ctx.strokeStyle = "#CBD5E1";
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(PAGE_W / 2, 10);
    ctx.lineTo(PAGE_W / 2, PAGE_H - 10);
    ctx.stroke();
    ctx.restore();
  }

  // ---- Left Half: Back Cover ----
  const x0 = 22;
  drawCenter(
    ctx,
    "শ্রেণি শিক্ষকের সার্বিক মতামত ও স্বাক্ষর",
    x0 + 377 / 2,
    32,
    12,
    true,
    370
  );
  const cw = [44, 125, 70, 70, 68];
  const hd = [
    "প্রান্তিক",
    "শ্রেণি শিক্ষকের\nসার্বিক মতামত",
    "শ্রেণি শিক্ষকের\nস্বাক্ষর",
    "প্রধান শিক্ষকের\nস্বাক্ষর",
    "অভিভাবকের\nস্বাক্ষর",
  ];
  let x = x0;
  hd.forEach((h, i) => {
    drawCell(ctx, h, x, 42, cw[i], 30, 8.5, true, true);
    x += cw[i];
  });
  const tl = ["১ম\nপ্রান্তিক", "২য়\nপ্রান্তিক", "৩য়\nপ্রান্তিক"];
  for (let t = 1; t <= 3; t++) {
    const y = 72 + (t - 1) * 78;
    x = x0;
    drawCell(ctx, tl[t - 1], x, y, cw[0], 78, 8.5, true);
    x += cw[0];
    drawBox(ctx, s.x[`REM_${t}`] ?? "", x, y, cw[1], 78, 8);
    x += cw[1];
    for (let i = 2; i <= 4; i++) {
      drawCell(ctx, "", x, y, cw[i], 78);
      x += cw[i];
    }
  }

  const gy = 322;
  const gw = [120, 70, 187];
  x = x0;
  ["প্রাপ্ত নম্বর", "গ্রেড", "শিখন অর্জন"].forEach((h, i) => {
    drawCell(ctx, h, x, gy, gw[i], 24, 9.5, true, true);
    x += gw[i];
  });
  const scale: Array<[string, string, string]> = [
    ["৮০%-১০০%", "A", "অতি উত্তম"],
    ["৬০%-৭৯%", "B", "উত্তম"],
    ["৪০%-৫৯%", "C", "সন্তোষজনক"],
    ["০%-৩৯%", "D", "সহায়তা প্রয়োজন"],
  ];
  scale.forEach(([a, g, d], r) => {
    x = x0;
    const y = gy + 24 + r * 26;
    drawCell(ctx, a, x, y, gw[0], 26, 10);
    x += gw[0];
    drawCell(ctx, g, x, y, gw[1], 26, 11, true);
    x += gw[1];
    drawCell(ctx, d, x, y, gw[2], 26, 10);
  });

  const tn = st.teachers[s.cls] ?? "";
  const tm = st.teacherMobiles[s.cls] ?? "";
  ctx.font = fontStr(11, false);
  ctx.fillStyle = "#0F172A";
  ctx.textAlign = "left";
  ctx.fillText(`শ্রেণি শিক্ষকের নাম: ${tn}`, x0, 480);
  ctx.fillText(`মোবাইল নম্বর: ${bn(tm)}`, x0, 504);
  drawCenter(
    ctx,
    "জাতীয় শিক্ষাক্রম ও পাঠ্যপুস্তক বোর্ড, বাংলাদেশ",
    x0 + 377 / 2,
    560,
    11,
    true,
    370
  );

  // ---- Right Half: Front Cover ----
  const l = 441;
  const w = 379;
  ctx.strokeStyle = "#0F172A";
  ctx.lineWidth = 3;
  ctx.strokeRect(l, 22, w, 551);
  ctx.lineWidth = 0.8;
  ctx.strokeRect(l + 7, 29, w - 14, 537);

  const cx = l + w / 2;
  drawCenter(ctx, "শিখন অগ্রগতি প্রতিবেদন", cx, 105, 25, true);
  drawCenter(ctx, `শিক্ষাবর্ষ: ${bn(st.year)}`, cx, 135, 13);
  drawCenter(
    ctx,
    st.school.trim() || "বিদ্যালয়ের নাম",
    cx,
    175,
    17,
    true,
    345
  );
  drawCenter(ctx, `(${CLSN[s.cls - 1] ?? "প্রথম"} শ্রেণি)`, cx, 202, 13);

  const rows: Array<[string, string]> = [
    ["শিক্ষার্থীর নাম", s.name],
    ["শ্রেণি", CLSN[s.cls - 1] ?? "প্রথম"],
    ["রোল", bn(String(s.roll))],
    ["অভিভাবকের নাম", s.guardian],
    ["গ্রাম", s.village],
    ["উপজেলা", s.upazila],
    ["জেলা", s.district],
    ["মোবাইল নম্বর", bn(s.mobile)],
  ];

  let ry = 262;
  for (const [lab, v] of rows) {
    const tx = l + 22;
    ctx.font = fontStr(12, false);
    ctx.fillStyle = "#0F172A";
    ctx.textAlign = "left";
    ctx.fillText(`${lab} :`, tx, ry);

    const vx = tx + 112;
    ctx.font = fontStr(12, true);
    ctx.fillText(v, vx, ry - 1);

    ctx.strokeStyle = "#0F172A";
    ctx.lineWidth = 0.8;
    ctx.beginPath();
    ctx.moveTo(vx, ry + 4);
    ctx.lineTo(l + w - 22, ry + 4);
    ctx.stroke();
    ry += 36;
  }
}

export function renderMarksPage(
  ctx: CanvasRenderingContext2D,
  s: Student,
  fontId?: PdfFontId
) {
  activeFontFamily = getPdfFontOption(fontId).cssFamily;
  ctx.fillStyle = "#FFFFFF";
  ctx.fillRect(0, 0, PAGE_W, PAGE_H);

  const subs = studentSubs(s);
  const L = 24;
  drawCenter(
    ctx,
    `শিখন অগ্রগতি প্রতিবেদন — ${s.name}   |   শ্রেণি: ${CLSN[s.cls - 1] ?? "প্রথম"}   |   রোল: ${bn(String(s.roll))}`,
    PAGE_W / 2,
    28,
    13,
    true,
    790
  );

  let y = 40;
  const sw = 170;
  const cw = (PAGE_W - 2 * L - sw) / 12;
  drawCell(ctx, "বিষয় (পূর্ণমান)", L, y, sw, 50, 9.5, true, true);
  const ord = ["১ম", "২য়", "৩য়"];
  for (let t = 0; t < 3; t++) {
    drawCell(
      ctx,
      `${ord[t]} প্রান্তিক`,
      L + sw + t * 3 * cw,
      y,
      3 * cw,
      22,
      9,
      true,
      true
    );
  }
  drawCell(
    ctx,
    "চূড়ান্ত ফলাফল",
    L + sw + 9 * cw,
    y,
    3 * cw,
    22,
    9,
    true,
    true
  );

  const sub1 = ["ধারাবাহিক\nনম্বর", "সামষ্টিক\nনম্বর", "মোট\nনম্বর"];
  const sub2 = ["প্রান্তিকের\nগড় নম্বর", "বিষয়\nগ্রেড", "সমন্বিত\nগ্রেড"];
  for (let t = 0; t < 3; t++) {
    for (let i = 0; i < 3; i++) {
      drawCell(
        ctx,
        sub1[i],
        L + sw + (t * 3 + i) * cw,
        y + 22,
        cw,
        28,
        7.5,
        true,
        true
      );
    }
  }
  for (let i = 0; i < 3; i++) {
    drawCell(
      ctx,
      sub2[i],
      L + sw + (9 + i) * cw,
      y + 22,
      cw,
      28,
      7.5,
      true,
      true
    );
  }

  y += 50;
  const rh = 32;
  const anyData = [1, 2, 3].some((t) => hasTerm(s, t));

  subs.forEach((sub, i) => {
    const ry = y + i * rh;
    drawCell(ctx, `${sub.bn} (${bn(String(sub.max))})`, L, ry, sw, rh, 9);
    for (let t = 1; t <= 3; t++) {
      const has = hasSub(s, sub, t);
      const b = L + sw + (t - 1) * 3 * cw;
      drawCell(ctx, has ? fmt(cont(s, sub, t)) : "", b, ry, cw, rh);
      drawCell(
        ctx,
        sub.written === 0 ? "--" : has ? fmt(wr(s, sub, t)) : "",
        b + cw,
        ry,
        cw,
        rh
      );
      drawCell(
        ctx,
        has ? fmt(subTotal(s, sub, t)) : "",
        b + 2 * cw,
        ry,
        cw,
        rh,
        9,
        true
      );
    }
    const a = subAvg(s, sub);
    drawCell(
      ctx,
      anyData ? fmt(a) : "",
      L + sw + 9 * cw,
      ry,
      cw,
      rh,
      9,
      true
    );
    drawCell(
      ctx,
      anyData ? gradeOf((a / sub.max) * 100) : "",
      L + sw + 10 * cw,
      ry,
      cw,
      rh,
      10,
      true
    );
  });

  // Merged Overall Grade cell
  const pct = overallPct(s);
  drawCell(
    ctx,
    anyData ? `${gradeOf(pct)}\n${fmt(pct)}%` : "",
    L + sw + 11 * cw,
    y,
    cw,
    rh * subs.length,
    12,
    true
  );

  // Total Row
  const ty = y + subs.length * rh;
  const totMax = subs.reduce((acc, sub) => acc + sub.max, 0);
  drawCell(
    ctx,
    `সর্বমোট নম্বর (${bn(String(totMax))})`,
    L,
    ty,
    sw,
    rh,
    9,
    true,
    true
  );
  for (let t = 1; t <= 3; t++) {
    const b = L + sw + (t - 1) * 3 * cw;
    const has = hasTerm(s, t);
    drawCell(
      ctx,
      has ? fmt(subs.reduce((acc, sub) => acc + cont(s, sub, t), 0)) : "",
      b,
      ty,
      cw,
      rh,
      9,
      false,
      true
    );
    drawCell(
      ctx,
      has ? fmt(subs.reduce((acc, sub) => acc + wr(s, sub, t), 0)) : "",
      b + cw,
      ty,
      cw,
      rh,
      9,
      false,
      true
    );
    drawCell(
      ctx,
      has ? fmt(subs.reduce((acc, sub) => acc + subTotal(s, sub, t), 0)) : "",
      b + 2 * cw,
      ty,
      cw,
      rh,
      9,
      true,
      true
    );
  }
  drawCell(
    ctx,
    anyData ? fmt(subs.reduce((acc, sub) => acc + subAvg(s, sub), 0)) : "",
    L + sw + 9 * cw,
    ty,
    cw,
    rh,
    9,
    true,
    true
  );
  drawCell(ctx, "", L + sw + 10 * cw, ty, cw, rh, 9, false, true);
  drawCell(ctx, "", L + sw + 11 * cw, ty, cw, rh, 9, false, true);

  // ---- Personal & Social Qualities ----
  const by = ty + rh + 18;
  const qw = [70, 270, 46, 46, 46];
  let x = L;
  ["গুণাবলি", "বর্ণনা", "১ম প্রান্তিক", "২য় প্রান্তিক", "৩য় প্রান্তিক"].forEach(
    (h, i) => {
      drawCell(ctx, h, x, by, qw[i], 24, 8.5, true, true);
      x += qw[i];
    }
  );
  const traits: Array<[string, string, string]> = [
    [
      "সক্রিয়তা",
      "ACT",
      "শিক্ষকের নির্দেশনামতো পাঠ্যপুস্তকের কাজ সম্পন্ন করা; বাড়ির কাজ সময়মতো করা; জোড়ায়/দলগত কাজে অংশগ্রহণ; প্রশ্নোত্তরে অংশ নেওয়া ইত্যাদি",
    ],
    [
      "শৃঙ্খলা",
      "DISC",
      "বিদ্যালয়ে নিয়মিত উপস্থিত থাকা; বিদ্যালয়ের নিয়ম-শৃঙ্খলা মেনে চলা; পরিষ্কার-পরিচ্ছন্ন থাকা ইত্যাদি",
    ],
    [
      "আচরণ",
      "COND",
      "শিক্ষক/বড়দের সম্মান করা; ছোটদের স্নেহ করা; সহযোগিতা করা; ভুল হলে স্বীকার করা; অন্যকে কষ্ট না দেওয়া ইত্যাদি",
    ],
  ];

  traits.forEach(([lab, k, d], r) => {
    const yy = by + 24 + r * 38;
    x = L;
    drawCell(ctx, lab, x, yy, qw[0], 38, 9, true);
    x += qw[0];
    drawBox(ctx, d, x, yy, qw[1], 38, 7);
    x += qw[1];
    for (let t = 1; t <= 3; t++) {
      drawCell(ctx, s.x[`${k}_${t}`] ?? "", x, yy, qw[1 + t], 38, 11, true);
      x += qw[1 + t];
    }
  });
  ctx.font = fontStr(8, false);
  ctx.fillStyle = "#0F172A";
  ctx.textAlign = "left";
  ctx.fillText(
    "ক = উত্তম,  খ = সন্তোষজনক,  গ = উন্নতি প্রয়োজন",
    L,
    by + 24 + 3 * 38 + 12
  );

  // ---- Attendance ----
  const ax = L + 478 + 16;
  const aw = [108, 64, 64, 64];
  x = ax;
  ["উপস্থিতি", "১ম প্রান্তিক", "২য় প্রান্তিক", "৩য় প্রান্তিক"].forEach(
    (h, i) => {
      drawCell(ctx, h, x, by, aw[i], 24, 8.5, true, true);
      x += aw[i];
    }
  );
  const labels = ["মোট কার্যদিবস", "শিক্ষার্থীর উপস্থিতি", "উপস্থিতির হার"];
  labels.forEach((lab, r) => {
    const yy = by + 24 + r * 38;
    x = ax;
    drawCell(ctx, lab, x, yy, aw[0], 38, 9, true);
    x += aw[0];
    for (let t = 1; t <= 3; t++) {
      const dRaw = s.x[`DAYS_${t}`];
      const pRaw = s.x[`PRES_${t}`];
      const d = dRaw ? Number.parseFloat(fromBn(dRaw)) : Number.NaN;
      const p = pRaw ? Number.parseFloat(fromBn(pRaw)) : Number.NaN;
      let txt = "";
      if (r === 0) txt = !Number.isNaN(d) ? fmt(d) : "";
      else if (r === 1) txt = !Number.isNaN(p) ? fmt(p) : "";
      else txt = !Number.isNaN(d) && !Number.isNaN(p) && d > 0 ? `${fmt((p / d) * 100)}%` : "";
      drawCell(ctx, txt, x, yy, aw[t], 38, 10);
      x += aw[t];
    }
  });
}

export async function exportStudentsToPdf(
  students: Student[],
  settings: Settings,
  filename: string
): Promise<void> {
  if (students.length === 0) return;

  await ensurePdfFontLoaded(settings.pdfFont);

  const doc = new jsPDF({
    orientation: "landscape",
    unit: "pt",
    format: [PAGE_W, PAGE_H],
    compress: true,
  });

  const scale = 2.5;
  const canvas = document.createElement("canvas");
  canvas.width = Math.round(PAGE_W * scale);
  canvas.height = Math.round(PAGE_H * scale);
  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("Canvas 2D context unavailable.");

  students.forEach((s, index) => {
    if (index > 0) doc.addPage([PAGE_W, PAGE_H], "landscape");
    ctx.setTransform(scale, 0, 0, scale, 0, 0);
    renderCoverPage(ctx, s, settings, false);
    const p1Img = canvas.toDataURL("image/png");
    doc.addImage(p1Img, "PNG", 0, 0, PAGE_W, PAGE_H, undefined, "FAST");

    doc.addPage([PAGE_W, PAGE_H], "landscape");
    ctx.setTransform(scale, 0, 0, scale, 0, 0);
    renderMarksPage(ctx, s, settings.pdfFont);
    const p2Img = canvas.toDataURL("image/png");
    doc.addImage(p2Img, "PNG", 0, 0, PAGE_W, PAGE_H, undefined, "FAST");
  });

  doc.save(filename);
}
