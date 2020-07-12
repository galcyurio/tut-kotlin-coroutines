package com.github.galcyurio

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import org.junit.Test

/**
 * 이번 섹션에서는 예외 처리와 예외가 발생했을 때의 취소에 대해서 알아봅니다.
 * 우리는 이미 코루틴이 suspension point에서 [CancellationException]을 던진다는 것과
 * 코루틴에 의해 무시된다는 것을 알고 있습니다.
 * 여기서 우리는 취소하는 동안 예외가 발생하거나 동일한 코루틴의 여러 자식이 예외를 던지는 경우
 * 어떻게 되는지 살펴봅시다.
 */
class ExceptionHandling {

    /**
     * ## 예외 전파 (Exception Propagation)
     *
     * 코루틴 빌더는 예외를 자동으로 전파하거나 ([launch], [actor])
     * 사용자에게 노출하는 ([async], [produce]) 두 가지 방식이 있습니다.
     *
     * 이 빌더가 root 코루틴을 만드는데 사용되었다면 이는 다른 코루틴의 자식이 아닙니다.
     * 전자의 빌더는 예외를 Java의 `Thread.uncaughtExceptionHandler`와 유사하게 uncaught 예외로 취급하지만
     * 후자는 최종 예외를 소비하기 위해 사용자에 의존합니다.
     * 예를 들면 await 또는 receive를 이용합니다.
     *
     * [GlobalScope]를 사용해 root 코루틴을 만드는 간단한 예제를 통해 설명할 수 있습니다.
     */
    @Test
    fun `Exception Propagation`() = runBlocking {
        val job = GlobalScope.launch {
            println("Throwing exception from launch")
            throw IndexOutOfBoundsException() // Thread.defaultUncaughtExceptionHandler에 의해서 콘솔에 출력될 것 입니다.
        }
        job.join()
        println("Joined failed job")
        val deferred = GlobalScope.async { // root coroutine with async
            println("Throwing exception from async")
            throw ArithmeticException() // 아무것도 출력되지 않고 사용자가 await를 호출하길 기다립니다.
        }
        try {
            deferred.await()
            println("Unreached")
        } catch (e: ArithmeticException) {
            println("Caught ArithmeticException")
        }
    }
}