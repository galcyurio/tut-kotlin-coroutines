@file:Suppress("SpellCheckingInspection")

package com.github.galcyurio

import com.github.galcyurio.support.printlnThread
import kotlinx.coroutines.*
import org.junit.Test
import kotlin.concurrent.thread

class Basics {

    @Test
    fun `Your first coroutine`() {
        GlobalScope.launch {
            // launch a new coroutine in background and continue
            delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
            printlnThread("World!") // print after delay
        }
        printlnThread("Hello,") // main thread continues while coroutine is delayed
        Thread.sleep(2000L) // block main thread for 2 seconds to keep JVM alive
    }

    @Test
    fun `Bridging blocking and non-blocking worlds`() {
        GlobalScope.launch {
            // launch a new coroutine in background and continue
            delay(1000L)
            printlnThread("World!")
        }
        printlnThread("Hello,") // main thread continues here immediately

        // but this expression blocks the main thread
        runBlocking {
            delay(2000L) // ... while we delay for 2 seconds to keep JVM alive
        }
    }

    @Test
    fun `Bridging blocking and non-blocking worlds 2`() = runBlocking {
        GlobalScope.launch {
            delay(1000L)
            printlnThread("World!")
        }
        printlnThread("Hello,")
        delay(2000L)
    }

    @Test
    fun `Waiting for a job`() = runBlocking {
        val job = GlobalScope.launch {
            delay(1000L)
            printlnThread("World!")
        }
        printlnThread("Hello,")
        job.join()
    }

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
    @Test
    fun `Structured concurrency`() = runBlocking {
        launch {
            // launch a new coroutine in the scope of runBlocking
            delay(1000L)
            printlnThread("World!")
        }
        printlnThread("Hello,")
    }

    /**
     * ## Scope builder
     *
     * 다른 빌더가 제공하는 코루틴 범위 외에, coroutineScope 빌더를 사용하여 자신의 범위를 선언할 수도 있습니다.
     * 코루틴 범위를 만들고 시작된 모든 자식이 완료될 때까지 완료되지 않습니다.
     *
     * [runBlocking]과 [coroutineScope]가 다른 점은 [coroutineScope]는 모든 자식이 완료될 때까지 기다리는 동안
     * 현재의 쓰레드를 차단하지 않는다는 것입니다.
     *
     * ### Non-official
     * 위의 차이점에 대한 추가적인 설명입니다.
     *
     * [runBlocking]과 [coroutineScope]의 차이는 `blocking`과 `suspending call`의 차이입니다.
     * 아는 바와 같이 한 스레드에서 여러개의 코루팅을 동시에 실행할 수 있습니다.
     * 이것은 스레드 생성 리소스가 무거워서 코루틴의 힘이 시작되는 곳입니다.
     *
     * `concurrent code`의 각 부분에 대한 스레드를 작성하는 대신 코루틴을 작성하고
     * 하나 또는 가능한 다중 스레드(대부분 400개가 아닌 4개)에서 해당 코루틴을 실행하세요.
     *
     * blocking은 스레드를 block한다는 뜻입니다.
     * 이것은 blocking call이 끝나기 전까지 어떠한 코루틴도 실행될 수 없다는 걸 의미합니다.
     * 반대로 suspending은 코루틴을 suspend한다는 뜻입니다.
     * **코루틴이 기다리는 동안 코루틴이 실행되고 있는 스레드에서 다른 코루틴이 실행될 수 있습니다.**
     * */
    @Test
    fun `Scope builder`() = runBlocking {
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
//    실행 결과
//    Thread[Test worker @coroutine#1,5,main]            ||| Task from coroutine scope
//    Thread[Test worker @coroutine#2,5,main]            ||| Task from runBlocking
//    Thread[Test worker @coroutine#3,5,main]            ||| Task from nested launch
//    Thread[Test worker @coroutine#1,5,main]            ||| Coroutine scope is over

    /**
     * ### Non-official
     *
     * 위의 경우와 모두 같지만 [coroutineScope] 내부에서
     * [launch]가 어떤 일을 하는건지 알아보기 위한 테스트입니다.
     *
     * 위와 비교해서 보면 [launch]를 사용하면 별도로 코루틴이 돌아갑니다.
     * */
    @Test
    fun `Scope builder 2`() = runBlocking {
        launch {
            delay(200L)
            printlnThread("Task from runBlocking")
        }

        coroutineScope {
            delay(500L)
            printlnThread("Task from nested launch")

            launch {
                delay(100L)
                printlnThread("Task from coroutine scope")
            }
        }

        printlnThread("Coroutine scope is over")
    }
//    실행결과
//    Thread[Test worker @coroutine#2,5,main]            ||| Task from runBlocking
//    Thread[Test worker @coroutine#1,5,main]            ||| Task from nested launch
//    Thread[Test worker @coroutine#3,5,main]            ||| Task from coroutine scope
//    Thread[Test worker @coroutine#1,5,main]            ||| Coroutine scope is over

    /**
     * ## Extract function refactoring
     *
     * `launch {...}` 코드 블록을 별도의 함수로 추출해 봅시다.
     * 이 코드에서 "Extract function"리팩토링을 수행하면 `suspend` 수정자를 가진 새로운 함수를 얻게됩니다.
     *
     * `suspending function`은 일반적인 함수과 마찬가지로 코루틴에서 사용할 수 있지만
     * 추가 기능은 이러한 예에서 [delay]와 같은 다른 `suspending function`을 사용하여
     * 동시 루틴 실행을 suspend(일시중단)할 수 있다는 것입니다.
     * */
    @Test
    fun `Extract function refactoring`() = runBlocking {
        launch { doWorld() }
        println("Hello,")
    }

    private suspend fun doWorld() {
        delay(1000L)
        println("World!")
    }

    /**
     * ## Coroutines ARE light-weight
     *
     * 10만개의 코루틴을 시작하고, 1초 후에 각 코루틴이 점을 찍습니다.
     * 자, 스레드로 시도해보십시오. 무슨 일이 일어날 지?
     * (대부분의 경우 코드에서 일종의 메모리 부족 오류가 발생합니다)
     * */
    @Test
    fun `Coroutines ARE light-weight`() = runBlocking {
        repeat(100_000) {
            launch {
                delay(1000L)
                printlnThread(".")
            }
        }
    }

    @Test
    fun `Coroutines ARE light-weight 2`() {
        repeat(100_000) {
            thread {
                Thread.sleep(1000L)
                printlnThread(".")
            }
        }
    }

    /**
     * ## Global coroutines are like daemon threads
     *
     * 다음 코드는 [GlobalScope]에서 "I'm sleeping"을 2초에 한 번 출력한 후
     * 약간 지연 후 main 함수에서 반환하는 장기 실행 코루틴을 시작합니다.
     * */
    @Test
    fun `Global coroutines are like daemon threads`() = runBlocking {
        GlobalScope.launch {
            repeat(1000) { i ->
                printlnThread("I'm sleeping $i ...")
                delay(500)
            }
        }
        delay(1300)
    }
//    실행결과
//    Thread[DefaultDispatcher-worker-1,5,main] ||| I'm sleeping 0 ...
//    Thread[DefaultDispatcher-worker-1,5,main] ||| I'm sleeping 1 ...
//    Thread[DefaultDispatcher-worker-1,5,main] ||| I'm sleeping 2 ...
}
