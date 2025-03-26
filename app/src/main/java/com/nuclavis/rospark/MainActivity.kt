package com.nuclavis.rospark

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.URLUtil
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import okhttp3.*
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import java.security.Security
import java.util.*
private const val ERROR_DIALOG_REQUEST_CODE = 1

class MainActivity : com.nuclavis.rospark.BaseActivity(), ProviderInstaller.ProviderInstallListener {
    private var retryProviderInstall: Boolean = false


    var logo_url = "";
    var client_class = "";
    var facebook_fundraiser_enabled = "";

    var install_url_google_primary = ""
    var install_url_google_secondary = ""

    init {
        System.setProperty("com.sun.net.ssl.checkRevocation", "true")
        Security.setProperty("ocsp.enable", "true")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        clearVariable("APP_LINK_PAGE_NAME")
        val intentData: Uri? = intent.data
        val pageName = intentData?.getQueryParameter("pageName")
        if(pageName != null && pageName != "") {
            setVariable("APP_LINK_PAGE_NAME", pageName)
        }
        super.onCreate(savedInstanceState)
        startUp()
    }

    private fun startUp(){
        installSplashScreen()
        setVariable("INITIAL_LAUNCH","true");
        clearVariable("PROGRAM_ID")
        clearVariable("BIOMETRIC_APP_CODE")
        clearVariable("CONTAINER_LOGIN_LOGO_IMAGE")
        clearVariable("INITIAL_DISRUPTION_SCREEN")
        setContentView(R.layout.activity_main)
        checkProvider()
        checkMultipleLanguages()
    }

    fun checkProvider(){
        ProviderInstaller.installIfNeededAsync(this, this)
        try {
            ProviderInstaller.installIfNeeded(this)
            println("SECURITY PROVIDER CHECKED -- UP TO DATE")
        } catch (e: GooglePlayServicesRepairableException) {
            println("PLAY SERVICES OUT OF DATE")
            // Indicates that Google Play services is out of date, disabled, etc.
            // Prompt the user to install/update/enable Google Play services.
            GoogleApiAvailability.getInstance()
                    .showErrorNotification(this, e.connectionStatusCode)

        } catch (e: GooglePlayServicesNotAvailableException) {
            // Indicates a non-recoverable error; the ProviderInstaller can't
            // install an up-to-date Provider.
            println("PLAY SERVICES NOT AVAILABLE")
        }
    }

    override fun onProviderInstalled() {}

    override fun onProviderInstallFailed(errorCode: Int, p1: Intent?) {
        GoogleApiAvailability.getInstance().apply {
            if (isUserResolvableError(errorCode)) {
                showErrorDialogFragment(this@MainActivity, errorCode, ERROR_DIALOG_REQUEST_CODE) {
                    onProviderInstallerNotAvailable()
                }
            } else {
                onProviderInstallerNotAvailable()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ERROR_DIALOG_REQUEST_CODE) {
            retryProviderInstall = true
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (retryProviderInstall) {
            ProviderInstaller.installIfNeededAsync(this, this)
        }
        retryProviderInstall = false
    }

    private fun onProviderInstallerNotAvailable() {}

    fun checkMultipleLanguages(){
        setVariable("CLIENT_CODE",getString(R.string.client_code))
        var multiple_enabled = getString(R.string.multi_language_enabled)
        var lang = Locale.getDefault().getLanguage()
        setVariable("DEVICE_LANGUAGE", lang)
        if(multiple_enabled == "true"){
            if(lang != "en"){
                val url = getResources().getString(R.string.base_server_url).plus("/").plus(getResources().getString(R.string.client_code)).plus("/configuration/getLanguages")
                var request = Request.Builder().url(url)
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()
                var client = OkHttpClient();
                setVariable("APP_LANG", "en")
                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: okhttp3.Response) {
                        if(response.code != 200){
                            throw Exception(response.body?.string())
                        }else {
                            val response = response.body?.string();
                            val json = JSONArray(response);
                            for(i in 0 .. json.length() - 1){
                                val obj = json.get(i) as JSONObject
                                if(obj.has("language") && obj.get("language") == lang){
                                    setVariable("CLIENT_CODE",obj.get("client_code") as String)
                                    setVariable("APP_LANG", obj.get("language") as String)
                                }
                            }
                            getServerConfigs();
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        println(e.message.toString())
                        val intent = Intent(this@MainActivity, Error::class.java);
                        startActivity(intent);
                    }
                })
            }else{
                getServerConfigs();
            }
        }else{
            getServerConfigs();
        }
    }

    fun getServerConfigs() {
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getResources().getString(R.string.client_code)).plus("/configuration/getMobileConfigs")
        var request = Request.Builder().url(url)
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else {
                    val response = response.body?.string();
                    val json = JSONArray(response);

                    var primary_color = ""

                    setVariable("LOGIN_BUTTON_COLOR","")
                    setVariable("LOGIN_BUTTON_TEXT_COLOR","")
                    setVariable("LOGIN_BACKGROUND_IMAGE_ENABLED","")
                    setVariable("LOGIN_BACKGROUND_IMAGE","")
                    clearVariable("REGISTER_NOW_ENABLED")
                    clearVariable("USER_FIRST_NAME")
                    clearVariable("USER_TEAM_NAME")
                    clearVariable("DISRUPTIONS_ENABLED")
                    clearVariable("LOGIN_APP_NAME")
                    clearVariable("LOGO_POWERED_BY_URL")
                    clearVariable("LOGIN_PRIMARY_COLOR")
                    clearVariable("CONTAINER_APP_PRIMARY_COLOR")
                    clearVariable("REGISTER_MULTIPLE_STUDENTS_ENABLED")
                    clearVariable("LOGIN_TEXTBOX_BORDER_ENABLED")
                    clearVariable("LOGIN_LOGO_VOICEOVER")
                    clearVariable("CHECK_DEPOSIT_MANAGER_STAFF_LOGIN_ENABLED")
                    clearVariable("CHECK_DEPOSIT_MANAGER_STAFF_LOGIN_REDIRECT_URI")

                    for (i in 0..json.length() - 1) {
                        try {
                            var obj = json.getJSONObject(i);
                            setVariable("HIDE_EVENT_COUNTDOWN", "false")
                            if (obj.has("config_name") && obj.has("config_value")) {
                                var key = obj.get("config_name") as String;
                                var value = obj.get("config_value") as String;
                                println("MAIN CONFIG NAME: " + key)
                                println("MAIN CONFIG VALUE: " + value)
                                if (key == "logo_url") {
                                    logo_url = value;
                                } else if (key == "fundraising_vendor") {
                                    client_class = value;
                                } else if (key == "facebook_fundraiser_enabled") {
                                    facebook_fundraiser_enabled = value
                                } else if (key == "hide_event_countdown") {
                                    if (value == "true") {
                                        setVariable("HIDE_EVENT_COUNTDOWN", "true")
                                    }
                                }else if (key == "build_number") {
                                    setVariable("STORE_BUILD_NUMBER", value)
                                }else if (key == "version_number") {
                                    setVariable("STORE_BUILD_VERSION", value)
                                }else if (key == "version_app_update_enabled") {
                                    setVariable("NEW_VERSION_UPDATE_ENABLED", value)
                                }else if (key == "version_app_update_forced") {
                                    setVariable("NEW_VERSION_UPDATE_FORCED", value)
                                }else if (key == "new_app_update_forced") {
                                    setVariable("NEW_APP_UPDATE_FORCED", value)
                                }else if (key == "new_app_update_enabled") {
                                    setVariable("NEW_APP_UPDATE_ENABLED", value)
                                }else if (key == "button_color"){
                                    setVariable("LOGIN_BUTTON_COLOR",value)
                                }else if (key == "button_text_color"){
                                    setVariable("LOGIN_BUTTON_TEXT_COLOR",value)
                                }else if (key == "primary_color"){
                                    val primary_color = value.trim().lowercase();
                                    setVariable("PRIMARY_COLOR",primary_color)
                                    setVariable("LOGIN_PRIMARY_COLOR",primary_color)
                                    setVariable("CONTAINER_LOGIN_PRIMARY_COLOR",primary_color)
                                }else if (key == "background_image_enabled"){
                                    setVariable("LOGIN_BACKGROUND_IMAGE_ENABLED",value)
                                }else if (key == "background_image"){
                                    setVariable("LOGIN_BACKGROUND_IMAGE",value)
                                }else if (key == "register_now_enabled"){
                                    setVariable("REGISTER_NOW_ENABLED",value)
                                }else if (key == "disruptions_enabled"){
                                    setVariable("DISRUPTIONS_ENABLED",value)
                                }else if (key == "app_name"){
                                    setVariable("LOGIN_APP_NAME",value)
                                }else if (key == "aha_support_chat_enabled"){
                                    setVariable("AHA_SUPPORT_CHAT_ENABLED",value)
                                }else if (key == "logo_voiceover"){
                                    setVariable("LOGIN_LOGO_VOICEOVER", value)
                                }else{
                                    setVariable(key.uppercase(), value)
                                }
                            }
                        } catch (exception: IOException) {
                            println("MAIN CONFIG ERROR")
                        }
                    }

                    if(getStringVariable("LOGIN_BUTTON_TEXT_COLOR") == ""){
                        setVariable("LOGIN_BUTTON_TEXT_COLOR","#ffffff")
                    }
                    if(getStringVariable("LOGIN_BUTTON_COLOR") == ""){
                        if(primary_color != ""){
                            setVariable("LOGIN_BUTTON_COLOR",primary_color)
                        }else{
                            primary_color = Integer.toHexString(getResources().getColor(R.color.primary_color)).substring(2);
                            setVariable("LOGIN_BUTTON_COLOR","#" + primary_color)
                        }
                    }

                    if (URLUtil.isValidUrl(logo_url)) {
                        setVariable("LOGIN_IMG_URL", logo_url);
                        setVariable("CLIENT_CLASS", client_class);
                        setVariable("FACEBOOK_FUNDRAISER_ENABLED", facebook_fundraiser_enabled);
                        getStringsUpdated({stringsCallback()})
                    } else {
                        val intent = Intent(this@MainActivity, Error::class.java);
                        startActivity(intent);
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                val intent = Intent(this@MainActivity, Error::class.java);
                startActivity(intent);
            }
        })
    }

    fun stringsCallback(){
        val client_class = getStringVariable("CLIENT_CLASS")
        val logo_url = getStringVariable("LOGIN_IMG_URL")
        var container_enabled = getString(R.string.container_app)
        setVariable("CONTAINER_APP_ENABLED",container_enabled)
        if(container_enabled == "true"){
            val intent = Intent(this@MainActivity, ContainerLogin::class.java);
            intent.putExtra("logo_url", logo_url);
            intent.putExtra("initial_login", true);
            startActivity(intent);
        }else{
            if (client_class == "classy") {
                val intent = Intent(this@MainActivity, LoginNoFields::class.java);
                intent.putExtra("logo_url", logo_url);
                intent.putExtra("initial", "true");
                startActivity(intent);
            } else {
                val intent = Intent(this@MainActivity, Login::class.java);
                intent.putExtra("logo_url", logo_url);
                intent.putExtra("initial_login", true);
                startActivity(intent);
            }
        }
    }

    override fun onNewIntent(intent: Intent){
        super.onNewIntent(intent)
        val intentAction: String? = intent.action
        val intentData: Uri? = intent.data
        val pageName = intentData?.getQueryParameter("pageName")
        val returnedCode = intentData?.getQueryParameter("code")

        clearVariable("APP_LINK_PAGE_NAME")
        if(pageName != null && pageName != "") {
            setVariable("APP_LINK_PAGE_NAME", pageName)
        }

        val clientCode = getStringVariable("CLIENT_CODE")
        val consId = getConsID()
        val eventId = getEvent().event_id
        val jwt = getAuth()

        if(returnedCode != "" && intentData.toString().contains(".link")){
            val redirect_uri = getStringVariable("CDM_STAFF_LOGIN_REDIRECT_URI")
            val url = getResources().getString(R.string.base_server_url).plus("/").plus(clientCode).plus("/user/neononeAuth?code=").plus(returnedCode).plus("&redirect_uri=").plus(redirect_uri)
            val request = Request.Builder()
                .url(url)
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .addHeader("Authorization" , "Bearer ".plus(jwt))
                .build()

            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: okhttp3.Response) {
                    if(response.code != 200){
                        startUp()
                    }else{
                        val jsonString = response.body?.string() as String

                        if(jsonString is String) {
                            val obj = JSONObject(jsonString)
                            if(obj.has("statusCode")){
                                if (obj.get("statusCode") == 200) {
                                    var data = JSONObject(obj.get("data").toString());
                                    if(data.has("jwt")){
                                        setAuth(data.get("jwt") as String);
                                    }else{
                                        setAuth("");
                                    }

                                    setVariable("CHECK_EVENT_MANAGER_UNIT_IDS", getSafeStringVariable(data, "unit_ids"))

                                    if(data.has("check_event_manager") && (data.get("check_event_manager") == true)){
                                        setVariable("CHECK_EVENT_MANAGER","true");
                                        setVariable("IS_EVENT_MANAGER_ONLY", "true")
                                    }else{
                                        setVariable("CHECK_EVENT_MANAGER","");
                                    }

                                    var eventId = getSafeStringVariable(data, "check_event_manager_event_id")
                                    setVariable("CHECK_EVENT_MANAGER_EVENT_ID",eventId)

                                    var consId = getSafeStringVariable(data, "cons_id")
                                    setConsID(consId)

                                    setEventEventManager(eventId);
                                } else {
                                    displayAlert(getResources().getString(R.string.mobile_login_failed));
                                }
                            } else{
                                displayAlert(getResources().getString(R.string.mobile_login_failed));
                            }
                        }else {
                            displayAlert(getResources().getString(R.string.mobile_login_failed))
                        }
                    }
                }
                override fun onFailure(call: Call, e: java.io.IOException) {
                    println(e.message.toString())
                    startUp()
                }
            })
        }else{
            if(clientCode != "" && consId != "" && eventId != "" && jwt != ""){
                val url = getResources().getString(R.string.base_server_url).plus("/").plus(clientCode).plus("/events/validateUser/").plus(consId).plus("/").plus(eventId)
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                    .addHeader("Authorization" , "Bearer ".plus(jwt))
                    .build()

                val client = OkHttpClient()
                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: okhttp3.Response) {
                        if(response.code != 200){
                            startUp()
                        }else{
                            val jsonString = response.body?.string() as String
                            println("validateUser jsonString: $jsonString")
                            val jsonObject = JSONObject(jsonString);

                            if (jsonObject.has("success") && jsonObject.get("success") == true) {
                                if(intentData != null){
                                    if(pageName != null && pageName != ""){
                                        if(pageName == "fundraise"){
                                            val newIntent = Intent(this@MainActivity, Fundraise::class.java)
                                            startActivity(newIntent)
                                        }
                                        else if(pageName == "donations"){
                                            val newIntent = Intent(this@MainActivity, Donations::class.java)
                                            startActivity(newIntent)
                                        }
                                        else if(pageName == "managepage"){
                                            val newIntent = Intent(this@MainActivity, ManagePage::class.java)
                                            startActivity(newIntent)
                                        }
                                        else if(pageName == "myteam"){
                                            val newIntent = Intent(this@MainActivity, Teams::class.java)
                                            startActivity(newIntent)
                                        }
                                        else if(pageName == "managecompany"){
                                            val newIntent = Intent(this@MainActivity, ManageCompany::class.java)
                                            startActivity(newIntent)
                                        }
                                        else if(pageName == "recruit"){
                                            val newIntent = Intent(this@MainActivity, Recruit::class.java)
                                            startActivity(newIntent)
                                        }
                                        else if(pageName == "trackactivity"){
                                            val newIntent = Intent(this@MainActivity, TrackActivity::class.java)
                                            startActivity(newIntent)
                                        }
                                        else if(pageName == "challenges"){
                                            val newIntent = Intent(this@MainActivity, Challenges::class.java)
                                            startActivity(newIntent)
                                        }
                                        else if(pageName == "gallery"){
                                            val newIntent = Intent(this@MainActivity, Gallery::class.java)
                                            startActivity(newIntent)
                                        }
                                        else if(pageName == "games"){
                                            val newIntent = Intent(this@MainActivity, Games::class.java)
                                            startActivity(newIntent)
                                        }
                                        else{
                                            val newIntent = Intent(this@MainActivity, Overview::class.java)
                                            startActivity(newIntent)
                                        }
                                    }
                                    else{
                                        val newIntent = Intent(this@MainActivity, Overview::class.java)
                                        startActivity(newIntent)
                                    }
                                }
                                else{
                                    val newIntent = Intent(this@MainActivity, Overview::class.java)
                                    startActivity(newIntent)
                                }
                            }
                            else{
                                startUp()
                            }
                        }
                    }
                    override fun onFailure(call: Call, e: java.io.IOException) {
                        println(e.message.toString())
                        startUp()
                    }
                })
            } else {
                startUp()
            }
        }
    }
}