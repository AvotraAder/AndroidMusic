package com.example.myapplication.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.PlaybackHistory
import com.example.myapplication.data.TopMedia
import com.example.myapplication.data.TopArtist
import com.example.myapplication.data.UserDao
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: com.example.myapplication.data.User,
    isDarkMode: Boolean,
    currentLanguage: String,
    onThemeToggle: () -> Unit,
    onLanguageToggle: () -> Unit,
    onLogout: () -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    userDao: UserDao,
    onMediaPlayed: (MediaItem) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val t = if (currentLanguage == "FR") {
        mapOf(
            "home" to "Statistiques",
            "profile" to "Modifier Profil",
            "music" to "Musique",
            "help" to "Aide",
            "contact" to "Contact",
            "logout" to "Déconnexion",
            "welcome" to "Bienvenue,",
            "stats_title" to "Statistiques d'écoute",
            "top_music_chart" to "Top 5 Musiques",
            "recent_music" to "Écouté récemment",
            "total_time" to "Temps total",
            "edit_profile" to "Modifier votre profil",
            "username" to "Nom d'utilisateur",
            "note_username" to "Note: Le nom d'utilisateur ne peut pas être modifié.",
            "new_password" to "Nouveau mot de passe",
            "save" to "Sauvegarder les modifications",
            "help_content" to "Besoin d'aide ? Consultez notre documentation ou contactez le support.",
            "contact_content" to "Email: andriniainaavotraader@gmail.com\nTéléphone: +261 33 79 210 96\nGitHub: https://github.com/AvotraAder"
        )
    } else {
        mapOf(
            "home" to "Statistics",
            "profile" to "Edit Profile",
            "music" to "Music",
            "help" to "Help",
            "contact" to "Contact",
            "logout" to "Logout",
            "welcome" to "Welcome,",
            "stats_title" to "Listening Stats",
            "top_music_chart" to "Top 5 Music",
            "recent_music" to "Recently Played",
            "total_time" to "Total Time",
            "edit_profile" to "Edit your profile",
            "username" to "Username",
            "note_username" to "Note: Username cannot be changed.",
            "new_password" to "New password",
            "save" to "Save changes",
            "help_content" to "Need help? Check our documentation or contact support.",
            "contact_content" to "Email: andriniainaavotraader@gmail.com\nPhone: +261 33 79 210 96\nGitHub: https://github.com/AvotraAder"
        )
    }

    var currentMenuKey by remember { mutableStateOf("music") }
    val currentTitle = t[currentMenuKey] ?: ""

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Menu",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()
                
                NavigationDrawerItem(
                    label = { Text(t["music"]!!) },
                    selected = currentMenuKey == "music",
                    onClick = { currentMenuKey = "music"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text(t["home"]!!) },
                    selected = currentMenuKey == "home",
                    onClick = { currentMenuKey = "home"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text(t["profile"]!!) },
                    selected = currentMenuKey == "profile",
                    onClick = { currentMenuKey = "profile"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text(t["help"]!!) },
                    selected = currentMenuKey == "help",
                    onClick = { currentMenuKey = "help"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text(t["contact"]!!) },
                    selected = currentMenuKey == "contact",
                    onClick = { currentMenuKey = "contact"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.ContactSupport, contentDescription = null) }
                )
                
                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text(t["logout"]!!) },
                    selected = false,
                    onClick = onLogout,
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentTitle) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onLanguageToggle, modifier = Modifier.width(64.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = currentLanguage, style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(imageVector = Icons.Default.Language, contentDescription = "Language")
                            }
                        }
                        IconButton(onClick = onThemeToggle) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Theme"
                            )
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (currentMenuKey) {
                    "home" -> HomeView(user.username, t, userDao)
                    "profile" -> ProfileEditView(user.username, t, onUpdate = { newName, newPwd -> 
                        onUpdateProfile(newName, newPwd)
                    })
                    "music" -> MusicPlayerView(currentLanguage, user.username, onMediaPlayed)
                    "help" -> InfoView(t["help"]!!, t["help_content"]!!)
                    "contact" -> InfoView(t["contact"]!!, t["contact_content"]!!)
                }
            }
        }
    }
}

@Composable
fun HomeView(username: String, t: Map<String, String>, userDao: UserDao) {
    var topMedias by remember { mutableStateOf<List<TopMedia>>(emptyList()) }
    var topArtists by remember { mutableStateOf<List<TopArtist>>(emptyList()) }
    var recentHistory by remember { mutableStateOf<List<PlaybackHistory>>(emptyList()) }
    var totalTime by remember { mutableLongStateOf(0L) }
    var playsToday by remember { mutableIntStateOf(0) }

    LaunchedEffect(username) {
        topMedias = userDao.getTopMedias(username)
        topArtists = userDao.getTopArtists(username)
        recentHistory = userDao.getRecentHistory(username)
        totalTime = userDao.getTotalListenTime(username) ?: 0L
        
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        playsToday = userDao.getPlaysToday(username, cal.timeInMillis)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Hero Section: Personalized Greeting
        Text(
            text = "${t["welcome"]!!} $username",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (t["home"] == "Statistiques") "Voici votre résumé musical" else "Here's your musical summary",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Metrics Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = t["total_time"]!!,
                value = formatDurationShort(totalTime),
                icon = Icons.Default.Timer,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = if (t["home"] == "Statistiques") "Écoutés aujourd'hui" else "Played Today",
                value = playsToday.toString(),
                icon = Icons.Default.Today,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Chart Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = t["stats_title"]!!,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                // Top Music Bar Chart
                Text(text = t["top_music_chart"]!!, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(12.dp))
                if (topMedias.isEmpty()) {
                    Text("---", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    topMedias.forEach { media ->
                        val max = topMedias.first().count.toFloat().coerceAtLeast(1f)
                        StatBar(
                            label = media.mediaTitle, 
                            value = media.count.toString(), 
                            progress = media.count / max, 
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Top Artists
                Text(
                    text = if (t["home"] == "Statistiques") "Top 5 Artistes" else "Top 5 Artists", 
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (topArtists.isEmpty()) {
                    Text("---", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    topArtists.forEach { artist ->
                        val max = topArtists.first().count.toFloat().coerceAtLeast(1f)
                        StatBar(
                            label = artist.artist, 
                            value = artist.count.toString(), 
                            progress = artist.count / max, 
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Activity
        Text(text = t["recent_music"]!!, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (recentHistory.isEmpty()) {
            Text(text = "---", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            recentHistory.forEach { history ->
                Card(
                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    ListItem(
                        headlineContent = { Text(history.mediaTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("${history.artist} • ${history.album}") },
                        leadingContent = { 
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    Icons.Default.History, 
                                    contentDescription = null, 
                                    modifier = Modifier.padding(8.dp).size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                ) 
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp)) // Padding for bottom player
    }
}

@Composable
fun MetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatBar(label: String, value: String, progress: Float, color: Color = MaterialTheme.colorScheme.primary) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000))
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(2.dp) // Thin line
            .background(Color.LightGray.copy(alpha = 0.2f))
        ) {
            Box(modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .background(color)
            )
        }
    }
}

fun formatDurationShort(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val hours = minutes / 60
    return if (hours > 0) "${hours}h ${minutes % 60}m" else "${minutes}m"
}

@Composable
fun ProfileEditView(currentUsername: String, t: Map<String, String>, onUpdate: (String, String) -> Unit) {
    var uName by remember { mutableStateOf(currentUsername) }
    var pwd by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(t["edit_profile"]!!, style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(value = uName, onValueChange = { uName = it }, label = { Text(t["username"]!!) }, modifier = Modifier.fillMaxWidth(), enabled = false)
        Text(t["note_username"]!!, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        OutlinedTextField(value = pwd, onValueChange = { pwd = it }, label = { Text(t["new_password"]!!) }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
        Button(onClick = { onUpdate(uName, pwd) }, modifier = Modifier.fillMaxWidth()) {
            Text(t["save"]!!)
        }
    }
}

@Composable
fun InfoView(title: String, content: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(content)
    }
}
