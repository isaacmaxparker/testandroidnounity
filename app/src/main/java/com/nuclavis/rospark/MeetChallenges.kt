package com.nuclavis.rospark

import android.os.Bundle

class MeetChallenge: com.nuclavis.rospark.BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        recolorTheme()
        super.onCreate(savedInstanceState)
        setPageContent(R.layout.meet_challenge, "meetChallenge")
        setTitle(getResources().getString(R.string.mobile_main_menu_meet_challenge));
    }
}