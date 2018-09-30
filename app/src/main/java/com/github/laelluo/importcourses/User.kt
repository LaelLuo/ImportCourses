package com.github.laelluo.importcourses

import cn.bmob.v3.BmobObject

data class User(var name: String? = null, var username: String? = null, var password: String? = null) : BmobObject()