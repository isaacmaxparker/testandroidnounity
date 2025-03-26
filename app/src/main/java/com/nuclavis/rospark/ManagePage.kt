package com.nuclavis.rospark

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import com.bumptech.glide.Glide
import com.github.onecode369.wysiwyg.WYSIWYG
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import kotlin.properties.Delegates


class ManagePage: com.nuclavis.rospark.BaseActivity(){
    var personal_page_prefix = "";
    var personal_page_custom_text = "";
    var personal_page_full_url = "";
    var personal_page_placeholder_html = ""
    var team_page_prefix = "";
    var team_page_custom_text = "";
    var team_page_full_url = "";
    var team_page_placeholder_html = ""
    var company_page_prefix = "";
    var company_page_custom_text = "";
    var company_page_full_url = "";
    var company_page_placeholder_html = ""
    lateinit var wysiwygEditor: WYSIWYG
    lateinit var teamWysiwygEditor: WYSIWYG
    lateinit var companyWysiwygEditor: WYSIWYG

    var plus_sign_placeholder = "PLUS_SIGN_HERE"
    var photo_picking_type = ""
    var saving_page_content = false;
    var saving_team_page_content = false;
    var saving_company_page_content = false;

    var edit_mode: Boolean by Delegates.observable<Boolean>(false) { _, old, new ->
        runOnUiThread {
            run {
                val pageContentContainer = findViewById<LinearLayout>(R.id.editContentContainer)
                if (new) {
                    pageContentContainer.setVisibility(View.VISIBLE)
                } else {
                    pageContentContainer.setVisibility(View.GONE)
                }
            };
        }
    }
    var team_edit_mode: Boolean by Delegates.observable<Boolean>(false) { _, old, new ->
        runOnUiThread {
            run {
                val pageContentContainer = findViewById<LinearLayout>(R.id.editTeamContentContainer)
                if (new) {
                    pageContentContainer.setVisibility(View.VISIBLE)
                } else {
                    pageContentContainer.setVisibility(View.GONE)
                }
            };
        }
    }
    var company_edit_mode: Boolean by Delegates.observable<Boolean>(false) { _, old, new ->
        runOnUiThread {
            run {
                val pageContentContainer = findViewById<LinearLayout>(R.id.editCompanyContentContainer)
                if (new) {
                    pageContentContainer.setVisibility(View.VISIBLE)
                } else {
                    pageContentContainer.setVisibility(View.GONE)
                }
            };
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        recolorTheme()
        super.onCreate(savedInstanceState)
        setPageContent(R.layout.manage_page,"managePage")
        wysiwygEditor = findViewById<ImageView>(R.id.editor) as WYSIWYG
        teamWysiwygEditor = findViewById<ImageView>(R.id.team_editor) as WYSIWYG
        companyWysiwygEditor = findViewById<ImageView>(R.id.company_editor) as WYSIWYG
        wysiwygEditor.setBackgroundColor(Color.parseColor("#FFFFFF"))
        teamWysiwygEditor.setBackgroundColor(Color.parseColor("#FFFFFF"))
        companyWysiwygEditor.setBackgroundColor(Color.parseColor("#FFFFFF"))
        setTitle(getResources().getString(R.string.mobile_main_menu_manage_page));
        sendGoogleAnalytics("manage_page_view", "manage_page")
        getImage();
        getTeamImage();
        getCompanyImage();

        val buttons_container = findViewById<LinearLayout>(R.id.manage_page_buttons)
        buttons_container.doOnLayout {
            val views = arrayOf(R.id.personal_page_title_container,R.id.team_page_title_container,R.id.company_page_title_container)

            val height = buttons_container.height - 50
            for(i in 0 .. views.size - 1){
                val view = findViewById<LinearLayout>(views[i])
                val params: ViewGroup.MarginLayoutParams =
                    view!!.layoutParams as ViewGroup.MarginLayoutParams
                params.bottomMargin = height
                view.layoutParams = params
            }
        }

        val personalTitle = getManagePageTitle("personal")
        findViewById<TextView>(R.id.manage_page_title).setText(personalTitle)
        val teamTitle = getManagePageTitle("team")
        findViewById<TextView>(R.id.manage_page_team_title).setText(teamTitle)
        val companyTitle = getManagePageTitle("company")
        findViewById<TextView>(R.id.manage_page_company_title).setText(companyTitle)

        setTooltipText(
            R.id.personalize_page_help_button,
            R.string.mobile_manage_page_personalize_page_tooltip,
            R.string.mobile_manage_page_personalize_page_title
        )

        setTooltipText(
            R.id.personalize_page_team_help_button,
            R.string.mobile_manage_page_personalize_team_page_tooltip,
            R.string.mobile_manage_page_personalize_team_page_title
        )

        setTooltipText(
            R.id.personalize_page_company_help_button,
            R.string.mobile_manage_page_personalize_company_page_tooltip,
            R.string.mobile_manage_page_personalize_company_page_title
        )

        findViewById<ImageView>(R.id.copy_personal_page_url).setOnClickListener{
            val myClipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?;
            val myClip = ClipData.newPlainText("company url",personal_page_full_url);
            myClipboard?.setPrimaryClip(myClip);
            displayAlert(getString(R.string.mobile_manage_page_personal_url_copy_success))
        }

        findViewById<ImageView>(R.id.copy_team_page_url).setOnClickListener{
            val myClipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?;
            val myClip = ClipData.newPlainText("company url",team_page_full_url);
            myClipboard?.setPrimaryClip(myClip);
            displayAlert(getString(R.string.mobile_manage_page_team_url_copy_success))
        }

        findViewById<ImageView>(R.id.copy_company_page_url).setOnClickListener{
            val myClipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?;
            val myClip = ClipData.newPlainText("company url",company_page_full_url);
            myClipboard?.setPrimaryClip(myClip);
            displayAlert(getString(R.string.mobile_manage_page_company_url_copy_success))
        }

        findViewById<LinearLayout>(R.id.editContentContainer).setVisibility(View.GONE)
        findViewById<LinearLayout>(R.id.editTeamContentContainer).setVisibility(View.GONE)
        findViewById<LinearLayout>(R.id.editCompanyContentContainer).setVisibility(View.GONE)

        var color = getStringVariable("PRIMARY_COLOR")
        if(getStringVariable("TILE_BACKGROUND_WHITE_ENABLED") == "true"){
            color = "#f2f1ef"
        }
        findViewById<ImageView>(R.id.personal_page_image).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(color)))
        findViewById<ImageView>(R.id.team_page_image).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(color)))
        findViewById<ImageView>(R.id.company_page_image).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(color)))
        findViewById<LinearLayout>(R.id.personal_editor_border).setBackgroundColor(Color.parseColor(color))
        findViewById<LinearLayout>(R.id.team_editor_border).setBackgroundColor(Color.parseColor(color))
        findViewById<LinearLayout>(R.id.company_editor_border).setBackgroundColor(Color.parseColor(color))

        if(getStringVariable("MANAGE_PAGE_CUSTOM_URL_DISABLED") == "true"){
            findViewById<LinearLayout>(R.id.personal_page_create_url_container).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.team_page_create_url_container).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.company_page_create_url_container).setVisibility(View.GONE)
        }else{
            findViewById<LinearLayout>(R.id.personal_page_create_url_container).setVisibility(View.VISIBLE)
            findViewById<LinearLayout>(R.id.team_page_create_url_container).setVisibility(View.VISIBLE)
            findViewById<LinearLayout>(R.id.company_page_create_url_container).setVisibility(View.VISIBLE)
        }

        loadPersonalPageData()
        loadTeamPageData()
        loadCompanyPageData()

        loadEditorData()
        loadTeamEditorData()
        loadCompanyEditorData()

        var upload_photo_button = findViewById<Button>(R.id.btn_upload_photos)
        var upload_team_photo_button = findViewById<Button>(R.id.btn_upload_team_photo)
        var upload_company_photo_button = findViewById<Button>(R.id.btn_upload_company_photo)

        if (getStringVariable("CLIENT_CLASS") == "classy") {
            upload_photo_button.setVisibility(View.GONE)
            upload_team_photo_button.setVisibility(View.GONE)
            upload_company_photo_button.setVisibility(View.GONE)
        } else {
            upload_photo_button.setOnClickListener {
                displayAlert("photoAlert", "personal")
                setAlertSender(upload_photo_button)
                sendSocialActivity("personal_page_update")
            }
            upload_team_photo_button.setOnClickListener {
                displayAlert("photoAlert", "team")
                setAlertSender(upload_team_photo_button)
            }
            upload_company_photo_button.setOnClickListener {
                displayAlert("photoAlert", "company")
                setAlertSender(upload_company_photo_button)
            }
        }

        var showTeam = false;
        var showCompany = false;

        val companyButton = findViewById<FrameLayout>(R.id.manage_page_company_button)
        val teamButton = findViewById<FrameLayout>(R.id.manage_page_team_button)

        if(
            getStringVariable("IS_COMPANY_COORDINATOR") == "true"
            && getStringVariable("DISABLE_EDIT_COMPANY_PAGE") != "true"
        ){
            showCompany = true;
        }else{
            companyButton.visibility = View.GONE
        }
        
        if(getStringVariable("IS_TEAM_CAPTAIN") == "true" && getStringVariable("DISABLE_EDIT_TEAM_PAGE") != "true"){
            showTeam = true;
        }else{
            teamButton.visibility = View.GONE
        }

        if(!showTeam){
            if(!showCompany){
                findViewById<LinearLayout>(R.id.manage_page_buttons).visibility = View.GONE
            }else{
                val companyButton = findViewById<FrameLayout>(R.id.manage_page_company_button)
                (findViewById<LinearLayout>(R.id.manage_page_buttons).getChildAt(1) as LinearLayout).removeView(
                    companyButton
                )
                (findViewById<LinearLayout>(R.id.manage_page_buttons).getChildAt(0) as LinearLayout).addView(
                    companyButton
                )
            }
        }else{
            if(!showCompany){
                findViewById<LinearLayout>(R.id.manage_page_company_button_container).visibility = View.GONE
            }
        }

        findViewById<FrameLayout>(R.id.manage_page_personal_button).setOnClickListener {
            switchSlide("personal");
        };

        findViewById<FrameLayout>(R.id.manage_page_team_button).setOnClickListener {
            switchSlide("team");
        };

        findViewById<FrameLayout>(R.id.manage_page_company_button).setOnClickListener {
            switchSlide("company");
        };

        val starting_page = intent.getStringExtra("starting_page")
        if(starting_page != "" && starting_page != null){
            switchSlide(starting_page)
        }else {
            switchSlide("personal");
        }
    }

    var launcher: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult<PickVisualMediaRequest, Uri>(
            ActivityResultContracts.PickVisualMedia(),
            object : ActivityResultCallback<Uri?> {
                override fun onActivityResult(result: Uri?) {
                    if (result == null) {
                        Toast.makeText(this@ManagePage, "No image Selected", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        println("photo_picking_type: " + photo_picking_type)
                        saveGalleryPhoto(result)
                    }
                }
            }
        )

    override fun slideButtonCallback(card: Any, forward:Boolean){}

    fun getCompanyImage(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getCompanyPageImage/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    runOnUiThread{
                        val jsonString = response.body?.string();
                        var obj = JSONObject(jsonString);
                        if(obj.has("image_url")){
                            loadCompanyImage(obj.get("image_url") as String)
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {

            }
        })
    }

    fun getTeamImage(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getTeamPageImage/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    runOnUiThread{
                        val jsonString = response.body?.string();
                        var obj = JSONObject(jsonString);
                        if(obj.has("image_url")){
                            loadTeamImage(obj.get("image_url") as String)
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {

            }
        })
    }

    fun getImage(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getPersonalPageImage/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    runOnUiThread{
                        val jsonString = response.body?.string();
                        var obj = JSONObject(jsonString);
                        println("URL")
                        println(obj)
                        if(obj.has("image_url")){
                            loadImage(obj.get("image_url") as String)
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {

            }
        })
    }

    fun loadCompanyPageData() {
        runOnUiThread {
            findViewById<LinearLayout>(R.id.company_page_url_container).setVisibility(View.GONE);
            findViewById<Button>(R.id.btn_create_company_url).setVisibility(View.GONE);
        }
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getCompanyPageShortcut/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    val jsonString = response.body?.string();
                    var obj = JSONObject(jsonString);

                    if(obj.has("prefix_url") && obj.get("prefix_url") is String){
                        company_page_prefix = (obj.get("prefix_url") as String);
                    }
                    if(obj.has("custom_url_ending") && obj.get("custom_url_ending") is String){
                        company_page_custom_text = (obj.get("custom_url_ending") as String);
                    }
                    if(obj.has("full_url") && obj.get("full_url") is String){
                        company_page_full_url = (obj.get("full_url") as String);
                    }

                    if(company_page_prefix == ""){
                        val temp = company_page_full_url.replace("\\/","/")
                        company_page_prefix = temp.substring(0, temp.lastIndexOf("/") + 1)
                    }

                    runOnUiThread {
                        toggleCompanyUrl()
                    }

                    findViewById<TextView>(R.id.btn_edit_company_url).setOnClickListener(){
                        showEditLinkDialog("company")
                        setAlertSender(findViewById<TextView>(R.id.btn_edit_company_url))
                    }

                    findViewById<Button>(R.id.btn_create_company_url).setOnClickListener(){
                        showEditLinkDialog("company")
                        setAlertSender(findViewById<Button>(R.id.btn_create_company_url))
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {

            }
        })
    }

    fun loadTeamPageData() {
        runOnUiThread {
            findViewById<LinearLayout>(R.id.team_page_url_container).setVisibility(View.GONE);
            findViewById<Button>(R.id.btn_create_team_url).setVisibility(View.GONE);
        }
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getTeamPageShortcut/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    val jsonString = response.body?.string();
                    var obj = JSONObject(jsonString);

                    if(obj.has("prefix_url") && obj.get("prefix_url") is String){
                        team_page_prefix = (obj.get("prefix_url") as String);
                    }
                    if(obj.has("custom_url_ending") && obj.get("custom_url_ending") is String){
                        team_page_custom_text = (obj.get("custom_url_ending") as String);
                    }
                    if(obj.has("full_url") && obj.get("full_url") is String){
                        team_page_full_url = (obj.get("full_url") as String);
                    }

                    if(team_page_prefix == ""){
                        val temp = team_page_full_url.replace("\\/","/")
                        team_page_prefix = temp.substring(0, temp.lastIndexOf("/") + 1)
                    }

                    runOnUiThread {
                        toggleTeamUrl()
                    }

                    findViewById<TextView>(R.id.btn_edit_team_url).setOnClickListener(){
                        showEditLinkDialog("team")
                        setAlertSender(findViewById<TextView>(R.id.btn_edit_team_url))
                    }

                    findViewById<Button>(R.id.btn_create_team_url).setOnClickListener(){
                        showEditLinkDialog("team")
                        setAlertSender(findViewById<Button>(R.id.btn_create_team_url))
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {

            }
        })
    }

    fun loadPersonalPageData() {
        runOnUiThread {
            findViewById<LinearLayout>(R.id.personal_page_url_container).setVisibility(View.GONE);
            findViewById<Button>(R.id.btn_create_personal_url).setVisibility(View.GONE);
        }
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getPersonalPageShortcut/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    val jsonString = response.body?.string();
                    var obj = JSONObject(jsonString);
                    if(obj.has("prefix_url") && obj.get("prefix_url") is String){
                        personal_page_prefix = (obj.get("prefix_url") as String);
                    }
                    if(obj.has("custom_url_ending") && obj.get("custom_url_ending") is String){
                        personal_page_custom_text = (obj.get("custom_url_ending") as String);
                    }
                    if(obj.has("full_url") && obj.get("full_url") is String){
                        personal_page_full_url = (obj.get("full_url") as String);
                    }

                    if(personal_page_prefix == ""){
                        val temp = personal_page_full_url.replace("\\/","/")
                        personal_page_prefix = temp.substring(0, temp.lastIndexOf("/") + 1)
                    }
                    
                    runOnUiThread {
                        togglePersonalUrl()
                    }

                    findViewById<TextView>(R.id.btn_edit_personal_url).setOnClickListener(){
                        showEditLinkDialog("personal")
                        setAlertSender(findViewById<TextView>(R.id.btn_edit_personal_url))
                    }

                    findViewById<Button>(R.id.btn_create_personal_url).setOnClickListener(){
                        showEditLinkDialog("personal")
                        setAlertSender(findViewById<Button>(R.id.btn_create_personal_url))
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {

            }
        })
    }

    fun togglePersonalUrl(){
        if(personal_page_custom_text == ""){
            findViewById<LinearLayout>(R.id.personal_page_url_container).setVisibility(View.GONE);
            findViewById<Button>(R.id.btn_create_personal_url).setVisibility(View.VISIBLE);
            findViewById<TextView>(R.id.personal_page_url).setText("");
        }else{
            findViewById<LinearLayout>(R.id.personal_page_url_container).setVisibility(View.VISIBLE);
            findViewById<Button>(R.id.btn_create_personal_url).setVisibility(View.GONE);
            findViewById<TextView>(R.id.personal_page_url).setText(
                personal_page_full_url
            )
        }
    }

    fun toggleTeamUrl(){
        if(team_page_custom_text == ""){
            findViewById<LinearLayout>(R.id.team_page_url_container).setVisibility(View.GONE);
            findViewById<Button>(R.id.btn_create_team_url).setVisibility(View.VISIBLE);
            findViewById<TextView>(R.id.team_page_url).setText("");
        }else{
            findViewById<LinearLayout>(R.id.team_page_url_container).setVisibility(View.VISIBLE);
            findViewById<Button>(R.id.btn_create_team_url).setVisibility(View.GONE);
            findViewById<TextView>(R.id.team_page_url).setText(
                team_page_full_url
            )
        }
    }

    fun toggleCompanyUrl(){
        if(company_page_custom_text == ""){
            findViewById<LinearLayout>(R.id.company_page_url_container).setVisibility(View.GONE);
            findViewById<Button>(R.id.btn_create_company_url).setVisibility(View.VISIBLE);
            findViewById<TextView>(R.id.company_page_url).setText("");
        }else{
            findViewById<LinearLayout>(R.id.company_page_url_container).setVisibility(View.VISIBLE);
            findViewById<Button>(R.id.btn_create_company_url).setVisibility(View.GONE);
            findViewById<TextView>(R.id.company_page_url).setText(
                company_page_full_url
            )
        }
    }

    fun showEditLinkDialog(type:String){
        if(type == "team"){
            displayAlert("editTeamUrl", team_page_custom_text, { childviewCallback("editTeamLink","") })
        }else if(type == "company"){
            displayAlert("editCompanyUrl", company_page_custom_text, { childviewCallback("editCompanyLink","") })
        }else {
            displayAlert("editUrl", personal_page_custom_text, { childviewCallback("editLink","") })
        }

    }

    fun checkForIllegalChars(html: String): String {
        var illegalString = ""
        var illegalChars = arrayOf("《", "》")

        if(html.contains("&lt;")){
            illegalString += "<, "
        }

        if(html.contains("&gt;")){
            illegalString += ">, "
        }

        if(containsEmoji(html)){
            illegalString += "emojis, "
        }

        illegalChars.forEach {
            if(html.contains(it)){
                illegalString += it + ", "
            }
        }

        if(illegalString.length > 0){
            illegalString = illegalString.substring(0, illegalString.length - 2)
        }
        return illegalString
    }

    fun encodeHTMLtoSend(html: String): String{
        var client_class = getStringVariable("CLIENT_CLASS");
        var encodedHtml = html;
        if(client_class != "classy") {
            encodedHtml = encodedHtml.replace("+","&#43;")
            encodedHtml = encodedHtml.replace("style=","styleEQUALPLACEHOLDER")
            encodedHtml = encodedHtml.replace("=","&#61;")
            encodedHtml = encodedHtml.replace("EQUALPLACEHOLDER","=")
            encodedHtml = encodedHtml.replace("×","&times;")
            encodedHtml = encodedHtml.replace("÷","&divide;")
            encodedHtml = encodedHtml.replace("€","&euro;")
            encodedHtml = encodedHtml.replace("₩","&#8361;")
            encodedHtml = encodedHtml.replace("°","&deg;")
            encodedHtml = encodedHtml.replace("•","&bull;")
            encodedHtml = encodedHtml.replace("○","&#9675;")
            encodedHtml = encodedHtml.replace("●","&#9679;")
            encodedHtml = encodedHtml.replace("□","&#9633;")
            encodedHtml = encodedHtml.replace("■","&#9632;")
            encodedHtml = encodedHtml.replace("—","&#8212;")
            encodedHtml = encodedHtml.replace("\'","&#39;")
            encodedHtml = encodedHtml.replace("♠","&spades;")
            encodedHtml = encodedHtml.replace("♣","&clubs;")
            encodedHtml = encodedHtml.replace("♥","&hearts;")
            encodedHtml = encodedHtml.replace("♦","&diams;")
            encodedHtml = encodedHtml.replace("♠","&spades;")
            encodedHtml = encodedHtml.replace("♣︎","&clubs;")
            encodedHtml = encodedHtml.replace("♥","&hearts;")
            encodedHtml = encodedHtml.replace("︎◆","&diams;")
            encodedHtml = encodedHtml.replace("♤","&spades;")
            encodedHtml = encodedHtml.replace("♧","&clubs;")
            encodedHtml = encodedHtml.replace("♡","&hearts;")
            encodedHtml = encodedHtml.replace("◇","&diams;")
            encodedHtml = encodedHtml.replace("☆", "&#9734;")
            encodedHtml = encodedHtml.replace("★","&#9733;")
            encodedHtml = encodedHtml.replace("¿","&iquest;")
            encodedHtml = encodedHtml.replace("︎¡","&iexcl;")
            encodedHtml = encodedHtml.replace("¤","&curren;")
            encodedHtml = encodedHtml.replace("▪︎︎","&#9642;")
            encodedHtml = encodedHtml.replace("《","")
            encodedHtml = encodedHtml.replace("︎》","")
            encodedHtml = encodedHtml.replace("<b>","<span style=\"font-weight:bold\">")
            encodedHtml = encodedHtml.replace("<b style=\"","<span style=\"font-weight:bold;")
            encodedHtml = encodedHtml.replace("<i>","<span style=\"font-style:italic;font-weight:inherit;\">")
            encodedHtml = encodedHtml.replace("<i style=\"","<span style=\"font-style:italic;font-weight:inherit;")
            encodedHtml = encodedHtml.replace("<u>","<span style=\"text-decoration: underline;font-weight:inherit;\">")
            encodedHtml = encodedHtml.replace("<u style=\"","<span style=\"text-decoration: underline;font-weight:inherit;")
            encodedHtml = encodedHtml.replace("</b>","</span>")
            encodedHtml = encodedHtml.replace("</i>","</span>")
            encodedHtml = encodedHtml.replace("</u>","</span>")
            encodedHtml = encodedHtml.replace("<font face&#61;\"Verdana, sans-serif\">","")
            encodedHtml = encodedHtml.replace("</font>","")
            encodedHtml = encodedHtml.replace("+",plus_sign_placeholder)
            encodedHtml = URLEncoder.encode(encodedHtml, "utf-8")
            encodedHtml = encodedHtml.replace("+"," ")
            encodedHtml = encodedHtml.replace(plus_sign_placeholder,"+")
        }
        return encodedHtml
    }

    fun updatePageContent(newContent: String){
        var illegalString = checkForIllegalChars(newContent);
        if(illegalString.length > 0){
            var message = getResources().getString(R.string.mobile_manage_page_illegal_chars_prefix)
            message += "\r\n" + illegalString + " "
            message += getResources().getString(R.string.mobile_manage_page_illegal_chars_suffix)
            displayAlert(message, "")
            setAlertSender(findViewById<Button>(R.id.btn_team_edit_save_story))

            var edit_save_button = findViewById<Button>(R.id.btn_edit_save_story)
            var view_cancel_story_button = findViewById<Button>(R.id.btn_view_story)
            var save_text = getResources().getString(R.string.mobile_manage_page_share_story_save);
            var cancel_text = getResources().getString(R.string.mobile_manage_page_share_story_cancel);
            edit_save_button.setText(save_text);
            view_cancel_story_button.setText(cancel_text);
            saving_page_content = false;
            edit_mode = true;
            wysiwygEditor.setInputEnabled(true);
            findViewById<HorizontalScrollView>(R.id.toolbarHorizontalScrollView).setVisibility(View.VISIBLE);
        }else{
            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/updatePersonalPage/")
            var encodedContent = encodeHTMLtoSend(newContent)
            val formBody = FormBody.Builder()
                .add("cons_id", getConsID())
                .add("event_id", getEvent().event_id)
                .add("content", encodedContent)
                .build()

            var request = Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()

            var client = OkHttpClient();

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if(response.code != 200){
                        try{
                            val response = response.body?.string();
                            val obj = JSONObject(response);
                            if(obj.get("statusCode") as Int == 550){
                                if(obj.has("message") && obj.get("message") is String) {
                                    displayAlert(obj.get("message") as String)
                                    setAlertSender(findViewById<Button>(R.id.btn_edit_save_story))
                                }else{
                                    displayAlert(getResources().getString(R.string.mobile_overview_save_story_error))
                                    setAlertSender(findViewById<Button>(R.id.btn_edit_save_story))
                                }
                            }
                        }catch(e:Exception){
                            displayAlert(getResources().getString(R.string.mobile_overview_save_story_error))
                            setAlertSender(findViewById<Button>(R.id.btn_edit_save_story))
                        }

                        saving_page_content = false;
                    }else{
                        val response = response.body?.string();
                        val obj = JSONObject(response);
                        personal_page_placeholder_html = newContent;
                        edit_mode = false
                        saving_page_content = false;
                        wysiwygEditor.setBackgroundColor(Color.parseColor("#FFFFFF"))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("ERROR SAVING")
                    println(e.message.toString())
                    displayAlert(getResources().getString(R.string.mobile_overview_save_story_error), hideAlert())
                    setAlertSender(findViewById<Button>(R.id.btn_edit_save_story))
                    hideAlert()
                    saving_page_content = false;
                }
            })
        }
    }

    fun updateTeamPageContent(newContent: String){
        var illegalString = checkForIllegalChars(newContent);
        if(illegalString.length > 0){
            var message = getResources().getString(R.string.mobile_manage_page_illegal_chars_prefix)
            message += "\r\n" + illegalString + " "
            message += getResources().getString(R.string.mobile_manage_page_illegal_chars_suffix)
            displayAlert(message, "")
            setAlertSender(findViewById<Button>(R.id.btn_team_edit_save_story))

            var edit_save_button = findViewById<Button>(R.id.btn_team_edit_save_story)
            var view_cancel_story_button = findViewById<Button>(R.id.btn_team_view_story)
            var save_text = getResources().getString(R.string.mobile_manage_page_share_story_save);
            var cancel_text = getResources().getString(R.string.mobile_manage_page_share_story_cancel);
            edit_save_button.setText(save_text);
            view_cancel_story_button.setText(cancel_text);
            teamWysiwygEditor.setInputEnabled(true);
            team_edit_mode = true
            saving_team_page_content = false;
            findViewById<HorizontalScrollView>(R.id.teamToolbarHorizontalScrollView).setVisibility(View.VISIBLE);
        }else{
            saving_team_page_content = true;
            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/updateTeamPage/")
            var encodedContent = encodeHTMLtoSend(newContent)
            val formBody = FormBody.Builder()
                .add("cons_id", getConsID())
                .add("event_id", getEvent().event_id)
                .add("content", encodedContent)
                .build()

            var request = Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()

            var client = OkHttpClient();

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if(response.code != 200){
                        val response = response.body?.string();
                        try{
                            val obj = JSONObject(response);
                            if(obj.get("statusCode") as Int == 550){
                                if(obj.has("message") && obj.get("message") is String) {
                                    displayAlert(obj.get("message") as String)
                                    setAlertSender(findViewById<Button>(R.id.btn_team_edit_save_story))
                                }else{
                                    displayAlert(getResources().getString(R.string.mobile_overview_save_story_error))
                                    setAlertSender(findViewById<Button>(R.id.btn_team_edit_save_story))
                                }
                            }
                        }catch(e:Exception){
                            displayAlert(getResources().getString(R.string.mobile_overview_save_story_error))
                            setAlertSender(findViewById<Button>(R.id.btn_team_edit_save_story))
                        }
                        saving_team_page_content = false;
                    }else{
                        val response = response.body?.string();
                        val obj = JSONObject(response);
                        team_page_placeholder_html = newContent;
                        team_edit_mode = false
                        saving_team_page_content = false;
                        teamWysiwygEditor.setBackgroundColor(Color.parseColor("#FFFFFF"))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("ERROR SAVING")
                    saving_team_page_content = false;
                    println(e.message.toString());
                    displayAlert(getResources().getString(R.string.mobile_overview_save_story_error), hideAlert())
                    setAlertSender(findViewById<Button>(R.id.btn_team_edit_save_story))
                    hideAlert()
                }
            })
        }
    }

    fun updateCompanyPageContent(newContent: String){
        var illegalString = checkForIllegalChars(newContent);
        if(illegalString.length > 0){
            var message = getResources().getString(R.string.mobile_manage_page_illegal_chars_prefix)
            message += "\r\n" + illegalString + " "
            message += getResources().getString(R.string.mobile_manage_page_illegal_chars_suffix)
            displayAlert(message, "")
            setAlertSender(findViewById<Button>(R.id.btn_company_edit_save_story))

            var edit_save_button = findViewById<Button>(R.id.btn_company_edit_save_story)
            var view_cancel_story_button = findViewById<Button>(R.id.btn_company_view_story)
            var save_text = getResources().getString(R.string.mobile_manage_page_share_story_save);
            var cancel_text = getResources().getString(R.string.mobile_manage_page_share_story_cancel);
            edit_save_button.setText(save_text);
            view_cancel_story_button.setText(cancel_text);
            companyWysiwygEditor.setInputEnabled(true);
            company_edit_mode = true
            saving_company_page_content = false;
            findViewById<HorizontalScrollView>(R.id.companyToolbarHorizontalScrollView).setVisibility(View.VISIBLE);
        }else{
            saving_company_page_content = true;
            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/updateCompanyPage/")
            var encodedContent = encodeHTMLtoSend(newContent)
            val formBody = FormBody.Builder()
                .add("cons_id", getConsID())
                .add("event_id", getEvent().event_id)
                .add("content", encodedContent)
                .build()

            var request = Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()

            var client = OkHttpClient();

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if(response.code != 200){
                        val response = response.body?.string();
                        try{
                            val obj = JSONObject(response);
                            if(obj.get("statusCode") as Int == 550){
                                if(obj.has("message") && obj.get("message") is String) {
                                    displayAlert(obj.get("message") as String)
                                    setAlertSender(findViewById<Button>(R.id.btn_company_edit_save_story))
                                }else{
                                    displayAlert(getResources().getString(R.string.mobile_overview_save_story_error))
                                    setAlertSender(findViewById<Button>(R.id.btn_company_edit_save_story))
                                }
                            }
                        }catch(e:Exception){
                            displayAlert(getResources().getString(R.string.mobile_overview_save_story_error))
                            setAlertSender(findViewById<Button>(R.id.btn_company_edit_save_story))
                        }
                        saving_company_page_content = false;
                    }else{
                        val response = response.body?.string();
                        company_page_placeholder_html = newContent;
                        company_edit_mode = false
                        saving_company_page_content = false;
                        companyWysiwygEditor.setBackgroundColor(Color.parseColor("#FFFFFF"))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    saving_company_page_content = false;
                    displayAlert(getResources().getString(R.string.mobile_overview_save_story_error), hideAlert())
                    setAlertSender(findViewById<Button>(R.id.btn_company_edit_save_story))
                    hideAlert()
                }
            })
        }
    }

    fun loadCompanyEditorData(){
        companyWysiwygEditor.setEditorHeight(200)
        companyWysiwygEditor.setEditorFontSize(16)
        companyWysiwygEditor.setPadding(10, 10, 10, 10)
        companyWysiwygEditor.setPlaceholder("")

        findViewById<ImageView>(R.id.action_company_undo).setOnClickListener{ companyWysiwygEditor.undo() }
        findViewById<ImageView>(R.id.action_company_redo).setOnClickListener{ companyWysiwygEditor.redo() }
        findViewById<ImageView>(R.id.action_company_bold).setOnClickListener{ companyWysiwygEditor.setBold() }
        findViewById<ImageView>(R.id.action_company_italic).setOnClickListener{ companyWysiwygEditor.setItalic() }
        findViewById<ImageView>(R.id.action_company_underline).setOnClickListener { companyWysiwygEditor.setUnderline() }
        findViewById<ImageView>(R.id.action_company_heading1).setOnClickListener{companyWysiwygEditor.setHeading(1)}
        findViewById<ImageView>(R.id.action_company_heading2).setOnClickListener{companyWysiwygEditor.setHeading(2)}
        findViewById<ImageView>(R.id.action_company_heading3).setOnClickListener{companyWysiwygEditor.setHeading(3)}
        findViewById<ImageView>(R.id.action_company_heading4).setOnClickListener{companyWysiwygEditor.setHeading(4)}
        findViewById<ImageView>(R.id.action_company_heading5).setOnClickListener{companyWysiwygEditor.setHeading(5)}
        findViewById<ImageView>(R.id.action_company_heading6).setOnClickListener{companyWysiwygEditor.setHeading(6)}
        findViewById<ImageView>(R.id.action_company_indent).setOnClickListener{companyWysiwygEditor.setIndent() }
        findViewById<ImageView>(R.id.action_company_outdent).setOnClickListener { companyWysiwygEditor.setOutdent() }
        findViewById<ImageView>(R.id.action_company_align_left).setOnClickListener{ companyWysiwygEditor.setAlignLeft() }
        findViewById<ImageView>(R.id.action_company_align_center).setOnClickListener{ companyWysiwygEditor.setAlignCenter() }
        findViewById<ImageView>(R.id.action_company_align_right).setOnClickListener { companyWysiwygEditor.setAlignRight() }
        findViewById<ImageView>(R.id.action_company_align_justify).setOnClickListener { companyWysiwygEditor.setAlignJustifyFull() }
        findViewById<ImageView>(R.id.action_company_insert_bullets).setOnClickListener { companyWysiwygEditor.setBullets() }
        findViewById<ImageView>(R.id.action_company_insert_numbers).setOnClickListener { companyWysiwygEditor.setNumbers() }

        companyWysiwygEditor.setInputEnabled(false)
        findViewById<HorizontalScrollView>(R.id.toolbarHorizontalScrollView).setVisibility(View.GONE);

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getCompanyPage/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    val jsonString = response.body?.string();
                    var obj = JSONObject(jsonString);
                    var content = ""
                    if(obj.has("content")){
                        content = (obj.get("content") as String);
                    }
                    val cleanedContent = decodeContent(content);
                    company_page_placeholder_html = cleanedContent
                    runOnUiThread{
                        companyWysiwygEditor.html = cleanedContent;
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    companyWysiwygEditor.html =
                        getResources().getString(R.string.mobile_overview_load_story_error)
                }
            }
        })

        var edit_save_button = findViewById<Button>(R.id.btn_company_edit_save_story)
        var view_cancel_story_button = findViewById<Button>(R.id.btn_company_view_story)
        var edit_text = getResources().getString(R.string.mobile_manage_page_share_story_edit);
        var save_text = getResources().getString(R.string.mobile_manage_page_share_story_save);
        var view_text = getResources().getString(R.string.mobile_manage_page_share_story_view);
        var cancel_text = getResources().getString(R.string.mobile_manage_page_share_story_cancel);

        edit_save_button.setOnClickListener {
            companyWysiwygEditor.setEditorBackgroundColor(Color.parseColor("#FFFFFF"))
            if(!saving_company_page_content){
                if (!company_edit_mode) {
                    company_edit_mode = true;
                    edit_save_button.setText(save_text);
                    view_cancel_story_button.setText(cancel_text);
                    companyWysiwygEditor.setInputEnabled(true);
                    companyWysiwygEditor.setEditorBackgroundColor(Color.parseColor("#FFFFFF"))
                    findViewById<HorizontalScrollView>(R.id.companyToolbarHorizontalScrollView).setVisibility(View.VISIBLE);
                } else {
                    saving_company_page_content = true;
                    company_edit_mode = false;
                    edit_save_button.setText(edit_text);
                    companyWysiwygEditor.setInputEnabled(false);
                    view_cancel_story_button.setText(view_text);
                    findViewById<HorizontalScrollView>(R.id.companyToolbarHorizontalScrollView).setVisibility(View.GONE);
                    updateCompanyPageContent((companyWysiwygEditor.html as String));
                }
            }
        }

        view_cancel_story_button.setOnClickListener {
            companyWysiwygEditor.setEditorBackgroundColor(Color.parseColor("#FFFFFF"))
            if(!company_edit_mode){
                var url = company_page_full_url;
                if(url != ""){
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }else{
                    displayAlert(getString(R.string.mobile_url_opening_error))
                }
            }else{
                company_edit_mode = false;
                view_cancel_story_button.setText(view_text)
                companyWysiwygEditor.html = company_page_placeholder_html
                edit_save_button.setText(edit_text)
                companyWysiwygEditor.setInputEnabled(false)
                findViewById<HorizontalScrollView>(R.id.companyToolbarHorizontalScrollView).setVisibility(View.GONE);
            }
        }
    }

    fun loadTeamEditorData(){
        teamWysiwygEditor.setEditorHeight(200)
        teamWysiwygEditor.setEditorFontSize(16)
        teamWysiwygEditor.setPadding(10, 10, 10, 10)
        teamWysiwygEditor.setPlaceholder("")

        findViewById<ImageView>(R.id.action_team_undo).setOnClickListener{ teamWysiwygEditor.undo() }
        findViewById<ImageView>(R.id.action_team_redo).setOnClickListener{ teamWysiwygEditor.redo() }
        findViewById<ImageView>(R.id.action_team_bold).setOnClickListener{ teamWysiwygEditor.setBold() }
        findViewById<ImageView>(R.id.action_team_italic).setOnClickListener{ teamWysiwygEditor.setItalic() }
        findViewById<ImageView>(R.id.action_team_underline).setOnClickListener { teamWysiwygEditor.setUnderline() }
        findViewById<ImageView>(R.id.action_team_heading1).setOnClickListener{teamWysiwygEditor.setHeading(1)}
        findViewById<ImageView>(R.id.action_team_heading2).setOnClickListener{teamWysiwygEditor.setHeading(2)}
        findViewById<ImageView>(R.id.action_team_heading3).setOnClickListener{teamWysiwygEditor.setHeading(3)}
        findViewById<ImageView>(R.id.action_team_heading4).setOnClickListener{teamWysiwygEditor.setHeading(4)}
        findViewById<ImageView>(R.id.action_team_heading5).setOnClickListener{teamWysiwygEditor.setHeading(5)}
        findViewById<ImageView>(R.id.action_team_heading6).setOnClickListener{teamWysiwygEditor.setHeading(6)}
        findViewById<ImageView>(R.id.action_team_indent).setOnClickListener{teamWysiwygEditor.setIndent() }
        findViewById<ImageView>(R.id.action_team_outdent).setOnClickListener { teamWysiwygEditor.setOutdent() }
        findViewById<ImageView>(R.id.action_team_align_left).setOnClickListener{ teamWysiwygEditor.setAlignLeft() }
        findViewById<ImageView>(R.id.action_team_align_center).setOnClickListener{ teamWysiwygEditor.setAlignCenter() }
        findViewById<ImageView>(R.id.action_team_align_right).setOnClickListener { teamWysiwygEditor.setAlignRight() }
        findViewById<ImageView>(R.id.action_team_align_justify).setOnClickListener { teamWysiwygEditor.setAlignJustifyFull() }
        findViewById<ImageView>(R.id.action_team_insert_bullets).setOnClickListener { teamWysiwygEditor.setBullets() }
        findViewById<ImageView>(R.id.action_team_insert_numbers).setOnClickListener { teamWysiwygEditor.setNumbers() }

        teamWysiwygEditor.setInputEnabled(false)
        findViewById<HorizontalScrollView>(R.id.toolbarHorizontalScrollView).setVisibility(View.GONE);

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getTeamPage/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    val jsonString = response.body?.string();
                    var obj = JSONObject(jsonString);
                    var content = ""
                    if(obj.has("content")){
                        content = (obj.get("content") as String);
                    }
                    val cleanedContent = decodeContent(content);
                    team_page_placeholder_html = cleanedContent
                    runOnUiThread{
                        teamWysiwygEditor.html = cleanedContent;
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    teamWysiwygEditor.html =
                        getResources().getString(R.string.mobile_overview_load_story_error)
                }
            }
        })

        var edit_save_button = findViewById<Button>(R.id.btn_team_edit_save_story)
        var view_cancel_story_button = findViewById<Button>(R.id.btn_team_view_story)
        var edit_text = getResources().getString(R.string.mobile_manage_page_share_story_edit);
        var save_text = getResources().getString(R.string.mobile_manage_page_share_story_save);
        var view_text = getResources().getString(R.string.mobile_manage_page_share_story_view);
        var cancel_text = getResources().getString(R.string.mobile_manage_page_share_story_cancel);

        edit_save_button.setOnClickListener {
            teamWysiwygEditor.setEditorBackgroundColor(Color.parseColor("#FFFFFF"))
            if(!saving_team_page_content){
                if (!team_edit_mode) {
                    team_edit_mode = true;
                    edit_save_button.setText(save_text);
                    view_cancel_story_button.setText(cancel_text);
                    teamWysiwygEditor.setInputEnabled(true);
                    teamWysiwygEditor.setEditorBackgroundColor(Color.parseColor("#FFFFFF"))
                    findViewById<HorizontalScrollView>(R.id.teamToolbarHorizontalScrollView).setVisibility(View.VISIBLE);
                } else {
                    saving_team_page_content = true;
                    team_edit_mode = false;
                    edit_save_button.setText(edit_text);
                    teamWysiwygEditor.setInputEnabled(false);
                    view_cancel_story_button.setText(view_text);
                    findViewById<HorizontalScrollView>(R.id.teamToolbarHorizontalScrollView).setVisibility(View.GONE);
                    updateTeamPageContent((teamWysiwygEditor.html as String));
                }
            }
        }

        view_cancel_story_button.setOnClickListener {
            teamWysiwygEditor.setEditorBackgroundColor(Color.parseColor("#FFFFFF"))
            if(!team_edit_mode){
                var url = team_page_full_url;
                if(url != ""){
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }else{
                    displayAlert(getString(R.string.mobile_url_opening_error))
                }
            }else{
                team_edit_mode = false;
                view_cancel_story_button.setText(view_text)
                teamWysiwygEditor.html = team_page_placeholder_html
                edit_save_button.setText(edit_text)
                teamWysiwygEditor.setInputEnabled(false)
                findViewById<HorizontalScrollView>(R.id.teamToolbarHorizontalScrollView).setVisibility(View.GONE);
            }


        }
    }

    fun loadEditorData(){
        wysiwygEditor.setEditorHeight(200)
        wysiwygEditor.setEditorFontSize(16)
        wysiwygEditor.setPadding(10, 10, 10, 10)
        wysiwygEditor.setPlaceholder("")

        findViewById<ImageView>(R.id.action_undo).setOnClickListener{ wysiwygEditor.undo() }
        findViewById<ImageView>(R.id.action_redo).setOnClickListener{ wysiwygEditor.redo() }
        findViewById<ImageView>(R.id.action_bold).setOnClickListener{ wysiwygEditor.setBold() }
        findViewById<ImageView>(R.id.action_italic).setOnClickListener{ wysiwygEditor.setItalic() }
        findViewById<ImageView>(R.id.action_underline).setOnClickListener { wysiwygEditor.setUnderline() }
        findViewById<ImageView>(R.id.action_heading1).setOnClickListener{wysiwygEditor.setHeading(1)}
        findViewById<ImageView>(R.id.action_heading2).setOnClickListener{wysiwygEditor.setHeading(2)}
        findViewById<ImageView>(R.id.action_heading3).setOnClickListener{wysiwygEditor.setHeading(3)}
        findViewById<ImageView>(R.id.action_heading4).setOnClickListener{wysiwygEditor.setHeading(4)}
        findViewById<ImageView>(R.id.action_heading5).setOnClickListener{wysiwygEditor.setHeading(5)}
        findViewById<ImageView>(R.id.action_heading6).setOnClickListener{wysiwygEditor.setHeading(6)}
        findViewById<ImageView>(R.id.action_indent).setOnClickListener{wysiwygEditor.setIndent() }
        findViewById<ImageView>(R.id.action_outdent).setOnClickListener { wysiwygEditor.setOutdent() }
        findViewById<ImageView>(R.id.action_align_left).setOnClickListener{ wysiwygEditor.setAlignLeft() }
        findViewById<ImageView>(R.id.action_align_center).setOnClickListener{ wysiwygEditor.setAlignCenter() }
        findViewById<ImageView>(R.id.action_align_right).setOnClickListener { wysiwygEditor.setAlignRight() }
        findViewById<ImageView>(R.id.action_align_justify).setOnClickListener { wysiwygEditor.setAlignJustifyFull() }
        findViewById<ImageView>(R.id.action_insert_bullets).setOnClickListener { wysiwygEditor.setBullets() }
        findViewById<ImageView>(R.id.action_insert_numbers).setOnClickListener { wysiwygEditor.setNumbers() }

        wysiwygEditor.setInputEnabled(false)
        findViewById<HorizontalScrollView>(R.id.toolbarHorizontalScrollView).setVisibility(View.GONE);

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getPersonalPage/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    val jsonString = response.body?.string();
                    var obj = JSONObject(jsonString);
                    var content = ""
                    if(obj.has("content")){
                        content = (obj.get("content") as String);
                    }
                    val cleanedContent = decodeContent(content);
                    personal_page_placeholder_html = cleanedContent
                    runOnUiThread{
                        wysiwygEditor.html = cleanedContent;
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    wysiwygEditor.html =
                        getResources().getString(R.string.mobile_overview_load_story_error)
                }
            }
        })

        var edit_save_button = findViewById<Button>(R.id.btn_edit_save_story)
        var view_cancel_story_button = findViewById<Button>(R.id.btn_view_story)
        var edit_text = getResources().getString(R.string.mobile_manage_page_share_story_edit);
        var save_text = getResources().getString(R.string.mobile_manage_page_share_story_save);
        var view_text = getResources().getString(R.string.mobile_manage_page_share_story_view);
        var cancel_text = getResources().getString(R.string.mobile_manage_page_share_story_cancel);

        edit_save_button.setOnClickListener {
            var current_mode = edit_save_button.getText().toString();
            wysiwygEditor.setEditorBackgroundColor(Color.parseColor("#FFFFFF"))
            if(!saving_page_content){
                if (!edit_mode) {
                    edit_mode = true;
                    edit_save_button.setText(save_text);
                    view_cancel_story_button.setText(cancel_text);
                    wysiwygEditor.setInputEnabled(true);
                    wysiwygEditor.setEditorBackgroundColor(Color.parseColor("#FFFFFF"))
                    findViewById<HorizontalScrollView>(R.id.toolbarHorizontalScrollView).setVisibility(View.VISIBLE);
                } else {
                    edit_mode = false;
                    saving_page_content = true;
                    edit_save_button.setText(edit_text);
                    wysiwygEditor.setInputEnabled(false);
                    view_cancel_story_button.setText(view_text);
                    findViewById<HorizontalScrollView>(R.id.toolbarHorizontalScrollView).setVisibility(View.GONE);
                    updatePageContent(wysiwygEditor.html as String);
                }
            }
        }

        view_cancel_story_button.setOnClickListener {
            wysiwygEditor.setEditorBackgroundColor(Color.parseColor("#00FFFFFF"))
            if(!edit_mode){
                var url = personal_page_full_url;
                if(url != ""){
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }else{
                    displayAlert(getString(R.string.mobile_url_opening_error))
                }
            }else{
                edit_mode = false;
                view_cancel_story_button.setText(view_text)
                wysiwygEditor.html = personal_page_placeholder_html
                edit_save_button.setText(edit_text)
                wysiwygEditor.setInputEnabled(false)
                findViewById<HorizontalScrollView>(R.id.toolbarHorizontalScrollView).setVisibility(View.GONE);
            }


        }
    }

    fun decodeContent(content: String) : String{
        var decodedContent = content.replace("Ã¢&#8364;&#8221;","—")
        decodedContent = decodedContent.replace("Ã¢&#8364;&#8482;","\'")
        return decodedContent;
    }

    fun openGallery(){
        launcher.launch(
            PickVisualMediaRequest.Builder()
                .setMediaType(ImageOnly)
                .build()
        )
    }

    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor =
                contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)

            parcelFileDescriptor!!.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    fun saveGalleryPhoto(result: Uri){
        var personal_image = findViewById<ImageView>(R.id.personal_page_image);
        var team_image = findViewById<ImageView>(R.id.team_page_image);
        var company_image = findViewById<ImageView>(R.id.company_page_image);

        println(result)

        val photo = uriToBitmap(result)
        val resizedBitmap = resize(photo as Bitmap, 1000, 1000);
        val encodedImage: String = encodeImage(resizedBitmap!!)

        println("ENCODED")
        //println(encodedImage)

        if(photo_picking_type == "personal"){
            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/updatePersonalPageImage/")
            val formBody = FormBody.Builder()
                .add("cons_id", getConsID())
                .add("event_id", getEvent().event_id)
                .add("image", encodedImage)
                .build()

            var request = Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()
            var client = OkHttpClient();
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if(response.code != 200){
                        throw Exception(response.body?.string())
                    }else{
                        val jsonString = response.body?.string();
                        println("JSON STrING")
                        println(jsonString)
                        val url = getSafeStringVariable(JSONObject(jsonString), "image_url")
                        runOnUiThread{
                            personal_image.setImageBitmap(photo)
                            if (url !== null) {
                                Glide.with(this@ManagePage)
                                    .load(url)
                                    .into(personal_image)
                            }
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                }
            })
        }else if (photo_picking_type == "team"){
            //val photo: Bitmap =  MediaStore.Images.Media.getBitmap(this.contentResolver, Uri.parse(result.toString()))
            val resizedBitmap = resize(photo as Bitmap, 1000, 1000);
            val encodedImage: String = encodeImage(resizedBitmap!!)
            println("ENCODED")
            println(encodedImage)
            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/updateTeamPageImage/")
            val formBody = FormBody.Builder()
                .add("cons_id", getConsID())
                .add("event_id", getEvent().event_id)
                .add("image", encodedImage)
                .build()

            var request = Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()
            var client = OkHttpClient();
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if(response.code != 200){
                        throw Exception(response.body?.string())
                    }else{
                        val jsonString = response.body?.string();
                        runOnUiThread{
                            team_image.setImageBitmap(photo)
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                }
            })
        } else if (photo_picking_type == "company"){
            //val photo: Bitmap =  MediaStore.Images.Media.getBitmap(this.contentResolver, Uri.parse( data!!.dataString)   ) d
            val resizedBitmap = resize(photo as Bitmap, 1000, 1000);
            val encodedImage: String = encodeImage(resizedBitmap!!)
            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/updateCompanyPageImage/")
            val formBody = FormBody.Builder()
                .add("cons_id", getConsID())
                .add("event_id", getEvent().event_id)
                .add("image", encodedImage)
                .build()

            var request = Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()
            var client = OkHttpClient();
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if(response.code != 200){
                        throw Exception(response.body?.string())
                    }else{
                        val jsonString = response.body?.string();
                        runOnUiThread{
                            company_image.setImageBitmap(photo)
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                }
            })
        }
    }

    override fun childviewCallback(newUrl: String, data: String){
        if(newUrl == "gallery"){
            photo_picking_type = "personal"
            openGallery()
        }else if (newUrl == "camera"){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1040);
            }else{
                launchCamera("personal")
            }
        }else if(newUrl == "team_gallery"){
            photo_picking_type = "team"
            openGallery()
        }else if (newUrl == "team_camera"){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1050);
            }else{
                launchCamera("team")
            }
        }else if(newUrl == "company_gallery"){
            photo_picking_type = "company"
            openGallery()
        }else if (newUrl == "company_camera"){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1060);
            }else{
                launchCamera("company")
            }
        }else if ( data == "editLink"){
            personal_page_custom_text = newUrl;
            personal_page_full_url = personal_page_prefix + personal_page_custom_text
            runOnUiThread{
                togglePersonalUrl()
            }
        }else if ( data == "editTeamLink"){
            team_page_custom_text = newUrl;
            team_page_full_url = team_page_prefix + team_page_custom_text
            runOnUiThread{
                toggleTeamUrl()
            }
        }else if ( data == "editCompanyLink"){
            company_page_custom_text = newUrl;
            company_page_full_url = company_page_prefix + company_page_custom_text
            runOnUiThread{
                toggleCompanyUrl()
            }
        }
    }

    fun launchCamera(type:String){
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if(type == "team"){
            startActivityForResult(takePictureIntent, 1003)
        }else if(type == "company"){
            startActivityForResult(takePictureIntent, 1004)
        }else {
            startActivityForResult(takePictureIntent, 1002)
        }
        return
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        var personal_image = findViewById<ImageView>(R.id.personal_page_image);
        var team_image = findViewById<ImageView>(R.id.team_page_image);
        var company_image = findViewById<ImageView>(R.id.company_page_image);
        super.onActivityResult(requestCode, resultCode, data)

        println("CODE: " + requestCode + " | " + resultCode)
        println(Activity.RESULT_OK)
        if (resultCode == Activity.RESULT_OK && requestCode == 1040) {
            launchCamera("personal")
        }

        if (resultCode == Activity.RESULT_OK && requestCode == 1050) {
            launchCamera("team")
        }

        if (resultCode == Activity.RESULT_OK && requestCode == 1060) {
            launchCamera("company")
        }

        if (resultCode == Activity.RESULT_OK && requestCode == 1002) {
            val photo = data?.getExtras()?.get("data")
            //val photo: Bitmap =  MediaStore.Images.Media.getBitmap(this.contentResolver, Uri.parse( data!!.dataString)   ) d
            val resizedBitmap = resize(photo as Bitmap, 1000, 1000);
            val encodedImage: String = encodeImage(resizedBitmap!!)
            println("ENCODED")
            println(encodedImage)
            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/updatePersonalPageImage/")
            val formBody = FormBody.Builder()
                .add("cons_id", getConsID())
                .add("event_id", getEvent().event_id)
                .add("image", encodedImage)
                .build()

            var request = Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()
            var client = OkHttpClient();
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if(response.code != 200){
                        throw Exception(response.body?.string())
                    }else{
                        val jsonString = response.body?.string();
                        runOnUiThread{
                            personal_image.setImageBitmap(photo)
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                }
            })
        }

        if (resultCode == Activity.RESULT_OK && requestCode == 1003) {
            val photo = data?.getExtras()?.get("data")
            val resizedBitmap = resize(photo as Bitmap, 1000, 1000);
            val encodedImage: String = encodeImage(resizedBitmap!!)
            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/updateTeamPageImage/")
            val formBody = FormBody.Builder()
                .add("cons_id", getConsID())
                .add("event_id", getEvent().event_id)
                .add("image", encodedImage)
                .build()
            var request = Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()
            var client = OkHttpClient();
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if(response.code != 200){
                        throw Exception(response.body?.string())
                    }else{
                        val jsonString = response.body?.string();
                        runOnUiThread{
                            team_image.setImageBitmap(photo)
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                }
            })
        }
        if (resultCode == Activity.RESULT_OK && requestCode == 1004) {
            val photo = data?.getExtras()?.get("data")
            val resizedBitmap = resize(photo as Bitmap, 1000, 1000);
            val encodedImage: String = encodeImage(resizedBitmap!!)
            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/updateCompanyPageImage/")
            val formBody = FormBody.Builder()
                .add("cons_id", getConsID())
                .add("event_id", getEvent().event_id)
                .add("image", encodedImage)
                .build()

            var request = Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()
            var client = OkHttpClient();
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if(response.code != 200){
                        throw Exception(response.body?.string())
                    }else{
                        val jsonString = response.body?.string();
                        runOnUiThread{
                            company_image.setImageBitmap(photo)
                        }
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                }
            })
        }

        if (resultCode == Activity.RESULT_OK && requestCode == 1001){
            var personal_image = findViewById<ImageView>(R.id.personal_page_image);

            val imageUri: Uri = data?.data!!
            val imageStream: InputStream? = contentResolver.openInputStream(imageUri)
            val selectedImage = BitmapFactory.decodeStream(imageStream)
            val resizedBitmap = resize(selectedImage, 1000, 1000);
            val encodedImage: String = encodeImage(resizedBitmap!!)

            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/updatePersonalPageImage/")
            val formBody = FormBody.Builder()
                .add("cons_id", getConsID())
                .add("event_id", getEvent().event_id)
                .add("image", encodedImage)
                .build()

            var request = Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()
            var client = OkHttpClient();
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if(response.code != 200){
                        throw Exception(response.body?.string())
                    }else{
                        val jsonString = response.body?.string();
                        runOnUiThread{
                            personal_image.setImageURI(data?.data)
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                }
            })
        }

        if (resultCode == Activity.RESULT_OK && requestCode == 1000){
            var team_image = findViewById<ImageView>(R.id.team_page_image);
            val imageUri: Uri = data?.data!!
            val imageStream: InputStream? = contentResolver.openInputStream(imageUri)
            val selectedImage = BitmapFactory.decodeStream(imageStream)
            val resizedBitmap = resize(selectedImage, 1000, 1000);
            val encodedImage: String = encodeImage(resizedBitmap!!)

            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/updateTeamPageImage/")
            val formBody = FormBody.Builder()
                .add("cons_id", getConsID())
                .add("event_id", getEvent().event_id)
                .add("image", encodedImage)
                .build()

            var request = Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()

            var client = OkHttpClient();
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if(response.code != 200){
                        throw Exception(response.body?.string())
                    }else{
                        val jsonString = response.body?.string();
                        runOnUiThread{
                            team_image.setImageURI(data?.data)
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                }
            })
        }

        if (resultCode == Activity.RESULT_OK && requestCode == 1010){
            var company_image = findViewById<ImageView>(R.id.company_page_image);
            val imageUri: Uri = data?.data!!
            val imageStream: InputStream? = contentResolver.openInputStream(imageUri)
            val selectedImage = BitmapFactory.decodeStream(imageStream)
            val resizedBitmap = resize(selectedImage, 1000, 1000);
            val encodedImage: String = encodeImage(resizedBitmap!!)

            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/updateCompanyPageImage/")
            val formBody = FormBody.Builder()
                .add("cons_id", getConsID())
                .add("event_id", getEvent().event_id)
                .add("image", encodedImage)
                .build()

            var request = Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()

            var client = OkHttpClient();
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if(response.code != 200){
                        throw Exception(response.body?.string())
                    }else{
                        val jsonString = response.body?.string();
                        runOnUiThread{
                            company_image.setImageURI(data?.data)
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                }
            })
        }
    }

    private fun resize(image: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap? {
        var image = image
        return if (maxHeight > 0 && maxWidth > 0) {
            val width = image.width
            val height = image.height
            val ratioBitmap = width.toFloat() / height.toFloat()
            val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
            var finalWidth = maxWidth
            var finalHeight = maxHeight
            if (ratioMax > ratioBitmap) {
                finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
            } else {
                finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true)
            image
        } else {
            image
        }
    }

    private fun switchSlide(newSlide:String){
        val firstSlide = findViewById<LinearLayout>(R.id.manage_page_personal_container);
        val firstSlideHeader = findViewById<LinearLayout>(R.id.manage_page_personal_heading_container)
        val secondSlide = findViewById<LinearLayout>(R.id.manage_page_team_container);
        val secondSlideHeader = findViewById<LinearLayout>(R.id.manage_page_team_heading_container);
        val thirdSlide = findViewById<LinearLayout>(R.id.manage_page_company_container);
        val thirdSlideHeader = findViewById<LinearLayout>(R.id.manage_page_company_heading_container);
        val personalButton = findViewById<FrameLayout>(R.id.manage_page_personal_button);
        val teamButton = findViewById<FrameLayout>(R.id.manage_page_team_button);
        val companyButton = findViewById<FrameLayout>(R.id.manage_page_company_button);

        setCustomToggleButtonColor(personalButton,"inactive")
        setCustomToggleButtonColor(teamButton,"inactive")
        setCustomToggleButtonColor(companyButton,"inactive")

        setToggleButtonVO(personalButton, getString(R.string.mobile_manage_page_toggle_personal_button), false)
        setToggleButtonVO(teamButton, getString(R.string.mobile_manage_page_toggle_team_button), false)
        setToggleButtonVO(companyButton, getString(R.string.mobile_manage_page_toggle_company_button), false)

        if(newSlide == "personal"){
            firstSlide.setVisibility(View.VISIBLE);
            firstSlideHeader.setVisibility(View.VISIBLE)
            secondSlide.setVisibility(View.GONE);
            secondSlideHeader.setVisibility(View.GONE);
            thirdSlide.setVisibility(View.GONE);
            thirdSlideHeader.setVisibility(View.GONE);
            setCustomToggleButtonColor(personalButton,"active");
            setToggleButtonVO(personalButton, getString(R.string.mobile_manage_page_toggle_personal_button), true)
            findViewById<ImageView>(R.id.manage_page_personal_button_button_back_image).setColorFilter(
                Color.parseColor("#ffffff")
            )
            findViewById<TextView>(R.id.manage_page_personal_button_button_back_text).setTextColor(
                ColorStateList.valueOf(Color.parseColor("#ffffff"))
            )
        } else if (newSlide == "team") {
            firstSlide.setVisibility(View.GONE);
            firstSlideHeader.setVisibility(View.GONE)
            secondSlide.setVisibility(View.VISIBLE);
            secondSlideHeader.setVisibility(View.VISIBLE);
            thirdSlide.setVisibility(View.GONE);
            thirdSlideHeader.setVisibility(View.GONE);
            setCustomToggleButtonColor(teamButton,"active");
            setToggleButtonVO(teamButton, getString(R.string.mobile_manage_page_toggle_team_button), true)
            findViewById<TextView>(R.id.manage_page_team_button_button_back_text).setTextColor(
                ColorStateList.valueOf(Color.parseColor("#ffffff"))
            )
        } else if (newSlide == "company") {
            firstSlide.setVisibility(View.GONE);
            firstSlideHeader.setVisibility(View.GONE)
            secondSlide.setVisibility(View.GONE);
            secondSlideHeader.setVisibility(View.GONE);
            thirdSlide.setVisibility(View.VISIBLE);
            thirdSlideHeader.setVisibility(View.VISIBLE);
            setCustomToggleButtonColor(companyButton,"active");
            setToggleButtonVO(companyButton, getString(R.string.mobile_manage_page_toggle_company_button), true)
            findViewById<TextView>(R.id.manage_page_company_button_button_back_text).setTextColor(
                ColorStateList.valueOf(Color.parseColor("#ffffff"))
            )
        }
    }

    private fun encodeImage(bm: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b: ByteArray = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    fun loadImage(url: String){
        var personal_image = findViewById<ImageView>(R.id.personal_page_image);
        if (url !== null) {
            Glide.with(this)
                .load(url)
                .into(personal_image)
        }
    }

    fun loadTeamImage(url: String){
        var team_image = findViewById<ImageView>(R.id.team_page_image);
        if (url !== null) {
            Glide.with(this)
                .load(url)
                .into(team_image)
        }
    }

    fun loadCompanyImage(url: String){
        var company_image = findViewById<ImageView>(R.id.company_page_image);
        if (url !== null) {
            Glide.with(this)
                .load(url)
                .into(company_image)
        }
    }

    fun getManagePageTitle(type: String): String{
        if(type == "personal"){
            return getStringVariable("USER_FIRST_NAME").uppercase() + getString(R.string.mobile_manage_page_personalize_plural_title) + " " + getString(R.string.mobile_manage_page_personalize_custom_page_title)
        }else if (type == "team") {
            return getStringVariable("USER_TEAM_NAME").uppercase() + " " + getString(R.string.mobile_manage_page_personalize_custom_team_page_title)
        }else if (type == "company") {
            return getStringVariable("USER_COMPANY_NAME").uppercase() + " " + getString(R.string.mobile_manage_page_personalize_custom_company_page_title)
        }else{
            return ""
        }
    }
}