package com.nuclavis.rospark

import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.app.Activity
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import app.com.kotlinapp.OnSwipeTouchListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.comm100.livechat.ChatActivity
import com.comm100.livechat.VisitorClientInterface
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.CallbackManager.Factory.create
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk.sdkInitialize
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.facebook.share.model.ShareContent
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.widget.ShareDialog
import com.miteksystems.misnap.controller.b.i
import com.nuclavis.rospark.databinding.*
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.system.exitProcess

open class BaseActivity : com.nuclavis.rospark.BaseLanguageActivity() {
    override fun childviewCallback(string: String, data: String) {

    }

    override fun slideButtonCallback(card: Any, forward:Boolean){}

    private var xDistance = 0f
    private var yDistance = 0f
    private var lastX = 0f
    private var lastY = 0f

    fun getLocalBitmapUri(imageView: ImageView): Uri? {
        val drawable = imageView.drawable
        val bmp: Bitmap? = if (drawable is BitmapDrawable) {
            (imageView.drawable as BitmapDrawable).bitmap
        } else {
            return null
        }
        var bmpUri: Uri? = null
        try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "share_image_" + System.currentTimeMillis() + ".png"
            )
            file.parentFile?.mkdirs()
            val out = FileOutputStream(file)
            bmp?.compress(Bitmap.CompressFormat.PNG, 90, out)
            out.close()
            bmpUri = FileProvider.getUriForFile(
                Objects.requireNonNull(getApplicationContext()),
                BuildConfig.APPLICATION_ID + ".provider",file)
        } catch (e: IOException) {
            println("GET LOCAL BITMAP URI ERROR")
        }
        return bmpUri
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
         val binding: ActivityBaseBinding = DataBindingUtil.setContentView(
            this, R.layout.activity_base)
        binding.colorList = getColorList("")
        val background = findViewById<ImageView>(R.id.page_background)
        try{
            if(getStringVariable("BACKGROUND_IMAGE_ENABLED") == "true"){
                val media = getStringVariable("CUSTOM_BACKGROUND_URL")
                val drawableId = getStringVariable("CUSTOM_BACKGROUND_DRAWABLE").toInt()

                if(drawableId != 0){
                    background.setImageDrawable(getDrawable(drawableId))
                }else{
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
        }catch(e: Exception){
            background.setImageResource(android.R.color.transparent);
            background.setBackgroundColor(Color.WHITE)
        }
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            if(e.message == "{\"statusCode\":401,\"message\":\"Unauthorized\"}"){
                Logout(false)
            } 
           
            println("Uncaught Exception");

            val erm = e.stackTraceToString()
            val url = getResources().getString(R.string.base_server_url).plus("/client/error")
        
            val formBody = FormBody.Builder()
                .add("client_code", getStringVariable("CLIENT_CODE"))
                .add("cons_id", getConsID())
                .add("error_msg", erm)
                .build()
                    
            var request = Request.Builder().url(url)
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .post(formBody)
                .build()

            var client = OkHttpClient();
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val jsonString = response.body?.string();
                    if(jsonString is String) {
                        if(jsonString.contains("Bad Gateway")){
                            val intent = Intent(this@BaseActivity, Error::class.java);
                            startActivity(intent);
                        }else{
                            if(e is RuntimeException){
                                exitProcess(0)
                            }
                        }
                    }else{
                        if(e is RuntimeException){
                            exitProcess(0)
                        }
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    println("Error logging error")
                    println(e.message.toString())
                    if(e is RuntimeException){
                        exitProcess(0)
                    }
                }
            })
        }

        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

        val menu_items = findViewById<VerticalScrollView>(R.id.main_menu_view_scroll_view)
        menu_items.setOnTouchListener(object : OnSwipeTouchListener(this@BaseActivity) {

            override fun onSwipeLeft() {
                super.onSwipeLeft()

            }
            override fun onSwipeRight() {
                super.onSwipeRight()
            }

            override fun onTouch(
                view: View,
                motionEvent: MotionEvent
            ): Boolean {
                super.onTouch(view, motionEvent)
                when (motionEvent?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        run {
                            yDistance = 0f
                            xDistance = yDistance
                        }
                        lastX = motionEvent.x
                        lastY = motionEvent.y
                    } MotionEvent.ACTION_UP -> {
                        val curX = motionEvent.x
                        val curY = motionEvent.y
                        val direction=(curX - lastX)<0
                        xDistance += Math.abs(curX - lastX)
                        yDistance += Math.abs(curY - lastY)
                        lastX = curX
                        lastY = curY

                        if(direction) {
                            if (xDistance > yDistance && (xDistance > 100 && yDistance < xDistance)) {
                                hideMenu()
                            }else{
                                println("TOO SMALL SWIPE")
                            }
                        }else{
                            println("WRONG DIRECTION")
                        }
                    }
                }
                return true
            }
        })


        hideAlert()
        toggleMenuVisibility()
        setMenuContent()
        setMenuLinks()

        if(getEvents().size > 1){
            toggleSwitchEventsVisibility(true)
        }else{
            toggleSwitchEventsVisibility(false)
        }
        val team_button = findViewById<LinearLayout>(R.id.menu_option_teams);
        val recruit_button = findViewById<LinearLayout>(R.id.menu_option_recruit);
        
        if(getStringVariable("HAS_TEAM") == "true" && getStringVariable("IS_EVENT_MANAGER_ONLY") != "true"){
            if (getStringVariable("IS_TEAM_CAPTAIN") == "true") {
                if(getStringVariable("MANAGE_TEAM_ENABLED") == "false"){
                    team_button.setVisibility(View.GONE)
                }else{
                    team_button.setVisibility(View.VISIBLE)
                }     
                recruit_button.setVisibility(View.VISIBLE)
            } else {
                if (getStringVariable("MY_TEAM_DISABLED_TEAM_MEMBERS") == "true") {
                    team_button.setVisibility(View.GONE)
                }else{
                    if(getStringVariable("MANAGE_TEAM_ENABLED") == "false"){
                        team_button.setVisibility(View.GONE)
                    }else{
                        team_button.setVisibility(View.VISIBLE)
                    }
                }
                if (getStringVariable("RECRUIT_DISABLED_TEAM_MEMBERS") == "true") {
                    recruit_button.setVisibility(View.GONE)
                }else{
                    recruit_button.setVisibility(View.VISIBLE)
                }
            }
        }else{
            team_button.setVisibility(View.GONE)
            recruit_button.setVisibility(View.GONE)
        }
    }

    fun shareFacebook(activity: Activity, url: String) {
        try {
            var facebookAppFound = false
            var facebookDisabled = true
            var shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, url)
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(url))
            val pm = activity.packageManager
            val activityList = pm.queryIntentActivities(shareIntent, 0)

            for (app in activityList) {
                if (app.activityInfo.packageName.contains("com.facebook.katana")) {
                    facebookAppFound = true;
                    break
                }
            }

            if (!facebookAppFound) {
                var encodedUrl = Uri.encode(url)
                val shareUrl = "https://www.facebook.com/sharer/sharer.php?u=$encodedUrl"
                shareIntent = Intent(Intent.ACTION_VIEW, Uri.parse(shareUrl))
                activity.startActivity(shareIntent)
            }else{
                try {
                    sdkInitialize(applicationContext)
                    val shareDialog = ShareDialog(this)
                    if (ShareDialog.canShow(ShareLinkContent::class.java)) {
                        val linkContent: ShareLinkContent = ShareLinkContent.Builder()
                            .setContentUrl(Uri.parse(url))
                            .build()
                        shareDialog.show(linkContent)
                    }
                }catch(e: Exception){
                    var encodedUrl = Uri.encode(url)
                    val shareUrl = "https://www.facebook.com/sharer/sharer.php?u=$encodedUrl"
                    shareIntent = Intent(Intent.ACTION_VIEW, Uri.parse(shareUrl))
                    activity.startActivity(shareIntent)
                }
            }
        }catch(exception: Exception){
            displayAlert(getResources().getString(R.string.mobile_fundraise_share_dialog_error))
        }
    }
    fun setTitle(title:String){
        val title_text_view = findViewById<TextView>(R.id.page_title);
        title_text_view.setText(title);
    }

    fun setPageContent(xml: Int, page: String){
        val linear = findViewById<View>(R.id.page_content) as LinearLayout
        for (childView in linear.children) {
            linear.removeView(childView);
        }
        val inflater = layoutInflater

        if(page == "overview"){
            val binding: OverviewBinding = DataBindingUtil.inflate(
                inflater, xml,linear, true)
            binding.colorList = getColorList("")
        }else if(page == "fundraise"){
            val binding: FundraiseBinding = DataBindingUtil.inflate(
                inflater, xml,linear, true)
            binding.colorList = getColorList("")
        }else if(page == "donations"){
            val binding: DonationsBinding = DataBindingUtil.inflate(
                inflater, xml,linear, true)
            binding.colorList = getColorList("")
        }else if(page == "checkDeposit"){
            val binding: CheckDepositBinding = DataBindingUtil.inflate(
                inflater, xml,linear, true)
            binding.colorList = getColorList("")
        }else if(page == "gifts"){
            val binding: GiftsBinding = DataBindingUtil.inflate(
                inflater, xml,linear, true)
            binding.colorList = getColorList("")
        }else if(page == "recruit"){
            val binding: RecruitBinding = DataBindingUtil.inflate(
                inflater, xml,linear, true)
            binding.colorList = getColorList("")
        }else if(page == "teams") {
            val binding: TeamsBinding = DataBindingUtil.inflate(
                inflater, xml, linear, true)
            binding.colorList = getColorList("")
        }else if(page == "managePage"){
            val binding: ManagePageBinding = DataBindingUtil.inflate(
                inflater, xml,linear, true)
            binding.colorList = getColorList("")
        }else if(page == "manageSchool"){
            val binding: ManageSchoolBinding = DataBindingUtil.inflate(
                inflater, xml,linear, true)
            binding.colorList = getColorList("")
        }else if(page == "manageCompany"){
            val binding: ManageCompanyBinding = DataBindingUtil.inflate(
                inflater, xml,linear, true)
            binding.colorList = getColorList("")
        }else if(page == "games"){
            val binding: GamesBinding = DataBindingUtil.inflate(
                inflater, xml,linear, true)
            binding.colorList = getColorList("")
        }else if(page == "trackActivity"){
            val binding: TrackActivityBinding = DataBindingUtil.inflate(
                inflater, xml,linear, true)
            binding.colorList = getColorList("")
        }else if(page == "challenges"){
            val binding: ChallengesBinding = DataBindingUtil.inflate(
                inflater, xml,linear, true)
            binding.colorList = getColorList("")
        }else if(page == "trainingGuide"){
            val binding: TrainingGuideBinding = DataBindingUtil.inflate(
                inflater, xml,linear, true)
            binding.colorList = getColorList("")
        }else if(page == "gallery"){
            val binding: GalleryBinding = DataBindingUtil.inflate(
                inflater, xml,linear, true)
            binding.colorList = getColorList("")
        }
            /*else if(page == "meetChallenges"){
            val binding: MeetChallengeBinding = DataBindingUtil.inflate(
                inflater, xml,linear, true)
            binding.colorList = getColorList("")
        }
        }*/
        updateView()
    }

    fun toggleSwitchEventsVisibility(show: Boolean){
        if(show) {
            findViewById<LinearLayout>(R.id.menu_option_switch_events).setVisibility(View.VISIBLE)
        }else{
            findViewById<LinearLayout>(R.id.menu_option_switch_events).setVisibility(View.GONE)
        }
    }

    fun toggleMenuVisibility(){
        if(getStringVariable("IS_EVENT_MANAGER_ONLY") == "true"){
            findViewById<LinearLayout>(R.id.menu_option_overview).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.menu_option_fundraise).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.menu_option_donations).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.menu_option_manage_page).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.menu_option_teams).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.menu_option_recruit).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.menu_option_track_activity).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.menu_option_switch_events).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.menu_option_challenges).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.menu_option_gallery).setVisibility(View.GONE)
        }
    }

    fun hideMenu(){
        val menu_view = findViewById<LinearLayout>(R.id.main_menu_view);
        ObjectAnimator.ofFloat(menu_view, "translationX", -900f).apply {
            duration = 600
            start()
        }
        findViewById<ImageView>(R.id.menu_hamburger_icon).contentDescription = getString(R.string.mobile_menu_description_collapsed)
    }

    fun setMenuContent(){
        val menu_view = findViewById<LinearLayout>(R.id.main_menu_view);
        menu_view.setVisibility(View.INVISIBLE);
        hideMenu()
        findViewById<LinearLayout>(R.id.menu_overlay).setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN ->
                        hideMenu()
                }
                return v?.onTouchEvent(event) ?: true
            }
        })

        findViewById<LinearLayout>(R.id.main_menu_view).setOnTouchListener(object : OnSwipeTouchListener(this@BaseActivity) {
            override fun onSwipeLeft() {
                super.onSwipeLeft();
                println("Swipe left to close menu activated")
                //hideMenu()
            }
        })

        val menu_button = findViewById<ImageView>(R.id.menu_hamburger_icon);
        menu_button.setOnClickListener {
            val menu_view = findViewById<LinearLayout>(R.id.main_menu_view);
            val location: IntArray = intArrayOf(1,2)
            menu_view.getLocationOnScreen(location);

            if(location[0] == 0){
                ObjectAnimator.ofFloat(menu_view, "translationX", -900f).apply {
                    duration = 600
                    start()
                }
                findViewById<ImageView>(R.id.menu_hamburger_icon).contentDescription = getString(R.string.mobile_menu_description_collapsed)
            }else{
                menu_view.setVisibility(View.VISIBLE);
                ObjectAnimator.ofFloat(menu_view, "translationX", 0f).apply {
                    duration = 400
                    start()
                }
                findViewById<ImageView>(R.id.menu_hamburger_icon).contentDescription = getString(R.string.mobile_menu_description_expanded)
            }
        }
    }

    fun setupMessage(obj: JSONObject): StandardMessage{
        var bool = false
        var email_body = ""
        var text = ""
        var subject = ""
        var url = ""
        var facebook_url = ""
        var email_url = ""
        var sms_url = ""
        var linkedin_url = ""

        if(obj.has("message")){
            text = obj.get("message") as String
        }
        if(obj.has("email_body")){
            email_body = obj.get("email_body") as String
        }
        if(obj.has("subject")){
            subject = obj.get("subject") as String
        }
        if(obj.has("url") && obj.get("url") is String){
            url = obj.get("url") as String
        }
        if(obj.has("facebook_url") && obj.get("facebook_url") is String){
            facebook_url = obj.get("facebook_url") as String
        }else{
            facebook_url = url
        }
        if(obj.has("email_url") && obj.get("email_url") is String){
            email_url = obj.get("email_url") as String
        }else{
            email_url = url
        }
        if(obj.has("sms_url") && obj.get("sms_url") is String){
            sms_url = obj.get("sms_url") as String
        }else{
            sms_url = url
        }
        if(obj.has("linkedin_url") && obj.get("linkedin_url") is String){
            linkedin_url = obj.get("linkedin_url") as String
        }else{
            linkedin_url = url
        }
        if(obj.has("hide_content")){
            if(obj.get("hide_content") as Int == 1){
                bool = true
            }
        }
        return StandardMessage(text, email_body, subject, url, facebook_url, linkedin_url, email_url, sms_url, bool)
    }
    
    //BEGIN_FACEBOOK_CONTENT
    fun loadFbFundraiserData(facebookCard: View, initial: Boolean){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getFacebookFundraiser/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                if(response.code != 200){
                    //throw Exception(response.body?.string())
                    val fbFundraiserCreateUrl = getStringVariable("FACEBOOK_FUNDRAISER_CREATE_URL")

                    runOnUiThread {
                        val fbLoginButton = findViewById<Button>(R.id.fb_login_button)
                        fbLoginButton.setVisibility(View.GONE)
                        val fbFundraiserCreating =
                            findViewById<TextView>(R.id.facebook_overview_card_creating)
                        fbFundraiserCreating.setVisibility(View.GONE)
                        val fbFundraiserTitle =
                            findViewById<TextView>(R.id.facebook_overview_card_title)
                        fbFundraiserTitle.setText(R.string.mobile_overview_facebook_connect_create_a_facebook_fundraiser)

                        val fbFundraiserGetStarted =
                            findViewById<Button>(R.id.overview_btn_facebook_connect)

                        if (fbFundraiserCreateUrl.isNotEmpty()) {
                            fbFundraiserGetStarted.setText(R.string.mobile_overview_facebook_connect_go_to_website)
                            fbFundraiserGetStarted.setVisibility(View.VISIBLE)
                            fbFundraiserGetStarted.setOnClickListener {
                                val browserIntent =
                                    Intent(Intent.ACTION_VIEW, Uri.parse(fbFundraiserCreateUrl))
                                startActivity(browserIntent)
                            }
                        } else {
                            fbFundraiserGetStarted.setVisibility(View.VISIBLE)
                        }

                        findViewById<TextView>(R.id.facebook_overview_card_details).setVisibility(
                            View.GONE
                        )
                        findViewById<TextView>(R.id.facebook_overview_url).setVisibility(View.GONE)
                        findViewById<LinearLayout>(R.id.fbFundraiserButtons).setVisibility(View.GONE)

                        fadeInView(facebookCard)
                    }
                }else{
                    val jsonString = response.body?.string();
                    val obj = JSONObject(jsonString);

                    val fbFundraiserCreateDisabled = getStringVariable("FACEBOOK_FUNDRAISER_CREATED_DISABLED").toBoolean()
                    val fbFundraiserEnabled = getStringVariable("FACEBOOK_FUNDRAISER_ENABLED").toBoolean()

                    if(obj.has("fundraiser_url") == true && (obj.get("fundraiser_url") as String) != ""){
                        runOnUiThread {
                            setTooltipText(R.id.overview_facebook_help_button,R.string.mobile_overview_facebook_connect_connected_tooltip, R.string.mobile_overview_facebook_connect_title)
                            val fbUrl = obj.get("fundraiser_url") as String;
                            val fbLoginButton = findViewById<Button>(R.id.fb_login_button);
                            fbLoginButton.setVisibility(View.GONE)
                            val fbFundraiserTitle = findViewById<TextView>(R.id.facebook_overview_card_title);
                            fbFundraiserTitle.setText(R.string.mobile_overview_facebook_connect_your_fundraiser_is_connected_title);
                            val fbFundraiserGetStarted = findViewById<Button>(R.id.overview_btn_facebook_connect);
                            fbFundraiserGetStarted.setVisibility(View.GONE)
                            val fbFundraiserDetails = findViewById<TextView>(R.id.facebook_overview_card_details);
                            fbFundraiserDetails.setVisibility(View.VISIBLE)
                            val fbFundraiserUrl = findViewById<TextView>(R.id.facebook_overview_url);
                            fbFundraiserUrl.setVisibility(View.VISIBLE)
                            fbFundraiserUrl.setText(fbUrl);
                            val fbFundraiserCreating = findViewById<TextView>(R.id.facebook_overview_card_creating);
                            fbFundraiserCreating.setVisibility(View.GONE)
                            val fbFundraiserButtons = findViewById<LinearLayout>(R.id.fbFundraiserButtons);
                            fbFundraiserButtons.setVisibility(View.VISIBLE)

                            val fbFundraiserView = findViewById<Button>(R.id.overview_btn_facebook_view);
                            fbFundraiserView.setOnClickListener{
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(obj.get("fundraiser_url").toString()))
                                startActivity(browserIntent)
                            }

                            val fbFundraiserCopy = findViewById<Button>(R.id.overview_btn_facebook_copy);
                            fbFundraiserCopy.setOnClickListener{
                                val myClipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?;
                                val myClip = ClipData.newPlainText("text", obj.get("fundraiser_url").toString());
                                myClipboard?.setPrimaryClip(myClip);
                                displayAlert(getResources().getString(R.string.mobile_overview_facebook_connect_copy_success))
                                setAlertSender(fbFundraiserCopy)
                            }

                            fadeInView(facebookCard);
                        }
                    }else if (fbFundraiserEnabled && fbFundraiserCreateDisabled) {
                        val fbFundraiserCreateUrl = getStringVariable("FACEBOOK_FUNDRAISER_CREATE_URL")

                        runOnUiThread {
                            val fbLoginButton = findViewById<Button>(R.id.fb_login_button)
                            fbLoginButton.setVisibility(View.GONE)
                            val fbFundraiserCreating =
                                findViewById<TextView>(R.id.facebook_overview_card_creating)
                            fbFundraiserCreating.setVisibility(View.GONE)
                            val fbFundraiserTitle =
                                findViewById<TextView>(R.id.facebook_overview_card_title)
                            fbFundraiserTitle.setText(R.string.mobile_overview_facebook_connect_create_a_facebook_fundraiser)

                            val fbFundraiserGetStarted =
                                findViewById<Button>(R.id.overview_btn_facebook_connect)

                            if (fbFundraiserCreateUrl.isNotEmpty()) {
                                fbFundraiserGetStarted.setText(R.string.mobile_overview_facebook_connect_go_to_website)
                                fbFundraiserGetStarted.setVisibility(View.VISIBLE)
                                fbFundraiserGetStarted.setOnClickListener {
                                    val browserIntent =
                                        Intent(Intent.ACTION_VIEW, Uri.parse(fbFundraiserCreateUrl))
                                    startActivity(browserIntent)
                                }
                            } else {
                                fbFundraiserGetStarted.setVisibility(View.VISIBLE)
                            }

                            findViewById<TextView>(R.id.facebook_overview_card_details).setVisibility(
                                View.GONE
                            )
                            findViewById<TextView>(R.id.facebook_overview_url).setVisibility(View.GONE)
                            findViewById<LinearLayout>(R.id.fbFundraiserButtons).setVisibility(View.GONE)

                            fadeInView(facebookCard)
                        }
                    }else{
                        runOnUiThread {
                            setTooltipText(R.id.overview_facebook_help_button,R.string.mobile_overview_facebook_connect_not_connected_tooltip, R.string.mobile_overview_facebook_connect_title)
                            val fbLoginButton = findViewById<Button>(R.id.fb_login_button);
                            fbLoginButton.setVisibility(View.GONE)
                            val fbFundraiserTitle = findViewById<TextView>(R.id.facebook_overview_card_title);
                            fbFundraiserTitle.setText(R.string.mobile_overview_facebook_connect_create_a_facebook_fundraiser);
                            val fbFundraiserGetStarted = findViewById<Button>(R.id.overview_btn_facebook_connect);
                            fbFundraiserGetStarted.setVisibility(View.VISIBLE)
                            val fbFundraiserDetails = findViewById<TextView>(R.id.facebook_overview_card_details);
                            fbFundraiserDetails.setVisibility(View.GONE)
                            val fbFundraiserUrl = findViewById<TextView>(R.id.facebook_overview_url);
                            fbFundraiserUrl.setVisibility(View.GONE)
                            val fbFundraiserCreating = findViewById<TextView>(R.id.facebook_overview_card_creating);
                            fbFundraiserCreating.setVisibility(View.GONE)
                            val fbFundraiserButtons = findViewById<LinearLayout>(R.id.fbFundraiserButtons);
                            fbFundraiserButtons.setVisibility(View.GONE)

                            fbFundraiserGetStarted.setOnClickListener{
                                val fbFundraiserGetStarted = findViewById<Button>(R.id.overview_btn_facebook_connect);
                                fbFundraiserGetStarted.setVisibility(View.GONE)
                                val fbFundraiserCreating = findViewById<TextView>(R.id.facebook_overview_card_creating);
                                fbFundraiserCreating.setVisibility(View.VISIBLE)
                                val loginButton = findViewById<LoginButton>(R.id.fb_login_button)

                                val accessToken = AccessToken.getCurrentAccessToken();

                                if(accessToken != null && accessToken.isExpired == false){
                                    val token = accessToken.token
                                    createFacebookFundraiser(token)
                                }else{
                                    loginButton.performClick();
                                }
                            }

                            val callbackManager = CallbackManager.Factory.create()

                            val loginButton = findViewById<LoginButton>(R.id.fb_login_button)
                            loginButton.setReadPermissions(listOf("public_profile", "email", "manage_fundraisers"))
                            loginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                                override fun onSuccess(loginResult: LoginResult) {
                                    val token = loginResult.accessToken.token
                                    createFacebookFundraiser(token)
                                }

                                override fun onCancel() {
                                    val fbFundraiserGetStarted = findViewById<Button>(R.id.overview_btn_facebook_connect);
                                    fbFundraiserGetStarted.setVisibility(View.VISIBLE)
                                    val fbFundraiserCreating = findViewById<TextView>(R.id.facebook_overview_card_creating);
                                    fbFundraiserCreating.setVisibility(View.GONE)
                                }

                                override fun onError(exception: FacebookException) {
                                    val fbFundraiserGetStarted = findViewById<Button>(R.id.overview_btn_facebook_connect);
                                    fbFundraiserGetStarted.setVisibility(View.VISIBLE)
                                    val fbFundraiserCreating = findViewById<TextView>(R.id.facebook_overview_card_creating);
                                    fbFundraiserCreating.setVisibility(View.GONE)

                                    displayAlert(getResources().getString(R.string.mobile_overview_facebook_connect_fundraiser_error));
                                    setAlertSender(loginButton)
                                }
                            })

                            fadeInView(facebookCard);
                        }
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
            }
        })
    }

    fun loadFacebookCard(inflater: LayoutInflater, parent: LinearLayout, index: Int = 0){
        val facebook_card: OverviewFacebookCardBinding = DataBindingUtil.inflate(
            inflater, R.layout.overview_facebook_card,parent, false)
        facebook_card.colorList = getColorList("")

        if(index > 0){
            parent.addView(facebook_card.root, index)
        }else{
            parent.addView(facebook_card.root)
        }

        val facebook_connect_button = findViewById<Button>(R.id.overview_btn_facebook_connect);
        facebook_connect_button.setOnClickListener {
            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder.setMessage("Get Started Facebook Button Has Been Clicked")
                .setCancelable(false)
                .setNegativeButton("Close Alert", DialogInterface.OnClickListener { dialog, id ->
                    dialog.cancel()
                })

            val alert = dialogBuilder.create()
            alert.setTitle("Alert")
            alert.show()
        }

        var facebookCardV = findViewById<LinearLayout>(R.id.overview_facebook_card);
        loadFbFundraiserData(facebookCardV, true)
    }

    fun createFacebookFundraiser(token: String){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/createFacebookFundraiser")

        val formBody = FormBody.Builder().add("cons_id", getConsID()).add("event_id", getEvent().event_id).add("token", token.toString())
            .build()

        var request = Request.Builder().url(url)
            .post(formBody)
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .build()

        var client = OkHttpClient();
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    val jsonString = response.body?.string();
                    if(jsonString is String) {
                        val obj = JSONObject(jsonString)
                        if(obj.has("fundraiser_url") && obj.get("fundraiser_url") != ""){
                            runOnUiThread {
                                val fbFundraiserTitle = findViewById<TextView>(R.id.facebook_overview_card_title);
                                fbFundraiserTitle.setText(R.string.mobile_overview_facebook_connect_your_fundraiser_is_connected_title);
                                val fbFundraiserGetStarted = findViewById<Button>(R.id.overview_btn_facebook_connect);
                                fbFundraiserGetStarted.setVisibility(View.GONE)
                                val fbFundraiserDetails = findViewById<TextView>(R.id.facebook_overview_card_details);
                                fbFundraiserDetails.setVisibility(View.VISIBLE)
                                val fbFundraiserUrl = findViewById<TextView>(R.id.facebook_overview_url);
                                fbFundraiserUrl.setVisibility(View.VISIBLE)
                                fbFundraiserUrl.setText(obj.get("fundraiser_url").toString());
                                val fbFundraiserCreating = findViewById<TextView>(R.id.facebook_overview_card_creating);
                                fbFundraiserCreating.setVisibility(View.GONE)
                                val fbFundraiserButtons = findViewById<LinearLayout>(R.id.fbFundraiserButtons);
                                fbFundraiserButtons.setVisibility(View.VISIBLE)

                                val fbFundraiserView = findViewById<Button>(R.id.overview_btn_facebook_view);
                                fbFundraiserView.setOnClickListener{
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(obj.get("fundraiser_url").toString()))
                                    startActivity(browserIntent)
                                }

                                val fbFundraiserCopy = findViewById<Button>(R.id.overview_btn_facebook_copy);
                                fbFundraiserCopy.setOnClickListener{
                                    val myClipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?;
                                    val myClip = ClipData.newPlainText("text", obj.get("fundraiser_url").toString());
                                    myClipboard?.setPrimaryClip(myClip);
                                    displayAlert(getResources().getString(R.string.mobile_overview_facebook_connect_copy_success))
                                    setAlertSender(fbFundraiserCopy)
                                }
                            }
                        } else{
                            runOnUiThread{
                                val fbFundraiserGetStarted = findViewById<Button>(R.id.overview_btn_facebook_connect);
                                fbFundraiserGetStarted.setVisibility(View.VISIBLE)
                                val fbFundraiserCreating = findViewById<TextView>(R.id.facebook_overview_card_creating);
                                fbFundraiserCreating.setVisibility(View.GONE)
                                displayAlert(getResources().getString(R.string.mobile_overview_facebook_connect_fundraiser_error));
                            }
                        }
                    }else {
                        runOnUiThread{
                            val fbFundraiserGetStarted = findViewById<Button>(R.id.overview_btn_facebook_connect);
                            fbFundraiserGetStarted.setVisibility(View.VISIBLE)
                            val fbFundraiserCreating = findViewById<TextView>(R.id.facebook_overview_card_creating);
                            fbFundraiserCreating.setVisibility(View.GONE)
                            displayAlert(getResources().getString(R.string.mobile_overview_facebook_connect_fundraiser_error));
                        }
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread{
                    val fbFundraiserGetStarted = findViewById<Button>(R.id.overview_btn_facebook_connect);
                    fbFundraiserGetStarted.setVisibility(View.VISIBLE)
                    val fbFundraiserCreating = findViewById<TextView>(R.id.facebook_overview_card_creating);
                    fbFundraiserCreating.setVisibility(View.GONE)
                    displayAlert(getResources().getString(R.string.mobile_overview_facebook_connect_fundraiser_error));
                }
            }
        })
    }
    //END_FACEBOOK_CONTENT

    class StandardMessage(
        val text: String,
        val email_body: String,
        val subject: String,
        val url: String,
        val facebook_url: String,
        val linkedin_url: String,
        val email_url: String,
        val sms_url: String,
        val custom_content: Boolean
    )

    fun Logout(isClicked: Boolean){
        clearVariable("EVENT");
        clearVariable("EVENTLIST");

        clearVariable("CONS_ID");
        clearVariable("IS_EVENT_MANAGER_ONLY")
        clearVariable("CHECK_EVENT_MANAGER")
        clearVariable("CHECK_EVENT_MANAGER_EVENT_ID")
        clearVariable("IS_TEAM_CAPTAIN")

        setVariable("INITIAL_LAUNCH","false");

        setVariable("PRIMARY_COLOR",getStringVariable("LOGIN_PRIMARY_COLOR"))

        if(isClicked){
            getBiometricString("CLEAR")
            clearVariable("REMEMBER_ME")
            clearVariable("REMEMBER_ME_USERNAME")
            clearVariable("REMEMBER_ME_PASSWORD")
        }

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/user/logout")
        var request = Request.Builder()
            .url(url)
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .build()

        var login_logo_url = getStringVariable("LOGIN_IMG_URL")
        if(getStringVariable("CONTAINER_APP_TYPE") == "DISPLAY_LIST"){
            if(getStringVariable("SECOND_PAGE_LOGO_URL") != ""){
                login_logo_url = getStringVariable("SECOND_PAGE_LOGO_URL")
            }
        }

        var client = OkHttpClient();
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                clearVariable("JTX_TOKEN");
                var client_class = getStringVariable("CLIENT_CLASS");
                if(client_class == "classy"){
                    val intent = Intent(this@BaseActivity, com.nuclavis.rospark.LoginNoFields::class.java);
                    intent.putExtra("logo_url",login_logo_url);
                    intent.putExtra("initial",true);
                    startActivity(intent);
                    this@BaseActivity.overridePendingTransition(0, 0);
                }else if(client_class == "internal"){
                    val intent = Intent(this@BaseActivity, com.nuclavis.rospark.LoginWithRegister::class.java);
                    intent.putExtra("logo_url",login_logo_url);
                    intent.putExtra("initial_login",false);
                    startActivity(intent);
                    this@BaseActivity.overridePendingTransition(0, 0);
                }else{
                    val intent = Intent(this@BaseActivity, com.nuclavis.rospark.Login::class.java);
                    intent.putExtra("logo_url",login_logo_url);
                    intent.putExtra("initial_login",false);
                    startActivity(intent);
                    this@BaseActivity.overridePendingTransition(0, 0);
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                displayAlert(getResources().getString(R.string.mobile_login_no_events));
            }
        })
    }

    fun setMenuLinks(){
        var is_captain = getStringVariable("IS_TEAM_CAPTAIN")

        if(getStringVariable("DISABLE_CORE_FUNDRAISING_PAGES") == "true"){
            findViewById<LinearLayout>(R.id.menu_option_overview).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.menu_option_fundraise).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.menu_option_donations).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.menu_option_manage_page).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.menu_option_teams).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.menu_option_recruit).setVisibility(View.GONE)
        } else {
            val overview_button = findViewById<LinearLayout>(R.id.menu_option_overview);
            overview_button.setOnClickListener(View.OnClickListener() {
                val intent = Intent(this@BaseActivity, com.nuclavis.rospark.Overview::class.java);
                startActivity(intent);
                this.overridePendingTransition(0, 0);
            })

            val fundraise_button = findViewById<LinearLayout>(R.id.menu_option_fundraise);
            fundraise_button.setOnClickListener(View.OnClickListener() {
                val intent = Intent(this@BaseActivity, com.nuclavis.rospark.Fundraise::class.java);
                startActivity(intent);
                this.overridePendingTransition(0, 0);
            })

            val donations_button = findViewById<LinearLayout>(R.id.menu_option_donations);
            donations_button.setOnClickListener(View.OnClickListener() {
                val intent = Intent(this@BaseActivity, com.nuclavis.rospark.Donations::class.java);
                startActivity(intent);
                this.overridePendingTransition(0, 0);
            })

            val manage_company_button = findViewById<LinearLayout>(R.id.menu_option_manage_company);

            if(getStringVariable("IS_COMPANY_COORDINATOR") == "true"
                && getStringVariable("DISABLE_COMPANY_PAGE") != "true"){
                manage_company_button.setOnClickListener(View.OnClickListener() {
                    val intent = Intent(this@BaseActivity, com.nuclavis.rospark.ManageCompany::class.java);
                    startActivity(intent);
                    this.overridePendingTransition(0, 0);
                });
                manage_company_button.visibility = View.VISIBLE
            }else{
                manage_company_button.visibility = View.GONE
            }

            val manage_page_button = findViewById<LinearLayout>(R.id.menu_option_manage_page);
            if(getStringVariable("IS_EVENT_MANAGER_ONLY") == "true"){
                manage_page_button.setVisibility(View.GONE)
            } else {
                if(getStringVariable("MANAGE_PAGE_ENABLED") == "false"){
                    manage_page_button.setVisibility(View.GONE)
                }else{
                    manage_page_button.setVisibility(View.VISIBLE)
                    manage_page_button.setOnClickListener(View.OnClickListener() {
                        val intent = Intent(this@BaseActivity, com.nuclavis.rospark.ManagePage::class.java);
                        startActivity(intent);
                        this.overridePendingTransition(0, 0);
                    })
                }
            }

            val team_button = findViewById<LinearLayout>(R.id.menu_option_teams);
            team_button.setOnClickListener(View.OnClickListener() {
                val intent = Intent(this@BaseActivity, com.nuclavis.rospark.Teams::class.java);
                startActivity(intent);
                this.overridePendingTransition(0, 0);
            })

            val recruit_button = findViewById<LinearLayout>(R.id.menu_option_recruit);
            recruit_button.setOnClickListener(View.OnClickListener() {
                val intent = Intent(this@BaseActivity, com.nuclavis.rospark.Recruit::class.java);
                startActivity(intent);
                this.overridePendingTransition(0, 0);
            })
        }

        val gifts_button = findViewById<LinearLayout>(R.id.menu_option_gifts);
        gifts_button.setOnClickListener{
            val intent = Intent(this@BaseActivity, Gifts::class.java);
            startActivity(intent);
            this.overridePendingTransition(0, 0);
        }

        if(getStringVariable("HAS_GIFTS") != "" && is_captain != "true") {
            gifts_button.setVisibility(View.VISIBLE)
        }else{
            gifts_button.setVisibility(View.GONE)
        }

        val games_button = findViewById<LinearLayout>(R.id.menu_option_games);
        if(getStringVariable("GAMES_ENABLED") == "true") {
            // games_button.setVisibility(View.GONE)
            games_button.setVisibility(View.VISIBLE)
            games_button.setOnClickListener{
                val intent = Intent(this@BaseActivity, Games::class.java);
                startActivity(intent);
                this.overridePendingTransition(0, 0);
            }
        }else{
            games_button.setVisibility(View.GONE)
        }

        val track_activity_button = findViewById<LinearLayout>(R.id.menu_option_track_activity);
        if(getStringVariable("IS_EVENT_MANAGER_ONLY") == "true"){
            track_activity_button.setVisibility(View.GONE)
        }else{
            if(getStringVariable("ACTIVITY_TRACKING_ENABLED") == "true") {
                track_activity_button.setVisibility(View.VISIBLE)
                track_activity_button.setOnClickListener{
                    val intent = Intent(this@BaseActivity, TrackActivity::class.java);
                    startActivity(intent);
                    this.overridePendingTransition(0, 0);
                }
            }else{
                track_activity_button.setVisibility(View.GONE)
            }
        }

        // val meet_challenge_button = findViewById<LinearLayout>(R.id.menu_option_meet_challenge);
        // meet_challenge_button.setOnClickListener(View.OnClickListener() {
        //     val intent = Intent(this@BaseActivity, MeetChallenge::class.java);
        //     startActivity(intent);
        //     this.overridePendingTransition(0, 0);
        // })

        val menu_option_distance_challenge_leaderboard_button = findViewById<LinearLayout>(R.id.menu_option_distance_challenge_leaderboard);
        val menu_option_distance_challenge_leaderboard_link = getStringVariable("DISTANCE_CHALLENGE_LEADERBOARD_URL")
        if(menu_option_distance_challenge_leaderboard_link != ""){
            menu_option_distance_challenge_leaderboard_button.setVisibility(View.VISIBLE)
            menu_option_distance_challenge_leaderboard_button.setOnClickListener{
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(menu_option_distance_challenge_leaderboard_link))
                startActivity(browserIntent)
            }
        } else {
            menu_option_distance_challenge_leaderboard_button.setVisibility(View.GONE)
        }

        val mobile_main_menu_about_event_button = findViewById<LinearLayout>(R.id.menu_option_about_event);
        val mobile_main_menu_about_event_link = getStringVariable("ABOUT_EVENT_URL")
        if(mobile_main_menu_about_event_link != ""){
            mobile_main_menu_about_event_button.setVisibility(View.VISIBLE)
            mobile_main_menu_about_event_button.setOnClickListener{
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mobile_main_menu_about_event_link))
                startActivity(browserIntent)
            }
        } else {
            mobile_main_menu_about_event_button.setVisibility(View.GONE)
        }

        var gallery_enabled = getStringVariable("GALLERY_ENABLED") == "true";
        val gallery_button = findViewById<LinearLayout>(R.id.menu_option_gallery);

        if(getStringVariable("IS_EVENT_MANAGER_ONLY") == "true"){
            gallery_button.setVisibility(View.GONE)
        } else {
            if(gallery_enabled){
                gallery_button.setVisibility(View.VISIBLE)
                gallery_button.setOnClickListener(View.OnClickListener() {
                    val intent = Intent(this@BaseActivity, Gallery::class.java);
                    startActivity(intent);
                    this.overridePendingTransition(0, 0);
                })
            }else{
                gallery_button.setVisibility(View.GONE)
            }
        }

        updateChallengesMenuOption()

        var guide_enabled = getStringVariable("TRAINING_GUIDE_ENABLED") == "true"
        if(guide_enabled){
            findViewById<LinearLayout>(R.id.menu_option_training_guide).setVisibility(View.VISIBLE)
        }else{
            findViewById<LinearLayout>(R.id.menu_option_training_guide).setVisibility(View.GONE)
        }

        val training_guide_button = findViewById<LinearLayout>(R.id.menu_option_training_guide);
        training_guide_button.setOnClickListener(View.OnClickListener() {
            val intent = Intent(this@BaseActivity, TrainingGuide::class.java);
            startActivity(intent);
            this.overridePendingTransition(0, 0);
        })

        var parents_corner_link = getStringVariable("PARENTS_CORNER_URL")

        val parents_corner_button = findViewById<LinearLayout>(R.id.menu_option_parents_corner);
        if(parents_corner_link != "" && is_captain != "true"){
            parents_corner_button.setVisibility(View.VISIBLE)
            parents_corner_button.setOnClickListener{
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(parents_corner_link))
                startActivity(browserIntent)
            }
        } else {
            parents_corner_button.setVisibility(View.GONE)
        }

        var reward_center_link = getStringVariable("AHA_REWARDS_CENTER")

        val reward_center_button = findViewById<LinearLayout>(R.id.menu_option_rewards_center);
        if(reward_center_link != ""){

            var days_after = 0;
            var days_til_event = 0;

            if(getStringVariable("REWARDS_CENTER_NAV_DAYS_AFTER_EVENT") != ""){
                days_after = getStringVariable("REWARDS_CENTER_NAV_DAYS_AFTER_EVENT").toInt()
            }

            if(getStringVariable("DAYS_UNITL_EVENT") != ""){
                days_til_event = getStringVariable("DAYS_UNITL_EVENT").toInt()
            }

            if(days_til_event < 0){
                val diff = days_til_event + days_after
                if(diff < 0){
                    reward_center_button.visibility = View.GONE
                }else{
                    reward_center_button.visibility = View.VISIBLE
                    reward_center_button.setOnClickListener{
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(reward_center_link))
                        startActivity(browserIntent)
                    }
                }
            }else{
                reward_center_button.visibility = View.VISIBLE
                reward_center_button.setOnClickListener{
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(reward_center_link))
                    startActivity(browserIntent)
                }
            }
        } else {
            reward_center_button.setVisibility(View.GONE)
        }

        val resource_center_button = findViewById<LinearLayout>(R.id.menu_option_resource_center);
        val resource_center_link = getStringVariable("RESOURCE_CENTER_URL")
        if(resource_center_link != ""){
            resource_center_button.setVisibility(View.VISIBLE)
            resource_center_button.setOnClickListener{
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(resource_center_link))
                startActivity(browserIntent)
            }
        } else {
            resource_center_button.setVisibility(View.GONE)
        }

        val switch_events_button = findViewById<LinearLayout>(R.id.menu_option_switch_events);
        switch_events_button.setOnClickListener(View.OnClickListener() {
            setVariable("SWITCH_EVENTS_SOURCE","menu")
            displayAlert("switchEvents");
        })

        val manage_schools_button = findViewById<LinearLayout>(R.id.menu_option_manage_school);

        val manage_schools_url = getStringVariable("MANAGE_SCHOOL_URL")
        if(manage_schools_url != "" && is_captain == "true" && getStringVariable("CLIENT_CODE") == "ahayouthmarket"){
            manage_schools_button.setVisibility(View.VISIBLE)
            if(getStringVariable("MANAGE_SCHOOL_ENABLED") == "true"){
                manage_schools_button.setOnClickListener {
                    val intent = Intent(this@BaseActivity, ManageSchool::class.java);
                    startActivity(intent);
                    this.overridePendingTransition(0, 0);
                }
            }else{
                manage_schools_button.setOnClickListener {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(manage_schools_url))
                    startActivity(browserIntent)
                }
            }
        }else{
            manage_schools_button.setVisibility(View.GONE)
        }

        val support_chat_button = findViewById<LinearLayout>(R.id.menu_option_support_chat);
        val support_chat_enabled  = getStringVariable("AHA_SUPPORT_CHAT_ENABLED")
        val support_chat_url  = getStringVariable("AHA_SUPPORT_CHAT_URL")

        if(support_chat_enabled == "true" && support_chat_url != ""){
            support_chat_button.setVisibility(View.VISIBLE)
            support_chat_button.setOnClickListener {
                val browserIntent =
                    Intent(Intent.ACTION_VIEW, Uri.parse(support_chat_url))
                startActivity(browserIntent)
            }
        }else{
            support_chat_button.setVisibility(View.GONE)
        }

        val review_button = findViewById<LinearLayout>(R.id.menu_option_review);
        val review_url  = getStringVariable("ANDROID_APP_REVIEW_URL")

        if(review_url != ""){
            review_button.setVisibility(View.VISIBLE)
            review_button.setOnClickListener {
                val browserIntent =
                    Intent(Intent.ACTION_VIEW, Uri.parse(review_url))
                startActivity(browserIntent)
            }
        }else{
            review_button.setVisibility(View.GONE)
        }


        val privacy_policy_button = findViewById<LinearLayout>(R.id.menu_option_privacy_policy);
        val privacy_policy_link = getStringVariable("PRIVACY_POLICY_URL")

        if(privacy_policy_link != ""){
            privacy_policy_button.setVisibility(View.VISIBLE)
            privacy_policy_button.setOnClickListener {
                val browserIntent =
                    Intent(Intent.ACTION_VIEW, Uri.parse(privacy_policy_link))
                startActivity(browserIntent)
            }
        }else{
            privacy_policy_button.setVisibility(View.GONE)
        }

        var educational_resources_link = getStringVariable("EDUCATIONAL_RESOURCES_URL")
        val educational_resources_button = findViewById<LinearLayout>(R.id.menu_option_educational_resources);
        if(is_captain == "true" && educational_resources_link != ""){
            educational_resources_button.setOnClickListener{
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(educational_resources_link))
                startActivity(browserIntent)
            }
        } else {
            educational_resources_button.setVisibility(View.GONE)
        }

        var help_link = getStringVariable("HELP_LINK");
        val need_help_button = findViewById<LinearLayout>(R.id.menu_option_need_help);

        if(help_link == ""){
            need_help_button.setVisibility(View.GONE)
        }

        need_help_button.setOnClickListener(View.OnClickListener() {
            needHelpClicked(getStringVariable("MOBILE_CLIENT_HELP_URL"), "")
        })

        val logout_button = findViewById<LinearLayout>(R.id.menu_option_logout);
        logout_button.setOnClickListener(View.OnClickListener() {
            Logout(true)
        })

        toggleMenuVisibility()
    }
    private var shortAnimationDuration: Int = 0

    fun fadeInView(view: View){
        view.apply {
            // Set the content view to 0% opacity but visible, so that it is visible
            // (but fully transparent) during the animation.
            alpha = 0f
            visibility = View.VISIBLE
            //scaleY = 0f

            // Animate the content view to 100% opacity, and clear any animation
            // listener set on the view.
            animate()
                .alpha(1f)
                //.scaleY(1f)
                .setDuration(shortAnimationDuration.toLong())
                .setListener(null)
        }
    }
    fun setupVideo(obj: JSONObject): StandardVideo{
        var display_image = ""
        var video_url = ""
        var accessibility_text = ""
        var sort_order = ""

        if(obj.has("display_image")){
            display_image = obj.get("display_image") as String
        }
        if(obj.has("video_url")){
            video_url = obj.get("video_url") as String
        }
        if(obj.has("accessibility_text")){
            accessibility_text = obj.get("accessibility_text") as String
        }

        if(obj.has("sort_order")){
            sort_order = (obj.get("sort_order") as Int).toString()
        }
        
        return StandardVideo(display_image,video_url,accessibility_text,sort_order)
    }

    class StandardVideo(
        val display_image: String,
        val video_url: String,
        val accessibility_text: String,
        val sort_order: String,
    )
}
