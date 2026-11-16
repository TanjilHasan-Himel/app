# Audia Instructions for Claude

Refer to `AGENTS.md` and `docs/audia/AUDIA_EXECUTION_ROADMAP.md` for full project guidelines.

## Key Rules
1. **Living Diary Requirement:** Always read `docs/audia/AUDIA_CHECKPOINT.md` first to know the current ground truth. Before ending your turn or handoff, append a new dated section to `docs/audia/AUDIA_CHECKPOINT.md` documenting what you did, verified evidence, and what is next.
2. **Roadmap Precedence:** `docs/audia/AUDIA_EXECUTION_ROADMAP.md` is the canonical engineering order. Continue only the first unverified gate unless explicitly redirected.
3. **Debug First:** Develop and verify using `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`. Never deploy or push release tags without full debug verification.
4. **Git Safety:** Treat worktree as shared and dirty. Check `git status --short` before and after work. Never overwrite unrelated changes. Push to remote only upon explicit user request.
