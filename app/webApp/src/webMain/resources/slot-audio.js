// Buffers are prepared once, on the first user gesture (browser autoplay policy).
globalThis.gameReelSlotAudio = (() => {
    let context;
    let buffers;
    function unlock() {
        try {
            if (!context) {
                const Audio = globalThis.AudioContext || globalThis.webkitAudioContext;
                if (!Audio) return;
                context = new Audio({ latencyHint: "interactive" });
                buffers = [[0.022, 1900], [0.055, 740]].map(([duration, frequency]) => {
                    const buffer = context.createBuffer(1, Math.floor(context.sampleRate * duration), context.sampleRate);
                    const samples = buffer.getChannelData(0);
                    for (let i = 0; i < samples.length; i++) {
                        const t = i / context.sampleRate;
                        const envelope = Math.min(t / 0.0008, 1) * Math.exp(-t / (duration / 6));
                        samples[i] = 0.3 * envelope * (0.65 * Math.sin(2 * Math.PI * frequency * t) + 0.35 * (Math.random() * 2 - 1));
                    }
                    return buffer;
                });
            }
            if (context.state === "suspended") context.resume().catch(() => {});
        } catch (_) { /* Audio is optional when the browser has no output device. */ }
    }
    return {
        init() {
            document.addEventListener("pointerdown", unlock, true);
            document.addEventListener("keydown", unlock, true);
        },
        play(stopped) {
            if (!context || context.state !== "running" || !buffers || document.hidden) return;
            const source = context.createBufferSource();
            source.buffer = buffers[stopped ? 1 : 0];
            source.connect(context.destination);
            source.onended = () => source.disconnect();
            source.start();
        },
        close() {
            document.removeEventListener("pointerdown", unlock, true);
            document.removeEventListener("keydown", unlock, true);
            if (context) context.close().catch(() => {});
            context = undefined;
            buffers = undefined;
        }
    };
})();
