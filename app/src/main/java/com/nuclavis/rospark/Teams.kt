package com.nuclavis.rospark

import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import app.com.kotlinapp.OnSwipeTouchListener
import com.nuclavis.rospark.databinding.FundraisingMessageBinding
import com.nuclavis.rospark.databinding.TeamRowBinding
import com.nuclavis.rospark.databinding.TeamRowNoEmailBinding
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*

class Teams : BaseActivity() {
    var totalMessagesSlideCount = 0;
    var currentMessagesSlideIndex = 0;
    var team_members = emptyList<Member>();
    var team_emails = emptyArray<String>();
    var totalTeamSlideCount = 0;
    var currentTeamsSlideIndex = 0;
    var currentSortBy = 1;
    var currentSortDir = false;
    var team_messages = emptyList<TeamMessage>();

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        recolorTheme()
        super.onCreate(savedInstanceState)
        setPageContent(R.layout.teams, "teams")
        setTitle(getResources().getString(R.string.mobile_main_menu_teams));
        sendGoogleAnalytics("teams_view","teams")
        loadMessagesData()
        loadTeamData()
        loadTeamEmails()
        findViewById<LinearLayout>(R.id.build_team_card).setVisibility(View.GONE);
        findViewById<LinearLayout>(R.id.teams_card).setVisibility(View.GONE);

        setTooltipText(R.id.teams_help_button,R.string.mobile_teams_team_members_tooltip, R.string.mobile_teams_team_members_title)
        setTooltipText(R.id.messages_help_button,R.string.mobile_teams_team_messages_tooltip, R.string.mobile_teams_team_messages_title)
        setTooltipText(R.id.build_team_help_button,R.string.mobile_teams_no_teams_tooltip, R.string.mobile_teams_no_teams_title)

        var teamsMessageLayout = findViewById<FrameLayout>(R.id.donation_messages_layout);
        val inflater = LayoutInflater.from(this@Teams)
        var i = 0;
        totalMessagesSlideCount = team_messages.count();
        for (message in team_messages) {
            val binding: FundraisingMessageBinding = DataBindingUtil.inflate(
                                inflater, R.layout.fundraising_message ,teamsMessageLayout, true)
                            binding.colorList = getColorList("")
                            val row = teamsMessageLayout.getChildAt(i) as TextView
            row.setText(message.text);
            if(i == 0){
                row.setVisibility(View.VISIBLE);
            } else {
                row.setVisibility(View.INVISIBLE);
            }
            i = i + 1;
        }
        totalMessagesSlideCount = i;

        teamsMessageLayout.setOnTouchListener(object : OnSwipeTouchListener(this@Teams) {
            override fun onSwipeLeft() {
                super.onSwipeLeft();
                switchMessageSlide(currentMessagesSlideIndex + 1);
            }
            override fun onSwipeRight() {
                super.onSwipeRight();
                switchMessageSlide(currentMessagesSlideIndex - 1);
            }
        })

        val teamsMessageCard = findViewById<LinearLayout>(R.id.team_messages_card)
        if (getStringVariable("IS_TEAM_CAPTAIN") == "true") {
            teamsMessageCard.visibility = View.VISIBLE;
            teamsMessageCard.setOnTouchListener(object : OnSwipeTouchListener(this@Teams) {
                override fun onSwipeLeft() {
                    super.onSwipeLeft();
                    switchMessageSlide(currentMessagesSlideIndex + 1);
                }
                override fun onSwipeRight() {
                    super.onSwipeRight();
                    switchMessageSlide(currentMessagesSlideIndex - 1);
                }
            })
        }else{
            teamsMessageCard.visibility = View.GONE;
        }

        val teamTable = findViewById<FrameLayout>(R.id.teams_table_container);
        teamTable.setOnTouchListener(object : OnSwipeTouchListener(this@Teams) {
            override fun onSwipeLeft() {
                super.onSwipeLeft();
                switchTeamsSlide(currentTeamsSlideIndex + 1);
            }
            override fun onSwipeRight() {
                super.onSwipeRight();
                switchTeamsSlide(currentTeamsSlideIndex - 1);
            }
        })

        var table_header_row = findViewById<TableRow>(R.id.teams_table_header_row);

        if(getStringVariable("HIDE_TEAM_EMAIL") == "true"){
            table_header_row = findViewById<TableRow>(R.id.teams_table_header_row_no_email);
        }

        table_header_row.getChildAt(0).setOnClickListener {
            if(currentSortBy == 0){
                currentSortDir = !currentSortDir
            }else{
                currentSortBy = 0;
                currentSortDir = false;
            }
            updateSortArrows(false, table_header_row)
        }

        table_header_row.getChildAt(1).setOnClickListener {
            if(currentSortBy == 1){
                currentSortDir = !currentSortDir
            }else{
                currentSortBy = 1;
                currentSortDir = false;
            }
            updateSortArrows(false, table_header_row)
        }

        table_header_row.getChildAt(2).setOnClickListener {
            if(currentSortBy == 2){
                currentSortDir = !currentSortDir
            }else{
                currentSortBy = 2;
            currentSortDir = false;
            }
            updateSortArrows(false, table_header_row)
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
                composeEmail(team_emails, team_messages[currentMessagesSlideIndex].subject, message)
            }else{
                displayAlert(getResources().getString(R.string.mobile_fundraise_share_dialog_error))
                setAlertSender(emailShareButton)
            }
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

    fun composeEmail(recipient: Array<String>, subject: String, body: String) {
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

    override fun childviewCallback(string: String, data:String){
        hideAlert();
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("team message",team_messages[currentMessagesSlideIndex].text)
        clipboard.setPrimaryClip(clip)

        if(string == "linkedin"){
            shareLinkedIn(Uri.encode(team_messages[currentMessagesSlideIndex].linkedin_url))
        }else if(string == "facebook") {
            shareFacebook(this@Teams, team_messages[currentMessagesSlideIndex].facebook_url)
        }
    }

    fun loadTeamEmails(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getTeamEmails/").plus(getEvent().event_id).plus("/").plus(getStringVariable("TEAM_ID")).plus("/").plus(getConsID())
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
                    val jsonResponse = JSONObject(jsonString)
                    if(jsonResponse.has("data")) {
                        val array = (jsonResponse.get("data") as String).split(';')
                        for(email in array){
                            team_emails += email
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

    fun shareLinkedIn(url: String){
        val url = "https://www.linkedin.com/shareArticle?mini=true&url=".plus(url)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    fun loadMessagesData(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/messages/FULLTEAM/").plus(getConsID()).plus("/").plus(getEvent().event_id).plus("/android/").plus(getDeviceType())
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

                        val message = TeamMessage(
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
                        var donationsMessageLayout = findViewById<FrameLayout>(R.id.donation_messages_layout);
                        val inflater = LayoutInflater.from(this@Teams)
                        var i = 0;
                        totalMessagesSlideCount = team_messages.count();
                        for (message in team_messages) {
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
    }

    fun loadTeamData(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getTeamParticipants/").plus(getEvent().event_id).plus("/").plus(getStringVariable("TEAM_ID")).plus("/").plus(getConsID())
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
                    var memberArray = emptyList<Teams.Member>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i);
                        var email = ""
                        if(obj.has("email")){
                            if(obj.get("email") is String){
                                email = obj.get("email") as String
                            }else{
                                email = ""
                            }
                        }
                        var amount = 0.00;
                        var goal = 0.00;

                        if(obj.has("amount_raised") && obj.get("amount_raised") is Int){
                            amount = amount + (obj.get("amount_raised") as Int)
                            amount = amount.toDouble()
                        } else if(obj.get("amount_raised") is Double){
                            amount = amount + (obj.get("amount_raised") as Double)
                        }

                        if(obj.has("goal") && obj.get("goal") is Int){
                            goal = goal + (obj.get("goal") as Int)
                            goal = goal.toDouble()
                        } else if(obj.get("goal") is Double){
                            goal = goal + (obj.get("goal") as Double)
                        }


                        if(!(email is String)){
                            email = ""
                        }

                        var cons_id = ""
                        var first_name = ""
                        var last_name = ""
                        var team_captain = false

                        if(obj.has("cons_id")){
                            cons_id = obj.get("cons_id") as String
                        }
                        if(obj.has("first_name")){
                            first_name = (obj.get("first_name") as String)
                        }
                        if(obj.has("last_name")){
                            last_name = (obj.get("last_name") as String)
                        }
                        if(obj.has("team_captain")){
                            team_captain = (obj.get("team_captain") as Boolean)
                        }

                        val member = Teams.Member(
                            i,
                            cons_id,
                            first_name,
                            last_name,
                            amount,
                            goal,
                            email,
                            team_captain
                        )
                        memberArray += member;
                    }
                    team_members = memberArray;
                    runOnUiThread(){
                        if (team_members.isEmpty() || (team_members.size == 1 && team_members[0].cons_id == getConsID())) {
                            fadeInView(findViewById<LinearLayout>(R.id.build_team_card))
                        }else{
                            fadeInView(findViewById<LinearLayout>(R.id.teams_card));
                            updateMemberEmails()
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

    fun updateMemberEmails(){
        var finished_students = 0;
        var recheck = true;
        for (i in 0 until team_members.count()) {
            val member = team_members[i];

            if(member.email == ""){
                val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/getParticipantData/").plus(member.cons_id)
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
                            if(jsonObject.has("email")){
                                if(jsonObject.get("email") is String){
                                    updateMemberEmail(member.id, (jsonObject.get("email") as String))
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
            if(finished_students >= team_members.size){
                recheck = false;
                var table_header_row = findViewById<TableRow>(R.id.teams_table_header_row);

                if(getStringVariable("HIDE_TEAM_EMAIL") == "true"){
                    table_header_row = findViewById<TableRow>(R.id.teams_table_header_row_no_email);
                }

                updateSortArrows(true, table_header_row)
            }
        }while(recheck)
    }

    fun updateMemberEmail(id: Int, email: String){
        var member = team_members.find { it.id == id }
        team_members.find { it.id == id }!!.email = email;
    }

    fun loadTeamRows(sortedTeams: List<Member>){
        val teamTable = findViewById<FrameLayout>(R.id.teams_table_container);
        currentTeamsSlideIndex = 0
        for (childView in teamTable.children) {
            teamTable.removeView(childView);
        }

        val inflater = LayoutInflater.from(this@Teams)

        val page_chunks = sortedTeams.chunked(5);
        var j = 0;
        for (chunk in page_chunks){
            if(j <= page_chunks.size - 1){
            val page = inflater.inflate(R.layout.team_table_page, null) as TableLayout
            for (team in chunk) {
                if(getStringVariable("HIDE_TEAM_EMAIL") == "true"){
                    val binding: TeamRowNoEmailBinding = DataBindingUtil.inflate(inflater, R.layout.team_row_no_email, null, true)
                    binding.colorList = getColorList("");
                    val row = binding.root as TableRow
                    loadTeamRow(row, team, page);
                }else{
                    val binding : TeamRowBinding = DataBindingUtil.inflate(inflater, R.layout.team_row, null, true)
                    binding.colorList = getColorList("");
                    val row = binding.root as TableRow
                    loadTeamRow(row, team, page);
                }
            }
            teamTable.addView(page);

            if(j > 0){
                page.setVisibility(View.INVISIBLE)
            }
            j = j + 1;
            }
        }
        totalTeamSlideCount = page_chunks.size;
        setupSlideButtons(totalTeamSlideCount,R.id.teams_slide_buttons,"members")
        if(teamTable.children.count() > totalTeamSlideCount){
            teamTable.removeView(teamTable.getChildAt(0))
        }

        addEmailClickEvents(sortedTeams);
    }

    fun loadTeamRow(row: TableRow, team: Member, page: TableLayout){
        val name_container = row.getChildAt(0) as LinearLayout
        if(team.team_captain){
            (name_container.getChildAt(0) as ImageView).setVisibility(View.VISIBLE)
            (row.getChildAt(3) as LinearLayout).getChildAt(0).setVisibility(View.VISIBLE)
        }else{
            (name_container.getChildAt(0) as ImageView).setVisibility(View.GONE)
            if(getStringVariable("IS_TEAM_CAPTAIN") != "true"){
                (row.getChildAt(3) as LinearLayout).getChildAt(0).setVisibility(View.GONE)
            } 
        }

        (name_container.getChildAt(1) as TextView).setText(team.first_name.plus(" ").plus(team.last_name))
        (row.getChildAt(1) as TextView).setText(formatDoubleToLocalizedCurrency(team.amount))
        (row.getChildAt(2) as TextView).setText(formatDoubleToLocalizedCurrency(team.goal))
        page.addView(row)
    }

    fun getArrowVO(index: Int, sortDir: Boolean) : String{
        var sorts = listOf(
            R.string.mobile_teams_team_members_name_sorted,
            R.string.mobile_teams_team_members_amount_sorted,
            R.string.mobile_teams_team_members_goal_sorted,
        )

        if(sortDir){
            return getString(sorts[index]) + " " + getString(R.string.mobile_teams_team_members_asc)
        }else{
            return getString(sorts[index]) + " " + getString(R.string.mobile_teams_team_members_desc)
        }
    }

    fun updateSortArrows(initial: Boolean, tableHeader: TableRow){
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
                    R.string.mobile_teams_team_members_name_sort,
                    R.string.mobile_teams_team_members_amount_sort,
                    R.string.mobile_teams_team_members_goal_sort,
                )

                arrowContainer.contentDescription = getString(sorts[indx])
                arrowImage.setImageResource(R.drawable.sort_arrows_icon)
            }
            arrowImage.setTintValue(getStringVariable("PRIMARY_COLOR"))
        }

        var sortedTeams = emptyList<Member>();

        if(initial){
            sortedTeams = team_members.sortedByDescending{ it.amount }
        }else{
            if(currentSortDir){
                if(currentSortBy == 0){
                    sortedTeams = team_members.sortedBy { it.last_name }
                }else if (currentSortBy == 1){
                    sortedTeams = team_members.sortedBy { it.amount }
                }else if(currentSortBy == 2){
                    sortedTeams = team_members.sortedBy { it.goal }
                }
            } else{
                if(currentSortBy == 0){
                    sortedTeams = team_members.sortedByDescending { it.last_name }
                }else if (currentSortBy == 1){
                    sortedTeams = team_members.sortedByDescending { it.amount }
                }else if(currentSortBy == 2){
                    sortedTeams = team_members.sortedByDescending { it.goal }
                }
            }
        }

        val teamTable = findViewById<FrameLayout>(R.id.teams_table_container);
        for (childView in teamTable.children) {
            teamTable.removeView(childView);
        }

        runOnUiThread {
            if(getStringVariable("HIDE_TEAM_EMAIL") == "true"){
                findViewById<TableRow>(R.id.teams_table_header_row).visibility = View.GONE
                findViewById<TableRow>(R.id.teams_table_header_row_no_email).visibility = View.VISIBLE
            }else{
                findViewById<TableRow>(R.id.teams_table_header_row).visibility = View.VISIBLE
                findViewById<TableRow>(R.id.teams_table_header_row_no_email).visibility = View.GONE
            }
            loadTeamRows(sortedTeams);
        }

    }

    fun switchTeamsSlide(newIndex:Int){
        var teamsTableLayout = findViewById<FrameLayout>(R.id.teams_table_container);
        switchSlideButton(newIndex + 1,totalTeamSlideCount,R.id.teams_slide_buttons)
        if((newIndex >= 0) and (newIndex < totalTeamSlideCount)){
            teamsTableLayout.getChildAt(currentTeamsSlideIndex).setVisibility(View.INVISIBLE);
            teamsTableLayout.getChildAt(newIndex).setVisibility(View.VISIBLE);
            currentTeamsSlideIndex = newIndex;
        }
    }
    
    fun addEmailClickEvents(sortedTeamMembers:List<Member>){
        val teamTable = findViewById<FrameLayout>(R.id.teams_table_container);
        for(num in 0 .. teamTable.childCount - 1){
            var page = teamTable.getChildAt(num) as LinearLayout;
            for(indx in 0 .. page.childCount - 1){
                var teamId = (num * 5) + indx;
                if(getStringVariable("HIDE_TEAM_EMAIL") != "true"){
                    (page.getChildAt(indx) as TableRow).getChildAt(3).setOnClickListener {
                        sendEmail(sortedTeamMembers[teamId].email,"","");
                    }
                }
            }
        }
    }

    fun switchMessageSlide(newIndex:Int){
        switchSlideButton(newIndex + 1,totalMessagesSlideCount,R.id.team_messages_slide_buttons)
        var donationMessageLayout = findViewById<FrameLayout>(R.id.donation_messages_layout);
        if((newIndex >= 0) and (newIndex < totalMessagesSlideCount)){
            donationMessageLayout.getChildAt(currentMessagesSlideIndex).setVisibility(View.INVISIBLE);
            donationMessageLayout.getChildAt(newIndex).setVisibility(View.VISIBLE);
            currentMessagesSlideIndex = newIndex;
        }
    }

    fun sendEmail(address:String, subject: String, message:String){
         val intent = Intent(Intent.ACTION_SENDTO)
         intent.data = Uri.parse("mailto:".plus(address))
         intent.putExtra(Intent.EXTRA_SUBJECT, subject)
         intent.putExtra(Intent.EXTRA_TEXT,message)
         startActivity(Intent.createChooser(intent, getResources().getString(R.string.mobile_teams_share_dialog_title)))
     }
    
    class Member(
        val id: Int,
        val cons_id: String,
        val first_name: String,
        val last_name: String,
        val amount: Double,
        val goal: Double,
        var email: String,
        val team_captain: Boolean
    )

    class TeamMessage(
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
