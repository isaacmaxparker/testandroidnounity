package com.nuclavis.rospark

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.amangarg.localizationdemo.JsonHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.fasterxml.jackson.databind.ObjectMapper
import com.nuclavis.rospark.databinding.LoginBinding
import com.nuclavis.rospark.databinding.LoginCodeBinding
import okhttp3.*
import okio.IOException
import org.json.JSONObject
import java.util.*
import java.util.concurrent.Executor
import kotlin.collections.HashMap

class LoginCode : BaseLanguageActivity() {

    var logo_url = "";
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun childviewCallback(string: String, data:String){}
    override fun slideButtonCallback(card: Any, forward:Boolean){}
    override fun onCreate(savedInstanceState: Bundle?) {
        setVariable("EVENT_THEME",R.style.StandardTheme.toString())
        recolorTheme()
        super.onCreate(savedInstanceState)
        val binding: LoginCodeBinding = DataBindingUtil.setContentView(
            this, R.layout.login_code)
        binding.colorList = getColorList("login")
        binding.loginInputBorder = getStringVariable("LOGIN_TEXTBOX_BORDER_ENABLED") == "true"
        
        hideAlert()
        val logoview = findViewById<ImageView>(R.id.login_page_logo)
        val media = intent.getStringExtra("logo_url")
        if (media != null) {
            Glide.with(this)
                .load(media)
                .into(logoview)

            if(getStringVariable("LOGIN_APP_NAME") != ""){
                logoview.contentDescription = getStringVariable("LOGIN_APP_NAME")
            }
        } else {
            val intent = Intent(this@LoginCode, Error::class.java);
            startActivity(intent);
        }

        loadUrls(false, true);
        updateView();

        val newcodebutton = findViewById<Button>(R.id.btn_new_code);
        newcodebutton.setOnClickListener {
            getBiometricString("CLEAR")
            clearVariable("REMEMBER_ME")
            clearVariable("REMEMBER_ME_USERNAME")
            clearVariable("REMEMBER_ME_PASSWORD")
            val newIntent = Intent(this@LoginCode, LoginNoFields::class.java);
            newIntent.putExtra("logo_url",logo_url);
            startActivity(newIntent);
        }

        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this@LoginCode, executor, object : BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                //auth error, stop tasks that requires auth
                if(errorCode == 11){
                    displayAlert(getResources().getString(R.string.mobile_login_biometric_failed_no_fingerprints),"login")
                }else{
                    displayAlert(getResources().getString(R.string.mobile_login_biometric_failed),"login")
                }

                getBiometricString("CLEAR")
                println("Authentication Error: $errString")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                var email = intent.getStringExtra("classy_email").toString();
                var password = UUID.randomUUID().toString().replace("-","");
                var codeInput = findViewById<EditText>(R.id.login_code);
                var code = codeInput.getText().toString()
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
                codeLogin(email, password, code, false)
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
            val email = intent.getStringExtra("classy_email").toString();
            val password = UUID.randomUUID().toString().replace("-","");
            val codeInput = findViewById<EditText>(R.id.login_code);
            val code = codeInput.getText().toString()
            codeLogin(email, password, code, false)
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
        setBackground()
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
