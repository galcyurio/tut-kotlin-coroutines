@file:Suppress("SpellCheckingInspection")

package com.github.galcyurio

import com.github.galcyurio.support.printlnThread
import kotlinx.coroutines.*
import org.junit.Test
import kotlin.system.measureTimeMillis

class ComposingSuspendingFunctions {

    /**
     * ## Sequential by default
     *
     * 원격 서비스 요청 또는 연산 같은 유용한 작업을 하는 2개의 suspending function이 있다고 가정해봅시다.
     * 이 2개의 함수가 유용한 작업을 수행한다고 가정하지만 실제로는 이 예제의 목적을 위해 잠깐 지연될 뿐입니다.
     */
    @Suppress("unused") fun dummy1() {}

    private suspend fun doSomethingUsefulOne(): Int {
        delay(1000L) // pretend we are doing something useful here
        return 13
    }

    private suspend fun doSomethingUsefulTwo(): Int {
        delay(1000L) // pretend we are doing something useful here, too
        return 29
    }

    /**
     * 위 함수들을 순차적으로 호출하려면 어떻게 해야할까요?
     * 코루틴의 코드는 일반 코드와 마찬가지로 기본적으로 순차적이기 때문에 보통 순차적으로 호출합니다.
     */
    @Suppress("unused") fun dummy2() {}

    @Test
    fun `Sequential by default`() = runBlocking {
        val time = measureTimeMillis {
            val one = doSomethingUsefulOne()
            val two = doSomethingUsefulTwo()
            println("The answer is ${one + two}")
        }
        println("Completed in $time ms")
    }
//    실행결과
//    The answer is 42
//    Completed in 2015 ms

    /**
     * ## Concurrent using async
     *
     * 만약 [doSomethingUsefulOne] 함수와 [doSomethingUsefulTwo] 함수간에 의존성이 없어서
     * 동시에 실행되서 더 빠르게 반환받고 싶다면 어떻게 할까요?
     * 이러한 상황에선 [async]가 도움이 되줄 겁니다.
     *
     * 개념적으로 [async]는 [launch]와 같습니다.
     * [async]는 다른 모든 코루틴과 동시에 작동하는 별도의 코루틴을 시작합니다.
     * 차이점은 [launch]는 어떠한 반환값도 없는 [Job]을 반환하지만
     * [async]는 나중에 반환되는 값을 제공해주는 경량의 non-blocking future인 [Deferred]를 반환합니다.
     * [Deferred]에 `.await()`를 사용해서 값을 얻을 수 있으며 [Deferred] 또한 [Job] 이기 때문에 취소할 수도 있습니다.
     *
     * 아래 예제는 두 개의 코루틴을 동시에 실행하기 때문에 두 배 빠릅니다.
     * 코루틴의 동시성은 항상 명시적이라는 걸 기억하세요.
     */
    @Test
    fun `Concurrent using async`() = runBlocking {
        val time = measureTimeMillis {
            val one = async { doSomethingUsefulOne() }
            val two = async { doSomethingUsefulTwo() }
            println("The answer is ${one.await() + two.await()}")
        }
        println("Completed in $time ms")
    }
//    실행결과
//    The answer is 42
//    Completed in 1030 ms

    /**
     * ## Lazily started async
     *
     * `start` 매개변수에 [CoroutineStart.LAZY]를 넘겨서 [async]에서 지연시킬 수 있습니다.
     * 이렇게하면 `await` 또는 `start` 함수가 호출되었을 경우와 같이 결과값이 필요할 때 코루틴이 시작됩니다.
     */
    @Test
    fun `Lazily started async`() = runBlocking {
        val time = measureTimeMillis {
            val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
            val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }
            // some computation
            one.start()
            two.start()
            println("The answer is ${one.await() + two.await()}")
        }
        println("Completed in $time ms")
    }
//    실행결과
//    The answer is 42
//    Completed in 1033 ms

    /**
     * 두 개의 코루틴들이 정의되어 있지만 이전 예제처럼 실행되지는 않았습니다.
     * 그리고 `start` 함수를 통해 프로그래머에게 제어권이 주어집니다.
     * 위 예제에서는 먼저 `one`을 시작하고 그 다음 `two`를 시작한 뒤
     * 각각의 코루틴에 대해서 `await` 했습니다.
     *
     * 여기서 중요한 점은 우리가 만약 `start`를 제외하고 `println`에서 `await`만 했다면
     * 코루틴은 순차적으로 동작할 것입니다.
     * 이것은 지연 동작의 의도적인 사용방법이 아닙니다.
     *
     * `async (start = CoroutineStart.LAZY)`의 사용은 값 계산에
     * suspending function이 포함된 경우 `lazy` 함수를 대체합니다.
     */
    @Suppress("unused") fun dummy3() {}

    /**
     * ## Async-style functions
     *
     * 명시적 GlobalScope 참조가 있는 비동기 코루틴 빌더를 사용하여
     * [doSomethingUsefulOne] 및 [doSomethingUsefulTwo]를 비동기 적으로 호출하는
     * 비동기 스타일 함수를 정의 할 수 있습니다.
     *
     * 이러한 함수들에는 뒤에 "Async" 라는 접미사를 붙여서 이 함수가 비동기 계산만 시작하고
     * 결과값을 얻기 위해서는 지연된 결과값을 사용해야한다는 사실을 강조합니다.
     */

    private fun somethingUsefulOneAsync(): Deferred<Int> =
        GlobalScope.async {
            doSomethingUsefulOne()
        }

    private fun somethingUsefulTwoAsync(): Deferred<Int> =
        GlobalScope.async {
            doSomethingUsefulTwo()
        }

    /**
     * 위의 `xxxAsync` 함수들은 suspending function이 아니라는 걸 기억하세요.
     * 이 함수들은 어느 곳에서든 호출될 수 있습니다.
     * 하지만 이들의 사용은 호출하는 코드와 함께 동작이 비동기적으로(즉, 동시적으로) 실행됨을 의미합니다.
     *
     * 다음 예제는 위 함수들이 코루틴 바깥에서 쓰이는 경우입니다.
     */
    @Test
    fun `Async-style functions`() {
        val time = measureTimeMillis {
            val one = somethingUsefulOneAsync()
            val two = somethingUsefulTwoAsync()
            runBlocking {
                printlnThread("The answer is ${one.await() + two.await()}")
            }
        }
        printlnThread("Completed in $time ms")
    }
//    실행결과
//    Thread[Test worker @coroutine#3,5,main]            ||| The answer is 42
//    Thread[Test worker,5,main]                         ||| Completed in 1181 ms

    /**
     * 위와 같은 스타일의 프로그래밍은 다른 여러 언어에서 쓰였기 때문에 소개되었지만
     * **코틀린에서 굉장히 비추천하는 방식입니다.**
     *
     * 만약 `val one = somethingUsefulOneAsync()` 행과 `one.await()` 표현식 사이에
     * 어떤 논리 오류가 발생하면 프로그램이 예외를 발생시키고 프로그램에서 수행중인 작업이 중단됩니다.
     *
     * 일반적으로 전역 오류 처리기는 이 예외를 catch하고 개발자에게
     * 오류를 보고하지만 프로그램은 다른 작업을 계속 수행 할 수 있습니다.
     *
     * 하지만 `somethingUsefulOneAsync`는 그것을 시작한 작업이
     * 중단되었음에도 불구하고 여전히 background에서 돌아갑니다.
     *
     * 이 문제는 아래 절에서 볼 수 있듯이 structured concurrency 에서는 발생하지 않습니다.
     */
    @Suppress("unused") fun dummy5() {}

    /**
     * ## Structured concurrency with async
     *
     * `Concurrent using async`의 예제에서 [doSomethingUsefulOne]과 [doSomethingUsefulTwo]를
     * 동시에 수행하고 이 결과값을 반환하는 함수를 추출해보겠습니다.
     *
     * `async` 코루틴 빌더는 [CoroutineScope]의 확장으로 정의되어 있어서 범위에 포함되어야합니다.
     * 이럴 때 아래와 같이 `coroutineScope` 함수를 사용해주면 됩니다.
     */
    private suspend fun concurrentSum(): Int = coroutineScope {
        val one = async { doSomethingUsefulOne() }
        val two = async { doSomethingUsefulTwo() }
        one.await() + two.await()
    }

    /**
     * 이 방법에서는 [concurrentSum] 함수내에서 무언가 잘못되면 예외가 던져지고
     * 해당 scope 내에서 실행된 모든 코루틴이 취소됩니다.
     */
    @Test
    fun `Structured concurrency with async`() = runBlocking {
        val time = measureTimeMillis {
            println("The answer is ${concurrentSum()}")
        }
        println("Completed in $time ms")
    }
//    실행결과
//    The answer is 42
//    Completed in 1027 ms


    /**
     * 우리는 위의 main 함수의 출력으로부터 분명히 알 수 있듯이 여전히 두 연산을 동시에 실행합니다.
     * 취소는 항상 coroutines 계층 구조를 통해 전파됩니다.
     */
    @Test
    fun `Structured concurrency with async 2`() = runBlocking<Unit> {
        try {
            failedConcurrentSum()
        } catch (e: ArithmeticException) {
            println("Computation failed with ArithmeticException")
        }
    }

    private suspend fun failedConcurrentSum(): Int =
        coroutineScope {
            val one = async {
                try {
                    delay(Long.MAX_VALUE) // Emulates very long computation
                    42
                } finally {
                    println("First child was cancelled")
                }
            }
            val two = async<Int> {
                println("Second child throws an exception")
                throw ArithmeticException()
            }
            one.await() + two.await()
        }
//    실행결과
//    Second child throws an exception
//    First child was cancelled
//    Computation failed with ArithmeticException
}