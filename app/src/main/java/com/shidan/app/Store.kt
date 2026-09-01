package com.shidan.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

/**
 * 全部数据就一个 JSON 文件。
 *
 * 没上 Room 是故意的：一个人几百条记录，SQLite 带来的复杂度换不回任何东西，
 * 而少一个注解处理器就少一整类编译不过的问题。真到了几千条再换不迟。
 */
object Store {

    private const val FILE_NAME = "shidan.json"

    private lateinit var appContext: Context

    /** Compose 直接读这两个，改了就会自动刷新界面 */
    var visits by mutableStateOf<List<Visit>>(emptyList())
        private set

    var wishes by mutableStateOf<List<Wish>>(emptyList())
        private set

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        load()
    }

    private fun file(): File = File(appContext.filesDir, FILE_NAME)

    private fun load() {
        val f = file()
        if (!f.exists()) return
        try {
            val root = JSONObject(f.readText())
            val vs = ArrayList<Visit>()
            root.optJSONArray("visits")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { vs.add(Visit.fromJson(it)) }
                }
            }
            val ws = ArrayList<Wish>()
            root.optJSONArray("wishes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { ws.add(Wish.fromJson(it)) }
                }
            }
            visits = vs
            wishes = ws
        } catch (e: Exception) {
            // 文件坏了就当空的开始，但把坏文件留一份，别直接毁尸灭迹
            try {
                f.copyTo(File(appContext.filesDir, "shidan-broken-${System.currentTimeMillis()}.json"), true)
            } catch (ignored: Exception) {
            }
        }
    }

    /** 写盘丢到单线程队列上，界面不等磁盘。单线程保证先后顺序，不会写串 */
    private val writer = Executors.newSingleThreadExecutor { r ->
        Thread(r, "shidan-store").apply { isDaemon = true }
    }

    private fun persist() {
        val snapshotVisits = visits
        val snapshotWishes = wishes
        writer.execute { writeToDisk(snapshotVisits, snapshotWishes) }
    }

    private fun writeToDisk(vs: List<Visit>, ws: List<Wish>) {
        try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("visits", JSONArray().apply { vs.forEach { put(it.toJson()) } })
            root.put("wishes", JSONArray().apply { ws.forEach { put(it.toJson()) } })
            // 先写临时文件再改名，中途被杀掉也不会留下半个文件
            val tmp = File(appContext.filesDir, "$FILE_NAME.tmp")
            tmp.writeText(root.toString())
            if (!tmp.renameTo(file())) {
                file().writeText(root.toString())
                tmp.delete()
            }
        } catch (e: Exception) {
            // 存不下就存不下，界面上的数据还在，下次操作会再试
        }
    }

    // ---------- 记录 ----------

    fun upsertVisit(v: Visit) {
        val stamped = v.copy(updated = System.currentTimeMillis())
        val idx = visits.indexOfFirst { it.id == stamped.id }
        visits = if (idx >= 0) {
            visits.toMutableList().also { it[idx] = stamped }
        } else {
            visits + stamped
        }
        persist()
    }

    fun deleteVisit(id: String) {
        val v = visits.firstOrNull { it.id == id }
        visits = visits.filter { it.id != id }
        persist()
        val doomed = v?.photos ?: emptyList()
        if (doomed.isNotEmpty()) {
            writer.execute { doomed.forEach { Photos.delete(appContext, it) } }
        }
    }

    fun visit(id: String?): Visit? = if (id == null) null else visits.firstOrNull { it.id == id }

    /** 新的在上面 */
    fun visitsSorted(): List<Visit> =
        visits.sortedWith(compareByDescending<Visit> { it.date }.thenByDescending { it.created })

    // ---------- 想去 ----------

    fun addWish(w: Wish) {
        wishes = wishes + w
        persist()
    }

    fun deleteWish(id: String) {
        wishes = wishes.filter { it.id != id }
        persist()
    }

    fun wishesSorted(): List<Wish> = wishes.sortedByDescending { it.created }

    // ---------- 收拾没人认领的照片 ----------

    /** 每次启动扫一遍：文件夹里有、但没有任何记录用到的照片，删掉 */
    fun sweepOrphanPhotos() {
        val used = HashSet<String>()
        visits.forEach { used.addAll(it.photos) }
        writer.execute {
            try {
                Photos.dir(appContext).listFiles()?.forEach { f ->
                    if (f.isFile && f.name !in used) f.delete()
                }
            } catch (ignored: Exception) {
            }
        }
    }
}
