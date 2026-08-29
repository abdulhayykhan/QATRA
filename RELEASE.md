# Downloading and Installing QATRA

Since QATRA is distributed directly via GitHub Releases instead of the Google Play Store, here is a quick guide on how to install and update the app.

## 1. Which APK should I download?
When you visit the [GitHub Releases page](https://github.com/abdulhayykhan/QATRA/releases/latest), you will see multiple APK files attached to the latest release.
* **For 99% of modern Android phones** (released in the last 5+ years), download the **`app-arm64-v8a-release.apk`** file.
* If you have an exceptionally old budget device, you might need `app-armeabi-v7a-release.apk`.

## 2. Bypassing Google Play Protect / "Unknown Apps"
Because this APK is downloaded from GitHub and not the Play Store, Android will display a security warning during installation. 

**To install the app on a modern Android version (Android 10+):**
1. Tap on the downloaded APK file in your browser's downloads list or your file manager.
2. A prompt will appear saying: **"For your security, your phone currently isn't allowed to install unknown apps from this source."**
3. Tap **Settings** on that prompt.
4. Toggle the switch for **"Allow from this source"** to ON.
5. Tap the back arrow at the top left to return to the installation screen.
6. Tap **Install**.

*Note: If Google Play Protect subsequently displays an "Unsafe app blocked" or "Unrecognized app" warning, tap **More details** (the small dropdown arrow) and then tap **Install anyway**.*

## 3. In-App Updates
QATRA includes a built-in update checker. When you open the app, it briefly checks the GitHub Releases API for a newer version. 

If an update is available, you will see an "Update Available" prompt inside the app. Tapping **"Update Now"** will automatically open your web browser and take you directly to the latest GitHub Release page so you can download the newest APK following the same steps above.
