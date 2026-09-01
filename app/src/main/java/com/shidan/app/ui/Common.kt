package com.shidan.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shidan.app.Photos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

/**
 * 读一张照片。解码放在 IO 线程，界面不会卡。
 * maxSide 按用途给：列表封面 900 够了，全屏看图给 1600。
 */
@Composable
fun rememberPhoto(name: String, maxSide: Int): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(name, maxSide) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(name, maxSide) {
        bitmap = withContext(Dispatchers.IO) { Photos.load(context, name, maxSide) }
    }
    return bitmap?.asImageBitmap()
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Text(
        text = text,
        color = p.inkSoft,
        fontSize = 11.sp,
        letterSpacing = 1.4.sp,
        modifier = modifier
    )
}

@Composable
fun SectionHead(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            color = LocalPalette.current.ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) Eyebrow(trailing)
    }
}

@Composable
fun HairLine(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalPalette.current.line)
    )
}

@Composable
fun EmptyNote(text: String) {
    val p = LocalPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(p.card2, RoundedCornerShape(14.dp))
            .padding(horizontal = 22.dp, vertical = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = p.inkSoft,
            fontSize = 13.5.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ---------- 日期 ----------

fun todayIso(): String = isoOf(0)

fun isoOf(daysAgo: Int): String {
    val c = Calendar.getInstance()
    c.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return String.format(
        Locale.US, "%04d-%02d-%02d",
        c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
    )
}

/** 2026-08-30 → 26.08.30 周日 */
fun prettyDate(iso: String): String {
    val parts = iso.split("-")
    if (parts.size != 3) return iso
    return try {
        val y = parts[0].toInt()
        val m = parts[1].toInt()
        val d = parts[2].toInt()
        val c = Calendar.getInstance()
        c.set(y, m - 1, d, 12, 0, 0)
        val week = when (c.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            Calendar.SATURDAY -> "周六"
            else -> "周日"
        }
        String.format(Locale.US, "%02d.%02d.%02d ", y % 100, m, d) + week
    } catch (e: Exception) {
        iso
    }
}

/**
 * 用地址去调系统里的地图 app（高德、百度、Google Maps，装了哪个用哪个）。
 *
 * 走的是标准的 geo: 协议，所以不需要任何地图 SDK、不需要申请 key、
 * 不需要定位权限，也就一分钱不花。地址是什么就搜什么。
 */
fun openInMap(context: Context, name: String, address: String) {
    val query = address.ifBlank { name }.trim()
    if (query.isEmpty()) return
    try {
        val uri = Uri.parse("geo:0,0?q=" + Uri.encode(query))
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: Exception) {
        Toast.makeText(context, "手机上好像没装地图 app", Toast.LENGTH_SHORT).show()
    }
}
