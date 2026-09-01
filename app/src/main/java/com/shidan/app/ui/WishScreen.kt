package com.shidan.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shidan.app.Again
import com.shidan.app.Store
import com.shidan.app.Wish

@Composable
fun WishScreen(onCook: (String) -> Unit) {
    val p = LocalPalette.current
    val wishes = Store.wishesSorted()

    var draft by remember { mutableStateOf("") }
    var rolled by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 40.dp)
    ) {
        item {
            Text(
                text = "想去",
                color = p.ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Eyebrow("刷到什么先存着")
        }

        // 今晚吃啥
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(p.card)
                    .clickable { rolled = pickRandom() }
                    .padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Eyebrow("今晚吃啥")
                val out = rolled
                Text(
                    text = out ?: "点一下随机抽一个",
                    color = if (out == null) p.inkSoft else p.ink,
                    fontSize = if (out == null) 14.sp else 20.sp,
                    fontWeight = if (out == null) FontWeight.Normal else FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, start = 16.dp, end = 16.dp)
                )
                if (out != null) {
                    Text(
                        text = "从想去清单和你标过「还会来」的店里抽",
                        color = p.inkSoft,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // 加一条
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("小红书刷到的那家", color = p.inkSoft, fontSize = 14.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(p.pine)
                        .clickable {
                            val n = draft.trim()
                            if (n.isNotEmpty()) {
                                Store.addWish(Wish(name = n))
                                draft = ""
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Text("加", color = p.onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item { SectionHead("清单", if (wishes.isEmpty()) null else "${wishes.size} 家") }

        if (wishes.isEmpty()) {
            item { EmptyNote("空的。\n以后想吃什么先丢进来，省得到点了想不起来。") }
        }

        items(wishes, key = { it.id }) { w ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 9.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(p.card)
                    .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = w.name,
                    color = p.ink,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    Store.deleteWish(w.id)
                    onCook(w.name)
                }) {
                    Text("去过了", color = p.pine, fontSize = 13.sp)
                }
                TextButton(onClick = { Store.deleteWish(w.id) }) {
                    Text("删", color = p.inkSoft, fontSize = 13.sp)
                }
            }
        }
    }
}

/** 想去清单 + 打过「还会来」的店，去重后随机一个 */
private fun pickRandom(): String {
    val pool = LinkedHashSet<String>()
    Store.wishes.forEach { pool.add(it.name) }
    Store.visits.forEach { v ->
        if (v.rating?.again == Again.YES && v.name.isNotBlank()) pool.add(v.name)
    }
    if (pool.isEmpty()) return "清单是空的，先加两家"
    return pool.random()
}
