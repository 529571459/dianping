package com.shidan.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.shidan.app.Again
import com.shidan.app.Photos
import com.shidan.app.Rating
import com.shidan.app.Store
import com.shidan.app.Visit
import com.shidan.app.formatScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_PHOTOS = 9

@Composable
fun EditVisitScreen(
    initial: Visit,
    onClose: () -> Unit,
    onSaved: () -> Unit
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isNew = Store.visit(initial.id) == null

    // 用 rememberSaveable：转屏、切后台被系统回收，填了一半的东西还在
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var date by rememberSaveable { mutableStateOf(if (initial.date.isBlank()) todayIso() else initial.date) }
    var cost by rememberSaveable { mutableStateOf(initial.cost) }
    var area by rememberSaveable { mutableStateOf(initial.area) }
    var items by rememberSaveable { mutableStateOf(initial.items) }
    var tags by rememberSaveable { mutableStateOf(initial.tags) }

    val photos = remember { initial.photos.toMutableStateList() }
    /** 这次新拍/新选的，取消时要删掉，别在磁盘上留垃圾 */
    val freshPhotos = remember { mutableListOf<String>() }
    /** 界面上撤掉的，等真的保存了才动磁盘——不然取消之后记录会指向一个不存在的文件 */
    val removedPhotos = remember { mutableListOf<String>() }

    val start = initial.rating ?: Rating()
    var taste by rememberSaveable { mutableStateOf(start.taste) }
    var vibe by rememberSaveable { mutableStateOf(start.vibe) }
    var serve by rememberSaveable { mutableStateOf(start.serve) }
    var worth by rememberSaveable { mutableStateOf(start.worth) }
    var againKey by rememberSaveable { mutableStateOf(start.again.key) }
    var note by rememberSaveable { mutableStateOf(start.note) }
    var rated by rememberSaveable { mutableStateOf(initial.rating != null) }

    val again = Again.from(againKey)

    var busy by remember { mutableStateOf(false) }
    // 拍照期间进程可能被回收，这个也得存住，否则拍完的照片会被丢掉
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        val fileName = pendingCapture
        pendingCapture = null
        if (fileName == null) return@rememberLauncherForActivityResult
        if (!ok) {
            Photos.delete(context, fileName)
            return@rememberLauncherForActivityResult
        }
        busy = true
        scope.launch {
            val good = withContext(Dispatchers.IO) { Photos.finishCapture(context, fileName) }
            busy = false
            if (good) {
                photos.add(fileName)
                freshPhotos.add(fileName)
            } else {
                Toast.makeText(context, "这张没存下来", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val room = (MAX_PHOTOS - photos.size).coerceAtLeast(0)
            val wanted = uris.take(room)
            val added = withContext(Dispatchers.IO) {
                wanted.mapNotNull { Photos.importFrom(context, it) }
            }
            busy = false
            photos.addAll(added)
            freshPhotos.addAll(added)
            val skipped = uris.size - wanted.size
            val failed = wanted.size - added.size
            when {
                skipped > 0 -> Toast.makeText(context, "最多 $MAX_PHOTOS 张，多的没要", Toast.LENGTH_SHORT).show()
                failed > 0 -> Toast.makeText(context, "有 $failed 张读不出来", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun discardAndClose() {
        // 只清掉这次新加的；被撤下来的老照片留着，因为这条记录还是原样
        freshPhotos.forEach { Photos.delete(context, it) }
        onClose()
    }

    BackHandler { discardAndClose() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.paper)
    ) {
        // 顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isNew) "记一顿" else "编辑",
                color = p.ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { discardAndClose() }) {
                Text("取消", color = p.inkSoft, fontSize = 13.5.sp)
            }
        }
        HairLine()

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            Field(value = name, onChange = { name = it }, label = "店名 / 这顿叫什么", placeholder = "老陈牛肉面")

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    Field(value = date, onChange = { date = it }, label = "日期", placeholder = "2026-08-30")
                }
                Box(modifier = Modifier.weight(1f)) {
                    Field(
                        value = cost, onChange = { cost = it }, label = "人均 ¥",
                        placeholder = "168", numeric = true
                    )
                }
            }

            Row(modifier = Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickDate("今天") { date = isoOf(0) }
                QuickDate("昨天") { date = isoOf(1) }
                QuickDate("前天") { date = isoOf(2) }
            }

            Field(
                value = area, onChange = { area = it },
                label = "地址 / 在哪儿",
                placeholder = "东四十条 22 号 · 填得细一点，以后能一键点开地图"
            )
            Field(value = items, onChange = { items = it }, label = "点了什么", placeholder = "毛血旺 冰啤酒 凉拌木耳")
            Field(value = tags, onChange = { tags = it }, label = "标签", placeholder = "适合聊天 太吵 贵但值")

            // ---------- 照片 ----------
            SectionHead("照片", if (photos.isEmpty()) null else "${photos.size} / $MAX_PHOTOS")

            PhotoRow(
                photos = photos,
                busy = busy,
                onRemove = { fileName ->
                    photos.remove(fileName)
                    if (freshPhotos.remove(fileName)) {
                        // 刚拍的，还没进任何记录，直接删干净
                        Photos.delete(context, fileName)
                    } else {
                        // 老照片，等保存时再动手
                        removedPhotos.add(fileName)
                    }
                },
                onCamera = {
                    if (photos.size >= MAX_PHOTOS) {
                        Toast.makeText(context, "最多 $MAX_PHOTOS 张", Toast.LENGTH_SHORT).show()
                    } else {
                        val (fileName, uri) = Photos.newCaptureTarget(context)
                        pendingCapture = fileName
                        try {
                            cameraLauncher.launch(uri)
                        } catch (e: Exception) {
                            pendingCapture = null
                            Photos.delete(context, fileName)
                            Toast.makeText(context, "这台手机没有能用的相机", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onPick = {
                    if (photos.size >= MAX_PHOTOS) {
                        Toast.makeText(context, "最多 $MAX_PHOTOS 张", Toast.LENGTH_SHORT).show()
                    } else {
                        pickLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }
            )

            // ---------- 评分 ----------
            SectionHead("打分")

            if (!rated) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.card2)
                        .clickable { rated = true }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("现在打分", color = p.pine, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = "也可以先记下来，回头再补。",
                    color = p.inkSoft,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                ScoreSlider("味道", taste) { taste = it }
                ScoreSlider("环境", vibe) { vibe = it }
                ScoreSlider("服务", serve) { serve = it }
                ScoreSlider("性价比", worth) { worth = it }

                Text(
                    text = "还会再来吗",
                    color = p.inkSoft,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Again.values().forEach { option ->
                        val selected = again == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) p.pine else p.card2)
                                .clickable { againKey = option.key }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.label,
                                color = if (selected) p.onAccent else p.inkSoft,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Field(
                    value = note, onChange = { note = it },
                    label = "一句话", placeholder = "汤底真行，服务员脸有点臭", tall = true
                )
            }

            Spacer(modifier = Modifier.height(26.dp))
        }

        // 底部按钮
        HairLine()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { discardAndClose() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = p.card2,
                    contentColor = p.ink
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) { Text("取消") }

            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "至少写个名字", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (busy) {
                        Toast.makeText(context, "照片还在压缩，等一秒", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    Store.upsertVisit(
                        initial.copy(
                            name = name.trim(),
                            date = date.trim().ifBlank { todayIso() },
                            cost = cost.trim(),
                            area = area.trim(),
                            items = items.trim(),
                            tags = tags.trim(),
                            photos = photos.toList(),
                            rating = if (rated) {
                                Rating(taste, vibe, serve, worth, again, note.trim())
                            } else {
                                null
                            }
                        )
                    )
                    // 记录落盘了，这时候删文件才安全
                    removedPhotos.forEach { Photos.delete(context, it) }
                    onSaved()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = p.pine,
                    contentColor = p.onAccent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) { Text("保存", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    numeric: Boolean = false,
    tall: Boolean = false
) {
    val p = LocalPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(placeholder, color = p.inkSoft, fontSize = 14.sp) },
        singleLine = !tall,
        minLines = if (tall) 2 else 1,
        keyboardOptions = if (numeric) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    )
}

@Composable
private fun QuickDate(label: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(p.card2)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(label, color = p.inkSoft, fontSize = 12.sp)
    }
}

@Composable
private fun ScoreSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = p.inkSoft, fontSize = 13.sp, modifier = Modifier.width(54.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 1f..10f,
            steps = 17,
            colors = SliderDefaults.colors(
                thumbColor = p.pine,
                activeTrackColor = p.pine,
                inactiveTrackColor = p.card2
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatScore(value),
            color = p.ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(38.dp)
        )
    }
}

@Composable
private fun PhotoRow(
    photos: List<String>,
    busy: Boolean,
    onRemove: (String) -> Unit,
    onCamera: () -> Unit,
    onPick: () -> Unit
) {
    val p = LocalPalette.current

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ActionTile(
                text = if (busy) "处理中…" else "拍照",
                enabled = !busy,
                onClick = onCamera,
                modifier = Modifier.weight(1f)
            )
            ActionTile(
                text = "从相册选",
                enabled = !busy,
                onClick = onPick,
                modifier = Modifier.weight(1f)
            )
        }

        if (photos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            // 一行四个，够 9 张排两行多
            photos.chunked(4).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { fileName ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(p.card2)
                        ) {
                            val thumb = rememberPhoto(fileName, 400)
                            if (thumb != null) {
                                Image(
                                    bitmap = thumb,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp)
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0xA80C100E))
                                    .clickable { onRemove(fileName) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("×", color = Color(0xFFF2F0E9), fontSize = 13.sp)
                            }
                        }
                    }
                    // 补齐空位，免得三张图被拉宽
                    repeat(4 - rowItems.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionTile(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalPalette.current
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(p.card2)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) p.ink else p.inkSoft,
            fontSize = 13.5.sp
        )
    }
}
