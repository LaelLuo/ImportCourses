package com.github.laelluo.importcourses

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.startActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        username_input_login.setText(Data.username)
        password_input_login.setText(Data.password)
        sign_button_login.setOnClickListener {
            startLogin()
        }
        password_input_login.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                startLogin()
                return@OnEditorActionListener true
            }
            false
        })
    }

    private fun startLogin() =
            login(username_input_login.text.toString().toUpperCase(), password_input_login.text.toString()) {
                loginSuccess()
            }


    private fun loginSuccess() {
        Data.isLogon = true
        toast(getString(R.string.login_success))
        startActivity<MainActivity>()
        finish()
    }

}
