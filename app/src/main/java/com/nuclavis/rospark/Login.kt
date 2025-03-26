package com.nuclavis.rospark
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.telephony.TelephonyManager
import android.text.SpannableString
import android.text.InputType
import android.text.style.UnderlineSpan
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.amangarg.localizationdemo.JsonHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.comm100.livechat.ChatActivity
import com.comm100.livechat.VisitorClientInterface
import com.fasterxml.jackson.databind.ObjectMapper
import com.nuclavis.rospark.databinding.LoginBinding
import okhttp3.*
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executor

class Login : BaseLanguageActivity() {
    override fun childviewCallback(string: String, data:String) {
        if(string == "updateRecommended"){
            if(intent.getStringExtra("initial") == "true"){
                if(getBiometricString("BIOMETRIC_LOGIN_ENABLED") == "enabled") {
                    launchBiometricLogin();
                }
            }
        }
    }
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var register_link = ""

    override fun slideButtonCallback(card: Any, forward:Boolean){}

    override fun onCreate(savedInstanceState: Bundle?) {
        setVariable("INITIAL_LOGIN","true")
        setVariable("EVENT_THEME",R.style.StandardTheme.toString())
        recolorTheme()
        super.onCreate(savedInstanceState)
        val binding: LoginBinding = DataBindingUtil.setContentView(
            this, R.layout.login)
        binding.colorList = getColorList("login")
        binding.loginInputBorder = getStringVariable("LOGIN_TEXTBOX_BORDER_ENABLED") == "true"

        val manager = applicationContext.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        hideAlert()
        sendGoogleAnalytics("standard_login_view","standard_login")
        lateinit var sharedPreferences: SharedPreferences
        sharedPreferences = getSharedPreferences("PREFS_KEY", Context.MODE_PRIVATE)
        setBackground()
        setVariable("EVENT","");
        val logoview = findViewById<ImageView>(R.id.login_page_logo)
        val media = intent.getStringExtra("logo_url");
        if (media !== null) {
            Glide.with(this)
                .load(media)
                .into(logoview)
            if(getStringVariable("LOGIN_APP_NAME") != ""){
                logoview.contentDescription = getStringVariable("LOGIN_APP_NAME")
            }

        } else {
            val intent = Intent(this@Login, Error::class.java);
            startActivity(intent);
        }

        var remCheckBox = findViewById<CheckBox>(R.id.remember_me);
        var remember_me = getStringVariable("REMEMBER_ME");
        if(remember_me == "true"){
            var username = getStringVariable("REMEMBER_ME_USERNAME");
            remCheckBox.isChecked = true;
            val usernameInput = findViewById<EditText>(R.id.login_user_name);
            usernameInput.setText(username);
        }

        val reconnectButton = findViewById<LinearLayout>(R.id.btn_reconnect_event_link)
        if(getStringVariable("CONTAINER_APP_ENABLED") == "true" && getStringVariable("PROGRAM_ID") != ""){
            reconnectButton.setVisibility(View.VISIBLE)
            reconnectButton.setOnClickListener {
                val intent = Intent(this@Login, ContainerLogin::class.java);
                intent.putExtra("logo_url", getStringVariable("LOGIN_IMG_URL"));
                intent.putExtra("initial_login", true);
                startActivity(intent);
            }
        }else{
           reconnectButton.setVisibility(View.GONE)
        }

        if(getStringVariable("CHECK_DEPOSIT_MANAGER_STAFF_LOGIN_ENABLED") == "true"){
            findViewById<LinearLayout>(R.id.staff_login_button).visibility = View.VISIBLE
        }

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/urls/get/android/").plus(getDeviceType());
        var request = Request.Builder().url(url)
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    val responseString = response.body?.string();
                    val listTypeJson: HashMap<String, String> = HashMap()
                    try {
                        JsonHelper().getFlattenedHashmapFromJsonForLocalization(
                            "",
                            ObjectMapper().readTree(responseString),
                            listTypeJson
                        )
                        setVariable("PLAY_STORE_PRIMARY_URL", "");
                        setVariable("PLAY_STORE_SECONDARY_URL", "");
                        setVariable("AHA_SUPPORT_CHAT_URL","")

                        println("LIST TYPE URLS: ")
                        println(listTypeJson)

                        listTypeJson.forEach {
                            newStringsMap[it.key] = it.value
                            if(it.key == "mobile_register"){
                                register_link = it.value;
                            }
                            else if(it.key == "register_now"){
                                setVariable("REGISTER_NOW_URL", it.value);
                            }else if(it.key == "mobile_register_multiple"){
                                setVariable("MOBILE_REGISTER_MULTIPLE_URL", it.value);
                            }
                            else if(it.key == "privacy_policy_login"){
                                setVariable("PRIVACY_POLICY_LOGIN_URL", it.value);
                            }
                            else if (it.key == "donations_page"){
                                setVariable("DONATIONS_URL", it.value);
                            }
                            else if (it.key == "mobile_help"){
                                var help_link = it.value;
                                setVariable("HELP_LINK", help_link);
                                val helpButton = findViewById<TextView>(R.id.btn_help_link);
                                runOnUiThread {
                                    helpButton.setOnClickListener {
                                        val support_chat_enabled  = getStringVariable("AHA_SUPPORT_CHAT_ENABLED")
                                        val support_chat_url  = getStringVariable("AHA_SUPPORT_CHAT_URL")
                                        if(support_chat_enabled == "true" && support_chat_url != ""){
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(support_chat_url))
                                            startActivity(browserIntent)
                                        }else{
                                            needHelpClicked(getStringVariable("MOBILE_CLIENT_HELP_LOGIN_URL"), "login")
                                        }
                                    }
                                    
                                }
                            }
                            else if (it.key == "aha_support_chat"){
                                setVariable("AHA_SUPPORT_CHAT_URL",it.value)
                            }
                            else if(it.key == "mobile_forgot_password"){
                                var password_link = it.value;
                                val passwordButton = findViewById<LinearLayout>(R.id.btn_forgot_password_link);
                                runOnUiThread {
                                    val content = SpannableString(getResources().getString(R.string.mobile_login_forgot_password))
                                    content.setSpan(UnderlineSpan(), 0, content.length, 0)
                                    (findViewById<LinearLayout>(R.id.btn_forgot_password_link).getChildAt(0) as TextView).setText(content)
                                    (findViewById<LinearLayout>(R.id.btn_forgot_password_link).getChildAt(0) as TextView).setContentDescription(getString(R.string.mobile_login_forgot_password_description))    
                                    passwordButton.setOnClickListener {
                                        val browserIntent =
                                            Intent(Intent.ACTION_VIEW, Uri.parse(password_link))
                                        startActivity(browserIntent)
                                    }
                                }
                            }else if (it.key == "install_url_google_primary"){
                                setVariable("PLAY_STORE_PRIMARY_URL", it.value);
                            }else if (it.key == "install_url_google_secondary"){
                                setVariable("PLAY_STORE_SECONDARY_URL", it.value);
                            }else if (it.key == "mobile_client_help"){
                                setVariable("MOBILE_CLIENT_HELP_LOGIN_URL",it.value)
                            }else if (it.key == "check_deposit_manager_staff_login"){
                                var login_link = it.value;
                                setVariable("STAFF_LOGIN_LINK", login_link);
                                val staffLoginButton = findViewById<TextView>(R.id.btn_staff_login);
                                runOnUiThread {
                                    staffLoginButton.setOnClickListener {
                                        if(login_link != ""){
                                            val redirect_url_val = getStringVariable("CHECK_DEPOSIT_MANAGER_STAFF_LOGIN_REDIRECT_URI")
                                            setVariable("CDM_STAFF_LOGIN_REDIRECT_URI", redirect_url_val)
                                            val updated_link = login_link.replace("<redirect_uri>", redirect_url_val)
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updated_link))
                                            startActivity(browserIntent)
                                        }
                                    }
                                }
                            }
                        }

                        val registerButton = findViewById<TextView>(R.id.btn_registration_link);
                        val registerContainer = findViewById<LinearLayout>(R.id.btn_registration_container)
                        runOnUiThread {
                            if( getStringVariable("REGISTER_NOW_ENABLED") == "true" && getStringVariable("REGISTER_NOW_LOGIN_ENABLED") == "true"){
                                sendGoogleAnalytics("register_now","standard_login")
                                registerButton.setOnClickListener {
                                    displayAlert("registerNow","")
                                    setAlertSender(registerButton)
                                }
                            }else{
                                if(getStringVariable("REGISTER_MULTIPLE_STUDENTS_ENABLED") == "true" && getStringVariable("MOBILE_REGISTER_MULTIPLE_URL") != ""){
                                    findViewById<LinearLayout>(R.id.btn_registration_container).visibility = View.GONE
                                    findViewById<TextView>(R.id.login_subheading).visibility = View.GONE
                                    findViewById<LinearLayout>(R.id.multiple_registration_subheading_container).visibility = View.VISIBLE

                                    findViewById<LinearLayout>(R.id.btn_multiple_registration_container).visibility = View.VISIBLE

                                    findViewById<TextView>(R.id.subheading_btn_registration_link).setOnClickListener {
                                        val browserIntent =
                                            Intent(Intent.ACTION_VIEW, Uri.parse(register_link))
                                        startActivity(browserIntent)
                                    }

                                    findViewById<TextView>(R.id.btn_multiple_registration_link).setOnClickListener {
                                        val browserIntent =
                                            Intent(Intent.ACTION_VIEW, Uri.parse(getStringVariable("MOBILE_REGISTER_MULTIPLE_URL")))
                                        startActivity(browserIntent)
                                    }
                                }else{
                                    findViewById<LinearLayout>(R.id.btn_registration_container).visibility = View.VISIBLE
                                    findViewById<TextView>(R.id.login_subheading).visibility = View.VISIBLE
                                    findViewById<LinearLayout>(R.id.multiple_registration_subheading_container).visibility = View.GONE
                                    findViewById<LinearLayout>(R.id.btn_multiple_registration_container).visibility = View.GONE

                                    if(register_link == ""){
                                        registerContainer.setVisibility(View.GONE)
                                    }else{
                                        sendGoogleAnalytics("mobile_register","standard_login")
                                        registerContainer.setVisibility(View.VISIBLE)
                                        registerButton.setOnClickListener {
                                            val browserIntent =
                                                Intent(Intent.ACTION_VIEW, Uri.parse(register_link))
                                            startActivity(browserIntent)
                                        }
                                    }
                                }
                            }

                        }
                         val privacyPolicyButton = findViewById<TextView>(R.id.btn_privacy_policy_link)
                        val privacyUnderline = findViewById<View>(R.id.btn_privacy_policy_link_underline)
                        val privacyPolicyUrl = getStringVariable("PRIVACY_POLICY_LOGIN_URL")

                        runOnUiThread {
                            if (privacyPolicyUrl != "") {
                                privacyPolicyButton.visibility = View.VISIBLE
                                privacyUnderline .visibility = View.VISIBLE
                                privacyPolicyButton.setOnClickListener {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
                                    startActivity(browserIntent)
                                }
                            } else {
                                privacyPolicyButton.visibility = View.GONE
                                privacyUnderline.visibility = View.GONE
                            }
                        }

                    } catch (exception: IOException) {
                        println("LOGIN ONCREATE ERROR")
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
            }
        })

        var isPasswordVisible = false

        fun togglePasswordView() {
            val passwordField = findViewById<EditText>(R.id.login_password)
            val toggleButton = findViewById<ImageView>(R.id.password_toggle)



            toggleButton.setOnClickListener {
                isPasswordVisible = !isPasswordVisible
                if (isPasswordVisible) {
                    // Show password
                    passwordField.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    toggleButton.setImageResource(R.drawable.eye_password_show) // Change to eye-open icon
                } else {
                    // Hide password
                    passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    toggleButton.setImageResource(R.drawable.eye_password_hide) // Change to eye-closed icon
                }
                // Move cursor to the end
                passwordField.setSelection(passwordField.text.length)
            }
        }

        togglePasswordView()

        executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this@Login, executor, object : BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                //auth error, stop tasks that requires auth
                println(errorCode)
                if(errorCode == 11){
                    displayAlert(getResources().getString(R.string.mobile_login_biometric_failed_no_fingerprints),"login")
                }else{
                    displayAlert(getResources().getString(R.string.mobile_login_biometric_failed),"login")
                }
                println("Authentication Error: $errString")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                var username = findViewById<EditText>(R.id.login_user_name).text.toString();
                var password = findViewById<EditText>(R.id.login_password).text.toString();
                if(getBiometricString("BIOMETRIC_LOGIN_ENABLED") == "enabled"){
                    username = getBiometricString("BIOMETRIC_USERNAME")
                    password = getBiometricString("BIOMETRIC_PASSWORD")
                    if(username == ""){
                        username = findViewById<EditText>(R.id.login_user_name).text.toString();
                    }
                    if(password == ""){
                        password = findViewById<EditText>(R.id.login_password).text.toString();
                    }
                    
                }else{
                    setVariable(getBiometricStringName("BIOMETRIC_USERNAME"),username)
                    setVariable(getBiometricStringName("BIOMETRIC_PASSWORD"),password)
                    setVariable(getBiometricStringName("BIOMETRIC_LOGIN_ENABLED"),"enabled")
                }
                login(username,password)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        })

        //set properties like title and description on auth dialog
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Login using fingerprint authentication")
            .setConfirmationRequired(true)
            .setDeviceCredentialAllowed(true)
            .build()

        val biometricLoginButton = findViewById<LinearLayout>(R.id.btn_biometric_login);
        if(checkForBiometrics()){
            biometricLoginButton.setVisibility(View.VISIBLE)
            biometricLoginButton.setOnClickListener {
                launchBiometricLogin()
            }
        }else{
            biometricLoginButton.setVisibility(View.GONE)
        }

        val loginbutton = findViewById<Button>(R.id.btn_login);
        loginbutton.setOnClickListener {
            val username = findViewById<EditText>(R.id.login_user_name).text.toString();
            val password = findViewById<EditText>(R.id.login_password).text.toString();
            login(username, password)
        }

        //BEGIN_GAMES_CONTENT
        //END_GAMES_CONTENT

        setVariable("INITIAL_LAUNCH","")
        getNotificationPermission()
        getPoweredByLogo();
        getLogoVO();   
    }

    fun getLogoVO(){
        var vo_string = getStringVariable("LOGIN_APP_NAME") + "logo";
        if(getStringVariable("LOGIN_LOGO_VOICEOVER") != ""){
            vo_string = getStringVariable("LOGIN_LOGO_VOICEOVER");
        }
        findViewById<ImageView>(R.id.login_page_logo).contentDescription = vo_string
    }

    fun getPoweredByLogo(){
        val logo_view = findViewById<ImageView>(R.id.additional_powered_by_logo)
        if(getStringVariable("LOGO_POWERED_BY_URL") != ""){
            Glide.with(this)
                .load(getStringVariable("LOGO_POWERED_BY_URL"))
                .into(logo_view);
            logo_view.visibility = View.VISIBLE
        }else{
            logo_view.visibility = View.GONE
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 12040){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                loadUnityContent("poster")
            }
            else
            {
                displayAlert(getString(R.string.mobile_login_ar_camera_permissions_error),"login")
            }
        }
    }

    fun setBackground(){
        val background = findViewById<ImageView>(R.id.page_background)
        if(getStringVariable("LOGIN_BACKGROUND_IMAGE_ENABLED") == "true"){
            val media = getStringVariable("LOGIN_BACKGROUND_IMAGE")
                val requestOptions: RequestOptions = RequestOptions
                    .diskCacheStrategyOf(DiskCacheStrategy.ALL)
                if (media !== null) {
                    Glide.with(this)
                        .load(media)
                        .apply(requestOptions)
                        .skipMemoryCache(false)
                        .into(background)
                } else {
                    background.setImageResource(android.R.color.transparent);
                    background.setBackgroundColor(Color.WHITE)
                }
        }
    }

    fun getNotificationPermission(){
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                println("Push notifications enabled")
            } else {
                println("Please enable the push notification permission from the settings")
            }
            if(!checkForUpdate()){
                if(intent.getBooleanExtra("initial_login", false)){
                    if(getBiometricString("BIOMETRIC_LOGIN_ENABLED") == "enabled") {
                        launchBiometricLogin();
                    }
                }
            }
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if(intent.getBooleanExtra("initial_login", false)){
                if(!checkForUpdate()){
                    if(getBiometricString("BIOMETRIC_LOGIN_ENABLED") == "enabled") {
                        launchBiometricLogin();
                    }
                }
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val buildCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME
        findViewById<TextView>(R.id.build_version_info).setText(getResources().getString(R.string.mobile_login_version)+ " " + versionName + " " + getResources().getString(R.string.mobile_login_build) + " " + buildCode);
    }

    fun login(username: String, password: String){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/user/login")
            if(username == "" || password == ""){
                displayAlert(getResources().getString(R.string.mobile_login_missing_credentials), "login");
                setAlertSender(findViewById(R.id.btn_login))
            } else{
                val formBody = FormBody.Builder().add("username", username).add("password", password)
                    .build()
                    
                val request = Request.Builder().url(url)
                    .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                    .post(formBody)
                    .build()

                val client = OkHttpClient();
                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        val jsonString = response.body?.string();
                        if (jsonString is String) {
                            val obj = JSONObject(jsonString)
                            if(response.code == 401){
                                if (obj.get("locked") == true || obj.get("locked") == "true") {
                                    displayAlert(getResources().getString(R.string.mobile_login_locked),"login")
                                    setAlertSender(findViewById(R.id.btn_login))
                                }else{
                                    displayAlert(getResources().getString(R.string.mobile_login_failed),"login")
                                    setAlertSender(findViewById(R.id.btn_login))
                                    getBiometricString("CLEAR")

                                }
                            }else{
                                if(obj.has("statusCode")){
                                    if (obj.get("statusCode") == 200) {
                                        setVariable("INITIAL_DISRUPTION_SCREEN","true")
                                        var data = JSONObject(obj.get("data").toString());
                                        if(data.has("jwt")){
                                            setAuth(data.get("jwt") as String);
                                        }else{
                                            setAuth("");
                                        }

                                        if(data.has("check_event_manager") && (data.get("check_event_manager") == true)){
                                            setVariable("CHECK_EVENT_MANAGER","true");
                                        }else{
                                            setVariable("CHECK_EVENT_MANAGER","");
                                        }

                                        if(data.has("check_event_manager_event_id") && data.get("check_event_manager_event_id") is String){
                                            setVariable("CHECK_EVENT_MANAGER_EVENT_ID", data.get("check_event_manager_event_id") as String);
                                        }else{
                                            setVariable("CHECK_EVENT_MANAGER_EVENT_ID","");
                                        }

                                        if(data.has("cons_id") && data.get("cons_id") is String){
                                            setConsID(data.get("cons_id") as String);
                                        }else{
                                            if((getStringVariable("CLIENT_CLASS") == "crowdchange" || getStringVariable("CLIENT_CLASS") == "donordrive" || getStringVariable("CLIENT_CLASS") == "onecause" || getStringVariable("CLIENT_CLASS") == "neonone") && data.has("user_id")){
                                                if(data.get("user_id") is Int){
                                                    setConsID((data.get("user_id") as Int).toString())
                                                }else{
                                                    setConsID((data.get("user_id") as String))
                                                }
                                            }else{
                                                setConsID("")
                                            }
                                        }
                                        
                                        val remCheckBox = findViewById<CheckBox>(R.id.remember_me)
                                        if (remCheckBox.isChecked) {
                                            setVariable("REMEMBER_ME", "true");
                                            setVariable("REMEMBER_ME_USERNAME", username)
                                        } else {
                                            setVariable("REMEMBER_ME", "false");
                                            setVariable("REMEMBER_ME_USERNAME", "")
                                        }
                                        loadEvents("")
                                    } else {
                                        displayAlert(getResources().getString(R.string.mobile_login_failed),"login");
                                        getBiometricString("CLEAR")
                                        setAlertSender(findViewById(R.id.btn_login))
                                    }
                                    }
                                }
                        }else {
                            displayAlert(getResources().getString(R.string.mobile_login_failed),"login")
                            setAlertSender(findViewById(R.id.btn_login))
                        }   
                    }
                    override fun onFailure(call: Call, e: IOException) {
                        println("ERROR")
                        println(e.message.toString())
                        displayAlert(getResources().getString(R.string.mobile_login_failed),"login")
                        setAlertSender(findViewById(R.id.btn_login))
                    }
                })
            }
    }
    fun launchBiometricLogin(){
        val username = findViewById<EditText>(R.id.login_user_name).text.toString()
        val password = findViewById<EditText>(R.id.login_password).text.toString()
        if(getBiometricString("BIOMETRIC_LOGIN_ENABLED") == "enabled"){
            val biometric_username = getBiometricString("BIOMETRIC_USERNAME")
            val biometric_password = getBiometricString("BIOMETRIC_PASSWORD")

            if(biometric_username.isNotEmpty() && biometric_password.isNotEmpty()) {
                biometricPrompt.authenticate(promptInfo)
            } else {
                displayAlert(getResources().getString(R.string.mobile_login_biometric_failed), "login")
                setAlertSender(findViewById(R.id.btn_login))
            }
        } else {
            if(username == "" || username == null || password == "" || password == null){
                displayAlert(getResources().getString(R.string.mobile_login_biometric_missing_credentials), "login")
                setAlertSender(findViewById(R.id.btn_biometric_login))
            } else {
                biometricPrompt.authenticate(promptInfo)
            }
        }
    }

    override fun loadEvents(source: String){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getevents/").plus(getConsID())
        var request = Request.Builder()
            .url(url)
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    val jsonString = response.body?.string();
                    try{
                        val jsonArray = JSONArray(jsonString)
                        var eventArray = emptyList<Event>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            var event_id = "";
                            var event_name = "";
                            var event_date = "";
                            var event_cons_id = "";

                            if(obj.has("event_id")){
                                event_id = obj.get("event_id").toString()
                            }

                            if(obj.has("event_name")){
                                event_name = obj.get("event_name").toString()
                            }

                            if(obj.has("event_date")){
                                event_date = obj.get("event_date").toString()
                            }

                            if(obj.has("cons_id") && obj.get("cons_id") is Int){
                                event_cons_id = (obj.get("cons_id") as Int).toString()
                            }

                            val event = Event(event_id, event_name, event_date, event_cons_id)
                            eventArray += event;
                        }
                        setEvents(eventArray);

                        clearVariable("IS_EVENT_MANAGER_ONLY")
                        clearVariable("IS_TEAM_CAPTAIN")
                        
                        if(eventArray.size > 1){
                            setVariable("SWITCH_EVENTS_SOURCE","login")
                            displayAlert("switchEvents", "initial");
                        }else if (eventArray.size == 1){
                            setEvent(eventArray[0]);
                        }else{
                            if(getStringVariable("CHECK_EVENT_MANAGER") == "true" && getStringVariable("CHECK_EVENT_MANAGER_EVENT_ID") != ""){
                                setVariable("IS_EVENT_MANAGER_ONLY", "true")  
                                setEventEventManager(getStringVariable("CHECK_EVENT_MANAGER_EVENT_ID"))
                            }else{
                                if(getStringVariable("REGISTER_NOW_ENABLED") == "true"){
                                    if(source != "register_now"){
                                        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getPreviousEvent/").plus(getConsID())

                                        var request = Request.Builder().url(url)
                                            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                                            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                                            .build()

                                        var client = OkHttpClient();
                                        client.newCall(request).enqueue(object : Callback {
                                            override fun onResponse(call: Call, response: okhttp3.Response) {
                                                val jsonString = response.body?.string();
                                                try {
                                                    val obj = JSONObject(jsonString);
                                                    if(obj.get("register_now_event").toString() == "false"){
                                                        if(getStringVariable("REGISTER_NOW_FIND_EVENT_ENABLED") != "true"){
                                                            displayAlert(getResources().getString(R.string.mobile_login_no_events),"login");
                                                        }else{
                                                            displayAlert("registerNow","no_event")
                                                        }
                                                    }else{
                                                        displayAlert("registerNowRecommendedEvent",arrayOf((obj.get("event_id") as Int).toString(), obj.get("event_name") as String))
                                                    }
                                                }catch(e: Exception){
                                                    println("Get Previous Event Error")
                                                }
                                            }

                                            override fun onFailure(call: Call, e: IOException) {
                                                println(e.message.toString())
                                            }
                                        })
                                    }else{
                                        displayAlert(getResources().getString(R.string.mobile_login_register_now_wrong_account),"login");
                                    }   
                                }else{
                                    displayAlert(getResources().getString(R.string.mobile_login_no_events),"login");
                                }
                            }
                        }
                    } catch(exception:IOException){
                        displayAlert(getResources().getString(R.string.mobile_error_unavailable),"login");
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                displayAlert(getResources().getString(R.string.mobile_login_no_events),"login");
                setAlertSender(findViewById(R.id.btn_biometric_login))
            }
        })
    }

    fun checkForBiometrics() : Boolean{
        var returnValue = false;
        val biometricManager = androidx.biometric.BiometricManager.from(this)
        when (biometricManager.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS ->
            returnValue = true
        }
        return(returnValue)
    }
}

class Foo(json: String) : JSONObject(json) {
    val id = this.optInt("id")
    val title: String? = this.optString("title")
}