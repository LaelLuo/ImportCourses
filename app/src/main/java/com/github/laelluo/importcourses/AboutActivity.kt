package com.github.laelluo.importcourses

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var count = 1
        setContentView(AboutPage(this)
                .isRTL(false)
                .setImage(R.mipmap.ic_launcher)
                .setDescription(getString(R.string.thank))
                .addItem(Element().setTitle("Version 1.0").setOnClickListener {
                    if (count % 10 == 0) toast("超喜欢你的~❤")
                    count++
                })
                .addEmail("LaelLuo@qq.com")
                .addGitHub("LaelLuo")
                .create())
    }
}
