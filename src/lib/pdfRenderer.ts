import { jsPDF } from "jspdf";
import {
  CLSN,
  DpeLogoStyle,
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
const FOLD_X = PAGE_W / 2; // 421pt

let activeFontFamily = getPdfFontOption("noto_serif").cssFamily;
const logoImgCache: Partial<Record<DpeLogoStyle, HTMLImageElement>> = {};

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error(`Failed to load ${src}`));
    img.src = src;
  });
}

export async function ensurePdfFontLoaded(fontId?: PdfFontId): Promise<void> {
  const opt = getPdfFontOption(fontId);
  const logoPromises = (
    [
      ["simple", "/dpe-logo-simple.png"],
      ["color", "/dpe-logo-color.png"],
    ] as const
  ).map(async ([key, src]) => {
    if (!logoImgCache[key]) {
      logoImgCache[key] = await loadImage(src);
    }
  });
  if (document.fonts?.load) {
    await Promise.allSettled([
      document.fonts.load(`400 16px "${opt.primaryFace}"`, "শিখন অগ্রগতি প্রতিবেদন ০১২৩"),
      document.fonts.load(`700 16px "${opt.primaryFace}"`, "শিখন অগ্রগতি প্রতিবেদন ০১২৩"),
      ...logoPromises,
    ]);
  } else {
    await Promise.allSettled(logoPromises);
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

function drawPanelFrames(ctx: CanvasRenderingContext2D, showFoldGuide: boolean) {
  if (showFoldGuide) {
    ctx.save();
    ctx.setLineDash([4, 4]);
    ctx.strokeStyle = "#94A3B8";
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(FOLD_X, 6);
    ctx.lineTo(FOLD_X, PAGE_H - 6);
    ctx.stroke();
    ctx.restore();
  }

  // Left A5 Panel Frame (18..409)
  ctx.strokeStyle = "#0F172A";
  ctx.lineWidth = 2.4;
  ctx.strokeRect(18, 18, 391, 559);
  ctx.lineWidth = 0.8;
  ctx.strokeRect(24, 24, 379, 547);

  // Right A5 Panel Frame (433..824)
  ctx.lineWidth = 2.4;
  ctx.strokeRect(433, 18, 391, 559);
  ctx.lineWidth = 0.8;
  ctx.strokeRect(439, 24, 379, 547);
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
    ctx.fillStyle = "#E2E8F0";
    ctx.fillRect(l, top, w, h);
  }
  ctx.strokeStyle = "#0F172A";
  ctx.lineWidth = 0.75;
  ctx.strokeRect(l, top, w, h);

  if (!text) return;

  let curSize = size;
  const lines = text.split("\n");
  ctx.font = fontStr(curSize, bold);
  while (
    lines.some((ln) => ctx.measureText(ln).width > w - 4) &&
    curSize > 4.5
  ) {
    curSize -= 0.4;
    ctx.font = fontStr(curSize, bold);
  }

  ctx.fillStyle = "#0F172A";
  ctx.textAlign = "center";
  ctx.textBaseline = "alphabetic";
  const lh = curSize * 1.24;
  let y = top + (h - lh * lines.length) / 2 + curSize * 0.92;
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
  size = 7.2
) {
  ctx.strokeStyle = "#0F172A";
  ctx.lineWidth = 0.75;
  ctx.strokeRect(l, top, w, h);
  if (!text) return;

  let curSize = size;
  let lines = wrapText(ctx, text, w - 7);
  ctx.font = fontStr(curSize, false);
  while (lines.length * (curSize * 1.25) > h - 4 && curSize > 5.2) {
    curSize -= 0.3;
    ctx.font = fontStr(curSize, false);
    lines = wrapText(ctx, text, w - 7);
  }

  ctx.fillStyle = "#0F172A";
  ctx.textAlign = "left";
  ctx.textBaseline = "alphabetic";
  const lh = curSize * 1.25;
  const totalH = lines.length * lh;
  let y = top + Math.max(curSize + 2, (h - totalH) / 2 + curSize * 0.9);
  for (const line of lines) {
    if (y < top + h - 2) {
      ctx.fillText(line, l + 3.5, y);
    }
    y += lh;
  }
}

function drawCenter(
  ctx: CanvasRenderingContext2D,
  text: string,
  cx: number,
  y: number,
  size: number,
  bold = false,
  maxW = 340,
  color = "#0F172A"
) {
  let curSize = size;
  ctx.font = fontStr(curSize, bold);
  while (ctx.measureText(text).width > maxW && curSize > 5.5) {
    curSize -= 0.5;
    ctx.font = fontStr(curSize, bold);
  }
  ctx.fillStyle = color;
  ctx.textAlign = "center";
  ctx.textBaseline = "alphabetic";
  ctx.fillText(text, cx, y);
}

function drawDottedLine(
  ctx: CanvasRenderingContext2D,
  x1: number,
  x2: number,
  y: number
) {
  ctx.save();
  ctx.setLineDash([1.6, 2.4]);
  ctx.strokeStyle = "#334155";
  ctx.lineWidth = 0.85;
  ctx.beginPath();
  ctx.moveTo(x1, y);
  ctx.lineTo(x2, y);
  ctx.stroke();
  ctx.restore();
}

function drawDottedField(
  ctx: CanvasRenderingContext2D,
  label: string,
  value: string,
  x: number,
  y: number,
  maxX: number,
  fontSize = 10.5
) {
  ctx.font = fontStr(fontSize, false);
  ctx.fillStyle = "#0F172A";
  ctx.textAlign = "left";
  ctx.textBaseline = "alphabetic";
  ctx.fillText(label, x, y);
  const labelW = ctx.measureText(label).width;
  const lineStart = x + labelW + 5;
  drawDottedLine(ctx, lineStart, maxX, y + 3);

  const val = value.trim();
  if (val) {
    let vs = fontSize;
    ctx.font = fontStr(vs, true);
    const avail = Math.max(20, maxX - lineStart - 4);
    while (ctx.measureText(val).width > avail && vs > 6.5) {
      vs -= 0.4;
      ctx.font = fontStr(vs, true);
    }
    ctx.fillStyle = "#0F172A";
    ctx.fillText(val, lineStart + 3, y - 0.5);
  }
}

function drawDpeLogo(
  ctx: CanvasRenderingContext2D,
  cx: number,
  topY: number,
  targetH: number,
  style: DpeLogoStyle = "simple"
) {
  const img = logoImgCache[style] || logoImgCache.simple;
  if (img && img.naturalWidth > 0 && img.naturalHeight > 0) {
    const w = (img.naturalWidth / img.naturalHeight) * targetH;
    ctx.drawImage(img, cx - w / 2, topY, w, targetH);
    return;
  }

  // Fallback vector seal if bitmap is still loading
  const cy = topY + targetH / 2 - 4;
  const r = 34;
  ctx.save();
  ctx.strokeStyle = "#0F172A";
  ctx.lineWidth = 1.6;
  ctx.beginPath();
  ctx.arc(cx, cy, r, 0, Math.PI * 2);
  ctx.stroke();
  drawCenter(ctx, "প্রাথমিক শিক্ষা অধিদপ্তর", cx, cy + r + 12, 8.5, true, 160);
  ctx.restore();
}

function drawNctbEmblem(ctx: CanvasRenderingContext2D, cx: number, cy: number, r = 19) {
  ctx.save();
  ctx.strokeStyle = "#0F172A";
  ctx.lineWidth = 1.4;
  ctx.beginPath();
  ctx.arc(cx, cy, r, 0, Math.PI * 2);
  ctx.stroke();

  ctx.lineWidth = 0.7;
  ctx.beginPath();
  ctx.arc(cx, cy, r - 3.5, 0, Math.PI * 2);
  ctx.stroke();

  // Torch & book motif
  ctx.lineWidth = 1.1;
  ctx.beginPath();
  ctx.moveTo(cx, cy - 10);
  ctx.lineTo(cx, cy + 8);
  ctx.stroke();

  ctx.beginPath();
  ctx.moveTo(cx - 8, cy + 4);
  ctx.quadraticCurveTo(cx - 4, cy + 1, cx, cy + 4);
  ctx.quadraticCurveTo(cx + 4, cy + 1, cx + 8, cy + 4);
  ctx.stroke();

  ctx.beginPath();
  ctx.arc(cx, cy - 6, 3.2, 0, Math.PI * 2);
  ctx.stroke();

  ctx.font = fontStr(6.5, true);
  ctx.fillStyle = "#0F172A";
  ctx.textAlign = "center";
  ctx.fillText("NCTB", cx, cy + 12.5);
  ctx.restore();
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

  drawPanelFrames(ctx, showFoldGuide);

  // =========================================================================
  // LEFT HALF (x: 18..409) = BACK COVER (Page 4 when folded)
  // =========================================================================
  const bx0 = 32;
  const bCw = [34, 134, 65, 65, 65]; // sum = 363 (32..395)
  const bHd = [
    "প্রান্তিক",
    "শ্রেণি শিক্ষকের\nসার্বিক মতামত",
    "শ্রেণি শিক্ষকের\nস্বাক্ষর",
    "প্রধান শিক্ষকের\nস্বাক্ষর",
    "অভিভাবকের\nস্বাক্ষর",
  ];
  let x = bx0;
  bHd.forEach((h, i) => {
    drawCell(ctx, h, x, 36, bCw[i], 30, 8, true, true);
    x += bCw[i];
  });

  const termLabels = ["১ম\nপ্রান্তিক", "২য়\nপ্রান্তিক", "৩য়\nপ্রান্তিক"];
  for (let t = 1; t <= 3; t++) {
    const y = 66 + (t - 1) * 66;
    x = bx0;
    drawCell(ctx, termLabels[t - 1], x, y, bCw[0], 66, 8, true);
    x += bCw[0];
    drawBox(ctx, s.x[`REM_${t}`] ?? "", x, y, bCw[1], 66, 7.5);
    x += bCw[1];
    for (let i = 2; i <= 4; i++) {
      drawCell(ctx, "", x, y, bCw[i], 66);
      x += bCw[i];
    }
  }

  // Centered Grade Scale Table
  const gy = 282;
  const gw = [102, 54, 140]; // sum = 296
  const gx = bx0 + (363 - 296) / 2;
  x = gx;
  ["প্রাপ্ত নম্বর", "গ্রেড", "শিখন অর্জন"].forEach((h, i) => {
    drawCell(ctx, h, x, gy, gw[i], 22, 9, true, true);
    x += gw[i];
  });
  const scale: Array<[string, string, string]> = [
    ["৮০%-১০০%", "A", "অতি উত্তম"],
    ["৬০%-৭৯%", "B", "উত্তম"],
    ["৪০%-৫৯%", "C", "সন্তোষজনক"],
    ["০%-৩৯%", "D", "সহায়তা প্রয়োজন"],
  ];
  scale.forEach(([a, g, d], r) => {
    x = gx;
    const y = gy + 22 + r * 23;
    drawCell(ctx, a, x, y, gw[0], 23, 9);
    x += gw[0];
    drawCell(ctx, g, x, y, gw[1], 23, 10, true);
    x += gw[1];
    drawCell(ctx, d, x, y, gw[2], 23, 9);
  });

  // Single-line Class Teacher Name + Mobile with dotted lines
  const tn = st.teachers[s.cls] ?? "";
  const tm = st.teacherMobiles[s.cls] ?? "";
  drawDottedField(ctx, "শ্রেণি শিক্ষকের নাম:", tn, 32, 438, 240, 9.5);
  drawDottedField(ctx, "মোবাইল নম্বর:", bn(tm), 246, 438, 395, 9.5);

  // NCTB Emblem & Footer
  const backCx = 18 + 391 / 2;
  drawNctbEmblem(ctx, backCx, 488, 19);
  drawCenter(
    ctx,
    "জাতীয় শিক্ষাক্রম ও পাঠ্যপুস্তক বোর্ড, বাংলাদেশ",
    backCx,
    532,
    10.5,
    true,
    355
  );
  drawCenter(
    ctx,
    "প্রাথমিক শিক্ষা স্তর (১ম–৫ম শ্রেণি)",
    backCx,
    549,
    8,
    false,
    355,
    "#475569"
  );

  // =========================================================================
  // RIGHT HALF (x: 433..824) = FRONT COVER (Page 1 when folded)
  // =========================================================================
  const frontCx = 433 + 391 / 2; // 628.5

  // Dark rounded pill title banner
  const pillW = 312;
  const pillH = 44;
  const pillX = frontCx - pillW / 2;
  const pillY = 46;
  ctx.save();
  ctx.fillStyle = "#0F172A";
  ctx.beginPath();
  ctx.roundRect(pillX, pillY, pillW, pillH, 22);
  ctx.fill();
  ctx.restore();
  drawCenter(ctx, "শিখন অগ্রগতি প্রতিবেদন", frontCx, 75, 20, true, 290, "#FFFFFF");

  // Official DPE Logo (Simple B&W or Color per Settings)
  drawDpeLogo(ctx, frontCx, 102, 104, st.dpeLogo || "simple");

  // School Name & Academic Year with dotted lines
  drawDottedField(
    ctx,
    "বিদ্যালয়ের নাম:",
    st.school.trim() || "বিদ্যালয়ের নাম",
    456,
    236,
    801,
    11
  );
  drawDottedField(
    ctx,
    "শিক্ষাবর্ষ:",
    bn(st.year),
    556,
    266,
    701,
    11
  );

  // 2-Column Dotted-Line Student Info Form (compact 32pt row spacing, positioned in lower half)
  const formY0 = 360;
  const formDy = 32;
  drawDottedField(ctx, "শিক্ষার্থীর নাম:", s.name, 456, formY0, 801, 10.5);

  drawDottedField(
    ctx,
    "শ্রেণি:",
    CLSN[s.cls - 1] ?? "প্রথম",
    456,
    formY0 + formDy,
    622,
    10.5
  );
  drawDottedField(
    ctx,
    "রোল/আইডি:",
    bn(String(s.roll)),
    632,
    formY0 + formDy,
    801,
    10.5
  );

  drawDottedField(ctx, "গ্রাম:", s.village, 456, formY0 + formDy * 2, 622, 10.5);
  drawDottedField(ctx, "পোস্ট:", s.post ?? "", 632, formY0 + formDy * 2, 801, 10.5);

  drawDottedField(ctx, "উপজেলা/থানা:", s.upazila, 456, formY0 + formDy * 3, 636, 10.5);
  drawDottedField(ctx, "জেলা:", s.district, 646, formY0 + formDy * 3, 801, 10.5);

  drawDottedField(ctx, "অভিভাবকের নাম:", s.guardian, 456, formY0 + formDy * 4, 656, 10.5);
  drawDottedField(ctx, "মোবাইল:", bn(s.mobile), 664, formY0 + formDy * 4, 801, 10.5);

  drawCenter(
    ctx,
    `(${CLSN[s.cls - 1] ?? "প্রথম"} শ্রেণি — শিখন অগ্রগতি প্রতিবেদন)`,
    frontCx,
    536,
    9.5,
    true,
    340,
    "#334155"
  );
}

export function renderMarksPage(
  ctx: CanvasRenderingContext2D,
  s: Student,
  fontId?: PdfFontId,
  showFoldGuide = false
) {
  activeFontFamily = getPdfFontOption(fontId).cssFamily;
  ctx.fillStyle = "#FFFFFF";
  ctx.fillRect(0, 0, PAGE_W, PAGE_H);

  drawPanelFrames(ctx, showFoldGuide);

  const subs = studentSubs(s);
  const anyData = [1, 2, 3].some((t) => hasTerm(s, t));
  const totMax = subs.reduce((acc, sub) => acc + sub.max, 0);

  // Subtle top bar inside each folded inner page
  ctx.font = fontStr(8.5, true);
  ctx.fillStyle = "#334155";
  ctx.textAlign = "left";
  ctx.fillText(`শিক্ষার্থীর নাম: ${s.name}`, 28, 38);
  ctx.textAlign = "right";
  ctx.fillText(`শ্রেণি: ${CLSN[s.cls - 1] ?? "প্রথম"}`, 399, 38);

  ctx.textAlign = "left";
  ctx.fillText(`রোল/আইডি: ${bn(String(s.roll))}`, 443, 38);
  ctx.textAlign = "right";
  ctx.fillText(`ধর্ম: ${s.religion}`, 814, 38);

  // =========================================================================
  // LEFT INNER HALF (x: 28..399) = Page 2: Marks (Term 1 & 2) + গুণাবলি
  // =========================================================================
  const lx = 28;
  const sw = 125;
  const lcw = 41; // 125 + 6*41 = 371
  const ty0 = 46;
  const hdr1H = 18;
  const hdr2H = 26;
  const hdrH = hdr1H + hdr2H; // 44
  const rh = 27.5;

  drawCell(ctx, "বিষয় (পূর্ণমান)", lx, ty0, sw, hdrH, 8.5, true, true);
  drawCell(ctx, "১ম প্রান্তিক", lx + sw, ty0, 3 * lcw, hdr1H, 8.5, true, true);
  drawCell(
    ctx,
    "২য় প্রান্তিক",
    lx + sw + 3 * lcw,
    ty0,
    3 * lcw,
    hdr1H,
    8.5,
    true,
    true
  );

  const termSubHdr = ["ধারাবাহিক\nনম্বর", "সামষ্টিক\nনম্বর", "মোট\nনম্বর"];
  for (let t = 0; t < 2; t++) {
    for (let i = 0; i < 3; i++) {
      drawCell(
        ctx,
        termSubHdr[i],
        lx + sw + (t * 3 + i) * lcw,
        ty0 + hdr1H,
        lcw,
        hdr2H,
        7,
        true,
        true
      );
    }
  }

  const rowsStartY = ty0 + hdrH; // 90
  subs.forEach((sub, i) => {
    const ry = rowsStartY + i * rh;
    drawCell(ctx, `${sub.bn} (${bn(String(sub.max))})`, lx, ry, sw, rh, 8);
    for (let t = 1; t <= 2; t++) {
      const has = hasSub(s, sub, t);
      const b = lx + sw + (t - 1) * 3 * lcw;
      const contTxt =
        sub.key === "ART" ? "......" : has ? fmt(cont(s, sub, t)) : "";
      const wrTxt =
        sub.written === 0 ? "......" : has ? fmt(wr(s, sub, t)) : "";
      const totTxt = has ? fmt(subTotal(s, sub, t)) : "";

      drawCell(ctx, contTxt, b, ry, lcw, rh, 8);
      drawCell(ctx, wrTxt, b + lcw, ry, lcw, rh, 8);
      drawCell(ctx, totTxt, b + 2 * lcw, ry, lcw, rh, 8.5, true);
    }
  });

  // Left Total Row
  const ty = rowsStartY + subs.length * rh;
  const totalH = 26;
  drawCell(
    ctx,
    `সর্বমোট নম্বর (${bn(String(totMax))})`,
    lx,
    ty,
    sw,
    totalH,
    8,
    true,
    true
  );
  for (let t = 1; t <= 2; t++) {
    const b = lx + sw + (t - 1) * 3 * lcw;
    const has = hasTerm(s, t);
    const label = t === 1 ? "১ম প্রান্তিকে মোট" : "২য় প্রান্তিকে মোট";
    drawCell(ctx, label, b, ty, 2 * lcw, totalH, 7.5, true, true);
    drawCell(
      ctx,
      has ? fmt(subs.reduce((acc, sub) => acc + subTotal(s, sub, t), 0)) : "",
      b + 2 * lcw,
      ty,
      lcw,
      totalH,
      8.5,
      true,
      true
    );
  }

  // ---- Bottom Left: ব্যক্তিগত ও সামাজিক গুণাবলি (width = 371) ----
  const by = ty + totalH + 14;
  const qw = [52, 187, 44, 44, 44]; // sum = 371
  const qTopH = 16;
  const qSub1H = 13;
  const qSub2H = 19;
  const qHdrH = qTopH + qSub1H + qSub2H; // 48
  const qRowH = 42;

  // Tier 1 merged headers
  drawCell(ctx, "ব্যক্তিগত ও সামাজিক গুণাবলি", lx, by, 177, qTopH, 7.5, true, true);
  drawCell(ctx, "অবস্থান", lx + 177, by, 62, qTopH, 7.5, true, true);
  drawCell(ctx, "উত্তম", lx + 239, by, qw[2], qTopH, 7.2, true, true);
  drawCell(ctx, "সন্তোষজনক", lx + 239 + qw[2], by, qw[3], qTopH, 6.8, true, true);
  drawCell(
    ctx,
    "উন্নতি প্রয়োজন",
    lx + 239 + qw[2] + qw[3],
    by,
    qw[4],
    qTopH,
    6.4,
    true,
    true
  );

  // Tier 2
  drawCell(ctx, "গুণাবলি", lx, by + qTopH, qw[0], qSub1H + qSub2H, 8, true, true);
  drawCell(
    ctx,
    "বর্ণনা",
    lx + qw[0],
    by + qTopH,
    qw[1],
    qSub1H + qSub2H,
    8,
    true,
    true
  );
  const kkg = ["ক", "খ", "গ"];
  const termShort = ["১ম প্রান্তিক", "২য় প্রান্তিক", "৩য় প্রান্তিক"];
  for (let i = 0; i < 3; i++) {
    const cx = lx + qw[0] + qw[1] + i * qw[2 + i];
    drawCell(ctx, kkg[i], cx, by + qTopH, qw[2 + i], qSub1H, 7.5, true, true);
    drawCell(
      ctx,
      termShort[i],
      cx,
      by + qTopH + qSub1H,
      qw[2 + i],
      qSub2H,
      6.8,
      true,
      true
    );
  }

  const traits: Array<[string, string, string]> = [
    [
      "সক্রিয়তা",
      "ACT",
      "শিক্ষকের নির্দেশনামতো পাঠ্যপুস্তকের কাজ সম্পন্ন করা; বাড়ির কাজ সময়মতো করা; জোড়ায় বা দলগত কাজে অংশ নেওয়া; প্রশ্নোত্তরে অংশগ্রহণ ইত্যাদি",
    ],
    [
      "শৃঙ্খলা",
      "DISC",
      "বিদ্যালয়ে নিয়মিত উপস্থিত থাকা; বিদ্যালয়ের নিয়ম-শৃঙ্খলা মেনে চলা; পরিষ্কার-পরিচ্ছন্ন থাকা ইত্যাদি",
    ],
    [
      "আচরণ",
      "COND",
      "শিক্ষক ও বড়দের সম্মান করা; ছোটদের স্নেহ করা; সহযোগিতা করা; ভুল হলে তা স্বীকার করা; অন্যকে কষ্ট না দেওয়া ইত্যাদি",
    ],
  ];

  traits.forEach(([lab, k, d], r) => {
    const yy = by + qHdrH + r * qRowH;
    let cx = lx;
    drawCell(ctx, lab, cx, yy, qw[0], qRowH, 8.5, true);
    cx += qw[0];
    drawBox(ctx, d, cx, yy, qw[1], qRowH, 6.8);
    cx += qw[1];
    for (let t = 1; t <= 3; t++) {
      drawCell(ctx, s.x[`${k}_${t}`] ?? "", cx, yy, qw[1 + t], qRowH, 10, true);
      cx += qw[1 + t];
    }
  });

  ctx.font = fontStr(7.5, false);
  ctx.fillStyle = "#334155";
  ctx.textAlign = "left";
  ctx.fillText(
    "ক = উত্তম,  খ = সন্তোষজনক,  গ = উন্নতি প্রয়োজন",
    lx,
    by + qHdrH + 3 * qRowH + 11
  );

  // =========================================================================
  // RIGHT INNER HALF (x: 443..814) = Page 3: Marks (Term 3 & Final) + উপস্থিতি
  // =========================================================================
  const rx = 443;
  const rW = 371;
  const rcw = rW / 6; // 61.8333

  drawCell(ctx, "৩য় প্রান্তিক", rx, ty0, 3 * rcw, hdr1H, 8.5, true, true);
  drawCell(
    ctx,
    "চূড়ান্ত ফলাফল",
    rx + 3 * rcw,
    ty0,
    3 * rcw,
    hdr1H,
    8.5,
    true,
    true
  );

  const finalSubHdr = ["প্রান্তিকের\nগড় নম্বর", "বিষয়\nগ্রেড", "সমন্বিত\nগ্রেড"];
  for (let i = 0; i < 3; i++) {
    drawCell(
      ctx,
      termSubHdr[i],
      rx + i * rcw,
      ty0 + hdr1H,
      rcw,
      hdr2H,
      7,
      true,
      true
    );
    drawCell(
      ctx,
      finalSubHdr[i],
      rx + (3 + i) * rcw,
      ty0 + hdr1H,
      rcw,
      hdr2H,
      7,
      true,
      true
    );
  }

  subs.forEach((sub, i) => {
    const ry = rowsStartY + i * rh;
    const has3 = hasSub(s, sub, 3);
    const cont3 =
      sub.key === "ART" ? "......" : has3 ? fmt(cont(s, sub, 3)) : "";
    const wr3 =
      sub.written === 0 ? "......" : has3 ? fmt(wr(s, sub, 3)) : "";
    const tot3 = has3 ? fmt(subTotal(s, sub, 3)) : "";

    drawCell(ctx, cont3, rx, ry, rcw, rh, 8);
    drawCell(ctx, wr3, rx + rcw, ry, rcw, rh, 8);
    drawCell(ctx, tot3, rx + 2 * rcw, ry, rcw, rh, 8.5, true);

    const a = subAvg(s, sub);
    drawCell(
      ctx,
      anyData ? fmt(a) : "",
      rx + 3 * rcw,
      ry,
      rcw,
      rh,
      8.5,
      true
    );
    drawCell(
      ctx,
      anyData ? gradeOf((a / sub.max) * 100) : "",
      rx + 4 * rcw,
      ry,
      rcw,
      rh,
      9.5,
      true
    );
  });

  // Merged Overall Grade cell across all subjects
  const pct = overallPct(s);
  drawCell(
    ctx,
    anyData ? `${gradeOf(pct)}\n${fmt(pct)}%` : "",
    rx + 5 * rcw,
    rowsStartY,
    rcw,
    rh * subs.length,
    11.5,
    true
  );

  // Right Total Row (aligned horizontally with Left Total Row)
  const has3Term = hasTerm(s, 3);
  drawCell(ctx, "৩য় প্রান্তিকে মোট", rx, ty, 2 * rcw, totalH, 7.5, true, true);
  drawCell(
    ctx,
    has3Term
      ? fmt(subs.reduce((acc, sub) => acc + subTotal(s, sub, 3), 0))
      : "",
    rx + 2 * rcw,
    ty,
    rcw,
    totalH,
    8.5,
    true,
    true
  );
  drawCell(
    ctx,
    "প্রান্তিকের গড় মোট",
    rx + 3 * rcw,
    ty,
    rcw,
    totalH,
    7,
    true,
    true
  );
  drawCell(
    ctx,
    anyData ? fmt(subs.reduce((acc, sub) => acc + subAvg(s, sub), 0)) : "",
    rx + 4 * rcw,
    ty,
    rcw,
    totalH,
    8.5,
    true,
    true
  );
  drawCell(ctx, "", rx + 5 * rcw, ty, rcw, totalH, 8, false, true);

  // ---- Bottom Right: উপস্থিতি (aligned with গুণাবলি) ----
  const aw = [125, 82, 82, 82]; // sum = 371
  const attTopH = 20;
  const attSubH = 28; // total header = 48 (matches qHdrH)
  drawCell(ctx, "উপস্থিতি", rx, by, rW, attTopH, 9, true, true);
  let ax = rx;
  ["বিবরণ", "১ম প্রান্তিক", "২য় প্রান্তিক", "৩য় প্রান্তিক"].forEach((h, i) => {
    drawCell(ctx, h, ax, by + attTopH, aw[i], attSubH, 8, true, true);
    ax += aw[i];
  });

  const attLabels = [
    "মোট কার্যদিবস",
    "শিক্ষার্থীর উপস্থিতি",
    "উপস্থিতির হার",
  ];
  attLabels.forEach((lab, r) => {
    const yy = by + qHdrH + r * qRowH;
    ax = rx;
    drawCell(ctx, lab, ax, yy, aw[0], qRowH, 8.5, true);
    ax += aw[0];
    for (let t = 1; t <= 3; t++) {
      const dRaw = s.x[`DAYS_${t}`];
      const pRaw = s.x[`PRES_${t}`];
      const d = dRaw ? Number.parseFloat(fromBn(dRaw)) : Number.NaN;
      const p = pRaw ? Number.parseFloat(fromBn(pRaw)) : Number.NaN;
      let txt = "";
      if (r === 0) txt = !Number.isNaN(d) ? fmt(d) : "";
      else if (r === 1) txt = !Number.isNaN(p) ? fmt(p) : "";
      else
        txt =
          !Number.isNaN(d) && !Number.isNaN(p) && d > 0
            ? `${fmt((p / d) * 100)}%`
            : "";
      drawCell(ctx, txt, ax, yy, aw[t], qRowH, 9.5, r === 2);
      ax += aw[t];
    }
  });

  ctx.font = fontStr(7.5, false);
  ctx.fillStyle = "#334155";
  ctx.textAlign = "left";
  ctx.fillText(
    "উপস্থিতির হার = (শিক্ষার্থীর উপস্থিতি ÷ মোট কার্যদিবস) × ১০০%",
    rx,
    by + qHdrH + 3 * qRowH + 11
  );
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
    renderMarksPage(ctx, s, settings.pdfFont, false);
    const p2Img = canvas.toDataURL("image/png");
    doc.addImage(p2Img, "PNG", 0, 0, PAGE_W, PAGE_H, undefined, "FAST");
  });

  doc.save(filename);
}
