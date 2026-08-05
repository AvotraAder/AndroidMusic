package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.PlaybackHistory
import com.example.myapplication.data.User
import com.example.myapplication.ui.DashboardScreen
import com.example.myapplication.ui.LoginScreen
import com.example.myapplication.ui.RegisterScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val Context.dataStore by preferencesDataStore(name = "user_session")
val USERNAME_KEY = stringPreferencesKey("username")

enum class Screen { LOGIN, REGISTER, DASHBOARD }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(this)
        val userDao = db.userDao()

        setContent {
            var isDarkMode by remember { mutableStateOf(true) }
            var currentLanguage by remember { mutableStateOf("EN") }
            
            var currentUser by remember { mutableStateOf<User?>(null) }
            var currentScreen by remember { mutableStateOf(Screen.LOGIN) }
            var isLoading by remember { mutableStateOf(true) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                val savedUsername = dataStore.data.map { it[USERNAME_KEY] }.first()
                if (savedUsername != null) {
                    val user = withContext(Dispatchers.IO) {
                        userDao.getUserByUsername(savedUsername)
                    }
                    if (user != null) {
                        currentUser = user
                        currentScreen = Screen.DASHBOARD
                    }
                }
                isLoading = false
            }

            MyApplicationTheme(darkTheme = isDarkMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isLoading) {
                        // Empty while loading
                    } else {
                        when (currentScreen) {
                            Screen.DASHBOARD -> {
                                currentUser?.let { user ->
                                    var currentHistoryId by remember { mutableStateOf<Long?>(null) }
                                    DashboardScreen(
                                        user = user,
                                        isDarkMode = isDarkMode,
                                        currentLanguage = currentLanguage,
                                        onThemeToggle = { isDarkMode = !isDarkMode },
                                        onLanguageToggle = { currentLanguage = if (currentLanguage == "FR") "EN" else "FR" },
                                        onLogout = {
                                            scope.launch {
                                                // Stop playback service on logout
                                                stopService(android.content.Intent(this@MainActivity, com.example.myapplication.data.PlaybackService::class.java))
                                                dataStore.edit { it.remove(USERNAME_KEY) }
                                                currentUser = null
                                                currentScreen = Screen.LOGIN
                                            }
                                        },
                                        onUpdateProfile = { _, newPassword ->
                                            if (newPassword.isNotBlank()) {
                                                scope.launch(Dispatchers.IO) {
                                                    val updatedUser = user.copy(password = newPassword)
                                                    userDao.updateUser(updatedUser)
                                                    withContext(Dispatchers.Main) {
                                                        currentUser = updatedUser
                                                        val msg = if (currentLanguage == "FR") "Profil mis à jour !" else "Profile updated!"
                                                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                val msg = if (currentLanguage == "FR") "Le mot de passe ne peut pas être vide" else "Password cannot be empty"
                                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        userDao = userDao,
                                        onMediaStart = { media ->
                                            scope.launch(Dispatchers.IO) {
                                                val id = userDao.insertHistory(
                                                    PlaybackHistory(
                                                        username = user.username,
                                                        mediaTitle = media.title,
                                                        artist = media.artist,
                                                        album = media.album,
                                                        durationMs = 0
                                                    )
                                                )
                                                withContext(Dispatchers.Main) {
                                                    currentHistoryId = id
                                                }
                                            }
                                        },
                                        onMediaProgress = { _, delta ->
                                            currentHistoryId?.let { id ->
                                                scope.launch(Dispatchers.IO) {
                                                    userDao.incrementDuration(id, delta)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            Screen.LOGIN -> {
                                LoginScreen(
                                    isDarkMode = isDarkMode,
                                    currentLanguage = currentLanguage,
                                    onThemeToggle = { isDarkMode = !isDarkMode },
                                    onLanguageToggle = { currentLanguage = if (currentLanguage == "FR") "EN" else "FR" },
                                    onLogin = { username, password ->
                                        scope.launch(Dispatchers.IO) {
                                            val foundUser = userDao.getUserByUsername(username)
                                            withContext(Dispatchers.Main) {
                                                if (foundUser != null && foundUser.password == password) {
                                                    scope.launch {
                                                        dataStore.edit { it[USERNAME_KEY] = username }
                                                    }
                                                    currentUser = foundUser
                                                    currentScreen = Screen.DASHBOARD
                                                } else {
                                                    val msg = if (currentLanguage == "FR") "Identifiants incorrects" else "Invalid credentials"
                                                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    onGuestLogin = {
                                        scope.launch(Dispatchers.IO) {
                                            val guestUsername = "Guest"
                                            var guestUser = userDao.getUserByUsername(guestUsername)
                                            if (guestUser == null) {
                                                guestUser = User(guestUsername, "")
                                                userDao.insertUser(guestUser)
                                            }
                                            withContext(Dispatchers.Main) {
                                                scope.launch {
                                                    dataStore.edit { it[USERNAME_KEY] = guestUsername }
                                                }
                                                currentUser = guestUser
                                                currentScreen = Screen.DASHBOARD
                                            }
                                        }
                                    },
                                    onNavigateToRegister = { currentScreen = Screen.REGISTER }
                                )
                            }
                            Screen.REGISTER -> {
                                RegisterScreen(
                                    isDarkMode = isDarkMode,
                                    currentLanguage = currentLanguage,
                                    onThemeToggle = { isDarkMode = !isDarkMode },
                                    onLanguageToggle = { currentLanguage = if (currentLanguage == "FR") "EN" else "FR" },
                                    onRegister = { username, password ->
                                        if (username.isBlank() || password.isBlank()) {
                                            val msg = if (currentLanguage == "FR") "Veuillez remplir tous les champs" else "Please fill all fields"
                                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                        } else {
                                            scope.launch(Dispatchers.IO) {
                                                val existing = userDao.getUserByUsername(username)
                                                withContext(Dispatchers.Main) {
                                                    if (existing != null) {
                                                        val msg = if (currentLanguage == "FR") "Utilisateur déjà existant" else "User already exists"
                                                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        scope.launch(Dispatchers.IO) {
                                                            userDao.insertUser(User(username, password))
                                                            withContext(Dispatchers.Main) {
                                                                val msg = if (currentLanguage == "FR") "Compte créé !" else "Account created!"
                                                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                                                                currentScreen = Screen.LOGIN
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onBackToLogin = { currentScreen = Screen.LOGIN }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
