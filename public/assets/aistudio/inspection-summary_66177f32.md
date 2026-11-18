# Result Maker CI Inspection Report

| Field | Value |
|---|---|
| Trigger | `push` (`main`) |
| Commit | `e8b24709e296f4ed9139e202c7510d30f9c9734a` |
| Version | `1.0.17` (code `17`) |
| Release Tag | `v1.0.17` |
| Keystore Mode | `not-run` |

## Step Outcomes

| Step | Outcome |
|---|---|
| Environment & Config Pre-check | `failure` |
| Keystore Preparation | `skipped` |
| Gradle Release Build | `skipped` |
| Stage APK | `skipped` |
| Verify APK Signature | `skipped` |
| Upload APK Artifact | `skipped` |
| Publish GitHub Release | `skipped` |

## Environment & Firebase Config
```text
=== Timestamp (UTC) ===
Wed Nov 18 08:01:26 UTC 2026

=== Java ===
openjdk version "21.0.13" 2026-10-20 LTS
OpenJDK Runtime Environment Temurin-21.0.13+9 (build 21.0.13+9-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.13+9 (build 21.0.13+9-LTS, mixed mode, sharing)

=== Gradle ===

------------------------------------------------------------
Gradle 8.11.1
------------------------------------------------------------

Build time:    2024-11-20 16:56:46 UTC
Revision:      481cb05a490e0ef9f8620f7873b83bd8a72e7c39

Kotlin:        2.0.20
Groovy:        3.0.22
Ant:           Apache Ant(TM) version 1.10.14 compiled on August 16 2023
Launcher JVM:  21.0.13 (Eclipse Adoptium 21.0.13+9-LTS)
Daemon JVM:    /usr/lib/jvm/temurin-21-jdk-amd64 (no JDK specified, using current Java home)
OS:            Linux 7.0.0-1019-azure amd64


=== Android SDK ===
SDK_DIR=/usr/local/lib/android/sdk
android-34
android-34-ext10
android-34-ext11
android-34-ext12
android-34-ext8
android-35
android-35-ext14
android-35-ext15
android-36
android-36-ext18
android-36-ext19
android-36.1
android-37.0
android-37.1
android-37.2
android-37.2-beta1
android-37.2-beta2
android-37.2-beta3
34.0.0
35.0.0
35.0.1
36.0.0
36.1.0
37.0.0

=== Firebase & Manifest Files ===
app/google-services.json: MISSING
firebase_applet_config.xml: PRESENT
```

## Firebase Config Troubleshooting
- Add repository secret `GOOGLE_SERVICES_JSON_BASE64` (base64 of `app/google-services.json`) or `GOOGLE_SERVICES_JSON` (raw JSON) under **Settings -> Secrets and variables -> Actions**, or commit `app/google-services.json`.
- Ensure your Firebase project includes Android app `com.kgps.result` and Google Sign-In enabled (Web OAuth `client_type: 3`).

