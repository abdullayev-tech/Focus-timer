# Focus Timer - Android App

A minimalist, bottom-first focus timer built with Jetpack Compose and Material 3.

## Automated APK Build (GitHub Actions)

A GitHub Actions workflow is set up in `.github/workflows/build-apk.yml` to automatically build your Android APK.

### How to Build & Download the APK:

1. **Push to GitHub**:
   - Push your code to the `main` or `master` branch, OR
   - Go to the **Actions** tab in your GitHub repository, select **Build Android APK**, and click **Run workflow**.

2. **Download the APK File**:
   - In GitHub, go to the **Actions** tab.
   - Click on the latest workflow run (e.g., "Build & Package APK").
   - Scroll down to the **Artifacts** section at the bottom of the summary page.
   - Click on **FocusTimer-APK** to download the ZIP file.
   - Extract the ZIP file to get `FocusTimer-debug.apk`.

3. **Install on Your Android Phone**:
   - Transfer `FocusTimer-debug.apk` to your phone (via USB cable, Google Drive, email, or download directly on your phone's browser from GitHub).
   - Tap the APK file to install it.
   - If prompted with *"For your security, your phone is not allowed to install unknown apps from this source"*, tap **Settings** and toggle **Allow from this source**.
   - Tap **Install**, and the app is ready to use!
