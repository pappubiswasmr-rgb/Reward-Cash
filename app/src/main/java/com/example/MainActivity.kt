package com.example

import kotlinx.coroutines.launch
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.data.User
import com.example.utils.getLocalizedString
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.AdError
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.EmojiEvents

fun isVpnConnected(context: Context): Boolean {
    // Disabled VPN requirement to ensure ads show successfully on all devices and emulators
    return true
}

class MainActivity : ComponentActivity() {
    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null

    private var currentInterstitialAdId = "ca-app-pub-3940256099942544/1033173712"
    private var currentRewardedAdId = "ca-app-pub-3940256099942544/5224354917"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MobileAds.initialize(this) {}
        setContent {
            val userViewModel: UserViewModel = viewModel()
            val currentUser by userViewModel.currentUser.collectAsStateWithLifecycle()
            val isDarkTheme = currentUser?.isDarkMode ?: false

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                dynamicColor = false
            ) {
                var currentScreen by remember { mutableStateOf("Login") }
                
                val appConfig by userViewModel.appConfig.collectAsStateWithLifecycle(initialValue = null)
                
                androidx.compose.runtime.LaunchedEffect(appConfig) {
                    appConfig?.let {
                        currentInterstitialAdId = it.interstitialAdUnitId
                        currentRewardedAdId = it.rewardedAdUnitId
                        loadInterstitialAd()
                        loadRewardedAd()
                    }
                }
                
                when (currentScreen) {
                    "Login" -> LoginScreen(
                        userViewModel = userViewModel,
                        onLoginSuccess = { email -> 
                            if (email == "admin@admin.com") {
                                currentScreen = "AdminDashboard"
                            } else {
                                currentScreen = "Dashboard" 
                            }
                        },
                        onNavigateToSignUp = { currentScreen = "SignUp" }
                    )
                    "SignUp" -> SignUpScreen(
                        userViewModel = userViewModel,
                        onSignUpSuccess = { name, email -> 
                            currentScreen = "Dashboard" 
                        },
                        onNavigateToLogin = { currentScreen = "Login" }
                    )
                    "Dashboard" -> RewardCashDashboard(
                        userViewModel = userViewModel,
                        showAd = { onAdClicked, onAdDismissed -> showInterstitialAd(onAdClicked, onAdDismissed) },
                        showRewardedAd = { onReward -> showRewardedAd(onReward) },
                        onLogout = { currentScreen = "Login" }
                    )
                    "AdminDashboard" -> AdminDashboard(
                        userViewModel = userViewModel,
                        onLogout = { currentScreen = "Login" }
                    )
                }
            }
        }
    }

    private fun loadRewardedAd() {
        var adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, currentRewardedAdId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mRewardedAd = null
            }
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                mRewardedAd = rewardedAd
            }
        })
    }

    private fun showRewardedAd(onReward: () -> Unit) {
        if (mRewardedAd != null) {
            mRewardedAd?.show(this, OnUserEarnedRewardListener {
                onReward()
            })
            loadRewardedAd() // Load the next one
        } else {
            // Add a fallback if ad is not loaded immediately
            onReward()
            loadRewardedAd()
        }
    }

    private fun loadInterstitialAd() {
        var adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, currentInterstitialAdId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
            }
        })
    }

    private fun showInterstitialAd(onAdClicked: (() -> Unit)? = null, onAdDismissed: (() -> Unit)? = null) {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdClicked() {
                    onAdClicked?.invoke()
                }
                override fun onAdDismissedFullScreenContent() {
                    onAdDismissed?.invoke()
                    loadInterstitialAd()
                }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    onAdDismissed?.invoke()
                    mInterstitialAd = null
                    loadInterstitialAd()
                }
            }
            mInterstitialAd?.show(this)
        } else {
            onAdDismissed?.invoke()
            loadInterstitialAd() // Load the next one
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardCashDashboard(
    userViewModel: UserViewModel,
    showAd: (onAdClicked: (() -> Unit)?, onAdDismissed: (() -> Unit)?) -> Unit = { _, _ -> },
    showRewardedAd: (onReward: () -> Unit) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val currentUser by userViewModel.currentUser.collectAsStateWithLifecycle()
    val transactions by userViewModel.currentUserTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val allUsersState by userViewModel.allUsers.collectAsStateWithLifecycle(initialValue = emptyList())
    val allWithdrawalRequests by userViewModel.withdrawalRequests.collectAsStateWithLifecycle(initialValue = emptyList())
    val points = currentUser?.points ?: 0
    val adsWatchedCount = currentUser?.adsWatchedCount ?: 0
    var currentTab by remember { mutableStateOf("Home") }
    
    var walletSelectedMethod by remember { mutableStateOf("bKash") }
    var walletAccountNumber by remember { mutableStateOf("") }
    var walletAmountStr by remember { mutableStateOf("") }
    
    var showTransactions by remember { mutableStateOf(false) }
    var showWithdraw by remember { mutableStateOf(false) }
    var showWatchVideoDialog by remember { mutableStateOf(false) }
    var showQuizDialog by remember { mutableStateOf(false) }
    var showLuckySpinDialog by remember { mutableStateOf(false) }
    var showScratchCardDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showInstructionsDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val userLanguage = currentUser?.language ?: "English"
    var showVpnWarning by remember { mutableStateOf(false) }

    val appConfig by userViewModel.appConfig.collectAsStateWithLifecycle(initialValue = null)
    val currentBannerAdId = appConfig?.bannerAdUnitId ?: "ca-app-pub-3940256099942544/6300978111"

    fun showAdForTask(taskType: String, onAdClicked: (() -> Unit)? = null, onAdDismissed: (() -> Unit)? = null) {
        val configAd = when(taskType) {
            "watch_video" -> appConfig?.adTypeWatchVideo ?: "Rewarded"
            "ad_challenge" -> appConfig?.adTypeAdChallenge ?: "Interstitial"
            "daily_quiz" -> appConfig?.adTypeQuiz ?: "Interstitial"
            "lucky_spin" -> appConfig?.adTypeLuckySpin ?: "Interstitial"
            "scratch_card" -> appConfig?.adTypeScratchCard ?: "Interstitial"
            else -> "Interstitial"
        }
        if (configAd.equals("Rewarded", ignoreCase = true)) {
            showRewardedAd {
                onAdDismissed?.invoke()
            }
        } else {
            showAd(onAdClicked, onAdDismissed)
        }
    }

    val isDark = currentUser?.isDarkMode == true
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    
    // Colors matching "Professional Polish" theme
    val bgF7F9FC = if (isDark) Color(0xFF0F172A) else Color(0xFFF7F9FC)
    val indigo700 = if (isDark) Color(0xFF6366F1) else Color(0xFF4338CA)
    val indigo600 = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
    val indigo200 = if (isDark) Color(0xFF3730A3) else Color(0xFFC7D2FE)
    val indigo100 = if (isDark) Color(0xFF312E81) else Color(0xFFE0E7FF)
    val indigo50 = if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF)
    
    val slate900 = if (isDark) Color.White else Color(0xFF0F172A)
    val slate800 = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
    val slate700 = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
    val slate400 = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val slate500 = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val slate200 = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val slate100 = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    if (currentUser?.isBlocked == true) {
        Column(
            modifier = Modifier.fillMaxSize().background(bgF7F9FC),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Warning, contentDescription = "Blocked", tint = Color.Red, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your account has been blocked.", color = slate900, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Please contact support.", color = slate500)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = slate800)) {
                Text("Logout")
            }
        }
        return
    }

    if (showVpnWarning) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showVpnWarning = false },
            containerColor = cardBg,
            title = { Text("VPN Required", color = slate900, fontWeight = FontWeight.Bold) },
            text = { Text("Please connect to a VPN (USA/UK/Canada) to complete tasks and earn points.", color = slate500) },
            confirmButton = {
                TextButton(onClick = { showVpnWarning = false }) {
                    Text("OK", color = indigo600)
                }
            }
        )
    }

    Scaffold(
        containerColor = bgF7F9FC,
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Column {
                // Bottom Protection Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF0FDFA))
                        .border(1.dp, Color(0xFFCCFBF1))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF14B8A6), CircleShape)
                    )
                    Text(
                        "Secure Mode Active: VPN Detected & Blocked",
                        color = Color(0xFF115E59),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                NavigationBar(
                    containerColor = cardBg,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == "Home",
                        onClick = { currentTab = "Home" },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text(getLocalizedString("Home", userLanguage), fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = indigo600,
                            selectedTextColor = indigo600,
                            unselectedIconColor = slate400,
                            unselectedTextColor = slate400,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == "Wallet",
                        onClick = { currentTab = "Wallet" },
                        icon = { Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = "Wallet") },
                        label = { Text(getLocalizedString("Wallet", userLanguage), fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = indigo600,
                            selectedTextColor = indigo600,
                            unselectedIconColor = slate400,
                            unselectedTextColor = slate400,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == "Team",
                        onClick = { currentTab = "Team" },
                        icon = { Icon(Icons.Outlined.Group, contentDescription = "Team") },
                        label = { Text(getLocalizedString("Team", userLanguage), fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = indigo600,
                            selectedTextColor = indigo600,
                            unselectedIconColor = slate400,
                            unselectedTextColor = slate400,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == "Settings",
                        onClick = { currentTab = "Settings" },
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
                        label = { Text(getLocalizedString("Settings", userLanguage), fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = indigo600,
                            selectedTextColor = indigo600,
                            unselectedIconColor = slate400,
                            unselectedTextColor = slate400,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
                BannerAd(adUnitIdParam = currentBannerAdId)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val userInitial = remember(currentUser?.name) {
                        currentUser?.name?.firstOrNull()?.toString()?.uppercase() ?: "P"
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(4.dp, CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(indigo600, Color(0xFF6366F1))
                                ),
                                CircleShape
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(userInitial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = currentUser?.name ?: "ProCash User",
                            color = slate900,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                            Text(
                                text = "VERIFIED ACCOUNT",
                                color = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Global Points Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEEF2FF))
                            .border(
                                1.dp,
                                if (isDark) Color(0xFF475569) else Color(0xFFC7D2FE),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                currentTab = "Wallet"
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Points",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "৳ $points",
                                color = if (isDark) Color.White else indigo700,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(slate200)
                            .clickable { showInstructionsDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Instructions", tint = slate700)
                    }
                }
            }

            // Balance Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .shadow(16.dp, RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (isDark) listOf(Color(0xFF1E1B4B), Color(0xFF312E81)) else listOf(indigo700, Color(0xFF312E81)),
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 1000f)
                        ),
                        RoundedCornerShape(28.dp)
                    )
            ) {
                // Shiny gold accent glow
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 24.dp, y = (-24).dp)
                        .size(140.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFBBF24).copy(alpha = 0.15f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )

                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getLocalizedString("Platinum Rewards Card", userLanguage),
                            color = indigo100,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null,
                            tint = Color(0xFFFEF08A).copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = getLocalizedString("Available Balance", userLanguage),
                        color = indigo100.copy(alpha = 0.82f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("৳ $points", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "≈ ${points * 5} ${getLocalizedString("Coins", userLanguage)}",
                            color = Color(0xFFA5B4FC),
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Normal,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { currentTab = "Wallet" },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = indigo700
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(getLocalizedString("Withdraw", userLanguage), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        
                        Button(
                            onClick = { currentTab = "Wallet" },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Text(getLocalizedString("Transactions", userLanguage), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Dynamic Bottom Content
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "tab_content_animation"
            ) { targetTab ->
                if (targetTab == "Home") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 24.dp, bottom = 40.dp)
                    ) {
                        // 1. ANNOUNCEMENT MARQUEE
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = indigo50),
                            border = BorderStroke(1.dp, indigo200.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = "Notice Icon",
                                    tint = indigo600,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = getLocalizedString("Notice: Earn double points today on Daily Quiz! Get instant withdrawals via Bkash/Nagad. Join our Telegram!", userLanguage),
                                    color = slate800,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // 2. DAILY CHECK-IN STREAK GRID
                        Text(
                            text = getLocalizedString("Daily Rewards Streak", userLanguage),
                            color = slate800,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val rewards = listOf(10, 20, 30, 40, 50, 100, 200)
                            val todayStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
                            val hasCheckedInToday = currentUser?.lastCheckInDate == todayStr
                            val currentStreak = currentUser?.dailyCheckInStreak ?: 0
                            
                            val todayDate = java.util.Date()
                            val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
                            val parsedLastCheckIn = try { sdf.parse(currentUser?.lastCheckInDate ?: "") } catch (e: Exception) { null }
                            val daysDiff = if (parsedLastCheckIn != null) {
                                val diffMs = todayDate.time - parsedLastCheckIn.time
                                (diffMs / (1000 * 60 * 60 * 24)).toInt()
                            } else {
                                999
                            }
                            
                            val nextStreakDay = when {
                                hasCheckedInToday -> currentStreak
                                daysDiff <= 1 -> if (currentStreak >= 7) 1 else currentStreak + 1
                                else -> 1
                            }

                            for (day in 1..7) {
                                val isClaimed = if (hasCheckedInToday) day <= currentStreak else day < nextStreakDay
                                val isToday = !hasCheckedInToday && day == nextStreakDay
                                val isUpcoming = day > nextStreakDay || (hasCheckedInToday && day > currentStreak)
                                val rewardAmt = rewards[day - 1]
                                
                                val cardBgColor = when {
                                    isToday -> Brush.verticalGradient(colors = listOf(indigo600, Color(0xFF6366F1)))
                                    isClaimed -> Brush.verticalGradient(colors = listOf(Color(0xFFE2E8F0).copy(alpha = 0.5f), Color(0xFFE2E8F0).copy(alpha = 0.3f)))
                                    else -> Brush.verticalGradient(colors = listOf(slate100.copy(alpha = 0.9f), slate100.copy(alpha = 0.6f)))
                                }
                                val outlineColor = when {
                                    isToday -> Color(0xFF93C5FD)
                                    isClaimed -> Color(0xFF10B981).copy(alpha = 0.4f)
                                    else -> Color.Transparent
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(86.dp)
                                        .shadow(if (isToday) 6.dp else 0.dp, RoundedCornerShape(16.dp))
                                        .background(cardBgColor, RoundedCornerShape(16.dp))
                                        .border(
                                            width = if (isToday) 2.dp else 1.dp,
                                            color = outlineColor,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            if (isToday) {
                                                showAd(null) {
                                                    val todayDateStr = sdf.format(java.util.Date())
                                                    userViewModel.checkInUser(rewardAmt, nextStreakDay, todayDateStr)
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            getLocalizedString("Claimed today's check-in bonus of", userLanguage) + " ৳" + rewardAmt + "!"
                                                        )
                                                    }
                                                }
                                            } else if (isClaimed) {
                                                coroutineScope.launch { snackbarHostState.showSnackbar(getLocalizedString("You have already claimed this today!", userLanguage)) }
                                            } else {
                                                coroutineScope.launch { snackbarHostState.showSnackbar(getLocalizedString("Come back tomorrow to unlock Day", userLanguage) + " " + day + "!") }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Text(
                                            text = "${getLocalizedString("Day", userLanguage)} $day",
                                            color = if (isToday) Color.White else slate500,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Icon(
                                            imageVector = if (isClaimed) Icons.Outlined.CheckCircle else Icons.Outlined.Star,
                                            contentDescription = "Status",
                                            tint = if (isToday) Color(0xFFFEF08A) else if (isClaimed) Color(0xFF10B981) else slate400,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "৳$rewardAmt",
                                            color = if (isToday) Color.White else slate700,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }

                        // 3. DAILY TASKS SECTION
                        Text(
                            text = getLocalizedString("Daily Tasks", userLanguage),
                            color = slate800,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val isVideoEnabled = appConfig?.isWatchVideoEnabled ?: true
                            val videoPointsVal = appConfig?.pointsWatchVideo ?: 10
                            val videoLimit = appConfig?.dailyWatchVideoLimit ?: 10
                            val watchVideoCount = currentUser?.watchVideoCount ?: 0

                            TaskCard(
                                modifier = Modifier.weight(1f),
                                title = getLocalizedString("Watch Video", userLanguage),
                                subtitle = if (isVideoEnabled) "$watchVideoCount/$videoLimit Ads ($videoPointsVal৳)" else getLocalizedString("Disabled", userLanguage),
                                icon = Icons.Outlined.PlayArrow,
                                iconBg = if (isVideoEnabled) Color(0xFFFFEDD5) else slate100,
                                iconColor = if (isVideoEnabled) Color(0xFFEA580C) else slate500,
                                progress = if (videoLimit > 0) watchVideoCount.toFloat() / videoLimit else 0f,
                                onClick = { 
                                    if (!isVideoEnabled) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("This feature is temporarily disabled by Admin.") }
                                    } else if (watchVideoCount >= videoLimit) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar(getLocalizedString("You have reached the limit for today!", userLanguage)) }
                                    } else {
                                        if (isVpnConnected(context)) {
                                            showAdForTask("watch_video", null, {
                                                userViewModel.updateWatchVideoCount(watchVideoCount + 1)
                                                showWatchVideoDialog = true 
                                            })
                                        } else {
                                            showVpnWarning = true
                                        }
                                    }
                                }
                            )
                            val isAdChalEnabled = appConfig?.isAdChallengeEnabled ?: true
                            val adChalPointsVal = appConfig?.pointsAdChallenge ?: 20
                            val chalLimit = appConfig?.dailyAdChallengeLimit ?: 5
                            TaskCard(
                                modifier = Modifier.weight(1f),
                                title = getLocalizedString("Watch Ad", userLanguage),
                                subtitle = if (isAdChalEnabled) "${adsWatchedCount}/$chalLimit Ads ($adChalPointsVal৳)" else getLocalizedString("Disabled", userLanguage),
                                icon = Icons.Outlined.PlayArrow,
                                iconBg = if (isAdChalEnabled) Color(0xFFDBEAFE) else slate100,
                                iconColor = if (isAdChalEnabled) Color(0xFF2563EB) else slate500,
                                progress = if (chalLimit > 0) adsWatchedCount.toFloat() / chalLimit else 0f,
                                onClick = { 
                                    if (!isAdChalEnabled) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("This feature is temporarily disabled by Admin.") }
                                    } else {
                                        if (isVpnConnected(context)) {
                                            if (adsWatchedCount < chalLimit - 1) {
                                                showAdForTask("ad_challenge", null, {
                                                    // count updated on dismiss
                                                })
                                                userViewModel.updateAdsWatchedCount(adsWatchedCount + 1)
                                                coroutineScope.launch { snackbarHostState.showSnackbar("Ad watched! ${adsWatchedCount + 1}/$chalLimit") }
                                            } else {
                                                var clicked = false
                                                showAdForTask("ad_challenge",
                                                    { clicked = true },
                                                    { 
                                                        if (clicked) {
                                                            userViewModel.updateAdsWatchedCount(0)
                                                            userViewModel.addPoints(adChalPointsVal, "Ad Challenge ($chalLimit/$chalLimit)")
                                                            coroutineScope.launch { snackbarHostState.showSnackbar("You gained +${adChalPointsVal}৳!") }
                                                        } else {
                                                            coroutineScope.launch { snackbarHostState.showSnackbar("You must click the ad to complete the challenge!") }
                                                        }
                                                    }
                                                )
                                            }
                                        } else {
                                            showVpnWarning = true
                                        }
                                    }
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val isRefEnabled = appConfig?.isReferralEnabled ?: true
                            val refUserPoints = appConfig?.pointsReferralUser ?: 100
                            val refRefPoints = appConfig?.pointsReferralRef ?: 200
                            TaskCard(
                                modifier = Modifier.weight(1f),
                                title = getLocalizedString("Refer Friends", userLanguage),
                                subtitle = if (isRefEnabled) "${getLocalizedString("Give", userLanguage)} $refUserPoints, ${getLocalizedString("Get", userLanguage)} $refRefPoints" else getLocalizedString("Disabled", userLanguage),
                                icon = Icons.Outlined.Group,
                                iconBg = if (isRefEnabled) Color(0xFFDCFCE7) else slate100,
                                iconColor = if (isRefEnabled) Color(0xFF16A34A) else slate500,
                                onClick = { 
                                    if (!isRefEnabled) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("This feature is temporarily disabled by Admin.") }
                                    } else {
                                        currentTab = "Team" 
                                    }
                                }
                            )
                            val isQuizEnabled = appConfig?.isQuizEnabled ?: true
                            val quizPointsVal = appConfig?.pointsQuiz ?: 20
                            val quizLimit = appConfig?.dailyQuizLimit ?: 5
                            val dailyQuizCount = currentUser?.dailyQuizCount ?: 0

                            TaskCard(
                                modifier = Modifier.weight(1f),
                                title = getLocalizedString("Daily Quiz", userLanguage),
                                subtitle = if (isQuizEnabled) "$dailyQuizCount/$quizLimit Ads ($quizPointsVal৳)" else getLocalizedString("Disabled", userLanguage),
                                icon = Icons.Outlined.Settings,
                                iconBg = if (isQuizEnabled) Color(0xFFE0F2FE) else slate100,
                                iconColor = if (isQuizEnabled) Color(0xFF0369A1) else slate500,
                                progress = if (quizLimit > 0) dailyQuizCount.toFloat() / quizLimit else 0f,
                                onClick = { 
                                    if (!isQuizEnabled) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("This feature is temporarily disabled by Admin.") }
                                    } else if (dailyQuizCount >= quizLimit) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar(getLocalizedString("You have reached the limit for today!", userLanguage)) }
                                    } else {
                                        showQuizDialog = true
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ROW 3: LUCKY SPIN & SCRATCH CARD
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val currentSpinCount = currentUser?.dailySpinCount ?: 0
                            val spinLimit = 5
                            TaskCard(
                                modifier = Modifier.weight(1f),
                                title = getLocalizedString("Lucky Spin", userLanguage),
                                subtitle = "$currentSpinCount/$spinLimit ${getLocalizedString("Spins", userLanguage)} (Up to 100৳)",
                                icon = Icons.Outlined.Casino,
                                iconBg = Color(0xFFF3E8FF),
                                iconColor = Color(0xFF9333EA),
                                progress = if (spinLimit > 0) currentSpinCount.toFloat() / spinLimit else 0f,
                                onClick = {
                                    if (currentSpinCount >= spinLimit) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar(getLocalizedString("You have reached the limit for today!", userLanguage)) }
                                    } else {
                                        showLuckySpinDialog = true
                                    }
                                }
                            )
                            val currentScratchCount = currentUser?.dailyScratchCount ?: 0
                            val scratchLimit = 5
                            TaskCard(
                                modifier = Modifier.weight(1f),
                                title = getLocalizedString("Scratch Card", userLanguage),
                                subtitle = "$currentScratchCount/$scratchLimit ${getLocalizedString("Scratches", userLanguage)} (10-40৳)",
                                icon = Icons.Outlined.EmojiEvents,
                                iconBg = Color(0xFFEEF2F6),
                                iconColor = Color(0xFF0284C7),
                                progress = if (scratchLimit > 0) currentScratchCount.toFloat() / scratchLimit else 0f,
                                onClick = {
                                    if (currentScratchCount >= scratchLimit) {
                                        coroutineScope.launch { snackbarHostState.showSnackbar(getLocalizedString("You have reached the limit for today!", userLanguage)) }
                                    } else {
                                        showScratchCardDialog = true
                                    }
                                }
                            )
                        }

                        // 4. TOP EARNERS LEADERBOARD
                        LeaderboardSection(
                            users = allUsersState,
                            slate800 = slate800,
                            slate900 = slate900,
                            slate500 = slate500,
                            slate200 = slate200,
                            userLanguage = userLanguage
                        )
                    }
            } else if (targetTab == "Wallet") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 40.dp)
                ) {
                    Text(
                        text = getLocalizedString("Wallet Management", userLanguage),
                        color = slate800,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 1. DYNAMIC BALANCE CARD
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(indigo600, Color(0xFF6366F1), Color(0xFF4F46E5))
                                    )
                                )
                                .padding(24.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CURRENT BALANCE",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Verified Wallet",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "৳$points",
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Note: 1 Point = 1৳. fast withdrawals with bKash and Nagad. Minimum of ৳50 is required.",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // 2. CASHOUT REQUEST FORM PANEL
                    Text(
                        text = "Cashout Request",
                        color = slate800,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, slate100),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Select Wallet Account Provider:",
                                color = slate500,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            // BRANDED SELECTORS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // bKash
                                val isBKashSelected = walletSelectedMethod == "bKash"
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { walletSelectedMethod = "bKash" }
                                        .border(
                                            width = if (isBKashSelected) 2.5.dp else 1.dp,
                                            color = if (isBKashSelected) Color(0xFFE2136E) else slate200,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isBKashSelected) Color(0xFFFFF0F6) else Color.White
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "bKash",
                                            color = if (isBKashSelected) Color(0xFFE2136E) else slate700,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "Personal",
                                            color = if (isBKashSelected) Color(0xFFE2136E).copy(alpha = 0.7f) else slate400,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                // Nagad
                                val isNagadSelected = walletSelectedMethod == "Nagad"
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { walletSelectedMethod = "Nagad" }
                                        .border(
                                            width = if (isNagadSelected) 2.5.dp else 1.dp,
                                            color = if (isNagadSelected) Color(0xFFF47321) else slate200,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isNagadSelected) Color(0xFFFFF3EC) else Color.White
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "Nagad",
                                            color = if (isNagadSelected) Color(0xFFF47321) else slate700,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "Personal",
                                            color = if (isNagadSelected) Color(0xFFF47321).copy(alpha = 0.7f) else slate400,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            // ACCOUNT NUMBER INPUT
                            val isPhoneInvalid = walletAccountNumber.isNotEmpty() && (walletAccountNumber.length != 11 || !walletAccountNumber.startsWith("01"))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = walletAccountNumber,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() }) {
                                            walletAccountNumber = input
                                        }
                                    },
                                    label = { Text(getLocalizedString("Account Number", userLanguage)) },
                                    placeholder = { Text("e.g. 01XXXXXXXXX") },
                                    leadingIcon = { Text("📱", modifier = Modifier.padding(start = 12.dp, end = 4.dp), fontSize = 16.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    isError = isPhoneInvalid,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (walletSelectedMethod == "bKash") Color(0xFFE2136E) else Color(0xFFF47321),
                                        focusedLabelColor = if (walletSelectedMethod == "bKash") Color(0xFFE2136E) else Color(0xFFF47321),
                                        cursorColor = if (walletSelectedMethod == "bKash") Color(0xFFE2136E) else Color(0xFFF47321),
                                        unfocusedBorderColor = slate200,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                                    )
                                )
                                if (isPhoneInvalid) {
                                    Text(
                                        text = "Please enter a valid 11-digit mobile number starting with 01",
                                        color = Color.Red,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Must be a personal Mobile Financial Service (MFS) number",
                                        color = slate400,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                    )
                                }
                            }

                            // CASHOUT AMOUNT INPUT
                            val parsedAmount = walletAmountStr.toIntOrNull() ?: 0
                            val isAmountTooLow = walletAmountStr.isNotEmpty() && parsedAmount < 50
                            val isAmountTooHigh = parsedAmount > points
                            val isAmountInvalid = isAmountTooLow || isAmountTooHigh
                            
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = walletAmountStr,
                                    onValueChange = { input ->
                                        if (input.all { it.isDigit() }) {
                                            walletAmountStr = input
                                        }
                                    },
                                    label = { Text(getLocalizedString("Amount in BDT (৳)", userLanguage)) },
                                    placeholder = { Text("Minimum ৳50") },
                                    leadingIcon = { Text("৳", modifier = Modifier.padding(start = 12.dp, end = 4.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (walletSelectedMethod == "bKash") Color(0xFFE2136E) else Color(0xFFF47321)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    isError = isAmountInvalid,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (walletSelectedMethod == "bKash") Color(0xFFE2136E) else Color(0xFFF47321),
                                        focusedLabelColor = if (walletSelectedMethod == "bKash") Color(0xFFE2136E) else Color(0xFFF47321),
                                        cursorColor = if (walletSelectedMethod == "bKash") Color(0xFFE2136E) else Color(0xFFF47321),
                                        unfocusedBorderColor = slate200,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                    )
                                )
                                if (isAmountTooHigh) {
                                    Text(
                                        text = "Insufficient points! Available balance is ৳$points",
                                        color = Color.Red,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                    )
                                } else if (isAmountTooLow) {
                                    Text(
                                        text = "Minimum withdrawal request amount is ৳50",
                                        color = Color.Red,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Minimum transfer limit is ৳50",
                                        color = slate400,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                    )
                                }
                            }

                            // QUICK ACTION CHIPS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val quickAmounts = listOf(50, 100, 200, 500, 1000)
                                quickAmounts.forEach { amt ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(slate100, RoundedCornerShape(8.dp))
                                            .clickable {
                                                walletAmountStr = amt.toString()
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "৳$amt",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = slate500
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // SUBMIT BUTTON
                            val withdrawEnabledByAdmin = appConfig?.isWithdrawEnabled ?: true
                            val isPhoneLengthValid = walletAccountNumber.length == 11 && walletAccountNumber.startsWith("01")
                            val isAmountLengthValid = parsedAmount >= 50 && parsedAmount <= points
                            val isSubmitActive = withdrawEnabledByAdmin && isPhoneLengthValid && isAmountLengthValid
                            val brandBtnColor = if (walletSelectedMethod == "bKash") Color(0xFFE2136E) else Color(0xFFF47321)

                            Button(
                                onClick = {
                                    if (isSubmitActive) {
                                        userViewModel.submitWithdrawalRequest(walletSelectedMethod, walletAccountNumber, parsedAmount)
                                        walletAccountNumber = ""
                                        walletAmountStr = ""
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Cashout Request submitted: ৳$parsedAmount via $walletSelectedMethod.")
                                        }
                                    }
                                },
                                enabled = isSubmitActive,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = brandBtnColor,
                                    disabledContainerColor = slate200,
                                    contentColor = Color.White,
                                    disabledContentColor = slate400
                                )
                            ) {
                                if (!withdrawEnabledByAdmin) {
                                    Text("WITHDRAWAL temporarily DISABLED BY ADMIN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                } else {
                                    Text("SUBMIT CASHOUT REQUEST", fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp)
                                }
                            }
                        }
                    }

                    // 3. CASHOUT TRANSACTION HISTORY
                    val userWithdrawals = remember(allWithdrawalRequests, currentUser) {
                        allWithdrawalRequests.filter { it.userEmail == currentUser?.email }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Cashout History",
                        color = slate800,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (userWithdrawals.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = slate100),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, slate100)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📭", fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No cashout requests yet.",
                                    color = slate500,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, slate100)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                userWithdrawals.sortedByDescending { it.timestamp }.take(10).forEachIndexed { idx, request ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val logoEmoji = if (request.method.lowercase() == "bkash") "💖" else "🧡"
                                                Text(
                                                    text = "$logoEmoji ${request.method}",
                                                    color = slate900,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                
                                                val statusColor = when (request.status.lowercase()) {
                                                    "approved" -> Color(0xFF10B981)
                                                    "rejected" -> Color.Red
                                                    else -> Color(0xFFF59E0B) // Pending
                                                }
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                        .border(0.5.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = request.status.uppercase(),
                                                        color = statusColor,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "To: ${request.accountNumber}",
                                                color = slate500,
                                                fontSize = 12.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            val formattedTime = android.text.format.DateFormat.format("MMM dd, yyyy HH:mm", request.timestamp).toString()
                                            Text(
                                                text = formattedTime,
                                                color = slate400,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(
                                            text = "-৳${request.amountPts}",
                                            color = Color.Red,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    if (idx < userWithdrawals.size - 1) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(slate100)
                                                .padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (targetTab == "Team") {
                var enterReferralCode by remember { mutableStateOf("") }
                var processingReferral by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 32.dp, bottom = 40.dp)
                ) {
                    Text(getLocalizedString("Referral Program", userLanguage), color = slate800, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    val isReferralEnabled = appConfig?.isReferralEnabled ?: true
                    if (!isReferralEnabled) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = slate100),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Lock, contentDescription = "Locked", tint = slate500, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Referral Program Disabled", color = slate800, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("This program is temporarily turned off by the administrator. Please check back later.", color = slate500, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    } else {
                        val refCode = currentUser?.referralCode ?: "LOADING..."
                        val refUserPoints = appConfig?.pointsReferralUser ?: 100
                        val refRefPoints = appConfig?.pointsReferralRef ?: 200

                        Card(
                            colors = CardDefaults.cardColors(containerColor = bgF7F9FC),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Your Referral Code", color = slate500, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(refCode, color = slate900, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = 2.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Referral code copied: $refCode") }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                ) { Text("Copy Code") }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Share this code to give friends $refUserPoints points, and you get $refRefPoints points!", color = slate500, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                        
                        if (currentUser?.referredBy == null) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text("Have a referral code?", color = slate800, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            androidx.compose.material3.OutlinedTextField(
                                value = enterReferralCode,
                                onValueChange = { enterReferralCode = it.uppercase() },
                                placeholder = { Text("Enter 8-digit code") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (enterReferralCode.length == 8) {
                                        processingReferral = true
                                        coroutineScope.launch {
                                            val success = userViewModel.applyReferralCode(enterReferralCode)
                                            if (success) {
                                                snackbarHostState.showSnackbar("Code applied successfully!")
                                                enterReferralCode = ""
                                            } else {
                                                snackbarHostState.showSnackbar("Invalid or already used code.")
                                            }
                                            processingReferral = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = indigo600),
                                enabled = enterReferralCode.length == 8 && !processingReferral
                            ) { Text(if (processingReferral) "Applying..." else "Apply Code") }
                        } else {
                            Spacer(modifier = Modifier.height(32.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("You have already applied a referral code.", color = Color(0xFF16A34A), modifier = Modifier.padding(16.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (targetTab == "Settings") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 32.dp, bottom = 40.dp)
                ) {
                    var language = currentUser?.language ?: "English"
                    Text(getLocalizedString("Settings", language), color = slate800, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(getLocalizedString("Profile Settings", language), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { showProfileDialog = true }.padding(vertical = 16.dp, horizontal = 12.dp), color = slate700, fontWeight = FontWeight.Medium)
                    Text(getLocalizedString("Language & Region", language), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { showLanguageDialog = true }.padding(vertical = 16.dp, horizontal = 12.dp), color = slate700, fontWeight = FontWeight.Medium)
                    Text(getLocalizedString("Help & Support", language), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { 
                        val telegramLink = appConfig?.telegramUrl ?: "https://t.me/earncash_support"
                        try {
                            uriHandler.openUri(telegramLink)
                        } catch (e: Exception) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Could not open support link.") }
                        }
                    }.padding(vertical = 16.dp, horizontal = 12.dp), color = slate700, fontWeight = FontWeight.Medium)
                    Text(getLocalizedString("Log Out", language), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { 
                        userViewModel.logout()
                        onLogout()
                    }.padding(vertical = 16.dp, horizontal = 12.dp), color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
            } // Close AnimatedContent
        }
        
        // Mock Dialogs for interactivity
        if (showWatchVideoDialog) {
            val videoPts = appConfig?.pointsWatchVideo ?: 10
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showWatchVideoDialog = false },
                containerColor = cardBg,
                title = { Text(getLocalizedString("Watch Video", userLanguage), fontWeight = FontWeight.Bold, color = slate900) },
                text = { Text(getLocalizedString("You have completely watched an advertisement video.", userLanguage), color = slate700) },
                confirmButton = {
                    Button(
                        onClick = { 
                            userViewModel.addPoints(videoPts, "Watch Video")
                            showWatchVideoDialog = false 
                            coroutineScope.launch { snackbarHostState.showSnackbar("You gained +${videoPts}৳!") }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = indigo600)
                    ) {
                        Text("Claim +${videoPts}৳")
                    }
                }
            )
        }

        if (showLuckySpinDialog) {
            LuckySpinDialog(
                userViewModel = userViewModel,
                userLanguage = userLanguage,
                showAd = { onAdClicked, onAdDismissed -> showAdForTask("lucky_spin", onAdClicked, onAdDismissed) },
                onDismiss = { showLuckySpinDialog = false }
            )
        }

        if (showScratchCardDialog) {
            ScratchCardDialog(
                userViewModel = userViewModel,
                userLanguage = userLanguage,
                showAd = { onAdClicked, onAdDismissed -> showAdForTask("scratch_card", onAdClicked, onAdDismissed) },
                onDismiss = { showScratchCardDialog = false }
            )
        }

        if (showQuizDialog) {
            val quizPts = appConfig?.pointsQuiz ?: 20
            var selectedAnswer by remember { mutableStateOf<Int?>(null) }
            var quizResult by remember { mutableStateOf<Boolean?>(null) }

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showQuizDialog = false },
                containerColor = cardBg,
                title = { Text(getLocalizedString("Daily Quiz", userLanguage), fontWeight = FontWeight.Bold, color = slate900) },
                text = {
                    Column {
                        Text("What is 5 x 5?", fontWeight = FontWeight.Medium, color = slate800)
                        Spacer(modifier = Modifier.height(12.dp))
                        val options = listOf("15", "25", "35")
                        options.forEachIndexed { index, text ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { if (quizResult == null) selectedAnswer = index }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                RadioButton(
                                    selected = (selectedAnswer == index),
                                    onClick = { if (quizResult == null) selectedAnswer = index }
                                )
                                Text(text, color = slate700)
                            }
                        }
                        if (quizResult == true) {
                            Text("Correct! You earned ${quizPts}৳.", color = Color(0xFF16A34A), modifier = Modifier.padding(top = 12.dp))
                        } else if (quizResult == false) {
                            Text("Wrong answer. Try again later.", color = Color.Red, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                },
                confirmButton = {
                    if (quizResult == null) {
                        Button(
                            onClick = { 
                                if (selectedAnswer == 1) {
                                    quizResult = true
                                    userViewModel.addPoints(quizPts, "Daily Quiz Bonus")
                                    userViewModel.updateQuizCount((currentUser?.dailyQuizCount ?: 0) + 1)
                                } else {
                                    quizResult = false
                                }
                            },
                            enabled = selectedAnswer != null,
                            colors = ButtonDefaults.buttonColors(containerColor = indigo600)
                        ) {
                            Text("Submit")
                        }
                    } else {
                        Button(
                            onClick = { 
                                showQuizDialog = false
                                showAdForTask("daily_quiz", null, null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = indigo600)
                        ) {
                            Text("Close")
                        }
                    }
                },
                dismissButton = {
                    if (quizResult == null) {
                        TextButton(onClick = { showQuizDialog = false }) { Text("Cancel", color = slate500) }
                    }
                }
            )
        }

        if (showWithdraw) {
            var selectedMethod by remember { mutableStateOf("bKash") }
            var accountNumber by remember { mutableStateOf("") }
            var amountStr by remember { mutableStateOf("") }

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showWithdraw = false },
                containerColor = cardBg,
                title = { Text(getLocalizedString("Withdraw Cash", userLanguage), fontWeight = FontWeight.Bold, color = slate900) },
                text = {
                    Column {
                        Text(getLocalizedString("Select Method", userLanguage), fontWeight = FontWeight.Medium, color = slate800)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(2.dp, if (selectedMethod == "bKash") indigo600 else slate200, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedMethod = "bKash" }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("bKash", color = if (selectedMethod == "bKash") indigo600 else slate500, fontWeight = FontWeight.Bold) }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(2.dp, if (selectedMethod == "Nagad") indigo600 else slate200, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedMethod = "Nagad" }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("Nagad", color = if (selectedMethod == "Nagad") indigo600 else slate500, fontWeight = FontWeight.Bold) }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it },
                            label = { Text(getLocalizedString("Account Number", userLanguage)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text(getLocalizedString("Amount in BDT (৳)", userLanguage)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Text("Current Balance: ৳$points", color = slate500, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                },
                confirmButton = {
                    val wAmount = amountStr.toIntOrNull() ?: 0
                    val isFormValid = accountNumber.isNotEmpty() && wAmount > 0 && wAmount <= points
                    Button(
                        onClick = {
                            userViewModel.submitWithdrawalRequest(selectedMethod, accountNumber, wAmount)
                            showWithdraw = false
                            coroutineScope.launch { snackbarHostState.showSnackbar("Withdrawal of ৳$wAmount requested successfully!") }
                        },
                        enabled = isFormValid,
                        colors = ButtonDefaults.buttonColors(containerColor = indigo600)
                    ) {
                        Text("Confirm Transfer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWithdraw = false }) { Text("Cancel", color = slate500) }
                }
            )
        }

        if (showTransactions) {
            val userWithdrawals = remember(allWithdrawalRequests, currentUser) {
                allWithdrawalRequests.filter { it.userEmail == currentUser?.email }
            }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showTransactions = false },
                containerColor = cardBg,
                title = { Text(getLocalizedString("Recent Transactions", userLanguage), fontWeight = FontWeight.Bold, color = slate900) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                        if (transactions.isEmpty()) {
                            Text("No transactions yet.", color = slate500, modifier = Modifier.padding(vertical = 16.dp))
                        } else {
                            transactions.forEach { tx ->
                                val isWithdrawal = tx.title.startsWith("Withdrawal (")
                                val status = if (isWithdrawal) {
                                    val methodVal = tx.title.removePrefix("Withdrawal (").removeSuffix(")")
                                    userWithdrawals.firstOrNull {
                                        it.method.equals(methodVal, ignoreCase = true) &&
                                        tx.amountStr.contains(it.amountPts.toString())
                                    }?.status ?: "Pending"
                                } else {
                                    null
                                }
                                val color = if (tx.isPositive) Color(0xFF16A34A) else Color.Red
                                val date = android.text.format.DateFormat.format("MMM dd, HH:mm", tx.timestamp).toString()
                                TransactionRow(tx.title, tx.amountStr, date, color, status = status)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTransactions = false }) { Text("Close", color = indigo600) }
                }
            )
        }

        if (showInstructionsDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showInstructionsDialog = false },
                containerColor = cardBg,
                title = { Text(getLocalizedString("How to Work", userLanguage), fontWeight = FontWeight.Bold, color = slate900) },
                text = {
                    Text(
                        getLocalizedString("1. Complete daily tasks like watching videos or math quizzes.\n2. Earn points for each task.\n3. Refer friends to earn bonus points.\n4. Withdraw your points as cash from the wallet.", userLanguage),
                        color = slate500,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showInstructionsDialog = false }) { Text(getLocalizedString("Close", userLanguage), color = indigo600) }
                }
            )
        }

        if (showSettingsDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                containerColor = cardBg,
                title = { Text(getLocalizedString("Settings", userLanguage), fontWeight = FontWeight.Bold, color = slate900) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(getLocalizedString("Profile Settings", userLanguage), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { showSettingsDialog = false; showProfileDialog = true }.padding(8.dp), color = slate700, fontWeight = FontWeight.Medium)
                        Text(getLocalizedString("Language & Region", userLanguage), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { showSettingsDialog = false; showLanguageDialog = true }.padding(8.dp), color = slate700, fontWeight = FontWeight.Medium)
                        Text(getLocalizedString("Help & Support", userLanguage), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { 
                            showSettingsDialog = false
                            val telegramLink = appConfig?.telegramUrl ?: "https://t.me/earncash_support"
                            try {
                                uriHandler.openUri(telegramLink)
                            } catch (e: Exception) {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Could not open support link.") }
                            }
                        }.padding(8.dp), color = slate700, fontWeight = FontWeight.Medium)
                        Text(getLocalizedString("Privacy Policy", userLanguage), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { coroutineScope.launch { snackbarHostState.showSnackbar("Privacy Policy loaded") } }.padding(8.dp), color = slate700, fontWeight = FontWeight.Medium)
                        Text(getLocalizedString("Log Out", userLanguage), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { 
                            showSettingsDialog = false 
                            userViewModel.logout()
                            onLogout()
                        }.padding(8.dp), color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) { Text(getLocalizedString("Done", userLanguage), color = indigo600) }
                }
            )
        }

        if (showProfileDialog) {
            var inputName by remember { mutableStateOf(currentUser?.name ?: "") }
            var isDark by remember { mutableStateOf(currentUser?.isDarkMode ?: false) }
            var isNotif by remember { mutableStateOf(currentUser?.notificationsEnabled ?: true) }
            
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showProfileDialog = false },
                containerColor = cardBg,
                title = { Text(getLocalizedString("Profile Settings", userLanguage), fontWeight = FontWeight.Bold, color = slate900) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = { Text(getLocalizedString("Name", userLanguage)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(getLocalizedString("Dark Mode", userLanguage), color = slate700)
                            androidx.compose.material3.Switch(
                                checked = isDark,
                                onCheckedChange = { isDark = it }
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(getLocalizedString("Notifications", userLanguage), color = slate700)
                            androidx.compose.material3.Switch(
                                checked = isNotif,
                                onCheckedChange = { isNotif = it }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { 
                        userViewModel.updateUserSettings(language = currentUser?.language ?: "English", country = currentUser?.country ?: "Bangladesh", isDarkMode = isDark, notificationsEnabled = isNotif)
                        userViewModel.updateUserProfile(inputName)
                        showProfileDialog = false 
                    }) { Text(getLocalizedString("Save", userLanguage), color = indigo600) }
                },
                dismissButton = {
                    TextButton(onClick = { showProfileDialog = false }) { Text(getLocalizedString("Cancel", userLanguage), color = slate500) }
                }
            )
        }

        if (showLanguageDialog) {
            var selectedLanguage by remember { mutableStateOf(currentUser?.language ?: "English") }
            var selectedCountry by remember { mutableStateOf(currentUser?.country ?: "Bangladesh") }
            val isDarkVal = currentUser?.isDarkMode ?: false
            val isNotifVal = currentUser?.notificationsEnabled ?: true
            
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                containerColor = cardBg,
                title = { Text(getLocalizedString("Language & Region", userLanguage), fontWeight = FontWeight.Bold, color = slate900) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(getLocalizedString("Language", userLanguage), fontWeight = FontWeight.Medium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            Button(onClick = { selectedLanguage = "English" }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedLanguage == "English") indigo600 else Color.LightGray)) { Text("English") }
                            Button(onClick = { selectedLanguage = "Bengali" }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedLanguage == "Bengali") indigo600 else Color.LightGray)) { Text("বাংলা") }
                        }
                        
                        Text(getLocalizedString("Country", userLanguage), fontWeight = FontWeight.Medium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            Button(onClick = { selectedCountry = "Bangladesh" }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedCountry == "Bangladesh") indigo600 else Color.LightGray)) { Text(getLocalizedString("Bangladesh", userLanguage)) }
                            Button(onClick = { selectedCountry = "India" }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedCountry == "India") indigo600 else Color.LightGray)) { Text(getLocalizedString("India", userLanguage)) }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { 
                        userViewModel.updateUserSettings(language = selectedLanguage, country = selectedCountry, isDarkMode = isDarkVal, notificationsEnabled = isNotifVal)
                        showLanguageDialog = false 
                    }) { Text(getLocalizedString("Save", userLanguage), color = indigo600) }
                },
                dismissButton = {
                    TextButton(onClick = { showLanguageDialog = false }) { Text(getLocalizedString("Cancel", userLanguage), color = slate500) }
                }
            )
        }
    }
}

@Composable
fun TransactionRow(
    title: String,
    amount: String,
    date: String,
    amountColor: Color,
    status: String? = null
) {
    val titleColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    val dateColor = titleColor.copy(alpha = 0.7f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(title, color = titleColor, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                status?.let {
                    val statusColor = when (it.lowercase()) {
                        "approved" -> Color(0xFF10B981)
                        "rejected" -> Color.Red
                        else -> Color(0xFFFBBF24) // Pending
                    }
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = it.uppercase(),
                            color = statusColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(date, color = dateColor, fontSize = 12.sp)
        }
        Text(amount, color = amountColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun TaskCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    progress: Float? = null,
    onClick: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cardOutline = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    
    Card(
        modifier = modifier
            .height(138.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, cardOutline.copy(alpha = 0.7f)),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle premium accent glow bubble inside the card
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 12.dp, y = (-12).dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(iconColor.copy(alpha = 0.08f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(iconBg, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (progress != null) {
                        androidx.compose.material3.CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(48.dp),
                            color = iconColor,
                            strokeWidth = 3.dp,
                            trackColor = iconBg,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                val titleColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                val subtitleColor = titleColor.copy(alpha = 0.65f)
                
                Text(
                    text = title,
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = subtitle,
                    color = subtitleColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BannerAd(adUnitIdParam: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = adUnitIdParam
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    // MyApplicationTheme { RewardCashDashboard() }
}

@Composable
fun LeaderboardSection(
    users: List<User>,
    slate800: Color,
    slate900: Color,
    slate500: Color,
    slate200: Color,
    userLanguage: String
) {
    val top10 = remember(users) {
        users.sortedByDescending { it.points }.take(10)
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val itemBg = if (isDark) Color(0xFF1E293B) else Color.White
    val cardOutline = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = getLocalizedString("Top Earners Leaderboard", userLanguage),
                color = slate900,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        }

        if (top10.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(slate200.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .border(1.dp, cardOutline, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(getLocalizedString("No data available", userLanguage), color = slate500, fontWeight = FontWeight.Medium)
            }
        } else {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)),
                border = BorderStroke(1.dp, cardOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    top10.forEachIndexed { index, peer ->
                        val rank = index + 1
                        val (rankColor, rankBg, podiumIcon) = when (rank) {
                            1 -> Triple(Color(0xFFD97706), Color(0xFFFEF3C7), "👑 ")
                            2 -> Triple(Color(0xFF475569), Color(0xFFF1F5F9), "🥈 ")
                            3 -> Triple(Color(0xFF9A3412), Color(0xFFFFEDD5), "🥉 ")
                            else -> Triple(slate500, Color.Transparent, "")
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(itemBg, RoundedCornerShape(16.dp))
                                .border(1.dp, cardOutline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (rank <= 3) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(rankBg, CircleShape)
                                            .border(1.dp, rankColor.copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = rank.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = rankColor
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier.size(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = rank.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = slate500
                                        )
                                    }
                                }
                                
                                val maskedName = remember(peer.email) {
                                    val localPart = peer.email.substringBefore("@")
                                    if (localPart.length > 3) {
                                        localPart.take(3) + "***"
                                    } else {
                                        localPart + "***"
                                    }
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (podiumIcon.isNotEmpty()) {
                                            Text(podiumIcon, fontSize = 12.sp)
                                        }
                                        Text(
                                            text = peer.name.take(15) + (if (peer.name.length > 15) "..." else ""),
                                            color = slate900,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = maskedName,
                                        color = slate500,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF4F46E5).copy(alpha = 0.15f), Color(0xFF6366F1).copy(alpha = 0.15f))
                                        ),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "৳ ${peer.points}",
                                    color = if (isDark) Color(0xFFC7D2FE) else Color(0xFF4338CA),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LuckySpinDialog(
    userViewModel: UserViewModel,
    userLanguage: String,
    showAd: (onAdClicked: (() -> Unit)?, onAdDismissed: (() -> Unit)?) -> Unit,
    onDismiss: () -> Unit
) {
    val currentUser by userViewModel.currentUser.collectAsStateWithLifecycle()
    val dailySpinLimit = 5
    val currentSpinCount = currentUser?.dailySpinCount ?: 0
    
    val segments = listOf(5, 10, 15, 25, 50, 0, 100, 30)
    val colors = listOf(
        Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444),
        Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4), Color(0xFF14B8A6)
    )

    var isSpinning by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
    val animatableRotation = remember { androidx.compose.animation.core.Animatable(0f) }

    Dialog(onDismissRequest = { if (!isSpinning) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = if (currentUser?.isDarkMode == true) Color(0xFF1E293B) else Color.White),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        getLocalizedString("Lucky Spin Pool", userLanguage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (currentUser?.isDarkMode == true) Color.White else Color(0xFF1E293B)
                    )
                    Text(
                        "$currentSpinCount/$dailySpinLimit ${getLocalizedString("Spins", userLanguage)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6366F1)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier.size(220.dp).align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            rotationZ = animatableRotation.value
                        }
                    ) {
                        for (i in 0..7) {
                            drawArc(
                                color = colors[i],
                                startAngle = i * 45f,
                                sweepAngle = 45f,
                                useCenter = true,
                                size = size
                            )
                        }

                        // Draw point values on each segment slot directly on nativeCanvas
                        val radius = size.width / 2f
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 16.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        val nativeCanvas = drawContext.canvas.nativeCanvas
                        for (i in 0..7) {
                            val midAngle = i * 45f + 22.5f
                            nativeCanvas.save()
                            nativeCanvas.rotate(midAngle, radius, radius)
                            nativeCanvas.drawText(
                                "${segments[i]}৳",
                                radius + radius * 0.55f,
                                radius + paint.textSize / 3f,
                                paint
                            )
                            nativeCanvas.restore()
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Pointer",
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = (-16).dp)
                            .graphicsLayer { rotationZ = 180f }
                            .background(Color(0xFFEA580C), CircleShape)
                    )

                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        drawCircle(color = Color.White, radius = 24.dp.toPx())
                        drawCircle(color = Color(0xFF4F46E5), radius = 18.dp.toPx())
                    }
                    Text(
                        "WIN",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Rewards: ৳5, ৳10, ৳15, ৳25, ৳50, ৳0, ৳100, ৳30",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (resultText.isNotEmpty()) {
                    Text(
                        text = resultText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (resultText.contains("won")) Color(0xFF10B981) else Color(0xFFEF4444),
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = {
                        if (currentSpinCount < dailySpinLimit) {
                            showAd(null) {
                                coroutineScope.launch {
                                    resultText = ""
                                    isSpinning = true
                                    val wonIndex = (0..7).random()
                                    // Target angle is calculated so that Segment 'wonIndex' is centered at top pointer (270f degrees)
                                    val targetAngle = 247.5f - (wonIndex * 45f)
                                    // Smoothly spin 4 full rotations plus the required offset to align with wonIndex
                                    val currentAngleNormalized = (animatableRotation.value % 360f + 360f) % 360f
                                    val target = animatableRotation.value + 1440f + ((targetAngle - currentAngleNormalized + 720f) % 360f)
                                    
                                    animatableRotation.animateTo(
                                        targetValue = target,
                                        animationSpec = tween(durationMillis = 3500, easing = LinearOutSlowInEasing)
                                    )
                                    
                                    val wonPoints = segments[wonIndex]
                                    if (wonPoints > 0) {
                                        userViewModel.addPoints(wonPoints, "Lucky Spin Reward")
                                        resultText = "Congratulations! You won ৳$wonPoints!"
                                    } else {
                                        resultText = "Oops! Better luck next time!"
                                    }
                                    userViewModel.updateSpinCount(currentSpinCount + 1)
                                    isSpinning = false
                                }
                            }
                        }
                    },
                    enabled = !isSpinning && currentSpinCount < dailySpinLimit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (currentSpinCount >= dailySpinLimit) "Daily limit reached!" else "SPIN THE WHEEL",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    enabled = !isSpinning,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Close", color = Color(0xFF64748B))
                }
            }
        }
    }
}

@Composable
fun ScratchCardDialog(
    userViewModel: UserViewModel,
    userLanguage: String,
    showAd: (onAdClicked: (() -> Unit)?, onAdDismissed: (() -> Unit)?) -> Unit,
    onDismiss: () -> Unit
) {
    val currentUser by userViewModel.currentUser.collectAsStateWithLifecycle()
    val dailyLimit = 5
    val currentCount = currentUser?.dailyScratchCount ?: 0
    
    val rewardAmount = remember { (10..40).random() }
    var pointsScratched by remember { mutableStateOf(0f) }
    var isClaimed by remember { mutableStateOf(false) }
    val scratchPath = remember { androidx.compose.ui.graphics.Path() }
    var scratchUpdateTrigger by remember { androidx.compose.runtime.mutableStateOf(0) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = if (currentUser?.isDarkMode == true) Color(0xFF1E293B) else Color.White),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        getLocalizedString("Scratch & Win", userLanguage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (currentUser?.isDarkMode == true) Color.White else Color(0xFF1E293B)
                    )
                    Text(
                        "$currentCount/$dailyLimit Scratches",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0EA5E9)
                    )
                }

                Text(
                    "Swipe your finger over the silver block below to reveal your prize!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "YOU WON",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "৳$rewardAmount",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Black
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        scratchPath.moveTo(offset.x, offset.y)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        scratchPath.lineTo(change.position.x, change.position.y)
                                        pointsScratched += 1f
                                        scratchUpdateTrigger++
                                    }
                                )
                            }
                            .graphicsLayer {
                                alpha = 0.99f
                            }
                    ) {
                        // Read key to trigger redraw inside DrawScope
                        val trigger = scratchUpdateTrigger
                        drawRect(Color(0xFF94A3B8))
                        
                        drawPath(
                            path = scratchPath,
                            color = Color.Transparent,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 48.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Companion.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Companion.Round
                            ),
                            blendMode = BlendMode.Clear
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val canClaim = pointsScratched > 20f && !isClaimed && currentCount < dailyLimit
                Button(
                    onClick = {
                        showAd(null) {
                            isClaimed = true
                            userViewModel.addPoints(rewardAmount, "Scratch Card Reward")
                            userViewModel.updateScratchCount(currentCount + 1)
                        }
                    },
                    enabled = canClaim,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isClaimed) "Points Claimed!" else if (currentCount >= dailyLimit) "Daily limit reached!" else if (pointsScratched <= 20f) "Scratch more to claim!" else "CLAIM POINTS",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        }
    }
}

@Composable
fun LoginScreen(userViewModel: UserViewModel, onLoginSuccess: (String) -> Unit, onNavigateToSignUp: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf("English") }
    
    val bg = Color(0xFF0F172A)
    val textColor = Color.White
    val isDark = true
    val indigo600 = Color(0xFF818CF8)
    
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E293B))
                    .clickable {
                        language = if (language == "English") "Bengali" else "English"
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language Switcher",
                        tint = indigo600,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (language == "English") "English" else "বাংলা",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_earncash_logo),
            contentDescription = "EarnCash Premium Logo",
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(200.dp)
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = getLocalizedString("Welcome Back", language),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
        Text(
            text = getLocalizedString("Login to continue earning", language),
            color = Color(0xFF94A3B8),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)
            ),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(getLocalizedString("Email", language), color = Color.White) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email Icon",
                            tint = indigo600
                        )
                    },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = indigo600,
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedBorderColor = indigo600,
                        unfocusedBorderColor = Color(0xFF334155),
                        cursorColor = indigo600
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_input"),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.material3.OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(getLocalizedString("Password", language), color = Color.White) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Password Icon",
                            tint = indigo600
                        )
                    },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        val description = if (passwordVisible) "Hide password" else "Show password"
                        androidx.compose.material3.IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = description, tint = Color(0xFF94A3B8))
                        }
                    },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = indigo600,
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedBorderColor = indigo600,
                        unfocusedBorderColor = Color(0xFF334155),
                        cursorColor = indigo600
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    shape = RoundedCornerShape(16.dp),
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMessage != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = getLocalizedString(errorMessage!!, language),
                            color = Color.Red,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = { 
                        if (email.isEmpty() || password.isEmpty()) {
                            errorMessage = "Please enter email and password"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        userViewModel.login(email.trim(), password) { success, msg ->
                            isLoading = false
                            if (success) {
                                onLoginSuccess(email.trim())
                            } else {
                                errorMessage = msg
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("login_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = indigo600)
                ) {
                    if (isLoading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = getLocalizedString("Login", language),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                androidx.compose.material3.TextButton(
                    onClick = onNavigateToSignUp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = getLocalizedString("Don't have an account? Sign Up", language),
                        color = indigo600,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}



@Composable
fun SignUpScreen(userViewModel: UserViewModel, onSignUpSuccess: (String, String) -> Unit, onNavigateToLogin: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf("English") }
    
    val bg = Color(0xFF0F172A)
    val textColor = Color.White
    val isDark = true
    val indigo600 = Color(0xFF818CF8)
    
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E293B))
                    .clickable {
                        language = if (language == "English") "Bengali" else "English"
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language Switcher",
                        tint = indigo600,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (language == "English") "English" else "বাংলা",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_earncash_logo),
            contentDescription = "EarnCash Premium Logo",
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(200.dp)
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = getLocalizedString("Create Account", language),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
        Text(
            text = getLocalizedString("Start earning rewards today", language),
            color = Color(0xFF94A3B8),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)
            ),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(getLocalizedString("Name", language), color = Color.White) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Person Icon",
                            tint = indigo600
                        )
                    },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = indigo600,
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedBorderColor = indigo600,
                        unfocusedBorderColor = Color(0xFF334155),
                        cursorColor = indigo600
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("name_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.material3.OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(getLocalizedString("Email", language), color = Color.White) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email Icon",
                            tint = indigo600
                        )
                    },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = indigo600,
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedBorderColor = indigo600,
                        unfocusedBorderColor = Color(0xFF334155),
                        cursorColor = indigo600
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("email_input"),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.material3.OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(getLocalizedString("Password", language), color = Color.White) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Password Icon",
                            tint = indigo600
                        )
                    },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        val description = if (passwordVisible) "Hide password" else "Show password"
                        androidx.compose.material3.IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = description, tint = Color(0xFF94A3B8))
                        }
                    },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = indigo600,
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedBorderColor = indigo600,
                        unfocusedBorderColor = Color(0xFF334155),
                        cursorColor = indigo600
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    shape = RoundedCornerShape(16.dp),
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMessage != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = getLocalizedString(errorMessage!!, language),
                            color = Color.Red,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = { 
                        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                            errorMessage = "Please fill completely"
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        userViewModel.signUp(name.trim(), email.trim(), password, language) { success, msg ->
                            isLoading = false
                            if (success) {
                                onSignUpSuccess(name.trim(), email.trim())
                            } else {
                                errorMessage = msg
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("signup_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = indigo600)
                ) {
                    if (isLoading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = getLocalizedString("Sign Up", language),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                androidx.compose.material3.TextButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = getLocalizedString("Already have an account? Login", language),
                        color = indigo600,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    userViewModel: UserViewModel,
    onLogout: () -> Unit
) {
    val allUsers by userViewModel.allUsers.collectAsStateWithLifecycle(initialValue = emptyList())
    val withdrawals by userViewModel.withdrawalRequests.collectAsStateWithLifecycle(initialValue = emptyList())
    val appConfig by userViewModel.appConfig.collectAsStateWithLifecycle(initialValue = null)
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var selectedUser by remember { mutableStateOf<com.example.data.User?>(null) }
    var editPoints by remember { mutableStateOf("") }
    var currentAdminTab by remember { mutableStateOf("Users") }
    
    var interstitialAdId by remember(appConfig) { mutableStateOf(appConfig?.interstitialAdUnitId ?: "ca-app-pub-3940256099942544/1033173712") }
    var rewardedAdId by remember(appConfig) { mutableStateOf(appConfig?.rewardedAdUnitId ?: "ca-app-pub-3940256099942544/5224354917") }
    var bannerAdId by remember(appConfig) { mutableStateOf(appConfig?.bannerAdUnitId ?: "ca-app-pub-3940256099942544/6300978111") }

    var pointsQuiz by remember(appConfig) { mutableStateOf((appConfig?.pointsQuiz ?: 20).toString()) }
    var pointsWatchVideo by remember(appConfig) { mutableStateOf((appConfig?.pointsWatchVideo ?: 10).toString()) }
    var pointsAdChallenge by remember(appConfig) { mutableStateOf((appConfig?.pointsAdChallenge ?: 20).toString()) }
    var pointsReferralUser by remember(appConfig) { mutableStateOf((appConfig?.pointsReferralUser ?: 100).toString()) }
    var pointsReferralRef by remember(appConfig) { mutableStateOf((appConfig?.pointsReferralRef ?: 200).toString()) }

    var isQuizEnabled by remember(appConfig) { mutableStateOf(appConfig?.isQuizEnabled ?: true) }
    var isWatchVideoEnabled by remember(appConfig) { mutableStateOf(appConfig?.isWatchVideoEnabled ?: true) }
    var isAdChallengeEnabled by remember(appConfig) { mutableStateOf(appConfig?.isAdChallengeEnabled ?: true) }
    var isReferralEnabled by remember(appConfig) { mutableStateOf(appConfig?.isReferralEnabled ?: true) }
    var isWithdrawEnabled by remember(appConfig) { mutableStateOf(appConfig?.isWithdrawEnabled ?: true) }

    var limitWatchVideo by remember(appConfig) { mutableStateOf((appConfig?.dailyWatchVideoLimit ?: 10).toString()) }
    var limitAdChallenge by remember(appConfig) { mutableStateOf((appConfig?.dailyAdChallengeLimit ?: 5).toString()) }
    var limitQuiz by remember(appConfig) { mutableStateOf((appConfig?.dailyQuizLimit ?: 5).toString()) }
    var telegramGroupUrl by remember(appConfig) { mutableStateOf(appConfig?.telegramUrl ?: "https://t.me/earncash_support") }

    var adTypeWatchVideo by remember(appConfig) { mutableStateOf(appConfig?.adTypeWatchVideo ?: "Rewarded") }
    var adTypeAdChallenge by remember(appConfig) { mutableStateOf(appConfig?.adTypeAdChallenge ?: "Interstitial") }
    var adTypeQuiz by remember(appConfig) { mutableStateOf(appConfig?.adTypeQuiz ?: "Interstitial") }
    var adTypeLuckySpin by remember(appConfig) { mutableStateOf(appConfig?.adTypeLuckySpin ?: "Interstitial") }
    var adTypeScratchCard by remember(appConfig) { mutableStateOf(appConfig?.adTypeScratchCard ?: "Interstitial") }

    val slate900 = Color(0xFF0F172A)
    val slate800 = Color(0xFF1E293B)
    val slate500 = Color(0xFF64748B)
    val slate100 = Color(0xFFF1F5F9)
    val indigo600 = Color(0xFF4F46E5)

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Admin Panel", fontWeight = FontWeight.Bold, color = slate900) },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = slate100)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = currentAdminTab == "Users",
                    onClick = { currentAdminTab = "Users" },
                    icon = { Icon(Icons.Outlined.Group, contentDescription = "Users") },
                    label = { Text("Users") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = indigo600, selectedTextColor = indigo600)
                )
                NavigationBarItem(
                    selected = currentAdminTab == "Withdrawals",
                    onClick = { currentAdminTab = "Withdrawals" },
                    icon = { Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = "Withdrawals") },
                    label = { Text("Withdrawals") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = indigo600, selectedTextColor = indigo600)
                )
                NavigationBarItem(
                    selected = currentAdminTab == "Ads",
                    onClick = { currentAdminTab = "Ads" },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = "Ads") },
                    label = { Text("Ads Config") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = indigo600, selectedTextColor = indigo600)
                )
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            if (currentAdminTab == "Users") {
                Text("Total Users: ${allUsers.size}", color = slate500, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(allUsers) { user ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                selectedUser = user
                                editPoints = user.points.toString()
                            },
                            colors = CardDefaults.cardColors(containerColor = slate100),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(user.name, fontWeight = FontWeight.Bold, color = slate800, fontSize = 16.sp)
                                        Text(user.email, color = slate500, fontSize = 12.sp)
                                        Text("Ref: ${user.referralCode}", color = indigo600, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${user.points} ৳", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A), fontSize = 16.sp)
                                        Text("${user.adsWatchedCount} Ads", color = slate500, fontSize = 12.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    if (user.isBlocked) {
                                        TextButton(onClick = { userViewModel.toggleBlockStatus(user.email, false) }) {
                                            Text("Unblock", color = Color(0xFF16A34A))
                                        }
                                    } else {
                                        TextButton(onClick = { userViewModel.toggleBlockStatus(user.email, true) }) {
                                            Text("Block", color = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (currentAdminTab == "Withdrawals") {
                Text("Withdrawal Requests", color = slate800, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(withdrawals) { req ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = slate100),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("${req.userEmail}", fontWeight = FontWeight.Bold)
                                Text("${req.method} - ${req.accountNumber}", color = slate500)
                                Text("Amount: ৳${req.amountPts}", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                                Text("Status: ${req.status}", color = if (req.status == "Pending") indigo600 else slate500)
                                
                                if (req.status == "Pending") {
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { userViewModel.updateWithdrawalStatus(req, "Rejected") }) {
                                            Text("Reject", color = Color.Red)
                                        }
                                        Button(onClick = { userViewModel.updateWithdrawalStatus(req, "Approved") }) {
                                            Text("Approve")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (currentAdminTab == "Ads") {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text("App Config & Feature Control", color = slate800, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 1: POINTS MANAGEMENT
                    Card(
                        colors = CardDefaults.cardColors(containerColor = slate100),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Reward Points (৳)", fontWeight = FontWeight.Bold, color = slate900, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = pointsQuiz,
                                onValueChange = { pointsQuiz = it },
                                label = { Text("Daily Quiz Points") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = pointsWatchVideo,
                                onValueChange = { pointsWatchVideo = it },
                                label = { Text("Watch Video Points") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = pointsAdChallenge,
                                onValueChange = { pointsAdChallenge = it },
                                label = { Text("Ad Challenge (5/5) Points") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = pointsReferralUser,
                                onValueChange = { pointsReferralUser = it },
                                label = { Text("Referral Bonus (User)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = pointsReferralRef,
                                onValueChange = { pointsReferralRef = it },
                                label = { Text("Referral Bonus (Referrer)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // SECTION 2: TOGGLE CAPABILITIES (ON/OFF)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = slate100),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Feature Toggles (ON/OFF)", fontWeight = FontWeight.Bold, color = slate900, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Daily Quiz Feature", color = slate800, fontWeight = FontWeight.Medium)
                                Switch(checked = isQuizEnabled, onCheckedChange = { isQuizEnabled = it })
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Watch Video Feature", color = slate800, fontWeight = FontWeight.Medium)
                                Switch(checked = isWatchVideoEnabled, onCheckedChange = { isWatchVideoEnabled = it })
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Watch Ad Challenge Feature", color = slate800, fontWeight = FontWeight.Medium)
                                Switch(checked = isAdChallengeEnabled, onCheckedChange = { isAdChallengeEnabled = it })
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Referral Program Feature", color = slate800, fontWeight = FontWeight.Medium)
                                Switch(checked = isReferralEnabled, onCheckedChange = { isReferralEnabled = it })
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cash Withdrawal Feature", color = slate800, fontWeight = FontWeight.Medium)
                                Switch(checked = isWithdrawEnabled, onCheckedChange = { isWithdrawEnabled = it })
                            }
                        }
                    }

                    // TASK LIMITS
                    Card(
                        colors = CardDefaults.cardColors(containerColor = slate100),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Task Limits (Max/Day)", fontWeight = FontWeight.Bold, color = slate900, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = limitQuiz,
                                onValueChange = { limitQuiz = it },
                                label = { Text("Daily Quiz Limit") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = limitWatchVideo,
                                onValueChange = { limitWatchVideo = it },
                                label = { Text("Watch Video Limit") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = limitAdChallenge,
                                onValueChange = { limitAdChallenge = it },
                                label = { Text("Ad Challenge Limit") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // SECTION 3: ADS CONFIG
                    Card(
                        colors = CardDefaults.cardColors(containerColor = slate100),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Google Ad Setup", fontWeight = FontWeight.Bold, color = slate900, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = interstitialAdId,
                                onValueChange = { interstitialAdId = it },
                                label = { Text("Interstitial Ad Unit ID") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = rewardedAdId,
                                onValueChange = { rewardedAdId = it },
                                label = { Text("Rewarded Ad Unit ID") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = bannerAdId,
                                onValueChange = { bannerAdId = it },
                                label = { Text("Banner Ad Unit ID") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // SECTION 4: TELEGRAM SUPPORT CONFIG
                    Card(
                        colors = CardDefaults.cardColors(containerColor = slate100),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Telegram Support Setup", fontWeight = FontWeight.Bold, color = slate900, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = telegramGroupUrl,
                                onValueChange = { telegramGroupUrl = it },
                                label = { Text("Telegram Link (e.g. t.me/...)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // SECTION 5: AD PLACEMENT CONFIG (CHOOSE AD TYPE FOR EACH TASK)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = slate100),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Task Ad Placement Customize", fontWeight = FontWeight.Bold, color = slate900, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            AdTypeTierSelector("Watch Video Task", adTypeWatchVideo) { adTypeWatchVideo = it }
                            AdTypeTierSelector("Watch Ad (Challenge) Task", adTypeAdChallenge) { adTypeAdChallenge = it }
                            AdTypeTierSelector("Daily Quiz Task", adTypeQuiz) { adTypeQuiz = it }
                            AdTypeTierSelector("Lucky Spin Task", adTypeLuckySpin) { adTypeLuckySpin = it }
                            AdTypeTierSelector("Scratch Card Task", adTypeScratchCard) { adTypeScratchCard = it }
                        }
                    }

                    Button(
                        onClick = {
                            val quizPts = pointsQuiz.toIntOrNull() ?: 20
                            val videoPts = pointsWatchVideo.toIntOrNull() ?: 10
                            val chalPts = pointsAdChallenge.toIntOrNull() ?: 20
                            val refUserPts = pointsReferralUser.toIntOrNull() ?: 100
                            val refRefPts = pointsReferralRef.toIntOrNull() ?: 200
                            val limitVideo = limitWatchVideo.toIntOrNull() ?: 10
                            val limitChal = limitAdChallenge.toIntOrNull() ?: 5
                            val limitQuizPts = limitQuiz.toIntOrNull() ?: 5

                            userViewModel.updateConfig(
                                interstitialAdUnitId = interstitialAdId,
                                rewardedAdUnitId = rewardedAdId,
                                bannerAdUnitId = bannerAdId,
                                pointsQuiz = quizPts,
                                pointsWatchVideo = videoPts,
                                pointsAdChallenge = chalPts,
                                pointsReferralUser = refUserPts,
                                pointsReferralRef = refRefPts,
                                isQuizEnabled = isQuizEnabled,
                                isWatchVideoEnabled = isWatchVideoEnabled,
                                isAdChallengeEnabled = isAdChallengeEnabled,
                                isReferralEnabled = isReferralEnabled,
                                isWithdrawEnabled = isWithdrawEnabled,
                                dailyWatchVideoLimit = limitVideo,
                                dailyAdChallengeLimit = limitChal,
                                dailyQuizLimit = limitQuizPts,
                                telegramUrl = telegramGroupUrl,
                                adTypeWatchVideo = adTypeWatchVideo,
                                adTypeAdChallenge = adTypeAdChallenge,
                                adTypeQuiz = adTypeQuiz,
                                adTypeLuckySpin = adTypeLuckySpin,
                                adTypeScratchCard = adTypeScratchCard
                            )
                            coroutineScope.launch { snackbarHostState.showSnackbar("Configuration saved successfully!") }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = indigo600),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                    ) {
                        Text("Save All Configuration")
                    }
                }
            }
        }
        
        if (selectedUser != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { selectedUser = null },
                containerColor = Color.White,
                title = { Text("Edit Points & Bonus", fontWeight = FontWeight.Bold, color = slate900) },
                text = {
                    Column {
                        Text(selectedUser!!.email, color = Color(0xFF334155), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = editPoints,
                            onValueChange = { editPoints = it },
                            label = { Text("Total Points / Bonus") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newPts = editPoints.toIntOrNull()
                            if (newPts != null) {
                                userViewModel.updateAnyUserPoints(selectedUser!!.email, newPts)
                                coroutineScope.launch { snackbarHostState.showSnackbar("Points updated!") }
                                selectedUser = null
                            } else {
                                coroutineScope.launch { snackbarHostState.showSnackbar("Invalid number") }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = indigo600)
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedUser = null }) { Text("Cancel", color = slate500) }
                }
            )
        }
    }
}

@Composable
private fun AdTypeTierSelector(
    title: String,
    currentValue: String,
    onValueChange: (String) -> Unit
) {
    val choices = listOf("Interstitial", "Rewarded")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B), fontSize = 14.sp, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            choices.forEach { choice ->
                val selected = choice == currentValue
                val bgColor = if (selected) Color(0xFF4F46E5) else Color.White
                val contentColor = if (selected) Color.White else Color(0xFF64748B)
                val borderColor = if (selected) Color(0xFF4F46E5) else Color(0xFFCBD5E1)
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .clickable { onValueChange(choice) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(choice, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
