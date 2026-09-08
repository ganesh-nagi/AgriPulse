/**
 * Voice extension point. No voice AI ships in the MVP; a future
 * implementation (speech synthesis / voice input) plugs in here without
 * touching pages: call setVoiceReader() once at startup.
 */
export interface VoiceReader {
  speak(text: string): void
  stop(): void
}

class NullVoiceReader implements VoiceReader {
  speak(): void {}
  stop(): void {}
}

let reader: VoiceReader = new NullVoiceReader()

export function setVoiceReader(next: VoiceReader): void {
  reader = next
}

export function getVoiceReader(): VoiceReader {
  return reader
}
