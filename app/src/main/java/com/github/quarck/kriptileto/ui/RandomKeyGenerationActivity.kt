package com.github.quarck.kriptileto.ui

import android.os.Bundle
import android.app.Activity
import com.github.quarck.kriptileto.R

import kotlinx.android.synthetic.main.activity_random_key_generation.*

class RandomKeyGenerationActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_random_key_generation)
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

}
