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
import android.os.StrictMode
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import app.com.kotlinapp.OnSwipeTouchListener
import com.bumptech.glide.Glide
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

class Recruit : BaseActivity() {
    var totalMessagesSlideCount = 0;
    var currentMessagesSlideIndex = 0;
    var team_messages = emptyList<TeamMessage>();
    var imageData = ""
    var imageUrl = ""

    @RequiresApi(Build.VERSION_CODES.O)
    public override fun onCreate(savedInstanceState: Bundle?) {
        recolorTheme()
        super.onCreate(savedInstanceState)
        setPageContent(R.layout.recruit, "recruit")
        setTitle(getResources().getString(R.string.mobile_main_menu_recruit));
        //sendGoogleAnalytics("teams_view","teams")
        loadQRCode()
        loadMessagesData()
        setTooltipText(R.id.messages_help_button,R.string.mobile_recruit_messages_tooltip, R.string.mobile_recruit_messages_title)
        setTooltipText(R.id.qr_code_help_button,R.string.mobile_recruit_qr_code_tooltip, R.string.mobile_recruit_qr_code_title)

        var teamsMessageLayout = findViewById<FrameLayout>(R.id.recruit_messages_layout);
        val inflater = LayoutInflater.from(this@Recruit)
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
        findViewById<LinearLayout>(R.id.recruit_messages_card).setOnTouchListener(object : OnSwipeTouchListener(this@Recruit) {
            override fun onSwipeLeft() {
                super.onSwipeLeft();
                switchMessageSlide(currentMessagesSlideIndex + 1);
            }
            override fun onSwipeRight() {
                super.onSwipeRight();
                switchMessageSlide(currentMessagesSlideIndex - 1);
            }
        })

        val qrShareButton = findViewById<ImageView>(R.id.fundraise_qr_share_button)
        qrShareButton.setOnClickListener {
            shareQRDialog()
        }

        val imgView = findViewById<ImageView>(R.id.fundraise_qr_image)
        imgView.setOnClickListener {
            shareQRDialog()
        }

        val qrSaveButton = findViewById<ImageView>(R.id.fundraise_qr_save_button)
        qrSaveButton.setOnClickListener {
            try {
                val imgView = findViewById<ImageView>(R.id.fundraise_qr_image)
                val dw = imgView.getDrawable();
                saveImage(dw)
                displayAlert(getResources().getString(R.string.mobile_fundraise_save_dialog_success))
                setAlertSender(qrSaveButton)
            } catch (e: IOException) {
                println("QR SAVE BUTTON ERROR")
                displayAlert(getResources().getString(R.string.mobile_fundraise_save_dialog_error))
                setAlertSender(qrSaveButton)
            }
        }

        val qrEmailButton = findViewById<ImageView>(R.id.fundraise_qr_email_button)
        qrEmailButton.setOnClickListener {
            val builder = StrictMode.VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
            val imgView = findViewById<ImageView>(R.id.fundraise_qr_image)
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
                    println("ERROR, THERE ARE NO EMAIL CLIENTS INSTALLED.")
                    displayAlert(getResources().getString(R.string.mobile_fundraise_share_dialog_error))
                    setAlertSender(qrEmailButton)
                }
            }else{
                displayAlert(getResources().getString(R.string.mobile_fundraise_share_dialog_error))
                setAlertSender(qrEmailButton)
            }
        }

        val facebookShareButton = findViewById<FrameLayout>(R.id.facebook_share_button)
        facebookShareButton.setOnClickListener {
            sendGoogleAnalytics("teams_facebook_share","teams")
            sendSocialActivity("facebook")
            if(team_messages[currentMessagesSlideIndex].custom_content){
                shareFacebook(this,team_messages[currentMessagesSlideIndex].facebook_url)
            }else{
                displayAlert(
                    resources.getString(R.string.mobile_fundraise_message_facebook_prompt), ""
                ) { childviewCallback("facebook","") }
                setAlertSender(facebookShareButton)
            }

        }

        val emailShareButton = findViewById<FrameLayout>(R.id.email_share_button)
        emailShareButton.setOnClickListener {
            sendGoogleAnalytics("teams_email_share","teams")
            sendSocialActivity("email")
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:") // only email apps should handle this
            intent.putExtra(Intent.EXTRA_SUBJECT, team_messages[currentMessagesSlideIndex].subject)
            if(team_messages[currentMessagesSlideIndex].custom_content){
                intent.putExtra(Intent.EXTRA_TEXT, (team_messages[currentMessagesSlideIndex].email_url))
            }else {
                intent.putExtra(Intent.EXTRA_TEXT,
                    (team_messages[currentMessagesSlideIndex].email_body).replace("<br>","\r\n").plus("\r\n\r\n")
                        .plus(team_messages[currentMessagesSlideIndex].email_url)
                )
            }
            startActivity(Intent.createChooser(intent,getResources().getString(R.string.mobile_fundraise_share_dialog_title)))
        }

        val smsShareButton = findViewById<FrameLayout>(R.id.sms_share_button)
        smsShareButton.setOnClickListener {
            sendGoogleAnalytics("teams_sms_share","teams")
            sendSocialActivity("sms")
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("smsto:") // only email apps should handle this
            //intent.putExtra(Intent.EXTRA_SUBJECT, fundraisingMessages[currentSlideIndex].subject)
            if(team_messages[currentMessagesSlideIndex].custom_content){
                intent.putExtra(Intent.EXTRA_TEXT, (team_messages[currentMessagesSlideIndex].sms_url))
            }else {
                intent.putExtra(Intent.EXTRA_TEXT,
                    team_messages[currentMessagesSlideIndex].text.plus(" ")
                        .plus(team_messages[currentMessagesSlideIndex].sms_url)
                )
            }
            startActivity(Intent.createChooser(intent,getResources().getString(R.string.mobile_fundraise_share_dialog_title)))
        }

        val linkedinShareButton = findViewById<FrameLayout>(R.id.linkedin_share_button)
        linkedinShareButton.setOnClickListener {
            sendGoogleAnalytics("teams_linkedin_share","teams")
            sendSocialActivity("linkedin")
            if(team_messages[currentMessagesSlideIndex].custom_content){
                shareLinkedIn(team_messages[currentMessagesSlideIndex].linkedin_url )
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

    override fun slideButtonCallback(card: Any, forward:Boolean){
        var currentIndex = currentMessagesSlideIndex;

        if(card == "messages"){
            if(forward){
                currentIndex += 1;
            }else{
                currentIndex -= 1;
            }
        }
        switchMessageSlide(currentIndex)
    }

    fun shareLinkedIn(url: String){
        val url = "https://www.linkedin.com/shareArticle?mini=true&url=".plus(url)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(browserIntent)
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
            shareFacebook(this@Recruit, team_messages[currentMessagesSlideIndex].facebook_url)
        }
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
            println("SAVE IMAGE FILE NOT FOUND ERROR")
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
        startActivity(Intent.createChooser(shareIntent,  getResources().getString(R.string.mobile_fundraise_qr_code_share_button_description)))
    }

    private fun loadQRCode(){
        val url = resources.getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/events/teamPageQRIncludeURL/").plus(getConsID()).plus("/").plus(getEvent().event_id).plus("/android/").plus(getDeviceType())

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
                            Glide.with(this@Recruit)
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

    fun loadMessagesData(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/messages/TEAMS/").plus(getConsID()).plus("/").plus(getEvent().event_id).plus("/android/").plus(getDeviceType())
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
                        var donationsMessageLayout = findViewById<FrameLayout>(R.id.recruit_messages_layout);
                        val inflater = LayoutInflater.from(this@Recruit)
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
                        setupSlideButtons(totalMessagesSlideCount, R.id.recruit_messages_slide_buttons,"messages")
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
            }
        })
    }

    public fun switchMessageSlide(newIndex:Int){
        var donationMessageLayout = findViewById<FrameLayout>(R.id.recruit_messages_layout);
        switchSlideButton(newIndex + 1,totalMessagesSlideCount,R.id.recruit_messages_slide_buttons)
        if((newIndex >= 0) and (newIndex < totalMessagesSlideCount)){
            donationMessageLayout.getChildAt(currentMessagesSlideIndex).setVisibility(View.INVISIBLE);
            donationMessageLayout.getChildAt(newIndex).setVisibility(View.VISIBLE);
            currentMessagesSlideIndex = newIndex;
        }
    }

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
