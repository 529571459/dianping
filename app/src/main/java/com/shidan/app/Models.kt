package com.shidan.app

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 单人版的数据模型。
 *
 * 之所以每条都带 id 和 updated，是为了以后加"两个人互通"时不用改结构：
 * 到时候只要给 Rating 加一个 authorId，一条记录挂两份评分就行。
 */

enum class Again(val key: String, val label: String) {
    YES("yes", "还会来"),
    MAYBE("maybe", "看情况"),
    NO("no", "不来了");

    companion object {
        fun from(key: String?): Again = values().firstOrNull { it.key == key } ?: MAYBE
    }
}

data class Rating(
    val taste: Float = 7f,
    val vibe: Float = 7f,
    val serve: Float = 7f,
    val worth: Float = 7f,
    val again: Again = Again.MAYBE,
    val note: String = ""
) {
    val overall: Float get() = (taste + vibe + serve + worth) / 4f

    fun toJson(): JSONObject = JSONObject().apply {
        put("taste", taste.toDouble())
        put("vibe", vibe.toDouble())
        put("serve", serve.toDouble())
        put("worth", worth.toDouble())
        put("again", again.key)
        put("note", note)
    }

    companion object {
        fun fromJson(o: JSONObject): Rating = Rating(
            taste = o.optDouble("taste", 7.0).toFloat(),
            vibe = o.optDouble("vibe", 7.0).toFloat(),
            serve = o.optDouble("serve", 7.0).toFloat(),
            worth = o.optDouble("worth", 7.0).toFloat(),
            again = Again.from(o.optString("again", "maybe")),
            note = o.optString("note", "")
        )
    }
}

data class Visit(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    /** yyyy-MM-dd */
    val date: String = "",
    val cost: String = "",
    val area: String = "",
    val items: String = "",
    val tags: String = "",
    /** 照片文件名，文件躺在 filesDir/photos/ 下面 */
    val photos: List<String> = emptyList(),
    val rating: Rating? = null,
    val created: Long = System.currentTimeMillis(),
    val updated: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("date", date)
        put("cost", cost)
        put("area", area)
        put("items", items)
        put("tags", tags)
        put("photos", JSONArray(photos))
        put("rating", rating?.toJson() ?: JSONObject.NULL)
        put("created", created)
        put("updated", updated)
    }

    companion object {
        fun fromJson(o: JSONObject): Visit {
            val arr = o.optJSONArray("photos")
            val pics = ArrayList<String>()
            if (arr != null) for (i in 0 until arr.length()) pics.add(arr.optString(i))
            val r = o.optJSONObject("rating")
            return Visit(
                id = o.optString("id", UUID.randomUUID().toString()),
                name = o.optString("name", ""),
                date = o.optString("date", ""),
                cost = o.optString("cost", ""),
                area = o.optString("area", ""),
                items = o.optString("items", ""),
                tags = o.optString("tags", ""),
                photos = pics,
                rating = if (r != null) Rating.fromJson(r) else null,
                created = o.optLong("created", 0L),
                updated = o.optLong("updated", 0L)
            )
        }
    }
}

data class Wish(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val note: String = "",
    val created: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("note", note)
        put("created", created)
    }

    companion object {
        fun fromJson(o: JSONObject): Wish = Wish(
            id = o.optString("id", UUID.randomUUID().toString()),
            name = o.optString("name", ""),
            note = o.optString("note", ""),
            created = o.optLong("created", 0L)
        )
    }
}

/** 把"毛血旺 冰啤酒, 凉拌木耳"这种随手输入切成一个个词 */
fun splitWords(raw: String): List<String> =
    raw.split(' ', ',', '，', '、', '\n', '\t')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

fun formatScore(v: Float?): String =
    if (v == null) "—" else String.format(java.util.Locale.US, "%.1f", v)
