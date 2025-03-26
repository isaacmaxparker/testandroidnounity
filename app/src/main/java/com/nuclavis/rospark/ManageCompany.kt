package com.nuclavis.rospark

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.os.Handler
import android.view.accessibility.AccessibilityEvent
import android.widget.*
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.databinding.DataBindingUtil
import app.com.kotlinapp.OnSwipeTouchListener
import com.bumptech.glide.Glide
import com.nuclavis.rospark.databinding.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.properties.Delegates

class ManageCompany : BaseActivity() {
    var totalMessagesSlideCount = 0;
    var currentMessagesSlideIndex = 0;
    var company_teams = emptyList<CompanyTeam>();
    var totalTeamSlideCount = 0;
    var currentTeamsSlideIndex = 0;
    var currentSortBy = 1;
    var currentSortDir = false;
    var team_messages = emptyList<Teams.TeamMessage>();
    var team_emails = emptyArray<String>();

    var progressBarWidth = 0;
    var progressBarHeight = 0;

    override fun childviewCallback(string: String, data: String) {
        loadProgressCard(false)
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
        }else if (card == "members"){
            var currentIndex = currentTeamsSlideIndex;
            if(forward){
                currentIndex += 1;
            }else{
                currentIndex -= 1;
            }
            switchTeamsSlide(currentIndex)
        }
    }

    var companyProgressPercent: Int by Delegates.observable(0) { _, old, new ->
        run {
            if (old == 0 && new != 0) {
                resizeProgressBar(companyProgressPercent.toDouble()/100, "Company")
            }
        };
    }

    fun resizeProgressBar(percent: Double, bar: String){
        runOnUiThread {
            var newBarWidth = progressBarHeight;
            if((percent * progressBarWidth) > progressBarHeight){
                newBarWidth = (percent * progressBarWidth).toInt()
            }else if(percent == 0.00){
                newBarWidth = 0
            }

            val companyProgress = findViewById<LinearLayout>(R.id.progress_card_raised_company_progress_bar);
            companyProgress.layoutParams = FrameLayout.LayoutParams(newBarWidth, progressBarHeight)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        recolorTheme()
        super.onCreate(savedInstanceState)
        setPageContent(R.layout.manage_company, "manageCompany")
        setTitle(getResources().getString(R.string.mobile_main_menu_company));
        sendGoogleAnalytics("manage_company_view","manage_company")

        findViewById<TextView>(R.id.company_progress_card_title).setText(
            getStringVariable("USER_COMPANY_NAME").uppercase()
        )

        val eventRaisedBar = findViewById<LinearLayout>(R.id.progress_card_raised_company_bar);
        eventRaisedBar.doOnLayout {
            progressBarWidth = it.measuredWidth
            progressBarHeight = it.measuredHeight
        }

        loadProgressCard(true)
        loadMessagesData()

        val teams_table_name_sort_button = findViewById<LinearLayout>(R.id.manage_company_teams_table_name_sort_link);
        teams_table_name_sort_button.setOnClickListener {
            if(currentSortBy == 0){
                currentSortDir = !currentSortDir
            }else{
                currentSortBy = 0;
                currentSortDir = false;
            }
            updateSortArrows(false)
        }

        val teams_table_amount_sort_button = findViewById<LinearLayout>(R.id.manage_company_teams_table_amount_sort_link);
        teams_table_amount_sort_button.setOnClickListener {
            if(currentSortBy == 1){
                currentSortDir = !currentSortDir
            }else{
                currentSortBy = 1;
                currentSortDir = false;
            }
            updateSortArrows(false)
        }

        val teams_table_goal_sort_button = findViewById<LinearLayout>(R.id.manage_company_teams_table_goal_sort_link);
        teams_table_goal_sort_button.setOnClickListener {
            if(currentSortBy == 2){
                currentSortDir = !currentSortDir
            }else{
                currentSortBy = 2;
                currentSortDir = false;
            }
            updateSortArrows(false)
        }

        val emailShareButton = findViewById<Button>(R.id.email_team_btn)
        emailShareButton.setOnClickListener {
            var message = ""
            if(team_messages.size > 0){
                if(team_messages[currentMessagesSlideIndex].custom_content){
                    message = team_messages[currentMessagesSlideIndex].email_url;
                }else {
                    message = team_messages[currentMessagesSlideIndex].email_body.replace("<br>","\r\n") + "\r\n\r\n" + team_messages[currentMessagesSlideIndex].email_url;
                }
                composeMultipleEmail(team_emails, team_messages[currentMessagesSlideIndex].subject, message)
            }else{
                displayAlert(getResources().getString(R.string.mobile_fundraise_share_dialog_error))
                setAlertSender(emailShareButton)
            }
        }

        loadTeamData()

        setTooltipText(R.id.manage_company_progress_help_button,R.string.mobile_company_progress_tooltip, getStringVariable("USER_COMPANY_NAME") + " " + getString(R.string.mobile_manage_page_personalize_custom_company_page_title))
        setTooltipText(R.id.manage_company_teams_help_button,R.string.mobile_company_teams_tooltip, R.string.mobile_company_teams_title)
        setTooltipText(R.id.manage_company_messages_help_button,R.string.mobile_company_team_messages_tooltip, R.string.mobile_company_team_messages_title)

        findViewById<LinearLayout>(R.id.progress_card_raised_company_progress_bar).post{
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

            findViewById<LinearLayout>(R.id.progress_card_raised_company_progress_bar).background.setColorFilter(
                Color.parseColor(color),
                PorterDuff.Mode.SRC_ATOP
            )
        }

        findViewById<TextView>(R.id.progress_card_edit_company_goal).setOnClickListener {
            sendGoogleAnalytics("manage_company_company_edit_goal","overview")
            displayAlert("updateCompanyGoal", toDouble(companyGoal))
            setAlertSender(findViewById<TextView>(R.id.progress_card_edit_company_goal))
        }

        if(getStringVariable("DISABLE_EDIT_COMPANY_PAGE") == "true"){
            findViewById<LinearLayout>(R.id.progress_card_edit_company_page).visibility = View.GONE
        }else{
            findViewById<LinearLayout>(R.id.progress_card_edit_company_page).setOnClickListener {
                val intent = Intent(this@ManageCompany, com.nuclavis.rospark.ManagePage::class.java);
                intent.putExtra("starting_page","company");
                startActivity(intent);
                this@ManageCompany.overridePendingTransition(0, 0);
            }
        }
    }

    fun addEmailClickEvents(sortedTeamMembers:List<CompanyTeam>){
        val teamTable = findViewById<FrameLayout>(R.id.manage_company_teams_table_container);
        for(num in 0 .. teamTable.childCount - 1){
            var page = teamTable.getChildAt(num) as LinearLayout;
            for(indx in 0 .. page.childCount - 1){
                var teamId = (num * 5) + indx;
                (page.getChildAt(indx) as TableRow).getChildAt(3).setOnClickListener {
                    sendEmail(sortedTeamMembers[teamId].captain_email,"","");
                }
            }
        }
    }

    fun loadProgressCard(initial: Boolean){
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

                    runOnUiThread {
                        if(obj.has("company_percent") && obj.get("company_percent") is Int){
                            companyProgressPercent = obj.get("company_percent") as Int;
                            setVariable("COMPANY_PROGRESS_PERCENT",companyProgressPercent.toString())
                            resizeProgressBar((companyProgressPercent.toDouble()/100),"")
                        }else{
                            companyProgressPercent = 0;
                        }
                        if(obj.has("company_goal")){
                            companyGoal = toDouble(obj.get("company_goal"));
                            setVariable("COMPANY_GOAL",companyGoal.toString())
                        }

                        val companyProgressRaisedAmountText =
                            findViewById<TextView>(R.id.progress_card_company_raised_amount);

                        if (obj.has("company_raised")) {
                            companyProgressRaisedAmountText.setText(
                                getString(R.string.mobile_overview_company_progress_raised).plus(
                                    " "
                                ).plus(" $").plus(withCommas(toDouble(obj.get("company_raised"))))
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
                        val companyProgressRaisedGoalText =
                            findViewById<TextView>(R.id.progress_card_company_raised_goal);

                        if (obj.has("team_goal")) {
                            companyProgressRaisedGoalText.setText(
                                getString(R.string.mobile_overview_team_progress_goal).plus(
                                    " "
                                ).plus(" $").plus(withCommas(toDouble(obj.get("company_goal"))))
                            );
                        } else {
                            companyProgressRaisedGoalText.setText(
                                getString(R.string.mobile_overview_team_progress_goal).plus(
                                    " $0.00"
                                )
                            );
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

    fun loadMessagesData(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/messages/COMPANY/").plus(getConsID()).plus("/").plus(getEvent().event_id).plus("/android/").plus(getDeviceType())
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
                    println("JSON RESPONSe")
                    println(jsonString)
                    val jsonArray = JSONArray(jsonString);
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i);
                        var safeMessage = setupMessage(obj);

                        val message = Teams.TeamMessage(
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
                        team_messages += message;
                    }

                    runOnUiThread(){
                        var messageLayout = findViewById<FrameLayout>(R.id.manage_company_team_messages_layout);
                        val inflater = LayoutInflater.from(this@ManageCompany)
                        var i = 0;
                        totalMessagesSlideCount = team_messages.count();
                        for (message in team_messages) {
                            val binding: FundraisingMessageBinding = DataBindingUtil.inflate(
                                inflater, R.layout.fundraising_message ,messageLayout, true)
                            binding.colorList = getColorList("")
                            val row = messageLayout.getChildAt(i) as TextView
                            row.setText(message.text);
                            if(i == 0){
                                row.setVisibility(View.VISIBLE);
                            } else {
                                row.setVisibility(View.INVISIBLE);
                            }
                            i = i + 1;
                        }
                        totalMessagesSlideCount = i;
                        setupSlideButtons(totalMessagesSlideCount, R.id.team_messages_slide_buttons,"messages")
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                //displayAlert(getResources().getString(R.string.mobile_overview_edit_goal_error), hideAlert())
                //hideAlert()
            }
        })

        findViewById<LinearLayout>(R.id.manage_company_messages_card).setOnTouchListener(object : OnSwipeTouchListener(this@ManageCompany) {
            override fun onSwipeLeft() {
                super.onSwipeLeft();
                switchMessageSlide(currentMessagesSlideIndex + 1);
            }
            override fun onSwipeRight() {
                super.onSwipeRight();
                switchMessageSlide(currentMessagesSlideIndex - 1);
            }
        })
    }

    fun loadTeamData(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/companyData/").plus(getEvent().event_id).plus("/").plus(getStringVariable("COMPANY_ID"))

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
                    val obj = JSONObject(jsonString)

                    runOnUiThread {
                        findViewById<TextView>(R.id.company_progress_card_team_count).text =
                            getSafeIntegerVariable(obj, "team_count").toString()
                        findViewById<TextView>(R.id.company_progress_card_participant_count).text =
                            getSafeIntegerVariable(obj, "participant_count").toString()
                    }

                    val jsonArray = obj.get("teams") as JSONArray;
                    var memberArray = emptyList<CompanyTeam>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i);

                        println("ONJ")
                        println(obj)

                        var team_captain_id = getSafeStringVariable(obj, "team_captain_id")
                        var amount = getSafeDoubleVariable(obj, "amount_raised")
                        var goal = getSafeDoubleVariable(obj, "goal")
                        var team_member_count = getSafeIntegerVariable(obj, "num_team_members")
                        var team_captain_name = getSafeStringVariable(obj, "team_captain_name")
                        var name = getSafeStringVariable(obj, "name")

                        val member = CompanyTeam(
                            name,
                            goal,
                            amount,
                            team_captain_name,
                            team_captain_id,
                            "",
                            team_member_count,
                        )
                        memberArray += member;
                    }
                    company_teams = memberArray;
                    updateEmails()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                //displayAlert(getResources().getString(R.string.mobile_overview_edit_goal_error), hideAlert())
                //hideAlert()
            }
        })
    }

    fun updateEmails(){
        team_emails = emptyArray()
        var finished_students = 0;
        var recheck = true;
        for (i in 0 until company_teams.count()) {
            val member = company_teams[i];

            println("MEMBER: ")
            println(member.captain_id)

            if(member.captain_email == ""){
                val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getParticipantDataByCompany/").plus(member.captain_id).plus("/").plus(getEvent().event_id)
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
                            val jsonObject = JSONObject(jsonString);
                            println("JSON ")
                            println(jsonString)
                            if(jsonObject.has("email")){
                                if(jsonObject.get("email") is String){
                                    updateCaptainEmail(member.captain_id, (jsonObject.get("email") as String))
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
                finished_students += 1
            }else{
                finished_students += 1;
            }
        }

        do{
            if(finished_students >= company_teams.size){
                recheck = false;
                updateSortArrows(true)
            }
        }while(recheck)
    }

    fun updateCaptainEmail(id: String, email: String){
        team_emails += email
        company_teams.find { it.captain_id == id }!!.captain_email = email;
    }

    fun loadTeamRows(sortedTeams: List<CompanyTeam>){
        val teamTable = findViewById<FrameLayout>(R.id.manage_company_teams_table_container);
        currentTeamsSlideIndex = 0
        for (childView in teamTable.children) {
            teamTable.removeView(childView);
        }

        val inflater = LayoutInflater.from(this@ManageCompany)

        val page_chunks = sortedTeams.chunked(5);
        var j = 0;
        for (chunk in page_chunks){
            if(j <= page_chunks.size - 1){
                val page = inflater.inflate(R.layout.team_table_page, null) as TableLayout
                for (team in chunk) {

                    val binding: TeamRowBinding = DataBindingUtil.inflate(inflater, R.layout.team_row, null, true)
                    binding.colorList = getColorList("")

                    val row = binding.root as TableRow
                    val name_container = row.getChildAt(0) as LinearLayout
                    (name_container.getChildAt(0) as ImageView).setVisibility(View.GONE)

                    val content = SpannableString(team.name)
                    content.setSpan(UnderlineSpan(), 0, content.length, 0)
                    content.setSpan(ForegroundColorSpan(Color.parseColor(getStringVariable("PRIMARY_COLOR"))), 0, content.length, 0)


                    (name_container.getChildAt(1) as TextView).setText(content)

                    (name_container.getChildAt(1) as TextView).setOnClickListener{
                        displayCompanyInfoAlert(team)
                    }

                    (row.getChildAt(1) as TextView).setText(formatDoubleToLocalizedCurrency(team.amount_raised))
                    (row.getChildAt(2) as TextView).setText(formatDoubleToLocalizedCurrency(team.goal))
                    page.addView(row)
                }
                teamTable.addView(page);

                if(j > 0){
                    page.setVisibility(View.INVISIBLE)
                }
                j = j + 1;
            }
        }
        totalTeamSlideCount = page_chunks.size;
        setupSlideButtons(totalTeamSlideCount,R.id.manage_company_teams_slide_buttons,"members")
        if(teamTable.children.count() > totalTeamSlideCount){
            teamTable.removeView(teamTable.getChildAt(0))
        }

        addEmailClickEvents(sortedTeams);
    }

    fun getArrowVO(index: Int, sortDir: Boolean) : String{
        var sorts = listOf(
            R.string.mobile_manage_company_teams_card_team_sorted,
            R.string.mobile_manage_company_teams_card_amount_sort,
            R.string.mobile_manage_company_teams_card_amount_sorted,
        )

        if(sortDir){
            return getString(sorts[index]) + " " + getString(R.string.mobile_manage_company_team_messages_asc)
        }else{
            return getString(sorts[index]) + " " + getString(R.string.mobile_manage_company_team_messages_desc)
        }
    }

    fun updateSortArrows(initial: Boolean){
        runOnUiThread {
            val tableHeader = findViewById<TableRow>(R.id.manage_company_teams_table_header_row);
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
                        R.string.mobile_manage_company_teams_card_team_sort,
                        R.string.mobile_manage_company_teams_card_amount_sort,
                        R.string.mobile_manage_company_teams_card_goal_sort,
                    )

                    arrowContainer.contentDescription = getString(sorts[indx])
                    arrowImage.setImageResource(R.drawable.sort_arrows_icon)
                }
                arrowImage.setTintValue(getStringVariable("PRIMARY_COLOR"))
            }

            var sortedTeams = emptyList<CompanyTeam>();

            if(initial){
                sortedTeams = company_teams.sortedByDescending{ it.amount_raised }
            }else{
                if(currentSortDir){
                    if(currentSortBy == 0){
                        sortedTeams = company_teams.sortedBy { it.name }
                    }else if (currentSortBy == 1){
                        sortedTeams = company_teams.sortedBy { it.amount_raised }
                    }else if(currentSortBy == 2){
                        sortedTeams = company_teams.sortedBy { it.goal }
                    }
                } else{
                    if(currentSortBy == 0){
                        sortedTeams = company_teams.sortedByDescending { it.name }
                    }else if (currentSortBy == 1){
                        sortedTeams = company_teams.sortedByDescending { it.amount_raised }
                    }else if(currentSortBy == 2){
                        sortedTeams = company_teams.sortedByDescending { it.goal }
                    }
                }
            }

            val teamTable = findViewById<FrameLayout>(R.id.manage_company_teams_table_container);
            for (childView in teamTable.children) {
                teamTable.removeView(childView);
            }
            loadTeamRows(sortedTeams);
        }

    }

    fun switchTeamsSlide(newIndex:Int){
        var teamsTableLayout = findViewById<FrameLayout>(R.id.manage_company_teams_table_container);
        switchSlideButton(newIndex + 1,totalTeamSlideCount,R.id.manage_company_teams_slide_buttons)
        if((newIndex >= 0) and (newIndex < totalTeamSlideCount)){
            teamsTableLayout.getChildAt(currentTeamsSlideIndex).setVisibility(View.INVISIBLE);
            teamsTableLayout.getChildAt(newIndex).setVisibility(View.VISIBLE);
            currentTeamsSlideIndex = newIndex;
        }
    }

    fun switchMessageSlide(newIndex:Int){
        switchSlideButton(newIndex + 1,totalMessagesSlideCount,R.id.team_messages_slide_buttons)
        var messageLayout = findViewById<FrameLayout>(R.id.manage_company_team_messages_layout);
        if((newIndex >= 0) and (newIndex < totalMessagesSlideCount)){
            messageLayout.getChildAt(currentMessagesSlideIndex).setVisibility(View.INVISIBLE);
            messageLayout.getChildAt(newIndex).setVisibility(View.VISIBLE);
            currentMessagesSlideIndex = newIndex;
        }
    }

    fun composeMultipleEmail(recipient: Array<String>, subject: String, body: String) {
        try{
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:") // only email apps should handle this
            intent.putExtra(Intent.EXTRA_BCC, recipient)
            intent.putExtra(Intent.EXTRA_SUBJECT, subject)
            intent.putExtra(Intent.EXTRA_TEXT, body)

            startActivity(Intent.createChooser(intent,getResources().getString(R.string.mobile_fundraise_share_dialog_title)))
        }catch(exception: IOException){
            displayAlert(getResources().getString(R.string.mobile_fundraise_share_dialog_error))
        }
    }

    fun sendEmail(address:String, subject: String, message:String){
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:".plus(address))
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT,message)
        startActivity(Intent.createChooser(intent, getResources().getString(R.string.mobile_teams_share_dialog_title)))
    }

    fun displayCompanyInfoAlert(team: CompanyTeam){
        setAlertSender()
        hideKeyboard(this)
        this.runOnUiThread(Runnable {
            val inflater = LayoutInflater.from(this@ManageCompany)

            val alertsContainer = findViewById<LinearLayout>(R.id.alert_container)
            alertsContainer.setVisibility(View.INVISIBLE)
            hideAlertScrollView(true)
            for (childView in alertsContainer.children) {
                alertsContainer.removeView(childView);
            }

            var focusableView: View = alertsContainer;
            val binding: CompanyInfoAlertBinding = DataBindingUtil.inflate(
                inflater, R.layout.company_info_alert, alertsContainer, true)
            binding.colorList = getColorList("")

            val action_button = findViewById<TextView>(R.id.standard_alert_action_button);
            val close_button = findViewById<ImageView>(R.id.standard_alert_cancel_button);

            findViewById<TextView>(R.id.company_info_alert_captain_name).setText(team.captain_name)

            val content = SpannableString(team.team_members_count.toString() + " " + getString(R.string.mobile_company_teams_modal_team_members))
            content.setSpan(StyleSpan(Typeface.BOLD), 0, team.team_members_count.toString().length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            findViewById<TextView>(R.id.company_info_alert_member_count).setText(content)

            action_button.setOnClickListener{
                hideAlert()
            }

            close_button.setOnClickListener{
                hideAlert()
            }

            focusableView = close_button;
            alertsContainer.setVisibility(View.VISIBLE)
            hideAlertScrollView(false)
            focusableView.requestFocus();
            findViewById<ImageView>(R.id.standard_alert_cancel_button).requestFocus();
            Handler().postDelayed({
                findViewById<View>(R.id.company_info_alert_heading).sendAccessibilityEvent(
                    AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }, 175)
        });
    }

    class CompanyTeam(
        val name: String,
        val goal: Double,
        val amount_raised: Double,
        val captain_name: String,
        val captain_id: String,
        var captain_email: String,
        val team_members_count: Int,
    )
}