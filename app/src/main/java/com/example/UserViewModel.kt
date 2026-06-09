package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.TransactionHistory
import com.example.data.User
import com.example.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: UserRepository
    
    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail = _currentUserEmail.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    val currentUserTransactions: StateFlow<List<TransactionHistory>> = _currentUserEmail.flatMapLatest { email ->
        if (email != null) {
            repository.getTransactionHistoryByEmail(email)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    lateinit var allUsers: StateFlow<List<User>>
        private set

    lateinit var withdrawalRequests: StateFlow<List<com.example.data.WithdrawalRequest>>
        private set

    lateinit var appConfig: StateFlow<com.example.data.AppConfig?>
        private set

    init {
        val userDao = AppDatabase.getDatabase(application).userDao()
        repository = UserRepository(userDao)
        
        allUsers = repository.getAllUsers().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        withdrawalRequests = repository.getAllWithdrawalRequests().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        appConfig = repository.getAppConfig().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        // Seed default configuration if empty
        viewModelScope.launch {
            try {
                val config = repository.getAppConfig().firstOrNull()
                if (config == null) {
                    repository.insertAppConfig(com.example.data.AppConfig())
                }
            } catch (e: Exception) {
                // Ignore initial seed error if any
                e.printStackTrace()
            }
        }

        viewModelScope.launch {
            try {
                _currentUserEmail.collectLatest { email ->
                    if (email != null) {
                        repository.getUser(email).collectLatest { user ->
                            if (user != null) {
                                val todayStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
                                if (user.lastLimitResetDate != todayStr) {
                                    val resetUser = user.copy(
                                        watchVideoCount = 0,
                                        dailyQuizCount = 0,
                                        adsWatchedCount = 0,
                                        dailySpinCount = 0,
                                        dailyScratchCount = 0,
                                        lastLimitResetDate = todayStr
                                    )
                                    repository.insertUser(resetUser)
                                    _currentUser.value = resetUser
                                } else {
                                    _currentUser.value = user
                                }
                            } else {
                                _currentUser.value = null
                            }
                        }
                    } else {
                        _currentUser.value = null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val user = repository.getUser(email).firstOrNull()
                if (email == "admin@admin.com") {
                    if (password == "123456" || password == "admin" || password == "admin123") {
                        _currentUserEmail.value = email
                        onResult(true, "Success")
                    } else {
                        onResult(false, "Incorrect admin password")
                    }
                } else if (user == null) {
                    onResult(false, "No account found for this email")
                } else {
                    if (user.password.isEmpty()) {
                        repository.insertUser(user.copy(password = password))
                        _currentUserEmail.value = email
                        onResult(true, "Success")
                    } else if (user.password == password) {
                        _currentUserEmail.value = email
                        onResult(true, "Success")
                    } else {
                        onResult(false, "Incorrect password")
                    }
                }
            } catch (e: Exception) {
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    fun signUp(name: String, email: String, password: String, language: String = "English", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val existing = repository.getUser(email).firstOrNull()
                if (existing != null) {
                    onResult(false, "Email already registered")
                    return@launch
                }
                val referralCode = generateReferralCode()
                repository.insertUser(User(email = email, name = name, password = password, referralCode = referralCode, language = language))
                _currentUserEmail.value = email
                onResult(true, "Success")
            } catch (e: Exception) {
                onResult(false, "Error: ${e.message}")
            }
        }
    }
    
    private fun generateReferralCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars.random() }.joinToString("")
    }

    suspend fun applyReferralCode(code: String): Boolean {
        val user = _currentUser.value ?: return false
        if (user.referredBy != null) return false // Already referred
        if (user.referralCode == code) return false // Self referral

        val referrer = repository.getUserByReferralCode(code) ?: return false
        
        val config = appConfig.value
        val userBonus = config?.pointsReferralUser ?: 100
        val refBonus = config?.pointsReferralRef ?: 200
        
        // Give bonuses using dynamic values from config
        repository.updateReferredBy(user.email, referrer.email)
        repository.updatePoints(user.email, user.points + userBonus)
        repository.insertTransactionHistory(TransactionHistory(userEmail = user.email, title = "Referral Bonus", amountStr = "+৳$userBonus", isPositive = true))

        repository.updatePoints(referrer.email, referrer.points + refBonus)
        repository.insertTransactionHistory(TransactionHistory(userEmail = referrer.email, title = "Referred a Friend", amountStr = "+৳$refBonus", isPositive = true))
        
        return true
    }
    
    fun logout() {
        _currentUserEmail.value = null
    }

    fun addPoints(points: Int, reason: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.addPoints(user.email, points)
            repository.insertTransactionHistory(TransactionHistory(userEmail = user.email, title = reason, amountStr = "+৳$points", isPositive = true))
        }
    }

    fun checkInUser(earnedPoints: Int, newStreak: Int, todayStr: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updatedUser = user.copy(
                points = user.points + earnedPoints,
                lastCheckInDate = todayStr,
                dailyCheckInStreak = newStreak
            )
            repository.insertUser(updatedUser)
            repository.insertTransactionHistory(TransactionHistory(
                userEmail = user.email,
                title = "Daily Check-In (Day $newStreak)",
                amountStr = "+৳$earnedPoints",
                isPositive = true
            ))
        }
    }

    fun updateSpinCount(count: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateSpinCount(user.email, count)
        }
    }

    fun updateScratchCount(count: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateScratchCount(user.email, count)
        }
    }

    fun updateAnyUserPoints(email: String, newPoints: Int) {
        viewModelScope.launch {
            repository.updatePoints(email, newPoints)
        }
    }

    fun updateAdsWatchedCount(count: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateAdsWatchedCount(user.email, count)
        }
    }

    fun updateWatchVideoCount(count: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateWatchVideoCount(user.email, count)
        }
    }

    fun updateQuizCount(count: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updateQuizCount(user.email, count)
        }
    }

    fun updateUserSettings(language: String, country: String, isDarkMode: Boolean, notificationsEnabled: Boolean) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updatedUser = user.copy(
                language = language,
                country = country,
                isDarkMode = isDarkMode,
                notificationsEnabled = notificationsEnabled
            )
            repository.insertUser(updatedUser)
        }
    }

    fun updateUserProfile(name: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updatedUser = user.copy(name = name)
            repository.insertUser(updatedUser)
        }
    }

    fun submitWithdrawalRequest(method: String, accountNumber: String, amountPts: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            if (user.points >= amountPts) {
                repository.updatePoints(user.email, user.points - amountPts)
                repository.insertWithdrawalRequest(com.example.data.WithdrawalRequest(
                    userEmail = user.email,
                    method = method,
                    accountNumber = accountNumber,
                    amountPts = amountPts
                ))
                repository.insertTransactionHistory(TransactionHistory(userEmail = user.email, title = "Withdrawal ($method)", amountStr = "-৳$amountPts", isPositive = false))
            }
        }
    }

    fun updateWithdrawalStatus(request: com.example.data.WithdrawalRequest, status: String) {
        viewModelScope.launch {
            repository.updateWithdrawalStatus(request.id, status)
            if (status == "Rejected") {
                val user = repository.getUser(request.userEmail).firstOrNull()
                if (user != null) {
                    repository.updatePoints(user.email, user.points + request.amountPts)
                    repository.insertTransactionHistory(TransactionHistory(
                        userEmail = user.email, title = "Withdrawal Refund (${request.method})", amountStr = "+৳${request.amountPts}", isPositive = true
                    ))
                }
            }
        }
    }

    fun updateConfig(
        interstitialAdUnitId: String,
        rewardedAdUnitId: String,
        bannerAdUnitId: String,
        pointsQuiz: Int,
        pointsWatchVideo: Int,
        pointsAdChallenge: Int,
        pointsReferralUser: Int,
        pointsReferralRef: Int,
        isQuizEnabled: Boolean,
        isWatchVideoEnabled: Boolean,
        isAdChallengeEnabled: Boolean,
        isReferralEnabled: Boolean,
        isWithdrawEnabled: Boolean,
        dailyWatchVideoLimit: Int,
        dailyAdChallengeLimit: Int,
        dailyQuizLimit: Int,
        telegramUrl: String,
        adTypeWatchVideo: String,
        adTypeAdChallenge: String,
        adTypeQuiz: String,
        adTypeLuckySpin: String,
        adTypeScratchCard: String
    ) {
        viewModelScope.launch {
            repository.insertAppConfig(com.example.data.AppConfig(
                id = 1,
                interstitialAdUnitId = interstitialAdUnitId,
                rewardedAdUnitId = rewardedAdUnitId,
                bannerAdUnitId = bannerAdUnitId,
                pointsQuiz = pointsQuiz,
                pointsWatchVideo = pointsWatchVideo,
                pointsAdChallenge = pointsAdChallenge,
                pointsReferralUser = pointsReferralUser,
                pointsReferralRef = pointsReferralRef,
                isQuizEnabled = isQuizEnabled,
                isWatchVideoEnabled = isWatchVideoEnabled,
                isAdChallengeEnabled = isAdChallengeEnabled,
                isReferralEnabled = isReferralEnabled,
                isWithdrawEnabled = isWithdrawEnabled,
                dailyWatchVideoLimit = dailyWatchVideoLimit,
                dailyAdChallengeLimit = dailyAdChallengeLimit,
                dailyQuizLimit = dailyQuizLimit,
                telegramUrl = telegramUrl,
                adTypeWatchVideo = adTypeWatchVideo,
                adTypeAdChallenge = adTypeAdChallenge,
                adTypeQuiz = adTypeQuiz,
                adTypeLuckySpin = adTypeLuckySpin,
                adTypeScratchCard = adTypeScratchCard
            ))
        }
    }

    fun toggleBlockStatus(email: String, isBlocked: Boolean) {
        viewModelScope.launch {
            repository.updateBlockedStatus(email, isBlocked)
        }
    }
}
