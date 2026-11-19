import "dotenv/config";
import express from "express";
import { initializeApp, getApps, getApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { GoogleGenAI } from "@google/genai";

const PORT = Number(process.env.PORT || 8080);
const FIREBASE_PROJECT_ID = (process.env.FIREBASE_PROJECT_ID || "result-maker-kgps").trim();
const GEMINI_MODEL = (process.env.GEMINI_MODEL || "gemini-4-hydrogen").trim();
const GEMINI_API_KEY = (process.env.GEMINI_API_KEY || "").trim();

const firebaseApp = getApps().length
  ? getApp()
  : initializeApp({ projectId: FIREBASE_PROJECT_ID });

const firebaseAuth = getAuth(firebaseApp);

function createAiClient() {
  if (!GEMINI_API_KEY) {
    throw new Error("GEMINI_API_KEY environment variable is not configured on Cloud Run.");
  }
  return new GoogleGenAI({
    apiKey: GEMINI_API_KEY,
    httpOptions: {
      headers: {
        "User-Agent": "aistudio-build",
      },
    },
  });
}

function stripJsonFences(raw) {
  const trimmed = String(raw || "").trim();
  if (!trimmed.startsWith("```")) return trimmed;
  return trimmed
    .replace(/^```json\s*/i, "")
    .replace(/^```\s*/, "")
    .replace(/\s*```$/, "")
    .trim();
}

async function requireFirebaseAuth(req, res, next) {
  const authHeader = req.headers.authorization || "";
  if (!authHeader.startsWith("Bearer ")) {
    return res.status(401).json({
      error: "অননুমোদিত অনুরোধ: Firebase Auth টোকেন পাওয়া যায়নি।",
      code: "UNAUTHENTICATED",
    });
  }
  const idToken = authHeader.slice("Bearer ".length).trim();
  if (!idToken) {
    return res.status(401).json({
      error: "অননুমোদিত অনুরোধ: খালি Firebase টোকেন।",
      code: "UNAUTHENTICATED",
    });
  }
  try {
    const decoded = await firebaseAuth.verifyIdToken(idToken);
    req.user = decoded;
    return next();
  } catch (err) {
    console.error("Firebase ID token verification failed:", err?.message || err);
    return res.status(401).json({
      error: "সেশন যাচাই ব্যর্থ হয়েছে। অনুগ্রহ করে আবার সাইন-ইন করে চেষ্টা করুন।",
      code: "INVALID_TOKEN",
    });
  }
}

function handleUpstreamError(res, err) {
  const msg = String(err?.message || err || "Unknown error");
  console.error("Gemini proxy error:", msg);
  const status = Number(err?.status || err?.code || 500);
  if (msg.includes("GEMINI_API_KEY")) {
    return res.status(503).json({ error: "সার্ভারে GEMINI_API_KEY সেট করা নেই।", code: "MISSING_API_KEY" });
  }
  if (status === 429 || msg.toLowerCase().includes("resource_exhausted") || msg.includes("429")) {
    return res.status(429).json({ error: "Gemini API-এর ব্যবহার সীমা পূর্ণ হয়েছে। কিছুক্ষণ পর চেষ্টা করুন।", code: "QUOTA_EXCEEDED" });
  }
  if (status === 403 || msg.toLowerCase().includes("permission_denied")) {
    return res.status(403).json({ error: "সার্ভারের Gemini API অনুমতি বা কী সঠিক নয়।", code: "API_PERMISSION_DENIED" });
  }
  return res.status(500).json({ error: `AI সার্ভার ত্রুটি: ${msg}`, code: "AI_ERROR" });
}

const app = express();
app.use(express.json({ limit: "15mb" }));

app.get("/health", (_req, res) => {
  res.json({
    ok: true,
    service: "result-maker-ai-proxy",
    projectId: FIREBASE_PROJECT_ID,
    model: GEMINI_MODEL,
    geminiConfigured: Boolean(GEMINI_API_KEY),
  });
});

app.post("/api/ai/chat", requireFirebaseAuth, async (req, res) => {
  try {
    const ai = createAiClient();
    const { history = [], message = "", mode = "GENERAL", studentContext = "" } = req.body || {};
    if (!String(message).trim()) {
      return res.status(400).json({ error: "বার্তা খালি হতে পারবে না।" });
    }
    const recentTurns = Array.isArray(history)
      ? history.slice(-6).map((m) => `${m.isUser ? "শিক্ষক" : "সহায়ক"}: ${String(m.text || "").slice(0, 400)}`).join("\n")
      : "";

    const prompt = [
      studentContext ? `নির্বাচিত শিক্ষার্থীর তথ্য: ${studentContext}` : "",
      recentTurns ? `পূর্ববর্তী কথোপকথন:\n${recentTurns}` : "",
      `শিক্ষকের বর্তমান প্রশ্ন: ${String(message).trim()}`,
    ].filter(Boolean).join("\n\n");

    const response = await ai.models.generateContent({
      model: GEMINI_MODEL,
      contents: prompt,
      config: {
        systemInstruction:
          "আপনি বাংলাদেশের সরকারি প্রাথমিক বিদ্যালয়ের (১ম–৫ম শ্রেণি) একজন অভিজ্ঞ শিক্ষামূলক AI সহায়ক। সব উত্তর সহজ, প্রাঞ্জল ও প্রাতিষ্ঠানিক বাংলায় দিন। NCTB প্রাথমিক মূল্যায়ন কাঠামো: ১ম–২য় শ্রেণি (বাংলা/ইংরেজি/গণিত = ধারাবাহিক ৫০ + সামষ্টিক ৫০), ৩য়–৫ম শ্রেণি (বাংলা/ইংরেজি/গণিত/বিজ্ঞান/বাওবি/ধর্ম = ধারাবাহিক ৩০ [পাঠ্যবই ১০ + সক্রিয়তা ৫ + ভাষা ৫ + ক্লাস টেস্ট ১০] + সামষ্টিক ৭০)।",
      },
    });

    const text = (response.text || "").trim() || "দুঃখিত, এই মুহূর্তে উত্তর পাওয়া যায়নি।";
    const modelLabel = mode === "FAST" ? "Cloud Run · Gemini দ্রুত" : "Cloud Run · Gemini AI";
    return res.json({ text, modelLabel });
  } catch (err) {
    return handleUpstreamError(res, err);
  }
});

app.post("/api/ai/draft-remark", requireFirebaseAuth, async (req, res) => {
  try {
    const ai = createAiClient();
    const { studentSummary = "", term = 1 } = req.body || {};
    const prompt = `বাংলাদেশের প্রাথমিক বিদ্যালয়ের ${term}ম প্রান্তিক মূল্যায়নের জন্য নিচের শিক্ষার্থীর তথ্যের ভিত্তিতে ২–৩ বাক্যের একটি ইতিবাচক, গঠনমূলক ও প্রাতিষ্ঠানিক বাংলা মন্তব্য (Remark) লিখে দিন। শুধু মন্তব্যটি লিখুন, কোনো শিরোনাম বা অতিরিক্ত ভূমিকা দেবেন না।\nতথ্য: ${studentSummary}`;

    const response = await ai.models.generateContent({
      model: GEMINI_MODEL,
      contents: prompt,
    });
    const remark = (response.text || "").trim() || "প্রান্তিক মূল্যায়নে শিক্ষার্থীর অগ্রগতি সন্তোষজনক; নিয়মিত অনুশীলন চালিয়ে গেলে আরও ভালো করবে।";
    return res.json({ remark });
  } catch (err) {
    return handleUpstreamError(res, err);
  }
});

app.post("/api/ai/analyze-marksheet", requireFirebaseAuth, async (req, res) => {
  try {
    const ai = createAiClient();
    const { imageBase64 = "", mimeType = "image/jpeg", teacherNote = "" } = req.body || {};
    if (!imageBase64) {
      return res.status(400).json({ error: "ছবির ডেটা পাওয়া যায়নি।" });
    }

    const prompt = `আপনি বাংলাদেশের প্রাথমিক বিদ্যালয়ের নম্বরপত্র OCR বিশেষজ্ঞ। ছবি থেকে একজন শিক্ষার্থীর তথ্য ও নম্বর JSON আকারে বের করুন।
অতিরিক্ত নির্দেশনা: ${teacherNote || "নেই"}
বিষয় কোড: BAN (বাংলা), ENG (ইংরেজি), MATH (গণিত), SOC (সামাজিক ও বিজ্ঞান ১-২য়), SCI (বিজ্ঞান ৩-৫ম), BGW (বাওবি), REL (ধর্ম), PE (শারীরিক), ART (শিল্পকলা)।
অংশ কোড: TB (পাঠ্যবই), ACT (সক্রিয়তা), LANG (ভাষা), CT (ক্লাস টেস্ট), WR (সামষ্টিক লিখিত), TOTAL (শিল্পকলা মোট)।
কী ফরম্যাট: "<SUB>_<TERM>_<COMP>", যেমন "MATH_1_TB", "MATH_1_WR"।
যদি কোনো লেখা বুঝতে অসুবিধা হয় বা কাটাকাটি থাকে, uncertainNotes লিস্টে বাংলায় উল্লেখ করুন।
ঠিক এই JSON স্কিমায় উত্তর দিন:
{
  "summary": "সংক্ষিপ্ত বাংলা বিবরণ",
  "term": 1,
  "student": { "name": "", "cls": 3, "roll": 1, "guardian": "", "religion": "ইসলাম" },
  "marks": [ { "key": "MATH_1_TB", "value": 9.0, "subjectLabel": "গণিত (পাঠ্যবই)" } ],
  "attendanceAndTraits": { "days": "", "present": "", "act": "", "disc": "", "cond": "", "remark": "" },
  "uncertainNotes": ["যদি কোনো লেখা অস্পষ্ট থাকে তার বিবরণ"]
}`;

    const response = await ai.models.generateContent({
      model: GEMINI_MODEL,
      contents: {
        parts: [
          { inlineData: { mimeType, data: imageBase64 } },
          { text: prompt },
        ],
      },
      config: {
        responseMimeType: "application/json",
      },
    });

    const parsed = JSON.parse(stripJsonFences(response.text));
    return res.json({ data: parsed });
  } catch (err) {
    return handleUpstreamError(res, err);
  }
});

app.post("/api/ai/analyze-class-sheet", requireFirebaseAuth, async (req, res) => {
  try {
    const ai = createAiClient();
    const {
      imageBase64 = "",
      mimeType = "image/jpeg",
      clsHint = null,
      termHint = null,
      subKeyHint = null,
      subsInfo = "",
      teacherNote = "",
    } = req.body || {};
    if (!imageBase64) {
      return res.status(400).json({ error: "ছবির ডেটা পাওয়া যায়নি।" });
    }

    const prompt = `আপনি বাংলাদেশের প্রাথমিক বিদ্যালয়ের (১ম–৫ম শ্রেণি) হাতে লেখা শ্রেণি-নম্বরপত্র (Marksheet Register) OCR বিশেষজ্ঞ।
ছবিতে একাধিক শিক্ষার্থীর রোল, নাম ও একটি বিষয়ের অংশভিত্তিক নম্বর রয়েছে। সব সারি নিখুঁতভাবে পড়ে শুধু JSON দিন।

ব্যবহারকারীর নির্বাচন/ইঙ্গিত:
- শ্রেণি: ${clsHint || "ছবি থেকে শনাক্ত করুন (1 থেকে 5)"}
- প্রান্তিক: ${termHint || "ছবি থেকে শনাক্ত করুন (1, 2 বা 3)"}
- বিষয় কোড: ${subKeyHint || "ছবি থেকে শনাক্ত করুন (BAN, ENG, MATH, SCI, BGW, SOC, REL, PE, ART)"}
- অতিরিক্ত নোট: ${teacherNote || "নেই"}

বিষয় ও সর্বোচ্চ নম্বরের কাঠামো:
${subsInfo}

গুরুত্বপূর্ণ OCR ও যাচাইকরণ নিয়ম:
1. বাংলা ও ইংরেজি সব সংখ্যা JSON-এ ইংরেজি অঙ্কে লিখুন (যেমন ০৭ -> "7", ২৫ -> "25")। শিক্ষার্থীর নাম বাংলাতেই পরিষ্কারভাবে লিখুন।
2. ক্লাস টেস্ট (CT) ঘরে যদি "০৬+০৭=১৩/২=০৭" ধরনের হিসাব থাকে, তবে "=" চিহ্নের পরের চূড়ান্ত গড় মানটি CT ফিল্ডে দিন এবং rawNotes-এ সংক্ষেপে লিখুন।
3. কোনো ঘরে A বা অনু লেখা থাকলে 0 বসান এবং rawNotes-এ "অনুপস্থিত (A)" লিখুন। কোনো ঘর সম্পূর্ণ ফাঁকা থাকলে খালি স্ট্রিং "" দিন।
4. যদি খাতায় "মোট নম্বর (ধারাবাহিক)" বা "প্রান্তিক সর্বমোট" কলাম থাকে, সেগুলোও sheetContinuousTotal ও sheetGrandTotal ফিল্ডে দিন।
5. কোনো শিক্ষার্থীর নাম, রোল বা কোনো নম্বর অস্পষ্ট, ঝাপসা, কাটা-ছেঁড়া বা বুঝতে অসুবিধা হলে সেরা অনুমানটি ফিল্ডে দিন এবং uncertainFields অবজেক্টে সেই ফিল্ড কী ("roll", "name", "TB", "ACT", "LANG", "CT", "WR", "TOTAL") দিয়ে বাংলায় সংক্ষিপ্ত কারণ লিখুন। একেবারেই পড়া না গেলে মান "" দিন এবং uncertainFields-এ অবশ্যই উল্লেখ করুন।

ঠিক নিচের JSON ফরম্যাটে উত্তর দিন:
{
  "cls": ${clsHint || 3},
  "term": ${termHint || 1},
  "subKey": "${subKeyHint || "MATH"}",
  "summary": "ছবি থেকে শনাক্ত শ্রেণি, বিষয় ও মোট সারির সংক্ষিপ্ত বিবরণ",
  "warnings": [],
  "rows": [
    {
      "roll": "1",
      "name": "শিক্ষার্থীর নাম",
      "marks": { "TB": "9", "ACT": "5", "LANG": "5", "CT": "7", "WR": "50" },
      "sheetContinuousTotal": "26",
      "sheetGrandTotal": "76",
      "rawNotes": "",
      "uncertainFields": {}
    }
  ]
}`;

    const response = await ai.models.generateContent({
      model: GEMINI_MODEL,
      contents: {
        parts: [
          { inlineData: { mimeType, data: imageBase64 } },
          { text: prompt },
        ],
      },
      config: {
        responseMimeType: "application/json",
      },
    });

    const parsed = JSON.parse(stripJsonFences(response.text));
    return res.json({ data: parsed });
  } catch (err) {
    return handleUpstreamError(res, err);
  }
});

app.listen(PORT, () => {
  console.log(`Result Maker AI Proxy listening on port ${PORT} (project=${FIREBASE_PROJECT_ID}, model=${GEMINI_MODEL})`);
});
