# Audia MiniPlayer visual specification

## Direction

One coherent fluid-glass surface, not stacked cards. Outfit typography and the existing circular artwork progress remain.

## Layer model

1. Dark translucent glass base for legibility.
2. Low-opacity artwork accent moving inside the clipped fluid shape while playing.
3. Thin white/accent glass rim.
4. Top highlight for material depth.
5. One-shot diagonal shimmer on track transition.

## Motion rules

- Liquid phase: 20fps maximum and only while playing.
- Shimmer: one 650ms pass when `song.uri` changes.
- Paused: retain the glass/tint but stop continuous motion.
- System animator scale disabled: no liquid/shimmer motion.
- Never animate component size, text position or control hit boxes.

## Accessibility and performance

- Previous, Play/Pause and Next retain descriptive semantics and at least 48dp hit areas.
- Text contrast remains based on `TextPrimary`/`TextSecondary`.
- Heavy blur/render effects are intentionally excluded; the effect is composited from gradients inside the existing surface.
- Validate compact width, landscape, large font and available physical device separately; untested configurations remain documented.

## Validation result — 2026-11-12

- Unit tests and debug assembly: passed.
- Available physical-device portrait smoke test: passed on Samsung SM-M315F / Android 12; this is a regression datapoint, not the product benchmark.
- Landscape, enlarged-font and reduced-motion setting tests: not tested in this phase.
- Final UIAutomator semantics re-check was blocked because the device was on Samsung lock/AOD; source inspection confirms 48dp controls and explicit icon descriptions.

