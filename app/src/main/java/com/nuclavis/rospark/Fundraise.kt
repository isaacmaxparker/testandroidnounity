package com.nuclavis.rospark


import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import app.com.kotlinapp.OnSwipeTouchListener
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewManagerFactory
import com.nuclavis.rospark.databinding.FundraisingMessageBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date


class Fundraise : com.nuclavis.rospark.BaseActivity() {
    var currentSlideIndex = 0
    var totalSlideCount = 0
    var imageData = ""
    var fundraisingMessages = emptyList<FundraisingMessage>()
    var imageUrl = ""
    var isLoadingUrl = false;

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        recolorTheme()
        super.onCreate(savedInstanceState)
        setPageContent(R.layout.fundraise,"fundraise")
        setTitle(resources.getString(R.string.mobile_main_menu_fundraise))
        sendGoogleAnalytics("fundraise_view","fundraise")

        if(getStringVariable("FUNDRAISE_MESSAGES_PROMOTED") == "true"){
            val copy = findViewById<LinearLayout>(R.id.fundraise_messages_card)
            findViewById<LinearLayout>(R.id.fundraise_content).removeView(copy)
            findViewById<LinearLayout>(R.id.fundraise_content).addView(copy, 0)
        }

        loadQRCode()
        loadFundraisingMessageData()
        loadSocialOptOut()

        val fundraisingMessageLayout = findViewById<LinearLayout>(R.id.fundraise_messages_card)
        fundraisingMessageLayout.setOnTouchListener(object : OnSwipeTouchListener(this@Fundraise) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                switchSlide(currentSlideIndex + 1)
            }
            override fun onSwipeRight() {
                super.onSwipeRight()
                switchSlide(currentSlideIndex - 1)
            }
        })

        //BEGIN_FACEBOOK_CONTENT
        val linear = findViewById<LinearLayout>(R.id.fundraise_content)
        val inflater = layoutInflater
        var fb_fundraiser_enabled = getStringVariable("FACEBOOK_FUNDRAISER_ENABLED");
        if(fb_fundraiser_enabled == "true"){
            loadFacebookCard(inflater, linear, 2)
        }
        //END_FACEBOOK_CONTENT

        var ecardCard = findViewById<LinearLayout>(R.id.ecard_card)
        if(getStringVariable("AHA_ECARD_URL") == ""){
            ecardCard.setVisibility(View.GONE)
        }else{
            ecardCard.setVisibility(View.VISIBLE)
            findViewById<Button>(R.id.send_ecard_button).setOnClickListener{
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getStringVariable("AHA_ECARD_URL")))
                startActivity(browserIntent)
            }
        }

        val qrShareButton = findViewById<ImageView>(R.id.fundraise_qr_share_button)
        qrShareButton.setOnClickListener {
            shareQRDialog()
            sendSocialActivity("qr_code_share")
        }

        val imgView = findViewById<ImageView>(R.id.fundraise_qr_image)
        imgView.setOnClickListener {
            shareQRDialog()
            sendSocialActivity("qr_code_share")
        }

        val qrSaveButton = findViewById<ImageView>(R.id.fundraise_qr_save_button)
        qrSaveButton.setOnClickListener {
            sendSocialActivity("qr_code_share")
            try {
                val imgView = findViewById<ImageView>(R.id.fundraise_qr_image)
                val dw = imgView.getDrawable();
                saveImage(dw)
                displayAlert(getResources().getString(R.string.mobile_fundraise_save_dialog_success))
                setAlertSender(qrSaveButton)
            } catch (e: IOException) {
                displayAlert(getResources().getString(R.string.mobile_fundraise_save_dialog_error))
                setAlertSender(qrSaveButton)
            }
        }

        val qrEmailButton = findViewById<ImageView>(R.id.fundraise_qr_email_button)
        qrEmailButton.setOnClickListener {
            sendSocialActivity("qr_code_share")
            val bmpUri = getLocalBitmapUri(imgView)
            if (bmpUri != null) {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SENDTO
                shareIntent.setData(Uri.parse("mailto:"));
                shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                shareIntent.putExtra(Intent.EXTRA_TEXT, imageUrl);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val resolvedInfoActivities: List<ResolveInfo> = this.getPackageManager()
                    .queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY)

                for (ri in resolvedInfoActivities) {
                    println("Granting permission to - " + ri.activityInfo.packageName)
                    applicationContext.grantUriPermission(
                        ri.activityInfo.packageName,
                        bmpUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

                try {
                    val chooserIntent = Intent.createChooser(shareIntent, "Send mail...")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    this.startActivity(
                        chooserIntent
                    )
                } catch (ex: ActivityNotFoundException) {
                    println( "ERROR, THERE ARE NO EMAIL CLIENTS INSTALLED.")
                    displayAlert(getResources().getString(R.string.mobile_fundraise_share_dialog_error))
                    setAlertSender(qrEmailButton)
                }
            }else{
                displayAlert(getResources().getString(R.string.mobile_fundraise_share_dialog_error))
                setAlertSender(qrEmailButton)
            }
        }

        setTooltipText(R.id.ecard_help_button, R.string.mobile_fundraise_ecard_tooltip, R.string.mobile_fundraise_ecard)
        setTooltipText(R.id.qr_code_help_button,R.string.mobile_fundraise_qr_code_tooltip, R.string.mobile_fundraise_qr_code_title)
        setTooltipText(R.id.messages_help_button,R.string.mobile_fundraise_donations_tooltip, R.string.mobile_fundraise_donations)
        setTooltipText(R.id.social_optout_help_button, R.string.mobile_fundraise_social_optout_tooltip, R.string.mobile_fundraise_social_optout_title)

        val facebookShareButton = findViewById<FrameLayout>(R.id.facebook_share_button)
        facebookShareButton.setOnClickListener {
            sendSocialActivity("facebook")
            sendGoogleAnalytics("fundraise_facebook_share","fundraise")
            if(fundraisingMessages[currentSlideIndex].custom_content){
                shareFacebook(this,fundraisingMessages[currentSlideIndex].facebook_url)
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
                    fundraisingMessages[currentSlideIndex].subject
                )
                if (fundraisingMessages[currentSlideIndex].custom_content) {
                    intent.putExtra(Intent.EXTRA_TEXT, (fundraisingMessages[currentSlideIndex].email_url))
                } else {
                    intent.putExtra(
                        Intent.EXTRA_TEXT,
                        (fundraisingMessages[currentSlideIndex].email_body).replace("<br>","\r\n").plus("\r\n\r\n")
                            .plus(fundraisingMessages[currentSlideIndex].email_url)
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
            if(fundraisingMessages[currentSlideIndex].custom_content){
                intent.putExtra(Intent.EXTRA_TEXT, (fundraisingMessages[currentSlideIndex].sms_url))
            }else {
                intent.putExtra(Intent.EXTRA_TEXT,
                    fundraisingMessages[currentSlideIndex].text.plus(" ")
                        .plus(fundraisingMessages[currentSlideIndex].sms_url)
                )
            }
            startActivity(Intent.createChooser(intent,getResources().getString(R.string.mobile_fundraise_share_dialog_title)))
        }

        val linkedinShareButton = findViewById<FrameLayout>(R.id.linkedin_share_button)
        linkedinShareButton.setOnClickListener {
            sendGoogleAnalytics("fundraise_linkedin_share","fundraise")
            sendSocialActivity("linkedin")
            if(fundraisingMessages[currentSlideIndex].custom_content){
                shareLinkedIn(fundraisingMessages[currentSlideIndex].linkedin_url )
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

        if((getStringVariable("LOGIN_NUMBER") == "SECOND" && getStringVariable("RATE_DIALOG_SHOWN") != "true")){
            val manager = ReviewManagerFactory.create(this@Fundraise)
            val request = manager.requestReviewFlow()

            request.addOnCompleteListener { task ->
                try {
                    if (task.isSuccessful) {
                        val reviewInfo = task.result
                        val flow: Task<Void> = manager.launchReviewFlow(this@Fundraise, reviewInfo)
                        flow.addOnCompleteListener { task2 ->
                            setVariable("RATE_DIALOG_SHOWN","true")
                        }
                    }
                } catch (ex: java.lang.Exception) {}
            }
        }

        loadSendVideoCard()
    }

    override fun slideButtonCallback(card: Any, forward:Boolean){
        var currentIndex = currentSlideIndex;

        if(card == "messages"){
            if(forward){
                currentIndex += 1;
            }else{
                currentIndex -= 1;
            }
        }
        switchSlide(currentIndex)
    }

    private fun saveImage(drawable: Drawable) {
        val file = getDisc()

        if (!file.exists() && !file.mkdirs()) {
            file.mkdir()
        }

        val simpleDateFormat = SimpleDateFormat("yyyymmsshhmmss")
        val date = simpleDateFormat.format(Date())
        try {
            val draw = drawable as BitmapDrawable
            val bitmap = draw.bitmap
            MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Fundraising QR Code" , date);
        } catch (e: FileNotFoundException) {
            println("SAVE IMAGE -- FILE NOT FOUND ERROR")
        } catch (e: IOException) {
            println("SAVE IMAGE ERROR")
        }
    }

    private fun getDisc(): File {
        val file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File(file, "")
    }
    fun shareQRDialog(){
        sendGoogleAnalytics("fundraise_qr_code_copy","fundraise")
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.setType("text/html");
        shareIntent.putExtra(Intent.EXTRA_TEXT, imageUrl);
        startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.mobile_fundraise_qr_code_share_button_description)))
    }

    private fun loadQRCode(){
        val url = resources.getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/personalPageQRIncludeURL/").plus(getConsID()).plus("/").plus(getEvent().event_id).plus("/android/").plus(getDeviceType())
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
                    val jsonString = response.body?.string() as String
                    val obj = JSONObject(jsonString);
                    runOnUiThread{
                        loadSendLinkCard(getSafeStringVariable(obj, "instaTkTkUrl"))
                    }
                    if(obj.has("url")){
                        imageUrl = obj.get("url") as String
                    }
                    if(obj.has("image")){
                        val string = (obj.get("image") as String)
                        imageData = string.substring(string.indexOf("base64,") + 7)
                    }else{
                        imageData = jsonString.substring(jsonString.indexOf("base64,") + 7)
                    }
                    val decodedString: ByteArray = Base64.decode(imageData, Base64.DEFAULT)
                    val decodedByte =
                        BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                    runOnUiThread{
                        val qrView = findViewById<ImageView>(R.id.fundraise_qr_image)
                        if (decodedByte !== null) {
                            Glide.with(this@Fundraise)
                                .load(decodedByte)
                                .into(qrView)
                        } else {
                            qrView.visibility = View.INVISIBLE
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
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


    fun loadSendVideoCard() {
        if(getStringVariable("AHA_CPR_CARD") == "true"){
            findViewById<LinearLayout>(R.id.send_video_card).visibility = View.VISIBLE
            setTooltipText(R.id.send_video_help_button, R.string.mobile_fundraise_cpr_video_card_tooltip_android, R.string.mobile_fundraise_cpr_video_card_title)

            val image = getStringVariable("AHA_CPR_CARD_IMAGE");
            if(image != ""){
                Glide.with(this@Fundraise)
                    .load(image)
                    .into((findViewById<LinearLayout>(R.id.send_video_thumbnail).getChildAt(0) as ImageView))


                findViewById<LinearLayout>(R.id.send_video_thumbnail).setOnClickListener{
                    val send_video_link = getStringVariable("AHA_CPR_VIDEO")
                    if(send_video_link != ""){
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(send_video_link))
                        startActivity(browserIntent)
                    }
                }

                val string =  getString(R.string.mobile_fundraise_cpr_video_card_description_android).replace("PARTICIPANT_FIRST_NAME",getStringVariable("USER_FIRST_NAME").lowercase().capitalize())
                findViewById<TextView>(R.id.send_video_description).text = string;
                (findViewById<LinearLayout>(R.id.send_video_thumbnail).getChildAt(0) as ImageView).contentDescription = "CPR Video Link";
            }else{
                findViewById<LinearLayout>(R.id.send_video_card).visibility = View.GONE
            }
        }else{
            findViewById<LinearLayout>(R.id.send_video_card).visibility = View.GONE
        }

    }

    fun loadSendLinkCard(share_url: String){
        if(getStringVariable("SHARE_INSTAGRAM_TIKTOK_DISABLED") == "true"){
            findViewById<LinearLayout>(R.id.fundraise_share_link_card).visibility = View.GONE
        }else{
            setTooltipText(R.id.link_help_button, R.string.mobile_fundraise_share_link_help, R.string.mobile_fundraise_share_link_title )
            val linkCopyButton = findViewById<FrameLayout>(R.id.copy_link_button)
            linkCopyButton.setOnClickListener {
                val clipboard: ClipboardManager =
                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("personal page url", share_url)
                clipboard.setPrimaryClip(clip)
                displayAlert(getString(R.string.mobile_fundraise_share_link_copy_link_success))
            }

            findViewById<FrameLayout>(R.id.insta_button).setOnClickListener{
                val apppackage = "com.instagram.android"
                val cx: Context = this
                try {
                    val i = cx.packageManager.getLaunchIntentForPackage(apppackage)
                    cx.startActivity(i)
                } catch (e: java.lang.Exception) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://instagram.com/")
                        )
                    )
                }
            }

            findViewById<FrameLayout>(R.id.tiktok_button).setOnClickListener{
                val apppackage = "com.zhiliaoapp.musically"
                val cx: Context = this
                try {
                    val i = cx.packageManager.getLaunchIntentForPackage(apppackage)
                    cx.startActivity(i)
                } catch (e: java.lang.Exception) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://tiktok.com/")
                        )
                    )
                }
            }
        }
    }

    private fun loadFundraisingMessageData(){
        val url = resources.getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/messages/FUNDRAISING/").plus(getConsID()).plus("/").plus(getEvent().event_id).plus("/android/").plus(getDeviceType())
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
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)

                        var safeMessage = setupMessage(obj);
                        
                        val message = FundraisingMessage(
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
                        fundraisingMessages += message
                    }

                    runOnUiThread {
                        val fundraisingMessageLayout = findViewById<FrameLayout>(R.id.fundraising_messages_layout)
                        val inflater = LayoutInflater.from(this@Fundraise)
                        var i = 0
                        totalSlideCount = fundraisingMessages.count()
                        for (message in fundraisingMessages) {
                            val binding: FundraisingMessageBinding = DataBindingUtil.inflate(
                                inflater, R.layout.fundraising_message ,fundraisingMessageLayout, true)
                            binding.colorList = getColorList("")
                            val row = fundraisingMessageLayout.getChildAt(i) as TextView
                            row.text = message.text
                            if(i == 0){
                                row.visibility = View.VISIBLE
                            } else {
                                row.visibility = View.INVISIBLE
                            }
                            i += 1
                        }
                        totalSlideCount = i

                        setupSlideButtons(totalSlideCount, R.id.fundraise_messages_slide_buttons,"messages")
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

    fun loadSocialOptOut(){

        val cb = findViewById<CheckBox>(R.id.social_opt_out)

        if(getStringVariable("AHA_SOCIAL_OPTOUT_ENABLED") == "true"){
            val url = resources.getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/getOptOutFlag/").plus(getConsID()).plus("/").plus(getEvent().event_id)
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                .build()

            val client = OkHttpClient()
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.code != 200) {
                        throw Exception(response.body?.string())
                    } else {
                        val jsonString = response.body?.string()
                        println("JOSN STRING: " + jsonString)
                        val obj = JSONObject(jsonString)
                        if(obj.has("data")){
                            val optout = (obj.get("data") as JSONObject).get("socialOptOut") as Boolean

                            runOnUiThread {
                                cb.isChecked = optout
                            }
                        }else {
                            findViewById<LinearLayout>(R.id.social_optout_card).setVisibility(View.GONE)
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    findViewById<LinearLayout>(R.id.social_optout_card).setVisibility(View.GONE)
                }
            })

            cb.setOnClickListener{
               if(cb.isChecked){
                   val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/addAction/").plus(getConsID()).plus("/").plus(getEvent().event_id).plus("/opt_out")
                   var request = Request.Builder()
                       .url(url)
                       .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                       .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                       .build()

                   var client = OkHttpClient();

                   client.newCall(request).enqueue(object : Callback {
                       override fun onResponse(call: Call, response: okhttp3.Response) {
                           println("DISABLING REPSONSE")
                           println(response)
                       }
                       override fun onFailure(call: Call, e: IOException) {
                           println(e.message.toString());
                       }
                   })
               }else{
                   val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/clearOptOutFlag/").plus(getConsID()).plus("/").plus(getEvent().event_id)
                   var request = Request.Builder()
                       .url(url)
                       .addHeader("Authorization" , "Bearer ".plus(getAuth()))
                       .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
                       .build()

                   var client = OkHttpClient();

                   client.newCall(request).enqueue(object : Callback {
                       override fun onResponse(call: Call, response: okhttp3.Response) {
                           println("CLEARING FLAG REPSONSE")
                           println(response)
                       }
                       override fun onFailure(call: Call, e: IOException) {
                           println(e.message.toString());
                       }
                   })
               }
            }
        }else{
            findViewById<LinearLayout>(R.id.social_optout_card).setVisibility(View.GONE)
        }
    }

    override fun childviewCallback(string: String, data:String){
        hideAlert();
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("fundraising message", fundraisingMessages[currentSlideIndex].text)
        clipboard.setPrimaryClip(clip)
        if(string == "linkedin"){
            shareLinkedIn(Uri.encode(fundraisingMessages[currentSlideIndex].linkedin_url))
        }else if(string == "facebook") {
            shareFacebook(this@Fundraise, fundraisingMessages[currentSlideIndex].facebook_url)
        }
    }

    fun shareLinkedIn(url: String){
        val url = "https://www.linkedin.com/shareArticle?mini=true&url=".plus(url)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
    }

    fun switchSlide(newIndex:Int){
        switchSlideButton(newIndex + 1,totalSlideCount,R.id.fundraise_messages_slide_buttons)
        val fundraisingMessageLayout = findViewById<FrameLayout>(R.id.fundraising_messages_layout)
        if((newIndex >= 0) and (newIndex < totalSlideCount)){
            fundraisingMessageLayout.getChildAt(currentSlideIndex).visibility = View.INVISIBLE
            fundraisingMessageLayout.getChildAt(newIndex).visibility = View.VISIBLE
            fundraisingMessageLayout.getChildAt(newIndex).requestFocus()
            currentSlideIndex = newIndex
        }
    }

    class FundraisingMessage(
        val text: String,
        val email_body: String,
        val subject: String,
        val url: String,
        val facebook_url: String,
        val linkedin_url: String,
        val email_url: String,
        val sms_url: String,
        val custom_content: Boolean
    )
}