package com.nuclavis.rospark

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.os.Handler
import android.view.accessibility.AccessibilityEvent
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
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
import java.util.concurrent.TimeUnit

class ManageSchool : com.nuclavis.rospark.BaseActivity(),View.OnTouchListener, ViewTreeObserver.OnScrollChangedListener  {
    var team_emails = emptyArray<String>();
    var add_student_grade = "";
    var add_student_shirt = "";
    var shirt_items = arrayOf(shirtItem("",""));
    var grade_items = arrayOf(gradeItem("",""));
    var grade_filter_items = arrayOf(gradeItem("", ""))
    var amount_filter_items = arrayOf(filterItem("", ""))
    var badge_items = arrayOf(badgeItem("", ""))
    var badge_status_items = emptyArray<statusItem>();
    var grade_filter_index = 0;
    var amount_filter_index = 0;
    var badge_filter_index = 0;
    var status_filter_index = 0;
    var gift_array = arrayOf<Int>()
    var sort_by_field = "";
    var sort_by_direction = "";
    var gifts_status = 0;
    var advanced_search_last_name = "";
    var advanced_search_teacher = "";
    var advanced_search_grade = "";
    var advanced_search_amount_raised = "";
    var advanced_search_amount_filter = "";
    var advanced_search_badge_type = "";
    var advanced_search_badge_status = "";
    var progressBarWidth = 0;
    var progressBarHeight = 0;

    var students_loaded = false;
    var loading_chunk = false;
    var student_index = 0
    var chunk_size = 4

    var students = listOf<StudentItem>();

    private lateinit var mScrollView: ScrollView

    override fun childviewCallback(string: String, data: String) {
        val url = getResources().getString(R.string.base_server_url).plus("/")
            .plus(getStringVariable("CLIENT_CODE")).plus("/events/getUserEvent/").plus(getConsID())
            .plus("/").plus(getEvent().event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ".plus(getAuth()))
            .addHeader("Program-Id", getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(
            object : Callback {
                override fun onResponse(call: Call, response: okhttp3.Response) {
                    if (response.code != 200) {
                        throw Exception(response.body?.string())
                    } else {
                        val jsonString = response.body?.string();
                        val obj = JSONObject(jsonString);

                        runOnUiThread {
                            if (obj.has("team_percent") && obj.get("team_percent") is Int) {
                                teamProgressPercent = obj.get("team_percent") as Int;
                            } else {
                                teamProgressPercent = 0;
                            }

                            setVariable("TEAM_PROGRESS_PERCENT", teamProgressPercent.toString())

                            if (obj.has("team_goal")) {
                                var newTeamGoal = toDouble(obj.get("team_goal")).toString();
                                if(!newTeamGoal.contains(".")){
                                    newTeamGoal = newTeamGoal + ".00"
                                }else if(newTeamGoal.substring(newTeamGoal.indexOf(".") + 1, newTeamGoal.length).length == 0){
                                    newTeamGoal = newTeamGoal + "00"
                                }else if(newTeamGoal.substring(newTeamGoal.indexOf(".") + 1, newTeamGoal.length).length == 1){
                                    newTeamGoal = newTeamGoal + "0"
                                }
                                setVariable("TEAM_GOAL", newTeamGoal)
                            }

                            if (obj.has("team_raised")) {
                                setVariable(
                                    "TEAM_PROGRESS_RAISED",
                                    withCommas(toDouble(obj.get("team_raised")))
                                )
                            }
                            loadProgressBar()
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    println(e.message.toString());
                }
            })
    }

    var teamProgressPercent: Int by Delegates.observable(0) { _, old, new ->
        run {
            if (old == 0 && new != 0) {
                resizeProgressBar(teamProgressPercent.toDouble() / 100)
            }
        };
    }

    var hide_badge_dropdown = false;
    var hide_delivery_status = false;

    val deliver_all_image = "https://nt-dev-clients.s3.amazonaws.com/ahayouthmarket/custom/FY24/give_all.png";

    override fun onCreate(savedInstanceState: Bundle?) {
        recolorTheme()
        super.onCreate(savedInstanceState)
        setPageContent(R.layout.manage_school, "manageSchool")
        setTitle(getResources().getString(R.string.mobile_main_menu_manage_school));

        mScrollView = findViewById(R.id.manage_school_scroll_view)
        mScrollView.viewTreeObserver.addOnScrollChangedListener(this)

        val view = findViewById<LinearLayout>(R.id.raised_team_bar)
        view.post(Runnable {
            val width: Int = view.getMeasuredWidth()
            val height: Int = view.getMeasuredHeight()
            progressBarWidth = width
            progressBarHeight = height

            try {
                teamProgressPercent = getStringVariable("TEAM_PROGRESS_PERCENT").toInt()
            }catch(e: Exception){
                teamProgressPercent = 0;
            }
            resizeProgressBar(getStringVariable("TEAM_PROGRESS_PERCENT").toInt().toDouble()/100)
        })

        setTooltipText(
            R.id.school_stats_help_button,
            R.string.mobile_manage_school_stats_help,
            R.string.mobile_manage_school_stats_title
        )
        setTooltipText(
            R.id.students_card_help_button,
            R.string.mobile_manage_school_students_help,
            R.string.mobile_manage_school_students_title
        )
        setTooltipText(
            R.id.activities_card_help_button,
            R.string.mobile_manage_school_activities_help,
            R.string.mobile_manage_school_activities_title
        )

        badge_status_items = arrayOf(
            statusItem(0, ""),
            statusItem(
                1,
                getString(R.string.mobile_manage_school_students_advanced_search_modal_not_delivered)
            ),
            statusItem(
                2,
                getString(R.string.mobile_manage_school_students_advanced_search_modal_delivered)
            )
        )

        loadStatsCard()
        loadFilters()
    }

    override fun onScrollChanged() {
        val view = mScrollView.getChildAt(mScrollView.childCount - 1)
        val topDetector = mScrollView.scrollY
        val bottomDetector: Int = view.bottom - (mScrollView.height + mScrollView.scrollY)
        if (bottomDetector == 0) {
            println("SCROLL VIEW BOTTOM REACHED")
            loadStudentBatch(student_index + 1,student_index + chunk_size)
        }
    }

    fun loadProgressBar(){

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

        findViewById<LinearLayout>(R.id.raised_team_progress_bar).background.setColorFilter(Color.parseColor(color), PorterDuff.Mode.SRC_ATOP)

        try {
            teamProgressPercent = getStringVariable("TEAM_PROGRESS_PERCENT").toInt()
        }catch(e: Exception){
            teamProgressPercent = 0;
        }

        resizeProgressBar(teamProgressPercent.toDouble()/100)

        val teamProgressRaisedAmountText = findViewById<TextView>(R.id.team_raised_amount);
        var teamProgressRaised = getStringVariable("TEAM_PROGRESS_RAISED")
        var teamProgressGoal = getStringVariable("TEAM_GOAL")
        if(teamProgressRaised == ""){
            teamProgressRaised = "0.00"
        }
        if(teamProgressGoal == ""){
            teamProgressGoal = "0.00"
        }

        if(!teamProgressGoal.contains(".")){
            teamProgressGoal = teamProgressGoal + ".00"
        }else if(teamProgressGoal.substring(teamProgressGoal.indexOf(".") + 1, teamProgressGoal.length).length == 0){
            teamProgressGoal = teamProgressGoal + "00"
        }else if(teamProgressGoal.substring(teamProgressGoal.indexOf(".") + 1, teamProgressGoal.length).length == 1){
            teamProgressGoal = teamProgressGoal + "0"
        }

        teamProgressRaisedAmountText.setText(getString(R.string.mobile_overview_team_progress_raised).plus(" ").plus(" $").plus(teamProgressRaised))

        val teamProgressRaisedPercentText= findViewById<TextView>(R.id.team_raised_percent);
        teamProgressRaisedPercentText.setText(teamProgressPercent.toString() + "%");

        runOnUiThread{
            val editTeamGoal = findViewById<TextView>(R.id.edit_team_goal);
            editTeamGoal.setOnClickListener{
                displayAlert("updateTeamGoal", toDouble(teamProgressGoal))
                setAlertSender(editTeamGoal)
            }

            val teamProgressRaisedGoalText = findViewById<TextView>(R.id.team_raised_goal);
            teamProgressRaisedGoalText.setText(getString(R.string.mobile_overview_team_progress_goal).plus(" ").plus(" $").plus(teamProgressGoal));
        }
    }

    fun resizeProgressBar(percent: Double){
        runOnUiThread {
            var newBarWidth = progressBarHeight;
            if((percent * progressBarWidth) > progressBarHeight){
                newBarWidth = (percent * progressBarWidth).toInt()
            }else if(percent == 0.00){
                newBarWidth = 0
            }

            val teamRaisedProgress = findViewById<LinearLayout>(R.id.raised_team_progress_bar);
            teamRaisedProgress.layoutParams = FrameLayout.LayoutParams(newBarWidth, progressBarHeight)
        }
    }

    fun loadFilters(){

        findViewById<EditText>(R.id.students_search_last_name_input).addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                val str = s.toString().trim()
                advanced_search_last_name = str.capitalize()
            }
        })

        findViewById<LinearLayout>(R.id.students_card_search_button).setOnClickListener{
            findViewById<EditText>(R.id.students_search_last_name_input).setText(advanced_search_last_name)
            advanced_search_teacher = "";
            advanced_search_grade = "";
            advanced_search_amount_raised = "";
            advanced_search_amount_filter = "";
            advanced_search_badge_type = "";
            advanced_search_badge_status = "";
            removeAllChildren(findViewById(R.id.manage_school_students_container),
                { loadStudentsCard(sort_by_field,sort_by_direction)})
        }

        findViewById<Button>(R.id.btn_gift_status_toggle).setOnClickListener{

            if(gifts_status == 0){
                gifts_status = 1
                findViewById<Button>(R.id.btn_gift_status_toggle).setText(R.string.mobile_manage_school_students_view_all_gifts)
            }else{
                gifts_status = 0
                findViewById<Button>(R.id.btn_gift_status_toggle).setText(R.string.mobile_manage_school_students_view_undelivered_gifts)
            }

            removeAllChildren(findViewById(R.id.manage_school_students_container),{ loadStudentsCard(sort_by_field,sort_by_direction)})
        }

        findViewById<Button>(R.id.btn_advanced_search).setOnClickListener {
            showAdvancedSearchModal()
        }

        loadFilterItems()
    }

    fun loadSortBy(){

        val url = getResources().getString(R.string.base_server_url).plus("/")
            .plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/studentSorts")

        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ".plus(getAuth()))
            .addHeader("Program-Id", getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient();
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.code != 200) {
                    throw Exception(response.body?.string())
                } else {
                    val jsonString = response.body?.string();
                    val jsonResponse = JSONObject(jsonString)
                    var fields = emptyList<String>()
                    var orders = emptyList<String>()
                    if (jsonResponse.has("data")) {
                        val data = jsonResponse.get("data") as JSONObject
                        if (data.has("fields")) {
                            val fields_array = data.get("fields") as JSONArray
                            for (i in 0 until fields_array.length()) {
                                fields += fields_array[i] as String
                            }
                        }

                        if (data.has("sort_orders")) {
                            val order_array = data.get("sort_orders") as JSONArray
                            for (i in 0 until order_array.length()) {
                                orders += order_array[i] as String
                            }
                        }
                    }

                    if (fields.count() > 0 && orders.count() > 0) {
                        sort_by_field = fields[0]
                        sort_by_direction = orders[0]
                    } else {
                        sort_by_field = "Student Last Name"
                        sort_by_direction = "Descending"
                    }


                    val fields_adapter = ArrayAdapter(
                        this@ManageSchool,
                        android.R.layout.simple_spinner_dropdown_item,
                        fields
                    )
                    val order_adapter = ArrayAdapter(
                        this@ManageSchool,
                        android.R.layout.simple_spinner_dropdown_item,
                        orders
                    )
                    findViewById<Spinner>(R.id.sort_by_field_spinner).setAdapter(
                        fields_adapter
                    )
                    findViewById<Spinner>(R.id.sort_by_sort_dir_spinner).setAdapter(
                        order_adapter
                    )

                    findViewById<Spinner>(R.id.sort_by_field_spinner).setOnItemSelectedListener(
                        object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parentView: AdapterView<*>?,
                                selectedItemView: View?,
                                position: Int,
                                id: Long
                            ) {
                                sort_by_field = fields[position]
                                findViewById<Spinner>(R.id.sort_by_field_spinner).requestFocus()
                            }

                            override fun onNothingSelected(parentView: AdapterView<*>?) {
                                // your code here
                            }
                        })

                    findViewById<Spinner>(R.id.sort_by_sort_dir_spinner).setOnItemSelectedListener(
                        object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parentView: AdapterView<*>?,
                                selectedItemView: View?,
                                position: Int,
                                id: Long
                            ) {
                                sort_by_direction = orders[position]
                                findViewById<Spinner>(R.id.sort_by_sort_dir_spinner).requestFocus()
                            }

                            override fun onNothingSelected(parentView: AdapterView<*>?) {
                                // your code here
                            }
                        })
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                //displayAlert(getResources().getString(R.string.mobile_overview_edit_goal_error), hideAlert())
                //hideAlert()
            }
        })

        runOnUiThread {
            findViewById<ImageView>(R.id.sort_by_expand_button).setOnClickListener {
                if(findViewById<LinearLayout>(R.id.sort_by_expanded_container).visibility == View.VISIBLE){
                    findViewById<LinearLayout>(R.id.sort_by_expanded_container).setVisibility(View.GONE)
                    findViewById<ImageView>(R.id.sort_by_expand_button).setVisibility(View.VISIBLE)
                }else{
                    findViewById<LinearLayout>(R.id.sort_by_expanded_container).setVisibility(View.VISIBLE)
                    findViewById<ImageView>(R.id.sort_by_expand_button).setVisibility(View.GONE)
                }
            }

            findViewById<TextView>(R.id.sort_by_close_button).setOnClickListener {
                findViewById<LinearLayout>(R.id.sort_by_expanded_container).setVisibility(View.GONE)
                findViewById<ImageView>(R.id.sort_by_expand_button).setVisibility(View.VISIBLE)
                findViewById<ImageView>(R.id.sort_by_expand_button).requestFocus()
            }

            findViewById<Button>(R.id.sort_by_sort_button).setOnClickListener {
                removeAllChildren(findViewById(R.id.manage_school_students_container),
                    { loadStudentsCard(sort_by_field, sort_by_direction) })
            }
        }
    }

    fun loadStudentsCard(sort_field:String, sort_dir: String){

        students = listOf<StudentItem>()
        student_index = 0;
        students_loaded = false;

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/students")
        val formBody = FormBody.Builder()
            .add("cons_id", getConsID())
            .add("event_id", getEvent().event_id)
            .add("sort_field",sort_field)
            .add("sort_order",sort_dir)

            if(gifts_status == 1 || gifts_status == 2){
                formBody.add("delivered_status", gifts_status.toString())
            }

            if(advanced_search_last_name != ""){
                formBody.add("last_name",advanced_search_last_name)
            }

            if(advanced_search_teacher != ""){
                formBody.add("teacher",advanced_search_teacher)
            }

            if(advanced_search_grade != ""){
                formBody.add("grade",advanced_search_grade)
            }

            if(advanced_search_amount_raised != ""){
                formBody.add("amount_raised",advanced_search_amount_raised)
            }

            if(advanced_search_amount_filter != ""){
                formBody.add("amount_filter",advanced_search_amount_filter)
            }

            if(advanced_search_badge_type != ""){
                formBody.add("badge_type",advanced_search_badge_type)
            }

        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .post(formBody.build())
            .build()

        var client = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    val jsonString = response.body?.string();
                    val jsonResponse = JSONObject(jsonString)
                    if(jsonResponse.has("data")){
                        val data = jsonResponse.get("data") as JSONObject
                        if(data.has("students")){
                            val jsonArray = data.get("students") as JSONArray
                            println("LENGTH: " + jsonArray.length())
                            if(jsonArray.length() > 0) {
                                for (i in 0 until jsonArray.length()) {
                                    val student = jsonArray.getJSONObject(i);
                                    var id = 0;
                                    var name = ""
                                    var teacher = ""
                                    var grade = ""
                                    var amount = "$0.00"
                                    var email = ""

                                    if(student.has("student_event_id") && student.get("student_event_id") is Int){
                                        id = student.get("student_event_id") as Int
                                    }

                                    if(student.has("first_name") && student.get("first_name") is String){
                                        name = student.get("first_name") as String
                                    }

                                    if(student.has("last_name") && student.get("last_name") is String){
                                        name += " " + student.get("last_name") as String
                                    }

                                    if(student.has("teacher") && student.get("teacher") is String){
                                        teacher = student.get("teacher") as String
                                    }

                                    if(student.has("grade") && student.get("grade") is String){
                                        grade = student.get("grade") as String
                                    }

                                    if(student.has("amount") && student.get("amount") is String){
                                        amount = student.get("amount") as String
                                    }

                                    if(student.has("email") && student.get("email") is String){
                                        email = student.get("email") as String
                                    }

                                    var gifts = listOf<StudentGift>()

                                    if(student.has("gifts") && student.get("gifts") is JSONArray){
                                        val gift_array = student.get("gifts") as JSONArray;

                                        for (i in 0 until gift_array.length()) {
                                            var gift = gift_array.get(i) as JSONObject
                                            var gift_id = 0
                                            var type = ""
                                            var status = false
                                            var gift_name = ""
                                            var date = ""
                                            var delivered_image_url = ""
                                            var undelivered_image_url = ""

                                            if(gift.has("student_gift_id") && gift.get("student_gift_id") is Int){
                                                gift_id = gift.get("student_gift_id") as Int
                                            }

                                            if(gift.has("gift_type_id") && gift.get("gift_type_id") is String){
                                                type = gift.get("gift_type_id") as String
                                            }

                                            if(gift.has("delivered_status") && gift.get("delivered_status") is Boolean && gift.get("delivered_status") as Boolean){
                                                status = true;
                                            }

                                            if(gift.has("gift_name") && gift.get("gift_name") is String){
                                                gift_name = gift.get("gift_name") as String
                                            }

                                            if(gift.has("earned_image") && gift.get("earned_image") is String){
                                                delivered_image_url = gift.get("earned_image") as String
                                            }

                                            if(gift.has("unearned_image") && gift.get("unearned_image") is String){
                                                undelivered_image_url = gift.get("unearned_image") as String
                                            }

                                            if(gift.has("created_at") && gift.get("created_at") is String){
                                                val date_string = gift.get("created_at") as String;
                                                val year = date_string.substring(0,4)
                                                val month = date_string.substring(5,7)
                                                val day = date_string.substring(8,10)

                                                date = month + "/" + day + "/" + year
                                            }

                                            gifts += StudentGift(gift_id,type,status,gift_name,date,delivered_image_url,undelivered_image_url)
                                        }
                                    
                                    }

                                    students += StudentItem(id,name,teacher,grade,gifts,email,amount)
                                }
                                loadStudentRows()
                            }
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

    fun loadStudentBatch(start_index:Int, end_index: Int){
        println("STARTING INDEX: " + start_index)
        println("END INDEX: " + end_index)
        if(students_loaded && !loading_chunk){
            loading_chunk = true
            for (i in start_index until end_index) {
                if(i < students.count()){
                    val student = students[i];
                    val container = findViewById<LinearLayout>(R.id.manage_school_students_container)
                    AsyncLayoutInflater(this).inflate(
                        R.layout.manage_school_student, container
                    ) { view, resid, parent ->
                        val binding: ManageSchoolStudentBinding? = DataBindingUtil.bind(view)
                        if (binding != null) {
                            binding.colorList = getColorList("")
                            val root = (binding.root as LinearLayout).getChildAt(0) as LinearLayout
                            val info_container = root.getChildAt(0) as LinearLayout
                            val badge_container = (binding.root as LinearLayout).getChildAt(1) as LinearLayout
                            val button_container = root.getChildAt(1) as LinearLayout
                            (info_container.getChildAt(0) as TextView).setText(student.name)
                            ((info_container.getChildAt(1) as LinearLayout).getChildAt(1) as TextView).setText(student.teacher)
                            ((info_container.getChildAt(2) as LinearLayout).getChildAt(1) as TextView).setText(student.grade)
                            ((info_container.getChildAt(3) as LinearLayout).getChildAt(1) as TextView).setText(student.amount)

                            loadBadgeRows(student.gifts, badge_container, student.id, root )

                            button_container.getChildAt(0).setOnClickListener{
                                val intent = Intent(Intent.ACTION_SENDTO)
                                intent.data = Uri.parse("mailto:".plus(student.email))
                                intent.putExtra(Intent.EXTRA_SUBJECT, "")
                                intent.putExtra(Intent.EXTRA_TEXT,"")
                                startActivity(Intent.createChooser(intent, getResources().getString(R.string.mobile_teams_share_dialog_title)))
                            }

                            button_container.getChildAt(1).setOnClickListener{
                                showDonationModal(button_container.getChildAt(1),student.id,student.name,student.teacher,student.grade,((info_container.getChildAt(3) as LinearLayout).getChildAt(1) as TextView),badge_container)
                            }
                            container.addView(view)
                        }
                    }
                    student_index = i
                }
            }
            loading_chunk = false;
        }
    }

    fun loadStudentRows(){
        students_loaded = true
        runOnUiThread{
            loadStudentBatch(0,chunk_size)
        }
    }

    fun loadBadgeRows(gifts_array: List<StudentGift>,badge_container:LinearLayout, id: Int, student_container: LinearLayout){
        val inflater = layoutInflater

        runOnUiThread {
            var has_undelivered_gift = false;
            var gifts = gifts_array

            for (gift in gifts_array) {
                if (!gift.delivered_status) {
                    has_undelivered_gift = true
                }
            }


            if(gifts_status == 1 && !has_undelivered_gift){
                val all_students_container = student_container.parent

                (all_students_container.getParent() as ViewGroup).removeView(
                    all_students_container as View
                )
            }else{
                if (has_undelivered_gift) {
                    val newGiftArray =
                        listOf(StudentGift(0, "deliver_all", true, "", "", "", "")) + gifts
                    gifts = newGiftArray
                }

                val chunks = gifts.chunked(4)
                var j = 0;
                for (chunk in chunks) {
                    val row = (inflater.inflate(
                        R.layout.manage_school_student_badge_row,
                        badge_container,
                        true
                    ) as LinearLayout).getChildAt(j) as LinearLayout

                    for (i in 0 until 4) {
                        val badge = (inflater.inflate(
                            R.layout.manage_school_student_badge,
                            row,
                            true
                        ) as LinearLayout).getChildAt(i) as LinearLayout
                        if (i < chunk.count()) {
                            val gift = chunk[i]
                            val img =
                                badge.getChildAt(0) as ImageView
                            val text =
                                badge.getChildAt(1) as TextView
                            val status_text =
                                badge.getChildAt(2) as TextView

                            if (gift.delivered_status) {
                                status_text.text = "2"
                            } else {
                                status_text.text = "1"
                            }

                            var img_url = gift.undelivered_image_url
                            var text_content = ""

                            if (gift.gift_type == "deliver_all") {
                                img.contentDescription = getString(R.string.mobile_manage_school_students_deliver_all)
                                img_url = deliver_all_image
                                text_content =
                                    getString(R.string.mobile_manage_school_students_deliver_all)

                                img.setOnClickListener {
                                    gift_array = arrayOf()
                                    for (gift in gifts) {
                                        if (!gift.delivered_status) {
                                            gift_array += gift.gift_id
                                        }
                                    }
                                    changeGiftStatus(gift_array, true , id, badge_container, gifts, student_container)
                                }
                            } else {
                                if (gift.delivered_status) {
                                    img_url = gift.delivered_image_url
                                    img.contentDescription = gift.badge_name + " " + getString(R.string.mobile_manage_school_students_advanced_search_modal_delivered)
                                } else {
                                    img_url = gift.undelivered_image_url
                                    img.contentDescription = gift.badge_name + " " + getString(R.string.mobile_manage_school_students_advanced_search_modal_not_delivered)
                                }

                                text_content = gift.gift_date

                                img.setOnClickListener {
                                    var new_status = true
                                    if (status_text.text == "2") {
                                        new_status = false
                                    }
                                    changeGiftStatus(
                                        arrayOf(gift.gift_id),
                                        new_status,
                                        id,
                                        badge_container,
                                        gifts,
                                        student_container
                                    )
                                }
                            }

                            text.setText(text_content)

                            Glide.with(this@ManageSchool)
                                .load(img_url)
                                .into(img)
                        } else {
                            badge.setVisibility(View.INVISIBLE)
                        }
                    }
                    j+= 1
                }
            }
        }
    }

    fun changeGiftStatus(gifts: Array<Int>,status: Boolean,student_id: Int, badge_container: LinearLayout, all_gifts: List<StudentGift>, student_container: LinearLayout){
        var id_array = arrayOf<Int>()
        var id_string = "["
        for(item in gifts){
            id_array += item
            id_string += item.toString() + ","
        }
        id_string = id_string.substring(0,id_string.length - 1)
        id_string += "]"

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/giftstatus")
        var json = "{\"event_id\":${getEvent().event_id.toInt()}"
        json += ",\"gift_status\":\"${status}\""
        json += ",\"gift_ids\":${id_string}"
        json += "}"

        json = json.replace("\n","")

        val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body: RequestBody = json.toRequestBody(JSON)
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .post(body)
            .build()

        var client = OkHttpClient();
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    runOnUiThread{
                        val jsonString = response.body?.string();
                        val jsonResponse = JSONObject(jsonString)
                        if(jsonResponse.has("success") && jsonResponse.get("success") == true){
                            val new_status = status
                            var new_gift_array = listOf<StudentGift>()
                            for(gift in all_gifts){
                                var copy = gift
                                if(gifts.contains(copy.gift_id)){
                                    copy.delivered_status = new_status
                                }
                                if(copy.gift_type != "deliver_all") {
                                    new_gift_array += copy
                                }
                            }
                            removeAllChildren(badge_container,
                                { loadBadgeRows(new_gift_array,badge_container,student_id,student_container) })

                        }else{
                            displayAlert(getString(R.string.mobile_manage_school_students_update_gift_status_error))
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                displayAlert(getString(R.string.mobile_manage_school_students_update_gift_status_error))
            }
        })
    }

    fun loadStatsCard(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/schoolstats/").plus(getStringVariable("TEAM_ID")).plus("/").plus(getEvent().event_id)
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
                    if(jsonResponse.has("data")){
                        val data = jsonResponse.get("data") as JSONObject
                        runOnUiThread {
                            if(data.has("display_badge_dropdown") && data.get("display_badge_dropdown") is Int){
                                if(data.get("display_badge_dropdown") as Int == 0){
                                    hide_badge_dropdown = true
                                }else{
                                    hide_badge_dropdown = false
                                }
                            }else{
                                hide_badge_dropdown = false
                            }

                            if(data.has("display_delivery_status") && data.get("display_delivery_status") is Int){
                                if(data.get("display_delivery_status") as Int == 0){
                                    hide_delivery_status = true
                                    findViewById<Button>(R.id.btn_gift_status_toggle).setVisibility(View.GONE)
                                }else{
                                    hide_delivery_status = false
                                    findViewById<Button>(R.id.btn_gift_status_toggle).setVisibility(View.VISIBLE)
                                }
                            }else{
                                hide_delivery_status = false
                                findViewById<Button>(R.id.btn_gift_status_toggle).setVisibility(View.VISIBLE)
                            }

                            if (data.has("students_registered") && data.get("students_registered") is Int) {
                                findViewById<TextView>(R.id.students_registered_circle).setText(intWithCommas((data.get("students_registered") as Int)))
                            }

                            if (data.has("students_raising_money") && data.get("students_raising_money") is Int) {
                                findViewById<TextView>(R.id.students_raising_circle).setText(intWithCommas((data.get("students_raising_money") as Int)))
                            }

                            if (data.has("students_completed_finns_mission") && data.get("students_completed_finns_mission") is Int) {
                                findViewById<TextView>(R.id.completed_finns_circle).setText(intWithCommas((data.get("students_completed_finns_mission") as Int)))
                            }
                        }
                        loadProgressBar()
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

    fun showAdvancedSearchModal(){
        var temp_grade_filter_index = 0;
        var temp_amount_filter_index = 0;
        var temp_badge_filter_index = 0;
        var temp_status_filter_index = 0;

        val inflater = LayoutInflater.from(this@ManageSchool)

        val alertsContainer = findViewById<LinearLayout>(R.id.alert_container)
        alertsContainer.setVisibility(View.INVISIBLE)
        hideAlertScrollView(true)

        for (childView in alertsContainer.children) {
            alertsContainer.removeView(childView);
        }

        val binding: AdvancedSearchAlertBinding = DataBindingUtil.inflate(
            inflater, R.layout.advanced_search_alert, alertsContainer, true)
        binding.colorList = getColorList("")

        findViewById<EditText>(R.id.advanced_search_last_name_input).setText(advanced_search_last_name)
        findViewById<EditText>(R.id.advanced_search_teacher_input).setText(advanced_search_teacher)
        findViewById<EditText>(R.id.advanced_search_amount_raised_input).setText(advanced_search_amount_raised)

        findViewById<EditText>(R.id.advanced_search_amount_raised_input).addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                val str = s.toString().trim()
                var valid = isValidCurrency(str);

                if(valid){
                    findViewById<TextView>(R.id.amount_raised_error_message).visibility = View.GONE
                }else{
                    findViewById<TextView>(R.id.amount_raised_error_message).visibility = View.VISIBLE
                }
                checkCanSearch()
            }
        })

        var filter_items_array = emptyArray<String>()

        for (item in amount_filter_items){
            filter_items_array += item.filter_name
        }

        val filter_adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, filter_items_array)
        findViewById<Spinner>(R.id.advanced_search_amount_filter_spinner).setAdapter(filter_adapter)
        findViewById<Spinner>(R.id.advanced_search_amount_filter_spinner).setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                temp_amount_filter_index = position
                advanced_search_amount_filter = amount_filter_items[position].filter_id
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        })

        if(hide_badge_dropdown) {
            findViewById<LinearLayout>(R.id.advanced_search_badge_type_spinner_container).setVisibility(View.GONE)
        } else {
            var badge_items_array = emptyArray<String>()

            for (item in badge_items) {
                badge_items_array += item.badge_name
            }

            val badge_adapter =
                ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, badge_items_array)
            findViewById<Spinner>(R.id.advanced_search_badge_type_spinner).setAdapter(badge_adapter)
            findViewById<Spinner>(R.id.advanced_search_badge_type_spinner).setOnItemSelectedListener(
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parentView: AdapterView<*>?,
                        selectedItemView: View?,
                        position: Int,
                        id: Long
                    ) {
                        temp_badge_filter_index = position
                        advanced_search_badge_type = badge_items[position].badge_id
                    }

                    override fun onNothingSelected(parentView: AdapterView<*>?) {}
                }
            )
        }

        var grade_items_array = emptyArray<String>()

        for (item in grade_filter_items){
            grade_items_array += item.grade_name
        }

        val grade_adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, grade_items_array)
        findViewById<Spinner>(R.id.advanced_search_grade_spinner).setAdapter(grade_adapter)
        findViewById<Spinner>(R.id.advanced_search_grade_spinner).setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                temp_grade_filter_index = position
                advanced_search_grade = grade_filter_items[position].grade_id
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        })

        if(hide_delivery_status){
            findViewById<LinearLayout>(R.id.advanced_search_badge_status_spinner_container).setVisibility(View.GONE)
        }else{
            var status_items_array = emptyArray<String>()

            for (item in badge_status_items){
                status_items_array += item.status_name
            }

            val status_adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, status_items_array)
            findViewById<Spinner>(R.id.advanced_search_badge_status_spinner).setAdapter(status_adapter)
            findViewById<Spinner>(R.id.advanced_search_badge_status_spinner).setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parentView: AdapterView<*>?,
                    selectedItemView: View?,
                    position: Int,
                    id: Long
                ) {
                    temp_status_filter_index = position
                    gifts_status = badge_status_items[position].status_id
                    if(gifts_status == 0){
                        findViewById<Button>(R.id.btn_gift_status_toggle).setText(R.string.mobile_manage_school_students_view_undelivered_gifts)
                    }else{
                        findViewById<Button>(R.id.btn_gift_status_toggle).setText(R.string.mobile_manage_school_students_view_all_gifts)
                    }
                }

                override fun onNothingSelected(parentView: AdapterView<*>?) {}
            })

            findViewById<Spinner>(R.id.advanced_search_badge_status_spinner).setSelection(status_filter_index)
        }

        findViewById<Spinner>(R.id.advanced_search_amount_filter_spinner).setSelection(amount_filter_index)
        findViewById<Spinner>(R.id.advanced_search_grade_spinner).setSelection(grade_filter_index)
        findViewById<Spinner>(R.id.advanced_search_badge_type_spinner).setSelection(badge_filter_index)

        val search_button = findViewById<TextView>(R.id.advanced_search_alert_search_button);
        search_button.setOnClickListener{
            if(checkCanSearch()){
                grade_filter_index = temp_grade_filter_index
                amount_filter_index = temp_amount_filter_index
                badge_filter_index = temp_badge_filter_index;
                status_filter_index = temp_status_filter_index;
                advanced_search_last_name = findViewById<EditText>(R.id.advanced_search_last_name_input).text.toString().capitalize()
                findViewById<EditText>(R.id.advanced_search_last_name_input).setText(advanced_search_last_name)
                findViewById<EditText>(R.id.students_search_last_name_input).setText(advanced_search_last_name)
                advanced_search_teacher = findViewById<EditText>(R.id.advanced_search_teacher_input).text.toString()
                advanced_search_amount_raised = findViewById<EditText>(R.id.advanced_search_amount_raised_input).text.toString()
                hideAlert()
                findViewById<Button>(R.id.btn_advanced_search).requestFocus()
                removeAllChildren(findViewById(R.id.manage_school_students_container),
                    { loadStudentsCard(sort_by_field,sort_by_direction)})
            }
        }

        val close_button = findViewById<ImageView>(R.id.advanced_search_alert_close_button);
        close_button.setOnClickListener{
            hideAlert()
            findViewById<Button>(R.id.btn_advanced_search).requestFocus()
        }

        alertsContainer.setVisibility(View.VISIBLE)
        hideAlertScrollView(false)
        close_button.requestFocus()
        findViewById<ImageView>(R.id.advanced_search_alert_close_button).requestFocus();
        Handler().postDelayed({
            findViewById<View>(R.id.advanced_search_alert_heading).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        }, 175)
    }

    fun showDonationModal(button: View, id: Int, name:String, teacher: String, grade:String, amount_text: TextView, badge_container: LinearLayout){
        val inflater = LayoutInflater.from(this@ManageSchool)

        val alertsContainer = findViewById<LinearLayout>(R.id.alert_container)
        alertsContainer.setVisibility(View.INVISIBLE)
        hideAlertScrollView(true)
        for (childView in alertsContainer.children) {
            alertsContainer.removeView(childView);
        }

        val binding: AddDonationAlertBinding = DataBindingUtil.inflate(
            inflater, R.layout.add_donation_alert, alertsContainer, true)
        binding.colorList = getColorList("")

        findViewById<TextView>(R.id.add_donation_student_name).setText(name.uppercase())
        findViewById<TextView>(R.id.add_donation_student_teacher).setText(teacher.uppercase())
        findViewById<TextView>(R.id.add_donation_student_grade).setText(grade.uppercase())

        checkCanAddDonation()

        findViewById<EditText>(R.id.donation_offline_cash_input).addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                val str = s.toString().trim()
                var valid = isValidCurrency(str);

                if(valid){
                    findViewById<TextView>(R.id.add_donation_cash_error_message).visibility = View.GONE
                }else{
                    findViewById<TextView>(R.id.add_donation_cash_error_message).visibility = View.VISIBLE
                }

                checkCanAddDonation()
            }
        })

        findViewById<EditText>(R.id.donation_offline_check_input).addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                val str = s.toString().trim()

                var valid = isValidCurrency(str);

                if(valid){
                    findViewById<TextView>(R.id.add_donation_check_error_message).visibility = View.GONE
                }else{
                    findViewById<TextView>(R.id.add_donation_check_error_message).visibility = View.VISIBLE
                }

                checkCanAddDonation()
            }
        })

        findViewById<Button>(R.id.add_donation_save_button).setOnClickListener{
            if(checkCanAddDonation()){

                var cash = findViewById<EditText>(R.id.donation_offline_cash_input).text.toString()
                var check = findViewById<EditText>(R.id.donation_offline_check_input).text.toString()

                if(cash == ""){
                    cash = "0.00"
                }else{
                    if(!cash.contains(".")){
                        cash = cash + ".00"
                    }else if(cash.substring(cash.indexOf(".") + 1, cash.length).length == 0){
                        cash = cash + "00"
                    }else if(cash.substring(cash.indexOf(".") + 1, cash.length).length == 1){
                        cash = cash + "0"
                    }
                }

                if(check == ""){
                    check = "0.00"
                }else{
                    if(!check.contains(".")){
                        check = check + ".00"
                    }else if(check.substring(check.indexOf(".") + 1, check.length).length == 0){
                        check = check + "00"
                    }else if(check.substring(check.indexOf(".") + 1, check.length).length == 1){
                        check = check + "0"
                    }
                }

                findViewById<EditText>(R.id.donation_offline_cash_input).setText(cash)
                findViewById<EditText>(R.id.donation_offline_check_input).setText(check)

                val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/addDonation");
                val formBody = FormBody.Builder()
                    .add("event_id", getEvent().event_id)
                    .add("cash_total", cash)
                    .add("checks_total", check)
                    .add("student_id", id.toString())
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
                            displayAlert(getString(R.string.mobile_manage_school_students_add_donation_modal_error))
                            //throw Exception(response.body?.string())
                        }else{
                            val response = response.body?.string();
                            val json = JSONObject(response);
                            if(json.has("success") && json.get("success") is Boolean && json.get("success") as Boolean){
                                hideAlert()
                                runOnUiThread {
                                    removeAllChildren(findViewById(R.id.manage_school_students_container),
                                        { loadStudentsCard(sort_by_field,sort_by_direction)})
                                }

                            }else{
                                hideAlert()
                                displayAlert(getString(R.string.mobile_manage_school_students_add_donation_modal_error),{button.requestFocus()})
                            }
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        println(e.message.toString())
                    }
                })
            }
        }

        val close_button = findViewById<ImageView>(R.id.add_donation_alert_close_button);
        close_button.setOnClickListener{
            hideAlert()
            button.requestFocus()
        }

        val cancel_button = findViewById<TextView>(R.id.add_donation_cancel_button);
        cancel_button.setOnClickListener{
            hideAlert()
            button.requestFocus()
        }

        alertsContainer.setVisibility(View.VISIBLE)
        hideAlertScrollView(false)
        close_button.requestFocus()
        findViewById<ImageView>(R.id.add_donation_alert_close_button).requestFocus();
        Handler().postDelayed({
            findViewById<View>(R.id.add_donation_alert_heading).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        }, 175)
    }

    fun showStudentModal(){
        val inflater = LayoutInflater.from(this@ManageSchool)

        val alertsContainer = findViewById<LinearLayout>(R.id.alert_container)
        alertsContainer.setVisibility(View.INVISIBLE)
        hideAlertScrollView(true)
        for (childView in alertsContainer.children) {
            alertsContainer.removeView(childView);
        }

        val binding: AddStudentAlertBinding = DataBindingUtil.inflate(
            inflater, R.layout.add_student_alert, alertsContainer, true)
        binding.colorList = getColorList("")

        checkCanAddStudent()

        findViewById<EditText>(R.id.student_first_name_input).addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                checkCanAddStudent()
            }
        })

        findViewById<EditText>(R.id.student_last_name_input).addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                checkCanAddStudent()
            }
        })

        findViewById<EditText>(R.id.student_teacher_input).addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                checkCanAddStudent()
            }
        })

        findViewById<EditText>(R.id.student_offline_cash_input).addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                val str = s.toString().trim()
                var valid = isValidCurrency(str);

                if(valid){
                    findViewById<TextView>(R.id.add_student_cash_error_message).visibility = View.GONE
                }else{
                    findViewById<TextView>(R.id.add_student_cash_error_message).visibility = View.VISIBLE
                    checkCanAddStudent()
                }
            }
        })

        findViewById<EditText>(R.id.student_offline_check_input).addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                val str = s.toString().trim()

                var valid = isValidCurrency(str);

                if(valid){
                    findViewById<TextView>(R.id.add_student_check_error_message).visibility = View.GONE
                }else{
                    findViewById<TextView>(R.id.add_student_check_error_message).visibility = View.VISIBLE
                    checkCanAddStudent()
                }
            }
        })

        var grade_items_array = emptyArray<String>()
        var shirt_items_array = emptyArray<String>()

        for (item in grade_items){
            grade_items_array += item.grade_name
        }

        for (item in shirt_items){
            shirt_items_array += item.tshirt_name
        }

        val grade_adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, grade_items_array)
        val shirt_adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shirt_items_array)
        findViewById<Spinner>(R.id.student_grade_spinner).setAdapter(grade_adapter)
        findViewById<Spinner>(R.id.select_tshirt_spinner).setAdapter(shirt_adapter)

        findViewById<Spinner>(R.id.student_grade_spinner).setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                add_student_grade = grade_items[position].grade_id
                checkCanAddStudent()
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        })

        findViewById<Spinner>(R.id.select_tshirt_spinner).setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                add_student_shirt = shirt_items[position].tshirt_id
                checkCanAddStudent()
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        })

        findViewById<Button>(R.id.add_student_save_button).setOnClickListener{
            if(checkCanAddStudent()){

                var cash = findViewById<EditText>(R.id.student_offline_cash_input).text.toString()
                var check = findViewById<EditText>(R.id.student_offline_check_input).text.toString()

                if(cash == ""){
                    cash = "0.00"
                }else{
                    if(!cash.contains(".")){
                        cash = cash + ".00"
                    }else if(cash.substring(cash.indexOf(".") + 1, cash.length).length == 0){
                        cash = cash + "00"
                    }else if(cash.substring(cash.indexOf(".") + 1, cash.length).length == 1){
                        cash = cash + "0"
                    }
                }

                if(check == ""){
                    check = "0.00"
                }else{
                    if(!check.contains(".")){
                        check = check + ".00"
                    }else if(check.substring(check.indexOf(".") + 1, check.length).length == 0){
                        check = check + "00"
                    }else if(check.substring(check.indexOf(".") + 1, check.length).length == 1){
                        check = check + "0"
                    }
                }

                findViewById<EditText>(R.id.student_offline_cash_input).setText(cash)
                findViewById<EditText>(R.id.student_offline_check_input).setText(check)

                val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/createStudent");
                val formBody = FormBody.Builder()
                    .add("cons_id", getConsID())
                    .add("event_id", getEvent().event_id)
                    .add("cash_total", cash)
                    .add("checks_total", check)
                    .add("first_name", findViewById<EditText>(R.id.student_first_name_input).text.toString())
                    .add("last_name", findViewById<EditText>(R.id.student_last_name_input).text.toString())
                    .add("teacher", findViewById<EditText>(R.id.student_teacher_input).text.toString())
                    .add("grade", add_student_grade)
                    .add("imported_shirt", add_student_shirt)
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
                            if(json.has("success") && json.get("success") is Boolean && json.get("success") as Boolean){
                                hideAlert()
                                displayAlert(getString(R.string.mobile_manage_school_activities_add_student_success),{findViewById<TextView>(R.id.btn_add_student).requestFocus()})
                                loadStudentsCard(sort_by_field, sort_by_direction)
                            }else{
                                hideAlert()
                                displayAlert(getString(R.string.mobile_manage_school_activities_add_student_failure),{findViewById<TextView>(R.id.btn_add_student).requestFocus()})
                            }
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        println(e.message.toString())
                    }
                })
            }
        }

        val close_button = findViewById<ImageView>(R.id.add_student_alert_close_button);
        close_button.setOnClickListener{
            hideAlert()
            findViewById<TextView>(R.id.btn_add_student).requestFocus()
        }

        val cancel_button = findViewById<TextView>(R.id.add_student_cancel_button);
        cancel_button.setOnClickListener{
            hideAlert()
            findViewById<TextView>(R.id.btn_add_student).requestFocus()
        }

        alertsContainer.setVisibility(View.VISIBLE)
        hideAlertScrollView(false)
        close_button.requestFocus()
        findViewById<ImageView>(R.id.add_student_alert_close_button).requestFocus();
        Handler().postDelayed({
            findViewById<View>(R.id.add_student_alert_heading).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        }, 175)
    }

    fun isValidCurrency(str: String): Boolean{
        var valid = true
        if(str.contains(".")){
            if(isCharacterRepeated(str, ".")){
                valid = false
            }else{
                if(str.substring(str.indexOf(".") + 1, str.length).length > 2){
                    valid = false
                }
            }
        }
        return valid
    }

    fun isCharacterRepeated(input: String, field:String): Boolean{
        return input.indexOf(field) != input.lastIndexOf(field)
    }

    fun checkCanSearch():Boolean{
        if ( findViewById<TextView>(R.id.amount_raised_error_message).visibility == View.VISIBLE){
            findViewById<Button>(R.id.advanced_search_alert_search_button).setAlpha(.5f)
            return false
        }else {
            findViewById<Button>(R.id.advanced_search_alert_search_button).setAlpha(1f)
            return true
        }
    }

    fun checkCanAddDonation():Boolean{
        var return_val = true;
        if(findViewById<EditText>(R.id.donation_offline_check_input).text.toString() == "" && findViewById<EditText>(R.id.donation_offline_cash_input).text.toString() == "" ){
            return_val = false
        } else if ( findViewById<TextView>(R.id.add_donation_cash_error_message).visibility == View.VISIBLE){
            return_val = false
        }else if ( findViewById<TextView>(R.id.add_donation_check_error_message).visibility == View.VISIBLE){
            return_val = false
        }

        if(return_val){
            findViewById<Button>(R.id.add_donation_save_button).setAlpha(1f)
        }else{
            findViewById<Button>(R.id.add_donation_save_button).setAlpha(.5f)
        }

        return return_val
    }

    fun checkCanAddStudent(): Boolean{
        var return_val = true;
        if(findViewById<EditText>(R.id.student_first_name_input).text.toString() == ""){
            return_val = false
        }else if (findViewById<EditText>(R.id.student_last_name_input).text.toString() == ""){
            return_val = false
        }else if (findViewById<EditText>(R.id.student_teacher_input).text.toString() == ""){
            return_val = false
        }else if ( findViewById<TextView>(R.id.add_student_cash_error_message).visibility == View.VISIBLE){
            return_val = false
        }else if ( findViewById<TextView>(R.id.add_student_check_error_message).visibility == View.VISIBLE){
            return_val = false
        }

        if(return_val){
            findViewById<Button>(R.id.add_student_save_button).setAlpha(1f)
        }else{
            findViewById<Button>(R.id.add_student_save_button).setAlpha(.5f)
        }

        return return_val
    }

    fun loadFilterItems(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/amountFilters")
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
                    val jsonString = response.body?.string();
                    val obj = JSONObject(jsonString);
                    if(obj.has("data") && obj.get("data") is JSONArray){
                        val jsonArray = obj.get("data") as JSONArray
                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i);
                            if(item.has("filters_id") && item.has("filters_name") && item.get("filters_id") is String && item.get("filters_name") is String){
                                amount_filter_items += filterItem(item.get("filters_id") as String,item.get("filters_name") as String)
                            }
                        }
                    }
                    loadBadgeItems()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
            }
        })
    }

    fun loadBadgeItems(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/badgeTypeFilters/").plus(getEvent().event_id)
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
                    val jsonString = response.body?.string();
                    val obj = JSONObject(jsonString);
                    if(obj.has("data") && obj.get("data") is JSONArray){
                        val jsonArray = obj.get("data") as JSONArray
                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i);
                            if(item.has("badge_id") && item.has("badge_name") && item.get("badge_id") is String && item.get("badge_name") is String){
                                badge_items += badgeItem(item.get("badge_id") as String,item.get("badge_name") as String)
                            }
                        }

                    }
                    loadGradeItems()

                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
            }
        })
    }

    fun loadGradeItems(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/grades")
        var request = Request.Builder()
            .url(url)
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                loadShirtItems()
                if(response.code != 200){
                    throw Exception(response.body?.string())
                }else{
                    val jsonString = response.body?.string();
                    val obj = JSONObject(jsonString);
                    if(obj.has("data") && obj.get("data") is JSONArray){
                        val jsonArray = obj.get("data") as JSONArray
                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i);
                            if(item.has("grade_id") && item.has("grade_name") && item.get("grade_id") is String && item.get("grade_name") is String){
                                grade_items += gradeItem(item.get("grade_id") as String,item.get("grade_name") as String)
                                grade_filter_items += gradeItem(item.get("grade_id") as String,item.get("grade_name") as String)
                            }
                        }
                    }
                    loadSortBy()
                    loadTeamEmails()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
            }
        })
    }

    fun loadShirtItems(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/tshirts")
        var request = Request.Builder()
            .url(url)
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .build()

        var client = OkHttpClient();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                if(response.code != 200){
                    loadActivityCard()
                    //throw Exception(response.body?.string())
                }else{
                    val jsonString = response.body?.string();
                    val obj = JSONObject(jsonString);
                    if(obj.has("data") && obj.get("data") is JSONArray){
                        val jsonArray = obj.get("data") as JSONArray
                        for (i in 0 until jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i);
                            if(item.has("tshirt_id") && item.has("tshirt_name") && item.get("tshirt_id") is String && item.get("tshirt_name") is String){
                                shirt_items += shirtItem(item.get("tshirt_id") as String,item.get("tshirt_name") as String)
                            }
                        }
                    }
                    loadActivityCard()
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString());
            }
        })
    }

    fun loadActivityCard(){
        findViewById<Button>(R.id.btn_resources).setOnClickListener{
            val manage_schools_resources_link = getStringVariable("MANAGE_SCHOOL_RESOURCES_URL")
            if(manage_schools_resources_link != ""){
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(manage_schools_resources_link))
                startActivity(browserIntent)
            }else{
                displayAlert(getString(R.string.mobile_url_opening_error),"")
            }
        }

        findViewById<Button>(R.id.btn_manage_event).setOnClickListener{
            val manage_schools_link = getStringVariable("MANAGE_SCHOOL_URL")
            if(manage_schools_link != "")
            {
                val browserIntent =
                    Intent(Intent.ACTION_VIEW, Uri.parse(manage_schools_link))
                startActivity(browserIntent)
            }else{
                displayAlert(getString(R.string.mobile_url_opening_error),"")
            }
        }

        findViewById<Button>(R.id.btn_add_student).setOnClickListener{
            showStudentModal()
        }

        findViewById<Button>(R.id.btn_email_students).setOnClickListener{
            try{
                composeEmail(team_emails, "", "")
            }catch(e: java.lang.Exception){
                displayAlert(getResources().getString(R.string.mobile_fundraise_share_dialog_error))
            }
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
                    loadStudentsCard(sort_by_field,sort_by_direction)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
                //displayAlert(getResources().getString(R.string.mobile_overview_edit_goal_error), hideAlert())
                //hideAlert()
            }
        })
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

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return false
    }
}
class StudentGift(
    val gift_id: Int,
    val gift_type: String,
    var delivered_status: Boolean,
    val badge_name: String,
    val gift_date: String,
    val delivered_image_url: String,
    val undelivered_image_url: String,
)

class StudentItem(
    val id: Int,
    val name: String,
    val teacher: String,
    val grade: String,
    val gifts: List<StudentGift>,
    val email: String,
    val amount: String
)

class filterItem(
    val filter_id: String,
    val filter_name: String
)

class badgeItem(
    val badge_id: String,
    val badge_name: String
)

class statusItem(
    val status_id: Int,
    val status_name: String
)

class shirtItem(
    val tshirt_id: String,
    val tshirt_name: String
)

class gradeItem(
    val grade_id: String,
    val grade_name: String
)