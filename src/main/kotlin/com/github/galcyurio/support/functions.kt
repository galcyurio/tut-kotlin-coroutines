package com.github.galcyurio.support

fun printlnThread(any: Any? = "") {
    println("${Thread.currentThread().toString().padEnd(50)} ||| $any")
}