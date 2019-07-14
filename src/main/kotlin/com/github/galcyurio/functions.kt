package com.github.galcyurio

fun printlnThread(any: Any? = "") {
    println("${Thread.currentThread().toString().padEnd(50)} ||| $any")
}