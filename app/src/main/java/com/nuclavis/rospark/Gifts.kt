package com.nuclavis.rospark

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.*
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.google.android.material.button.MaterialButton
import com.nuclavis.rospark.databinding.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class Gifts : com.nuclavis.rospark.BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        recolorTheme()
        super.onCreate(savedInstanceState)
        setPageContent(R.layout.gifts,"gifts")
        setTitle(getResources().getString(R.string.mobile_main_menu_gifts));

        setTooltipText(R.id.gifts_spread_the_word_help_button, R.string.mobile_gifts_spread_the_word_tooltip, R.string.mobile_gifts_spread_the_word_title)
        setTooltipText(R.id.gifts_gifts_card_help_button, R.string.mobile_gifts_gifts_tooltip, R.string.mobile_gifts_gifts_title)

        findViewById<Button>(R.id.btn_spread_the_word).setOnClickListener{
            val intent = Intent(this@Gifts, Fundraise::class.java);
            startActivity(intent);
            this.overridePendingTransition(0, 0);
        }

        if(getStringVariable("GIFTS_ARRAY_STRING") != ""){
            loadGiftsData(JSONArray(getStringVariable("GIFTS_ARRAY_STRING")))
        }else{
            getGifts()
        }
    }

    fun getGifts(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/getGifts/").plus(getConsID()).plus("/").plus(getEvent().event_id)
        val gifts_button = findViewById<LinearLayout>(R.id.menu_option_gifts);
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
                    val response = response.body?.string();
                    val json = JSONObject(response);
                    if(json.has("data")){
                        runOnUiThread{
                            val data = json.get("data") as JSONObject
                            if(data.has("gifts")){
                                val giftsArray = data.get("gifts") as JSONArray
                                if(giftsArray.length() > 0){
                                    setVariable("GIFTS_ARRAY_STRING",giftsArray.toString())
                                    loadGiftsData(giftsArray)       
                                }
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
            }
        })
    } 

    fun loadGiftsData(giftsJSONArray: JSONArray){
        println("GIFTS ARRAY")
        println(giftsJSONArray)
        var gifts = listOf<Gift>()
        var num_earned = 0
        for(index in 0 .. giftsJSONArray.length() - 1){
            val gift = giftsJSONArray[index] as JSONObject
            var name = ""
            var instant = false;
            var earned = false;
            var image = ""
            var info_text = ""

            if(gift.has("name") && gift.get("name") is String){
                name = gift.get("name") as String
            }

            if(gift.has("instant") && gift.get("instant") is Boolean){
                instant = gift.get("instant") as Boolean
            }else if (gift.has("instant") && gift.get("instant") is Int){
                if(gift.get("instant") == 1){
                    instant = true
                }else{
                    instant = false
                }
            }

            if(gift.has("earned") && gift.get("earned") is Boolean){
                earned = gift.get("earned") as Boolean
            }else if (gift.has("earned") && gift.get("earned") is Int){
                if(gift.get("earned") == 1){
                    earned = true
                    num_earned += 1
                }else{
                    earned = false
                }
            }

            if(gift.has("image") && gift.get("image") is String){
                image = gift.get("image") as String
            }

            if(gift.has("info_text") && gift.get("info_text") is String){
                info_text = gift.get("info_text") as String
            }

            gifts += Gift(name, instant, earned, image, info_text)

        }

        findViewById<TextView>(R.id.gifts_card_description).text = getString(R.string.mobile_gifts_gifts_description).replace("earned_gifts",num_earned.toString()).replace("total_gifts",gifts.size.toString()).replace("raised_amount",getStringVariable("PERSONAL_RAISED"))

        val chunks = gifts.chunked(2)
        val inflater = layoutInflater
        val container = findViewById<LinearLayout>(R.id.gifts_cards_container)

        val earned_color = getStringVariable("GIFTS_CARD_BACKGROUND_COLOR")
        var button_color = getStringVariable("GIFTS_CARD_BUTTON_COLOR")

        if(button_color == ""){
            button_color = getStringVariable("BUTTON_TEXT_COLOR")
            if(button_color == "") {
                getStringVariable("PRIMARY_COLOR")
            }
        }

        for(chunk in chunks){
            val binding: GiftCardRowBinding = DataBindingUtil.inflate(
                inflater, R.layout.gift_card_row, container, true)
            binding.colorList = getColorList("")
            val root = binding.root as LinearLayout

            for(i in 0 .. 1){

                val giftsBinding: GiftCardBinding = DataBindingUtil.inflate(
                    inflater, R.layout.gift_card, root, true)

                if(i < chunk.size){
                    val gift = chunk[i]
                    giftsBinding.colorList = ColorList("no_stroke",!gift.earned,getStringVariable("PRIMARY_COLOR"), button_color)
                    giftsBinding.white = "#ffffff"
                    val card_back = giftsBinding.root as LinearLayout
                    val card = card_back.getChildAt(0) as LinearLayout
                    if(gift.earned){
                        card_back.getBackground().setColorFilter(Color.parseColor(earned_color), PorterDuff.Mode.MULTIPLY);
                    }else{
                        card_back.getBackground().setColorFilter(Color.parseColor("#ffffff"), PorterDuff.Mode.MULTIPLY);
                    }

                    (card.getChildAt(0) as TextView).text = gift.name

                    Glide.with(this@Gifts).asBitmap().load(gift.image).into(
                        BitmapImageViewTarget(card.getChildAt(1) as ImageView)
                    )

                    var earned_string = getString(R.string.mobile_gifts_gifts_card_unearned)

                    if(gift.earned){
                        earned_string =  getString(R.string.mobile_gifts_gifts_card_earned)
                    }


                    (card.getChildAt(1) as ImageView).contentDescription = gift.name + " " + "gift" + " " + earned_string


                    (card.getChildAt(2) as TextView).text = gift.info_text

                    if(gift.instant){
                        (card.getChildAt(3) as TextView).setVisibility(View.INVISIBLE)
                    }

                    (card.getChildAt(5) as LinearLayout).getChildAt(0).setOnClickListener{
                        showModal(gift, button_color, (card.getChildAt(5) as LinearLayout).getChildAt(0))
                    }
                }else{
                    giftsBinding.colorList = getColorList("")
                    giftsBinding.root.setVisibility(View.INVISIBLE)
                }
            }
        }
    }

    fun showModal(gift: Gift, button_color: String, sender: View){
        val inflater = LayoutInflater.from(this@Gifts)

        val alertsContainer = findViewById<LinearLayout>(R.id.alert_container)
        alertsContainer.setVisibility(View.INVISIBLE)
        hideAlertScrollView(true)
        for (childView in alertsContainer.children) {
            alertsContainer.removeView(childView);
        }

        val binding: GiftAlertBinding = DataBindingUtil.inflate(
            inflater, R.layout.gift_alert, alertsContainer, true)
        binding.colorList = ColorList(getStringVariable("PRIMARY_COLOR"),!gift.earned, "#ffffff", button_color)

        val card = findViewById<LinearLayout>(R.id.gift_alert_row)

        findViewById<TextView>(R.id.gift_alert_title).text = gift.name

        Glide.with(this@Gifts).asBitmap().load(gift.image).into(
            BitmapImageViewTarget(card.getChildAt(0) as ImageView)
        )

        var earned_string = getString(R.string.mobile_gifts_gifts_modal_unearned)

        if(gift.earned){
            earned_string = getString(R.string.mobile_gifts_gifts_modal_earned)
        }

        (card.getChildAt(0) as ImageView).contentDescription = gift.name + " " + "gift" + " " + earned_string


        (card.getChildAt(1) as TextView).text = gift.info_text

        if(gift.instant){
            (card.getChildAt(2) as TextView).setVisibility(View.GONE)
        }

        val close_button = findViewById<ImageView>(R.id.gift_alert_close_button);
        close_button.setOnClickListener{
            hideAlert()
            sender.requestFocus()
            Handler().postDelayed({
                sender.requestFocus()
            }, 175)
        }

        alertsContainer.setVisibility(View.VISIBLE)
        hideAlertScrollView(false)
        close_button.requestFocus()
        Handler().postDelayed({
            findViewById<LinearLayout>(R.id.gift_alert_row).sendAccessibilityEvent(
                AccessibilityEvent.TYPE_VIEW_FOCUSED);
        }, 175)
    }


    class Gift (
        var name: String,
        var instant: Boolean,
        var earned: Boolean,
        var image: String,
        var info_text: String
    )
}