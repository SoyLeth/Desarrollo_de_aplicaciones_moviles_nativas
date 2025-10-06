package com.example.tarea2

data class GroupInfo(
    val id: String,         // "twice", "straykids", "itzy", "nmixx", "xdh"
    val name: String,
    val slogan: String,
    val logoResId: Int,
    val photoResId: Int,    // imagen del reverso
    val backgroundColor: Int
)
