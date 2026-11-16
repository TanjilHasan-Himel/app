# Audia: GitHub-first release, consent, updates and growth

**Decision draft for Tanjil Hasan Himel · 14 November 2026**  
**Status:** researched plan, **not implemented or approved**. This document changes no app, website, Firebase project or release. The canonical engineering order remains `AUDIA_EXECUTION_ROADMAP.md`; its first unverified F0 gate is still open.

## 1. What is true in this repository today

Audia is distributed outside Google Play for now. The configured Git remote is `TanjilHasan-Himel/playtime-musicapp`; this does **not** prove that its Releases are public or populated. The supplied landing-page URL and GitHub Releases page could not be fetched during this audit, so their live content and link behavior are **NOT VERIFIED**.

`app/build.gradle.kts` already accepts a release keystore path, passwords and alias through `AUDIA_KEYSTORE_PATH`, `AUDIA_KEYSTORE_PASSWORD`, `AUDIA_KEY_ALIAS`, `AUDIA_KEY_PASSWORD`; absent these, release signing is not configured. `app/version.properties` currently says `versionCode=90`, `versionName=9.0.78`. A real signed release APK, certificate continuity and install-over-previous-version test are **NOT VERIFIED**. No Firebase Analytics/Crashlytics dependency, Google Services plugin, GitHub release workflow, in-app release checker or unified dashboard was found in the inspected configuration. The current privacy text says there is no developer-operated analytics or remote crash collection; it **must change before** either SDK sends data. Existing `RELEASE_CHECKLIST.md` assumes Play Console in places and needs a GitHub-specific companion, not silent reuse as proof of readiness.

## 2. The short answer: one publishing path, one download link

```text
QA on release candidate → signed, verified APK → GitHub stable Release
                                              ├→ one permanent latest-APK URL
                                              ├→ website Download button and Android Sharesheet
                                              ├→ optional in-app update check / Obtainium
                                              └→ release notes + old-version archive
```

Use one **fixed APK asset name**, for example `Audia.apk`, on every *stable* GitHub Release. Then the official stable link can be:

`https://github.com/TanjilHasan-Himel/playtime-musicapp/releases/latest/download/Audia.apk`

GitHub redirects this `latest/download` URL to the asset in the latest full release. The filename must match exactly; pre-releases are a separate beta channel, not this stable link. Put the same URL behind the landing page's Download button and in Share Audia. Better still, make a memorable website URL such as `/audia/download` that redirects to it; then a future host move changes one website redirect, not old social posts. **Do not label either URL as tested until a real public release and HTTP/download test pass.** [GitHub's release-link documentation](https://docs.github.com/en/repositories/releasing-projects-on-github/linking-to-releases)

An update announcement is not an APK silently installing itself. Recommended default: when Audia opens, or at a low-frequency background interval selected by the user, fetch stable release metadata over HTTPS, compare its `versionCode` to the installed build, and show **one dismissible in-app update card** with version, release notes and Download. Never repeatedly toast on every launch. If the user wants OS notification alerts, ask for notification permission in context; otherwise stay in-app. An app-based checker still needs implementation and network/privacy review. Users who install via Obtainium can instead let Obtainium track the GitHub Release source and notify them; it is an optional third-party app, not something every Audia user must install. [Obtainium project](https://github.com/ImranR98/Obtainium)

## 3. Signing: the rule that protects every future update

Android treats a new APK as an in-place update only when the **application ID and signing certificate are compatible** and `versionCode` increases. Audia's application ID is `com.eplaytime.app`; `versionName` is the human-readable label, while `versionCode` controls update ordering. A debug-signed APK generally cannot update a release-signed APK in place. The release key is therefore a product asset, not a disposable build file. [Android app-signing guide](https://developer.android.com/studio/publish/app-signing)

Before the first public release:

1. Create one dedicated release keystore; store it **outside Git**, with two encrypted offline backups and a recovery note available only to the developer. Record alias, creation date and **public certificate SHA-256 fingerprint**, never passwords/private-key bytes in docs or issues.
2. Build `:app:assembleRelease` with the four environment variables already supported by Gradle. Fail the publish workflow if signing configuration is missing or the APK is unsigned. Do not publish a debug APK as stable.
3. Verify the exact output with Android SDK `apksigner verify --verbose --print-certs Audia.apk`; compare the certificate fingerprint against the pinned release fingerprint. Generate and publish `SHA256SUMS` for download integrity.
4. Install that APK cleanly and **over the previous signed release** on a test phone; check user data, scheduled playback, queue, notification, local/radio playback and crash logs. This is an acceptance test, not implied by compilation.
5. Increment `versionCode` on every shipped update. Never overwrite an old tag/release asset to pretend a different binary has the same version.

Losing the private signing key can make ordinary in-place updates impossible. Do not promise an easy reset. The key is not an API key and must never be embedded in the APK or shared with an AI agent in chat.

## 4. Safe installation instructions users should see

Landing page and Release notes should say: download only from the linked official GitHub Release; confirm expected app name/version and, for advanced users, SHA-256; open the downloaded APK; Android may ask the browser/file manager for **Install unknown apps** permission and may show Play Protect/security warnings. Explain what the warnings mean and let the user decide—never teach them to turn off system protection globally. To update, download the newer signed APK and approve Android's installer; app data should stay if the certificate/package match. If Android reports a signature conflict, **do not tell users to uninstall** before preserving local schedules/favorites/loops; investigate the certificate first. [Android alternative-distribution guidance](https://developer.android.com/distribute/marketing-tools/alternative-distribution)

**2026–27 caveat:** Google is rolling out Android developer verification for apps distributed outside Play on certified devices, first in selected regions and then more broadly. A GitHub Release is still a valid hosting method, but it does not by itself settle device registration/verification. Check the current Android Developer Console's **limited-distribution** eligibility and cost/identity requirements before promising effortless global installs. This is distinct from buying a Play Console account. Android also documents an advanced sideload flow; it is not a good default onboarding path for ordinary users. [Android verification overview](https://developer.android.com/developer-verification), [limited distribution](https://developer.android.com/developer-verification/guides/limited-distribution)

## 5. Privacy, Terms and evidence of choice—where the proposed shortcut fails

“By using Audia you agree to Terms” can be a Terms notice, subject to applicable law. It is **not a reliable substitute** for permission to turn on optional Analytics/Crashlytics or an OS media/notification/scheduling permission. A Privacy Policy explains processing; it is not automatically blanket consent. Jurisdiction matters; obtain local legal review before publishing contractual language. As an example of why the blanket approach is unsafe, UK regulator guidance explicitly says consent should be separate from Terms and based on a positive action. [ICO consent guidance](https://ico.org.uk/for-organisations/direct-marketing-and-privacy-and-electronic-communications/guidance-on-the-use-of-storage-and-access-technologies/how-do-we-manage-consent-in-practice)

**Low-friction Audia design:** one first-run screen with short plain-language Terms/Privacy links, then two independent, **off-by-default** optional choices: “Help improve Audia with anonymous usage analytics” and “Send crash reports.” Offer “Continue with essential features” equally clearly. Do not block offline music playback for refusing optional telemetry. Record the chosen policy version, choice values and timestamp locally; expose them under Settings → Privacy so users can change them later. Keep Android permission prompts tied to the feature being used, not to this screen. On a material policy/data-use change, show the new text and obtain any necessary fresh choice; do not silently broaden an old grant.

If you want a receipt on **your** side too, that is a new backend/data-processing feature, not a PDF trick. An accountless installation cannot prove the named human who tapped the screen. A possible minimal receipt is a random installation ID, policy hash/version, choice values and server timestamp sent over HTTPS **only after a clearly disclosed, valid choice**; define retention, deletion, access and compromise controls. Avoid collecting device IDs, email, listening history, file names or IP logs merely to manufacture “proof.” For a small GitHub-distributed app, local versioned choices plus actual SDK suppression are the sensible first phase; server receipts need a separate legal/security decision. No receipt or PDF is currently implemented or sent to the developer.

Firebase's own Android guidance supports disabling Analytics collection by default and enabling it later; Crashlytics **collects by default** unless configured otherwise. Thus install-time SDK defaults must be checked before shipping any consent UI. Note also that opt-out Crashlytics can hold reports locally and upload them after later opt-in, which must be disclosed or cleared deliberately. [Firebase Analytics collection](https://firebase.google.com/docs/analytics/android/configure-data-collection), [Crashlytics opt-in](https://firebase.google.com/docs/crashlytics/android/customize-crash-reports)

## 6. Analytics, Crashlytics and a real unified dashboard

The dashboard should be **private to the developer**; the public Audia landing page must never contain Google service-account credentials, admin tokens or raw diagnostic events. Separate the three metrics:

| Card | Source | Honest meaning |
|---|---|---|
| Release downloads | GitHub Releases asset `download_count` | APK asset downloads, **not** unique users or installs; website mirrors/Obtainium behavior can affect interpretation. |
| Active users | GA4/Firebase Analytics Data API `activeUsers` | Only devices that actually send permitted Analytics events; opt-outs are absent. |
| Stability | Crashlytics + Firebase Sessions export / console | Crash-free users/sessions for reporting devices, not every installed device. |

GitHub's release-asset API exposes `download_count` and an SHA-256 `digest`. GA4 Data API exposes `activeUsers`. Firebase documents Crashlytics/Sessions export to BigQuery for custom dashboards, with daily batch export possible on lower-cost setups; real-time streaming is not available on the Spark plan. Check BigQuery limits/billing before enabling export. [GitHub release assets](https://docs.github.com/en/rest/releases/assets), [GA4 Data API](https://developers.google.com/analytics/devguides/reporting/data/v1/basics), [Crashlytics BigQuery export](https://firebase.google.com/docs/crashlytics/bigquery-export)

**Build in two slices, not a fake all-green dashboard:**

- **Slice D1:** private dashboard shell with real GitHub download/release numbers, clear `NOT CONNECTED` cards for Analytics/Crashlytics, source links, last-updated timestamp and no placeholder numbers presented as facts. Can be built without Firebase access.
- **Slice D2:** create the Firebase project under the developer's account; register exact Android application ID; update privacy text; implement off-by-default consent and verify zero pre-consent network telemetry; then connect GA4 Data API and Crashlytics/Sessions export to a server-side authenticated aggregator. Add a secure admin login and budget alerts. No service-account JSON in GitHub, website JavaScript or APK.

The dashboard cannot be honestly “fully live” until the owner supplies or creates the Firebase project, enables the relevant exports/APIs and chooses a private hosting/authentication setup. Dashboard availability must not become a dependency for local playback or schedules.

## 7. One controlled release operation, not seven manual edits

The desired operator experience is **approve one release candidate**, not “upload any APK and hope.” A proposed GitHub Actions workflow (none exists here yet):

1. Developer completes F0 release gates and selects a clean commit/tag. CI runs tests, lint, release build and dependency/security checks. An agent may prepare the candidate but cannot assert hardware tests it did not execute.
2. Protected release job obtains signing material from restricted GitHub secrets or a trusted self-hosted signing environment; it produces `Audia.apk`, checksum, certificate fingerprint and build provenance. Human approval before publishing is appropriate while releases are young.
3. Workflow creates a GitHub Release from the version tag, with notes, compatibility/known issues, APK and checksum. Beta tags are marked **pre-release**; the stable `latest` path remains stable.
4. Landing page always points at the stable URL or own redirect; it need not be edited for every version. A small public `latest.json`/release-feed may drive the site version badge and app update card, generated from the approved release, not manually typed in seven places.
5. Post-publish smoke: download URL returns the new file; checksum/signature match; in-place update works; app update card shows one notice; website and Share Audia land on the official page. Rollback means ship a **new higher-versionCode** repair, not silently replacing an already shipped APK.

GitHub Actions can reduce repetitive work, but first public signing setup, hardware QA and release approval remain human gates. A fixed link does not remove the need to verify each build. GitHub's release API can also list historical assets and counts for the private dashboard/archive. [GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases), [GitHub release API](https://docs.github.com/en/rest/releases/releases)

## 8. Old releases are a museum, not lost debug sessions

Keep every public **versioned GitHub Release** with immutable tag, APK, checksum, certificate fingerprint, notes and known issues. Add an `Archive / Previous versions` link on the website pointing to Releases; mark old versions as potentially insecure and never make them the main Download button. Keep one encrypted offline/cloud backup of release APKs **and the keystore separately**: a GitHub account or repository can be lost. Maintain `CHANGELOG.md` or release notes with a link to the commit, tests and device matrix. Debug builds should be named/retained as temporary QA artifacts, **not** mixed with public stable releases. A Git commit preserves source, not necessarily the exact signed APK; retain both.

## 9. Sharing and responsible growth

**Share Audia (P1):** add a visible action in Settings/About and the More menu. Use Android's standard Sharesheet (`ACTION_SEND`, `text/plain`) with app name, one-line value proposition and the stable official landing/download URL. No contacts permission, address-book upload, hidden invite tracking or auto-sending. Test WhatsApp, Telegram, Messenger and copy-link destinations. The *landing page* is usually the better share URL because it explains installation and trust; keep the direct APK as its Download button. [Android Sharesheet guidance](https://developer.android.com/training/sharing/send)

**Share My Listening Setup (later P2):** user chooses an EQ preset/theme (never automatically expose local song names or file paths), sees a preview and explicitly shares an image through the Sharesheet. A small Audia mark and readable short link/QR can appear in a non-obstructive corner. Provide “share without branding” if feasible and honor artwork/lyrics copyright; do not export private queue/history by default. This is a creative distribution feature, not a substitute for app quality or user trust.

For discoverability: one polished 30–60-second demo of the real scheduled-playback/immersive-player USP, concise README, screenshots, clear release notes, a website FAQ, accessible install/update instructions, honest privacy story and a low-friction bug-report link. Track which channel brought a visitor with privacy-conscious **website** aggregate analytics if desired; do not equate clicks, APK downloads and actual active users. Avoid unsolicited messages or dark-pattern invitations. A public source repository also requires deliberate license/IP/third-party attribution review; do not assume uploading code grants permission to redistribute all included media/artwork.

## 10. Approval gates and executable order

| Gate | Scope | Done when |
|---|---|---|
| G0 | Finish roadmap F0 reliability/security gate | Existing F0 evidence complete; no release claim based on compilation alone. |
| G1 | Distribution identity and consent decision | Public/private repo, legal text, optional telemetry default, release signing owner and verification account path chosen. |
| G2 | Signed release pilot | Same certificate, higher versionCode, clean + in-place install, checksum, official GitHub asset and stable URL physically verified. |
| G3 | Automated controlled release + archive | Tagged workflow, human approval, website redirect/link, release notes and previous-version archive verified. |
| G4 | Share Audia + update discovery | Sharesheet targets and single-notice update UX pass device QA; no silent install claim. |
| G5 | Dashboard D1 | Private real GitHub metrics; disconnected Firebase cards explicitly labelled. |
| G6 | Firebase consent + dashboard D2 | Updated policy, opt-in controls, pre-consent packet check, real GA4/Crashlytics aggregation, access/billing review. |
| G7 | Listening setup sharing/growth experiment | Preview, privacy/copyright checks, accessible share flow and actual user feedback. |

**Immediate next action in the current canonical roadmap:** F0C persistence/security and F0 playback/device regression. This document proposes distribution work after that release foundation; it does not authorize skipping it. For a future agent: inspect `git status --short`, this plan and the canonical roadmap; confirm the live website/repo access; do only the first unverified gate; record files, exact commands/results, `NOT TESTED` cases, risk and next task. Do **not** create/publish a release, turn on Firebase collection or upload signing keys merely because this document exists.

## Research limits

Official documentation was checked on 14 November 2026. The live Audia website and GitHub Releases were inaccessible to the browsing tool, so links, existing APKs, signing continuity, Firebase ownership, costs for this owner's account and device-install behavior remain **NOT TESTED**. Legal examples are not legal advice. Android verification, Firebase pricing and platform behavior can change; re-check official guidance at the actual publication date.
