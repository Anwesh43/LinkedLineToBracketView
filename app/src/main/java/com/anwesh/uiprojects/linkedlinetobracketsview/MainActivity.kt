package com.anwesh.uiprojects.linkedlinetobracketsview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import com.anwesh.uiprojects.linetobracketview.LineToBracketView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view : LineToBracketView = LineToBracketView.create(this)
        view.addTransformListener({createToast("Line number $it converted to bracket")}) {
            createToast("Bracket number $it converted to line")
        }
        fullScreen()
    }
}

fun MainActivity.fullScreen() {
    supportActionBar?.hide()
    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
}

fun MainActivity.createToast(txt : String) {
    Toast.makeText(this, txt, Toast.LENGTH_SHORT).show()
}