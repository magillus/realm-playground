package com.perlak.example.realmplayground

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.perlak.example.realmplayground.model.RepoGenerator
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnGenerate.setOnClickListener { v ->

            var repoGenerator = RepoGenerator()
            var repo = repoGenerator.generateData("test")

        }
    }
}
