package com.wiantex.app.ui

import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.wiantex.app.data.*
import com.wiantex.app.ui.components.RemoteAvatar

private enum class MainTab(val title: String) {
    Forum("Forum"), Pulses("Pulses"), Messages("Mesajlar"), Market("Market"), Profile("Profil"), Notifications("Bildirimler")
}

@Composable
fun WiantexApp(vm: WiantexViewModel) {
    when {
        !vm.authChecked -> SplashScreen()
        vm.user == null -> LoginScreen(vm)
        else -> MainShell(vm)
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            WiantexMark()
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun LoginScreen(vm: WiantexViewModel) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current

    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.widthIn(max = 440.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                WiantexMark()
                Text("Wiantex", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Topluluğuna native Android uygulamasından bağlan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = login,
                    onValueChange = { login = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Kullanıcı adı veya e-posta") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Şifre") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focus.clearFocus()
                        if (login.isNotBlank() && password.isNotBlank()) vm.login(login, password)
                    }),
                )
                Button(
                    onClick = { focus.clearFocus(); vm.login(login, password) },
                    enabled = !vm.busy && login.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (vm.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Giriş yap")
                }
                vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(vm: WiantexViewModel) {
    var tab by rememberSaveable { mutableStateOf(MainTab.Forum) }
    val user = vm.user ?: return
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(vm.error) {
        vm.error?.let {
            snackbarHostState.showSnackbar(it)
            vm.dismissError()
        }
    }
    LaunchedEffect(vm.info) {
        vm.info?.let {
            snackbarHostState.showSnackbar(it)
            vm.dismissInfo()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tab.title, fontWeight = FontWeight.Bold)
                        Text("@${user.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    if (tab == MainTab.Notifications) {
                        IconButton(onClick = { tab = MainTab.Forum }) { Icon(Icons.Default.ArrowBack, "Geri") }
                    } else {
                        Box(Modifier.padding(start = 12.dp)) { WiantexMark(34.dp) }
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (vm.notifications.unread > 0) Badge { Text(vm.notifications.unread.coerceAtMost(99).toString()) }
                        }
                    ) {
                        IconButton(onClick = { tab = MainTab.Notifications; vm.loadNotifications() }) {
                            Icon(Icons.Default.Notifications, "Bildirimler")
                        }
                    }
                    IconButton(onClick = { vm.logout() }) { Icon(Icons.Default.Logout, "Çıkış") }
                }
            )
        },
        bottomBar = {
            if (tab != MainTab.Notifications) {
                NavigationBar {
                    NavItem(MainTab.Forum, tab, Icons.Default.Home) { tab = it; vm.loadForum() }
                    NavItem(MainTab.Pulses, tab, Icons.Default.PlayArrow) { tab = it; vm.loadPulses() }
                    NavItem(MainTab.Messages, tab, Icons.Default.Chat) { tab = it }
                    NavItem(MainTab.Market, tab, Icons.Default.ShoppingCart) { tab = it; vm.loadMarket() }
                    NavItem(MainTab.Profile, tab, Icons.Default.Person) { tab = it; vm.loadProfile() }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                MainTab.Forum -> ForumScreen(vm)
                MainTab.Pulses -> PulsesScreen(vm)
                MainTab.Messages -> MessagesScreen(vm)
                MainTab.Market -> MarketScreen(vm)
                MainTab.Profile -> ProfileScreen(vm)
                MainTab.Notifications -> NotificationsScreen(vm)
            }
            if (vm.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(tab: MainTab, selected: MainTab, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (MainTab) -> Unit) {
    NavigationBarItem(
        selected = tab == selected,
        onClick = { onClick(tab) },
        icon = { Icon(icon, tab.title) },
        label = { Text(tab.title, maxLines = 1) },
    )
}

@Composable
private fun ForumScreen(vm: WiantexViewModel) {
    LaunchedEffect(Unit) { if (vm.forumFeed.topics.isEmpty()) vm.loadForum() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Topluluk akışı", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("${vm.forumFeed.total} konu", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { vm.loadForum() }) { Icon(Icons.Default.Refresh, "Yenile") }
            }
        }
        if (vm.forumFeed.categories.isNotEmpty()) {
            item {
                Text(
                    vm.forumFeed.categories.take(8).joinToString("  •  ") { it.name },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        items(vm.forumFeed.topics, key = { it.id }) { topic ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (topic.username.isNotBlank()) Text("@${topic.username}", color = MaterialTheme.colorScheme.primary)
                        if (topic.category.isNotBlank()) Text(topic.category, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${topic.replyCount} yanıt", style = MaterialTheme.typography.labelMedium)
                        Text(topic.createdAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (vm.forumFeed.topics.isEmpty() && !vm.busy) item { EmptyState("Henüz konu bulunamadı.") }
    }
}

@Composable
private fun PulsesScreen(vm: WiantexViewModel) {
    LaunchedEffect(Unit) { if (vm.pulses.isEmpty()) vm.loadPulses() }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Pulses", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Wiantex kısa videoları", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { vm.loadPulses() }) { Icon(Icons.Default.Refresh, "Yenile") }
            }
        }
        items(vm.pulses, key = { it.id }) { pulse ->
            PulseCard(vm, pulse)
        }
        if (vm.pulses.isEmpty() && !vm.busy) item { EmptyState("Henüz Pulse bulunamadı.") }
    }
}

@Composable
private fun PulseCard(vm: WiantexViewModel, pulse: Pulse) {
    Card(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RemoteAvatar(vm.absoluteUrl(pulse.avatarPath), pulse.username)
                Column(Modifier.weight(1f)) {
                    Text("@${pulse.username}", fontWeight = FontWeight.Bold)
                    Text(pulse.createdAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val videoUrl = vm.absoluteUrl(pulse.videoPath)
            if (!videoUrl.isNullOrBlank()) {
                NativeVideo(videoUrl)
            } else {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(9f / 16f).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.PlayArrow, "Video", Modifier.size(52.dp)) }
            }
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pulse.caption.isNotBlank()) Text(pulse.caption)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = { vm.togglePulseLike(pulse.id) }) {
                        Icon(if (pulse.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Beğen")
                    }
                    Text(pulse.likesCount.toString())
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Default.ChatBubbleOutline, "Yorum")
                    Text(pulse.commentsCount.toString())
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Default.Share, "Paylaşım")
                    Text(pulse.sharesCount.toString())
                }
            }
        }
    }
}

@Composable
private fun NativeVideo(url: String) {
    AndroidView(
        modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f).background(Color.Black),
        factory = { context ->
            VideoView(context).apply {
                setVideoPath(url)
                setOnPreparedListener { player ->
                    player.isLooping = true
                    player.setVolume(0f, 0f)
                    start()
                }
            }
        },
        update = { view ->
            if (!view.isPlaying) view.start()
        },
    )
}

@Composable
private fun MessagesScreen(vm: WiantexViewModel) {
    var username by rememberSaveable { mutableStateOf(vm.activeChatUsername) }
    var text by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.weight(1f),
                label = { Text("Kullanıcı adı") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus(); vm.openConversation(username) }),
            )
            Button(onClick = { focus.clearFocus(); vm.openConversation(username) }) { Text("Aç") }
        }

        if (vm.activeChatUsername.isNotBlank()) {
            Text(
                "@${vm.activeChatUsername}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(vm.messages, key = { it.id }) { message ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start,
                ) {
                    Surface(
                        color = if (message.mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (message.mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.widthIn(max = 320.dp),
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(if (message.deleted) "Mesaj silindi" else message.content)
                            Text(message.createdAt, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
                        }
                    }
                }
            }
        }

        if (vm.activeChatUsername.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 2000) text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Mesaj yaz…") },
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (text.isNotBlank()) vm.sendMessage(text) { text = "" }
                    }),
                )
                FilledIconButton(
                    onClick = { vm.sendMessage(text) { text = "" } },
                    enabled = text.isNotBlank() && !vm.busy,
                ) { Icon(Icons.Default.Send, "Gönder") }
            }
        } else {
            Text("Sohbet açmak için kullanıcı adını yaz.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MarketScreen(vm: WiantexViewModel) {
    var pendingBuy by remember { mutableStateOf<MarketProduct?>(null) }
    LaunchedEffect(Unit) { if (vm.market.products.isEmpty()) vm.loadMarket() }

    pendingBuy?.let { product ->
        AlertDialog(
            onDismissRequest = { pendingBuy = null },
            title = { Text("Satın al") },
            text = { Text("${product.title} ürününü ${product.priceWoin} Woin karşılığında satın almak istiyor musun?") },
            confirmButton = {
                Button(onClick = { vm.buyProduct(product.id); pendingBuy = null }) { Text("Satın al") }
            },
            dismissButton = { TextButton(onClick = { pendingBuy = null }) { Text("Vazgeç") } },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Woin bakiyesi", style = MaterialTheme.typography.labelLarge)
                        Text(vm.market.balance.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    }
                    IconButton(onClick = { vm.loadMarket() }) { Icon(Icons.Default.Refresh, "Yenile") }
                }
            }
        }
        items(vm.market.products, key = { it.id }) { product ->
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(product.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(product.description, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("@${product.username}", color = MaterialTheme.colorScheme.primary)
                        Text("${product.priceWoin} Woin", fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { pendingBuy = product }, modifier = Modifier.fillMaxWidth(), enabled = !product.sold) {
                        Text(if (product.sold) "Satıldı" else "Satın al")
                    }
                }
            }
        }
        if (vm.market.products.isEmpty() && !vm.busy) item { EmptyState("Markette aktif ürün yok.") }
    }
}

@Composable
private fun ProfileScreen(vm: WiantexViewModel) {
    LaunchedEffect(Unit) { if (vm.profile == null) vm.loadProfile() }
    val p = vm.profile
    if (p == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            RemoteAvatar(vm.absoluteUrl(p.avatarPath), p.username, 92.dp)
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("@${p.username}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                if (p.roleName.isNotBlank()) Text(p.roleName, color = MaterialTheme.colorScheme.primary)
                p.presenceStatus?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        if (p.bio.isNotBlank()) item {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(p.bio, modifier = Modifier.fillMaxWidth().padding(16.dp))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Konu", p.topicCount, Modifier.weight(1f))
                StatCard("Mesaj", p.postCount, Modifier.weight(1f))
                StatCard("Beğeni", p.likeCount, Modifier.weight(1f))
            }
        }
        item {
            Button(onClick = { vm.loadProfile() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Profili yenile")
            }
        }
    }
}

@Composable
private fun NotificationsScreen(vm: WiantexViewModel) {
    LaunchedEffect(Unit) { vm.loadNotifications() }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(vm.notifications.items, key = { it.id }) { n ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (n.read) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 1.dp,
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RemoteAvatar(vm.absoluteUrl(n.actorAvatar), n.actorUsername ?: "Wiantex")
                    Column(Modifier.weight(1f)) {
                        Text(n.text, fontWeight = if (n.read) FontWeight.Normal else FontWeight.SemiBold)
                        Text(n.createdAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (vm.notifications.items.isEmpty() && !vm.busy) item { EmptyState("Bildirim yok.") }
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(text, Modifier.fillMaxWidth().padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WiantexMark(size: androidx.compose.ui.unit.Dp = 50.dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(size * 0.33f),
        color = MaterialTheme.colorScheme.primary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("W", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
