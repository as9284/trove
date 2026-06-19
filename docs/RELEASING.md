# Releasing

How to build a signed release of Trove. The signing key and its passwords are
secret: they live only on your machine and are never committed.

## One-time: create a signing key

Generate a keystore with `keytool` (bundled with the JDK). Pick your own strong
passwords when prompted, and keep the resulting file safe and backed up. If you
lose it, you cannot ship updates that the system accepts as the same app.

```
keytool -genkeypair -v \
  -keystore trove-release.jks \
  -alias trove \
  -keyalg RSA -keysize 4096 -validity 10000
```

Move `trove-release.jks` somewhere safe (the repo root is fine; it is gitignored).

## One-time: point the build at the key

Copy the template and fill in your values:

```
cp keystore.properties.example keystore.properties
```

`keystore.properties` is gitignored. Its fields:

| Key | Meaning |
|---|---|
| `storeFile` | Path to the keystore, relative to the repo root or absolute. |
| `storePassword` | The keystore password. |
| `keyAlias` | The key alias (`trove` if you used the command above). |
| `keyPassword` | The key password. |

For CI, you can instead set `TROVE_STORE_FILE`, `TROVE_STORE_PASSWORD`,
`TROVE_KEY_ALIAS`, and `TROVE_KEY_PASSWORD` as environment variables.

## Build

Signed APKs (for direct download / GitHub releases). The build splits per ABI,
so distribute the arm64-v8a one:

```
./gradlew assembleRelease
# app/build/outputs/apk/release/app-arm64-v8a-release.apk
# app/build/outputs/apk/release/app-x86_64-release.apk
```

If no keystore is configured, these still build but produce an unsigned artifact.

## Before tagging a release

- Bump `versionCode` (every release) and `versionName` in
  [`app/build.gradle.kts`](../app/build.gradle.kts).
- Confirm the build is clean and installs on a device.
