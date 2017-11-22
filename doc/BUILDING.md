    Copyright 2015 Google Inc. All rights reserved.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


# How to build [Treffen](https://github.com/gdg-campinas/treffen) ( [I/O Sched](https://github.com/google/iosched) based app )

Note: while these instructions allow you to build treffen, much of the
functionality that depends on server APIs won't work because in order to
do that you need to configure your own project in Google Developer
Console, create API keys, etc. For more information about what you
need to set up, refer to [Server side setup](#server-side-setup).

This is a Gradle-based project that works best with
[Android Studio](https://developer.android.com/studio/index.html)

To build the app:

1. Install the following software:

    * Gradle: http://www.gradle.org/downloads
    * Android Studio: https://developer.android.com/studio/install.html

1. Run the Android SDK Manager using one of the options below.
   
    * Android Studio Welcome Screen (Recommended)<br>
      In the bottom right of the screen, click in "Configure -> SDK Manager"
    * Android Studio Project Screen (Recommended)<br>
      In the bottom right of the screen, click in "Tools -> Android -> SDK Manager"
    * [sdkmanager](https://developer.android.com/studio/command-line/sdkmanager.html) (Advanced)<br>
      In the console, go to the "ANDROID_SDK_HOME\tools\bin" folder, and use the "sdkmanager" command line tool.
      Please refer its [guide page](https://developer.android.com/studio/command-line/sdkmanager.html) info for more information

1. In the Android SDK Manager, ensure that the following are installed,
   and are updated to the latest available version:
    * SDK Platforms Tab:
      * Android 6.0 (Marshmallow) > Android SDK Platform 23
	* SDK Tools Tab:
      * Android SDK Build-Tools
      * Android SDK Platform-tools
      * Android SDK Tools	
      * Android Support Library
      * Google Play services
      * Support Repository > Android Support Repository
      * Support Repository > Google Repository

1. Create a file in your working directory called local.properties,
   containing the path to your Android SDK. Use local.properties.example as a
   model.

1. Import the project in Android Studio:

   1. Using one of the windows below:
      * Welcome Screen: Click in "Import project (Gradle, Eclipse ADT, etc.)"
      * Project Screen: Press "File" > "New" > "Import Project..."
   3. Navigate to and choose the settings.gradle file in this project
   4. Press OK

1. Add your debug keystore to the project (save it as android/debug.keystore),
   or modify the build.gradle file to point to your key.
   The keystore properties can be changed in the "gradle.properties" file
   in the "android_debugkey_xxx" and android_releasekey_xxx" keys

1. Choose Build > Make Project in Android Studio or run the following
    command in the project root directory:
   ```
    ./gradlew clean assembleNormalDebug
   ```
1. To install on your test device:

   ```
    ./gradlew installNormalDebug
   ```
1. [Refer to 'Navigating the Android app' doc](NAVIGATING_CODE.md).

# Server-side setup

These steps are optional, in the sense that IOSched will build and run
even if you don't set up the server side. However, unless you perform
this setup, your build won't be able to use any of the Google APIs
such as Google sign in, Google Drive integration, Google Maps integration,
etc. So following the instructions in this section is highly
recommended.

0. Change the project's package name to your own package name.
To do that, you can set the "package" attribute of the <manifest>
tag in AndroidManifest.xml.

1. Create a project in the Google Developers Console,
at https://cloud.google.com/console

2. Note your project's Project ID. It is a 11-12 digit sequence
that appears in the URL, indicated by ########### below:
    ```
    https://console.developers.google.com/project/###########/...
    ```
    Write down this project ID number you will need it soon.

3. In the APIs and Auth section, enable these APIs:
    - Google Drive API
    - Firebase Cloud Messaging API ( substitui Google Cloud Messaging for Android )
    - Google Maps Android API
    - Google+ API
    - YouTube Data API v3

4. In the Credentials section, create a Client ID for Android applications
with your package name and your certificate fingerprint. As a reminder,
you can get your debug certificate fingerprint by issuing this command:
    ```
    keytool -exportcert -alias androiddebugkey \
        -keystore $HOME/.android/debug.keystore -list -v
    ```
    It's also advisable to create a Client ID corresponding to your release
    certificate as well. To get your release certificate's fingerprint, run:
    ```
    keytool -exportcert -alias your-key-name \
        -keystore /path/to/your/release/keystore/file -list -v
    ```

5. Still in the Credentials section, create an API Key for Public API
access. The key's type is Key for Android applications, and you should
add both your debug and release certificate fingerprints in the
list of accepted certificates. Note the API key you created. It's
a long alphanumeric string with digits, letters and with a 
few underscores.

6. Enter your API Key from Step 5 into this file:
    ```
    android/src/main/res/values/maps_api_key.xml
    ```
7. Enter your API key in the YOUTUBE_API_KEY constant in Config.java

Done. IOSched should now work with Google sign in, Google Drive
integration and Maps integration. Note that we did NOT include
setting up GCM. For more information about this, see [CUSTOM.md](CUSTOM.md).

