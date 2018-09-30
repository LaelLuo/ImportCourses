package com.github.laelluo.importcourses

import android.Manifest
import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.github.dfqin.grantor.PermissionListener
import com.github.dfqin.grantor.PermissionsUtil
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.jetbrains.anko.startActivity
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Data.isLogon) {
            goToLoginActivity()
        } else {
            setContentView(R.layout.activity_main)
            refresh()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.claer_user_main -> {
                if (Data.hasPermission && hasCalendarAccount()) deleteCalendarAccount()
                Data.username = ""
                Data.password = ""
                Data.cookie = ""
                Data.isLogon = false
                goToLoginActivity()
            }
            R.id.refresh -> {
                refresh()
            }
            R.id.about_me_main -> {
                startActivity<AboutActivity>()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun refresh() {
        val dialog = waitDialog(getString(R.string.crawl_course_page), getString(R.string.loading))
        content_text_main.clear()
        output("开始刷新")
        output("开始爬取课程信息")
        launch(UI) {
            try {
                val coursesHtml = withContext(DefaultDispatcher) { crawlingCourse() }
                val exCoursesHtml = withContext(DefaultDispatcher) { crawlingExCourse() }
                dialog.cancel()
                output("爬取成功")
                output("开始解析")
//                    是否成功登陆
                if (coursesHtml.title() == "今日课表") {
                    Data.courses.clear()
                    Data.courses.addAll(parseCourses(coursesHtml))
                    Data.courses.addAll(parseExCourses(exCoursesHtml))
                    Data.courses.forEach { course ->
                        output(course.toString())
                    }
                    output("解析成功")

                    content_text_main.snackbar(getString(R.string.parse_success), "开始导入") {
                        if (Data.hasPermission) importCourses() else initPermissions { importCourses() }
                    }
                } else {
                    output("解析失败")
                    content_text_main.snackbar(getString(R.string.cookie_error), getString(R.string.login_again)) { goToLoginActivity() }
                }
            } catch (e: SocketTimeoutException) {
                dialog.cancel()
                output("请求失败：超时")
                content_text_main.snackbar(getString(R.string.timeout), getString(R.string.restart)) { reStart() }
            } catch (e: ConnectException) {
                dialog.cancel()
                output("请求失败：网络错误")
                content_text_main.snackbar(getString(R.string.net_error), getString(R.string.restart)) { reStart() }
            }
        }
    }

    private fun importCourses() {
        output("开始导入日历")
        launch(UI) {
            val dialog = waitDialog(getString(R.string.importing), getString(R.string.loading))
            val timeList = listOf("8:15-9:00", "9:10-9:55", "10:10-10:55", "11:05-11:50", "14:30-15:15", "15:25-16:10", "16:20-17:05", "17:15-18:00", "18:10-18:55", "19:30-20:15", "20:25-21:10", "21:20-22:05")
            output("删除旧软件日历账户")
            output("新建软件日历账户")
            output("正在导入")
            withContext(DefaultDispatcher) {
                deleteCalendarAccount()
                createCalenderAccount()
                val startDay = Calendar.getInstance()
                val endDay = Calendar.getInstance().apply { add(Calendar.YEAR, 2) }
                while (startDay.before(endDay)) {
                    Data.courses.filter { it.isThatDayCourse(startDay) }.forEach { course ->
                        startDay.setHourMinute(timeList[course.start - 1].split("-")[0])
                        val startTimeInMillis = startDay.timeInMillis
                        startDay.setHourMinute(timeList[course.end - 1].split("-")[1])
                        val endTimeInMillis = startDay.timeInMillis
                        addEvent(course.name, "教师:${course.teacher} 备注:${if (course.remark.isEmpty()) "空" else course.remark}", course.address, startTimeInMillis, endTimeInMillis)
                    }
                    startDay.add(Calendar.DATE, 1)
                }
                dialog.cancel()
            }
            output("导入日历完成")
            content_text_main.snackbar(getString(R.string.import_success), "打开日历", -2) { openCalendar() }
        }
    }

    private fun initPermissions(call: () -> Unit) {
        if (!PermissionsUtil.hasPermission(this, Manifest.permission.READ_CALENDAR) || !PermissionsUtil.hasPermission(this, Manifest.permission.WRITE_CALENDAR,Manifest.permission.READ_CALENDAR)) {
            PermissionsUtil.requestPermission(this, object : PermissionListener {
                override fun permissionDenied(permission: Array<out String>) {
                    finish()
                }

                override fun permissionGranted(permission: Array<out String>) {
                    Data.hasPermission = true
                    call()
                }
            }, Manifest.permission.WRITE_CALENDAR)
        }
    }

    private fun reStart() {
        startActivity<MainActivity>()
        finish()
    }

    private fun goToLoginActivity() {
        startActivity<LoginActivity>()
        finish()
    }

    @SuppressLint("SetTextI18n")
    private fun output(text: String) {
        content_text_main.text = "${content_text_main.text}$text\n\n"
    }
}