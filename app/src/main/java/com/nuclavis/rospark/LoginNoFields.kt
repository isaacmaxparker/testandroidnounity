package com.nuclavis.rospark

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.nuclavis.rospark.databinding.LoginNoFieldsBinding
import okhttp3.*
import okio.IOException
import org.json.JSONObject
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class LoginNoFields : BaseLanguageActivity() {
    var logo_url = "";
    var initial = "";
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun childviewCallback(string: String, data:String) {
        if(string == "updateRecommended"){
            if(intent.getStringExtra("initial") == "true"){
                if(getBiometricString("BIOMETRIC_LOGIN_ENABLED") == "enabled") {
                    launchBiometricLogin();
                }
            }
        }
    }
    override fun slideButtonCallback(card: Any, forward:Boolean){}
    override fun onCreate(savedInstanceState: Bundle?) {
        setVariable("EVENT_THEME",R.style.StandardTheme.toString())
        recolorTheme()
        super.onCreate(savedInstanceState)
        val binding: LoginNoFieldsBinding = DataBindingUtil.setContentView(
            this, R.layout.login_no_fields)
        binding.colorList = getColorList("login")
        binding.loginInputBorder = getStringVariable("LOGIN_TEXTBOX_BORDER_ENABLED") == "true"
        logo_url = intent.getStringExtra("logo_url").toString();
        initial = intent.getStringExtra("initial").toString();
        findViewById<LinearLayout>(R.id.login_page_layout).setVisibility(View.GONE)
        if(initial == "true") {
            if (getStringVariable("REMEMBER_ME") == "true") {
                val username = getStringVariable("REMEMBER_ME_USERNAME");
                val password = getStringVariable("REMEMBER_ME_PASSWORD");
                codeLogin(username, password, "", true)
            }else{
                findViewById<LinearLayout>(R.id.login_page_layout).setVisibility(View.VISIBLE)
            }
        }else{
            findViewById<LinearLayout>(R.id.login_page_layout).setVisibility(View.VISIBLE)
        }

        val reconnectButton = findViewById<LinearLayout>(R.id.btn_reconnect_event_link)
        if(getStringVariable("CONTAINER_APP_ENABLED") == "true" && getStringVariable("PROGRAM_ID") != ""){
            reconnectButton.setVisibility(View.VISIBLE)
            reconnectButton.setOnClickListener {
                val intent = Intent(this@LoginNoFields, ContainerLogin::class.java);
                intent.putExtra("logo_url", getStringVariable("LOGIN_IMG_URL"));
                intent.putExtra("initial_login", true);
                startActivity(intent);
            }
        }else{
           reconnectButton.setVisibility(View.GONE)
        }

        hideAlert()
        val logoview = findViewById<ImageView>(R.id.login_page_logo)
        val media = logo_url;
        if (media !== null) {
            Glide.with(this)
                .load(media)
                .into(logoview)
            if(getStringVariable("LOGIN_APP_NAME") != ""){
                logoview.contentDescription = getStringVariable("LOGIN_APP_NAME")
            }
        } else {
            val intent = Intent(this@LoginNoFields, Error::class.java);
            startActivity(intent);
        }

        setBackground()
        loadUrls(true, true);
        updateView();

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this@LoginNoFields, executor, object : BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                getBiometricString("CLEAR")
                //auth error, stop tasks that requires auth
                if(errorCode == 11){
                    displayAlert(getResources().getString(R.string.mobile_login_biometric_failed_no_fingerprints),"login")
                }else{
                    displayAlert(getResources().getString(R.string.mobile_login_biometric_failed),"login")
                }
                println("Authentication Error: $errString")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                var email = "";
                var password = "";
                if(getBiometricString("BIOMETRIC_LOGIN_ENABLED") == "enabled"){
                    email = getBiometricString("BIOMETRIC_USERNAME")
                    password = getBiometricString("BIOMETRIC_PASSWORD")
                    if(email == ""){
                        email = intent.getStringExtra("classy_email").toString();
                    }
                    if(password == ""){
                        password = UUID.randomUUID().toString().replace("-","");
                    }
                }else{
                    setVariable(getBiometricStringName("BIOMETRIC_USERNAME"),email)
                    setVariable(getBiometricStringName("BIOMETRIC_PASSWORD"),password)
                    setVariable(getBiometricStringName("BIOMETRIC_LOGIN_ENABLED"),"enabled")
                }
                codeLogin(email, password, "", true)
            }

            override fun onAuthenticationFailed() {
                println("ERROR FAILED")
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



        getNotificationPermission()

        val loginbutton = findViewById<Button>(R.id.btn_login_code);
        loginbutton.setOnClickListener {
            val emailInput = findViewById<EditText>(R.id.login_email);
            var email = emailInput.getText().toString()
            // Check if email id is valid or not
            email = email.replace(". ","")
            email = email.trim()
            if(email != "" && isEmailValid(email)){
                val newIntent = Intent(this@LoginNoFields, LoginCode::class.java);
                newIntent.putExtra("logo_url",logo_url);
                newIntent.putExtra("classy_email",email);
                startActivity(newIntent);
                Executors.newSingleThreadExecutor().execute(Runnable {
                    val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/user/email")
                    // add parameter
                    val formBody = FormBody.Builder()
                        .add("email", email)
                        .build()

                    // creating request
                    var request = Request.Builder()
                        .url(url)
                        .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                        .post(formBody)
                        .build()

                    var client = OkHttpClient();

                    client.newCall(request).enqueue(object : Callback {
                        override fun onResponse(call: Call, response: Response) {
                            if(response.code == 401){
                                throw Exception(response.body?.string())
                            }else{
                                val response = response.body?.string();
                                val obj = JSONObject(response);
                                println("OBJ: ")
                                println(obj)
                                if(obj.has("statusCode")) {
                                    val status_code = obj.get("statusCode")
                                    if(status_code == 403 || status_code == 500){
                                        println("ERROR")
                                    }else{
                                        hideAlert()
                                    }
                                }
                            }
                        }
                        override fun onFailure(call: Call, e: IOException) {
                            println(e.message.toString())
                            hideAlert()
                        }
                    })
                })
            }else{
                displayAlert(getResources().getString(R.string.mobile_login_missing_email),"login")
                setAlertSender(loginbutton)
            }
        }

        val registerbutton = findViewById<Button>(R.id.btn_registration_link);
        registerbutton.setOnClickListener {
            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder.setMessage("Register Button has been clicked")
                .setCancelable(false)
                .setNegativeButton("Close Alert", DialogInterface.OnClickListener { dialog, id ->
                    dialog.cancel()
                })

            val alert = dialogBuilder.create()
            alert.setTitle("Alert")
            alert.show()
        }

        val helpbutton = findViewById<Button>(R.id.btn_help_link);
        helpbutton.setOnClickListener {
            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder.setMessage("Need Help Link has been clicked")
                .setCancelable(false)
                .setNegativeButton("Close Alert", DialogInterface.OnClickListener { dialog, id ->
                    dialog.cancel()
                })

            val alert = dialogBuilder.create()
            alert.setTitle("Alert")
            alert.show()
        }
        val buildCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME
        findViewById<TextView>(R.id.build_version_info).setText(getResources().getString(R.string.mobile_login_version)+ " " + versionName + " " + getResources().getString(R.string.mobile_login_build) + " " + buildCode);
        getPoweredByLogo()
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

    fun setBackground(){
        val background = findViewById<ImageView>(R.id.page_background);
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
                if(intent.getStringExtra("initial") == "true"){
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
            if(intent.getStringExtra("initial") == "true"){
                if(getBiometricString("BIOMETRIC_LOGIN_ENABLED") == "enabled") {
                    launchBiometricLogin();
                }
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val buildCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME
        findViewById<TextView>(R.id.build_version_info).setText(getResources().getString(R.string.mobile_login_version)+ " " + versionName + " " + getResources().getString(R.string.mobile_login_build) + " " + buildCode);
    }

    fun launchBiometricLogin(){
        if(getBiometricString("BIOMETRIC_LOGIN_ENABLED") == "enabled"){
            val biometric_username = getBiometricString("BIOMETRIC_USERNAME")
            val biometric_password = getBiometricString("BIOMETRIC_PASSWORD")
            if(biometric_username == "" || biometric_password == ""){
                displayAlert(getResources().getString(R.string.mobile_login_biometric_failed),"login")
            }else{
                biometricPrompt.authenticate(promptInfo)
            }
        }else{
            biometricPrompt.authenticate(promptInfo)
        }
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