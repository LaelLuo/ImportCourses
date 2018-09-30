package com.github.laelluo.importcourses

data class Course(
        var day: Int = 0,
        var range: String = "",
        var start: Int = 0,
        var end: Int = 0,
        var name: String = "",
        var time: Int = 0,
        var teacher: String = "",
        var address: String = "",
        var remark: String = "",
        var skip: String = ""
) {
    override fun toString() = "课程名称：$name 教师：$teacher 时间：第 $range 周"
}