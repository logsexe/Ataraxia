# Independent review brief

## Status and exact claim

This project has NOT received an independent security/privacy audit. CI tests and AI code review are not certification. Reproducible builds have not been demonstrated. The repository is currently private; reviewers need owner-granted access or an owner-approved source snapshot. Do not publish the repository or invite reviewers automatically.

Claim to assess: the Ataraxia-owned native code and bundled filter contain no developer analytics SDK, developer telemetry endpoint, or screen-content upload mechanism. This does NOT mean Instagram is tracking-free. Instagram serves its own remote code and receives login, messages, media and browsing activity. WebView/system-provider networking must be accounted for separately. The host allowlist governs top-level navigation, not every subresource; the ad blocklist is not a comprehensive outbound firewall.

## Reviewer deliverables

Record reviewer identity/organisation, independence, exact commit, APK SHA-256, signing-certificate fingerprint, tool versions, test date, coverage, findings and unresolved limitations. Publish the report only with owner approval. Every later code change requires review of the diff; an audit applies to the reviewed version.

1. Source and dependency review: inspect app/build.gradle, manifest, all Java and bundled JavaScript, build scripts and CI actions. Review all outgoing requests, WebView evaluation, storage, redirects, file uploads, identifiers, logging, dynamic code and permissions. Distinguish platform APIs and website dependencies from bundled libraries. Verify timer/post IDs stay local.
2. Installed APK review: inspect merged manifest, DEX, bundled assets and dependencies; verify Internet-only permission, no Accessibility/admin/background service, no native JS bridge, disabled debugging, no backup and secure WebView defaults. Compare APK assets with the reviewed source. Source scanning alone is insufficient.
3. Runtime network test: use a test account/device, not private messages. Capture startup before login, login, idle, feed, inbox, profile, upload, background, restart, clearing login and error paths. Attribute destinations to the wrapper, Instagram or WebView. Capture under different conditions. Encrypted traffic limits payload visibility; any interception belongs on the test device and must not weaken the distributed app. A clean capture is evidence for those scenarios, not proof of all future behaviour.
4. Independent build: document exact JDK, Android SDK/build tools, Gradle/plugin versions and dependencies. Build the same commit independently and compare APK contents using a documented Android reproducibility process, accounting for signatures. Repeat in a clean environment. Current checksums/signature checks only establish artifact identity/integrity, not reproducibility or privacy.
5. Functional checks: test Focused/ Balanced mode switching, feed redirects after login, in-page history routes, profile links, restored sessions, capped sessions and file picker. Focused mode removes the home feed, not every recommendation on profile/post pages. Neither mode modifies the native Instagram app.

## Available evidence and remaining gates

`python3 tools/privacy_check.py` produces build/privacy-review.json and fails on a narrow set of changed permissions, dependencies and common networking/telemetry APIs. It is deliberately labelled a regression guard, not a malware detector. CI also checks browser fixtures, JVM policies, compiled APK permission list and signature.

Before stable distribution: independent review and remediation; persistent protected signing key; reproducibility work with independent confirmation; phone testing. Keep signing secrets out of source, logs and artifacts. Do not describe this build as audited, certified, zero-tracking or reproducible until the relevant evidence exists.

References:
- https://mas.owasp.org/MASTG/
- https://f-droid.org/docs/Reproducible_Builds/
