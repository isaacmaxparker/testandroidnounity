package com.nuclavis.rospark

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.*
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import app.com.kotlinapp.OnSwipeTouchListener
import com.bumptech.glide.Glide
import com.nuclavis.rospark.databinding.*
import ly.img.android.pesdk.PhotoEditorSettingsList
import ly.img.android.pesdk.assets.filter.basic.FilterPackBasic
import ly.img.android.pesdk.assets.font.basic.FontPackBasic
import ly.img.android.pesdk.assets.frame.basic.FramePackBasic
import ly.img.android.pesdk.assets.overlay.basic.OverlayPackBasic
import ly.img.android.pesdk.backend.decoder.ImageSource
import ly.img.android.pesdk.backend.model.EditorSDKResult
import ly.img.android.pesdk.backend.model.config.ImageStickerAsset
import ly.img.android.pesdk.backend.model.state.LoadSettings
import ly.img.android.pesdk.backend.model.state.PhotoEditorSaveSettings
import ly.img.android.pesdk.ui.activity.CameraPreviewBuilder
import ly.img.android.pesdk.ui.activity.PhotoEditorBuilder
import ly.img.android.pesdk.ui.model.state.*
import ly.img.android.pesdk.ui.panels.item.ImageStickerItem
import ly.img.android.pesdk.ui.panels.item.StickerCategoryItem
import ly.img.android.pesdk.ui.utils.PermissionRequest
import ly.img.android.serializer._3.IMGLYFileWriter
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException


class Games : com.nuclavis.rospark.BaseActivity() {
    var currentSlideIndex = 0
    var totalSlideCount = 0
    var maskTotalSlideCount = 0;
    var maskCurrentSlideIndex = 0
    var videoTotalSlideCount = 0;
    var videoCurrentSlideIndex = 0
    var videoAr = emptyList<Video>()

    var sticker_urls = emptyList<Sticker>()
    var sticker_category_url = ""
    var sticker_category_title = ""

    private var xDistance = 0f
    private var yDistance = 0f
    private var lastX = 0f
    private var lastY = 0f

    val PESDK_RESULT = 1
    val GALLERY_RESULT = 2
    val CAMERA_RESULT = 3

    var running_stickers = false;

    override fun slideButtonCallback(card: Any, forward:Boolean) {
        if (card == "messages") {
            var currentIndex = currentSlideIndex;
            if (forward) {
                currentIndex += 1;
            } else {
                currentIndex -= 1;
            }
            switchSlide(currentIndex)
        } else if (card == "coloring") {
            var currentIndex = maskCurrentSlideIndex;
            if (forward) {
                currentIndex += 1;
            } else {
                currentIndex -= 1;
            }
            maskSwitchSlide(currentIndex)
        }
        else if (card == "videos") {
            var currentIndex = videoCurrentSlideIndex;
            if (forward) {
                currentIndex += 1;
            } else {
                currentIndex -= 1;
            }
            videoSwitchSlide(currentIndex)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        recolorTheme()
        super.onCreate(savedInstanceState)
        setPageContent(R.layout.games,"games")
        setTitle(getResources().getString(R.string.mobile_main_menu_games));

        //BEGIN_GAMES_CONTENT
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val isTalkBackOn = accessibilityManager.isEnabled && accessibilityManager.isTouchExplorationEnabled

        if(!isTalkBackOn){
            findViewById<LinearLayout>(R.id.games_mask_card).setVisibility(View.VISIBLE)
            findViewById<LinearLayout>(R.id.games_coloring_card).setVisibility(View.VISIBLE)
            loadArData()
        }
        //END_GAMES_CONTENT
        
        loadVideo()
        loadStickerData()

        setTooltipText(R.id.coloring_help_button,R.string.mobile_games_tooltip, R.string.mobile_games_coloring_ar)
        setTooltipText(R.id.mask_help_button,R.string.mobile_games_mask_tooltip, R.string.mobile_games_coloring_ar)
        setTooltipText(R.id.video_help_button,R.string.mobile_games_videos_tooltip, R.string.mobile_games_videos_title)
        setTooltipText(R.id.sticker_help_button,R.string.mobile_games_sticker_tooltip, R.string.mobile_games_sticker)

        val coloringArLayout = findViewById<FrameLayout>(R.id.coloring_ar_layout)
        coloringArLayout.setOnTouchListener(object : OnSwipeTouchListener(this@Games) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                switchSlide(currentSlideIndex + 1)
            }
            override fun onSwipeRight() {
                super.onSwipeRight()
                switchSlide(currentSlideIndex - 1)
            }
        })
        val maskArLayout = findViewById<FrameLayout>(R.id.mask_ar_layout)
        maskArLayout.setOnTouchListener(object : OnSwipeTouchListener(this@Games) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                maskSwitchSlide(maskCurrentSlideIndex + 1)
            }
            override fun onSwipeRight() {
                super.onSwipeRight()
                maskSwitchSlide(maskCurrentSlideIndex - 1)
            }
        })
    }

    var launcher: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult<PickVisualMediaRequest, Uri>(
            ActivityResultContracts.PickVisualMedia(),
            object : ActivityResultCallback<Uri?> {
                override fun onActivityResult(result: Uri?) {
                    if (result == null) {
                        Toast.makeText(this@Games, "No image Selected", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        openEditor(result)
                    }
                }
            }
        )

    fun loadArData() {
        val url = getResources().getString(R.string.base_server_url).plus("/")
            .plus(getStringVariable("CLIENT_CODE"))
            .plus("/custom/arColoringMasks/")
            .plus(getConsID())
            .plus("/")
            .plus(getEvent().event_id)

        val request = Request.Builder()
            .url(url)
            .addHeader("Program-Id", getStringVariable("PROGRAM_ID"))
            .addHeader("Authorization", "Bearer ".plus(getAuth()))
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: okhttp3.Response) {
                if (response.code != 200) {
                    throw Exception(response.body?.string())
                } else {
                    val responseBody = response.body?.string()

                    if(responseBody != null) {
                        try {
                            val json = JSONObject(responseBody)

                            if (json.has("data")) {
                                val dataObject = json.getJSONObject("data")

                                if (dataObject.has("ar_coloring") && dataObject.get("ar_coloring") is JSONArray) {
                                    val arColoring = dataObject.getJSONArray("ar_coloring")
                                    val coloringImageUrls = mutableListOf<String>()
                                    val pdfDownloadUrls = mutableListOf<String>()
                                    val coloringNames = mutableListOf<String>()

                                    for (i in 0 until arColoring.length()) {
                                        val item = arColoring.getJSONObject(i)
                                        coloringImageUrls.add(item.getString("image_url"))
                                        pdfDownloadUrls.add(item.getString("pdf_url"))
                                        coloringNames.add(item.getString("name"))
                                    }

                                    runOnUiThread {
                                        loadColoring(coloringImageUrls, pdfDownloadUrls, coloringNames)
                                    }
                                } else {
                                    println("No ar_coloring array found")
                                }

                                if (dataObject.has("ar_masks") && dataObject.get("ar_masks") is JSONArray) {
                                    val arMasks = dataObject.getJSONArray("ar_masks")
                                    val maskImageUrls = mutableListOf<String>()
                                    val maskNames = mutableListOf<String>()

                                    for (index in 0 until arMasks.length()) {
                                        val item = arMasks.getJSONObject(index)
                                        maskImageUrls.add(item.optString("image_url", ""))
                                        maskNames.add(item.optString("name", ""))
                                    }

                                    runOnUiThread {
                                        loadMasks(maskImageUrls, maskNames)
                                    }
                                } else {
                                    println("No ar_masks array found")
                                }

                            } else {
                                println("No data object found")
                            }
                        } catch (e: Exception) {
                            println("JSON parsing error: ${e.message}")
                        }
                    } else {
                        println("Response body is null")
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println("Network request failed: ${e.message}")
            }
        })
    }

    fun loadColoring(coloringImageUrls: List<String>, pdfDownloadUrls: List<String>, names: List<String>) {
        val coloringArLayout = findViewById<FrameLayout>(R.id.coloring_ar_layout)
        coloringArLayout.removeAllViews()
        val inflater = LayoutInflater.from(this@Games)
        var i = 0
        totalSlideCount = coloringImageUrls.count()
        for (imageUrl in coloringImageUrls) {
            val imageName = names[i]
            val binding: ColoringSliderBinding = DataBindingUtil.inflate(
                inflater, R.layout.coloring_slider, coloringArLayout, true
            )
            binding.colorList = getColorList("")
            val row = coloringArLayout.getChildAt(i) as LinearLayout
            val imageView = row.getChildAt(0) as ImageView
            val textView = row.getChildAt(1) as TextView
            val downloadButton = (row.getChildAt(2) as LinearLayout).getChildAt(0) as Button
            val pdfDownloadUrl = pdfDownloadUrls[i]
            downloadButton.setOnClickListener {
                sendGoogleAnalytics(imageName.lowercase() + "_coloring_download", "games")
                val uri = Uri.parse(pdfDownloadUrl)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }

            val scanButton = (row.getChildAt(2) as LinearLayout).getChildAt(1) as Button
            scanButton.setOnClickListener {
                sendGoogleAnalytics(imageName.lowercase() + "_coloring_scan", "games")
                startArTracking(imageName.lowercase() + "_coloring_scan")
                loadUnityContent("colouring_page")
            }

            textView.text = imageName

            Glide.with(this@Games)
                .load(imageUrl)
                .into(imageView)

            imageView.contentDescription = imageName

            if(i == 0){
                row.visibility = View.VISIBLE
            } else {
                row.visibility = View.INVISIBLE
            }
            i += 1
        }
        totalSlideCount = i
        setupSlideButtons(totalSlideCount, R.id.fundraise_messages_slide_buttons, "messages")
    }

    fun loadMasks(maskImageUrls: List<String>, names: List<String>) {
        val maskLayout = findViewById<FrameLayout>(R.id.mask_ar_layout)
        maskLayout.removeAllViews() // Clear existing views
        val inflater = LayoutInflater.from(this@Games)
        var i = 0
        maskTotalSlideCount = maskImageUrls.count()
        for (imageUrl in maskImageUrls) {
            val maskName = names[i]
            val binding: MaskSliderBinding = DataBindingUtil.inflate(
                inflater, R.layout.mask_slider, maskLayout, true
            )
            binding.colorList = getColorList("")
            val row = maskLayout.getChildAt(i) as LinearLayout
            val imageView = row.getChildAt(0) as ImageView
            val textView = row.getChildAt(1) as TextView
            val openButton = (row.getChildAt(2) as LinearLayout).getChildAt(0) as Button

            openButton.setOnClickListener {
                sendGoogleAnalytics(maskName.lowercase() + "_mask_open", "games")
                startArTracking(maskName.lowercase() + "_mask_open")
                loadUnityContent("ar_masks")
            }

            textView.text = maskName

            Glide.with(this@Games)
                .load(imageUrl)
                .into(imageView)

            imageView.contentDescription = maskName

            if(i == 0){
                row.visibility = View.VISIBLE
            } else {
                row.visibility = View.INVISIBLE
            }
            i += 1
        }
        maskTotalSlideCount = i
        setupSlideButtons(maskTotalSlideCount, R.id.mask_slide_buttons, "coloring")
    }

    fun showStickerModal(){
        val button_color = getStringVariable("BUTTON_TEXT_COLOR")
        if(button_color == "") {
            getStringVariable("PRIMARY_COLOR")
        }

        val inflater = LayoutInflater.from(this@Games)
        val alertsContainer = findViewById<LinearLayout>(R.id.alert_container)
        alertsContainer.setVisibility(View.INVISIBLE)
        hideAlertScrollView(true)
        for (childView in alertsContainer.children) {
            alertsContainer.removeView(childView);
        }

        val binding: StickerSelectionAlertBinding = DataBindingUtil.inflate(
            inflater, R.layout.sticker_selection_alert, alertsContainer, true)
        binding.colorList = getColorList("")

        findViewById<TextView>(R.id.camera_sticker_button).setOnClickListener{
            openCamera()
            running_stickers = true
        }

        findViewById<TextView>(R.id.gallery_sticker_button).setOnClickListener{
            openSystemGalleryToSelectAnImage()
            running_stickers = true
        }

        val close_button = findViewById<ImageView>(R.id.gift_alert_close_button);
        close_button.setOnClickListener{
            hideAlert()
            focusStickerButton()
        }

        alertsContainer.setVisibility(View.VISIBLE)
        hideAlertScrollView(false)
        close_button.requestFocus();
        Handler().postDelayed({
            findViewById<View>(R.id.photo_alert_heading).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        }, 175)
    }

    override fun onResume() {
        super.onResume()
        if(running_stickers){
            runOnUiThread{
                focusStickerButton()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        PermissionRequest.onRequestPermissionsResult(requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun openCamera() {
        hideAlert()
        val settingsList = createPESDKSettingsList()

        settingsList.configure<PhotoEditorSaveSettings> {
            it.setOutputToGallery(Environment.DIRECTORY_DCIM)
        }

        CameraPreviewBuilder(this)
            .setSettingsList(settingsList)
            .startActivityForResult(this, PESDK_RESULT)

        settingsList.release()
    }

    fun openSystemGalleryToSelectAnImage() {
        hideAlert()
        launcher.launch(
            PickVisualMediaRequest.Builder()
                .setMediaType(ImageOnly)
                .build()
        )
    }

    private fun createPESDKSettingsList() : PhotoEditorSettingsList {
        val settingsList = PhotoEditorSettingsList(false)

            settingsList.configure<UiConfigFilter> {
                it.setFilterList(FilterPackBasic.getFilterPack())
            }
            .configure<UiConfigText> {
                it.setFontList(FontPackBasic.getFontPack())
            }
            .configure<UiConfigFrame> {
                it.setFrameList(FramePackBasic.getFramePack())
            }
            .configure<UiConfigOverlay> {
                it.setOverlayList(OverlayPackBasic.getOverlayPack())
            }

        var sticker_list = listOf<ImageStickerItem>();

        for (sticker in sticker_urls){

            val id = sticker.id+"_"+getStringVariable("CLIENT_CODE")

            val customSticker = ImageStickerAsset(
                id,
                ImageSource.create(Uri.parse(sticker.url))
            )

            sticker_list += ImageStickerItem(
                id,
                id,
                ImageSource.create(Uri.parse(sticker.url))
            )

            settingsList.config.addAsset(customSticker)
        }

        val customStickerCategory = StickerCategoryItem(
            getStringVariable("CLIENT_CODE") + "_stickers",
            sticker_category_title,
            ImageSource.create(Uri.parse(sticker_category_url)),
            sticker_list
        )

        settingsList.configure<UiConfigSticker> {
            it.setStickerLists(customStickerCategory)
        }

        settingsList.configure<PhotoEditorSaveSettings> {
            it.setOutputToGallery(Environment.DIRECTORY_DCIM)
        }

        return settingsList
    }

    fun openEditor(inputImage: Uri?) {
        val settingsList = PhotoEditorSettingsList(false)

        settingsList.configure<LoadSettings> {
            it.source = inputImage
        }

        var sticker_list = listOf<ImageStickerItem>();

        for (sticker in sticker_urls){

            val id = sticker.id+"_"+getStringVariable("CLIENT_CODE")

            val customSticker = ImageStickerAsset(
                id,
                ImageSource.create(Uri.parse(sticker.url))
            )

            sticker_list += ImageStickerItem(
                id,
                id,
                ImageSource.create(Uri.parse(sticker.url))
            )

            settingsList.config.addAsset(customSticker)

        }

        val customStickerCategory = StickerCategoryItem(
            getStringVariable("CLIENT_CODE") + "_stickers",
            sticker_category_title,
            ImageSource.create(Uri.parse(sticker_category_url)),
            sticker_list
        )

        settingsList.configure<UiConfigSticker> {
            it.setStickerLists(customStickerCategory)
        }

        settingsList.configure<PhotoEditorSaveSettings> {
            it.setOutputToGallery(Environment.DIRECTORY_DCIM)
        }

        PhotoEditorBuilder(this)
            .setSettingsList(settingsList)
            .startActivityForResult(this, PESDK_RESULT)

        settingsList.release()
    }

   override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == RESULT_OK && requestCode == GALLERY_RESULT) {
            if (intent != null) {

            }

        } else if (resultCode == RESULT_OK && requestCode == CAMERA_RESULT) {
            val data = intent?.let { EditorSDKResult(it) }
            val lastState = data?.settingsList
            try {
                if (lastState != null) {
                    IMGLYFileWriter(lastState).writeJson(File(
                        getExternalFilesDir(null),
                        "serialisationReadyToReadWithPESDKFileReader.json"
                    ))
                }
            } catch (e: IOException) {
                println("PESDK CAMERA ERROR")
            }

            if (lastState != null) {
                lastState.release()
            }

        } else if (resultCode == RESULT_OK && requestCode == PESDK_RESULT) {
            val data = intent?.let { EditorSDKResult(it) }
            val lastState = data?.settingsList
            try {
                if (lastState != null) {
                    IMGLYFileWriter(lastState).writeJson(File(
                        getExternalFilesDir(null),
                        "serialisationReadyToReadWithPESDKFileReader.json"
                    ))
                }
            } catch (e: IOException) {
                println("EDITOR PESDK ERROR")
            }

            if (lastState != null) {
                lastState.release()
            }
            focusStickerButton()

        } else if (resultCode == RESULT_CANCELED && requestCode == PESDK_RESULT) {
            val data = intent?.let { EditorSDKResult(it) }
            val sourceURI = data?.sourceUri
            focusStickerButton()
        }
    }

    fun focusStickerButton() {
        val sticker_button = findViewById<Button>(R.id.sticker_button)

        runOnUiThread {
            sticker_button.requestFocus()
            sticker_button.requestFocusFromTouch();
            sticker_button.setFocusable(true)
        }
        running_stickers = false
    }

    fun loadStickerData(){
        val url = getResources().getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/configuration/stickers/").plus(getEvent().event_id)
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
                    println("JSON STRING")
                    println(jsonString);
                    val jsonObj = JSONObject(jsonString);
                    if(jsonObj.has("data")){
                        val data = jsonObj.get("data") as JSONObject
                        if(data.has("category_url")){
                            sticker_category_url = data.get("category_url") as String
                        }
                        if(data.has("category_title")){
                            sticker_category_title = data.get("category_title") as String
                        }
                        val stickers = data.get("stickers") as JSONArray
                        for (i in 0 until stickers.length()) {
                            val sticker = stickers.getJSONObject(i);
                            println("STICKER")
                            println(sticker)
                            val name = ""
                            var id = ""

                            if(sticker.has("image_url") && sticker.has("id")){
                                sticker_urls += Sticker((sticker.get("id") as Int).toString(),sticker.get("image_url") as String)
                            }
                        }

                        loadSticker()
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
    fun loadSticker() {
        val androidStickerPackEnabled = getStringVariable("ANDROID_STICKER_PACK_ENABLED")
        if (androidStickerPackEnabled.isNotBlank() && androidStickerPackEnabled == "true" ) {
            val stickerPackUrl = getStringVariable("ANDROID_STICKER_PACK")
            runOnUiThread {
                val stickerLayout = findViewById<FrameLayout>(R.id.sticker_layout)
                val inflater = LayoutInflater.from(this@Games)
                val binding: StickerSliderBinding = DataBindingUtil.inflate(
                    inflater, R.layout.sticker_slider, stickerLayout, true
                )
                binding.colorList = getColorList("")
                val row = stickerLayout.getChildAt(0) as LinearLayout
                val button_open = (row.getChildAt(1) as LinearLayout).getChildAt(0) as Button
                
                button_open.setOnClickListener {
                    showStickerModal()
                }
            }
        } else {
            runOnUiThread {
                val stickerLinearLayout = findViewById<LinearLayout>(R.id.sticker_linear_layout)
                stickerLinearLayout.visibility = View.GONE
            }
        }
    }

    fun loadVideo() {

        val url = resources.getString(R.string.base_server_url).plus("/").plus(getStringVariable("CLIENT_CODE")).plus("/custom/aha/getAhaVideos")
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
                    val obj = JSONObject(jsonString)
                    val data = obj.get("data").toString()
                    val jsonArray = JSONArray(data)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val safeVideo = setupVideo(obj);
                        val video = Video(
                            safeVideo.display_image,
                            safeVideo.video_url,
                            safeVideo.accessibility_text,
                            safeVideo.sort_order
                        )
                        videoAr += video
                    }

                        runOnUiThread {
                        val videoLayout = findViewById<FrameLayout>(R.id.video_ar_layout)
                        val inflater = LayoutInflater.from(this@Games)
                        var i = 0
                        videoTotalSlideCount = videoAr.count()

                        for (video in videoAr) {
                            val binding: VideoSliderBinding = DataBindingUtil.inflate(
                                inflater, R.layout.video_slider, videoLayout, true
                            )
                            binding.colorList = getColorList("")
                            val row = videoLayout.getChildAt(i) as LinearLayout

                            val show_image = row.getChildAt(1) as ImageView
                            show_image.contentDescription = video.accessibility_text

                            show_image.setOnTouchListener(object : OnSwipeTouchListener(this@Games) {

                                override fun onSwipeLeft() {
                                    super.onSwipeLeft()

                                }
                                override fun onSwipeRight() {
                                    super.onSwipeRight()
                                    videoSwitchSlide(videoCurrentSlideIndex - 1)
                                }

                                override fun onTouch(
                                    view: View,
                                    motionEvent: MotionEvent
                                ): Boolean {
                                    super.onTouch(view, motionEvent)
                                    when (motionEvent?.action) {
                                        MotionEvent.ACTION_DOWN -> {
                                            run {
                                                yDistance = 0f
                                                xDistance = yDistance
                                            }
                                            lastX = motionEvent.x
                                            lastY = motionEvent.y
                                        } MotionEvent.ACTION_UP -> {
                                            val curX = motionEvent.x
                                            val curY = motionEvent.y
                                            val direction=(curX - lastX)>0
                                            xDistance += Math.abs(curX - lastX)
                                            yDistance += Math.abs(curY - lastY)
                                            lastX = curX
                                            lastY = curY
                                            if (xDistance > yDistance && (xDistance > 50 && yDistance < xDistance)) {
                                                if(direction){
                                                    videoSwitchSlide(videoCurrentSlideIndex - 1)
                                                }else{
                                                    videoSwitchSlide(videoCurrentSlideIndex + 1)
                                                }

                                            } else {
                                                sendGoogleAnalytics("video_" + video.sort_order,"games")
                                                val browserIntent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(video.video_url)
                                                )
                                                startActivity(browserIntent)
                                            }
                                        }
                                    }
                                    return true
                                }
                            })




                            val media = video.display_image

                            if (media !== null) {
                                Glide.with(this@Games)
                                    .load(media)
                                    .into(show_image)

                            }

                            if (i == 0) {
                                row.visibility = View.VISIBLE
                            } else {
                                row.visibility = View.INVISIBLE
                            }
                            i += 1
                            }
                            videoTotalSlideCount = i

                            setupSlideButtons(videoTotalSlideCount, R.id.video_slide_buttons, "videos")
                      }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                println(e.message.toString())
            }
        })
    }
    fun switchSlide(newIndex:Int){
        switchSlideButton(newIndex + 1,totalSlideCount,R.id.fundraise_messages_slide_buttons)
        val coloringArLayout = findViewById<FrameLayout>(R.id.coloring_ar_layout)
        if((newIndex >= 0) and (newIndex < totalSlideCount)){
            coloringArLayout.getChildAt(currentSlideIndex).visibility = View.INVISIBLE
            coloringArLayout.getChildAt(newIndex).visibility = View.VISIBLE
            currentSlideIndex = newIndex
            (coloringArLayout.getChildAt(newIndex) as LinearLayout).getChildAt(0).requestFocus()
        }
    }

    fun maskSwitchSlide(newIndex:Int){
        switchSlideButton(newIndex + 1,maskTotalSlideCount,R.id.mask_slide_buttons)
        val maskArLayout = findViewById<FrameLayout>(R.id.mask_ar_layout)
        if((newIndex >= 0) and (newIndex < maskTotalSlideCount)){
            maskArLayout.getChildAt(maskCurrentSlideIndex).visibility = View.INVISIBLE
            maskArLayout.getChildAt(newIndex).visibility = View.VISIBLE
            maskCurrentSlideIndex = newIndex
            (maskArLayout.getChildAt(newIndex) as LinearLayout).getChildAt(0).requestFocus()
        }
    }

    fun videoSwitchSlide(newIndex:Int) {
        switchSlideButton(newIndex + 1, videoTotalSlideCount, R.id.video_slide_buttons)
        val videoArLayout = findViewById<FrameLayout>(R.id.video_ar_layout)
        if ((newIndex >= 0) and (newIndex < videoTotalSlideCount)) {
            videoArLayout.getChildAt(videoCurrentSlideIndex).visibility = View.INVISIBLE
            videoArLayout.getChildAt(newIndex).visibility = View.VISIBLE
            videoCurrentSlideIndex = newIndex
        }
    }

    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor =
                contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)

            parcelFileDescriptor!!.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    class Sticker(
        val id: String,
        val url: String
    )

   class Video(
        val display_image: String,
        val video_url: String,
        val accessibility_text: String,
        val sort_order: String,
    )
}
