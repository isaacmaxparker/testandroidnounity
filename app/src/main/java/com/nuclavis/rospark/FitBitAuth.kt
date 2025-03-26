package com.nuclavis.rospark

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.regex.Matcher
import java.util.regex.Pattern

class FitBitAuth: AppCompatActivity() {
    var string = "";
    override fun onNewIntent(intent: Intent){
        super.onNewIntent(intent)
        string = intent.getDataString().toString();
        var code = ""
        val pattern: Pattern = Pattern.compile("(?<=fitbitauth:\\/\\/fitbitcallback\\?code=)(.*)(?=&state=)")
        val matcher: Matcher = pattern.matcher(string)
        if (matcher.find()) {
            code = matcher.group(1);
            val intent = Intent(this@FitBitAuth, TrackActivity::class.java);
            intent.putExtra("fitbit_code",code)
            startActivity(intent);
        }else{
            val intent = Intent(this@FitBitAuth, TrackActivity::class.java);
            startActivity(intent);
        }
    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fitbit_auth)
        onNewIntent(getIntent());
    }
}