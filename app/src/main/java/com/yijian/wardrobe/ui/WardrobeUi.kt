@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yijian.wardrobe.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.yijian.wardrobe.R
import com.yijian.wardrobe.UiSnapshot
import com.yijian.wardrobe.WardrobeViewModel
import com.yijian.wardrobe.data.AiCapability
import com.yijian.wardrobe.data.AiPreview
import com.yijian.wardrobe.data.AiProvider
import com.yijian.wardrobe.data.Garment
import com.yijian.wardrobe.data.GarmentStatus
import com.yijian.wardrobe.data.JobStatus
import com.yijian.wardrobe.data.Outfit
import com.yijian.wardrobe.data.OutfitItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class MainTab(val label: String) { CLOSET("衣橱"), STUDIO("造型间"), HISTORY("衣迹") }

@Composable
fun YiJianApp(viewModel: WardrobeViewModel) = YiJianTheme {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableStateOf(MainTab.CLOSET) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var selectedGarment by remember { mutableStateOf<Garment?>(null) }
    var editingGarment by remember { mutableStateOf<Garment?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
    }
    LaunchedEffect(Unit) { while (true) { delay(5000); viewModel.refresh() } }

    if (settings) {
        SettingsScreen(viewModel, state, onBack = { settings = false })
        return@YiJianTheme
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Cream,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream),
                title = { Column { Text("衣见", style = MaterialTheme.typography.headlineSmall); Text(tab.label, color = Mist, style = MaterialTheme.typography.labelMedium) } },
                actions = {
                    if (state.busy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    IconButton(onClick = viewModel::refresh) { Icon(Icons.Default.Refresh, "刷新本地状态") }
                    IconButton(onClick = { settings = true }) { Icon(Icons.Default.Settings, "设置") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            BottomAppBar(containerColor = WarmWhite, modifier = Modifier.navigationBarsPadding()) {
                MainTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item, onClick = { tab = item }, label = { Text(item.label) },
                        icon = { Icon(when (item) { MainTab.CLOSET -> Icons.Default.Checkroom; MainTab.STUDIO -> Icons.Default.Style; MainTab.HISTORY -> Icons.Default.History }, item.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == MainTab.CLOSET) ExtendedFloatingActionButton(onClick = { adding = true }, icon = { Icon(Icons.Default.Add, "添加衣物") }, text = { Text("添加衣物") }, modifier = Modifier.testTag("add-garment-fab"), containerColor = Rose, contentColor = Color.White)
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                MainTab.CLOSET -> ClosetScreen(state, viewModel, onSelect = { selectedGarment = it }, onAdd = { adding = true })
                MainTab.STUDIO -> StudioScreen(state, viewModel)
                MainTab.HISTORY -> HistoryScreen(state, viewModel)
            }
        }
    }

    if (adding) AddGarmentSheet(null, viewModel, onDismiss = { adding = false })
    editingGarment?.let { AddGarmentSheet(it, viewModel, onDismiss = { editingGarment = null }) }
    selectedGarment?.let { garment -> GarmentDetailSheet(garment, viewModel, onDismiss = { selectedGarment = null }, onEdit = { selectedGarment = null; editingGarment = garment }) }
}

@Composable
private fun ClosetScreen(state: UiSnapshot, viewModel: WardrobeViewModel, onSelect: (Garment) -> Unit, onAdd: () -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("全部") }
    var status by rememberSaveable { mutableStateOf<GarmentStatus?>(null) }
    var aiPreview by remember { mutableStateOf<AiPreview?>(null) }
    var aiAnswer by remember { mutableStateOf<String?>(null) }
    var buyPath by remember { mutableStateOf<String?>(null) }
    var buyCameraUri by remember { mutableStateOf<Uri?>(null) }
    var buyCameraDenied by remember { mutableStateOf(false) }
    val buyPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) viewModel.importAiPreview(uri) { path -> buyPath = path; aiPreview = AiPreview(AiCapability.BUY_CHECK, listOf(path), "与本地衣橱比较重复度、三套兼容性与场景覆盖", "只发送必要的候选衣物摘要", "DeepSeek 按图片与文本 token 计费") } }
    val buyTakePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) buyCameraUri?.let { uri -> viewModel.importAiPreview(uri) { path -> buyPath = path; aiPreview = AiPreview(AiCapability.BUY_CHECK, listOf(path), "与本地衣橱比较重复度、三套兼容性与场景覆盖", "只发送必要的候选衣物摘要", "DeepSeek 按图片与文本 token 计费") } }
    }
    fun launchBuyCamera() {
        buyCameraUri = createCameraUri(context, "buy-check")
        buyTakePicture.launch(buyCameraUri!!)
    }
    val buyCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { buyCameraDenied = false; launchBuyCamera() } else buyCameraDenied = true
    }
    val categories = listOf("全部", "上装", "下装", "连衣裙", "外套", "鞋", "包", "配饰")
    val filtered = state.garments.filter { g ->
        (category == "全部" || g.category == category) && (status == null || g.status == status) &&
            (query.isBlank() || listOf(g.name, g.colors, g.material, g.occasions, g.styleTags, g.brand).any { it.contains(query, true) })
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric("总件数", state.garments.size.toString(), Modifier.weight(1f))
            Metric("现在可穿", state.garments.count { it.status.wearable }.toString(), Modifier.weight(1f))
            Metric("清洗/送洗", state.garments.count { it.status == GarmentStatus.WASHING || it.status == GarmentStatus.DRY_CLEANING }.toString(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(value = query, onValueChange = { query = it }, leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("搜索颜色、材质、场景或品牌") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        LazyRow(contentPadding = PaddingValues(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { item -> FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) }) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(selected = status == null, onClick = { status = null }, label = { Text("全部状态") }) }
            items(GarmentStatus.entries) { item -> FilterChip(selected = status == item, onClick = { status = item }, label = { Text(item.label) }) }
        }
        LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { AssistChip(onClick = {
                val summary = state.garments.joinToString("；") { "${it.id}:${it.name}/${it.category}/${it.colors}/${it.seasons}/${it.occasions}/${it.status.label}" }
                aiPreview = AiPreview(AiCapability.NATURAL_SEARCH, emptyList(), "理解搜索语义并只返回本地存在的衣物 ID", "问题=$query；本地候选=$summary", "DeepSeek 文本模型按 token 计费")
            }, enabled = query.isNotBlank(), label = { Text("自然语言搜索") }, leadingIcon = { Icon(Icons.Default.AutoAwesome, null) }) }
            item { AssistChip(onClick = { buyPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, label = { Text("相册买前检查") }, leadingIcon = { Icon(Icons.Default.Image, null) }) }
            item { AssistChip(onClick = { if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchBuyCamera() else buyCameraPermission.launch(Manifest.permission.CAMERA) }, label = { Text("拍照买前检查") }, leadingIcon = { Icon(Icons.Default.CameraAlt, null) }) }
        }
        if (buyCameraDenied) Text("相机权限被拒绝；可继续使用系统照片选择器按张授权。", color = SoftRed, style = MaterialTheme.typography.labelMedium)
        aiAnswer?.let { Surface(color = WarmWhite, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Column(Modifier.padding(12.dp)) { Text("AI 辅助结果", fontWeight = FontWeight.SemiBold); Text(it); Text("结果仅引用本地候选；请以当前筛选和单品详情为准。", color = Mist) } } }
        Spacer(Modifier.height(8.dp))
        if (filtered.isEmpty()) Box(Modifier.weight(1f)) { EmptyState(if (state.garments.isEmpty()) "从第一件衣物开始\n点击右下角添加衣物，核心衣橱无需 Key、无需联网" else "没有符合筛选条件的单品") }
        else LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 100.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(filtered, key = { it.id }) { garment -> GarmentCard(garment) { onSelect(garment) } }
        }
    }
    aiPreview?.let { p -> SendPreviewDialog(p, { aiPreview = null }, {
        aiPreview = null
        val summary = state.garments.joinToString("；") { "${it.id}:${it.name}/${it.category}/${it.colors}/${it.occasions}/${it.wearCount}次" }
        val prompt = if (p.capability == AiCapability.BUY_CHECK) "待购图片与衣橱候选：$summary。判断重复度，给出至少三套兼容方案或明确不足，不替用户做消费决定。" else p.textSummary
        viewModel.deepSeekText(p.capability, prompt, p.imagePaths) { aiAnswer = it }
    }) }
}

@Composable private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(WarmWhite, RoundedCornerShape(18.dp)).padding(14.dp)) { Text(value, style = MaterialTheme.typography.titleLarge, color = Rose); Text(label, style = MaterialTheme.typography.labelMedium, color = Mist) }
}

@Composable private fun GarmentCard(garment: Garment, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(.82f).clip(RoundedCornerShape(18.dp)).background(Color(0xFFE9E1D6))) {
            garment.photos.firstOrNull()?.let { AsyncImage(model = File(LocalContext.current.filesDir, it.relativePath), contentDescription = garment.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            Surface(Modifier.align(Alignment.TopEnd).padding(8.dp), shape = CircleShape, color = if (garment.status.wearable) Forest.copy(alpha = .88f) else SoftRed.copy(alpha = .88f)) { Text(garment.status.label, color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
        }
        Spacer(Modifier.height(7.dp)); Text(garment.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        Text(listOf(garment.category, garment.colors).filter { it.isNotBlank() }.joinToString(" · "), color = Mist, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable private fun EmptyState(text: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Checkroom, null, tint = Sage, modifier = Modifier.size(60.dp)); Spacer(Modifier.height(16.dp)); Text(text, color = Mist)
    }
}

@Composable
private fun AddGarmentSheet(existing: Garment?, viewModel: WardrobeViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var category by remember { mutableStateOf(existing?.category ?: "上装") }
    var colors by remember { mutableStateOf(existing?.colors.orEmpty()) }
    var material by remember { mutableStateOf(existing?.material.orEmpty()) }
    var seasons by remember { mutableStateOf(existing?.seasons.orEmpty()) }
    var occasions by remember { mutableStateOf(existing?.occasions.orEmpty()) }
    var style by remember { mutableStateOf(existing?.styleTags.orEmpty()) }
    var brand by remember { mutableStateOf(existing?.brand.orEmpty()) }
    var price by remember { mutableStateOf(existing?.priceCents?.takeIf { it > 0 }?.let { "%.2f".format(it / 100.0) }.orEmpty()) }
    var status by remember { mutableStateOf(existing?.status ?: GarmentStatus.AVAILABLE) }
    val photos = remember { mutableStateListOf<Uri>() }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraDenied by remember { mutableStateOf(false) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris -> photos.clear(); photos.addAll(uris.take((5 - (existing?.photos?.size ?: 0)).coerceAtLeast(0))) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) cameraUri?.let { photos.add(it) } }
    fun launchCamera() {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        cameraUri = uri
        takePicture.launch(uri)
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) launchCamera() else cameraDenied = true }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Cream) {
        LazyColumn(
            Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(if (existing == null) "添加衣物" else "编辑衣物", style = MaterialTheme.typography.headlineSmall); Text("数据和原图保存在本机。照片最多 5 张；系统照片选择器会按所选图片逐次授权，不读取整个相册。", color = Mist) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    existing?.photos?.forEach { p -> Box(Modifier.size(72.dp)) { AsyncImage(File(LocalContext.current.filesDir, p.relativePath), null, Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop); IconButton(onClick = { viewModel.deletePhoto(existing, p.id); onDismiss() }, modifier = Modifier.align(Alignment.TopEnd).size(28.dp).background(WarmWhite.copy(alpha = .85f), CircleShape)) { Icon(Icons.Default.Close, "删除照片", tint = SoftRed) } } }
                    photos.forEach { uri -> AsyncImage(uri, null, Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop) }
                    if ((existing?.photos?.size ?: 0) + photos.size < 5) OutlinedButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.height(72.dp)) { Icon(Icons.Default.Image, null); Text("选照片") }
                }
            }
            item { OutlinedButton(onClick = { if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCamera() else cameraPermission.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth()) { Text("相机拍摄") }; if (cameraDenied) Text("相机权限被拒绝，仍可使用系统照片选择器导入。", color = SoftRed) }
            item { OutlinedTextField(name, { name = it }, label = { Text("名称 *") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { ChoiceRow("分类", listOf("上装", "下装", "连衣裙", "外套", "鞋", "包", "配饰"), category) { category = it } }
            item { OutlinedTextField(colors, { colors = it }, label = { Text("颜色") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(material, { material = it }, label = { Text("材质（视觉推测需自行核对）") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(seasons, { seasons = it }, label = { Text("季节，如：春,秋") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(occasions, { occasions = it }, label = { Text("场合，如：通勤,休闲") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(style, { style = it }, label = { Text("风格标签") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(brand, { brand = it }, label = { Text("品牌") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(price, { price = it }, label = { Text("购入价（元）") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { Text("当前状态", fontWeight = FontWeight.SemiBold); LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(GarmentStatus.entries) { s -> FilterChip(status == s, { status = s }, { Text(s.label) }) } } }
            item {
                Button(onClick = {
                    viewModel.saveGarment(existing, name, category, colors, material, seasons, occasions, style, brand, price, status, photos.toList())
                    onDismiss()
                }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("保存到本地衣橱") }
            }
        }
    }
}

@Composable private fun ChoiceRow(label: String, choices: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column { Text(label, fontWeight = FontWeight.SemiBold); LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(choices) { c -> FilterChip(selected == c, { onSelect(c) }, { Text(c) }) } } }
}

@Composable
private fun GarmentDetailSheet(garment: Garment, viewModel: WardrobeViewModel, onDismiss: () -> Unit, onEdit: () -> Unit) {
    var deleteConfirm by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<AiPreview?>(null) }
    var aiResult by remember { mutableStateOf<String?>(null) }
    val associations = remember(garment.id) { viewModel.associations(garment.id) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Cream) {
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { items(garment.photos) { photo -> AsyncImage(File(LocalContext.current.filesDir, photo.relativePath), garment.name, Modifier.size(210.dp).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop) } }
            }
            item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(garment.name, style = MaterialTheme.typography.headlineSmall); Text("${garment.category} · ${garment.colors}", color = Mist) }; IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "编辑") } } }
            item { Text("状态会立即联动今天可穿与造型候选", color = Mist); LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(GarmentStatus.entries) { s -> FilterChip(garment.status == s, { viewModel.updateStatus(garment, s); onDismiss() }, { Text(s.label) }) } } }
            item { DetailLine("材质", garment.material); DetailLine("季节", garment.seasons); DetailLine("场合", garment.occasions); DetailLine("风格", garment.styleTags); DetailLine("穿着", "${garment.wearCount} 次"); DetailLine("单次成本", garment.costPerWearCents?.let { "¥%.2f".format(it / 100.0) } ?: "尚无足够数据") }
            item {
                Button(onClick = { garment.photos.firstOrNull()?.let { preview = AiPreview(AiCapability.RECOGNITION, listOf(it.relativePath), "识别分类、颜色、季节、场合与风格；结果先预览，不自动写入", garment.name, "DeepSeek 按图片 token 与模型当时价格计费") } }, enabled = garment.photos.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("AI 帮我识别") }
            }
            aiResult?.let { result -> item { Surface(color = WarmWhite, shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp)) { Text("AI 候选结果", fontWeight = FontWeight.SemiBold); Text(result, style = MaterialTheme.typography.bodyMedium); Text("请点击编辑并人工核对后保存，AI 不会擅自改正式数据。", color = Rose) } } } }
            item { OutlinedButton(onClick = { deleteConfirm = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Delete, null); Text("删除单品") } }
        }
    }
    preview?.let { p -> SendPreviewDialog(p, onCancel = { preview = null }, onConfirm = { preview = null; viewModel.recognizeGarment(garment) { aiResult = it } }) }
    if (deleteConfirm) AlertDialog(onDismissRequest = { deleteConfirm = false }, title = { Text("删除这件单品？") }, text = { Text("关联 ${associations.first} 个造型、${associations.second} 条穿着记录。删除会移除当前衣橱中的关联；历史统计不会伪造。") }, confirmButton = { TextButton(onClick = { viewModel.deleteGarment(garment); deleteConfirm = false; onDismiss() }) { Text("确认删除", color = SoftRed) } }, dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("取消") } })
}

@Composable private fun DetailLine(label: String, value: String) { if (value.isNotBlank()) Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text(label, color = Mist, modifier = Modifier.width(72.dp)); Text(value) } }

@Composable
private fun SendPreviewDialog(preview: AiPreview, onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("确认发送内容") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("服务商：${if (preview.capability.provider == AiProvider.DEEPSEEK) "DeepSeek 官方 API" else "阿里云百炼（华北2 北京）"}"); Text("模型：${preview.capability.model}"); Text("用途：${preview.purpose}") }
                item { Text("实际发送图片 ${preview.imagePaths.size} 张", fontWeight = FontWeight.SemiBold); LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(preview.imagePaths) { path -> AsyncImage(File(LocalContext.current.filesDir, path), null, Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop) } } }
                item { Text("文字/标签：${preview.textSummary.ifBlank { "无" }}"); Text("费用参考：${preview.costReference}", color = Rose); Text("选中素材会发送给第三方云模型处理，不能视为完全离线。", color = Mist) }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("确认并发送") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("取消") } }
    )
}

private data class CanvasTransform(var x: Float = 0f, var y: Float = 0f, var scale: Float = 1f, var rotation: Float = 0f)

private fun createCameraUri(context: Context, prefix: String): Uri {
    val directory = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(directory, "$prefix-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
}

@Composable
private fun StudioScreen(state: UiSnapshot, viewModel: WardrobeViewModel) {
    val context = LocalContext.current
    val available = state.garments.filter { it.status.wearable }
    val selected = remember { mutableStateListOf<String>() }
    val transforms = remember { mutableStateMapOf<String, CanvasTransform>() }
    var outfitName by rememberSaveable { mutableStateOf("") }
    var scene by rememberSaveable { mutableStateOf("通勤") }
    var aiPreview by remember { mutableStateOf<AiPreview?>(null) }
    var aiAnswer by remember { mutableStateOf<String?>(null) }
    var pendingTryOn by remember { mutableStateOf<AiCapability?>(null) }
    var personUri by remember { mutableStateOf<Uri?>(null) }
    var mirrorCameraUri by remember { mutableStateOf<Uri?>(null) }
    var mirrorCameraDenied by remember { mutableStateOf(false) }
    val personPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) personUri = uri else pendingTryOn = null }
    val mirrorPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) viewModel.importAiPreview(uri) { path -> aiPreview = AiPreview(AiCapability.MIRROR_ANALYSIS, listOf(path), "中性分析颜色、层次、比例目标与场景适配，不做身体或颜值评分", "场景=$scene", "DeepSeek 按图片与文本 token 计费") } }
    val mirrorTakePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) mirrorCameraUri?.let { uri -> viewModel.importAiPreview(uri) { path -> aiPreview = AiPreview(AiCapability.MIRROR_ANALYSIS, listOf(path), "中性分析颜色、层次、比例目标与场景适配，不做身体或颜值评分", "场景=$scene", "DeepSeek 按图片与文本 token 计费") } }
    }
    fun launchMirrorCamera() {
        mirrorCameraUri = createCameraUri(context, "mirror-analysis")
        mirrorTakePicture.launch(mirrorCameraUri!!)
    }
    val mirrorCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { mirrorCameraDenied = false; launchMirrorCamera() } else mirrorCameraDenied = true
    }

    val selectedGarments = selected.mapNotNull { id -> available.find { it.id == id } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("搭配画布", style = MaterialTheme.typography.headlineSmall)
            Text("拖动、双指缩放或旋转。不可穿状态已自动排除。", color = Mist)
        }
        item {
            Box(Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(24.dp)).background(WarmWhite).border(1.dp, Sage.copy(alpha = .45f), RoundedCornerShape(24.dp))) {
                if (selected.isEmpty()) Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Style, null, tint = Sage, modifier = Modifier.size(52.dp)); Text("从下方加入可穿单品", color = Mist) }
                selected.forEachIndexed { index, id ->
                    val garment = available.find { it.id == id } ?: return@forEachIndexed
                    val t = transforms.getOrPut(id) { CanvasTransform((index % 2) * 130f, (index / 2) * 135f, .9f, 0f) }
                    Box(Modifier.offset { IntOffset(t.x.roundToInt(), t.y.roundToInt()) }.size(130.dp).scale(t.scale).rotate(t.rotation).pointerInput(id) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val pan = event.calculatePan()
                                val zoom = event.calculateZoom()
                                val rotation = event.calculateRotation()
                                if (pan.x != 0f || pan.y != 0f || zoom != 1f || rotation != 0f) {
                                    transforms[id] = (transforms[id] ?: t).copy(
                                        x = (transforms[id]?.x ?: t.x) + pan.x,
                                        y = (transforms[id]?.y ?: t.y) + pan.y,
                                        scale = ((transforms[id]?.scale ?: t.scale) * zoom).coerceIn(.55f, 1.8f),
                                        rotation = (transforms[id]?.rotation ?: t.rotation) + rotation
                                    )
                                    event.changes.forEach { it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }.clip(RoundedCornerShape(16.dp)).background(Cream).border(1.dp, Rose.copy(alpha = .4f), RoundedCornerShape(16.dp))) {
                        garment.photos.firstOrNull()?.let { AsyncImage(File(LocalContext.current.filesDir, it.relativePath), garment.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                        IconButton(onClick = { selected.remove(id); transforms.remove(id) }, modifier = Modifier.align(Alignment.TopEnd).size(34.dp).background(WarmWhite.copy(alpha = .85f), CircleShape)) { Icon(Icons.Default.Close, "从画布移除", tint = SoftRed) }
                        Text(garment.name, Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Forest.copy(alpha = .78f)).padding(6.dp), color = Color.White, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        item {
            Text("可穿单品", fontWeight = FontWeight.SemiBold)
            if (available.isEmpty()) Text("暂无可穿单品；请先录入或恢复衣物状态", color = Mist)
            else LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { items(available, key = { it.id }) { garment ->
                Column(Modifier.width(86.dp).clickable { if (garment.id in selected) selected.remove(garment.id) else selected.add(garment.id) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(82.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE9E1D6)).then(if (garment.id in selected) Modifier.border(3.dp, Rose, RoundedCornerShape(14.dp)) else Modifier)) { garment.photos.firstOrNull()?.let { AsyncImage(File(LocalContext.current.filesDir, it.relativePath), garment.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } }
                    Text(garment.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                }
            } }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedTextField(outfitName, { outfitName = it }, label = { Text("造型名称") }, modifier = Modifier.weight(1f)); OutlinedTextField(scene, { scene = it }, label = { Text("场景") }, modifier = Modifier.weight(1f)) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { viewModel.saveOutfit(outfitName, scene, selected.mapIndexed { index, id -> val t = transforms[id] ?: CanvasTransform(); OutfitItem(id, t.x, t.y, t.scale, t.rotation, index) }) }, modifier = Modifier.weight(1f)) { Text("保存造型") }
                OutlinedButton(onClick = {
                    val summary = selectedGarments.joinToString("；") { "${it.id}:${it.name}/${it.category}/${it.colors}/${it.styleTags}" }
                    aiPreview = AiPreview(AiCapability.OUTFIT_ANALYSIS, emptyList(), "只从当前可穿的本地衣物 ID 中给出 1–3 套可执行方案", "场景=$scene；候选=$summary", "DeepSeek 按当前模型 token 计费")
                }, enabled = selected.isNotEmpty(), modifier = Modifier.weight(1f)) { Icon(Icons.Default.AutoAwesome, null); Text("AI 搭配") }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { mirrorPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(6.dp)); Text("相册分析") }
                OutlinedButton(onClick = { if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchMirrorCamera() else mirrorCameraPermission.launch(Manifest.permission.CAMERA) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(6.dp)); Text("拍照分析") }
            }
            Text("相册由系统照片选择器按张授权；拍照首次使用会请求相机权限。", color = Mist, style = MaterialTheme.typography.labelMedium)
            if (mirrorCameraDenied) Text("相机权限被拒绝，可改用相册；如需拍照请在系统设置中允许相机权限。", color = SoftRed, style = MaterialTheme.typography.labelMedium)
        }
        aiAnswer?.let { answer -> item { Surface(color = WarmWhite, shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp)) { Text("AI 搭配理由", fontWeight = FontWeight.SemiBold); Text(answer); Text("保存前请核对衣物 ID；AI 不会创建不存在的衣物。", color = Rose) } } } }
        item {
            Text("AI 虚拟试衣（个人账号 Beta）", style = MaterialTheme.typography.titleLarge)
            Text("选择人物照后仍会展示实际发送预览。每次只创建 1 张；精修不会暗中连调。", color = Mist)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(AiCapability.TRYON_BASIC, AiCapability.TRYON_PLUS, AiCapability.TRYON_PARSING, AiCapability.TRYON_REFINER, AiCapability.SHOE)) { cap ->
                    AssistChip(onClick = { pendingTryOn = cap; personPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, label = { Text(cap.label) }, leadingIcon = { Icon(Icons.Default.AutoAwesome, null) })
                }
            }
        }
        item {
            Text("生成任务", style = MaterialTheme.typography.titleLarge)
            if (state.tryOnJobs.isEmpty()) Text("暂无任务", color = Mist) else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { state.tryOnJobs.take(8).forEach { job -> TryOnJobCard(job) } }
        }
        item {
            Text("已保存造型", style = MaterialTheme.typography.titleLarge)
            if (state.outfits.isEmpty()) Text("尚未保存造型", color = Mist) else LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) { items(state.outfits, key = { it.id }) { outfit -> OutfitCard(outfit, state.garments) } }
        }
    }
    aiPreview?.let { p -> SendPreviewDialog(p, { aiPreview = null }, {
        aiPreview = null
        val prompt = when (p.capability) {
            AiCapability.MIRROR_ANALYSIS -> "请分析实际发送的镜面照，从颜色协调、穿搭层次、比例目标和场景适配四方面给出简短建议。不得评价身材或颜值；如果图片并非人物穿搭，请明确说明无法完成穿搭判断，并描述还需要怎样的照片。${p.textSummary}"
            else -> p.textSummary
        }
        viewModel.deepSeekText(p.capability, prompt, p.imagePaths) { aiAnswer = it }
    }) }
    val cap = pendingTryOn; val person = personUri
    if (cap != null && person != null) TryOnConfirmDialog(cap, person, selectedGarments, state.tryOnJobs.firstOrNull { it.status == JobStatus.SUCCEEDED }?.resultPath, onCancel = { pendingTryOn = null; personUri = null }, onConfirm = { top, bottom, shoe, coarse ->
        viewModel.startTryOn(cap, person, top, bottom, coarse, shoe); pendingTryOn = null; personUri = null
    })
}

@Composable private fun OutfitCard(outfit: Outfit, garments: List<Garment>) {
    Surface(color = WarmWhite, shape = RoundedCornerShape(18.dp), modifier = Modifier.width(210.dp)) { Column(Modifier.padding(14.dp)) { Text(outfit.name, fontWeight = FontWeight.SemiBold); Text(outfit.scene, color = Rose); Text(outfit.items.mapNotNull { item -> garments.find { it.id == item.garmentId }?.name }.joinToString(" + "), maxLines = 2, color = Mist) } }
}

@Composable private fun TryOnJobCard(job: com.yijian.wardrobe.data.TryOnJob) {
    val stage = when (job.status) { JobStatus.PREPARING -> "正在准备图片"; JobStatus.UPLOADING -> "正在安全上传所选素材"; JobStatus.SUBMITTED, JobStatus.PENDING -> "已提交，等待模型处理"; JobStatus.RUNNING -> "正在生成试穿效果"; JobStatus.REFINING -> "正在精修细节"; JobStatus.REPLACING_SHOES -> "正在替换鞋靴"; JobStatus.DOWNLOADING -> "正在下载并保存到本机"; JobStatus.SUCCEEDED -> "生成完成"; JobStatus.FAILED -> "生成失败" }
    Surface(color = WarmWhite, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(74.dp).clip(RoundedCornerShape(12.dp)).background(Cream)) {
                if (job.resultPath != null) AsyncImage(File(LocalContext.current.filesDir, job.resultPath), "试衣结果", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else if (job.status !in listOf(JobStatus.FAILED, JobStatus.SUCCEEDED)) WarmShimmer(Modifier.fillMaxSize())
                else Icon(if (job.status == JobStatus.FAILED) Icons.Default.Close else Icons.Default.Checkroom, null, Modifier.align(Alignment.Center), tint = if (job.status == JobStatus.FAILED) SoftRed else Sage)
            }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(job.model, fontWeight = FontWeight.SemiBold); Text(stage, color = if (job.status == JobStatus.FAILED) SoftRed else Mist); job.errorMessage?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = SoftRed) } }
        }
    }
}

@Composable private fun WarmShimmer(modifier: Modifier = Modifier) {
    val reduceMotion = android.provider.Settings.Global.getFloat(LocalContext.current.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    if (reduceMotion) Box(modifier.background(Rose.copy(alpha = .10f)))
    else {
        val transition = rememberInfiniteTransition(label = "warmShimmer")
        val x by transition.animateFloat(-1f, 2f, infiniteRepeatable(tween(1900), RepeatMode.Restart), label = "x")
        Box(modifier.background(Brush.linearGradient(listOf(Cream, Rose.copy(alpha = .18f), WarmWhite), start = androidx.compose.ui.geometry.Offset(x * 250f, 0f), end = androidx.compose.ui.geometry.Offset(x * 250f + 90f, 180f))))
    }
}

@Composable
private fun TryOnConfirmDialog(cap: AiCapability, person: Uri, garments: List<Garment>, coarsePath: String?, onCancel: () -> Unit, onConfirm: (Garment?, Garment?, Garment?, String?) -> Unit) {
    val tops = garments.filter { it.category in listOf("上装", "连衣裙", "外套") && it.photos.isNotEmpty() }
    val bottoms = garments.filter { it.category == "下装" && it.photos.isNotEmpty() }
    val shoes = garments.filter { it.category == "鞋" && it.photos.isNotEmpty() }
    var top by remember { mutableStateOf(tops.firstOrNull()) }
    var bottom by remember { mutableStateOf(bottoms.firstOrNull()) }
    var shoe by remember { mutableStateOf(shoes.firstOrNull()) }
    val cost = when (cap) { AiCapability.TRYON_BASIC -> "官方参考 ¥0.20/成功图；免费额度资格以百炼控制台为准"; AiCapability.TRYON_PLUS -> "官方参考 ¥0.50/成功图；免费额度资格以百炼控制台为准"; AiCapability.TRYON_REFINER -> "独立计费，需再次确认；不是免费升级"; AiCapability.TRYON_PARSING -> "按输入图片计费；官方价格可能变化"; AiCapability.SHOE -> "500 张免费体验，额度用尽后官方暂不支持付费"; else -> "" }
    AlertDialog(onDismissRequest = onCancel, title = { Text("确认发送给百炼") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("服务商：阿里云百炼 · 华北2（北京）"); Text("模型：${cap.model}"); Text("用途：${cap.label}"); Text("人物照片：") ; AsyncImage(person, "实际发送的人物照", Modifier.fillMaxWidth().aspectRatio(.75f).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop) }
            if (cap != AiCapability.SHOE && cap != AiCapability.TRYON_PARSING) item { GarmentSelector("上装/连衣裙", tops, top) { top = it }; GarmentSelector("下装", bottoms, bottom) { bottom = it } }
            if (cap == AiCapability.SHOE) item { GarmentSelector("鞋靴", shoes, shoe) { shoe = it } }
            if (cap == AiCapability.TRYON_REFINER) item { Text(if (coarsePath == null) "没有可用粗试衣结果，无法精修" else "将同时发送粗试衣结果与原始素材", color = if (coarsePath == null) SoftRed else Mist) }
            item { Text("费用：$cost", color = Rose); Text("APP 会去除 EXIF 后使用你的 Key 申请模型绑定临时上传凭证；图片进入第三方云端处理。", color = Mist); Text("AI 生成仅供穿搭灵感，不能代表真实尺码、面料、颜色或舒适度。", fontWeight = FontWeight.SemiBold) }
        }
    }, confirmButton = { Button(onClick = { onConfirm(top, bottom, shoe, coarsePath) }, enabled = cap != AiCapability.TRYON_REFINER || coarsePath != null) { Text("确认并创建 1 张") } }, dismissButton = { TextButton(onClick = onCancel) { Text("取消") } })
}

@Composable private fun GarmentSelector(label: String, choices: List<Garment>, selected: Garment?, onSelect: (Garment?) -> Unit) {
    Text(label, fontWeight = FontWeight.SemiBold)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { item { FilterChip(selected == null, { onSelect(null) }, { Text("不发送") }) }; items(choices) { g -> FilterChip(selected?.id == g.id, { onSelect(g) }, { Text(g.name) }) } }
}

@Composable
private fun HistoryScreen(state: UiSnapshot, viewModel: WardrobeViewModel) {
    var recordDialog by remember { mutableStateOf(false) }
    var reviewPreview by remember { mutableStateOf<AiPreview?>(null) }
    var review by remember { mutableStateOf<String?>(null) }
    val currentMonth = SimpleDateFormat("yyyy年M月", Locale.CHINA).format(Date())
    val wornIds = state.wearLogs.flatMap { it.garmentIds }
    val top = wornIds.groupingBy { it }.eachCount().maxByOrNull { it.value }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("$currentMonth 衣迹", style = MaterialTheme.typography.headlineSmall); Text("真实穿着会联动次数、最近使用和单次成本", color = Mist) }; Button(onClick = { recordDialog = true }) { Icon(Icons.Default.Add, null); Text("记录今天") } } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric("穿着记录", state.wearLogs.size.toString(), Modifier.weight(1f))
                Metric("穿过单品", wornIds.distinct().size.toString(), Modifier.weight(1f))
                Metric("最常穿", top?.let { entry -> state.garments.find { it.id == entry.key }?.name } ?: "—", Modifier.weight(1f))
            }
        }
        item { MonthCalendar(state) }
        item {
            Text("穿着频次与单次成本", style = MaterialTheme.typography.titleLarge)
            state.garments.sortedByDescending { it.wearCount }.take(8).forEach { garment ->
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    garment.photos.firstOrNull()?.let { AsyncImage(File(LocalContext.current.filesDir, it.relativePath), null, Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop) }
                    Spacer(Modifier.width(10.dp)); Text(garment.name, Modifier.weight(1f)); Text("${garment.wearCount} 次", color = Rose); Spacer(Modifier.width(12.dp)); Text(garment.costPerWearCents?.let { "¥%.2f/次".format(it / 100.0) } ?: "—", color = Mist)
                }
            }
        }
        item {
            Text("最近记录", style = MaterialTheme.typography.titleLarge)
            if (state.wearLogs.isEmpty()) Text("还没有穿着记录", color = Mist) else state.wearLogs.take(12).forEach { log ->
                val names = log.garmentIds.mapNotNull { id -> state.garments.find { it.id == id }?.name }
                Surface(color = WarmWhite, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) { Row(Modifier.padding(14.dp)) { Column(Modifier.weight(1f)) { Text(SimpleDateFormat("M月d日 E", Locale.CHINA).format(Date(log.wornAt)), fontWeight = FontWeight.SemiBold); Text("${log.scene} · ${names.joinToString(" + ")}", color = Mist) }; Text("满意 ${log.satisfaction}/5\n舒适 ${log.comfort}/5", style = MaterialTheme.typography.labelMedium, color = Rose) } }
            }
        }
        item {
            OutlinedButton(onClick = {
                val summary = "总单品=${state.garments.size};记录=${state.wearLogs.size};高频=" + state.garments.sortedByDescending { it.wearCount }.take(8).joinToString { "${it.name}:${it.wearCount}次" } + ";闲置=" + state.garments.filter { it.wearCount == 0 }.take(12).joinToString { it.name }
                reviewPreview = AiPreview(AiCapability.REVIEW, emptyList(), "依据本地统计摘要生成衣橱复盘，不发送原始照片", summary, "DeepSeek 文本模型按 token 计费")
            }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.AutoAwesome, null); Text("生成本月 AI 衣橱复盘") }
        }
        review?.let { item { Surface(color = WarmWhite, shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp)) { Text("本月复盘", fontWeight = FontWeight.SemiBold); Text(it) } } } }
    }
    if (recordDialog) WearRecordDialog(state, viewModel) { recordDialog = false }
    reviewPreview?.let { p -> SendPreviewDialog(p, { reviewPreview = null }, { reviewPreview = null; viewModel.deepSeekText(AiCapability.REVIEW, p.textSummary) { review = it } }) }
}

@Composable private fun MonthCalendar(state: UiSnapshot) {
    val cal = Calendar.getInstance(); val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH); cal.set(Calendar.DAY_OF_MONTH, 1); val offset = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    Surface(color = WarmWhite, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth()) { listOf("一", "二", "三", "四", "五", "六", "日").forEach { Text(it, Modifier.weight(1f), color = Mist) } }
            repeat(((offset + days + 6) / 7)) { week ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { weekday ->
                        val day = week * 7 + weekday - offset + 1
                        val worn = if (day in 1..days) state.wearLogs.any { log -> val c = Calendar.getInstance().apply { timeInMillis = log.wornAt }; c.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && c.get(Calendar.MONTH) == cal.get(Calendar.MONTH) && c.get(Calendar.DAY_OF_MONTH) == day } else false
                        Box(Modifier.weight(1f).height(42.dp), contentAlignment = Alignment.Center) { if (day in 1..days) { if (worn) Box(Modifier.size(32.dp).background(Rose.copy(alpha = .18f), CircleShape)); Text(day.toString(), color = if (day == today) Rose else Forest, fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal) } }
                    }
                }
            }
        }
    }
}

@Composable private fun WearRecordDialog(state: UiSnapshot, viewModel: WardrobeViewModel, onDismiss: () -> Unit) {
    val chosen = remember { mutableStateListOf<String>() }
    var outfitId by remember { mutableStateOf<String?>(null) }
    var scene by remember { mutableStateOf("通勤") }
    var satisfaction by remember { mutableIntStateOf(4) }
    var comfort by remember { mutableIntStateOf(4) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("记录今天穿了什么") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("从已保存造型") ; LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(state.outfits) { o -> FilterChip(outfitId == o.id, { outfitId = o.id; chosen.clear(); chosen.addAll(o.items.map { it.garmentId }) }, { Text(o.name) }) } } }
            item { Text("或临时选择单品") ; LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(state.garments.filter { it.status.wearable }) { g -> FilterChip(g.id in chosen, { if (g.id in chosen) chosen.remove(g.id) else chosen.add(g.id); outfitId = null }, { Text(g.name) }) } } }
            item { OutlinedTextField(scene, { scene = it }, label = { Text("场景") }, modifier = Modifier.fillMaxWidth()) }
            item { Text("满意度 $satisfaction/5"); Slider(satisfaction.toFloat(), { satisfaction = it.roundToInt() }, valueRange = 1f..5f, steps = 3); Text("舒适度 $comfort/5"); Slider(comfort.toFloat(), { comfort = it.roundToInt() }, valueRange = 1f..5f, steps = 3) }
        }
    }, confirmButton = { Button(onClick = { viewModel.addWearLog(chosen.toList(), outfitId, scene, satisfaction, comfort); onDismiss() }, enabled = chosen.isNotEmpty()) { Text("保存记录") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

@Composable
private fun SettingsScreen(viewModel: WardrobeViewModel, state: UiSnapshot, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    BackHandler(onBack = onBack)
    var deepKey by remember { mutableStateOf("") }
    var deepBase by rememberSaveable { mutableStateOf(viewModel.deepSeekBaseUrl()) }
    var visionModel by rememberSaveable { mutableStateOf(viewModel.deepSeekVisionModel()) }
    var textModel by rememberSaveable { mutableStateOf(viewModel.deepSeekTextModel()) }
    var bailianKey by remember { mutableStateOf("") }
    var workspace by remember { mutableStateOf(viewModel.workspaceId()) }
    var pendingToggle by remember { mutableStateOf<AiCapability?>(null) }
    var includeTryOn by remember { mutableStateOf(true) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var restoreSummary by remember { mutableStateOf<String?>(null) }
    var clearProvider by remember { mutableStateOf<AiProvider?>(null) }
    var notificationDenied by remember { mutableStateOf(false) }
    var confirmDeepSeekTest by remember { mutableStateOf(false) }
    val deepMaster = remember(state.revision) { viewModel.deepSeekEnabled() }
    val bailianMaster = remember(state.revision) { viewModel.bailianEnabled() }
    val capabilityStates = remember(state.revision) { AiCapability.entries.associateWith(viewModel::isEnabled) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> if (uri != null) viewModel.exportBackup(uri, includeTryOn) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) viewModel.validateBackup(uri) { validation -> restoreSummary = "${validation.message}\n衣物 ${validation.garments} · 造型 ${validation.outfits} · 记录 ${validation.wearLogs}"; if (validation.valid) restoreUri = uri } }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) viewModel.sendTestMaintenanceNotification() else notificationDenied = true }

    Scaffold(
        modifier = Modifier.fillMaxSize(), containerColor = Cream,
        topBar = { TopAppBar(title = { Text("设置") }, navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream)) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize().imePadding(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 60.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("AI 模型中心", style = MaterialTheme.typography.headlineSmall); Text("Key 由 Android Keystore 加密，只显示末四位，不进入 SQLite、备份、日志或 APK。BYOK 仍不等同服务端密钥托管。", color = Mist) }
            item {
                SettingsCard {
                    Row(Modifier.fillMaxWidth().clickable { viewModel.setDeepSeekEnabled(!deepMaster) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("DeepSeek 视觉搭配师", style = MaterialTheme.typography.titleLarge); Text(viewModel.maskedDeepSeek(), color = Mist) }; Switch(deepMaster, null) }
                    OutlinedTextField(deepKey, { deepKey = it }, label = { Text("API Key（留空则不修改）") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(deepBase, { deepBase = it }, label = { Text("API 地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(visionModel, { visionModel = it }, label = { Text("视觉模型 ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(textModel, { textModel = it }, label = { Text("文本模型 ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("当前官方视觉模型：deepseek-v4-flash-vision-exp。支持 JPEG、PNG、GIF、WebP；测试会发送下方确认框展示的内置 PNG 图标。", color = Mist, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { viewModel.saveDeepSeekConfig(deepKey, deepBase, visionModel, textModel); deepKey = "" }, modifier = Modifier.weight(1f)) { Text("安全保存") }; OutlinedButton(onClick = { confirmDeepSeekTest = true }, enabled = !state.busy, modifier = Modifier.weight(1f)) { Text("测试视觉连接") } }
                    Text("最近连接：${viewModel.lastDeepSeekTest()}", color = Mist, style = MaterialTheme.typography.labelMedium)
                    AiCapability.entries.filter { it.provider == AiProvider.DEEPSEEK }.forEach { cap -> CapabilitySwitch(cap, capabilityStates[cap] == true) { viewModel.setEnabled(cap, it) } }
                    TextButton(onClick = { clearProvider = AiProvider.DEEPSEEK }) { Text("删除 DeepSeek 配置", color = SoftRed) }
                }
            }
            item {
                SettingsCard {
                    Row(Modifier.fillMaxWidth().clickable { viewModel.setBailianEnabled(!bailianMaster) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("百炼 AI 试穿", style = MaterialTheme.typography.titleLarge); Text("${viewModel.maskedBailian()} · 华北2（北京）", color = Mist) }; Switch(bailianMaster, null) }
                    OutlinedTextField(bailianKey, { bailianKey = it }, label = { Text("百炼 API Key（留空则不修改）") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(workspace, { workspace = it }, label = { Text("Workspace ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Key 与 Workspace ID 必须属于同一业务空间。APP 使用用户 Key 自动申请临时上传凭证，不要求配置 OSS。", color = Mist)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { viewModel.saveBailianConfig(bailianKey, workspace); bailianKey = "" }, modifier = Modifier.weight(1f)) { Text("安全保存") }; OutlinedButton(onClick = { viewModel.testProvider(AiProvider.BAILIAN) }, modifier = Modifier.weight(1f)) { Text("测试连接") } }
                    Text("最近连接：${viewModel.lastBailianTest()}", color = Mist, style = MaterialTheme.typography.labelMedium)
                    AiCapability.entries.filter { it.provider == AiProvider.BAILIAN }.forEach { cap -> CapabilitySwitch(cap, capabilityStates[cap] == true) { enabled -> if (enabled) pendingToggle = cap else viewModel.setEnabled(cap, false) } }
                    Text("费用参考：aitryon ¥0.20/图，aitryon-plus ¥0.50/图；分割按输入计费；精修阶梯计费。官方页面对精修免费额度当前存在 400/100 张不一致，最终以你的控制台为准。APP 不代收费。", color = Rose, style = MaterialTheme.typography.labelMedium)
                    Text("shoemodel-v1 当前 500 张免费体验且不支持付费。只有 HTTP 403 + AllocationQuota.FreeTierOnly 或官方明确等价终止响应才会绑定当前 Key 指纹锁定；网络、限流、图片和 Workspace 错误不会误锁。", color = Mist, style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { clearProvider = AiProvider.BAILIAN }) { Text("删除百炼配置与能力开关", color = SoftRed) }
                }
            }
            item { Text("本地数据备份与恢复", style = MaterialTheme.typography.headlineSmall); Text("备份包含数据库、衣物照片、造型与记录；Key、临时上传凭证和调试日志始终排除。", color = Mist) }
            item {
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(includeTryOn, { includeTryOn = it }); Text("包含用户主动保存的试衣形象与结果") }
                    Button(onClick = { exportLauncher.launch("衣见备份-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())}.yijian.zip") }, modifier = Modifier.fillMaxWidth()) { Text("导出完整备份") }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) { Text("选择备份并先验证") }
                    restoreSummary?.let { Text(it, color = if (restoreUri != null) Forest else SoftRed) }
                }
            }
            item { Text("显示与无障碍", style = MaterialTheme.typography.titleLarge); Text("界面遵循系统字体缩放、手势安全区与动画时长；关闭系统动画时，生图扫光自动替换为静态占位。", color = Mist); OutlinedButton(onClick = { if (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) viewModel.sendTestMaintenanceNotification() else notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.fillMaxWidth()) { Text("测试衣物维护通知") }; if (notificationDenied) Text("通知权限已拒绝。衣橱仍可完整使用，可稍后在系统设置中手动开启。", color = SoftRed) }
            item { Text("隐私与免责声明", style = MaterialTheme.typography.titleLarge); Text("核心衣橱可完全离线使用。只有你逐次确认的选中图片和文字会发送给对应第三方：DeepSeek 或阿里云百炼。两家服务的 Key 与素材不会隐式互传。AI 试衣不代表真实尺码、垂坠、面料、颜色或舒适度。", color = Mist) }
        }
    }
    pendingToggle?.let { cap -> AlertDialog(onDismissRequest = { pendingToggle = null }, title = { Text("开启 ${cap.label}？") }, text = { Text(when (cap) {
        AiCapability.SHOE -> "官方当前提供 500 张免费体验，额度用尽后不支持付费。真正生成前仍会逐次确认。"
        AiCapability.TRYON_BASIC, AiCapability.TRYON_PLUS, AiCapability.TRYON_REFINER, AiCapability.TRYON_PARSING -> "免费额度结束或失效后，阿里云可能按官网价格向你的账号计费。开启开关本身不调用 API、不产生费用；每次生图前仍会逐次确认。"
        else -> "调用会把你确认的素材发送给 DeepSeek，并按账户实际规则计费。"
    }) }, confirmButton = { Button(onClick = { viewModel.setEnabled(cap, true); pendingToggle = null }) { Text("理解并开启") } }, dismissButton = { TextButton(onClick = { pendingToggle = null }) { Text("取消") } }) }
    if (confirmDeepSeekTest) AlertDialog(
        onDismissRequest = { confirmDeepSeekTest = false },
        title = { Text("确认测试 DeepSeek 视觉连接") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.ic_launcher_art), "本次实际发送的内置测试图片", Modifier.size(150.dp).clip(RoundedCornerShape(28.dp)))
                Text("服务商：DeepSeek\n模型：${viewModel.deepSeekVisionModel()}\n用途：验证 Key、模型 ID 与 PNG 图片解析\n费用：按图片和文本 token 计费；这不是离线测试。", color = Mist)
            }
        },
        confirmButton = { Button(onClick = { confirmDeepSeekTest = false; viewModel.testProvider(AiProvider.DEEPSEEK) }) { Text("确认发送并测试") } },
        dismissButton = { TextButton(onClick = { confirmDeepSeekTest = false }) { Text("取消") } }
    )
    if (restoreUri != null) AlertDialog(onDismissRequest = { restoreUri = null }, title = { Text("完整替换本地数据？") }, text = { Text("备份已通过校验。恢复会替换当前衣橱、造型和穿着记录；AI Key 不恢复。") }, confirmButton = { Button(onClick = { viewModel.restoreBackup(restoreUri!!); restoreUri = null }) { Text("确认恢复") } }, dismissButton = { TextButton(onClick = { restoreUri = null }) { Text("取消") } })
    clearProvider?.let { provider -> AlertDialog(onDismissRequest = { clearProvider = null }, title = { Text("删除 ${if (provider == AiProvider.DEEPSEEK) "DeepSeek" else "百炼"} 配置？") }, text = { Text("会清除本地加密 Key 和对应能力开关；衣橱、任务历史与已下载结果保留。") }, confirmButton = { TextButton(onClick = { viewModel.clearProvider(provider); clearProvider = null }) { Text("确认删除", color = SoftRed) } }, dismissButton = { TextButton(onClick = { clearProvider = null }) { Text("取消") } }) }
}

@Composable private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = WarmWhite, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }
}

@Composable private fun CapabilitySwitch(capability: AiCapability, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChecked(!checked) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(capability.label); Text(capability.model, color = Mist, style = MaterialTheme.typography.labelMedium) }; Switch(checked, null) }
}
