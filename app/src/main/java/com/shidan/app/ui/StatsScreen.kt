package com.shidan.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shidan.app.Again
import com.shidan.app.Store
import com.shidan.app.formatScore
import com.shidan.app.splitWords
import kotlin.math.roundToInt

@Composable
fun StatsScreen() {
    val p = LocalPalette.current
    val visits = Store.visits

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 40.dp)
    ) {
        item {
            Text("统计", color = p.ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Eyebrow("吃了这么多")
        }

        if (visits.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(top = 30.dp)) {
                    EmptyNote("记满几顿之后这里才有东西看。")
                }
            }
            return@LazyColumn
        }

        val rated = visits.filter { it.rating != null }
        val avg = if (rated.isEmpty()) null else rated.map { it.rating!!.overall }.average().toFloat()
        val costs = visits.mapNotNull { it.cost.trim().toDoubleOrNull() }
        val places = visits.groupingBy { it.name }.eachCount()
        val photoCount = visits.sumOf { it.photos.size }

        item {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile("${visits.size}", "顿", "总记录", Modifier.weight(1f))
                StatTile("${places.size}", "家", "去过的店", Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    if (costs.isEmpty()) "—" else "¥${(costs.average()).roundToInt()}",
                    null, "平均人均", Modifier.weight(1f)
                )
                StatTile("$photoCount", "张", "拍下来的", Modifier.weight(1f))
            }
        }

        if (avg != null) {
            item {
                SectionHead("你的平均分", "${rated.size} 次打分")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(p.card)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatScore(avg),
                            color = p.pine,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "  / 10",
                            color = p.inkSoft,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(p.card2)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(avg / 10f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(p.pine)
                        )
                    }
                    Text(
                        text = when {
                            avg >= 8.5f -> "你是个好说话的食客。"
                            avg >= 7f -> "标准的中间派。"
                            avg >= 5.5f -> "要求不低。"
                            else -> "很难伺候，但至少诚实。"
                        },
                        color = p.inkSoft,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }

        // 最高分
        val best = rated.sortedByDescending { it.rating!!.overall }.take(3)
        if (best.isNotEmpty()) {
            item { SectionHead("打分最高的") }
            item {
                RankBox(best.map { v ->
                    v.name to formatScore(v.rating!!.overall)
                })
            }
        }

        // 回头率
        val repeats = places.filter { it.value > 1 }.toList().sortedByDescending { it.second }.take(5)
        if (repeats.isNotEmpty()) {
            item { SectionHead("回头率") }
            item { RankBox(repeats.map { it.first to "去过 ${it.second} 次" }) }
        }

        // 还会来
        val comeback = visits.filter { it.rating?.again == Again.YES }
            .map { it.name }.distinct().take(8)
        if (comeback.isNotEmpty()) {
            item { SectionHead("标了「还会来」") }
            item { RankBox(comeback.map { it to "" }) }
        }

        // 最常点的菜
        val dishes = HashMap<String, Int>()
        visits.forEach { v -> splitWords(v.items).forEach { d -> dishes[d] = (dishes[d] ?: 0) + 1 } }
        val topDishes = dishes.filter { it.value > 1 }.toList().sortedByDescending { it.second }.take(5)
        if (topDishes.isNotEmpty()) {
            item { SectionHead("反复点的") }
            item { RankBox(topDishes.map { it.first to "${it.second} 次" }) }
        }
    }
}

@Composable
private fun StatTile(value: String, unit: String?, label: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(p.card)
            .padding(horizontal = 15.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = p.ink, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            if (unit != null) {
                Text(
                    text = " $unit",
                    color = p.inkSoft,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
        Text(label, color = p.inkSoft, fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun RankBox(rows: List<Pair<String, String>>) {
    val p = LocalPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.card)
    ) {
        rows.forEachIndexed { i, row ->
            if (i > 0) HairLine()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(row.first, color = p.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                if (row.second.isNotBlank()) {
                    Text(row.second, color = p.inkSoft, fontSize = 12.5.sp)
                }
            }
        }
    }
}
