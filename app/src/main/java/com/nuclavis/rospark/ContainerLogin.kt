package com.nuclavis.rospark
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.URLUtil
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.amangarg.localizationdemo.JsonHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.comm100.livechat.ChatActivity
import com.comm100.livechat.VisitorClientInterface
import com.fasterxml.jackson.databind.ObjectMapper
import com.nuclavis.rospark.databinding.ContainerLoginBinding
import com.nuclavis.rospark.databinding.ContainerLoginDisplayListBinding
import com.nuclavis.rospark.databinding.ContainerLoginDisplayListRowBinding
import okhttp3.*
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.Executor

class ContainerLogin : BaseLanguageActivity() {
    override fun childviewCallback(string: String, data:String) {

    }
    private lateinit var executor: Executor
    private var register_link = ""

    var temp_logo_url = ""
    var logo_media_url = "";
    var second_login_page_logo_url = "";

    override fun slideButtonCallback(card: Any, forward:Boolean){}

    override fun onCreate(savedInstanceState: Bundle?) {
        setVariable("EVENT_THEME",R.style.StandardTheme.toString())
        recolorTheme()
        super.onCreate(savedInstanceState)
        if(getStringVariable("CONTAINER_LOGIN_PRIMARY_COLOR") != ""){
            setVariable("PRIMARY_COLOR",getStringVariable("CONTAINER_LOGIN_PRIMARY_COLOR"))
        }else{
            setVariable("CONTAINER_LOGIN_PRIMARY_COLOR",getStringVariable("PRIMARY_COLOR"))
        }
        setVariable("LOGIN_PRIMARY_COLOR", getStringVariable("CONTAINER_LOGIN_PRIMARY_COLOR"))

        if(getStringVariable("CONTAINER_APP_TYPE") == "DISPLAY_LIST"){
            val binding: ContainerLoginDisplayListBinding = DataBindingUtil.setContentView(this, R.layout.container_login_display_list)
            binding.colorList = getColorList("login")
            hideAlert()
            getEventOptions()
            setBackground()
        }else{
            val binding: ContainerLoginBinding = DataBindingUtil.setContentView(this, R.layout.container_login)
            binding.colorList = getColorList("login")
            hideAlert()
            val logoview = findViewById<ImageView>(R.id.container_login_page_logo)

            val loginbutton = findViewById<Button>(R.id.container_btn_login);
            loginbutton.setOnClickListener {
                val code = findViewById<EditText>(R.id.container_login_code).text.toString();
                loginWithCode(code)
            }

            setVariable("INITIAL_LAUNCH","")
            getNotificationPermission()
            getPreviousEvents()
            getPoweredByLogo()
            getUrls()

            if(getStringVariable("CLIENT_CLASS") != "internal"){
                setBackground()
            }

            if(getStringVariable("CONTAINER_LOGIN_LOGO_IMAGE") == ""){
                logo_media_url = intent.getStringExtra("logo_url").toString();
                setVariable("CONTAINER_LOGIN_LOGO_IMAGE",logo_media_url)
            }else{
                logo_media_url = getStringVariable("CONTAINER_LOGIN_LOGO_IMAGE")
            }

            if (logo_media_url !== null) {
                Glide.with(this)
                    .load(logo_media_url)
                    .into(logoview)
                if(getStringVariable("LOGIN_APP_NAME") != ""){
                    logoview.contentDescription = getStringVariable("LOGIN_APP_NAME")
                }

            } else {
                val intent = Intent(this@ContainerLogin, Error::class.java);
                startActivity(intent);
            }
        }
    }

    fun getEventOptions(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/containerAppPrograms/");
        var request = Request.Builder().url(url)
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    val response = response.body?.string();
                    val obj = JSONObject(response);
                    val parent = findViewById<LinearLayout>(R.id.container_login_display_list_page_layout)
                    if(obj.has("data")){
                        val array = obj.get("data") as JSONArray
                        val inflater = LayoutInflater.from(this@ContainerLogin)
                        for(i in 0 until array.length()) {
                            val event_obj = array[i] as JSONObject;
                            runOnUiThread{
                                val binding2: ContainerLoginDisplayListRowBinding = DataBindingUtil.inflate(
                                    inflater, R.layout.container_login_display_list_row, parent, true)
                                binding2.colorList = getColorList("")
                                val root2 = binding2.root as LinearLayout
                                ((root2.getChildAt(0) as LinearLayout).getChildAt(0) as TextView).text = getSafeStringVariable(event_obj, "container_app_text")

                                val media = getSafeStringVariable(event_obj, "container_app_logo")
                                if (media !== null) {
                                    Glide.with(this@ContainerLogin)
                                        .load(media)
                                        .into(root2.getChildAt(1) as ImageView)
                                }

                                root2.setOnClickListener {
                                    temp_logo_url = media
                                    loginWithCode(getSafeStringVariable(event_obj,"login_code"))
                                }
                            }
                        }

                    }else{
                        val intent = Intent(this@ContainerLogin, Error::class.java);
                        startActivity(intent);
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
            }
        })
    }

    fun getUrls(){
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

                        setVariable("AHA_SUPPORT_CHAT_URL","")

                        listTypeJson.forEach {
                            newStringsMap[it.key] = it.value
                            if (it.key == "mobile_help"){
                                var help_link = it.value;
                                setVariable("HELP_LINK", help_link);
                                val helpButton = findViewById<TextView>(R.id.btn_help_link);
                                runOnUiThread {
                                    helpButton.setOnClickListener {
                                        val help_link = getStringVariable("HELP_LINK")
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
                            } else if (it.key == "aha_support_chat"){
                                setVariable("AHA_SUPPORT_CHAT_URL",it.value)
                            } else if (it.key == "mobile_client_help"){
                                setVariable("MOBILE_CLIENT_HELP_LOGIN_URL",it.value)
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

    fun getPreviousEvents(){
        if(getStringVariable(getStringVariable("CLIENT_CODE") + "_LAST_CONTAINER_APP_CODE") != "" && getStringVariable(getStringVariable("CLIENT_CODE") + "_LAST_CONTAINER_APP_IMAGE") != ""){
            findViewById<LinearLayout>(R.id.previous_events_container).setVisibility(View.VISIBLE)
            findViewById<ImageView>(R.id.previous_event_image).setVisibility(View.VISIBLE)
            val image_view = findViewById<ImageView>(R.id.previous_event_image);
            Glide.with(this)
            .load(getStringVariable(getStringVariable("CLIENT_CODE") + "_LAST_CONTAINER_APP_IMAGE"))
            .into(image_view);

            image_view.setOnClickListener{
                loginWithCode(getStringVariable(getStringVariable("CLIENT_CODE") + "_LAST_CONTAINER_APP_CODE"))
            }
        }
        if(getStringVariable(getStringVariable("CLIENT_CODE") + "_SECOND_LAST_CONTAINER_APP_CODE") != "" && getStringVariable(getStringVariable("CLIENT_CODE") + "_SECOND_LAST_CONTAINER_APP_IMAGE") != ""){
            val image_view = findViewById<ImageView>(R.id.second_previous_event_image);
            findViewById<ImageView>(R.id.second_previous_event_image).setVisibility(View.VISIBLE)
            Glide.with(this)
            .load(getStringVariable(getStringVariable("CLIENT_CODE") + "_SECOND_LAST_CONTAINER_APP_IMAGE"))
            .into(image_view);

            image_view.setOnClickListener{
                loginWithCode(getStringVariable(getStringVariable("CLIENT_CODE") + "_SECOND_LAST_CONTAINER_APP_CODE"))
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
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
        } else {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val buildCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME
        findViewById<TextView>(R.id.build_version_info).setText(getResources().getString(R.string.mobile_container_login_version)+ " " + versionName + " " + getResources().getString(R.string.mobile_container_login_build) + " " + buildCode);
    }

    fun loginWithCode(code: String){
        setVariable("PROGRAM_ID","")
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/program/").plus(code.replace(getStringVariable("CLIENT_CODE") + "_", ""))
        if(code == ""){
            displayAlert(getResources().getString(R.string.mobile_container_login_missing_code), "login");
            setAlertSender(findViewById(R.id.container_btn_login))
        } else{
            var request = Request.Builder().url(url)
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()
            var client = OkHttpClient();
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val jsonString = response.body?.string();
                    if(response.code != 200){
                        displayAlert(getResources().getString(R.string.mobile_container_login_invalid_code),"login")
                        setAlertSender(findViewById(R.id.container_btn_login))
                    }else{
                        if(jsonString is String) {
                            val obj = JSONObject(jsonString)
                            var logo_url = intent.getStringExtra("logo_url");
                            if(obj.has("program_id") && obj.get("program_id") is Int){
                                val client_class = getStringVariable("CLIENT_CLASS")
                                setVariable("PROGRAM_ID",(obj.get("program_id") as Int).toString())
                                getServerConfigs(code.uppercase())
                            }
                        }else {
                            displayAlert(getResources().getString(R.string.mobile_container_login_invalid_code),"login")
                            setAlertSender(findViewById(R.id.container_btn_login))
                        }
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    println("ERROR")
                    println(e.message.toString())
                    displayAlert(getResources().getString(R.string.mobile_container_login_invalid_code),"login")
                    setAlertSender(findViewById(R.id.container_btn_login))
                }
            })
        }
    }

    fun getServerConfigs(code:String) {
        setVariable("BIOMETRIC_APP_CODE",code)
        var new_logo_url = "";
        var client_class = "";
        var facebook_fundraiser_enabled = "";

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
                    clearVariable("AHA_LOGIN_CTA_ENABLED")
                    clearVariable("DISRUPTIONS_ENABLED")
                    clearVariable("LOGIN_APP_NAME")
                    clearVariable("SECOND_PAGE_LOGO_URL")
                    clearVariable("LOGIN_lOGO_VOICEOVER")

                    for (i in 0..json.length() - 1) {
                        try {
                            var obj = json.getJSONObject(i);
                            setVariable("HIDE_EVENT_COUNTDOWN", "false")
                            if (obj.has("config_name") && obj.has("config_value")) {
                                var key = obj.get("config_name") as String;
                                var value = obj.get("config_value") as String;
                                if (key == "logo_url") {
                                    setVariable("REGISTER_LOGO_URL",value)
                                    setVariable("SECOND_PAGE_LOGO_URL", value)
                                    second_login_page_logo_url = value
                                } else if (key == "logo_url_container") {
                                    new_logo_url = value;
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
                                    primary_color = value.trim().lowercase();
                                    setVariable("PRIMARY_COLOR",primary_color)
                                }else if (key == "background_image_enabled"){
                                    setVariable("LOGIN_BACKGROUND_IMAGE_ENABLED",value)
                                }else if (key == "background_image"){
                                    setVariable("LOGIN_BACKGROUND_IMAGE",value)
                                }else if (key == "register_now_enabled"){
                                    setVariable("REGISTER_NOW_ENABLED",value)
                                }else if (key == "aha_login_cta_enabled"){
                                    setVariable("AHA_LOGIN_CTA_ENABLED",value)
                                }else if (key == "disruptions_enabled"){
                                    setVariable("DISRUPTIONS_ENABLED",value)
                                }else if (key == "app_name"){
                                    setVariable("LOGIN_APP_NAME",value)
                                }else if (key == "login_voiceover"){
                                    setVariable("LOGIN_lOGO_VOICEOVER",value)
                                }else{
                                    setVariable(key.uppercase(), value)
                                }
                            }
                        } catch (exception: IOException) {
                            println("GET SERVER CONFIGS ERROR")
                        }
                    }

                    if(getStringVariable("LOGIN_BUTTON_TEXT_COLOR") == ""){
                        setVariable("LOGIN_BUTTON_TEXT_COLOR","#ffffff")
                    }
                    if(getStringVariable("LOGIN_BUTTON_COLOR") == ""){
                        if(primary_color != ""){
                            setVariable("LOGIN_BUTTON_COLOR",primary_color)
                        }else{
                            var stored_primary_color = Integer.toHexString(getResources().getColor(R.color.primary_color)).substring(2);
                            setVariable("LOGIN_BUTTON_COLOR","#" + stored_primary_color)
                        }
                    }

                    if(new_logo_url == ""){
                        new_logo_url = temp_logo_url
                    }

                    if (URLUtil.isValidUrl(new_logo_url)) {
                        setVariable("LOGIN_IMG_URL", new_logo_url);
                        setVariable("CLIENT_CLASS", client_class);
                        setVariable("FACEBOOK_FUNDRAISER_ENABLED", facebook_fundraiser_enabled);
                        getStringsUpdated({stringsCallback(new_logo_url,code.uppercase())})
                    } else {
                        val intent = Intent(this@ContainerLogin, Error::class.java);
                         startActivity(intent);
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                val intent = Intent(this@ContainerLogin, Error::class.java);
                startActivity(intent);
            }
        })
    }

    fun stringsCallback(logo_url: String, code: String){
        val last_container_app_code = getStringVariable(getStringVariable("CLIENT_CODE") + "_LAST_CONTAINER_APP_CODE")
        val last_container_app_image = getStringVariable(getStringVariable("CLIENT_CODE") + "_LAST_CONTAINER_APP_IMAGE")
        val second_last_container_app_code = getStringVariable(getStringVariable("CLIENT_CODE") + "_SECOND_LAST_CONTAINER_APP_CODE")
        val second_last_container_app_image = getStringVariable(getStringVariable("CLIENT_CODE") + "_SECOND_LAST_CONTAINER_APP_IMAGE") 

        if((last_container_app_code == code && last_container_app_image == logo_url) || (second_last_container_app_code == code && second_last_container_app_image == logo_url)){
            //println("Duplicate Last Event")
        }else{
            if(getStringVariable(getStringVariable("CLIENT_CODE") + "_LAST_CONTAINER_APP_IMAGE") != ""){
                val old_last_image = getStringVariable(getStringVariable("CLIENT_CODE") + "_LAST_CONTAINER_APP_IMAGE")
                val old_last_code = getStringVariable(getStringVariable("CLIENT_CODE") + "_LAST_CONTAINER_APP_CODE")
                if(old_last_image != logo_url || old_last_code != code){
                    setVariable((getStringVariable("CLIENT_CODE") + "_SECOND_LAST_CONTAINER_APP_IMAGE"), old_last_image)
                    setVariable((getStringVariable("CLIENT_CODE") + "_SECOND_LAST_CONTAINER_APP_CODE"), old_last_code)
                }
            }

            setVariable(getStringVariable("CLIENT_CODE") + "_LAST_CONTAINER_APP_IMAGE", logo_url)
            setVariable(getStringVariable("CLIENT_CODE") + "_LAST_CONTAINER_APP_CODE", code)
        }

        val client_class = getStringVariable("CLIENT_CLASS")

        var login_logo_url = logo_url
        if(getStringVariable("CONTAINER_APP_TYPE") == "DISPLAY_LIST"){
            login_logo_url = second_login_page_logo_url
            if (login_logo_url == "") {
                login_logo_url = getStringVariable("LOGIN_IMG_URL");
            }
        }

        if (client_class == "classy") {
            val intent = Intent(this@ContainerLogin, LoginNoFields::class.java);
            intent.putExtra("logo_url", login_logo_url);
            intent.putExtra("initial", "true");
            startActivity(intent);
        } else if (client_class == "internal"){
            val intent = Intent(this@ContainerLogin, LoginWithRegister::class.java);
            intent.putExtra("logo_url", login_logo_url);
            intent.putExtra("initial", "true");
            startActivity(intent);
        } else {
            val intent = Intent(this@ContainerLogin, Login::class.java);
            intent.putExtra("logo_url", login_logo_url);
            intent.putExtra("initial_login", true);
            startActivity(intent);
        }
    }
}