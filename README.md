# Client Settings Update Instructions

## **CONFIG SETTINGS**
Replace the following values in the app_settings.xml file found at *app/src/main/res/values/app_settings.xml*
- < string name="app_name" type="string">APP NAME</ string>
- < string name="client_code" type="string">CLIENT CODE</ string>
- < string name="bundle_id" type="string">BUNDLE ID</ string>
- < string name="base_server_url" type="string">BASE SERVER URL</ string>

## **PRIMARY COLOR UPDATE**
Replace the following values in the colors.xml file in *app/src/main/res/values/colors.xml*
- < color name="primary_color">PRIMARY_COLOR</ color>
- < color name="facebook_blue">PRIMARY_COLOR</ color>

## **EVENT COLOR UPDATE**
To add a new color option: 
1) In the styles.xml file add a new theme using this template
    <style name="COLORDESCRIPTIONTheme" parent="Theme.NTMobileAndroid">
        <item name="colorSecondary">#f2f1ef</item>
        <item name="colorPrimary">COLORHASH</item>
        <item name="colorPrimaryVariant">#f2f1ef</item>
    </style>
    <style name="COLORDESCRIPTIONThemeWhite" parent="Theme.NTMobileAndroid">
        <item name="colorSecondary">#ffffff</item>
        <item name="colorPrimary">COLORHASH</item>
        <item name="colorPrimaryVariant">COLORHASH</item>
    </style>
2) In the BaseLanguageActivity.kt file add a new use case to the getEventMobileConfigs() function's callback:
    else if(value == "COLORHASH"){
        app_theme = R.style.COLORDESCRIPTIONTheme
    }

## **IMAGE LOCATIONS**
All of the image folders are found in *app/src/main/res/*. The files should keep the same name but the image content can change

**mipmap-mdpi**
- icon_foreground.png (square 108x108)
- icon_round.png(circle 48x48)
- icon.png (rounded rectangle 48x48)

**mipmap-hdpi**
- icon_foreground.png (square 162x162)
- icon_round.png(circle 72x72)
- icon.png (rounded rectangle 72x72)

**mipmap-xhdpi**
- icon_foreground.png (square 216x216)
- icon_round.png(circle 96x96)
- icon.png (rounded rectangle 96x96)

**mipmap-xxhdpi**
- icon_foreground.png (square 324x324)
- icon_round.png(circle 144x144)
- icon.png (rounded rectangle 144x144)

**mipmap-xxxhdpi**
- icon_foreground.png (square 432x432)
- icon_round.png(circle 192x192)
- icon.png (rounded rectangle 192x192)

You also need to update the icon-playstore.png file in the app/src/main/ folder

## Facebook Settings
For apps that use facebook, in /app/res/values/strings.xml add the following, and replace [APP_ID] and [CLIENT_TOKEN] with the app id and client token for the app in facebook. 
```
<string name="facebook_app_id">[APP_ID]</string>
<string name="fb_login_protocol_scheme">fb[APP_ID]</string>
<string name="facebook_client_token">[CLIENT_TOKEN]</string>
```

In the manifest at /app/manifest/AndroidManifest.xml add the following meta-data tags to the application element. 
```
<application android:label="@string/app_name" ...>
    ...
   	<meta-data android:name="com.facebook.sdk.ApplicationId" android:value="@string/facebook_app_id"/>
   	<meta-data android:name="com.facebook.sdk.ClientToken" android:value="@string/facebook_client_token"/>
    ...
</application>
```

Also add the following inside the application element. 

```
    <activity android:name="com.facebook.FacebookActivity"
        android:configChanges=
                "keyboard|keyboardHidden|screenLayout|screenSize|orientation"
        android:label="@string/app_name" />
    <activity
        android:name="com.facebook.CustomTabActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.DEFAULT" />
            <category android:name="android.intent.category.BROWSABLE" />
            <data android:scheme="@string/fb_login_protocol_scheme" />
        </intent-filter>
    </activity>
```

Add the following after the application element. 

```
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="com.google.android.gms.permission.AD_ID" tools:node="remove"/>
```

Add the following to the build.gradle (Module) under dependancies 
```
implementation 'com.facebook.android:facebook-android-sdk:15.2.0'
```

## **Firebase Settings (Google Analytics & Push Notifications)**
To add a new app to use the Firebase Google Analytics or Push Notifications you can replace the google-services.json with the new one downloaded from the project settings page in Firebase, or you can simply add the new client section to the existing google-services.json file for the project to accomodate all apps.

## **build.gradle**
The app level build.gradle file also needs to have the version number, build number, and applicationId updated to match the new app's information. If signing from the command line then you also need to add the Signing Configs and Build Types inside the android element

        android {
            signingConfigs {
                create("release") {
                    storeFile = file("path_to_file")
                    storePassword = "password"
                    keyAlias = "alias"
                    keyPassword = "keypassword"
                }
            }
            buildTypes {
                getByName("release") {
                    signingConfig = signingConfigs.getByName("release")
                }
            }`


