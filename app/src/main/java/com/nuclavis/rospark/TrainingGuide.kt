package com.nuclavis.rospark

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import app.com.kotlinapp.OnSwipeTouchListener
import com.nuclavis.rospark.databinding.FundraisingMessageBinding
import android.os.Handler
import android.view.accessibility.AccessibilityEvent
import com.nuclavis.rospark.databinding.TrainingGuideSearchAlertBinding
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import com.nuclavis.rospark.customcalendar.CustomCalendar
import com.nuclavis.rospark.customcalendar.Property
import java.io.IOException
import java.util.*


class TrainingGuide : com.nuclavis.rospark.BaseActivity() {
    lateinit var customCalendar: CustomCalendar
    lateinit var defaultProperty: Property;
    lateinit var disabledProperty: Property;
    lateinit var currentProperty: Property;
    lateinit var completedProperty: Property;
    lateinit var restProperty: Property;
    lateinit var incompletedProperty: Property;
    lateinit var datesInformation: Array<ActivityMonth>;

    var currentSlideIndex = 0
    var totalSlideCount = 0
    var shareMessages = emptyList<Fundraise.FundraisingMessage>()

    var todaysMonthIndex = 0;
    var currentMonthIndex = 0;
    var totalMonths = 0;

    var month_names = arrayOf(
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

    var training_guides = emptyArray<Guide>()

    var currentTrainingGuide = Guide(0,"","")

    override fun childviewCallback(string: String, data:String){
        hideAlert();
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("fundraising message", shareMessages[currentSlideIndex].text)
        clipboard.setPrimaryClip(clip)
        if(string == "linkedin"){
            shareLinkedIn(Uri.encode(shareMessages[currentSlideIndex].linkedin_url))
        }else if(string == "facebook") {
            shareFacebook(this@TrainingGuide, shareMessages[currentSlideIndex].facebook_url)
        }
    }

    override fun slideButtonCallback(card: Any, forward:Boolean) {
        if(card == "messages"){
            var currentIndex = currentSlideIndex;
            if(forward){
                currentIndex += 1;
            }else{
                currentIndex -= 1;
            }
            switchSlide(currentIndex)
        }else{
            setUpCalendarMonth(forward)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        recolorTheme()
        super.onCreate(savedInstanceState)
        setPageContent(R.layout.training_guide, "trainingGuide")
        setTitle(getResources().getString(R.string.mobile_main_menu_training_guide));
        getTrainingGuide()
        setTooltipText(R.id.training_guide_manage_card_help_button,R.string.mobile_training_guide_manage_tooltip,R.string.mobile_training_guide_manage_title)
        setTooltipText(R.id.training_guide_share_help_button,R.string.mobile_training_guide_share_tooltip,R.string.mobile_training_guide_share_title)
        setupManageCard()
        setupCalendar()
        setupShareMessages()
    }

    fun getTrainingGuide(){
        findViewById<LinearLayout>(R.id.change_training_plan_button).setOnClickListener{
            displayGuideAlert()
        }

        val url = resources.getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/trainingguide/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    //throw Exception(response.body?.string())
                }else{
                    val jsonString = response.body?.string()
                    training_guides = emptyArray();

                    val resp = JSONObject(jsonString);
                    if(resp.has("data")){
                        val data = resp.get("data") as JSONObject;
                        if(data.has("training_guides")){
                            println("JSON DATA")
                            val json = data.get("training_guides") as JSONArray
                            if(json.length() > 0){
                                for(i in 0 .. json.length() - 1) {
                                    val obj = json[i] as JSONObject;
                                    println(obj)
                                    var id = 0;
                                    var display_name = "";
                                    var pdf = "";

                                    if(obj.has("id")){
                                        id = obj.get("id") as Int
                                    }

                                    if(obj.has("training_guide_name")){
                                        display_name = obj.get("training_guide_name") as String
                                    }

                                    if(obj.has("pdf")){
                                        pdf = obj.get("pdf") as String
                                    }

                                    training_guides += Guide(id, display_name,pdf)
                                }
                            }
                        }

                        var id = getSafeIntegerVariable(data, "training_guide_id")
                        var name = getSafeStringVariable(data, "training_guide_name")
                        var pdf = getSafeStringVariable(data, "training_guide_pdf")

                        if(id != 0 && name != "" && pdf != ""){
                            currentTrainingGuide = Guide(id,name,pdf);
                            hideAlert()
                        }else{
                            displayGuideAlert()
                        }
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
    
    fun setupShareMessages(){

        findViewById<LinearLayout>(R.id.mobile_training_guide_share_close).setOnClickListener{
            findViewById<LinearLayout>(R.id.share_messages_content).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.training_guide_share_help_button).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.training_guide_share_expand_button).setVisibility(View.VISIBLE)
        }

        findViewById<LinearLayout>(R.id.training_guide_share_expand_button).setOnClickListener{
            findViewById<LinearLayout>(R.id.share_messages_content).setVisibility(View.VISIBLE)
            findViewById<LinearLayout>(R.id.training_guide_share_help_button).setVisibility(View.VISIBLE)
            findViewById<LinearLayout>(R.id.training_guide_share_expand_button).setVisibility(View.GONE)
        }

        val facebookShareButton = findViewById<FrameLayout>(R.id.facebook_share_button)
        facebookShareButton.setOnClickListener {
            sendSocialActivity("facebook")
            sendGoogleAnalytics("fundraise_facebook_share","fundraise")
            if(shareMessages[currentSlideIndex].custom_content){
                shareFacebook(this,shareMessages[currentSlideIndex].facebook_url)
            }else{
                displayAlert(
                    resources.getString(R.string.mobile_fundraise_message_facebook_prompt), ""
                ) { childviewCallback("facebook","") }
                setAlertSender(facebookShareButton)
            }

        }

        val emailShareButton = findViewById<FrameLayout>(R.id.email_share_button)
        emailShareButton.setOnClickListener {
            sendSocialActivity("email")
            sendGoogleAnalytics("fundraise_email_share","fundraise")
            try {
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:") // only email apps should handle this
                intent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    shareMessages[currentSlideIndex].subject
                )
                if (shareMessages[currentSlideIndex].custom_content) {
                    intent.putExtra(Intent.EXTRA_TEXT, (shareMessages[currentSlideIndex].email_url))
                } else {
                    intent.putExtra(
                        Intent.EXTRA_TEXT,
                        (shareMessages[currentSlideIndex].email_body).replace("<br>","\r\n").plus("\r\n\r\n")
                            .plus(shareMessages[currentSlideIndex].email_url)
                    )
                }
                startActivity(
                    Intent.createChooser(
                        intent,
                        getResources().getString(R.string.mobile_fundraise_share_dialog_title)
                    )
                )
            }catch(exception: IOException){
                displayAlert(getResources().getString(R.string.mobile_fundraise_share_dialog_error))
                setAlertSender(emailShareButton)
            }
        }

        val smsShareButton = findViewById<FrameLayout>(R.id.sms_share_button)
        smsShareButton.setOnClickListener {
            sendGoogleAnalytics("fundraise_sms_share","fundraise")
            sendSocialActivity("sms")
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("smsto:") // only email apps should handle this
            //intent.putExtra(Intent.EXTRA_SUBJECT, fundraisingMessages[currentSlideIndex].subject)
            if(shareMessages[currentSlideIndex].custom_content){
                intent.putExtra(Intent.EXTRA_TEXT, (shareMessages[currentSlideIndex].sms_url))
            }else {
                intent.putExtra(Intent.EXTRA_TEXT,
                    shareMessages[currentSlideIndex].text.plus(" ")
                        .plus(shareMessages[currentSlideIndex].sms_url)
                )
            }
            startActivity(Intent.createChooser(intent,getResources().getString(R.string.mobile_fundraise_share_dialog_title)))
        }

        val linkedinShareButton = findViewById<FrameLayout>(R.id.linkedin_share_button)
        linkedinShareButton.setOnClickListener {
            sendGoogleAnalytics("fundraise_linkedin_share","fundraise")
            sendSocialActivity("linkedin")
            if(shareMessages[currentSlideIndex].custom_content){
                shareLinkedIn(shareMessages[currentSlideIndex].linkedin_url )
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

        val shareMessageLayout = findViewById<FrameLayout>(R.id.share_messages_layout)
        shareMessageLayout.setOnTouchListener(object : OnSwipeTouchListener(this@TrainingGuide) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                switchSlide(currentSlideIndex + 1)
            }
            override fun onSwipeRight() {
                super.onSwipeRight()
                switchSlide(currentSlideIndex - 1)
            }
        })

        val url = resources.getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/messages/TRAININGGUIDE/").plus(getConsID()).plus("/").plus(getEvent().event_id).plus("/android/").plus(getDeviceType())
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
                    println("RESPONSE ")
                    println(jsonString)
                    val jsonArray = JSONArray(jsonString)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)

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
                        shareMessages += message
                    }

                    runOnUiThread {
                        val shareMessageLayout = findViewById<FrameLayout>(R.id.share_messages_layout)
                        val inflater = LayoutInflater.from(this@TrainingGuide)
                        var i = 0
                        totalSlideCount = shareMessages.count()
                        for (message in shareMessages) {
                            val binding: FundraisingMessageBinding = DataBindingUtil.inflate(
                                inflater, R.layout.fundraising_message ,shareMessageLayout, true)
                            binding.colorList = getColorList("")
                            val row = shareMessageLayout.getChildAt(i) as TextView
                            row.text = message.text
                            if(i == 0){
                                row.visibility = View.VISIBLE
                            } else {
                                row.visibility = View.INVISIBLE
                            }
                            i += 1
                        }
                        totalSlideCount = i

                        setupSlideButtons(totalSlideCount, R.id.share_messages_slide_buttons,"messages")
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

    fun shareLinkedIn(url: String){
        val url = "https://www.linkedin.com/shareArticle?mini=true&url=".plus(url)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    fun switchSlide(newIndex:Int){
        switchSlideButton(newIndex + 1,totalSlideCount,R.id.share_messages_slide_buttons)
        val shareMessageLayout = findViewById<FrameLayout>(R.id.share_messages_layout)
        if((newIndex >= 0) and (newIndex < totalSlideCount)){
            shareMessageLayout.getChildAt(currentSlideIndex).visibility = View.INVISIBLE
            shareMessageLayout.getChildAt(newIndex).visibility = View.VISIBLE
            shareMessageLayout.getChildAt(newIndex).requestFocus()
            currentSlideIndex = newIndex
        }
    }

    fun setupManageCard(){
        findViewById<LinearLayout>(R.id.mobile_training_guide_manage_close).setOnClickListener{
            findViewById<LinearLayout>(R.id.manage_training_guide_card_collapsed).setVisibility(View.VISIBLE)
            findViewById<LinearLayout>(R.id.training_guide_manage_card).setVisibility(View.GONE)
        }

        findViewById<ImageView>(R.id.manage_card_expand_button).setOnClickListener{
            findViewById<LinearLayout>(R.id.manage_training_guide_card_collapsed).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.training_guide_manage_card).setVisibility(View.VISIBLE)
        }

        findViewById<LinearLayout>(R.id.download_training_plan_button).setOnClickListener{
            var url = getStringVariable("TRAINING_GUIDE_URL")
            if(url != ""){
                try {
                    if(url.contains("s3://")){
                        url = url.replace("s3://nt-dev-clients/","https://nt-dev-clients.s3.amazonaws.com/")
                    }
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(browserIntent)
                }catch(e: Exception){
                    println("Can't open: " + url)
                }
            }
        }
    }

    fun setupCalendar(){
        customCalendar = findViewById<View>(R.id.custom_calendar) as CustomCalendar

        val descHashMap: HashMap<Any, Property> = HashMap()
        defaultProperty = Property()
        defaultProperty.layoutResource = R.layout.calendar_default_view
        defaultProperty.dateTextViewResource = R.id.text_view
        descHashMap["default"] = defaultProperty

        disabledProperty = Property()
        disabledProperty.layoutResource = R.layout.calendar_disabled_view
        disabledProperty.dateTextViewResource = R.id.text_view
        descHashMap["disabled"] = disabledProperty

        currentProperty = Property()
        currentProperty.layoutResource = R.layout.calendar_today_view
        currentProperty.dateTextViewResource = R.id.text_view
        descHashMap["current"] = currentProperty

        completedProperty = Property()
        completedProperty.layoutResource = R.layout.calendar_completed_challenge_view
        completedProperty.dateTextViewResource = R.id.text_view
        descHashMap["complete"] = completedProperty

        incompletedProperty = Property()
        incompletedProperty.layoutResource = R.layout.calendar_incompleted_challenge_view
        incompletedProperty.dateTextViewResource = R.id.text_view
        descHashMap["incomplete"] = incompletedProperty

        restProperty = Property()
        restProperty.layoutResource = R.layout.calendar_rest_day_view
        restProperty.dateTextViewResource = R.id.text_view
        descHashMap["rest"] = restProperty

        val container = customCalendar.getChildAt(0) as LinearLayout
        container.getChildAt(0).setVisibility(View.GONE)
        container.getChildAt(1).setVisibility(View.GONE)

        customCalendar.setMapDescToProp(descHashMap)
        getActivityData()

        findViewById<LinearLayout>(R.id.calendar_today_button).setOnClickListener{
            setUpCalendarMonth(null)
        }

        var calendarContainer = findViewById<CustomCalendar>(R.id.custom_calendar);
        calendarContainer.setOnTouchListener(object : OnSwipeTouchListener(this@TrainingGuide) {
            override fun onSwipeLeft() {
                super.onSwipeLeft();
                setUpCalendarMonth(true);
            }
            override fun onSwipeRight() {
                super.onSwipeRight();
                setUpCalendarMonth(false);
            }
        })
    }

    fun getActivityData(){
        datesInformation = arrayOf(
            ActivityMonth(
                2022,
                11,
                arrayOf(
                    ActivityDay(1, "complete"),
                    ActivityDay(2, "incomplete"),
                    ActivityDay(3, "incomplete"),
                    ActivityDay(4, "complete")
                ),
            ),
            ActivityMonth(
                2023,
                0,
                arrayOf(
                    ActivityDay(2, "incomplete"),
                    ActivityDay(3, "complete"),
                    ActivityDay(4, "complete"),
                    ActivityDay(5, "incomplete")
                ),
            ),
            ActivityMonth(
                2023,
                1,
                arrayOf(
                    ActivityDay(3, "incomplete"),
                    ActivityDay(4, "complete"),
                    ActivityDay(5, "complete"),
                    ActivityDay(6, "complete")
                ),
            ),
            ActivityMonth(
                2023,
                2,
                arrayOf(
                    ActivityDay(7, "complete"),
                    ActivityDay(8, "complete"),
                    ActivityDay(9, "incomplete"),
                    ActivityDay(10, "incomplete")
                ),
            ),
            ActivityMonth(
                2023,
                3,
                arrayOf(
                    ActivityDay(20, "incomplete"),
                    ActivityDay(23, "complete"),
                    ActivityDay(24, "complete"),
                    ActivityDay(25, "complete")
                ),
            ),
            ActivityMonth(
                2023,
                4,
                arrayOf(
                    ActivityDay(12, "incomplete"),
                    ActivityDay(13, "complete"),
                    ActivityDay(14, "incomplete"),
                    ActivityDay(15, "complete")
                ),
            ),
            ActivityMonth(
                2023,
                5,
                arrayOf(
                    ActivityDay(10, "complete"),
                    ActivityDay(13, "incomplete"),
                    ActivityDay(14, "complete"),
                    ActivityDay(15, "incomplete")
                ),
            ),
            ActivityMonth(
                2023,
                6,
                arrayOf(
                    ActivityDay(20, "complete"),
                    ActivityDay(21, "complete"),
                    ActivityDay(22, "incomplete"),
                    ActivityDay(15, "incomplete")
                ),
            ),
            ActivityMonth(
                2023,
                7,
                arrayOf(
                    ActivityDay(13, "incomplete"),
                    ActivityDay(24, "incomplete"),
                    ActivityDay(15, "incomplete"),
                    ActivityDay(16, "complete")
                ),
            ),
            ActivityMonth(
                2023,
                8,
                arrayOf(
                    ActivityDay(27, "incomplete"),
                    ActivityDay(28, "complete"),
                    ActivityDay(29, "incomplete"),
                    ActivityDay(40, "incomplete")
                ),
            ),
            ActivityMonth(
                2023,
                9,
                arrayOf(
                    ActivityDay(12, "incomplete"),
                    ActivityDay(13, "incomplete"),
                    ActivityDay(14, "complete"),
                    ActivityDay(25, "complete")
                ),
            ),
            ActivityMonth(
                2023,
                10,
                arrayOf(
                    ActivityDay(1, "complete"),
                    ActivityDay(17, "incomplete"),
                    ActivityDay(24, "incomplete"),
                    ActivityDay(29, "complete")
                ),
            ),
            ActivityMonth(
                2023,
                11,
                arrayOf(
                    ActivityDay(10, "incomplete"),
                    ActivityDay(23, "incomplete"),
                    ActivityDay(14, "complete"),
                    ActivityDay(25, "incomplete")
                ),
            ),
            ActivityMonth(
                2024,
                0,
                arrayOf(
                    ActivityDay(12, "complete"),
                    ActivityDay(13, "complete"),
                    ActivityDay(24, "incomplete"),
                    ActivityDay(25, "incomplete")
                ),
            ),
        )
        totalMonths = datesInformation.size

        val todaysMonth = Calendar.getInstance().get(Calendar.MONTH)
        val todaysYear = Calendar.getInstance().get(Calendar.YEAR)
        var i = 0;
        for(month in datesInformation){
            if(month.year == todaysYear){
                if(month.month == todaysMonth){
                    todaysMonthIndex = i;
                }
            }
            i++;
        }

        setupSlideButtons(totalMonths, R.id.calendar_slide_buttons, "CALENDAR")
        setUpCalendarMonth(null)
    }

    fun displayGuideAlert(){
        setAlertSender()
        hideKeyboard(this)
        this.runOnUiThread(Runnable {
            val inflater = LayoutInflater.from(this@TrainingGuide)

            val alertsContainer = findViewById<LinearLayout>(R.id.alert_container)
            alertsContainer.setVisibility(View.INVISIBLE)
            hideAlertScrollView(true)
            for (childView in alertsContainer.children) {
                alertsContainer.removeView(childView);
            }

            var focusableView: View = alertsContainer;
            val binding: TrainingGuideSearchAlertBinding = DataBindingUtil.inflate(
                inflater, R.layout.training_guide_search_alert, alertsContainer, true)
            binding.colorList = getColorList("")

            val dropdown: Spinner = findViewById(R.id.select_guide)

            var current_pos = 0;
            var items = arrayOf<String>(getString(R.string.mobile_training_guide_find_guide_alert_please_select))
            var i = 1;
            for(guide in training_guides){
                items += (guide.training_guide_name)
                if(guide.training_guide_id == currentTrainingGuide.training_guide_id){
                    current_pos = i
                }
                i++;
            }
            val action_button = findViewById<TextView>(R.id.training_guide_search_action_button);
            val adapter = ArrayAdapter<String>(this, R.layout.spinner_item, items)
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            dropdown.adapter = adapter
            dropdown.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parentView: AdapterView<*>?,
                    selectedItemView: View?,
                    position: Int,
                    id: Long
                ) {
                    if(position == 0){
                        action_button.setAlpha(.5F)
                        currentTrainingGuide = Guide(0,"","")
                    }else {
                        currentTrainingGuide = training_guides[position - 1]
                        println("NEw: ")
                        println(training_guides[position - 1])
                        action_button.setAlpha(1F)
                    }
                }

                override fun onNothingSelected(parentView: AdapterView<*>?) {
                    // your code here
                }
            })

            if(currentTrainingGuide.training_guide_id != 0){
                dropdown.setSelection(current_pos)
            }

            val close_button = findViewById<ImageView>(R.id.training_guide_search_alert_close_button);
            action_button.setOnClickListener {
                if(currentTrainingGuide.training_guide_id != 0){
                    val url = resources.getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/activity/trainingguide/").plus(getConsID()).plus("/").plus(getEvent().event_id).plus("/").plus(currentTrainingGuide.training_guide_id)

                    val formBody = FormBody.Builder()
                        .add("client_code", getStringVariable("CLIENT_CODE"))
                        .build()

                    var request = Request.Builder().url(url)
                        .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                        .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                        .post(formBody)
                        .build()

                    var client = OkHttpClient();
                    client.newCall(request).enqueue(object : Callback {
                        override fun onResponse(call: Call, response: Response) {
                            val jsonString = response.body?.string();
                            var id = 0
                            var name = ""
                            var pdf = ""

                            val resp = JSONObject(jsonString);
                            if(resp.has("data")){
                                val data = resp.get("data") as JSONObject;
                                if(data.has("training_guides")){
                                    println("JSON DATA")
                                    val json = data.get("training_guides") as JSONArray
                                    if(json.length() > 0){
                                        for(i in 0 .. json.length() - 1) {
                                            val obj = json[i] as JSONObject;
                                            println(obj)
                                            var id = 0;
                                            var display_name = "";
                                            var pdf = "";

                                            if(obj.has("id")){
                                                id = obj.get("id") as Int
                                            }

                                            if(obj.has("training_guide_name")){
                                                display_name = obj.get("training_guide_name") as String
                                            }

                                            if(obj.has("pdf")){
                                                pdf = obj.get("pdf") as String
                                            }

                                            training_guides += Guide(id, display_name,pdf)
                                        }
                                    }
                                }

                                var id = ""
                                var name = ""
                                var pdf = ""

                                if(data.has("id")){
                                    id = data.get("id") as String
                                }

                                if(data.has("training_guide_name") && data.get("training_guide_name") != null){
                                    name = data.get("training_guide_name") as String
                                }

                                if(data.has("training_guide_pdf")){
                                    pdf = data.get("training_guide_pdf") as String
                                }

                                if(id != "" && name != "" && pdf != ""){
                                    currentTrainingGuide = Guide(id.toInt(),name,pdf);
                                    hideAlert()
                                }else {
                                    displayAlert(getString(R.string.mobile_training_guide_find_guide_alert_error))
                                }
                            }
                        }
                        override fun onFailure(call: Call, e: IOException) {
                            println("Error logging error")
                            println(e.message.toString())
                        }
                    })
                }else{
                    action_button.setAlpha(.5F)
                }
            }

            if(currentTrainingGuide.training_guide_id == 0) {
                close_button.setVisibility(View.GONE)
            }else{
                close_button.setOnClickListener{
                    hideAlert()
                }
            }

            focusableView = close_button;
            alertsContainer.setVisibility(View.VISIBLE)
            hideAlertScrollView(false)
            focusableView.requestFocus();

            Handler().postDelayed({
                findViewById<View>(R.id.training_guide_alert_heading).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }, 175)
        });
    }

    fun setUpCalendarMonth(direction: Boolean?){
        var dateHashmap: HashMap<Int, Any> = HashMap()
        val calendar: Calendar = Calendar.getInstance()
        var canSwitch =  false;

        if(direction == true && currentMonthIndex < totalMonths - 1){
            currentMonthIndex = currentMonthIndex + 1
            canSwitch = true
        }else if (direction == false && currentMonthIndex > 0){
            currentMonthIndex = currentMonthIndex - 1
            canSwitch = true
        }else if(direction != true && direction != false){
            canSwitch = true
            currentMonthIndex = todaysMonthIndex
        }

        if(canSwitch){
            try {
                var month = datesInformation[currentMonthIndex]

                calendar.set(Calendar.MONTH, month.month)
                calendar.set(Calendar.YEAR, month.year)

                for(day in month.days){
                    dateHashmap[day.day] = day.status
                }

                val today = calendar.get(Calendar.DAY_OF_MONTH)
                val max = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                for (d in 1..max) {
                    calendar[Calendar.DAY_OF_MONTH] = d
                    val dayOfWeek = calendar[Calendar.DAY_OF_WEEK]
                    if (dayOfWeek == Calendar.SUNDAY) {
                        if(d != today){
                            dateHashmap[d] = "rest"
                        }
                    }
                }

                if(currentMonthIndex == todaysMonthIndex){
                    dateHashmap[today] = "current"
                }

                customCalendar.setDate(calendar, dateHashmap)
                findViewById<TextView>(R.id.calendar_title).text = (getResources().getString(month_names[month.month]) + " " + month.year)

                switchSlideButton(currentMonthIndex + 1,totalMonths,R.id.calendar_slide_buttons)
            }catch(e: Exception){
                println("ISSUE LOADING CALENDAR")
            }
        }
    }
}

class Guide(
    val training_guide_id: Int,
    val training_guide_name: String,
    val training_guide_pdf: String
)
class ActivityDay(
    val day: Int,
    val status: String
)
class ActivityMonth(
    val year: Int,
    val month: Int,
    val days: Array<ActivityDay>
    )