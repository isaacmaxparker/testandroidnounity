package com.nuclavis.rospark

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.children
import androidx.core.widget.ImageViewCompat
import androidx.databinding.DataBindingUtil
import com.nuclavis.rospark.databinding.*
import com.miteksystems.misnap.core.MiSnapSettings
import com.miteksystems.misnap.workflow.MiSnapFinalResult
import com.miteksystems.misnap.workflow.MiSnapWorkflowActivity
import com.miteksystems.misnap.workflow.MiSnapWorkflowError
import com.miteksystems.misnap.workflow.MiSnapWorkflowStep
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class CheckDeposit : BaseActivity() {
    var front_base64 = ""
    var back_base64 = ""
    var page_credit = "participant"
    var check_credit = "";
    var donor_type = "individual"
    var check_credit_event_id = "";
    var check_credit_value = JSONObject();
    val items = arrayOf("","AK", "AL", "AR", "AS", "AZ", "CA", "CO", "CT", "DC", "DE", "FL", "GA", "GU", "HI", "IA", "ID", "IL", "IN", "KS", "KY", "LA", "MA", "MD", "ME", "MI", "MN", "MO", "MP", "MS", "MT", "NC", "ND", "NE", "NH", "NJ", "NM", "NV", "NY", "OH", "OK", "OR", "PA", "PR", "RI", "SC", "SD", "TN", "TX", "UM", "UT", "VA", "VI", "VT", "WA", "WI", "WV", "WY")
    var company_name = ""
    var first_name = ""
    var last_name = ""
    var address_1 = ""
    var address_2 = ""
    var city = ""
    var zip = ""
    var state = ""
    var email = ""
    var check_amount = ""
    var check_number = ""
    var current_type = ""
    var initial = false;
    var license = ""
    var team_member_cons_id = ""
    var split_value_total = 0.00

    var creditSplits = listOf<CreditMember>();

    private val registerForActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            MiSnapWorkflowActivity.Result.results.forEachIndexed { index, stepResult ->
                when (stepResult) {
                    is MiSnapWorkflowStep.Result.Success -> {
                        when (val misnapResult = stepResult.result) {
                            is MiSnapFinalResult.DocumentSession -> {
                                var byteArrayDocumentImage = misnapResult.jpegImage
                                val documentImageBase64 = Base64.encodeToString(byteArrayDocumentImage, Base64.DEFAULT)
                                val arrayBImg: ByteArray = misnapResult.jpegImage
                                val bmp = BitmapFactory.decodeByteArray(arrayBImg, 0, arrayBImg.size)
                                if(current_type == "front"){
                                    checkForDepositAbility()
                                    if(arrayBImg.size > 0){
                                        front_base64 = documentImageBase64
                                        Executors.newSingleThreadExecutor().execute(Runnable {
                                            sendPayerExtract()
                                        })
                                        checkForDepositAbility()
                                        val image = findViewById<ImageView>(R.id.front_camera_image);
                                        ImageViewCompat.setImageTintList(image, null);
                                        image.setImageBitmap(Bitmap.createScaledBitmap(bmp, 100, 50, false));
                                    }
                                }else{
                                    checkForDepositAbility()
                                    if(arrayBImg.size > 0){
                                        val image = findViewById<ImageView>(R.id.back_camera_image);
                                        back_base64 = documentImageBase64
                                        checkForDepositAbility()
                                        ImageViewCompat.setImageTintList(image, null);
                                        image.setImageBitmap(Bitmap.createScaledBitmap(bmp, 100, 50, false));
                                    }
                                }
                            }else -> {
                                displayAlert("Error")
                            }
                        }
                    }
                    is MiSnapWorkflowStep.Result.Error -> {
                        println("ERROR")
                        when (val errorResult = stepResult.errorResult.error) {
                            is MiSnapWorkflowError.Permission -> {
                                println(errorResult)
                            }
                            is MiSnapWorkflowError.Camera -> {
                                println(errorResult)
                            }
                            is MiSnapWorkflowError.Cancelled -> {
                                println(errorResult)
                            } else -> {
                                println(errorResult)
                            }
                        }
                    }
                }
            }
            MiSnapWorkflowActivity.Result.clearResults();
        }

    override fun childviewCallback(string: String, data: String) {
        if(string == "donor_data"){
            var donor = JSONObject(data)
            if(getSafeStringVariable(donor, "donor_type") == "company"){
                findViewById<Spinner>(R.id.select_donation_page_donor_type).setSelection(1)
            }else if (getSafeStringVariable(donor, "donor_type") == "individual"){
                findViewById<Spinner>(R.id.select_donation_page_donor_type).setSelection(0)
            }
            if(donor.has("first_name")  && donor.get("first_name") is String){
                findViewById<EditText>(R.id.input_first_name).setText(donor.get("first_name") as String)
            }else{
                findViewById<EditText>(R.id.input_first_name).setText("")
            }
            if(donor.has("last_name")  && donor.get("last_name") is String) {
                findViewById<EditText>(R.id.input_last_name).setText(donor.get("last_name") as String)
            }else{
                findViewById<EditText>(R.id.input_last_name).setText("")
            }
            if(donor.has("first_name") && donor.has("last_name")){
                val company_name = getSafeStringVariable(donor, "company_name")
                if(getSafeStringVariable(donor, "donor_type") == "company" && company_name != ""){
                    findViewById<EditText>(R.id.input_company_name).setText(company_name)
                }else{
                    findViewById<EditText>(R.id.input_company_name).setText(getSafeStringVariable(donor, "first_name") + " " + getSafeStringVariable(donor, "last_name"))
                }
            }else{
                findViewById<EditText>(R.id.input_company_name).setText("")
            }
            if(donor.has("street1")  && donor.get("street1") is String) {
                findViewById<EditText>(R.id.input_address_1).setText(donor.get("street1") as String)
            }else{
                findViewById<EditText>(R.id.input_address_1).setText("")
            }
            if(donor.has("street2") && donor.get("street2") is String) {
                findViewById<EditText>(R.id.input_address_2).setText(donor.get("street2") as String)
            }else{
                findViewById<EditText>(R.id.input_address_2).setText("")
            }
            if(donor.has("city")  && donor.get("city") is String) {
                findViewById<EditText>(R.id.input_city).setText(donor.get("city") as String)
            }else{
                findViewById<EditText>(R.id.input_city).setText("")
            }
            if(donor.has("state")  && donor.get("state") is String) {
                findViewById<Spinner>(R.id.select_state).setSelection(items.indexOf(donor.get("state") as String))
            }else{
                findViewById<Spinner>(R.id.select_state).setSelection(0)
            }
            if(donor.has("zip")  && donor.get("zip") is String) {
                findViewById<EditText>(R.id.input_zip).setText(donor.get("zip") as String)
            }else{
                findViewById<EditText>(R.id.input_zip).setText("")
            }
            if(donor.has("email")  && donor.get("email") is String){
                findViewById<EditText>(R.id.input_email).setText(donor.get("email") as String)
            }else{
                findViewById<EditText>(R.id.input_email).setText("")
            }
            checkForDepositAbility()
        } else if (string == "member_data") {
            var member = JSONObject(data)
            team_member_cons_id = member.get("cons_id") as String
        } else if (string == "split_member_data") {
            var member = JSONObject(data)
            if(check_credit == "participant"){
                creditSplits += (CreditMember(getSafeStringVariable(member, "first_name") + " " + getSafeStringVariable(member, "last_name"), 0.00, getSafeStringVariable(member, "cons_id")))
                loadSplitsRows()
            }

            if(page_credit == "captain"){
                creditSplits += (CreditMember(getSafeStringVariable(member, "first_name") + " " + getSafeStringVariable(member, "last_name"), 0.00, getSafeStringVariable(member, "cons_id")))
                loadMemberSplitsRows()
            }            
        }
        else if (string == "run_check"){
            checkForDepositAbility()
        }
        else if (string == "selected_event"){
            check_credit_event_id = data
            setVariable("CHECK_CREDIT_SELECTED_EVENT_ID", data)
            loadDonorDropdown()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        recolorTheme()
        super.onCreate(savedInstanceState)
        setPageContent(R.layout.check_deposit,"checkDeposit")
        setTitle(getResources().getString(R.string.mobile_main_menu_donations));

        sendGoogleAnalytics("check_deposit_view","check_deposit")
        setTooltipText(R.id.check_deposit_find_event_tooltip,R.string.mobile_donations_check_deposit_find_an_event_tooltip, R.string.mobile_donations_check_deposit_find_an_event_title);
        setTooltipText(R.id.check_deposit_donation_credit_tooltip, R.string.mobile_donations_check_deposit_have_a_check_tooltip,R.string.mobile_donations_check_deposit_have_a_check_title);
        setTooltipText(R.id.check_deposit_donor_tooltip, R.string.mobile_donations_check_deposit_donor_tooltip, R.string.mobile_donations_check_deposit_donor_title);
        setTooltipText(R.id.check_deposit_check_credit_tooltip, R.string.mobile_donations_check_deposit_check_credit_tooltip, R.string.mobile_donations_credit_title);
        setTooltipText(R.id.check_deposit_check_details_tooltip, R.string.mobile_donations_check_deposit_check_details_tooltip, R.string.mobile_donations_check_deposit_check_details_title);
        setTooltipText(R.id.check_deposit_donor_details_tooltip, R.string.mobile_donations_check_deposit_details_tooltip, R.string.mobile_donations_check_deposit_details_title);

        findViewById<LinearLayout>(R.id.check_deposit_email_error).setVisibility(View.GONE)
        findViewById<LinearLayout>(R.id.check_deposit_check_amount_error).setVisibility(View.GONE)
        findViewById<LinearLayout>(R.id.check_deposit_zip_error).setVisibility(View.GONE)

        findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.GONE)
        findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(View.GONE)
        findViewById<LinearLayout>(R.id.split_check_credit_select_row).setVisibility(View.GONE)

        findViewById<Button>(R.id.btn_check_deposit_deposit).setAlpha(.5F)
        if(getStringVariable("CHECK_EVENT_MANAGER") == "true"){
            if(intent.getStringExtra("event_id").toString() != "" && intent.getStringExtra("event_id") != null){
                findViewById<LinearLayout>(R.id.find_event_container).setVisibility(View.GONE)
                findViewById<LinearLayout>(R.id.event_details_container).setVisibility(View.VISIBLE)
                findViewById<TextView>(R.id.selected_event_name).text = (intent.getStringExtra("event_name").toString())
                check_credit_event_id = intent.getStringExtra("event_id").toString()
                setVariable("CHECK_CREDIT_SELECTED_EVENT_ID", check_credit_event_id)
            }else{
                clearVariable("CHECK_DEPOSIT_EVENT_ID")
                clearVariable("CHECK_DEPOSIT_EVENT_NAME")
                findViewById<LinearLayout>(R.id.event_details_container).setVisibility(View.GONE)
            }

            if(getStringVariable("CHECK_EVENT_MANAGER_ANNUAL_FUND_ENABLED") == "true"){
                setTooltipText(R.id.check_deposit_event_manager_annual_credit_tooltip, R.string.mobile_donations_check_deposit_deposit_type_tooltip, R.string.mobile_donations_check_deposit_deposit_type_title);

                val items = arrayOf(getString(R.string.mobile_donations_check_deposit_deposit_type_dropdown_event), getString(R.string.mobile_donations_check_deposit_deposit_type_dropdown_annual_fund))
                val dropdown: Spinner = findViewById(R.id.select_annual_credit)
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
                        findViewById<LinearLayout>(R.id.find_event_container).visibility = View.VISIBLE
                        findViewById<LinearLayout>(R.id.event_details_container).visibility = View.GONE
                        if(position == 1){
                            setVariable("EVENT_SEARCH_TYPE", "ANNUAL")
                            findViewById<TextView>(R.id.check_deposit_find_event_card_title).text = getString(R.string.mobile_donations_check_deposit_find_a_chapter_title)
                            findViewById<TextView>(R.id.check_deposit_find_event_card_description).text = getString(R.string.mobile_donations_check_deposit_find_a_chapter_description)
                            findViewById<TextView>(R.id.find_event_btn).text = getString(R.string.mobile_donations_check_deposit_find_a_chapter_seach_button)
                            findViewById<TextView>(R.id.event_selected_label).text = getString(R.string.mobile_donations_check_deposit_find_a_chapter_selected_chapter)
                            setVariable("CHECK_CREDIT", "event")
                            check_credit = "event"
                            findViewById<LinearLayout>(R.id.check_deposit_donation_credit_card).visibility = View.GONE
                            setTooltipText(R.id.check_deposit_find_event_tooltip,R.string.mobile_donations_check_deposit_find_a_chapter_description, R.string.mobile_donations_check_deposit_find_a_chapter_title);
                        }else{
                            setVariable("EVENT_SEARCH_TYPE", "EVENT")
                            clearVariable("CHECK_CREDIT")
                            check_credit = ""
                            findViewById<LinearLayout>(R.id.check_deposit_donation_credit_card).visibility = View.VISIBLE
                            findViewById<TextView>(R.id.check_deposit_find_event_card_title).text = getString(R.string.mobile_donations_check_deposit_find_an_event_title)
                            findViewById<TextView>(R.id.check_deposit_find_event_card_description).text = getString(R.string.mobile_donations_check_deposit_find_an_event_description)
                            findViewById<TextView>(R.id.find_event_btn).text = getString(R.string.mobile_donations_check_deposit_find_an_event_seach_button)
                            findViewById<TextView>(R.id.event_selected_label).text = getString(R.string.mobile_donations_check_deposit_find_an_event_selected_event)

                            setTooltipText(R.id.check_deposit_find_event_tooltip,R.string.mobile_donations_check_deposit_find_an_event_tooltip, R.string.mobile_donations_check_deposit_find_an_event_title);
                        }
                        selectedItemView?.contentDescription = items[position]
                    }

                    override fun onNothingSelected(parentView: AdapterView<*>?) {
                        // your code here
                    }
                })

                findViewById<LinearLayout>(R.id.check_deposit_event_manager_annual_card).setVisibility(View.VISIBLE)
            }

            findViewById<LinearLayout>(R.id.check_deposit_credit_card).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.check_deposit_donation_credit_card).setVisibility(View.VISIBLE)
            findViewById<LinearLayout>(R.id.check_deposit_donor_card).setVisibility(View.VISIBLE)
            if(getStringVariable("CHECK_EVENT_MANAGER_HIDE_FIND_DONOR") == "true"){
                findViewById<LinearLayout>(R.id.check_deposit_donor_card).setVisibility(View.GONE)
            }
            findViewById<LinearLayout>(R.id.check_deposit_event_card).setVisibility(View.VISIBLE)


            findViewById<Button>(R.id.find_event_btn).setOnClickListener{
                sendGoogleAnalytics("check_deposit_find_event_show","check_deposit")
                displayAlert("findEvent")
                setAlertSender(findViewById<Button>(R.id.find_event_btn))
            }
            findViewById<LinearLayout>(R.id.edit_selected_event).setOnClickListener{
                sendGoogleAnalytics("check_deposit_find_event_show","check_deposit")
                displayAlert("findEvent")
                setAlertSender(findViewById<LinearLayout>(R.id.edit_selected_event))
            }
        }else{
            clearVariable("CHECK_DEPOSIT_EVENT_ID")
            clearVariable("CHECK_DEPOSIT_EVENT_NAME")
            clearVariable("CHECK_CREDIT")
            clearVariable("CHECK_CREDIT_VALUE")
            findViewById<LinearLayout>(R.id.check_deposit_credit_card).setVisibility(View.VISIBLE)
            findViewById<LinearLayout>(R.id.check_deposit_event_card).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.check_deposit_donation_credit_card).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.check_deposit_donor_card).setVisibility(View.GONE)

            if(getStringVariable("HAS_TEAM") == "true"){
                findViewById<LinearLayout>(R.id.check_deposit_credit_card).setVisibility(View.VISIBLE)
            }else{
                findViewById<LinearLayout>(R.id.check_deposit_credit_card).setVisibility(View.GONE)
            }
        }

        license = getStringVariable("CHECK_DEPOSIT_MISNAP_LICENSE")

        findViewById<LinearLayout>(R.id.front_camera_button).setOnClickListener{
            sendGoogleAnalytics("check_deposit_front_camera","check_deposit")
            if(license != ""){
                startMiSnapCamera(MiSnapSettings.UseCase.CHECK_FRONT)
                current_type = "front"
            }else{
                displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_error_message))
                setAlertSender(findViewById<LinearLayout>(R.id.front_camera_button))
            }
        }

        findViewById<LinearLayout>(R.id.back_camera_button).setOnClickListener {
            sendGoogleAnalytics("check_deposit_back_camera","check_deposit")
            if(license != ""){
                startMiSnapCamera(MiSnapSettings.UseCase.CHECK_BACK)
                current_type = "back"
            }else{
                displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_error_message))
                setAlertSender(findViewById<LinearLayout>(R.id.back_camera_button))
            }
        }

        if(getStringVariable("IS_EVENT_MANAGER_ONLY") == "true"){
            findViewById<Button>(R.id.btn_check_deposit_cancel).visibility = View.GONE
        }else{
            findViewById<Button>(R.id.btn_check_deposit_cancel).setOnClickListener{
                sendGoogleAnalytics("check_deposit_cancel","check_deposit")
                clearVariable("CHECK_DEPOSIT_EVENT_ID")
                clearVariable("CHECK_DEPOSIT_EVENT_NAME")
                clearVariable("CHECK_CREDIT")
                clearVariable("CHECK_CREDIT_VALUE")
                val intent = Intent(this@CheckDeposit, Donations::class.java);
                startActivity(intent);
                this.overridePendingTransition(0, 0);
            }
        }

        findViewById<LinearLayout>(R.id.donor_name_credit_row).setVisibility(View.GONE)
        loadDetailsCard()
        loadDonorDropdown()
        loadCreditDropdown()
        loadPageCreditDropdown()
        loadDonorTypeDropdown()

        if(getStringVariable("CHECK_DEPOSIT_ALLOW_RECOGNITION_NAME") == "true"){
            findViewById<LinearLayout>(R.id.recognition_name_row).setVisibility(View.VISIBLE)
        }

        if(getStringVariable("CHECK_DEPOSIT_ALLOW_DISPLAY_AMOUNT_PUBLICLY") == "true"){
            findViewById<LinearLayout>(R.id.cb_display_publicly_row).setVisibility(View.VISIBLE)
        }

        findViewById<LinearLayout>(R.id.edit_check_credit_member_name).setOnClickListener{
            sendGoogleAnalytics("check_deposit_find_member_show","check_deposit")
            displayAlert("findMember")
            setAlertSender(findViewById<LinearLayout>(R.id.select_check_credit_team_member_link))
        }

        findViewById<LinearLayout>(R.id.edit_check_credit_type).setOnClickListener {
            if(check_credit == "participant"){
                sendGoogleAnalytics("check_deposit_find_participant_show","check_deposit")
                if(getStringVariable("CHECK_DEPOSIT_EVENT_ID") != ""){
                    displayAlert("findParticipant")
                    setAlertSender(findViewById<LinearLayout>(R.id.edit_check_credit_type))
                }else{
                    displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_select_part_error))
                    setAlertSender(findViewById<LinearLayout>(R.id.edit_check_credit_type))
                }
            }else if(check_credit == "team"){
                sendGoogleAnalytics("check_deposit_find_team_show","check_deposit")
                if(getStringVariable("CHECK_DEPOSIT_EVENT_ID") != ""){
                    displayAlert("findTeam")
                    setAlertSender(findViewById<LinearLayout>(R.id.edit_check_credit_type))
                }else{
                    displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_select_team_error))
                    setAlertSender(findViewById<LinearLayout>(R.id.edit_check_credit_type))
                }
            }else if(check_credit == "event"){
                sendGoogleAnalytics("check_deposit_find_event_show","check_deposit")
                displayAlert("findEvent")
                setAlertSender(findViewById<LinearLayout>(R.id.edit_check_credit_type))
            }

        }
        findViewById<Button>(R.id.btn_check_deposit_deposit).setOnClickListener {
            sendGoogleAnalytics("check_deposit_submit","check_deposit")
            var depositable = checkForDepositAbility()
            if(depositable){
                submitDonation()
            }
        }
    }

    fun clearDonor(){
        findViewById<EditText>(R.id.input_first_name).setText("")
        findViewById<EditText>(R.id.input_last_name).setText("")
        findViewById<EditText>(R.id.input_company_name).setText("")
        findViewById<EditText>(R.id.input_address_1).setText("")
        findViewById<EditText>(R.id.input_address_2).setText("")
        findViewById<EditText>(R.id.input_city).setText("")
        findViewById<Spinner>(R.id.select_state).setSelection(0)
        findViewById<EditText>(R.id.input_zip).setText("")
        findViewById<EditText>(R.id.input_email).setText("")
        checkForDepositAbility()
    }

    fun sendPayerExtract(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/check/extractData/")
        var json = "{\"image\":\"${front_base64}\"}"
        json = json.replace("\n","")

        val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body: RequestBody = json.toRequestBody(JSON)
        var request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient
            .Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val jsonString = response.body?.string()
                val obj = JSONObject(jsonString)
                val data = obj.get("data").toString()
                val donor = JSONObject(data)
                runOnUiThread {
                    if(findViewById<EditText>(R.id.input_first_name).text.toString() == "") {
                        if (donor.has("first_name") && donor.get("first_name") is String) {
                            findViewById<EditText>(R.id.input_first_name).setText(donor.get("first_name") as String)
                        }
                    }
                    if(findViewById<EditText>(R.id.input_last_name).text.toString() == ""){
                        if (donor.has("last_name") && donor.get("last_name") is String) {
                            findViewById<EditText>(R.id.input_last_name).setText(donor.get("last_name") as String)
                        }
                    }

                    if(findViewById<EditText>(R.id.input_company_name).text.toString() == ""){
                        if (donor.has("full_name") && donor.get("full_name") is String) {
                            findViewById<EditText>(R.id.input_company_name).setText(donor.get("full_name") as String)
                        }
                    }

                    if(findViewById<EditText>(R.id.input_address_1).text.toString() == ""){
                        if (donor.has("address1") && donor.get("address1") is String && findViewById<EditText>(R.id.input_address_1).text.toString() == "") {
                            findViewById<EditText>(R.id.input_address_1).setText(donor.get("address1") as String)
                        }
                    }

                    if(findViewById<EditText>(R.id.input_city).text.toString() == "") {
                        if (donor.has("city") && donor.get("city") is String) {
                            findViewById<EditText>(R.id.input_city).setText(donor.get("city") as String)
                        }
                    }

                    if(findViewById<Spinner>(R.id.select_state).getSelectedItem().toString() == ""){
                        if (donor.has("state") && donor.get("state") is String) {
                            findViewById<Spinner>(R.id.select_state).setSelection(
                                items.indexOf(donor.get("state") as String)
                            )
                        }
                    }

                    if(findViewById<EditText>(R.id.input_zip).text.toString() == ""){
                        if (donor.has("zip") && donor.get("zip") is String) {
                            findViewById<EditText>(R.id.input_zip).setText(donor.get("zip") as String)
                        }
                    }

                    if(findViewById<EditText>(R.id.input_email).text.toString() == ""){
                        if (donor.has("email") && donor.get("email") is String) {
                            findViewById<EditText>(R.id.input_email).setText(donor.get("email") as String)
                        }
                    }
                    checkForDepositAbility()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println("THERE WAS AN ERROR EXTRACTING PAYER")
                println(e)
            }
        })
    }

    fun loadInputListeners(){
        findViewById<EditText>(R.id.input_first_name).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                first_name = s.toString()
                checkForDepositAbility()
            }
        })

        findViewById<EditText>(R.id.input_last_name).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                last_name = s.toString()
                checkForDepositAbility()
            }
        })

        findViewById<EditText>(R.id.input_company_name).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                company_name = s.toString()
                checkForDepositAbility()
            }
        })

        findViewById<EditText>(R.id.input_address_1).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                address_1 = s.toString()
                checkForDepositAbility()
            }
        })

        findViewById<EditText>(R.id.input_address_2).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                address_2 = s.toString()
                checkForDepositAbility()
            }
        })

        findViewById<EditText>(R.id.input_city).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                city = s.toString()
                checkForDepositAbility()
            }
        })

        findViewById<EditText>(R.id.input_zip).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                zip = s.toString()
                if(zip != "" && !checkZipCode(zip)){
                    findViewById<LinearLayout>(R.id.check_deposit_zip_error).setVisibility(View.VISIBLE)
                }else{
                    findViewById<LinearLayout>(R.id.check_deposit_zip_error).setVisibility(View.GONE)
                }
                checkForDepositAbility()
            }
        })

        findViewById<EditText>(R.id.input_email).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {checkForDepositAbility()}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                email = s.toString()
                if(email != "" && !email.contains("@")){
                    findViewById<LinearLayout>(R.id.check_deposit_email_error).setVisibility(View.VISIBLE)
                }else{
                    findViewById<LinearLayout>(R.id.check_deposit_email_error).setVisibility(View.GONE)
                }
                checkForDepositAbility()
            }
        })

        findViewById<EditText>(R.id.check_amount).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {checkForDepositAbility()}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                check_amount = s.toString()
                if(check_amount != "" && !checkAmount(check_amount)){
                    findViewById<LinearLayout>(R.id.check_deposit_check_amount_error).setVisibility(View.VISIBLE)
                }else{
                    findViewById<LinearLayout>(R.id.check_deposit_check_amount_error).setVisibility(View.GONE)
                }
                checkForDepositAbility()
            }
        })

        findViewById<EditText>(R.id.check_number).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {checkForDepositAbility()}
            override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                check_number = s.toString()
                checkForDepositAbility()
            }
        })
    }

    fun checkAmount(amount:String): Boolean{
        val pattern = Regex("^[0-9]*\\.[0-9][0-9]\$")
        return pattern.containsMatchIn(amount)
    }

    fun checkZipCode(zip:String): Boolean{
        val pattern = Regex("^\\d{5}\$|^\\d{9}\$|^\\d{5}-\\d{4}\$")
        return pattern.containsMatchIn(zip)
    }

    fun checkForDepositAbility(): Boolean{
        var depositable = true;

        if(getStringVariable("CHECK_DEPOSIT_SPLIT_CHECK_ENABLED") == "true") {
            if(check_credit == "participant" ||  page_credit == "captain"){
                if (isValidCurrency(check_amount) && check_amount != ""){
                    try {
                        if (check_amount.toDouble() != split_value_total) {
                            findViewById<LinearLayout>(R.id.check_deposit_check_split_amount_error).visibility =
                                View.VISIBLE
                            depositable = false;
                        } else {
                            findViewById<LinearLayout>(R.id.check_deposit_check_split_amount_error).visibility =
                                View.GONE
                        }
                    }catch(e: Exception){
                        findViewById<LinearLayout>(R.id.check_deposit_check_split_amount_error).visibility =
                            View.VISIBLE
                        depositable = false
                    }
                }else{
                    findViewById<LinearLayout>(R.id.check_deposit_check_split_amount_error).visibility = View.GONE
                }
            }else{
                findViewById<LinearLayout>(R.id.check_deposit_check_split_amount_error).visibility = View.GONE
            }
        }else{
            findViewById<LinearLayout>(R.id.check_deposit_check_split_amount_error).visibility = View.GONE
        }

        println("-------- FIRST DEPOSITABLE: " + depositable)

        if(front_base64 == ""){
            depositable = false
        } else if(back_base64 == ""){
            depositable = false
        } else if(address_1 == ""){
            depositable = false
        } else if(city == ""){
            depositable = false
        } else if(!checkZipCode(zip)){
            depositable = false
        } else if(state == ""){
            depositable = false
        } else if(email != "" && !email.contains("@")){
            depositable = false
        }else if(!checkAmount(check_amount)){
            depositable = false
        }else if(check_number == ""){
            depositable = false
        }else if (getStringVariable("HAS_TEAM") == "true" && page_credit == ""){
            depositable = false
        }else if (page_credit == "captain" && team_member_cons_id == ""){
            if(getStringVariable("CHECK_DEPOSIT_SPLIT_CHECK_ENABLED") != "true"){
                depositable = false
            }
        }else{
            if(getStringVariable("CHECK_EVENT_MANAGER") == "true") {
                if (getStringVariable("CHECK_DEPOSIT_EVENT_NAME") == "" || getStringVariable("CHECK_DEPOSIT_EVENT_ID") == "") {
                    depositable = false
                } else if(check_credit == "") {
                    depositable = false
                } else if (check_credit != "event" && (getStringVariable("CHECK_CREDIT") == "" || getStringVariable("CHECK_CREDIT_VALUE") == "")){
                    depositable = false
                }
            }else{
                if(donor_type == "individual"){
                    if(first_name == "" || last_name == ""){
                        depositable = false
                    }
                }else if(donor_type == "company"){
                    if(company_name == ""){
                        depositable = false
                    }
                }
            }
        }

        if(depositable){
            findViewById<Button>(R.id.btn_check_deposit_deposit).setAlpha(1F)
        }else{
            findViewById<Button>(R.id.btn_check_deposit_deposit).setAlpha(.5F)
        }
        return depositable
    }

    fun loadDetailsCard(){
        val dropdown: Spinner = findViewById(R.id.select_state)
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
                state = items[position]
                if(state != ""){
                    dropdown.setContentDescription("")
                }else{
                    dropdown.setContentDescription(getString(R.string.mobile_donations_check_deposit_details_state))
                }
                checkForDepositAbility();
                selectedItemView?.contentDescription = getString(R.string.mobile_donations_check_deposit_details_state) + ", " + items[position]
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // your code here
            }
        })

        var comboString = "";
        var bold_end = 0;
        if(getStringVariable("CHECK_DEPOSIT_VENDOR") == "ensenta"){
            comboString = getResources().getString(R.string.mobile_donations_check_deposit_details_ensenta_note) + " " + getResources().getString(R.string.mobile_donations_check_deposit_details_ensenta_note_description);
            bold_end = getResources().getString(R.string.mobile_donations_check_deposit_details_ensenta_note).length
        }else{
            comboString = getResources().getString(R.string.mobile_donations_check_deposit_details_note) + " " + getResources().getString(R.string.mobile_donations_check_deposit_details_note_description);
            bold_end = getResources().getString(R.string.mobile_donations_check_deposit_details_note).length
        }

        val tv = findViewById<TextView>(R.id.note_description)
        val str = SpannableStringBuilder(comboString)

        val total_length = comboString.length
        str.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            bold_end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        str.setSpan(
            ForegroundColorSpan(Color.BLACK),
            bold_end,
            total_length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tv.setText(str)
        loadInputListeners()
    }

    fun loadDonorTypeDropdown(){
        val dropdown: Spinner = findViewById(R.id.select_donation_page_donor_type)
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
                    donor_type = "individual"
                    findViewById<LinearLayout>(R.id.donor_name_row).setVisibility(View.VISIBLE)
                    findViewById<LinearLayout>(R.id.company_name_row).setVisibility(View.GONE)
                }else if(position == 1) {
                    donor_type = "company"
                    findViewById<LinearLayout>(R.id.donor_name_row).setVisibility(View.GONE)
                    findViewById<LinearLayout>(R.id.company_name_row).setVisibility(View.VISIBLE)
                }
                selectedItemView?.contentDescription = getString(R.string.mobile_donations_check_deposit_check_details_donor_type) + " , " + items[position]
            }
        }
    }

    fun loadPageCreditDropdown(){
        val description = findViewById<TextView>(R.id.check_credit_credit_description)
        var is_captain = getStringVariable("IS_TEAM_CAPTAIN") == "true";
        val dropdown: Spinner = findViewById(R.id.select_donation_page_credit)
        val team_page_disabled = getStringVariable("CHECK_DEPOSIT_DEPOSIT_TEAM_ENABLED") == "false";
        if(getStringVariable("CHECK_DEPOSIT_DEPOSIT_TEAM_MEMBER") == "true" && is_captain){
            description.setText(R.string.mobile_donations_check_deposit_have_a_check_team_member_description_android)

            if(team_page_disabled){
                val items = arrayOf(getString(R.string.mobile_donations_check_deposit_part_team_event_please_select), getString(R.string.mobile_donations_check_deposit_have_a_check_team_member), getString(R.string.mobile_donations_check_deposit_have_a_check_my_fundraising_page))
                val adapter = ArrayAdapter(this, R.layout.spinner_item, items)
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                dropdown.adapter = adapter
                dropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        println("NOTHING SELECTED")
                    }

                    override fun onItemSelected(parent: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                        if(position == 1){
                            page_credit = "captain"
                            findViewById<LinearLayout>(R.id.check_credit_team_captain_row).setVisibility(View.VISIBLE)
                            checkForDepositAbility()
                        }else if(position == 2){
                            page_credit = "participant"
                            findViewById<LinearLayout>(R.id.check_credit_team_captain_row).setVisibility(View.GONE)
                            findViewById<LinearLayout>(R.id.donor_name_credit_row).setVisibility(View.GONE)
                            checkForDepositAbility()
                        }else{
                            page_credit = "";
                            findViewById<LinearLayout>(R.id.check_credit_team_captain_row).setVisibility(View.GONE)
                            findViewById<LinearLayout>(R.id.donor_name_credit_row).setVisibility(View.GONE)
                        }
                        selectedItemView?.contentDescription = getString(R.string.mobile_donations_check_deposit_part_team_event_credit_label) + ", " + items[position]
                    }
                }
            }else{
                val items = arrayOf(getString(R.string.mobile_donations_check_deposit_part_team_event_please_select), getString(R.string.mobile_donations_check_deposit_have_a_check_team_member), getString(R.string.mobile_donations_check_deposit_have_a_check_my_team_page), getString(R.string.mobile_donations_check_deposit_have_a_check_my_fundraising_page))
                val adapter = ArrayAdapter(this, R.layout.spinner_item, items)
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                dropdown.adapter = adapter
                dropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        println("NOTHING SELECTED")
                    }

                    override fun onItemSelected(parent: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                        if(position == 1){
                            page_credit = "captain"
                            findViewById<LinearLayout>(R.id.check_credit_team_captain_row).setVisibility(View.VISIBLE)
                            checkForDepositAbility()
                        }else if (position == 2) {
                            page_credit = "team"
                            findViewById<LinearLayout>(R.id.check_credit_team_captain_row).setVisibility(View.GONE)
                            checkForDepositAbility()
                        }else if(position == 3){
                            findViewById<LinearLayout>(R.id.check_credit_team_captain_row).setVisibility(View.GONE)
                            page_credit = "participant"
                            checkForDepositAbility()
                        }else{
                            findViewById<LinearLayout>(R.id.check_credit_team_captain_row).setVisibility(View.GONE)
                            findViewById<LinearLayout>(R.id.donor_name_credit_row).setVisibility(View.GONE)
                        }
                        selectedItemView?.contentDescription = getString(R.string.mobile_donations_check_deposit_part_team_event_credit_label) + ", " + items[position]
                    }
                }
            }

            findViewById<LinearLayout>(R.id.select_check_credit_team_member_link).setOnClickListener{
                displayAlert("findMember")
                sendGoogleAnalytics("check_deposit_find_member_show","check_deposit")
                setAlertSender(findViewById<LinearLayout>(R.id.select_check_credit_team_member_link))
            }
        }else{
            if(team_page_disabled){
                findViewById<LinearLayout>(R.id.check_deposit_credit_card).setVisibility(View.GONE)
            }else {
                findViewById<LinearLayout>(R.id.check_credit_team_captain_row).setVisibility(View.GONE)
                description.setText(R.string.mobile_donations_check_deposit_have_a_check_description)
                val items = arrayOf(getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_please_select),getResources().getString(R.string.mobile_donations_check_deposit_have_a_check_my_fundraising_page),getResources().getString(R.string.mobile_donations_check_deposit_have_a_check_my_team_page))
                val adapter = ArrayAdapter<String>(this, R.layout.spinner_item, items)
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                dropdown.adapter = adapter
                dropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        println("NOTHING SELECTED")
                    }

                    override fun onItemSelected(parent: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                        if(position == 1){
                            page_credit = "participant"
                            checkForDepositAbility()
                        }else if (position == 2) {
                            page_credit = "team"
                            checkForDepositAbility()
                        }
                        else{
                            findViewById<LinearLayout>(R.id.donor_name_credit_row).setVisibility(View.GONE)
                        }
                        selectedItemView?.contentDescription = getString(R.string.mobile_donations_check_deposit_part_team_event_credit_label) + ", " + items[position]
                    }
                }
            }
        }
    }

    fun loadDonorDropdown(){
        val dropdown: Spinner = findViewById(R.id.select_donor)
        val donor_select_row = findViewById<LinearLayout>(R.id.find_donor_select_row)
        val donor_credit_row = findViewById<LinearLayout>(R.id.donor_name_credit_row)

        findViewById<LinearLayout>(R.id.find_donor_link).setOnClickListener{
            displayAlert("findDonor")
            setAlertSender(findViewById<LinearLayout>(R.id.find_donor_link))
            sendGoogleAnalytics("check_deposit_find_donor_show","check_deposit")
        }

        findViewById<LinearLayout>(R.id.edit_donor_credit_donor_name).setOnClickListener {
            displayAlert("findDonor")
            setAlertSender(findViewById<LinearLayout>(R.id.edit_donor_credit_donor_name))
            sendGoogleAnalytics("check_deposit_find_donor_show","check_deposit")
        }
        
        if(getStringVariable("CHECK_EVENT_MANAGER_DONOR_BY_EVENT") == "true" && check_credit_event_id == ""){
            var items = arrayOf(getResources().getString(R.string.mobile_donations_check_deposit_donor_create_a_donor))
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
            dropdown.adapter = adapter

            dropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    println("NOTHING SELECTED")
                }

                override fun onItemSelected(parent: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                    donor_select_row.setVisibility(View.GONE)
                    donor_credit_row.setVisibility(View.GONE)
                    clearDonor()
                    selectedItemView?.contentDescription = getString(R.string.mobile_donations_check_deposit_donor_label) + ", " + items[position]
                }
            }
        }else{
            var items = arrayOf(getResources().getString(R.string.mobile_donations_check_deposit_donor_create_a_donor),getResources().getString(R.string.mobile_donations_check_deposit_donor_find_a_donor))
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
            dropdown.adapter = adapter

            dropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    println("NOTHING SELECTED")
                }

                override fun onItemSelected(parent: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                    if(position == 1){
                        donor_select_row.setVisibility(View.VISIBLE)
                    }else{
                        donor_select_row.setVisibility(View.GONE)
                        donor_credit_row.setVisibility(View.GONE)
                        clearDonor()
                    }
                    selectedItemView?.contentDescription = getString(R.string.mobile_donations_check_deposit_donor_label) + ", " + items[position]
                }
            }
        }
    }

    fun loadCreditDropdown(){
        if(intent.getStringExtra("check_credit").toString() != ""){
            check_credit = intent.getStringExtra("check_credit").toString()
            if(intent.getStringExtra("check_credit_value").toString() != "") {
                check_credit_value = JSONObject(intent.getStringExtra("check_credit_value"));
            }
        }else{
            clearVariable("CHECK_CREDIT")
            clearVariable("CHECK_CREDIT_VALUE")
        }

        val dropdown: Spinner = findViewById(R.id.select_donation_credit)

        if(getStringVariable("CHECK_DEPOSIT_DEPOSIT_TEAM_ENABLED") == "false"){
            val items = arrayOf(getResources().getString(R.string.mobile_donations_check_deposit_have_a_check_please_select),getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_participant),getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_event))

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
            dropdown.adapter = adapter

            if(check_credit == "participant"){
                initial = true
                dropdown.setSelection(1)
                findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.VISIBLE)
                findViewById<TextView>(R.id.donation_check_credit_entity_name).text = (check_credit_value.get("first_name") as String) + " " + (check_credit_value.get("last_name") as String)
                findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(View.GONE)
            } else if (check_credit == "event"){
                initial = true;
                findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.VISIBLE)
                findViewById<TextView>(R.id.donation_check_credit_entity_name).text = (intent.getStringExtra("event_name").toString() )
                findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(View.GONE)
                findViewById<LinearLayout>(R.id.split_check_credit_select_row).setVisibility(View.GONE)
                findViewById<LinearLayout>(R.id.split_check_credit_rows).setVisibility(View.GONE)
                findViewById<LinearLayout>(R.id.split_check_credit_totals).setVisibility(View.GONE)
                dropdown.setSelection(2)
            }

            findViewById<LinearLayout>(R.id.select_check_credit_entity_link).setOnClickListener{
                checkCreditClicked();
            }

            findViewById<LinearLayout>(R.id.select_check_split_credit_entity_link).setOnClickListener{
                splitCheckCreditClicked();
            }

            findViewById<LinearLayout>(R.id.member_select_check_split_credit_entity_link).setOnClickListener{
                memberSplitCheckCreditClicked();
            }

            dropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    //println("NOTHING SELECTED")
                }

                override fun onItemSelected(parent: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                    if(!initial) {
                        if (position == 1) {
                            checkCreditSelected("participant")
                        } else if (position == 2) {
                            checkCreditSelected("event")
                        } else {
                            check_credit = ""
                            findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(View.GONE)
                            findViewById<LinearLayout>(R.id.split_check_credit_select_row).setVisibility(View.GONE)
                            findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.GONE)
                        }
                    }else{
                        initial = false
                    }
                    checkForDepositAbility()
                    selectedItemView?.contentDescription = getString(R.string.mobile_donations_check_deposit_part_team_event_credit_label) + ", " + items[position]
                }
            }
        } else {
            val items = arrayOf(getResources().getString(R.string.mobile_donations_check_deposit_have_a_check_please_select),getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_participant),getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_team),getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_event))

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
            dropdown.adapter = adapter

            if(check_credit == "participant"){
                initial = true
                dropdown.setSelection(1)
                findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.VISIBLE)
                findViewById<TextView>(R.id.donation_check_credit_entity_name).text = (check_credit_value.get("first_name") as String) + " " + (check_credit_value.get("last_name") as String)
                findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(View.GONE)
            }else if (check_credit == "team"){
                initial = true
                dropdown.setSelection(2)
                findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.VISIBLE)
                findViewById<TextView>(R.id.donation_check_credit_entity_name).text = (check_credit_value.get("team_name") as String)
                findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(View.GONE)
            } else if (check_credit == "event"){
                initial = true;
                findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.VISIBLE)
                findViewById<TextView>(R.id.donation_check_credit_entity_name).text = (intent.getStringExtra("event_name").toString() )
                findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(View.GONE)
                findViewById<LinearLayout>(R.id.split_check_credit_select_row).setVisibility(View.GONE)
                findViewById<LinearLayout>(R.id.split_check_credit_rows).setVisibility(View.GONE)
                findViewById<LinearLayout>(R.id.split_check_credit_totals).setVisibility(View.GONE)
                dropdown.setSelection(3)
            }

            findViewById<LinearLayout>(R.id.select_check_credit_entity_link).setOnClickListener{
                checkCreditClicked()
            }

            findViewById<LinearLayout>(R.id.select_check_split_credit_entity_link).setOnClickListener{
                splitCheckCreditClicked();
            }

            findViewById<LinearLayout>(R.id.member_select_check_split_credit_entity_link).setOnClickListener{
                memberSplitCheckCreditClicked();
            }

            dropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    println("NOTHING SELECTED")
                }

                override fun onItemSelected(parent: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                    if(!initial) {
                        if (position == 1) {
                            checkCreditSelected("participant")
                        } else if (position == 2) {
                            checkCreditSelected("team")
                        } else if (position == 3) {
                            checkCreditSelected("event")
                        } else {
                            check_credit = ""
                            findViewById<LinearLayout>(R.id.split_check_credit_select_row).setVisibility(View.GONE)
                            findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(View.GONE)
                            findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.GONE)
                        }
                    }else{
                        initial = false
                    }
                    checkForDepositAbility()
                    selectedItemView?.contentDescription = getString(R.string.mobile_donations_check_deposit_part_team_event_credit_label) + ", " + items[position]
                }
            }
        }
    }

    fun loadMemberSplitsRows(){
        val parent = findViewById<LinearLayout>(R.id.member_split_check_credit_rows)
        removeAllChildren(parent);

        val inflater = layoutInflater
        for (i in 0..creditSplits.size - 1) {
            val binding: CheckDepositCreditSplitRowBinding = DataBindingUtil.inflate(
                inflater, R.layout.check_deposit_credit_split_row, parent, true)
            binding.colorList = getColorList("")

            val root = ((binding.root as LinearLayout).getChildAt(0) as LinearLayout)
            (root.getChildAt(0) as TextView).text = creditSplits[i].name
            (root.getChildAt(2) as EditText).setText(formatDoubleToLocalizedCurrency(creditSplits[i].amount))
            (root.getChildAt(2) as EditText).addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                    updateMemberSplitTotals()
                }
            })

            (root.getChildAt(3) as LinearLayout).setOnClickListener{
                removeCreditSplit(i)
            }
        };

        if(creditSplits.size >= 5){
            findViewById<LinearLayout>(R.id.member_split_check_credit_select_row).visibility = View.GONE
        }else if(creditSplits.size > 0){
            findViewById<LinearLayout>(R.id.member_split_check_credit_select_row).visibility = View.VISIBLE
            findViewById<LinearLayout>(R.id.member_split_check_credit_rows).setVisibility(View.VISIBLE)
            findViewById<LinearLayout>(R.id.member_split_check_credit_totals).setVisibility(View.VISIBLE)
        }else{
            findViewById<LinearLayout>(R.id.member_split_check_credit_select_row).visibility = View.VISIBLE
            findViewById<LinearLayout>(R.id.member_split_check_credit_rows).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.member_split_check_credit_totals).setVisibility(View.GONE)
        }

        updateMemberSplitTotals();
    }

    fun loadSplitsRows(){
        val parent = findViewById<LinearLayout>(R.id.split_check_credit_rows)
        removeAllChildren(parent);

        val inflater = layoutInflater
        for (i in 0..creditSplits.size - 1) {
            val binding: CheckDepositCreditSplitRowBinding = DataBindingUtil.inflate(
                inflater, R.layout.check_deposit_credit_split_row, parent, true)
            binding.colorList = getColorList("")

            val root = ((binding.root as LinearLayout).getChildAt(0) as LinearLayout)
            (root.getChildAt(0) as TextView).text = creditSplits[i].name
            (root.getChildAt(2) as EditText).setText(formatDoubleToLocalizedCurrency(creditSplits[i].amount))
            (root.getChildAt(2) as EditText).addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(s: CharSequence, start: Int,count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int,before: Int, count: Int) {
                    updateSplitTotals()
                }
            })

            (root.getChildAt(3) as LinearLayout).setOnClickListener{
                removeCreditSplit(i)
            }
        };

        if(creditSplits.size >= 5){
            findViewById<LinearLayout>(R.id.split_check_credit_select_row).visibility = View.GONE
        }else if(creditSplits.size > 0){
            findViewById<LinearLayout>(R.id.split_check_credit_select_row).visibility = View.VISIBLE
            findViewById<LinearLayout>(R.id.split_check_credit_rows).setVisibility(View.VISIBLE)
            findViewById<LinearLayout>(R.id.split_check_credit_totals).setVisibility(View.VISIBLE)
        }else{
            findViewById<LinearLayout>(R.id.split_check_credit_select_row).visibility = View.VISIBLE
            findViewById<LinearLayout>(R.id.split_check_credit_rows).setVisibility(View.GONE)
            findViewById<LinearLayout>(R.id.split_check_credit_totals).setVisibility(View.GONE)
        }

        updateSplitTotals();
    }

    fun updateSplitTotals(){
        val parent = findViewById<LinearLayout>(R.id.split_check_credit_rows)
        var valid_value = true;
        split_value_total = 0.00;

        var index = 0;
        for (childView in parent.children) {
            val value = (((parent.getChildAt(index) as LinearLayout).getChildAt(0) as LinearLayout).getChildAt(2) as EditText).text.toString();
            var valid = isValidCurrency(value.trim()) && value.trim() != "" && value != "0" && value != "0." && value != "0.0" && value != "0.00"
            try{
                if(valid) {
                    split_value_total += value.toDouble()
                    creditSplits[index].amount = value.toDouble()
                }else{
                    valid_value = false;
                }
            }catch(e: Exception){
                valid_value = false;
            }
            index += 1;
        }

        findViewById<TextView>(R.id.split_check_credit_total).text = formatDoubleToLocalizedCurrency(split_value_total);

        if(creditSplits.size > 0){
            findViewById<LinearLayout>(R.id.split_check_credit_total_row).visibility = View.VISIBLE
        }else{
            findViewById<LinearLayout>(R.id.split_check_credit_total_row).visibility = View.GONE
        }

        if(valid_value){
            findViewById<LinearLayout>(R.id.split_check_credit_invalid_format).visibility = View.GONE
        }else{
            findViewById<View>(R.id.split_check_credit_total_row_line).visibility = View.VISIBLE
           // findViewById<LinearLayout>(R.id.split_check_credit_total_row).visibility = View.GONE
            findViewById<LinearLayout>(R.id.split_check_credit_invalid_format).visibility = View.VISIBLE
        }

        checkForDepositAbility()
    };

    fun updateMemberSplitTotals(){
        val parent = findViewById<LinearLayout>(R.id.member_split_check_credit_rows)
        var valid_value = true;
        split_value_total = 0.00;

        var index = 0;
        for (childView in parent.children) {
            val value = (((parent.getChildAt(index) as LinearLayout).getChildAt(0) as LinearLayout).getChildAt(2) as EditText).text.toString();
            var valid = isValidCurrency(value.trim()) && value.trim() != "" && value != "0" && value != "0." && value != "0.0" && value != "0.00"
            try{
                if(valid) {
                    split_value_total += value.toDouble()
                    creditSplits[index].amount = value.toDouble()
                }else{
                    valid_value = false;
                }
            }catch(e: Exception){
                valid_value = false;
            }
            index += 1;
        }

        if(creditSplits.size > 0){
            findViewById<LinearLayout>(R.id.member_split_check_credit_total_row).visibility = View.VISIBLE 
        }else{
            findViewById<LinearLayout>(R.id.member_split_check_credit_total_row).visibility = View.GONE
        }

        findViewById<TextView>(R.id.member_split_check_credit_total).text = formatDoubleToLocalizedCurrency(split_value_total);

        if(valid_value){
            findViewById<LinearLayout>(R.id.member_split_check_credit_invalid_format).visibility = View.GONE
        }else{
            findViewById<View>(R.id.member_split_check_credit_total_row_line).visibility = View.VISIBLE
            //findViewById<LinearLayout>(R.id.member_split_check_credit_total_row).visibility = View.GONE
            findViewById<LinearLayout>(R.id.member_split_check_credit_invalid_format).visibility = View.VISIBLE
        }

        checkForDepositAbility()
    };

    fun removeCreditSplit(index: Int){
        var newlist = listOf<CreditMember>();
        for (i in 0..creditSplits.size - 1) {
            if(i != index){
                newlist += creditSplits[i];
            };
        };

        creditSplits = newlist;
        loadSplitsRows();
        loadMemberSplitsRows();
    }

    fun memberSplitCheckCreditClicked(){
        val entity_link = findViewById<LinearLayout>(R.id.member_select_check_split_credit_entity_link)
            displayAlert("findMember")
            setAlertSender(entity_link)
    }

    fun splitCheckCreditClicked(){
        val entity_link = findViewById<LinearLayout>(R.id.select_check_split_credit_entity_link)
        val is_captain = getStringVariable("IS_TEAM_CAPTAIN") == "true";
        if(getStringVariable("CHECK_DEPOSIT_EVENT_ID") != ""){
            if(getStringVariable("CHECK_EVENT_MANAGER") != "true"){
                sendGoogleAnalytics("check_deposit_find_member_show","check_deposit")
                displayAlert("findMember")
            }else{
                sendGoogleAnalytics("check_deposit_find_participant_show","check_deposit")
                displayAlert("findParticipant")
            }
            setAlertSender(entity_link)
        }else{
            displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_select_part_error))
            setAlertSender(entity_link)
        }
    }

    fun checkCreditClicked(){
        val entity_link = findViewById<LinearLayout>(R.id.select_check_credit_entity_link)
        if(check_credit == "participant"){
            sendGoogleAnalytics("check_deposit_find_participant_show","check_deposit")
            if(getStringVariable("CHECK_DEPOSIT_EVENT_ID") != ""){
                displayAlert("findParticipant")
                setAlertSender(entity_link)
            }else{
                displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_select_part_error))
                setAlertSender(entity_link)
            }
        }else if (check_credit == "team"){
            sendGoogleAnalytics("check_deposit_find_team_show","check_deposit")
            if(getStringVariable("CHECK_DEPOSIT_EVENT_ID") != ""){
                displayAlert("findTeam")
                setAlertSender(entity_link)
            }else{
                displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_select_team_error))
                setAlertSender(entity_link)
            }
        } else if (check_credit == "event"){
            sendGoogleAnalytics("check_deposit_find_event_show","check_deposit")
            displayAlert("findEvent")
            setAlertSender(entity_link)
        }
    }

    fun checkCreditSelected(type: String){
        if(getStringVariable("CHECK_DEPOSIT_SPLIT_CHECK_ENABLED") == "true"){
            checkForDepositAbility()
            findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(View.GONE)
            if(type == "participant") {
                if(creditSplits.size >= 5){
                    findViewById<LinearLayout>(R.id.split_check_credit_select_row).visibility = View.GONE
                }else if(creditSplits.size > 0){
                    findViewById<LinearLayout>(R.id.split_check_credit_select_row).visibility = View.VISIBLE
                    findViewById<LinearLayout>(R.id.split_check_credit_rows).setVisibility(View.VISIBLE)
                    findViewById<LinearLayout>(R.id.split_check_credit_totals).setVisibility(View.VISIBLE)
                }else{
                    findViewById<LinearLayout>(R.id.split_check_credit_select_row).visibility = View.VISIBLE
                    findViewById<LinearLayout>(R.id.split_check_credit_rows).setVisibility(View.GONE)
                    findViewById<LinearLayout>(R.id.split_check_credit_totals).setVisibility(View.GONE)
                }

                if (check_credit != "participant") {
                    findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.GONE)
                }
                check_credit = "participant"
                setVariable("CHECK_CREDIT", check_credit)
                findViewById<LinearLayout>(R.id.split_check_credit_select_row).setVisibility(View.VISIBLE)

                var is_captain = getStringVariable("IS_TEAM_CAPTAIN") == "true";
                if(getStringVariable("CHECK_EVENT_MANAGER") != "true"){
                    (findViewById<LinearLayout>(R.id.select_check_split_credit_entity_link).getChildAt(
                        0
                    ) as TextView).text =
                        getResources().getString(R.string.mobile_donations_check_deposit_have_a_check_team_members_select)
                }else{
                    (findViewById<LinearLayout>(R.id.select_check_split_credit_entity_link).getChildAt(
                        0
                    ) as TextView).text =
                        getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_select_multiple_part)
                }
            } else if (type == "team"){
                if (check_credit != "team") {
                    findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.GONE)
                }
                check_credit = "team"
                setVariable("CHECK_CREDIT", check_credit)
                findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(View.VISIBLE)
                (findViewById<LinearLayout>(R.id.select_check_credit_entity_link).getChildAt(
                    0
                ) as TextView).text =
                    getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_select_team)
                findViewById<LinearLayout>(R.id.split_check_credit_select_row).setVisibility(View.GONE)
                findViewById<LinearLayout>(R.id.split_check_credit_rows).setVisibility(View.GONE)
                findViewById<LinearLayout>(R.id.split_check_credit_totals).setVisibility(View.GONE)
            } else if (type == "event"){
                if (check_credit != "event") {
                    findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.GONE)
                }
                check_credit = "event"
                setVariable("CHECK_CREDIT", check_credit)
                if (getStringVariable("CHECK_DEPOSIT_EVENT_NAME") == "" || getStringVariable("CHECK_DEPOSIT_EVENT_ID") == "") {
                    findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(
                        View.VISIBLE
                    )
                    (findViewById<LinearLayout>(R.id.select_check_credit_entity_link).getChildAt(
                        0
                    ) as TextView).text =
                        getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_select_event)
                } else {
                    findViewById<LinearLayout>(R.id.split_check_credit_select_row).setVisibility(
                        View.GONE
                    )
                    findViewById<TextView>(R.id.donation_check_credit_entity_name).text =
                        getStringVariable("CHECK_DEPOSIT_EVENT_NAME")
                    findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.VISIBLE)
                    checkForDepositAbility()
                }
                findViewById<LinearLayout>(R.id.split_check_credit_select_row).setVisibility(View.GONE)
                findViewById<LinearLayout>(R.id.split_check_credit_rows).setVisibility(View.GONE)
                findViewById<LinearLayout>(R.id.split_check_credit_totals).setVisibility(View.GONE)
            }
        }else{
            if(type == "participant") {
                if (check_credit != "participant") {
                    findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.GONE)
                }
                check_credit = "participant"
                setVariable("CHECK_CREDIT", check_credit)
                findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(View.VISIBLE)
                (findViewById<LinearLayout>(R.id.select_check_credit_entity_link).getChildAt(
                    0
                ) as TextView).text =
                    getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_select_part)
            } else if (type == "team"){
                if (check_credit != "team") {
                    findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.GONE)
                }
                check_credit = "team"
                setVariable("CHECK_CREDIT", check_credit)
                findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(View.VISIBLE)
                (findViewById<LinearLayout>(R.id.select_check_credit_entity_link).getChildAt(
                    0
                ) as TextView).text =
                    getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_select_team)
            } else if (type == "event"){
                if (check_credit != "event") {
                    findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.GONE)
                }
                check_credit = "event"
                setVariable("CHECK_CREDIT", check_credit)
                if (getStringVariable("CHECK_DEPOSIT_EVENT_NAME") == "" || getStringVariable("CHECK_DEPOSIT_EVENT_ID") == "") {
                    findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(
                        View.VISIBLE
                    )
                    (findViewById<LinearLayout>(R.id.select_check_credit_entity_link).getChildAt(
                        0
                    ) as TextView).text =
                        getResources().getString(R.string.mobile_donations_check_deposit_part_team_event_select_event)
                } else {
                    findViewById<LinearLayout>(R.id.check_credit_select_row).setVisibility(
                        View.GONE
                    )
                    findViewById<TextView>(R.id.donation_check_credit_entity_name).text =
                        getStringVariable("CHECK_DEPOSIT_EVENT_NAME")
                    findViewById<LinearLayout>(R.id.check_credit_row).setVisibility(View.VISIBLE)
                    checkForDepositAbility()
                }
            }
        }
    }

    fun startMiSnapCamera(type: MiSnapSettings.UseCase){
        registerForActivityResult.launch(
            MiSnapWorkflowActivity.buildIntent(
                this,
                MiSnapWorkflowStep(
                    MiSnapSettings(
                        type,
                        license
                    ).apply {
                        //analysis.document.trigger = MiSnapSettings.Analysis.Document.Trigger.MANUAL
                        //analysis.document.enableEnhancedManual = true // Enabling hints in manual mode

                        //Disabling the image review screen
                        // workflow.add(
                        //     getString(R.string.misnapWorkflowDocumentAnalysisFlowDocumentAnalysisFragmentLabel),
                        //     DocumentAnalysisFragment.buildWorkflowSettings(reviewCondition = DocumentAnalysisFragment.ReviewCondition.NEVER)
                        // )
                    })
            )
        )
    }

    fun submitDonation(){
        var checked = 0
        var display_amount_publicly = "0";
        var cons_id = getConsID().toInt();
        var jwt = getAuth();
        var recognition_name = "";

        var event_manager = 0;
        var device_type = getDeviceType();

        if(device_type == "phone"){
            device_type = "mobile"
        }

        if(getStringVariable("CHECK_EVENT_MANAGER") == "true"){
            event_manager = 1;
        }

        if(findViewById<CheckBox>(R.id.cb_anonymous).isChecked()){
            checked = 1
        }

        val container = findViewById<LinearLayout>(R.id.error_container)
        for (childView in container.children) {
            container.removeView(childView);
        }

        val mainScrollView = findViewById<VerticalScrollView>(R.id.check_deposit_scrollview)
        mainScrollView.smoothScrollTo(0,0)

        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/check/deposit/")
        var json = ""
        if(getStringVariable("IS_EVENT_MANAGER_ONLY") == "true"){
            json += "{\"cons_id\":${cons_id},\"event_id\":${getStringVariable("CHECK_EVENT_MANAGER_EVENT_ID").toInt()}"
        }else{
            json += "{\"cons_id\":${cons_id},\"event_id\":${getEvent().event_id.toInt()}"
        }

        var version = android.os.Build.VERSION.RELEASE
        if(version == ""){
            version = android.os.Build.VERSION.SDK_INT.toString()
        }

        json += ",\"device_type\":\"${device_type}\""
        json += ",\"device_os\":\"android\""
        json += ",\"device_name\":\"${android.os.Build.MODEL}\""
        json += ",\"device_version\":\"${version}\""
        json += ",\"event_manager\":\"${event_manager}\""
        json += ",\"check_amount\":\"${check_amount}\""
        json += ",\"check_number\":\"${check_number}\""
        json += ",\"check_front\":\"${front_base64}\""
        json += ",\"check_back\":\"${back_base64}\""
        json += ",\"check_amount\":\"${check_amount}\""
        json += ",\"donor_type\":\"${donor_type}\""
        json += ",\"donor_address_1\":\"${address_1}\""
        json += ",\"donor_address_2\":\"${address_2}\""
        json += ",\"donor_state\":\"${state}\""
        json += ",\"donor_city\":\"${city}\""
        json += ",\"donor_zip\":\"${zip}\""
        json += ",\"donor_email\":\"${email}\""
        json += ",\"donor_anonymous\":\"${checked}\""

        if(getStringVariable("CHECK_DEPOSIT_ALLOW_RECOGNITION_NAME") == "true") {
            recognition_name = findViewById<EditText>(R.id.input_recognition_name).text.toString();
            json += ",\"recognition_name\":\"${recognition_name}\""
        }

        if(getStringVariable("CHECK_DEPOSIT_ALLOW_DISPLAY_AMOUNT_PUBLICLY") == "true"){
            if(findViewById<CheckBox>(R.id.cb_display_publicly).isChecked()){
                display_amount_publicly = "1"
            }
            json += ",\"display_amount_publicly\":\"${display_amount_publicly}\""
        }

        if(page_credit == "captain"){
            if(getStringVariable("CHECK_DEPOSIT_SPLIT_CHECK_ENABLED") == "true"){
                json = addSplitCreditToJson(json)
            }else{
                json += ",\"credit_cons_id\":${team_member_cons_id}"
                json += ",\"split_check\":${0}"
            }
            json += ",\"credit_event_id\":${getEvent().event_id}"
            json += ",\"credit_team_id\":${getStringVariable("TEAM_ID").toInt()}"
        }

        if(getStringVariable("CHECK_EVENT_MANAGER") == "true") {
            json += ",\"credit\":\"${check_credit}\""
            if(page_credit != "captain"){
                json += ",\"credit_cons_id\":${cons_id}"
                json += ",\"split_check\":${0}"
            }

            if(page_credit != "captain" && getStringVariable("CHECK_CREDIT_VALUE") != ""){
                val data = JSONObject(getStringVariable("CHECK_CREDIT_VALUE"))
                if(check_credit == "participant" && data.has("cons_id")) {
                    if(getStringVariable("CHECK_DEPOSIT_SPLIT_CHECK_ENABLED") == "true"){
                        json = addSplitCreditToJson(json)
                    }else{
                        json += ",\"credit_cons_id\":${(data.get("cons_id") as String).toInt()}"
                        json += ",\"split_check\":${0}"
                    }
                }
                if(page_credit != "captain" && check_credit == "team" && data.has("team_id")) {
                    json += ",\"credit_team_id\":${(data.get("team_id") as String).toInt()}"
                }
            }
            if(page_credit != "captain" && getStringVariable("CHECK_DEPOSIT_EVENT_ID") != "") {
                json += ",\"credit_event_id\":${getStringVariable("CHECK_DEPOSIT_EVENT_ID").toInt()}"
            }
        }else{
            if(page_credit == "captain"){
                json += ",\"credit\":\"participant\""
            }else{
                json += ",\"credit\":\"${page_credit}\""
                json += ",\"credit_cons_id\":${cons_id}"
                json += ",\"split_check\":${0}"
            }

            if(page_credit != "captain" && getStringVariable("HAS_TEAM") == "true"){
                json += ",\"credit_team_id\":${getStringVariable("TEAM_ID").toInt()}"
            }
            if(page_credit != "captain" && getEvent().event_id != "") {
                json += ",\"credit_event_id\":${getEvent().event_id.toInt()}"
            }
        }
        if(donor_type == "individual") {
            json += ",\"donor_first_name\":\"${first_name}\""
            json += ",\"donor_last_name\":\"${last_name}\""
        }else if (donor_type == "company"){
            json += ",\"donor_company_name\":\"${company_name}\""
        }

        json += "}"

        json = json.replace("\n","")

        val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()

        val body: RequestBody = json.toRequestBody(JSON)

        displayAlert("donationProcessing")
        var request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization" , "Bearer ".plus(jwt))
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .build()

        var client = OkHttpClient
            .Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    sendGoogleAnalytics("check_deposit_donation_error","check_deposit")
                    displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_error_message))
                    val jsonString = response.body?.string();
                    println(jsonString)
                    val obj = JSONObject(jsonString);
                    if(obj.has("message") == true) {
                        runOnUiThread {
                            val inflater = LayoutInflater.from(this@CheckDeposit)
                            val container = findViewById<LinearLayout>(R.id.error_container)
                            var errorJsonString = (obj.get("message") as String)
                            if (errorJsonString.contains("ERRORLIST:")) {
                                errorJsonString = errorJsonString.replace("ERRORLIST: ", "");
                                val errorJson = JSONArray(errorJsonString);
                                for (i in 0 until errorJson.length()) {
                                    val error = errorJson.getJSONObject(i);
                                    val error_row = inflater.inflate(
                                        R.layout.deposit_error_row,
                                        null
                                    ) as LinearLayout
                                    if (i > 0) {
                                        error_row.getChildAt(0).setVisibility(View.INVISIBLE)
                                    }
                                    (error_row.getChildAt(1) as TextView).text =
                                        (error.get("reason") as String).replace("\"","")
                                    container.addView(error_row)
                                }
                            } else if (obj.has("errorList") && obj.get("errorList") != "null") {
                                val errorJson  = obj.get("errorList") as JSONArray
                                for (i in 0 until errorJson.length()) {
                                    val error = errorJson.getJSONObject(i);
                                    var reason = "";
                                    if(error.has("reason")){
                                        try{
                                            val reasonObj = error.get("reason") as JSONObject
                                            reason = reasonObj.get("reason") as String
                                        }catch(e: Exception){
                                            if(error.get("reason") is String){
                                                reason = error.get("reason") as String
                                            }else if (error.get("reason") is JSONArray){
                                                val errorJson = error.get("reason") as JSONArray;
                                                for (i in 0 until errorJson.length()) {
                                                    val error = errorJson[i] as String
                                                    val error_row = inflater.inflate(
                                                        R.layout.deposit_error_row,
                                                        null
                                                    ) as LinearLayout
                                                    if (i > 0) {
                                                        error_row.getChildAt(0).setVisibility(View.INVISIBLE)
                                                    }
                                                    (error_row.getChildAt(1) as TextView).text = error
                                                    container.addView(error_row)
                                                }
                                            }
                                        }
                                    } else{
                                        reason = errorJsonString.replace("\"","")
                                    }

                                    val error_row = inflater.inflate(
                                        R.layout.deposit_error_row,
                                        null
                                    ) as LinearLayout
                                    if (i > 0) {
                                        error_row.getChildAt(0).setVisibility(View.INVISIBLE)
                                    }
                                    (error_row.getChildAt(1) as TextView).text = reason
                                    container.addView(error_row)
                                }
                            } else {
                                val error_row =
                                    inflater.inflate(R.layout.deposit_error_row, null) as LinearLayout
                                (error_row.getChildAt(1) as TextView).text = errorJsonString.replace("\"","")
                                container.addView(error_row)
                            }
                        }
                    }
                }else{
                    if(donor_type == "individual"){
                        displayAlert("donationSucceeded",arrayOf(first_name + " " + last_name,formatLocalizedCurrencyString(check_amount)))
                    }else{
                        displayAlert("donationSucceeded",arrayOf(company_name,formatLocalizedCurrencyString(check_amount)))
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("THERE WAS AN ERROR")
                println(e)
                displayAlert(getResources().getString(R.string.mobile_donations_check_deposit_error_message))
            }
        })
    }

    fun addSplitCreditToJson(json: String): String{
        var new_json = json;
        if(creditSplits.size > 1){
            new_json += ",\"split_check\":${1}"
        }else{
            new_json += ",\"split_check\":${0}"
        }

        for (i in 0..creditSplits.size - 1) {
            val credit = creditSplits[i]
            if(i == 0){
                new_json += ",\"credit_cons_id\":${credit.cons_id}"
                new_json += ",\"credit_amount\":${credit.amount}"
            }else{
                new_json += ",\"credit_cons_id_${i + 1}\":${credit.cons_id}"
                new_json += ",\"credit_amount_${i + 1}\":${credit.amount}"
            }
        }
        return new_json;
    }

    fun currencyString(str: String): String{
        if(str.contains('.')){
            val dotIndex = str.indexOf('.');
            if(dotIndex == (str.length - 2)){
                return str + "0";
            }else if (dotIndex == (str.length - 1)){
                return str + "00";
            }
        }else{
            return str + ".00";
        }
        return str;
    }

    fun isValidCurrency(str: String): Boolean{
        var valid = true
        if(str.contains(".")){
            val dotIndex = str.indexOf('.');
            if(isCharacterRepeated(str, ".")){
                valid = false
            }else if(str.length == dotIndex + 1) {
                valid = false
            }else if(str.length == dotIndex + 2) {
                valid = false
            }else if(str.substring(str.indexOf(".") + 1, str.length).length > 2){
                valid = false
            }
        }else{
            valid = false;
        }
        return valid
    }

    fun isCharacterRepeated(input: String, field:String): Boolean{
        return input.indexOf(field) != input.lastIndexOf(field)
    }

    private fun encodeImage(bm: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    class CreditMember(
        val name: String,
        var amount: Double,
        val cons_id: String
    )
}