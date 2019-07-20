package com.github.galcyurio

/**
 * ## Cancelling coroutine execution
 *
 * 장기 실행 응용 프로그램에서는 배경 코루틴을 세밀하게 제어해야 할 수 있습니다.
 * 예를 들어, 사용자가 코루틴을 시작한 페이지를 닫았을 수 있으며
 * 결과가 더 이상 필요 없으며 그 작업을 취소 할 수 있습니다.
 *
 * [launch] 함수는 실행중인 코 루틴을 취소하는 데 사용할 수있는 [Job]을 반환합니다.
 */
//fun main() = runBlocking {
//    val job: Job = launch {
//        repeat(1000) { i ->
//            println("job: I'm sleeping $i ...")
//            delay(500L)
//        }
//    }
//    delay(1300L) // delay a bit
//    println("main: I'm tired of waiting!")
//    job.cancel() // cancels the job
//    job.join() // waits for job's completion
//    job.cancelAndJoin()
//    println("main: Now I can quit.")
//}

// 실행결과
// job: I'm sleeping 0 ...
// job: I'm sleeping 1 ...
// job: I'm sleeping 2 ...
// main: I'm tired of waiting!
// main: Now I can quit.


/**
 * ## Cancellation is cooperative
 *
 * 코루틴 취소는 협조적입니다. 코루틴 코드는 취소 가능하도록 협조해야합니다.
 * `kotlinx.coroutines`의 모든 `suspending function`은 취소 가능합니다.
 * 그들은 코루틴의 취소를 확인하고 취소할 때 [CancellationException]을 던집니다.
 * 그러나 코루틴이 계산에서 작동하고 취소를 확인하지 않으면 다음 예제와 같이 취소 할 수 없습니다.
 * */

//fun main() = runBlocking {
//    val startTime = System.currentTimeMillis()
//    val job = launch(Dispatchers.Default) {
//        var nextPrintTime = startTime
//        var i = 0
//        while (i < 5) { // computation loop, just wastes CPU
//            // print a message twice a second
//            if (System.currentTimeMillis() >= nextPrintTime) {
//                println("job: I'm sleeping ${i++} ...")
//                nextPrintTime += 500L
//            }
//        }
//    }
//    delay(1300L) // delay a bit
//    println("main: I'm tired of waiting!")
//    job.cancelAndJoin() // cancels the job and waits for its completion
//    println("main: Now I can quit.")
//}

// 실행결과
// job: I'm sleeping 0 ...
// job: I'm sleeping 1 ...
// job: I'm sleeping 2 ...
// main: I'm tired of waiting!
// job: I'm sleeping 3 ...
// job: I'm sleeping 4 ...
// main: Now I can quit.


/**
 * ## Making computation code cancellable
 *
 * computation code를 취소할 수 잇도록 만드는 두 가지 방법이 있습니다.
 * 첫째는 주기적으로 suspending function을 호출하여 취소를 체크하는 것입니다.
 * 이 목적에 알맞게 [yield] 함수가 있습니다.
 *
 * 다른 방법으로는 취소 상태를 확인하는 것입니다.
 * 여기서는 후자의 방식으로 시도해보겠습니다.
 *
 * 이전 코드에서 `while (i < 5)` 부분을 `while (isActive)` 로 바꾸고 실행해보세요.
 * Replace while (i < 5) in the previous example with while (isActive) and rerun it.
 * */

//fun main() = runBlocking {
//    val startTime = System.currentTimeMillis()
//    val job = launch(Dispatchers.Default) {
//        var nextPrintTime = startTime
//        var i = 0
//        while(isActive) {
//            if (System.currentTimeMillis() >= nextPrintTime) {
//                println("job: I'm sleeping ${i++} ...")
//                nextPrintTime += 500L
//            }
//        }
//    }
//    delay(1300L)
//    println("main: I'm tired of waiting!")
//    job.cancelAndJoin()
//    println("main: Now I can quit.")
//}

// 실행결과
// job: I'm sleeping 0 ...
// job: I'm sleeping 1 ...
// job: I'm sleeping 2 ...
// main: I'm tired of waiting!
// main: Now I can quit.


/**
 * ## Closing resources with finally
 *
 * 취소가능한 suspending function은 일반적인 방식으로 처리가능한 [CancellationException]을 던집니다.
 * 예를 들면, `try {...} finally {...}` 표현식이나 코틀린의 `use` 함수는
 * 코루틴이 취소될 때 정상적으로 finalization 작업을 실행합니다.
 * */
//fun main() = runBlocking {
//    val job = launch {
//        try {
//            repeat(1000) { i ->
//                println("job: i'm sleeping $i ...")
//                delay(500L)
//            }
//        } finally {
//            println("job: I'm running finally")
//        }
//    }
//    delay(1300L)
//    println("main: I'm tired of waiting!")
//    job.cancelAndJoin()
//    println("main: Now I can quit.")
//}

// 실행결과
// job: i'm sleeping 0 ...
// job: i'm sleeping 1 ...
// job: i'm sleeping 2 ...
// main: I'm tired of waiting!
// job: I'm running finally
// main: Now I can quit.

/**
 * ## Run non-cancellable block
 *
 * 이전 예제의 finally 블록에서 suspending function을 사용하려고 하면
 * 코루틴이 이미 취소되었기 때문에 [CancellationException] 에러가 일어납니다.
 * 일반적으로 닫는 작업은 non-blocking 이며 suspending function을 포함하지 않으므로 문제가 되지 않습니다.
 *
 * 하지만 취소된 코루틴에서 suspend 해야하는 특이한 경우에는 다음 예제와 같이 `withContext` 함수와
 * `NonCancellable` 컨텍스트를 사용하여 해당 코드를 `withContext (NonCancellable) {...}`에 래핑할 수 있습니다.
 * */
//fun main() = runBlocking {
//    val job = launch {
//        try {
//            repeat(1000) { i ->
//                println("job: I'm sleeping $i ...")
//                delay(500)
//            }
//        } finally {
//            withContext(NonCancellable) {
//                println("job: I'm running finally")
//                delay(1000L)
//                println("job: And I've just delayed for 1 sec because I'm non-cancellable")
//            }
//        }
//    }
//    delay(1300)
//    println("main: I'm tired of waiting!")
//    job.cancelAndJoin()
//    println("main: Now I can quit.")
//}

// 실행결과
// job: I'm sleeping 0 ...
// job: I'm sleeping 1 ...
// job: I'm sleeping 2 ...
// main: I'm tired of waiting!
// job: I'm running finally
// job: And I've just delayed for 1 sec because I'm non-cancellable
// main: Now I can quit.

/**
 * ## Timeout
 *
 * 코루틴의 실행을 취소하는 가장 실질적인 이유는 실행 시간이 timeout 되기 때문입니다.
 * 해당 작업에 대한 참조를 수동으로 추적하고 지연된 후에 별도의 코루틴을 실행하여
 * 추적했던 것을 취소할 수는 있지만 `withTimeout` 함수를 통해서도 가능합니다.
 * */
//fun main()  = runBlocking {
//    withTimeout(1300L) {
//        repeat(1000) { i ->
//            println("I'm sleeping $i ...")
//            delay(500L)
//        }
//    }
//}

// 실행결과
// I'm sleeping 0 ...
// I'm sleeping 1 ...
// I'm sleeping 2 ...
// Exception in thread "main" kotlinx.coroutines.TimeoutCancellationException: Timed out waiting for 1300 ms
//   at kotlinx.coroutines.TimeoutKt.TimeoutCancellationException(Timeout.kt:128)
//   at kotlinx.coroutines.TimeoutCoroutine.run(Timeout.kt:94)
//   at kotlinx.coroutines.EventLoopImplBase$DelayedRunnableTask.run(EventLoop.kt:307)
//   at kotlinx.coroutines.EventLoopImplBase.processNextEvent(EventLoop.kt:116)
//   at kotlinx.coroutines.DefaultExecutor.run(DefaultExecutor.kt:68)
//   at java.base/java.lang.Thread.run(Thread.java:844)

/**
 * `withTimeout`에서 던져지는 [TimeoutCancellationException]은 [CancellationException]의 서브클래스입니다.
 * 이전에는 콘솔에 스택 추적이 인쇄된 것을 보지 못했습니다.
 * 왜냐하면 [CancellationException]은 코루틴이 완료된 정상적인 것으로 간주되기 때문입니다.
 * 하지만 위 예제에서는 `withTimeout`을 `main`함수 내부에서 사용했습니다.
 *
 * 취소는 단지 예외이므로 모든 자원들은 일반적인 방법으로 종료됩니다.
 * 타임아웃이 발생되었을 때 추가적으로 무언가를 하고 싶다면
 * 타임아웃이 발생하는 코드를 `try {...} catch (e: TimoutCancellationException) {...}` 블록을 통해 처리할 수 있습니다.
 *
 * 또는 `withTimeout`과 유사하지만 예외를 던지지 않고 null을 반환하는 `withTimeoutOrNull` 함수를 사용할 수도 있습니다.
 * */
//fun main() = runBlocking {
//    val result = withTimeoutOrNull(1300) {
//        repeat(1000) { i ->
//            println("I'm sleeping $i ...")
//            delay(500)
//        }
//        "Done"
//    }
//    println("Result is $result")
//}
// 실행결과
// I'm sleeping 0 ...
// I'm sleeping 1 ...
// I'm sleeping 2 ...
// Result is null