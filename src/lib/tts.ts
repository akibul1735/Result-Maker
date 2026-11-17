export async function requestTtsAudio(
  text: string,
  voiceName = "Kore",
  style?: string
): Promise<string> {
  const res = await fetch("/api/gemini/tts", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ text, voiceName, style }),
  });
  const data = (await res.json()) as {
    audioBase64?: string;
    mimeType?: string;
    error?: string;
  };
  if (!res.ok || !data.audioBase64) {
    throw new Error(data.error || "Failed to generate TTS audio");
  }
  return `data:${data.mimeType || "audio/wav"};base64,${data.audioBase64}`;
}
