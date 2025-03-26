package com.nuclavis.rospark

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.*
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.ViewPumpAppCompatDelegate
import androidx.core.graphics.drawable.DrawableCompat.setTint
import androidx.core.view.children
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import com.amangarg.localizationdemo.JsonHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.android.material.button.MaterialButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nuclavis.rospark.Donations.Match
import com.nuclavis.rospark.databinding.*
import com.unity3d.player.UnityPlayerActivity
import dev.b3nedikt.restring.Restring
import dev.b3nedikt.restring.Restring.wrapContext
import dev.b3nedikt.reword.Reword.reword
import dev.b3nedikt.reword.RewordInterceptor
import dev.b3nedikt.viewpump.ViewPump
import okhttp3.*
import okio.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.Math.floor
import java.security.Security
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern


abstract class BaseLanguageActivity : AppCompatActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    val PERMISSION_REQUEST_CODE: Int = 1002
    var newStringLocale: Locale = Locale.getDefault();
    private var appCompatDelegate: AppCompatDelegate? = null
    var newStringsMap: HashMap<String, String> = HashMap()
    var gson = Gson();

    var searching = false;

    var personalGoal = 0.00;
    var teamGoal = 0.00;
    var companyGoal = 0.00;

    var find_donor_type = "individual"

    private lateinit var alertSender: View

    var donorSearching = false;
    var matchSearching = false;
    var emojiCharSequence = ""
    lateinit var selectedDonorToMatch: Match;
    var selectedCompanyToMatch = JSONObject("{}")

    var activity_modal_distance_enabled = true;
    var activity_internal_name = ""

    open fun refresh() {
        childviewCallback("","")
    }

    var app_lang = "en"

    var arScene: String? = null;
    var arStart: Date? = null;

    var UNITY_CONTENT_REQUEST = 4;

    val myCalendar: Calendar = Calendar.getInstance()
    var currentPicker = 0;
    var activity_hour = "";
    var activity_minute = "";
    var activity_type = "workout";
    var activity_day = "";
    var activity_month = "";
    var activity_year = 0;

    var nonmatched = emptyList<Match>();

    val states = arrayOf("State","Alabama","Alaska","Arizona","Arkansas","California","Colorado","Connecticut","Delaware","District Of Columbia","Florida","Georgia","Hawaii","Idaho","Illinois","Indiana","Iowa","Kansas","Kentucky","Louisiana","Maine","Maryland","Massachusetts","Michigan","Minnesota","Mississippi","Missouri","Montana","Nebraska","Nevada","New Hampshire","New Jersey","New Mexico","New York","North Carolina","North Dakota","Ohio","Oklahoma","Oregon","Pennsylvania","Rhode Island","South Carolina","South Dakota","Tennessee","Texas","Utah","Vermont","Virginia","Washington","West Virginia","Wisconsin","Wyoming")
    val state_abbreviations = arrayOf("","AL","AK","AZ","AR","CA","CO","CT","DE","DC","FL","GA","HI","ID","IL","IN","IA","KS","KY","LA","ME","MD","MA","MI","MN","MS","MO","MT","NE","NV","NH","NJ","NM","NY","NC","ND","OH","OK","OR","PA","RI","SC","SD","TN","TX","UT","VT","VA","WA","WV","WI","WY")
    var month_abbreviations = arrayOf(
        R.string.mobile_training_guide_calendar_january,
        R.string.mobile_training_guide_calendar_february,
        R.string.mobile_training_guide_calendar_march,
        R.string.mobile_training_guide_calendar_april,
        R.string.mobile_training_guide_calendar_may,
        R.string.mobile_training_guide_calendar_june,
        R.string.mobile_training_guide_calendar_july,
        R.string.mobile_training_guide_calendar_august,
        R.string.mobile_training_guide_calendar_september,
        R.string.mobile_training_guide_calendar_october,
        R.string.mobile_training_guide_calendar_november,
        R.string.mobile_training_guide_calendar_december
    )

    val vars = listOf(
        "CHECK_DEPOSIT_ENABLED", "BACKGROUND_IMAGE_ENABLED", "HIDE_EVENT_COUNTDOWN", "GOOGLE_FIT_CLIENT_ID", "GOOGLE_FIT_CLIENT_SECRET",
        "CHECK_DEPOSIT_MISNAP_LICENSE", "CUSTOM_BACKGROUND_URL", "THERMOMETER_COLOR", "PRIMARY_COLOR", "ALZ_PROMISE_GARDEN_ENABLED",
        "BUTTON_COLOR", "BUTTON_TEXT_COLOR", "CHECK_DEPOSIT_VENDOR", "AUTISM_CHALLENGES_ENABLED", "ACTIVITY_TRACKING_ENABLED", "ACTIVITY_TRACKING_FITBIT_ENABLED",
        "ACTIVITY_TRACKING_GOOGLE_ENABLED", "ACTIVITY_TRACKING_GARMIN_ENABLED","ACTIVITY_TRACKING_ANDROID_HEALTH_ENABLED", "ACTIVITY_TRACKING_APPLE_ENABLED","GIFTS_ENABLED","GIFTS_JSON","GIFTS_CHECKED",
        "ACTIVITY_TRACKING_STRAVA_ENABLED", "ALZ_PROMISE_GARDEN_ENABLED","BUTTON_COLOR", "BUTTON_TEXT_COLOR", "CHECK_DEPOSIT_VENDOR", "HIDE_ALL_LEADERBOARDS",
        "AUTISM_CHALLENGES_ENABLED", "HIDE_TEAM_LEADERBOARD", "HIDE_EVENT_OVERVIEW", "MANAGE_TEAM_ENABLED", "GAMES_ENABLED", "SOCIAL_POST_GROUP_TRACKING_ENABLED",
        "AHA_SMT_WEBHOOKS_ENABLED", "AHA_SOCIAL_OPTOUT_ENABLED", "AHA_FINNS_MISSION_LEADERBOARD_ENABLED", "AHA_TOP_CLASSROOM_LEADERBOARD_ENABLED",
        "GIFTS_CARD_BACKGROUND_COLOR", "GIFTS_CARD_BUTTON_COLOR","AHA_KHC_FINNS_MISSION", "AHA_AHC_FINNS_MISSION","ACTIVITY_TRACKING_STEPS_ENABLED", "CASH_DONATIONS_ENABLED",
        "ACTIVITY_TRACKING_WORKOUTS_ENABLED","TRAINING_GUIDE_ENABLED","ACTIVITY_TRACKING_TYPE","MANAGE_PAGE_ENABLED","MANAGE_SCHOOLS_ENABLED","CHECK_DEPOSIT_DEPOSIT_TEAM_MEMBER",
        "AHA_SUPPORT_CHAT_ENABLED","HAS_GIFTS","GIFTS_ARRAY_STRING","FUNDRAISE_MESSAGES_PROMOTED","BANNER_TILE_ENABLED","MANAGE_PAGE_CUSTOM_URL_DISABLED","ACTIVITY_CHALLENGES_ENABLED",
        "DISABLE_CORE_FUNDRAISING_PAGES","ALZ_LUMINARIES_ENABLED","ALZ_PHOTO_FILTERS_ENABLED","CHECK_DEPOSIT_DEPOSIT_TEAM_ENABLED","STRAVA_CLIENT_ID","STRAVA_CLIENT_SECRET",
        "EVENT_CHECKIN_ENABLED","ALZ_JERSEY_AR_ENABLED","ACTIVITY_TRACKING_POINTS_USE_HEART_LOGO","TOGGLE_CUSTOM_COLORS_ENABLED","TOGGLE_SELECTED_COLOR","DISABLED_LEADERBOARD_TEAM_IDS",
        "TOGGLE_NOT_SELECTED_COLOR","SWIPING_DOT_INACTIVE_COLOR","BADGES_ENABLED","GALLERY_ENABLED","COMPANY_GOAL_EDIT_ENABLED","REWARDS_CENTER_NAV_DAYS_AFTER_EVENT",
        "HIDE_TEAM_EMAIL","GALLERY_DESCRIPTION_ANDROID","DISABLE_EDIT_COMPANY_PAGE","CHECK_DEPOSIT_ALLOW_RECOGNITION_NAME", "CHECK_DEPOSIT_ALLOW_DISPLAY_AMOUNT_PUBLICLY",
        "AHA_CPR_CARD_IMAGE", "AHA_CPR_CARD", "CHECK_DEPOSIT_SPLIT_CHECK_ENABLED", "OVERRIDE_OVERVIEW_ACTIVITY_LEADERBOARD_CHALLENGES", "DISABLE_EDIT_TEAM_PAGE",
        "DISABLE_COMPANY_PAGE","DISABLE_COMPANY_PROGRESS","DISABLE_COMPANY_LEADERBOARD","DISABLE_COMPANY_CHALLENGE_LEADERBOARD", "LOGO_VOICEOVER", "AHA_IMPACT_POINTS_ENABLED",
        "OVERVIEW_CUSTOM_IMAGE_CARD_ENABLED","OVERVIEW_CUSTOM_IMAGE_CARD_IMAGE_URL","OVERVIEW_CUSTOM_IMAGE_CARD_IMAGE_ALT_TEXT","OVERVIEW_TEAM_PROGRESS_CAPTAIN_ONLY",
        "OVERVIEW_WEEKLY_STRATEGY_ENABLED", "AHA_IMPACT_POINTS_COLOR", "MY_TEAM_DISABLED_TEAM_MEMBERS", "RECRUIT_DISABLED_TEAM_MEMBERS", "AHA_IMPACT_BADGES_ENABLED",
        "DISABLE_OVERVIEW_ACTIVITY_LEADERBOARDS", "DOUBLE_DONATION_ENABLED", "ACTIVITY_TRACKING_DISTANCE_METRIC", "EVENT_CHECKIN_TSHIRT_PROMPT_ENABLED",
        "EVENT_CHECKIN_TSHIRT_PROMPT_AMOUNT", "EVENT_CHECKIN_TSHIRT_PROMPT_IMAGE", "CHECK_DEPOSIT_MAINTENANCE_ENABLED", "CHECK_EVENT_MANAGER_DONOR_BY_EVENT", 
        "CHECK_EVENT_MANAGER_HIDE_FIND_DONOR",  "CHECK_EVENT_MANAGER_ANNUAL_FUND_ENABLED", "SHARE_INSTAGRAM_TIKTOK_DISABLED",
    )

    init {
        System.setProperty("com.sun.net.ssl.checkRevocation", "true")
        Security.setProperty("ocsp.enable", "true")
    }

    protected abstract fun childviewCallback(string: String, data: String)
    protected abstract fun slideButtonCallback(card: Any, forward:Boolean)
    fun clearVariable(key: String){
        setVariable(key, "");
    }
    override fun onBackPressed() {
        try {
            if(findViewById<WebView>(R.id.register_now_webview) != null){
                findViewById<WebView>(R.id.register_now_webview).setVisibility(View.GONE)
            }else{
                super.onBackPressed();
            }
        }catch (e: Exception) {
            super.onBackPressed();
        }
    }

    fun getStringVariable(key: String): String{
        var sharedPreferences = getSharedPreferences("PREFS_KEY", Context.MODE_PRIVATE)
        return sharedPreferences.getString(key, "") as String;
    }

    fun setVariable(key: String, value: String){
        var sharedPreferences = getSharedPreferences("PREFS_KEY", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString(key, value);
        editor.apply()
    }

    fun getEvent(): Event{
        var sharedPreferences = getSharedPreferences("PREFS_KEY", Context.MODE_PRIVATE)
        val string = sharedPreferences.getString("EVENT", "") as String;
        if(string != ""){
            return gson.fromJson(string, Event::class.java);
        }else {
            return Event("","","","");
        }

    }

    fun showDefaultPage(){
        if(getStringVariable("DISABLE_CORE_FUNDRAISING_PAGES") == "true"){
            val intent = Intent(this@BaseLanguageActivity, Challenges::class.java);
            startActivity(intent);
        }else{
            val pageName = getStringVariable("APP_LINK_PAGE_NAME")
            println("showDefaultPage pageName: $pageName")
            if(pageName != ""){
                if(pageName == "fundraise"){
                    val newIntent = Intent(this@BaseLanguageActivity, Fundraise::class.java)
                    startActivity(newIntent)
                }
                else if(pageName == "donations"){
                    val newIntent = Intent(this@BaseLanguageActivity, Donations::class.java)
                    startActivity(newIntent)
                }
                else if(pageName == "managepage"){
                    val newIntent = Intent(this@BaseLanguageActivity, ManagePage::class.java)
                    startActivity(newIntent)
                }
                else if(pageName == "myteam"){
                    val newIntent = Intent(this@BaseLanguageActivity, Teams::class.java)
                    startActivity(newIntent)
                }
                else if(pageName == "managecompany"){
                    val newIntent = Intent(this@BaseLanguageActivity, ManageCompany::class.java)
                    startActivity(newIntent)
                }
                else if(pageName == "recruit"){
                    val newIntent = Intent(this@BaseLanguageActivity, Recruit::class.java)
                    startActivity(newIntent)
                }
                else if(pageName == "trackactivity"){
                    val newIntent = Intent(this@BaseLanguageActivity, TrackActivity::class.java)
                    startActivity(newIntent)
                }
                else if(pageName == "challenges"){
                    val newIntent = Intent(this@BaseLanguageActivity, Challenges::class.java)
                    startActivity(newIntent)
                }
                else if(pageName == "gallery"){
                    val newIntent = Intent(this@BaseLanguageActivity, Gallery::class.java)
                    startActivity(newIntent)
                }
                else if(pageName == "games"){
                    val newIntent = Intent(this@BaseLanguageActivity, Games::class.java)
                    startActivity(newIntent)
                }
                else{
                    println("pageName else")
                    val newIntent = Intent(this@BaseLanguageActivity, Overview::class.java)
                    startActivity(newIntent)
                }
            }
            else{
                val intent = Intent(this@BaseLanguageActivity, Overview::class.java);
                startActivity(intent);
            }
            clearVariable("APP_LINK_PAGE_NAME")
        }
    }

    fun setEvent(newEvent: Event){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/user/getUserJwt");

        if(newEvent.event_cons_id.isNotEmpty()) {
            setConsID(newEvent.event_cons_id)
        }

        val formBody = FormBody.Builder().add("cons_id", getConsID()).add("event_id", newEvent.event_id)
            .build()

        var request = Request.Builder()
            .url(url)
            .post(formBody)
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .build()

        var client = OkHttpClient();
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    val response = response.body?.string();
                    val json = JSONObject(response);
                    if(json.has("data")){
                        val data = json.get("data") as JSONObject
                        val jwt = data.get("jwt").toString()
                        setAuth(jwt)
                        if(data.has("program_id")){
                            if(getString(R.string.container_app) != "true"){
                            val program_id = data.get("program_id") as Int
                            setVariable("PROGRAM_ID",program_id.toString())
                            }
                        }
                    }


                    val eventJson = gson.toJson(newEvent);
                    setVariable("EVENT",eventJson);
                    logRegToken();
                    addUserToGroup();
                    getEventMobileConfigs(false, newEvent.event_id, getConsID());
                    loadUserUrls(newEvent.event_id);
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
            }
        })
    }

    fun setEventEventManager(eventId: String){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/user/getUserJwt");

        val formBody = FormBody.Builder().add("cons_id", getConsID()).add("event_id", eventId)
            .build()

        var request = Request.Builder()
            .url(url)
            .post(formBody)
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .build()

        var client = OkHttpClient();
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    val response = response.body?.string();
                    val json = JSONObject(response);
                    val jwt = JSONObject(json.get("data").toString()).get("jwt").toString()
                    setAuth(jwt)
                    getEventMobileConfigs(true, eventId, getConsID());
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
            }
        })
    }

    fun getGiftsEnabled(){
        setVariable("GIFTS_CHECKED", "true")
        println("GIFTS ENABLED: " + getStringVariable("GIFTS_ENABLED"))
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/getGiftsEnabled/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        val gifts_button = findViewById<LinearLayout>(R.id.menu_option_gifts);
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
                    val response = response.body?.string();
                    val json = JSONObject(response);
                    println("JSON: ")
                    println(json)
                    if(json.has("data")){
                        runOnUiThread{
                            val data = json.get("data") as JSONObject;
                            val parents_corner_button = findViewById<LinearLayout>(R.id.menu_option_parents_corner);
                            val parents_corner_link = getStringVariable("PARENTS_CORNER_URL");
                            if((getStringVariable("IS_TEAM_CAPTAIN") == "true") && parents_corner_link != ""){
                                parents_corner_button.setVisibility(View.VISIBLE);
                                parents_corner_button.setOnClickListener{
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(parents_corner_link))
                                    startActivity(browserIntent)
                                }
                            }else{
                                parents_corner_button.setVisibility(View.GONE)
                            }

                            if(data.has("gifts_enabled") && data.get("gifts_enabled") is Int){
                                val enabled = data.get("gifts_enabled") as Int
                                if(enabled == 1){
                                    setVariable("HAS_GIFTS","true")
                                }

                                val gifts_button = findViewById<LinearLayout>(R.id.menu_option_gifts);
                                gifts_button.setOnClickListener{
                                    val intent = Intent(this@BaseLanguageActivity, Gifts::class.java);
                                    startActivity(intent);
                                }

                                if(getStringVariable("HAS_GIFTS") != "" && getStringVariable("IS_TEAM_CAPTAIN") != "true") {
                                    gifts_button.setVisibility(View.VISIBLE)
                                }else{
                                    gifts_button.setVisibility(View.GONE)
                                }

                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
            }
        })
    }

    fun recolorTheme(){
        var themeString = getStringVariable("EVENT_THEME");
        var themeId = 0;
        println("THEME STRING: " + themeString)
        if(themeString == ""){
            themeId = R.style.StandardTheme
        }else{
            themeId = themeString.toInt()
        }
        setTheme(themeId)
        theme.applyStyle(themeId, true)
    }

    fun getEventMobileConfigs(is_event_manager: Boolean, event_id: String, cons_id: String){

        var url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/getMobileConfigs/").plus(getEvent().event_id);
        
        if(is_event_manager){
            url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/getMobileConfigs/").plus(getStringVariable("CHECK_EVENT_MANAGER_EVENT_ID"));
        }
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
                    val response = response.body?.string();
                    val json = JSONArray(response);
                    var colorhash = ""

                    for(i in 0 .. vars.size - 1){
                        clearVariable(vars[i])
                    }

                    for(i in 0 .. json.length() - 1){
                        try {
                            var obj = json.getJSONObject(i);
                            if(obj.has("config_name") && obj.has("config_value")){
                                var key = obj.get("config_name") as String;
                                var value = obj.get("config_value") as String;
    
                                println("CONFIG NAME: " + key)
                                println("CONFIG VALUE: " + value)
    
                                if (key == "primary_color"){
                                    colorhash = value.trim().lowercase();
                                    setVariable("PRIMARY_COLOR",value)
                                    setVariable("CONTAINER_LOGIN_PRIMARY_COLOR",value)
                                } else if (key == "background_image"){
                                    setVariable("CUSTOM_BACKGROUND_URL",value)
                                    val filename = "@drawable/" + value.substring(value.lastIndexOf("/") + 1, value.lastIndexOf("."))
                                    val drawableId = this@BaseLanguageActivity.resources.getIdentifier(filename,null,getPackageName())
                                    setVariable("CUSTOM_BACKGROUND_DRAWABLE", drawableId.toString())
                                    if(drawableId != 0){
                                        val requestOptions: RequestOptions = RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL)
                                        Glide.with(this@BaseLanguageActivity).load(value).apply(requestOptions).preload()
                                    }
                                } else if (key.contains("_color")){
                                    //println("SETTING COLOR SAFE VARIABLE: " + key.uppercase())
                                    setVariable(key.uppercase(),value.trim().lowercase())
                                } else {
                                    //println("SETTING STANDARD VARIABLE: " + key.uppercase())
                                    setVariable(key.uppercase() ,value)
                                }
                            }
                        } catch (exception: IOException) {
                            println("GET EVENT MOBILE CONFIGS ERROR")
                        }
                    }

                    if(getStringVariable("BUTTON_TEXT_COLOR") == ""){
                        setVariable("BUTTON_TEXT_COLOR","#ffffff")
                    }
                    if(getStringVariable("BUTTON_COLOR") == ""){
                        setVariable("BUTTON_COLOR",colorhash)
                    }

                    var app_theme: Int;
                    if(colorhash == "#105f70"){
                        app_theme = R.style.DarkTealTheme
                    }else if(colorhash == "#424242"){
                        app_theme = R.style.DarkGrayTheme
                    }else{
                        app_theme = R.style.StandardTheme
                    }

                    if(getStringVariable("ACTIVITY_CHALLENGES_ENABLED") == "true"){
                        println("EVENT ID: " + getEvent().event_id)
                        var url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/challenges/").plus(getEvent().event_id);

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
                                    val response = response.body?.string();
                                    val obj = JSONObject(response)
                                    if(obj.has("data")){
                                        val json = obj.get("data") as JSONArray
                                        if(json.length() > 0){
                                            setVariable("CHALLENGES_OBJECT",json[0].toString())
                                            setVariable("CHALLENGES_ENABLED", "true")
                                            updateChallengesMenuOption()
                                        }else{
                                            setVariable("CHALLENGES_OBJECT","")
                                            setVariable("CHALLENGES_ENABLED", "false")
                                            updateChallengesMenuOption()
                                        }
                                    }
                                }
                            }

                            override fun onFailure(call: Call, e: IOException) {
                                println(e.message.toString())
                                val intent = Intent(this@BaseLanguageActivity, Error::class.java);
                                startActivity(intent);
                            }
                        })
                    }else{
                        setVariable("CHALLENGES_OBJECT","")
                        setVariable("CHALLENGES_ENABLED", "false")
                        updateChallengesMenuOption()
                    }

                    setVariable("EVENT_THEME",app_theme.toString())
                    hideAlert()

                    if(getStringVariable("SWITCH_EVENTS_SOURCE") == "login"){
                        setVariable("SWITCH_EVENTS_SOURCE","")
                        if(getStringVariable("LOGIN_NUMBER") == ""){
                            setVariable("LOGIN_NUMBER","FIRST")
                        }else if(getStringVariable("LOGIN_NUMBER") == "FIRST"){
                            setVariable("LOGIN_NUMBER","SECOND")
                        }
                    }


                    if(is_event_manager){
                        val intent = Intent(this@BaseLanguageActivity, CheckDeposit::class.java);
                        intent.putExtra("event_id","")
                        intent.putExtra("event_name","")
                        intent.putExtra("check_credit","")
                        startActivity(intent);
                    }else{
                        if(getStringVariable("INITIAL_DISRUPTION_SCREEN") == "true" && getStringVariable("DISRUPTIONS_ENABLED") == "true"){
                            setVariable("INITIAL_DISRUPTION_SCREEN","false")
                            val intent = Intent(this@BaseLanguageActivity, DisruptorScreens::class.java);
                            intent.putExtra("event_id", event_id)
                            intent.putExtra("cons_id", cons_id)
                            startActivity(intent);
                        }else{
                            setVariable("INITIAL_DISRUPTION_SCREEN","false")
                            showDefaultPage()
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                val intent = Intent(this@BaseLanguageActivity, Error::class.java);
                startActivity(intent);
            }
        })
    }

    fun updateChallengesMenuOption(){
        var challenges_enabled = getStringVariable("ACTIVITY_CHALLENGES_ENABLED") == "true" && getStringVariable("CHALLENGES_ENABLED") == "true";
        val challenges_button = findViewById<LinearLayout>(R.id.menu_option_challenges);

        if(challenges_button != null){
            if(getStringVariable("IS_EVENT_MANAGER_ONLY") == "true"){
                runOnUiThread{
                    challenges_button.setVisibility(View.GONE)
                }
            }else{
                runOnUiThread{
                    if(challenges_enabled){
                        challenges_button.setVisibility(View.VISIBLE)
                        challenges_button.setOnClickListener(View.OnClickListener() {
                            val intent = Intent(this@BaseLanguageActivity, Challenges::class.java);
                            startActivity(intent);
                            this.overridePendingTransition(0, 0);
                        })
                    }else{
                        challenges_button.setVisibility(View.GONE)
                    }
                }
            }
        }
    }

    fun getEvents(): List<Event>{
        var sharedPreferences = getSharedPreferences("PREFS_KEY", Context.MODE_PRIVATE)
        val string = sharedPreferences.getString("EVENTLIST", "") as String;
        if(string.length > 0){
            return gson.fromJson(string, object : TypeToken<List<Event?>?>() {}.type)
        }else{
            return emptyList()
        }
    }

    fun setEvents(newEvents: List<Event>){
        val json = gson.toJson(newEvents);
        setVariable("EVENTLIST",json)
    }

    fun getConsID(): String {
        return getStringVariable("CONS_ID");
    }

    fun setConsID(myValue: String) {
        setVariable("CONS_ID", myValue);
    }

    fun getAuth(): String {
        return getStringVariable("JTX_TOKEN");
    }

    fun setAuth(myValue: String) {
        setVariable("JTX_TOKEN", myValue);
    }

    fun setTooltipText(id: Int, stringId: Int, voiceoverStringId: Any, sender: String = ""){
        val icon = (findViewById<LinearLayout>(id).getChildAt(0)) as ImageView
        findViewById<LinearLayout>(id).setOnClickListener{
            displayAlert(getResources().getString(stringId), sender);
            setAlertSender(icon)
        }
        icon.setOnClickListener{
            displayAlert(getResources().getString(stringId), sender);
            setAlertSender(icon)
        }
        if(voiceoverStringId is Int){
            icon.contentDescription = (getResources().getString(voiceoverStringId) + " " + getResources().getString(R.string.mobile_help_button_description))
        }else if (voiceoverStringId is String){
            icon.contentDescription = (voiceoverStringId + " " + getResources().getString(R.string.mobile_help_button_description))
        }
    }

    fun setTooltipWithAnalytics(id: Int, stringId: Int, voiceoverStringId: Any, analyticsKey: String, sender: String = "") {
        val layout = findViewById<LinearLayout>(id)

        if(layout != null){
            val icon = layout.getChildAt(0) as ImageView
            icon.setOnClickListener {
                sendGoogleAnalytics(analyticsKey, "overview")
                displayAlert(getResources().getString(stringId), sender)
                setAlertSender(icon)
            }
            if(voiceoverStringId is Int){
                icon.contentDescription = (getResources().getString(voiceoverStringId) + " " + getResources().getString(R.string.mobile_help_button_description))
            }else if (voiceoverStringId is String){
                icon.contentDescription = (voiceoverStringId + " " + getResources().getString(R.string.mobile_help_button_description))
            }
        }

       
    }

    @NonNull
    override fun getDelegate(): AppCompatDelegate {
        if (appCompatDelegate == null) {
            appCompatDelegate = ViewPumpAppCompatDelegate(
                super.getDelegate(),
                this
            ) { base: Context -> wrapContext(base) }
        }
        return appCompatDelegate as AppCompatDelegate
    }

    private fun configureLocales() {
        app_lang = getStringVariable("APP_LANG")
        newStringLocale = Locale(app_lang)
    }


    fun updateGoal(type: String, oldGoal: Double){

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/").plus(type)

        var goalString = findViewById<EditText>(R.id.edit_goal_amount).text.toString().replace(",", "");

        if(getStringVariable("APP_LANG") == "fr"){
            goalString = findViewById<EditText>(R.id.edit_goal_amount).text.toString().replace(" ", "").replace(" ", "").replace(",", ".");
        }

        goalString = goalString.replace("&nbsp;","");
        goalString = goalString.replace(" ","");

        if(goalString == ""){
            if(type == "updatePersonalGoal"){
                displayAlert(getResources().getString(R.string.mobile_overview_edit_goal_invalid), "", { displayAlert(type, oldGoal) })
            }else if(type == "updateCompanyGoal"){
                displayAlert(getResources().getString(R.string.mobile_company_progress_edit_goal_invalid), "", { displayAlert(type, oldGoal) })
            }else{
                displayAlert(getResources().getString(R.string.mobile_overview_edit_team_goal_invalid), "", { displayAlert(type, oldGoal) })
            }
            return
        }

        var goal = 0.00
        try{
            goal = goalString.toDouble()
        }catch(e: Exception){
            if(type == "updatePersonalGoal"){
                displayAlert(getResources().getString(R.string.mobile_overview_edit_goal_invalid), "", { displayAlert(type, oldGoal) })
            }else if(type == "updateCompanyGoal"){
                displayAlert(getResources().getString(R.string.mobile_company_progress_edit_goal_invalid), "", { displayAlert(type, oldGoal) })
            }else{
                displayAlert(getResources().getString(R.string.mobile_overview_edit_team_goal_invalid), "", { displayAlert(type, oldGoal) })
            }
            return
        }

        println("UPDATING GOAL TO: " + goal)


        // add parameter
        val formBody = FormBody.Builder().add("cons_id", getConsID()).add("event_id", getEvent().event_id).add("goal", goal.toString())
            .build()

        // creating request
        var request = Request.Builder()
            .url(url)
            .post(formBody)
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code == 401){
                    throw Exception(response.body?.string())
                }else{
                    val response = response.body?.string();
                    val obj = JSONObject(response);
                    var message = ""
                    if(obj.has("statusCode")) {
                        val status_code = obj.get("statusCode")
                        if(status_code != "200" && status_code != 200){
                            if(obj.has("message")){
                                if(isJSONValid(obj.get("message") as String)){
                                    var status_obj = JSONObject(obj.get("message") as String);
                                    if(status_obj.has("message")) {
                                        message = status_obj.get("message") as String;
                                    }
                                }else{
                                    message = obj.get("message") as String
                                }

                                if(message == ""){
                                    if(type == "updateTeamGoal"){
                                        message = getResources().getString(R.string.mobile_overview_edit_team_goal_error)
                                    } else if(type == "updateCompanyGoal"){
                                        message = getResources().getString(R.string.mobile_company_progress_edit_goal_invalid)
                                    } else {
                                        message = getResources().getString(R.string.mobile_overview_edit_goal_error)
                                    }
                                }
                                displayAlert(message, "", { displayAlert(type, oldGoal) })
                            }else{
                                displayAlert(getResources().getString(R.string.mobile_overview_edit_goal_error))
                            }
                        }else{
                            hideAlert()
                        }
                    }else{
                        displayAlert(getResources().getString(R.string.mobile_overview_edit_goal_error))
                    }
                    childviewCallback("","")
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                println(e.message.toString())
                displayAlert(getResources().getString(R.string.mobile_overview_edit_goal_error), hideAlert())
                hideAlert()
            }
        })
    }

    fun validateText(text: String): Boolean{
       val pattern = Regex("([A-Za-z0-9\\-\\_]+)")
        return pattern.matches(text)
    }

    fun setToggleButtonVO(buttonFrame: FrameLayout, label: String, status: Boolean){
        if(status){
            buttonFrame.contentDescription = label + " " + getString(R.string.mobile_manage_page_toggle_button_selected)
        }else{
            buttonFrame.contentDescription = label + " " + getString(R.string.mobile_manage_page_toggle_button_unselected)
        }

    }

    fun setCustomToggleButtonColor(buttonFrame: FrameLayout, status: String){
        val newVal = getStringVariable("PRIMARY_COLOR").replace("#", "#33")
        val button = buttonFrame.getChildAt(0) as LinearLayout
        val buttonBorder = buttonFrame.getChildAt(1) as LinearLayout
        val selectedColor = getStringVariable("TOGGLE_SELECTED_COLOR")
        val unselectedColor = getStringVariable("TOGGLE_NOT_SELECTED_COLOR")
        if(getStringVariable("TOGGLE_CUSTOM_COLORS_ENABLED") == "true") {
            if (status == "active") {
                button.setBackgroundResource(R.drawable.button_active)
                (button.getChildAt(0) as ImageView).setColorFilter(Color.parseColor(unselectedColor))
                (button.getChildAt(1) as TextView).setTextColor(Color.parseColor(unselectedColor))
                button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(selectedColor)))
                buttonBorder.visibility = View.GONE
            } else {
                button.setBackgroundResource(R.drawable.button_inactive)
                (buttonBorder.getChildAt(0) as ImageView).setColorFilter(Color.parseColor(selectedColor))
                (buttonBorder.getChildAt(1) as TextView).setTextColor(Color.parseColor(selectedColor))
                button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(unselectedColor)))
                buttonBorder.setBackgroundResource(R.drawable.button_inactive_border)
                buttonBorder.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(selectedColor)))
                buttonBorder.visibility = View.VISIBLE
            }
        }else{
            button.setVisibility(View.GONE)
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(getStringVariable("PRIMARY_COLOR"))))
            buttonBorder.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(getStringVariable("PRIMARY_COLOR"))))
            if(status == "active") {
                buttonBorder.setBackgroundResource(R.drawable.button_active)
                buttonBorder.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(newVal)))
            }else {
                buttonBorder.setBackgroundResource(R.drawable.button_inactive)
                buttonBorder.setBackgroundTintList(null)
            }
        }
    }

    fun updateURL(type:String){
        val text = findViewById<EditText>(R.id.edit_page_url).text.toString();
        if(validateText(text)){
            hideAlert()
            var dataString = "editLink"
            var url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/updatePersonalPageShortcut/")
            if(type == "team"){
                url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/updateTeamPageShortcut/")
                dataString = "editTeamLink"
            }else if(type == "company"){
                url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/updateCompanyPageShortcut/")
                dataString = "editCompanyLink"
            }

            val formBody = FormBody.Builder().add("cons_id", getConsID()).add("event_id", getEvent().event_id).add("text", text)
                .build()

            var request = Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .build()

            var client = OkHttpClient();
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if(response.code == 401){
                        throw Exception(response.body?.string())
                    }else{
                        val response = response.body?.string();
                        val obj = JSONObject(response);
                        if(obj.has("statusCode")) {
                            val status_code = obj.get("statusCode")
                            if(status_code == 403 || status_code == 500){
                                var message = obj.get("message") as String;
                                if(message == ""){
                                    message = getResources().getString(R.string.mobile_overview_edit_url_error);
                                    displayAlert(message)
                                }
                            }else{
                                hideAlert()
                            }
                        }
                        childviewCallback(text, dataString)
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    println("ERROR SAVING")
                    println(e.message.toString())
                    displayAlert(getResources().getString(R.string.mobile_overview_edit_url_error), hideAlert())
                    hideAlert()
                }
            })
        }else{
            displayAlert(getResources().getString(R.string.mobile_overview_edit_url_invalid_error),"",
                { displayAlert("editUrl", text, { childviewCallback("editLink","") }) })
        }
    }

    fun hideAlert(){
        hideKeyboard(this);
        runOnUiThread {
            val alertsContainer = findViewById<LinearLayout>(R.id.alert_container)
            if(alertsContainer != null){
                for (childView in alertsContainer.children) {
                    alertsContainer.removeView(childView);
                }
                alertsContainer.setVisibility(View.GONE)
                hideAlertScrollView(true)

                if (this::alertSender.isInitialized) {
                    if(alertSender != findViewById<TextView>(R.id.page_title)){
                        alertSender.setFocusableInTouchMode(true);
                        alertSender.requestFocus()
                    }
                }
            }
        }
    }

    fun hideAlertScrollView(hide:Boolean){
        try{
            if(hide){
                findViewById<ScrollView>(R.id.alert_scroll_container).setVisibility(View.GONE)
            }else{
                findViewById<ScrollView>(R.id.alert_scroll_container).setVisibility(View.VISIBLE)
            }
        }catch(e: Exception){
            println("HIDE ALERT SCROLL VIEW EXCEPTION")
        }
    }

    fun sendGoogleAnalytics(event_name: String,page: String){
        println("SENDING GOOGLE ANALYTICS: [" + event_name + ", " + page + "]" )
        firebaseAnalytics.logEvent(event_name) {
            param("page", page)
        }
    }

    fun addUserToGroup(){
        Executors.newSingleThreadExecutor().execute(Runnable {
            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/addUserToGroup")
            val cons_id = getConsID()
            val event_id = getEvent().event_id
            val formBody = FormBody.Builder()
                .add("cons_id", cons_id)
                .add("event_id", event_id)
                .add("group_type", "EVENT_LOGIN")
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
                        println(response)
                        println("ADDED USER SUCCESSFULLY")
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    println("ADD USER ERROR")
                    println(e)
                }
            })
        })
    }

    fun checkForUpdate(): Boolean{
        if(getStringVariable("INITIAL_LAUNCH") == "false"){
            return false
        }else{
            setVariable("INITIAL_LAUNCH","false");
        }

        val buildCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME

        var app_version_flag = false
        var current_app_version = versionName.toFloatOrNull()
        var version_number = getStringVariable("STORE_BUILD_VERSION").toFloatOrNull()
        if(current_app_version != null && version_number != null){
            if(current_app_version < version_number){
                app_version_flag = true
            }
        }
        var app_build_flag = false
        var current_app_build = buildCode
        var build_number = getStringVariable("STORE_BUILD_NUMBER").toIntOrNull()
        if(build_number != null){
            if(current_app_build < build_number){
                app_build_flag = true
            }
        }

        if(getStringVariable("NEW_APP_UPDATE_ENABLED") == "true"){
            if(getStringVariable("NEW_APP_UPDATE_FORCED") == "true"){
                displayAlert("updateAlertForced",arrayOf(getResources().getString(R.string.mobile_new_app_update_forced_description),getResources().getString(R.string.mobile_new_app_update_forced_button)))
            }else{
                displayAlert("updateAlertRecommended", arrayOf(getResources().getString(R.string.mobile_new_app_update_recommended_description),getResources().getString(R.string.mobile_new_app_update_recommended_button)))
            }
            return true
        }else if(app_version_flag || app_build_flag){
            if (getStringVariable("NEW_VERSION_UPDATE_ENABLED") == "true"){
                if(getStringVariable("NEW_VERSION_UPDATE_FORCED") == "true"){
                    displayAlert("updateAlertForced", arrayOf(getResources().getString(R.string.mobile_new_version_update_forced_description),getResources().getString(R.string.mobile_new_version_update_forced_button)))
                }else{
                    displayAlert("updateAlertRecommended", arrayOf(getResources().getString(R.string.mobile_new_version_update_recommended_description),getResources().getString(R.string.mobile_new_version_update_recommended_button)))
                }
            }
            return true
        }
        return false
    }

    fun isEmojiCharacter(codePoint: Char): Boolean {
        val type = Character.getType(codePoint)
        if (type == Character.SURROGATE.toInt() || type == Character.NON_SPACING_MARK.toInt()) {
            return true
        }else{
            return false
        }
    }

    fun containsEmoji(source: String): Boolean {
        val len = source.length
        for (i in 0 until len) {
            val codePoint = source[i]
            if (isEmojiCharacter(codePoint)) {
                return true
            }
        }
        return false
    }

    fun setAlertSender(sendingElement:View = findViewById(R.id.page_title)){
        alertSender = sendingElement
    }

    fun displayImageAlert(title: String, message: String, image: String){
        hideKeyboard(this)
        this.runOnUiThread(Runnable {
            val inflater = LayoutInflater.from(this@BaseLanguageActivity)

            val alertsContainer = findViewById<LinearLayout>(R.id.alert_container)
            alertsContainer.setVisibility(View.INVISIBLE)
            hideAlertScrollView(true)
            for (childView in alertsContainer.children) {
                alertsContainer.removeView(childView);
            }

            var focusableView: View = alertsContainer;
            val binding: StandardImageAlertBinding = DataBindingUtil.inflate(
                inflater, R.layout.standard_image_alert, alertsContainer, true)

            binding.colorList = getColorList("")

            val alert_image = findViewById<ImageView>(R.id.standard_alert_image);
            val alert_title = findViewById<TextView>(R.id.standard_image_alert_title);
            val alert_message = findViewById<TextView>(R.id.standard_image_alert_message);
            val close_button = findViewById<TextView>(R.id.standard_alert_close_button);
            focusableView = close_button;

            if(image != ""){
                Glide.with(this@BaseLanguageActivity)
                    .load(image)
                    .into(alert_image)
            }

            alert_title.setText(title)
            alert_message.setText(message)

            close_button.setOnClickListener {
                hideAlert()
            }

            alertsContainer.setVisibility(View.VISIBLE)
            hideAlertScrollView(false)
            focusableView.requestFocus();
            Handler().postDelayed({
                findViewById<View>(R.id.standard_image_alert_heading).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }, 175)
        })
    }

    fun displayQRAlert(title: String, message: String, image: String){
        hideKeyboard(this)
        this.runOnUiThread(Runnable {
            val inflater = LayoutInflater.from(this@BaseLanguageActivity)

            val alertsContainer = findViewById<LinearLayout>(R.id.alert_container)
            alertsContainer.setVisibility(View.INVISIBLE)
            hideAlertScrollView(true)
            for (childView in alertsContainer.children) {
                alertsContainer.removeView(childView);
            }

            var focusableView: View = alertsContainer;
            val binding: StandardQrAlertBinding = DataBindingUtil.inflate(
                inflater, R.layout.standard_qr_alert, alertsContainer, true)

            binding.colorList = getColorList("")

            val alert_image = findViewById<ImageView>(R.id.standard_alert_qr_image);
            val alert_title = findViewById<TextView>(R.id.standard_qr_alert_title);
            val alert_message = findViewById<TextView>(R.id.standard_qr_alert_message);
            val close_button = findViewById<TextView>(R.id.standard_alert_close_button);
            focusableView = close_button;

            if(image != ""){
                Glide.with(this@BaseLanguageActivity)
                    .load(image)
                    .into(alert_image)
            }

            if(title != ""){
                alert_title.setText(title)
            }else{
                alert_title.visibility = View.GONE
            }

            if(message != ""){
                alert_message.setText(message)
            }else{
                alert_message.visibility = View.GONE
            }

            close_button.setOnClickListener {
                hideAlert()
            }

            alertsContainer.setVisibility(View.VISIBLE)
            hideAlertScrollView(false)
            focusableView.requestFocus();
            Handler().postDelayed({
                findViewById<View>(R.id.standard_qr_alert_heading).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }, 175)
        })
    }

    fun displayAlert(message: String, alertData:Any = "", function: () -> (Unit) = { hideAlert() }){
        setAlertSender()
        hideKeyboard(this)
        var modal_vo_descriptor = 0
        var modal_vo_delay = 475
        this.runOnUiThread(Runnable {
            val inflater = LayoutInflater.from(this@BaseLanguageActivity)

            val alertsContainer = findViewById<LinearLayout>(R.id.alert_container)
            alertsContainer.setVisibility(View.INVISIBLE)
            hideAlertScrollView(true)
            for (childView in alertsContainer.children) {
                alertsContainer.removeView(childView);
            }
            
            var focusableView: View = alertsContainer;

            if(alertData == "standard_close_alert"){
                modal_vo_descriptor = R.id.alert_background_layout
                val binding: StandardCloseAlertBinding = DataBindingUtil.inflate(
                    inflater, R.layout.standard_close_alert, alertsContainer, true)
                    binding.colorList = getColorList("")

                val message_textview = findViewById<TextView>(R.id.standard_alert_message);
                val close_button = findViewById<ImageView>(R.id.standard_alert_cancel_button);
                val action_button = findViewById<TextView>(R.id.standard_alert_action_button);
                message_textview.setText(message)
                action_button.setText(getResources().getString(R.string.mobile_alert_close))
                action_button.setOnClickListener {
                    hideAlert()
                }

                close_button.setOnClickListener{
                    function()
                    hideAlert()
                }
                focusableView = close_button;
            } else {
                if(message == "updatePersonalGoal" || message == "updateTeamGoal" || message == "updateCompanyGoal"){
                    modal_vo_descriptor = R.id.alert_goal_background_layout
                    val binding: EditGoalAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.edit_goal_alert, alertsContainer, true)
                    binding.colorList = getColorList("")

                    var voiceover_text = getResources().getString(R.string.mobile_overview_edit_goal_alert_title);

                    if(message == "updateTeamGoal"){
                        voiceover_text = getResources().getString(R.string.mobile_overview_edit_team_goal_alert_title)
                        findViewById<TextView>(R.id.edit_goal_alert_title).text = getResources().getString(R.string.mobile_overview_edit_team_goal_alert_title)
                    }else if (message == "updateCompanyGoal") {
                        voiceover_text = getResources().getString(R.string.mobile_overview_edit_company_goal_alert_title)
                        findViewById<TextView>(R.id.edit_goal_alert_title).text = getResources().getString(R.string.mobile_overview_edit_company_goal_alert_title)
                    }else{
                        findViewById<TextView>(R.id.edit_goal_alert_title).text = getResources().getString(R.string.mobile_overview_edit_goal_alert_title)
                    }

                    val amountTextBox = findViewById<EditText>(R.id.edit_goal_amount)
                    if(Locale.getDefault().getLanguage() == "fr"){
                        findViewById<TextView>(R.id.edit_goal_pre_symbol).visibility = View.GONE
                        findViewById<TextView>(R.id.edit_goal_post_symbol).visibility = View.VISIBLE
                    }else{
                        findViewById<TextView>(R.id.edit_goal_pre_symbol).visibility = View.VISIBLE
                        findViewById<TextView>(R.id.edit_goal_post_symbol).visibility = View.GONE
                    }
                    amountTextBox.setText(formatDoubleToLocalizedCurrency(toDouble(alertData)).replace("$",""));
                    setCurrencyVoiceover(amountTextBox)

                    if(getStringVariable("APP_LANG") != "fr"){
                        amountTextBox.addTextChangedListener(object : TextWatcher {
                            override fun afterTextChanged(s: Editable) {}
                            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                                setCurrencyVoiceover(amountTextBox)
                            }
                        })
                    

                        amountTextBox.setOnClickListener{
                            setCurrencyVoiceover(amountTextBox)
                        }

                        amountTextBox.hint = voiceover_text
                    }
                    
                    val saveButton = findViewById<TextView>(R.id.edit_goal_alert_save_button)
                    val cancelButton = findViewById<ImageView>(R.id.edit_goal_alert_cancel_button)
                    focusableView = cancelButton
                    cancelButton.setOnClickListener(){
                        hideAlert()
                    }
                    saveButton.setOnClickListener(){
                        updateGoal(message, alertData as Double);
                    }
                }
                else if (message == "needHelp"){
                    val binding: NeedHelpAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.need_help_alert, alertsContainer, true)
                    if(alertData == "login"){
                        binding.colorList = getColorList("login")
                    }else{
                        binding.colorList = getColorList("")
                    }

                    var main_help_link = getStringVariable("MOBILE_CLIENT_HELP_URL");
                    if(alertData == "login"){
                        main_help_link = getStringVariable("MOBILE_CLIENT_HELP_LOGIN_URL");
                    }
                    val secondary_help_link = getStringVariable("HELP_LINK");

                    findViewById<Button>(R.id.mobile_need_help_alert_button_1).setOnClickListener{
                        if(main_help_link != ""){
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(main_help_link))
                            startActivity(browserIntent)
                        }
                    };

                    findViewById<Button>(R.id.mobile_need_help_alert_button_2).setOnClickListener{
                        if(secondary_help_link != ""){
                            if(secondary_help_link.contains("mailto:")){
                                val intent = Intent(Intent.ACTION_SENDTO)
                                intent.data = Uri.parse(secondary_help_link) // only email apps should handle this
                                startActivity(Intent.createChooser(intent,getResources().getString(R.string.mobile_need_help_alert_title)))
                            }else{
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(secondary_help_link))
                                startActivity(browserIntent)
                            }
                        }
                    };

                    val close_button = findViewById<ImageView>(R.id.need_help_alert_cancel_button);
                    close_button.setOnClickListener{
                        function()
                        hideAlert()
                    }
                    focusableView = close_button;
                }
                else if (message =="editUrl"){
                    val binding: EditPageUrlAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.edit_page_url_alert, alertsContainer, true)
                    binding.colorList = getColorList("")

                    val urlTextBox = findViewById<EditText>(R.id.edit_page_url)
                    urlTextBox.setText((alertData as String));
                    val saveButton = findViewById<TextView>(R.id.edit_url_alert_save_button)
                    val cancelButton = findViewById<ImageView>(R.id.edit_goal_alert_cancel_button)
                    cancelButton.setOnClickListener(){
                        hideAlert()
                    }
                    saveButton.setOnClickListener(){
                        updateURL("personal")
                    }
                    modal_vo_descriptor = R.id.alert_vo_heading
                    focusableView = cancelButton
                }
                else if (message =="editTeamUrl"){
                    val binding: EditTeamPageUrlAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.edit_team_page_url_alert, alertsContainer, true)
                    binding.colorList = getColorList("")
                    val urlTextBox = findViewById<EditText>(R.id.edit_page_url)
                    urlTextBox.setText((alertData as String));
                    val saveButton = findViewById<TextView>(R.id.edit_url_alert_save_button)
                    val cancelButton = findViewById<ImageView>(R.id.edit_goal_alert_cancel_button)
                    cancelButton.setOnClickListener(){
                        hideAlert()
                    }
                    saveButton.setOnClickListener(){
                        updateURL("team")
                    }
                    modal_vo_descriptor = R.id.alert_vo_heading
                    focusableView = cancelButton
                }
                else if (message =="editCompanyUrl"){
                    val binding: EditCompanyPageUrlAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.edit_company_page_url_alert, alertsContainer, true)
                    binding.colorList = getColorList("")
                    val urlTextBox = findViewById<EditText>(R.id.edit_page_url)
                    urlTextBox.setText((alertData as String));
                    val saveButton = findViewById<TextView>(R.id.edit_url_alert_save_button)
                    val cancelButton = findViewById<ImageView>(R.id.edit_goal_alert_cancel_button)
                    cancelButton.setOnClickListener(){
                        hideAlert()
                    }
                    saveButton.setOnClickListener(){
                        updateURL("company")
                    }
                    modal_vo_descriptor = R.id.company_alert_vo_heading
                    focusableView = cancelButton
                }
                else if (message == "switchEvents"){
                    val binding: SwitchEventsAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.switch_events_alert, alertsContainer, true)

                    val button_container = findViewById<LinearLayout>(R.id.switch_events_alert_button_container)

                    binding.colorList = getColorList("login")
                    val cancel_button = findViewById<ImageView>(R.id.switch_events_alert_cancel_button);
                    if(alertData == ""){
                        cancel_button.setVisibility(View.VISIBLE)
                        cancel_button.setOnClickListener{
                            hideAlert()
                        }

                        focusableView = cancel_button
                        alertSender = findViewById<LinearLayout>(R.id.menu_option_switch_events)
                    }else{
                        cancel_button.setVisibility(View.GONE)
                        focusableView = button_container
                    }

                    modal_vo_descriptor = R.id.switch_events_alert_heading
                    modal_vo_delay = 250

                    val events = getEvents();

                    for (i in 0..events.size - 1) {

                        val binding: CustomButtonBinding = DataBindingUtil.inflate(
                            inflater, R.layout.custom_button, button_container, true)
                        binding.colorList = getColorList("login")

                        val inflated_button = binding.root as LinearLayout
                        var button = (inflated_button.getChildAt(0) as Button)
                        button.text = events[i].event_name
                        button.setOnClickListener{
                            setEvent(events[i])
                        }
                    }
                }
                else if (message == "photoAlert"){
                    val binding: PhotoAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.photo_alert, alertsContainer, true)
                    binding.colorList = getColorList("")
                    val close_button = binding.root.findViewById<ImageView>(R.id.photo_alert_close_button);
                    val gallery_button = binding.root.findViewById<TextView>(R.id.photo_alert_gallery_button);
                    val camera_button = binding.root.findViewById<TextView>(R.id.photo_alert_camera_button);

                    modal_vo_descriptor = R.id.photo_alert_heading

                    close_button.setOnClickListener {
                        function()
                        hideAlert()
                    }
                    focusableView = close_button

                    gallery_button.setOnClickListener {
                        if(alertData == "team"){
                            childviewCallback("team_gallery","")
                        }else if(alertData == "company"){
                            childviewCallback("company_gallery","")
                        }else {
                            childviewCallback("gallery","")
                        }
                        hideAlert()
                    }

                    camera_button.setOnClickListener {
                        if(alertData == "team"){
                            childviewCallback("team_camera","")
                        }else if(alertData == "company"){
                            childviewCallback("company_camera","")
                        } else {
                            childviewCallback("camera","")
                        }
                        hideAlert()
                    }
                }
                else if (message == "updateAlertForced" || message == "updateAlertRecommended"){
                    val binding: StandardCloseAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.standard_close_alert, alertsContainer, true)
                    binding.colorList = getColorList("login")

                    val message_textview = findViewById<TextView>(R.id.standard_alert_message);
                    val close_button = findViewById<ImageView>(R.id.standard_alert_cancel_button);
                    val action_button = findViewById<TextView>(R.id.standard_alert_action_button);
                    val alertDataArray = alertData as Array<String>
                    message_textview.setText(alertDataArray[0])
                    action_button.setText(alertDataArray[1])

                    action_button.setOnClickListener {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getStringVariable("PLAY_STORE_PRIMARY_URL"))))
                        } catch (e: ActivityNotFoundException) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getStringVariable("PLAY_STORE_SECONDARY_URL"))))
                        }
                    }

                    if(message == "updateAlertForced"){
                        close_button.setVisibility(View.GONE)
                        focusableView = action_button
                    }else{
                        close_button.setVisibility(View.VISIBLE)
                        close_button.setOnClickListener{
                            childviewCallback("updateRecommended","")
                            function()
                            hideAlert()
                        }
                        focusableView = close_button
                    }
                }
                else if (message == "donationProcessing"){
                    sendGoogleAnalytics("check_deposit_donation_processing","check_deposit")
                    val binding: DepositProcessingAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.deposit_processing_alert, alertsContainer, true)
                    binding.colorList = getColorList("")
                    val view = findViewById<ImageView>(R.id.rotating_arrows_icon);
                    val anim = ObjectAnimator.ofFloat(view, "rotation", 1800f).apply {
                        duration = 60000
                        start()
                    }
                    anim.setInterpolator(AccelerateDecelerateInterpolator());
                    modal_vo_descriptor = R.id.donation_processing_information
                }
                else if (message == "donationSucceeded"){
                    sendGoogleAnalytics("check_deposit_donation_success","check_deposit")
                    val binding: DepositSuccessAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.deposit_success_alert, alertsContainer, true)
                    binding.colorList = getColorList("")
                    val array = alertData as Array<String>
                    val alert = binding.root
                    alert.findViewById<TextView>(R.id.success_donor_name).text = array.get(0)
                    alert.findViewById<TextView>(R.id.success_check_amount).text = array.get(1)

                    val close_button = alert.findViewById<ImageView>(R.id.donation_success_alert_close_button);
                    val done_button = alert.findViewById<TextView>(R.id.deposit_success_done_button);

                    modal_vo_descriptor = R.id.donation_successful_information

                    focusableView = close_button
                    close_button.setOnClickListener {
                        hideAlert()
                        if(getStringVariable("CHECK_EVENT_MANAGER") != "true"){
                            val intent = Intent(this@BaseLanguageActivity, Donations::class.java);
                            startActivity(intent);
                            this.overridePendingTransition(0, 0);
                        }else{
                            val intent = Intent(this@BaseLanguageActivity, CheckDeposit::class.java);
                            intent.putExtra("event_id",getStringVariable("CHECK_DEPOSIT_EVENT_ID"))
                            intent.putExtra("event_name",getStringVariable("CHECK_DEPOSIT_EVENT_NAME"))
                            if(getStringVariable("CHECK_DEPOSIT_SPLIT_CHECK_ENABLED") != "true"){
                                intent.putExtra("check_credit",getStringVariable("CHECK_CREDIT"))
                                intent.putExtra("check_credit_value",getStringVariable("CHECK_CREDIT_VALUE"))
                            }else{
                                intent.putExtra("check_credit","")
                                intent.putExtra("check_credit_value","")
                            }

                            startActivity(intent);
                            this.overridePendingTransition(0, 0);
                        }
                    }

                    done_button.setOnClickListener {
                        hideAlert()
                        if(getStringVariable("CHECK_EVENT_MANAGER") != "true"){
                            val intent = Intent(this@BaseLanguageActivity, Donations::class.java);
                            startActivity(intent);
                            this.overridePendingTransition(0, 0);
                        }else{
                            val intent = Intent(this@BaseLanguageActivity, CheckDeposit::class.java);
                            intent.putExtra("event_id",getStringVariable("CHECK_DEPOSIT_EVENT_ID"))
                            intent.putExtra("event_name",getStringVariable("CHECK_DEPOSIT_EVENT_NAME"))
                            if(getStringVariable("CHECK_DEPOSIT_SPLIT_CHECK_ENABLED") != "true"){
                                intent.putExtra("check_credit",getStringVariable("CHECK_CREDIT"))
                                intent.putExtra("check_credit_value",getStringVariable("CHECK_CREDIT_VALUE"))
                            }else{
                                intent.putExtra("check_credit","")
                                intent.putExtra("check_credit_value","")
                            }
                            startActivity(intent);
                            this.overridePendingTransition(0, 0);
                        }
                    }
                }
                else if (message == "findMember"){
                    val binding: FindMemberAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.find_member_alert, alertsContainer, true)
                    binding.colorList = getColorList("")
                    val alert = binding.root
                    findViewById<LinearLayout>(R.id.search_results_container).setVisibility(View.GONE)
                    val close_button = alert.findViewById<ImageView>(R.id.member_alert_close_button);
                    val search_button = alert.findViewById<TextView>(R.id.btn_search);
                    focusableView = close_button
                    modal_vo_descriptor = R.id.alert_vo_heading
                    if(alertData != ""){
                        val array = alertData as Array<String>
                        findViewById<EditText>(R.id.input_member_first_name).setText(array[0])
                        findViewById<EditText>(R.id.input_member_last_name).setText(array[1])
                    }
                    close_button.setOnClickListener {
                        function()
                        hideAlert()
                        sendGoogleAnalytics("check_deposit_find_member_close","check_deposit")
                    }

                    search_button.setOnClickListener {
                        if(!donorSearching){
                            sendGoogleAnalytics("check_deposit_find_member_search","check_deposit")
                            it.hideKeyboard()
                            loadMembers(alert.findViewById<TextView>(R.id.btn_search))
                        }
                    }
                }
                else if (message == "findMatch"){
                    selectedDonorToMatch = Match("","",0.00,Date(),"",false,"", "", "")
                    val binding: FindMatchAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.find_match_alert, alertsContainer, true)
                    binding.colorList = getColorList("")
                    val alert = binding.root

                    val close_button = alert.findViewById<ImageView>(R.id.match_alert_close_button);
                    close_button.setOnClickListener {
                        function()
                        hideAlert()
                    }

                    val search_button = alert.findViewById<TextView>(R.id.btn_search);

                    if(nonmatched.count() <= 10){
                        findViewById<LinearLayout>(R.id.search_fields_container).setVisibility(View.GONE)
                        findViewById<TextView>(R.id.search_results_title).setVisibility(View.GONE)
                        loadMatchesSearchRows(nonmatched)
                    }else{
                        findViewById<LinearLayout>(R.id.search_results_container).setVisibility(View.GONE)
                        findViewById<LinearLayout>(R.id.search_fields_container).setVisibility(View.VISIBLE)

                        focusableView = close_button
                        modal_vo_descriptor = R.id.match_alert_vo_heading
                        modal_vo_delay = 500;
                        if(alertData != ""){
                            val array = alertData as Array<String>
                            findViewById<EditText>(R.id.input_match_first_name).setText(array[0])
                            findViewById<EditText>(R.id.input_match_last_name).setText(array[1])
                        }

                        search_button.setOnClickListener {
                            if(!matchSearching){
                                it.hideKeyboard()
                                matchSearching = true;
                                findViewById<TextView>(R.id.btn_search).setAlpha(.5F)
                                findViewById<TextView>(R.id.btn_search).text = getResources().getString(R.string.mobile_donations_double_donation_donor_searching_button)

                                val first_name_search = findViewById<EditText>(R.id.input_match_first_name).text
                                val last_name_search = findViewById<EditText>(R.id.input_match_last_name).text

                                if(first_name_search.length < 1 || last_name_search.length < 1){
                                    displayAlert(getString(R.string.mobile_donations_double_donation_donor_search_results_missing_fields), arrayOf(first_name_search, last_name_search),{ displayAlert("findMatch") })
                                }

                                var foundMatches = listOf<Match>();

                                for (i in 0 until nonmatched.count()) {
                                    val donation = nonmatched.get(i);
                                    if(donation.first_name.contains(first_name_search.toString(), ignoreCase = true) && donation.last_name.contains(last_name_search, ignoreCase = true)){
                                        foundMatches += donation;
                                    }
                                }

                                loadMatchesSearchRows(foundMatches);
                                matchSearching = false
                                findViewById<TextView>(R.id.btn_search).setAlpha(1F)
                                findViewById<TextView>(R.id.btn_search).text = getResources().getString(R.string.mobile_donations_double_donation_donor_search_button)
                            }
                        }
                    }
                }
                else if (message == "findMatchCompany"){
                    selectedCompanyToMatch = JSONObject("{}")
                    val binding: FindMatchCompanyAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.find_match_company_alert, alertsContainer, true)
                    binding.colorList = getColorList("")
                    val alert = binding.root

                    val close_button = alert.findViewById<ImageView>(R.id.match_alert_close_button);
                    close_button.setOnClickListener {
                        function()
                        hideAlert()
                    }

                    val tv = findViewById<TextView>(R.id.company_search_description)
                    val name = selectedDonorToMatch.first_name + " " + selectedDonorToMatch.last_name
                    val content = SpannableString(
                        getString(R.string.mobile_donations_double_donation_company_search_sms_message_1) + " " +
                        name + " " + getString(R.string.mobile_donations_double_donation_company_search_sms_message_2)
                    )
                    val bold_start = getString(R.string.mobile_donations_double_donation_company_search_sms_message_1).length
                    val bold_end = bold_start + name.length + 2
                    content.setSpan(StyleSpan(Typeface.BOLD), bold_start, bold_end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    tv.setText(content)

                    val content2 = SpannableString(
                        getString(R.string.mobile_donations_double_donation_company_search_description_1_android) + " " + getString(R.string.mobile_donations_double_donation_company_search_description_2)
                    )
                    val bold_start_2 = getString(R.string.mobile_donations_double_donation_company_search_description_1_android).length
                    val bold_end_2 = bold_start_2 + getString(R.string.mobile_donations_double_donation_company_search_description_2).length;
                    content2.setSpan(StyleSpan(Typeface.BOLD), bold_start_2, bold_end_2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    findViewById<TextView>(R.id.company_search_description_2).setText(content2)

                    val content3 = SpannableString(
                        getString(
                            R.string.mobile_donations_double_donation_company_search_results_subtitle_1_android)
                                + " " + getString(R.string.mobile_donations_double_donation_company_search_results_subtitle_2)
                                + " " + getString(R.string.mobile_donations_double_donation_company_search_results_subtitle_3)

                    )
                    val bold_start_3 = getString(R.string.mobile_donations_double_donation_company_search_results_subtitle_1_android).length
                    val bold_end_3 = bold_start_3 + getString(R.string.mobile_donations_double_donation_company_search_results_subtitle_2).length + 1;
                    content3.setSpan(StyleSpan(Typeface.BOLD), bold_start_3, bold_end_3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    findViewById<TextView>(R.id.search_results_description).setText(content3)

                    findViewById<Button>(R.id.match_sms_share_button).setOnClickListener{
                        val intent = Intent(Intent.ACTION_SENDTO)
                        intent.data = Uri.parse("smsto:")
                        intent.putExtra(Intent.EXTRA_TEXT,getString(R.string.mobile_donations_double_donation_company_search_sms_message_text_android))
                        startActivity(Intent.createChooser(intent,getResources().getString(R.string.mobile_fundraise_share_dialog_title)))
                    }

                    val search_button = alert.findViewById<TextView>(R.id.btn_search);
                    search_button.setOnClickListener{
                        loadMatchCompanies(search_button)
                    }
                    findViewById<TextView>(R.id.company_match_back).setOnClickListener{
                        displayAlert("findMatch")
                    }
                    findViewById<LinearLayout>(R.id.search_results_container).setVisibility(View.GONE)

                    findViewById<Button>(R.id.donor_match_next_btn).setOnClickListener{
                        displayAlert("matchGift")
                    }
                }
                else if (message == "matchGift"){
                    val binding: FindMatchGiftAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.find_match_gift_alert, alertsContainer, true)
                    binding.colorList = getColorList("")
                    val alert = binding.root

                    val close_button = alert.findViewById<ImageView>(R.id.match_alert_close_button);
                    close_button.setOnClickListener {
                        function()
                        hideAlert()
                    }

                    findViewById<TextView>(R.id.match_gift_donor_name).setText(selectedDonorToMatch.first_name + " " + selectedDonorToMatch.last_name)
                    val comp_name = getSafeStringVariable(selectedCompanyToMatch,"company_name");
                    if(comp_name != ""){
                        findViewById<TextView>(R.id.match_gift_company_name).setText(comp_name)
                    }else{
                        findViewById<TextView>(R.id.match_gift_company_name).visibility = View.GONE
                    }

                    findViewById<TextView>(R.id.match_gift_amount).setText(formatDoubleToLocalizedCurrency(selectedDonorToMatch.amount))

                    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                    val donorDate = dateFormat.format(selectedDonorToMatch.date);
                    findViewById<TextView>(R.id.match_gift_date).setText(donorDate)

                    findViewById<Button>(R.id.match_sms_share_button).setOnClickListener{
                        val intent = Intent(Intent.ACTION_SENDTO)
                        intent.data = Uri.parse("smsto:")
                        intent.putExtra(Intent.EXTRA_TEXT,getString(R.string.mobile_donations_double_donation_match_gift_sms_message_android))
                        startActivity(Intent.createChooser(intent,getResources().getString(R.string.mobile_fundraise_share_dialog_title)))
                    }

                    findViewById<TextView>(R.id.company_match_back).setOnClickListener{
                        displayAlert("findMatchCompany")
                    }

                    findViewById<Button>(R.id.donor_match_initiate_btn).setOnClickListener{
                        var company_id = getSafeIntegerVariable(selectedCompanyToMatch, "id");
                        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/check/doubledonation/registerDonation")
                        val formBody = FormBody.Builder()
                            .add("cons_id", getConsID())
                            .add("event_id", getEvent().event_id)
                            .add("donor_first_name", selectedDonorToMatch.first_name)
                            .add("donor_last_name", selectedDonorToMatch.last_name)
                            .add("donation_date", selectedDonorToMatch.date.toString())
                            .add("donor_email", selectedDonorToMatch.email)
                            .add("donation_amount", selectedDonorToMatch.amount.toString())
                            .add("vendor_donation_id", selectedDonorToMatch.vendor_donation_id)
                            .add("doublethedonation_company_id", company_id.toString())
                            .build()

                        var request = Request.Builder().url(url)
                            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                            .post(formBody)
                            .build()

                        var client = OkHttpClient();
                        client.newCall(request).enqueue(object : Callback {
                            override fun onResponse(call: Call, response: Response) {
                                if(response.code != 200){
                                    donorSearching = false
                                    childviewCallback("matchError","")
                                } else {
                                    donorSearching = false
                                    childviewCallback("matchSuccess","")
                                }
                            }

                            override fun onFailure(call: Call, e: java.io.IOException) {
                                println(e.message.toString())
                                donorSearching = false;
                            }
                        })
                    }
                }
                else if (message == "findDonor"){
                    val binding: FindDonorAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.find_donor_alert, alertsContainer, true)
                    binding.colorList = getColorList("")
                    val alert = binding.root
                    findViewById<LinearLayout>(R.id.search_results_container).setVisibility(View.GONE)
                    val close_button = alert.findViewById<ImageView>(R.id.donor_alert_close_button);
                    val search_button = alert.findViewById<TextView>(R.id.btn_search);
                    focusableView = close_button
                    modal_vo_descriptor = R.id.donor_alert_vo_heading
                    modal_vo_delay = 500
                    if(alertData != ""){
                        val array = alertData as Array<String>
                        findViewById<EditText>(R.id.input_donor_first_name).setText(array[0])
                        findViewById<EditText>(R.id.input_donor_last_name).setText(array[1])
                        findViewById<EditText>(R.id.input_donor_zip).setText(array[2])
                    }
                    close_button.setOnClickListener {
                        function()
                        hideAlert()
                        sendGoogleAnalytics("check_deposit_find_donor_close","check_deposit")
                    }

                    search_button.setOnClickListener {
                        if(!donorSearching){
                            sendGoogleAnalytics("check_deposit_find_donor_search","check_deposit")
                            it.hideKeyboard()
                            loadDonors(alert.findViewById<TextView>(R.id.btn_search))
                        }
                    }

                    val dropdown: Spinner = findViewById(R.id.find_donor_alert_donor_type)
                    val items = arrayOf(getResources().getString(R.string.mobile_donations_check_deposit_check_details_individual_type),getResources().getString(R.string.mobile_donations_check_deposit_check_details_company_type))
                    val adapter = ArrayAdapter<String>(this, R.layout.spinner_item, items)
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

                    dropdown.adapter = adapter
                    dropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            println("NOTHING SELECTED")
                        }

                        override fun onItemSelected(parent: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                            if(position == 0){
                                find_donor_type = "individual"
                                findViewById<LinearLayout>(R.id.find_donor_alert_individual_container).setVisibility(View.VISIBLE)
                                findViewById<LinearLayout>(R.id.find_donor_alert_company_container).setVisibility(View.GONE)
                            }else if(position == 1) {
                                find_donor_type = "company"
                                findViewById<LinearLayout>(R.id.find_donor_alert_individual_container).setVisibility(View.GONE)
                                findViewById<LinearLayout>(R.id.find_donor_alert_company_container).setVisibility(View.VISIBLE)
                            }
                            selectedItemView?.contentDescription = getString(R.string.mobile_donations_check_deposit_check_details_donor_type) + " , " + items[position]
                        }
                    }

                    if(find_donor_type == "company"){
                        dropdown.setSelection(1)
                    }
                }
                else if (message == "findParticipant"){
                    val binding: FindParticipantAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.find_participant_alert, alertsContainer, true)
                    binding.colorList = getColorList("")
                    findViewById<LinearLayout>(R.id.search_results_container).setVisibility(View.GONE)
                    val close_button = findViewById<ImageView>(R.id.participant_alert_close_button);
                    val search_button = findViewById<TextView>(R.id.btn_search);
                    focusableView = close_button
                    modal_vo_descriptor = R.id.alert_vo_heading
                    if(alertData != ""){
                        val array = alertData as Array<String>
                        findViewById<EditText>(R.id.input_participant_first_name).setText(array[0])
                        findViewById<EditText>(R.id.input_participant_last_name).setText(array[1])
                    }

                    close_button.setOnClickListener {
                        sendGoogleAnalytics("check_deposit_find_participant_close","check_deposit")
                        function()
                        hideAlert()
                    }

                    search_button.setOnClickListener {
                        sendGoogleAnalytics("check_deposit_find_participant_search","check_deposit")
                        it.hideKeyboard()
                        loadParticipants()
                    }
                }
                else if (message == "findTeam"){
                    val binding: FindTeamAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.find_team_alert, alertsContainer, true)
                    binding.colorList = getColorList("")
                    val alert = binding.root
                    findViewById<LinearLayout>(R.id.search_results_container).setVisibility(View.GONE)
                    val close_button = alert.findViewById<ImageView>(R.id.team_alert_close_button);
                    val search_button = alert.findViewById<TextView>(R.id.btn_search);
                    focusableView = close_button
                    modal_vo_descriptor = R.id.alert_vo_heading
                    if(alertData != ""){
                        val array = alertData as Array<String>
                        findViewById<EditText>(R.id.input_team_name).setText(array[0])
                    }

                    close_button.setOnClickListener {
                        sendGoogleAnalytics("check_deposit_find_team_close","check_deposit")
                        function()
                        hideAlert()
                    }

                    search_button.setOnClickListener {
                        sendGoogleAnalytics("check_deposit_find_team_search","check_deposit")
                        it.hideKeyboard()
                        loadTeams()
                    }
                }
                else if (message == "findEvent"){
                    if(getStringVariable("EVENT_SEARCH_TYPE") == "ANNUAL"){
                        val binding: FindEventChapterAlertBinding = DataBindingUtil.inflate(
                            inflater, R.layout.find_event_chapter_alert, alertsContainer, true)
                        binding.colorList = getColorList("")
                        val alert = binding.root
                        findViewById<LinearLayout>(R.id.search_results_container).setVisibility(View.GONE)
                    }else{
                        val binding: FindEventAlertBinding = DataBindingUtil.inflate(
                            inflater, R.layout.find_event_alert, alertsContainer, true)
                        binding.colorList = getColorList("")
                        val alert = binding.root
                        findViewById<LinearLayout>(R.id.search_results_container).setVisibility(View.GONE)
                    }

                    val close_button = findViewById<ImageView>(R.id.donor_alert_close_button);
                    val search_button = findViewById<TextView>(R.id.btn_search);
                    focusableView = close_button
                    modal_vo_descriptor = R.id.alert_vo_heading
                    if(alertData != ""){
                        val array = alertData as Array<String>
                        findViewById<EditText>(R.id.input_event_name).setText(array[0])
                    }

                    close_button.setOnClickListener {
                        sendGoogleAnalytics("check_deposit_find_event_close","check_deposit")
                        function()
                        hideAlert()
                    }

                    search_button.setOnClickListener {
                        sendGoogleAnalytics("check_deposit_find_event_search","check_deposit")
                        it.hideKeyboard()
                        loadSearchEvents()
                    }
                }
                else if (message == "plantFlower"){
                    val binding: PlantFlowerAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.plant_flower_alert, alertsContainer, true)
                    binding.colorList = getColorList("")

                    val alert = binding.root
                    val delete_button = alert.findViewById<TextView>(R.id.plant_flower_alert_delete_button);
                    val close_button = alert.findViewById<TextView>(R.id.plant_flower_alert_cancel_button);
                    val cancel_button = alert.findViewById<ImageView>(R.id.plant_flower_alert_close_button);
                    val plant_button = alert.findViewById<TextView>(R.id.plant_flower_alert_plant_button);
                    focusableView = close_button
                    var colorArrayString =  getStringVariable("FLOWER_COLORS_ARRAY")
                    var colorArrayDescriptionString =  getStringVariable("FLOWER_COLORS_TEXT_ARRAY")

                    var updating_flower = false;
                    if(alertData != ""){
                        updating_flower = true;
                    }

                    if(colorArrayString != "" && colorArrayDescriptionString != ""){
                        val colorArray: Array<String> = gson.fromJson(
                            colorArrayString,
                            Array<String>::class.java
                        )

                        val colorDescriptionArray: Array<String> = gson.fromJson(
                            colorArrayDescriptionString,
                            Array<String>::class.java
                        )

                        findViewById<Button>(R.id.plant_flower_alert_plant_button).setAlpha(.5F)
                        findViewById<TextView>(R.id.plant_flower_alert_color_description).setText(colorDescriptionArray[0])

                        val container = findViewById<LinearLayout>(R.id.plant_flower_alert_colors_container)
                        val buttonInflater = LayoutInflater.from(this@BaseLanguageActivity)
                        for (i in 0 until colorArray.size) {
                            val color = colorArray[i]
                            val buttonLayout = buttonInflater.inflate(R.layout.plant_flower_color_rounded_button, container) as LinearLayout
                            val button = (buttonLayout.getChildAt(i) as FrameLayout).getChildAt(0) as ImageView

                            val border = (buttonLayout.getChildAt(i) as FrameLayout).getChildAt(1) as ImageView
                            setTint(button.getBackground(), Color.parseColor(color));
                            if(i == 0){
                                setVariable("FLOWER_COLOR",color)
                                (buttonLayout.getChildAt(i) as FrameLayout).getChildAt(1).setVisibility(View.VISIBLE)
                            }else{
                                (buttonLayout.getChildAt(i) as FrameLayout).getChildAt(1).setVisibility(View.GONE)
                            }

                            button.setOnClickListener {
                                setVariable("FLOWER_COLOR",color)
                                for (j in 0 until colorArray.size) {
                                    (container.getChildAt(j) as FrameLayout).getChildAt(1).setVisibility(View.GONE)
                                }

                                println("DESCRIPTION: " + colorDescriptionArray[i])
                                findViewById<TextView>(R.id.plant_flower_alert_color_description).setText(colorDescriptionArray[i])
                                border.setVisibility(View.VISIBLE)
                            }
                        }

                        if(updating_flower){
                            findViewById<TextView>(R.id.select_flower_title).setVisibility(View.GONE)
                            findViewById<TextView>(R.id.plant_flower_description).setVisibility(View.GONE)
                            delete_button.setVisibility(View.VISIBLE)
                            findViewById<TextView>(R.id.plant_flower_alert_title).setText(getResources().getString(R.string.mobile_overview_promise_garden_edit_flower_modal_title))
                            findViewById<Button>(R.id.plant_flower_alert_plant_button).setText(getResources().getString(R.string.mobile_overview_promise_garden_update_flower_modal_button))
                            setVariable("FLOWER_MODE","update");
                            val array = alertData as Array<String>
                            setVariable("FLOWER_ID",array[0])
                            for (k in 0 until colorArray.size) {
                                if(colorArray[k] == array[1]){
                                    setVariable("FLOWER_COLOR",colorArray[k])
                                    (container.getChildAt(k) as FrameLayout).getChildAt(1).setVisibility(View.VISIBLE)
                                }else{
                                    (container.getChildAt(k) as FrameLayout).getChildAt(1).setVisibility(View.GONE)
                                }
                            }
                            findViewById<EditText>(R.id.input_flower_dedication).setText(array[2])
                            findViewById<EditText>(R.id.input_flower_message).setText(array[3])
                            checkFlowerForPlanting()
                        }else{
                            findViewById<TextView>(R.id.select_flower_title).setVisibility(View.VISIBLE)
                            findViewById<TextView>(R.id.plant_flower_description).setVisibility(View.VISIBLE)
                            delete_button.setVisibility(View.GONE)
                            findViewById<TextView>(R.id.plant_flower_alert_title).setText(getResources().getString(R.string.mobile_overview_promise_garden_plant_flower_modal_title))
                            findViewById<Button>(R.id.plant_flower_alert_plant_button).setText(getResources().getString(R.string.mobile_overview_promise_garden_plant_flower_modal_button))
                            clearVariable("FLOWER_ID")
                            setVariable("FLOWER_MODE","create");
                        }
                    }

                    findViewById<EditText>(R.id.input_flower_dedication).filters = arrayOf(EMOJI_FILTER,InputFilter.LengthFilter(20))
                    findViewById<EditText>(R.id.input_flower_message).filters = arrayOf(EMOJI_FILTER,InputFilter.LengthFilter(256))

                    findViewById<EditText>(R.id.input_flower_dedication).addTextChangedListener(object :
                        TextWatcher {
                        override fun afterTextChanged(s: Editable) {}
                        override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                            checkFlowerForPlanting()
                        }
                    })

                    findViewById<EditText>(R.id.input_flower_message).addTextChangedListener(object :
                        TextWatcher {
                        override fun afterTextChanged(s: Editable) {}
                        override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                            checkFlowerForPlanting()
                        }
                    })

                    close_button.setOnClickListener {
                        sendGoogleAnalytics("close","plant_flower_modal")
                        function()
                        hideAlert()
                    }

                    cancel_button.setOnClickListener {
                        sendGoogleAnalytics("close","plant_flower_modal")
                        function()
                        hideAlert()
                    }


                    delete_button.setOnClickListener {
                        function()
                        deleteFlower()
                    }

                    plant_button.setOnClickListener {
                        if(updating_flower){
                            sendGoogleAnalytics("update_flower","plant_flower_modal")
                        }else{
                            sendGoogleAnalytics("add_flower","plant_flower_modal")
                        }
                        it.hideKeyboard()
                        plantFlower()
                    }

                    modal_vo_descriptor = R.id.plant_flower_alert_heading
                    focusableView = cancel_button
                }
                else if (message == "createLuminary"){
                    val binding: CreateLuminaryAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.create_luminary_alert, alertsContainer, true)
                    binding.colorList = getColorList("")

                    val alert = binding.root
                    val delete_button = alert.findViewById<TextView>(R.id.create_luminary_alert_delete_button);
                    val close_button = alert.findViewById<TextView>(R.id.create_luminary_alert_cancel_button);
                    val cancel_button = alert.findViewById<ImageView>(R.id.create_luminary_alert_close_button);
                    val plant_button = alert.findViewById<TextView>(R.id.create_luminary_alert_create_button);
                    focusableView = close_button
                    var updating_luminary = false;
                    if(alertData != "") {
                        updating_luminary = true;
                        val array = alertData as Array<String>
                        setVariable("LUMINARY_MODE","update")
                        setVariable("LUMINARY_ID",array[0])
                        findViewById<EditText>(R.id.input_luminary_dedication).setText(array[1])
                        findViewById<EditText>(R.id.input_luminary_message).setText(array[2])
                        checkLuminaryForCreation()
                        findViewById<TextView>(R.id.create_luminary_alert_title).text = getString(R.string.mobile_overview_luminary_edit_luminary_modal_title)
                        findViewById<TextView>(R.id.create_luminary_alert_create_button).text = getString(R.string.mobile_overview_luminary_update_luminary_modal_button)
                    }else{
                        setVariable("LUMINARY_MODE","create")
                        clearVariable("LUMINARY_ID")
                        findViewById<TextView>(R.id.create_luminary_alert_title).text = getString(R.string.mobile_overview_luminary_create_luminary_modal_title)
                        findViewById<TextView>(R.id.create_luminary_alert_create_button).text = getString(R.string.mobile_overview_luminary_create_luminary_modal_button)
                        delete_button.setVisibility(View.GONE)
                    }

                    findViewById<EditText>(R.id.input_luminary_dedication).filters = arrayOf(EMOJI_FILTER,InputFilter.LengthFilter(20))
                    findViewById<EditText>(R.id.input_luminary_message).filters = arrayOf(EMOJI_FILTER,InputFilter.LengthFilter(256))

                    findViewById<EditText>(R.id.input_luminary_dedication).addTextChangedListener(object :
                        TextWatcher {
                        override fun afterTextChanged(s: Editable) {}
                        override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                            checkLuminaryForCreation()
                        }
                    })

                    findViewById<EditText>(R.id.input_luminary_message).addTextChangedListener(object :
                        TextWatcher {
                        override fun afterTextChanged(s: Editable) {}
                        override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                            checkLuminaryForCreation()
                        }
                    })

                    close_button.setOnClickListener {
                        sendGoogleAnalytics("close","create_luminary_modal")
                        function()
                        hideAlert()
                    }

                    cancel_button.setOnClickListener {
                        sendGoogleAnalytics("close","create_luminary_modal")
                        function()
                        hideAlert()
                    }


                    delete_button.setOnClickListener {
                        function()
                        deleteLuminary()
                    }

                    plant_button.setOnClickListener {
                        if(updating_luminary){
                            sendGoogleAnalytics("update_luminary","create_luminary_modal")
                        }else{
                            sendGoogleAnalytics("create_luminary","create_luminary_modal")
                        }
                        it.hideKeyboard()
                        createLuminary()
                    }

                    modal_vo_descriptor = R.id.create_luminary_alert_heading
                    focusableView = cancel_button
                }
                else if (message == "createJersey"){
                    val binding: CreateJerseyAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.create_jersey_alert, alertsContainer, true)
                    binding.colorList = getColorList("")

                    val alert = binding.root
                    val delete_button = alert.findViewById<TextView>(R.id.create_jersey_alert_delete_button);
                    val close_button = alert.findViewById<TextView>(R.id.create_jersey_alert_cancel_button);
                    val cancel_button = alert.findViewById<ImageView>(R.id.create_jersey_alert_close_button);
                    val plant_button = alert.findViewById<TextView>(R.id.create_jersey_alert_create_button);
                    focusableView = close_button
                    var updating_jersey = false;
                    if(alertData != "") {
                        updating_jersey = true;
                        val array = alertData as Array<String>
                        setVariable("JERSEY_MODE","update")
                        setVariable("JERSEY_ID",array[0])
                        findViewById<EditText>(R.id.input_jersey_dedication).setText(array[1])
                        findViewById<EditText>(R.id.input_jersey_message).setText(array[2])
                        checkJerseyForCreation()
                        findViewById<TextView>(R.id.create_jersey_alert_title).text = getString(R.string.mobile_overview_jersey_edit_jersey_modal_title)
                        findViewById<TextView>(R.id.create_jersey_alert_create_button).text = getString(R.string.mobile_overview_jersey_update_jersey_modal_button)
                    }else{
                        setVariable("JERSEY_MODE","create")
                        clearVariable("JERSEY_ID")
                        findViewById<TextView>(R.id.create_jersey_alert_title).text = getString(R.string.mobile_overview_jersey_create_jersey_modal_title)
                        findViewById<TextView>(R.id.create_jersey_alert_create_button).text = getString(R.string.mobile_overview_jersey_create_jersey_modal_button)
                        delete_button.setVisibility(View.GONE)
                    }

                    findViewById<EditText>(R.id.input_jersey_dedication).filters = arrayOf(EMOJI_FILTER,InputFilter.LengthFilter(20))
                    findViewById<EditText>(R.id.input_jersey_message).filters = arrayOf(EMOJI_FILTER,InputFilter.LengthFilter(256))

                    findViewById<EditText>(R.id.input_jersey_dedication).addTextChangedListener(object :
                        TextWatcher {
                        override fun afterTextChanged(s: Editable) {}
                        override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                            checkJerseyForCreation()
                        }
                    })

                    findViewById<EditText>(R.id.input_jersey_message).addTextChangedListener(object :
                        TextWatcher {
                        override fun afterTextChanged(s: Editable) {}
                        override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                            checkJerseyForCreation()
                        }
                    })

                    close_button.setOnClickListener {
                        sendGoogleAnalytics("close","create_jersey_modal")
                        function()
                        hideAlert()
                    }

                    cancel_button.setOnClickListener {
                        sendGoogleAnalytics("close","create_jersey_modal")
                        function()
                        hideAlert()
                    }


                    delete_button.setOnClickListener {
                        function()
                        deleteJersey()
                    }

                    plant_button.setOnClickListener {
                        if(updating_jersey){
                            sendGoogleAnalytics("update_jersey","create_jersey_modal")
                        }else{
                            sendGoogleAnalytics("create_jersey","create_jersey_modal")
                        }
                        it.hideKeyboard()
                        createJersey()
                    }

                    modal_vo_descriptor = R.id.create_jersey_alert_heading
                    focusableView = cancel_button
                }
                else if (message == "addManualActivity"){
                    activity_type = "workout"
                    activity_day = ""
                    activity_hour = ""
                    activity_month = ""
                    activity_year = 0
                    activity_minute = ""
                    val binding: AddManualActivityBinding = DataBindingUtil.inflate(
                        inflater, R.layout.add_manual_activity, alertsContainer, true)
                    binding.colorList = getColorList("")

                    modal_vo_descriptor = R.id.activity_alert_heading
                    modal_vo_delay = 350

                    val save_button = findViewById<Button>(R.id.add_manual_activity_save_button)
                    save_button.setAlpha(.5F)

                    val alert = binding.root

                    val workout_container = findViewById<LinearLayout>(R.id.add_manual_activity_workouts_container)
                    val steps_container = findViewById<LinearLayout>(R.id.add_manual_activity_steps_container)
                    steps_container.setVisibility(View.GONE)

                    var type_items = emptyArray<String>()

                    if(getStringVariable("ACTIVITY_TRACKING_WORKOUTS_ENABLED") == "true"){
                        type_items += getString(R.string.mobile_track_activity_add_activity_modal_workout)
                    }else{
                        activity_type = "steps"
                        steps_container.setVisibility(View.VISIBLE)
                        workout_container.setVisibility(View.GONE)
                    }

                    if(getStringVariable("ACTIVITY_TRACKING_STEPS_ENABLED") == "true"){
                        type_items += getString(R.string.mobile_track_activity_add_activity_modal_steps)
                    }

                    val type_adapter = ArrayAdapter<String>(this, R.layout.spinner_item, type_items)
                    type_adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                    val selectActivityTypeSpinner = findViewById<Spinner>(R.id.select_activity_type)
                    selectActivityTypeSpinner.adapter = type_adapter

                    val alertDataArray = alertData as Array<Any>

                    val arrayData =  alertDataArray[0] as Array<WorkoutType>
                    val mutableList = arrayData.toMutableList()
                    mutableList.add(0, WorkoutType(getString(R.string.mobile_track_activity_add_activity_modal_select_workout),"","",false))
                    val itemsArray = mutableList.toTypedArray() 

                    val source = alertDataArray[1] as String
                    var items = emptyArray<String>()

                    itemsArray.forEach {
                        items += it.name
                    }

                    findViewById<Spinner>(R.id.select_activity_type).setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parentView: AdapterView<*>?,
                            selectedItemView: View?,
                            position: Int,
                            id: Long
                        ) {
                            val selected = type_items[position]
                            if(selected == getString(R.string.mobile_track_activity_add_activity_modal_workout)){
                                activity_type = "workout"
                                checkCanAddActivity(save_button,"add")
                                workout_container.setVisibility(View.VISIBLE)
                                steps_container.setVisibility(View.GONE)
                            }else{
                                activity_type = "steps"
                                checkCanAddActivity(save_button,"add")
                                steps_container.setVisibility(View.VISIBLE)
                                workout_container.setVisibility(View.GONE)
                            }
                        }

                        override fun onNothingSelected(parentView: AdapterView<*>?) {
                            // your code here
                        }
                    })

                    val adapter = ArrayAdapter(this, R.layout.spinner_item, items)
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                    val selectWorkoutTypeSpinner = findViewById<Spinner>(R.id.select_workout_type)
                    selectWorkoutTypeSpinner.adapter = adapter
                    findViewById<Spinner>(R.id.select_workout_type).setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                        @RequiresApi(Build.VERSION_CODES.O)
                        override fun onItemSelected(
                            parentView: AdapterView<*>?,
                            selectedItemView: View?,
                            position: Int,
                            id: Long
                        ) {
                            val item = itemsArray[position]
                            activity_internal_name = item.internal_name
                            if(item.allow_distance){
                                activity_modal_distance_enabled = true
                                findViewById<LinearLayout>(R.id.activity_distance_input_container).setVisibility(View.VISIBLE)
                                checkCanAddActivity(save_button,"add")
                            }else{
                                activity_modal_distance_enabled = false
                                findViewById<LinearLayout>(R.id.activity_distance_input_container).setVisibility(View.GONE)
                                checkCanAddActivity(save_button,"add")
                            }

                        }

                        override fun onNothingSelected(parentView: AdapterView<*>?) {
                            // your code here
                        }
                    })

                    findViewById<EditText>(R.id.activity_distance_input).addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable) {}
                        override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                            checkCanAddActivity(save_button,"add")
                        }
                    })

                    findViewById<EditText>(R.id.activity_time_input).addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable) {}
                        override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                            val validFormat = checkTimeFormat(s.toString().trim())
                            if(validFormat || s == ""){
                                findViewById<TextView>(R.id.add_activity_time_error_message).visibility = View.GONE
                                activity_hour = s.substring(0, 2)
                                activity_minute = s.substring(3, 5)
                                if(activity_hour.toInt() > 23){
                                    findViewById<EditText>(R.id.activity_time_input).setText("23:59")
                                    activity_hour = "23";
                                    activity_minute = "59";
                                }else if(activity_minute.toInt() > 59){
                                    findViewById<EditText>(R.id.activity_time_input).setText(activity_hour + ":59")
                                    activity_minute = "59";
                                }
                                setInitialContentDescription(findViewById<EditText>(R.id.activity_time_input), activity_hour + ":" + activity_minute)
                            }else{
                                findViewById<TextView>(R.id.add_activity_time_error_message).visibility = View.VISIBLE
                            }
                            checkCanAddActivity(save_button,"add")
                        }
                    })

                    findViewById<EditText>(R.id.activity_steps_input).addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable) {}
                        override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                            checkCanAddActivity(save_button,"add")
                        }
                    })

                    val pickerDialogContainer = findViewById<ScrollView>(R.id.picker_modal_container)
                    val datePickerModal = findViewById<DatePicker>(R.id.add_manual_activity_date_picker)
                    val timePickerModal = findViewById<TimePicker>(R.id.add_manual_activity_time_picker)

                    val close_button = alert.findViewById<ImageView>(R.id.add_manual_activity_alert_close_button);
                    close_button.setOnClickListener {
                        if(source == "challenges"){
                            sendGoogleAnalytics("challenges_close_add_activity_modal",source)
                        }else{
                            sendGoogleAnalytics("save_add_manual_activity_modal",source)
                        }
                        function()
                        hideAlert()
                    }
                    focusableView = close_button

                    findViewById<View>(R.id.add_manual_activity_save_button).setOnClickListener{
                        hideKeyboard(this)
                        if(source == "challenges"){
                            sendGoogleAnalytics("challenges_save_add_activity_modal",source)
                        }else{
                            sendGoogleAnalytics("save_add_manual_activity_modal",source)
                        }
                        if(activity_type == "workout"){
                            if(safeToInt(activity_hour) > 23){
                                findViewById<EditText>(R.id.activity_time_input).setText("23:59")
                                activity_hour = "23";
                                activity_minute = "59";
                            }else if(safeToInt(activity_minute) > 59){
                                findViewById<EditText>(R.id.activity_time_input).setText(activity_hour + ":59")
                                activity_minute = "59";
                            }

                            val vendor_id = UUID.randomUUID().toString().replace("-","");
                            var type = activity_internal_name
                            if(type == ""){
                                findViewById<Spinner>(R.id.select_workout_type).selectedItem.toString().lowercase()
                            }
                            val distance = findViewById<EditText>(R.id.activity_distance_input).text.toString()

                            var start_time = activity_year.toString() + "-" + activity_month + "-"+ activity_day + "T00:00:00.000-0500"
                            var end_time = activity_year.toString() + "-" + activity_month + "-"+ activity_day + "T" + activity_hour + ":" + activity_minute + ":00.000-0500"

                            if(checkCanAddActivity(save_button,"add")){
                                val client = OkHttpClient()

                                val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/addWorkout")
                                val formBody = FormBody.Builder()
                                    .add("cons_id", getConsID())
                                    .add("event_id", getEvent().event_id)
                                    .add("vendor_id", vendor_id)
                                    .add("vendor", "MANUAL")
                                    .add("start_time", start_time)
                                    .add("end_time", end_time)
                                    .add("vendor_activity_type", type)
                                    .add("steps", "")
                                    .add("distance", distance)

                                if(getStringVariable("ACTIVITY_TRACKING_DISTANCE_METRIC") != ""){
                                    formBody.add("metric", getStringVariable("ACTIVITY_TRACKING_DISTANCE_METRIC"))
                                }

                                val request = Request.Builder()
                                    .url(url)
                                    .post(formBody.build())
                                    .header("Authorization", "Bearer " + getAuth())
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .header("Program-Id" , getStringVariable("PROGRAM_ID"))
                                    .build()

                                client.newCall(request).enqueue(object : Callback {
                                    override fun onResponse(call: Call, response: Response) {
                                        if(response.code != 200){
                                            throw Exception(response.body?.string())
                                        }else{
                                            val response = response.body?.string();
                                            val obj = JSONObject(response);
                                            hideAlert()
                                            childviewCallback("sync","")
                                        }
                                    }

                                    override fun onFailure(call: Call, e: IOException) {

                                    }
                                })
                            }
                        }else{
                            val steps = findViewById<EditText>(R.id.activity_steps_input).text.toString()
                            if(checkCanAddActivity(save_button,"add")){
                                val client = OkHttpClient()
                                val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/addSteps")
                                val formBody = FormBody.Builder()
                                    .add("event_id", getEvent().event_id)
                                    .add("cons_id", getConsID())
                                    .add("vendor", "MANUAL")
                                    .add("date", activity_year.toString() + "-" + activity_month + "-"+ activity_day)
                                    .add("steps", steps)
                                    .build()

                                val request = Request.Builder()
                                    .url(url)
                                    .post(formBody)
                                    .header("Authorization", "Bearer " + getAuth())
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .header("Program-Id" , getStringVariable("PROGRAM_ID"))
                                    .build()

                                client.newCall(request).enqueue(object : Callback {
                                    override fun onResponse(call: Call, response: Response) {
                                        if(response.code != 200){
                                            throw Exception(response.body?.string())
                                        }else{
                                            val response = response.body?.string();
                                            val obj = JSONObject(response);
                                            hideAlert()
                                            println("ADDED STEPS SUCCESFULLY")
                                            childviewCallback("sync","")
                                        }
                                    }

                                    override fun onFailure(call: Call, e: IOException) {

                                    }
                                })
                            }
                        }
                    }

                    val editText = findViewById<EditText>(R.id.activity_date_input)
                    val today = Calendar.getInstance();
                    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                    val todayString = dateFormat.format(today.time);
                    editText.setText(todayString);
                    activity_year = today.get(Calendar.YEAR);
                    activity_month = twoDigitString(today.get(Calendar.MONTH) + 1);
                    activity_day = twoDigitString(today.get(Calendar.DAY_OF_MONTH));

                    if(getStringVariable("TRACKING_STARTED") == "true"){
                        editText.setOnClickListener{
                            hideDateTimePicker()
                            hideKeyboard(this)
                            currentPicker = R.id.add_manual_activity_date_picker
                            datePickerModal.setVisibility(View.VISIBLE)
                            pickerDialogContainer.setVisibility(View.VISIBLE);
                           
                            datePickerModal.init(today.get(Calendar.YEAR), today.get(Calendar.MONTH),
                                today.get(Calendar.DAY_OF_MONTH)
                            ) { view, year, month, day ->

                            }

                            val cancel_button = findViewById<TextView>(R.id.date_picker_cancel_button);
                            val select_button = findViewById<TextView>(R.id.date_picker_select_button);

                            val cancel_name = cancel_button.text
                            val cancel_content = SpannableString(cancel_name)
                            cancel_content.setSpan(UnderlineSpan(), 0, cancel_content.length, 0)
                            cancel_button.setText(cancel_content);

                            val select_name = select_button.text
                            val select_content = SpannableString(select_name)
                            select_content.setSpan(UnderlineSpan(), 0, select_content.length, 0)
                            select_button.setText(select_content);
                        }
                    }else{
                        activity_day = twoDigitString(today.get(Calendar.DAY_OF_MONTH))
                        activity_month = twoDigitString((today.get(Calendar.MONTH) + 1))
                        activity_year = today.get(Calendar.YEAR)

                        val today_string = activity_month + "/" + activity_day + "/" + activity_year

                        editText.setOnClickListener{
                            findViewById<TextView>(R.id.add_activity_date_error_message).text = getString(R.string.mobile_track_activity_add_activity_modal_date_invalid_android).replace("XXXXXXX", today_string)
                            findViewById<TextView>(R.id.add_activity_date_error_message).setVisibility(View.VISIBLE)
                        }
                        editText.setText(today_string)


                        editText.isFocusable = false;
                    }


                    val timeEditText = findViewById<View>(R.id.activity_time_input) as EditText
                    timeEditText.accessibilityDelegate = TimeInputAccessibilityDelegate();

                    timeEditText.setOnClickListener{
                        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
                        val isTalkBackOn = accessibilityManager.isEnabled && accessibilityManager.isTouchExplorationEnabled

                        if(!isTalkBackOn){
                            hideKeyboard(this)
                            currentPicker = R.id.add_manual_activity_time_picker
                            timePickerModal.setIs24HourView(true)
                            timePickerModal.hour = 0;
                            timePickerModal.minute = 0;
                            findViewById<LinearLayout>(R.id.time_picker_overlay).visibility = View.VISIBLE
                            timePickerModal.setVisibility(View.VISIBLE)
                            pickerDialogContainer.setVisibility(View.VISIBLE);

                            val cancel_button = findViewById<TextView>(R.id.date_picker_cancel_button);
                            val select_button = findViewById<TextView>(R.id.date_picker_select_button);

                            val cancel_name = cancel_button.text
                            val cancel_content = SpannableString(cancel_name)
                            cancel_content.setSpan(UnderlineSpan(), 0, cancel_content.length, 0)
                            cancel_button.setText(cancel_content);

                            val select_name = select_button.text
                            val select_content = SpannableString(select_name)
                            select_content.setSpan(UnderlineSpan(), 0, select_content.length, 0)
                            select_button.setText(select_content);

                        } else {
                            timeEditText.isFocusableInTouchMode = true
                            timeEditText.setFocusable(true);
                            timeEditText.setClickable(false);
                            if(timeEditText.text.toString() == "00:00"){
                                timeEditText.setText("")
                            }

                            timeEditText.postDelayed({
                                timeEditText.requestFocus()
                                val imm = this@BaseLanguageActivity.getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                                imm?.showSoftInput(timeEditText, InputMethodManager.SHOW_IMPLICIT)
                            }, 100)
                        }
                    }

                    findViewById<Button>(R.id.date_picker_select_button).setOnClickListener{
                        if(currentPicker == R.id.add_manual_activity_date_picker){
                            activity_day = twoDigitString(datePickerModal.dayOfMonth)
                            activity_month = twoDigitString(datePickerModal.month + 1)
                            activity_year = datePickerModal.year
                            checkCanAddActivity(save_button,"add")
                            editText.setText(activity_month + "/" + activity_day + "/" + activity_year)
                        }else if (currentPicker == R.id.add_manual_activity_time_picker){
                            activity_hour = twoDigitString(timePickerModal.hour)
                            activity_minute = twoDigitString(timePickerModal.minute)
                            checkCanAddActivity(save_button,"add")
                            timeEditText.setText(activity_hour + ":" + activity_minute + " ")

                            val hourInt = activity_hour.toInt()
                            val minuteInt = activity_minute.toInt()
                            val contentDescription = "$hourInt hour${if (hourInt != 1) "s" else ""} $minuteInt minute${if (minuteInt != 1) "s" else ""}"

                            timeEditText.contentDescription = contentDescription
                            timeEditText.accessibilityDelegate = TimeInputAccessibilityDelegate()
                        }
                        hideDateTimePicker()
                    }
                    findViewById<Button>(R.id.date_picker_cancel_button).setOnClickListener{
                        hideDateTimePicker()
                    }

                    activity_hour = "";
                    activity_minute = "";
                }
                else if (message == "editManualActivity"){
                    val binding: EditManualActivityAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.edit_manual_activity_alert, alertsContainer, true)
                    binding.colorList = getColorList("")

                    modal_vo_descriptor = R.id.edit_activity_alert_heading
                    modal_vo_delay = 350

                    val save_button = findViewById<Button>(R.id.edit_manual_activity_save_button)
                    save_button.setAlpha(1F)
                    val activity = (alertData as Array<Any>)[1] as TrackActivity.Activity

                    val arrayData =  alertData[0] as Array<WorkoutType>
                    val mutableList = arrayData.toMutableList()
                    mutableList.add(0, WorkoutType(getString(R.string.mobile_track_activity_add_activity_modal_select_workout),"","",false))
                    val itemsArray = mutableList.toTypedArray() 
                    var items = emptyArray<String>()
                    itemsArray.forEach {
                        items += it.name
                    }
                    
                    if (activity.activity_type == "steps") {

                    } else {
                        activity_internal_name = itemsArray[items.indexOf(activity.activity_type_display_name)].internal_name
                    }
                    val alert = binding.root
                    val workout_container = findViewById<LinearLayout>(R.id.edit_manual_activity_workouts_container)
                    val steps_container = findViewById<LinearLayout>(R.id.edit_manual_activity_steps_container)

                    if(activity.activity_type == "steps"){
                        activity_type = "steps"
                        workout_container.setVisibility(View.GONE)
                    }else{
                        activity_type = "workout"
                        steps_container.setVisibility(View.GONE)
                    }
                    val adapter = ArrayAdapter<String>(this, R.layout.spinner_item, items)
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                    val spinner = findViewById<Spinner>(R.id.select_workout_type)
                    spinner.adapter = adapter
                    val selectionIndex = items.indexOf(activity.activity_type_display_name)
                    spinner.setSelection(selectionIndex)
                    checkCanAddActivity(save_button,"edit")

                    findViewById<Spinner>(R.id.select_workout_type).setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parentView: AdapterView<*>?,
                            selectedItemView: View?,
                            position: Int,
                            id: Long
                        ) {
                            val item = itemsArray[position]
                            activity_internal_name = item.internal_name
                            if(item.allow_distance){
                                activity_modal_distance_enabled = true
                                findViewById<LinearLayout>(R.id.activity_distance_input_container).setVisibility(View.VISIBLE)
                                checkCanAddActivity(save_button,"add")
                            }else{
                                activity_modal_distance_enabled = false
                                findViewById<LinearLayout>(R.id.activity_distance_input_container).setVisibility(View.GONE)
                                checkCanAddActivity(save_button,"add")
                            }

                        }

                        override fun onNothingSelected(parentView: AdapterView<*>?) {
                            // your code here
                        }
                    })

                    findViewById<EditText>(R.id.activity_distance_input).setText(activity.miles.toString())
                    findViewById<EditText>(R.id.activity_distance_input).addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable) {}
                        override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                            checkCanAddActivity(save_button,"edit")
                        }
                    })

                    findViewById<EditText>(R.id.activity_time_input).addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable) {
                            val validFormat = checkTimeFormat(s.toString().trim());
                            if(s.toString() != s.toString().trim()){
                                findViewById<EditText>(R.id.activity_time_input).setText(s.trim())
                            }else{
                                if(validFormat || s.toString() == ""){
                                    findViewById<TextView>(R.id.edit_activity_time_error_message).visibility = View.GONE
                                    activity_hour = s.substring(0, 2)
                                    activity_minute = s.substring(3, 5)
                                    if(activity_hour.toInt() > 23){
                                        findViewById<EditText>(R.id.activity_time_input).setText("23:59")
                                        activity_hour = "23";
                                        activity_minute = "59";
                                    }else if(activity_minute.toInt() > 59){
                                        findViewById<EditText>(R.id.activity_time_input).setText(activity_hour + ":59")
                                        activity_minute = "59";
                                    }
                                    setInitialContentDescription(findViewById<EditText>(R.id.activity_time_input), s.toString())
                                }else{
                                    findViewById<TextView>(R.id.edit_activity_time_error_message).visibility = View.VISIBLE
                                }
                                checkCanAddActivity(save_button,"edit")
                            }
                        }
                        override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {}
                    })

                    findViewById<EditText>(R.id.activity_steps_input).setText(activity.steps.toString())
                    findViewById<EditText>(R.id.activity_steps_input).addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable) {}
                        override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                            checkCanAddActivity(save_button,"edit")
                        }
                    })

                    val pickerDialogContainer = findViewById<ScrollView>(R.id.picker_modal_container)
                    val datePickerModal = findViewById<DatePicker>(R.id.add_manual_activity_date_picker)
                    val timePickerModal = findViewById<TimePicker>(R.id.add_manual_activity_time_picker)

                    val input = activity.date
                    val out = input.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                    activity_day = out[1]
                    activity_month = out[0]
                    activity_year = 2000 + out[2].toInt()
                    datePickerModal.updateDate(out[2].toInt(), out[0].toInt(), out[1].toInt())
                    val hour_int = floor((activity.minutes/60).toDouble()).toInt()
                    val minute_int = (activity.minutes % 60)

                    activity_hour = twoDigitString(hour_int)
                    activity_minute = twoDigitString(minute_int)

                    val time_string = activity_hour + ":" + activity_minute
                    val close_button = alert.findViewById<ImageView>(R.id.edit_manual_activity_alert_close_button);
                    close_button.setOnClickListener {
                        sendGoogleAnalytics("close_edit_manual_activity_modal","track_activity")
                        function()
                        hideAlert()
                    }
                    focusableView = close_button

                    findViewById<View>(R.id.edit_manual_activity_save_button).setOnClickListener{
                        val s = findViewById<EditText>(R.id.activity_time_input).text.toString();
                        activity_hour = s.substring(0, 2)
                        activity_minute = s.substring(3, 5)

                        hideKeyboard(this)
                        sendGoogleAnalytics("save_edit_manual_activity_modal","track_activity")
                        if(activity_type == "workout"){
                            val vendor_id = activity.vendor_id
                            var type = activity_internal_name
                            if(type == ""){
                                findViewById<Spinner>(R.id.select_workout_type).selectedItem.toString().lowercase()
                            }

                            if(activity_hour.toInt() > 23){
                                findViewById<EditText>(R.id.activity_time_input).setText("23:59")
                                activity_hour = "23";
                                activity_minute = "59";
                            }else if(activity_minute.toInt() > 59){
                                findViewById<EditText>(R.id.activity_time_input).setText(activity_hour + ":59")
                                activity_minute = "59";
                            }

                            val distance = findViewById<EditText>(R.id.activity_distance_input).text.toString()
                            var start_time = activity_year.toString() + "-" + activity_month + "-"+ activity_day + "T00:00:00.000-0500"
                            var end_time = activity_year.toString() + "-" + activity_month + "-"+ activity_day + "T" + activity_hour + ":" + activity_minute + ":00.000-0500"

                            if(checkCanAddActivity(save_button,"edit")){
                                val client = OkHttpClient()
                                val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/addWorkout")
                                val formBody = FormBody.Builder()
                                    .add("cons_id", getConsID())
                                    .add("vendor_id", vendor_id)
                                    .add("event_id", getEvent().event_id)
                                    .add("vendor", "MANUAL")
                                    .add("start_time", start_time)
                                    .add("end_time", end_time)
                                    .add("vendor_activity_type", type)
                                    .add("steps", "")
                                    .add("distance", distance)

                                if(getStringVariable("ACTIVITY_TRACKING_DISTANCE_METRIC") != ""){
                                    formBody.add("metric", getStringVariable("ACTIVITY_TRACKING_DISTANCE_METRIC"))
                                }

                                val request = Request.Builder()
                                    .url(url)
                                    .post(formBody.build())
                                    .header("Authorization", "Bearer " + getAuth())
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .header("Program-Id" , getStringVariable("PROGRAM_ID"))
                                    .build()

                                client.newCall(request).enqueue(object : Callback {
                                    override fun onResponse(call: Call, response: Response) {
                                        if(response.code != 200){
                                            throw Exception(response.body?.string())
                                        }else{
                                            val response = response.body?.string();
                                            val obj = JSONObject(response);
                                            hideAlert()
                                            runOnUiThread {
                                                childviewCallback("sync","")
                                            }
                                        }
                                    }

                                    override fun onFailure(call: Call, e: IOException) {

                                    }
                                })
                            }
                        }else{
                            val steps = findViewById<EditText>(R.id.activity_steps_input).text.toString()
                            if(checkCanAddActivity(save_button, "edit")){
                                val client = OkHttpClient()
                                val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/addSteps")
                                val formBody = FormBody.Builder()
                                    .add("event_id",getEvent().event_id)
                                    .add("cons_id", getConsID())
                                    .add("vendor", "MANUAL")
                                    .add("date", activity_year.toString() + "-" + activity_month + "-"+ activity_day)
                                    .add("steps", steps)
                                    .add("replace_date_steps","true")
                                    .build()

                                val request = Request.Builder()
                                    .url(url)
                                    .post(formBody)
                                    .header("Authorization", "Bearer " + getAuth())
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .header("Program-Id" , getStringVariable("PROGRAM_ID"))
                                    .build()

                                client.newCall(request).enqueue(object : Callback {
                                    override fun onResponse(call: Call, response: Response) {
                                        if(response.code != 200){
                                            throw Exception(response.body?.string())
                                        }else{
                                            val response = response.body?.string();
                                            val obj = JSONObject(response);
                                            println("UPDATED STEPS SUCCESFULLY")

                                            runOnUiThread {
                                                childviewCallback("sync","")
                                            }
                                            hideAlert()
                                        }
                                    }

                                    override fun onFailure(call: Call, e: IOException) {

                                    }
                                })
                            }
                        }
                    }

                    val editText = findViewById<View>(R.id.activity_date_input) as EditText
                    editText.setOnClickListener{
                        hideKeyboard(this)
                        currentPicker = R.id.add_manual_activity_date_picker
                        datePickerModal.setVisibility(View.VISIBLE)
                        pickerDialogContainer.setVisibility(View.VISIBLE);
                        val datePicker = findViewById<DatePicker>(R.id.add_manual_activity_date_picker)
                        val today = Calendar.getInstance()

                        datePicker.init(today.get(Calendar.YEAR), today.get(Calendar.MONTH),
                            today.get(Calendar.DAY_OF_MONTH)
                        ) { view, year, month, day ->

                        }

                        val cancel_button = findViewById<TextView>(R.id.date_picker_cancel_button);
                        val select_button = findViewById<TextView>(R.id.date_picker_select_button);

                        val cancel_name = cancel_button.text
                        val cancel_content = SpannableString(cancel_name)
                        cancel_content.setSpan(UnderlineSpan(), 0, cancel_content.length, 0)
                        cancel_button.setText(cancel_content);

                        val select_name = select_button.text
                        val select_content = SpannableString(select_name)
                        select_content.setSpan(UnderlineSpan(), 0, select_content.length, 0)
                        select_button.setText(select_content);
                    }

                    val timeEditText = findViewById<View>(R.id.activity_time_input) as EditText
                    timeEditText.setOnClickListener{
                        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
                        val isTalkBackOn = accessibilityManager.isEnabled && accessibilityManager.isTouchExplorationEnabled
                        
                        if(!isTalkBackOn){
                            timeEditText.isFocusableInTouchMode = true
                            hideKeyboard(this)
                            currentPicker = R.id.add_manual_activity_time_picker
                            timePickerModal.hour = 0;
                            timePickerModal.minute = 0;
                            findViewById<LinearLayout>(R.id.time_picker_overlay).visibility = View.VISIBLE
                            timePickerModal.setIs24HourView(true)
                            timePickerModal.setVisibility(View.VISIBLE)
                            pickerDialogContainer.setVisibility(View.VISIBLE);

                            val cancel_button = findViewById<TextView>(R.id.date_picker_cancel_button);
                            val select_button = findViewById<TextView>(R.id.date_picker_select_button);

                            val cancel_name = cancel_button.text
                            val cancel_content = SpannableString(cancel_name)
                            cancel_content.setSpan(UnderlineSpan(), 0, cancel_content.length, 0)
                            cancel_button.setText(cancel_content);

                            val select_name = select_button.text
                            val select_content = SpannableString(select_name)
                            select_content.setSpan(UnderlineSpan(), 0, select_content.length, 0)
                            select_button.setText(select_content);
                        } else {
                            timeEditText.isFocusableInTouchMode = true
                            timeEditText.setFocusable(true);
                            timeEditText.setClickable(false);
                            if(timeEditText.text.toString() == "00:00"){
                                timeEditText.setText("")
                            }

                            timeEditText.postDelayed({
                                timeEditText.requestFocus()
                                val imm = this@BaseLanguageActivity.getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                                imm?.showSoftInput(timeEditText, InputMethodManager.SHOW_IMPLICIT)
                                timeEditText.requestFocus()
                            }, 100)
                        }
                    }

                    timePickerModal.setHour(hour_int)
                    timePickerModal.setMinute(minute_int)

                    editText.setText(activity.date)
                    timeEditText.setText(time_string)

                    setInitialContentDescription(timeEditText, timeEditText.text.toString())
                    timeEditText.accessibilityDelegate = TimeInputAccessibilityDelegate()

                    findViewById<Button>(R.id.date_picker_select_button).setOnClickListener{
                        if(currentPicker == R.id.add_manual_activity_date_picker){
                            activity_day = twoDigitString(datePickerModal.dayOfMonth)
                            activity_month = twoDigitString(datePickerModal.month + 1)
                            activity_year = datePickerModal.year
                            checkCanAddActivity(save_button,"edit")
                            editText.setText(activity_month + "/" + activity_day + "/" + activity_year)
                        }else if (currentPicker == R.id.add_manual_activity_time_picker){
                            activity_hour = twoDigitString(timePickerModal.hour)
                            activity_minute = twoDigitString(timePickerModal.minute)
                            checkCanAddActivity(save_button,"edit")
                            timeEditText.setText(activity_hour + ":" + activity_minute + " ")

                            val hourInt = activity_hour.toInt()
                            val minuteInt = activity_minute.toInt()
                            val contentDescription = "$hourInt hour${if (hourInt != 1) "s" else ""} $minuteInt minute${if (minuteInt != 1) "s" else ""}"

                            timeEditText.contentDescription = contentDescription
                            timeEditText.accessibilityDelegate = TimeInputAccessibilityDelegate()
                        }
                        hideDateTimePicker()
                    }
                    findViewById<Button>(R.id.date_picker_cancel_button).setOnClickListener{
                        hideDateTimePicker()
                    }
                }
                else if (message == "deleteManualActivity"){
                    val binding: StandardCloseAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.standard_close_alert, alertsContainer, true)
                    binding.colorList = getColorList("")

                    val activity = alertData as TrackActivity.Activity

                    modal_vo_descriptor = R.id.standard_close_alert_heading
                    modal_vo_delay = 350

                    val message_textview = findViewById<TextView>(R.id.standard_alert_message);
                    val close_button = findViewById<ImageView>(R.id.standard_alert_cancel_button);
                    val action_button = findViewById<TextView>(R.id.standard_alert_action_button);
                    message_textview.setText(getResources().getString(R.string.mobile_track_activity_activity_card_delete_activity_prompt))
                    action_button.setText(getResources().getString(R.string.mobile_track_activity_activity_card_delete_activity_ok))
                    action_button.setOnClickListener {
                        var url = "";
                        if(activity.activity_type == "steps"){
                            url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/deleteSteps").plus("/").plus(getConsID()).plus("/").plus(getEvent().event_id).plus("/").plus(activity.vendor).plus("/").plus(activity.date.replace("/","-"))
                        }else{
                            url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/deleteWorkout").plus("/").plus(getConsID()).plus("/").plus(getEvent().event_id).plus("/").plus(activity.vendor).plus("/").plus(activity.vendor_id)
                        }

                        var client = OkHttpClient();
                        var request = Request.Builder()
                            .url(url)
                            .delete()
                            .addHeader("Authorization", "Bearer ".plus(getAuth()))
                            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                            .build()

                        client.newCall(request).enqueue(object : Callback {
                            override fun onResponse(call: Call, response: Response) {
                                println("RESPONSE: ")
                                println(response)
                                if(response.code != 200){
                                    //throw Exception(response.body?.string())
                                }else{
                                    val response = response.body?.string();
                                    val json = JSONObject(response);
                                    hideAlert()
                                    childviewCallback("sync","")
                                }
                            }

                            override fun onFailure(call: Call, e: IOException) {
                                println("ERROR DELETING FLOWER")
                                println(e.message.toString())
                                hideAlert()
                            }
                        })
                    }
                    focusableView = close_button

                    close_button.setOnClickListener{
                        function()
                        hideAlert()
                    }
                }
                else if (message == "viewAllFinnsMission"){
                    val binding: FinnsMissionViewAllAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.finns_mission_view_all_alert, alertsContainer, true)
                    binding.colorList = getColorList("")

                    childviewCallback("finnsMissionModal","")

                    val close_button = findViewById<ImageView>(R.id.finns_mission_close_button);
                    close_button.setOnClickListener{
                        function()
                        hideAlert()
                    }
                    focusableView = close_button
                    modal_vo_descriptor = R.id.finns_mission_alert_heading
                }
                else if (message == "viewAllTopClassrooms"){
                    val binding: TopClassroomsViewAllAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.top_classrooms_view_all_alert, alertsContainer, true)
                    binding.colorList = getColorList("")

                    childviewCallback("topClassroomsModal","")

                    val close_button = findViewById<ImageView>(R.id.top_classroom_close_button);
                    close_button.setOnClickListener{
                        function()
                        hideAlert()
                    }
                    focusableView = close_button
                    modal_vo_descriptor = R.id.top_classrooms_alert_heading
                }
                else if (message == "earnPointsViewAll"){
                    val binding: EarnPointsActivityPointsAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.earn_points_activity_points_alert, alertsContainer, true)
                    binding.colorList = getColorList("")

                    childviewCallback("default", "")

                    findViewById<LinearLayout>(R.id.earn_points_activity_modal_activity_sort_link).setOnClickListener{
                        childviewCallback("activity", "")
                    }

                    findViewById<LinearLayout>(R.id.earn_points_activity_modal_activity_points_link).setOnClickListener{
                        childviewCallback("points", "")
                    }

                    findViewById<ImageView>(R.id.earn_points_activity_points_close_button).setOnClickListener{
                        sendGoogleAnalytics("hide_view_all_modal","earn_points")
                        function()
                        hideAlert()
                    }
                    focusableView = findViewById<ImageView>(R.id.earn_points_activity_points_close_button)
                }
                else if (message == "registerNow"){
                    val binding: RegisterNowAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.register_now_alert, alertsContainer, true)
                    binding.colorList = ColorList(getStringVariable("PRIMARY_COLOR"), false,getStringVariable("LOGIN_BUTTON_COLOR"),getStringVariable("LOGIN_BUTTON_TEXT_COLOR"))

                    val dropdown: Spinner = findViewById(R.id.register_now_select_state)
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, states)
                    dropdown.setAdapter(adapter)

                    val close_button = findViewById<ImageView>(R.id.register_alert_close_button)
                    val action_button = findViewById<TextView>(R.id.register_alert_search_button)
                    val event_links_container = findViewById<LinearLayout>(R.id.walks_links_section)
                    val event_container = findViewById<LinearLayout>(R.id.walks_section)

                    event_container.setVisibility(View.GONE)
                    findViewById<TextView>(R.id.no_walks_found).setVisibility(View.GONE)

                    val type = alertData as String;
                    if(type == ""){
                        findViewById<TextView>(R.id.walks_section_description).setVisibility(View.GONE)
                    }

                    action_button.setOnClickListener {
                        sendGoogleAnalytics("search","register_now_modal")
                        removeAllChildren(event_links_container)
                        event_container.setVisibility(View.GONE)
                        findViewById<TextView>(R.id.no_walks_found).setVisibility(View.GONE)
                        val selected_state_index = dropdown.selectedItemPosition
                        if(selected_state_index != 0){
                            val state_code = state_abbreviations.get(selected_state_index)
                            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/state/").plus(state_code)
                            println("URL: " + url)
                            var client = OkHttpClient();
                            var request = Request.Builder()
                                .url(url)
                                .addHeader("Authorization", "Bearer ".plus(getAuth()))
                                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                                .build()

                            client.newCall(request).enqueue(object : Callback {
                                @RequiresApi(Build.VERSION_CODES.O)
                                override fun onResponse(call: Call, response: Response) {
                                    val jsonString = response.body?.string();
                                    println("SEARCHING EVENTS RESPONSE")
                                    println(jsonString)
                                    val jsonArray = JSONArray(jsonString)
                                    println(jsonArray)
                                    if(jsonArray.length() > 0) {
                                        println("LENGTH IS GREATER THAN 0")
                                        for (i in 0 until jsonArray.length()) {
                                            val event = jsonArray.getJSONObject(i);
                                            runOnUiThread {
                                                val binding: RegisterNowEventRowBinding =
                                                    DataBindingUtil.inflate(
                                                        inflater,
                                                        R.layout.register_now_event_row,
                                                        event_links_container,
                                                        true
                                                    )
                                                binding.colorList = getColorList("")

                                                val timeFormatter = DateTimeFormatter.ISO_DATE_TIME

                                                val offsetDateTime: OffsetDateTime =
                                                    OffsetDateTime.parse(event.get("event_date") as String,
                                                        timeFormatter
                                                    )

                                                val date = Date.from(Instant.from(offsetDateTime))
                                                val row = binding.root as LinearLayout

                                                val cal = Calendar.getInstance()
                                                cal.time = date
                                                val year = cal[Calendar.YEAR]
                                                var city = ""
                                                var state = ""

                                                if(event.has("city") && event.get("city") is String){
                                                    city = event.get("city") as String
                                                }

                                                if(event.has("state") && event.get("state") is String){
                                                    state = event.get("state") as String
                                                }

                                                if(city != "" && state != ""){
                                                    city = city + ", "
                                                }

                                                val string = city + state + " | " + getResources().getString(month_abbreviations[date.month]) + " " + date.date + ", " + year
                                                ((row.getChildAt(0) as LinearLayout).getChildAt(0) as TextView).setText(string)
                                                row.setOnClickListener {
                                                    runOnUiThread {
                                                        var register_url = getStringVariable("REGISTER_NOW_URL");
                                                        if(register_url != ""){
                                                            register_url = register_url.replace("<EVENT_ID>",(event.get("event_id") as Int).toString())
                                                            val webView = findViewById<WebView>(R.id.register_now_webview)
                                                            webView.setVisibility(View.VISIBLE)
                                                            webView.settings.setJavaScriptEnabled(true)
                                                            webView.webViewClient = object : WebViewClient() {
                                                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                                                    if (url != null) {
                                                                        view?.loadUrl(url)
                                                                    }
                                                                    return true
                                                                }

                                                                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                                                    if (url != null) {
                                                                        if(url.contains("reg=completed")){
                                                                            webView.setVisibility(View.GONE)
                                                                            hideAlert()
                                                                            if (type == "no_event"){
                                                                                loadEvents("register_now")
                                                                            }
                                                                        }
                                                                    }
                                                                    super.doUpdateVisitedHistory(view, url, isReload)
                                                                }
                                                            }
                                                            webView.loadUrl(register_url)
                                                        }
                                                    }
                                                    hideAlert()
                                                }
                                            }
                                        }
                                        runOnUiThread {
                                            event_container.setVisibility(View.VISIBLE)
                                        }
                                    }else{
                                        runOnUiThread {
                                            findViewById<TextView>(R.id.no_walks_found).setVisibility(View.VISIBLE)
                                        }
                                    }
                                }

                                override fun onFailure(call: Call, e: IOException) {
                                    println("ERROR GETTING WALKS")
                                    println(e.message.toString())
                                }
                            })
                        }
                    }

                    close_button.setVisibility(View.VISIBLE)
                    close_button.setOnClickListener{
                        sendGoogleAnalytics("close","register_now_modal")
                        function()
                        hideAlert()
                    }
                    focusableView = close_button
                }
                else if (message == "registerNowRecommendedEvent"){
                    val binding: RegisterNowRecommendedEventAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.register_now_recommended_event_alert, alertsContainer, true)
                    binding.colorList = ColorList(getStringVariable("PRIMARY_COLOR"), false,getStringVariable("LOGIN_BUTTON_COLOR"),getStringVariable("LOGIN_BUTTON_TEXT_COLOR"))

                    val dropdown: Spinner = findViewById(R.id.register_now_select_state)
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, states)
                    dropdown.setAdapter(adapter)

                    val close_button = findViewById<ImageView>(R.id.register_alert_close_button)
                    val search_button = findViewById<TextView>(R.id.register_alert_search_button)
                    val register_button = findViewById<TextView>(R.id.register_alert_register_button)
                    val event_links_container = findViewById<LinearLayout>(R.id.walks_links_section)
                    val event_container = findViewById<LinearLayout>(R.id.walks_section)
                    event_container.setVisibility(View.GONE)
                    findViewById<TextView>(R.id.no_walks_found).setVisibility(View.GONE)

                    val event_info = alertData as Array<String>
                    findViewById<TextView>(R.id.recommended_event_name).setText(event_info[1])

                    register_button.setOnClickListener{
                        sendGoogleAnalytics("register","register_now_modal")
                        runOnUiThread {
                            var register_url = getStringVariable("REGISTER_NOW_URL");
                            if(register_url != ""){
                                register_url = register_url.replace("<EVENT_ID>",event_info[0])
                                val webView = findViewById<WebView>(R.id.register_now_webview)
                                webView.setVisibility(View.VISIBLE)
                                webView.settings.setJavaScriptEnabled(true)
                                webView.webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                        if (url != null) {
                                            view?.loadUrl(url)
                                        }
                                        return true
                                    }

                                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                        if (url != null) {
                                            if(url.contains("reg=completed")){
                                                webView.setVisibility(View.GONE)
                                                hideAlert()
                                                loadEvents("register_now")
                                            }
                                        }
                                        super.doUpdateVisitedHistory(view, url, isReload)
                                    }
                                }
                                webView.loadUrl(register_url)
                            }
                        }
                        hideAlert()
                    }

                    search_button.setOnClickListener {
                        sendGoogleAnalytics("search","register_now_modal")
                        removeAllChildren(event_links_container)
                        event_container.setVisibility(View.GONE)
                        findViewById<TextView>(R.id.no_walks_found).setVisibility(View.GONE)
                        val selected_state_index = dropdown.selectedItemPosition
                        if(selected_state_index != 0){
                            val state_code = state_abbreviations.get(selected_state_index)
                            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/state/").plus(state_code)
                            println("URL: " + url)
                            var client = OkHttpClient();
                            var request = Request.Builder()
                                .url(url)
                                .addHeader("Authorization", "Bearer ".plus(getAuth()))
                                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                                .build()

                            client.newCall(request).enqueue(object : Callback {
                                @RequiresApi(Build.VERSION_CODES.O)
                                override fun onResponse(call: Call, response: Response) {
                                    val jsonString = response.body?.string();
                                    val jsonArray = JSONArray(jsonString)
                                    if(jsonArray.length() > 0) {
                                        for (i in 0 until jsonArray.length()) {
                                            val event = jsonArray.getJSONObject(i);
                                            runOnUiThread {
                                                val binding: RegisterNowEventRowBinding =
                                                    DataBindingUtil.inflate(
                                                        inflater,
                                                        R.layout.register_now_event_row,
                                                        event_links_container,
                                                        true
                                                    )
                                                binding.colorList = getColorList("")

                                                val timeFormatter = DateTimeFormatter.ISO_DATE_TIME

                                                val offsetDateTime: OffsetDateTime =
                                                    OffsetDateTime.parse(event.get("event_date") as String,
                                                        timeFormatter
                                                    )

                                                val date = Date.from(Instant.from(offsetDateTime))
                                                val cal = Calendar.getInstance()
                                                cal.time = date
                                                val year = cal[Calendar.YEAR]

                                                val row = binding.root as LinearLayout
                                                val string = event.get("city") as String + ", " + event.get("state") as String + " | " + getResources().getString(month_abbreviations[date.month]) + " " + date.day + ", " + year
                                                ((row.getChildAt(0) as LinearLayout).getChildAt(0) as TextView).setText(string)
                                                row.setOnClickListener {
                                                    runOnUiThread {
                                                        var register_url = getStringVariable("REGISTER_NOW_URL");
                                                        if(register_url != ""){
                                                            register_url = register_url.replace("<EVENT_ID>",(event.get("event_id") as Int).toString())
                                                            val webView = findViewById<WebView>(R.id.register_now_webview)
                                                            webView.setVisibility(View.VISIBLE)
                                                            webView.settings.setJavaScriptEnabled(true)
                                                            webView.webViewClient = object : WebViewClient() {
                                                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                                                    if (url != null) {
                                                                        view?.loadUrl(url)
                                                                    }
                                                                    return true
                                                                }

                                                                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                                                    println("NEW URL: " + url)
                                                                    if (url != null) {
                                                                        if(url.contains("reg=completed")){
                                                                            webView.setVisibility(View.GONE)
                                                                            hideAlert()
                                                                            loadEvents("register_now")
                                                                        }
                                                                    }
                                                                    super.doUpdateVisitedHistory(view, url, isReload)
                                                                }
                                                            }
                                                            webView.loadUrl(register_url)
                                                        }
                                                    }
                                                    hideAlert()
                                                }
                                            }
                                        }
                                        runOnUiThread {
                                            event_container.setVisibility(View.VISIBLE)
                                        }
                                    }else{
                                        runOnUiThread {
                                            findViewById<TextView>(R.id.no_walks_found).setVisibility(View.VISIBLE)
                                        }
                                    }
                                }

                                override fun onFailure(call: Call, e: IOException) {
                                    println("ERROR GETTING WALKS")
                                    println(e.message.toString())
                                }
                            })
                        }
                    }

                    close_button.setVisibility(View.VISIBLE)
                    close_button.setOnClickListener{
                        function()
                        hideAlert()
                    }
                    focusableView = close_button
                }
                else if (message == "checkinTshirt"){
                    val binding: CheckinTshirtAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.checkin_tshirt_alert, alertsContainer, true)

                    binding.colorList = getColorList("")

                    val alert_image = findViewById<ImageView>(R.id.checkin_tshirt_alert_image);
                    val close_button = findViewById<ImageView>(R.id.checkin_tshirt_cancel_button);
                    focusableView = close_button;

                    close_button.setOnClickListener {
                        hideAlert()
                    }

                    alertsContainer.setVisibility(View.VISIBLE)
                    hideAlertScrollView(false)
                    focusableView.requestFocus();
                    Handler().postDelayed({
                        findViewById<View>(R.id.standard_image_alert_heading).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                    }, 175)

                    val image = getStringVariable("EVENT_CHECKIN_TSHIRT_PROMPT_IMAGE")

                    if(image != ""){
                        Glide.with(this@BaseLanguageActivity)
                            .load(image)
                            .into(alert_image)
                    }
                }
                else{
                    modal_vo_descriptor = R.id.alert_background_layout
                    val binding: StandardAlertBinding = DataBindingUtil.inflate(
                        inflater, R.layout.standard_alert, alertsContainer, true)

                    if(alertData == "login"){
                        binding.colorList = getColorList("login")
                    }else{
                        binding.colorList = getColorList("")
                    }

                    val message_textview = findViewById<TextView>(R.id.standard_alert_message);
                    val close_button = findViewById<TextView>(R.id.standard_alert_close_button);
                    focusableView = close_button;
                    message_textview.setText(message)
                    close_button.setOnClickListener {
                        function()
                    }
                }
            }
            alertsContainer.setVisibility(View.VISIBLE)
            hideAlertScrollView(false)

            if(modal_vo_descriptor != 0){
                Handler().postDelayed({
                    focusableView.requestFocus();
                },50)
                Handler().postDelayed({
                    findViewById<View>(modal_vo_descriptor).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                }, modal_vo_delay.toLong())
            }else{
                focusableView.requestFocus();
            }
        })
    }

      fun checkDateValidity(activityDate: LocalDate, connectedDate: LocalDate, today: LocalDate): Pair<Boolean, Boolean> {
        val isDateValid = !activityDate.isBefore(connectedDate)
        val isFutureDate = activityDate.isAfter(today)
        return Pair(isDateValid, isFutureDate)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun checkCanAddActivity(button: Button, source: String): Boolean{
        val connected_month = getStringVariable("PLATFORM_CONNECTED_MONTH")
        val connected_day = getStringVariable("PLATFORM_CONNECTED_DAY")
        val connected_year = getStringVariable("PLATFORM_CONNECTED_YEAR")

        if(getStringVariable("PLATFORM_CONNECTED") == "true") {
            if (connected_month.isNotBlank() && connected_day.isNotBlank() && connected_year.isNotBlank()) {
                val connectedDate = LocalDate.of(
                    connected_year.toInt(),
                    connected_month.toInt(),
                    connected_day.toInt()
                )
                val today = LocalDate.now()
                if (activity_year.toString()
                        .isNotBlank() && activity_month.isNotBlank() && activity_day.isNotBlank()
                ) {
                    val activityDate = LocalDate.of(
                        activity_year.toInt(),
                        activity_month.toInt(),
                        activity_day.toInt()
                    )
                    val (dateValid, futureDate) = checkDateValidity(
                        activityDate,
                        connectedDate,
                        today
                    )

                    if (!dateValid) {
                        findViewById<TextView>(R.id.add_activity_date_error_message).setVisibility(
                            View.VISIBLE
                        );
                        findViewById<TextView>(R.id.add_activity_date_error_message).setText(
                            getString(R.string.mobile_track_activity_add_activity_modal_date_invalid_android).replace(
                                "XXXXXXX",
                                connected_month + "/" + connected_day + "/" + connected_year
                            )
                        )
                        setButtonEnabledStatus(button, false)
                        return false
                    } else {
                        findViewById<TextView>(R.id.add_activity_date_error_message).setVisibility(
                            View.GONE
                        );
                    }
                    if (futureDate) {
                        findViewById<TextView>(R.id.add_activity_date_error_message).setVisibility(
                            View.VISIBLE
                        );
                        findViewById<TextView>(R.id.add_activity_date_error_message).setText(
                            getString(R.string.mobile_track_activity_add_activity_modal_future_date_invalid_android)
                        )
                        setButtonEnabledStatus(button, false)
                        return false
                    }
                } else {
                    setButtonEnabledStatus(button, false)
                    return false
                }
            } else {
                setButtonEnabledStatus(button, false)
                return false
            }
        }else if(getStringVariable("TRACKING_STARTED") == "true"){
            val started_month = getStringVariable("TRACKING_STARTED_MONTH")
            val started_day = getStringVariable("TRACKING_STARTED_DAY")
            val started_year = getStringVariable("TRACKING_STARTED_YEAR")

            if (started_month.isNotBlank() && started_day.isNotBlank() && started_year.isNotBlank()) {
                val connectedDate = LocalDate.of(
                    started_year.toInt(),
                    started_month.toInt(),
                    started_day.toInt()
                )
                val today = LocalDate.now()
                if (activity_year.toString()
                        .isNotBlank() && activity_month.isNotBlank() && activity_day.isNotBlank()
                ) {
                    val activityDate = LocalDate.of(
                        activity_year,
                        activity_month.toInt(),
                        activity_day.toInt()
                    )
                    val (dateValid, futureDate) = checkDateValidity(
                        activityDate,
                        connectedDate,
                        today
                    )

                    if (!dateValid) {
                        findViewById<TextView>(R.id.add_activity_date_error_message).setVisibility(
                            View.VISIBLE
                        );
                        findViewById<TextView>(R.id.add_activity_date_error_message).setText(
                            getString(R.string.mobile_track_activity_add_activity_modal_date_invalid_android).replace(
                                "XXXXXXX",
                                started_month + "/" + started_day + "/" + started_year
                            )
                        )
                        setButtonEnabledStatus(button, false)
                        return false
                    } else {
                        findViewById<TextView>(R.id.add_activity_date_error_message).setVisibility(
                            View.GONE
                        );
                    }
                    if (futureDate) {
                        findViewById<TextView>(R.id.add_activity_date_error_message).setVisibility(
                            View.VISIBLE
                        );
                        findViewById<TextView>(R.id.add_activity_date_error_message).setText(
                            getString(R.string.mobile_track_activity_add_activity_modal_future_date_invalid_android)
                        )
                        button.setAlpha(.5F)
                        if (button.id == R.id.add_manual_activity_save_button) {
                            button.contentDescription =
                            getString(R.string.mobile_track_activity_add_activity_modal_button_content_description) + ", disabled";
                        }
                        setButtonEnabledStatus(button, false)
                        return false
                    }
                } else {
                    setButtonEnabledStatus(button, false)
                    return false
                }
            } else {
                setButtonEnabledStatus(button, false)
                return false
            }
        }

        var return_value = true
        if (activity_type == "steps") {
            val steps = findViewById<EditText>(R.id.activity_steps_input).text.toString()
            if (steps == "" || steps == "0") {
                return_value = false
            } else {
                return_value = true
            }
        } else {
            val distanceEditText = findViewById<EditText>(R.id.activity_distance_input)
            val distance = distanceEditText.text.toString().ifBlank { "0" }
            if (activity_modal_distance_enabled && (distance == "" || distance == "0")) {
                return_value = false
            } else if ((activity_hour == "" || activity_hour == "00" || activity_hour == "0") && (activity_minute == "" || activity_minute == "00" || activity_minute == "0")) {
                return_value = false
            } else if (!checkTimeFormat(findViewById<EditText>(R.id.activity_time_input).text.toString().trim())) {
                return_value = false
            } else {
                return_value = true
            }

            if(findViewById<Spinner>(R.id.select_workout_type).selectedItemPosition == 0){
                return_value = false
            }
        }

        if(return_value){
            button.setAlpha(1F)
            if (button.id == R.id.add_manual_activity_save_button) {
                button.contentDescription =
                    getString(R.string.mobile_track_activity_add_activity_modal_button_content_description) + ", enabled";
            }
        }else{
            button.setAlpha(.5F)
            if (button.id == R.id.add_manual_activity_save_button) {
                button.contentDescription =
                    getString(R.string.mobile_track_activity_add_activity_modal_button_content_description) + ", disabled";
            }
        }
        return return_value
    }

    fun setCurrencyVoiceover(input: EditText){
        var voiceover = "";
        var value = input.text.toString();
        val dotindex = value.indexOf('.');

        if(dotindex != -1){
            val dollars = value.substring(0,dotindex);
            val cents = value.substring(dotindex + 1, value.length);
            voiceover = dollars + " dollars and " + cents + " cents";
        }else{
            voiceover = value + " dollars";
        }

        input.contentDescription = voiceover;
        input.accessibilityDelegate = TimeInputAccessibilityDelegate()
    }

    fun setButtonEnabledStatus(button: Button, enabled: Boolean) {
        if (enabled) {
            button.setAlpha(1F)
            button.contentDescription =
                getString(R.string.mobile_track_activity_add_activity_modal_button_content_description) + ", enabled";
        } else {
            button.setAlpha(.5F)
            button.contentDescription =
                getString(R.string.mobile_track_activity_add_activity_modal_button_content_description) + ", disabled";
        }
    };

    fun checkTimeFormat(text: String): Boolean {
        val pattern = Regex("(\\d\\d:\\d\\d)");
        return pattern.matches(text);
    }

    fun twoDigitString(int: Int) : String{
        val i = int.toString()
        if(i.length == 1){
            return "0" + i
        }else{
            return i
        }
    }
    
    fun hideDateTimePicker(){
        Locale.setDefault(newStringLocale);
        currentPicker = 0
        findViewById<ScrollView>(R.id.picker_modal_container).setVisibility(View.GONE)
        findViewById<DatePicker>(R.id.add_manual_activity_date_picker).setVisibility(View.GONE)
        findViewById<TimePicker>(R.id.add_manual_activity_time_picker).setVisibility(View.GONE)
        findViewById<LinearLayout>(R.id.time_picker_overlay).setVisibility(View.GONE)
    }

    fun updateAddManualActivityLabel() {
        val myFormat = "MM/dd/yy"
        val dateFormat = SimpleDateFormat(myFormat, Locale.US)
        findViewById<TextView>(R.id.activity_date_input).setText(dateFormat.format(myCalendar.time))
    }

    fun checkFlowerForPlanting(): Boolean{
        val dedication = findViewById<EditText>(R.id.input_flower_dedication).text.toString()
        val message = findViewById<EditText>(R.id.input_flower_message).text.toString()

        if(dedication == "" || message == ""){
            findViewById<Button>(R.id.plant_flower_alert_plant_button).setAlpha(.5F)
            return false
        }else{
            findViewById<Button>(R.id.plant_flower_alert_plant_button).setAlpha(1F)
            return true
        }
    }

    fun checkLuminaryForCreation(): Boolean{
        val dedication = findViewById<EditText>(R.id.input_luminary_dedication).text.toString()
        val message = findViewById<EditText>(R.id.input_luminary_message).text.toString()

        if(dedication == "" || message == ""){
            findViewById<Button>(R.id.create_luminary_alert_create_button).setAlpha(.5F)
            return false
        }else{
            findViewById<Button>(R.id.create_luminary_alert_create_button).setAlpha(1F)
            return true
        }
    }

    fun checkJerseyForCreation(): Boolean{
        val dedication = findViewById<EditText>(R.id.input_jersey_dedication).text.toString()
        val message = findViewById<EditText>(R.id.input_jersey_message).text.toString()

        if(dedication == "" || message == ""){
            findViewById<Button>(R.id.create_jersey_alert_create_button).setAlpha(.5F)
            return false
        }else{
            findViewById<Button>(R.id.create_jersey_alert_create_button).setAlpha(1F)
            return true
        }
    }

    fun deleteFlower(){
        val id = getStringVariable("FLOWER_ID");
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/deleteFlower/").plus(id).plus("/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var client = OkHttpClient();
        var request = Request.Builder()
            .url(url)
            .delete()
            .addHeader("Authorization", "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                hideAlert()
                childviewCallback("flowerDeleted","")
            }

            override fun onFailure(call: Call, e: IOException) {
                println("ERROR DELETING FLOWER")
                println(e.message.toString())
                hideAlert()
            }
        })
    }

    fun plantFlower(){
        if(checkFlowerForPlanting()){
            val dedication = findViewById<EditText>(R.id.input_flower_dedication).text.toString()
            val message = findViewById<EditText>(R.id.input_flower_message).text.toString()

            println("CHECK FLOWERS SUCCESS")
            var url = "";
            if(getStringVariable("FLOWER_MODE") == "create"){
                url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/addFlower/")
            }else {
                url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/updateFlower/")
            }



            var client = OkHttpClient();

            if(getStringVariable("FLOWER_MODE") == "create") {
                val formBody = FormBody.Builder()
                    .add("cons_id", getConsID())
                    .add("event_id", getEvent().event_id)
                    .add("dedicated_to", dedication)
                    .add("message", message)
                    .add("color", getStringVariable("FLOWER_COLOR"))
                    .build()
                var request = Request.Builder()
                    .url(url)
                    .post(formBody)
                    .addHeader("Authorization", "Bearer ".plus(getAuth()))
                    .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        hideAlert()
                        childviewCallback("flowerPlanted","")
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        println("ERROR SAVING FLOWER")

                        println(e.message.toString())
                        hideAlert()
                    }
                })

            }else{
                val formBody = FormBody.Builder()
                    .add("id", getStringVariable("FLOWER_ID"))
                    .add("cons_id", getConsID())
                    .add("event_id", getEvent().event_id)
                    .add("dedicated_to", dedication)
                    .add("message", message)
                    .add("color", getStringVariable("FLOWER_COLOR"))
                    .build()
                var request = Request.Builder()
                    .url(url)
                    .put(formBody)
                    .addHeader("Authorization", "Bearer ".plus(getAuth()))
                    .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        hideAlert()
                        println("PUT RESPONSE")
                        println(response)
                        childviewCallback("flowerPlanted","")
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        println("ERROR SAVING FLOWER")
                        println(e.message.toString())
                        hideAlert()
                    }
                })
            }
        }
    }

    fun deleteLuminary(){
        val id = getStringVariable("LUMINARY_ID");
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/deleteLuminary/").plus(id).plus("/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var client = OkHttpClient();
        var request = Request.Builder()
            .url(url)
            .delete()
            .addHeader("Authorization", "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                hideAlert()
                childviewCallback("luminaryDeleted","")
            }

            override fun onFailure(call: Call, e: IOException) {
                println("ERROR DELETING LUMINARY")
                println(e.message.toString())
                hideAlert()
            }
        })
    }

    fun createLuminary(){
        if(checkLuminaryForCreation()){
            val dedication = findViewById<EditText>(R.id.input_luminary_dedication).text.toString()
            val message = findViewById<EditText>(R.id.input_luminary_message).text.toString()

            var url = "";
            if(getStringVariable("LUMINARY_MODE") == "create"){
                url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/addLuminary/")
            }else {
                url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/updateLuminary/")
            }

            var client = OkHttpClient();

            if(getStringVariable("LUMINARY_MODE") == "create") {
                val formBody = FormBody.Builder()
                    .add("cons_id", getConsID())
                    .add("event_id", getEvent().event_id)
                    .add("dedicated_to", dedication)
                    .add("message", message)
                    .build()
                var request = Request.Builder()
                    .url(url)
                    .post(formBody)
                    .addHeader("Authorization", "Bearer ".plus(getAuth()))
                    .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        hideAlert()
                        println("CREATE RESPONSE: " )
                        println(response)
                        childviewCallback("luminaryCreated","")
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        println("ERROR SAVING LUMINARY")

                        println(e.message.toString())
                        hideAlert()
                    }
                })

            }else{
                val formBody = FormBody.Builder()
                    .add("id", getStringVariable("LUMINARY_ID"))
                    .add("cons_id", getConsID())
                    .add("event_id", getEvent().event_id)
                    .add("dedicated_to", dedication)
                    .add("message", message)
                    .build()
                var request = Request.Builder()
                    .url(url)
                    .put(formBody)
                    .addHeader("Authorization", "Bearer ".plus(getAuth()))
                    .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        hideAlert()
                        childviewCallback("luminaryCreated","")
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        println("ERROR SAVING LUMINARY")
                        println(e.message.toString())
                        hideAlert()
                    }
                })
            }
        }
    }

    fun deleteJersey(){
        val id = getStringVariable("JERSEY_ID");
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/deleteJersey/").plus(id).plus("/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var client = OkHttpClient();
        var request = Request.Builder()
            .url(url)
            .delete()
            .addHeader("Authorization", "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                hideAlert()
                childviewCallback("jerseyDeleted","")
            }

            override fun onFailure(call: Call, e: IOException) {
                println("ERROR DELETING JERSEY")
                println(e.message.toString())
                hideAlert()
            }
        })
    }

    fun createJersey(){
        if(checkJerseyForCreation()){
            val dedication = findViewById<EditText>(R.id.input_jersey_dedication).text.toString()
            val message = findViewById<EditText>(R.id.input_jersey_message).text.toString()

            var url = "";
            if(getStringVariable("JERSEY_MODE") == "create"){
                url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/addJersey/")
            }else {
                url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/updateJersey/")
            }

            var client = OkHttpClient();

            if(getStringVariable("JERSEY_MODE") == "create") {
                val formBody = FormBody.Builder()
                    .add("cons_id", getConsID())
                    .add("event_id", getEvent().event_id)
                    .add("dedicated_to", dedication)
                    .add("message", message)
                    .build()
                var request = Request.Builder()
                    .url(url)
                    .post(formBody)
                    .addHeader("Authorization", "Bearer ".plus(getAuth()))
                    .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        hideAlert()
                        println("CREATE RESPONSE: " )
                        println(response)
                        childviewCallback("jerseyCreated","")
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        println("ERROR SAVING JERSEY")

                        println(e.message.toString())
                        hideAlert()
                    }
                })

            }else{
                val formBody = FormBody.Builder()
                    .add("id", getStringVariable("JERSEY_ID"))
                    .add("cons_id", getConsID())
                    .add("event_id", getEvent().event_id)
                    .add("dedicated_to", dedication)
                    .add("message", message)
                    .build()
                var request = Request.Builder()
                    .url(url)
                    .put(formBody)
                    .addHeader("Authorization", "Bearer ".plus(getAuth()))
                    .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        hideAlert()
                        childviewCallback("jerseyCreated","")
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        println("ERROR SAVING JERSEY")
                        println(e.message.toString())
                        hideAlert()
                    }
                })
            }
        }
    }

    fun loadMatchesSearchRows(matches: List<Match>){
        val inflater = LayoutInflater.from(this@BaseLanguageActivity)
        val container = findViewById<LinearLayout>(R.id.search_results);
        container.setVisibility(View.GONE)
        for (childView in container.children) {
            container.removeView(childView);
        }

        for (i in 0 until matches.count()) {
            val donor = matches.get(i)
            val row = inflater.inflate(R.layout.match_result_row, null) as LinearLayout;
            if(i % 2 == 0){
                row.setBackgroundColor(Color.WHITE)
            }
            row.setOnClickListener {
                selectedDonorToMatch = donor
                displayAlert("findMatchCompany")
                findViewById<Button>(R.id.donor_match_next_btn).visibility = View.VISIBLE
            }

            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            val donorDate = dateFormat.format(donor.date);

            (row.getChildAt(0) as TextView).text = donor.first_name + " " + donor.last_name
            (row.getChildAt(1) as TextView).text = formatDoubleToLocalizedCurrency(donor.amount)
            (row.getChildAt(2) as TextView).text = donorDate
            container.addView(row)
        }
        container.setVisibility(View.VISIBLE)
        findViewById<LinearLayout>(R.id.search_results_container).setVisibility(View.VISIBLE)
        donorSearching = false;

        findViewById<Button>(R.id.donor_match_next_btn).setOnClickListener{
            displayAlert("findMatchCompany")
        }
    }

    fun loadMatchCompanies(button: TextView){
        hideKeyboard(this);
        donorSearching = true;
        button.setAlpha(.5F)
        button.text = getResources().getString(R.string.mobile_donations_check_deposit_member_searching_button)
        val inflater = LayoutInflater.from(this@BaseLanguageActivity)
        val container = findViewById<LinearLayout>(R.id.search_results);
        container.setVisibility(View.GONE)
        for (childView in container.children) {
            container.removeView(childView);
        }

        val company_name = findViewById<EditText>(R.id.input_match_company).text.toString()
        if((company_name.count()) < 1){
            displayAlert(getResources().getString(R.string.mobile_donations_double_donation_company_search_results_missing_field),"",{displayAlert("findMatchCompany")})
            button.setAlpha(1F)
            button.text = getResources().getString(R.string.mobile_donations_check_deposit_team_member_search_button)
            return
        }

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/check/doubledonation/companySearch")
        val formBody = FormBody.Builder()
            .add("company_name",company_name)
            .build()
        var request = Request.Builder().url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .post(formBody)
            .build()

        var client = OkHttpClient();
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    donorSearching = false
                    button.setAlpha(1F)
                    button.text = getResources().getString(R.string.mobile_donations_check_deposit_team_member_search_button)
                }else {
                    try{
                        runOnUiThread {
                            val jsonString = response.body?.string()
                            val obj = JSONObject(jsonString)
                            val data = obj.get("data").toString()
                            val jsonArray = JSONArray(data)

                            if(jsonArray.length() > 0){
                                for (i in 0 until jsonArray.length()) {
                                    val company = jsonArray.getJSONObject(i)
                                    val row = inflater.inflate(R.layout.match_company_result_row, null) as LinearLayout;
                                    if(i % 2 == 0){
                                        row.setBackgroundColor(Color.WHITE)
                                    }

                                    row.setOnClickListener {
                                        selectedCompanyToMatch = company
                                        findViewById<Button>(R.id.donor_match_next_btn).visibility = View.VISIBLE
                                        displayAlert("matchGift")
                                    }

                                    (row.getChildAt(0) as TextView).text = (company.get("company_name") as String)
                                    container.addView(row)
                                }
                                container.setVisibility(View.VISIBLE)
                                findViewById<LinearLayout>(R.id.search_results_container).setVisibility(View.VISIBLE)
                            }else{
                                displayAlert(getResources().getString(R.string.mobile_donations_double_donation_company_search_results_none_android),"",{displayAlert("findMatchCompany",arrayOf(company_name))})
                            }
                        }
                        button.setAlpha(1F)
                        button.text = getResources().getString(R.string.mobile_donations_check_deposit_team_member_search_button)
                        donorSearching = false;
                    }catch(e: Exception){
                        button.setAlpha(1F)
                        button.text = getResources().getString(R.string.mobile_donations_check_deposit_team_member_search_button)
                        donorSearching = false;
                    }
                }
            }

            override fun onFailure(call: Call, e: java.io.IOException) {
                println(e.message.toString())
                donorSearching = false;
            }
        })
    }

    fun loadMembers(button: TextView){
        donorSearching = true;
        button.setAlpha(.5F)
        button.text = getResources().getString(R.string.mobile_donations_check_deposit_member_searching_button)
        findViewById<LinearLayout>(R.id.donor_name_credit_row).setVisibility(View.GONE)
        val inflater = LayoutInflater.from(this@BaseLanguageActivity)
        val container = findViewById<LinearLayout>(R.id.search_results);
        container.setVisibility(View.GONE)
        for (childView in container.children) {
            container.removeView(childView);
        }

        val first_name = findViewById<EditText>(R.id.input_member_first_name).text.toString()
        val last_name = findViewById<EditText>(R.id.input_member_last_name).text.toString()
        val event_id = getEvent().event_id
        val team_id = getStringVariable("TEAM_ID")

        if((first_name.count() + last_name.count()) < 3){
            displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_team_member_search_results_length_error),"",{displayAlert("findMember")})
            button.setAlpha(1F)
            button.text = getResources().getString(R.string.mobile_donations_check_deposit_team_member_search_button)
            return
        }

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/check/getTeamMembers/").plus(event_id).plus("/").plus(team_id).plus("?").plus("first_name=").plus(first_name).plus("&").plus("last_name=").plus(last_name)

        var request = Request.Builder().url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    println("ERROR FINDING MEMBER")
                    donorSearching = false
                    button.setAlpha(1F)
                    button.text = getResources().getString(R.string.mobile_donations_check_deposit_team_member_search_button)
                }else {
                    try{
                        runOnUiThread {
                            val jsonString = response.body?.string()
                            val obj = JSONObject(jsonString)
                            val data = obj.get("data").toString()
                            val jsonArray = JSONArray(data)
                            if(jsonArray.length() > 0){
                                for (i in 0 until jsonArray.length()) {
                                    val member = jsonArray.getJSONObject(i)
                                    val row = inflater.inflate(R.layout.member_result_row, null) as LinearLayout;
                                    if(i % 2 == 0){
                                        row.setBackgroundColor(Color.WHITE)
                                    }
                                    row.setOnClickListener {
                                        sendGoogleAnalytics("check_deposit_find_member_select","check_deposit")
                                        runOnUiThread{
                                            findViewById<LinearLayout>(R.id.check_credit_team_captain_row).setVisibility(View.GONE)
                                            if(getStringVariable("CHECK_DEPOSIT_SPLIT_CHECK_ENABLED") == "true") {
                                                childviewCallback("split_member_data",member.toString())
                                                findViewById<LinearLayout>(R.id.check_credit_member_credit_row).setVisibility(View.GONE)
                                            }else{
                                                findViewById<LinearLayout>(R.id.check_credit_member_credit_row).setVisibility(View.VISIBLE)
                                                findViewById<TextView>(R.id.check_credit_member_name).text = (member.get("first_name") as String) + " " + (member.get("last_name") as String)
                                                childviewCallback("member_data",member.toString())
                                            }
                                            
                                        }
                                        hideAlert()
                                    }


                                    ((row.getChildAt(0) as LinearLayout).getChildAt(0) as TextView).text = (member.get("first_name") as String)
                                    ((row.getChildAt(0) as LinearLayout).getChildAt(1) as TextView).text = (member.get("last_name") as String)
                                    container.addView(row)
                                }
                                container.setVisibility(View.VISIBLE)
                                findViewById<LinearLayout>(R.id.search_results_container).setVisibility(View.VISIBLE)
                            }else{
                                displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_member_search_results_none),"",{displayAlert("findMember",arrayOf(first_name, last_name))})
                            }
                        }
                        button.setAlpha(1F)
                        button.text = getResources().getString(R.string.mobile_donations_check_deposit_team_member_search_button)
                        donorSearching = false;
                    }catch(e: Exception){
                        println("LOAD MEMBERS ERROR")
                        button.setAlpha(1F)
                        button.text = getResources().getString(R.string.mobile_donations_check_deposit_team_member_search_button)
                        donorSearching = false;
                    }
                }
            }

            override fun onFailure(call: Call, e: java.io.IOException) {
                println(e.message.toString())
                donorSearching = false;
            }
        })
    }

    fun loadDonors(button: TextView){
        donorSearching = true;
        button.setAlpha(.5F)
        button.text = getResources().getString(R.string.mobile_donations_check_deposit_donor_searching_button)
        findViewById<LinearLayout>(R.id.donor_name_credit_row).setVisibility(View.GONE)
        val inflater = LayoutInflater.from(this@BaseLanguageActivity)
        val container = findViewById<LinearLayout>(R.id.search_results);
        container.setVisibility(View.GONE)
        for (childView in container.children) {
            container.removeView(childView);
        }

        var url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/check/getDonors")

        if(find_donor_type == "company"){
            url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/check/getCompanyDonors")
        }

        val first_name = findViewById<EditText>(R.id.input_donor_first_name).text.toString()
        val last_name = findViewById<EditText>(R.id.input_donor_last_name).text.toString()
        val company_name = findViewById<EditText>(R.id.input_donor_company_name).text.toString()
        val zip = findViewById<EditText>(R.id.input_donor_zip).text.toString()

        if(
            (find_donor_type == "individual" && (first_name == "" || last_name == ""))
            || (find_donor_type == "company" && (company_name == ""))
            || zip == ""){
            displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_donor_search_results_missing_fields),"",{displayAlert("findDonor")})
            button.setAlpha(1F)
            button.text = getResources().getString(R.string.mobile_donations_check_deposit_donor_search_button)
            donorSearching = false
            return
        }

        var formBodyToBuild = FormBody.Builder()
            .add("first_name", first_name)
            .add("last_name", last_name)
            .add("zip", zip)

        if(find_donor_type == "company"){
            formBodyToBuild = FormBody.Builder()
                .add("company_name", company_name)
                .add("zip", zip)
        }

        if(getStringVariable("CHECK_EVENT_MANAGER_DONOR_BY_EVENT") == "true"){
            formBodyToBuild.add("event_id", getStringVariable("CHECK_CREDIT_SELECTED_EVENT_ID"))
        }

        val formBody = formBodyToBuild.build()

        var request = Request.Builder().url(url)
            .post(formBody)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                   println("ERROR FINDING DONOR")
                   donorSearching = false
                    button.setAlpha(1F)
                    button.text = getResources().getString(R.string.mobile_donations_check_deposit_donor_search_button)
                    displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_donor_search_results_none),"",{displayAlert("findDonor",arrayOf(first_name, last_name, zip))})
                }else{
                    try{
                    runOnUiThread {
                        val jsonString = response.body?.string()
                        val obj = JSONObject(jsonString)
                        val data = obj.get("data").toString()
                        val jsonArray = JSONArray(data)
                        if(jsonArray.length() > 0){
                            for (i in 0 until jsonArray.length()) {
                                val donor = jsonArray.getJSONObject(i)
                                donor.put("donor_type",find_donor_type)
                                val row = inflater.inflate(R.layout.donor_result_row, null) as LinearLayout;
                                if(i % 2 == 0){
                                    row.setBackgroundColor(Color.WHITE)
                                }
                                row.setOnClickListener {
                                    setVariable("CHECK_DONOR",donor.toString())
                                    runOnUiThread{
                                        sendGoogleAnalytics("check_deposit_find_donor_select","check_deposit")
                                        findViewById<LinearLayout>(R.id.find_donor_select_row).setVisibility(View.GONE)
                                        findViewById<LinearLayout>(R.id.donor_name_credit_row).setVisibility(View.VISIBLE)
                                        findViewById<TextView>(R.id.donor_name).text = getSafeStringVariable(donor, "first_name") + " " + getSafeStringVariable(donor, "last_name");
                                        childviewCallback("donor_data",donor.toString())
                                    }
                                    hideAlert()
                                }
                                var address = "";
                                if(donor.get("street2") is String){
                                    address = (donor.get("street1") as String) + " " + (donor.get("street2") as String)
                                }else{
                                    if(donor.get("street1") is String){
                                        address = (donor.get("street1") as String)
                                    }else{
                                        address = ""
                                    }
                                }
                                var cityString = ""
                                if(donor.get("city") is String){
                                    cityString = donor.get("city") as String;
                                }
                                if(donor.get("state") is String){
                                    cityString += donor.get("state") as String + ", ";
                                }
                                if(donor.get("zip") is String){
                                    cityString += donor.get("zip") as String;
                                }


                                val company_name = getSafeStringVariable(donor, "company_name")
                                if(getSafeStringVariable(donor, "donor_type") == "company" && company_name != ""){
                                    (row.getChildAt(0) as TextView).text = company_name
                                }else{
                                    (row.getChildAt(0) as TextView).text = getSafeStringVariable(donor, "first_name") + " " + getSafeStringVariable(donor, "last_name")
                                }


                                (row.getChildAt(1) as TextView).text = address
                                (row.getChildAt(2) as TextView).text = cityString
                                container.addView(row)
                            }
                            container.setVisibility(View.VISIBLE)
                            findViewById<LinearLayout>(R.id.search_results_container).setVisibility(View.VISIBLE)
                        }else{
                            displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_donor_search_results_none),"",{displayAlert("findDonor",arrayOf(first_name, last_name, zip))})
                        }
                    }
                        button.setAlpha(1F)
                        button.text = getResources().getString(R.string.mobile_donations_check_deposit_donor_search_button)
                    donorSearching = false;
                    }catch(e: Exception){
                        button.setAlpha(1F)
                        button.text = getResources().getString(R.string.mobile_donations_check_deposit_donor_search_button)
                        donorSearching = false;
                        displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_donor_search_results_none),"",{displayAlert("findDonor",arrayOf(first_name, last_name, zip))})
                    }
                }   
            }

            override fun onFailure(call: Call, e: java.io.IOException) {
                println(e.message.toString())
                donorSearching = false;
                displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_donor_search_results_none),"",{displayAlert("findDonor",arrayOf(first_name, last_name, zip))})
                //displayAlert(getResources().getString(R.string.mobile_overview_edit_goal_error), hideAlert())
                //hideAlert()
            }
        })
    }

    fun loadParticipants(){
        findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.GONE)
        val inflater = LayoutInflater.from(this@BaseLanguageActivity)
        val container = findViewById<LinearLayout>(R.id.search_results);
        container.setVisibility(View.GONE)
        for (childView in container.children) {
            container.removeView(childView);
        }
        var first_name = findViewById<EditText>(R.id.input_participant_first_name).text.toString().trim()
        var last_name = findViewById<EditText>(R.id.input_participant_last_name).text.toString().trim()
        
        if((first_name.length + last_name.length) < 3){
            displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_part_search_results_fields_error),"",{displayAlert("findParticipant")})
            return
        }

        var event_id = ""
        if(getStringVariable("CHECK_DEPOSIT_EVENT_ID") != ""){
            event_id = getStringVariable("CHECK_DEPOSIT_EVENT_ID")
        }else{
            displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_select_part_error))
        }


        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/check/getParticipants/").plus(event_id).plus("?").plus("first_name=").plus(first_name).plus("&").plus("last_name=").plus(last_name)
        var request = Request.Builder().url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val jsonString = response.body?.string()
                println(jsonString)
                if(response.code != 200){
                    //throw Exception(response.body?.string())
                    val obj = JSONObject(jsonString)
                    if(obj.has("message") && obj.get("message") != ""){
                        displayAlert(obj.get("message") as String,"",{displayAlert("findParticipant")})
                    }
                }else {
                    runOnUiThread {
                        println(jsonString)
                        val obj = JSONObject(jsonString)
                        val data = obj.get("data").toString()
                        val jsonArray = JSONArray(data)
                        if(jsonArray.length() > 0){
                            for (i in 0 until jsonArray.length()) {
                                val participant = jsonArray.getJSONObject(i)
                                val big_row = inflater.inflate(R.layout.participant_result_row, null) as LinearLayout;
                                big_row.setOnClickListener {
                                    setVariable("CHECK_CREDIT_VALUE",participant.toString())
                                    runOnUiThread{
                                        childviewCallback("run_check","")
                                        sendGoogleAnalytics("check_deposit_find_participant_select","check_deposit")
                                        findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(View.GONE)
                                        if(getStringVariable("CHECK_DEPOSIT_SPLIT_CHECK_ENABLED") == "true") {
                                            childviewCallback("split_member_data",participant.toString())
                                        }else{
                                            findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.VISIBLE)
                                            findViewById<TextView>(R.id.donation_check_credit_entity_name).text = (participant.get("first_name") as String) + " " + (participant.get("last_name") as String)
                                        }
                                    }
                                    hideAlert()
                                }

                                val row = big_row.getChildAt(0) as LinearLayout
                                (row.getChildAt(0) as TextView).text = (participant.get("first_name") as String)
                                (row.getChildAt(1) as TextView).text = (participant.get("last_name") as String)
                                if(participant.has("team_name") && participant.get("team_name") is String) {
                                    (row.getChildAt(2) as TextView).text =
                                        (participant.get("team_name") as String)
                                }
                                container.addView(big_row)
                            }
                            container.setVisibility(View.VISIBLE)
                            findViewById<LinearLayout>(R.id.search_results_container).setVisibility(View.VISIBLE)
                        }else{
                            displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_part_search_results_none),"",{displayAlert("findParticipant",arrayOf(first_name, last_name))})
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: java.io.IOException) {
                println(e.message.toString())
                //displayAlert(getResources().getString(R.string.mobile_overview_edit_goal_error), hideAlert())
                //hideAlert()
            }
        })
    }

    fun loadTeams(){
        findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.GONE)
        val inflater = LayoutInflater.from(this@BaseLanguageActivity)
        val container = findViewById<LinearLayout>(R.id.search_results);
        container.setVisibility(View.GONE)
        for (childView in container.children) {
            container.removeView(childView);
        }
        var team_name = findViewById<EditText>(R.id.input_team_name).text.toString().trim()

        var event_id = "";
        if(getStringVariable("CHECK_DEPOSIT_EVENT_ID") != ""){
            event_id = getStringVariable("CHECK_DEPOSIT_EVENT_ID")
        }

        if(team_name == ""){
            displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_team_search_results_error),"",{displayAlert("findTeam")})
            return
        }

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/check/getTeams/").plus(event_id).plus("/").plus(team_name)
        println("URL IS: " + url)
        var request = Request.Builder().url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val jsonString = response.body?.string()
                println(jsonString)
                if(response.code != 200){
                    val obj = JSONObject(jsonString)
                    if(obj.has("message") && obj.get("message") != ""){
                        displayAlert(obj.get("message") as String,"",{displayAlert("findParticipant")})
                    }
                }else {
                    runOnUiThread {
                        println(jsonString)
                        val obj = JSONObject(jsonString)
                        val data = obj.get("data").toString()
                        val jsonArray = JSONArray(data)
                        if(jsonArray.length() > 0) {
                            for (i in 0 until jsonArray.length()) {
                                val participant = jsonArray.getJSONObject(i)
                                val row = inflater.inflate(
                                    R.layout.team_result_row,
                                    null
                                ) as LinearLayout;
                                row.setOnClickListener {
                                    setVariable("CHECK_CREDIT_VALUE", participant.toString())
                                    runOnUiThread {
                                        sendGoogleAnalytics("check_deposit_find_team_select","check_deposit")
                                        childviewCallback("run_check", "")
                                        findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(
                                            View.GONE
                                        )
                                        findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(
                                            View.VISIBLE
                                        )
                                        findViewById<TextView>(R.id.donation_check_credit_entity_name).text =
                                            (participant.get("team_name") as String)
                                    }
                                    hideAlert()
                                }

                                (row.getChildAt(0) as TextView).text =
                                    (participant.get("team_name") as String)
                                container.addView(row)
                            }
                            container.setVisibility(View.VISIBLE)
                            findViewById<LinearLayout>(R.id.search_results_container).setVisibility(
                                View.VISIBLE
                            )
                        }else{
                            displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_team_search_results_none),"",{displayAlert("findTeam",arrayOf(team_name))})
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: java.io.IOException) {
                println(e.message.toString())
                //displayAlert(getResources().getString(R.string.mobile_overview_edit_goal_error), hideAlert())
                //hideAlert()
            }
        })
    }

    fun loadSearchEvents(){
        val inflater = LayoutInflater.from(this@BaseLanguageActivity)
        val container = findViewById<LinearLayout>(R.id.search_results);
        container.setVisibility(View.GONE)
        for (childView in container.children) {
            container.removeView(childView);
        }

        val event_name = findViewById<EditText>(R.id.input_event_name).text.toString().trim()
        if(event_name == ""){
            if(getStringVariable("EVENT_SEARCH_TYPE") == "ANNUAL"){
                displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_chapter_search_chapter_results_error),"",{displayAlert("findEvent")})
            }else{
                displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_event_search_results_error),"",{displayAlert("findEvent")})
            }
            return
        }

        var url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/searchEvents?event_name=").plus(event_name)
        var request = Request.Builder().url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        if(
            getStringVariable("CHECK_EVENT_MANAGER_ANNUAL_FUND_ENABLED") == "true" &&
            getStringVariable("EVENT_SEARCH_TYPE") == "EVENT"){

            url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/searchEventsByType")

            val formBody = FormBody.Builder()
                .add("unit_id", getStringVariable("CHECK_EVENT_MANAGER_UNIT_ID"))
                .add("event_name", event_name)
                .build();

            request = Request.Builder().url(url)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .post(formBody)
                .build()
        }

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val jsonString = response.body?.string()
                if(response.code != 200){
                    val obj = JSONObject(jsonString)
                    if(obj.has("message") && obj.get("message") != ""){
                        displayAlert(obj.get("message") as String,"",{displayAlert("findParticipant")})
                    }
                }else {
                    runOnUiThread {
                        for (childView in container.children) {
                            container.removeView(childView);
                        }
                        val obj = JSONObject(jsonString)
                        val data = obj.get("data").toString()
                        val jsonArray = JSONArray(data)
                        if(jsonArray.length() > 0){
                            for (i in 0 until jsonArray.length()) {
                            val event = jsonArray.getJSONObject(i)
                            val row = inflater.inflate(R.layout.event_result_row, null) as LinearLayout;
                            if(i % 2 == 0){
                                row.setBackgroundColor(Color.WHITE)
                            }
                            (row.getChildAt(0) as TextView).text = event.get("event_name") as String
                            row.setOnClickListener {

                                sendGoogleAnalytics("check_deposit_find_event_select","check_deposit")

                                if(getStringVariable("CHECK_DEPOSIT_EVENT_ID") != ""){
                                    if((event.get("event_id") as Int).toString() != getStringVariable("CHECK_DEPOSIT_EVENT_ID")){
                                        if(findViewById<Spinner>(R.id.select_donation_credit).selectedItemPosition != 3){
                                            childviewCallback("run_check","")
                                            findViewById<Spinner>(R.id.select_donation_credit).setSelection(0)
                                            clearVariable("CHECK_CREDIT")
                                            clearVariable("CHECK_CREDIT_VALUE")
                                        }
                                    }
                                }

                                setVariable("CHECK_DEPOSIT_EVENT_ID",(event.get("event_id") as Int).toString())
                                setVariable("CHECK_DEPOSIT_EVENT_NAME",(event.get("event_name") as String))
                                runOnUiThread{
                                    findViewById<LinearLayout>(R.id.find_event_container).setVisibility(View.GONE)
                                    findViewById<LinearLayout>(R.id.event_details_container).setVisibility(View.VISIBLE)
                                    findViewById<TextView>(R.id.selected_event_name).text = (event.get("event_name") as String)
                                    childviewCallback("selected_event", getSafeIntegerVariable(event, "event_id").toString())

                                    if(getStringVariable("CHECK_CREDIT") == "event"){
                                        findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(View.GONE)
                                        findViewById<LinearLayout>(R.id.split_check_credit_select_row).setVisibility(View.GONE)
                                        findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.VISIBLE)
                                        findViewById<TextView>(R.id.donation_check_credit_entity_name).text = (event.get("event_name") as String)
                                    }
                                }
                                hideAlert()
                            }
                            println(row)
                            container.addView(row)
                        }
                            container.setVisibility(View.VISIBLE)
                            findViewById<LinearLayout>(R.id.search_results_container).setVisibility(View.VISIBLE)
                        }else{
                            if(getStringVariable("EVENT_SEARCH_TYPE") == "ANNUAL"){
                                displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_chapter_search_results_none_android),"",{displayAlert("findEvent",arrayOf(event_name))})
                            }else{
                                displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_event_search_results_none),"",{displayAlert("findEvent",arrayOf(event_name))})
                            }
                        }

                        searching = false
                    }
                }
            }

            override fun onFailure(call: Call, e: java.io.IOException) {
                println(e.message.toString())
                //displayAlert(getResources().getString(R.string.mobile_overview_edit_goal_error), hideAlert())
                //hideAlert()
            }
        })
    }

    fun View.hideKeyboard() {
        val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)
    }

    fun loadUserUrls(event_id: String){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/urls/").plus(getConsID()).plus("/").plus(event_id).plus("/android/").plus(getDeviceType())
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    val jsonString = response.body?.string();
                    try{
                        val urls = JSONObject(jsonString)

                        val iter: Iterator<String> = urls.keys()
                        while (iter.hasNext()) {
                            val key = iter.next()
                            try  {
                                val value: Any = urls.get(key)
                                setVariable(key.uppercase() + "_URL", value.toString())
                            }catch (e: JSONException) {
                                // Something went wrong!
                            }
                        }

                        if(urls.has("personal_page")){
                            setVariable("PERSONAL_PAGE_URL",urls.get("personal_page") as String)
                        }else{
                            setVariable("PERSONAL_PAGE_URL","")
                        }

                        if(urls.has("aha_ecard")){
                            setVariable("AHA_ECARD_URL",urls.get("aha_ecard") as String)
                        }else{
                            setVariable("AHA_ECARD_URL","")
                        }

                        if(urls.has("prize_base_url")){
                            setVariable("PRIZE_BASE_URL",urls.get("prize_base_url") as String)
                        }else{
                            setVariable("PRIZE_BASE_URL","")
                        }

                        if(urls.has("aha_oot_manage_school")){
                            setVariable("MANAGE_SCHOOL_URL",urls.get("aha_oot_manage_school") as String)
                        }else{
                            setVariable("MANAGE_SCHOOL_URL","")
                        }

                        if(urls.has("aha_support_chat")){
                            setVariable("AHA_SUPPORT_CHAT_URL",urls.get("aha_support_chat") as String)
                        }else{
                            setVariable("AHA_SUPPORT_CHAT_URL","")
                        }

                        if(urls.has("android_app_review")){
                            setVariable("ANDROID_APP_REVIEW_URL",urls.get("android_app_review") as String)
                        }else{
                            setVariable("ANDROID_APP_REVIEW_URL","")
                        }

                        if(urls.has("manage_school_resources")){
                            setVariable("MANAGE_SCHOOL_RESOURCES_URL",urls.get("manage_school_resources") as String)
                        }else{
                            setVariable("MANAGE_SCHOOL_RESOURCES_URL","")
                        }

                        if(urls.has("donations_page")){
                            setVariable("DONATIONS_URL",urls.get("donations_page") as String)
                        }else{
                            setVariable("DONATIONS_URL","")
                        }

                        if(urls.has("parents_corner")){
                            setVariable("PARENTS_CORNER_URL",urls.get("parents_corner") as String)
                        }else {
                            setVariable("PARENTS_CORNER_URL", "")
                        }

                        if(urls.has("training_guide")){
                            setVariable("TRAINING_GUIDE_URL",urls.get("training_guide") as String)
                        }else {
                            setVariable("TRAINING_GUIDE_URL", "")
                        }

                        if(urls.has("privacy_policy")){
                            setVariable("PRIVACY_POLICY_URL",urls.get("privacy_policy") as String)
                        }else{
                            setVariable("PRIVACY_POLICY_URL","")
                        }

                        if(urls.has("educational_resources")){
                            setVariable("EDUCATIONAL_RESOURCES_URL",urls.get("educational_resources") as String)
                        }else{
                            setVariable("EDUCATIONAL_RESOURCES_URL","")
                        }

                        if(urls.has("event_page")){
                            setVariable("EVENT_PAGE_URL",urls.get("event_page") as String)
                        }else{
                            setVariable("EVENT_PAGE_URL","")
                        }

                        if(urls.has("finns_mission_completed")){
                            setVariable("FINNS_MISSION_COMPLETED_URL",urls.get("finns_mission_completed") as String)
                        }else{
                            setVariable("FINNS_MISSION_COMPLETED_URL","")
                        }

                        if(urls.has("zuri_cpr_quiz")){
                            setVariable("ZURI_CPR_QUIZ_URL",urls.get("zuri_cpr_quiz") as String)
                        }else{
                            setVariable("ZURI_CPR_QUIZ_URL","")
                        }

                        if(urls.has("zuri_stroke_quiz")){
                            setVariable("ZURI_STROKE_QUIZ_URL",urls.get("zuri_stroke_quiz") as String)
                        }else{
                            setVariable("ZURI_STROKE_QUIZ_URL","")
                        }

                        if(urls.has("zuri_mindfulness_quiz")){
                            setVariable("ZURI_MINDFULNESS_QUIZ_URL",urls.get("zuri_mindfulness_quiz") as String)
                        }else{
                            setVariable("ZURI_MINDFULNESS_QUIZ_URL","")
                        }

                        if(urls.has("zuri_vaping_quiz")){
                            setVariable("ZURI_VAPING_QUIZ_URL",urls.get("zuri_vaping_quiz") as String)
                        }else{
                            setVariable("ZURI_VAPING_QUIZ_URL","")
                        }

                        if(urls.has("android_sticker_pack")){
                            setVariable("ANDROID_STICKER_PACK",urls.get("android_sticker_pack") as String)
                        }else{
                            setVariable("ANDROID_STICKER_PACK","")
                        }

                        if(urls.has("gallery_post_picture")){
                            setVariable("GALLERY_POST_PICTURE",urls.get("gallery_post_picture") as String)
                        }else{
                            setVariable("GALLERY_POST_PICTURE","")
                        }

                        if(urls.has("waiver")){
                            setVariable("WAIVER_URL",urls.get("waiver") as String)
                        }else{
                            setVariable("WAIVER_URL","")
                        }

                        if(urls.has("cpr_tile_button")){
                            setVariable("CPR_TILE_BUTTON_URL", urls.get("cpr_tile_button") as String)
                        }else{
                            setVariable("CPR_TILE_BUTTON_URL","")
                        }

                        if(urls.has("aha_rewards_center")){
                           setVariable("AHA_REWARDS_CENTER", urls.get("aha_rewards_center") as String)
                        }else{
                            setVariable("AHA_REWARDS_CENTER","")
                        }

                        if(urls.has("aha_cpr_video")){
                           setVariable("AHA_CPR_VIDEO", urls.get("aha_cpr_video") as String)
                        }else{
                            setVariable("AHA_CPR_VIDEO","")
                        }

                        if(urls.has("overview_progress_manage_hq")){
                           setVariable("OVERVIEW_PROGRESS_MANAGE_HQ", urls.get("overview_progress_manage_hq") as String)
                        }else{
                            setVariable("OVERVIEW_PROGRESS_MANAGE_HQ","")
                        }
                        
                        if(urls.has("facebook_fundraiser_create")) {
                            setVariable("FACEBOOK_FUNDRAISER_CREATE_URL", urls.get("facebook_fundraiser_create") as String)
                        } else {
                            setVariable("FACEBOOK_FUNDRAISER_CREATE_URL", "")
                        }

                        if(urls.has("impact_points_hq")) {
                            setVariable("IMPACT_POINTS_HQ", urls.get("impact_points_hq") as String)
                        } else {
                            setVariable("IMPACT_POINTS_HQ", "")
                        }

                        if(urls.has("impact_points_team_hq")) {
                            setVariable("IMPACT_POINTS_TEAM_HQ", urls.get("impact_points_team_hq") as String)
                        } else {
                            setVariable("IMPACT_POINTS_TEAM_HQ", "")
                        }

                        if(urls.has("impact_badges_hq")) {
                            setVariable("IMPACT_BADGES_HQ", urls.get("impact_badges_hq") as String)
                        } else {
                            setVariable("IMPACT_BADGES_HQ", "")
                        }

                        if(urls.has("resource_center")){
                            setVariable("RESOURCE_CENTER_URL", urls.get("resource_center") as String)
                        } else {
                            setVariable("RESOURCE_CENTER_URL", "")
                        }

                        if(urls.has("distance_challenge_leaderboard")){
                            setVariable("DISTANCE_CHALLENGE_LEADERBOARD_URL", urls.get("DISTANCE_CHALLENGE_LEADERBOARD_URL") as String)
                        } else {
                            setVariable("DISTANCE_CHALLENGE_LEADERBOARD_URL", "")
                        }

                        if(urls.has("about_event")){
                            setVariable("ABOUT_EVENT_URL", urls.get("ABOUT_EVENT_URL") as String)
                        } else {
                            setVariable("ABOUT_EVENT_URL", "")
                        }


                        if(urls.has("mobile_client_help")){
                            setVariable("MOBILE_CLIENT_HELP_URL", urls.get("mobile_client_help") as String)
                        } else {
                            setVariable("MOBILE_CLIENT_HELP_URL", "")
                        }
                    } catch(exception:IOException){
                        displayAlert(getResources().getString(R.string.mobile_error_unavailable));
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                displayAlert(getResources().getString(R.string.mobile_login_no_events));
            }
        })
    }

    fun logRegToken() {
        Firebase.messaging.getToken().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                println("Fetching FCM registration token failed")
                return@addOnCompleteListener
            }
            val token = task.result
            sendFCMTokenToServer(token)
        }
    }

    fun sendFCMTokenToServer(token: String){
        Executors.newSingleThreadExecutor().execute(Runnable {
            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/user/addUserDevice")
            val cons_id = getConsID()
            val device_info = (Build.BRAND + " " + Build.MODEL + " / Android / " + Build.VERSION.RELEASE)
            val formBody = FormBody.Builder()
                .add("cons_id", cons_id)
                .add("token", token)
                .add("device_type", "android")
                .add("device_info",device_info)
                .build()

            val request = Request.Builder().url(url)
                .post(formBody)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()

            val client = OkHttpClient();
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if(response.code != 200){
                        throw Exception(response.body?.string())
                    }else{
                        println("SENT TOKEN")
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    println("TOKEN ERROR")
                }
            })
        })
    }

    fun loginCallback(jsonString: String, username: String, password: String, remembered: Boolean){
        print("CALL BACK")
        print(jsonString)
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

                    println("SETTING CONS +ID")
                    if(data.has("cons_id")){
                        if(data.get("cons_id") is String){
                            setConsID(data.get("cons_id") as String);
                        }else{
                            setConsID((data.get("cons_id") as Int).toString());
                        }
                    }else{
                        setConsID("")
                    }

                    if(!remembered){
                        val remCheckBox = findViewById<CheckBox>(R.id.remember_me);
                        if (remCheckBox.isChecked && getBiometricString("BIOMETRIC_LOGIN_ENABLED") != "enabled") {
                            setVariable("REMEMBER_ME", "true");
                            setVariable("REMEMBER_ME_USERNAME", username)
                            if(getStringVariable("CLIENT_CLASS") == "classy") {
                                setVariable("REMEMBER_ME_PASSWORD", password)
                            }
                        } else {
                            setVariable("REMEMBER_ME", "false");
                            setVariable("REMEMBER_ME_USERNAME", "")
                            if(getStringVariable("CLIENT_CLASS") == "classy") {
                                setVariable("REMEMBER_ME_PASSWORD", "")
                            }
                        }
                    }

                    loadEvents("")
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

    fun codeLogin(email: String, password: String, code: String, remembered: Boolean){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/user/login")
        val formBody = FormBody.Builder()
            .add("username", email)
            .add("password", password)
            .add("token", code)
            .build()

        var request = Request.Builder()
            .url(url)
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .post(formBody)
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code == 401){
                    displayAlert(getResources().getString(R.string.mobile_login_incorrect_code))
                }else{
                    val response = response.body?.string();
                    val obj = JSONObject(response);
                    if(obj.has("statusCode")) {
                        val status_code = obj.get("statusCode")
                        if(status_code == 403 || status_code == 500){
                            displayAlert(getResources().getString(R.string.mobile_login_incorrect_code))
                        }else{
                            if(response != null){
                                loginCallback(response, email, password, remembered)
                            }
                        }
                    }else{
                        displayAlert(getResources().getString(R.string.mobile_login_incorrect_code))
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                displayAlert(getResources().getString(R.string.mobile_overview_edit_goal_error), hideAlert())
                hideAlert()
            }
        })
    }

    open fun loadEvents(source: String){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getevents/").plus(getConsID())
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
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

                            val event = Event(event_id, event_name, event_date,event_cons_id)
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
                                if(source == "register_now"){
                                    displayAlert(getResources().getString(R.string.mobile_login_register_now_wrong_account));
                                }else{
                                    displayAlert(getResources().getString(R.string.mobile_login_no_events));
                                }
                            }
                        }
                    } catch(exception:IOException){
                        displayAlert(getResources().getString(R.string.mobile_error_unavailable));
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                displayAlert(getResources().getString(R.string.mobile_login_no_events));
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Restring.init(this)
        ViewPump.init(RewordInterceptor)
        firebaseAnalytics = Firebase.analytics
    }

    fun getStringsUpdated(function: () -> (Unit) = {}){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/strings")

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
                    configureLocales()
                    updateStringsHashmap(response!!)
                    function()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
            }
        })
    }

        fun updateStringsHashmap(jsonString: String) {
            getLocalStringJsonHashmap(app_lang, jsonString).forEach {
                newStringsMap[it.key] = it.value
            }
            updateAppLanguage(app_lang)
        }

        override fun attachBaseContext(newBase: Context?) {
            super.attachBaseContext(wrapContext(newBase!!))
        }

        private fun updateAppLanguage(language: String) {
            Restring.putStrings(newStringLocale, newStringsMap)
            Restring.locale = newStringLocale
            Locale.setDefault(newStringLocale);
            runOnUiThread {
                updateView()
            }
        }

        fun needHelpClicked(secondary_help_link: String, source: String){
            val main_help_link = getStringVariable("HELP_LINK")
            if(secondary_help_link != ""){
                displayAlert("needHelp", source)
            }else{
                if(main_help_link != ""){
                    val browserIntent =
                        Intent(Intent.ACTION_VIEW, Uri.parse(main_help_link))
                    startActivity(browserIntent)
                }
            }
        }

        fun updateView() {
            val rootView: View = findViewById(android.R.id.content)
            reword(rootView)
        }

        fun getLocalStringJsonHashmap(language: String, jsonString:String): HashMap<String, String> {
            val listTypeJson: HashMap<String, String> = HashMap()
            try {

                JsonHelper().getFlattenedHashmapFromJsonForLocalization(
                    "",
                    ObjectMapper().readTree(jsonString),
                    listTypeJson
                )
            } catch (exception: IOException) {
            }
            return listTypeJson
        }

    fun loadUrls(hasRegisterButton: Boolean, hasHelpButton: Boolean){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/urls")
        var request = Request.Builder()
            .url(url)
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

                        var register_link = "";
                        var help_link = "";
                        setVariable("PLAY_STORE_PRIMARY_URL", "");
                        setVariable("PLAY_STORE_SECONDARY_URL", "");

                        listTypeJson.forEach {

                            println("URL KEY: " + it.key)
                            println("URL VALUE: " + it.value)

                            newStringsMap[it.key] = it.value
                            if(it.key == "mobile_register"){
                                register_link = it.value;
                            }else if (it.key == "donations_page"){
                                setVariable("DONATIONS_URL", it.value);
                            }
                            else if (it.key == "mobile_help") {
                                help_link = it.value;
                            }else if (it.key == "install_url_google_primary"){
                                setVariable("PLAY_STORE_PRIMARY_URL", it.value);
                            }else if (it.key == "install_url_google_secondary"){
                                setVariable("PLAY_STORE_SECONDARY_URL", it.value);
                            }else if (it.key == "waiver"){
                                setVariable("WAIVER_URL", it.value)
                            }
                        }

                        setVariable("HELP_LINK",help_link)
                        if(hasRegisterButton){
                            if(register_link != ""){
                                val registerButton = findViewById<Button>(R.id.btn_registration_link);
                                registerButton.setOnClickListener {
                                    val browserIntent =
                                        Intent(Intent.ACTION_VIEW, Uri.parse(register_link))
                                    startActivity(browserIntent)
                                }
                            }else{
                                findViewById<Button>(R.id.btn_registration_link).setVisibility(View.INVISIBLE);
                                findViewById<TextView>(R.id.registration_link_text).setVisibility(View.INVISIBLE);
                            }
                        }

                        if(hasHelpButton){
                            if(help_link != ""){
                                findViewById<Button>(R.id.btn_help_link).setOnClickListener{
                                    needHelpClicked(getStringVariable("MOBILE_CLIENT_HELP_LOGIN_URL"), "login")
                                }
                            }else{
                                findViewById<Button>(R.id.btn_help_link).setVisibility(View.INVISIBLE);
                            }
                        }
                        checkForUpdate()

                    } catch (exception: IOException) {
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
            }
        })
    }

    class Event(
        val event_id: String,
        val event_name: String,
        val event_date: String,
        val event_cons_id: String
    )

    fun getDeviceType() : String{
        val isTablet = getResources().getBoolean(R.bool.isTablet)
        if(isTablet){
            return "tablet"
        }else{
            return "phone"
        }
    }

    fun addCents(input: String) : String{
        val index = input.indexOf(".");
        var newString = input
        if(index <= -1){
            newString = input + ".00"
        }
        return newString;
    }
    fun toDouble(input: Any) : Double {
        var double = 0.00
        when (input) {
            is String -> double = input.toDoubleOrNull() ?: 0.00
            is Int -> double = input.toDouble()
            is Double -> double = input
 //           else -> {
                // This can be used Handle unexpected types
 //           }
        }
        return double
    }

    fun isJSONValid(test: String?): Boolean {
        try {
            JSONObject(test)
        } catch (ex: JSONException) {
            try {
                JSONArray(test)
            } catch (ex1: JSONException) {
                return false
            }
        }
        return true
    }

    fun switchSlideButton(index: Int, length: Int, containerId: Int){
        val parentView = findViewById<LinearLayout>(containerId) ?: return
        val parent_container = parentView.getChildAt(0) as? LinearLayout ?: return

        for (childView in parent_container.children) {
            println(childView)
        }

        val container = parent_container.getChildAt(1) as? LinearLayout ?: return

        if(index <= length && index > 0){
            for (childView in container.children) {
                val img2 = (childView as LinearLayout).getChildAt(0) as ImageView
                img2.setBackgroundResource(R.drawable.slide_button_background)
                val backgroundOff: Drawable = img2.getBackground()
                if(getStringVariable("SWIPING_DOT_INACTIVE_COLOR") != ""){
                    backgroundOff.setTint(Color.parseColor(getStringVariable("SWIPING_DOT_INACTIVE_COLOR")))
                }else{
                    backgroundOff.setTint(Color.parseColor("#d9d6cd") )
                }
                img2.setBackground(backgroundOff)
            }

            if(index == 1){
                parent_container.getChildAt(0).setVisibility(View.INVISIBLE)
                parent_container.getChildAt(2).setVisibility(View.VISIBLE)
            }else if (index == length){
                parent_container.getChildAt(0).setVisibility(View.VISIBLE)
                parent_container.getChildAt(2).setVisibility(View.INVISIBLE)
            }else{
                parent_container.getChildAt(0).setVisibility(View.VISIBLE)
                parent_container.getChildAt(2).setVisibility(View.VISIBLE)
            }

            val img: ImageView;
            if(index == 1){
                img = (container.getChildAt(0) as LinearLayout).getChildAt(0) as ImageView
            }else if(index == length && length >=3){
                img = (container.getChildAt(2) as LinearLayout).getChildAt(0) as ImageView
            }else {
                img = (container.getChildAt(1) as LinearLayout).getChildAt(0) as ImageView
            }

            val backgroundOff: Drawable = img.getBackground()
            backgroundOff.setTint(Color.parseColor(getStringVariable("PRIMARY_COLOR").trim()))
            img.setBackground(backgroundOff)
        }
    }

    fun setupSlideButtons(length: Int, containerId: Int, card:String){
        runOnUiThread {
            var container_id = findViewById<LinearLayout>(containerId);
            val inflater = layoutInflater

            for (i in 0..container_id.children.count()) {
                container_id.removeView(container_id.getChildAt(i))
            }

            if(card == "overview_challenges"){
                var binding: SwipeLargeArrowsButtonsBinding = DataBindingUtil.inflate(
                    inflater, R.layout.swipe_large_arrows_buttons ,container_id, true)
                binding.colorList = getColorList("")
            }else{
                var binding: SwipeArrowsButtonsBinding = DataBindingUtil.inflate(
                    inflater, R.layout.swipe_arrows_buttons ,container_id, true)
                binding.colorList = getColorList("")
            }

            var parent_container = (container_id.getChildAt(0) as LinearLayout)
            var button_container = (parent_container.getChildAt(1) as LinearLayout)

            for (i in 0..button_container.children.count()) {
                button_container.removeView(button_container.getChildAt(i))
            }


            parent_container.setVisibility(View.VISIBLE);

            var endingIndex = 0;
            if (length >= 3) {
                for (i in 1..3) {
                    inflater.inflate(R.layout.slide_button, button_container)
                }
                endingIndex = 2;
                switchSlideButton(1, length, containerId)
            } else if (length == 2) {
                for (i in 1..2) {
                    inflater.inflate(R.layout.slide_button, button_container)
                }
                endingIndex = 1;
                switchSlideButton(1, length, containerId)
            } else {
                parent_container.setVisibility(View.GONE)
                endingIndex = 0;
            }

            var left_arrow = (parent_container.getChildAt(0) as LinearLayout);
            var right_arrow = (parent_container.getChildAt(2) as LinearLayout);

            if (endingIndex > 0) {
                button_container.getChildAt(0).setOnClickListener {
                    slideButtonCallback(card, false)
                }
                button_container.getChildAt(endingIndex).setOnClickListener {
                    slideButtonCallback(card, true)
                }

                left_arrow.setOnClickListener{
                    slideButtonCallback(card, false)
                }

                right_arrow.setOnClickListener{
                    slideButtonCallback(card, true)
                }
            }



            if (button_container.children.count() > length) {
                button_container.removeView(button_container.getChildAt(length - 1))
            }
        }
    }

    fun switchVariableSlideButton(index: Int, length: Int, input_container: LinearLayout){
        val parent_container = input_container.getChildAt(0) as LinearLayout;
        var container: LinearLayout;
        container = (parent_container.getChildAt(1) as LinearLayout)



        if(index <= length && index > 0){
            for (childView in container.children) {
                val img2 = (childView as LinearLayout).getChildAt(0) as ImageView
                img2.setBackgroundResource(R.drawable.slide_button_background)
                val backgroundOff: Drawable = img2.getBackground()
                backgroundOff.setTint(Color.parseColor("#d9d6cd") )
                img2.setBackground(backgroundOff)
            }

            if(index == 1){
                parent_container.getChildAt(0).setVisibility(View.INVISIBLE)
                parent_container.getChildAt(2).setVisibility(View.VISIBLE)
            }else if (index == length){
                parent_container.getChildAt(0).setVisibility(View.VISIBLE)
                parent_container.getChildAt(2).setVisibility(View.INVISIBLE)
            }else{
                parent_container.getChildAt(0).setVisibility(View.VISIBLE)
                parent_container.getChildAt(2).setVisibility(View.VISIBLE)
            }

            if(index == 1){
                (container.getChildAt(0) as LinearLayout).getChildAt(0).setBackgroundResource(R.drawable.slide_button_white)
            }else if(index == length && length >=3){
                (container.getChildAt(2) as LinearLayout).getChildAt(0).setBackgroundResource(R.drawable.slide_button_white)
            }else{
                (container.getChildAt(1) as LinearLayout).getChildAt(0).setBackgroundResource(R.drawable.slide_button_white)
            }
        }
    }

    fun setupVariableSlideButtons(length: Int, container: LinearLayout, card:FrameLayout){
        runOnUiThread {
            val inflater = layoutInflater

            val binding: SwipeArrowsButtonsBinding = DataBindingUtil.inflate(
                inflater, R.layout.swipe_arrows_buttons ,container, true)
            binding.colorList = getColorList("")

            val parent_container = binding.root as LinearLayout
            var button_container = (parent_container.getChildAt(1) as LinearLayout)

            for (i in 0..button_container.children.count()) {
                button_container.removeView(button_container.getChildAt(i))
            }

            parent_container.setVisibility(View.VISIBLE);

            var endingIndex = 0;
            if (length >= 3) {
                for (i in 1..3) {
                    inflater.inflate(R.layout.slide_button, button_container)
                }
                endingIndex = 2;
                switchVariableSlideButton(1, length, container)
            } else if (length == 2) {
                for (i in 1..2) {
                    inflater.inflate(R.layout.slide_button, button_container)
                }
                endingIndex = 1;
                switchVariableSlideButton(1, length, container)
            } else {
                parent_container.setVisibility(View.GONE)
                endingIndex = 0;
            }

            if (endingIndex > 0) {
                ((parent_container.getChildAt(0) as LinearLayout).getChildAt(0) as ImageView).setColorFilter(Color.argb(255, 255, 255, 255));
                ((parent_container.getChildAt(2) as LinearLayout).getChildAt(0) as ImageView).setColorFilter(Color.argb(255, 255, 255, 255));
                button_container.getChildAt(0).setOnClickListener {
                    slideButtonCallback(card, false)
                }
                button_container.getChildAt(endingIndex).setOnClickListener {
                    slideButtonCallback(card, true)
                }

                (parent_container.getChildAt(0) as LinearLayout).setOnClickListener{
                    slideButtonCallback(card, false)
                }

                (parent_container.getChildAt(2) as LinearLayout).setOnClickListener{
                    slideButtonCallback(card, true)
                }
            }

            if (button_container.children.count() > length) {
                button_container.removeView(button_container.getChildAt(length - 1))
            }
        }
    }

    fun sendSocialActivity(channel: String){
        println("SEND SOCIAL ACTIVITY CHANNEL: " + channel)
        if(getStringVariable("AHA_SMT_WEBHOOKS_ENABLED") == "true"){
            Executors.newSingleThreadExecutor().execute(Runnable {
                val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/addAction/").plus(getConsID()).plus("/").plus(getEvent().event_id).plus("/").plus(channel)
                var request = Request.Builder()
                    .url(url)
                    .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                    .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                    .build()

                var client = OkHttpClient();

                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: okhttp3.Response) {
                        println("SEND SOCIAL ACTIVITY RESPONSE")
                        println(response)
                    }
                    override fun onFailure(call: Call, e: IOException) {
                        println(e.message.toString());
                    }
                })
            })
        }

        if(getStringVariable("SOCIAL_POST_GROUP_TRACKING_ENABLED") == "true" && channel != "personal_page_update"){
            Executors.newSingleThreadExecutor().execute(Runnable {
            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/addUserToGroup")
            val cons_id = getConsID()
            val event_id = getEvent().event_id
            val formBody = FormBody.Builder()
                .add("cons_id", cons_id)
                .add("event_id", event_id)
                .add("group_type", "SOCIAL_POST")
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
                        println(response)
                        println("SOCIAL_POST SUCCESFULLY SENT")
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    println("SOCIAL_POST ERROR")
                    println(e)
                }
            })
        })
        }
    }

    fun intWithCommas(amount: Int): String{
        val numberFormat = DecimalFormat("#,###")
        return numberFormat.format(amount).replace(".",",")
    }

    fun withCommas(number: Double): String{
        val formatter = DecimalFormat("#,###,###.##")
        var formatted_string = formatter.format(number);
        val periodIndex = formatted_string.indexOf(".");
        val length = formatted_string.length
        if(periodIndex == -1){
            formatted_string += ".00"
        } else if (periodIndex == (length - 2)){
            formatted_string += "0"
        }
        return formatted_string;
    }

    fun formatDoubleToLocalizedCurrency(amount: Double): String {
        var lang = getStringVariable("APP_LANG")

        if(lang == ""){
            lang = "en"
        }

        if(lang != "en"){
            val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
            formatter.currency = Currency.getInstance("USD")
            return formatter.format(amount).replace("US","")
        }else{
            val formatter = NumberFormat.getCurrencyInstance(Locale("en"))
            formatter.currency = Currency.getInstance("USD")
            return formatter.format(amount).replace("US","")
        }
    }

    fun formatLocalizedCurrencyString(amount: String): String {
        var lang = getStringVariable("APP_LANG")

        if(lang == ""){
            lang = "en"
        }
        
        if(lang != "en"){
            val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
            formatter.currency = Currency.getInstance("USD")
            return formatter.format(amount.replace(",","").toDouble()).replace("US","")
        }else{
            val formatter = NumberFormat.getCurrencyInstance(Locale("en"))
            formatter.currency = Currency.getInstance("USD")
            return formatter.format(amount.replace(",","").toDouble()).replace("US","")
        }
    }
    
    open fun hideKeyboard(activity: Activity) {
        val v = activity.window.currentFocus
        if (v != null) {
            val imm: InputMethodManager =
                activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    fun getColorList(location: String?): ColorList {
        if (location == "login") {
            val customBack = getStringVariable("LOGIN_BACKGROUND_IMAGE_ENABLED") == "true" && getStringVariable("LOGIN_BACKGROUND_IMAGE").isNotEmpty()
            val isGrey = getStringVariable("LOGIN_TILE_BACKGROUND_GREY_ENABLED") == "true"
            val isWhite = !isGrey && !customBack

            return ColorList(
                primaryColor = getStringVariable("PRIMARY_COLOR"),
                isWhite = isWhite,
                buttonColor = getStringVariable("LOGIN_BUTTON_COLOR"),
                buttonTextColor = getStringVariable("LOGIN_BUTTON_TEXT_COLOR"),
                isGrey = isGrey
            )
        } else {
            return ColorList(
                primaryColor = getStringVariable("PRIMARY_COLOR").trim().lowercase(),
                isWhite = getStringVariable("TILE_BACKGROUND_WHITE_ENABLED") == "true",
                buttonColor = getStringVariable("BUTTON_COLOR").trim().lowercase(),
                buttonTextColor = getStringVariable("BUTTON_TEXT_COLOR").trim().lowercase(),
                metricIsKm = getStringVariable("ACTIVITY_TRACKING_DISTANCE_METRIC") == "km"
            )
        }
    }

    fun removeAllPages(parent:FrameLayout){
        var length = 0
        for (childView in parent.children) {
            length+= 1
        }
        for (i in 0 until length) {
            parent.removeView(parent.getChildAt(0))
        }
    }
    
    fun removeAllChildren(parent:LinearLayout, function: () -> (Unit) = {}){
        var length = 0
        for (childView in parent.children) {
            length+= 1
        }
        for (i in 0 until length) {
            parent.removeView(parent.getChildAt(0))
        }
        function()
    }

    fun removeAllRows(parent:TableLayout, function: () -> (Unit) = {}){
        var length = 0
        for (childView in parent.children) {
            length+= 1
        }
        for (i in 2 until length) {
            parent.removeView(parent.getChildAt(2))
        }
        function()
    }

    fun removeAllSlides(parent: FrameLayout, afterRemoval: () -> Unit) {
        parent.removeAllViews()
        afterRemoval()
    }

    fun startArTracking(scene: String) {
        arScene = scene
        arStart = Date()
    }

    fun endArTracking(){
        if(arScene != null){
            val arEnd = Date()

            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/arTracking")
            val client = OkHttpClient()
            val formBody = FormBody.Builder()
                .add("cons_id", getConsID())
                .add("event_id", getEvent().event_id)
                .add("scene_name", arScene.toString())
                .add("start_time", arStart.toString())
                .add("end_time", arEnd.toString())
                .build()
    
            var request = Request.Builder().url(url)
                .post(formBody)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()
    
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if(response.code != 200){
                        println("Successfully sent AR tracking")
                        arScene = null
                        arStart = null
                        
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread{
                        println("Error sending AR tracking")
                        arScene = null
                        arStart = null
                    }
                }
            })
        }
    }
    
    fun loadUnityContent(type: String){
        //BEGIN_UNITY_CONTENT
        val intent = Intent(this, UnityPlayerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT

        if(type == "ar_masks"){
            intent.putExtra("unity_action","ShowFaceFilters");
            intent.putExtra("unity_details", "")
        }else if(type == "colouring_page"){
            intent.putExtra("unity_action","ShowColouringPage");
            intent.putExtra("unity_details", "")
        }else if(type == "poster"){
            intent.putExtra("unity_action","ShowPosterVideos");
            intent.putExtra("unity_details", "{IsLoggedIn:false}")
        }else if(type == "poster_logged_in"){
            intent.putExtra("unity_action","ShowPosterVideos");
            intent.putExtra("unity_details", "{IsLoggedIn:true}")
        }

        startActivityForResult(intent, UNITY_CONTENT_REQUEST)
        //END_UNITY_CONTENT
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if(requestCode == UNITY_CONTENT_REQUEST ){
            endArTracking()
        }
    }

    fun mimicThrowError(message: String?){
        if(message is String){
            println("EXCEPTION WAS THROWN: " + message)
            try{
                throw Error(message)
            }catch(e: Exception){
                sendErrorToServer(e)
            }
        }
        showDefaultPage()
    }

    fun sendErrorToServer(e: Exception){
        println("Sending Error to Server");
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
                        val intent = Intent(this@BaseLanguageActivity, Error::class.java);
                        startActivity(intent);
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Error logging error")
                println(e.message.toString())
            }
        })
    }

     fun getBiometricStringName(value: String): String{
        var preface = getStringVariable("CLIENT_CODE") + "_"
        if(getStringVariable("BIOMETRIC_APP_CODE") != "") {
            preface = getStringVariable("BIOMETRIC_APP_CODE") + "_" + getStringVariable("CLIENT_CODE") + "_"
        }
        return preface + value
    }

    fun getBiometricString(value: String): String{
        var preface = getStringVariable("CLIENT_CODE") + "_"
        if(getStringVariable("BIOMETRIC_APP_CODE") != "") {
            preface = getStringVariable("BIOMETRIC_APP_CODE") + "_" + getStringVariable("CLIENT_CODE") + "_"
        }

        if(value == "CLEAR"){
            clearVariable("BIOMETRIC_APP_CODE")
            clearVariable(preface + "BIOMETRIC_LOGIN_ENABLED")
            clearVariable(preface + "BIOMETRIC_USERNAME")
            clearVariable(preface + "BIOMETRIC_PASSWORD")
        }

        return getStringVariable(preface + value)
    }

    fun openUrlInBrowser(url: String){
        if(url != ""){
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        }
    }

    fun getDistanceLabel(miles_label: Int, kilometers_label: Int): String{
        if(getStringVariable("ACTIVITY_TRACKING_DISTANCE_METRIC") == "km"){
            return getString(kilometers_label)
        }else{
            return getString(miles_label)
        }
    }
}

class WorkoutType(
    val name: String,
    val internal_name: String,
    val image_url: String,
    val allow_distance:Boolean,
)

@BindingAdapter("android:customBackground")
fun View.setBackgroundTintColorValue(colorValue: String) {
    if (colorValue == "") return
    setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorValue)))
}

@BindingAdapter("android:customBackgroundAlpha")
fun View.setBackgroundTintColorAlpha(colorValue: String) {
    if (colorValue == "") return
    val newVal = colorValue.replace("#", "#33")
    println("NEW VAL IS: " + newVal)
    setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(newVal)))
}

@BindingAdapter("android:customBackground")
fun VerticalScrollView.setBackgroundTintColorValue(colorValue: String) {
    if (colorValue == "") return
    setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorValue)))
}

@BindingAdapter("android:customScrollbarThumbVertical")
fun VerticalScrollView.setScrollBarColorValue(colorValue: String){
    if (colorValue == "") return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        this.verticalScrollbarThumbDrawable = ColorDrawable(Color.parseColor(colorValue))
    }
}

@BindingAdapter("android:customBackgroundTintColor")
fun Button.setBackgroundTintColorValue(colorValue: String) {
    if (colorValue == "") return
    setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(colorValue)))
}

@BindingAdapter("android:customTextColor")
fun Button.setTextColorValue(colorValue: String) {
    if (colorValue == "") return
    if (colorValue == "no_stroke"){
        setTextColor(Color.parseColor("#ffffff"))
    }else {
        setTextColor(Color.parseColor(colorValue))
    }
}

@BindingAdapter("android:customStrokeColor")
fun MaterialButton.setStrokeColor(colorValue: String) {
    if (colorValue == "") return
    if (colorValue == "no_stroke"){
        this.setStrokeWidth(0)
    }else{
        this.setStrokeColor(ColorStateList.valueOf(Color.parseColor(colorValue)))
        this.setStrokeWidth(5)
    }
}

@BindingAdapter("android:customTextColor")
fun TextView.setTextColorValue(colorValue: String) {
    if (colorValue == "") return
    setTextColor(Color.parseColor(colorValue))
}

@BindingAdapter("android:customCheckboxTint")
fun CheckBox.setTintValue(colorValue: String) {
    if (colorValue == "") return
    setButtonTintList(ColorStateList.valueOf(Color.parseColor(colorValue)))
}

@BindingAdapter("android:customImageTint")
fun ImageButton.setTintValue(colorValue: String) {
    if (colorValue == "") return
    setTint(this.drawable,Color.parseColor(colorValue))
}

@BindingAdapter("android:customImageTint")
fun ImageView.setTintValue(colorValue: String) {
    if (colorValue == "") return
    setTint(this.drawable,Color.parseColor(colorValue))
}

class ColorList(
    val primaryColor: String,
    val isWhite: Boolean,
    val buttonColor: String,
    val buttonTextColor: String,
    val isGrey: Boolean = false,
    val metricIsKm: Boolean = false,
)

val EMOJI_FILTER =InputFilter { source, start, end, dest, dstart, dend ->
    for (index in start until end) {
        val type = Character.getType(source[index])
    if (type == Character.SURROGATE.toInt() || type == Character.OTHER_SYMBOL.toInt() || type == Character.OTHER_SYMBOL.toInt() || type == Character.NON_SPACING_MARK.toInt()) {
            return@InputFilter ""
        }
    }
    null
}

fun getSafeBooleanVariable(json_object: JSONObject, variableName: String): Boolean{
    var newVariable = false
    if(json_object.has(variableName) && json_object.get(variableName) is Boolean){
        newVariable = json_object.get(variableName) as Boolean
    }
    return newVariable;
}

fun safeToInt(string: String): Int{
    if(string == ""){
        return 0
    }
    try{
        val int = string.toInt();
        return int;
    } catch(exception: Exception) {
        return 0
    }
}

fun getSafeStringVariable(json_object: JSONObject, variableName: String):String{
    var newVariable = ""
    if(json_object.has(variableName) && json_object.get(variableName) is String){
        newVariable = json_object.get(variableName) as String
    }
    return newVariable;
}

fun getSafeDoubleVariable(json_object: JSONObject, variableName: String):Double{
    var newVariable = 0.00
    if(json_object.has(variableName) && json_object.get(variableName) is Double){
        newVariable = json_object.get(variableName) as Double
    }else if (json_object.has(variableName) && json_object.get(variableName) is Int){
        newVariable = (json_object.get(variableName) as Int).toDouble()
    }
    return newVariable;
}


fun getSafeIntegerVariable(json_object: JSONObject, variableName: String): Int{
    var newVariable = 0;
    if(json_object.has(variableName) && json_object.get(variableName) is Int){
        newVariable = json_object.get(variableName) as Int
    }
    return newVariable;
}

fun makeLinks(
    text: String,
    phrase: String,
    phraseColor: Int,
    listener: View.OnClickListener
): SpannableString {
    val spannableString = SpannableString(text)
    val clickableSpan = object : ClickableSpan() {
        override fun updateDrawState(ds: TextPaint) {
            ds.color = phraseColor      // you can use custom color
            ds.isUnderlineText = true // this remove the underline
        }
        override fun onClick(view: View) {
            listener.onClick(view)
        }
    }
    val start = text.indexOf(phrase)
    val end = start + phrase.length
    spannableString.setSpan(
        clickableSpan,
        start,
        end,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    return spannableString
}

fun isEmailValid(email: String?): Boolean {
    val expression = "^\\S+@\\S+\\.\\S+$"
    val pattern: Pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
    val matcher: Matcher = pattern.matcher(email)
    return matcher.matches()
}

class TimeInputAccessibilityDelegate : View.AccessibilityDelegate() {
    override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(host, info)
        val editText = host as EditText
        info.text = editText.contentDescription
    }
}

fun setInitialContentDescription(editText: EditText, text: String) {
    val parts = text.split(":")
    val hour = parts[0].toInt()
    val minute = parts[1].toInt()
    val contentDescription = "$hour hour${if (hour != 1) "s" else ""} $minute minute${if (minute != 1) "s" else ""}"
    editText.contentDescription = contentDescription
}