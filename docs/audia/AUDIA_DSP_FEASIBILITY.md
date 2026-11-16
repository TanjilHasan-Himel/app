# Audia DSP feasibility

Date: 2026-11-13

## Decision

Do not embed JamesDSP in the closed-source Audia APK at this stage. Keep the current Android audio-effect path as the compatibility baseline and develop an Audia-owned PCM biquad chain behind an explicit experimental flag only after renderer integration, device profiling and listening/measurement gates are ready.

An isolated, dependency-free peaking-biquad foundation now exists in `app/src/main/java/com/eplaytime/app/dsp/BiquadFilter.kt`. It is intentionally **not connected to playback**, does not alter presets, and can be removed without touching the working engine.

## Current Audia architecture

- Media3 ExoPlayer `1.3.1` is created with the default renderer/audio-sink path.
- Audia generates an audio-session ID and attaches Android `Equalizer`, `BassBoost` and `Virtualizer` through `AudioEffectManager`.
- Band count, center frequencies and level range come from the device implementation; the UI must not assume five or ten physical bands.
- There is no custom `AudioProcessor`, native DSP library, JNI layer, convolver or AutoEq dataset in the APK.
- Effect availability and behavior are OEM/device dependent.

Android documents that `Equalizer` is attached to a specific audio-session ID and exposes the implementation-supported band count. Audia's session-scoped approach matches that model: [Android Equalizer API](https://developer.android.com/reference/android/media/audiofx/Equalizer).

## Candidate comparison

| Candidate | Strength | Cost/risk | Decision |
|---|---|---|---|
| Existing Android AudioEffect | Small, already integrated, OEM accelerated where available | Band layout and behavior vary; not a deterministic Audia DSP | Keep as compatibility baseline |
| Audia Media3 PCM biquad chain | Audia controls frequency, gain, Q, preamp and coefficient safety | Requires custom renderer/sink wiring, PCM-only path, measurements and offload policy | Recommended staged implementation |
| JamesDSP / RootlessJamesDSP | Convolver and advanced effects already exist | GPL obligations, JNI/porting work, latency and maintenance; rootless app uses capture-based architecture not needed inside Audia | Do not integrate without approved licensing strategy |
| AutoEq profiles | Valuable headphone correction parameters | AutoEq code is MIT, but measurement/profile sources and redistribution attribution must be audited separately | Later adapter after data-rights audit |

Media3 supports custom audio processors in `DefaultAudioSink`, but explicitly limits processing chains to PCM playback—not passthrough or offload. Float output also disables some audio processing. These constraints must be made visible in the eventual feature behavior: [Media3 DefaultAudioSink.Builder](https://developer.android.com/reference/androidx/media3/exoplayer/audio/DefaultAudioSink.Builder).

The original JamesDSP project is designed around Android's system-effect framework rather than being a drop-in Media3 processor. RootlessJamesDSP is GPL-3.0 and documents capture limitations plus increased latency: [JamesDSPManager](https://github.com/james34602/JamesDSPManager), [RootlessJamesDSP](https://github.com/timschneeb/RootlessJamesDSP). A copied or linked GPL engine must not enter a closed-source distributed APK without legal approval and a compliant source-distribution plan.

AutoEq's software license is MIT: [AutoEq license](https://github.com/jaakkopasanen/AutoEq/blob/master/LICENSE). That does not automatically grant blanket redistribution rights for every third-party measurement source represented in generated profiles; profile provenance still needs a separate inventory.

## Isolated prototype

The prototype implements:

- normalized RBJ peaking-EQ coefficients;
- configurable frequency, gain and Q with bounded validation;
- mono or interleaved multi-channel float PCM;
- bit-identical disabled bypass;
- per-frame coefficient ramping to reduce parameter-change discontinuities;
- output clamping as a prototype safety net;
- state reset without changing saved user settings.

Executed JVM checks:

| Check | Result |
|---|---|
| Disabled bypass | Bit-identical pass |
| Flat coefficients | Identity within `1e-6` pass |
| 1 kHz, +6 dB peaking response at 48 kHz | Approximately expected `1.995x` amplitude ratio pass |
| Finite output | Pass |

This proves coefficient math only. It does **not** prove audible quality, realtime deadline safety, device routing compatibility or battery performance.

## Proposed production pipeline

`Decoder → PCM format adapter → preamp/headroom → ordered biquad bands → optional limiter → Media3 audio sink → Android output`

Rules:

1. Start with 32-bit internal float math but make no claim about 32-bit device output.
2. Disable Media3 audio offload while Audia PCM processing is active; restore the normal path when bypassed.
3. Keep radio/live streams on the existing path until buffer and reconnect regressions are measured.
4. Smooth gain/coefficient changes and reset filter state on format/sample-rate changes.
5. Add preamp/automatic headroom before allowing positive multi-band boosts.
6. Never silently combine Audia DSP and OEM effects; provide an explicit conflict/bypass policy.

## Required validation before playback integration

- Frequency-response sweeps at 44.1/48/96 kHz for every filter type.
- THD+N and clipping comparison against bypass at matched output level.
- Impulse/step stability and rapid parameter-update tests.
- Gapless, seek, speed, Bluetooth, wired, speaker, USB DAC and audio-focus regression.
- CPU, underrun, latency and battery measurements on at least a low/mid/high device tier.
- Offload-on baseline versus DSP/offload-off power measurement.
- APK size delta by ABI if native code is ever introduced.

None of the device audio-quality metrics above has been measured yet. Therefore no “audiophile”, “bit-perfect”, “studio quality” or battery claim is approved.

## Rollback plan

- Keep the custom processor absent from the default renderer until all gates pass.
- Experimental enablement must be a single construction-time switch; bypass uses the existing ExoPlayer path.
- Preserve existing Android EQ presets and DataStore keys.
- If glitches, routing failures or excessive CPU occur, disable the experimental renderer and release the isolated processor without migrating user data.

## Next approved engineering step

Upgrade Media3 in a separate compatibility change, then build an instrumented debug-only `AudioProcessor` adapter around the tested biquad core. Measure it on physical devices before exposing any production control.
