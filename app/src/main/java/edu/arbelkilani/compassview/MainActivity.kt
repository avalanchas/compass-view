package com.elliecoding.compassview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.elliecoding.compassview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.submitTarget.setOnClickListener { submit() }
    }

    private fun submit() {
        binding.compass.setTargetBearing(binding.inputTarget.text.toString().toInt())
    }
}
