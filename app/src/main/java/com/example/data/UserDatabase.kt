package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val email: String,
    val name: String,
    val password: String = "",
    val points: Int = 0,
    val adsWatchedCount: Int = 0,
    val watchVideoCount: Int = 0,
    val dailyQuizCount: Int = 0,
    val referralCode: String = "",
    val referredBy: String? = null,
    val isBlocked: Boolean = false,
    val language: String = "English",
    val country: String = "Bangladesh",
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val lastCheckInDate: String = "",
    val dailyCheckInStreak: Int = 0,
    val dailySpinCount: Int = 0,
    val dailyScratchCount: Int = 0,
    val lastLimitResetDate: String = ""
)

@Entity(tableName = "withdrawal_requests")
data class WithdrawalRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val method: String,
    val accountNumber: String,
    val amountPts: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Pending" // Pending, Approved, Rejected
)

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val id: Int = 1,
    val interstitialAdUnitId: String = "ca-app-pub-9642029536118170/9916554475",
    val rewardedAdUnitId: String = "ca-app-pub-9642029536118170/9916554475",
    val bannerAdUnitId: String = "ca-app-pub-9642029536118170/9916554475",
    val pointsQuiz: Int = 20,
    val pointsWatchVideo: Int = 10,
    val pointsAdChallenge: Int = 20,
    val pointsReferralUser: Int = 100,
    val pointsReferralRef: Int = 200,
    val isQuizEnabled: Boolean = true,
    val isWatchVideoEnabled: Boolean = true,
    val isAdChallengeEnabled: Boolean = true,
    val isReferralEnabled: Boolean = true,
    val isWithdrawEnabled: Boolean = true,
    val dailyWatchVideoLimit: Int = 10,
    val dailyAdChallengeLimit: Int = 5,
    val dailyQuizLimit: Int = 5,
    val telegramUrl: String = "https://t.me/earncash_support",
    val adTypeWatchVideo: String = "Rewarded",
    val adTypeAdChallenge: String = "Interstitial",
    val adTypeQuiz: String = "Interstitial",
    val adTypeLuckySpin: String = "Interstitial",
    val adTypeScratchCard: String = "Interstitial"
)

@Entity(tableName = "transaction_history")
data class TransactionHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val title: String,
    val amountStr: String,
    val isPositive: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun getUserByEmail(email: String): Flow<User?>

    @Query("SELECT * FROM users WHERE referralCode = :code LIMIT 1")
    suspend fun getUserByReferralCode(code: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("UPDATE users SET points = :points WHERE email = :email")
    suspend fun updatePoints(email: String, points: Int)

    @Query("UPDATE users SET points = points + :pointsToAdd WHERE email = :email")
    suspend fun addPoints(email: String, pointsToAdd: Int)

    @Query("UPDATE users SET adsWatchedCount = :count WHERE email = :email")
    suspend fun updateAdsWatchedCount(email: String, count: Int)

    @Query("UPDATE users SET watchVideoCount = :count WHERE email = :email")
    suspend fun updateWatchVideoCount(email: String, count: Int)

    @Query("UPDATE users SET dailyQuizCount = :count WHERE email = :email")
    suspend fun updateQuizCount(email: String, count: Int)

    @Query("UPDATE users SET dailySpinCount = :count WHERE email = :email")
    suspend fun updateSpinCount(email: String, count: Int)

    @Query("UPDATE users SET dailyScratchCount = :count WHERE email = :email")
    suspend fun updateScratchCount(email: String, count: Int)

    @Query("UPDATE users SET referredBy = :referrerEmail WHERE email = :email")
    suspend fun updateReferredBy(email: String, referrerEmail: String)

    @Query("UPDATE users SET isBlocked = :isBlocked WHERE email = :email")
    suspend fun updateBlockedStatus(email: String, isBlocked: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawalRequest(request: WithdrawalRequest)

    @Query("SELECT * FROM withdrawal_requests ORDER BY timestamp DESC")
    fun getAllWithdrawalRequests(): Flow<List<WithdrawalRequest>>

    @Query("UPDATE withdrawal_requests SET status = :status WHERE id = :id")
    suspend fun updateWithdrawalStatus(id: Int, status: String)

    @Query("SELECT * FROM app_config WHERE id = 1 LIMIT 1")
    fun getAppConfig(): Flow<AppConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppConfig(config: AppConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionHistory(transactionHistory: TransactionHistory)

    @Query("SELECT * FROM transaction_history WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getTransactionHistoryByEmail(email: String): Flow<List<TransactionHistory>>
}

class UserRepository(private val userDao: UserDao) {
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    fun getUser(email: String): Flow<User?> = userDao.getUserByEmail(email)

    suspend fun getUserByReferralCode(code: String): User? = userDao.getUserByReferralCode(code)

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun updatePoints(email: String, points: Int) {
        userDao.updatePoints(email, points)
    }

    suspend fun addPoints(email: String, pointsToAdd: Int) {
        userDao.addPoints(email, pointsToAdd)
    }

    suspend fun updateAdsWatchedCount(email: String, count: Int) {
        userDao.updateAdsWatchedCount(email, count)
    }

    suspend fun updateWatchVideoCount(email: String, count: Int) {
        userDao.updateWatchVideoCount(email, count)
    }

    suspend fun updateQuizCount(email: String, count: Int) {
        userDao.updateQuizCount(email, count)
    }

    suspend fun updateSpinCount(email: String, count: Int) {
        userDao.updateSpinCount(email, count)
    }

    suspend fun updateScratchCount(email: String, count: Int) {
        userDao.updateScratchCount(email, count)
    }

    suspend fun updateReferredBy(email: String, referrerEmail: String) {
        userDao.updateReferredBy(email, referrerEmail)
    }

    suspend fun updateBlockedStatus(email: String, isBlocked: Boolean) {
        userDao.updateBlockedStatus(email, isBlocked)
    }

    suspend fun insertWithdrawalRequest(request: WithdrawalRequest) {
        userDao.insertWithdrawalRequest(request)
    }

    fun getAllWithdrawalRequests(): Flow<List<WithdrawalRequest>> = userDao.getAllWithdrawalRequests()

    suspend fun updateWithdrawalStatus(id: Int, status: String) {
        userDao.updateWithdrawalStatus(id, status)
    }

    fun getAppConfig(): Flow<AppConfig?> = userDao.getAppConfig()

    suspend fun insertAppConfig(config: AppConfig) {
        userDao.insertAppConfig(config)
    }

    suspend fun insertTransactionHistory(transactionHistory: TransactionHistory) {
        userDao.insertTransactionHistory(transactionHistory)
    }

    fun getTransactionHistoryByEmail(email: String): Flow<List<TransactionHistory>> = userDao.getTransactionHistoryByEmail(email)
}

@Database(entities = [User::class, WithdrawalRequest::class, AppConfig::class, TransactionHistory::class], version = 13, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reward_cash_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
