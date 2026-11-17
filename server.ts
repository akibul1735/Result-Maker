import "dotenv/config";
import express from "express";
import fs from "fs/promises";
import path from "path";
import { createServer as createViteServer } from "vite";
import { GoogleGenAI, ThinkingLevel, Type } from "@google/genai";

const PORT = 3000;

function getAiClient(): GoogleGenAI {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    throw new Error("GEMINI_API_KEY is not configured in environment secrets.");
  }
  return new GoogleGenAI({
    apiKey,
    httpOptions: {
      headers: {
        "User-Agent": "aistudio-build",
      },
    },
  });
}

function extractApiError(err: unknown): { status: number; message: string } {
  const e = err as { status?: number; message?: string };
  const msg = e?.message || "Unexpected Gemini API error";
  if (msg.includes("GEMINI_API_KEY")) {
    return { status: 400, message: msg };
  }
  if (e?.status && e.status >= 400 && e.status < 600) {
    return { status: e.status, message: msg };
  }
  return { status: 500, message: msg };
}

const INSPECTABLE_FILES: Record<string, string> = {
  "Models.kt": "app/src/main/java/com/kgps/result/Models.kt",
  "Repo.kt": "app/src/main/java/com/kgps/result/Repo.kt",
  "PdfGen.kt": "app/src/main/java/com/kgps/result/PdfGen.kt",
  "MainActivity.kt": "app/src/main/java/com/kgps/result/MainActivity.kt",
  "app/build.gradle.kts": "app/build.gradle.kts",
  "AndroidManifest.xml": "app/src/main/AndroidManifest.xml",
  "android-ci.yml": ".github/workflows/android-ci.yml",
  "android-release-deploy.yml": ".github/workflows/android-release-deploy.yml",
  "web-ci-deploy.yml": ".github/workflows/web-ci-deploy.yml",
};

async function startServer() {
  const app = express();
  app.use(express.json({ limit: "15mb" }));

  app.get("/api/health", (_req, res) => {
    res.json({ ok: true, app: "Result Maker", packageId: "com.kgps.result" });
  });

  app.get("/api/project-files", async (_req, res) => {
    try {
      const entries = await Promise.all(
        Object.entries(INSPECTABLE_FILES).map(async ([label, relPath]) => {
          const full = path.resolve(process.cwd(), relPath);
          const content = await fs.readFile(full, "utf8");
          return { label, path: relPath, content };
        })
      );
      res.json({ files: entries });
    } catch (err) {
      res.status(500).json({ error: (err as Error).message });
    }
  });

  app.post("/api/gemini/chat", async (req, res) => {
    try {
      const { messages, modelMode, studentContext } = req.body as {
        messages: Array<{ role: "user" | "model"; text: string }>;
        modelMode?: "general" | "fast";
        studentContext?: string;
      };

      if (!Array.isArray(messages) || messages.length === 0) {
        res.status(400).json({ error: "messages array is required." });
        return;
      }

      const ai = getAiClient();
      const model = modelMode === "fast" ? "gemini-3.1-flash-lite" : "gemini-4-hydrogen";

      const systemInstruction = [
        "You are the Result Maker (শিখন অগ্রগতি প্রতিবেদন) Assistant for Bangladeshi primary school teachers (Class 1 to Class 5, NCTB curriculum).",
        "Always be accurate about the Result Maker grading rules:",
        "- Class 1-2: Full subjects (BAN, ENG, MATH = 100 marks: TB 20 + ACT 5 + LANG 5 + CT 20 = 50 continuous, WR 50 summative); Half subjects (SOC, REL, PE = 50 marks: TB 20 + ACT 5 + LANG 5 + CT 20 = 50 continuous, 0 written); ART = 50 marks (TOTAL 50).",
        "- Class 3-5: Full subjects (BAN, ENG, MATH, SCI, BGW = 100 marks: TB 10 + ACT 5 + LANG 5 + CT 10 = 30 continuous, WR 70 summative); REL = 100 marks (TB 10 + ACT 5 + LANG 5 + CT 10 + WR 70); PE = 50 marks (TB 20 + ACT 5 + LANG 5 + CT 20); ART = 50 marks.",
        "- Grades: 80-100% = A (অতি উত্তম), 60-79% = B (উত্তম), 40-59% = C (সন্তোষজনক), 0-39% = D (সহায়তা প্রয়োজন).",
        "- Traits (সক্রিয়তা, শৃঙ্খলা, আচরণ): ক = উত্তম, খ = সন্তোষজনক, গ = উন্নতি প্রয়োজন.",
        "Respond in the user's language (Bengali or English). Keep answers practical, concise, and ready for teachers to copy into report cards.",
        studentContext ? `\nCurrent Workspace Context:\n${studentContext}` : "",
      ]
        .filter(Boolean)
        .join("\n");

      const prior = messages.slice(0, -1).map((m) => ({
        role: m.role,
        parts: [{ text: m.text }],
      }));
      const latest = messages[messages.length - 1];

      const chat = ai.chats.create({
        model,
        history: prior,
        config: {
          systemInstruction,
          ...(model === "gemini-4-hydrogen"
            ? { thinkingConfig: { thinkingLevel: ThinkingLevel.MEDIUM } }
            : {}),
        },
      });

      const response = await chat.sendMessage({ message: latest.text });
      res.json({
        reply: response.text || "",
        model,
      });
    } catch (err) {
      const { status, message } = extractApiError(err);
      res.status(status).json({ error: message });
    }
  });

  app.post("/api/gemini/analyze-image", async (req, res) => {
    try {
      const { imageBase64, mimeType, prompt } = req.body as {
        imageBase64: string;
        mimeType: string;
        prompt?: string;
      };

      if (!imageBase64 || !mimeType) {
        res.status(400).json({ error: "imageBase64 and mimeType are required." });
        return;
      }

      const ai = getAiClient();

      const instruction = `Analyze this uploaded photo of a student marksheet, attendance ledger, or progress report card.
Extract any visible student profile fields, term number (1, 2, or 3), subject marks using exact Result Maker keys (<SUB>_<TERM>_<COMP>, where SUB is BAN, ENG, MATH, SOC, SCI, BGW, REL, PE, or ART; COMP is TB, ACT, LANG, CT, WR, or TOTAL for ART), and attendance/trait info.
Respond with valid JSON matching the schema. If a field is not clearly visible in the image, leave it null or omit it from marks.`;

      const response = await ai.models.generateContent({
        model: "gemini-4-hydrogen",
        contents: {
          parts: [
            {
              inlineData: {
                mimeType,
                data: imageBase64,
              },
            },
            {
              text: prompt?.trim()
                ? `${instruction}\n\nTeacher's additional note: ${prompt.trim()}`
                : instruction,
            },
          ],
        },
        config: {
          responseMimeType: "application/json",
          responseSchema: {
            type: Type.OBJECT,
            properties: {
              summary: {
                type: Type.STRING,
                description: "Clear Bengali/English summary of what was detected in the image.",
              },
              student: {
                type: Type.OBJECT,
                properties: {
                  name: { type: Type.STRING },
                  cls: { type: Type.INTEGER },
                  roll: { type: Type.INTEGER },
                  guardian: { type: Type.STRING },
                  village: { type: Type.STRING },
                  upazila: { type: Type.STRING },
                  district: { type: Type.STRING },
                  mobile: { type: Type.STRING },
                  religion: { type: Type.STRING },
                },
              },
              term: {
                type: Type.INTEGER,
                description: "Detected term number 1, 2, or 3.",
              },
              marks: {
                type: Type.ARRAY,
                items: {
                  type: Type.OBJECT,
                  properties: {
                    key: { type: Type.STRING, description: "Mark key like BAN_1_TB or MATH_2_WR" },
                    value: { type: Type.NUMBER },
                    subjectLabel: { type: Type.STRING },
                  },
                },
              },
              attendanceAndTraits: {
                type: Type.OBJECT,
                properties: {
                  days: { type: Type.STRING },
                  present: { type: Type.STRING },
                  act: { type: Type.STRING },
                  disc: { type: Type.STRING },
                  cond: { type: Type.STRING },
                  remark: { type: Type.STRING },
                },
              },
            },
          },
        },
      });

      const raw = (response.text ?? "{}").trim();
      const parsed = JSON.parse(raw);
      res.json({ analysis: parsed, model: "gemini-4-hydrogen" });
    } catch (err) {
      const { status, message } = extractApiError(err);
      res.status(status).json({ error: message });
    }
  });

  app.post("/api/gemini/draft-remark", async (req, res) => {
    try {
      const { studentSummary, term } = req.body as {
        studentSummary: string;
        term: number;
      };
      if (!studentSummary) {
        res.status(400).json({ error: "studentSummary is required." });
        return;
      }
      const ai = getAiClient();
      const response = await ai.models.generateContent({
        model: "gemini-3.1-flash-lite",
        contents: `Write a concise, encouraging, natural 2-sentence Bengali teacher remark (শ্রেণি শিক্ষকের সার্বিক মতামত) for Term ${term} based on this student's data. Return only the Bengali remark text without quotes.\n\n${studentSummary}`,
      });
      res.json({ remark: (response.text || "").trim() });
    } catch (err) {
      const { status, message } = extractApiError(err);
      res.status(status).json({ error: message });
    }
  });

  app.post("/api/gemini/tts", async (req, res) => {
    try {
      const { text, voiceName, style } = req.body as {
        text: string;
        voiceName?: string;
        style?: string;
      };

      if (!text || !text.trim()) {
        res.status(400).json({ error: "text is required for TTS." });
        return;
      }

      const ai = getAiClient();
      const response = await ai.models.generateContent({
        model: "gemini-3.8-flash-tts",
        contents: [
          {
            role: "user",
            parts: [
              {
                text: text.trim().slice(0, 2000),
                speechMetadata: {
                  style:
                    style ||
                    "Warm, articulate primary school teacher reading a progress report clearly",
                },
              },
            ],
          },
        ],
        config: {
          responseModalities: ["AUDIO"],
          speechConfig: {
            voiceConfig: {
              prebuiltVoiceConfig: {
                voiceName: voiceName || "Kore",
              },
            },
          },
        },
      });

      const base64Audio =
        response.candidates?.[0]?.content?.parts?.[0]?.inlineData?.data;

      if (!base64Audio) {
        res.status(502).json({ error: "No audio data returned by TTS model." });
        return;
      }

      res.json({
        audioBase64: base64Audio,
        mimeType: "audio/wav",
        model: "gemini-3.8-flash-tts",
      });
    } catch (err) {
      const { status, message } = extractApiError(err);
      res.status(status).json({ error: message });
    }
  });

  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.resolve(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (_req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Result Maker server running on http://0.0.0.0:${PORT}`);
  });
}

startServer();
