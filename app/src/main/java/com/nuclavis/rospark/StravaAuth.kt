package com.nuclavis.rospark

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.regex.Matcher
import java.util.regex.Pattern

class StravaAuth: AppCompatActivity() {
    var string = "";
    override fun onNewIntent(intent: Intent){
        super.onNewIntent(intent)
        string = intent.getDataString().toString();
        var code = ""
        val strava_pattern: Pattern = Pattern.compile("(?<=stravaauth:\\/\\/stravacallback\\?state=&code=)(.*)(?=&scope=)")
        val strava_matcher: Matcher = strava_pattern.matcher(string)
        if (strava_matcher.find()){
            code = strava_matcher.group(1);
            val intent = Intent(this@StravaAuth, TrackActivity::class.java);
            intent.putExtra("strava_code",code)
            startActivity(intent);
        } else{
            val strava_pattern: Pattern = Pattern.compile("(?<=stravaauth:\\/\\/stravacallback\\?code=)(.*)(?=&scope=)")
            val strava_matcher: Matcher = strava_pattern.matcher(string)
            if (strava_matcher.find()){
                code = strava_matcher.group(1);
                val intent = Intent(this@StravaAuth, TrackActivity::class.java);
                intent.putExtra("strava_code",code)
                startActivity(intent);
            } else{
                val intent = Intent(this@StravaAuth, TrackActivity::class.java);
                startActivity(intent);
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.strava_auth)
        onNewIntent(getIntent());
    }
}