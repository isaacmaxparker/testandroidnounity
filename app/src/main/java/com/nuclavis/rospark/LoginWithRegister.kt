package com.nuclavis.rospark

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.amangarg.localizationdemo.JsonHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.fasterxml.jackson.databind.ObjectMapper
import com.nuclavis.rospark.databinding.LoginBinding
import com.nuclavis.rospark.databinding.LoginWithRegisterBinding
import okhttp3.*
import okio.IOException
import org.json.JSONObject
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.collections.HashMap

class LoginWithRegister : BaseLanguageActivity() {
    var logo_url = "";
    var initial = "";

    override fun childviewCallback(string: String, data:String) {}
    override fun slideButtonCallback(card: Any, forward:Boolean){}
    override fun onCreate(savedInstanceState: Bundle?) {
        setVariable("EVENT_THEME",R.style.StandardTheme.toString())
        recolorTheme()
        super.onCreate(savedInstanceState)
        val binding: LoginWithRegisterBinding = DataBindingUtil.setContentView(
            this, R.layout.login_with_register)
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

        setBackground()

        val text = makeLinks(getString(R.string.mobile_login_internal_waiver_prefix_android),getString(R.string.mobile_login_internal_waiver_android),
            Color.parseColor(getStringVariable("PRIMARY_COLOR")),{
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getStringVariable("WAIVER_URL")))
                startActivity(browserIntent)
            }
        );

        findViewById<TextView>(R.id.waiver_cb_label).setText(text);
        findViewById<TextView>(R.id.waiver_cb_label).setOnClickListener{
            if(getStringVariable("WAIVER_URL") != ""){
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getStringVariable("WAIVER_URL")))
                startActivity(browserIntent)
            }
        }

        val reconnectButton = findViewById<LinearLayout>(R.id.btn_reconnect_event_link)
        if(getStringVariable("CONTAINER_APP_ENABLED") == "true" && getStringVariable("PROGRAM_ID") != ""){
            reconnectButton.setVisibility(View.VISIBLE)
            reconnectButton.setOnClickListener {
                val intent = Intent(this@LoginWithRegister, ContainerLogin::class.java);
                intent.putExtra("logo_url", getStringVariable("LOGIN_IMG_URL"));
                intent.putExtra("initial_login", true);
                startActivity(intent);
            }
        }else{
           reconnectButton.setVisibility(View.GONE)
        }

        hideAlert()
        val logoview = findViewById<ImageView>(R.id.login_page_logo)
        var media = getStringVariable("REGISTER_LOGO_URL")
        if(media == ""){
            media = getStringVariable("LOGO_URL_CONTAINER");
        }

        if (media !== null) {
            Glide.with(this)
                .load(media)
                .into(logoview)
            if(getStringVariable("LOGIN_APP_NAME") != ""){
                logoview.contentDescription = getStringVariable("LOGIN_APP_NAME")
            }
        } else {
            val intent = Intent(this@LoginWithRegister, Error::class.java);
            startActivity(intent);
        }

        loadUrls(false, true);
        updateView();

        getNotificationPermission()

        val registerbutton = findViewById<Button>(R.id.btn_register_sign_in);
        registerbutton.setOnClickListener {
            val emailInput = findViewById<EditText>(R.id.login_email);
            val nameInput = findViewById<EditText>(R.id.login_name)
            val name = nameInput.getText().toString()
            val email = emailInput.getText().toString()
            val checkbox = findViewById<CheckBox>(R.id.waiver_cb)
            val waiver = checkbox.isChecked
            if(email != "" && name != "" && waiver){
                getCode(email, name, registerbutton);
            }else{
                displayAlert(getString(R.string.mobile_login_internal_missing_credentials))
            }
        }

        val loginbutton = findViewById<Button>(R.id.btn_login_code);
        loginbutton.setOnClickListener {
            val emailInput = findViewById<EditText>(R.id.login_code_email);
            var email = emailInput.getText().toString()
            getCode(email, "", loginbutton);
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
                background.setBackgroundColor(Color.BLACK)
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

    fun getNotificationPermission(){
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                println("Push notifications enabled")
            } else {
                println("Please enable the push notification permission from the settings")
            }
            checkForUpdate()
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) else {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val buildCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME
        findViewById<TextView>(R.id.build_version_info).setText(getResources().getString(R.string.mobile_login_version)+ " " + versionName + " " + getResources().getString(R.string.mobile_login_build) + " " + buildCode);
    }

    fun getCode(emailstring: String, name: String, button: Button){
        var email = emailstring.replace(". ","")
        email = email.trim()
        if(email != "" && isEmailValid(email)){
            val newIntent = Intent(this@LoginWithRegister, LoginInternalCode::class.java);
            newIntent.putExtra("logo_url",logo_url);
            newIntent.putExtra("internal_email",email);
            startActivity(newIntent);

            Executors.newSingleThreadExecutor().execute(Runnable {
                val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/user/email")
                var formBody = FormBody.Builder()
                    .add("email", email);

                if(name != ""){
                    formBody = formBody.add("name",name)
                }

                val built = formBody.build()

                // creating request
                var request = Request.Builder()
                    .url(url)
                    .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                    .post(built)
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
            setAlertSender(button)
        }
    }
}