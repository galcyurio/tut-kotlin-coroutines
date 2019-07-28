@file:Suppress("SpellCheckingInspection", "unused")

package com.github.galcyurio

import kotlinx.coroutines.*
import org.junit.Test
import kotlin.coroutines.CoroutineContext

/**
 * 코루틴은 항상 Kotlin 표준 라이브러리에 정의 된 [CoroutineContext] 타입의 context에서 실행됩니다.
 * coroutine context는 다양한 elements의 집합입니다.
 * 주요 element는 이전에 본 코루틴의 [Job]과 이 섹션에서 다루는 `dispatcher`입니다.
 *
 * @author galcyurio
 */
class CoroutineContextAndDispatchers {

    /**
     * ## Dispatchers and threads
     *
     * 코루틴 컨텍스트에는 해당 코루틴이 실행에 사용하는
     * 스레드를 결정하는 coroutine dispatcher가 포함되어 있습니다.
     * coroutine dispatcher는 코루틴 실행을 특정 스레드로 제한하거나
     * 스레드 풀로 dispatch하거나 제한하지 않은 상태로 둘 수 있습니다.
     *
     * [launch]와 [async]와 같은 모든 코루틴 빌더는 새로운 코루틴 및 다른 컨텍스트 요소에 대한
     * dispatcher를 명시적으로 지정하는 데 사용할 수 있는 선택적 [CoroutineContext] 매개변수가 있습니다.
     * */
    @Test fun `Dispatchers and threads`() = runBlocking {
        launch {
            // context of the parent, main runBlocking coroutine
            println("main runBlocking      : I'm working in thread ${Thread.currentThread().name}")
        }
        launch(Dispatchers.Unconfined) {
            // not confined -- will work with main thread
            println("Unconfined            : I'm working in thread ${Thread.currentThread().name}")
        }
        launch(Dispatchers.Default) {
            // will get dispatched to DefaultDispatcher
            println("Default               : I'm working in thread ${Thread.currentThread().name}")
        }
        @Suppress("EXPERIMENTAL_API_USAGE")
        launch(newSingleThreadContext("MyOwnThread")) {
            // will get its own new thread
            println("newSingleThreadContext: I'm working in thread ${Thread.currentThread().name}")
        }
        Unit
    }
//    Unconfined            : I'm working in thread Test worker @coroutine#3
//    Default               : I'm working in thread DefaultDispatcher-worker-1 @coroutine#4
//    newSingleThreadContext: I'm working in thread MyOwnThread @coroutine#5
//    main runBlocking      : I'm working in thread Test worker @coroutine#2

    /**
     * `launch {...}`이 매개변수 없이 쓰이면 시작된
     * [CoroutineScope]의 context를 상속받습니다. (따라서 dispatcher도 상속됨)
     * 이 경우에는 main 스레드에서 돌아가는 main `runBlocking`의 context를 상속받습니다.
     *
     * [Dispatchers.Unconfined]는 main 스레드에서 실행되는 것으로 보이는 특수한 dispatcher지만
     * 사실은 나중에 설명할 다른 매커니즘입니다.
     *
     * [GlobalScope]에서 코루틴을 시작할 때 사용되는 default dispatcher는
     * [Dispatchers.Default]가 대표적이며 공유되는 백그라운드 스레드 풀이 사용됩니다.
     * 따라서 `launch(Dispatchers.Default) {...}`는 `GlobalScope.launch {...}`와 같은 dispatcher를 사용합니다.
     *
     * [newSingleThreadContext] 코루틴이 실행될 새로운 스레드를 생성합니다.
     * 전용 스레드는 매우 값 비싼 자원입니다.
     * 실제 응용 프로그램에서는 더 이상 필요하지 않을 때 [ExecutorCoroutineDispatcher.close]를
     * 통해서 반드시 해제되거나 또는 가장 높은 수준의 변수에 저장된 후 다시 재사용되어야 합니다.
     */
    fun dummy1() {}


    /**
     * ## Unconfined vs confined dispatcher
     *
     * [Dispatchers.Unconfined] 코루틴 dispatcher는 첫번째 suspension 지점까지는 코루틴을 호출한 스레드에서 시작합니다.
     * 그리고 suspension 이후 호출된 susepending function에 의해 완전히 결정된 스레드에서 재개됩니다.
     * Unconfined dispatcher는 코루틴이 CPU 시간을 소비하지 않고 특정 스레드에만 국한된
     * 공유 데이터(UI 등)을 업데이트하지 않을 때 적합합니다.
     *
     * 그 외에는 기본적으로 외부 [CoroutineScope]에 대한 dispatcher가 상속됩니다.
     * [runBlocking] 코루틴을 위한 기본 dispatcher는 호출한 스레드에 한정되며
     * 따라서 이를 상속하면 예측가능한 FIFO 스케쥴링을 사용하여 이 스레드에 실행을 한정하는 효과가 있습니다.
     */
    @Test fun `Unconfined vs confined dispatcher`() = runBlocking {
        launch(Dispatchers.Unconfined) {
            // not confined -- will work with main thread
            println("Unconfined      : I'm working in thread ${Thread.currentThread().name}")
            delay(500)
            println("Unconfined      : After delay in thread ${Thread.currentThread().name}")
        }
        launch {
            // context of the parent, main runBlocking coroutine
            println("main runBlocking: I'm working in thread ${Thread.currentThread().name}")
            delay(1000)
            println("main runBlocking: After delay in thread ${Thread.currentThread().name}")
        }
        Unit
    }
//    Unconfined      : I'm working in thread Test worker @coroutine#2
//    main runBlocking: I'm working in thread Test worker @coroutine#3
//    Unconfined      : After delay in thread kotlinx.coroutines.DefaultExecutor @coroutine#2
//    main runBlocking: After delay in thread Test worker @coroutine#3

    /**
     * `runBlocking {...}` 컨텍스트를 상속받은 코루틴은 계속해서 `main` 스레드에서 작동하지만
     * unconfined의 경우는 [delay] 함수가 사용하는 기본 executor 스레드에서 재개되었습니다.
     *
     * Unconfined dispatcher는 코루틴에서 일부 연산은 즉시 수행되어야 하기 때문에
     * 코루틴을 나중에 실행할 필요가 없거나 원하지 않은 부작용을 유발하는
     * 특별한 케이스에 도움이 되는 고급 메카니즘입니다.
     * 일반적인 코드에서는 Unconfined dispatcher는 사용하지 않아야 합니다.
     */
    fun dummy2() {}


}