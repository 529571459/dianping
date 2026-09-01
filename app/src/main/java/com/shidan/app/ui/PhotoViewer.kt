package com.shidan.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shidan.app.Photos
import com.shidan.app.Store
import com.shidan.app.Visit

/** 全屏看图：左右翻页，可以单张删除 */
@Composable
fun PhotoViewer(
    visit: Visit,
    startIndex: Int,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var current by remember { mutableStateOf(Store.visit(visit.id) ?: visit) }
    var index by remember { mutableStateOf(startIndex.coerceIn(0, maxOf(0, current.photos.size - 1))) }

    // 最后一张被删掉时收工。放在 LaunchedEffect 里，别在组合过程中改父级状态。
    LaunchedEffect(current.photos.isEmpty()) {
        if (current.photos.isEmpty()) onClose()
    }
    if (current.photos.isEmpty()) return

    val safeIndex = index.coerceIn(0, current.photos.size - 1)
    val bitmap = rememberPhoto(current.photos[safeIndex], 1600)
    val tint = Color(0xFFEDEAE1)

    BackHandler { onClose() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF00A0D0C)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = current.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("照片读不出来了", color = tint, fontSize = 13.sp)
                }
            }

            Row2(
                left = {
                    if (current.photos.size > 1) {
                        TextButton(onClick = {
                            index = (safeIndex - 1 + current.photos.size) % current.photos.size
                        }) { Text("上一张", color = tint, fontSize = 13.sp) }
                    }
                },
                middle = {
                    Text(
                        "${safeIndex + 1} / ${current.photos.size}",
                        color = tint.copy(alpha = 0.62f),
                        fontSize = 12.sp
                    )
                },
                right = {
                    if (current.photos.size > 1) {
                        TextButton(onClick = {
                            index = (safeIndex + 1) % current.photos.size
                        }) { Text("下一张", color = tint, fontSize = 13.sp) }
                    }
                }
            )

            Row2(
                left = {
                    TextButton(onClick = onClose) { Text("关闭", color = tint, fontSize = 13.sp) }
                },
                middle = {},
                right = {
                    TextButton(onClick = {
                        val name = current.photos[safeIndex]
                        val remaining = current.photos.filter { it != name }
                        val updated = current.copy(photos = remaining)
                        Store.upsertVisit(updated)
                        Photos.delete(context, name)
                        if (remaining.isEmpty()) {
                            onClose()
                        } else {
                            current = updated
                            index = safeIndex.coerceAtMost(remaining.size - 1)
                        }
                    }) { Text("删掉这张", color = Color(0xFFDC8189), fontSize = 13.sp) }
                }
            )

            Box(modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun Row2(
    left: @Composable () -> Unit,
    middle: @Composable () -> Unit,
    right: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.CenterStart) { left() }
        Box(contentAlignment = Alignment.Center) { middle() }
        Box(contentAlignment = Alignment.CenterEnd) { right() }
    }
}
