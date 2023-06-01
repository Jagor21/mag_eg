package com.eternal_joy.magic_egypt

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.webkit.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.appsflyer.AppsFlyerLib
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.IOException

private const val WEB_VEW_URL = "WEB_VEW_URL"

class MainActivity : AppCompatActivity() {
    lateinit var mp: MediaPlayer
    lateinit var win: MediaPlayer
    lateinit var bgsound: MediaPlayer
    var myCoins_val = 0
    var bet_val = 0
    var jackpot_val = 0
    var soundclick = 0
    var soundspinbg = 0
    var spinstop = 0
    private val comboNumber = 7
    private val coef1 = 72
    private val coef2 = 142
    private val coef3 = 212
    private var position1 = 5
    private var position2 = 5
    private var position3 = 5
    private val slot = intArrayOf(1, 2, 3, 4, 5, 6, 7)
    private lateinit var rv1: RecyclerView
    private lateinit var rv2: RecyclerView
    private lateinit var rv3: RecyclerView
    private lateinit var adapter: SpinnerAdapter
    private lateinit var layoutManager1: MyLayoutManager
    private lateinit var layoutManager2: MyLayoutManager
    private lateinit var layoutManager3: MyLayoutManager
    private lateinit var spinButton: ImageButton
    private lateinit var plusButton: ImageView
    private lateinit var settingsButton: ImageView
    private lateinit var jackpot: TextView
    private lateinit var myCoins: TextView
    private lateinit var bet: TextView
    private var firstRun = false
    private lateinit var gameLogic: Game
    private lateinit var pref: SharedPreferences
    private lateinit var mSoundPool: SoundPool
    private var countads = 0
    private var playmusic = 0
    private var playsound = 0
    private lateinit var music_off: ImageView
    private lateinit var music_on: ImageView
    private lateinit var soundon: ImageView
    private lateinit var soundoff: ImageView
    private lateinit var referrerClient: InstallReferrerClient
    var sharedPrefEditor: SharedPreferences.Editor? = null
    private lateinit var sharedPreferences: SharedPreferences
    private var webView: WebView? = null
    private var subParameter = ""
//    private var testSubParameter = "i1sj6dwxFtFM^subid1"

    var filePath: ValueCallback<Array<Uri>>? = null

    val getFile = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_CANCELED) {
            filePath?.onReceiveValue(null)
        } else if (it.resultCode == Activity.RESULT_OK && filePath != null) {
            filePath!!.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(it.resultCode, it.data)
            )
            filePath = null
        }
    }
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        }

    private val permissionsList = listOf(
        android.Manifest.permission.POST_NOTIFICATIONS,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private var needExitApp = false

    private var gaid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        determineAdvertisingInfo(this)
        requestPermission(permissionsList)
        getNotificationToken()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        val metrics = DisplayMetrics()
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(metrics)
        val densityDpi = metrics.densityDpi
        setContentView(R.layout.activity_main)
        initInstallReferrer()
        mSoundPool = SoundPool(5, AudioManager.STREAM_MUSIC, 100)
        soundclick = mSoundPool.load(this, R.raw.click, 1)
        soundspinbg = mSoundPool.load(this, R.raw.spin_bg, 1)
        spinstop = mSoundPool.load(this, R.raw.spin_stop, 1)
        val typeface = ResourcesCompat.getFont(this, R.font.font)
        bgsound = MediaPlayer.create(this, R.raw.bg_music)
        bgsound.setLooping(true)
        mp = MediaPlayer.create(this, R.raw.spin)
        win = MediaPlayer.create(this, R.raw.win)
        pref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        firstRun = pref.getBoolean("firstRun", true)
        if (firstRun) {
            playmusic = 1
            playsound = 1
            val editor = pref.edit()
            editor.putBoolean("firstRun", false)
            editor.apply()
        } else {
            playmusic = pref.getInt("music", 1)
            playsound = pref.getInt("sound", 1)
            checkmusic()
        }
        Log.d("MUSIC", playmusic.toString())
        sharedPreferences = getSharedPreferences("your_prefs", Activity.MODE_PRIVATE)
        sharedPrefEditor = sharedPreferences.edit()
        (application as MagicEgyptApp).conversionData.onEach { subId1 ->
            if (subId1 != null && subId1 is String && subId1.isNotEmpty()) {
                manageUILogic(subId1 = subId1)
            }
        }.launchIn(CoroutineScope(Dispatchers.Main))
        //Initializations
        gameLogic = Game()
        settingsButton = findViewById(R.id.settings)
        spinButton = findViewById(R.id.spinButton)
        plusButton = findViewById(R.id.plusButton)
        jackpot = findViewById(R.id.jackpot)
        jackpot.setTypeface(typeface)
        myCoins = findViewById(R.id.myCoins)
        myCoins.setTypeface(typeface)
        bet = findViewById(R.id.bet)
        bet.setTypeface(typeface)
        adapter = SpinnerAdapter()

        webView = findViewById(R.id.web_view)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView?.settings?.let { settings ->
            with(settings) {
                javaScriptEnabled = true
                databaseEnabled = true
                domStorageEnabled = true
                loadsImagesAutomatically = true
                setSupportMultipleWindows(true)
                allowFileAccess = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                javaScriptCanOpenWindowsAutomatically = true

            }
        }
        webView?.webChromeClient = MyWebChromeClient(this)
        webView?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                view?.loadUrl(request?.url.toString())
                return false
            }
        }


        //RecyclerView settings
        rv1 = findViewById(R.id.spinner1)
        rv2 = findViewById(R.id.spinner2)
        rv3 = findViewById(R.id.spinner3)
        rv1.setHasFixedSize(true)
        rv2.setHasFixedSize(true)
        rv3.setHasFixedSize(true)
        layoutManager1 = MyLayoutManager(this)
        layoutManager1!!.setScrollEnabled(false)
        rv1.setLayoutManager(layoutManager1)
        layoutManager2 = MyLayoutManager(this)
        layoutManager2!!.setScrollEnabled(false)
        rv2.setLayoutManager(layoutManager2)
        layoutManager3 = MyLayoutManager(this)
        layoutManager3!!.setScrollEnabled(false)
        rv3.setLayoutManager(layoutManager3)
        rv1.setAdapter(adapter)
        rv2.setAdapter(adapter)
        rv3.setAdapter(adapter)
        rv1.scrollToPosition(position1)
        rv2.scrollToPosition(position2)
        rv3.scrollToPosition(position3)
        setText()
        updateText()
        val appsflyerUserId = AppsFlyerLib.getInstance().getAppsFlyerUID(this)

        //RecyclerView listeners
        rv1.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        rv1.scrollToPosition(gameLogic!!.getPosition(0))
                        layoutManager1!!.setScrollEnabled(false)
                        if (playsound == 1) {
                            mSoundPool!!.play(spinstop, 1f, 1f, 0, 0, 1f)
                        }
                    }
                }
            }
        })
        rv2.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        rv2.scrollToPosition(gameLogic!!.getPosition(1))
                        layoutManager2!!.setScrollEnabled(false)
                        if (playsound == 1) {
                            mSoundPool!!.play(spinstop, 1f, 1f, 0, 0, 1f)
                        }
                    }
                }
            }
        })
        rv3.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        rv3.scrollToPosition(gameLogic!!.getPosition(2))
                        layoutManager3!!.setScrollEnabled(false)
                        if (playsound == 1) {
                            mSoundPool!!.play(spinstop, 1f, 1f, 0, 0, 1f)
                        }
                        updateText()
                        if (gameLogic!!.hasWon) {
                            if (playsound == 1) {
                                win.start()
                            }
                            countads++
                            Log.d("ADS", countads.toString())
                            if (countads == 2) {
                                val handler = Handler()
                                countads = 0
                            }
                            val inflater = layoutInflater
                            val layout = inflater.inflate(
                                R.layout.win_splash,
                                findViewById(R.id.win_splash)
                            )
                            val winCoins = layout.findViewById<TextView>(R.id.win_coins)
                            winCoins.setTypeface(typeface)
                            winCoins.text = gameLogic!!.prize
                            val toast = Toast(this@MainActivity)
                            toast.duration = Toast.LENGTH_SHORT
                            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
                            toast.setView(layout)
                            toast.show()
                            gameLogic!!.hasWon = false
                        }
                    }
                }
            }
        })

        //Button listeners
        spinButton.setOnClickListener(View.OnClickListener {
            if (playsound == 1) {
                mp.start()
                mSoundPool!!.play(soundspinbg, 1f, 1f, 0, 0, 1f)
            }
            layoutManager1!!.setScrollEnabled(true)
            layoutManager2!!.setScrollEnabled(true)
            layoutManager3!!.setScrollEnabled(true)
            gameLogic!!.getSpinResults()
            position1 = gameLogic!!.getPosition(0) + coef1
            position2 = gameLogic!!.getPosition(1) + coef2
            position3 = gameLogic!!.getPosition(2) + coef3
            rv1.smoothScrollToPosition(position1)
            rv2.smoothScrollToPosition(position2)
            rv3.smoothScrollToPosition(position3)
        })
        plusButton.setOnClickListener(View.OnClickListener {
            if (playsound == 1) {
                mSoundPool!!.play(soundclick, 1f, 1f, 0, 0, 1f)
            }
            gameLogic!!.betUp()
            updateText()
        })
        settingsButton.setOnClickListener(View.OnClickListener {
            if (playsound == 1) {
                mSoundPool!!.play(soundclick, 1f, 1f, 0, 0, 1f)
            }
            val dialog = ShowSettingsDialog()
            dialog.show()
        })
    }

    private fun requestPermission(permissionsList: List<String>) {
        val newPermissions = mutableListOf<String>()
        permissionsList.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_DENIED
            ) {
                newPermissions.add(permission)
            }
        }
        if (newPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(newPermissions.toTypedArray())
        }
    }

    private fun initInstallReferrer() {
        referrerClient = InstallReferrerClient.newBuilder(this).build()
        // on below line we are starting its connection.

        // on below line we are starting its connection.
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        var response: ReferrerDetails? = null
                        try {
                            response = referrerClient.installReferrer
                            response.installReferrer
                            val referrerUrl = response.installReferrer
                            Log.d("INSTALL_REFERRER", referrerUrl)
                            subParameter = referrerUrl
//                            val referrer = "CHXMi2L6Oy1W^subid1"
//                            manageUILogic()
                        } catch (e: RemoteException) {
                            // handling error case.
                            e.printStackTrace()
                        }
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        // API not available on the current Play Store app.
                        Toast.makeText(
                            this@MainActivity,
                            "Feature not supported..",
                            Toast.LENGTH_SHORT
                        ).show()
//                        manageUILogic(testSubParameter)
                    }
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE ->                         // Connection couldn't be established.
                        Toast.makeText(
                            this@MainActivity,
                            "Fail to establish connection",
                            Toast.LENGTH_SHORT
                        ).show()
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Toast.makeText(this@MainActivity, "Service disconnected..", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    override fun onBackPressed() {
        webView?.let { wv ->
            if (wv.canGoBack()) {
                wv.goBack()
                return
            } else {
                if (needExitApp) {
                    super.onBackPressed()
                } else {
                    Toast.makeText(
                        this,
                        "Click Back one more time\nto exit the application",
                        Toast.LENGTH_SHORT
                    ).show()
                    needExitApp = true
                }
            }
        }
    }

    private fun determineAdvertisingInfo(context: Context) {
        AsyncTask.execute {
            try {
                val advertisingIdInfo: com.google.android.gms.ads.identifier.AdvertisingIdClient.Info =
                    com.google.android.gms.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(
                        context
                    )
                // You should check this in case the user disabled it from settings
                if (!advertisingIdInfo.isLimitAdTrackingEnabled()) {
                    gaid = if (advertisingIdInfo.id != null) {
                        advertisingIdInfo.id!!
                    } else {
                        ""
                    }
                    Log.d("MY_APP_TAG", "determineAdvertisingInfo gaid: $gaid")
                    // getUserAttributes(id);
                } else {
                    //If you stored the id you should remove it
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: GooglePlayServicesNotAvailableException) {
                e.printStackTrace()
            } catch (e: GooglePlayServicesRepairableException) {
                e.printStackTrace()
            }
        }
    }

        private fun manageUILogic(subId1: String, advertisingId: String = gaid) {
        val appsflyerUserId = AppsFlyerLib.getInstance().getAppsFlyerUID(this)
        val isFirstRun = sharedPreferences.getBoolean("isFirstRun", true)
        val isFromNotification: Boolean =
            intent.extras?.getBoolean("is_from_notification", false) ?: false
        if (isFromNotification) {
            val url = sharedPreferences.getString(WEB_VEW_URL, "")
            if (url.isNullOrEmpty()) {
                if (subId1.isNotEmpty() && subId1 != "null") {
                    val queue = Volley.newRequestQueue(this)
                    val stringRequest = StringRequest(
                        Request.Method.GET,
                        "https://moraxylu.com/click?ecid=&bundle_id=com.eternal_joy.magic_egypt&subid1=$subId1&appsflyer_id=$appsflyerUserId&advertising_id=$advertisingId",
                        { response ->
                            // Display the first 500 characters of the response string.
                            Log.d("STRING_RESPONSE", response)
                            if (response.isNotEmpty()) {
                                sharedPrefEditor?.putString(WEB_VEW_URL, response)?.commit()
                                webView?.visibility = View.VISIBLE
                                webView?.loadUrl(response)
                                bgsound.stop()
                            }
                            sharedPrefEditor?.putBoolean("isFirstRun", false)?.commit()
                        },
                        { Log.d("MainActivity", it.message.toString())})
                    queue.add(stringRequest)
                }
            } else {
                webView?.visibility = View.VISIBLE
                webView?.loadUrl(url)
                bgsound.stop()
            }
            return
        }
        if (isFirstRun) {
            if (subId1.isNotEmpty() && subId1 != "null") {
                val queue = Volley.newRequestQueue(this)
                val stringRequest = StringRequest(
                    Request.Method.GET,
                    "https://moraxylu.com/click?ecid=&bundle_id=com.eternal_joy.magic_egypt&subid1=$subId1&appsflyer_id=$appsflyerUserId&advertising_id=$advertisingId",
                    { response ->
                        // Display the first 500 characters of the response string.
                        Log.d("STRING_RESPONSE", response)
                        if (response.isNotEmpty()) {
                            sharedPrefEditor?.putString(WEB_VEW_URL, response)?.commit()
                            webView?.visibility = View.VISIBLE
                            webView?.loadUrl(response)
                            bgsound.stop()
                        }
                        sharedPrefEditor?.putBoolean("isFirstRun", false)?.commit()
                    },
                    {
                        Log.d("MainActivity", it.message.toString())
                    })
                queue.add(stringRequest)
            }
        } else {
            val url = sharedPreferences.getString(WEB_VEW_URL, "")
            if (url.isNullOrEmpty()) {
                webView?.visibility = View.INVISIBLE
            } else {
                webView?.visibility = View.VISIBLE
                webView?.loadUrl(url)
                bgsound.stop()
            }
        }
    }

    private fun setText() {
        if (firstRun) {
            gameLogic!!.setMyCoins(1000)
            gameLogic!!.setBet(5)
            gameLogic!!.setJackpot(100000)
            val editor = pref!!.edit()
            editor.putBoolean("firstRun", false)
            editor.apply()
        } else {
            val coins = pref!!.getString("coins", "")
            val bet = pref!!.getString("bet", "")
            val jackpot = pref!!.getString("jackpot", "")
            Log.d("COINS", coins!!)
            myCoins_val = Integer.valueOf(coins)
            bet_val = Integer.valueOf(bet)
            jackpot_val = Integer.valueOf(jackpot)
            gameLogic!!.setMyCoins(myCoins_val)
            gameLogic!!.setBet(bet_val)
            gameLogic!!.setJackpot(jackpot_val)
        }
    }

    private fun updateText() {
        jackpot!!.text = gameLogic!!.jackpot
        myCoins!!.text = gameLogic!!.myCoins
        bet!!.text = gameLogic!!.bet
        val editor = pref!!.edit()
        editor.putString("coins", gameLogic!!.myCoins)
        editor.putString("bet", gameLogic!!.bet)
        editor.putString("jackpot", gameLogic!!.jackpot)
        editor.apply()
    }

    private fun ShowSettingsDialog(): Dialog {
        val dialog: Dialog
        dialog = Dialog(this, R.style.WinDialog)
        dialog.window!!.setContentView(R.layout.settings)
        dialog.window!!.setGravity(Gravity.CENTER_HORIZONTAL)
        dialog.window!!.setBackgroundDrawableResource(R.color.transparent)
        val close = dialog.findViewById<ImageView>(R.id.close)
        close.setOnClickListener { dialog.dismiss() }
        music_on = dialog.findViewById(R.id.music_on)
        music_on.setOnClickListener(View.OnClickListener {
            playmusic = 0
            checkmusic()
            music_on.setVisibility(View.INVISIBLE)
            music_off!!.visibility = View.VISIBLE
            val editor = pref!!.edit()
            editor.putInt("music", playmusic)
            editor.apply()
        })
        music_off = dialog.findViewById(R.id.music_off)
        music_off.setOnClickListener(View.OnClickListener {
            playmusic = 1
            bgsound!!.start()
            recreate()
            dialog.show()
            music_on.setVisibility(View.VISIBLE)
            music_off.setVisibility(View.INVISIBLE)
            val editor = pref!!.edit()
            editor.putInt("music", playmusic)
            editor.apply()
        })
        soundon = dialog.findViewById(R.id.sounds_on)
        soundon.setOnClickListener(View.OnClickListener {
            playsound = 0
            soundon.setVisibility(View.INVISIBLE)
            soundoff!!.visibility = View.VISIBLE
            val editor = pref!!.edit()
            editor.putInt("sound", playsound)
            editor.apply()
        })
        soundoff = dialog.findViewById(R.id.sounds_off)
        soundoff.setOnClickListener(View.OnClickListener {
            playsound = 1
            recreate()
            dialog.show()
            soundon.visibility = View.INVISIBLE
            soundoff.visibility = View.VISIBLE
            val editor = pref.edit()
            editor.putInt("sound", playsound)
            editor.apply()
        })
        checkmusicdraw()
        checksounddraw()
        dialog.show()
        return dialog
    }

    public override fun onPause() {
        super.onPause()
        bgsound!!.pause()
    }

    public override fun onResume() {
        super.onResume()
        checkmusic()
    }

    private fun checkmusic() {
        if (playmusic == 1) {
            bgsound!!.start()
        } else {
            bgsound!!.pause()
        }
    }

    private fun checkmusicdraw() {
        if (playmusic == 1) {
            music_on!!.visibility = View.VISIBLE
            music_off!!.visibility = View.INVISIBLE
        } else {
            music_on!!.visibility = View.INVISIBLE
            music_off!!.visibility = View.VISIBLE
        }
    }

    private fun checksounddraw() {
        if (playsound == 1) {
            soundon!!.visibility = View.VISIBLE
            soundoff!!.visibility = View.INVISIBLE
        } else {
            soundon!!.visibility = View.INVISIBLE
            soundoff!!.visibility = View.VISIBLE
        }
    }

    private inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var pic: ImageView

        init {
            pic = itemView.findViewById(R.id.spinner_item)
        }
    }

    private inner class SpinnerAdapter : RecyclerView.Adapter<ItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val layoutInflater = LayoutInflater.from(this@MainActivity)
            val view = layoutInflater.inflate(R.layout.spinner_item, parent, false)
            return ItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val i = if (position < 7) position else position % comboNumber
            when (slot[i]) {
                1 -> holder.pic.setImageResource(R.drawable.combination_1)
                2 -> holder.pic.setImageResource(R.drawable.combination_2)
                3 -> holder.pic.setImageResource(R.drawable.combination_3)
                4 -> holder.pic.setImageResource(R.drawable.combination_4)
                5 -> holder.pic.setImageResource(R.drawable.combination_5)
                6 -> holder.pic.setImageResource(R.drawable.combination_6)
                7 -> holder.pic.setImageResource(R.drawable.combination_7)
            }
        }

        override fun getItemCount(): Int {
            return Int.MAX_VALUE
        }
    }

    private fun getNotificationToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(
                    MainActivity::class.simpleName,
                    "Fetching FCM registration token failed",
                    task.exception
                )
                return@OnCompleteListener
            }
            val token = task.result
            Log.d(MainActivity::class.simpleName, token)
        })
    }


    companion object {
        const val PREFS_NAME = "FirstRun"
    }
}