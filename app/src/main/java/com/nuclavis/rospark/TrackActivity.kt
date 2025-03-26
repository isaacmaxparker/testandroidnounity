package com.nuclavis.rospark

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.view.children
import androidx.core.widget.ImageViewCompat
import androidx.databinding.DataBindingUtil
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import app.com.kotlinapp.OnSwipeTouchListener
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType.*
import com.google.android.gms.tasks.Task
import com.nuclavis.rospark.databinding.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.lang.reflect.Modifier
import java.nio.charset.Charset
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class TrackActivity : com.nuclavis.rospark.BaseActivity() {

    val GOOGLE_SIGN_IN = 1909

    var fitbit_redirect_uri = ""
    var strava_redirect_uri = ""

    var weeks = listOf<Week>();
    var google_fit_connected = false;
    var apple_fit_connected = false;
    var strava_connected = false;
    var garmin_connected = false;
    var android_health_connected = false;
    var fitbit_connected = false;
    var platform_connected = false;

    var workout_enabled = false;
    var steps_enabled = false;

    var google_client_id = ""
    var google_client_secret = ""
    var fitbit_client_id = ""
    var fitbit_client_secret = ""
    var fitbit_state = ""
    var fitbit_pkce = ""
    var fitbit_challenge = ""
    var strava_client_id = ""
    var strava_client_secret = ""

    var totalSlideCount = 5;
    var currentSlideIndex = 0;
    var currentWeekSlideIndex = 0;
    var totalWeekSlideCount = 5;

    var isFirst = false;

    var currentPointsSlideIndex = 0
    var totalPointsSlideCount = 0

    var platform_connected_time = "";
    var platform_sync_time = "";

    var activities = listOf<ActivityGoal>()
    var modalSortBy = "default"
    var modalSortDir = "asc"
    var help_strings = arrayOf(R.string.mobile_earn_points_help_workout_text)
    var workoutTypes = emptyArray<WorkoutType>()

    var hobby_names = arrayOf<String>(
        "artscrafts",
        "bakingcooking",
        "boardgames",
        "boatingfishing",
        "bridge",
        "carmotorsports",
        "cards",
        "chess",
        "cookout",
        "gala",
        "gardening",
        "kickball",
        "knittingcrocheting",
        "livestreaming",
        "mahjongg",
        "motorcycle",
        "musicsinging",
        "party",
        "readingbookclub",
        "tabletennis",
        "videogaming",
    )

    //ANDROID_HEALTH_CONTENT_1
    val REQUIRED_ANDROID_HEALTH_CONNECT_PERMISSIONS = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class)
    )

    val androidHealthConnectPermissionRequestContract = PermissionController.createRequestPermissionResultContract()

    val androidHealthConnectPermissionsLauncher =
        registerForActivityResult(
            androidHealthConnectPermissionRequestContract
        ) {  permissions ->
            CoroutineScope(Dispatchers.Main).launch {
                val healthConnectClient = HealthConnectClient.getOrCreate(getApplicationContext())
                val granted = healthConnectClient.permissionController.getGrantedPermissions()

                if (granted.containsAll(REQUIRED_ANDROID_HEALTH_CONNECT_PERMISSIONS)) {
                    sendAuthTokenToServer("", "", "ANDROID")
                } else {
                    val e: java.lang.Exception =  Exception("Missing Android Health Connect Permissions. ")
                    sendErrorToServer(e)
                    displayAlert(getResources().getString(R.string.mobile_track_activity_android_health_permissions_error))
                }
            }
        }

    //END_ANDROID_HEALTH_CONTENT_1

    override fun childviewCallback(string: String, data: String) {
        if(string == "sync"){
            runOnUiThread {
                removeAllSlides(
                    findViewById<FrameLayout>(R.id.activity_weeks_slide_container)
                ) { loadActivityData("sync post update") }
            }
        }else{
            var sorted_activities = listOf<ActivityGoal>();

            if(modalSortBy == string){
                if(modalSortDir == "asc"){
                    modalSortDir = "desc"
                }else{
                    modalSortDir = "asc"
                }
            }else{
                modalSortDir = "asc"
                modalSortBy = string
            }

            var activitySortImg = (findViewById<LinearLayout>(R.id.earn_points_activity_modal_activity_sort_link).getChildAt(1) as ImageView)
            var pointsSortImg = (findViewById<LinearLayout>(R.id.earn_points_activity_modal_activity_points_link).getChildAt(1) as ImageView)

            activitySortImg.setImageResource(R.drawable.sort_arrows_icon)
            pointsSortImg.setImageResource(R.drawable.sort_arrows_icon)
            activitySortImg.setTintValue(getStringVariable("PRIMARY_COLOR"))
            pointsSortImg.setTintValue(getStringVariable("PRIMARY_COLOR"))
            if(string == "default"){
                sorted_activities = activities
            }else if (string == "activity"){
                if(modalSortDir == "asc"){
                    sorted_activities = activities.sortedBy { it.name }
                    activitySortImg.setImageResource(R.drawable.sort_arrows_up)
                }else{
                    sorted_activities = activities.sortedByDescending { it.name }
                    activitySortImg.setImageResource(R.drawable.sort_arrows_down)
                }
                activitySortImg.setTintValue(getStringVariable("PRIMARY_COLOR"))
            }else if (string == "points"){
                if(modalSortDir == "asc"){
                    sorted_activities = activities.sortedBy { it.points }
                    pointsSortImg.setImageResource(R.drawable.sort_arrows_up)
                }else{
                    sorted_activities = activities.sortedByDescending { it.points }
                    pointsSortImg.setImageResource(R.drawable.sort_arrows_down)
                }
                pointsSortImg.setTintValue(getStringVariable("PRIMARY_COLOR"))
            }

            if(sorted_activities.size > 0){
                val table = findViewById<TableLayout>(R.id.activity_points_modal_table)

                while(table.children.count() > 0){
                    table.removeView(table.getChildAt(0))
                }

                loadModalActivities(sorted_activities)
            }
        }
    }

    override fun slideButtonCallback(card: Any, forward:Boolean){
        if(card == "types"){
            if(forward){
                switchTypeSlide(currentSlideIndex + 1)
            }else{
                switchTypeSlide(currentSlideIndex - 1)
            }
        } else if(card == "weeks"){
            if(forward){
                switchWeekSlide(currentWeekSlideIndex + 1)
            }else{
                switchWeekSlide(currentWeekSlideIndex - 1)
            }
        } else if(card == "points"){
            if(forward){
                switchPointsSlide(currentPointsSlideIndex + 1)
            }else{
                switchPointsSlide(currentPointsSlideIndex - 1)
            }
        } else{
            switchActivitySlide(card as FrameLayout, forward)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        recolorTheme()
        super.onCreate(savedInstanceState)
        setPageContent(R.layout.track_activity, "trackActivity")
        sendGoogleAnalytics("track_activity_view","track_activity")
        setTitle(getResources().getString(R.string.mobile_main_menu_track_activity));
        google_client_id = getStringVariable("GOOGLE_FIT_CLIENT_ID")
        google_client_secret = getStringVariable("GOOGLE_FIT_CLIENT_SECRET")
        fitbit_client_id = getStringVariable("FITBIT_CLIENT_ID")
        fitbit_client_secret = getStringVariable("FITBIT_CLIENT_SECRET")
        fitbit_redirect_uri =  getString(R.string.fitbit_login_protocol_scheme) + "://fitbitcallback"
        strava_client_id = getStringVariable("STRAVA_CLIENT_ID")
        strava_client_secret = getStringVariable("STRAVA_CLIENT_SECRET")
        strava_redirect_uri = getString(R.string.strava_login_protocol_scheme) + "://stravacallback"
        strava_redirect_uri = getString(R.string.strava_login_protocol_scheme) + "://stravacallback"

        val fitnessConnectCollapsedCard = findViewById<LinearLayout>(R.id.fitness_connect_card_collapsed)
        fitnessConnectCollapsedCard.setVisibility(View.GONE)
        val fitnessDisconnectedCard = findViewById<LinearLayout>(R.id.fitness_disconnected_card)
        fitnessDisconnectedCard.setVisibility(View.GONE)
        val fitnessConnectedCard = findViewById<LinearLayout>(R.id.fitness_connected_card)
        fitnessConnectedCard.setVisibility(View.GONE)
        setTooltipText(R.id.fitness_connect_help_button, R.string.mobile_track_activity_connect_tooltip, R.string.mobile_track_activity_connect_title)
        setTooltipText(R.id.fitness_connect_help_button_connected, R.string.mobile_track_activity_connect_tooltip, R.string.mobile_track_activity_connect_title)
        setTooltipText(R.id.fitness_activity_help_button, R.string.mobile_track_activity_activity_tooltip, R.string.mobile_track_activity_activity_title)
        setTooltipText(R.id.fitness_manual_help_button, R.string.mobile_track_activity_add_activity_tooltip, R.string.mobile_track_activity_manual_title)

        var tracking_type = getStringVariable("ACTIVITY_TRACKING_TYPE")
        if(tracking_type == "distance"){
            findViewById<TextView>(R.id.track_activity_card_unit_title).setText(
                getDistanceLabel(R.string.mobile_track_activity_activity_card_miles, R.string.mobile_track_activity_activity_card_km)
            )
        }else if (tracking_type == "minutes"){
            findViewById<TextView>(R.id.track_activity_card_unit_title).setText(R.string.mobile_track_activity_activity_card_minutes)
        }else{
            findViewById<TextView>(R.id.track_activity_card_unit_title).setText(R.string.mobile_track_activity_activity_card_points)
        }

        updateConnectIcons()

        steps_enabled = getStringVariable("ACTIVITY_TRACKING_STEPS_ENABLED") == "true"
        workout_enabled = getStringVariable("ACTIVITY_TRACKING_WORKOUTS_ENABLED") == "true"

        val activityCard = findViewById<LinearLayout>(R.id.mobile_track_activity_activity_card)
        //activityCard.setVisibility(View.GONE)
        val manualActivityCard = findViewById<LinearLayout>(R.id.manual_activity_card)
        //manualActivityCard.setVisibility(View.GONE)

        setupSlideButtons(totalSlideCount, R.id.track_activity_types_slide_buttons, "types")
        findViewById<FrameLayout>(R.id.track_activity_types_slide_container).setOnTouchListener(object : OnSwipeTouchListener(this@TrackActivity) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                switchTypeSlide(currentSlideIndex + 1)
            }
            override fun onSwipeRight() {
                super.onSwipeRight()
                switchTypeSlide(currentSlideIndex - 1)
            }
        })

        for(child in findViewById<FrameLayout>(R.id.track_activity_types_slide_container).children){
            child.setVisibility(View.INVISIBLE)
        }

        switchTypeSlide(currentSlideIndex)

        val disconnectButton = findViewById<Button>(R.id.btn_disconnect)
        disconnectButton.setOnClickListener {
            sendGoogleAnalytics("disconnect_activity_tracker","track_activity")
            disconnectPlatform()
        }

        getActivityTypes()

        var trackActivityCloseButton = findViewById<LinearLayout>(R.id.mobile_track_activity_close)
        trackActivityCloseButton.setOnClickListener {
            sendGoogleAnalytics("fitness_tracker_card_close","track_activity")
            val fitnessConnectCard = findViewById<LinearLayout>(R.id.fitness_connected_card)
            fitnessConnectCard.setVisibility(View.GONE)
            val fitnessDisconnectCard = findViewById<LinearLayout>(R.id.fitness_disconnected_card)
            fitnessDisconnectCard.setVisibility(View.GONE)
            val fitnessConnectCollapsedCard = findViewById<LinearLayout>(R.id.fitness_connect_card_collapsed)
            fitnessConnectCollapsedCard.setVisibility(View.VISIBLE)
        }
        trackActivityCloseButton = findViewById<LinearLayout>(R.id.mobile_track_activity_close_connected)
        trackActivityCloseButton.setOnClickListener {
            sendGoogleAnalytics("fitness_tracker_card_close","track_activity")
            val fitnessConnectCard = findViewById<LinearLayout>(R.id.fitness_connected_card)
            fitnessConnectCard.setVisibility(View.GONE)
            val fitnessDisconnectCard = findViewById<LinearLayout>(R.id.fitness_disconnected_card)
            fitnessDisconnectCard.setVisibility(View.GONE)
            val fitnessConnectCollapsedCard = findViewById<LinearLayout>(R.id.fitness_connect_card_collapsed)
            fitnessConnectCollapsedCard.setVisibility(View.VISIBLE)
        }

        val trackActivityExpandButton = findViewById<ImageView>(R.id.fitness_connect_expand_button)
        trackActivityExpandButton.setOnClickListener {
            sendGoogleAnalytics("fitness_tracker_card_expand","track_activity")
            updateFitnessCard()
        }

        val addManualActivityButton = findViewById<TextView>(R.id.add_manual_activity_button)
        addManualActivityButton.setOnClickListener {
            sendGoogleAnalytics("show_add_manual_activity_modal","track_activity")
            displayAlert("addManualActivity",arrayOf(workoutTypes,"track_activity"))
            setAlertSender(addManualActivityButton)
        }

        if(intent.getStringExtra("fitbit_code") != "" && intent.getStringExtra("fitbit_code") != null){
            getFitBitAuthToken()
        }else if(intent.getStringExtra("strava_code") != "" && intent.getStringExtra("strava_code") != null){
            getStravaAuthToken()
        }else{
            loadActivityData("onload")
            loadConnectCard()
        }

        findViewById<Button>(R.id.btn_resync).setOnClickListener{
            sendGoogleAnalytics("resync_activity","track_activity")
            if(apple_fit_connected == true){
                displayAlert(getResources().getString(R.string.mobile_overview_resync_ios_error));
                setAlertSender(findViewById<Button>(R.id.btn_resync))
            }else{
                displayAlert(getResources().getString(R.string.mobile_track_activity_resyncing_data));
                loadActivityData("resync")
            }
        }

        setupPointsCard()
    }

    fun setupPointsCard(){
        loadActivityPoints()
        val earn_points_card = findViewById<LinearLayout>(R.id.earn_points_card)
        if(getStringVariable("ACTIVITY_TRACKING_TYPE") == "distance" || getStringVariable("ACTIVITY_TRACKING_TYPE") == "minutes"){
            earn_points_card.setVisibility(View.GONE)
        }else{

            findViewById<Button>(R.id.activity_points_view_all).setOnClickListener(){
                sendGoogleAnalytics("show_view_all_modal","earn_points")
                isFirst = true;
                displayAlert("earnPointsViewAll")
                setAlertSender(findViewById<LinearLayout>(R.id.activity_points_view_all))
            }

            earn_points_card.setVisibility(View.VISIBLE)

            val frame = findViewById<FrameLayout>(R.id.earn_points_help_messages_layout)
            val inflater = LayoutInflater.from(this@TrackActivity)
            var i = 0;

            var strings = emptyArray<String>()

            for(message in help_strings){
                if(message != 0 && getString(message) != "") {
                    strings += getString(message)
                }
            }

            println("STRINGS: ")
            println(strings)

            totalPointsSlideCount = strings.count()

            for (string in strings) {
                val binding: EarnPointsHelpTextBinding = DataBindingUtil.inflate(
                    inflater, R.layout.earn_points_help_text ,frame, true)
                binding.colorList = getColorList("")
                val root = binding.root as LinearLayout;
                if(getStringVariable("ACTIVITY_TRACKING_POINTS_USE_HEART_LOGO") == "true"){
                    (root.getChildAt(0) as ImageView).visibility = View.GONE
                    (root.getChildAt(1) as ImageView).visibility = View.VISIBLE
                }

                (root.getChildAt(2) as TextView).setText(string)

                if(i == 0){
                    root.setVisibility(View.VISIBLE);
                } else {
                    root.setVisibility(View.INVISIBLE);
                }
                i = i + 1;
            }

            frame.setOnTouchListener(object : OnSwipeTouchListener(this@TrackActivity) {
                override fun onSwipeLeft() {
                    super.onSwipeLeft()
                    switchPointsSlide(currentPointsSlideIndex + 1)
                }
                override fun onSwipeRight() {
                    super.onSwipeRight()
                    switchPointsSlide(currentPointsSlideIndex - 1)
                }
            })

            setupSlideButtons(totalPointsSlideCount, R.id.earn_points_help_messages_slide_buttons,"points")

        }
    }

    fun switchPointsSlide(newIndex:Int){
        switchSlideButton(newIndex + 1,totalPointsSlideCount,R.id.earn_points_help_messages_slide_buttons)
        val helpMessageLayout = findViewById<FrameLayout>(R.id.earn_points_help_messages_layout)
        if((newIndex >= 0) and (newIndex < totalPointsSlideCount)){
            helpMessageLayout.getChildAt(currentPointsSlideIndex).visibility = View.INVISIBLE
            helpMessageLayout.getChildAt(newIndex).visibility = View.VISIBLE
            helpMessageLayout.getChildAt(newIndex).requestFocus()
            currentPointsSlideIndex = newIndex
        }
    }

    fun loadActivityPoints(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/getActivitiesAndPoints/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.code != 200) {
                    val response_string = response.body?.string()
                    if (response_string != null) {
                        if(response_string.contains("This user isn't connected to an activity tracker") != true) {
                            mimicThrowError(response_string)
                        }
                    }
                } else {
                    val jsonString = response.body?.string();
                    var obj = JSONObject(jsonString);
                    val data = obj.get("data").toString()
                    val jsonArray = JSONArray(data)
                    if (jsonArray.length() > 0) {
                        activities = listOf<ActivityGoal>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray[i] as JSONObject
                            var name = ""
                            if(obj.has("display_name") && obj.get("display_name") is String){
                                name = obj.get("display_name") as String
                            }

                            var type = ""
                            if(obj.has("activity_type") && obj.get("activity_type") is String){
                                type = obj.get("activity_type") as String
                            }

                            var points = ""
                            if(obj.has("points_per_hour") && obj.get("points_per_hour") is Int){
                                points = (obj.get("points_per_hour") as Int).toString() + " " + getString(R.string.mobile_earn_points_points)
                            }

                            if(points != "" && type != ""  && name != ""){
                                activities += ActivityGoal(name,type,points);
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {}
            }
        })
    }

    fun disconnectPlatform(){
        //GOOGLE_FIT_CONTENT_3
        if(google_fit_connected){
            val fitnessOptions: GoogleSignInOptionsExtension = FitnessOptions.builder()
                .addDataType(AGGREGATE_ACTIVITY_SUMMARY,FitnessOptions.ACCESS_READ)
                .addDataType(AGGREGATE_LOCATION_BOUNDING_BOX,FitnessOptions.ACCESS_READ).build()

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .addExtension(fitnessOptions)
                .requestServerAuthCode(google_client_id)
                .build()

            val mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addScope(Fitness.SCOPE_ACTIVITY_READ)
                .addScope(Fitness.SCOPE_LOCATION_READ)
                .build()

            val mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            mGoogleSignInClient.revokeAccess()
        }
        //END_GOOGLE_FIT_CONTENT_3

        val disconnectButton = findViewById<Button>(R.id.btn_disconnect)

        val url = resources.getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/disconnect")
        val formBody = FormBody.Builder()
            .add("cons_id", getConsID())
            .add("event_id",getEvent().event_id)
            .build()

        var request = Request.Builder().url(url)
            .post(formBody)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    runOnUiThread{
                        mimicThrowError(response.body?.string())
                    }
                }else{
                    platform_connected = false
                    google_fit_connected = false
                    apple_fit_connected = false
                    strava_connected = false
                    garmin_connected = false
                    android_health_connected = false
                    clearVariable("PLATFORM_CONNECTED_MONTH")
                    clearVariable("PLATFORM_CONNECTED_DAY")
                    clearVariable("PLATFORM_CONNECTED_YEAR")
                    clearVariable("PLATFORM_CONNECTED")
                    runOnUiThread{
                        updateFitnessCard()
                        loadActivityData("disconnect")
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                displayAlert(getResources().getString(R.string.mobile_track_activity_google_fit_error))
                setAlertSender(disconnectButton)
                loadActivityData("disconnect")
            }
        })
    }

    fun updateConnectIcons(){
        val googleFitButton = findViewById<ImageView>(R.id.connect_google_fit_button)
        val googleFitConnectedButton = findViewById<ImageView>(R.id.google_fit_button_connected)

        if(getStringVariable("ACTIVITY_TRACKING_GOOGLE_ENABLED") == "true"){
            googleFitButton.setVisibility(View.VISIBLE)
            googleFitConnectedButton.setVisibility(View.VISIBLE)
            //GOOGLE_FIT_CONTENT_1
            googleFitButton.setOnClickListener {
                sendGoogleAnalytics("connect_google_fit_button","track_activity")
                val fitnessOptions: GoogleSignInOptionsExtension = FitnessOptions.builder()
                    .addDataType(AGGREGATE_ACTIVITY_SUMMARY,FitnessOptions.ACCESS_READ)
                    .addDataType(AGGREGATE_LOCATION_BOUNDING_BOX,FitnessOptions.ACCESS_READ).build()

                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .addExtension(fitnessOptions)
                    .requestServerAuthCode(google_client_id)
                    .build()

                val mGoogleApiClient = GoogleApiClient.Builder(this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .addScope(Fitness.SCOPE_ACTIVITY_READ)
                    .addScope(Fitness.SCOPE_LOCATION_READ)
                    .build()

                val mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
                val signInIntent = mGoogleSignInClient.signInIntent
                startActivityForResult(signInIntent, GOOGLE_SIGN_IN)
            }
            //END_GOOGLE_FIT_CONTENT_1
        }else{
            googleFitButton.setVisibility(View.GONE)
            googleFitConnectedButton.setVisibility(View.GONE)
        }

        val fitBitButton = findViewById<ImageView>(R.id.connect_fitbit_button)
        val fitBitConnectedButton = findViewById<ImageView>(R.id.fitbit_button_connected)
        if(getStringVariable("ACTIVITY_TRACKING_FITBIT_ENABLED") == "true"){
            fitBitButton.setOnClickListener {
                sendGoogleAnalytics("fitbit_connect_button","track_activity")
                getFitBitAuthCode()
            }
            fitBitButton.setVisibility(View.VISIBLE)
            fitBitConnectedButton.setVisibility(View.VISIBLE)
        }else{
            fitBitButton.setVisibility(View.GONE)
            fitBitConnectedButton.setVisibility(View.GONE)
        }

        val stravaButton = findViewById<ImageView>(R.id.connect_strava_button)
        val stravaConnectedButton = findViewById<ImageView>(R.id.strava_button_connected)
        if(getStringVariable("ACTIVITY_TRACKING_STRAVA_ENABLED") == "true"){
            stravaButton.setVisibility(View.VISIBLE)
            stravaConnectedButton.setVisibility(View.VISIBLE)
            stravaButton.setOnClickListener{
                sendGoogleAnalytics("strava_connect_button","track_activity")
                getStravaAuthCode()
            }
        }else{
            stravaButton.setVisibility(View.GONE)
            stravaConnectedButton.setVisibility(View.GONE)
        }

        val garminButton = findViewById<ImageView>(R.id.connect_garmin_button)
        val garminConnectedButton = findViewById<ImageView>(R.id.garmin_button_connected)
        if(getStringVariable("ACTIVITY_TRACKING_GARMIN_ENABLED") == "true"){
            garminButton.setVisibility(View.VISIBLE)
            garminConnectedButton.setVisibility(View.VISIBLE)
            garminButton.setOnClickListener{
                sendGoogleAnalytics("garmin_connect_button","track_activity")
                getGarminLogin()
            }
        }else{
            garminButton.setVisibility(View.GONE)
            garminConnectedButton.setVisibility(View.GONE)
        }

        val androidHealthButton = findViewById<ImageView>(R.id.connect_android_health_button)
        val androidHealthConnectedButton = findViewById<ImageView>(R.id.android_health_button_connected)

        if(getStringVariable("ACTIVITY_TRACKING_ANDROID_HEALTH_ENABLED") == "true"){
            //ANDROID_HEALTH_CONTENT_2
            androidHealthButton.setOnClickListener {
                sendGoogleAnalytics("android_health_connect_button","track_activity")
                getAndroidHealthConnection()
            }
            //END_ANDROID_HEALTH_CONTENT_2
            androidHealthButton.setVisibility(View.VISIBLE)
            androidHealthConnectedButton.setVisibility(View.VISIBLE)
        }else{
            androidHealthButton.setVisibility(View.GONE)
            androidHealthConnectedButton.setVisibility(View.GONE)
        }

        val appleFitConnectedButton = findViewById<ImageView>(R.id.apple_fit_button_connected)

        if(getStringVariable("ACTIVITY_TRACKING_APPLE_ENABLED") == "true"){
            appleFitConnectedButton.setVisibility(View.VISIBLE)
        }else{
            appleFitConnectedButton.setVisibility(View.GONE)
        }

        val connectIconsRow = findViewById<LinearLayout>(R.id.connect_icons_row)
        val connectIconsFirstRow = findViewById<LinearLayout>(R.id.connect_icons_first_row)
        val connectIconsSecondRow = findViewById<LinearLayout>(R.id.connect_icons_second_row)
        val connectIconsThirdRow = findViewById<LinearLayout>(R.id.connect_icons_third_row)

        val connectIcons = mutableListOf<View>()

        for (i in 0 until connectIconsRow.childCount) {
            val childView = connectIconsRow.getChildAt(i)
            if(childView.visibility == View.VISIBLE){
                connectIcons.add(childView)
            }
        }

        for (view in connectIcons) {
            if(connectIconsFirstRow.childCount <2){
                connectIconsRow.removeView(view)
                connectIconsFirstRow.addView(view)
            }
            else if(connectIconsSecondRow.childCount < 2){
                connectIconsRow.removeView(view)
                connectIconsSecondRow.addView(view)

            }else{
                connectIconsRow.removeView(view)
                connectIconsThirdRow.addView(view)

            }
        }


        val connectedIconsRow = findViewById<LinearLayout>(R.id.connected_icons_row)
        val connectedIconsFirstRow = findViewById<LinearLayout>(R.id.connected_icons_first_row)
        val connectedIconsSecondRow = findViewById<LinearLayout>(R.id.connected_icons_second_row)
        val connectedIconsThirdRow = findViewById<LinearLayout>(R.id.connected_icons_third_row)

        val connectedIcons = mutableListOf<View>()

        for (i in 0 until connectedIconsRow.childCount) {
            val childView = connectedIconsRow.getChildAt(i)
            if(childView.visibility == View.VISIBLE){
                connectedIcons.add(childView)
            }
        }

        for (view in connectedIcons) {
            if(connectedIconsFirstRow.childCount <2){
                connectedIconsRow.removeView(view)
                connectedIconsFirstRow.addView(view)
            }
            else if(connectedIconsSecondRow.childCount < 2){
                connectedIconsRow.removeView(view)
                connectedIconsSecondRow.addView(view)

            }else{
                connectedIconsRow.removeView(view)
                connectedIconsThirdRow.addView(view)

            }
        }
    }

    //GOOGLE_FIT_CONTENT_2
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                getGoogleAuthToken(account.serverAuthCode as String)
            } catch (e: ApiException) {
                sendErrorToServer(e)
                displayAlert(getResources().getString(R.string.mobile_track_activity_google_fit_error))
            }
        }
    }

    fun getGoogleAuthToken(serverAuthCode: String){
        val url = "https://www.googleapis.com/oauth2/v4/token"

        val formBody = FormBody.Builder()
            .add("code", serverAuthCode)
            .add("client_secret",google_client_secret)
            .add("client_id",google_client_id)
            .add("redirect_uri","")
            .add("grant_type", "authorization_code")
            .build()

        var request = Request.Builder().url(url)
            .post(formBody)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    runOnUiThread{
                        mimicThrowError(response.body?.string())
                    }
                }else{
                    val jsonString = response.body?.string() as String
                    val jsonObject = JSONObject(jsonString);
                    var refresh_token = ""
                    if(jsonObject.has("refresh_token")){
                        sendAuthTokenToServer(jsonObject.get("access_token") as String, jsonObject.get("refresh_token") as String as String,"GOOGLE")
                    }else{
                        google_fit_connected = true
                        disconnectPlatform()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                displayAlert(getResources().getString(R.string.mobile_track_activity_google_fit_error))
                setAlertSender(findViewById(R.id.connect_google_fit_button))
            }
        })
    }
    //END_GOOGLE_FIT_CONTENT_2

    fun getActivityTypes(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/getWorkoutTypes/").plus(getEvent().event_id)
        var client = OkHttpClient();
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val jsonString = response.body?.string()
                val obj = JSONObject(jsonString)
                if(obj.has("data")){
                    val data = obj.get("data")
                    val jsonTypesArray = data as JSONArray
                    if(jsonTypesArray.length() > 0) {
                        for (i in 0..jsonTypesArray.length() - 1) {
                            var name = (jsonTypesArray[i] as JSONObject).get("display_name") as String
                            var image = "";
                            var internal_name = "";
                            var distance_enabled = false;
                            if((jsonTypesArray[i] as JSONObject).has("image_url") && (jsonTypesArray[i] as JSONObject).get("image_url") is String){
                                image = (jsonTypesArray[i] as JSONObject).get("image_url") as String
                            }
                            if((jsonTypesArray[i] as JSONObject).has("allow_distance") && (jsonTypesArray[i] as JSONObject).get("allow_distance") is Int){
                                distance_enabled = (jsonTypesArray[i] as JSONObject).get("allow_distance") as Int == 1
                            }
                            if((jsonTypesArray[i] as JSONObject).has("workout_type") && (jsonTypesArray[i] as JSONObject).get("workout_type") is String){
                                internal_name = (jsonTypesArray[i] as JSONObject).get("workout_type") as String
                            }
                            workoutTypes += WorkoutType(name, internal_name, image,distance_enabled)
                        }
                        workoutTypes = workoutTypes.sortedBy{ it.name }.toTypedArray()
                    }else{
                        workoutTypes = arrayOf(WorkoutType("Run","","",false),WorkoutType("Walk","","",false))
                    }
                }else{
                    workoutTypes = arrayOf(WorkoutType("Run","","",false),WorkoutType("Walk","","",false))
                }

            }

            override fun onFailure(call: Call, e: IOException) {
                println("ERROR GETTING ACTIVITY TYPES")
                println(e.message.toString())
            }
        })
    }

    fun sendAuthTokenToServer(authToken: String, refreshToken: String, activity_platform: String){
        val url = resources.getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/addUserActivityPlatform")

        clearVariable("PLATFORM_CONNECTED")

        var token_name = ""
        var refresh_token_name = ""
        if(activity_platform == "GOOGLE"){
            token_name = "google_fit_access_token"
            refresh_token_name = "google_refresh_token"
        }else if (activity_platform == "FITBIT"){
            token_name = "fitbit_access_token"
            refresh_token_name = "fitbit_refresh_token"
        }else if (activity_platform == "STRAVA"){
            token_name = "strava_access_token"
            refresh_token_name = "strava_refresh_token"
        }

        val formBody = FormBody.Builder()
            .add("cons_id", getConsID())
            .add("event_id",getEvent().event_id)
            .add("activity_platform",activity_platform)
            .add(token_name,authToken)
            .add(refresh_token_name,refreshToken)
            .build()

        var request = Request.Builder().url(url)
            .post(formBody)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    if(response.body?.string()?.contains("This user isn't connected to an activity tracker") != true) {
                        mimicThrowError(response.body?.string())
                    }else{
                        loadConnectCard()
                        loadActivityData("sync")
                    }
                }else{
                    val jsonString = response.body?.string() as String
                    val jsonObject = JSONObject(jsonString);
                    if(jsonObject.has("success")){
                        if(jsonObject.get("success") == true){
                            platform_connected = true;
                            setVariable("PLATFORM_CONNECTED","true")
                            if(activity_platform == "GOOGLE"){
                                google_fit_connected = true;
                            }else if (activity_platform == "FITBIT"){
                                fitbit_connected = true;
                            }else if (activity_platform == "STRAVA"){
                                strava_connected = true;
                            }else if (activity_platform == "ANDROID"){
                                android_health_connected = true;
                            }
                            runOnUiThread{
                                loadConnectCard()
                                loadActivityData("sync")
                            }
                        }else{
                            displayAlert(getResources().getString(R.string.mobile_track_activity_google_fit_error))
                        }
                    }else{
                        displayAlert(getResources().getString(R.string.mobile_track_activity_google_fit_error))
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                displayAlert(getResources().getString(R.string.mobile_track_activity_google_fit_error))
            }
        })
    }

    fun sha256Hash(string: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = string.toByteArray(Charsets.UTF_8)
        val bytes = md.digest(input)
        return Base64.encodeToString(bytes, Base64.NO_PADDING or Base64.NO_WRAP).replace("+","-").replace("/","_")
    }

    fun generateFitbitVariables(){
        fitbit_state = getRandomString(32)
        fitbit_pkce = getRandomString(64)
        fitbit_challenge = sha256Hash(fitbit_pkce).replace("+","-")
        setVariable("FITBIT_PKCE",fitbit_pkce)
    }

    fun base64Encode(token: String): String? {
        val encodedBytes: ByteArray = Base64.encode(token.toByteArray(), Base64.NO_PADDING or Base64.NO_WRAP)
        return String(encodedBytes, Charset.forName("UTF-8"))
    }

    fun getRandomString(length: Int) : String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    fun getFitBitAuthToken(){
        val code = intent.getStringExtra("fitbit_code").toString();
        val basic_auth = base64Encode(fitbit_client_id + ":" + fitbit_client_secret)
        val url = "https://api.fitbit.com/oauth2/token?redirect_uri=" + fitbit_redirect_uri
        val formBody = FormBody.Builder()
            .add("client_id",fitbit_client_id)
            .add("redirect_uri",fitbit_redirect_uri)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("code_verifier",getStringVariable("FITBIT_PKCE"))
            .build()

        var request = Request.Builder().url(url)
            .post(formBody)
            .addHeader("Authorization" , "Basic ".plus(basic_auth))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    runOnUiThread{
                        mimicThrowError(response.body?.string())
                    }
                }else{
                    val jsonString = response.body?.string() as String
                    val jsonObject = JSONObject(jsonString);
                    println(jsonObject)
                    sendAuthTokenToServer(jsonObject.get("access_token") as String, jsonObject.get("refresh_token") as String,"FITBIT")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                displayAlert(getResources().getString(R.string.mobile_track_activity_google_fit_error))
                setAlertSender(findViewById(R.id.connect_fitbit_button))
            }
        })
    }

    fun getFitBitAuthCode(){
        generateFitbitVariables()
        val url = "https://www.fitbit.com/oauth2/authorize?response_type=code&client_id=" + fitbit_client_id + "&scope=activity+location&code_challenge=" + fitbit_challenge + "&code_challenge_method=S256&state=" + fitbit_state + "&redirect_uri=" + fitbit_redirect_uri
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    //ANDROID_HEALTH_CONTENT_3

    private fun checkAndroidHealthConnectPermission() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val healthConnectClient = HealthConnectClient.getOrCreate(getApplicationContext())
                val granted = healthConnectClient.permissionController.getGrantedPermissions()

                if (granted.containsAll(REQUIRED_ANDROID_HEALTH_CONNECT_PERMISSIONS)) {
                    sendAuthTokenToServer("", "", "ANDROID")
                } else {
                    androidHealthConnectPermissionsLauncher.launch(REQUIRED_ANDROID_HEALTH_CONNECT_PERMISSIONS)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                sendErrorToServer(e)
                displayAlert(getResources().getString(R.string.mobile_track_activity_android_health_error))
            }
        }
    }

    fun getAndroidHealthConnection(){
        val availabilityStatus = HealthConnectClient.getSdkStatus(getApplicationContext())
        if (availabilityStatus == HealthConnectClient.SDK_AVAILABLE) {
            checkAndroidHealthConnectPermission()
        }else{
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
            )
            startActivity(intent)
        }
    }

    fun getAllExerciseTypes(): Map<Int, String> {
        val exerciseSessionClass = ExerciseSessionRecord::class.java

        // Filter for static fields that start with "EXERCISE_TYPE_"
        return exerciseSessionClass.declaredFields
            .filter { field ->
                Modifier.isStatic(field.modifiers) &&
                        field.name.startsWith("EXERCISE_TYPE_") &&
                        field.type == Int::class.java
            }
            .associate { field ->
                field.isAccessible = true
                val value = field.get(null) as Int
                value to field.name.replace("EXERCISE_TYPE_", "")
            }
    }

    fun syncAndroidHealthData(type: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val healthConnectClient = HealthConnectClient.getOrCreate(getApplicationContext())

                var start_time = platform_connected_time
                if(type == "sync" && platform_sync_time != ""){
                    start_time = platform_sync_time
                }

                val ldt = LocalDateTime.parse(start_time.substring(0,19))
                val startTime = ldt.toInstant(ZoneOffset.UTC)
                var current_sync_date_start = ldt.toInstant(ZoneOffset.UTC)
                var current_sync_date_end = ldt.toInstant(ZoneOffset.UTC)

                val endTime = Instant.now()

                val workouts_response =
                    healthConnectClient.readRecords(
                        ReadRecordsRequest(
                            ExerciseSessionRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                        )
                    )

                val exercise_types = getAllExerciseTypes()

                for (woRecord in workouts_response.records) {
                    var distance: Double = 0.0

                    val distanceRecord =
                        healthConnectClient.aggregate(
                            AggregateRequest(
                                setOf(DistanceRecord.DISTANCE_TOTAL),
                                TimeRangeFilter.between(woRecord.startTime, woRecord.endTime)
                            )
                        )

                    distance = distanceRecord[DistanceRecord.DISTANCE_TOTAL]?.inMiles ?: 0.0

                    val url = resources.getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/addWorkout")

                    val formBody = FormBody.Builder()
                        .add("cons_id", getConsID())
                        .add("event_id",getEvent().event_id)
                        .add("vendor","ANDROID")
                        .add("vendor_id", woRecord.metadata.id.toString())
                        .add("start_time", woRecord.startTime.toString())
                        .add("end_time", woRecord.endTime.toString())
                        .add("vendor_activity_type", exercise_types[woRecord.exerciseType].toString())
                        .add("distance", distance.toString())

                    if(getStringVariable("ACTIVITY_TRACKING_DISTANCE_METRIC") != ""){
                        formBody.add("metric", getStringVariable("ACTIVITY_TRACKING_DISTANCE_METRIC"))
                    }

                    var request = Request.Builder().url(url)
                        .post(formBody.build())
                        .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                        .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                        .build()

                    val client = OkHttpClient()
                    client.newCall(request).enqueue(object : Callback {
                        override fun onResponse(call: Call, response: Response) {
                            if(response.code != 200){
                                runOnUiThread{
                                    mimicThrowError(response.body?.string())
                                }
                            }
                        }

                        override fun onFailure(call: Call, e: IOException) {
                            println(e.message.toString())
                            sendErrorToServer(e)
                            displayAlert(getResources().getString(R.string.mobile_track_activity_android_health_error))
                        }
                    })
                }

                while (current_sync_date_start <= endTime){
                    current_sync_date_end =  current_sync_date_end.plus(1, ChronoUnit.DAYS) // Add one day

                    val stepRecord =
                        healthConnectClient.aggregate(
                            AggregateRequest(
                                setOf(StepsRecord.COUNT_TOTAL),
                                TimeRangeFilter.between(current_sync_date_start, current_sync_date_end)
                            )
                        )

                    val url = resources.getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/addSteps")

                    if(stepRecord[StepsRecord.COUNT_TOTAL] != null){
                        val formBody = FormBody.Builder()
                            .add("cons_id", getConsID())
                            .add("event_id",getEvent().event_id)
                            .add("vendor","ANDROID")
                            .add("date", current_sync_date_start.toString())
                            .add("steps", stepRecord[StepsRecord.COUNT_TOTAL].toString())
                            .build()

                        var request = Request.Builder().url(url)
                            .post(formBody)
                            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                            .build()

                        val client = OkHttpClient()
                        client.newCall(request).enqueue(object : Callback {
                            override fun onResponse(call: Call, response: Response) {
                                if(response.code != 200){
                                    runOnUiThread{
                                        mimicThrowError(response.body?.string())
                                    }
                                }
                            }

                            override fun onFailure(call: Call, e: IOException) {
                                println(e.message.toString())
                                sendErrorToServer(e)
                                displayAlert(getResources().getString(R.string.mobile_track_activity_android_health_error))
                            }
                        })
                    }

                    current_sync_date_start =  current_sync_date_start.plus(1, ChronoUnit.DAYS) // Add one day
                }

                platform_sync_time = endTime.toString()

                val url = resources.getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/setLastSyncTime")

                val formBody = FormBody.Builder()
                    .add("cons_id", getConsID())
                    .add("last_sync_time",platform_sync_time)
                    .add("activity_platform","ANDROID")
                    .build()

                var request = Request.Builder().url(url)
                    .post(formBody)
                    .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                    .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                    .build()

                val client = OkHttpClient()
                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        if(response.code != 200){
                            runOnUiThread{
                                mimicThrowError(response.body?.string())
                            }
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        println(e.message.toString())
                        sendErrorToServer(e)
                        displayAlert(getResources().getString(R.string.mobile_track_activity_android_health_error))
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //END_ANDROID_HEALTH_CONTENT_3

    fun getStravaAuthCode(){
        val intentUri = Uri.parse("https://www.strava.com/oauth/mobile/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", strava_client_id)
            .appendQueryParameter("redirect_uri", strava_redirect_uri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("approval_prompt", "auto")
            .appendQueryParameter("scope", "activity:read_all")
            .build()

        println("INTENT URI")
        println(intentUri)


        val intent = Intent(Intent.ACTION_VIEW, intentUri)
        startActivity(intent)
    }

    fun getStravaAuthToken(){
        val code = intent.getStringExtra("strava_code").toString();
        val url = "https://www.strava.com/oauth/token"
        val formBody = FormBody.Builder()
            .add("client_id",strava_client_id)
            .add("client_secret",strava_client_secret)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .build()

        var request = Request.Builder().url(url)
            .post(formBody)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    println("ERROR IN STRAVA CODE")
                    runOnUiThread{
                        mimicThrowError(response.body?.string())
                    }
                }else{
                    val jsonString = response.body?.string() as String
                    val jsonObject = JSONObject(jsonString);
                    println(jsonObject)
                    sendAuthTokenToServer(jsonObject.get("access_token") as String, jsonObject.get("refresh_token") as String,"STRAVA")
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                displayAlert(getResources().getString(R.string.mobile_track_activity_google_fit_error))
                setAlertSender(findViewById(R.id.connect_fitbit_button))
            }
        })
    }


    fun getGarminLogin(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/garminUrl/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    runOnUiThread{
                        mimicThrowError(response.body?.string())
                    }
                }else{
                    val jsonString = response.body?.string();
                    var obj = JSONObject(jsonString);
                    var garmin_login_url = ""

                    if(obj.has("url")){
                        garmin_login_url = obj.get("url").toString()
                    }

                    runOnUiThread {
                        if(garmin_login_url != ""){
                            val webView = findViewById<WebView>(R.id.garmin_login_webview)
                            webView.setVisibility(View.VISIBLE)
                            webView.settings.setJavaScriptEnabled(true)
                            webView.settings.setDomStorageEnabled(true);

                            webView.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                    if (url != null) {
                                        view?.loadUrl(url)
                                    }
                                    return true
                                }

                                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                    if (url != null) {
                                        if(url.contains("garminauth://true")){
                                            webView.loadUrl("about:blank")
                                            webView.setVisibility(View.GONE)
                                            hideAlert()
                                            loadConnectCard()
                                            loadActivityData("resync")
                                        }
                                    }
                                    super.doUpdateVisitedHistory(view, url, isReload)
                                }
                            }
                            webView.loadUrl(garmin_login_url)
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {}
            }
        })
    }

    fun updateFitnessCard(){
        if(platform_connected){
            if(!google_fit_connected){
                (findViewById<ImageView>(R.id.google_fit_button_connected) as ImageView).setTint(Color.LTGRAY)
            }else{
                (findViewById<ImageView>(R.id.google_fit_button_connected) as ImageView).removeTint()
            }

            if(!fitbit_connected){
                (findViewById<ImageView>(R.id.fitbit_button_connected) as ImageView).setTint(Color.LTGRAY)
            }else{
                (findViewById<ImageView>(R.id.fitbit_button_connected) as ImageView).removeTint()
            }

            if(!strava_connected){
                (findViewById<ImageView>(R.id.strava_button_connected) as ImageView).setTint(Color.LTGRAY)
            }else{
                (findViewById<ImageView>(R.id.strava_button_connected) as ImageView).removeTint()
            }

            if(!garmin_connected){
                (findViewById<ImageView>(R.id.garmin_button_connected) as ImageView).setTint(Color.LTGRAY)
            }else{
                (findViewById<ImageView>(R.id.garmin_button_connected) as ImageView).removeTint()
            }

            if(!android_health_connected){
                (findViewById<ImageView>(R.id.android_health_button_connected) as ImageView).setTint(Color.LTGRAY)
            }else{
                (findViewById<ImageView>(R.id.android_health_button_connected) as ImageView).removeTint()
            }

            if(!apple_fit_connected){
                (findViewById<ImageView>(R.id.apple_fit_button_connected) as ImageView).setVisibility(View.GONE)
            }else{
                (findViewById<ImageView>(R.id.apple_fit_button_connected) as ImageView).setColorFilter(null)
            }

            val connectedIconsRow = findViewById<LinearLayout>(R.id.connected_icons_row)
            val connectedIconsFirstRow = findViewById<LinearLayout>(R.id.connected_icons_first_row)
            val connectedIconsSecondRow = findViewById<LinearLayout>(R.id.connected_icons_second_row)
            val connectedIconsThirdRow = findViewById<LinearLayout>(R.id.connected_icons_third_row)

            val connectedIcons = mutableListOf<View>()

            for (i in 0 until connectedIconsRow.childCount) {
                val childView = connectedIconsRow.getChildAt(i)
                connectedIcons.add(childView)
            }

            for (i in 0 until connectedIconsFirstRow.childCount) {
                val childView = connectedIconsFirstRow.getChildAt(i)
                connectedIcons.add(childView)
            }

            for (i in 0 until connectedIconsSecondRow.childCount) {
                val childView = connectedIconsSecondRow.getChildAt(i)
                connectedIcons.add(childView)
            }

            for (i in 0 until connectedIconsThirdRow.childCount) {
                val childView = connectedIconsThirdRow.getChildAt(i)
                connectedIcons.add(childView)
            }

            connectedIconsRow.removeAllViews()
            connectedIconsFirstRow.removeAllViews()
            connectedIconsSecondRow.removeAllViews()
            connectedIconsThirdRow.removeAllViews()

            for (view in connectedIcons) {
                if (view.visibility == View.VISIBLE) {
                    if (connectedIconsFirstRow.childCount < 2) {
                        connectedIconsFirstRow.addView(view)
                    } else if (connectedIconsSecondRow.childCount < 2) {
                        connectedIconsSecondRow.addView(view)
                    } else {
                        connectedIconsThirdRow.addView(view)
                    }
                }else{
                    connectedIconsRow.addView(view)
                }
            }

            val fitnessDisconnectCard = findViewById<LinearLayout>(R.id.fitness_disconnected_card)
            fitnessDisconnectCard.setVisibility(View.GONE)
            val fitnessConnectCard = findViewById<LinearLayout>(R.id.fitness_connected_card)
            fitnessConnectCard.setVisibility(View.VISIBLE)

        }else{
            val fitnessDisconnectCard = findViewById<LinearLayout>(R.id.fitness_disconnected_card)
            fitnessDisconnectCard.setVisibility(View.VISIBLE)
            val fitnessConnectCard = findViewById<LinearLayout>(R.id.fitness_connected_card)
            fitnessConnectCard.setVisibility(View.GONE)
        }
        val fitnessConnectCollapsedCard = findViewById<LinearLayout>(R.id.fitness_connect_card_collapsed)
        fitnessConnectCollapsedCard.setVisibility(View.GONE)
    }

    fun loadConnectCard(){
        val url = resources.getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/connection/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    platform_connected = false;
                    clearVariable("PLATFORM_CONNECTED")
                    runOnUiThread{
                        updateFitnessCard()
                    }
                    //throw Exception(response.body?.string())
                }else{
                    val jsonString = response.body?.string() as String
                    val jsonObject = JSONObject(jsonString);
                    runOnUiThread() {
                        if (jsonObject.has("success")) {
                            if (jsonObject.get("success") == true) {
                                platform_connected = true;
                                setVariable("PLATFORM_CONNECTED","true")
                                val data = jsonObject.get("data");
                                var platform = "";
                                var connected_time = "";
                                if(jsonObject.getJSONObject("data").has("activity_platform") && jsonObject.getJSONObject("data").get("activity_platform") is String)
                                {
                                    platform = jsonObject.getJSONObject("data").get("activity_platform") as String
                                }

                                if(jsonObject.getJSONObject("data").has("connected_time") && jsonObject.getJSONObject("data").get("connected_time") is String)
                                {
                                    platform_connected_time = (jsonObject.getJSONObject("data").get("connected_time") as String).substring(0,19);

                                    if(jsonObject.getJSONObject("data").has("last_sync_time") && jsonObject.getJSONObject("data").get("last_sync_time") is String){
                                        platform_sync_time =  (jsonObject.getJSONObject("data").get("last_sync_time")  as String).substring(0,19);
                                    }

                                    connected_time = (jsonObject.getJSONObject("data").get("connected_time") as String).substring(0,10);
                                    val connected_date = LocalDate.parse(connected_time);
                                    val connected_string = (connected_date.monthValue).toString() + "/" + connected_date.dayOfMonth + "/" + connected_date.year.toString().substring(2,4)
                                    setVariable("PLATFORM_CONNECTED_MONTH", (connected_date.monthValue).toString())
                                    setVariable("PLATFORM_CONNECTED_DAY", (connected_date.dayOfMonth).toString())
                                    setVariable("PLATFORM_CONNECTED_YEAR", (connected_date.year).toString())

                                    val string = getString(R.string.mobile_track_activity_connected_connected_time_android).replace("XXXXXXX",connected_string)
                                    findViewById<TextView>(R.id.mobile_track_activity_connected_time).setText(string)
                                    findViewById<TextView>(R.id.mobile_track_activity_connected_time).setVisibility(View.VISIBLE)
                                }else{
                                    findViewById<TextView>(R.id.mobile_track_activity_connected_time).setVisibility(View.GONE)
                                }


                                if(platform == "GOOGLE"){
                                    google_fit_connected = true
                                }
                                else if(platform == "APPLE"){
                                    apple_fit_connected = true
                                }
                                else if(platform == "STRAVA"){
                                    strava_connected = true
                                }
                                else if(platform == "GARMIN"){
                                    garmin_connected = true
                                }
                                else if(platform == "FITBIT"){
                                    fitbit_connected = true
                                }else if(platform == "ANDROID"){
                                    android_health_connected = true
                                }else{
                                    platform_connected = false
                                    clearVariable("PLATFORM_CONNECTED")
                                }
                                runOnUiThread{
                                    updateFitnessCard()
                                }
                            } else {
                                platform_connected = false;
                                clearVariable("PLATFORM_CONNECTED")
                                runOnUiThread{
                                    updateFitnessCard()
                                }
                            }
                        } else {
                            platform_connected = false;
                            clearVariable("PLATFORM_CONNECTED")
                            runOnUiThread{
                                updateFitnessCard()
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

    fun loadActivityData(source: String){
        try {
            findViewById<TextView>(R.id.mobile_track_activity_no_activity).setVisibility(View.GONE)
            val syncing = source == "resync" || source == "sync" || source == "sync post update"
            var url = getResources().getString(R.string.base_server_url).plus("/")
                .plus(getStringVariable("CLIENT_CODE")).plus("/activity/tracking/")
                .plus(getConsID()).plus("/").plus(getEvent().event_id)

            if (platform_connected) {
                if (source == "sync") {
                    if(android_health_connected){
                        //ANDROID_HEALTH_CONTENT_4
                        syncAndroidHealthData("sync")
                        //END_ANDROID_HEALTH_CONTENT_4
                    }else{
                        url = getResources().getString(R.string.base_server_url).plus("/")
                            .plus(getStringVariable("CLIENT_CODE")).plus("/activity/sync/")
                            .plus(getConsID()).plus("/").plus(getEvent().event_id)
                    }
                } else if (source == "resync") {
                    if(android_health_connected){
                        //ANDROID_HEALTH_CONTENT_5
                        syncAndroidHealthData("resync")
                        //END_ANDROID_HEALTH_CONTENT_5
                    }else{
                        url = getResources().getString(R.string.base_server_url).plus("/")
                            .plus(getStringVariable("CLIENT_CODE")).plus("/activity/resync/")
                            .plus(getConsID()).plus("/").plus(getEvent().event_id)
                    }
                }

            }

            val container = findViewById<FrameLayout>(R.id.activity_weeks_slide_container);
            totalWeekSlideCount = 0;
            runOnUiThread {
                container.setVisibility(View.GONE)
                for (childView in container.children) {
                    container.removeView(childView);
                }
                currentWeekSlideIndex = 0;
            }

            var request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()

            var client = OkHttpClient();

            client.newCall(request).enqueue(object : Callback {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call, response: okhttp3.Response) {
                    if (response.code != 200) {
                        setVariable("TRACKING_STARTED", "false")
                        if(response.code == 401){
                            disconnectPlatform()
                        }
                        if(response.body?.string()?.contains("This user isn't connected to an activity tracker") != true) {
                            showNoActivityErrorText()
                        }else{
                            showNoActivityText()
                        }
                    } else {
                        val jsonString = response.body?.string()!!;
                        val obj = JSONObject(jsonString)

                        val connected_time = getSafeStringVariable(obj, "connected_time")
                        if(connected_time == ""){
                            clearVariable("TRACKING_STARTED_MONTH")
                            clearVariable("TRACKING_STARTED_DAY")
                            clearVariable("TRACKING_STARTED_YEAR")
                            setVariable("TRACKING_STARTED", "false")
                        }else{
                            val connected_date = LocalDate.parse(connected_time);
                            setVariable("TRACKING_STARTED_MONTH", (connected_date.monthValue).toString())
                            setVariable("TRACKING_STARTED_DAY", (connected_date.dayOfMonth).toString())
                            setVariable("TRACKING_STARTED_YEAR", (connected_date.year).toString())
                            setVariable("TRACKING_STARTED", "true")
                        }
                        if (syncing) {
                            loadActivityData("after_sync")
                        } else {
                            weeks = listOf<Week>();
                            totalWeekSlideCount = 0;
                            try {
                                if (obj.has("data")) {
                                    val data = obj.get("data").toString()
                                    val jsonArray = JSONArray(data)
                                    if (jsonArray.length() > 0) {
                                        for (i in 0 until jsonArray.length()) {
                                            val week = jsonArray[i] as JSONArray;
                                            var week_days = listOf<Day>()
                                            for (j in 0 until week.length()) {
                                                val day = week[j] as JSONObject
                                                var points = 0;
                                                var miles = 0.00;
                                                var minutes = 0.00;

                                                if (day.has("number_points")) {
                                                    points = day.get("number_points") as Int
                                                }

                                                if (day.has("total_distance")) {
                                                    if(day.get("total_distance") is Int){
                                                        miles = (day.get("total_distance") as Int).toDouble()
                                                    }else if(day.get("total_distance") is Double) {
                                                        miles = day.get("total_distance") as Double
                                                    }
                                                }

                                                if (day.has("total_minutes")) {
                                                    if(day.get("total_minutes") is Int){
                                                        minutes = (day.get("total_minutes") as Int).toDouble()
                                                    }else if(day.get("total_distance") is Double) {
                                                        minutes = day.get("total_minutes") as Double
                                                    }
                                                }

                                                week_days += (Day(
                                                    day.get("date") as String,
                                                    day.get("total_steps") as Int,
                                                    day.get("number_workouts") as Int,
                                                    points,
                                                    miles,
                                                    minutes
                                                ))
                                            }

                                            var week_name = "";

                                            if (week.length() > 0) {
                                                week_name =
                                                    (week[0] as JSONObject).get("date") as String + " " + getResources().getString(
                                                        R.string.mobile_track_activity_activity_card_date_through_android
                                                    ) + " " + (week[week.length() - 1] as JSONObject).get(
                                                        "date"
                                                    ) as String
                                            }

                                            if (i == 0) {
                                                week_name =
                                                    getResources().getString(R.string.mobile_track_activity_activity_this_week)
                                            }

                                            weeks += (Week(
                                                week_name,
                                                week_days
                                            ))
                                        }

                                        runOnUiThread {
                                            totalWeekSlideCount = weeks.size
                                            loadWeeklyActivty();
                                            findViewById<LinearLayout>(R.id.mobile_track_activity_activity_card).setVisibility(
                                                View.VISIBLE
                                            )
                                            findViewById<LinearLayout>(R.id.manual_activity_card).setVisibility(
                                                View.VISIBLE
                                            )
                                        }
                                    } else {
                                        showNoActivityText()
                                    }
                                } else {
                                    showNoActivityText()
                                    println("No data was returned for the activity data")
                                    //reloadPage()
                                }
                            } catch (e: Exception) {
                                println("There was an exception loading the activity data")
                                showNoActivityErrorText()
                            }
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println(e.message.toString());
                    reloadPage()
                }
            })
        }catch(e: Exception){
            showNoActivityErrorText()
        }
    }

    fun showNoActivityText(){
        runOnUiThread{
            findViewById<TextView>(R.id.mobile_track_activity_no_activity).setVisibility(View.VISIBLE)
            findViewById<TextView>(R.id.mobile_track_activity_no_activity).setText(getString(R.string.mobile_track_activity_no_activities))
            findViewById<FrameLayout>(R.id.activity_weeks_slide_container).setVisibility(View.GONE)
        }
    }

    fun showNoActivityErrorText(){
        runOnUiThread{
            findViewById<TextView>(R.id.mobile_track_activity_no_activity).setVisibility(View.VISIBLE)
            findViewById<TextView>(R.id.mobile_track_activity_no_activity).setText(getString(R.string.mobile_track_activity_activities_error))
            findViewById<FrameLayout>(R.id.activity_weeks_slide_container).setVisibility(View.GONE)
        }
    }

    fun loadDailyActivities(dateName: String, row_image: ImageView, row_expanded_container: LinearLayout){
        ((row_expanded_container.getChildAt(0) as LinearLayout).getChildAt(0) as TextView).setText("1")
        val name = dateName.replace("/","-")
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/day/").plus(getConsID()).plus("/").plus(getEvent().event_id).plus("/").plus(name)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                val jsonString = response.body?.string();
                val obj = JSONObject(jsonString)
                if(obj.has("data")){
                    val data = obj.get("data").toString()
                    val jsonArray = JSONArray(data)
                    val inflater = LayoutInflater.from(this@TrackActivity)
                    runOnUiThread {
                        if (jsonArray.length() > 0) {
                            for (i in 0..jsonArray.length() - 1) {
                                val wk = jsonArray[i] as JSONObject

                                var miles = wk.get("distance");
                                miles = toDouble(miles)

                                var vendor_id = "0"
                                if(wk.has("vendor_id") && wk.get("vendor_id") is String){
                                    vendor_id = wk.get("vendor_id") as String
                                }

                                val parent = row_expanded_container.getChildAt(1) as FrameLayout

                                val binding: ExpandedActivityRowBinding = DataBindingUtil.inflate(
                                    inflater, R.layout.expanded_activity_row, parent, true
                                )
                                binding.colorList = getColorList("")

                                val expanded_row = binding.root as LinearLayout
                                var image_url = "";
                                if(wk.has("activity_type_image_url") && wk.get("activity_type_image_url") is String){
                                    image_url = wk.get("activity_type_image_url") as String
                                }

                                val activity = Activity(
                                    dateName,
                                    wk.get("vendor") as String,
                                    vendor_id,
                                    wk.get("steps") as Int,
                                    wk.get("minutes") as Int,
                                    miles,
                                    wk.get("points") as Int,
                                    wk.get("activity_type") as String,
                                    wk.get("activity_type_display_name") as String,
                                    image_url
                                )

                                val imageContainer = expanded_row.getChildAt(0) as LinearLayout;
                                (imageContainer.getChildAt(0) as ImageView).visibility = View.GONE

                                if(image_url != ""){
                                    Glide.with(this@TrackActivity)
                                        .load(image_url)
                                        .into((imageContainer.getChildAt(0) as ImageView))
                                    (imageContainer.getChildAt(0) as ImageView).visibility = View.VISIBLE
                                }

                                (imageContainer.getChildAt(1) as TextView).setText(activity.activity_type_display_name.capitalize())
                                val statsContainer = expanded_row.getChildAt(1) as LinearLayout;
                                val linksContainer = (expanded_row.getChildAt(2) as LinearLayout)
                                if(wk.get("can_modify") != true && wk.get("can_modify") != "true"){
                                    linksContainer.setVisibility(View.GONE);
                                }else{
                                    linksContainer.getChildAt(0).setOnClickListener{
                                        sendGoogleAnalytics("show_edit_manual_activity_modal","track_activity")
                                        displayAlert("editManualActivity", arrayOf(workoutTypes, activity, row_image, row_expanded_container))
                                    }
                                    linksContainer.getChildAt(2).setOnClickListener{
                                        sendGoogleAnalytics("delete_manual_activity","track_activity")
                                        displayAlert("deleteManualActivity", activity)
                                    }
                                }

                                if (wk.get("record_type") == "steps") {
                                    ((statsContainer.getChildAt(0) as LinearLayout).getChildAt(0) as TextView).text =
                                        activity.steps.toString();
                                    statsContainer.getChildAt(1).setVisibility(View.GONE)
                                    statsContainer.getChildAt(2).setVisibility(View.GONE)
                                    statsContainer.getChildAt(3).setVisibility(View.GONE)
                                    statsContainer.getChildAt(4).setVisibility(View.GONE)
                                    val stepsImageUrl = "https://nt-dev-clients.s3.amazonaws.com/global/activity_tracking/icons/steps.png"
                                    Glide.with(this@TrackActivity)
                                        .load(stepsImageUrl)
                                        .into((imageContainer.getChildAt(0) as ImageView))
                                    (imageContainer.getChildAt(0) as ImageView).visibility = View.VISIBLE
                                    ((statsContainer.getChildAt(6) as LinearLayout).getChildAt(0) as TextView).text =
                                        activity.points.toString()
                                } else if (wk.get("record_type") == "workout") {
                                    statsContainer.getChildAt(0).setVisibility(View.GONE)
                                    statsContainer.getChildAt(1).setVisibility(View.GONE)
                                    ((statsContainer.getChildAt(2) as LinearLayout).getChildAt(0) as TextView).text =
                                        activity.minutes.toString() + " " + getResources().getString(
                                            R.string.mobile_track_activity_activity_card_details_min
                                        )
                                    if(activity.miles > 0.0){
                                        ((statsContainer.getChildAt(4) as LinearLayout).getChildAt(0) as TextView).text =
                                            activity.miles.toString() + " " + getDistanceLabel(R.string.mobile_track_activity_activity_card_details_mi, R.string.mobile_track_activity_activity_card_details_km)
                                    }else{
                                        statsContainer.getChildAt(3).setVisibility(View.GONE)
                                        statsContainer.getChildAt(4).setVisibility(View.GONE)
                                    }
                                }
                                if(getStringVariable("ACTIVITY_TRACKING_TYPE") == "distance" || getStringVariable("ACTIVITY_TRACKING_TYPE") == "minutes"){
                                    (statsContainer.getChildAt(5) as View).setVisibility(View.GONE);
                                    (statsContainer.getChildAt(6) as LinearLayout).setVisibility(View.GONE)
                                }else{
                                    ((statsContainer.getChildAt(6) as LinearLayout).getChildAt(0) as TextView).text =
                                        activity.points.toString()
                                }

                                if(i>0){
                                    expanded_row.setVisibility(View.GONE)
                                }
                            }

                            row_image.setImageResource(R.drawable.minus_circle_icon)
                            row_image.setColorFilter(Color.parseColor(getStringVariable("PRIMARY_COLOR")))
                            row_expanded_container.setVisibility(View.VISIBLE)
                            setupVariableSlideButtons(jsonArray.length(),row_expanded_container.getChildAt(2) as LinearLayout,row_expanded_container.getChildAt(1) as FrameLayout)
                        } else {
                            row_image.setVisibility(View.INVISIBLE);
                            row_expanded_container.setVisibility(View.GONE)
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
            }
        })
    }

    fun loadWeeklyActivty(){
        currentWeekSlideIndex = 0;

        if(!workout_enabled){
            findViewById<TextView>(R.id.track_activity_card_steps_workouts_header).setVisibility(View.INVISIBLE)
        }
        if(!steps_enabled){
            findViewById<TextView>(R.id.track_activity_card_steps_header).setVisibility(View.GONE)
            findViewById<TextView>(R.id.track_activity_card_steps_spacer).setVisibility(View.VISIBLE)
        }

        if(totalWeekSlideCount > 1) {
            findViewById<FrameLayout>(R.id.activity_weeks_slide_container).setOnTouchListener(object : OnSwipeTouchListener(this@TrackActivity) {
                override fun onSwipeLeft() {
                    super.onSwipeLeft()
                    switchWeekSlide(currentWeekSlideIndex + 1)
                }
                override fun onSwipeRight() {
                    super.onSwipeRight()
                    switchWeekSlide(currentWeekSlideIndex - 1)
                }
            })
        }
        removeAllSlides(findViewById(R.id.activity_weeks_slide_container)) { addWeekSlides() }
    }

    fun addWeekSlides(){
        val inflater = LayoutInflater.from(this@TrackActivity)
        var k = 0;

        for(week in weeks){
            val slide = inflater.inflate(R.layout.track_activity_slide,null, false) as LinearLayout
            for(day in week.days){
                val tracking_type = getStringVariable("ACTIVITY_TRACKING_TYPE")
                val binding: TrackActivityRowBinding = DataBindingUtil.inflate(inflater, R.layout.track_activity_row, null, true)
                binding.colorList = getColorList("")
                val row = binding.root as LinearLayout
                val row_container = row.getChildAt(0) as LinearLayout
                val row_expanded_container = row.getChildAt(1) as LinearLayout
                row_expanded_container.setVisibility(View.GONE)

                (row_container.getChildAt(0) as TextView).text = day.date


                if(workout_enabled){
                    if(day.num_workouts == 1){
                        (row_container.getChildAt(2) as TextView).text = "1" + " " + (getResources().getString(R.string.mobile_track_activity_activity_card_row_workout))
                    }else if(day.num_workouts > 1){
                        (row_container.getChildAt(2) as TextView).text = day.num_workouts.toString() + " " + getResources().getString(R.string.mobile_track_activity_activity_card_row_workouts)
                    }else{
                        (row_container.getChildAt(2) as TextView).text = "-"
                        if(day.steps <= 0){
                            (row_container.getChildAt(5) as ImageView).setVisibility(View.INVISIBLE)
                        }
                    }
                }else{
                    (row_container.getChildAt(2) as TextView).setVisibility(View.INVISIBLE)
                }

                if(steps_enabled){
                    (row_container.getChildAt(1) as TextView).text = day.steps.toString()
                    (row_container.getChildAt(3) as TextView).setVisibility(View.GONE)
                }else{
                    (row_container.getChildAt(1) as TextView).setVisibility(View.GONE)
                    (row_container.getChildAt(3) as TextView).setVisibility(View.VISIBLE)
                }

                if(tracking_type == "distance"){
                    (row_container.getChildAt(4) as TextView).text = day.miles.toString()
                }else if (tracking_type == "minutes"){
                    (row_container.getChildAt(4) as TextView).text = day.minutes.toString()
                }else{
                    (row_container.getChildAt(4) as TextView).text = day.points.toString()
                }

                (row_expanded_container.getChildAt(1) as FrameLayout).setOnTouchListener(object :
                    OnSwipeTouchListener(this@TrackActivity) {
                    override fun onSwipeLeft() {
                        super.onSwipeLeft()
                        switchActivitySlide(
                            (row_expanded_container.getChildAt(1) as FrameLayout),
                            true
                        )
                    }

                    override fun onSwipeRight() {
                        super.onSwipeRight()
                        switchActivitySlide(
                            (row_expanded_container.getChildAt(1) as FrameLayout),
                            false
                        )
                    }
                })

                val row_image = row_container.getChildAt(5) as ImageView
                row_image.setOnClickListener{
                    if(row_expanded_container.visibility == View.GONE){
                        sendGoogleAnalytics("expand_daily_activity","track_activity")
                        removeAllSlides(row_expanded_container.getChildAt(1) as FrameLayout){
                            loadDailyActivities(
                                day.date,
                                row_image,
                                row_expanded_container
                            )
                        }
                    }else{
                        sendGoogleAnalytics("collapse_daily_activity","track_activity")
                        row_image.setImageResource(R.drawable.plus_icon)
                        row_expanded_container.setVisibility(View.GONE)
                    }
                }
                slide.addView(row)
            }
            if(k != currentWeekSlideIndex){
                slide.setVisibility(View.GONE)
            }
            k++;
            findViewById<FrameLayout>(R.id.activity_weeks_slide_container).addView(slide)
        }
        setupSlideButtons(totalWeekSlideCount,R.id.activity_weeks_slide_buttons, "weeks")
        findViewById<FrameLayout>(R.id.activity_weeks_slide_container).setVisibility(View.VISIBLE)
        if(totalWeekSlideCount > 1){
            switchWeekSlide(0)
        }
    }

    fun switchWeekSlide(newIndex:Int){
        switchSlideButton(newIndex + 1,totalWeekSlideCount,R.id.activity_weeks_slide_buttons)
        val weeksLayout = findViewById<FrameLayout>(R.id.activity_weeks_slide_container)
        if(newIndex > 0){
            weeksLayout.getChildAt(0).setVisibility(View.GONE)
        }
        if((newIndex >= 0) and (newIndex < totalWeekSlideCount)){
            findViewById<TextView>(R.id.weekly_table_header).setText(weeks[newIndex].name)
            weeksLayout.getChildAt(currentWeekSlideIndex).visibility = View.GONE
            weeksLayout.getChildAt(newIndex).visibility = View.VISIBLE
            currentWeekSlideIndex = newIndex
        }
    }

    fun switchTypeSlide(newIndex:Int){
        switchSlideButton(newIndex + 1,totalSlideCount,R.id.track_activity_types_slide_buttons)
        val activityTypesLayout = findViewById<FrameLayout>(R.id.track_activity_types_slide_container)
        if((newIndex >= 0) and (newIndex < totalSlideCount)){
            activityTypesLayout.getChildAt(currentSlideIndex).visibility = View.INVISIBLE
            activityTypesLayout.getChildAt(newIndex).visibility = View.VISIBLE
            currentSlideIndex = newIndex
        }
    }

    fun switchActivitySlide(slide: FrameLayout, direction: Boolean){
        val current_index = (((slide.parent as LinearLayout).getChildAt(0) as LinearLayout).getChildAt(0) as TextView).text;
        val container = (slide.parent as LinearLayout).getChildAt(2) as LinearLayout
        var length = slide.childCount;
        try {
            val indexInt = current_index.toString().toInt()
            var newIndex = 0;
            var change = false;
            if(direction){
                if(indexInt < length){
                    newIndex = indexInt + 1;
                    change = true;
                }
            }else{
                if(indexInt > 1){
                    newIndex = indexInt - 1;
                    change = true;
                }
            }

            if(change){
                (((slide.parent as LinearLayout).getChildAt(0) as LinearLayout).getChildAt(0) as TextView).text = newIndex.toString();
                slide.getChildAt(indexInt - 1 ).setVisibility(View.GONE)
                slide.getChildAt(newIndex - 1).setVisibility(View.VISIBLE)
                switchVariableSlideButton(newIndex, length, container)
            }
        }catch(e: Exception){
            reloadPage()
        }
    }

    fun reloadPage(){
        val intent = Intent(this@TrackActivity, TrackActivity::class.java);
        startActivity(intent);
    }

    fun loadModalActivities(sortedActivities: List<ActivityGoal>){
        val inflater = layoutInflater
        val table = findViewById<TableLayout>(R.id.activity_points_modal_table)

        for (childView in table.children) {
            table.removeView(childView);
        }

        runOnUiThread {
            for (i in 0 until sortedActivities.size) {
                val activity = sortedActivities[i]

                val binding: EarnPointsActivityRowBinding =
                    DataBindingUtil.inflate(
                        inflater,
                        R.layout.earn_points_activity_row,
                        table,
                        true
                    )
                binding.colorList = getColorList("")

                val row = binding.root as TableRow
                val img = getEarnActivityImage(activity.type)
                if(img != 0){
                    (row.getChildAt(0) as ImageView).setImageDrawable(
                        getDrawable(img)
                    )
                }else{
                    (row.getChildAt(0) as ImageView).setImageDrawable(getDrawable(R.drawable.activity))
                    (row.getChildAt(0) as ImageView).setVisibility(View.INVISIBLE)
                }

                (row.getChildAt(1) as TextView).setText(activity.name)
                (row.getChildAt(2) as TextView).setText(activity.points)
            }
        }
    }

    fun getEarnActivityImage(name: String): Int{
        if(name == "barre"){
            return R.drawable.barre
        }else if (name == "baseball"){
            return R.drawable.baseball
        }else if (name == "basketball"){

            return R.drawable.basketball
        }else if (name == "bowling"){
            return R.drawable.bowling
        }else if (name == "boxing"){
            return R.drawable.boxing
        }else if (name == "climbing"){
            return R.drawable.climbing
        }else if (name == "core"){
            return R.drawable.core
        }else if (name == "cycling"){
            return R.drawable.cycle
        }else if (name == "dance"){
            return R.drawable.dance
        }else if (name == "elliptical"){
            return R.drawable.elliptical
        }else if (name == "football"){
            return R.drawable.football
        }else if (name == "golf"){
            return R.drawable.golf
        }else if (name == "gymnastics"){
            return R.drawable.gymnastics
        }else if (name == "high intensity intervals"){
            return R.drawable.high_intesity_intervals
        }else if (name == "hiking"){
            return R.drawable.hiking
        }else if (name == "hockey"){
            return R.drawable.hockey
        }else if (name == "jump rope"){
            return R.drawable.jump_rope
        }else if (name == "kickboxing"){
            return R.drawable.kickboxing
        }else if (name == "martial arts"){
            return R.drawable.martial_arts
        }else if (name == "mind and body"){
            return R.drawable.mind_and_body
        }else if (name == "mixed cardio"){
            return R.drawable.mixed_cardio
        }else if (name == "other"){
            return R.drawable.other
        }else if (name == "pickleball"){
            return R.drawable.pickleball
        }else if (name == "pilates"){
            return R.drawable.pilates
        }else if (name == "rowing"){
            return R.drawable.rowing
        }else if (name == "running"){
            return R.drawable.running
        }else if (name == "soccer"){
            return R.drawable.soccer
        }else if (name == "softball"){
            return R.drawable.softball
        }else if (name == "stair climber"){
            return R.drawable.stair_climber
        }else if (name == "strength training"){
            return R.drawable.strength_training
        }else if (name == "swimming"){
            return R.drawable.swimming
        }else if (name == "tennis"){
            return R.drawable.tennis
        }else if (name == "triathalon"){
            return R.drawable.triathlon
        }else if (name == "volleyball"){
            return R.drawable.volleyball
        }else if (name == "walking"){
            return R.drawable.walking
        }else if (name == "water polo"){
            return 0
        }else if (name == "winter sports"){
            return R.drawable.winter_sports
        }else if (name == "yoga"){
            return R.drawable.yoga
        }else if (hobby_names.contains(name)){
            return R.drawable.hobbies
        }else{
            return R.drawable.other
        }
    }

    class ActivityGoal(
        var name: String,
        var type: String,
        var points: String,
    )

    class Week(
        val name: String,
        val days: List<Day>,
    )
    class Day(
        val date: String,
        val steps: Int,
        val num_workouts: Int,
        val points: Int,
        val miles: Double,
        val minutes: Double,
    )

    class Activity(
        val date: String,
        val vendor: String,
        val vendor_id: String,
        val steps: Int,
        val minutes: Int,
        val miles: Double,
        val points: Int,
        val activity_type: String,
        val activity_type_display_name: String,
        val activity_type_image_url: String,
    )

    fun ImageView.setTint(color: Int) {
        ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(color))
    }

    fun ImageView.removeTint() {
        ImageViewCompat.setImageTintList(this, null)
    }
}