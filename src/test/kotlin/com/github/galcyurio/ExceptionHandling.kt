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

    /**
     * ## CoroutineExceptionHandler
     *
     * uncaught exception을 콘솔에 출력하는 기본 동작을 커스텀하는 것도 가능합니다.
     * root 코루틴의 [CoroutineExceptionHandler] 컨텍스트 요소는 이 root 코루틴 및 사용자 정의 예외 처리가
     * 발생할 수있는 모든 하위 요소에 대한 일반 catch 블록으로 사용될 수 있습니다.
     * 이 점은 `Thread.uncaughtExceptionHandler`와 유사합니다.
     * CoroutineExceptionHandler에 있는 예외를 복구할 수 없습니다.
     * 핸들러가 호출될 때 코루틴이 이미 해당 예외와 함께 완료되었습니다.
     * 일반적으로 핸들러는 예외를 로그하고 오류 메세지를 표시한 뒤 응용 프로그램을 종료 또는 다시 시작하는데 사용됩니다.
     *
     * JVM에서는 ServiceLoader를 통해 CoroutineExceptionHandler를 등록하여 모든 코루틴에 대한 전역 예외 핸들러를 재정의 할 수 있습니다.
     * 전역 예외 핸들러는 더 이상 특정 핸들러가 등록되지 않은 경우 사용되는 `Thread.defaultUncaughtExceptionHandler`와 유사합니다.
     * 안드로이드에서는 `uncaughtExceptionPreHandler`를 전역 코루틴 예외 핸들러로 설정합니다.
     *
     * CoroutineExceptionHandler는 다른 방식으로 처리되지 않은 uncaught exception에서만 호출됩니다.
     * 특히, 모든 자식 코루틴들은 예외 처리를 부모 코루틴에게 위임합니다.
     * 그 부모는 다시 root 코루틴에 닿을 때까지 부모 코루틴에게 위임합니다.
     * 따라서 컨텍스트에 설정된 CoroutineExceptionhandler는 절대로 사용되지 않습니다.
     * 또한 [async] 빌더는 항상 모든 예외를 잡아서 결과를 [Deferred]에서
     * 이를 표시하므로 [CoroutineExceptionHandler]에 영향을 미치지 않습니다.
     *
     * > supervision scope에서 실행되는 코루틴은 부모로 예외를 전파하지 않으며 이 규칙에서 제외됩니다.
     */
    @Test
    fun CoroutineExceptionHandler() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("CoroutineExceptionHandler got $exception")
        }
        val job = GlobalScope.launch(handler) { // root coroutine, running in GlobalScope
            throw AssertionError()
        }
        val deferred = GlobalScope.async(handler) { // also root, but async instead of launch
            throw ArithmeticException() // Nothing will be printed, relying on user to call deferred.await()
        }
        joinAll(job, deferred)
    }
}