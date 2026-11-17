import React, { useRef, useState } from "react";
import {
  CheckCircle2,
  ImageUp,
  Loader2,
  Send,
  Sparkles,
  Volume2,
} from "lucide-react";
import {
  CLSN,
  Student,
  bn,
  createEmptyStudent,
  fmt,
  gradeOf,
  overallPct,
} from "../lib/models";

interface ChatMessage {
  id: string;
  role: "user" | "model";
  text: string;
  model?: string;
}

interface ExtractedAnalysis {
  summary?: string;
  student?: {
    name?: string;
    cls?: number;
    roll?: number;
    guardian?: string;
    village?: string;
    upazila?: string;
    district?: string;
    mobile?: string;
    religion?: string;
  };
  term?: number;
  marks?: Array<{
    key: string;
    value: number;
    subjectLabel?: string;
  }>;
  attendanceAndTraits?: {
    days?: string;
    present?: string;
    act?: string;
    disc?: string;
    cond?: string;
    remark?: string;
  };
}

interface AiAssistantPanelProps {
  students: Student[];
  onApplyExtractedStudent: (student: Student) => void;
  onSpeakText: (text: string, id: string) => Promise<void>;
  speakingId: string | null;
}

export const AiAssistantPanel: React.FC<AiAssistantPanelProps> = ({
  students,
  onApplyExtractedStudent,
  onSpeakText,
  speakingId,
}) => {
  // Chatbot state
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: "welcome",
      role: "model",
      text: "আসসালামু আলাইকুম। আমি Result Maker সহকারী। ১ম–৫ম শ্রেণির ধারাবাহিক ও সামষ্টিক মূল্যায়ন, গ্রেড হিসাব, শ্রেণি শিক্ষকের মতামত লেখা কিংবা CSV/PDF সংক্রান্ত যেকোনো প্রশ্ন করতে পারেন।",
    },
  ]);
  const [input, setInput] = useState("");
  const [modelMode, setModelMode] = useState<"general" | "fast">("general");
  const [contextStudentId, setContextStudentId] = useState<string>("");
  const [chatLoading, setChatLoading] = useState(false);
  const [chatError, setChatError] = useState<string | null>(null);
  const threadEndRef = useRef<HTMLDivElement | null>(null);

  // Image analysis state
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [imageBase64, setImageBase64] = useState<string | null>(null);
  const [imageMime, setImageMime] = useState<string>("image/jpeg");
  const [imagePrompt, setImagePrompt] = useState("");
  const [analyzing, setAnalyzing] = useState(false);
  const [analysisError, setAnalysisError] = useState<string | null>(null);
  const [analysis, setAnalysis] = useState<ExtractedAnalysis | null>(null);
  const [appliedBanner, setAppliedBanner] = useState<string | null>(null);

  const selectedContextStudent = students.find(
    (s) => s.id === contextStudentId
  );

  const sendMessage = async (customText?: string) => {
    const text = (customText ?? input).trim();
    if (!text || chatLoading) return;

    const userMsg: ChatMessage = {
      id: `u-${Date.now()}`,
      role: "user",
      text,
    };
    const nextHistory = [...messages, userMsg];
    setMessages(nextHistory);
    if (!customText) setInput("");
    setChatError(null);
    setChatLoading(true);

    try {
      const studentContext = selectedContextStudent
        ? `Student: ${selectedContextStudent.name}, Class: ${CLSN[selectedContextStudent.cls - 1]}, Roll: ${selectedContextStudent.roll}, Religion: ${selectedContextStudent.religion}, Overall: ${fmt(overallPct(selectedContextStudent))}% (${gradeOf(overallPct(selectedContextStudent))}), Marks JSON: ${JSON.stringify(selectedContextStudent.m)}, Extras JSON: ${JSON.stringify(selectedContextStudent.x)}`
        : `Total students in roster: ${students.length}`;

      const res = await fetch("/api/gemini/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          messages: nextHistory
            .filter((m) => m.id !== "welcome")
            .map((m) => ({ role: m.role, text: m.text })),
          modelMode,
          studentContext,
        }),
      });

      const data = (await res.json()) as {
        reply?: string;
        model?: string;
        error?: string;
      };
      if (!res.ok || !data.reply) {
        throw new Error(data.error || "Gemini response failed");
      }

      setMessages((prev) => [
        ...prev,
        {
          id: `m-${Date.now()}`,
          role: "model",
          text: data.reply!,
          model: data.model,
        },
      ]);
      setTimeout(() => {
        threadEndRef.current?.scrollIntoView({ behavior: "smooth" });
      }, 50);
    } catch (err) {
      setChatError((err as Error).message);
    } finally {
      setChatLoading(false);
    }
  };

  const handleImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setAnalysis(null);
    setAnalysisError(null);
    setAppliedBanner(null);
    setImageMime(file.type || "image/jpeg");

    const reader = new FileReader();
    reader.onload = () => {
      const result = String(reader.result || "");
      setImagePreview(result);
      const commaIdx = result.indexOf(",");
      setImageBase64(commaIdx >= 0 ? result.slice(commaIdx + 1) : result);
    };
    reader.readAsDataURL(file);
  };

  const runImageAnalysis = async () => {
    if (!imageBase64 || analyzing) return;
    setAnalyzing(true);
    setAnalysisError(null);
    setAppliedBanner(null);

    try {
      const res = await fetch("/api/gemini/analyze-image", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          imageBase64,
          mimeType: imageMime,
          prompt: imagePrompt,
        }),
      });
      const data = (await res.json()) as {
        analysis?: ExtractedAnalysis;
        error?: string;
      };
      if (!res.ok || !data.analysis) {
        throw new Error(data.error || "Image analysis failed");
      }
      setAnalysis(data.analysis);
    } catch (err) {
      setAnalysisError((err as Error).message);
    } finally {
      setAnalyzing(false);
    }
  };

  const applyExtractedToRoster = () => {
    if (!analysis) return;
    const ext = analysis.student || {};
    const cls = ext.cls && ext.cls >= 1 && ext.cls <= 5 ? ext.cls : 1;
    const roll =
      ext.roll && ext.roll > 0
        ? ext.roll
        : (students
            .filter((s) => s.cls === cls)
            .reduce((max, s) => Math.max(max, s.roll), 0) || 0) + 1;

    const existing = students.find((s) => s.cls === cls && s.roll === roll);
    const base = existing || createEmptyStudent(cls, roll);

    const newMarks: Record<string, number> = { ...base.m };
    for (const item of analysis.marks || []) {
      if (item.key && typeof item.value === "number" && !Number.isNaN(item.value)) {
        newMarks[item.key] = item.value;
      }
    }

    const t = analysis.term && [1, 2, 3].includes(analysis.term) ? analysis.term : 1;
    const newExtras: Record<string, string> = { ...base.x };
    const at = analysis.attendanceAndTraits;
    if (at?.days) newExtras[`DAYS_${t}`] = at.days;
    if (at?.present) newExtras[`PRES_${t}`] = at.present;
    if (at?.act) newExtras[`ACT_${t}`] = at.act;
    if (at?.disc) newExtras[`DISC_${t}`] = at.disc;
    if (at?.cond) newExtras[`COND_${t}`] = at.cond;
    if (at?.remark) newExtras[`REM_${t}`] = at.remark;

    const updated: Student = {
      ...base,
      name: ext.name || base.name || `শিক্ষার্থী ${bn(String(roll))}`,
      guardian: ext.guardian || base.guardian,
      village: ext.village || base.village,
      upazila: ext.upazila || base.upazila,
      district: ext.district || base.district,
      mobile: ext.mobile || base.mobile,
      religion: ext.religion || base.religion,
      m: newMarks,
      x: newExtras,
    };

    onApplyExtractedStudent(updated);
    setAppliedBanner(
      `${CLSN[updated.cls - 1]} শ্রেণি · রোল ${bn(String(updated.roll))} (${updated.name}) তালিকায় সংরক্ষিত হয়েছে।`
    );
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
      {/* Left: Multi-turn Gemini Chatbot */}
      <div className="lg:col-span-7 border border-slate-200 bg-white rounded-xl flex flex-col h-[660px]">
        <div className="p-4 border-b border-slate-200 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-base font-semibold text-slate-900">
              শিক্ষক সহায়ক চ্যাটবট
            </h2>
            <p className="text-xs text-slate-500">
              মতামত লেখা, গ্রেড যাচাই ও মূল্যায়ন পরামর্শ
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <select
              value={contextStudentId}
              onChange={(e) => setContextStudentId(e.target.value)}
              className="px-2.5 py-1.5 text-xs rounded-lg border border-slate-200 bg-white text-slate-700 max-w-[180px]"
            >
              <option value="">সব শিক্ষার্থী (সাধারণ)</option>
              {students.map((s) => (
                <option key={s.id} value={s.id}>
                  {CLSN[s.cls - 1]} · রোল {bn(String(s.roll))} ({s.name})
                </option>
              ))}
            </select>

            <div className="inline-flex rounded-lg border border-slate-200 bg-slate-100 p-0.5">
              <button
                onClick={() => setModelMode("general")}
                className={`px-2.5 py-1 text-xs font-medium rounded-md whitespace-nowrap ${
                  modelMode === "general"
                    ? "bg-white text-slate-900 shadow-xs"
                    : "text-slate-600"
                }`}
              >
                বিস্তারিত
              </button>
              <button
                onClick={() => setModelMode("fast")}
                className={`px-2.5 py-1 text-xs font-medium rounded-md whitespace-nowrap ${
                  modelMode === "fast"
                    ? "bg-white text-slate-900 shadow-xs"
                    : "text-slate-600"
                }`}
              >
                দ্রুত
              </button>
            </div>
          </div>
        </div>

        {/* Scrollable Message Thread */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {messages.map((m) => (
            <div
              key={m.id}
              className={`flex flex-col ${
                m.role === "user" ? "items-end" : "items-start"
              }`}
            >
              <div
                className={`max-w-[85%] rounded-xl px-4 py-3 text-sm leading-relaxed whitespace-pre-wrap ${
                  m.role === "user"
                    ? "bg-teal-700 text-white"
                    : "bg-slate-50 border border-slate-200 text-slate-800"
                }`}
              >
                {m.text}
              </div>
              {m.role === "model" && (
                <div className="mt-1 flex items-center gap-3 text-xs text-slate-400 px-1">
                  <button
                    onClick={() => onSpeakText(m.text, m.id)}
                    disabled={speakingId === m.id}
                    className="inline-flex items-center gap-1 hover:text-teal-700 transition-colors"
                  >
                    {speakingId === m.id ? (
                      <Loader2 className="w-3 h-3 animate-spin" />
                    ) : (
                      <Volume2 className="w-3 h-3" />
                    )}
                    <span>শুনুন</span>
                  </button>
                  {m.model && <span>· {m.model}</span>}
                </div>
              )}
            </div>
          ))}
          {chatLoading && (
            <div className="flex items-center gap-2 text-xs text-slate-500">
              <Loader2 className="w-4 h-4 animate-spin text-teal-700" />
              <span>উত্তর প্রস্তুত হচ্ছে...</span>
            </div>
          )}
          <div ref={threadEndRef} />
        </div>

        {/* Quick Prompt Bar & Input */}
        <div className="p-3 border-t border-slate-200 space-y-2.5 bg-slate-50/50">
          <div className="flex flex-wrap gap-1.5">
            {[
              "নির্বাচিত শিক্ষার্থীর ৩য় প্রান্তিকের মতামত লিখে দিন",
              "১ম-২য় ও ৩য়-৫ম শ্রেণির নম্বর বিভাজন বুঝিয়ে দিন",
              "অভিভাবক সভার জন্য সংক্ষিপ্ত সারসংক্ষেপ তৈরি করুন",
            ].map((promptText) => (
              <button
                key={promptText}
                onClick={() => sendMessage(promptText)}
                disabled={chatLoading}
                className="px-2.5 py-1 text-xs rounded-md border border-slate-200 bg-white text-slate-700 hover:border-teal-600 hover:text-teal-700 transition-colors"
              >
                {promptText}
              </button>
            ))}
          </div>

          {chatError && (
            <div className="text-xs text-rose-700 bg-rose-50 border border-rose-200 rounded-lg px-3 py-2">
              {chatError}
            </div>
          )}

          <form
            onSubmit={(e) => {
              e.preventDefault();
              sendMessage();
            }}
            className="flex items-center gap-2"
          >
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="প্রশ্ন লিখুন (বাংলা বা ইংরেজিতে)..."
              className="flex-1 px-3.5 py-2.5 text-sm rounded-lg border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-teal-700/20 focus:border-teal-700"
            />
            <button
              type="submit"
              disabled={!input.trim() || chatLoading}
              className="inline-flex items-center gap-1.5 px-4 py-2.5 text-sm font-medium rounded-lg bg-teal-700 text-white hover:bg-teal-800 disabled:opacity-50 transition-colors whitespace-nowrap shrink-0"
            >
              <Send className="w-4 h-4" />
              <span>পাঠান</span>
            </button>
          </form>
        </div>
      </div>

      {/* Right: Image Understanding (Marksheet / Register Scanner) */}
      <div className="lg:col-span-5 border border-slate-200 bg-white rounded-xl p-5 space-y-4">
        <div>
          <h2 className="text-base font-semibold text-slate-900">
            নম্বরপত্র ও খাতা স্ক্যানার (ছবি বিশ্লেষণ)
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            হাতে লেখা নম্বরপত্র বা রেজিস্টারের ছবি আপলোড করলে Gemini তথ্য ও নম্বর শনাক্ত করবে।
          </p>
        </div>

        <label className="block border border-dashed border-slate-300 rounded-xl p-5 text-center cursor-pointer hover:border-teal-600 hover:bg-slate-50/70 transition-colors">
          <input
            type="file"
            accept="image/*"
            onChange={handleImageSelect}
            className="hidden"
          />
          {imagePreview ? (
            <img
              src={imagePreview}
              alt="Uploaded marksheet preview"
              referrerPolicy="no-referrer"
              className="max-h-52 mx-auto rounded-lg object-contain border border-slate-200"
            />
          ) : (
            <div className="space-y-2 py-4">
              <ImageUp className="w-7 h-7 mx-auto text-slate-400" />
              <div className="text-sm font-medium text-slate-700">
                নম্বরপত্র বা প্রতিবেদন ছবি নির্বাচন করুন
              </div>
              <div className="text-xs text-slate-400">PNG, JPG বা WEBP</div>
            </div>
          )}
        </label>

        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">
            অতিরিক্ত নির্দেশনা (ঐচ্ছিক)
          </label>
          <input
            type="text"
            value={imagePrompt}
            onChange={(e) => setImagePrompt(e.target.value)}
            placeholder="যেমন: এটি ৩য় শ্রেণির ২য় প্রান্তিকের নম্বরপত্র"
            className="w-full px-3 py-2 text-sm rounded-lg border border-slate-200 focus:outline-none focus:border-teal-700"
          />
        </div>

        <button
          onClick={runImageAnalysis}
          disabled={!imageBase64 || analyzing}
          className="w-full inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm font-medium rounded-lg bg-teal-700 text-white hover:bg-teal-800 disabled:opacity-50 transition-colors"
        >
          {analyzing ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            <Sparkles className="w-4 h-4" />
          )}
          <span>{analyzing ? "বিশ্লেষণ চলছে..." : "ছবি বিশ্লেষণ করুন"}</span>
        </button>

        {analysisError && (
          <div className="p-3 rounded-lg border border-rose-200 bg-rose-50 text-xs text-rose-800">
            {analysisError}
          </div>
        )}

        {appliedBanner && (
          <div className="p-3 rounded-lg border border-emerald-200 bg-emerald-50 text-xs text-emerald-800 flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 shrink-0" />
            <span>{appliedBanner}</span>
          </div>
        )}

        {analysis && (
          <div className="border-t border-slate-200 pt-4 space-y-3">
            {analysis.summary && (
              <div className="text-sm text-slate-700 bg-slate-50 border border-slate-200 rounded-lg p-3">
                {analysis.summary}
              </div>
            )}

            {analysis.student && (
              <div className="text-xs text-slate-600 space-y-1">
                <div className="font-semibold text-slate-900">
                  শনাক্তকৃত তথ্য
                </div>
                <div>
                  নাম: {analysis.student.name || "—"} · শ্রেণি:{" "}
                  {analysis.student.cls ? bn(String(analysis.student.cls)) : "—"} ·
                  রোল:{" "}
                  {analysis.student.roll
                    ? bn(String(analysis.student.roll))
                    : "—"}
                </div>
              </div>
            )}

            {analysis.marks && analysis.marks.length > 0 && (
              <div className="max-h-44 overflow-y-auto border border-slate-200 rounded-lg">
                <table className="w-full text-xs text-left">
                  <thead className="bg-slate-50 border-b border-slate-200 text-slate-500">
                    <tr>
                      <th className="py-2 px-3">কী / বিষয়</th>
                      <th className="py-2 px-3 text-right">নম্বর</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200">
                    {analysis.marks.map((mkItem, idx) => (
                      <tr key={`${mkItem.key}-${idx}`}>
                        <td className="py-1.5 px-3 text-slate-700">
                          {mkItem.subjectLabel || mkItem.key}{" "}
                          <span className="font-mono text-slate-400">
                            ({mkItem.key})
                          </span>
                        </td>
                        <td className="py-1.5 px-3 text-right font-mono font-semibold tabular-nums text-slate-900">
                          {bn(String(mkItem.value))}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <button
              onClick={applyExtractedToRoster}
              className="w-full px-4 py-2 text-sm font-medium rounded-lg border border-teal-700 text-teal-700 hover:bg-teal-50 transition-colors"
            >
              তালিকায় যুক্ত / হালনাগাদ করুন
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
