package com.nuclavis.rospark

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.databinding.DataBindingUtil
import app.com.kotlinapp.OnSwipeTouchListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.nuclavis.rospark.databinding.FundraisingMessageBinding
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.properties.Delegates


class Challenges : com.nuclavis.rospark.BaseActivity() {

    var leaderboard_participants = emptyArray<challengeParticipant>();
    var leaderboard_teams = emptyArray<challengeParticipant>();
    var leaderboard_companies = emptyArray<challengeParticipant>();
    var current_leaderboard_slide = 0;
    var total_leaderboard_slides = 2;
    var leaderboard_amount_participants = emptyArray<challengeParticipant>();
    var leaderboard_amount_teams = emptyArray<challengeParticipant>();
    var leaderboard_amount_companies = emptyArray<challengeParticipant>();
    var current_leaderboard = "raised";

    var joined_challenge = false;
    var challenge_completed = false;

    var activity_metric = ""

    var totalMessagesSlideCount = 0;
    var currentMessagesSlideIndex = 0;
    var challenge_messages = emptyList<Fundraise.FundraisingMessage>()

    var progressBarWidth = 0;
    var progressBarHeight = 0;

    var challengeProgressPercent: Double by Delegates.observable(0.00) { _, old, new ->
        run {
            if (new != 0.00) {
                resizeProgressBar("progress");
            }
        };
    }

    var challengeDollarsPercent: Double by Delegates.observable(0.00) { _, old, new ->
        run {
            if (new != 0.00) {
                resizeProgressBar("dollars");
            }
        };
    }

    fun resizeProgressBar(type: String){
        runOnUiThread {

            var bar = findViewById<LinearLayout>(R.id.challenges_progress_card_raised_bar)
            var percent = challengeProgressPercent
            var raisedBar = findViewById<LinearLayout>(R.id.challenges_progress_card_raised_progress_bar)

            if(type == "dollars"){
                bar = findViewById(R.id.challenges_dollar_progress_card_raised_bar)
                percent = challengeDollarsPercent
                raisedBar = findViewById(R.id.challenges_dollar_progress_card_raised_progress_bar)
            }

            bar.doOnLayout {
                val totalBarWidth = it.measuredWidth
                var totalBarHeight = it.measuredHeight
                var new_width = 0;
                if(percent > 1.00){
                    new_width = totalBarWidth
                }else{
                    new_width = (totalBarWidth * percent).toInt()
                }

                var newBarWidth = totalBarHeight;
                if(new_width > totalBarHeight){
                    newBarWidth = new_width
                }else if(percent == 0.00){
                    newBarWidth = 0
                }

                raisedBar.layoutParams = FrameLayout.LayoutParams(newBarWidth, totalBarHeight)
            }
        }
    }

    override fun childviewCallback(string: String, data:String){
        hideAlert();
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("fundraising message",challenge_messages[currentMessagesSlideIndex].text)
        clipboard.setPrimaryClip(clip)
        if(string == "linkedin"){
            shareLinkedIn(Uri.encode(challenge_messages[currentMessagesSlideIndex].linkedin_url))
        }else if(string == "facebook") {
            shareFacebook(this@Challenges, challenge_messages[currentMessagesSlideIndex].facebook_url)
        }
    }

    override fun slideButtonCallback(card: Any, forward:Boolean){
        if(card == "messages"){
            var currentIndex = currentMessagesSlideIndex;
            if(forward){
                currentIndex += 1;
            }else{
                currentIndex -= 1;
            }
            switchMessageSlide(currentIndex)
        }else if(card == "leaderboard" || card == "amount-leaderboard"){
            var currentIndex = current_leaderboard_slide;
            if(forward){
                currentIndex += 1;
            }else{
                currentIndex -= 1;
            }
            switchLeaderboardSlide(currentIndex)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        recolorTheme()
        super.onCreate(savedInstanceState)
        setPageContent(R.layout.challenges, "challenges")
        sendGoogleAnalytics("challenges_view","challenges")
        setTitle(getResources().getString(R.string.mobile_main_menu_challenges));

        val challengeBar = findViewById<LinearLayout>(R.id.challenges_progress_card_raised_bar);
        challengeBar.doOnLayout {
            progressBarWidth = it.measuredWidth
            progressBarHeight = it.measuredHeight
        }

        val challenge_string = getStringVariable("CHALLENGES_OBJECT")
        if(challenge_string != "") {
            val challenge = JSONObject(challenge_string)
            setupChallengeCard(challenge)

            findViewById<LinearLayout>(R.id.challenges_leaderboard_card_container).visibility = View.GONE

            var challenge_status = getSafeStringVariable(challenge, "challenge_status")

            if(challenge_status == "Live"){
                setupShareCard(challenge)
                findViewById<LinearLayout>(R.id.challenges_share_card).visibility = View.VISIBLE
            }else if (challenge_status == "Ended"){
                findViewById<LinearLayout>(R.id.challenges_share_card).visibility = View.GONE
            }

            setupChallengeCard(challenge)

            val challenge_id = getSafeIntegerVariable(challenge, "id")
            if(challenge_id != 0) {
                var url = getResources().getString(R.string.base_server_url)
                    .plus("/").plus(getStringVariable("CLIENT_CODE"))
                    .plus("/activity/challenge/").plus(getConsID())
                    .plus("/").plus(getEvent().event_id)
                    .plus("/").plus(challenge_id);

                var request = Request.Builder()
                    .url(url)
                    .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                    .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                    .build()
                var client = OkHttpClient();
                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: okhttp3.Response) {
                        if(response.code != 200){
                            showDefaultPage()
                        }else{
                            val response = response.body?.string();
                            val obj = JSONObject(response);
                            if(obj.has("data") && obj.get("data") is JSONObject){
                                val data = obj.get("data") as JSONObject;
                                runOnUiThread{
                                    updateUserChallengeInfo(data, challenge)
                                }
                            }
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        showDefaultPage()
                    }
                })
            }
        }else{
            showDefaultPage()
        }
    }

    fun evaluateStatus(challenge: JSONObject, data: JSONObject){
        var challenge_status = getSafeStringVariable(challenge, "challenge_status")

        var joined_challenge_val = getSafeBooleanVariable(data, "joined_challenge");
        joined_challenge = joined_challenge_val

        if(joined_challenge){
            findViewById<LinearLayout>(R.id.challenge_add_activity_container).visibility = View.VISIBLE;
            if(challenge_status != "Ended"){
                checkConnectionStatus()
                if (challenge_status == "Live"){
                    setupLeaderboardCard(challenge)
                    if(getSafeBooleanVariable(data, "completed_challenge")){
                        challenge_completed = true
                        findViewById<LinearLayout>(R.id.challenge_completed_container).visibility = View.VISIBLE

                        findViewById<CardView>(R.id.challenges_challenge_card).doOnLayout {
                            val image = findViewById<ImageView>(R.id.challenges_confetti_background);

                            val options = RequestOptions()
                            options.centerCrop()
                            options.override(it.width, it.height)

                            Glide.with(this@Challenges)
                                .load("https://nt-dev-clients.s3.amazonaws.com/global/confetti.gif")
                                .listener(object : RequestListener<Drawable> {
                                    override fun onLoadFailed(
                                        p0: GlideException?,
                                        p1: Any?,
                                        target: com.bumptech.glide.request.target.Target<Drawable>?,
                                        p3: Boolean
                                    ): Boolean {

                                        //do something if error loading
                                        return false
                                    }
                                    override fun onResourceReady(
                                        p0: Drawable?,
                                        p1: Any?,
                                        target: Target<Drawable>?,
                                        p3: DataSource?,
                                        p4: Boolean
                                    ): Boolean {
                                        recursiveConfetti(image, 0)
                                        //do something when picture already loaded
                                        return false
                                    }
                                })
                                .apply(options)
                                .into(image);
                            image.visibility = View.VISIBLE
                        }
                    }
                    setupActivityButton()
                }
            }else{
                setupLeaderboardCard(challenge)
            }
        }else{
            findViewById<LinearLayout>(R.id.challenge_add_activity_container).visibility = View.GONE;
            findViewById<LinearLayout>(R.id.challenge_completed_container).visibility = View.GONE
            findViewById<LinearLayout>(R.id.challenges_share_card).visibility = View.GONE
            findViewById<LinearLayout>(R.id.challenges_leaderboard_card_container).visibility = View.GONE
        }
    }

    fun recursiveConfetti(image: ImageView, index: Int){
        val timer = Timer()
        timer.schedule(timerTask {this@Challenges?.runOnUiThread {
            if(index >= 5){
                image.setVisibility(View.GONE)
            }else{
                if(image.isLaidOut){
                    recursiveConfetti(image, index + 1)
                }else{
                    recursiveConfetti(image, index)
                }
            }
        } }, 1000)
    }

    fun setupActivityButton(){
        var workoutTypes = emptyArray<WorkoutType>()

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

                    runOnUiThread{
                        findViewById<Button>(R.id.btn_add_activity).setOnClickListener {
                            sendGoogleAnalytics("challenges_show_add_activity_modal","challenges")
                            displayAlert("addManualActivity",arrayOf(workoutTypes,"challenges"))
                            setAlertSender(findViewById<Button>(R.id.btn_add_activity))
                        }

                        findViewById<Button>(R.id.btn_make_donation).setOnClickListener {
                            sendGoogleAnalytics("challenges_donate", "challenges")
                            try{
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getStringVariable("DONATIONS_URL")))
                                startActivity(browserIntent)
                            }catch(e: Exception){
                                displayAlert(getString(R.string.mobile_url_opening_error))
                            }
                        }
                    }
                }else{
                    runOnUiThread {
                        findViewById<LinearLayout>(R.id.challenge_add_activity_container).visibility =
                            View.GONE;
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                findViewById<LinearLayout>(R.id.challenge_add_activity_container).visibility = View.GONE;
            }
        })
    }

    fun switchProgressBar(bar: String){
        var oldActionButton = findViewById<Button>(R.id.btn_make_donation)
        var newActionButton = findViewById<Button>(R.id.btn_add_activity)
        var newButton = findViewById<FrameLayout>(R.id.challenges_bars_activity_button)
        var oldButton = findViewById<FrameLayout>(R.id.challenges_bars_raised_button)
        var newSlide = findViewById<LinearLayout>(R.id.challenges_progress_container)
        var oldSlide = findViewById<LinearLayout>(R.id.challenges_dollar_progress_container)
        findViewById<TextView>(R.id.challenges_button_description).text = getString(R.string.mobile_challenges_add_activity_text)

        if(bar == "dollars"){
            oldButton = findViewById(R.id.challenges_bars_activity_button)
            newButton = findViewById(R.id.challenges_bars_raised_button)
            newSlide = findViewById(R.id.challenges_dollar_progress_container)
            oldSlide = findViewById(R.id.challenges_progress_container)
            newActionButton = findViewById(R.id.btn_make_donation)
            oldActionButton = findViewById(R.id.btn_add_activity)
            findViewById<TextView>(R.id.challenges_button_description).text = getString(R.string.mobile_challenges_donate_text)
        }

        oldSlide.setVisibility(View.GONE)
        newSlide.setVisibility(View.VISIBLE)
        newActionButton.setVisibility(View.VISIBLE)
        oldActionButton.setVisibility(View.GONE)

        newButton.contentDescription = "${newButton.tag} Button Selected"
        oldButton.contentDescription = "${oldButton.tag} Button Not Selected"

        setCustomToggleButtonColor(newButton, "active")
        setCustomToggleButtonColor(oldButton, "inactive")
    }

    fun switchLeaderboard(board: String){
        current_leaderboard = board
        var newSlide = findViewById<LinearLayout>(R.id.challenges_activity_leaderboard_container)
        var oldSlide = findViewById<LinearLayout>(R.id.challenges_amount_leaderboard_container)

        if(board == "amount"){
            oldSlide = findViewById<LinearLayout>(R.id.challenges_activity_leaderboard_container)
            newSlide = findViewById<LinearLayout>(R.id.challenges_amount_leaderboard_container)
        }

        newSlide.visibility = View.VISIBLE
        oldSlide.visibility = View.GONE
    }

    fun updateUserChallengeInfo(data: JSONObject, challenge: JSONObject){
        var challenge_status = getSafeStringVariable(challenge, "challenge_status")
        val joined_challenge_val = getSafeBooleanVariable(data, "joined_challenge")
        joined_challenge = joined_challenge_val
        var challenge_type = getSafeStringVariable(challenge, "challenge_type")
        evaluateStatus(challenge, data)
        if((challenge_status == "Live" || challenge_status == "Ended") && joined_challenge){
            var challenge_my_progress = intWithCommas(getSafeIntegerVariable(data, "my_challenge_points"))
            var my_challenge_goal = getSafeIntegerVariable(data, "my_challenge_goal")
            var my_challenge_percent_to_goal = getSafeIntegerVariable(data, "my_challenge_percent_to_goal")

            var label = getString(R.string.mobile_challenges_points);

            if(challenge_type == "BOTH" || challenge_type == "ACTIVITY"){
                val challenge_metric = getSafeStringVariable(challenge, "activity_metric")
                var header = getString(R.string.mobile_challenges_leaderboard_points)
                if(challenge_metric == "DISTANCE"){
                    activity_metric = "distance"
                    label = getDistanceLabel(R.string.mobile_challenges_miles, R.string.mobile_challenges_km)
                    challenge_my_progress = getSafeDoubleVariable(data, "my_challenge_distance").toString()
                    my_challenge_goal = getSafeIntegerVariable(data, "my_challenge_distance_goal")
                    my_challenge_percent_to_goal = getSafeIntegerVariable(data, "my_challenge_distance_percent_to_goal")
                    header = getDistanceLabel(R.string.mobile_challenges_leaderboard_miles, R.string.mobile_challenges_leaderboard_km)
                }
                findViewById<TextView>(R.id.leaderboard_activity_table_points_label).text = header
                findViewById<TextView>(R.id.leaderboard_activity_table_team_points_label).text = header
            }

            if(my_challenge_percent_to_goal > 100){
                my_challenge_percent_to_goal = 100
            }

            findViewById<TextView>(R.id.challenges_progress_card_raised_amount).setText(
                getString(R.string.mobile_challenges_points) + " " + challenge_my_progress
            )

            findViewById<TextView>(R.id.challenges_progress_card_raised_percent).setText(
                intWithCommas(my_challenge_percent_to_goal) + "%"
            )

            findViewById<TextView>(R.id.challenges_progress_card_raised_goal).setText(
                getString(R.string.mobile_challenges_goal) + " " + intWithCommas(
                    my_challenge_goal
                )
            )

            challengeProgressPercent = (my_challenge_percent_to_goal.toDouble()/100)
            resizeProgressBar("challenge")

            if(challenge_type == "BOTH"){
                findViewById<LinearLayout>(R.id.challenges_leaderboard_buttons).setVisibility(View.VISIBLE)
                findViewById<LinearLayout>(R.id.challenges_progress_bars_container).setVisibility(View.VISIBLE)
                val my_challenge_dollars = getSafeIntegerVariable(data, "my_challenge_dollars")
                val my_challenge_dollars_goal = getSafeIntegerVariable(data, "my_challenge_dollars_goal")
                var my_challenge_dollars_percent_to_goal = getSafeIntegerVariable(data, "my_challenge_dollars_percent_to_goal")
                findViewById<LinearLayout>(R.id.challenges_leaderboard_buttons).setVisibility(View.VISIBLE)
                findViewById<LinearLayout>(R.id.challenges_team_leaderboard_buttons).setVisibility(View.VISIBLE)
                findViewById<LinearLayout>(R.id.challenges_company_leaderboard_buttons).setVisibility(View.VISIBLE)

                if(my_challenge_dollars_percent_to_goal > 100){
                    my_challenge_dollars_percent_to_goal = 100;
                }

                findViewById<TextView>(R.id.challenges_dollar_progress_card_raised_amount).setText(getString(R.string.mobile_challenges_dollars_raised) + " " + formatDoubleToLocalizedCurrency(my_challenge_dollars.toDouble()))
                findViewById<TextView>(R.id.challenges_dollar_progress_card_raised_percent).setText(intWithCommas(my_challenge_dollars_percent_to_goal) + "%")
                findViewById<TextView>(R.id.challenges_dollar_progress_card_raised_goal).setText(getString(R.string.mobile_challenges_dollars_goal) + " " + formatDoubleToLocalizedCurrency(my_challenge_dollars_goal.toDouble()))

                challengeDollarsPercent = (my_challenge_dollars_percent_to_goal.toDouble()/100)

                setCustomToggleButtonColor(findViewById(R.id.challenges_leaderboard_raised_button_inactive),"inactive")
                setCustomToggleButtonColor(findViewById(R.id.challenges_leaderboard_active_button_active),"active")
                setCustomToggleButtonColor(findViewById(R.id.challenges_team_leaderboard_raised_button_inactive),"inactive")
                setCustomToggleButtonColor(findViewById(R.id.challenges_team_leaderboard_activity_button_active),"active")
                setCustomToggleButtonColor(findViewById(R.id.challenges_company_leaderboard_raised_button_inactive),"inactive")
                setCustomToggleButtonColor(findViewById(R.id.challenges_company_leaderboard_activity_button_active),"active")
                setCustomToggleButtonColor(findViewById(R.id.challenges_amount_leaderboard_raised_button_active),"active")
                setCustomToggleButtonColor(findViewById(R.id.challenges_amount_leaderboard_activity_button_inactive),"inactive")
                setCustomToggleButtonColor(findViewById(R.id.challenges_amount_team_leaderboard_raised_button_active),"active")
                setCustomToggleButtonColor(findViewById(R.id.challenges_amount_team_leaderboard_activity_button_inactive),"inactive")
                setCustomToggleButtonColor(findViewById(R.id.challenges_amount_company_leaderboard_raised_button_active),"active")
                setCustomToggleButtonColor(findViewById(R.id.challenges_amount_company_leaderboard_activity_button_inactive),"inactive")

                switchLeaderboard("raised")
                setupLeaderboardDollarsCard(challenge)

                findViewById<FrameLayout>(R.id.challenges_amount_leaderboard_activity_button_inactive).setOnClickListener {
                    sendGoogleAnalytics("challenges_top_indiv_activity_toggle", "challenges")
                    switchLeaderboard("activity")
                }
                findViewById<FrameLayout>(R.id.challenges_amount_team_leaderboard_activity_button_inactive).setOnClickListener {
                    sendGoogleAnalytics("challenges_top_teams_activity_toggle", "challenges")
                    switchLeaderboard("activity")
                }
                findViewById<FrameLayout>(R.id.challenges_amount_company_leaderboard_activity_button_inactive).setOnClickListener {
                    sendGoogleAnalytics("challenges_top_companies_activity_toggle", "challenges")
                    switchLeaderboard("activity")
                }
                findViewById<FrameLayout>(R.id.challenges_leaderboard_raised_button_inactive).setOnClickListener {
                    sendGoogleAnalytics("challenges_top_indiv_raised_toggle", "challenges")
                    switchLeaderboard("amount")
                }
                findViewById<FrameLayout>(R.id.challenges_team_leaderboard_raised_button_inactive).setOnClickListener {
                    sendGoogleAnalytics("challenges_top_teams_raised_toggle", "challenges")
                    switchLeaderboard("amount")
                }
                findViewById<FrameLayout>(R.id.challenges_company_leaderboard_raised_button_inactive).setOnClickListener {
                    sendGoogleAnalytics("challenges_top_companies_raised_toggle", "challenges")
                    switchLeaderboard("amount")
                }
                findViewById<LinearLayout>(R.id.challenges_dollar_progress_card_raised_progress_bar).post({
                    var color = getStringVariable("THERMOMETER_COLOR")
                    if(color == "") {
                        color = getStringVariable("PRIMARY_COLOR")
                        if(color == ""){
                            color = String.format(
                                "#%06x",
                                ContextCompat.getColor(this, R.color.primary_color) and 0xffffff
                            )
                        }
                    }

                    color = color.replace(" ","")

                    findViewById<LinearLayout>(R.id.challenges_dollar_progress_card_raised_progress_bar).background.setColorFilter(Color.parseColor(color), PorterDuff.Mode.SRC_ATOP)

                    var dollar_bar = findViewById<LinearLayout>(R.id.challenges_dollar_progress_card_raised_bar)
                    val dollar_height = dollar_bar.measuredHeight
                    val dollar_width = dollar_bar.measuredWidth
                    val dollarRaisedProgress = findViewById<LinearLayout>(R.id.challenges_dollar_progress_card_raised_progress_bar);
                    dollarRaisedProgress.layoutParams = FrameLayout.LayoutParams(dollar_width, dollar_height)
                    resizeProgressBar("dollars")
                });
                switchProgressBar("activity")
            }else{
                switchProgressBar("activity")
                findViewById<LinearLayout>(R.id.challenges_dollar_progress_container).setVisibility(View.GONE)
                findViewById<LinearLayout>(R.id.challenges_bars_buttons).setVisibility(View.GONE)
                findViewById<LinearLayout>(R.id.challenges_leaderboard_buttons).setVisibility(View.GONE)
                findViewById<LinearLayout>(R.id.challenges_team_leaderboard_buttons).setVisibility(View.GONE)
                findViewById<LinearLayout>(R.id.challenges_company_leaderboard_buttons).setVisibility(View.GONE)
                findViewById<LinearLayout>(R.id.challenges_amount_leaderboard_container).setVisibility(View.GONE)
            }

            findViewById<LinearLayout>(R.id.challenges_progress_card_raised_progress_bar).post({
                var color = getStringVariable("THERMOMETER_COLOR")
                if(color == "") {
                    color = getStringVariable("PRIMARY_COLOR")
                    if(color == ""){
                        color = String.format(
                            "#%06x",
                            ContextCompat.getColor(this, R.color.primary_color) and 0xffffff
                        )
                    }
                }

                color = color.replace(" ","")

                findViewById<LinearLayout>(R.id.challenges_progress_card_raised_progress_bar).background.setColorFilter(Color.parseColor(color), PorterDuff.Mode.SRC_ATOP)

                var bar = findViewById<LinearLayout>(R.id.challenges_progress_card_raised_bar)
                val height = bar.measuredHeight
                val width = bar.measuredWidth
                val raisedProgress = findViewById<LinearLayout>(R.id.challenges_progress_card_raised_progress_bar);
                raisedProgress.layoutParams = FrameLayout.LayoutParams(width, height)
                resizeProgressBar("challenges")
            });

            findViewById<LinearLayout>(R.id.challenges_progress_bars_container).visibility = View.VISIBLE;
            findViewById<TextView>(R.id.challenges_progress_card_raised_amount).setText(
                label + " " + challenge_my_progress
            )

            findViewById<TextView>(R.id.challenges_progress_card_raised_percent).setText(
                intWithCommas(my_challenge_percent_to_goal) + "%"
            )

            findViewById<TextView>(R.id.challenges_progress_card_raised_goal).setText(
                getString(R.string.mobile_challenges_goal) + " " + intWithCommas(
                    my_challenge_goal
                )
            )

            findViewById<LinearLayout>(R.id.challenges_progress_container).visibility = View.VISIBLE;

            if (challenge_type == "BOTH"){
                findViewById<FrameLayout>(R.id.challenges_bars_activity_button).setOnClickListener {
                    sendGoogleAnalytics("challenges_activity_toggle","challenges")
                    switchProgressBar("activity")
                }

                findViewById<FrameLayout>(R.id.challenges_bars_raised_button).setOnClickListener {
                    sendGoogleAnalytics("challenges_raised_toggle","challenges")
                    switchProgressBar("dollars")
                }
                switchProgressBar("activity")
                setCustomToggleButtonColor(findViewById(R.id.challenges_bars_activity_button),"active")
                setCustomToggleButtonColor(findViewById(R.id.challenges_bars_raised_button),"inactive")
            } else {
                switchProgressBar("activity")
            }
        } else {
            findViewById<LinearLayout>(R.id.challenges_share_card).visibility = View.GONE
            findViewById<LinearLayout>(R.id.challenges_leaderboard_card_container).visibility = View.GONE
            findViewById<LinearLayout>(R.id.challenges_leaderboard_buttons).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.challenges_team_leaderboard_buttons).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.challenges_company_leaderboard_buttons).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.challenges_amount_leaderboard_container).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.challenges_progress_container).visibility = View.GONE

            if(joined_challenge){
                findViewById<LinearLayout>(R.id.challenges_progress_bars_container).setVisibility(View.VISIBLE)
            }else{
                findViewById<LinearLayout>(R.id.challenges_progress_bars_container).setVisibility(View.GONE)
            }
        }

        if(challenge_status == "Active"){
            findViewById<LinearLayout>(R.id.challenges_progress_bars_container).visibility = View.GONE
            findViewById<LinearLayout>(R.id.challenge_add_activity_container).visibility = View.GONE
        }

        if(challenge_status != "Ended"){
            if(joined_challenge){
                findViewById<LinearLayout>(R.id.join_challenge_container).setVisibility(View.GONE)
                findViewById<LinearLayout>(R.id.leave_challenge_container).setVisibility(View.VISIBLE)
            }else{
                findViewById<LinearLayout>(R.id.join_challenge_container).setVisibility(View.VISIBLE)
                findViewById<LinearLayout>(R.id.leave_challenge_container).setVisibility(View.GONE)
            }
        }else{
            findViewById<LinearLayout>(R.id.challenge_add_activity_container).visibility = View.GONE
            findViewById<LinearLayout>(R.id.join_challenge_container).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.leave_challenge_container).setVisibility(View.GONE)
        }

        findViewById<Button>(R.id.btn_join_challenge).setOnClickListener{
            sendGoogleAnalytics("challenges_join","challenges")
            JoinChallenge(challenge)
        }

        findViewById<Button>(R.id.btn_leave_challenge).setOnClickListener{
            sendGoogleAnalytics("challenges_leave","challenges")
            leaveChallenge(challenge)
        }
    }

    fun JoinChallenge(challenge: JSONObject){
        val challenge_id = getSafeIntegerVariable(challenge, "id")

        var url = getResources().getString(R.string.base_server_url)
            .plus("/").plus(getStringVariable("CLIENT_CODE"))
            .plus("/activity/challenge/")
            .plus(getConsID()).plus("/")
            .plus(getEvent().event_id).plus("/")
            .plus(challenge_id);

        if(getStringVariable("HAS_TEAM") == "true" && getStringVariable("TEAM_ID") != ""){
            url = url.plus("/").plus(getStringVariable("TEAM_ID"))
        }

        val formBody = FormBody.Builder()
            .add("cons_id", getConsID())
            .add("event_id", getEvent().event_id)
            .add("challenge_id", challenge_id.toString())
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
                    runOnUiThread {
                        if (obj.has("data") && obj.get("data") is JSONObject) {
                            updateUserChallengeInfo(obj.get("data") as JSONObject, challenge)
                        }
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {

            }
        })
    }

    fun leaveChallenge(challenge: JSONObject){
        val challenge_id = getSafeIntegerVariable(challenge, "id")

        val url = getResources().getString(R.string.base_server_url)
            .plus("/").plus(getStringVariable("CLIENT_CODE"))
            .plus("/activity/challenge/")
            .plus(getConsID()).plus("/")
            .plus(getEvent().event_id).plus("/")
            .plus(challenge_id);

        val formBody = FormBody.Builder()
            .add("cons_id", getConsID())
            .add("event_id", getEvent().event_id)
            .add("challenge_id", challenge_id.toString())
            .build()

        var request = Request.Builder()
            .url(url)
            .delete(formBody)
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
                    runOnUiThread{
                        if(obj.has("data") && obj.get("data") is JSONObject){
                            val data = obj.get("data") as JSONObject;
                            updateUserChallengeInfo(data, challenge)
                        }
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {

            }
        })
    }

    fun setupChallengeCard(challenge: JSONObject){
        setupDetailsCard(challenge)
        val challenge_text = getSafeStringVariable(challenge, "title")
        if(challenge_text != ""){
            findViewById<TextView>(R.id.challenge_title).text = challenge_text
        }else{
            findViewById<TextView>(R.id.challenge_title).visibility = View.GONE
        }

        val challenge_description = getSafeStringVariable(challenge, "description")
        if(challenge_description != ""){
            findViewById<TextView>(R.id.challenge_description).text = challenge_description
        }else{
            findViewById<TextView>(R.id.challenge_description).visibility = View.GONE
        }

        var challenge_status = getSafeStringVariable(challenge, "challenge_status")
        var challenge_start_date = getSafeStringVariable(challenge, "challenge_starts_date")
        var challenge_end_date = getSafeStringVariable(challenge, "challenge_ends_date")

        if(challenge_status == "Live"  || challenge_status == "Active"){
            findViewById<TextView>(R.id.challenge_days_left).text = getString(R.string.mobile_challenges_dates_to_android).replace("ZZZZ",challenge_end_date).replace("XXXX",challenge_start_date)
        }else{
            findViewById<ImageView>(R.id.challenge_icon).visibility = View.GONE
            if(challenge_status == "Ended"){
                findViewById<TextView>(R.id.challenge_days_left).text = getString(R.string.mobile_challenges_days_ended)
                findViewById<LinearLayout>(R.id.challenges_share_card).visibility = View.GONE
            }else{
                findViewById<TextView>(R.id.challenge_days_left).visibility = View.GONE
            }
        }

        val challenge_icon_url = getSafeStringVariable(challenge, "icon_url")
        if(challenge_icon_url != ""){
            Glide.with(this)
            .load(challenge_icon_url)
            .into(findViewById<ImageView>(R.id.challenge_icon))
        }
    }

    fun getConnectedDate(){
        var url = getResources().getString(R.string.base_server_url).plus("/")
            .plus(getStringVariable("CLIENT_CODE")).plus("/activity/tracking/")
            .plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call, response: okhttp3.Response) {
                val jsonString = response.body?.string()!!;
                val obj = JSONObject(jsonString)
                val connected_time = getSafeStringVariable(obj, "connected_time")

                setVariable("TRACKING_STARTED", "false")

                if(connected_time == ""){
                    clearVariable("TRACKING_STARTED_MONTH");
                    clearVariable("TRACKING_STARTED_DAY");
                    clearVariable("TRACKING_STARTED_YEAR");
                }else{
                    val connected_date = LocalDate.parse(connected_time);
                    setVariable("TRACKING_STARTED_MONTH", (connected_date.monthValue).toString())
                    setVariable("TRACKING_STARTED_DAY", (connected_date.dayOfMonth).toString())
                    setVariable("TRACKING_STARTED_YEAR", (connected_date.year).toString())
                    setVariable("TRACKING_STARTED", "true")
                }
            }

            override fun onFailure(call: Call, e: IOException) {

            }
        })
    }

    fun checkConnectionStatus(){
        getConnectedDate()
        val url = resources.getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/connection/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        val client = OkHttpClient()

        var platform_connected = true;

        client.newCall(request).enqueue(object : Callback {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    platform_connected = false;
                    //throw Exception(response.body?.string())
                }else{
                    val jsonString = response.body?.string() as String
                    val jsonObject = JSONObject(jsonString);
                    var connected_time = "";

                    runOnUiThread() {
                        if (jsonObject.has("success")) {
                            if (jsonObject.get("success") == true) {
                                if(jsonObject.getJSONObject("data").has("connected_time") && jsonObject.getJSONObject("data").get("connected_time") is String)
                                {
                                    connected_time = (jsonObject.getJSONObject("data").get("connected_time") as String).substring(0,10);
                                    val connected_date = LocalDate.parse(connected_time);
                                    setVariable("PLATFORM_CONNECTED_MONTH", (connected_date.monthValue).toString())
                                    setVariable("PLATFORM_CONNECTED_DAY", (connected_date.dayOfMonth).toString())
                                    setVariable("PLATFORM_CONNECTED_YEAR", (connected_date.year).toString())
                                }

                                var platform = "";
                                if(jsonObject.getJSONObject("data").has("activity_platform") && jsonObject.getJSONObject("data").get("activity_platform") is String)
                                {
                                    platform = jsonObject.getJSONObject("data").get("activity_platform") as String
                                }

                                if(platform == "GOOGLE"){
                                    platform_connected = true
                                }
                                else if(platform == "APPLE"){
                                    platform_connected = true
                                }
                                else if(platform == "STRAVA"){
                                    platform_connected = true
                                }
                                else if(platform == "GARMIN"){
                                    platform_connected = true
                                }
                                else if(platform == "FITBIT"){
                                    platform_connected = true
                                }else{
                                    platform_connected = false
                                }
                            } else {
                                platform_connected = false;
                            }
                        } else {
                            platform_connected = false;
                        }
                    }
                }
                runOnUiThread {
                    if (!platform_connected) {
                        findViewById<Button>(R.id.btn_connect).setOnClickListener {
                            sendGoogleAnalytics("challenges_connect","challenges")
                            val intent = Intent(this@Challenges, TrackActivity::class.java);
                            startActivity(intent);
                        }
                        if(!challenge_completed){
                            findViewById<LinearLayout>(R.id.challenge_connect_container).visibility = View.VISIBLE
                        }
                    } else {
                        findViewById<LinearLayout>(R.id.challenge_connect_container).visibility =
                            View.GONE
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
            }
        })
    }

    fun setupDetailsCard(challenge: JSONObject){
        findViewById<LinearLayout>(R.id.challenges_details_expand_button).setOnClickListener(){
            sendGoogleAnalytics("challenges_details_expand", "challenges")
            findViewById<LinearLayout>(R.id.challenges_details_content).visibility = View.VISIBLE;
            findViewById<LinearLayout>(R.id.challenges_details_expand_button).visibility = View.GONE;
        }
        findViewById<Button>(R.id.challenges_details_close_button).setOnClickListener {
            sendGoogleAnalytics("challenges_details_close", "challenges")
            findViewById<LinearLayout>(R.id.challenges_details_content).visibility = View.GONE;
            findViewById<LinearLayout>(R.id.challenges_details_expand_button).visibility = View.VISIBLE;
        }

        val challengeCloseDetailsButtonText = findViewById<Button>(R.id.challenges_details_close_button)

        val content = SpannableString(getString(R.string.mobile_challenges_details_close))
        content.setSpan(UnderlineSpan(), 0, content.length, 0)
        content.setSpan(ForegroundColorSpan(Color.parseColor(getStringVariable("PRIMARY_COLOR"))), 0, content.length, 0)

        challengeCloseDetailsButtonText.text = content

        val details_text = getSafeStringVariable(challenge, "details_text")
        val details_text_2 = getSafeStringVariable(challenge, "details_text_2")
        val details_text_3 = getSafeStringVariable(challenge, "details_text_3")
        val details_text_4 = getSafeStringVariable(challenge, "details_text_4")
        val details_text_5 = getSafeStringVariable(challenge, "details_text_5")

        val details_photo_url = getSafeStringVariable(challenge, "details_photo_url")
        val details_video_url = getSafeStringVariable(challenge, "details_video_url")

        val video_view = findViewById<VideoView>(R.id.challenges_details_video);
        val video_anchor_view = findViewById<FrameLayout>(R.id.challenges_details_video_frame);
        val image_view = findViewById<ImageView>(R.id.challenges_details_image);
        val text_view = findViewById<TextView>(R.id.challenges_details_text);
        val text_view_2 = findViewById<TextView>(R.id.challenges_details_text_2);
        val text_view_3 = findViewById<TextView>(R.id.challenges_details_text_3);
        val text_view_4 = findViewById<TextView>(R.id.challenges_details_text_4);
        val text_view_5 = findViewById<TextView>(R.id.challenges_details_text_5);

        if(details_text != ""){
            text_view.text = details_text
        }else{
            text_view.visibility = View.GONE
        }

        if(details_text_2 != ""){
            text_view_2.text = details_text_2
        }else{
            text_view_2.visibility = View.GONE
        }

        if(details_text_3 != ""){
            text_view_3.text = details_text_3
        }else{
            text_view_3.visibility = View.GONE
        }

        if(details_text_4 != ""){
            text_view_4.text = details_text_4
        }else{
            text_view_4.visibility = View.GONE
        }

        if(details_text_5 != ""){
            text_view_5.text = details_text_5
        }else{
            text_view_5.visibility = View.GONE
        }

        try {
            if (details_photo_url != "") {
                Glide.with(this@Challenges)
                    .load(details_photo_url)
                    .into(image_view)
                image_view.setVisibility(View.VISIBLE)
            } else {
                image_view.setVisibility(View.GONE)
            }
        }catch(e: Exception){
            image_view.setVisibility(View.GONE)
        }

        if(details_video_url != ""){
            val uri = Uri.parse(details_video_url)
            val mc = MediaController(this@Challenges)
            mc.setAnchorView(video_anchor_view)
            video_view.setMediaController(mc)
            video_view.setVideoURI(uri)
            video_view.setVisibility(View.VISIBLE)
            video_view.start()
            video_view.setOnClickListener{
                    mc.show()
                }
            
        }else{
            video_view.setVisibility(View.GONE)
        }
    }

    fun setupShareCard(challenge: JSONObject){

        val challenge_id = getSafeIntegerVariable(challenge, "id")

        findViewById<LinearLayout>(R.id.challenges_share_expand_button).setOnClickListener(){
            sendGoogleAnalytics("challenges_share_expand", "challenges")
            findViewById<LinearLayout>(R.id.challenges_share_content).visibility = View.VISIBLE;
            findViewById<LinearLayout>(R.id.challenges_share_expand_button).visibility = View.GONE;
        }

        findViewById<Button>(R.id.challenges_share_close_button).setOnClickListener {
            sendGoogleAnalytics("challenges_share_close", "challenges")
            findViewById<LinearLayout>(R.id.challenges_share_content).visibility = View.GONE;
            findViewById<LinearLayout>(R.id.challenges_share_expand_button).visibility = View.VISIBLE;
        }

        val challengeCloseShareButtonText = findViewById<Button>(R.id.challenges_share_close_button)

        val content = SpannableString(getString(R.string.mobile_challenges_share_close))
        content.setSpan(UnderlineSpan(), 0, content.length, 0)
        content.setSpan(ForegroundColorSpan(Color.parseColor(getStringVariable("PRIMARY_COLOR"))), 0, content.length, 0)

        challengeCloseShareButtonText.text = content

        var donationsMessageLayout = findViewById<LinearLayout>(R.id.challenges_share_card);
        donationsMessageLayout.setOnTouchListener(object : OnSwipeTouchListener(this@Challenges) {
            override fun onSwipeLeft() {
                super.onSwipeLeft();
                switchMessageSlide(currentMessagesSlideIndex + 1);
            }
            override fun onSwipeRight() {
                super.onSwipeRight();
                switchMessageSlide(currentMessagesSlideIndex - 1);
            }
        })

        val url = getResources().getString(R.string.base_server_url).plus("/")
            .plus(getStringVariable("CLIENT_CODE"))
            .plus("/configuration/messages/ACTIVITYCHALLENGE/")
            .plus(getConsID()).plus("/")
            .plus(getEvent().event_id).plus("/android/")
            .plus(getDeviceType()).plus("/")
            .plus(challenge_id)
        var request = Request.Builder()
            .url(url)
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
                    val jsonArray = JSONArray(jsonString);
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i);
                        var safeMessage = setupMessage(obj);
                        val message = Fundraise.FundraisingMessage(
                            safeMessage.text,
                            safeMessage.email_body,
                            safeMessage.subject,
                            safeMessage.url,
                            safeMessage.facebook_url,
                            safeMessage.linkedin_url,
                            safeMessage.email_url,
                            safeMessage.sms_url,
                            safeMessage.custom_content
                        )
                        challenge_messages += message
                    }

                    runOnUiThread(){
                        var donationsMessageLayout = findViewById<FrameLayout>(R.id.challenges_share_layout);

                        val inflater = LayoutInflater.from(this@Challenges)
                        val imageInflater = LayoutInflater.from(this@Challenges)
                        var i = 0;
                        totalMessagesSlideCount = challenge_messages.count();
                        for (message in challenge_messages) {
                            val binding: FundraisingMessageBinding = DataBindingUtil.inflate(
                                inflater, R.layout.fundraising_message ,donationsMessageLayout, true)
                            binding.colorList = getColorList("")
                            val row = donationsMessageLayout.getChildAt(i) as TextView
                            row.setText(message.text);
                            if(i == 0){
                                row.setVisibility(View.VISIBLE);
                            } else {
                                row.setVisibility(View.INVISIBLE);
                            }
                            i = i + 1;
                        }
                        totalMessagesSlideCount = i;
                        setupSlideButtons(totalMessagesSlideCount, R.id.challenges_share_slide_buttons, "messages")
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                findViewById<LinearLayout>(R.id.challenges_share_card).visibility = View.GONE
            }
        })

        val facebookShareButton = findViewById<FrameLayout>(R.id.facebook_share_button)
        facebookShareButton.setOnClickListener {
            sendGoogleAnalytics("challenges_facebook_share","challenges")
            sendSocialActivity("facebook")
            if(challenge_messages[currentMessagesSlideIndex].custom_content){
                shareFacebook(this,challenge_messages[currentMessagesSlideIndex].facebook_url)
            }else{
                displayAlert(
                    resources.getString(R.string.mobile_fundraise_message_facebook_prompt), ""
                ) { childviewCallback("facebook","") }
                setAlertSender(facebookShareButton)
            }

        }

        val emailShareButton = findViewById<FrameLayout>(R.id.email_share_button)
        emailShareButton.setOnClickListener {
            sendGoogleAnalytics("challenges_email_share","challenges")
            sendSocialActivity("email")
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:") // only email apps should handle this
            intent.putExtra(Intent.EXTRA_SUBJECT, challenge_messages[currentMessagesSlideIndex].subject)
            if(challenge_messages[currentMessagesSlideIndex].custom_content){
                intent.putExtra(Intent.EXTRA_TEXT, (challenge_messages[currentMessagesSlideIndex].email_url))
            }else {
                intent.putExtra(Intent.EXTRA_TEXT,
                    (challenge_messages[currentMessagesSlideIndex].email_body).replace("<br>","\r\n").plus("\r\n\r\n")
                        .plus(challenge_messages[currentMessagesSlideIndex].email_url)
                )
            }
            startActivity(Intent.createChooser(intent,getResources().getString(R.string.mobile_fundraise_share_dialog_title)))
        }

        val smsShareButton = findViewById<FrameLayout>(R.id.sms_share_button)
        smsShareButton.setOnClickListener {
            sendGoogleAnalytics("challenges_sms_share","challenges")
            sendSocialActivity("sms")
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("smsto:") // only email apps should handle this
            //intent.putExtra(Intent.EXTRA_SUBJECT, fundraisingMessages[currentSlideIndex].subject)
            if(challenge_messages[currentMessagesSlideIndex].custom_content){
                intent.putExtra(Intent.EXTRA_TEXT, (challenge_messages[currentMessagesSlideIndex].sms_url))
            }else {
                intent.putExtra(Intent.EXTRA_TEXT,
                    challenge_messages[currentMessagesSlideIndex].text.plus(" ")
                        .plus(challenge_messages[currentMessagesSlideIndex].sms_url)
                )
            }
            startActivity(Intent.createChooser(intent,getResources().getString(R.string.mobile_fundraise_share_dialog_title)))
        }

        val linkedinShareButton = findViewById<FrameLayout>(R.id.linkedin_share_button)
        linkedinShareButton.setOnClickListener {
            sendGoogleAnalytics("challenges_linkedin_share","challenges")
            sendSocialActivity("linkedin")
            if(challenge_messages[currentMessagesSlideIndex].custom_content){
                shareLinkedIn(challenge_messages[currentMessagesSlideIndex].linkedin_url )
            }else {
                displayAlert(
                    resources.getString(R.string.mobile_fundraise_message_linkedin_prompt), ""
                ) { childviewCallback("linkedin","") }
                setAlertSender(linkedinShareButton)
            }
        }

        if(getStringVariable("LINKEDIN_DISABLED") == "true"){
            linkedinShareButton.visibility = View.GONE;
        }else{
            linkedinShareButton.visibility = View.VISIBLE;
        }
    }

    fun shareLinkedIn(url: String){
        val url = "https://www.linkedin.com/shareArticle?mini=true&url=".plus(url)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    fun setupLeaderboardCard(challenge: JSONObject){
        var path = "/activity/personalChallengeLeaderboard"

        val challenge_metric = getSafeStringVariable(challenge, "activity_metric")
        if(challenge_metric == "DISTANCE"){
            path = "/activity/personalChallengeDistanceLeaderboard"
        }
        var challenge_status = getSafeStringVariable(challenge, "challenge_status")
        val challenge_id = getSafeIntegerVariable(challenge, "id")
        var url = getResources().getString(R.string.base_server_url).plus("/")
            .plus(getStringVariable("CLIENT_CODE"))
            .plus(path)
            .plus("/").plus(getConsID())
            .plus("/").plus(getEvent().event_id)
            .plus("/").plus(challenge_id)


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
                    val jsonString = response.body?.string()!!;
                    val obj = JSONObject(jsonString)
                    if(obj.has("data") && obj.get("data") is JSONArray){
                        val json = obj.get("data") as JSONArray;
                        var length = json.length()-1;
                        if(length > 9){
                            length = 9;
                        }
                        leaderboard_participants = emptyArray()
                        for(i in 0 .. length){
                            try {
                                var obj = json.getJSONObject(i);
                                var name = ""
                                val first_name = getSafeStringVariable(obj, "firstName")
                                val last_name = getSafeStringVariable(obj, "lastName")

                                var rank = ""
                                if(getSafeIntegerVariable(obj, "rank") != 0){
                                    rank = getSafeIntegerVariable(obj, "rank").toString() + "."
                                }

                                if(last_name != ""){
                                    name = first_name + " " + last_name
                                }else{
                                    name = first_name
                                }

                                var value = 0.00;
                                if(activity_metric == "distance"){
                                    value = getSafeDoubleVariable(obj, "totalDistance");
                                }

                                leaderboard_participants += challengeParticipant(
                                    rank,
                                    name,
                                    getSafeIntegerVariable(obj, "totalPoints"),
                                    value,
                                );
                            } catch (exception: IOException) {
                                println("GET LEADERBOARD ERROR")
                            }
                        }
                    }

                    runOnUiThread {
                        val leaderboard_table = findViewById<TableLayout>(R.id.leaderboard_table);
                        addRows(leaderboard_table, leaderboard_participants);
                        var leaderboad_length = 1;
                        if(getStringVariable("HAS_TEAM") == "true") {
                            leaderboad_length += 1;
                            loadTeamLeaderboardPage(challenge)
                        }

                        if(challenge_status == "Ended"){
                            findViewById<LinearLayout>(R.id.challenges_share_card).setVisibility(View.GONE)
                        }else{
                            findViewById<LinearLayout>(R.id.challenges_share_card).setVisibility(View.VISIBLE)
                        }

                        if(getStringVariable("HAS_COMPANY") == "true" && getStringVariable("DISABLE_COMPANY_CHALLENGE_LEADERBOARD") != "true") {
                            leaderboad_length += 1;
                            loadCompanyLeaderboardPage(challenge)
                        }
                        total_leaderboard_slides = leaderboad_length
                        setupSlideButtons(leaderboad_length, R.id.leaderboard_slide_buttons_container, "leaderboard")
                        findViewById<LinearLayout>(R.id.challenges_leaderboard_card_container).setVisibility(View.VISIBLE)
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                //println(e.message.toString());
            }
        })
    }

    fun loadTeamLeaderboardPage(challenge: JSONObject){
        var path = "/activity/teamChallengeLeaderboard"

        val challenge_metric = getSafeStringVariable(challenge, "activity_metric")
        if(challenge_metric == "DISTANCE"){
            path = "/activity/teamChallengeDistanceLeaderboard"
        }

        val challenge_id = getSafeIntegerVariable(challenge, "id")
        val url = getResources().getString(R.string.base_server_url).plus("/")
            .plus(getStringVariable("CLIENT_CODE"))
            .plus(path)
            .plus("/").plus(getConsID())
            .plus("/").plus(getEvent().event_id)
            .plus("/").plus(challenge_id)

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
                    val jsonString = response.body?.string()!!;
                    val obj = JSONObject(jsonString)

                    if(obj.has("data") && obj.get("data") is JSONArray){
                        val json = obj.get("data") as JSONArray;
                        var length = json.length()-1;
                        if(length > 9){
                            length = 9;
                        }
                        leaderboard_teams = emptyArray()
                        for(i in 0 .. length){
                            try {
                                var obj = json.getJSONObject(i);

                                var rank = ""
                                if(getSafeIntegerVariable(obj, "rank") != 0){
                                    rank = getSafeIntegerVariable(obj, "rank").toString() + "."
                                }

                                println("OBJ")
                                println(obj)

                                var value = 0.00
                                if(activity_metric == "distance"){
                                    value = getSafeDoubleVariable(obj, "totalDistance");
                                }

                                leaderboard_teams += challengeParticipant(
                                    rank,
                                    getSafeStringVariable(obj, "teamName"),
                                    getSafeIntegerVariable(obj, "totalPoints"),
                                    value,
                                );
                            } catch (exception: IOException) {
                                println("GET LEADERBOARD ERROR")
                            }
                        }
                    }

                    runOnUiThread {

                        val second_leaderboard_page =
                            findViewById<LinearLayout>(R.id.team_leaderboard_container);
                        if(second_leaderboard_page != null){
                            second_leaderboard_page.setVisibility(View.INVISIBLE);
                        }

                        val team_leaderboard_table = findViewById<TableLayout>(R.id.leaderboard_teams_table);
                        addRows(team_leaderboard_table, leaderboard_teams);
                        current_leaderboard_slide = 0;

                        val leaderboardScreenLayout = findViewById<LinearLayout>(R.id.challenges_leaderboard_card_container)
                        leaderboardScreenLayout.setOnTouchListener(object :
                            OnSwipeTouchListener(this@Challenges) {

                            override fun onSwipeLeft() {
                                super.onSwipeLeft()
                                switchLeaderboardSlide(current_leaderboard_slide + 1)
                            }

                            override fun onSwipeRight() {
                                switchLeaderboardSlide(current_leaderboard_slide - 1)
                            }
                        })
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                //println(e.message.toString());
            }
        })
    }

    fun loadCompanyLeaderboardPage(challenge: JSONObject){
        var path = "/activity/companyChallengeLeaderboard"

        val challenge_metric = getSafeStringVariable(challenge, "activity_metric")

        if(challenge_metric == "DISTANCE"){
            path = "/activity/companyChallengeDistanceLeaderboard"
        }

        val challenge_id = getSafeIntegerVariable(challenge, "id")
        val url = getResources().getString(R.string.base_server_url).plus("/")
            .plus(getStringVariable("CLIENT_CODE"))
            .plus(path)
            .plus("/").plus(getConsID())
            .plus("/").plus(getEvent().event_id)
            .plus("/").plus(challenge_id)

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
                    val jsonString = response.body?.string()!!;
                    val obj = JSONObject(jsonString)
                    println("ORIGINAL OBJ")
                    println(obj)
                    if(obj.has("data") && obj.get("data") is JSONArray){
                        val json = obj.get("data") as JSONArray;
                        var length = json.length()-1;
                        if(length > 9){
                            length = 9;
                        }
                        leaderboard_companies = emptyArray()
                        for(i in 0 .. length){
                            try {
                                var obj = json.getJSONObject(i);

                                var rank = ""
                                if(getSafeIntegerVariable(obj, "rank") != 0){
                                    rank = getSafeIntegerVariable(obj, "rank").toString() + "."
                                }

                                var value = 0.00
                                if(activity_metric == "distance"){
                                    value = getSafeDoubleVariable(obj, "totalDistance");
                                }

                                leaderboard_companies += challengeParticipant(
                                    rank,
                                    getSafeStringVariable(obj, "companyName"),
                                    getSafeIntegerVariable(obj, "totalPoints"),
                                    value,
                                );
                            } catch (exception: IOException) {
                                println("GET LEADERBOARD ERROR")
                            }
                        }
                    }

                    runOnUiThread {
                        val third_leaderboard_page = findViewById<LinearLayout>(R.id.company_leaderboard_container);
                        if(third_leaderboard_page != null){
                            third_leaderboard_page.setVisibility(View.INVISIBLE);
                        }

                        val company_leaderboard_table = findViewById<TableLayout>(R.id.leaderboard_companies_table);
                        addRows(company_leaderboard_table, leaderboard_companies);
                        current_leaderboard_slide = 0;

                        val leaderboardScreenLayout = findViewById<LinearLayout>(R.id.challenges_leaderboard_card_container)
                        leaderboardScreenLayout.setOnTouchListener(object :
                            OnSwipeTouchListener(this@Challenges) {

                            override fun onSwipeLeft() {
                                super.onSwipeLeft()
                                switchLeaderboardSlide(current_leaderboard_slide + 1)
                            }

                            override fun onSwipeRight() {
                                switchLeaderboardSlide(current_leaderboard_slide - 1)
                            }
                        })
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                //println(e.message.toString());
            }
        })
    }

    fun setupLeaderboardDollarsCard(challenge: JSONObject){
        val challenge_id = getSafeIntegerVariable(challenge, "id")
        val url = getResources().getString(R.string.base_server_url).plus("/")
            .plus(getStringVariable("CLIENT_CODE"))
            .plus("/activity/personalChallengeDollarsLeaderboard")
            .plus("/").plus(getConsID())
            .plus("/").plus(getEvent().event_id)
            .plus("/").plus(challenge_id)

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
                    val jsonString = response.body?.string()!!;
                    val obj = JSONObject(jsonString)
                    if(obj.has("data") && obj.get("data") is JSONArray){
                        val json = obj.get("data") as JSONArray;
                        var length = json.length()-1;
                        if(length > 9){
                            length = 9;
                        }
                        leaderboard_amount_participants = emptyArray()
                        for(i in 0 .. length){
                            try {
                                var obj = json.getJSONObject(i);
                                var name = ""
                                val first_name = getSafeStringVariable(obj, "firstName")
                                val last_name = getSafeStringVariable(obj, "lastName")

                                var rank = ""
                                if(getSafeIntegerVariable(obj, "rank") != 0){
                                    rank = getSafeIntegerVariable(obj, "rank").toString() + "."
                                }

                                if(last_name != ""){
                                    name = first_name + " " + last_name
                                }else{
                                    name = first_name
                                }

                                leaderboard_amount_participants += challengeParticipant(
                                    rank,
                                    name,
                                    0,
                                    getSafeDoubleVariable(obj, "totalDollars"),
                                );
                            } catch (exception: IOException) {
                                println("GET LEADERBOARD ERROR")
                            }
                        }
                    }

                    runOnUiThread {
                        val leaderboard_table = findViewById<TableLayout>(R.id.leaderboard_amount_table);
                        addMoneyRows(leaderboard_table, leaderboard_amount_participants);
                        var leaderboad_length = 1;
                        if(getStringVariable("HAS_TEAM") == "true") {
                            leaderboad_length += 1;
                            loadDollarsTeamLeaderboardPage(challenge)
                        }
                        
                        if(getStringVariable("HAS_COMPANY") == "true") {
                            leaderboad_length += 1;
                            loadDollarsCompanyLeaderboardPage(challenge)
                        }
                        total_leaderboard_slides = leaderboad_length
                        setupSlideButtons(leaderboad_length, R.id.amount_leaderboard_slide_buttons_container, "amount-leaderboard")
                        findViewById<LinearLayout>(R.id.challenges_leaderboard_card_container).setVisibility(View.VISIBLE)
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                //println(e.message.toString());
            }
        })
    }

    fun loadDollarsTeamLeaderboardPage(challenge: JSONObject){
        val challenge_id = getSafeIntegerVariable(challenge, "id")
        val url = getResources().getString(R.string.base_server_url).plus("/")
            .plus(getStringVariable("CLIENT_CODE"))
            .plus("/activity/teamChallengeDollarsLeaderboard")
            .plus("/").plus(getConsID())
            .plus("/").plus(getEvent().event_id)
            .plus("/").plus(challenge_id)

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
                    val jsonString = response.body?.string()!!
                    val obj = JSONObject(jsonString)

                    if(obj.has("data") && obj.get("data") is JSONArray){
                        val json = obj.get("data") as JSONArray;
                        var length = json.length()-1;
                        if(length > 9){
                            length = 9;
                        }
                        leaderboard_amount_teams = emptyArray()
                        for(i in 0 .. length){
                            try {
                                var obj = json.getJSONObject(i);

                                var rank = ""
                                if(getSafeIntegerVariable(obj, "rank") != 0){
                                    rank = getSafeIntegerVariable(obj, "rank").toString() + "."
                                }

                                leaderboard_amount_teams += challengeParticipant(
                                    rank,
                                    getSafeStringVariable(obj, "teamName"),
                                    0,
                                    getSafeDoubleVariable(obj, "totalDollars"),
                                );
                            } catch (exception: IOException) {
                                println("GET LEADERBOARD ERROR")
                            }
                        }
                    }

                    runOnUiThread {

                        val second_leaderboard_page =
                            findViewById<LinearLayout>(R.id.team_amount_leaderboard_container);
                        if(second_leaderboard_page != null){
                            second_leaderboard_page.setVisibility(View.INVISIBLE);
                        }

                        val team_leaderboard_table = findViewById<TableLayout>(R.id.amount_leaderboard_teams_table);
                        addMoneyRows(team_leaderboard_table, leaderboard_amount_teams);
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                //println(e.message.toString());
            }
        })
    }

    fun loadDollarsCompanyLeaderboardPage(challenge: JSONObject){
        val challenge_id = getSafeIntegerVariable(challenge, "id")
        val url = getResources().getString(R.string.base_server_url).plus("/")
            .plus(getStringVariable("CLIENT_CODE"))
            .plus("/activity/companyChallengeDollarsLeaderboard")
            .plus("/").plus(getConsID())
            .plus("/").plus(getEvent().event_id)
            .plus("/").plus(challenge_id)

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
                    val jsonString = response.body?.string()!!
                    val obj = JSONObject(jsonString)

                    if(obj.has("data") && obj.get("data") is JSONArray){
                        val json = obj.get("data") as JSONArray;
                        var length = json.length()-1;
                        if(length > 9){
                            length = 9;
                        }
                        leaderboard_amount_companies = emptyArray();
                        for(i in 0 .. length){
                            try {
                                var obj = json.getJSONObject(i);
                                var rank = ""
                                if(getSafeIntegerVariable(obj, "rank") != 0){
                                    rank = getSafeIntegerVariable(obj, "rank").toString() + "."
                                }

                                leaderboard_amount_companies += challengeParticipant(
                                    rank,
                                    getSafeStringVariable(obj, "companyName"),
                                    0,
                                    getSafeDoubleVariable(obj, "totalDollars"),
                                );
                            } catch (exception: IOException) {
                                println("GET LEADERBOARD ERROR")
                            }
                        }
                    }

                    runOnUiThread {
                        val third_leaderboard_page = findViewById<LinearLayout>(R.id.company_amount_leaderboard_container);
                        if(third_leaderboard_page != null){
                            third_leaderboard_page.setVisibility(View.INVISIBLE);
                        }

                        val company_leaderboard_table = findViewById<TableLayout>(R.id.amount_leaderboard_companies_table);
                        addMoneyRows(company_leaderboard_table, leaderboard_amount_companies);
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                //println(e.message.toString());
            }
        })
    }

    fun switchLeaderboardSlide(newIndex:Int){
        switchSlideButton(newIndex + 1,total_leaderboard_slides,R.id.leaderboard_slide_buttons_container)
        switchSlideButton(newIndex + 1,total_leaderboard_slides,R.id.amount_leaderboard_slide_buttons_container)
        var leaderboardLayout = findViewById<FrameLayout>(R.id.challenges_leaderboard_card);
        var amountLeaderboardLayout = findViewById<FrameLayout>(R.id.challenges_amount_leaderboard_card);
        if((newIndex >= 0) and (newIndex < total_leaderboard_slides)){
            leaderboardLayout.getChildAt(current_leaderboard_slide).setVisibility(View.INVISIBLE);
            leaderboardLayout.getChildAt(newIndex).setVisibility(View.VISIBLE);
            amountLeaderboardLayout.getChildAt(current_leaderboard_slide).setVisibility(View.INVISIBLE);
            amountLeaderboardLayout.getChildAt(newIndex).setVisibility(View.VISIBLE);
            (((leaderboardLayout.getChildAt(newIndex) as LinearLayout).getChildAt(0) as LinearLayout).getChildAt(0)).requestFocus()
            current_leaderboard_slide = newIndex;
        }
    }

    private fun addRows(leaderboardTable: TableLayout, leaderboardParticipants: Array<challengeParticipant>) {
        removeAllRows(leaderboardTable,{
            for (i in 0..leaderboardParticipants.size - 1) {
                val inflater =
                    applicationContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val row = inflater.inflate(R.layout.leaderboard_row, null) as TableRow
                (row.getChildAt(0) as TextView).setText((leaderboardParticipants[i].rank) + " " + leaderboardParticipants[i].name)
                if(activity_metric != "distance"){
                    (row.getChildAt(1) as TextView).setText(leaderboardParticipants[i].points.toString())
                }else {
                    var value = withCommas(leaderboardParticipants[i].raised);
                    value = value.replace(".00","");
                    if(value.contains('.') && value.last() == '0' && value != "0"){
                        value = value.substring(0, value.length - 1)
                    }
                    try{
                        (row.getChildAt(1) as TextView).setText(withCommas(toDouble(value)));
                    }catch(e: Exception){
                        (row.getChildAt(1) as TextView).setText("0.00");
                    }
                }
                leaderboardTable.addView(row, i+2)
            }
        })
    }

    private fun addMoneyRows(leaderboardTable: TableLayout, leaderboardParticipants: Array<challengeParticipant>) {
        removeAllRows(leaderboardTable, {
            for (i in 0..leaderboardParticipants.size - 1) {
                val inflater =
                    applicationContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val row = inflater.inflate(R.layout.leaderboard_row, null) as TableRow
                (row.getChildAt(0) as TextView).setText((leaderboardParticipants[i].rank) + " " + leaderboardParticipants[i].name)
                (row.getChildAt(1) as TextView).setText(formatDoubleToLocalizedCurrency(leaderboardParticipants[i].raised))
                leaderboardTable.addView(row, i + 2)
            }
        })
    }

    fun switchMessageSlide(newIndex:Int){
        switchSlideButton(newIndex + 1,totalMessagesSlideCount,R.id.challenges_share_slide_buttons)
        var challengesMessageLayout = findViewById<FrameLayout>(R.id.challenges_share_layout);
        if((newIndex >= 0) and (newIndex < totalMessagesSlideCount)){
            challengesMessageLayout.getChildAt(currentMessagesSlideIndex).setVisibility(View.INVISIBLE);
            challengesMessageLayout.getChildAt(newIndex).setVisibility(View.VISIBLE);
            challengesMessageLayout.getChildAt(newIndex).requestFocus()
            currentMessagesSlideIndex = newIndex;
        }
    }
}

class challengeParticipant(
    val rank: String,
    val name: String,
    val points: Int,
    val raised: Double,
)
