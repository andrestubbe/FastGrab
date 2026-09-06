import javax.sound.sampled.*;

public class ChiptuneSynth {

    public static void main(String[] args) throws Exception {
        System.out.println("🎵 Playing 4-channel retro Chiptune (Square Lead, Pulse Arpeggio, Triangle Bass, White Noise)...");
        playChiptune();
    }

    public static void playChiptune() {
        int sampleRate = 44100;
        AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);

        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format, 4096);
            line.start();

            // Chiptune composition: 140 BPM, 16th-note ticks (~107 ms per 16th)
            int tickMs = 107;
            int tickSamples = sampleRate * tickMs / 1000;
            byte[] buffer = new byte[tickSamples];

            // Classic Game Boy / NES style chord progressions (frequencies in Hz)
            // Em -> C -> G -> D
            double[][] arpeggioChords = {
                { 329.63, 392.00, 493.88, 659.25 }, // E minor (E4, G4, B4, E5)
                { 261.63, 329.63, 392.00, 523.25 }, // C Major (C4, E4, G4, C5)
                { 196.00, 246.94, 293.66, 392.00 }, // G Major (G3, B3, D4, G4)
                { 293.66, 369.99, 440.00, 587.33 }  // D Major (D4, F#4, A4, D5)
            };

            double[] bassNotes = {
                82.41,  // E2
                65.41,  // C2
                98.00,  // G2
                73.42   // D2
            };

            double[] leadMelody = {
                659.25, 783.99, 987.77, 880.00, // E5, G5, B5, A5
                523.25, 659.25, 783.99, 659.25, // C5, E5, G5, E5
                783.99, 659.25, 587.33, 493.88, // G5, E5, D5, B4
                587.33, 739.99, 880.00, 987.77  // D5, F#5, A5, B5
            };

            long sampleIndex = 0;
            java.util.Random rnd = new java.util.Random(1337);

            int totalTicks = 32; // 2 bars of 16th notes
            for (int tick = 0; tick < totalTicks; tick++) {
                int chordIdx = (tick / 8) % arpeggioChords.length;
                double[] chord = arpeggioChords[chordIdx];
                double bassFreq = bassNotes[chordIdx];
                double leadFreq = leadMelody[tick % leadMelody.length];
                boolean drumHit = (tick % 4 == 0) || (tick % 8 == 6); // Kick/Snare pattern

                for (int s = 0; s < tickSamples; s++, sampleIndex++) {
                    double t = (double) sampleIndex / sampleRate;

                    // Channel 1: High-Speed Arpeggio (Pulse Wave, 25% duty cycle)
                    int arpSubIndex = (int) ((sampleIndex / (sampleRate * 0.025)) % chord.length);
                    double arpFreq = chord[arpSubIndex];
                    double pulsePhase = (t * arpFreq) % 1.0;
                    double pulseSample = (pulsePhase < 0.25) ? 0.35 : -0.35;

                    // Channel 2: Lead Melody (Classic 50% Square wave)
                    double squarePhase = (t * leadFreq) % 1.0;
                    double squareSample = (squarePhase < 0.5) ? 0.40 : -0.40;

                    // Channel 3: Bassline (Triangle Wave, NES style)
                    double triPhase = (t * bassFreq) % 1.0;
                    double triSample = (triPhase < 0.5) ? (4.0 * triPhase - 1.0) : (3.0 - 4.0 * triPhase);
                    triSample *= 0.50;

                    // Channel 4: Noise percussion (White noise with exponential decay)
                    double noiseSample = 0;
                    if (drumHit && s < (tickSamples * 0.45)) {
                        double decay = 1.0 - ((double) s / (tickSamples * 0.45));
                        noiseSample = (rnd.nextDouble() * 2.0 - 1.0) * decay * 0.40;
                    }

                    // Master Polyphonic Mix (Soft clipping limit)
                    double mixed = (pulseSample + squareSample + triSample + noiseSample) * 0.75;
                    mixed = Math.max(-1.0, Math.min(1.0, mixed));

                    buffer[s] = (byte) (mixed * 120);
                }

                line.write(buffer, 0, buffer.length);
            }

            line.drain();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
