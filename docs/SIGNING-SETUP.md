# Persistent signing: prepared, not provisioned

The build now accepts a permanent key. No key has been generated or uploaded, and the protected GitHub environment has not been configured. Current preview APKs still use temporary test signing. The GitHub connector available in this session cannot manage Actions secrets or environments.

## Owner setup

1. On a trusted computer with a JDK, create a key using the interactive command below. Keep the keystore and passwords in secure, separately backed-up storage. Do not send them through chat or commit them.

   `keytool -genkeypair -keystore ataraxia-release.p12 -storetype PKCS12 -alias ataraxia-release -keyalg RSA -keysize 3072 -validity 10000`

2. Run `keytool -list -v -keystore ataraxia-release.p12 -alias ataraxia-release` and retain the SHA-256 certificate fingerprint. The fingerprint is public; the private key is not.
3. In repository Settings → Environments, configure `release-signing`, restricting it to main and adding required review where your GitHub plan supports it. Add these environment secrets:
   - ATARAXIA_KEYSTORE_BASE64: base64 representation of the keystore, generated locally without exposing it in logs or source. Base64 is encoding, not encryption.
   - ATARAXIA_STORE_PASSWORD: keystore password.
   - ATARAXIA_KEY_ALIAS: ataraxia-release.
   - ATARAXIA_KEY_PASSWORD: key password (normally the same as the store password for PKCS12).
   - ATARAXIA_CERT_SHA256: expected SHA-256 certificate fingerprint.
4. After reviewing and merging the changes, manually dispatch `Manually build with persistent signing` from main. The workflow validates source, requires every secret, verifies the expected certificate and permissions, cleans up the temporary key and uploads a candidate. It does not publish a release.
5. Verify the first signed candidate on a test phone. A later build with the same key, application ID and higher version code must install over it while retaining login/settings. Do not call seamless updates verified until this test passes.

## Local builds

Set ATARAXIA_KEYSTORE (absolute path), ATARAXIA_STORE_PASSWORD, ATARAXIA_KEY_ALIAS and ATARAXIA_KEY_PASSWORD in the build process environment, then run `./gradlew --no-daemon :app:assembleRelease`. Missing configuration fails the release build; there is no debug-key fallback. Do not use command-line password arguments that persist in history.

A permanent key cannot retroactively update existing differently signed preview installs. Migration from those previews requires one more reinstall. Losing the permanent key can make future in-place updates impossible. Keep it out of ordinary CI artifacts and Gradle caches; never attach it to an issue.

This setup improves signing continuity, not proof of privacy or source reproducibility. Independent review and network testing remain separate work. The signed workflow is prepared but unexecuted until owner provisioning and merge.
