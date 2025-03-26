package com.nuclavis.rospark

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.databinding.DataBindingUtil
import app.com.kotlinapp.OnSwipeTouchListener
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.nuclavis.rospark.databinding.BadgeBinding
import com.nuclavis.rospark.databinding.BadgesRowBinding
import com.nuclavis.rospark.databinding.FinnsCardChallengeRowBinding
import com.nuclavis.rospark.databinding.FinnsMissionRowBinding
import com.nuclavis.rospark.databinding.JerseyRowBinding
import com.nuclavis.rospark.databinding.LuminaryRowBinding
import com.nuclavis.rospark.databinding.OverviewAdditionalSponsorsCardBinding
import com.nuclavis.rospark.databinding.OverviewAhcFinnsCardBinding
import com.nuclavis.rospark.databinding.OverviewBadgesCardBinding
import com.nuclavis.rospark.databinding.OverviewBannerCardBinding
import com.nuclavis.rospark.databinding.OverviewChallengesCardBinding
import com.nuclavis.rospark.databinding.OverviewCprCardBinding
import com.nuclavis.rospark.databinding.OverviewCustomImageCardBinding
import com.nuclavis.rospark.databinding.OverviewEventCheckinCardBinding
import com.nuclavis.rospark.databinding.OverviewEventProgressCardBinding
import com.nuclavis.rospark.databinding.OverviewImpactBadgeBinding
import com.nuclavis.rospark.databinding.OverviewImpactBadgesCardBinding
import com.nuclavis.rospark.databinding.OverviewImpactPointsCardBinding
import com.nuclavis.rospark.databinding.OverviewJerseyCardBinding
import com.nuclavis.rospark.databinding.OverviewKhcFinnsCardBinding
import com.nuclavis.rospark.databinding.OverviewLeaderboardCardBinding
import com.nuclavis.rospark.databinding.OverviewLocalSponsorsCardBinding
import com.nuclavis.rospark.databinding.OverviewLogoCardBinding
import com.nuclavis.rospark.databinding.OverviewLuminariesCardBinding
import com.nuclavis.rospark.databinding.OverviewNationalSponsorsCardBinding
import com.nuclavis.rospark.databinding.OverviewPhotoFiltersCardBinding
import com.nuclavis.rospark.databinding.OverviewProgressCardBinding
import com.nuclavis.rospark.databinding.OverviewPromiseGardenCardBinding
import com.nuclavis.rospark.databinding.OverviewWeeklyStrategyCardBinding
import com.nuclavis.rospark.databinding.TopClassroomsRowBinding
//import com.unity3d.player.UnityPlayerActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.properties.Delegates


open class Overview : BaseActivity() {

    private var xDistance = 0f
    private var yDistance = 0f
    private var lastX = 0f
    private var lastY = 0f

    var isUnityLoaded = false;

    var UNITY_GARDEN_REQUEST = 5;
    var UNITY_LUMINARY_REQUEST = 6;
    var UNITY_PHOTO_FILTERS_REQUEST = 7;
    var UNITY_JERSEY_REQUEST = 8;

    var eventProgressPercent: Int by Delegates.observable(0) { _, old, new ->
        run {
            if (old == 0 && new != 0) {
               resizeProgressBar(eventProgressPercent.toDouble()/100, "Event")
            }
        };
    }

    var personalProgressPercent: Int by Delegates.observable(0) { _, old, new ->
        run {
            if (old == 0 && new != 0) {
                resizeProgressBar(personalProgressPercent.toDouble()/100, "Personal")
            }
        };
    }

    var teamProgressPercent: Int by Delegates.observable(0) { _, old, new ->
        run {
            if (old == 0 && new != 0) {
                resizeProgressBar(teamProgressPercent.toDouble()/100, "Team")
            }
        };
    }

    var companyProgressPercent: Int by Delegates.observable(0) { _, old, new ->
        run {
            if (old == 0 && new != 0) {
                resizeProgressBar(companyProgressPercent.toDouble()/100, "Company")
            }
        };
    }

    var personalChallengePercent: Int by Delegates.observable(0) { _, old, new ->
        run {
            if (old == 0 && new != 0) {
                resizeProgressBar(personalChallengePercent.toDouble()/100, "Challenge")
            }
        };
    }

    var progressBarWidth = 0;
    var progressBarHeight = 0;

    var hasTeam = false;
    var isTeamCaptain = false;
    var hasCompany = false;

    var current_leaderboard_slide = 0;
    var total_leaderboard_slides = 0;
    var current_progress_slide = 0;
    var total_progress_slides = 0;
    var current_challenges_slide = 0;
    var total_challenge_slides = 0;
    var current_garden_slide = 1;
    var has_flowers = false;
    var current_luminaries_slide = 1;
    var has_luminaries = false;
    var has_jerseys = false;
    var current_jerseys_slide = 1;
    var leaderboard_participants = emptyArray<Participant>();
    var leaderboard_teams = emptyArray<Participant>();
    var leaderboard_companies = emptyArray<Participant>();
    var leaderboard_students = emptyList<FinnStudent>();
    var leaderboard_classrooms = emptyList<TopClassroom>();
    var activity_leaderboard_participants = emptyArray<ActivityParticipant>();
    var activity_leaderboard_teams = emptyArray<ActivityParticipant>();
    var activity_leaderboard_companies = emptyArray<ActivityParticipant>();
    var challenge_leaderboard_participants = emptyArray<challengeParticipant>();
    var challenge_leaderboard_teams = emptyArray<challengeParticipant>();
    var challenge_leaderboard_companies = emptyArray<challengeParticipant>();
    var personal_raised = 0.00;
    var challenge_metric = "";
    var current_challenge_type = "";
    var current_challenge_status = "";
    var isLoadingUrl = false;

    override fun slideButtonCallback(card: Any, forward:Boolean){
        var slide = "";
        if(card == "progress"){
            var currentIndex = current_progress_slide;
            if(forward){
                currentIndex += 1;
            }else{
                currentIndex -= 1;
            }
            switchProgressSlide(currentIndex)
        } else if(card == "garden"){
            val currentIndex = current_garden_slide;
            if(forward){
                current_garden_slide += 1;
            }else{
                current_garden_slide -= 1;
            }

            if(current_garden_slide == 1){
                slide = "first"
            }else if (current_garden_slide == 2){
                slide = "second"
            }else if(current_garden_slide == 3) {
                slide = "third"
            }else{
                current_garden_slide = currentIndex;
            }
        }else if(card == "luminaries"){
            val currentIndex = current_luminaries_slide;
            if(forward){
                current_luminaries_slide += 1;
            }else{
                current_luminaries_slide -= 1;
            }

            if(current_luminaries_slide == 1){
                slide = "first"
            }else if (current_luminaries_slide == 2){
                slide = "second"
            }else if(current_luminaries_slide == 3) {
                slide = "third"
            }else{
                current_luminaries_slide = currentIndex;
            }
        }else if(card == "jerseys"){
            val currentIndex = current_jerseys_slide;
            if(forward){
                current_jerseys_slide += 1;
            }else{
                current_jerseys_slide -= 1;
            }

            if(current_jerseys_slide == 1){
                slide = "first"
            }else if (current_jerseys_slide == 2){
                slide = "second"
            }else if(current_jerseys_slide == 3) {
                slide = "third"
            }else{
                current_jerseys_slide = currentIndex;
            }
        }else if(card == "overview_challenges"){
            var currentIndex = current_challenges_slide;
            if(forward){
                currentIndex += 1;
            }else{
                currentIndex -= 1;
            }
            switchChallengeSlide(currentIndex)
        }else if(card == "leaderboard"){
            var currentIndex = current_leaderboard_slide;
            if(forward){
                currentIndex += 1;
            }else{
                currentIndex -= 1;
            }
            switchLeaderboardSlide(currentIndex)
        }else{
            if(forward){
                slide="second"
            }else{
                slide="first"
            }
        }

        if(slide != ""){
            switchSlide(card as String, slide)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        recolorTheme()
        super.onCreate(savedInstanceState)
        setPageContent(R.layout.overview, "overview");
        setTitle(getResources().getString(R.string.mobile_main_menu_overview));
        sendGoogleAnalytics("overview_view", "overview")
        val linear = findViewById<LinearLayout>(R.id.overview_content)
        for (childView in linear.children) {
            linear.removeView(childView);
        }

        if (getStringVariable("CLIENT_CODE") == "ahayouthmarket" && getStringVariable("GIFTS_CHECKED") == "") {
            getGiftsEnabled()
        }

        //Set Up Views
        val inflater = layoutInflater
        val overview_logo_card: OverviewLogoCardBinding = DataBindingUtil.inflate(
            inflater, R.layout.overview_logo_card, linear, true
        )
        overview_logo_card.colorList = getColorList("")

        var vo_string = getStringVariable("APP_NAME") + "logo";
        if(getStringVariable("LOGO_VOICEOVER") != ""){
            vo_string = getStringVariable("LOGO_VOICEOVER");
        }
        findViewById<ImageView>(R.id.overview_page_logo).contentDescription = vo_string

        setVariable("EVENT_CHECKIN_ENABLED", "true")

        if (getStringVariable("EVENT_CHECKIN_ENABLED") == "true") {
            val overview_event_checkin_card: OverviewEventCheckinCardBinding =
                DataBindingUtil.inflate(
                    inflater, R.layout.overview_event_checkin_card, linear, true
                )
            overview_event_checkin_card.colorList = getColorList("")

            val sdf = SimpleDateFormat("YYYY-MM-d");
            val currentDate = sdf.format(Date());

            val url = getResources().getString(R.string.base_server_url).plus("/")
                .plus(getStringVariable("CLIENT_CODE")).plus("/user/checkin/").plus(getConsID())
                .plus("/").plus(getEvent().event_id).plus("/").plus(currentDate)
            var request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ".plus(getAuth()))
                .addHeader("Program-Id", getStringVariable("PROGRAM_ID"))
                .build()

            var client = OkHttpClient();

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: okhttp3.Response) {
                    if (response.code != 200) {
                        throw Exception(response.body?.string())
                        findViewById<LinearLayout>(R.id.overview_event_checkin_card).visibility =
                            View.GONE
                    } else {
                        val jsonString = response.body?.string();
                        val obj = JSONObject(jsonString);
                        if (obj.has("data") && obj.get("data") is JSONObject) {
                            val data = obj.get("data") as JSONObject;
                            if (getSafeBooleanVariable(data, "display_checkin_card")) {
                                if (getSafeStringVariable(
                                        data,
                                        "event_checkin_type"
                                    ) == "GRASSROOTS"
                                ) {
                                    runOnUiThread {
                                        findViewById<LinearLayout>(R.id.overview_event_barcode_check_in_section).visibility =
                                            View.VISIBLE
                                        findViewById<Button>(R.id.event_checkin_get_barcode_btn).setOnClickListener {
                                            sendGoogleAnalytics("event_checkin_get_barcode", "overview")
                                            displayQRAlert("", getString(R.string.mobile_overview_event_checkin_barcode_modal_text), getSafeStringVariable(data,"event_checkin_grassroots_url"))
                                        }
                                    }
                                } else {
                                    runOnUiThread {
                                        updateCheckinCard(
                                            getSafeBooleanVariable(
                                                data,
                                                "user_checked_in"
                                            )
                                        )
                                        findViewById<Button>(R.id.event_checkin_btn).setOnClickListener {
                                            sendGoogleAnalytics("event_checkin", "overview")
                                            checkInForEvent("true")

                                            if(getStringVariable("EVENT_CHECKIN_TSHIRT_PROMPT_ENABLED") == "true"){
                                                val raised = getStringVariable("PERSONAL_RAISED").replace("$","").toDouble()
                                                val threshold = getStringVariable("EVENT_CHECKIN_TSHIRT_PROMPT_AMOUNT").replace("$","").toDouble()
                                                if(raised >= threshold){
                                                    displayAlert("checkinTshirt")
                                                }
                                            }
                                        }
                                    }
                                }

                            } else {
                                runOnUiThread {
                                    findViewById<LinearLayout>(R.id.overview_event_checkin_card).visibility =
                                        View.GONE
                                }
                            }
                        } else {
                            runOnUiThread {
                                findViewById<LinearLayout>(R.id.overview_event_checkin_card).visibility =
                                    View.GONE
                            }
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println(e.message.toString());
                    runOnUiThread {
                        findViewById<LinearLayout>(R.id.overview_event_checkin_card).visibility =
                            View.GONE
                    }
                }
            })
        }

        if(getStringVariable("AHA_KHC_FINNS_MISSION") != "true" && getStringVariable("AHA_AHC_FINNS_MISSION") != "true"){
            if (getStringVariable("BANNER_TILE_ENABLED") == "true") {
                val overview_banner_card: OverviewBannerCardBinding = DataBindingUtil.inflate(
                    inflater, R.layout.overview_banner_card, linear, true
                )
                overview_banner_card.colorList = getColorList("")

                findViewById<Button>(R.id.banner_btn).setOnClickListener {
                    val intent = Intent(this@Overview, Fundraise::class.java);
                    startActivity(intent);
                }
            }
        }

        if (getStringVariable("AHA_KHC_FINNS_MISSION") == "true") {
            val finns_card: OverviewKhcFinnsCardBinding = DataBindingUtil.inflate(
                inflater, R.layout.overview_khc_finns_card, linear, true
            )
             if (getStringVariable("BANNER_TILE_ENABLED") == "true") {
                findViewById<LinearLayout>(R.id.khc_overview_banner_card).visibility = View.VISIBLE

                val url = resources.getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/messages/TEXTNOW/").plus(getConsID()).plus("/").plus(getEvent().event_id).plus("/android/").plus(getDeviceType())
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                    .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                    .build()

                val client = OkHttpClient()
                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        if(response.code != 200){
                            throw Exception(response.body?.string())
                        }else{
                            val jsonString = response.body?.string()
                            val jsonArray = JSONArray(jsonString)
                            if(jsonArray.length() > 0){
                                val obj = jsonArray.getJSONObject(0)

                                var safeMessage = setupMessage(obj);

                                findViewById<LinearLayout>(R.id.banner_link).setOnClickListener {
                                    sendGoogleAnalytics("challenges_sms_share","challenges")
                                    sendSocialActivity("sms")
                                    val intent = Intent(Intent.ACTION_SENDTO)
                                    intent.data = Uri.parse("smsto:")
                                    if(safeMessage.custom_content){
                                        intent.putExtra(Intent.EXTRA_TEXT, (obj.get("sms_url") as String))
                                    }else {
                                        intent.putExtra(Intent.EXTRA_TEXT,
                                            safeMessage.text.plus(" ").plus(obj.get("sms_url"))
                                        )
                                    }
                                    startActivity(Intent.createChooser(intent,getResources().getString(R.string.mobile_fundraise_share_dialog_title)))
                                }
                            }else{
                                findViewById<LinearLayout>(R.id.banner_link).setOnClickListener {
                                    sendGoogleAnalytics("challenges_sms_share","challenges")
                                    sendSocialActivity("sms")
                                    val intent = Intent(Intent.ACTION_SENDTO)
                                    intent.data = Uri.parse("smsto:")
                                    intent.putExtra(Intent.EXTRA_TEXT,"")
                                    startActivity(Intent.createChooser(intent,getResources().getString(R.string.mobile_fundraise_share_dialog_title)))
                                }
                            }
                        }
                        val bannerLinkTextView = findViewById<TextView>(R.id.banner_link_text)
                        val bannerLinkText = bannerLinkTextView.text.toString()
                        val bannerLinkContentDescription = "$bannerLinkText ' ' ${getString(R.string.mobile_overview_link_description)}"
                        bannerLinkTextView.contentDescription = bannerLinkContentDescription
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        //println(e.message.toString())
                    }
                })
            }else{
                findViewById<LinearLayout>(R.id.khc_overview_banner_card).visibility = View.GONE
            }

            var button_color = getStringVariable("GIFTS_CARD_BUTTON_COLOR")

            if (button_color == "") {
                button_color = getStringVariable("BUTTON_TEXT_COLOR")
                if (button_color == "") {
                    getStringVariable("PRIMARY_COLOR")
                }
            }
            finns_card.colorList = ColorList(
                getStringVariable("PRIMARY_COLOR").trim().lowercase(),
                (getStringVariable("TILE_BACKGROUND_WHITE_ENABLED") == "true"),
                button_color,
                "#ffffff"
            )

            runOnUiThread {
                (finns_card.root as LinearLayout).setVisibility(View.INVISIBLE)
                loadKHCFinnsMissionCard()
            }
        }

        if (getStringVariable("AHA_AHC_FINNS_MISSION") == "true") {
            val finns_card: OverviewAhcFinnsCardBinding = DataBindingUtil.inflate(
                inflater, R.layout.overview_ahc_finns_card, linear, true
            )

            if (getStringVariable("BANNER_TILE_ENABLED") == "true") {
                findViewById<LinearLayout>(R.id.ahc_overview_banner_card).visibility = View.VISIBLE

                val url = resources.getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/messages/TEXTNOW/").plus(getConsID()).plus("/").plus(getEvent().event_id).plus("/android/").plus(getDeviceType())
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                    .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                    .build()

                val client = OkHttpClient()
                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        if(response.code != 200){
                            throw Exception(response.body?.string())
                        }else{
                            val jsonString = response.body?.string()
                            val jsonArray = JSONArray(jsonString)
                            if(jsonArray.length() > 0){
                                val obj = jsonArray.getJSONObject(0)

                                var safeMessage = setupMessage(obj);

                                findViewById<LinearLayout>(R.id.banner_link).setOnClickListener {
                                    sendGoogleAnalytics("challenges_sms_share","challenges")
                                    sendSocialActivity("sms")
                                    val intent = Intent(Intent.ACTION_SENDTO)
                                    intent.data = Uri.parse("smsto:")
                                    if(safeMessage.custom_content){
                                        intent.putExtra(Intent.EXTRA_TEXT, (obj.get("sms_url") as String))
                                    }else {
                                        intent.putExtra(Intent.EXTRA_TEXT,
                                            safeMessage.text.plus(" ").plus(obj.get("sms_url"))
                                        )
                                    }
                                    startActivity(Intent.createChooser(intent,getResources().getString(R.string.mobile_fundraise_share_dialog_title)))
                                }
                            }else{
                                findViewById<LinearLayout>(R.id.banner_link).setOnClickListener {
                                    sendGoogleAnalytics("challenges_sms_share","challenges")
                                    sendSocialActivity("sms")
                                    val intent = Intent(Intent.ACTION_SENDTO)
                                    intent.data = Uri.parse("smsto:")
                                    intent.putExtra(Intent.EXTRA_TEXT,"")
                                    startActivity(Intent.createChooser(intent,getResources().getString(R.string.mobile_fundraise_share_dialog_title)))
                                }
                            }
                        }
                        val bannerLinkTextView = findViewById<TextView>(R.id.banner_link_text)
                        val bannerLinkText = bannerLinkTextView.text.toString()
                        val bannerLinkContentDescription = "$bannerLinkText ' ' ${getString(R.string.mobile_overview_link_description)}"
                        bannerLinkTextView.contentDescription = bannerLinkContentDescription
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        //println(e.message.toString())
                    }
                })
            }else{
                findViewById<LinearLayout>(R.id.ahc_overview_banner_card).visibility = View.GONE
            }

            var button_color = getStringVariable("GIFTS_CARD_BUTTON_COLOR")

            if (button_color == "") {
                button_color = getStringVariable("BUTTON_TEXT_COLOR")
                if (button_color == "") {
                    getStringVariable("PRIMARY_COLOR")
                }
            }
            finns_card.colorList = ColorList(
                getStringVariable("PRIMARY_COLOR").trim().lowercase(),
                (getStringVariable("TILE_BACKGROUND_WHITE_ENABLED") == "true"),
                button_color,
                "#ffffff"
            )

            runOnUiThread {
                (finns_card.root as LinearLayout).setVisibility(View.INVISIBLE)
                loadAHCFinnsMissionCard(finns_card.root as LinearLayout)
            }
        }

        if (getStringVariable("AUTISM_CHALLENGES_ENABLED") == "true") {
            val challenges_card: OverviewChallengesCardBinding = DataBindingUtil.inflate(
                inflater, R.layout.overview_challenges_card, linear, true
            )
            challenges_card.colorList = getColorList("")

            runOnUiThread {
                loadChallengeCard()
            }
        }

        val overview_event_progress_card: OverviewEventProgressCardBinding =
            DataBindingUtil.inflate(
                inflater, R.layout.overview_event_progress_card, linear, true
            )
        overview_event_progress_card.colorList = getColorList("")

        if(getStringVariable("AHA_IMPACT_POINTS_ENABLED") == "true"){
            val overview_impact_points_card: OverviewImpactPointsCardBinding =
                DataBindingUtil.inflate(
                    inflater, R.layout.overview_impact_points_card, linear, true
                )
            overview_impact_points_card.colorList = getColorList("")
        }

        val overview_progress_card: OverviewProgressCardBinding = DataBindingUtil.inflate(
            inflater, R.layout.overview_progress_card, linear, true
        )
        overview_progress_card.colorList = getColorList("")

        if(getStringVariable("BADGES_ENABLED") == "true"){
            val overview_badges_card: OverviewBadgesCardBinding =
                DataBindingUtil.inflate(
                    inflater, R.layout.overview_badges_card, linear, true
                )
            overview_badges_card.colorList = getColorList("")

            setTooltipText(R.id.overview_badges_help_button, R.string.mobile_overview_badges_card_tooltip, R.string.mobile_overview_badges_card_title)
            
            findViewById<LinearLayout>(R.id.overview_badges_help_button).setOnClickListener {
                sendGoogleAnalytics("overview_badges_help", "overview")
            }
            loadBadgesCard()
        }

        if (getStringVariable("AHAHEARTWALK_CPR_TILE_ENABLED") == "true") {
            val cpr_card: OverviewCprCardBinding = DataBindingUtil.inflate(
                inflater, R.layout.overview_cpr_card, linear, true
            )

            val overview_cpr_help_button = findViewById<LinearLayout>(R.id.overview_cpr_help_button);

            cpr_card.colorList = getColorList("")
            setTooltipText(R.id.overview_cpr_help_button, R.string.mobile_overview_cpr_card_tooltip , R.string.mobile_overview_cpr_card_title)
            
            overview_cpr_help_button.setOnClickListener{    
                sendGoogleAnalytics("overview_hw_cpr_help", "overview")
            }

            if(getStringVariable("CPR_TILE_BUTTON_URL") != ""){
                findViewById<Button>(R.id.overview_cpr_btn).setOnClickListener{
                    sendGoogleAnalytics("overview_hw_cpr_button", "overview")
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getStringVariable("CPR_TILE_BUTTON_URL")))
                    startActivity(browserIntent)
                }
            }else{
                findViewById<Button>(R.id.overview_cpr_btn).setVisibility(View.GONE)
            }

            val image_url = "https://nt-dev-clients.s3.amazonaws.com/ahaheartwalk/custom/NAT+Thumbnail.png"
            if(image_url != ""){
                Glide.with(this)
                    .load(image_url)
                    .into(findViewById<ImageView>(R.id.overview_cpr_image))
            }

            val comboString = getString(R.string.mobile_overview_cpr_card_subtitle) + " " + getString(R.string.mobile_overview_cpr_card_subtitle_2) + " " + getString(R.string.mobile_overview_cpr_card_subtitle_3)
            val str = SpannableStringBuilder(comboString)

            val bold_start = getString(R.string.mobile_overview_cpr_card_subtitle).length + 1
            val bold_end = getString(R.string.mobile_overview_cpr_card_subtitle).length + 1 + getString(R.string.mobile_overview_cpr_card_subtitle_2).length

            str.setSpan(
                StyleSpan(Typeface.BOLD),
                bold_start,
                bold_end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            str.setSpan(
                ForegroundColorSpan(Color.parseColor(getStringVariable("PRIMARY_COLOR"))),
                bold_start,
                bold_end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            findViewById<TextView>(R.id.overview_cpr_text).setText(str)
        }

        if (getString(R.string.flower_garden_enabled) == "true" && getStringVariable("ALZ_JERSEY_AR_ENABLED") == "true") {
            val jerseys_card: OverviewJerseyCardBinding = DataBindingUtil.inflate(
                inflater, R.layout.overview_jersey_card, linear, true
            )
            jerseys_card.colorList = getColorList("")
        }

        if (getStringVariable("ALZ_PROMISE_GARDEN_ENABLED") == "true" && getString(R.string.flower_garden_enabled) == "true") {
            val promise_garden_card: OverviewPromiseGardenCardBinding = DataBindingUtil.inflate(
                inflater, R.layout.overview_promise_garden_card, linear, true
            )
            promise_garden_card.colorList = getColorList("")
            runOnUiThread {
                loadPromiseGarden("first")
            }
        }

        if (getString(R.string.flower_garden_enabled) == "true" && getStringVariable("ALZ_LUMINARIES_ENABLED") == "true") {
            val luminaries_card: OverviewLuminariesCardBinding = DataBindingUtil.inflate(
                inflater, R.layout.overview_luminaries_card, linear, true
            )
            luminaries_card.colorList = getColorList("")
            runOnUiThread {
                loadLuminaries("first")
            }
        }

        if (getString(R.string.flower_garden_enabled) == "true" && getStringVariable("ALZ_PHOTO_FILTERS_ENABLED") == "true") {
            val photo_filters_card: OverviewPhotoFiltersCardBinding = DataBindingUtil.inflate(
                inflater, R.layout.overview_photo_filters_card, linear, true
            )
            photo_filters_card.colorList = getColorList("")
            runOnUiThread {
                loadPhotoFiltersCard()
            }
        }

        var fb_fundraiser_enabled = getStringVariable("FACEBOOK_FUNDRAISER_ENABLED");

        val event_progress_bar =
            findViewById<LinearLayout>(R.id.event_progress_card_raised_progress_bar)
        val personal_progress_bar =
            findViewById<LinearLayout>(R.id.progress_card_raised_progress_bar)
        val team_progress_bar =
            findViewById<LinearLayout>(R.id.progress_card_raised_team_progress_bar)
        val company_progress_bar =
            findViewById<LinearLayout>(R.id.progress_card_raised_company_progress_bar)

        val progressBars = arrayOf(event_progress_bar, personal_progress_bar, team_progress_bar, company_progress_bar)
        var color = getStringVariable("THERMOMETER_COLOR")
        if (color == "") {
            color = getStringVariable("PRIMARY_COLOR")
            if (color == "") {
                color = String.format(
                    "#%06x",
                    ContextCompat.getColor(this, R.color.primary_color) and 0xffffff
                )
            }
        }

        color = color.replace(" ", "")

        try {
            progressBars.forEach {
                it.background.setColorFilter(Color.parseColor(color), PorterDuff.Mode.SRC_ATOP)
            }
        } catch (e: Exception) {
            println("ERROR FROM PROGRESS BAR COLOR")
        }

        //BEGIN_FACEBOOK_CONTENT
        if (fb_fundraiser_enabled == "true") {
            loadFacebookCard(inflater, linear)
        }
        //END_FACEBOOK_CONTENT

        if(getStringVariable("AHA_IMPACT_BADGES_ENABLED") == "true"){
            val overview_impact_badges_card: OverviewImpactBadgesCardBinding = DataBindingUtil.inflate(
                inflater, R.layout.overview_impact_badges_card, linear, true
            )
            overview_impact_badges_card.colorList = getColorList("")
            setTooltipText(R.id.overview_impact_badges_card_help_button, R.string.mobile_overview_impact_badges_tooltip, R.string.mobile_overview_impact_badges_title)
            loadImpactBadgesCard();
        }

        if(getStringVariable("OVERVIEW_WEEKLY_STRATEGY_ENABLED") == "true"){
            val overview_weekly_strategy_card: OverviewWeeklyStrategyCardBinding = DataBindingUtil.inflate(
                inflater, R.layout.overview_weekly_strategy_card, linear, true
            )
            overview_weekly_strategy_card.colorList = getColorList("")
            setTooltipText(R.id.overview_weekly_strategy_card_help_button, R.string.mobile_overview_custom_image_tooltip_android, R.string.mobile_overview_custom_image_title)
            loadWeeklyStrategyCard();
        }

        if(getStringVariable("OVERVIEW_CUSTOM_IMAGE_CARD_ENABLED") == "true"){
            val overview_custom_image_card: OverviewCustomImageCardBinding = DataBindingUtil.inflate(
                inflater, R.layout.overview_custom_image_card, linear, true
            )
            overview_custom_image_card.colorList = getColorList("")
            setTooltipText(R.id.overview_custom_image_card_help_button, R.string.mobile_overview_custom_image_tooltip_android, R.string.mobile_overview_custom_image_title)

            val img_view = findViewById<ImageView>(R.id.overview_custom_image_card_image);
            val media = getStringVariable("OVERVIEW_CUSTOM_IMAGE_CARD_IMAGE_URL")
            if (media !== null) {
                Glide.with(this@Overview)
                    .load(media)
                    .into(img_view)
            }
            img_view.contentDescription = getStringVariable("OVERVIEW_CUSTOM_IMAGE_CARD_IMAGE_ALT_TEXT");
        }

        val overview_leaderboard_card: OverviewLeaderboardCardBinding = DataBindingUtil.inflate(
            inflater, R.layout.overview_leaderboard_card, linear, true
        )
        overview_leaderboard_card.colorList = getColorList("")

        var leaderboardCard = findViewById<FrameLayout>(R.id.overview_leaderboard_card);
        val editPersonalGoal = findViewById<TextView>(R.id.progress_card_edit_personal_goal);
        val editTeamGoal = findViewById<TextView>(R.id.progress_card_edit_team_goal);
        val editCompanyGoal = findViewById<TextView>(R.id.progress_card_edit_company_goal);
        val personalProgressRaisedButton =
            findViewById<FrameLayout>(R.id.personal_progress_raised_button);
        val personalProgressActivityButton =
            findViewById<FrameLayout>(R.id.personal_progress_activity_button);
        val teamProgressRaisedButton = findViewById<FrameLayout>(R.id.team_progress_raised_button);
        val teamProgressActivityButton =
            findViewById<FrameLayout>(R.id.team_progress_activity_button);

        personalProgressRaisedButton.contentDescription =
            getString(R.string.mobile_overview_progress_raised_button_initial_content_description);
        personalProgressActivityButton.contentDescription =
            getString(R.string.mobile_overview_progress_activity_button_initial_content_description);
        teamProgressRaisedButton.contentDescription =
            getString(R.string.mobile_overview_team_progress_raised_button_initial_content_description);
        teamProgressActivityButton.contentDescription =
            getString(R.string.mobile_overview_team_progress_activity_button_initial_content_description);

        val personal_leaderboard_raised_button =
            findViewById<FrameLayout>(R.id.personal_leaderboard_raised_button);
        val personal_leaderboard_activity_button =
            findViewById<FrameLayout>(R.id.personal_leaderboard_activity_button);
        val team_leaderboard_raised_button =
            findViewById<FrameLayout>(R.id.team_leaderboard_raised_button);
        val team_leaderboard_activity_button =
            findViewById<FrameLayout>(R.id.team_leaderboard_activity_button);

        personal_leaderboard_raised_button.contentDescription =
            getString(R.string.mobile_overview_leaderboard_raised_button_initial_content_description);
        personal_leaderboard_activity_button.contentDescription =
            getString(R.string.mobile_overview_leaderboard_activity_button_initial_content_description);
        team_leaderboard_raised_button.contentDescription =
            getString(R.string.mobile_overview_team_leaderboard_raised_button_initial_content_description);
        team_leaderboard_activity_button.contentDescription =
            getString(R.string.mobile_overview_team_leaderboard_activity_button_initial_content_description);

        editPersonalGoal.setOnClickListener {
            sendGoogleAnalytics("overview_personal_edit_goal","overview")
            displayAlert("updatePersonalGoal", toDouble(personalGoal))
            setAlertSender(editPersonalGoal)
        }

        editTeamGoal.setOnClickListener {
            sendGoogleAnalytics("overview_team_edit_goal","overview")
            displayAlert("updateTeamGoal", toDouble(teamGoal))
            setAlertSender(editTeamGoal)
        }

        editCompanyGoal.setOnClickListener {
            sendGoogleAnalytics("overview_company_edit_goal","overview")
            displayAlert("updateCompanyGoal", toDouble(companyGoal))
            setAlertSender(editCompanyGoal)
        }

        val eventProgressHelp = findViewById<LinearLayout>(R.id.event_progress_help_button);
        val overviewProgressHelp = findViewById<LinearLayout>(R.id.personal_progress_help_button);
        val overviewTeamProgressHelp = findViewById<LinearLayout>(R.id.team_progress_help_button);
        val overviewTopIndividualsHelp = findViewById<LinearLayout>(R.id.leaderboard_help_button);
        val overviewTopTeamsHelp = findViewById<LinearLayout>(R.id.team_leaderboard_help_button);
        val overviewTopCompanyHelp = findViewById<LinearLayout>(R.id.company_leaderboard_help_button);
        val overviewCompanyProgressHelp = findViewById<LinearLayout>(R.id.company_progress_help_button);
        val overviewFacebookFundraiserHelp = findViewById<LinearLayout>(R.id.overview_facebook_help_button);

        if (getStringVariable("ACTIVITY_TRACKING_ENABLED") == "true") {
            setTooltipWithAnalytics(R.id.event_progress_help_button, R.string.mobile_overview_event_progress_tooltip, R.string.mobile_overview_event_progress_title, "overview_event_help")
            setTooltipWithAnalytics(R.id.personal_progress_help_button, R.string.mobile_overview_progress_activity_tooltip, getProgressTitle("personal_help"), "overview_progress_help")
            setTooltipWithAnalytics(R.id.team_progress_help_button, R.string.mobile_overview_team_progress_activity_tooltip, getProgressTitle("team_help"), "overview_team_help")
            setTooltipWithAnalytics(R.id.company_progress_help_button, R.string.mobile_overview_company_progress_activity_tooltip, getProgressTitle("company_help"), "overview_company_help")
            if(getStringVariable("DISABLE_OVERVIEW_ACTIVITY_LEADERBOARDS") != "true") {
                setTooltipWithAnalytics(R.id.team_leaderboard_help_button, R.string.mobile_overview_team_leaderboard_activity_tooltip, R.string.mobile_overview_team_leaderboard_title, "overview_top_teams_help")
                setTooltipWithAnalytics(R.id.company_leaderboard_help_button, R.string.mobile_overview_company_leaderboard_activity_tooltip, R.string.mobile_overview_company_leaderboard_title, "overview_top_companies_help")
                setTooltipWithAnalytics(R.id.leaderboard_help_button, R.string.mobile_overview_leaderboard_activity_tooltip, R.string.mobile_overview_leaderboard_title, "overview_top_individuals_help")
            }
            setTooltipWithAnalytics(R.id.overview_facebook_help_button,R.string.mobile_overview_facebook_connect_connected_tooltip, R.string.mobile_overview_facebook_connect_title,"overview_facebook_help_button")
        } else {
            setTooltipWithAnalytics(R.id.event_progress_help_button, R.string.mobile_overview_event_progress_tooltip, R.string.mobile_overview_event_progress_title, "overview_event_help")
            setTooltipWithAnalytics(R.id.personal_progress_help_button, R.string.mobile_overview_progress_tooltip, getProgressTitle("personal_help"), "overview_progress_help")
            setTooltipWithAnalytics(R.id.team_progress_help_button, R.string.mobile_overview_team_progress_tooltip, getProgressTitle("team_help"), "overview_team_help")
            setTooltipWithAnalytics(R.id.leaderboard_help_button, R.string.mobile_overview_leaderboard_tooltip, R.string.mobile_overview_leaderboard_title, "overview_top_individuals_help")
            setTooltipWithAnalytics(R.id.team_leaderboard_help_button, R.string.mobile_overview_team_leaderboard_tooltip, R.string.mobile_overview_team_leaderboard_title, "overview_top_teams_help")
            setTooltipWithAnalytics(R.id.company_leaderboard_help_button, R.string.mobile_overview_company_leaderboard_tooltip, R.string.mobile_overview_company_leaderboard_title, "overview_top_companies_help")
            setTooltipWithAnalytics(R.id.company_progress_help_button, R.string.mobile_overview_company_progress_tooltip, getProgressTitle("company_help"), "overview_company_help")
            if(findViewById<LinearLayout>(R.id.overview_facebook_help_button) != null){
                setTooltipWithAnalytics(R.id.overview_facebook_help_button,R.string.mobile_overview_facebook_connect_connected_tooltip, R.string.mobile_overview_facebook_connect_title,"overview_facebook_help_button")
            }
        }


        //Load Data
        var media = getStringVariable("LOGO_URL")
        if (media == "") {
            media = getStringVariable("LOGIN_IMG_URL");
        }
        val overviewLogo = findViewById<ImageView>(R.id.overview_page_logo);

        if (media !== null) {
            Glide.with(this)
                .load(media)
                .into(overviewLogo)
            if (getStringVariable("APP_NAME") != "") {
                overviewLogo.contentDescription = getStringVariable("APP_NAME")
            } else if (getStringVariable("LOGIN_APP_NAME") != "") {
                overviewLogo.contentDescription = getStringVariable("LOGIN_APP_NAME")
            }
        } else {
            val intent = Intent(this@Overview, Error::class.java);
            startActivity(intent);
        }
        
        findViewById<TextView>(R.id.event_progress_card_event_title).setOnClickListener {
          sendGoogleAnalytics("event_progress_card_event_title","overview")
            val url = getStringVariable("EVENT_PAGE_URL")
            if (url != "") {
                val browserIntent =
                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            }
        }

        val eventProgressCard = findViewById<LinearLayout>(R.id.overview_event_progress_card);
        val progressCard = findViewById<FrameLayout>(R.id.overview_progress_card);
        loadProgressData(eventProgressCard, progressCard, leaderboardCard, true);
        if(getStringVariable("OVERVIEW_PROGRESS_MANAGE_HQ") != ""){
            findViewById<LinearLayout>(R.id.manage_campaign_button_container).visibility = View.VISIBLE
            findViewById<Button>(R.id.manage_campaign_button).setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getStringVariable("OVERVIEW_PROGRESS_MANAGE_HQ")))
                startActivity(browserIntent)
            }
        }else{
            findViewById<LinearLayout>(R.id.manage_campaign_button_container).visibility = View.GONE
        }
        val eventRaisedBar = findViewById<LinearLayout>(R.id.event_progress_card_raised_bar);
        eventRaisedBar.doOnLayout {
            progressBarWidth = it.measuredWidth
            progressBarHeight = it.measuredHeight
        }

        val overview_national_sponsors_card: OverviewNationalSponsorsCardBinding =
            DataBindingUtil.inflate(
                inflater, R.layout.overview_national_sponsors_card, linear, true
            )
        overview_national_sponsors_card.colorList = getColorList("")
        val overview_local_sponsors_card: OverviewLocalSponsorsCardBinding =
            DataBindingUtil.inflate(
                inflater, R.layout.overview_local_sponsors_card, linear, true
            )
        overview_local_sponsors_card.colorList = getColorList("")
        val overview_additional_sponsors_card: OverviewAdditionalSponsorsCardBinding =
            DataBindingUtil.inflate(
                inflater, R.layout.overview_additional_sponsors_card, linear, true
            )
        overview_additional_sponsors_card.colorList = getColorList("")
        loadSponsorData(
            overview_national_sponsors_card.root as LinearLayout,
            overview_local_sponsors_card.root as LinearLayout,
            overview_additional_sponsors_card.root as LinearLayout,
        )
    }

    fun updateCheckinCard(checkedin: Boolean){
        if(checkedin){
            findViewById<LinearLayout>(R.id.overview_event_check_in_section).visibility = View.GONE
            findViewById<LinearLayout>(R.id.overview_event_checked_in_section).visibility = View.VISIBLE

            setVariable("CHECKED_IN", "true")
            if(getStringVariable("EVENT_CHECKIN_TSHIRT_PROMPT_ENABLED") == "true"){
                try{
                    val raised = getStringVariable("PERSONAL_RAISED").replace("$","").toDouble()
                    val threshold = getStringVariable("EVENT_CHECKIN_TSHIRT_PROMPT_AMOUNT").replace("$","").toDouble()
                    if(raised >= threshold){
                        findViewById<TextView>(R.id.overview_event_checked_in_tshirt_subtitle).visibility = View.VISIBLE
                    }
                }catch(e: Exception){
                    findViewById<TextView>(R.id.overview_event_checked_in_tshirt_subtitle).visibility = View.GONE
                }
            }
        }else{
            setVariable("CHECKED_IN", "false")
            findViewById<LinearLayout>(R.id.overview_event_check_in_section).visibility = View.VISIBLE
            findViewById<LinearLayout>(R.id.overview_event_checked_in_section).visibility = View.GONE
        }
    }

    fun checkInForEvent(checkedin: String){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/user/checkin/")
        val formBody = FormBody.Builder()
            .add("cons_id", getConsID())
            .add("event_id", getEvent().event_id)
            .add("check_in", checkedin)
            .build()

        var request = Request.Builder().url(url)
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .post(formBody)
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                if(response.code != 200 && response.code != 201){
                    throw Exception(response.body?.string())
                }else{
                    runOnUiThread{
                        updateCheckinCard(true)
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
                runOnUiThread{
                    updateCheckinCard(false)
                }
            }
        })
    }

    fun loadImpactBadgesCard(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/getImpactBadges/").plus(getConsID()).plus("/").plus(getEvent().event_id)
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
                }else {
                    val jsonString = response.body?.string();
                    val obj = JSONObject(jsonString);
                    runOnUiThread{
                        if(obj.has("data")){
                            val data = obj.get("data") as JSONObject;
                            if(data.has("badges")){
                                val json = data.get("badges") as JSONArray;

                                var chunk_size = 3;

                                if (json.length() > 6){
                                    chunk_size = 4;
                                }
                                var current_chunk_index = 0;
                                var current_chunk_row = 0;
                                val inflater = layoutInflater;
                                var parent = findViewById<LinearLayout>(R.id.overview_impact_badges_container)

                                for(i in 0 .. json.length() - 1){
                                    try {
                                        var badge_obj = json.getJSONObject(i);
                                        val badge_name = getSafeStringVariable(badge_obj, "name")
                                        val badge_image = getSafeStringVariable(badge_obj, "image")
                                        var badge_earned = getSafeBooleanVariable(badge_obj, "earned")
                                        var badge_voiceover = badge_name

                                        if(badge_earned){
                                            badge_voiceover = badge_name + getString(R.string.mobile_overview_impact_badges_badge) + getString(R.string.mobile_overview_impact_badges_earned)
                                        }else{
                                            badge_voiceover = badge_name + getString(R.string.mobile_overview_impact_badges_badge) + getString(R.string.mobile_overview_impact_badges_not_earned)
                                        }

                                        runOnUiThread{
                                            if(current_chunk_index >= chunk_size){
                                                current_chunk_index = 0
                                                current_chunk_row += 1;
                                            }

                                            if(current_chunk_index == 0){
                                                val row: BadgesRowBinding = DataBindingUtil.inflate(
                                                    inflater, R.layout.badges_row, parent, true)
                                                row.colorList = getColorList("")
                                            }

                                            val row = parent.getChildAt(current_chunk_row) as LinearLayout

                                            row.weightSum = chunk_size.toFloat()
                                            row.gravity = Gravity.CENTER

                                            val binding: OverviewImpactBadgeBinding = DataBindingUtil.inflate(
                                                inflater, R.layout.overview_impact_badge, row, true)
                                            binding.colorList = getColorList("")

                                            val badge_xml = binding.root as LinearLayout
                                            val image_view = badge_xml.getChildAt(0) as ImageView

                                            if(badge_image != ""){
                                                Glide.with(this@Overview)
                                                    .load(badge_image)
                                                    .into(image_view)
                                            }

                                            image_view.contentDescription = badge_voiceover;

                                            val text_view = (badge_xml.getChildAt(1) as TextView)
                                            text_view.setText(badge_name)

                                            current_chunk_index += 1
                                        }
                                        
                                    } catch (exception: IOException) {
                                    }
                                }
                            }

                        }else{
                            findViewById<LinearLayout>(R.id.overview_impact_badges_card).visibility = View.GONE
                        }
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
            }
        })

        findViewById<Button>(R.id.badges_manage_campaign_button).setOnClickListener({
            openUrlInBrowser(getStringVariable("IMPACT_BADGES_HQ"))
        })
    }

    fun loadImpactPointsCard(){
        setTooltipText(R.id.overview_impact_points_card_help_button, R.string.mobile_overview_impact_points_tooltip, R.string.mobile_overview_impact_points_title_impact)
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/getImpactPoints/").plus(getConsID()).plus("/").plus(getEvent().event_id)
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
                }else {
                    val jsonString = response.body?.string();
                    val obj = JSONObject(jsonString);
                    runOnUiThread{
                        if(obj.has("data")){
                            val data = obj.get("data") as JSONObject;
                            val points = intWithCommas(getSafeIntegerVariable(data, "points"));
                            val team_points = getSafeIntegerVariable(data, "team_points")

                            val points_tv= findViewById<TextView>(R.id.impact_points_card_points)
                            val points_color = getStringVariable("AHA_IMPACT_POINTS_COLOR")
                            try{
                                points_tv.setTextColorValue(points_color)
                            }catch(e: Exception){
                                points_tv.setTextColorValue(getStringVariable("PRIMARY_COLOR"))
                            }

                            if(team_points != 0 && isTeamCaptain){
                                points_tv.text = intWithCommas(team_points)
                                findViewById<TextView>(R.id.impact_points_card_title).setText(R.string.mobile_overview_impact_points_team_total_title)
                            }else{
                                points_tv.text = points
                                findViewById<TextView>(R.id.impact_points_card_title).setText(R.string.mobile_overview_impact_points_total_title)
                            }

                            if(getStringVariable("IMPACT_POINTS_HQ") != ""){
                                findViewById<Button>(R.id.impact_points_card_button).setOnClickListener{
                                    val browserIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(getStringVariable("IMPACT_POINTS_HQ"))
                                    )
                                    startActivity(browserIntent)
                                }
                            }else{
                                findViewById<Button>(R.id.impact_points_card_button).visibility = View.GONE
                            }
                            findViewById<LinearLayout>(R.id.overview_impact_points_card).visibility = View.VISIBLE
                        }else{
                            findViewById<LinearLayout>(R.id.overview_weekly_strategy_card).visibility = View.GONE
                        }
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
            }
        })
    }

    fun loadWeeklyStrategyCard(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/weeklyStrategy/").plus(getConsID()).plus("/").plus(getEvent().event_id)
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
                } else {
                    val jsonString = response.body?.string();
                    val obj = JSONObject(jsonString);
                    runOnUiThread{
                        if(obj.has("data")){
                            val data = obj.get("data") as JSONObject;
                            if(getSafeBooleanVariable(data, "weekly_strategy_active")){
                                findViewById<TextView>(R.id.weekly_strategy_card_title).text = getSafeStringVariable(data, "weekly_strategy_title");
                                findViewById<TextView>(R.id.weekly_strategy_card_details).text = getSafeStringVariable(data, "weekly_strategy_details")
                                findViewById<TextView>(R.id.weekly_strategy_card_button).text = getSafeStringVariable(data, "weekly_strategy_button_text")
                                val action = getSafeStringVariable(data, "weekly_strategy_button_url");
                                findViewById<TextView>(R.id.weekly_strategy_card_button).setOnClickListener{
                                    if(action == "FUNDRAISE_PAGE"){
                                        val intent = Intent(this@Overview, com.nuclavis.rospark.Fundraise::class.java);
                                        startActivity(intent);
                                    }else if (action == "DONATION_PAGE"){
                                        val intent = Intent(this@Overview, com.nuclavis.rospark.Donations::class.java);
                                        startActivity(intent);
                                    }else if (action == "ACTIVITY_PAGE"){
                                        val intent = Intent(this@Overview, com.nuclavis.rospark.TrackActivity::class.java);
                                        startActivity(intent);
                                    }else{
                                        val url = getStringVariable((action.uppercase() + "_URL"))
                                        if(url != ""){
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            startActivity(browserIntent)
                                        }else{
                                            try{
                                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(action))
                                                startActivity(browserIntent)
                                            }catch(e: Exception){

                                            }
                                        }
                                    }
                                }

                            }else{
                                findViewById<LinearLayout>(R.id.overview_weekly_strategy_card).visibility = View.GONE
                            }
                        }else{
                            findViewById<LinearLayout>(R.id.overview_weekly_strategy_card).visibility = View.GONE
                        }
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
            }
        })
    }

    fun getProgressTitle(type: String): String{
        if(type == "personal"){
            return getStringVariable("USER_FIRST_NAME").uppercase() + getString(R.string.mobile_overview_progress_plural_title).uppercase() + " " + getString(R.string.mobile_overview_progress_progress_title).uppercase()
        }else if (type == "team") {
            return getStringVariable("USER_TEAM_NAME").uppercase() + " " + getString(R.string.mobile_overview_progress_progress_title).uppercase()
        }else if (type == "company") {
            return getStringVariable("USER_COMPANY_NAME").uppercase() + " " + getString(R.string.mobile_overview_progress_progress_title).uppercase()
        }else if (type == "personal_help"){
            return "Personal " + getString(R.string.mobile_overview_progress_progress_title)
        }else if (type == "team_help"){
            return "Team " + getString(R.string.mobile_overview_progress_progress_title)
        }else if (type == "company_help"){
            return "Company " + getString(R.string.mobile_overview_progress_progress_title)
        }
        else{
            return ""
        }
    }

    override fun childviewCallback(string: String,data:String) {
        if(string == "finnsMissionModal" || string == "topClassroomsModal"){
            loadModalRows(string)
        }else if(string == "flowerPlanted" || string == "flowerDeleted"){
            runOnUiThread {
                loadPromiseGarden("third")
            }
        }else if(string == "luminaryCreated" || string == "luminaryDeleted"){
            runOnUiThread {
                loadLuminaries("third")
            }
        }else if(string == "jerseyCreated" || string == "jerseyDeleted"){
            runOnUiThread {
                loadJerseys("third")
            }
        }else{
            val eventProgressCard = findViewById<LinearLayout>(R.id.overview_event_progress_card);
            val progressCard = findViewById<FrameLayout>(R.id.overview_progress_card);
            var leaderboardCard = findViewById<FrameLayout>(R.id.overview_leaderboard_card);
            var fb_fundraiser_enabled = getStringVariable("FACEBOOK_FUNDRAISER_ENABLED");
            if(fb_fundraiser_enabled == "true"){
                var facebookCard = findViewById<LinearLayout>(R.id.overview_facebook_card);
                loadFbFundraiserData(facebookCard, false)
            }
            loadProgressData(eventProgressCard, progressCard, leaderboardCard, false);
        }
    }

    fun resizeProgressBar(percent: Double, bar: String){
        runOnUiThread {
            var newBarWidth = progressBarHeight;
            if((percent * progressBarWidth) > progressBarHeight){
                newBarWidth = (percent * progressBarWidth).toInt()
            }else if(percent == 0.00){
                newBarWidth = 0
            }

            if(bar == "Event"){
                val eventRaisedProgress = findViewById<LinearLayout>(R.id.event_progress_card_raised_progress_bar);
                eventRaisedProgress.layoutParams = FrameLayout.LayoutParams(newBarWidth, progressBarHeight)
            }
            else if (bar == "Personal"){
                val personalRaisedProgress = findViewById<LinearLayout>(R.id.progress_card_raised_progress_bar);
                personalRaisedProgress.layoutParams = FrameLayout.LayoutParams(newBarWidth, progressBarHeight)
            }else if (bar == "Team"){
                val teamRaisedProgress = findViewById<LinearLayout>(R.id.progress_card_raised_team_progress_bar);
                teamRaisedProgress.layoutParams = FrameLayout.LayoutParams(newBarWidth, progressBarHeight)
            }else if (bar == "Challenges"){
                val challengeRaisedProgress = findViewById<LinearLayout>(R.id.progress_card_challenge_progress_bar);
                challengeRaisedProgress.layoutParams = FrameLayout.LayoutParams(newBarWidth, progressBarHeight)
            }else if (bar == "Company"){
                val companyProgress = findViewById<LinearLayout>(R.id.progress_card_raised_company_progress_bar);
                companyProgress.layoutParams = FrameLayout.LayoutParams(newBarWidth, progressBarHeight)
            }
        }
    }

    fun loadProgressData(eventCard: View, progressCard: View, leaderboardCard: View, initial: Boolean){
        if(initial){
            eventCard.setVisibility(View.INVISIBLE)
            progressCard.setVisibility(View.INVISIBLE)
            leaderboardCard.setVisibility(View.INVISIBLE)
        }

        setVariable("HAS_TEAM","false")

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getUserEvent/").plus(getConsID()).plus("/").plus(getEvent().event_id)
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
                    val obj = JSONObject(jsonString);

                    var reward_center_link = getStringVariable("AHA_REWARDS_CENTER")
                    if(reward_center_link != ""){
                        var days_til_event = getSafeIntegerVariable(obj, "days_til_event")
                        setVariable("DAYS_UNITL_EVENT",days_til_event.toString())
                        var days_after = 0;

                        if(getStringVariable("REWARDS_CENTER_NAV_DAYS_AFTER_EVENT") != ""){
                            days_after = getStringVariable("REWARDS_CENTER_NAV_DAYS_AFTER_EVENT").toInt()
                        }

                        if(days_til_event < 0){
                            val diff = days_til_event + days_after
                            runOnUiThread {
                                if (diff < 0) {
                                    findViewById<LinearLayout>(R.id.menu_option_rewards_center).visibility =
                                        View.GONE
                                } else {
                                    findViewById<LinearLayout>(R.id.menu_option_rewards_center).visibility =
                                        View.VISIBLE
                                }
                            }
                        }
                    }

                    runOnUiThread {
                        if (obj.has("first_name") && obj.get("first_name") is String) {
                            setVariable("USER_FIRST_NAME", obj.get("first_name") as String)
                            findViewById<TextView>(R.id.personal_progress_card_title).setText(
                                getProgressTitle("personal")
                            )
                            if(getStringVariable("AHA_IMPACT_POINTS_ENABLED") == "true"){
                                findViewById<TextView>(R.id.overview_impact_points_card_title).text =
                                    getString(R.string.mobile_overview_impact_points_title_android).replace("NAME", getStringVariable("USER_FIRST_NAME").uppercase() +"'S")
                            }
                        } else {
                            findViewById<TextView>(R.id.team_progress_card_title).setText(
                                getString(
                                    R.string.mobile_overview_progress_progress_title
                                )
                            )

                            if(getStringVariable("AHA_IMPACT_POINTS_ENABLED") == "true"){
                                findViewById<TextView>(R.id.overview_impact_points_card_title).text =
                                    getString(R.string.mobile_overview_impact_points_title_impact)
                            }
                        }

                        if (obj.has("team_name") && obj.get("team_name") is String) {
                            setVariable("USER_TEAM_NAME", obj.get("team_name") as String)
                            findViewById<TextView>(R.id.team_progress_card_title).setText(
                                getProgressTitle("team")
                            )
                        } else {
                            findViewById<TextView>(R.id.team_progress_card_title).setText(
                                getString(
                                    R.string.mobile_overview_progress_progress_title
                                )
                            )
                        }
                    }

                    if (obj.has("team_id") && (obj.get("team_id") is Int || obj.get("team_id") is String)) {
                        runOnUiThread {
                            val team_button = findViewById<LinearLayout>(R.id.menu_option_teams);
                            val recruit_button = findViewById<LinearLayout>(R.id.menu_option_recruit);
                            var team_id = "";
                            if (obj.get("team_id") is String) {
                                team_id = obj.get("team_id") as String;
                            } else {
                                team_id = (obj.get("team_id") as Int).toString();
                            }

                            if (team_id != "-1") {
                                setVariable("TEAM_ID", team_id)
                                setVariable("HAS_TEAM", "true")
                                hasTeam = true;
                                if (obj.get("team_captain") == true) {
                                    setVariable("IS_TEAM_CAPTAIN", "true")
                                    isTeamCaptain = true;
                                    updateMenu(true)
                                    if (getStringVariable("MANAGE_TEAM_ENABLED") == "false") {
                                        team_button.setVisibility(View.GONE)
                                    } else {
                                        team_button.setVisibility(View.VISIBLE)
                                    }
                                    recruit_button.setVisibility(View.VISIBLE)
                                } else {
                                    setVariable("IS_TEAM_CAPTAIN", "false")
                                    isTeamCaptain = false;
                                    updateMenu(false)
                                    if (getStringVariable("RECRUIT_DISABLED_TEAM_MEMBERS") == "true") {
                                        recruit_button.setVisibility(View.GONE)
                                    }else{
                                        recruit_button.setVisibility(View.VISIBLE)
                                    }

                                    if (getStringVariable("MY_TEAM_DISABLED_TEAM_MEMBERS") == "true") {
                                        team_button.setVisibility(View.GONE)
                                    }else{
                                        team_button.setVisibility(View.VISIBLE)
                                    }
                                }

                                val team_ids = getStringVariable("DISABLED_LEADERBOARD_TEAM_IDS")
                                if(team_ids != ""){
                                    val ids = team_ids.split("|");
                                    if(ids.contains(team_id)){
                                        findViewById<LinearLayout>(R.id.overview_leaderboard_card_layout).visibility = View.GONE
                                    }
                                }
                            } else {
                                setVariable("HAS_TEAM", "false")
                                hasTeam = false;
                                setVariable("IS_TEAM_CAPTAIN", "false");
                                updateMenu(false)
                                team_button.setVisibility(View.GONE)
                                recruit_button.setVisibility(View.GONE)
                            }
                            if(getStringVariable("AHA_IMPACT_POINTS_ENABLED") == "true") {
                                loadImpactPointsCard()
                            }
                        }
                    } else {
                        hasTeam = false
                        setVariable("HAS_TEAM", "false")
                    }

                    if(obj.has("company_id") && (obj.get("company_id") is Int || obj.get("company_id") is String)){
                        runOnUiThread {
                            var company_id = "";
                            if (obj.get("company_id") is String) {
                                company_id = obj.get("company_id") as String;
                            } else {
                                company_id = (obj.get("company_id") as Int).toString();
                            }

                            setVariable("COMPANY_ID", company_id)
                            setVariable("HAS_COMPANY", "true")
                            hasCompany = true;

                            if (obj.has("company_name") && obj.get("company_name") is String) {
                                setVariable("USER_COMPANY_NAME", obj.get("company_name") as String)
                                findViewById<TextView>(R.id.company_progress_card_title).setText(
                                    getProgressTitle("company")
                                )
                            } else {
                                findViewById<TextView>(R.id.company_progress_card_title).setText(
                                    getString(
                                        R.string.mobile_overview_progress_progress_title
                                    )
                                )
                            }

                            if (company_id != "" && company_id != "0") {
                                setVariable("COMPANY_ID", company_id)
                                setVariable("HAS_COMPANY", "true")
                                hasCompany = true;
                                if (obj.get("company_coordinator") == true) {
                                    setVariable("IS_COMPANY_COORDINATOR", "true");
                                } else {
                                    setVariable("IS_COMPANY_COORDINATOR", "false");
                                }
                            } else {
                                hasCompany = false;
                                setVariable("HAS_COMPANY", "false")
                                setVariable("IS_COMPANY_COORDINATOR", "false");
                            }

                            val manage_company_button = findViewById<LinearLayout>(R.id.menu_option_manage_company);
                            if(getStringVariable("IS_COMPANY_COORDINATOR") == "true"
                                && getStringVariable("DISABLE_COMPANY_PAGE") != "true"){
                                manage_company_button.setOnClickListener(View.OnClickListener() {
                                    val intent = Intent(this@Overview, com.nuclavis.rospark.ManageCompany::class.java);
                                    startActivity(intent);
                                });
                                manage_company_button.visibility = View.VISIBLE
                            }else{
                                manage_company_button.visibility = View.GONE
                            }
                        }
                    }else{
                        hasCompany = false
                        setVariable("HAS_COMPANY", "false")
                    }
                    runOnUiThread {
                        checkForChallengesLeaderboard()
                    }

                    loadProgressDataCallback(initial, leaderboardCard, obj, progressCard)

                    runOnUiThread {
                        if (getStringVariable("HIDE_EVENT_OVERVIEW") == "true") {
                            eventCard.setVisibility(View.GONE)
                        } else if (getStringVariable("HIDE_EVENT_OVERVIEW") != "true") {
                            fadeInView(eventCard);
                        }
                    }

                    val edit_team_goal_link = findViewById<LinearLayout>(R.id.progress_card_edit_team_goal_container);
                    runOnUiThread() {
                        if (getStringVariable("IS_TEAM_CAPTAIN") == "true") {
                            edit_team_goal_link.setVisibility(View.VISIBLE);
                        } else {
                            edit_team_goal_link.setVisibility(View.GONE);
                        }
                    }

                    val edit_company_goal_link = findViewById<LinearLayout>(R.id.progress_card_edit_company_goal_container);
                    runOnUiThread() {
                        if (getStringVariable("COMPANY_GOAL_EDIT_ENABLED") == "true" && getStringVariable("IS_COMPANY_COORDINATOR") == "true") {
                            edit_company_goal_link.setVisibility(View.VISIBLE);
                        } else {
                            edit_company_goal_link.setVisibility(View.GONE);
                        }
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
            }
        })
    }

    fun loadProgressDataCallback(initial: Boolean, leaderboardCard: View, obj: JSONObject, progressCard: View){
        runOnUiThread {
            if(initial){
                if(getStringVariable("HIDE_ALL_LEADERBOARDS") != "true"){
                    loadLeaderboardData(leaderboardCard)
                }else{
                    findViewById<LinearLayout>(R.id.overview_leaderboard_card_layout).visibility = View.GONE
                }
            }
            if(obj.has("event_percent") && obj.get("event_percent") is Int){
                eventProgressPercent = obj.get("event_percent") as Int;
            }
            if(obj.has("personal_percent") && obj.get("personal_percent") is Int){
                personalProgressPercent = obj.get("personal_percent") as Int;
            }
            if(obj.has("personal_goal")){
                if (obj.get("personal_goal") is Double){
                    personalGoal = obj.get("personal_goal") as Double;
                }else{
                    personalGoal = (obj.get("personal_goal") as Int).toDouble();
                }
            }

            var slideLength = 1

            if(hasTeam && (getStringVariable("OVERVIEW_TEAM_PROGRESS_CAPTAIN_ONLY") != "true" || isTeamCaptain)) {
                slideLength += 1
                if(obj.has("team_percent") && obj.get("team_percent") is Int){
                    teamProgressPercent = obj.get("team_percent") as Int;
                    setVariable("TEAM_PROGRESS_PERCENT",teamProgressPercent.toString())
                }else{
                    teamProgressPercent = 0;
                }
                if(obj.has("team_goal")){
                    teamGoal = toDouble(obj.get("team_goal"));
                    setVariable("TEAM_GOAL",teamGoal.toString())
                }
            }

            if(hasCompany && getStringVariable("DISABLE_COMPANY_PROGRESS") != "true"){
                slideLength += 1
                if(obj.has("company_percent") && obj.get("company_percent") is Int){
                    companyProgressPercent = obj.get("company_percent") as Int;
                    setVariable("COMPANY_PROGRESS_PERCENT",companyProgressPercent.toString())
                }else{
                    companyProgressPercent = 0;
                }
                if(obj.has("company_goal")){
                    companyGoal = toDouble(obj.get("company_goal"));
                    setVariable("COMPANY_GOAL",companyGoal.toString())
                }
            }

            total_progress_slides = slideLength

            if(!initial){
                if(obj.has("personal_percent") && obj.get("personal_percent") is Int){
                    resizeProgressBar(((obj.get("personal_percent") as Int).toDouble()/100), "Personal")
                }else{
                    resizeProgressBar(0.00, "Personal")
                }
                resizeProgressBar(((obj.get("personal_percent") as Int).toDouble()/100), "Personal")
                if(hasTeam){
                    if(obj.has("team_percent") && obj.get("team_percent") is Int){
                        resizeProgressBar(((obj.get("team_percent") as Int).toDouble()/100), "Team")
                    }else{
                        resizeProgressBar(0.00, "Team")
                    }

                }
            }

            getActivityProgressStats()

            if(initial){
                val eventName = findViewById<TextView>(R.id.event_progress_card_event_title);
                if(obj.has("event_name")){
                    eventName.setText(obj.get("event_name") as String);
                }
                val eventRaisedAmountText = findViewById<TextView>(R.id.event_progress_card_raised_amount);
                if(obj.has("event_raised")){
                    eventRaisedAmountText.setText(getString(R.string.mobile_overview_event_progress_raised).plus(" ").plus(formatDoubleToLocalizedCurrency(toDouble(obj.get("event_raised")))))
                }else{
                    eventRaisedAmountText.setText(getString(R.string.mobile_overview_event_progress_raised).plus(" ").plus(formatDoubleToLocalizedCurrency(0.00)))
                }
                if(obj.has("event_percent")){
                    eventProgressPercent = obj.get("event_percent") as Int
                }
                val eventRaisedPercentText= findViewById<TextView>(R.id.event_progress_card_raised_percent);
                eventRaisedPercentText.setText(eventProgressPercent.toString() + "%");
                eventRaisedPercentText.contentDescription = (eventProgressPercent.toString() + "% " + getString(R.string.mobile_overview_progress_percent_progress))
                val eventRaisedGoalText = findViewById<TextView>(R.id.event_progress_card_raised_goal);
                if(obj.has("event_goal")){
                    eventRaisedGoalText.setText(getString(R.string.mobile_overview_event_progress_goal).plus(" ").plus(formatDoubleToLocalizedCurrency(toDouble(obj.get("event_goal")))));
                }else{
                    eventRaisedGoalText.setText(getString(R.string.mobile_overview_event_progress_goal).plus(" $0.00"));
                }
                val eventDaysRemainingText = findViewById<TextView>(R.id.event_progress_card_days_remaining);

                var hideEvent = false;
                if(getStringVariable("HIDE_EVENT_COUNTDOWN") == "true"){
                    hideEvent = true;
                }
                if(obj.has("days_til_event") && !hideEvent){
                    if(obj.get("days_til_event") as Int <= 0){
                        eventDaysRemainingText.setVisibility(View.GONE)
                    }else{
                        eventDaysRemainingText.setVisibility(View.VISIBLE)
                        eventDaysRemainingText.setText(obj.get("days_til_event").toString().plus(" ").plus(getString(R.string.mobile_overview_event_progress_days_Remaininig)));
                    }
                }else{
                    eventDaysRemainingText.setVisibility(View.GONE)
                }
            }

            val progressRaisedAmountText = findViewById<TextView>(R.id.progress_card_raised_amount);
            if(obj.has("personal_raised")){
                personal_raised = getSafeDoubleVariable(obj, "personal_raised")
                progressRaisedAmountText.setText(getString(R.string.mobile_overview_progress_raised).plus(" ").plus(formatDoubleToLocalizedCurrency(toDouble(obj.get("personal_raised")))));
                setVariable("PERSONAL_RAISED",formatDoubleToLocalizedCurrency(toDouble(obj.get("personal_raised"))));
            }else{
                progressRaisedAmountText.setText(getString(R.string.mobile_overview_progress_raised).plus(" $0.00"));
                setVariable("PERSONAL_RAISED","$0")
            }

            updateCheckinCard(getStringVariable("CHECKED_IN") == "true")

            if(getString(R.string.flower_garden_enabled) == "true" && getStringVariable("ALZ_JERSEY_AR_ENABLED") == "true"){
                runOnUiThread {
                    loadJerseys("first")
                }
            }

            val progressRaisedPercentText= findViewById<TextView>(R.id.progress_card_raised_percent);
            progressRaisedPercentText.setText(personalProgressPercent.toString() + "%");
            progressRaisedPercentText.contentDescription = personalProgressPercent.toString() + "% " + getString(R.string.mobile_overview_progress_percent_progress)
            val progressRaisedGoalText = findViewById<TextView>(R.id.progress_card_raised_goal);
            progressRaisedGoalText.setText(getString(R.string.mobile_overview_progress_goal).plus(" ").plus(formatDoubleToLocalizedCurrency((personalGoal))))

            val second_progress_page = findViewById<LinearLayout>(R.id.my_progress_team_container);
            val third_progress_page = findViewById<LinearLayout>(R.id.my_progress_company_container);

            if(hasTeam || (hasCompany && getStringVariable("DISABLE_COMPANY_PROGRESS") != "true")){
                progressCard.setOnTouchListener(object :
                    OnSwipeTouchListener(this@Overview) {
                    override fun onSwipeLeft() {
                        super.onSwipeLeft();
                        switchProgressSlide(current_progress_slide + 1);
                    }

                    override fun onSwipeRight() {
                        super.onSwipeRight()
                        switchProgressSlide(current_progress_slide - 1);
                    }
                })

                if(hasTeam) {
                    val teamProgressRaisedAmountText =
                        findViewById<TextView>(R.id.progress_card_team_raised_amount);
                    if (obj.has("team_raised")) {
                        teamProgressRaisedAmountText.setText(
                            getString(R.string.mobile_overview_team_progress_raised).plus(
                                " "
                            ).plus(formatDoubleToLocalizedCurrency(toDouble(obj.get("team_raised"))))
                        );
                        setVariable(
                            "TEAM_PROGRESS_RAISED",
                            withCommas(toDouble(obj.get("team_raised")))
                        )
                    } else {
                        progressRaisedGoalText.setText(
                            getString(R.string.mobile_overview_team_progress_raised).plus(
                                " $0.00"
                            )
                        );
                    }

                    val teamProgressRaisedPercentText =
                        findViewById<TextView>(R.id.progress_card_team_raised_percent);
                    teamProgressRaisedPercentText.setText(teamProgressPercent.toString() + "%");
                    teamProgressRaisedPercentText.contentDescription = teamProgressPercent.toString() + "% " + getString(R.string.mobile_overview_progress_percent_progress)
                    val teamProgressRaisedGoalText =
                        findViewById<TextView>(R.id.progress_card_team_raised_goal);

                    if (obj.has("team_goal")) {
                        teamProgressRaisedGoalText.setText(
                            getString(R.string.mobile_overview_team_progress_goal).plus(
                                " "
                            ).plus(formatDoubleToLocalizedCurrency(toDouble(obj.get("team_goal"))))
                        );
                    } else {
                        teamProgressRaisedGoalText.setText(
                            getString(R.string.mobile_overview_team_progress_goal).plus(
                                " $0.00"
                            )
                        );
                    }
                }else{
                    second_progress_page.setVisibility(View.GONE);
                }

                if(hasCompany && getStringVariable("DISABLE_COMPANY_PROGRESS") != "true") {
                    val companyProgressRaisedAmountText =
                        findViewById<TextView>(R.id.progress_card_company_raised_amount);

                    if (obj.has("company_raised")) {
                        companyProgressRaisedAmountText.setText(
                            getString(R.string.mobile_overview_company_progress_raised).plus(
                                " "
                            ).plus(formatDoubleToLocalizedCurrency(toDouble(obj.get("company_raised"))))
                        );
                        setVariable(
                            "COMPANY_PROGRESS_RAISED",
                            withCommas(toDouble(obj.get("company_raised")))
                        )
                    } else {
                        companyProgressRaisedAmountText.setText(
                            getString(R.string.mobile_overview_company_progress_raised).plus(
                                " $0.00"
                            )
                        );
                    }

                    val companyProgressRaisedPercentText =
                        findViewById<TextView>(R.id.progress_card_company_raised_percent);
                    companyProgressRaisedPercentText.setText(companyProgressPercent.toString() + "%");
                    companyProgressRaisedPercentText.contentDescription = companyProgressPercent.toString() + "% " + getString(R.string.mobile_overview_progress_percent_progress)
                    val companyProgressRaisedGoalText =
                        findViewById<TextView>(R.id.progress_card_company_raised_goal);

                    if (obj.has("team_goal")) {
                        companyProgressRaisedGoalText.setText(
                            getString(R.string.mobile_overview_team_progress_goal).plus(
                                " "
                            ).plus(formatDoubleToLocalizedCurrency(toDouble(obj.get("company_goal"))))
                        );
                    } else {
                        companyProgressRaisedGoalText.setText(
                            getString(R.string.mobile_overview_team_progress_goal).plus(
                                " $0.00"
                            )
                        );
                    }
                }else{
                    third_progress_page.setVisibility(View.GONE);
                }
            }else{
                if(initial){
                    second_progress_page.setVisibility(View.GONE);
                    third_progress_page.setVisibility(View.GONE);
                    setupSlideButtons(slideLength,R.id.overview_progress_slide_buttons_container,"")
                }
            }
            
            if(initial){
                setupSlideButtons(slideLength,R.id.overview_progress_slide_buttons_container,"progress")
            }
            
            fadeInView(progressCard);
        }
    }

    fun getActivityProgressStats(){
        if(getStringVariable("ACTIVITY_TRACKING_ENABLED") == "true"){
            findViewById<LinearLayout>(R.id.personal_progress_buttons).setVisibility(View.VISIBLE)
            findViewById<LinearLayout>(R.id.team_progress_buttons).setVisibility(View.VISIBLE)
            findViewById<LinearLayout>(R.id.company_progress_buttons).setVisibility(View.VISIBLE)

            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/total/").plus(getConsID()).plus("/").plus(getEvent().event_id)
            var request = Request.Builder()
                .url(url)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()

            var client = OkHttpClient();

            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: okhttp3.Response) {
                    if(response.code != 200){
                        runOnUiThread {
                            findViewById<TextView>(R.id.progress_card_activity_stats_unit).text = "0";
                            findViewById<TextView>(R.id.team_progress_card_activity_stats_unit).text = "0";
                            findViewById<TextView>(R.id.progress_card_activity_stats_unit_label).text = getString(R.string.mobile_overview_team_progress_activity_points)
                            findViewById<TextView>(R.id.team_progress_card_activity_stats_unit_label).text = getString(R.string.mobile_overview_team_progress_activity_points)
                        }
                        val error = response.body?.string();
                        if (error != null) {
                            if(!error.contains("This user isn't connected to an activity tracker")){
                                throw Exception(response.body?.string())
                            }
                        }
                    }else{
                        val jsonString = response.body?.string();
                        val obj = JSONObject(jsonString);
                        if(obj.has("data")){
                            val data = obj.get("data") as JSONObject

                            var personal_points = 0;
                            var personal_minutes = 0;
                            var personal_miles = 0.00;
                            var personal_rank = 0;
                            var personal_total = 1;
                            var team_points = 0;
                            var team_minutes = 0;
                            var team_miles = 0.00;
                            var team_rank = 0;
                            var team_total = 0;
                            var company_points = 0;
                            var company_minutes = 0;
                            var company_miles = 0.00;
                            var company_rank = 0;
                            var company_total = 0;
                            val tracking_type = getStringVariable("ACTIVITY_TRACKING_TYPE")

                            if(data.has("totalPoints") && data.get("totalPoints") is Int){
                                personal_points = data.get("totalPoints") as Int
                            }
                            if(data.has("totalDistance")){
                                if(data.get("totalDistance") is Double){
                                    personal_miles = data.get("totalDistance") as Double
                                }else if (data.get("totalDistance") is Int){
                                    personal_miles = (data.get("totalDistance") as Int).toDouble()
                                }
                            }

                            if(data.has("totalMinutes") && data.get("totalMinutes") is Int){
                                personal_minutes = data.get("totalMinutes") as Int
                            }
                            if(data.has("rank") && data.get("rank") is Int){
                                personal_rank = data.get("rank") as Int
                            }
                            if(data.has("totalParticipants") && data.get("totalParticipants") is Int){
                                personal_total = data.get("totalParticipants") as Int
                            }
                            if(data.has("teamTotalPoints") && data.get("teamTotalPoints") is Int){
                                team_points = data.get("teamTotalPoints") as Int
                            }

                            if(data.has("teamTotalDistance")){
                                if(data.get("teamTotalDistance") is Double){
                                    team_miles = data.get("teamTotalDistance") as Double
                                }else if (data.get("teamTotalDistance") is Int){
                                    team_miles = (data.get("teamTotalDistance") as Int).toDouble()
                                }
                            }

                            if(data.has("teamTotalMinutes") && data.get("teamTotalMinutes") is Int){
                                team_minutes = data.get("teamTotalMinutes") as Int
                            }
                            if(data.has("teamRank") && data.get("teamRank") is Int){
                                team_rank = data.get("teamRank") as Int
                            }
                            if(data.has("totalTeams") && data.get("totalTeams") is Int){
                                team_total = data.get("totalTeams") as Int
                            }

                            if(data.has("companyTotalDistance")){
                                if(data.get("companyTotalDistance") is Double){
                                    company_miles = data.get("companyTotalDistance") as Double
                                }else if (data.get("companyTotalDistance") is Int){
                                    company_miles = (data.get("companyTotalDistance") as Int).toDouble()
                                }
                            }

                            if(data.has("companyTotalPoints") && data.get("companyTotalPoints") is Int){
                                company_points = data.get("companyTotalPoints") as Int
                            }
                            if(data.has("companyTotalMinutes") && data.get("companyTotalMinutes") is Int){
                                company_minutes = data.get("companyTotalMinutes") as Int
                            }
                            if(data.has("companyRank") && data.get("companyRank") is Int){
                                company_rank = data.get("companyRank") as Int
                            }
                            if(data.has("totalCompanies") && data.get("totalCompanies") is Int){
                                company_total = data.get("totalCompanies") as Int
                            }

                            var string = getString(R.string.mobile_overview_team_progress_activity_points)

                            runOnUiThread {
                                if(tracking_type == "distance"){
                                    string = getDistanceLabel(R.string.mobile_overview_team_progress_activity_miles, R.string.mobile_overview_team_progress_activity_km)
                                    findViewById<TextView>(R.id.progress_card_activity_stats_unit).text =
                                        personal_miles.toString()
                                }else if (tracking_type == "minutes"){
                                    string = getString(R.string.mobile_overview_team_progress_activity_minutes)
                                    findViewById<TextView>(R.id.progress_card_activity_stats_unit).text =
                                        personal_minutes.toString()
                                }else{
                                    string = getString(R.string.mobile_overview_team_progress_activity_points)
                                    findViewById<TextView>(R.id.progress_card_activity_stats_unit).text =
                                        personal_points.toString()
                                }

                                findViewById<TextView>(R.id.progress_card_activity_stats_unit_label).text = string
                                findViewById<TextView>(R.id.team_progress_card_activity_stats_unit_label).text = string
                                findViewById<TextView>(R.id.company_progress_card_activity_stats_unit_label).text = string

                                if(tracking_type == "distance"){
                                    findViewById<TextView>(R.id.team_progress_card_activity_stats_unit).text =
                                        team_miles.toString()
                                }else if (tracking_type == "minutes"){
                                    findViewById<TextView>(R.id.team_progress_card_activity_stats_unit).text =
                                        team_minutes.toString()
                                }else{
                                    findViewById<TextView>(R.id.team_progress_card_activity_stats_unit).text =
                                        team_points.toString()
                                }

                                if(tracking_type == "distance"){
                                    findViewById<TextView>(R.id.company_progress_card_activity_stats_unit).text =
                                        company_miles.toString()
                                }else if (tracking_type == "minutes"){
                                    findViewById<TextView>(R.id.company_progress_card_activity_stats_unit).text =
                                        company_minutes.toString()
                                }else{
                                    findViewById<TextView>(R.id.company_progress_card_activity_stats_unit).text =
                                        company_points.toString()
                                }
                            }
                        }else{
                            findViewById<FrameLayout>(R.id.personal_progress_raised_button).setVisibility(View.GONE)
                            findViewById<FrameLayout>(R.id.personal_progress_activity_button).setVisibility(View.GONE)
                            findViewById<FrameLayout>(R.id.personal_progress_challenges_button).setVisibility(View.GONE)
                            findViewById<FrameLayout>(R.id.team_progress_raised_button).setVisibility(View.GONE)
                            findViewById<FrameLayout>(R.id.team_progress_activity_button).setVisibility(View.GONE)
                            findViewById<FrameLayout>(R.id.company_progress_raised_button).setVisibility(View.GONE)
                            findViewById<FrameLayout>(R.id.company_progress_activity_button).setVisibility(View.GONE)
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println("GET PROGRESS STATS ERROR")
                    println(e)
                }
            })
            checkChallenge()
            setCustomToggleButtonColor(findViewById(R.id.personal_progress_raised_button), "active")
            setCustomToggleButtonColor(findViewById(R.id.personal_progress_activity_button), "inactive")
            setCustomToggleButtonColor(findViewById(R.id.personal_progress_challenges_button), "inactive")
            setCustomToggleButtonColor(findViewById(R.id.team_progress_raised_button), "active")
            setCustomToggleButtonColor(findViewById(R.id.team_progress_activity_button), "inactive")
            setCustomToggleButtonColor(findViewById(R.id.company_progress_raised_button), "active")
            setCustomToggleButtonColor(findViewById(R.id.company_progress_activity_button), "inactive")

            setCustomToggleButtonColor(findViewById(R.id.personal_leaderboard_raised_button), "active")
            setCustomToggleButtonColor(findViewById(R.id.personal_leaderboard_activity_button), "inactive")
            if (findViewById<FrameLayout>(R.id.team_leaderboard_raised_button) != null) {
                setCustomToggleButtonColor(findViewById(R.id.team_leaderboard_raised_button), "active")
                setCustomToggleButtonColor(findViewById(R.id.team_leaderboard_activity_button), "inactive")
            }

            if(findViewById<FrameLayout>(R.id.company_leaderboard_raised_button) != null){
                setCustomToggleButtonColor(findViewById(R.id.company_leaderboard_raised_button), "active")
                setCustomToggleButtonColor(findViewById(R.id.company_leaderboard_activity_button), "inactive")
            }

            findViewById<FrameLayout>(R.id.personal_progress_raised_button).setOnClickListener {
                sendGoogleAnalytics("overview_personal_raised_toggle","overview")
                switchProgressBar("personal", "raised")
            }

            findViewById<FrameLayout>(R.id.personal_progress_activity_button).setOnClickListener {
                sendGoogleAnalytics("overview_personal_activity_toggle","overview")
                switchProgressBar("personal", "activity")
            }

            findViewById<FrameLayout>(R.id.personal_progress_activity_button).getChildAt(1).setOnClickListener {
                sendGoogleAnalytics("overview_personal_activity_toggle","overview")
                switchProgressBar("personal", "activity")
            }

            findViewById<FrameLayout>(R.id.personal_progress_challenges_button).setOnClickListener {
                sendGoogleAnalytics("overview_personal_challenge_toggle","overview")
                switchProgressBar("personal",  "challenges")
            }

            findViewById<FrameLayout>(R.id.team_progress_raised_button).getChildAt(0).setOnClickListener {
                sendGoogleAnalytics("overview_team_raised_toggle","overview")
                switchProgressBar("team", "raised")
            }

            findViewById<FrameLayout>(R.id.team_progress_raised_button).getChildAt(1).setOnClickListener {
                sendGoogleAnalytics("overview_team_raised_toggle","overview")
                switchProgressBar("team", "raised")
            }

            findViewById<FrameLayout>(R.id.team_progress_activity_button).getChildAt(1).setOnClickListener {
                sendGoogleAnalytics("overview_team_activity_toggle","overview")
                switchProgressBar("team", "activity")
            }

            findViewById<FrameLayout>(R.id.team_progress_activity_button).getChildAt(0).setOnClickListener {
                sendGoogleAnalytics("overview_team_activity_toggle","overview")
                switchProgressBar("team", "activity")
            }

            findViewById<FrameLayout>(R.id.team_progress_activity_button).setOnClickListener {
                sendGoogleAnalytics("overview_team_activity_toggle","overview")
                switchProgressBar("team", "activity")
            }

            findViewById<FrameLayout>(R.id.company_progress_raised_button).getChildAt(0).setOnClickListener {
                sendGoogleAnalytics("overview_company_raised_toggle","overview")
                switchProgressBar("company", "raised")
            }

            findViewById<FrameLayout>(R.id.company_progress_raised_button).getChildAt(1).setOnClickListener {
                sendGoogleAnalytics("overview_company_raised_toggle","overview")
                switchProgressBar("company", "raised")
            }

            findViewById<FrameLayout>(R.id.company_progress_activity_button).getChildAt(1).setOnClickListener {
                sendGoogleAnalytics("overview_company_activity_toggle","overview")
                switchProgressBar("company", "activity")
            }

            findViewById<FrameLayout>(R.id.company_progress_activity_button).getChildAt(0).setOnClickListener {
                sendGoogleAnalytics("overview_company_activity_toggle","overview")
                switchProgressBar("company", "activity")
            }
        }else{
            checkChallenge()
            findViewById<FrameLayout>(R.id.personal_progress_raised_button).setVisibility(View.GONE)
            findViewById<FrameLayout>(R.id.personal_progress_activity_button).setVisibility(View.GONE)
            findViewById<FrameLayout>(R.id.personal_progress_challenges_button).setVisibility(View.GONE)
            findViewById<FrameLayout>(R.id.team_progress_raised_button).setVisibility(View.GONE)
            findViewById<FrameLayout>(R.id.team_progress_activity_button).setVisibility(View.GONE)
            findViewById<FrameLayout>(R.id.company_progress_raised_button).setVisibility(View.GONE)
            findViewById<FrameLayout>(R.id.company_progress_activity_button).setVisibility(View.GONE)
        }
    }

    fun loadBadgesCard(){
        var url = getResources().getString(R.string.base_server_url)
            .plus("/").plus(getStringVariable("CLIENT_CODE"))
            .plus("/events/badges/").plus(getConsID())
            .plus("/").plus(getEvent().event_id)

        var request = Request.Builder()
            .url(url)
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .build()
        var client = OkHttpClient();
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                if(response.code != 200){
                    runOnUiThread{
                        findViewById<LinearLayout>(R.id.overview_badges_container).visibility = View.GONE
                    }
                }else{
                    val response = response.body?.string();
                    val obj = JSONObject(response);

                    if(obj.has("badges") && obj.get("badges") is JSONArray){
                        val badges = obj.get("badges") as JSONArray;
                        val chunk_size = 4;
                        var current_chunk_index = 0;
                        var current_chunk_row = 0;
                        val inflater = layoutInflater;
                        var parent = findViewById<LinearLayout>(R.id.overview_badges_container)
                        runOnUiThread{
                            for (i in 0 until badges.length()) {
                                val badge = badges.getJSONObject(i);

                                if(current_chunk_index >= chunk_size){
                                    current_chunk_index = 0
                                    current_chunk_row += 1;
                                }

                                if(current_chunk_index == 0){
                                    val row: BadgesRowBinding = DataBindingUtil.inflate(
                                        inflater, R.layout.badges_row, parent, true)
                                    row.colorList = getColorList("")
                                }

                                val row = parent.getChildAt(current_chunk_row) as LinearLayout

                                val binding: BadgeBinding = DataBindingUtil.inflate(
                                    inflater, R.layout.badge, row, true)
                                binding.colorList = getColorList("")

                                val image = getSafeStringVariable(badge, "badge_image")

                                val badge_xml = binding.root as LinearLayout
                                val image_view = badge_xml.getChildAt(0) as ImageView

                                if(image != ""){
                                    Glide.with(this@Overview)
                                        .load(image)
                                        .into(image_view)
                                }

                                val text_view = (badge_xml.getChildAt(1) as TextView)
                                text_view.setText(getSafeStringVariable(badge,"badge_name"))

                                current_chunk_index += 1
                            }
                        }
                    }else{
                        findViewById<LinearLayout>(R.id.overview_badges_container).visibility = View.GONE
                    }
                }
            }

            override fun onFailure(call: Call, e: java.io.IOException) {
                runOnUiThread{
                    findViewById<LinearLayout>(R.id.overview_badges_container).visibility = View.GONE
                }
            }
        })
    }

    fun checkChallenge(){
        val challenge_string = getStringVariable("CHALLENGES_OBJECT")
        if(challenge_string != "") {
            val challenge = JSONObject(challenge_string)
            var challenge_type = getSafeStringVariable(challenge, "challenge_type")
            var challenge_status = getSafeStringVariable(challenge, "challenge_status")
            current_challenge_type = challenge_type;
            current_challenge_status = challenge_status;
            if(challenge_status == "Live" && (challenge_type == "BOTH" || challenge_type == "ACTIVITY")){
                getChallengeData(challenge)
                findViewById<Button>(R.id.btn_join_challenge).setOnClickListener{
                    sendGoogleAnalytics("challenges_join", "overview")
                    joinChallenge(challenge)                    
                }
            }else{
                findViewById<FrameLayout>(R.id.personal_progress_challenges_button).visibility = View.GONE;
            }
        }else{
            findViewById<FrameLayout>(R.id.personal_progress_challenges_button).visibility = View.GONE;
        }
    }

    fun checkForChallengesLeaderboard(){
        var challenge_valid = false;
        val leaderboard_table = findViewById<TableLayout>(R.id.leaderboard_activity_table);
        removeAllRows(leaderboard_table)
        val challenge_string = getStringVariable("CHALLENGES_OBJECT")
        if(challenge_string != "") {
            val challenge = JSONObject(challenge_string)
            var challenge_type = getSafeStringVariable(challenge, "challenge_type")
            var challenge_status = getSafeStringVariable(challenge, "challenge_status")
            current_challenge_type = challenge_type;
            current_challenge_status = challenge_status;
            if(challenge_status == "Live" && (challenge_type == "BOTH" || challenge_type == "ACTIVITY")){
                if(getStringVariable("OVERRIDE_OVERVIEW_ACTIVITY_LEADERBOARD_CHALLENGES") == "true"){
                    challenge_valid = true;
                }
            }
        }

        val button = findViewById<FrameLayout>(R.id.personal_leaderboard_activity_button)
        val header = findViewById<TextView>(R.id.leaderboard_activity_table_unit_label)
        val team_button = findViewById<FrameLayout>(R.id.team_leaderboard_activity_button)
        val team_header = findViewById<TextView>(R.id.team_leaderboard_activity_table_unit_label)
        val company_button = findViewById<FrameLayout>(R.id.company_leaderboard_activity_button)
        val company_header = findViewById<TextView>(R.id.company_leaderboard_activity_table_unit_label)
        if(challenge_valid){
            ((button.getChildAt(0) as LinearLayout).getChildAt(1) as TextView).text = getString(R.string.mobile_overview_leaderboard_challenges_button)
            ((button.getChildAt(1) as LinearLayout).getChildAt(1) as TextView).text = getString(R.string.mobile_overview_leaderboard_challenges_button)
            ((team_button.getChildAt(0) as LinearLayout).getChildAt(1) as TextView).text = getString(R.string.mobile_overview_team_leaderboard_challenge_button)
            ((team_button.getChildAt(1) as LinearLayout).getChildAt(1) as TextView).text = getString(R.string.mobile_overview_team_leaderboard_challenge_button)
            ((company_button.getChildAt(0) as LinearLayout).getChildAt(1) as TextView).text = getString(R.string.mobile_overview_team_leaderboard_challenge_button)
            ((company_button.getChildAt(1) as LinearLayout).getChildAt(1) as TextView).text = getString(R.string.mobile_overview_team_leaderboard_challenge_button)
            loadChallengesLeaderboard(JSONObject(challenge_string))
            if(challenge_metric == "DISTANCE"){
                header.text = getDistanceLabel(R.string.mobile_overview_leaderboard_challenge_miles, R.string.mobile_overview_leaderboard_challenge_km)
                team_header.text = getDistanceLabel(R.string.mobile_overview_team_leaderboard_challenge_miles, R.string.mobile_overview_team_leaderboard_challenge_km)
            }else{
                header.text = getString(R.string.mobile_overview_leaderboard_challenge_points)
                team_header.text = getString(R.string.mobile_overview_team_leaderboard_challenge_points)
            }
        }else{
            ((button.getChildAt(0) as LinearLayout).getChildAt(1) as TextView).text = getString(R.string.mobile_overview_leaderboard_activity_button)
            ((button.getChildAt(1) as LinearLayout).getChildAt(1) as TextView).text = getString(R.string.mobile_overview_leaderboard_activity_button)
            if(team_button != null){
                ((team_button.getChildAt(0) as LinearLayout).getChildAt(1) as TextView).text = getString(R.string.mobile_overview_leaderboard_activity_button)
                ((team_button.getChildAt(1) as LinearLayout).getChildAt(1) as TextView).text = getString(R.string.mobile_overview_leaderboard_activity_button)
            }
            if(company_button != null){
                ((company_button.getChildAt(0) as LinearLayout).getChildAt(1) as TextView).text = getString(R.string.mobile_overview_leaderboard_activity_button)
                ((company_button.getChildAt(1) as LinearLayout).getChildAt(1) as TextView).text = getString(R.string.mobile_overview_leaderboard_activity_button)
            }

            header.text = getString(R.string.mobile_overview_leaderboard_activity_points)
            if(team_header != null){
                team_header.text = getString(R.string.mobile_overview_leaderboard_activity_points)
            }
            if(company_header != null){
                company_header.text = getString(R.string.mobile_overview_leaderboard_activity_points)
            }
        }
    }

    fun loadChallengesLeaderboard(challenge: JSONObject){
        var path = "/activity/personalChallengeLeaderboard"

        challenge_metric = getSafeStringVariable(challenge, "activity_metric")
        if(challenge_metric == "DISTANCE"){
            path = "/activity/personalChallengeDistanceLeaderboard"
        }
        var challenge_status = getSafeStringVariable(challenge, "challenge_status")
        val challenge_id = getSafeIntegerVariable(challenge, "id")
        current_challenge_status = challenge_status;
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
                        challenge_leaderboard_participants = emptyArray()
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
                                val challenge_metric = getSafeStringVariable(challenge, "activity_metric")
                                if(challenge_metric == "DISTANCE"){
                                    value = getSafeDoubleVariable(obj, "totalDistance");
                                }

                                challenge_leaderboard_participants += challengeParticipant(
                                    rank,
                                    name,
                                    getSafeIntegerVariable(obj, "totalPoints"),
                                    value,
                                );
                            } catch (exception: java.io.IOException) {
                                println("GET LEADERBOARD ERROR")
                            }
                        }
                    }

                    runOnUiThread {
                        val leaderboard_table = findViewById<TableLayout>(R.id.leaderboard_activity_table);
                        removeAllRows(leaderboard_table)
                        addChallengeRows(leaderboard_table, challenge_leaderboard_participants);
                        if(getStringVariable("HAS_TEAM") == "true") {
                            loadTeamChallengesLeaderboard(challenge)
                        }

                        if(getStringVariable("HAS_COMPANY") == "true") {
                            loadCompanyChallengesLeaderboard(challenge)
                        }
                    }
                }
            }
            override fun onFailure(call: Call, e: java.io.IOException) {
                //println(e.message.toString());
            }
        })
    }

    fun loadTeamChallengesLeaderboard(challenge: JSONObject){
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
                        challenge_leaderboard_teams = emptyArray()
                        for(i in 0 .. length){
                            try {
                                var obj = json.getJSONObject(i);

                                var rank = ""
                                if(getSafeIntegerVariable(obj, "rank") != 0){
                                    rank = getSafeIntegerVariable(obj, "rank").toString() + "."
                                }

                                var value = 0.00
                                if(challenge_metric == "DISTANCE"){
                                    value = getSafeDoubleVariable(obj, "totalDistance");
                                }

                                challenge_leaderboard_teams += challengeParticipant(
                                    rank,
                                    getSafeStringVariable(obj, "teamName"),
                                    getSafeIntegerVariable(obj, "totalPoints"),
                                    value,
                                );
                            } catch (exception: java.io.IOException) {
                                println("GET TEAM LEADERBOARD ERROR")
                            }
                        }
                    }

                    runOnUiThread {
                        val team_leaderboard_table = findViewById<TableLayout>(R.id.leaderboard_teams_activity_table);
                        removeAllRows(team_leaderboard_table)
                        addChallengeRows(team_leaderboard_table, challenge_leaderboard_teams);
                    }
                }
            }
            override fun onFailure(call: Call, e: java.io.IOException) {
                //println(e.message.toString());
            }
        })
    }

    fun loadCompanyChallengesLeaderboard(challenge: JSONObject){
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

                    if(obj.has("data") && obj.get("data") is JSONArray){
                        val json = obj.get("data") as JSONArray;
                        var length = json.length()-1;
                        if(length > 9){
                            length = 9;
                        }
                        challenge_leaderboard_companies = emptyArray()
                        for(i in 0 .. length){
                            try {
                                var obj = json.getJSONObject(i);
                                var rank = ""
                                if(getSafeIntegerVariable(obj, "rank") != 0){
                                    rank = getSafeIntegerVariable(obj, "rank").toString() + "."
                                }

                                var value = 0.00
                                if(challenge_metric == "DISTANCE"){
                                    value = getSafeDoubleVariable(obj, "totalDistance");
                                }

                                challenge_leaderboard_companies += challengeParticipant(
                                    rank,
                                    getSafeStringVariable(obj, "companyName"),
                                    getSafeIntegerVariable(obj, "totalPoints"),
                                    value,
                                );
                            } catch (exception: java.io.IOException) {
                                println("GET COMPANY LEADERBOARD ERROR")
                            }
                        }
                    }

                    runOnUiThread {
                        val company_leaderboard_table = findViewById<TableLayout>(R.id.leaderboard_companies_activity_table);
                        removeAllRows(company_leaderboard_table)
                        addChallengeRows(company_leaderboard_table, challenge_leaderboard_companies);
                    }
                }
            }
            override fun onFailure(call: Call, e: java.io.IOException) {
                //println(e.message.toString());
            }
        })
    }

    fun getChallengeData(challenge: JSONObject){
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
                        runOnUiThread{
                            findViewById<LinearLayout>(R.id.personal_progress_buttons).setVisibility(View.GONE);
                            findViewById<LinearLayout>(R.id.team_progress_buttons).setVisibility(View.GONE);
                            findViewById<LinearLayout>(R.id.company_progress_buttons).setVisibility(View.GONE);
                        }
                    }else{
                        val response = response.body?.string();
                        val obj = JSONObject(response);
                        if(obj.has("data") && obj.get("data") is JSONObject){
                            val data = obj.get("data") as JSONObject;
                            runOnUiThread{
                                findViewById<FrameLayout>(R.id.personal_progress_activity_button).visibility = View.GONE;
                                findViewById<FrameLayout>(R.id.personal_progress_challenges_button).visibility = View.VISIBLE;
                                updateUserChallengeInfo(data, challenge)
                            }
                        }
                    }
                }

                override fun onFailure(call: Call, e: java.io.IOException) {
                    runOnUiThread{
                        findViewById<LinearLayout>(R.id.personal_progress_buttons).setVisibility(View.GONE);
                        findViewById<LinearLayout>(R.id.team_progress_buttons).setVisibility(View.GONE);
                        findViewById<LinearLayout>(R.id.company_progress_buttons).setVisibility(View.GONE);
                    }
                }
            })
        }else{
            findViewById<FrameLayout>(R.id.personal_progress_challenges_button).visibility = View.GONE;
        }
    }

    fun switchProgressBar(slide: String, newbar: String){
        var oldSlide = findViewById<LinearLayout>(R.id.personal_progress_activity_stats)
        var newSlide = findViewById<LinearLayout>(R.id.personal_progress_amount_bars)
        var oldButton = findViewById<FrameLayout>(R.id.personal_progress_activity_button)
        var newButton = findViewById<FrameLayout>(R.id.personal_progress_raised_button)

        if(slide == "personal"){
            if(newbar == "activity"){
                oldSlide = findViewById(R.id.personal_progress_amount_bars)
                newSlide = findViewById(R.id.personal_progress_activity_stats)
                oldButton = findViewById(R.id.personal_progress_raised_button)
                newButton = findViewById(R.id.personal_progress_activity_button)
            }else if (newbar == "challenges"){
                oldSlide = findViewById(R.id.personal_progress_amount_bars)
                newSlide = findViewById(R.id.personal_progress_challenge_slide)
                oldButton = findViewById(R.id.personal_progress_raised_button)
                newButton = findViewById(R.id.personal_progress_challenges_button)
            }else if (newbar == "raised"){
                newSlide = findViewById(R.id.personal_progress_amount_bars)
                newButton = findViewById(R.id.personal_progress_raised_button)

                if (findViewById<LinearLayout>(R.id.personal_progress_activity_stats).visibility == View.VISIBLE) {
                    oldSlide = findViewById(R.id.personal_progress_activity_stats)
                    oldButton = findViewById(R.id.personal_progress_activity_button)
                } else {
                    oldSlide = findViewById(R.id.personal_progress_challenge_slide)
                    oldButton = findViewById(R.id.personal_progress_challenges_button)
                }
            }else{
                findViewById<LinearLayout>(R.id.personal_progress_challenge_slide).visibility = View.GONE
                oldButton = findViewById(R.id.personal_progress_challenges_button)
                findViewById<FrameLayout>(R.id.personal_progress_challenges_button).contentDescription = "Challenges Button Not Selected"
            }
        }else if (slide == "team"){
            if(newbar == "activity"){
                oldSlide = findViewById(R.id.team_progress_amount_bars)
                newSlide = findViewById(R.id.team_progress_activity_stats)
                oldButton = findViewById(R.id.team_progress_raised_button)
                newButton = findViewById(R.id.team_progress_activity_button)
            }else{
                oldSlide = findViewById(R.id.team_progress_activity_stats)
                newSlide = findViewById(R.id.team_progress_amount_bars)
                oldButton = findViewById(R.id.team_progress_activity_button)
                newButton = findViewById(R.id.team_progress_raised_button)
            }
        }else if (slide == "company"){
            if(newbar == "activity"){
                oldSlide = findViewById(R.id.company_progress_amount_bars)
                newSlide = findViewById(R.id.company_progress_activity_stats)
                oldButton = findViewById(R.id.company_progress_raised_button)
                newButton = findViewById(R.id.company_progress_activity_button)
            }else{
                oldSlide = findViewById(R.id.company_progress_activity_stats)
                newSlide = findViewById(R.id.company_progress_amount_bars)
                oldButton = findViewById(R.id.company_progress_activity_button)
                newButton = findViewById(R.id.company_progress_raised_button)
            }
        }

        oldSlide.setVisibility(View.GONE)
        newSlide.setVisibility(View.VISIBLE)
        newButton.contentDescription = "${newButton.tag} Button Selected"

        oldButton.contentDescription = "${oldButton.tag} Button Not Selected"
        newButton.announceForAccessibility("${newButton.tag} Button Selected")

        setCustomToggleButtonColor(oldButton,"inactive")
        setCustomToggleButtonColor(newButton,"active")
    }

    fun getLeaderboardActivityStats(){
        if(getStringVariable("ACTIVITY_TRACKING_ENABLED") == "true" && getStringVariable("DISABLE_OVERVIEW_ACTIVITY_LEADERBOARDS") != "true"){
            findViewById<LinearLayout>(R.id.personal_leaderboard_buttons).setVisibility(View.VISIBLE)
            findViewById<LinearLayout>(R.id.team_leaderboard_buttons).setVisibility(View.VISIBLE)
            findViewById<LinearLayout>(R.id.company_leaderboard_buttons).setVisibility(View.VISIBLE)
            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/personalLeaderboard/").plus(getConsID()).plus("/").plus(getEvent().event_id)
            var request = Request.Builder()
                .url(url)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()

            var client = OkHttpClient();
            var override_challenges = false;

            if(getStringVariable("OVERRIDE_OVERVIEW_ACTIVITY_LEADERBOARD_CHALLENGES") == "true" &&
                (current_challenge_status == "Live" && (current_challenge_type == "BOTH" || current_challenge_type == "ACTIVITY"))
            ){  override_challenges = true }

            if(!override_challenges){
                client.newCall(request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: okhttp3.Response) {
                        val jsonString = response.body?.string();
                        val obj = JSONObject(jsonString)
                        if(obj.has("data")){
                            val dataArray = obj.get("data") as JSONArray
                            for(i in 0 until dataArray.length()) {
                                val element = dataArray[i] as JSONObject

                                var name = "";
                                var points = "";
                                var miles = "0";
                                var minutes = "0";
                                var rank = "";

                                if (element.has("firstName") && element.get("firstName") is String) {
                                    name = element.get("firstName") as String
                                }

                                if (element.has("lastName") && element.get("lastName") is String) {
                                    name += " " + element.get("lastName") as String
                                }

                                if (element.has("totalPoints") && element.get("totalPoints") is Int) {
                                    points = (element.get("totalPoints") as Int).toString()
                                }

                                if (element.has("totalMinutes") && element.get("totalMinutes") is Int) {
                                    minutes = (element.get("totalMinutes") as Int).toString()
                                }

                                if (element.has("totalDistance")){
                                    if (element.get("totalDistance") is Double) {
                                        miles = (element.get("totalDistance") as Double).toString()
                                    }else if (element.get("totalDistance") is Int){
                                        miles = (element.get("totalDistance") as Int).toString()
                                    }
                                }

                                if (element.has("rank") && element.get("rank") is Int) {
                                    rank = (element.get("rank") as Int).toString()
                                }

                                activity_leaderboard_participants += ActivityParticipant(
                                    rank,
                                    name,
                                    "",
                                    points,
                                    miles,
                                    minutes
                                );
                            }

                            runOnUiThread {
                                val tracking_type = getStringVariable("ACTIVITY_TRACKING_TYPE")
                                var string = getString(R.string.mobile_overview_team_progress_activity_points)
                                if(tracking_type == "distance"){
                                    string = getDistanceLabel(R.string.mobile_overview_progress_activity_miles, R.string.mobile_overview_progress_activity_km)
                                }else if (tracking_type == "minutes"){
                                    string = getString(R.string.mobile_overview_team_progress_activity_minutes)
                                }else{
                                    string = getString(R.string.mobile_overview_team_progress_activity_points)
                                }

                                findViewById<TextView>(R.id.leaderboard_activity_table_unit_label).setText(string)


                                addActivityRows(
                                    findViewById(R.id.leaderboard_activity_table),
                                    activity_leaderboard_participants
                                )
                            }
                        }
                    }

                    override fun onFailure(call: Call, e: java.io.IOException) {
                        println(e.message.toString());
                    }
                })
            }

            println("********* HIDE TEAM LEADERBOARD: " + getStringVariable("HIDE_TEAM_LEADERBOARD"))

            if(getStringVariable("HIDE_TEAM_LEADERBOARD") != "true"){
                val tracking_type = getStringVariable("ACTIVITY_TRACKING_TYPE")
                var string = getString(R.string.mobile_overview_team_progress_activity_points)
                if(tracking_type == "distance"){
                    string = getDistanceLabel(R.string.mobile_overview_team_progress_activity_miles, R.string.mobile_overview_team_progress_activity_km)
                }else if (tracking_type == "minutes"){
                    string = getString(R.string.mobile_overview_team_progress_activity_minutes)
                }

                var override_challenges = false;
                if(getStringVariable("OVERRIDE_OVERVIEW_ACTIVITY_LEADERBOARD_CHALLENGES") == "true" && 
                    (current_challenge_status == "Live" && (current_challenge_type == "BOTH" || current_challenge_type == "ACTIVITY"))
                ){  override_challenges = true }

                if(!override_challenges){
                    findViewById<TextView>(R.id.team_leaderboard_activity_table_unit_label).setText(string)
                }

                val team_url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/teamLeaderboard/").plus(getConsID()).plus("/").plus(getEvent().event_id)
                var team_request = Request.Builder()
                    .url(team_url)
                    .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                    .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                    .build()

                client.newCall(team_request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: okhttp3.Response) {
                        val jsonString = response.body?.string();
                        val obj = JSONObject(jsonString)
                        if(obj.has("data")){
                            val dataArray = obj.get("data") as JSONArray
                            for(i in 0 until dataArray.length()){
                                val element = dataArray[i] as JSONObject
                                var team_name = "";
                                var team_points = "";
                                var team_rank = "";
                                var team_minutes = "0";
                                var team_miles = "0";

                                if(element.has("teamName") && element.get("teamName") is String){
                                    team_name = element.get("teamName") as String
                                }

                                if(element.has("totalPoints") && element.get("totalPoints") is Int){
                                    team_points = (element.get("totalPoints") as Int).toString()
                                }

                                if (element.has("totalMinutes") && element.get("totalMinutes") is Int) {
                                    team_minutes = (element.get("totalMinutes") as Int).toString()
                                }

                                if (element.has("totalDistance")){
                                    if (element.get("totalDistance") is Double) {
                                        team_miles = (element.get("totalDistance") as Double).toString()
                                    }else if (element.get("totalDistance") is Int){
                                        team_miles = (element.get("totalDistance") as Int).toString()
                                    }
                                }

                                if(element.has("rank") && element.get("rank") is Int){
                                    team_rank = (element.get("rank") as Int).toString()
                                }
                                activity_leaderboard_teams += ActivityParticipant(team_rank,team_name, "", team_points, team_miles, team_minutes);
                            }

                            switchLeaderboardType("team","raised")

                            runOnUiThread {
                                addActivityRows(findViewById(R.id.leaderboard_teams_activity_table),activity_leaderboard_teams)

                                findViewById<FrameLayout>(R.id.team_leaderboard_raised_button).setOnClickListener{
                                    sendGoogleAnalytics("overview_top_teams_raised_toggle","overview")
                                    switchLeaderboardType("team","raised")
                                }

                                findViewById<FrameLayout>(R.id.team_leaderboard_activity_button).setOnClickListener{
                                    sendGoogleAnalytics("overview_top_teams_activity_toggle","overview")
                                    switchLeaderboardType("team","activity")
                                }
                            }
                        }
                    }

                    override fun onFailure(call: Call, e: java.io.IOException) {
                        println(e.message.toString());
                    }
                })
            }

            if(hasCompany){
                val tracking_type = getStringVariable("ACTIVITY_TRACKING_TYPE")
                var string = getString(R.string.mobile_overview_team_progress_activity_points)
                if(tracking_type == "distance"){
                    string = getDistanceLabel(R.string.mobile_overview_company_progress_activity_miles, R.string.mobile_overview_company_progress_activity_km)
                }else if (tracking_type == "minutes"){
                    string = getString(R.string.mobile_overview_team_progress_activity_minutes)
                }

                findViewById<TextView>(R.id.company_leaderboard_activity_table_unit_label).setText(string)

                val team_url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/companyLeaderboard/").plus(getConsID()).plus("/").plus(getEvent().event_id)
                var team_request = Request.Builder()
                    .url(team_url)
                    .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                    .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                    .build()

                client.newCall(team_request).enqueue(object : Callback {
                    override fun onResponse(call: Call, response: okhttp3.Response) {
                        val jsonString = response.body?.string();
                        val obj = JSONObject(jsonString)
                        if(obj.has("data")){
                            val dataArray = obj.get("data") as JSONArray
                            for(i in 0 until dataArray.length()){
                                val element = dataArray[i] as JSONObject

                                var company_name = "";
                                var company_points = "";
                                var company_rank = "";
                                var company_minutes = "0";
                                var company_miles = "0";

                                if(element.has("companyName") && element.get("companyName") is String){
                                    company_name = element.get("companyName") as String
                                }

                                if(element.has("totalPoints") && element.get("totalPoints") is Int){
                                    company_points = (element.get("totalPoints") as Int).toString()
                                }

                                if (element.has("totalMinutes") && element.get("totalMinutes") is Int) {
                                    company_minutes = (element.get("totalMinutes") as Int).toString()
                                }

                                if (element.has("totalDistance")){
                                    if (element.get("totalDistance") is Double) {
                                        company_miles = (element.get("totalDistance") as Double).toString()
                                    }else if (element.get("totalDistance") is Int){
                                        company_miles = (element.get("totalDistance") as Int).toString()
                                    }
                                }

                                if(element.has("rank") && element.get("rank") is Int){
                                    company_rank = (element.get("rank") as Int).toString()
                                }
                                activity_leaderboard_companies += ActivityParticipant(company_rank, company_name, "", company_points, company_miles, company_minutes);
                            }

                            runOnUiThread {
                                addActivityRows(findViewById(R.id.leaderboard_companies_activity_table),activity_leaderboard_companies)

                                findViewById<FrameLayout>(R.id.company_leaderboard_raised_button).setOnClickListener{
                                    sendGoogleAnalytics("overview_top_companies_raised_toggle","overview")
                                    switchLeaderboardType("company","raised")
                                }

                                findViewById<FrameLayout>(R.id.company_leaderboard_activity_button).setOnClickListener{
                                    sendGoogleAnalytics("overview_top_companies_activity_toggle","overview")
                                    switchLeaderboardType("company","activity")
                                }
                            }
                        }
                    }

                    override fun onFailure(call: Call, e: java.io.IOException) {
                        println(e.message.toString());
                    }
                })
            }
        }else{
            findViewById<LinearLayout>(R.id.personal_leaderboard_buttons).setVisibility(View.GONE)
            if(hasTeam){
                findViewById<LinearLayout>(R.id.team_leaderboard_buttons).setVisibility(View.GONE)
            }
            if(hasCompany) {
                findViewById<LinearLayout>(R.id.company_leaderboard_buttons).setVisibility(View.GONE)
            }
        }

        findViewById<FrameLayout>(R.id.personal_leaderboard_raised_button).getChildAt(0).setOnClickListener {
            sendGoogleAnalytics("overview_top_indiv_raised_toggle","overview")
            switchLeaderboardType("personal", "raised")
        }

        findViewById<FrameLayout>(R.id.personal_leaderboard_raised_button).getChildAt(1).setOnClickListener {
            sendGoogleAnalytics("overview_top_indiv_raised_toggle","overview")
            switchLeaderboardType("personal", "raised")
        }

        findViewById<FrameLayout>(R.id.personal_leaderboard_activity_button).getChildAt(0).setOnClickListener {
            sendGoogleAnalytics("overview_top_indiv_activity_toggle","overview")
            switchLeaderboardType("personal", "activity")
        }

        findViewById<FrameLayout>(R.id.personal_leaderboard_activity_button).getChildAt(1).setOnClickListener {
            sendGoogleAnalytics("overview_top_indiv_activity_toggle","overview")
            switchLeaderboardType("personal", "activity")
        }

        findViewById<FrameLayout>(R.id.team_leaderboard_raised_button).getChildAt(0).setOnClickListener {
            sendGoogleAnalytics("overview_top_team_raised_toggle","overview")
            switchLeaderboardType("team", "raised")
        }

        findViewById<FrameLayout>(R.id.team_leaderboard_raised_button).getChildAt(1).setOnClickListener {
            sendGoogleAnalytics("overview_top_team_raised_toggle","overview")
            switchLeaderboardType("team", "raised")
        }

        findViewById<FrameLayout>(R.id.team_leaderboard_activity_button).getChildAt(0).setOnClickListener {
            sendGoogleAnalytics("overview_top_team_activity_toggle","overview")
            switchLeaderboardType("team", "activity")
        }

        findViewById<FrameLayout>(R.id.team_leaderboard_activity_button).getChildAt(1).setOnClickListener {
            sendGoogleAnalytics("overview_top_team_activity_toggle","overview")
            switchLeaderboardType("team", "activity")
        }

        findViewById<FrameLayout>(R.id.company_leaderboard_raised_button).getChildAt(0).setOnClickListener {
            sendGoogleAnalytics("overview_top_company_raised_toggle","overview")
            switchLeaderboardType("company", "raised")
        }

        findViewById<FrameLayout>(R.id.company_leaderboard_raised_button).getChildAt(1).setOnClickListener {
            sendGoogleAnalytics("overview_top_company_raised_toggle","overview")
            switchLeaderboardType("company", "raised")
        }

        findViewById<FrameLayout>(R.id.company_leaderboard_activity_button).getChildAt(0).setOnClickListener {
            sendGoogleAnalytics("overview_top_company_activity_toggle","overview")
            switchLeaderboardType("company", "activity")
        }

        findViewById<FrameLayout>(R.id.company_leaderboard_activity_button).getChildAt(1).setOnClickListener {
            sendGoogleAnalytics("overview_top_company_activity_toggle","overview")
            switchLeaderboardType("company", "activity")
        }
    }

    fun switchLeaderboardType(slide: String, newtype: String){
        var oldSlide = findViewById<LinearLayout>(R.id.leaderboard_activity_table)
        var newSlide = findViewById<LinearLayout>(R.id.leaderboard_table)
        var oldButton = findViewById<FrameLayout>(R.id.personal_leaderboard_activity_button)
        var newButton = findViewById<FrameLayout>(R.id.personal_leaderboard_raised_button)

        if(slide == "personal"){
            if(newtype == "activity"){
                oldSlide = findViewById(R.id.leaderboard_table)
                newSlide = findViewById(R.id.leaderboard_activity_table)
                oldButton = findViewById(R.id.personal_leaderboard_raised_button)
                newButton = findViewById(R.id.personal_leaderboard_activity_button)
            }
        }else if (slide == "team"){
            if(newtype == "activity"){
                oldSlide = findViewById(R.id.leaderboard_teams_table)
                newSlide = findViewById(R.id.leaderboard_teams_activity_table)
                oldButton = findViewById(R.id.team_leaderboard_raised_button)
                newButton = findViewById(R.id.team_leaderboard_activity_button)
            }else{
                oldSlide = findViewById(R.id.leaderboard_teams_activity_table)
                newSlide = findViewById(R.id.leaderboard_teams_table)
                oldButton = findViewById(R.id.team_leaderboard_activity_button)
                newButton = findViewById(R.id.team_leaderboard_raised_button)
            }
        }else if (slide == "company"){
            if(newtype == "activity"){
                oldSlide = findViewById(R.id.leaderboard_companies_table)
                newSlide = findViewById(R.id.leaderboard_companies_activity_table)
                oldButton = findViewById(R.id.company_leaderboard_raised_button)
                newButton = findViewById(R.id.company_leaderboard_activity_button)
            }else{
                oldSlide = findViewById(R.id.leaderboard_companies_activity_table)
                newSlide = findViewById(R.id.leaderboard_companies_table)
                oldButton = findViewById(R.id.company_leaderboard_activity_button)
                newButton = findViewById(R.id.company_leaderboard_raised_button)
            }
        }

        oldSlide.setVisibility(View.GONE)
        newSlide.setVisibility(View.VISIBLE)

        newButton.contentDescription = "${newButton.tag} Button Selected"
        oldButton.contentDescription = "${oldButton.tag} Button Not Selected"

        newButton.announceForAccessibility("${newButton.tag} Button Selected")

        setCustomToggleButtonColor(oldButton,"inactive")
        setCustomToggleButtonColor(newButton,"active")
    }

    fun joinChallenge(challenge: JSONObject){
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

    fun updateUserChallengeInfo(data: JSONObject, challenge: JSONObject){
        findViewById<TextView>(R.id.personal_progress_challenge_title).text = getSafeStringVariable(challenge, "title")

        var joined_challenge = getSafeBooleanVariable(data, "joined_challenge");

        if(joined_challenge) {
            findViewById<LinearLayout>(R.id.personal_progress_challenge_bars).visibility =
                View.VISIBLE
            findViewById<LinearLayout>(R.id.personal_progress_challenge_join_slide).visibility =
                View.GONE

            var challenge_type = getSafeStringVariable(challenge, "challenge_type")
            var challenge_status = getSafeStringVariable(challenge, "challenge_status")
            current_challenge_type = challenge_type;
            current_challenge_status = challenge_status;
            var challenge_my_progress = intWithCommas(getSafeIntegerVariable(data, "my_challenge_points"))
            var my_challenge_goal = getSafeIntegerVariable(data, "my_challenge_goal")
            var my_challenge_percent_to_goal =
                getSafeIntegerVariable(data, "my_challenge_percent_to_goal")


            var label = getString(R.string.mobile_overview_progress_challenge_points)
            if(challenge_status == "Live" && (challenge_type == "BOTH" || challenge_type == "ACTIVITY")){
                val challenge_metric = getSafeStringVariable(challenge, "activity_metric")
                if(challenge_metric == "DISTANCE"){
                    label = getDistanceLabel(R.string.mobile_overview_progress_challenge_miles, R.string.mobile_overview_progress_challenge_km)
                    challenge_my_progress = getSafeDoubleVariable(data, "my_challenge_distance").toString()
                    my_challenge_goal = getSafeIntegerVariable(data, "my_challenge_distance_goal")
                    my_challenge_percent_to_goal = getSafeIntegerVariable(data, "my_challenge_distance_percent_to_goal")
                }
            }

            if (my_challenge_percent_to_goal > 100) {
                my_challenge_percent_to_goal = 100
            }

            findViewById<LinearLayout>(R.id.progress_card_challenge_progress_bar).post{
                var color = getStringVariable("THERMOMETER_COLOR")
                if (color == "") {
                    color = getStringVariable("PRIMARY_COLOR")
                    if (color == "") {
                        color = String.format(
                            "#%06x",
                            ContextCompat.getColor(this, R.color.primary_color) and 0xffffff
                        )
                    }
                }

                color = color.replace(" ", "")

                findViewById<LinearLayout>(R.id.progress_card_challenge_progress_bar).background.setColorFilter(
                    Color.parseColor(color),
                    PorterDuff.Mode.SRC_ATOP
                )
            }

            findViewById<TextView>(R.id.progress_card_challenge_amount).setText(
                label + " " + challenge_my_progress
            )

            findViewById<TextView>(R.id.progress_card_challenge_percent).setText(
                intWithCommas(my_challenge_percent_to_goal) + "%"
            )

            findViewById<TextView>(R.id.progress_card_challenge_percent).contentDescription = intWithCommas(my_challenge_percent_to_goal) + "%" + getString(R.string.mobile_overview_progress_percent_progress)

            findViewById<TextView>(R.id.progress_card_challenge_goal).setText(
                getString(R.string.mobile_overview_progress_challenge_goal) + " " + intWithCommas(
                    my_challenge_goal
                )
            )

            personalChallengePercent = my_challenge_percent_to_goal
            resizeProgressBar((my_challenge_percent_to_goal.toDouble()/100),"Challenges")
        }else{
            findViewById<LinearLayout>(R.id.personal_progress_challenge_bars).visibility = View.GONE
            findViewById<LinearLayout>(R.id.personal_progress_challenge_join_slide).visibility = View.VISIBLE
        }
    }

    fun updateMenu(is_team: Boolean){
        var parents_corner_link = getStringVariable("PARENTS_CORNER_URL")
        val parents_corner_button = findViewById<LinearLayout>(R.id.menu_option_parents_corner);
        var educational_resources_link = getStringVariable("EDUCATIONAL_RESOURCES_URL");
        val educational_resources_button = findViewById<LinearLayout>(R.id.menu_option_educational_resources);
        val manage_schools_button = findViewById<LinearLayout>(R.id.menu_option_manage_school);
        val manage_schools_link = getStringVariable("MANAGE_SCHOOL_URL")
        val gifts_button = findViewById<LinearLayout>(R.id.menu_option_gifts);

        if(is_team){
            parents_corner_button.setVisibility(View.GONE)
            gifts_button.setVisibility(View.GONE)
            if(educational_resources_link != ""){
                educational_resources_button.setVisibility(View.VISIBLE)
            }else{
                educational_resources_button.setVisibility(View.GONE)
            }
            val manage_schools_url = getStringVariable("MANAGE_SCHOOL_URL")
            if(manage_schools_url != ""){
                manage_schools_button.setVisibility(View.VISIBLE)
                if(getStringVariable("MANAGE_SCHOOL_ENABLED") == "true"){
                    manage_schools_button.setOnClickListener {
                        val intent = Intent(this@Overview, ManageSchool::class.java);
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
        }else{
            educational_resources_button.setVisibility(View.GONE)
            if(parents_corner_link != ""){
                parents_corner_button.setVisibility(View.VISIBLE)
            }else{
                parents_corner_button.setVisibility(View.GONE)
            }
            manage_schools_button.setVisibility(View.GONE)

        }

    }

    fun loadAHCFinnsMissionCard(card: LinearLayout){
        val inflater = LayoutInflater.from(this@Overview)
        val background_img = findViewById<ImageView>(R.id.ahc_finns_mission_background);

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/getFinnsMission/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.code != 200) {
                    throw Exception(response.body?.string())
                } else {
                    val prefix = "https://nt-dev-clients.s3.amazonaws.com/ahayouthmarket/custom/FY25/AHC/FinnsMission/"
                    val jsonString = response.body?.string();
                    var jsonObj = JSONObject(jsonString);
                    if(jsonObj.has("data")){
                        val data = JSONObject(jsonObj.get("data").toString());
                        if(data.has("overall_mission_status")){
                            val status = data.get("overall_mission_status") as JSONObject

                            runOnUiThread {
                                Glide.with(this@Overview)
                                    .asBitmap()
                                    .load(prefix + "finns-background.png")
                                    .into(
                                        BitmapImageViewTarget(background_img)
                                    )

                                if(status.get("completed") == 1) {
                                    Glide.with(this@Overview)
                                        .asBitmap()
                                        .load(prefix + "badge-cape-earned.png")
                                        .into(
                                            BitmapImageViewTarget(findViewById(R.id.ahc_finns_mission_background_badge_8))
                                        )

                                    Glide.with(this@Overview)
                                        .asBitmap()
                                        .load("https://nt-dev-clients.s3.amazonaws.com/ahayouthmarket/custom/FY25/AHC/FinnsMission/badge-cape-active.png")
                                        .into(
                                            BitmapImageViewTarget((findViewById<LinearLayout>(R.id.ahc_finns_mission_completed_badge).getChildAt(0) as ImageView))
                                        )

                                    val completedBadge = findViewById<LinearLayout>(R.id.ahc_finns_mission_completed_badge)
                                    val completedImage = completedBadge.getChildAt(0) as ImageView
                                    val completedText = completedBadge.getChildAt(1) as TextView

                                    completedImage.contentDescription = getString(R.string.mobile_login_finns_mission_completed)
                                    completedBadge.contentDescription = "${completedImage.contentDescription} – ${completedText.text}"
                                } else {
                                    Glide.with(this@Overview)
                                        .asBitmap()
                                        .load(prefix + "badge-cape-unearned.png")
                                        .into(
                                            BitmapImageViewTarget(findViewById(R.id.ahc_finns_mission_background_badge_8))
                                        )

                                    Glide.with(this@Overview)
                                        .asBitmap()
                                        .load("https://nt-dev-clients.s3.amazonaws.com/ahayouthmarket/custom/FY25/AHC/FinnsMission/badge-cape-inactive.png")
                                        .into(
                                            BitmapImageViewTarget((findViewById<LinearLayout>(R.id.ahc_finns_mission_completed_badge).getChildAt(0) as ImageView))
                                        )

                                    val completedBadge = findViewById<LinearLayout>(R.id.ahc_finns_mission_completed_badge)
                                    val completedImage = completedBadge.getChildAt(0) as ImageView
                                    val completedText = completedBadge.getChildAt(1) as TextView

                                    completedImage.contentDescription = getString(R.string.mobile_login_finns_mission_not_completed)
                                    completedBadge.contentDescription = "${completedImage.contentDescription} – ${completedText.text}"
                                }
                            }
                        }

                        if(data.has("missions")){
                            var earned_badge_count = 0;
                            var ahc_hq_action_url = "";
                            val missions = data.get("missions") as JSONArray
                            for(i in 0..7){
                                runOnUiThread {
                                    if(missions.length() > i){
                                        val mission = missions[i] as JSONObject
                                        val earned = mission.get("earned") == 1
                                        if(earned){
                                            earned_badge_count = earned_badge_count + 1;
                                        }

                                        val mission_id = getSafeIntegerVariable(mission, "mission_id")

                                        var missionItem = findViewById<LinearLayout>(R.id.ahc_finns_mission_badge_1)
                                        var backgroundBadge = findViewById<ImageView>(R.id.ahc_finns_mission_background_badge_1)
                                        var badgeUrl = prefix + "badge-"

                                        if(i == 0) {
                                            missionItem = findViewById<LinearLayout>(R.id.ahc_finns_mission_badge_1)
                                            backgroundBadge = findViewById<ImageView>(R.id.ahc_finns_mission_background_badge_1)
                                            badgeUrl += "give"

                                            missionItem.setOnClickListener {
                                                sendGoogleAnalytics("ahc_finns_mission_make_a_donation","overview")
                                                val browserIntent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(getStringVariable("DONATIONS_URL"))
                                                )
                                                startActivity(browserIntent)
                                            }
                                        }else if (i == 1){
                                            missionItem = findViewById<LinearLayout>(R.id.ahc_finns_mission_badge_2)
                                            backgroundBadge = findViewById<ImageView>(R.id.ahc_finns_mission_background_badge_2)
                                            badgeUrl += "send"
                                            missionItem.setOnClickListener {
                                                sendGoogleAnalytics("ahc_finns_mission_send_a_message","overview")
                                                val intent =
                                                    Intent(this@Overview, Fundraise::class.java);
                                                startActivity(intent);
                                            }
                                        }else if (i == 2){
                                            missionItem = findViewById<LinearLayout>(R.id.ahc_finns_mission_badge_3)
                                            backgroundBadge = findViewById<ImageView>(R.id.ahc_finns_mission_background_badge_3)
                                            badgeUrl += "share"
                                            missionItem.setOnClickListener {
                                                sendGoogleAnalytics("ahc_finns_mission_share_on_social","overview")
                                                val intent =
                                                    Intent(this@Overview, Fundraise::class.java);
                                                startActivity(intent);
                                            }
                                        }else if (i == 3){
                                            missionItem = findViewById<LinearLayout>(R.id.ahc_finns_mission_badge_4)
                                            backgroundBadge = findViewById<ImageView>(R.id.ahc_finns_mission_background_badge_4)
                                            badgeUrl += "cpr"
                                            missionItem.setOnClickListener {
                                                sendGoogleAnalytics("ahc_finns_mission_hands_only_cpr","overview")
                                                if(!isLoadingUrl){
                                                    loadCprUrl();
                                                }
                                            }
                                        }else if (i == 4){
                                            missionItem = findViewById<LinearLayout>(R.id.ahc_finns_mission_badge_5)
                                            backgroundBadge = findViewById<ImageView>(R.id.ahc_finns_mission_background_badge_5)
                                            badgeUrl += "get"
                                            missionItem.setOnClickListener {
                                                sendGoogleAnalytics("ahc_finns_mission_ask_for_donations","overview")
                                                val intent =
                                                    Intent(this@Overview, Fundraise::class.java);
                                                startActivity(intent);
                                            }
                                        }else if (i == 5) {
                                            missionItem =
                                                findViewById<LinearLayout>(R.id.ahc_finns_mission_badge_6)
                                            backgroundBadge =
                                                findViewById<ImageView>(R.id.ahc_finns_mission_background_badge_6)

                                            ahc_hq_action_url =
                                                getSafeStringVariable(mission, "hq_action_url")

                                            if (ahc_hq_action_url != "mindfulness") {
                                                badgeUrl += "stroke"
                                                missionItem.setOnClickListener {
                                                    sendGoogleAnalytics(
                                                        "ahc_finns_mission_signs_of_stroke",
                                                        "overview"
                                                    )
                                                    val browserIntent = Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse(getStringVariable("ZURI_STROKE_QUIZ_URL"))
                                                    )
                                                    startActivity(browserIntent)
                                                }
                                            } else {
                                                badgeUrl += "mind"
                                                missionItem.setOnClickListener {
                                                    sendGoogleAnalytics(
                                                        "ahc_finns_mission_mindfulness",
                                                        "overview"
                                                    )
                                                    val browserIntent = Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse(getStringVariable("ZURI_MINDFULNESS_QUIZ_URL"))
                                                    )
                                                    startActivity(browserIntent)
                                                }
                                            }
                                        }
                                        else if (i == 6){
                                            missionItem = findViewById<LinearLayout>(R.id.ahc_finns_mission_badge_7)
                                            backgroundBadge = findViewById<ImageView>(R.id.ahc_finns_mission_background_badge_7)

                                            badgeUrl += "vape"
                                            missionItem.setOnClickListener {
                                                sendGoogleAnalytics("ahc_finns_mission_avoid_vaping_tobacco","overview")
                                                val browserIntent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(getStringVariable("ZURI_VAPING_QUIZ_URL"))
                                                )
                                                startActivity(browserIntent);
                                            }
                                        }

                                        val image_view = missionItem.getChildAt(0) as ImageView
                                        val text_view = missionItem.getChildAt(1) as TextView
                                        val name = mission.get("name") as String
                                        if(earned){
                                            badgeUrl += "-earned.png"
                                            image_view.contentDescription = name + " " + getString(R.string.mobile_overview_finns_mission_earned) + " " + getString(R.string.mobile_overview_finns_mission_badge)
                                        }else{
                                            badgeUrl += "-unearned.png"
                                            image_view.contentDescription = name + " " + getString(R.string.mobile_overview_finns_mission_unearned) + " " + getString(R.string.mobile_overview_finns_mission_badge)
                                        }
                                        text_view.contentDescription = name + " " + getString(R.string.mobile_overview_link_description)

                                        Glide.with(this@Overview).asBitmap().load(badgeUrl).into(
                                            BitmapImageViewTarget(backgroundBadge)
                                        )

                                        val image_url = (mission.get("url") as String).replace(".svg",".png")
                                        Glide.with(this@Overview).asBitmap().load(image_url).into(
                                            BitmapImageViewTarget(image_view)
                                        )

                                        val content = SpannableString(name)
                                        content.setSpan(UnderlineSpan(), 0, content.length, 0)
                                        (missionItem.getChildAt(1) as TextView).setText(content)
                                    }

                                    Glide.with(this@Overview)
                                        .asBitmap()
                                        .load(prefix + "progress-meter-" + earned_badge_count + ".png")
                                        .into(
                                            BitmapImageViewTarget((findViewById<ImageView>(R.id.ahc_finns_mission_progress_meter)))
                                        )
                                }
                            }
                        }

                        displayFinnsMatchingAlert(data)

                        runOnUiThread {
                            findViewById<LinearLayout>(R.id.overview_ahc_finns_card).setVisibility(View.VISIBLE)
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {}
            }
        })
    }


    fun loadKHCFinnsMissionCard(){
        val url_prefix = "https://nt-dev-clients.s3.amazonaws.com/ahayouthmarket/custom/FY25/KHC/FinnsMission/"
        val inflater = LayoutInflater.from(this@Overview)

        Glide.with(this@Overview).asBitmap().load(url_prefix + "finns-background.png").into(
            BitmapImageViewTarget(findViewById(R.id.finns_mission_background))
        )

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/getFinnsMission/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.code != 200) {
                    throw Exception(response.body?.string())
                } else {
                    val jsonString = response.body?.string();
                    var jsonObj = JSONObject(jsonString);
                    if(jsonObj.has("data")){
                        val data = JSONObject(jsonObj.get("data").toString());
                        if(data.has("missions")){
                            var completedBadgeCount = 0;
                            val missions = data.get("missions") as JSONArray
                            var hq_action_url = ""
                            var incomplete_missions = 0;
                            for(i in 0..missions.length() - 1){
                                runOnUiThread {
                                    val mission = missions[i] as JSONObject
                                    var mission_id = i;
                                    if(mission.has("mission_id") && mission.get("mission_id") is Int){
                                        mission_id = mission.get("mission_id") as Int
                                    }
                                    var imageView = findViewById<ImageView>(R.id.finns_mission_badge_1)
                                    var badgeImageUrl = "";
                                    if(mission_id == 35){
                                        imageView = findViewById(R.id.finns_mission_badge_1)
                                        badgeImageUrl = url_prefix + "badge-give"
                                    }else if (mission_id == 33){
                                        imageView = findViewById(R.id.finns_mission_badge_2)
                                        badgeImageUrl = url_prefix + "badge-edit"
                                    }else if (mission_id == 30){
                                        imageView = findViewById(R.id.finns_mission_badge_3)
                                        badgeImageUrl = url_prefix + "badge-send"
                                    }else if (mission_id == 31){
                                        imageView = findViewById(R.id.finns_mission_badge_4)
                                        badgeImageUrl = url_prefix + "badge-share"
                                    }else if (mission_id == 29){
                                        imageView = findViewById(R.id.finns_mission_badge_5)
                                        badgeImageUrl = url_prefix + "badge-cpr"
                                    }else if (mission_id == 34){
                                        imageView = findViewById(R.id.finns_mission_badge_6)
                                        badgeImageUrl = url_prefix + "badge-get"
                                    }else{
                                        imageView = findViewById(R.id.finns_mission_badge_7)
                                        if(mission.get("hq_action_url") is String){
                                            hq_action_url = mission.get("hq_action_url") as String
                                        }
                                        if(hq_action_url == "mindfulness"){
                                            badgeImageUrl = url_prefix + "badge-mind"
                                        }else{
                                            badgeImageUrl = url_prefix + "badge-stroke"
                                        }
                                    }

                                    val priority_order = getSafeIntegerVariable(mission, "priority_order")

                                    var earned = false;
                                    if(mission.get("earned") == 1){
                                        imageView.setVisibility(View.VISIBLE);
                                        badgeImageUrl += "-earned"
                                        earned = true;
                                        completedBadgeCount += 1;
                                    }else{
                                        badgeImageUrl += "-unearned"
                                        incomplete_missions += 1;
                                    }

                                    badgeImageUrl += "-" + priority_order + ".png"

                                    Glide.with(this@Overview)
                                        .load(badgeImageUrl)
                                        .into(imageView)

                                    var container = findViewById<LinearLayout>(R.id.finns_card_challenge_container_2)
                                    if(i < 4){
                                         container = findViewById(R.id.finns_card_challenge_container_1)
                                    }
                                    val binding: FinnsCardChallengeRowBinding = DataBindingUtil.inflate(
                                        inflater, R.layout.finns_card_challenge_row, container, true
                                    )
                                    binding.colorList = getColorList("")
                                    val row = binding.root as LinearLayout

                                    val image_url = (mission.get("url") as String).replace(".svg",".png")
                                    val image_view = row.getChildAt(0) as ImageView

                                    val name = mission.get("name") as String

                                    row.getChildAt(1).setOnClickListener {
                                        if (mission_id == 35) {
                                            sendGoogleAnalytics("khc_finns_mission_make_a_donation","overview")
                                            val browserIntent =
                                                Intent(Intent.ACTION_VIEW, Uri.parse(getStringVariable("DONATIONS_URL")))
                                            startActivity(browserIntent)
                                        } else if (mission_id == 30) {
                                            sendGoogleAnalytics("khc_finns_mission_send_a_message","overview")
                                            val intent =
                                                Intent(this@Overview, Fundraise::class.java);
                                            startActivity(intent);
                                        } else if (mission_id == 31) {
                                            sendGoogleAnalytics("khc_finns_mission_share_on_social","overview")
                                            val intent =
                                                Intent(this@Overview, Fundraise::class.java);
                                            startActivity(intent);
                                        } else if (mission_id == 33) {
                                            sendGoogleAnalytics("khc_finns_mission_edit_your_page","overview")
                                            val intent =
                                                Intent(this@Overview, ManagePage::class.java);
                                            startActivity(intent);
                                        } else if (mission_id == 29) {
                                            sendGoogleAnalytics("khc_finns_mission_hands_only_cpr","overview")
                                            if(!isLoadingUrl){
                                                loadCprUrl();
                                            }
                                        } else if (mission_id == 34) {
                                            sendGoogleAnalytics("khc_finns_mission_ask_for_donations","overview")
                                            val intent =
                                                Intent(this@Overview, Fundraise::class.java);
                                            startActivity(intent);
                                        } else {
                                            if (hq_action_url != "mindfulness") {
                                                sendGoogleAnalytics("khc_finns_mission_signs_of_stroke","overview")
                                                val browserIntent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(getStringVariable("ZURI_STROKE_QUIZ_URL"))
                                                )
                                                startActivity(browserIntent)
                                            } else {
                                                sendGoogleAnalytics("khc_finns_mission_mindfulness","overview")
                                                val browserIntent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(getStringVariable("ZURI_MINDFULNESS_QUIZ_URL"))
                                                )
                                                startActivity(browserIntent)
                                            }
                                        }
                                    }

                                    Glide.with(this@Overview).asBitmap().load(image_url).into(
                                        BitmapImageViewTarget(image_view)
                                    )

                                    if(earned){
                                        image_view.contentDescription = name + " " + getString(R.string.mobile_overview_finns_mission_earned) + " " + getString(R.string.mobile_overview_finns_mission_badge)
                                    }else{
                                        image_view.contentDescription = name + " " + getString(R.string.mobile_overview_finns_mission_unearned)  + " " + getString(R.string.mobile_overview_finns_mission_badge)
                                    }

                                    (row.getChildAt(1) as LinearLayout).contentDescription = name + " " + getString(R.string.mobile_overview_link_description)
                                    val content = SpannableString(name)
                                    content.setSpan(UnderlineSpan(), 0, content.length, 0)
                                    ((row.getChildAt(1) as LinearLayout).getChildAt(0) as TextView).setText(content)
                                }
                            }
                            runOnUiThread{

                                val meter_url = url_prefix + "progress-meter-" + completedBadgeCount + ".png"

                                Glide.with(this@Overview).asBitmap().load(meter_url).into(
                                    BitmapImageViewTarget(findViewById<ImageView>(R.id.finns_mission_meter))
                                )
                            }
                        }

                        displayFinnsMatchingAlert(data)

                        runOnUiThread {
                            findViewById<LinearLayout>(R.id.overview_khc_finns_card).setVisibility(View.VISIBLE)
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {}
            }
        })
    }

    fun loadCprUrl(){
        isLoadingUrl = true
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/urls/").plus(getConsID()).plus("/").plus(getEvent().event_id).plus("/android/").plus(getDeviceType())
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                isLoadingUrl = false
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    val jsonString = response.body?.string();
                    try{
                        val urls = JSONObject(jsonString)
                        if(urls.has("aha_cpr_video")){
                            val cpr_url = urls.get("aha_cpr_video") as String
                            val browserIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(cpr_url)
                            )
                            startActivity(browserIntent)
                        }
                    } catch(exception:IOException){
                        displayAlert(getResources().getString(R.string.mobile_error_unavailable));
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                isLoadingUrl = false
                println(e.message.toString())
                displayAlert(getResources().getString(R.string.mobile_login_no_events));
            }
        })
    }

    fun displayFinnsMatchingAlert(data: JSONObject){
        if(getStringVariable("INITIAL_LOGIN") == "true"){
            setVariable("INITIAL_LOGIN","false")
            if(data.has("company_match") && data.get("company_match") is Int){
                val match = data.get("company_match") as Int
                if(match == 1){
                    displayImageAlert(getString(R.string.mobile_overview_finns_mission_matching_alert_title),getString(R.string.mobile_overview_finns_mission_matching_alert_subtitle_alert),"https://nt-dev-clients.s3.amazonaws.com/ahayouthmarket/custom/FY24/Matching_Gift.png")
                }
            }
        }
    }

    fun loadLeaderboardData(leaderboardCard: View){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getPersonalLeaderboard/").plus(getEvent().event_id)
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
                    val json = JSONArray(jsonString);
                    var length = json.length()-1;
                    if(length > 9){
                        length = 9;
                    }
                    for(i in 0 .. length){
                        try {
                            var obj = json.getJSONObject(i);
                            leaderboard_participants += Participant(obj.get("name").toString(), obj.get("total").toString(), "");

                        } catch (exception: IOException) {
                            println("LOAD LEADERBOARD DATA ERROR")
                        }
                    }
                    runOnUiThread {
                        val leaderboard_table = findViewById<TableLayout>(R.id.leaderboard_table);
                        addRows(leaderboard_table, leaderboard_participants);

                        if(getStringVariable("HIDE_TEAM_LEADERBOARD") == "true"){
                            var leaderboardLayout = findViewById<FrameLayout>(R.id.overview_leaderboard_card);
                            leaderboardLayout.removeView(findViewById<LinearLayout>(R.id.team_leaderboard_container));
                        }

                        fadeInView(leaderboardCard);
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
            }
        })

        if(getStringVariable("HIDE_TEAM_LEADERBOARD") != "true"){
            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getTeamLeaderboard/").plus(getEvent().event_id)
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
                        if(jsonString.contains("[")){
                            val json = JSONArray(jsonString);
                            var length = json.length()-1;
                            if(length > 9){
                                length = 9;
                            }
                            for(i in 0 .. length){
                                try {
                                    var obj = json.getJSONObject(i);
                                    leaderboard_teams += Participant(obj.get("name").toString(), obj.get("total").toString(), "");
                                } catch (exception: IOException) {
                                    println("GET TEAM LEADERBOARD ERROR")
                                }
                            }
                        }else{
                            var obj = JSONObject(jsonString);
                            leaderboard_teams += Participant(obj.get("name").toString(), obj.get("total").toString(), "")
                        }


                        runOnUiThread {
                            val second_leaderboard_page =
                                findViewById<LinearLayout>(R.id.team_leaderboard_container);
                            if(second_leaderboard_page != null){
                                second_leaderboard_page.setVisibility(View.INVISIBLE);
                            }

                            val team_leaderboard_table =
                                findViewById<TableLayout>(R.id.leaderboard_teams_table);

                            addRows(team_leaderboard_table, leaderboard_teams);
                        }
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    println(e.message.toString());
                }
            })
        }

        if(hasCompany && getStringVariable("DISABLE_COMPANY_PROGRESS") != "true"){
            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getCompanyLeaderboard/").plus(getEvent().event_id)
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
                        if(jsonString.contains("[")){
                            val json = JSONArray(jsonString);
                            var length = json.length()-1;
                            if(length > 9){
                                length = 9;
                            }
                            for(i in 0 .. length){
                                try {
                                    var obj = json.getJSONObject(i);
                                    leaderboard_companies += Participant(obj.get("name").toString(), obj.get("total").toString(), "");
                                } catch (exception: IOException) {
                                    println("GET COMPANY LEADERBOARD ERROR")
                                }
                            }
                        }else{
                            var obj = JSONObject(jsonString);
                            leaderboard_companies += Participant(obj.get("name").toString(), obj.get("total").toString(), "")
                        }


                        runOnUiThread {
                            val third_leaderboard_page =
                                findViewById<LinearLayout>(R.id.company_leaderboard_container);
                            if(third_leaderboard_page != null){
                                third_leaderboard_page.setVisibility(View.INVISIBLE);
                            }

                            val company_leaderboard_table =
                                findViewById<TableLayout>(R.id.leaderboard_companies_table);

                            addRows(company_leaderboard_table, leaderboard_companies);
                        }
                    }
                }
                override fun onFailure(call: Call, e: IOException) {
                    println(e.message.toString());
                }
            })
        }

        getLeaderboardActivityStats()
        loadCustomLeaderboards(leaderboardCard)
    }

    fun loadCustomLeaderboards(leaderboardCard: View){
        var leaderboard_count = 1;

        runOnUiThread {
            findViewById<LinearLayout>(R.id.top_schools_leaderboard_container).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.finns_mission_leaderboard_container).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.team_leaderboard_container).setVisibility(View.GONE)
            setTooltipText(R.id.top_classrooms_leaderboard_help_button,R.string.mobile_overview_top_classroom_leaderboard_tooltip, R.string.mobile_overview_top_classroom_learderboard_title)
            setTooltipText(R.id.finns_mission_leaderboard_help_button,R.string.mobile_overview_finns_mission_leaderboard_tooltip, R.string.mobile_overview_finns_mission_leaderboard_title)
        }

        if(getStringVariable("HIDE_TEAM_LEADERBOARD") != "true"){
            leaderboard_count += 1;
        }

        if(hasCompany && getStringVariable("DISABLE_COMPANY_PROGRESS") != "true"){
            leaderboard_count += 1;
        }else{
            var leaderboardLayout = findViewById<FrameLayout>(R.id.overview_leaderboard_card);
            leaderboardLayout.removeView(findViewById<LinearLayout>(R.id.company_leaderboard_container));
        }

        if(getStringVariable("AHA_FINNS_MISSION_LEADERBOARD_ENABLED") == "true"){
            leaderboard_count += 1;
            loadFinnsMissionData()
        }else{
            runOnUiThread {
                var leaderboardLayout = findViewById<FrameLayout>(R.id.overview_leaderboard_card);
                leaderboardLayout.removeView(findViewById<LinearLayout>(R.id.finns_mission_leaderboard_container));
            }
        }

        if(getStringVariable("AHA_TOP_CLASSROOM_LEADERBOARD_ENABLED") == "true"){
            leaderboard_count += 1;
            loadTopClassroomsData()
            runOnUiThread {
                findViewById<LinearLayout>(R.id.top_schools_leaderboard_container).setVisibility(
                    View.VISIBLE
                )
                findViewById<LinearLayout>(R.id.top_schools_leaderboard_container).setVisibility(
                    View.INVISIBLE
                )
            }
        }else{
            runOnUiThread {
                var leaderboardLayout = findViewById<FrameLayout>(R.id.overview_leaderboard_card);
                leaderboardLayout.removeView(findViewById<LinearLayout>(R.id.top_schools_leaderboard_container));
            }
        }

        total_leaderboard_slides = leaderboard_count

        setupSlideButtons(total_leaderboard_slides,R.id.overview_leaderboard_slide_buttons_container,"leaderboard")
        runOnUiThread {
            if(total_leaderboard_slides > 1){
                leaderboardCard.setOnTouchListener(object :
                    OnSwipeTouchListener(this@Overview) {
                    override fun onSwipeLeft() {
                        super.onSwipeLeft();
                        switchLeaderboardSlide(current_leaderboard_slide + 1)
                    }

                    override fun onSwipeRight() {
                        super.onSwipeRight();
                        switchLeaderboardSlide(current_leaderboard_slide - 1)
                    }
                })
            }
            fadeInView(leaderboardCard)
        }
    }

    fun loadFinnsMissionData(){
        val inflater = LayoutInflater.from(this@Overview)

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/getFinnsMissionCompleted/").plus(getStringVariable("TEAM_ID")).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.code != 200) {
                    throw Exception(response.body?.string())
                    findViewById<LinearLayout>(R.id.finns_missions_view_all).setVisibility(View.GONE)
                } else {
                    val jsonString = response.body?.string();
                    var obj = JSONObject(jsonString);
                    val data = JSONObject(obj.get("data").toString()).get("completedFinnsMission").toString()
                    val jsonArray = JSONArray(data)
                    val table = findViewById<TableLayout>(R.id.leaderboard_finns_mission_table)

                    if(jsonArray.length() > 10){
                        findViewById<LinearLayout>(R.id.finns_missions_view_all).setOnClickListener{
                            displayAlert("viewAllFinnsMission")
                        }
                    }else{
                        findViewById<LinearLayout>(R.id.finns_missions_view_all).setVisibility(View.GONE)
                    }

                    if (jsonArray.length() > 0) {
                        leaderboard_students = listOf<FinnStudent>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray[i] as JSONObject
                            if(obj.has("firstName") && obj.get("firstName") is String){
                                var name = obj.get("firstName") as String
                                if(obj.has("lastName") && obj.get("lastName") is String) {
                                    name = name + " " + (obj.get("lastName") as String).substring(0, 1) + "."
                                }
                                leaderboard_students += FinnStudent(name)
                                if(i < 10){
                                    runOnUiThread {
                                        val binding: FinnsMissionRowBinding =
                                            DataBindingUtil.inflate(
                                                inflater,
                                                R.layout.finns_mission_row,
                                                table,
                                                true
                                            )
                                        binding.colorList = getColorList("")

                                        val row = binding.root as TableRow
                                        (row.getChildAt(0) as TextView).setText(name)
                                    }
                                }
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

    fun loadTopClassroomsData(){
        val inflater = LayoutInflater.from(this@Overview)
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/getTopClassrooms/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.code != 200) {
                    throw Exception(response.body?.string())
                } else {
                    val jsonString = response.body?.string();
                    var jsonObj = JSONObject(jsonString);
                    val data = JSONObject(jsonObj.get("data").toString());

                    if(data.has("display_leaderboard") && data.get("display_leaderboard") is Int){
                        if(data.get("display_leaderboard") == 1){
                            if (data.has("topClassrooms") && data.get("topClassrooms") is JSONArray){
                                val jsonArray = data.get("topClassrooms") as JSONArray
                                val table = findViewById<TableLayout>(R.id.top_classrooms_leaderboard_table)
                                if (jsonArray.length() > 0) {
                                    leaderboard_classrooms = listOf<TopClassroom>()
                                    for (i in 0 until jsonArray.length()) {
                                        val obj = jsonArray[i] as JSONObject
                                        var teacher = ""
                                        var grade = ""
                                        var raised = ""
                                        if(obj.has("teacher") && obj.get("teacher") is String) {
                                            teacher = obj.get("teacher") as String
                                        }

                                        if(obj.has("grade") && obj.get("grade") is String) {
                                            grade = obj.get("grade") as String
                                        }

                                        if(obj.has("raised")){
                                            if (obj.get("raised") is String) {
                                                raised = (obj.get("raised") as String)
                                            }else if (obj.get("raised") is Double) {
                                                raised = (obj.get("raised") as Double).toString()
                                            } else if (obj.get("raised") is String){
                                                raised = (obj.get("raised") as Int).toString()
                                            }
                                        }

                                        leaderboard_classrooms += TopClassroom(teacher,grade,raised)
                                        if(i < 10){
                                            runOnUiThread {
                                                val binding: TopClassroomsRowBinding =
                                                    DataBindingUtil.inflate(
                                                        inflater,
                                                        R.layout.top_classrooms_row,
                                                        table,
                                                        true
                                                    )
                                                binding.colorList = getColorList("")

                                                val row = binding.root as TableRow
                                                (row.getChildAt(0) as TextView).setText(teacher)
                                                (row.getChildAt(1) as TextView).setText(grade)
                                                (row.getChildAt(2) as TextView).setText(raised)
                                            }
                                        }
                                    }
                                }
                            }
                        }else{
                            runOnUiThread {
                                var leaderboardLayout =
                                    findViewById<FrameLayout>(R.id.overview_leaderboard_card);
                                leaderboardLayout.removeView(findViewById<LinearLayout>(R.id.top_schools_leaderboard_container));
                                total_leaderboard_slides -= 1
                                setupSlideButtons(total_leaderboard_slides,R.id.overview_leaderboard_slide_buttons_container,"leaderboard")
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

    fun loadModalRows(modalName: String){
        val inflater = layoutInflater
        if(modalName == "finnsMissionModal"){
            val table = findViewById<TableLayout>(R.id.finns_mission_modal_table)

            for (childView in table.children) {
                table.removeView(childView);
            }

            for (i in 0 until leaderboard_students.size) {
                val obj = leaderboard_students[i]
                runOnUiThread {
                    val binding: FinnsMissionRowBinding =
                        DataBindingUtil.inflate(
                            inflater,
                            R.layout.finns_mission_row,
                            table,
                            true
                        )
                    binding.colorList = getColorList("")

                    val row = binding.root as TableRow
                    (row.getChildAt(0) as TextView).setText(obj.student_name)
                }
            }
        }else{
            val table = findViewById<TableLayout>(R.id.top_classrooms_modal_table)

            for (childView in table.children) {
                table.removeView(childView);
            }

            for (i in 0 until leaderboard_classrooms.size) {
                val obj = leaderboard_classrooms[i]
                runOnUiThread {
                    val binding: TopClassroomsRowBinding =
                        DataBindingUtil.inflate(
                            inflater,
                            R.layout.top_classrooms_row,
                            table,
                            true
                        )
                    binding.colorList = getColorList("")

                    val row = binding.root as TableRow
                    (row.getChildAt(0) as TextView).setText(obj.teacher)
                    (row.getChildAt(1) as TextView).setText(obj.grade)
                    (row.getChildAt(2) as TextView).setText(obj.raised)
                }
            }
        }
    }

    fun loadPromiseGarden(slide:String){
        val container = findViewById<LinearLayout>(R.id.flowers_table_container);
        removeAllChildren(container)
        findViewById<Button>(R.id.plant_flower_promise_garden_btn).setOnClickListener {
            sendGoogleAnalytics("plant_flower","overview")
            displayAlert("plantFlower")
            setAlertSender(findViewById<Button>(R.id.plant_flower_promise_garden_btn))
        }

        findViewById<Button>(R.id.view_promise_garden_btn).setOnClickListener {
            sendGoogleAnalytics("go_to_promise_garden","overview")
            loadGardenUnity()
        }

        clearVariable("COLORS_ARRAY")
        findViewById<LinearLayout>(R.id.promise_garden_flowers_table).setVisibility(View.GONE)
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/userflowers/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                val jsonString = response.body?.string()
                val obj = JSONObject(jsonString)
                if(obj.has("data")){
                    try{
                        val data = obj.get("data").toString();
                        val jsonArray = JSONArray(data);

                        if(jsonArray.length() > 0) {
                            has_flowers = true;
                            val inflater = LayoutInflater.from(this@Overview)
                            val container = findViewById<LinearLayout>(R.id.flowers_table_container);
                            for (i in 0 until jsonArray.length()) {
                                runOnUiThread {
                                    val flower = jsonArray.getJSONObject(i)
                                    val row = inflater.inflate(
                                        R.layout.promise_garden_flower_row,
                                        container
                                    ) as LinearLayout;
                                    val tablerow = row.getChildAt(i) as TableRow
                                    ((tablerow.getChildAt(0) as LinearLayout).getChildAt(0) as TextView).text =
                                        flower.get("dedicated_to") as String

                                    val image_view =
                                        ((tablerow.getChildAt(1) as LinearLayout).getChildAt(0) as ImageView)
                                    val media = flower.get("flower_image") as String
                                    if (media !== null) {
                                        Glide.with(this@Overview)
                                            .load(media)
                                            .into(image_view)
                                        image_view.visibility = View.VISIBLE
                                    } else {
                                        image_view.visibility = View.INVISIBLE
                                    }

                                    (tablerow.getChildAt(2) as LinearLayout).setOnClickListener {
                                        runOnUiThread {
                                            sendGoogleAnalytics("edit_flower","overview")
                                            var id = "";
                                            if(flower.has("id")){
                                                id = (flower.get("id") as Int).toString();
                                            }
                                            displayAlert(
                                                "plantFlower",
                                                arrayOf(
                                                    id,
                                                    (flower.get("color") as String),
                                                    (flower.get("dedicated_to") as String),
                                                    (flower.get("message") as String)
                                                )
                                            )
                                            setAlertSender(tablerow.getChildAt(2))
                                        }
                                    }
                                }
                            }
                            runOnUiThread {
                                findViewById<TextView>(R.id.promise_garden_plant_flower_swipe_text).setVisibility(View.VISIBLE)
                                findViewById<LinearLayout>(R.id.promise_garden_flowers_table).setVisibility(View.VISIBLE)
                            }
                        }else{
                            has_flowers = false;
                            runOnUiThread {
                                findViewById<TextView>(R.id.promise_garden_plant_flower_swipe_text).setVisibility(View.GONE)
                                findViewById<LinearLayout>(R.id.promise_garden_flowers_table).setVisibility(View.GONE)
                            }
                        }

                        var length = 2;
                        if(has_flowers){
                            length = 3;
                        }

                        setupSlideButtons(length,R.id.overview_promise_garden_slide_buttons_container,"garden")

                        runOnUiThread {
                            if (!has_flowers && slide == "third") {
                                switchSlide("garden", "second")
                            } else {
                                switchSlide("garden", slide)
                            }
                        }
                    }catch(e: Exception){
                        has_flowers = false;
                        runOnUiThread {
                            findViewById<TextView>(R.id.promise_garden_plant_flower_swipe_text).setVisibility(View.GONE)
                            findViewById<LinearLayout>(R.id.promise_garden_flowers_table).setVisibility(View.GONE)
                        }
                    }
                }else{
                    has_flowers = false;
                    runOnUiThread {
                        findViewById<TextView>(R.id.promise_garden_plant_flower_swipe_text).setVisibility(View.GONE)
                        findViewById<LinearLayout>(R.id.promise_garden_flowers_table).setVisibility(View.GONE)
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
            }
        })

        val color_url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/arflowercolors/")
        var color_request = Request.Builder()
            .url(color_url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var color_client = OkHttpClient();

        color_client.newCall(color_request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                val jsonString = response.body?.string()
                val obj = JSONObject(jsonString)
                val data = obj.get("data").toString()
                val jsonArray = JSONArray(data)
                val colorArray = arrayListOf<String>()
                val colorStringArray = arrayListOf<String>()
                if(jsonArray.length() > 0){
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i);
                        colorArray.add(obj.get("hex_color") as String)
                        if(obj.has("description") && obj.get("description") is String){
                            colorStringArray.add(jsonArray.getJSONObject(i).get("description") as String)
                        }else{
                            colorStringArray.add("")
                        }
                    }
                    setVariable("FLOWER_COLORS_ARRAY",gson.toJson(colorArray))
                    setVariable("FLOWER_COLORS_TEXT_ARRAY",gson.toJson(colorStringArray))
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
            }
        })

        val gardenCard = findViewById<FrameLayout>(R.id.overview_promise_garden_card)
        val firstSlide = findViewById<LinearLayout>(R.id.promise_garden_view_container);
        val secondSlide = findViewById<LinearLayout>(R.id.promise_garden_plant_container);
        val thirdSlide = findViewById<LinearLayout>(R.id.promise_garden_flowers_container);
        firstSlide.setVisibility(View.INVISIBLE);
        secondSlide.setVisibility(View.INVISIBLE);
        thirdSlide.setVisibility(View.INVISIBLE);

        val media = "https://nt-dev-clients.s3.amazonaws.com/alzheimers/custom/Flower_Rotation.gif"
        val flower_image = findViewById<ImageView>(R.id.promise_garden_view_flower_image)
        if (media !== null) {
            Glide.with(this)
                .load(media)
                .into(flower_image)
            flower_image.visibility = View.VISIBLE
        } else {
            flower_image.visibility = View.INVISIBLE
        }

        val flower_plant_image = findViewById<ImageView>(R.id.promise_garden_plant_flower_image)
        if (media !== null) {
            Glide.with(this)
                .load(media)
                .into(flower_plant_image)
            flower_image.visibility = View.VISIBLE
        } else {
            flower_image.visibility = View.INVISIBLE
        }

        findViewById<ScrollView>(R.id.flowers_table_scroll_container).setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (v != null) {
                    v.getParent().requestDisallowInterceptTouchEvent(true)
                };
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        run {
                            yDistance = 0f
                            xDistance = yDistance
                        }
                        lastX = event.x
                        lastY = event.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val curX = event.x
                        val curY = event.y
                        xDistance += Math.abs(curX - lastX)
                        yDistance += Math.abs(curY - lastY)
                        lastX = curX
                        lastY = curY
                        if (xDistance > yDistance && (xDistance > 200 && yDistance < 100)) {
                            switchSlide("garden", "second");
                        }
                        return false
                    }
                }
                return false
            }
        })

        gardenCard.setOnTouchListener(object :
            OnSwipeTouchListener(this@Overview) {
            override fun onSwipeLeft() {
                super.onSwipeLeft();
                if(current_garden_slide == 1){
                    switchSlide("garden", "second");
                }else if(current_garden_slide == 2 && has_flowers){
                    switchSlide("garden", "third");
                }
            }

            override fun onSwipeRight() {
                super.onSwipeRight();

                if(current_garden_slide == 2){
                    switchSlide("garden", "first");
                }else if(current_garden_slide == 3){
                    switchSlide("garden", "second");
                }
            }
        })

        findViewById<LinearLayout>(R.id.flowers_table_scroll_view).setOnTouchListener(object :
            OnSwipeTouchListener(this@Overview) {
                override fun onSwipeRight() {
                    super.onSwipeRight();
                    switchSlide("garden", "second");
                }
            })
    }

    fun loadLuminaries(slide:String){
        val container = findViewById<LinearLayout>(R.id.luminaries_table_container);
        removeAllChildren(container)
        findViewById<Button>(R.id.create_luminary_btn).setOnClickListener {
            sendGoogleAnalytics("create_luminary","overview")
            displayAlert("createLuminary")
            setAlertSender(findViewById<Button>(R.id.create_luminary_btn))
        }

        findViewById<Button>(R.id.view_luminaries_btn).setOnClickListener {
            sendGoogleAnalytics("go_to_luminary","overview")
            loadLuminaryUnity()
        }

        findViewById<LinearLayout>(R.id.luminaries_table).setVisibility(View.GONE)
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/userLuminaries/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                val jsonString = response.body?.string()
                val obj = JSONObject(jsonString)
                if(obj.has("data")){
                    val data = obj.get("data").toString()
                    val jsonArray = JSONArray(data)
                    if(jsonArray.length() > 0) {
                        has_luminaries = true;
                        val inflater = LayoutInflater.from(this@Overview)
                        val container = findViewById<LinearLayout>(R.id.luminaries_table_container);
                        for (i in 0 until jsonArray.length()) {
                            runOnUiThread {
                                val luminary = jsonArray.getJSONObject(i)

                                val binding: LuminaryRowBinding = DataBindingUtil.inflate(
                                    inflater, R.layout.luminary_row, container, false
                                )
                                binding.colorList = getColorList("")

                                val luminaryDedication = binding.luminaryRowDedication
                                luminaryDedication.text = luminary.getString("dedicated_to")

                                val editLuminary = binding.luminaryRowEditLuminary

                                editLuminary.setOnClickListener {
                                    runOnUiThread {
                                        sendGoogleAnalytics("edit_luminary","overview")
                                        var id = "";
                                        if(luminary.has("id")){
                                            id = (luminary.get("id") as Int).toString();
                                        }
                                        displayAlert(
                                            "createLuminary",
                                            arrayOf(
                                                id,
                                                (luminary.get("dedicated_to") as String),
                                                (luminary.get("message") as String)
                                            )
                                        )
                                        setAlertSender(editLuminary)
                                    }
                                }
                                container.addView(binding.root)
                            }
                        }
                        runOnUiThread {
                            findViewById<TextView>(R.id.luminaries_create_luminary_swipe_text).setVisibility(View.VISIBLE)
                            findViewById<LinearLayout>(R.id.luminaries_table).setVisibility(View.VISIBLE)
                        }
                    }else{
                        has_luminaries = false;
                        runOnUiThread {
                            findViewById<TextView>(R.id.luminaries_create_luminary_swipe_text).setVisibility(View.GONE)
                            findViewById<LinearLayout>(R.id.luminaries_table).setVisibility(View.GONE)
                        }
                    }

                    var length = 2;
                    if(has_luminaries){
                        length = 3;
                    }

                    setupSlideButtons(length,R.id.overview_luminaries_slide_buttons_container,"luminaries")

                    runOnUiThread {
                        if (!has_luminaries && slide == "third") {
                            switchSlide("luminaries", "second")
                        } else {
                            switchSlide("luminaries", slide)
                        }
                    }
                }else{
                    has_luminaries = false;
                    runOnUiThread {
                        findViewById<TextView>(R.id.luminaries_create_luminary_swipe_text).setVisibility(View.GONE)
                        findViewById<LinearLayout>(R.id.luminaries_table).setVisibility(View.GONE)
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
            }
        })

        val mainCard = findViewById<FrameLayout>(R.id.overview_luminaries_card)
        val firstSlide = findViewById<LinearLayout>(R.id.luminaries_view_container);
        val secondSlide = findViewById<LinearLayout>(R.id.luminaries_create_container);
        val thirdSlide = findViewById<LinearLayout>(R.id.luminaries_list_container);
        firstSlide.setVisibility(View.INVISIBLE);
        secondSlide.setVisibility(View.INVISIBLE);
        thirdSlide.setVisibility(View.INVISIBLE);

        val media = "https://nt-dev-clients.s3.amazonaws.com/alzheimerscom/custom/luminary_front_lit.png"
        val main_image = findViewById<ImageView>(R.id.luminaries_main_image)
        if (media !== null) {
            Glide.with(this)
                .load(media)
                .into(main_image)
            main_image.visibility = View.VISIBLE
        } else {
            main_image.visibility = View.INVISIBLE
        }

        val secondary_image = findViewById<ImageView>(R.id.luminaries_create_image)
        if (media !== null) {
            Glide.with(this)
                .load(media)
                .into(secondary_image)
            secondary_image.visibility = View.VISIBLE
        } else {
            secondary_image.visibility = View.INVISIBLE
        }

        findViewById<ScrollView>(R.id.luminaries_table_scroll_container).setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (v != null) {
                    v.getParent().requestDisallowInterceptTouchEvent(true)
                };
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        run {
                            yDistance = 0f
                            xDistance = yDistance
                        }
                        lastX = event.x
                        lastY = event.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val curX = event.x
                        val curY = event.y
                        xDistance += Math.abs(curX - lastX)
                        yDistance += Math.abs(curY - lastY)
                        lastX = curX
                        lastY = curY
                        if (xDistance > yDistance && (xDistance > 200 && yDistance < 100)) {
                            switchSlide("luminaries", "second");
                        }
                        return false
                    }
                }
                return false
            }
        })

        mainCard.setOnTouchListener(object :
            OnSwipeTouchListener(this@Overview) {
            override fun onSwipeLeft() {
                super.onSwipeLeft();
                if(current_luminaries_slide == 1){
                    switchSlide("luminaries", "second");
                }else if(current_luminaries_slide == 2 && has_luminaries){
                    switchSlide("luminaries", "third");
                }
            }

            override fun onSwipeRight() {
                super.onSwipeRight();

                if(current_luminaries_slide == 2){
                    switchSlide("luminaries", "first");
                }else if(current_luminaries_slide == 3){
                    switchSlide("luminaries", "second");
                }
            }
        })

        findViewById<LinearLayout>(R.id.luminaries_table_scroll_view).setOnTouchListener(object :
            OnSwipeTouchListener(this@Overview) {
            override fun onSwipeRight() {
                super.onSwipeRight();
                switchSlide("luminaries", "second");
            }
        })
    }

    fun loadJerseys(slide:String){
        val container = findViewById<LinearLayout>(R.id.jerseys_table_container);
        removeAllChildren(container)
        findViewById<Button>(R.id.create_jersey_btn).setOnClickListener {
            sendGoogleAnalytics("create_jersey","overview")
            displayAlert("createJersey")
            setAlertSender(findViewById<Button>(R.id.create_jersey_btn))
        }

        findViewById<Button>(R.id.view_jerseys_btn).setOnClickListener {
            sendGoogleAnalytics("go_to_jersey", "overview")
            loadJerseyUnity(true) 
        }

        findViewById<LinearLayout>(R.id.jerseys_table).setVisibility(View.GONE)
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/userJerseys/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                val jsonString = response.body?.string()
                val obj = JSONObject(jsonString)
                if(obj.has("data")){
                    val data = obj.get("data").toString()
                    val jsonArray = JSONArray(data)
                    if(jsonArray.length() > 0) {
                        has_jerseys = true;
                        val inflater = LayoutInflater.from(this@Overview)
                        val container = findViewById<LinearLayout>(R.id.jerseys_table_container);
                        for (i in 0 until jsonArray.length()) {
                            runOnUiThread {
                                val jersey = jsonArray.getJSONObject(i)

                                val binding: JerseyRowBinding = DataBindingUtil.inflate(
                                    inflater, R.layout.jersey_row, container, false
                                )
                                binding.colorList = getColorList("")

                                val jerseyDedication = binding.jerseyRowDedication
                                jerseyDedication.text = jersey.getString("dedicated_to")

                                val editJersey = binding.jerseyRowEditJersey

                                editJersey.setOnClickListener {
                                    runOnUiThread {
                                        sendGoogleAnalytics("edit_jersey","overview")
                                        var id = "";
                                        if(jersey.has("id")){
                                            id = (jersey.get("id") as Int).toString();
                                        }
                                        displayAlert(
                                            "createJersey",
                                            arrayOf(
                                                id,
                                                (jersey.get("dedicated_to") as String),
                                                (jersey.get("message") as String)
                                            )
                                        )
                                        setAlertSender(editJersey)
                                    }
                                }
                                container.addView(binding.root)
                            }
                        }
                        runOnUiThread {
                            findViewById<TextView>(R.id.jerseys_create_jersey_swipe_text).setVisibility(View.VISIBLE)
                            findViewById<LinearLayout>(R.id.jerseys_table).setVisibility(View.VISIBLE)
                        }
                    }else{
                        has_jerseys = false;
                        runOnUiThread {
                            findViewById<TextView>(R.id.jerseys_create_jersey_swipe_text).setVisibility(View.GONE)
                            findViewById<LinearLayout>(R.id.jerseys_table).setVisibility(View.GONE)
                        }
                    }

                    var cutoff_double = 0.00
                    if(getStringVariable("ALZ_JERSEY_AR_CREATE_CUTOFF") != ""){
                        cutoff_double = getStringVariable("ALZ_JERSEY_AR_CREATE_CUTOFF").toDouble()
                    }

                    val raised_enough = personal_raised >= cutoff_double

                    var length = 2;

                    if(!raised_enough){
                        has_jerseys = false;
                    }

                    if(has_jerseys){
                        length = 3;
                    }

                    runOnUiThread{
                        if(getStringVariable("ALZ_JERSEY_AR_CREATE_CUTOFF") != ""){
                            var string_id = R.string.mobile_overview_jersey_create_jersey_description
                            if(!raised_enough){
                                string_id = R.string.mobile_overview_jersey_create_jersey_description_2
                                findViewById<Button>(R.id.create_jersey_btn).visibility = View.GONE

                                findViewById<Button>(R.id.view_jerseys_btn).setOnClickListener {
                                    
                                    loadJerseyUnity(false)
                                }
                            }else{
                                findViewById<Button>(R.id.view_jerseys_btn).setOnClickListener {
                                    
                                    loadJerseyUnity(true)
                                }
                            }
                            findViewById<TextView>(R.id.jerseys_create_jersey_text).setText(getString(string_id))
                        }else{
                            findViewById<TextView>(R.id.jerseys_create_jersey_text).setText(getString(R.string.mobile_overview_jersey_create_jersey_description_2))
                            findViewById<Button>(R.id.create_jersey_btn).visibility = View.GONE

                            findViewById<Button>(R.id.view_jerseys_btn).setOnClickListener {
                                
                                loadJerseyUnity(false)
                            }
                        }
                    }


                    setupSlideButtons(length,R.id.overview_jerseys_slide_buttons_container,"jerseys")

                    runOnUiThread {
                        if (!has_jerseys && slide == "third") {
                            switchSlide("jerseys", "second")
                        } else {
                            switchSlide("jerseys", slide)
                        }
                    }
                } else {
                    has_jerseys = false;
                    runOnUiThread {
                        findViewById<TextView>(R.id.jerseys_create_jersey_swipe_text).setVisibility(View.GONE)
                        findViewById<LinearLayout>(R.id.jerseys_table).setVisibility(View.GONE)
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
            }
        })

        val mainCard = findViewById<FrameLayout>(R.id.overview_jerseys_card)
        val firstSlide = findViewById<LinearLayout>(R.id.jerseys_view_container);
        val secondSlide = findViewById<LinearLayout>(R.id.jerseys_create_container);
        val thirdSlide = findViewById<LinearLayout>(R.id.jerseys_list_container);
        firstSlide.setVisibility(View.INVISIBLE);
        secondSlide.setVisibility(View.INVISIBLE);
        thirdSlide.setVisibility(View.INVISIBLE);

        val media = "https://nt-dev-clients.s3.amazonaws.com/alzheimerscom/custom/ar_jersey.png"
        val main_image = findViewById<ImageView>(R.id.jerseys_main_image)
        if (media !== null) {
            Glide.with(this)
                .load(media)
                .into(main_image)
            main_image.visibility = View.VISIBLE
        } else {
            main_image.visibility = View.INVISIBLE
        }

        val secondary_image = findViewById<ImageView>(R.id.jerseys_create_image)
        if (media !== null) {
            Glide.with(this)
                .load(media)
                .into(secondary_image)
            secondary_image.visibility = View.VISIBLE
        } else {
            secondary_image.visibility = View.INVISIBLE
        }

        findViewById<ScrollView>(R.id.jerseys_table_scroll_container).setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (v != null) {
                    v.getParent().requestDisallowInterceptTouchEvent(true)
                };
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        run {
                            yDistance = 0f
                            xDistance = yDistance
                        }
                        lastX = event.x
                        lastY = event.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val curX = event.x
                        val curY = event.y
                        xDistance += Math.abs(curX - lastX)
                        yDistance += Math.abs(curY - lastY)
                        lastX = curX
                        lastY = curY
                        if (xDistance > yDistance && (xDistance > 200 && yDistance < 100)) {
                            switchSlide("jerseys", "second");
                        }
                        return false
                    }
                }
                return false
            }
        })

        mainCard.setOnTouchListener(object :
            OnSwipeTouchListener(this@Overview) {
            override fun onSwipeLeft() {
                super.onSwipeLeft();
                if(current_jerseys_slide == 1){
                    switchSlide("jerseys", "second");
                }else if(current_jerseys_slide == 2 && has_jerseys){
                    switchSlide("jerseys", "third");
                }
            }

            override fun onSwipeRight() {
                super.onSwipeRight();

                if(current_jerseys_slide == 2){
                    switchSlide("jerseys", "first");
                }else if(current_jerseys_slide == 3){
                    switchSlide("jerseys", "second");
                }
            }
        })

        findViewById<LinearLayout>(R.id.jerseys_table_scroll_view).setOnTouchListener(object :
            OnSwipeTouchListener(this@Overview) {
                override fun onSwipeRight() {
                    super.onSwipeRight();
                    switchSlide("jerseys", "second");
                }
            })
    }

    fun loadPhotoFiltersCard(){
        findViewById<Button>(R.id.view_photo_filters_btn).setOnClickListener {
            sendGoogleAnalytics("go_to_filters","overview")
            loadPhotoFiltersUnity()
        }

        val media = "https://nt-dev-clients.s3.amazonaws.com/alzheimerscom/custom/purple_frame_caps_alpha.png"
        val flower_image = findViewById<ImageView>(R.id.photo_filters_card_image)
        if (media !== null) {
            Glide.with(this)
                .load(media)
                .into(flower_image)
            flower_image.visibility = View.VISIBLE
        } else {
            flower_image.visibility = View.INVISIBLE
        }
    }

    fun loadChallengeCard(){
        val card = findViewById<LinearLayout>(R.id.overview_challenges_card);
        val container = findViewById<FrameLayout>(R.id.overview_challenges_card_container);
        runOnUiThread {
            card.setVisibility(View.GONE)
        }
        removeAllPages(container)

        container.setOnTouchListener(object : OnSwipeTouchListener(this@Overview) {
            override fun onSwipeLeft() {
                super.onSwipeLeft();
                switchChallengeSlide(current_challenges_slide + 1);
            }
            override fun onSwipeRight() {
                super.onSwipeRight();
                switchChallengeSlide(current_challenges_slide - 1);
            }
        })

        val inflater = layoutInflater

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/challenges/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        var color_request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();
        client.newCall(color_request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                val jsonString = response.body?.string()
                val obj = JSONObject(jsonString)
                val data = JSONObject(obj.get("data").toString())
                if(data.has("week_number")) {
                    current_challenges_slide = data.get("week_number") as Int
                }

                if(data.get("event_enabled") == true){
                    val weeksArray = JSONArray(JSONObject(data.toString()).get("challenges").toString())
                    if(weeksArray.length() > 0){
                        total_challenge_slides = weeksArray.length()
                        setupSlideButtons(weeksArray.length(),R.id.challenge_slide_buttons,"overview_challenges")
                        for (i in 0 until weeksArray.length()) {
                            val week = weeksArray.getJSONObject(i);
                            runOnUiThread {
                                val challenge_page: LinearLayout = inflater.inflate(
                                    R.layout.overview_challenge_page,
                                    null
                                ) as LinearLayout
                                container.addView(challenge_page)
                                challenge_page.setVisibility(View.GONE)
                                if(i == current_challenges_slide){
                                    challenge_page.setVisibility(View.VISIBLE)
                                    findViewById<TextView>(R.id.challenges_week_title).text = getResources().getString(R.string.mobile_overview_autism_kindness_card_title).replace("#",(i + 1).toString())
                                    switchSlideButton(i+1,total_challenge_slides,R.id.challenge_slide_buttons)
                                }else{
                                    challenge_page.setVisibility(View.GONE)
                                }

                                if(week.has("challenges")){
                                    val challenges = week.get("challenges")
                                    if(challenges is JSONArray){
                                        for (j in 0 until challenges.length()) {
                                            val challenge = challenges.getJSONObject(j);
                                            var type = "Light It Up!"
                                            if(challenge.has("type") && challenge.get("type") is String){
                                                type = challenge.get("type") as String
                                            }
                                            val colorObj = getChallengeColor(type)

                                            runOnUiThread {
                                                val challenge_row: LinearLayout = inflater.inflate(
                                                    R.layout.overview_challenge_row,
                                                    null
                                                ) as LinearLayout

                                                challenge_row.setBackgroundColor(colorObj.background_color)
                                                challenge_row.getChildAt(0).setBackgroundColor(Color.parseColor(colorObj.text_color))

                                                val heart_image = (challenge_row.getChildAt(1) as FrameLayout).getChildAt(0) as ImageView
                                                val row_content = ((challenge_row.getChildAt(1) as FrameLayout).getChildAt(1) as LinearLayout).getChildAt(0) as LinearLayout;
                                                val image_content = ((challenge_row.getChildAt(1) as FrameLayout).getChildAt(1) as LinearLayout).getChildAt(1) as LinearLayout;
                                                (image_content.getChildAt(0) as ImageView).setColorFilter(colorObj.icon_color)
                                                (image_content.getChildAt(1) as ImageView).setColorFilter(colorObj.icon_color)
                                                heart_image.setColorFilter(colorObj.icon_color);

                                                if(challenge.get("challengeCompleted") == "true" || challenge.get("challengeCompleted") == true){
                                                    image_content.getChildAt(1).setVisibility(View.VISIBLE)
                                                    image_content.getChildAt(0).setVisibility(View.GONE)
                                                }else{
                                                    image_content.getChildAt(0).setVisibility(View.VISIBLE)
                                                    image_content.getChildAt(1).setVisibility(View.GONE)
                                                }

                                                if(challenge.has("challengeCanRevert")){
                                                    if(challenge.get("challengeCanRevert") == "true" || challenge.get("challengeCanRevert") == true){
                                                        (image_content.getChildAt(0) as ImageView).setOnClickListener{
                                                            setChallengeCompletionStatus(true, image_content, heart_image, challenge.get("name") as String)
                                                        }

                                                        (image_content.getChildAt(1) as ImageView).setOnClickListener{
                                                            setChallengeCompletionStatus(false, image_content, heart_image, challenge.get("name") as String)
                                                        }
                                                    }
                                                }
                                                (row_content.getChildAt(0) as TextView).setTextColor(Color.parseColor(colorObj.text_color))
                                                (row_content.getChildAt(0) as TextView).text = challenge.get("type") as String

                                                if(challenge.has("name")){
                                                    (row_content.getChildAt(1) as TextView).text = challenge.get("name") as String
                                                }

                                                row_content.getChildAt(2).setVisibility(View.GONE)
                                                row_content.getChildAt(3).setVisibility(View.GONE)

                                                if(challenge.has("showinfo") && (challenge.get("showinfo") == "true" || challenge.get("showinfo") == true)){
                                                    row_content.getChildAt(2).setVisibility(View.VISIBLE)
                                                    row_content.setOnClickListener{
                                                        if(row_content.getChildAt(3).visibility == View.GONE){
                                                            row_content.getChildAt(3).setVisibility(View.VISIBLE)
                                                        }else{
                                                            row_content.getChildAt(3).setVisibility(View.GONE)
                                                        }
                                                    }

                                                    val html =
                                                        challenge.get("info") as String
                                                    val result: Spanned = HtmlCompat.fromHtml(
                                                        html,
                                                        HtmlCompat.FROM_HTML_MODE_LEGACY
                                                    )

                                                    val additionalContent = (row_content.getChildAt(3) as TextView);
                                                    additionalContent.setText(result)
                                                    additionalContent.setMovementMethod(LinkMovementMethod.getInstance())
                                                }
                                                challenge_page.addView(challenge_row)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    runOnUiThread {
                        card.setVisibility(View.VISIBLE)
                    }
                }else{
                    runOnUiThread {
                        card.setVisibility(View.GONE)
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
            }
        })
    }

    private fun addRows(leaderboardTable: TableLayout, leaderboardParticipants: Array<Participant>) {
        for (i in 0..leaderboardParticipants.size - 1) {
            val inflater =
                applicationContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val row = inflater.inflate(R.layout.leaderboard_row, null) as TableRow
            (row.getChildAt(0) as TextView).setText((i + 1).toString().plus(". ").plus(leaderboardParticipants[i].name))
            (row.getChildAt(1) as TextView).setText(formatLocalizedCurrencyString(leaderboardParticipants[i].raised.replace("$","")))
            //(row.getChildAt(2) as TextView).setText(leaderboardParticipants[i].points)
            leaderboardTable.addView(row, i+2)
        }
    }

    private fun addActivityRows(leaderboardTable: TableLayout, leaderboardParticipants: Array<ActivityParticipant>) {
        for (i in 0..leaderboardParticipants.size - 1) {
            val inflater =
                applicationContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val row = inflater.inflate(R.layout.leaderboard_row, null) as TableRow
            (row.getChildAt(0) as TextView).setText((leaderboardParticipants[i].rank).plus(". ").plus(leaderboardParticipants[i].name))
            val tracking_type = getStringVariable("ACTIVITY_TRACKING_TYPE")
            if(tracking_type == "distance"){
                (row.getChildAt(1) as TextView).setText(leaderboardParticipants[i].miles)
            }else if (tracking_type == "minutes"){
                (row.getChildAt(1) as TextView).setText(leaderboardParticipants[i].minutes)
            }else{
                (row.getChildAt(1) as TextView).setText(leaderboardParticipants[i].points)
            }

            //(row.getChildAt(2) as TextView).setText(leaderboardParticipants[i].points)
            leaderboardTable.addView(row, i+2)
        }
    }

    private fun addChallengeRows(leaderboardTable: TableLayout, leaderboardParticipants: Array<challengeParticipant>) {
        removeAllRows(leaderboardTable,{
            for (i in 0..leaderboardParticipants.size - 1) {
                val inflater =
                    applicationContext.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val row = inflater.inflate(R.layout.leaderboard_row, null) as TableRow
                (row.getChildAt(0) as TextView).setText((leaderboardParticipants[i].rank) + " " + leaderboardParticipants[i].name)
                if(challenge_metric != "distance"){
                    (row.getChildAt(1) as TextView).setText(leaderboardParticipants[i].points.toString())
                }else {
                    var value = withCommas(leaderboardParticipants[i].raised);
                    value = value.replace(".00","");
                    if(value.contains('.') && value.last() == '0' && value != "0"){
                        value = value.substring(0, value.length - 1)
                    }
                    (row.getChildAt(1) as TextView).setText(value);
                }
                leaderboardTable.addView(row, i+2)
            }
        })
    }

    fun loadGardenUnity() {}
    fun loadLuminaryUnity() {}
    fun loadJerseyUnity(bool: Boolean) {}
    fun loadPhotoFiltersUnity() {}

/*
    fun loadGardenUnity() {
        //BEGIN_GARDEN_UNITY_CONTENT
        isUnityLoaded = true
        val string = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/arflowers/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        val intent = Intent(this, UnityPlayerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        intent.putExtra("garden_url",string);
        intent.putExtra("scene_type","garden");
        startArTracking("go_to_promise_garden")
        startActivityForResult(intent, UNITY_GARDEN_REQUEST)
        //END_GARDEN_UNITY_CONTENT
    }

    fun loadLuminaryUnity() {
        //BEGIN_LUMINARY_UNITY_CONTENT
        isUnityLoaded = true
        val string = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/arluminaries/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        val intent = Intent(this, UnityPlayerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        intent.putExtra("garden_url",string);
        intent.putExtra("scene_type","luminary");
        startArTracking("go_to_luminary")
        startActivityForResult(intent, UNITY_LUMINARY_REQUEST)
        //END_LUMINARY_UNITY_CONTENT
    }

    fun loadJerseyUnity(raised: Boolean) {
        //BEGIN_JERSEY_UNITY_CONTENT
        if(raised){
            isUnityLoaded = true
            val string = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/arjerseys/").plus(getConsID()).plus("/").plus(getEvent().event_id)
            val intent = Intent(this, UnityPlayerActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            intent.putExtra("garden_url",string);
            intent.putExtra("scene_type","jersey_create");
            startArTracking("go_to_jersey")
            startActivityForResult(intent, UNITY_JERSEY_REQUEST)
        }else{
            isUnityLoaded = true
            val string = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/arjerseys/").plus(getConsID()).plus("/").plus(getEvent().event_id)
            val intent = Intent(this, UnityPlayerActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            intent.putExtra("garden_url",string);
            intent.putExtra("scene_type","jersey");
            startArTracking("go_to_jersey")
            startActivityForResult(intent, UNITY_JERSEY_REQUEST)
        }
        //END_JERSEY_UNITY_CONTENT
    }
     fun loadPhotoFiltersUnity() {
        //BEGIN_PHOTO_FILTERS_UNITY_CONTENT
        isUnityLoaded = true
        val intent = Intent(this, UnityPlayerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        if(getStringVariable("ALZ_PROMISE_GARDEN_ENABLED") == "true" && getString(R.string.flower_garden_enabled) == "true"){
            intent.putExtra("scene_type","photo_filters_garden");
        }else{
            intent.putExtra("scene_type","photo_filters");
        }
        startArTracking("go_to_filters")
        startActivityForResult(intent, UNITY_PHOTO_FILTERS_REQUEST)
        //END_PHOTO_FILTERS_UNITY_CONTENT
    }
*/
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (intent != null) {
            val return_action = intent.getStringExtra("return_action").toString()

            if(requestCode == UNITY_GARDEN_REQUEST || requestCode == UNITY_LUMINARY_REQUEST || requestCode == UNITY_JERSEY_REQUEST || requestCode == UNITY_PHOTO_FILTERS_REQUEST){
                endArTracking()
            }

            if(requestCode == UNITY_GARDEN_REQUEST && return_action == "Add"){
                displayAlert("plantFlower")
            }

            if(requestCode == UNITY_LUMINARY_REQUEST && return_action == "Add"){
                displayAlert("createLuminary")
            }

            if(requestCode == UNITY_JERSEY_REQUEST && return_action == "Add"){
                displayAlert("createJersey")
            }
        }
    }

    fun loadSponsorData(nationalCard: LinearLayout, localCard: LinearLayout, additionalCard: LinearLayout){
        val inflater = layoutInflater
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/getSponsors/").plus(getEvent().event_id)
        var color_request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();
        client.newCall(color_request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                val jsonString = response.body?.string()
                val obj = JSONObject(jsonString)

                var nationalSponsors = JSONArray();
                if(obj.has("nationalSponsors")){
                    nationalSponsors = JSONArray(obj.get("nationalSponsors").toString())
                }
                var localSponsors = JSONArray();
                if(obj.has("localSponsors")){
                    localSponsors = JSONArray(obj.get("localSponsors").toString())
                }
                var additionalSponsors = JSONArray();
                if(obj.has("tier3Sponsors")){
                    additionalSponsors = JSONArray(obj.get("tier3Sponsors").toString())
                }

                if(nationalSponsors.length() > 0){
                    val national_sponsor_count = nationalSponsors.length()
                    var nationalSponsorImages = listOf<String>()

                    for (i in 0 until nationalSponsors.length()) {
                        val sponsor = nationalSponsors.getJSONObject(i)
                        if(sponsor.has("sponsor_url") && sponsor.get("sponsor_url") is String){
                            nationalSponsorImages += sponsor.get("sponsor_url") as String
                        }
                    }

                    var nationalChunks = nationalSponsorImages.chunked(3)
                    val national_sponsors_container = findViewById<LinearLayout>(R.id.overview_national_sponsors_slide)

                    runOnUiThread {
                        for (i in 0 until nationalChunks.size) {
                            val array = nationalChunks[i]
                            val row = (inflater.inflate(
                                R.layout.sponsors_row,
                                national_sponsors_container,
                                true
                            ) as LinearLayout).getChildAt(i) as LinearLayout

                            if(national_sponsor_count == 1){
                                row.weightSum = 1f
                            }else if(national_sponsor_count == 2){
                                row.weightSum = 2f
                            }

                            for (i in 0 until 3) {
                                if (i < array.size) {
                                    if(array[i] != ""){
                                        Glide.with(this@Overview).load(array[i]).into(row.getChildAt(i) as ImageView)
                                        (row.getChildAt(i) as ImageView).setVisibility(View.VISIBLE)
                                    }
                                } else {
                                    row.removeView(row.getChildAt(i))
                                }

                            }
                        }
                    }
                }
                else{
                    runOnUiThread {
                        nationalCard.setVisibility(View.GONE)
                    }
                }

                if(localSponsors.length() > 0){
                    var localSponsorImages = listOf<String>()
                    for (i in 0 until localSponsors.length()) {
                        val sponsor = localSponsors.getJSONObject(i)
                        if(sponsor.has("sponsor_url") && sponsor.get("sponsor_url") is String){
                            localSponsorImages += sponsor.get("sponsor_url") as String
                        }
                    }

                    var localChunks = localSponsorImages.chunked(3)
                    val local_sponsors_container = findViewById<LinearLayout>(R.id.overview_local_sponsors_slide)

                    runOnUiThread {
                        for (i in 0 until localChunks.size) {
                            val array = localChunks[i]
                            val row = (inflater.inflate(
                                R.layout.sponsors_row,
                                local_sponsors_container,
                                true
                            ) as LinearLayout).getChildAt(i) as LinearLayout

                            for (i in 0 until 3) {
                                if (i < array.size) {
                                    if(array[i] != ""){
                                        Glide.with(this@Overview).load(array[i]).into(row.getChildAt(i) as ImageView)
                                        (row.getChildAt(i) as ImageView).setVisibility(View.VISIBLE)
                                    }
                                } else {
                                    row.removeView(row.getChildAt(i))
                                }
                            }
                        }
                    }
                }
                else {
                    runOnUiThread {
                        localCard.setVisibility(View.GONE)
                    }
                }

                if(additionalSponsors.length() > 0){
                    var additionalSponsorImages = listOf<String>()
                    for (i in 0 until additionalSponsors.length()) {
                        val sponsor = additionalSponsors.getJSONObject(i)
                        if(sponsor.has("sponsor_url") && sponsor.get("sponsor_url") is String){
                            additionalSponsorImages += sponsor.get("sponsor_url") as String
                        }
                    }

                    var additionalChunks = additionalSponsorImages.chunked(3)
                    val additional_sponsors_container = findViewById<LinearLayout>(R.id.overview_additional_sponsors_slide)

                    runOnUiThread {
                        for (i in 0 until additionalChunks.size) {
                            val array = additionalChunks[i]
                            val row = (inflater.inflate(
                                R.layout.sponsors_row,
                                additional_sponsors_container,
                                true
                            ) as LinearLayout).getChildAt(i) as LinearLayout

                            for (i in 0 until 3) {
                                if (i < array.size) {
                                    if(array[i] != ""){
                                        Glide.with(this@Overview).load(array[i]).into(row.getChildAt(i) as ImageView)
                                        (row.getChildAt(i) as ImageView).setVisibility(View.VISIBLE)
                                    }
                                } else {
                                    row.removeView(row.getChildAt(i))
                                }
                            }
                        }
                    }
                }
                else {
                    runOnUiThread {
                        additionalCard.setVisibility(View.GONE)
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
            }
        })
    }

    private fun switchSlide(card:String,newSlide:String){
        if (card == "garden"){
            val firstSlide = findViewById<LinearLayout>(R.id.promise_garden_view_container);
            val secondSlide = findViewById<LinearLayout>(R.id.promise_garden_plant_container);
            val thirdSlide = findViewById<LinearLayout>(R.id.promise_garden_flowers_container);

            var length = 3;
            if(newSlide == "third" && !has_flowers){
                newSlide == "second";
                length = 2;
            }

            if(newSlide == "first"){
                current_garden_slide = 1;
                firstSlide.setVisibility(View.VISIBLE);
                secondSlide.setVisibility(View.INVISIBLE);
                thirdSlide.setVisibility(View.INVISIBLE);
            } else if (newSlide == "second"){
                current_garden_slide = 2;
                firstSlide.setVisibility(View.INVISIBLE);
                secondSlide.setVisibility(View.VISIBLE);
                thirdSlide.setVisibility(View.INVISIBLE);
            } else if (newSlide == "third"){
                current_garden_slide = 3;
                firstSlide.setVisibility(View.INVISIBLE);
                secondSlide.setVisibility(View.INVISIBLE);
                thirdSlide.setVisibility(View.VISIBLE);
            }

            switchSlideButton(current_garden_slide,length,R.id.overview_promise_garden_slide_buttons_container)
        }else if (card == "luminaries"){
            val firstSlide = findViewById<LinearLayout>(R.id.luminaries_view_container);
            val secondSlide = findViewById<LinearLayout>(R.id.luminaries_create_container);
            val thirdSlide = findViewById<LinearLayout>(R.id.luminaries_list_container);

            var updatedSlide = newSlide;

            var length = 3;

            if(updatedSlide == "third" && !has_luminaries){
                updatedSlide = "second";
                length = 2;
            }

            if(has_luminaries){
                setupSlideButtons(3,R.id.overview_luminaries_slide_buttons_container,"luminaries")
            }else{
                setupSlideButtons(2,R.id.overview_luminaries_slide_buttons_container,"luminaries")
            }

            if(updatedSlide == "first"){
                current_luminaries_slide = 1;
                firstSlide.setVisibility(View.VISIBLE);
                secondSlide.setVisibility(View.INVISIBLE);
                thirdSlide.setVisibility(View.INVISIBLE);
            } else if (updatedSlide == "second"){
                current_luminaries_slide = 2;
                firstSlide.setVisibility(View.INVISIBLE);
                secondSlide.setVisibility(View.VISIBLE);
                thirdSlide.setVisibility(View.INVISIBLE);
            } else if (updatedSlide == "third"){
                current_luminaries_slide = 3;
                firstSlide.setVisibility(View.INVISIBLE);
                secondSlide.setVisibility(View.INVISIBLE);
                thirdSlide.setVisibility(View.VISIBLE);
            }

            switchSlideButton(current_luminaries_slide, length, R.id.overview_luminaries_slide_buttons_container)
        }else if (card == "jerseys"){
            val firstSlide = findViewById<LinearLayout>(R.id.jerseys_view_container);
            val secondSlide = findViewById<LinearLayout>(R.id.jerseys_create_container);
            val thirdSlide = findViewById<LinearLayout>(R.id.jerseys_list_container);

            var updatedSlide = newSlide;

            var length = 3;

            if(updatedSlide == "third" && !has_jerseys){
                updatedSlide = "second";
                length = 2;
            }

            if(has_jerseys){
                setupSlideButtons(3,R.id.overview_jerseys_slide_buttons_container,"jerseys")
            }else{
                setupSlideButtons(2,R.id.overview_jerseys_slide_buttons_container,"jerseys")
            }

            if(updatedSlide == "first"){
                current_jerseys_slide = 1;
                firstSlide.setVisibility(View.VISIBLE);
                secondSlide.setVisibility(View.INVISIBLE);
                thirdSlide.setVisibility(View.INVISIBLE);
            } else if (updatedSlide == "second"){
                current_jerseys_slide = 2;
                firstSlide.setVisibility(View.INVISIBLE);
                secondSlide.setVisibility(View.VISIBLE);
                thirdSlide.setVisibility(View.INVISIBLE);
            } else if (updatedSlide == "third"){
                current_jerseys_slide = 3;
                firstSlide.setVisibility(View.INVISIBLE);
                secondSlide.setVisibility(View.INVISIBLE);
                thirdSlide.setVisibility(View.VISIBLE);
            }

            switchSlideButton(current_jerseys_slide, length, R.id.overview_jerseys_slide_buttons_container)
        }
    }

    fun switchProgressSlide(newIndex:Int){
        val firstSlide = findViewById<LinearLayout>(R.id.my_progress_container);
        val secondSlide = findViewById<LinearLayout>(R.id.my_progress_team_container);
        val thirdSlide = findViewById<LinearLayout>(R.id.my_progress_company_container);

        var slides = arrayOf(firstSlide,secondSlide)

        if(hasCompany && getStringVariable("DISABLE_COMPANY_PROGRESS") != "true"){
            if(hasTeam){
                slides = arrayOf(firstSlide, secondSlide, thirdSlide)
            }else{
                slides = arrayOf(firstSlide, thirdSlide)
            }
        }

        var progressLayout = findViewById<FrameLayout>(R.id.overview_progress_card);
        if((newIndex >= 0) and (newIndex < total_progress_slides)){
            switchSlideButton(newIndex + 1,total_progress_slides,R.id.overview_progress_slide_buttons_container)
            firstSlide.setVisibility(View.INVISIBLE);
            secondSlide.setVisibility(View.INVISIBLE);
            thirdSlide.setVisibility(View.INVISIBLE);

            progressLayout.getChildAt(current_progress_slide).setVisibility(View.INVISIBLE);
            slides[newIndex].setVisibility(View.VISIBLE);
            (((progressLayout.getChildAt(newIndex) as LinearLayout).getChildAt(0) as LinearLayout).getChildAt(0)).requestFocus()
            current_progress_slide = newIndex;
        }
    }

    fun switchLeaderboardSlide(newIndex:Int){
        switchSlideButton(newIndex + 1,total_leaderboard_slides,R.id.overview_leaderboard_slide_buttons_container)
        var leaderboardLayout = findViewById<FrameLayout>(R.id.overview_leaderboard_card);
        if((newIndex >= 0) and (newIndex < total_leaderboard_slides)){
            leaderboardLayout.getChildAt(current_leaderboard_slide).setVisibility(View.INVISIBLE);
            leaderboardLayout.getChildAt(newIndex).setVisibility(View.VISIBLE);
            (((leaderboardLayout.getChildAt(newIndex) as LinearLayout).getChildAt(0) as LinearLayout).getChildAt(0)).requestFocus()
            current_leaderboard_slide = newIndex;
        }
    }

    fun switchChallengeSlide(newIndex:Int){
        switchSlideButton(newIndex + 1,total_challenge_slides,R.id.challenge_slide_buttons)
        findViewById<TextView>(R.id.challenges_week_title).text = getResources().getString(R.string.mobile_overview_autism_kindness_card_title).replace("#",(newIndex + 1).toString())
        var challengeLayout = findViewById<FrameLayout>(R.id.overview_challenges_card_container);
        if((newIndex >= 0) and (newIndex < total_challenge_slides)){
            challengeLayout.getChildAt(current_challenges_slide).setVisibility(View.GONE);
            challengeLayout.getChildAt(newIndex).setVisibility(View.VISIBLE);
            current_challenges_slide = newIndex;
        }
    }

    fun getChallengeColor(type:String) : ChallengeColor{
        if(type == "Support Kind"){
            return ChallengeColor("#33d6e2",Color.rgb(153,234,240),Color.argb(51, 153,234,240))
        }else if (type == "Kindness Note"){
            return ChallengeColor("#ea7c15",Color.rgb(253,224,145),Color.argb(51,253,224,145))
        }else if (type == "Share Kind"){
            return ChallengeColor("#b96dfc",Color.rgb(220,182,253),Color.argb(51,220,182,253))
        }else if (type == "Be Kind"){
            return ChallengeColor("#f74f9c",Color.rgb(250,167,205),Color.argb(51,250,167,205))
        }else if (type == "Keep going!" || type == "Keep going"){
            return ChallengeColor("#2e86ef",Color.rgb(150,194,247),Color.argb(51,150,194,247))
        }else{
            return ChallengeColor("#2e86ef",Color.rgb(150,194,247),Color.argb(51,150,194,247))
        }
    }

    fun setChallengeCompletionStatus(status: Boolean, container: LinearLayout, heart: ImageView, name: String){
        if (status) {
            container.getChildAt(1).setVisibility(View.VISIBLE)
            container.getChildAt(0).setVisibility(View.GONE)
            val animation = TranslateAnimation(0F, 2000F, 0F, 0F)
            animation.setDuration(1500)
            animation.setFillAfter(false)
            heart.startAnimation(animation)
        } else {
            container.getChildAt(0).setVisibility(View.VISIBLE)
            container.getChildAt(1).setVisibility(View.GONE)
        }

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/updateChallenge")
        val client = OkHttpClient()
        val formBody = FormBody.Builder()
            .add("cons_id", getConsID())
            .add("event_id", getEvent().event_id)
            .add("name", name)
            .add("current_value", (!status).toString())
            .build()

        var request = Request.Builder().url(url)
            .post(formBody)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    if (!status) {
                        container.getChildAt(1).setVisibility(View.VISIBLE)
                        container.getChildAt(0).setVisibility(View.GONE)
                    } else {
                        container.getChildAt(0).setVisibility(View.VISIBLE)
                        container.getChildAt(1).setVisibility(View.GONE)
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread{
                    if (!status) {
                        container.getChildAt(1).setVisibility(View.VISIBLE)
                        container.getChildAt(0).setVisibility(View.GONE)
                    } else {
                        container.getChildAt(0).setVisibility(View.VISIBLE)
                        container.getChildAt(1).setVisibility(View.GONE)
                    }
                }
            }
        })
    }

    class ChallengeColor(
        val text_color: String,
        val icon_color: Int,
        val background_color: Int,
    )

    class Participant(
        val name: String,
        val raised: String,
        val points: String
    )

    class ActivityParticipant(
        val rank: String,
        val name: String,
        val raised: String,
        val points: String,
        val miles: String,
        val minutes: String
    )

    class challengeParticipant(
        val rank: String,
        val name: String,
        val points: Int,
        val raised: Double,
    )

    class TopClassroom(
        val teacher: String,
        val grade: String,
        val raised: String
    )

    class FinnStudent(
        val student_name: String
    )
}
