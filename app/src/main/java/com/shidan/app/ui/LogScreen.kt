package com.shidan.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shidan.app.Store
import com.shidan.app.Visit
import com.shidan.app.formatScore
import com.shidan.app.splitWords
import kotlin.math.abs

@Composable
fun LogScreen(
    onNew: () -> Unit,
    onEdit: (Visit) -> Unit
) {
    val palette = LocalPalette.current
    val list = Store.visitsSorted()

    var confirmDelete by remember { mutableStateOf<Visit?>(null) }
    var gallery by remember { mutableStateOf<Pair<Visit, Int>?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "食单",
                        color = palette.ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Eyebrow(
                        if (list.isEmpty()) "一个人的吃喝账本"
                        else "记了 ${list.size} 顿"
                    )
                }
            }

            if (list.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(top = 40.dp)) {
                        EmptyNote("还没有记录。\n点右下角的 ＋ 记下第一顿。")
                    }
                }
            }

            items(list, key = { it.id }) { visit ->
                VisitCard(
                    visit = visit,
                    onEdit = { onEdit(visit) },
                    onDelete = { confirmDelete = visit },
                    onOpenPhoto = { index -> gallery = visit to index }
                )
            }
        }

        FloatingActionButton(
            onClick = onNew,
            containerColor = palette.pine,
            contentColor = palette.onAccent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(56.dp)
        ) {
            Text("＋", fontSize = 24.sp)
        }
    }

    val toDelete = confirmDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删掉这条？") },
            text = {
                Text("「${toDelete.name}」${prettyDate(toDelete.date)}，连同评分和照片一起删掉，删了找不回来。")
            },
            confirmButton = {
                TextButton(onClick = {
                    Store.deleteVisit(toDelete.id)
                    confirmDelete = null
                }) { Text("删掉", color = palette.plum) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("算了") }
            },
            containerColor = palette.card,
            titleContentColor = palette.ink,
            textContentColor = palette.inkSoft
        )
    }

    val open = gallery
    if (open != null) {
        PhotoViewer(
            visit = Store.visit(open.first.id) ?: open.first,
            startIndex = open.second,
            onClose = { gallery = null }
        )
    }
}

@Composable
private fun VisitCard(
    visit: Visit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenPhoto: (Int) -> Unit
) {
    val p = LocalPalette.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.card)
    ) {
        if (visit.photos.isNotEmpty()) {
            val cover = rememberPhoto(visit.photos.first(), 900)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 2f)
                    .background(p.card2)
                    .clickable { onOpenPhoto(0) }
            ) {
                if (cover != null) {
                    Image(
                        bitmap = cover,
                        contentDescription = "${visit.name}的照片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (visit.photos.size > 1) {
                    Text(
                        text = "${visit.photos.size} 张",
                        color = Color(0xFFF2F0E9),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(9.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xA60C100E))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)) {

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = prettyDate(visit.date),
                    color = p.inkSoft,
                    fontSize = 11.5.sp,
                    modifier = Modifier.weight(1f)
                )
                if (visit.cost.isNotBlank()) {
                    Text("¥${visit.cost}/人", color = p.inkSoft, fontSize = 11.5.sp)
                }
            }

            Text(
                text = visit.name,
                color = p.ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (visit.area.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { openInMap(context, visit.name, visit.area) }
                        .padding(vertical = 2.dp, horizontal = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(visit.area, color = p.inkSoft, fontSize = 12.sp)
                    Text(
                        text = "  地图 ↗",
                        color = p.pine,
                        fontSize = 11.sp
                    )
                }
            }

            val dishes = splitWords(visit.items)
            if (dishes.isNotEmpty()) {
                Text(
                    text = dishes.joinToString("  ·  "),
                    color = p.inkSoft,
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            val tags = splitWords(visit.tags)
            if (tags.isNotEmpty()) {
                Text(
                    text = tags.joinToString(" ") { "#$it" },
                    color = p.pine,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            HairLine(modifier = Modifier.padding(top = 12.dp))

            val r = visit.rating
            if (r == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(p.card2)
                        .clickable { onEdit() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("还没打分，点一下补上", color = p.inkSoft, fontSize = 13.sp)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatScore(r.overall),
                        color = p.pine,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(r.again.label, color = p.ink, fontSize = 13.sp)
                        Text(
                            text = "味 ${trim(r.taste)} · 环 ${trim(r.vibe)} · " +
                                "服 ${trim(r.serve)} · 价 ${trim(r.worth)}",
                            color = p.inkSoft,
                            fontSize = 11.5.sp
                        )
                    }
                }

                val spread = spreadOf(r.taste, r.vibe, r.serve, r.worth)
                if (spread >= 3f) {
                    Text(
                        text = "评价两极：最高和最低差 ${formatScore(spread)} 分",
                        color = p.gold,
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (r.note.isNotBlank()) {
                    Row(modifier = Modifier.padding(top = 10.dp)) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(if (r.note.length > 24) 40.dp else 20.dp)
                                .background(p.pine)
                        )
                        Text(
                            text = r.note,
                            color = p.ink,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                }
            }

            Row(modifier = Modifier.padding(top = 8.dp)) {
                TextButton(onClick = onEdit) {
                    Text("编辑", color = p.inkSoft, fontSize = 12.5.sp)
                }
                TextButton(onClick = onDelete) {
                    Text("删除", color = p.inkSoft, fontSize = 12.5.sp)
                }
            }
        }
    }
}

private fun trim(v: Float): String =
    if (v == v.toInt().toFloat()) v.toInt().toString() else formatScore(v)

private fun spreadOf(a: Float, b: Float, c: Float, d: Float): Float {
    val mx = maxOf(a, b, c, d)
    val mn = minOf(a, b, c, d)
    return abs(mx - mn)
}
