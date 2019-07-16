package com.github.galcyurio

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

/**
 * ## Sequential by default
 *
 * 원격 서비스 요청 또는 연산 같은 유용한 작업을 하는 2개의 suspending function이 있다고 가정해봅시다.
 * 이 2개의 함수가 유용하다고 가정하지만 실제로는 이 예제의 목적을 위해 잠깐 지연됩니다.
 * */

suspend fun doSomethingUsefulOne(): Int {
    delay(1000L) // pretend we are doing something useful here
    return 13
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(1000L) // pretend we are doing something useful here, too
    return 29
}

/**
 * 위 함수들을 순차적으로 호출하려면 어떻게 해야할까요?
 * 코루틴의 코드는 일반 코드와 마찬가지로 기본적으로 순차적이기 때문에 보통 순차적으로 호출합니다.
 * */

//fun main() = runBlocking {
//    val time = measureTimeMillis {
//        val one = doSomethingUsefulOne()
//        val two = doSomethingUsefulTwo()
//        println("The answer is ${one + two}")
//    }
//    println("Completed in $time ms")
//}

// 실행결과
// The answer is 42
// Completed in 2015 ms


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
 * [Deferred]에 `.await()`를 사용해서 값을 얻을 수 있으며 [Deffered] 또한 [Job] 이기 때문에 취소할 수도 있습니다.
 *
 * 아래 예제는 두 개의 코루틴을 동시에 실행하기 때문에 두 배 빠릅니다.
 * 코루틴의 동시성은 항상 명시적이라는 걸 기억하세요.
 * */
fun main() = runBlocking {
    val time = measureTimeMillis {
        val one = async { doSomethingUsefulOne() }
        val two = async { doSomethingUsefulTwo() }
        println("The answer is ${one.await() + two.await()}")
    }
    println("Completed in $time ms")
}
// 실행결과
// The answer is 42
// Completed in 1030 ms
