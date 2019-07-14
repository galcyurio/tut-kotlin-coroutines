package com.github.galcyurio

import kotlinx.coroutines.*

//fun main() {
//    GlobalScope.launch { // launch a new coroutine in background and continue
//        delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
//        printlnThread("World!") // print after delay
//    }
//    printlnThread("Hello,") // main thread continues while coroutine is delayed
//    Thread.sleep(2000L) // block main thread for 2 seconds to keep JVM alive
//}

//fun main() {
//    GlobalScope.launch {
//        // launch a new coroutine in background and continue
//        delay(1000L)
//        printlnThread("World!")
//    }
//    printlnThread("Hello,") // main thread continues here immediately
//
//    // but this expression blocks the main thread
//    runBlocking {
//        delay(2000L) // ... while we delay for 2 seconds to keep JVM alive
//        printlnThread()
//    }
//}

//fun main() = runBlocking<Unit> {
//    GlobalScope.launch {
//        delay(1000L)
//        printlnThread("World!")
//    }
//    printlnThread("Hello,")
//    delay(2000L)
//}


/** Waiting for a job */
//fun main() = runBlocking {
//    val job = GlobalScope.launch {
//        delay(1000L)
//        printlnThread("World!")
//    }
//    printlnThread("Hello,")
//    job.join()
//}

/**
 * ## Structured concurrency
 *
 * 코 루틴의 실용적인 사용을 위해 여전히 요구되는 사항이 있습니다.
 * [GlobalScope.launch]를 사용할 때 최상위 수준의 코루틴을 만듭니다.
 * 코루틴은 가볍지만 여전히 약간의 메모리 자원을 사용합니다.
 * 만약 새로 만들었던 코루틴의 레퍼런스를 잊어버린다면 이 코루틴은 계속해서 동작합니다.
 * 실행된 모든 코루틴에 대한 참조를 수동으로 유지하고 이들을 조인하는 것은 오류를 발생시키기 쉽습니다.
 *
 * 우리는 코드에서 구조화된 동시성을 사용할 수 있습니다.
 * [GlobalScope]에서 코루틴을 시작하는 대신에 우리가 일반적으로 스레드 (스레드는 항상 전역)와 같이하는 것처럼
 * 우리가 수행중인 작업의 특정 범위에서 코루틴을 시작할 수 있습니다.
 * */

//fun main() = runBlocking { // this: CoroutineScope
//    launch { // launch a new coroutine in the scope of runBlocking
//        delay(1000L)
//        printlnThread("World!")
//    }
//    printlnThread("Hello,")
//}

/**
 * ## Scope builder
 *
 * 다른 빌더가 제공하는 코루틴 범위 외에, coroutineScope 빌더를 사용하여 자신의 범위를 선언할 수도 있습니다.
 * 코루틴 범위를 만들고 시작된 모든 자식이 완료될 때까지 완료되지 않습니다.
 *
 * [runBlocking]과 [coroutineScope]가 다른 점은 모든 자식이 완료될 때까지 기다리는 동안
 * 현재의 쓰레드를 차단하지 않는다는 것입니다.
 * */

fun main() = runBlocking {
    launch {
        delay(200L)
        printlnThread("Task from runBlocking")
    }

    coroutineScope {
        launch {
            delay(500L)
            printlnThread("Task from nested launch")
        }

        delay(100L)
        printlnThread("Task from coroutine scope")
    }

    printlnThread("Coroutine scope is over")
}

// 실행결과
// Thread[main,5,main] ||| Task from coroutine scope
// Thread[main,5,main] ||| Task from runBlocking
// Thread[main,5,main] ||| Task from nested launch
// Thread[main,5,main] ||| Coroutine scope is over