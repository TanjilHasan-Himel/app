# Audia network audit

Remediation update (2026-11-12): the LRCLIB gaps below were repaired with cancellation/deduplication, bounded timeouts, Retry-After handling, metadata scoring, cache replacement and manual selection. Radio Browser mirror discovery/click counting and direct station streaming were added with updated privacy disclosure. Cleartext transport is enabled for legacy HTTP radio streams and remains a beta security-review risk.

## Destinations found

| Host | Purpose | Auth | Data sent | Current controls | Status |
|---|---|---|---|---|---|
| `lrclib.net` | synchronized/plain lyrics | none | cleaned title, artist, album, duration; title on fallback search | connect/read timeout, Room cache | Direct client; partial resilience |
| `en.wikipedia.org` / `wikipedia.org` | summary/search | none | cleaned song title and artist | short timeout, Room cache | Direct client; weak match risk |
| `musicbrainz.org` | recording/release metadata | User-Agent only | title and artist | short timeout, Room cache via combined result | Direct client; first-result risk |

No developer-controlled proxy, embedded secret key or paid service was found. Ordinary local playback and cached/local lyrics do not require these hosts.

## Gaps

- LRCLIB 429/Retry-After handling, request deduplication and cancellation on track changes were not found.
- Fallback result confidence is insufficient.
- Provider terms, commercial-display rights and cache rights were not legally verified in this code audit; that needs product/legal confirmation before public distribution.
- Network privacy disclosure should explicitly describe per-track metadata requests.

Preferred resolution order remains: user local lyrics → valid cache → direct LRCLIB → authorized secondary provider → manual selection → unavailable state.

