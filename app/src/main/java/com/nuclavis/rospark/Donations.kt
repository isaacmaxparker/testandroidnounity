package com.nuclavis.rospark

import android.content.*
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import app.com.kotlinapp.OnSwipeTouchListener
import com.nuclavis.rospark.databinding.AddCashDonationAlertBinding
import com.nuclavis.rospark.databinding.FundraisingMessageBinding
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*


class Donations : com.nuclavis.rospark.BaseActivity() {
    var totalMessagesSlideCount = 0;
    var currentMessagesSlideIndex = 0;
    var donors = emptyList<Donor>();
    var totalDonorSlideCount = 0;
    var currentDonorsSlideIndex = 0;
    var currentSortBy = 2;
    var currentSortDir = false;
    var matches = emptyList<Match>();
    var totalMatchSlideCount = 0;
    var currentMatchSlideIndex = 0;
    var currentMatchSortBy = 2;
    var currentMatchSortDir = false;
    var donation_messages = emptyList<DonationMessage>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        recolorTheme()
        super.onCreate(savedInstanceState)
        setPageContent(R.layout.donations,"donations")
        setTitle(getResources().getString(R.string.mobile_main_menu_donations));
        sendGoogleAnalytics("donations_view","donations")
        //Dummy Data
        findViewById<LinearLayout>(R.id.donations_card).setVisibility(View.GONE);
        loadDonorData()
        loadMessagesData()
        loadMatchingCard()

        findViewById<LinearLayout>(R.id.check_deposit_card).setVisibility(View.GONE);
        if(getStringVariable("CHECK_DEPOSIT_ENABLED") == "true"){
            if(getConsID() != getStringVariable("CHECK_DEPOSIT_DISABLED_CONS_ID")){
                findViewById<LinearLayout>(R.id.check_deposit_card).setVisibility(View.VISIBLE);
            }
        }

        val captain_text = findViewById<TextView>(R.id.team_check_deposit_text)

        if(getStringVariable("IS_TEAM_CAPTAIN") == "true"){
            captain_text.setVisibility(View.VISIBLE)
        }else{
            captain_text.setVisibility(View.GONE)
        }

        val check_deposit_button = findViewById<Button>(R.id.btn_check_deposit);
        check_deposit_button.setOnClickListener {
            if(getStringVariable("CHECK_DEPOSIT_MAINTENANCE_ENABLED") == "true"){
                displayAlert(getString(R.string.mobile_donations_deposit_check_maintenance_prompt))
            }else{
                val intent = Intent(this@Donations, CheckDeposit::class.java);
                intent.putExtra("event_id","")
                intent.putExtra("event_name","")
                intent.putExtra("check_credit","")
                startActivity(intent);
                this.overridePendingTransition(0, 0);
            }
        }

        setTooltipText(R.id.donate_help_button,R.string.mobile_donations_no_donors_tooltip, R.string.mobile_donations_no_donors_title)
        setTooltipText(R.id.check_deposit_help_button,R.string.mobile_donations_credit_tooltip, R.string.mobile_donations_credit_title)
        setTooltipText(R.id.donors_help_button,R.string.mobile_donations_donor_tooltip, R.string.mobile_donations_donor_title)
        setTooltipText(R.id.messages_help_button,R.string.mobile_donations_thank_donors_tooltip, R.string.mobile_donations_thank_donors_title)

        var donationsMessageLayout = findViewById<LinearLayout>(R.id.donation_messages_card);
        donationsMessageLayout.setOnTouchListener(object : OnSwipeTouchListener(this@Donations) {
            override fun onSwipeLeft() {
                super.onSwipeLeft();
                switchMessageSlide(currentMessagesSlideIndex + 1);
            }
            override fun onSwipeRight() {
                super.onSwipeRight();
                switchMessageSlide(currentMessagesSlideIndex - 1);
            }
        })

        val donorTable = findViewById<FrameLayout>(R.id.donors_table_container);
        donorTable.setOnTouchListener(object : OnSwipeTouchListener(this@Donations) {
            override fun onSwipeLeft() {
                super.onSwipeLeft();
                switchDonorsSlide(currentDonorsSlideIndex + 1);
            }
            override fun onSwipeRight() {
                super.onSwipeRight();
                switchDonorsSlide(currentDonorsSlideIndex - 1);
            }
        })

        val matchTable = findViewById<FrameLayout>(R.id.matches_table_container);
        matchTable.setOnTouchListener(object : OnSwipeTouchListener(this@Donations) {
            override fun onSwipeLeft() {
                super.onSwipeLeft();
                switchMatchingSlide(currentMatchSlideIndex + 1);
            }
            override fun onSwipeRight() {
                super.onSwipeRight();
                switchMatchingSlide(currentMatchSlideIndex - 1);
            }
        })

        val donors_table_name_sort_button = findViewById<LinearLayout>(R.id.donors_table_name_sort_link);
        donors_table_name_sort_button.setOnClickListener {
            if(currentSortBy == 0){
                currentSortDir = !currentSortDir
            }else{
                currentSortBy = 0;
                currentSortDir = false;
            }
            updateSortArrows()
        }

        val donors_table_amount_sort_button = findViewById<LinearLayout>(R.id.donors_table_amount_sort_link);
        donors_table_amount_sort_button.setOnClickListener {
            if(currentSortBy == 1){
                currentSortDir = !currentSortDir
            }else{
                currentSortBy = 1;
                currentSortDir = false;
            }
            updateSortArrows()
        }

        val donors_table_date_sort_button = findViewById<LinearLayout>(R.id.donors_table_date_sort_link);
        donors_table_date_sort_button.setOnClickListener {
            if(currentSortBy == 2){
                currentSortDir = !currentSortDir
            }else{
                currentSortBy = 2;
                currentSortDir = false;
            }
            updateSortArrows()
        }

        val donate_button = findViewById<Button>(R.id.btn_donate);
        donate_button.setOnClickListener {
            try{
                val browserIntent =
                    Intent(Intent.ACTION_VIEW, Uri.parse(getStringVariable("DONATIONS_URL")))
                    startActivity(browserIntent)
            }catch(e: Exception){
                displayAlert(getString(R.string.mobile_url_opening_error))
            }
        }

        val facebookShareButton = findViewById<FrameLayout>(R.id.facebook_share_button)
        facebookShareButton.setOnClickListener {
            sendGoogleAnalytics("donations_facebook_share","donations")
            sendSocialActivity("facebook")
            if(donation_messages[currentMessagesSlideIndex].custom_content){
                shareFacebook(this,donation_messages[currentMessagesSlideIndex].url)
                setAlertSender(facebookShareButton)
            }else{
                displayAlert(
                    resources.getString(R.string.mobile_fundraise_message_facebook_prompt), ""
                ) { childviewCallback("facebook","") }
                setAlertSender(facebookShareButton)
            }

        }

        val emailShareButton = findViewById<FrameLayout>(R.id.email_share_button)
        emailShareButton.setOnClickListener {
            sendGoogleAnalytics("donations_email_share","donations")
            sendSocialActivity("email")
            try{
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:") // only email apps should handle this
                intent.putExtra(Intent.EXTRA_SUBJECT, donation_messages[currentMessagesSlideIndex].subject)
                if(donation_messages[currentMessagesSlideIndex].custom_content){
                    intent.putExtra(Intent.EXTRA_TEXT, (donation_messages[currentMessagesSlideIndex].email_url))
                }else {
                    intent.putExtra(Intent.EXTRA_TEXT,
                        (donation_messages[currentMessagesSlideIndex].email_body).replace("<br>","\r\n").plus("\r\n\r\n")
                            .plus(donation_messages[currentMessagesSlideIndex].email_url)
                    )
                }
                startActivity(Intent.createChooser(intent,getResources().getString(R.string.mobile_fundraise_share_dialog_title)))
                setAlertSender(emailShareButton)
            }catch(exception: IOException){
                displayAlert(getResources().getString(R.string.mobile_fundraise_share_dialog_error))
                setAlertSender(emailShareButton)
            }
        }

        val smsShareButton = findViewById<FrameLayout>(R.id.sms_share_button)
        smsShareButton.setOnClickListener {
            sendGoogleAnalytics("donations_sms_share","donations")
            sendSocialActivity("sms")
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("smsto:") // only email apps should handle this
            //intent.putExtra(Intent.EXTRA_SUBJECT, fundraisingMessages[currentSlideIndex].subject)
            if(donation_messages[currentMessagesSlideIndex].custom_content){
                intent.putExtra(Intent.EXTRA_TEXT, (donation_messages[currentMessagesSlideIndex].sms_url))
            }else {
                intent.putExtra(Intent.EXTRA_TEXT,
                    donation_messages[currentMessagesSlideIndex].text.plus(" ")
                        .plus(donation_messages[currentMessagesSlideIndex].sms_url)
                )
            }
            startActivity(Intent.createChooser(intent,getResources().getString(R.string.mobile_fundraise_share_dialog_title)))
        }

        val linkedinShareButton = findViewById<FrameLayout>(R.id.linkedin_share_button)
        linkedinShareButton.setOnClickListener {
            sendGoogleAnalytics("donations_linkedin_share","donations")
            sendSocialActivity("linkedin")
            if(donation_messages[currentMessagesSlideIndex].custom_content){
                shareLinkedIn(donation_messages[currentMessagesSlideIndex].linkedin_url )
            }else {
                displayAlert(
                    resources.getString(R.string.mobile_fundraise_message_linkedin_prompt), ""
                ) { childviewCallback("linkedin","") }
            }
        }

        if(getStringVariable("LINKEDIN_DISABLED") == "true"){
            linkedinShareButton.visibility = View.GONE;
        }else {
            linkedinShareButton.visibility = View.VISIBLE;
        }

        checkCashDonations()
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
        }else if (card == "donors"){
            var currentIndex = currentDonorsSlideIndex;
            if(forward){
                currentIndex += 1;
            }else{
                currentIndex -= 1;
            }
            switchDonorsSlide(currentIndex)
        }else if (card == "matches"){
            var currentIndex = currentMatchSlideIndex;
            if(forward){
                currentIndex += 1;
            }else{
                currentIndex -= 1;
            }
            switchMatchingSlide(currentIndex)
        }
    }

    override fun childviewCallback(string: String, data:String){
        hideAlert();
        if(string == "linkedin"){
            shareLinkedIn(Uri.encode(donation_messages[currentMessagesSlideIndex].linkedin_url))
        }else if(string == "facebook") {
            shareFacebook(this@Donations, donation_messages[currentMessagesSlideIndex].facebook_url)
        }else if (string == "matchSuccess"){
            displayAlert(getString(R.string.mobile_donations_double_donation_match_gift_success))
            loadMatchingCard()
        }else if (string == "matchError"){
            displayAlert(getString(R.string.mobile_donations_double_donation_match_gift_failure))
            loadMatchingCard()
        }else{
            val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("fundraising message",donation_messages[currentMessagesSlideIndex].text)
            clipboard.setPrimaryClip(clip)
        }
    }

    fun checkCashDonations(){
        if(getStringVariable("CASH_DONATIONS_ENABLED") == "true"){
            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/donationInformation/").plus(getConsID()).plus("/").plus(getEvent().event_id)
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
                        val obj = JSONObject(jsonString)
                        if(obj.has("data")){
                            if(getSafeBooleanVariable((obj.get("data") as JSONObject), "cash_donations")){
                                runOnUiThread{
                                    findViewById<LinearLayout>(R.id.btn_cash_donation_container).visibility = View.VISIBLE
                                    findViewById<Button>(R.id.btn_donate).setText(getString(R.string.mobile_donations_cash_donations_credit_card))
                                    findViewById<TextView>(R.id.donate_description).setText(getString(R.string.mobile_donations_cash_donations_description))
                                    setTooltipText(R.id.donate_help_button, R.string.mobile_donations_cash_donations_tooltip, R.string.mobile_donations_no_donors_title)

                                    findViewById<Button>(R.id.btn_cash_donation).setOnClickListener{
                                        showAddCashAlert()
                                    }
                                }
                            }else{
                                runOnUiThread {
                                    findViewById<LinearLayout>(R.id.btn_cash_donation_container).visibility =
                                        View.GONE
                                }
                            }
                        }else{
                            runOnUiThread {
                                findViewById<LinearLayout>(R.id.btn_cash_donation_container).visibility =
                                    View.GONE
                            }
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println(e.message.toString())
                }
            })
        }else{
            findViewById<LinearLayout>(R.id.btn_cash_donation_container).visibility = View.GONE
        }
    }

    fun shareLinkedIn(url: String){
        val url = "https://www.linkedin.com/shareArticle?mini=true&url=".plus(url)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    fun loadMatchingCard(){
        if(getStringVariable("DOUBLE_DONATION_ENABLED") == "true"){
            val match_table_name_sort_button = findViewById<LinearLayout>(R.id.matching_table_name_sort_link);
            match_table_name_sort_button.setOnClickListener {
                if(currentMatchSortBy == 0){
                    currentMatchSortDir = !currentMatchSortDir
                }else{
                    currentMatchSortBy = 0;
                    currentMatchSortDir = false;
                }
                updateMatchSortArrows()
            }

            val match_table_amount_sort_button = findViewById<LinearLayout>(R.id.matching_table_amount_sort_link);
            match_table_amount_sort_button.setOnClickListener {
                if(currentMatchSortBy == 1){
                    currentMatchSortDir = !currentMatchSortDir
                }else{
                    currentMatchSortBy = 1;
                    currentMatchSortDir = false;
                }
                updateMatchSortArrows()
            }

            val match_table_date_sort_button = findViewById<LinearLayout>(R.id.matching_table_date_sort_link);
            match_table_date_sort_button.setOnClickListener {
                if(currentMatchSortBy == 2){
                    currentMatchSortDir = !currentMatchSortDir
                }else{
                    currentMatchSortBy = 2;
                    currentMatchSortDir = false;
                }
                updateMatchSortArrows()
            }

            setTooltipText(R.id.matching_help_button, R.string.mobile_donations_double_donation_tooltip, R.string.mobile_donations_double_donation_title)

            findViewById<TextView>(R.id.btn_request_match).setOnClickListener{
                displayAlert("findMatch")
            }

            val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/check/doubledonation/donations/").plus(getConsID()).plus("/").plus(getEvent().event_id)
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
                        val obj = JSONObject(jsonString)
                        val matchedJsonArray = obj.get("matched_donations") as JSONArray;
                        val unmatchedJsonArray = obj.get("unmatched_donations") as JSONArray;
                        var unmatchedArray = emptyList<Match>()
                        var matchedArray = emptyList<Match>()

                        var has_matched = false;
                        var has_unmatched = false;

                        for (i in 0 until unmatchedJsonArray.length()) {
                            has_unmatched = true;
                            val obj = unmatchedJsonArray.getJSONObject(i);
                            val first_name = getSafeStringVariable(obj, "donor_first_name");
                            val last_name = getSafeStringVariable(obj, "donor_last_name");
                            val email = getSafeStringVariable(obj, "donor_email")
                            val amount = getSafeDoubleVariable(obj, "donation_amount")
                            val status = getSafeBooleanVariable(obj,"complete")
                            var status_string = "In Process";
                            val vendor_donation_id = getSafeStringVariable(obj, "vendor_donation_id")
                            val doublethedonation_donation_id = getSafeStringVariable(obj, "doublethedonation_donation_id")

                            var dateFormat = SimpleDateFormat("MM/dd/yyyy");
                            var d = dateFormat.parse(getSafeStringVariable(obj, "donation_date"));

                            if(status){
                                status_string = "Confirmed";
                            }

                            val match = Match(
                                first_name,
                                last_name,
                                amount,
                                d,
                                email,
                                status,
                                status_string,
                                doublethedonation_donation_id,
                                vendor_donation_id,
                            )
                            unmatchedArray += match;
                        }
                        nonmatched = unmatchedArray;

                        if(matchedJsonArray.length() > 0){
                            has_matched = true;
                            for (i in 0 until matchedJsonArray.length()) {
                                val obj = matchedJsonArray.getJSONObject(i);
                                val first_name = getSafeStringVariable(obj, "donor_first_name");
                                val last_name = getSafeStringVariable(obj, "donor_last_name");
                                val email = getSafeStringVariable(obj, "donor_email")
                                val amount = getSafeDoubleVariable(obj, "donation_amount")
                                val status = getSafeBooleanVariable(obj,"complete")
                                var status_string = "In Process";
                                val ineligible = getSafeBooleanVariable(obj, "ineligible")
                                val vendor_donation_id = getSafeStringVariable(obj, "vendor_donation_id")
                                val doublethedonation_donation_id = getSafeStringVariable(obj, "doublethedonation_donation_id")

                                var dateFormat = SimpleDateFormat("MM/dd/yyyy");
                                var d = dateFormat.parse(getSafeStringVariable(obj, "donation_date"));

                                if(status){
                                    status_string = "Confirmed";
                                }

                                if(ineligible){
                                    status_string = "Not Eligible"
                                }

                                val match = Match(
                                    first_name,
                                    last_name,
                                    amount,
                                    d,
                                    email,
                                    status,
                                    status_string,
                                    doublethedonation_donation_id,
                                    vendor_donation_id,
                                    )
                                matchedArray += match;
                            }
                            matches = matchedArray;

                            runOnUiThread{
                                if(matches.size > 0){
                                    fadeInView(findViewById<LinearLayout>(R.id.matching_card))
                                }
                                updateMatchSortArrows()

                                if(has_unmatched){
                                    findViewById<LinearLayout>(R.id.btn_request_match_container).visibility = View.VISIBLE
                                }else{
                                    findViewById<LinearLayout>(R.id.btn_request_match_container).visibility = View.GONE
                                }

                                if(has_matched && !has_unmatched){
                                    findViewById<LinearLayout>(R.id.matching_text_both).visibility = View.GONE
                                    findViewById<LinearLayout>(R.id.matching_text_matched).visibility = View.VISIBLE
                                }else if (!has_matched && has_unmatched){
                                    findViewById<LinearLayout>(R.id.matching_text_both).visibility = View.GONE
                                    findViewById<LinearLayout>(R.id.matching_text_unmatched).visibility = View.VISIBLE
                                } else {
                                    findViewById<LinearLayout>(R.id.matching_text_both).visibility = View.VISIBLE
                                    findViewById<LinearLayout>(R.id.matching_text_unmatched).visibility = View.GONE
                                    findViewById<LinearLayout>(R.id.matching_text_matched).visibility = View.GONE
                                }
                            }
                        }else{
                            runOnUiThread{
                                findViewById<LinearLayout>(R.id.matching_table).visibility = View.GONE;
                                if(has_unmatched){
                                    findViewById<LinearLayout>(R.id.matching_text_both).visibility = View.VISIBLE
                                    findViewById<LinearLayout>(R.id.matching_text_unmatched).visibility = View.GONE
                                }else{
                                    findViewById<LinearLayout>(R.id.matching_text_both).visibility = View.GONE
                                    findViewById<LinearLayout>(R.id.matching_text_unmatched).visibility = View.VISIBLE
                                    findViewById<LinearLayout>(R.id.btn_request_match_container).visibility = View.GONE
                                }
                            }
                        }

                        runOnUiThread {
                            if (has_unmatched) {
                                findViewById<LinearLayout>(R.id.btn_request_match_container).visibility =
                                    View.VISIBLE
                            } else {
                                findViewById<LinearLayout>(R.id.btn_request_match_container).visibility =
                                    View.GONE
                            }
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println(e.message.toString())
                }
            })


        }else{
            findViewById<LinearLayout>(R.id.matching_card).visibility = View.GONE
        }
    }

    fun loadMessagesData(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/messages/DONATIONS/").plus(getConsID()).plus("/").plus(getEvent().event_id).plus("/android/").plus(getDeviceType())
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
                    println("JSON STRING")
                    println(jsonString);
                    val jsonArray = JSONArray(jsonString);
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i);
                        var safeMessage = setupMessage(obj);
                        val message = DonationMessage(
                            safeMessage.text,
                            safeMessage.email_body,
                            safeMessage.subject,
                            safeMessage.url,
                            safeMessage.facebook_url,
                            safeMessage.linkedin_url,
                            safeMessage.email_url,
                            safeMessage.sms_url,
                            safeMessage.custom_content,
                        )
                        donation_messages += message
                    }

                    runOnUiThread(){
                        var donationsMessageLayout = findViewById<FrameLayout>(R.id.donation_messages_layout);

                        val inflater = LayoutInflater.from(this@Donations)
                        val imageInflater = LayoutInflater.from(this@Donations)
                        var i = 0;
                        totalMessagesSlideCount = donation_messages.count();
                        for (message in donation_messages) {
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
                        setupSlideButtons(totalMessagesSlideCount, R.id.donation_messages_slide_buttons, "messages")
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                //displayAlert(getResources().getString(R.string.mobile_overview_edit_goal_error), hideAlert())
                //hideAlert()
            }
        })
    }

    fun loadDonorData(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getDonations/").plus(getConsID()).plus("/").plus(getEvent().event_id)
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
                    val jsonArray = JSONArray(jsonString);
                    var donorArray = emptyList<Donor>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i);

                        var full_name = ""
                        var email = ""

                        if(obj.has("name")){
                            full_name = obj.get("name").toString()
                        }

                        if(obj.has("email") && obj.get("email") is String){
                            email = obj.get("email") as String;
                        }else{
                            email = ""
                        }

                        var dateFormat = SimpleDateFormat("MM/dd/yyyy");
                        var d = dateFormat.parse(obj.get("date") as String);

                        var dAmt = 0.00;
                        if(obj.has("amount")){
                            dAmt = toDouble(obj.get("amount") );
                        }
                        
                        val bd = BigDecimal(dAmt)
                        val ceilRound = bd.setScale(3, RoundingMode.CEILING)
                        val roundedAmt = ceilRound.setScale(2, RoundingMode.FLOOR)

                        var first_name = "";
                        var last_name = "";

                        if(full_name.indexOf(" ") != -1){
                            first_name = full_name.substring(0,full_name.indexOf(" "))
                            last_name = full_name.substring(full_name.indexOf(" ") + 1)
                        }else{
                            first_name = full_name;
                        }


                        val donor = Donor(first_name,last_name,roundedAmt.toDouble(),d,email)
                        donorArray += donor;
                    }

                    donors = donorArray;

                    runOnUiThread{
                        if(donors.size > 0){
                            fadeInView(findViewById<LinearLayout>(R.id.donations_card))
                        }
                        updateSortArrows()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                //displayAlert(getResources().getString(R.string.mobile_overview_edit_goal_error), hideAlert())
                //hideAlert()
            }
        })
    }
    
    fun loadDonorRows(sortedDonors: List<Donor>){
        val donorTable = findViewById<FrameLayout>(R.id.donors_table_container);
        currentDonorsSlideIndex = 0
        donorTable.removeAllViews();
       
        val inflater = LayoutInflater.from(this@Donations)
        var j = 0;
        val page_chunks = sortedDonors.chunked(5);
        for (chunk in page_chunks){
            val page = inflater.inflate(R.layout.donor_table_page, null) as TableLayout
            for (donor in chunk) {
                val cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"))
                cal.time = donor.date
                val row = inflater.inflate(R.layout.donor_row, null) as TableRow
                (row.getChildAt(0) as TextView).setText(donor.first_name.plus(" ").plus(donor.last_name))
                (row.getChildAt(1) as TextView).setText(formatDoubleToLocalizedCurrency(donor.amount))
                (row.getChildAt(2) as TextView).setText((cal[Calendar.MONTH] + 1).toString().plus("/").plus(cal[Calendar.DAY_OF_MONTH]).plus("/").plus(cal[Calendar.YEAR]))
                ((row.getChildAt(3) as LinearLayout).getChildAt(0) as ImageView).setTintValue(getStringVariable("PRIMARY_COLOR"))
                page.addView(row)
            }
            donorTable.addView(page);
            if(j > 0){
                page.setVisibility(View.INVISIBLE)
            }
            j = j + 1;
        }

        setupSlideButtons(page_chunks.size,R.id.donors_slide_buttons,"donors")

        totalDonorSlideCount = j;
        addEmailClickEvents(sortedDonors);
    }

    fun loadMatchRows(sortedMatches: List<Match>){
        val matchTable = findViewById<FrameLayout>(R.id.matches_table_container);
        currentMatchSlideIndex = 0
        matchTable.removeAllViews();

        val inflater = LayoutInflater.from(this@Donations)
        var j = 0;
        val page_chunks = sortedMatches.chunked(5);
        for (chunk in page_chunks){
            val page = inflater.inflate(R.layout.donor_table_page, null) as TableLayout
            for (donor in chunk) {
                val cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"))
                cal.time = donor.date
                val row = inflater.inflate(R.layout.donor_match_row, null) as TableRow
                (row.getChildAt(0) as TextView).setText(donor.first_name.plus(" ").plus(donor.last_name))
                (row.getChildAt(1) as TextView).setText(formatDoubleToLocalizedCurrency(donor.amount))
                (row.getChildAt(2) as TextView).setText(donor.status_string)
                page.addView(row)
            }
            matchTable.addView(page);
            if(j > 0){
                page.setVisibility(View.INVISIBLE)
            }
            j = j + 1;
        }

        setupSlideButtons(page_chunks.size, R.id.matching_slide_buttons, "matches")
        totalMatchSlideCount = j;
    }

    fun getArrowVO(index: Int, sortDir: Boolean) : String{
        var sorts = listOf(
            R.string.mobile_donations_donors_name_sorted,
            R.string.mobile_donations_donors_amount_sorted,
            R.string.mobile_donations_donors_date_sorted,
        )

        if(sortDir){
            return getString(sorts[index]) + " " + getString(R.string.mobile_donations_donors_asc)
        }else{
            return getString(sorts[index]) + " " + getString(R.string.mobile_donations_donors_desc)
        }
    }

    fun getMatchArrowVO(index: Int, sortDir: Boolean) : String{
        var sorts = listOf(
            R.string.mobile_donations_double_donation_name_sorted,
            R.string.mobile_donations_double_donation_amount_sorted,
            R.string.mobile_donations_double_donation_status_sorted,
        )

        if(sortDir){
            return getString(sorts[index]) + " " + getString(R.string.mobile_donations_donors_asc)
        }else{
            return getString(sorts[index]) + " " + getString(R.string.mobile_donations_donors_desc)
        }
    }

    fun updateSortArrows(){
        val tableHeader = findViewById<TableRow>(R.id. donors_table_header_row);
        for(indx in 0 .. tableHeader.childCount - 2){
            var arrowContainer = (tableHeader.getChildAt(indx) as LinearLayout);
            var arrowImage = arrowContainer.getChildAt(1) as ImageView
            if(indx == currentSortBy){
                arrowContainer.contentDescription = getArrowVO(indx, currentSortDir)
                if(currentSortDir){
                    //ASCENDING
                    arrowImage.setImageResource(R.drawable.sort_arrows_up)
                }else{
                    //DESCENDING
                    arrowImage.setImageResource(R.drawable.sort_arrows_down)
                }
            }else{

                var sorts = listOf(
                    R.string.mobile_donations_donors_name_sort,
                    R.string.mobile_donations_donors_amount_sort,
                    R.string.mobile_donations_donors_date_sort,
                )

                arrowContainer.contentDescription = getString(sorts[indx])
                arrowImage.setImageResource(R.drawable.sort_arrows_icon)
            }
            arrowImage.setTintValue(getStringVariable("PRIMARY_COLOR"))
        }


        var sortedDonors = emptyList<Donor>();

        if(currentSortDir){
            if(currentSortBy == 0){
                sortedDonors = donors.sortedBy { it.last_name }
            }else if (currentSortBy == 1){
                sortedDonors = donors.sortedBy { it.amount }
            }else if(currentSortBy == 2){
                sortedDonors = donors.sortedBy { it.date }
            }
        } else{
            if(currentSortBy == 0){
                sortedDonors = donors.sortedByDescending { it.last_name }
            }else if (currentSortBy == 1){
                sortedDonors = donors.sortedByDescending { it.amount }
            }else if(currentSortBy == 2){
                sortedDonors = donors.sortedByDescending { it.date }
            }
        }

        val donorTable = findViewById<FrameLayout>(R.id.donors_table_container);
        for (childView in donorTable.children) {
            donorTable.removeView(childView);
        }
        loadDonorRows(sortedDonors);
    }

    fun updateMatchSortArrows(){
        val tableHeader = findViewById<TableRow>(R.id.matching_table_header_row);
        for(indx in 0 .. tableHeader.childCount - 1){
            var arrowContainer = (tableHeader.getChildAt(indx) as LinearLayout);
            var arrowImage = arrowContainer.getChildAt(1) as ImageView
            if(indx == currentMatchSortBy){
                arrowContainer.contentDescription = getMatchArrowVO(indx, currentSortDir)
                if(currentMatchSortDir){
                    //ASCENDING
                    arrowImage.setImageResource(R.drawable.sort_arrows_up)
                }else{
                    //DESCENDING
                    arrowImage.setImageResource(R.drawable.sort_arrows_down)
                }
            }else{

                var sorts = listOf(
                    R.string.mobile_donations_double_donation_name_sort,
                    R.string.mobile_donations_double_donation_amount_sort,
                    R.string.mobile_donations_double_donation_status_sort,
                )

                arrowContainer.contentDescription = getString(sorts[indx])
                arrowImage.setImageResource(R.drawable.sort_arrows_icon)
            }
            arrowImage.setTintValue(getStringVariable("PRIMARY_COLOR"))
        }

        var sortedMatches = emptyList<Match>();

        if(currentMatchSortDir){
            if(currentMatchSortBy == 0){
                sortedMatches = matches.sortedBy { it.last_name }
            }else if (currentMatchSortBy == 1){
                sortedMatches = matches.sortedBy { it.amount }
            }else if(currentMatchSortBy == 2){
                sortedMatches = matches.sortedBy { it.status_string }
            }
        } else{
            if(currentMatchSortBy == 0){
                sortedMatches = matches.sortedByDescending { it.last_name }
            }else if (currentMatchSortBy == 1){
                sortedMatches = matches.sortedByDescending { it.amount }
            }else if(currentMatchSortBy == 2){
                sortedMatches = matches.sortedByDescending { it.status_string }
            }
        }

        val matchTable = findViewById<FrameLayout>(R.id.matches_table_container);
        for (childView in matchTable.children) {
            matchTable.removeView(childView);
        }
        loadMatchRows(sortedMatches);
    }

    fun switchDonorsSlide(newIndex:Int){
        var donorsTableLayout = findViewById<FrameLayout>(R.id.donors_table_container);
        switchSlideButton(newIndex + 1,totalDonorSlideCount,R.id.donors_slide_buttons)
        if((newIndex >= 0) and (newIndex < totalDonorSlideCount)){
            donorsTableLayout.getChildAt(currentDonorsSlideIndex).setVisibility(View.INVISIBLE);
            donorsTableLayout.getChildAt(newIndex).setVisibility(View.VISIBLE);
            currentDonorsSlideIndex = newIndex;
        }
    }

    fun switchMatchingSlide(newIndex:Int){
        var donorsTableLayout = findViewById<FrameLayout>(R.id.matches_table_container);
        switchSlideButton(newIndex + 1,totalMatchSlideCount,R.id.matching_slide_buttons)
        if((newIndex >= 0) and (newIndex < totalMatchSlideCount)){
            donorsTableLayout.getChildAt(currentMatchSlideIndex).setVisibility(View.INVISIBLE);
            donorsTableLayout.getChildAt(newIndex).setVisibility(View.VISIBLE);
            currentMatchSlideIndex = newIndex;
        }
    }
    
    fun addEmailClickEvents(sortedDonors: List<Donor>){
        val donorTable = findViewById<FrameLayout>(R.id.donors_table_container);
        for(num in 0 .. donorTable.childCount - 1){
            var page = donorTable.getChildAt(num) as LinearLayout;
            for(indx in 0 .. page.childCount - 1){
                var donorId = (num * 5) + indx;
                (page.getChildAt(indx) as TableRow).getChildAt(3).setOnClickListener {
                    sendEmail(sortedDonors[donorId].email,"","");
                }
            }
        }
    }

    fun switchMessageSlide(newIndex:Int){
        switchSlideButton(newIndex + 1,totalMessagesSlideCount,R.id.donation_messages_slide_buttons)
        var donationMessageLayout = findViewById<FrameLayout>(R.id.donation_messages_layout);
        if((newIndex >= 0) and (newIndex < totalMessagesSlideCount)){
            donationMessageLayout.getChildAt(currentMessagesSlideIndex).setVisibility(View.INVISIBLE);
            donationMessageLayout.getChildAt(newIndex).setVisibility(View.VISIBLE);
            donationMessageLayout.getChildAt(newIndex).requestFocus()
            currentMessagesSlideIndex = newIndex;
        }
    }

     fun sendEmail(address:String, subject: String, message:String){
         val intent = Intent(Intent.ACTION_SENDTO)
         intent.data = Uri.parse("mailto:".plus(address)) // only email apps should handle this
         intent.putExtra(Intent.EXTRA_SUBJECT, subject)
         intent.putExtra(Intent.EXTRA_TEXT, message)
         startActivity(
             Intent.createChooser(
                 intent,
                 getResources().getString(R.string.mobile_donations_share_dialog_title)
             )
         )
     }

    fun showAddCashAlert(){
        setAlertSender()
        hideKeyboard(this)
        this.runOnUiThread(Runnable {
            val inflater = LayoutInflater.from(this@Donations)

            val alertsContainer = findViewById<LinearLayout>(R.id.alert_container)
            alertsContainer.setVisibility(View.INVISIBLE)
            hideAlertScrollView(true)
            for (childView in alertsContainer.children) {
                alertsContainer.removeView(childView);
            }

            val cashDonationButton = findViewById<Button>(R.id.btn_cash_donation)
            setAlertSender(cashDonationButton)

            var focusableView: View = alertsContainer;
            val binding: AddCashDonationAlertBinding = DataBindingUtil.inflate(
                inflater, R.layout.add_cash_donation_alert, alertsContainer, true)
            binding.colorList = getColorList("")

            val action_button = findViewById<Button>(R.id.add_cash_donation_save_button);
            val close_button = findViewById<ImageView>(R.id.add_cash_donation_alert_close_button);

            val first_name_input = findViewById<EditText>(R.id.cash_donation_first_name_input)
            val last_name_input = findViewById<EditText>(R.id.cash_donation_last_name_input)
            val amount_input = findViewById<EditText>(R.id.cash_donation_amount_input)

            findViewById<Button>(R.id.add_cash_donation_save_button).setAlpha(.5F)
            findViewById<LinearLayout>(R.id.add_cash_donation_amount_error).setVisibility(View.GONE)

            first_name_input.addTextChangedListener(object :
                TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                    val first_name = first_name_input.text.toString()
                    val last_name = last_name_input.text.toString()
                    val amount = amount_input.text.toString()
                    checkCanAddCash(first_name,last_name,amount)
                }
            })

            last_name_input.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                    val first_name = first_name_input.text.toString()
                    val last_name = last_name_input.text.toString()
                    val amount = amount_input.text.toString()
                    checkCanAddCash(first_name,last_name,amount)
                }
            })

            amount_input.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                    val first_name = first_name_input.text.toString()
                    val last_name = last_name_input.text.toString()
                    val amount = amount_input.text.toString()
                    checkCanAddCash(first_name,last_name,amount)
                    setCurrencyVoiceover(amount_input)
                }
            })

            amount_input.setOnClickListener{
                setCurrencyVoiceover(amount_input)
            }

            action_button.setOnClickListener{
                val first_name = first_name_input.text.toString()
                val last_name = last_name_input.text.toString()
                val amount = amount_input.text.toString()

                if(checkCanAddCash(first_name,last_name,amount)){
                    addCashDonation(first_name, last_name, amount)
                }
            }

            close_button.setOnClickListener{
                hideAlert()
            }

            focusableView = close_button;
            alertsContainer.setVisibility(View.VISIBLE)
            hideAlertScrollView(false)
            focusableView.requestFocus();
            findViewById<View>(R.id.add_cash_donation_alert_heading).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            Handler().postDelayed({
                findViewById<View>(R.id.add_cash_donation_alert_heading).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }, 175)
            findViewById<View>(R.id.add_cash_donation_alert_heading).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        });
    }
    
    fun checkCanAddCash(first_name: String, last_name: String, amount: String): Boolean{
        var return_val = true;
        findViewById<LinearLayout>(R.id.add_cash_donation_amount_error).setVisibility(View.GONE)
        if(first_name == ""){
            return_val = false
        }else if (last_name == ""){
            return_val = false
        }

        if(amount == ""){
            return_val = false
        }else{
            val text_view_container = findViewById<LinearLayout>(R.id.add_cash_donation_amount_error)
            if (!checkAmount(amount)){
                return_val = false
                text_view_container.setVisibility(View.VISIBLE);
                text_view_container.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }
        }
        
        val button = findViewById<Button>(R.id.add_cash_donation_save_button)

        if(return_val){
            button.setAlpha(1F)
        }else{
            button.setAlpha(.5F)
        }

        return return_val;
    }

    fun addCashDonation(first_name: String, last_name: String, amount: String){
        var url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/addDonation/")

        val formBody = FormBody.Builder()
            .add("cons_id", getConsID())
            .add("event_id", getEvent().event_id)
            .add("first_name", first_name)
            .add("last_name", last_name)
            .add("amount", amount)
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
                    var message = "";

                    if (obj.has("success") && obj.get("success") == true) {
                        message = getString(R.string.mobile_donations_cash_donations_success_message)
                    } else {
                        message = getString(R.string.mobile_donations_cash_donations_error_message)
                    }

                    displayAlertAndFocus(message)
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                displayAlert(getResources().getString(R.string.mobile_donations_cash_donations_error_message), hideAlert())
            }
        })
    }

    fun checkAmount(amount:String): Boolean{
        val pattern = Regex("^[0-9]*\\.[0-9][0-9]\$")
        return pattern.containsMatchIn(amount)
    }

    fun displayAlertAndFocus(message: String) {
        displayAlert(message, "") {
            hideAlert()

            val cashDonationButton = findViewById<View>(R.id.btn_cash_donation);
            if (cashDonationButton != null) {
                cashDonationButton.post(Runnable {
                    cashDonationButton.requestFocus()
                    cashDonationButton.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                })
            }
        }
    }

    class Donor(
        val first_name: String,
        val last_name: String,
        val amount: Double,
        val date: Date,
        val email: String
    )

    class Match(
        val first_name: String,
        val last_name: String,
        val amount: Double,
        val date: Date,
        val email: String,
        val status: Boolean,
        val status_string: String,
        val double_donation_id: String,
        val vendor_donation_id: String,
    )

    class DonationMessage(
        val text: String,
        val email_body: String,
        val subject: String,
        val url: String,
        val facebook_url: String,
        val linkedin_url: String,
        val email_url: String,
        val sms_url: String,
        val custom_content: Boolean,
    )
}
