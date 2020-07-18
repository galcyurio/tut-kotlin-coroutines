package com.github.galcyurio

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.produce
import org.junit.Test
import java.io.IOException

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

    /**
     * ## 취소와 예외 (Cancellation and exceptions)
     *
     * 취소는 예외와 밀접한 관련이 있습니다.
     * 코루틴은 취소를 위해 내부적으로 [CancellationException]을 사용하며 이 예외는 모든 핸들러에서 무시처리됩니다.
     * 따라서 catch 블록으로 얻을 수 있는 추가 디버그 정보의 소스로만 사용해야 합니다.
     * 코루틴이 [Job.cancel]로 취소되면 코루틴은 종료되지만 부모는 취소되지 않습니다.
     */
    @Test
    fun `Cancellation and exceptions`() = runBlocking {
        val job = launch {
            val child = launch {
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    println("Child is cancelled")
                }
            }
            yield()
            println("Cancelling child")
            child.cancel()
            child.join()
            yield()
            println("Parent is not cancelled")
        }
        job.join()
    }

    /**
     * 코루틴이 [CancellationException] 이외의 예외를 만나면 해당 예외로 부모를 취소합니다.
     * 이 동작은 재정의할 수 없으며 structured concurrency에 대한 안정적인 코루틴을 제공하는데 사용됩니다.
     * [CoroutineExceptionHandler] 구현체는 자식 코루틴에 사용되지 않습니다.
     *
     * > 이 예제에서 CoroutineExceptionHandler는 항상 GlobalScope에서 작성된 코 루틴에 설치됩니다.
     * main runBlocking에서 실행되는 코루틴에 예외 핸들러를 설치하는 것은 핸들러가 설치되어
     * 있음에도 불구하고 자식이 예외로 완료되면 main 코루틴이 항상 취소되므로 의미가 없습니다.
     *
     * 기존 예외는 모든 자식이 종료될 때만 부모에 의해 처리됩니다.
     */
    @Test
    fun `Cancellation and exceptions 2`() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("CoroutineExceptionHandler got $exception")
        }
        val job = GlobalScope.launch(handler) {
            launch { // the first child
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    withContext(NonCancellable) {
                        println("Children are cancelled, but exception is not handled until all children terminate")
                        delay(100)
                        println("The first child finished its non cancellable block")
                    }
                }
            }
            launch { // the second child
                delay(10)
                println("Second child throws an exception")
                throw ArithmeticException()
            }
        }
        job.join()
    }

    /**
     * ## Exceptions aggregation
     *
     * 여러개의 자식 코루틴이 실패하여 예외가 던져졌을 때 기본적인 규칙은 "첫 번째 예외가 우선" 이며
     * 따라서 첫번째 예외가 처리됩니다.
     * 첫번째 예외를 제외하고 나중에 일어난 모든 예외들은 첫번째 예외에 suppressed 예외로 첨부됩니다.
     */
    @Test
    fun `Exceptions aggregation`() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("CoroutineExceptionHandler got $exception with suppressed ${exception.suppressed?.contentToString()}")
        }
        val job = GlobalScope.launch(handler) {
            launch {
                try {
                    delay(Long.MAX_VALUE) // it gets cancelled when another sibling fails with IOException
                } finally {
                    throw ArithmeticException() // the second exception
                }
            }
            launch {
                delay(100)
                throw IOException() // the first exception
            }
            delay(Long.MAX_VALUE)
        }
        job.join()
    }

    /**
     * 취소 예외는 투명하며 기본적으로 래핑되지 않습니다.
     */
    @Test
    fun `Exceptions aggregation 2`() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("CoroutineExceptionHandler got $exception")
        }
        val job = GlobalScope.launch(handler) {
            val inner = launch { // all this stack of coroutines will get cancelled
                launch {
                    launch {
                        throw IOException() // the original exception
                    }
                }
            }
            try {
                inner.join()
            } catch (e: CancellationException) {
                println("Rethrowing CancellationException with original cause")
                throw e // cancellation exception is rethrown, yet the original IOException gets to the handler
            }
        }
        job.join()
    }

    /**
     * ## Supervision
     *
     * 이전에 공부했듯이 취소는 코루틴의 전체 계층을 통해 전파되는 양방향 관계입니다.
     * 단방향 취소가 필요한 경우를 살펴봅시다.
     *
     * 이러한 요구 사항의 좋은 예는 해당 scope에 job이 정의 된 UI 구성 요소입니다.
     * UI 자식 작업 중 하나가 실패하면 모든 UI 컴포넌트를 취소할 필요는 없습니다.
     * 그러나 UI 구성 요소가 삭제되고 (작업이 취소되면) 결과가 더 이상 필요하지 않으므로 모든 하위 작업이 실패해야 합니다.
     *
     * 또 다른 예는 여러 하위 작업을 생성하고 실행을 감독하고 실패를 추적하고
     * 실패한 작업만 다시 시작해야하는 서버 프로세스입니다.
     *
     * ### Supervision job
     *
     * [SupervisorJob]은 이러한 목적을 위해 사용됩니다.
     * 취소가 아래쪽으로만 전파된다는 점을 제외하면 일반 Job과 비슷합니다.
     */
    @Test
    fun `Supervision job`() = runBlocking {
        val supervisor = SupervisorJob()
        with(CoroutineScope(coroutineContext + supervisor)) {
            // launch the first child -- its exception is ignored for this example (don't do this in practice!)
            val firstChild = launch(CoroutineExceptionHandler { _, _ ->  }) {
                println("The first child is failing")
                throw AssertionError("The first child is cancelled")
            }
            // launch the second child
            val secondChild = launch {
                firstChild.join()
                // Cancellation of the first child is not propagated to the second child
                println("The first child is cancelled: ${firstChild.isCancelled}, but the second one is still active")
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    // But cancellation of the supervisor is propagated
                    println("The second child is cancelled because the supervisor was cancelled")
                }
            }
            // wait until the first child fails & completes
            firstChild.join()
            println("Cancelling the supervisor")
            supervisor.cancel()
            secondChild.join()
        }
    }

    /**
     * ### Supervision scope
     *
     * scoped concurrency를 위해서 [coroutineScope] 대신에 [supervisorScope]를 사용할 수 있습니다.
     * 취소는 한 방향으로만 전파하고 모든 자식들은 자신이 실패했을 경우에만 자신을 취소합니다.
     * 또한 [coroutineScope]와 마찬가지로 모든 자식들이 완료되기를 기다립니다.
     */
    @Test
    fun `Supervision scope`() = runBlocking {
        try {
            supervisorScope {
                val child = launch {
                    try {
                        println("The child is sleeping")
                        delay(Long.MAX_VALUE)
                    } finally {
                        println("The child is cancelled")
                    }
                }
                // Give our child a chance to execute and print using yield
                yield()
                println("Throwing an exception from the scope")
                throw AssertionError()
            }
        } catch(e: AssertionError) {
            println("Caught an assertion error")
        }
    }
}