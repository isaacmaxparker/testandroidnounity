package com.nuclavis.rospark

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.amangarg.localizationdemo.JsonHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.fasterxml.jackson.databind.ObjectMapper
import com.nuclavis.rospark.databinding.LoginBinding
import com.nuclavis.rospark.databinding.LoginInternalCodeBinding
import okhttp3.*
import okio.IOException
import org.json.JSONObject
import java.util.*
import java.util.concurrent.Executor
import kotlin.collections.HashMap

class LoginInternalCode : BaseLanguageActivity() {

    var logo_url = "";

    override fun childviewCallback(string: String, data:String){}
    override fun slideButtonCallback(card: Any, forward:Boolean){}
    override fun onCreate(savedInstanceState: Bundle?) {
        setVariable("EVENT_THEME",R.style.StandardTheme.toString())
        recolorTheme()
        super.onCreate(savedInstanceState)
        val binding: LoginInternalCodeBinding = DataBindingUtil.setContentView(
            this, R.layout.login_internal_code)
        binding.colorList = getColorList("login")
        binding.loginInputBorder = getStringVariable("LOGIN_TEXTBOX_BORDER_ENABLED") == "true"
        hideAlert()
        setBackground()
        val logoview = findViewById<ImageView>(R.id.login_page_logo)

        var media = getStringVariable("REGISTER_LOGO_URL")
        if(media == ""){
            media = getStringVariable("LOGO_URL_CONTAINER");
        }

        if (media != null) {
            Glide.with(this)
                .load(media)
                .into(logoview)

            if(getStringVariable("LOGIN_APP_NAME") != ""){
                logoview.contentDescription = getStringVariable("LOGIN_APP_NAME")
            }
        } else {
            val intent = Intent(this@LoginInternalCode, Error::class.java);
            startActivity(intent);
        }

        loadUrls(false, true);
        updateView();

        val newcodebutton = findViewById<Button>(R.id.btn_new_code);
        newcodebutton.setOnClickListener {
            val newIntent = Intent(this@LoginInternalCode, LoginWithRegister::class.java);
            newIntent.putExtra("logo_url",logo_url);
            startActivity(newIntent);
        }

        val loginbutton = findViewById<Button>(R.id.btn_login);
        loginbutton.setOnClickListener {
            val email = intent.getStringExtra("internal_email").toString();
            val password = UUID.randomUUID().toString().replace("-","");
            val codeInput = findViewById<EditText>(R.id.login_code);
            val code = codeInput.getText().toString()
            codeLogin(email, password, code, false)
        }
        
        val helpbutton = findViewById<Button>(R.id.btn_help_link);
        helpbutton.setOnClickListener {
            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder.setMessage("Need Help Link has been clicked")
                .setCancelable(false)
                .setNegativeButton("Close Alert", DialogInterface.OnClickListener { dialog, id ->
                    dialog.cancel()
                })

            val alert = dialogBuilder.create()
            alert.setTitle("Alert")
            alert.show()
        }
        val buildCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME
        findViewById<TextView>(R.id.build_version_info).setText(getResources().getString(R.string.mobile_login_version)+ " " + versionName + " " + getResources().getString(R.string.mobile_login_build) + " " + buildCode);
        getPoweredByLogo()
    }

    fun setBackground(){
        val background = findViewById<ImageView>(R.id.page_background)
        if(getStringVariable("LOGIN_BACKGROUND_IMAGE_ENABLED") == "true"){
            val media = getStringVariable("LOGIN_BACKGROUND_IMAGE")
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
                background.setBackgroundColor(Color.BLACK)
            }
        }
    }

    fun getPoweredByLogo(){
        val logo_view = findViewById<ImageView>(R.id.additional_powered_by_logo)
        if(getStringVariable("LOGO_POWERED_BY_URL") != ""){
            Glide.with(this)
                .load(getStringVariable("LOGO_POWERED_BY_URL"))
                .into(logo_view);
            logo_view.visibility = View.VISIBLE
        }else{
            logo_view.visibility = View.GONE
        }
    }
}
