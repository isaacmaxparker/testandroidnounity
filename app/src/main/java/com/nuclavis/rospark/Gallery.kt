package com.nuclavis.rospark

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.webkit.WebView
import android.widget.TextView
import java.util.regex.Matcher
import java.util.regex.Pattern

class Gallery : com.nuclavis.rospark.BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        recolorTheme()
        super.onCreate(savedInstanceState)
        setPageContent(R.layout.gallery,"gallery")
        setTitle(getResources().getString(R.string.mobile_main_menu_gallery))

        findViewById<WebView>(R.id.gallery_webview).getSettings().setJavaScriptEnabled(true);
        findViewById<WebView>(R.id.gallery_webview).loadUrl(getStringVariable("GALLERY_SOURCE"));
        findViewById<WebView>(R.id.gallery_webview).setVerticalScrollBarEnabled(true);

        var comboString = getString(R.string.mobile_gallery_description_android);
        
        if(getStringVariable("GALLERY_DESCRIPTION_ANDROID") != ""){
            comboString = getStringVariable("GALLERY_DESCRIPTION_ANDROID");
        }

        var hashtags = emptyArray<String>()

        val MY_PATTERN: Pattern = Pattern.compile("#(\\w+)")
        val mat: Matcher = MY_PATTERN.matcher(comboString)
        while (mat.find()) {
            hashtags += ("#" + mat.group(1))
        }

        val tv = findViewById<TextView>(R.id.gallery_post_picture_descriptions)
        val str = SpannableStringBuilder(comboString)

        for (tag in hashtags){
            val bold_start = comboString.indexOf(tag, 0);
            val bold_end = bold_start + tag.length
            str.setSpan(
                StyleSpan(Typeface.BOLD_ITALIC),
                bold_start,
                bold_end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        tv.setText(str);
    } 
}