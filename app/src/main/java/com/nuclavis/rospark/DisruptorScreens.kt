package com.nuclavis.rospark;

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.databinding.DataBindingUtil
import app.com.kotlinapp.OnSwipeTouchListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.nuclavis.rospark.databinding.DisruptorScreenBinding
import com.nuclavis.rospark.databinding.DisruptorScreensPageBinding
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.text.style.ForegroundColorSpan

open class DisruptorScreens : BaseLanguageActivity() {

    var currentIndex = 0
    var totalSlideCount = 0

    override fun childviewCallback(string: String, data: String) {

    }

    override fun slideButtonCallback(card: Any, forward: Boolean) {
        var indx = currentIndex;
        if(card == "disruptors"){
            if(forward){
                indx += 1;
            }else{
                indx -= 1;
            }
        }
        switchSlide(indx)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: DisruptorScreensPageBinding = DataBindingUtil.setContentView(
            this, R.layout.disruptor_screens_page)
        binding.colorList = getColorList("")
        val background = findViewById<ImageView>(R.id.page_background)
        if((getStringVariable("TILE_BACKGROUND_WHITE_ENABLED") == "true")){
            try{
                if(getStringVariable("BACKGROUND_IMAGE_ENABLED") == "true"){
                    val media = getStringVariable("CUSTOM_BACKGROUND_URL")
                    val drawableId = getStringVariable("CUSTOM_BACKGROUND_DRAWABLE").toInt()

                    if(drawableId != 0){
                        background.setImageDrawable(getDrawable(drawableId))
                    }else{
                        val requestOptions: RequestOptions = RequestOptions
                            .diskCacheStrategyOf(DiskCacheStrategy.ALL)
                        if (media !== null) {
                            Glide.with(this)
                                .load(media)
                                .apply(requestOptions)
                                .skipMemoryCache(false)
                                .into(background)
                        } else {
                            background.setImageResource(android.R.color.transparent);
                            background.setBackgroundColor(Color.WHITE)
                        }
                    }
                }
            }catch(e: Exception){
                background.setImageResource(android.R.color.transparent);
                background.setBackgroundColor(Color.WHITE)
            }
        }

        val disruptorScreenLayout = findViewById<LinearLayout>(R.id.disruptor_screens_container)
        disruptorScreenLayout.setOnTouchListener(object :
            OnSwipeTouchListener(this@DisruptorScreens) {

            override fun onSwipeLeft() {
                super.onSwipeLeft()
                switchSlide(currentIndex + 1)

            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                switchSlide(currentIndex - 1)
            }
        })


        var eventid = getEvent().event_id;
        var consid = getConsID();

        if(eventid == ""){
            eventid = intent.getStringExtra("event_id").toString()
        }

        if(consid == ""){
            consid = intent.getStringExtra("cons_id").toString()
            setConsID(consid)
        }

        getDisruptors(eventid, consid);
    }

    fun getDisruptors(event_id: String, cons_id: String){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/getDisruptions/").plus(cons_id).plus("/").plus(event_id)
        var request = Request.Builder()
            .url(url)
            .addHeader("Program-Id" , getStringVariable("PROGRAM_ID"))
            .addHeader("Authorization" , "Bearer ".plus(getAuth()))
            .build()

        var client = OkHttpClient();
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if(response.code != 200){
                    showDefaultPage()
                    //throw Exception(response.body?.string())
                }else{
                    runOnUiThread {
                        val jsonString = response.body?.string();
                        val jsonResponse = JSONObject(jsonString)
                        if (jsonResponse.has("disruptions") && jsonResponse.get("disruptions") is JSONArray) {
                            val jsonArray = jsonResponse.get("disruptions") as JSONArray
                            if (jsonArray.length() > 0) {
                                lateinit var close:LinearLayout
                                totalSlideCount = jsonArray.length()
                                val container = findViewById<FrameLayout>(R.id.disruptor_screens)
                                for (i in 0 until jsonArray.length()) {
                                    val event = jsonArray.getJSONObject(i)
                                    val inflater = LayoutInflater.from(this@DisruptorScreens)
                                    val binding: DisruptorScreenBinding = DataBindingUtil.inflate(
                                        inflater, R.layout.disruptor_screen, container, true
                                    )
                                    binding.colorList = getColorList("");

                                    var eventTitle = getSafeStringVariable(event, "title")

                                    val layout = (binding.root as LinearLayout).getChildAt(0) as LinearLayout
                                    if(eventTitle.isNotBlank()){
                                        ((layout.getChildAt(0) as LinearLayout).getChildAt(0) as TextView).text = eventTitle
                                    }

                                    val logo_view = layout.getChildAt(1) as LinearLayout;
                                    val video_view = (layout.getChildAt(2) as FrameLayout).getChildAt(0) as VideoView;
                                    val video_anchor_view = layout.getChildAt(2) as FrameLayout;
                                    val image_view = layout.getChildAt(3) as ImageView;
                                    val text_view = layout.getChildAt(4) as TextView;
                                    val close_button = layout.getChildAt(5) as LinearLayout;

                                    val closeDisruptionButton = close_button.findViewById<Button>(R.id.close_disruption_screen_button)

                                    val content = SpannableString(getString(R.string.mobile_disruptions_close))
                                    content.setSpan(UnderlineSpan(), 0, content.length, 0)
                                    content.setSpan(ForegroundColorSpan(Color.parseColor(getStringVariable("PRIMARY_COLOR"))), 0, content.length, 0)

                                    closeDisruptionButton.text = content

                                    closeDisruptionButton.setOnClickListener({
                                        showDefaultPage()
                                    })

                                    if(event.has("logo_url") && event.get("logo_url") is String){
                                        Glide.with(this@DisruptorScreens)
                                            .load(event.get("logo_url") as String)
                                            .into(logo_view.getChildAt(0) as ImageView)
                                        logo_view.setVisibility(View.VISIBLE)
                                    }else{
                                        logo_view.setVisibility(View.GONE)
                                    }

                                    try {
                                        if (event.has("photo_url") && event.get("photo_url") is String) {
                                            Glide.with(this@DisruptorScreens)
                                                .load(event.get("photo_url") as String)
                                                .into(image_view)
                                            image_view.setVisibility(View.VISIBLE)
                                        } else {
                                            image_view.setVisibility(View.GONE)
                                        }
                                    }catch(e: Exception){
                                        image_view.setVisibility(View.GONE)
                                    }

                                    if(event.has("video_url") && event.get("video_url") is String && event.get("video_url") != ""){
                                        val uri = Uri.parse(event.get("video_url") as String)
                                        val mc = MediaController(this@DisruptorScreens)
                                        mc.setAnchorView(video_anchor_view)
                                        video_view.setMediaController(mc)
                                        video_view.setVideoURI(uri)
                                        video_view.setVisibility(View.VISIBLE)

                                        video_view.setOnClickListener(
                                            {
                                                mc.show()
                                            }
                                        )


                                    }else{
                                        video_view.setVisibility(View.GONE)
                                    }

                                    //video_view.setOnClickListener({
                                    //video_view.start()
                                    //})

                                    if(event.has("message") && event.get("message") is String){
                                        text_view.text = event.get("message") as String
                                        text_view.setVisibility(View.VISIBLE)
                                    }else{
                                        text_view.setVisibility(View.GONE)
                                    }

                                    if(i == 0){
                                        binding.root.setVisibility(View.VISIBLE)
                                        if(video_view.visibility != View.GONE){
                                            video_view.start()
                                        }
                                        close_button.requestFocus()
                                        close_button.isFocusableInTouchMode = true
                                        close_button.isFocusable = true
                                        close_button.requestFocus()
                                        close_button.getChildAt(0).requestFocus()
                                        close = close_button;

                                    }else{
                                        binding.root.setVisibility(View.GONE)
                                    }
                                }
                                setupSlideButtons(
                                    jsonArray.length(),
                                    R.id.disruptor_screens_slide_buttons,
                                    "disruptors"
                                )
                                findViewById<LinearLayout>(R.id.disruptor_screens_container).setVisibility(View.VISIBLE)
                                close.isFocusableInTouchMode = true
                                close.isFocusable = true
                                close.requestFocus()
                                close.getChildAt(0).requestFocus()

                            } else {
                                showDefaultPage()
                            }
                        } else {
                            showDefaultPage()
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

    fun getVideoView(linear: LinearLayout): VideoView{
        return (((linear.getChildAt(0) as LinearLayout).getChildAt(2) as FrameLayout).getChildAt(0) as VideoView)
    }

    fun switchSlide(newIndex:Int){
        val disruptorScreenLayout = findViewById<FrameLayout>(R.id.disruptor_screens)
        if((newIndex >= 0) and (newIndex < totalSlideCount)){
            switchSlideButton(newIndex + 1,totalSlideCount,R.id.disruptor_screens_slide_buttons)
            disruptorScreenLayout.getChildAt(currentIndex).visibility = View.GONE
            val oldVideoView = getVideoView((disruptorScreenLayout.getChildAt(currentIndex) as LinearLayout))
            oldVideoView.stopPlayback();
            oldVideoView.resume();
            disruptorScreenLayout.getChildAt(newIndex).visibility = View.VISIBLE
            (disruptorScreenLayout.getChildAt(newIndex) as LinearLayout).getChildAt(0).requestFocus()
            val newVideoView = getVideoView((disruptorScreenLayout.getChildAt(newIndex) as LinearLayout))
            if(newVideoView.visibility != View.GONE){
                newVideoView.start()
            }
            currentIndex = newIndex
            //((disruptorScreenLayout.getChildAt(currentIndex) as LinearLayout).getChildAt(5) as LinearLayout).requestFocus();
        }
    }
}

