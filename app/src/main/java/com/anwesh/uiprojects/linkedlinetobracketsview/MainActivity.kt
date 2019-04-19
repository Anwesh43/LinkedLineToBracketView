package com.anwesh.uiprojects.linkedlinetobracketsview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.linetobracketview.LineToBracketView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LineToBracketView.create(this)
    }
}
