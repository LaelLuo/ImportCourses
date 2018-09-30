package com.github.laelluo.importcourses

import android.content.Context
import com.googlecode.tesseract.android.TessBaseAPI
import java.util.*

object Data {
    private val database = MyApplication.context.getSharedPreferences("database", Context.MODE_PRIVATE)!!
    const val HOST = "http://jwxt.jmpt.cn:8125/JspHelloWorld/"
    const val SERVER_URL_PATH = "servlets/CommonServlet"
    const val IMG_URL_PATH = "authImg"
    const val TESS_FILE_NAME = "eng.traineddata"

    val courses = ArrayList<Course>()
    val tessBaseAPI= TessBaseAPI()
    val IMG_FILE_PATH = "${MyApplication.context.filesDir.absolutePath}/temp.jpg"

    lateinit var bj: String

    var diff = 0
    var like = ""

    var userId: String
        get() = database.getString("user_id", "")
        set(value) = database.edit().putString("user_id", value).apply()
    var isLogon: Boolean
        get() = database.getBoolean("is_logon", false)
        set(value) = database.edit().putBoolean("is_logon", value).apply()
    var cookie: String
        get() = database.getString("cookie", "")
        set(value) = database.edit().putString("cookie", value).apply()
    var username: String
        get() = database.getString("username", "")
        set(value) = database.edit().putString("username", value).apply()
    var password: String
        get() = database.getString("password", "")
        set(value) = database.edit().putString("password", value).apply()
    var calId: Long
        get() = database.getLong("cal_id", -1)
        set(value) = database.edit().putLong("cal_id", value).apply()
    var hasPermission: Boolean
        get() = database.getBoolean("has_permission", false)
        set(value) = database.edit().putBoolean("has_permission", value).apply()
}