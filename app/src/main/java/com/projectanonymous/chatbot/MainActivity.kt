package com.projectanonymous.chatbot

import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.projectanonymous.chatbot.data.aimode.GoogleAiModeWebViewAdapter
import java.util.UUID

private val AppBackground = Color(0xFF101114)
private val PanelBackground = Color(0xFF1B1D22)
private val UserBubble = Color(0xFF292C34)
private val Accent = Color(0xFFE8EAF0)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val fromUser: Boolean
)

data class LibraryItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val isImage: Boolean
)

private enum class AppDestination { CHAT, LIBRARY, HISTORY }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Accent,
                onPrimary = Color(0xFF17191D),
                background = AppBackground,
                surface = PanelBackground,
                onSurface = Color(0xFFF3F4F6),
                onBackground = Color(0xFFF3F4F6)
            )) {
                ChatbotApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatbotApp() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text = "Halo! ChatbOt siap menjadi antarmuka percakapanmu. Integrasi Google AI Mode sedang dalam tahap pengembangan.",
                fromUser = false
            )
        )
    }
    val library = remember { mutableStateListOf<LibraryItem>() }
    var destination by remember { mutableStateOf(AppDestination.CHAT) }
    var input by remember { mutableStateOf("") }
    var attachmentMenu by remember { mutableStateOf(false) }
    var pendingAttachments by remember { mutableStateOf(listOf<LibraryItem>()) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showCancelInfo by remember { mutableStateOf(false) }
    var aiModeWebView by remember { mutableStateOf<WebView?>(null) }
    var showGoogleBrowser by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var generationStatus by remember { mutableStateOf("Siap") }
    var activeGeneration by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(aiModeWebView) {
        onDispose {
            aiModeWebView?.stopLoading()
            aiModeWebView?.destroy()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val item = LibraryItem(uri = it, name = it.lastPathSegment ?: "Gambar", isImage = true)
            library.add(item)
            pendingAttachments = pendingAttachments + item
        }
    }
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val item = LibraryItem(uri = it, name = it.lastPathSegment ?: "File", isImage = false)
            library.add(item)
            pendingAttachments = pendingAttachments + item
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = PanelBackground
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SmartToy, contentDescription = null, tint = Accent)
                        Spacer(Modifier.width(10.dp))
                        Text("ChatbOt", fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Divider(color = Color(0xFF353840))
                    NavigationDrawerItem(
                        label = { Text("Chat baru") },
                        selected = destination == AppDestination.CHAT,
                        icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            messages.clear()
                            messages.add(ChatMessage(
                                text = "Percakapan baru. Integrasi Google AI Mode belum aktif.",
                                fromUser = false
                            ))
                            destination = AppDestination.CHAT
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Cari") },
                        selected = false,
                        icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        onClick = {
                            showSearch = true
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Library") },
                        selected = destination == AppDestination.LIBRARY,
                        icon = { Icon(Icons.Filled.LibraryBooks, contentDescription = null) },
                        onClick = {
                            destination = AppDestination.LIBRARY
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Riwayat") },
                        selected = destination == AppDestination.HISTORY,
                        icon = { Icon(Icons.Filled.History, contentDescription = null) },
                        onClick = {
                            destination = AppDestination.HISTORY
                            scope.launch { drawerState.close() }
                        }
                    )
                    Spacer(Modifier.weight(1f))
                    Text("ChatbOt • 0.1.0", color = Color(0xFF858994), fontSize = 12.sp)
                }
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.navigationBars,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (destination) {
                                AppDestination.CHAT -> "ChatbOt"
                                AppDestination.LIBRARY -> "Library"
                                AppDestination.HISTORY -> "Riwayat"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Buka menu")
                        }
                    },
                    actions = {
                        if (destination == AppDestination.CHAT) {
                            IconButton(onClick = {
                                messages.clear()
                                messages.add(ChatMessage(
                                    text = "Percakapan baru. Integrasi Google AI Mode belum aktif.",
                                    fromUser = false
                                ))
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Chat baru")
                            }
                        }
                        IconButton(onClick = { showCancelInfo = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu lainnya")
                        }
                    }
                )
            },
            containerColor = AppBackground
        ) { innerPadding ->
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (destination) {
                AppDestination.CHAT -> ChatScreen(
                    modifier = Modifier.fillMaxSize(),
                    messages = messages,
                    input = input,
                    onInputChange = { input = it },
                    pendingAttachments = pendingAttachments,
                    isGenerating = isGenerating,
                    generationStatus = generationStatus,
                    onStop = {
                        activeGeneration?.cancel()
                        activeGeneration = null
                        isGenerating = false
                        showGoogleBrowser = false
                        generationStatus = "Dibatalkan"
                    },
                    onRemoveAttachment = { item ->
                        pendingAttachments = pendingAttachments.filterNot { it.id == item.id }
                    },
                    attachmentMenu = attachmentMenu,
                    onAttachmentMenuChange = { attachmentMenu = it },
                    onPickImage = { imagePicker.launch("image/*") },
                    onPickFile = { filePicker.launch(arrayOf("*/*")) },
                    onSend = {
                        val prompt = input.trim()
                        if ((prompt.isNotEmpty() || pendingAttachments.isNotEmpty()) && !isGenerating) {
                            val attachmentNote = if (pendingAttachments.isNotEmpty()) {
                                "\n\nLampiran dipilih (belum diteruskan ke Google AI Mode): " + pendingAttachments.joinToString { it.name }
                            } else ""
                            messages.add(ChatMessage(text = prompt + attachmentNote, fromUser = true))
                            input = ""
                            pendingAttachments = emptyList()
                            val currentWebView = aiModeWebView
                            if (prompt.isBlank()) {
                                messages.add(ChatMessage(text = "Tulis pertanyaan agar Google AI Mode dapat memprosesnya. Lampiran saja belum didukung.", fromUser = false))
                            } else if (currentWebView == null) {
                                messages.add(ChatMessage(text = "Browser belum siap. Coba kirim ulang sebentar lagi.", fromUser = false))
                            } else {
                                isGenerating = true
                                generationStatus = "Menghubungkan ke Google AI Mode…"
                                activeGeneration = scope.launch {
                                    try {
                                        val result = GoogleAiModeWebViewAdapter().ask(currentWebView, prompt) { status ->
                                            generationStatus = status
                                            showGoogleBrowser = status.startsWith("Login Google")
                                        }
                                        val answer = result.response.answer
                                        if (answer.isNotBlank()) {
                                            val sources = result.response.citations.joinToString("\n") { citation ->
                                                "• ${citation.title} (${citation.domain})\n  ${citation.url}"
                                            }
                                            val rendered = if (sources.isNotBlank()) "$answer\n\nSumber:\n$sources" else answer
                                            messages.add(ChatMessage(text = rendered, fromUser = false))
                                        } else {
                                            messages.add(ChatMessage(text = "Google belum memberikan jawaban yang dapat dibaca. Coba lagi, atau buka browser Google untuk memeriksa sesi dan halaman yang tampil.", fromUser = false))
                                        }
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (error: Exception) {
                                        messages.add(ChatMessage(text = "Gagal mengambil jawaban: ${error.message ?: "kesalahan tidak diketahui"}", fromUser = false))
                                    } finally {
                                        isGenerating = false
                                        activeGeneration = null
                                        showGoogleBrowser = false
                                        generationStatus = "Siap"
                                    }
                                }
                            }
                        }
                    }
                )
                AppDestination.LIBRARY -> LibraryScreen(
                    modifier = Modifier.fillMaxSize(),
                    items = library,
                    onDelete = { item -> library.remove(item) }
                )
                AppDestination.HISTORY -> HistoryScreen(Modifier.fillMaxSize())
            }
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        webViewClient = android.webkit.WebViewClient()
                        aiModeWebView = this
                        loadUrl("https://www.google.com/")
                    }
                },
                modifier = if (showGoogleBrowser) Modifier.fillMaxSize() else Modifier.size(1.dp),
                update = { aiModeWebView = it }
            )
            if (showGoogleBrowser) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                    color = PanelBackground
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        IconButton(onClick = { showGoogleBrowser = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Kembali ke chat")
                        }
                        Text("Selesaikan login Google di sini, lalu kembali ke chat.", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
            }
        }
    }

    if (showSearch) {
        AlertDialog(
            onDismissRequest = { showSearch = false },
            title = { Text("Cari percakapan") },
            text = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    placeholder = { Text("Ketik kata kunci") }
                )
            },
            confirmButton = {
                Text(
                    "Tutup",
                    modifier = Modifier.clickable { showSearch = false }.padding(12.dp)
                )
            }
        )
    }

    if (showCancelInfo) {
        AlertDialog(
            onDismissRequest = { showCancelInfo = false },
            title = { Text("Status ChatbOt") },
            text = { Text("UI percakapan sudah tersedia. Koneksi Google AI Mode, sinkronisasi percakapan, dan penyimpanan Library permanen belum diimplementasikan.") },
            confirmButton = {
                Text(
                    "Mengerti",
                    modifier = Modifier.clickable { showCancelInfo = false }.padding(12.dp)
                )
            }
        )
    }
}

@Composable
private fun ChatScreen(
    modifier: Modifier = Modifier,
    messages: List<ChatMessage>,
    input: String,
    onInputChange: (String) -> Unit,
    pendingAttachments: List<LibraryItem>,
    isGenerating: Boolean,
    generationStatus: String,
    onStop: () -> Unit,
    onRemoveAttachment: (LibraryItem) -> Unit,
    attachmentMenu: Boolean,
    onAttachmentMenuChange: (Boolean) -> Unit,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onSend: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = Color(0xFF27251F),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isGenerating) generationStatus else "Google AI Mode · koneksi tidak resmi",
                color = if (isGenerating) Color(0xFFE6C98B) else Color(0xFFB8BEC9),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
            )
        }

        if (messages.size <= 1) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(64.dp).background(PanelBackground, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.SmartToy, contentDescription = null, tint = Accent, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.size(20.dp))
                    Text(
                        "Apa yang ingin kamu tanyakan?",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Mulai percakapan baru dengan ChatbOt.",
                        color = Color(0xFF969AA5),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.size(18.dp))
                    Text(
                        "Jawaban AI belum tersedia sampai adapter Google AI Mode selesai dibuat.",
                        color = Color(0xFF777C87),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
                .background(PanelBackground, RoundedCornerShape(26.dp))
                .padding(12.dp)
        ) {
            if (pendingAttachments.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pendingAttachments.forEach { item ->
                        Surface(color = Color(0xFF343741), shape = RoundedCornerShape(12.dp)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (item.isImage) Icons.Filled.Image else Icons.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(item.name.take(16), fontSize = 11.sp)
                                IconButton(
                                    onClick = { onRemoveAttachment(item) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Hapus lampiran", modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.size(8.dp))
            }

            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFFF3F4F6)),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent),
                decorationBox = { innerTextField ->
                    Box {
                        if (input.isEmpty()) {
                            Text("Kirim pesan ke ChatbOt...", color = Color(0xFF8D929E))
                        }
                        innerTextField()
                    }
                }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(onClick = { onAttachmentMenuChange(true) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Tambah lampiran")
                    }
                    DropdownMenu(
                        expanded = attachmentMenu,
                        onDismissRequest = { onAttachmentMenuChange(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Kirim gambar") },
                            leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null) },
                            onClick = {
                                onAttachmentMenuChange(false)
                                onPickImage()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Pilih file") },
                            leadingIcon = { Icon(Icons.Filled.AttachFile, contentDescription = null) },
                            onClick = {
                                onAttachmentMenuChange(false)
                                onPickFile()
                            }
                        )
                    }
                }
                Text("Library tersedia dari menu utama", color = Color(0xFF777C87), fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = { if (isGenerating) onStop() else onSend() },
                    enabled = isGenerating || input.isNotBlank() || pendingAttachments.isNotEmpty(),
                    modifier = Modifier.size(42.dp).background(
                        if (input.isNotBlank() || pendingAttachments.isNotEmpty()) Accent else Color(0xFF3B3E46),
                        CircleShape
                    )
                ) {
                    Icon(
                        if (isGenerating) Icons.Filled.Stop else Icons.Filled.Send,
                        contentDescription = if (isGenerating) "Hentikan proses" else "Kirim pesan",
                        tint = if (isGenerating || input.isNotBlank() || pendingAttachments.isNotEmpty()) Color(0xFF17191D) else Color(0xFF858A95)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.fromUser) UserBubble else Color.Transparent,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(if (message.fromUser) 0.88f else 1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                if (!message.fromUser) {
                    Icon(Icons.Filled.SmartToy, contentDescription = null, tint = Color(0xFFB8BEC9), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Text(message.text, color = Color(0xFFF0F1F4), lineHeight = 22.sp)
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    modifier: Modifier = Modifier,
    items: List<LibraryItem>,
    onDelete: (LibraryItem) -> Unit
) {
    if (items.isEmpty()) {
        Box(modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.LibraryBooks, contentDescription = null, tint = Color(0xFF8D929E), modifier = Modifier.size(42.dp))
                Spacer(Modifier.size(14.dp))
                Text("Library masih kosong", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.size(6.dp))
                Text(
                    "Gambar dan file yang kamu lampirkan akan muncul di sini selama sesi aplikasi berjalan.",
                    color = Color(0xFF8D929E),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.id }) { item ->
                Surface(color = PanelBackground, shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (item.isImage) Icons.Filled.Image else Icons.Filled.InsertDriveFile,
                            contentDescription = null,
                            tint = Color(0xFFCDD1DA)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(item.name, modifier = Modifier.weight(1f), maxLines = 1)
                        IconButton(onClick = { onDelete(item) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Hapus file")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.History, contentDescription = null, tint = Color(0xFF8D929E), modifier = Modifier.size(42.dp))
            Spacer(Modifier.size(14.dp))
            Text("Belum ada riwayat tersimpan", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Spacer(Modifier.size(6.dp))
            Text(
                "Penyimpanan dan pemulihan percakapan akan ditambahkan pada tahap berikutnya.",
                color = Color(0xFF8D929E),
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
        }
    }
}
