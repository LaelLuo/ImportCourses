@file:Suppress("DEPRECATION")

package com.github.laelluo.importcourses

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.provider.CalendarContract
import android.support.design.widget.Snackbar
import android.view.View
import android.widget.Toast
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.QueryListener
import cn.bmob.v3.listener.SaveListener
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.*
import android.content.Intent
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.TextView
import okhttp3.*
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

fun Activity.waitDialog(title: String, message: String) =
        ProgressDialog(this).apply {
            setTitle(title)
            setMessage(message)
            setCancelable(false)
            show()
        }

fun toast(text: String, time: Int = Toast.LENGTH_SHORT) =
        Toast.makeText(MyApplication.context, text, time).show()

fun View.snackbar(text: String, actionText: String = "", time: Int = Snackbar.LENGTH_INDEFINITE, callback: () -> Unit = {}) =
        Snackbar.make(this, text, time).apply { if (!actionText.isEmpty()) setAction(actionText) { callback() } }.show()

fun Activity.login(username: String, password: String, function: () -> Unit) =
        launch(UI) {
            Data.username = username
            Data.password = password
            val dialog = waitDialog(getString(R.string.signning), getString(R.string.loading))
            initTessFile()
            try {
                val result = withContext(DefaultDispatcher) { getCookie() }
                dialog.cancel()
                when (result) {
                    0 -> function()
                    1 -> toast("用户名或密码错误")
                    2 -> toast("未知错误")
                }
            } catch (e: SocketTimeoutException) {
                toast(getString(R.string.timeout))
            } catch (e: ConnectException) {
                toast(getString(R.string.net_error))
            } finally {
                dialog.cancel()
            }
        }

private fun gray2Binary(graymap: Bitmap): Bitmap {
    //得到图形的宽度和长度
    val width = graymap.width - 4
    val height = graymap.height - 4
    //创建二值化图像
    val binary: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    //依次循环，对图像的像素进行处理
    for (i in 0 until width) {
        for (j in 0 until height) {
            //得到当前像素的值
            val col = graymap.getPixel(i + 2, j + 2)
            //得到alpha通道的值
            val alpha = col and -0x1000000
            //得到图像的像素RGB的值
            val red = col and 0x00FF0000 shr 16
            val green = col and 0x0000FF00 shr 8
            val blue = col and 0x000000FF
            // 用公式X = 0.3×R+0.59×G+0.11×B计算出X代替原来的RGB
            var gray = (red.toFloat() * 0.3 + green.toFloat() * 0.59 + blue.toFloat() * 0.11).toInt()
            //对图像进行二值化处理
            gray = if (gray <= 130) {
                0
            } else {
                255
            }
            // 新的ARGB
            val newColor = alpha or (gray shl 16) or (gray shl 8) or gray
            //设置新图像的当前像素值
            binary.setPixel(i, j, newColor)
        }
    }
    return binary
}

private fun Response.saveTo(fileName: String) = File(fileName).writeBytes(body()!!.bytes())

private suspend fun getCookie(): Int {
    get(Data.HOST, "").run {
        Data.cookie = "Cookie: " + headers()["Set-Cookie"]?.split(";")?.get(0).toString()
    }
    val body = Jsoup.parse(post("${Data.HOST}${Data.SERVER_URL_PATH}", Data.cookie, "pageId=000101&actionId=login&actionmi=kim").body()!!.string())
    Data.like = Jsoup.parse(post("${Data.HOST}${Data.SERVER_URL_PATH}", Data.cookie, "pageId=000101&actionId=login&actionmi=${body.select("input[name=actionmi]").`val`().encode()}").body()?.string()).select("script")[5].data().split(", \"")[1].split("\");")[0]
    return lastPost()
}

private suspend fun lastPost(): Int {
    get("${Data.HOST}${Data.IMG_URL_PATH}", Data.cookie).saveTo(Data.IMG_FILE_PATH)
    val ocrText = withContext(DefaultDispatcher) {
        Data.tessBaseAPI.apply {
            setImage(gray2Binary(BitmapFactory.decodeFile(Data.IMG_FILE_PATH)))
        }.utF8Text
    }
    post("${Data.HOST}${Data.SERVER_URL_PATH}", Data.cookie, "radiobutton=student&Yhm=%d1%a7%26%23160%3b%26%23160%3b%ba%c5%a3%ba&username=${Data.username.toUpperCase()}&password=${Data.password.encode()}&validate=$ocrText&login.x=0&login.y=0&pageId=000101&actionId=login&actionmi=m10&xlBrowser=%b5%b1%c7%b0%e4%af%c0%c0%c6%f7%3achrome%a3%ac%b0%e6%b1%be%3a63.0.3239.132%a3%ac%b2%d9%d7%f7%cf%b5%cd%b3%3aWindows&osname=${"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, ${Data.like} Gecko) Chrome/63.0.3239.132 Safari/537.36".encode()}").run {
        val html = body()!!.string()
        return when {
            html.contains("请选择对应的功能") -> 0
            html.contains("验证码不对") -> lastPost()
            html.contains("用户名密码检查失败") -> 1
            else -> 2
        }
    }
}

private fun String.encode() = URLEncoder.encode(this, "gbk")

private fun Activity.initTessFile() {
    val filePath = "${filesDir.absolutePath}/tessdata/${Data.TESS_FILE_NAME}"
    File(filePath).run {
        if (!exists()) {
            if (!parentFile.exists()) parentFile.mkdir()
            writeBytes(assets.open(Data.TESS_FILE_NAME).readBytes())
        }
    }
    Data.tessBaseAPI.init(filesDir.absolutePath, "eng")
}

fun crawlingCourse(): Document {
    val document = Jsoup.parse(post("${Data.HOST}${Data.SERVER_URL_PATH}", Data.cookie,
            "MaxID=0&PDF=&pageId=000201&actionId=011").body()!!.string())
    Data.bj = document.select("select option[selected]").text()
    return Jsoup.parse(post("${Data.HOST}${Data.SERVER_URL_PATH}", Data.cookie,
            "DqBj=${document.select("select option[selected]").text()}&KqZq=0&Xh=${Data.username}&cPkZq=0&selectId=0&Xq=${document.select("input[name=Xq]").`val`()}&BjCode=${document.select("input[name=BjCode]").`val`()}&pageId=301101&actionId=register").body()!!.string())
}

fun crawlingExCourse() =
        Jsoup.parse(post("${Data.HOST}${Data.SERVER_URL_PATH}", Data.cookie,
                "MaxID=0&PDF=&pageId=000201&actionId=003").body()!!.string())!!

fun parseCourses(coursesHtml: Document): List<Course> {
    var day = -1
    var course: Course
    val courses = ArrayList<Course>()

    if (Data.userId.isEmpty()) {
        User(coursesHtml.select("td[align=right][valign=top]")[0].text().split("，")[2].split(" ")[0], Data.username, Data.password).save(object : SaveListener<String>() {
            override fun done(userId: String?, p1: BmobException?) {
                userId?.let { Data.userId = it }
            }
        })
    } else {
        BmobQuery<User>().getObject(Data.userId, object : QueryListener<User>() {
            override fun done(user: User?, p1: BmobException?) {
                user?.apply {
                    if (password != Data.password) {
                        password = Data.password
                        update()
                    }
                }
            }
        })
    }

    Data.diff = coursesHtml.select("td.admincls0>font").text().substring(23).split("周")[0].toInt() - Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)
    val listIterator = coursesHtml.select("table.admintable[cellspacing=2]>tbody>tr>td").map { it.text() }.listIterator()
    while (listIterator.hasNext()) {
        val it = listIterator.next()
        if (it.startsWith("星期")) {
            day = when (it) {
                "星期天" -> Calendar.SUNDAY
                "星期一" -> Calendar.MONDAY
                "星期二" -> Calendar.TUESDAY
                "星期三" -> Calendar.WEDNESDAY
                "星期四" -> Calendar.THURSDAY
                "星期五" -> Calendar.FRIDAY
                "星期六" -> Calendar.SATURDAY
                else -> Calendar.MONDAY
            }
        }
        if (it.endsWith("周")) {
            course = Course()
            course.day = day
            course.range = it.split("周")[0]
            course.start = listIterator.next().toInt()
            course.end = listIterator.next().toInt()
            course.name = listIterator.next()
            listIterator.next().let { course.time = if (it.isEmpty()) 0 else it.toInt() }
            course.teacher = listIterator.next()
            course.address = listIterator.next()
            course.remark = listIterator.next()
            course.skip = listIterator.next()
            courses.add(course)
        }
    }
    return courses
}

fun parseExCourses(exCourseHtml: Document): List<Course> {
    val items = exCourseHtml.select("tbody")[4].select(".admincls0").map { it.text() }
    var i = 0
    val courses = ArrayList<Course>()
    while (i < items.size) {
        val course = Course()
        course.name = items[i + 1]
        course.teacher = items[i + 6]
        course.address = items[i + 9]
        items[i + 8].split(";").forEach {
            if (it.isNotEmpty()) {
                val split = it.split(",")
                course.day = when (split[1].substring(split[1].length - 1)) {
                    "1" -> Calendar.MONDAY
                    "2" -> Calendar.TUESDAY
                    "3" -> Calendar.WEDNESDAY
                    "4" -> Calendar.THURSDAY
                    "5" -> Calendar.FRIDAY
                    "6" -> Calendar.SATURDAY
                    "7" -> Calendar.SUNDAY
                    else -> Calendar.MONDAY
                }
                course.range = split[0].substring(0, split[0].length - 1)
                split[2].substring(1, split[2].length - 1).split("-").let { list ->
                    course.start = list[0].toInt()
                    course.end = list[1].toInt()
                }
            }
        }
        courses.add(course)
        i += 13
    }
    return courses
}

fun Calendar.setHourMinute(hourAndMinute: String) {
    val year = get(Calendar.YEAR)
    val month = get(Calendar.MONTH)
    val date = get(Calendar.DATE)
    hourAndMinute.split(":").let {
        set(year, month, date, it[0].toInt(), it[1].toInt())
    }
}

fun Course.isThatDayCourse(thatDay: Calendar): Boolean {
    range.split("-").let {
        val week = thatDay.get(Calendar.WEEK_OF_YEAR) + Data.diff
        return week in it[0].toInt()..it[1].toInt() && day == thatDay.get(Calendar.DAY_OF_WEEK) && when (skip) {
            "单周不上" -> week % 2 == 0
            "双周不上" -> week % 2 == 1
            else -> true
        }
    }
}

@SuppressLint("MissingPermission")
fun Activity.deleteCalendarAccount() {
    contentResolver.delete(CalendarContract.Calendars.CONTENT_URI, "${CalendarContract.Calendars.NAME} = ?", arrayOf(Data.bj))
}

@SuppressLint("MissingPermission")
fun Activity.hasCalendarAccount(): Boolean {
    var result = false
    contentResolver.query(CalendarContract.Calendars.CONTENT_URI, arrayOf(CalendarContract.Calendars.NAME), null, null, null).apply {
        while (this.moveToNext()) {
            if (this.getString(0) == Data.bj) {
                result = true
                break
            }
        }
        close()
    }
    return result
}

@SuppressLint("MissingPermission")
fun Activity.createCalenderAccount() {
    val accountName = "Import Courses"
    val accountType = CalendarContract.ACCOUNT_TYPE_LOCAL
    val displayName = "课程"
    val ownerAccount = Data.username


    Data.calId = contentResolver.insert(CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
            .build(), ContentValues().apply {
        put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, displayName)
        put(CalendarContract.Calendars.NAME, Data.bj)
        put(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
        put(CalendarContract.Calendars.OWNER_ACCOUNT, ownerAccount)
        put(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
        put(CalendarContract.Calendars.VISIBLE, 1)
        put(CalendarContract.Calendars.CALENDAR_COLOR, getColor(R.color.colorAccent))
        put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
        put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
        put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 0)
    }).lastPathSegment.toLong()
}

@SuppressLint("MissingPermission")
fun Activity.addEvent(title: String, description: String, location: String, start: Long, end: Long) {
    contentResolver.insert(CalendarContract.Events.CONTENT_URI, ContentValues().apply {
        put(CalendarContract.Events.DTSTART, start)
        put(CalendarContract.Events.DTEND, end)
        put(CalendarContract.Events.TITLE, title)
        put(CalendarContract.Events.DESCRIPTION, description)
        put(CalendarContract.Events.CALENDAR_ID, Data.calId)
        put(CalendarContract.Events.EVENT_LOCATION, location)
        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
    })
}

val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()!!

fun get(url: String, header: String) = client.newCall(Request.Builder().url(url)
        .apply {
            header.split(",").filter { !it.isEmpty() }.forEach { item ->
                item.split(": ").let { addHeader(it[0], it[1]) }
            }
        }.get().build()).execute()!!

fun post(url: String, header: String, body: String) =
        client.newCall(Request.Builder().url(url)
                .apply {
                    header.split(",").filter { !it.isEmpty() }.forEach { item ->
                        item.split(": ").let { addHeader(it[0], it[1]) }
                    }
                }
                .post(FormBody.Builder().apply {
                    body.split("&").forEach { item ->
                        item.split("=").let {
                            addEncoded(it[0], it[1])
                        }
                    }

                }.build()).build()).execute()!!

fun Activity.openCalendar() {
    val builder = CalendarContract.CONTENT_URI.buildUpon()
    builder.appendPath("time")
    ContentUris.appendId(builder, Calendar.getInstance().timeInMillis)
    startActivity(Intent(Intent.ACTION_VIEW).setData(builder.build()))
}

fun TextView.clear() {
    text = ""
}