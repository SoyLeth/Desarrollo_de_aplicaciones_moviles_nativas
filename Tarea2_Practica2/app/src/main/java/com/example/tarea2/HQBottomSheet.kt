package com.example.tarea2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class HQBottomSheet : BaseScrollableBottomSheet() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottomsheet_hq, container, false)
}
