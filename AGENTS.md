# Audia contributor handoff

- Treat this repository as a shared, dirty worktree. Never reset, checkout, delete, reformat, or
  overwrite unrelated user/agent changes. Inspect `git status --short` before and after work.
- Read `docs/audia/AUDIA_EXECUTION_ROADMAP.md` first, then `AUDIA_DECISIONS.md`, `AUDIA_TASKS.md`,
  `AUDIA_QA.md`, and `AUDIA_CHECKPOINT.md`. The execution roadmap is the canonical current order;
  older audit documents are historical evidence and may contain stale status.
- Continue only the first unverified gate unless the user explicitly changes priority. Record scope,
  dependencies, acceptance criteria, implementation, evidence, remaining risk, and the exact next task.
- Never mark a feature verified from compilation alone. Label physical-device, Android Auto,
  migration, performance, battery, accessibility, responsive, and audio-quality cases `NOT TESTED`
  when they were not executed.
- Preserve Audia's invariants: integrated artwork-first Player, reliable Scheduled Playback,
  per-song named A-B loops, real radio, local-first data, accessible controls, and reduced motion.
- Database changes require explicit migrations and migration tests. Playback/service work requires
  queue, lifecycle, focus/route, notification, and process-death regression. Public services and
  network changes require controller-trust and input-boundary review.
- Do not add scraping, embedded credentials, global cleartext exceptions, destructive migrations,
  or dependencies without an explicit product, licensing, security, and maintenance reason.
- Default Kotlin verification is `./gradlew :app:testDebugUnitTest :app:assembleDebug`; use
  `.\gradlew.bat` on Windows. Add focused instrumentation, Macrobenchmark, and device tests according
  to risk. Never claim hardware success without captured evidence.
- Cross-Agent Living Diary: Every AI agent (Gemini, Claude, Codex, Copilot, Cursor) MUST maintain
  `docs/audia/AUDIA_CHECKPOINT.md` as the living session diary. At session start, read it to understand
  the latest ground truth. At handoff, append a new dated milestone entry documenting files changed,
  behavior delivered, test evidence, and the exact next task. Never leave future agents or the user
  guessing what was completed.
- Development-First Lifecycle: Always develop and debug using `:app:testDebugUnitTest` and `:app:assembleDebug`.
  Only create or push a release tag (`v*`) once debug testing passes completely.
- At handoff report: files changed, behavior delivered, exact commands/results, untested cases,
  remaining risks, and the next executable roadmap gate.
