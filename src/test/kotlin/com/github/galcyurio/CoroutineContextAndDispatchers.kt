@file:Suppress("SpellCheckingInspection", "unused")

package com.github.galcyurio

import com.github.galcyurio.support.printlnThread
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
     */
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

    /**
     * ## Debugging coroutines and threads
     *
     * 코루틴은 한 스레드에서 suspend되고 다른 스레드에서 다시 시작할 수 있습니다.
     * 단일 스레드 디스패처를 사용하더라도 코루틴이 수행한 작업, 위치 및 시기를 파악하기 어려울 수 있습니다.
     * 스레드를 사용하는 응용 프로그램을 디버깅하는 일반적인 방법은 각 로그의 로그 파일에 스레드 이름을 출력하는 것입니다.
     * 이 기능은 보편적으로 로깅 프레임워크에서 지원됩니다.
     * 코루틴을 사용할 때 스레드 이름만으로는 많은 것을 알 수 없습니다.
     * `kotlinx.coroutines`에는 디버깅 기능이 포함되어 있어 보다 쉽게 사용할 수 있습니다.
     */
    @Test
    fun `Debugging coroutines and threads`() = runBlocking {
        val a = async {
            log("I'm computing a piece of the answer")
            6
        }
        val b = async {
            log("I'm computing another piece of the answer")
            7
        }
        log("The answer is ${a.await() * b.await()}")
    }
//    [Test worker @coroutine#2] I'm computing a piece of the answer
//    [Test worker @coroutine#3] I'm computing another piece of the answer
//    [Test worker @coroutine#1] The answer is 42

    fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

    /**
     * `log` 함수는 스레드 이름을 대괄호로 출력하며,
     * 현재 실행중인 코루틴의 식별자가 추가된 main 스레드임을 알 수 있습니다.
     * 이 식별자는 디버깅 모드가 켜져있을 때 생성된 모든 코루틴에 연속적으로 할당됩니다.
     */
    fun dummy3() {}

    /**
     * ## Jumping between threads
     *
     * 아래는 몇가지 새로운 기술들을 보여줍니다.
     * 하나는 명시적으로 지정된 컨텍스트와 함께 [runBlocking]을 사용하는 것입니다.
     * 그리고 다른 하나는 [withContext] 함수를 사용하여 아래 출력에서 볼 수 있듯이 여전히
     * 동일한 코루틴에 머무르면서 코루틴의 컨텍스트를 변경합니다.
     *
     * 이 예제는 또한 Kotlin 표준 라이브러리의 use 함수를 사용하여
     * 더 이상 필요하지 않은 newSingleThreadContext로 작성된 스레드를 해제합니다.
     */
    @Test
    fun `Jumping between threads`() {
        newSingleThreadContext("Ctx1").use { ctx1 ->
            newSingleThreadContext("Ctx2").use { ctx2 ->
                runBlocking(ctx1) {
                    log("Started in ctx1")
                    withContext(ctx2) {
                        log("Working in ctx2")
                    }
                    log("Back to ctx1")
                }
            }
        }
    }
//    [Ctx1 @coroutine#1] Started in ctx1
//    [Ctx2 @coroutine#1] Working in ctx2
//    [Ctx1 @coroutine#1] Back to ctx1

    /**
     * ## Job in the context
     *
     * 코루틴의 [Job]은 컨텍스트의 일부이며 `coroutineContext[Job]`을 통해서 가져 올 수도 있습니다.
     *
     * 참고: [CoroutineScope]의 [isActive]는 `coroutineContext[Job]?.isActive == true` 표현을 단축한 것입니다.
     */
    @Test
    fun `Job in the context`() = runBlocking {
        println("My job is ${coroutineContext[Job]}")
    }
//    My job is "coroutine#1":BlockingCoroutine{Active}@5d2cd86b

    /**
     * ## Children of a coroutine
     *
     * 코루틴이 다른 코루틴의 [CoroutineScope]에서 시작되면 [CoroutineScope.coroutineContext]를 통해
     * 컨텍스트를 상속하며 새 코루틴의 작업은 부모(parent) 코루틴 작업의 자식(child)이 됩니다.
     *
     * 그런데 [GlobalScope]를 사용하여 코루틴을 시작하면 새 코루틴 작업의 부모가 없습니다.
     * 따라서 시작된 범위와 독립적이며 독립적으로 작동합니다.
     */
    @Test
    fun `Children of a coroutine`() = runBlocking<Unit> {
        val request = launch {
            // GlobalScope를 사용해서 두 개의 작업(job)을 만듭니다.
            GlobalScope.launch {
                println("job1: 나는 GlobalScope에서 독립적으로 실행됩니다!")
                delay(1000)
                println("job1: 나는 요청의 취소에 아무런 영향을 받지 않습니다")
            }
            // 그리고 다른 작업들은 부모 컨텍스트를 상속받습니다.
            launch {
                delay(100)
                println("job2: 나는 요청한 코루틴의 자식입니다")
                delay(1000)
                println("job2: 나는 부모 요청이 취소되면 이 라인을 수행하지 않습니다")
            }
        }
        delay(500)
        request.cancel() // 요청 취소
        delay(1000) // 무슨 일이 벌어지는지 확인하기 위해 1초 지연
        println("main: 누가 요청 취소로부터 살아남았니?")
    }
//    job1: 나는 GlobalScope에서 독립적으로 실행됩니다!
//    job2: 나는 요청한 코루틴의 자식입니다
//    job1: 나는 요청의 취소에 아무런 영향을 받지 않습니다
//    main: 누가 요청 취소로부터 살아남았니?

    /**
     * ## 부모의 책임 (Parental responsibilities)
     *
     * 부모 코루틴은 항상 자식 코루틴들이 완료되기를 기다립니다.
     * 부모는 자식이 시작한 모든 자식을 명시적으로 추적할 필요가 없으며
     * [Job.join] 함수를 사용하여 끝에서 기다릴 필요도 없습니다.
     */
    @Test
    fun `Parental responsibilities`() = runBlocking<Unit> {
        val request = launch {
            repeat(3) { i ->
                launch {
                    delay((i + 1) * 200L) // variable delay 200ms, 400ms, 600ms
                    println("Coroutine $i is done")
                }
            }
            println("request: 나는 완료했고 아직 실행중인 내 자식들을 명시적으로 join 하지 않습니다")
        }
        request.join() // 모든 자식을 포함한 요청의 완료를 기다린다.
        println("요청이 완료되었습니다")
    }
//    request: 나는 완료했고 아직 실행중인 내 자식들을 명시적으로 join 하지 않습니다
//    Coroutine 0 is done
//    Coroutine 1 is done
//    Coroutine 2 is done
//    요청이 완료되었습니다

    /**
     * ## 디버깅을 위해 코루틴에 이름 지어주기 (Naming coroutines for debugging)
     *
     * 코루틴이 로그를 자주 기록할 때 자동으로 할당된 ID가 적합하며
     * 동일한 코루틴에서 오는 로그 레코드와 연관지어 합쳐주면 됩니다.
     *
     * 그러나 코루틴이 특정 요청을 처리하거나 특정 백그라운드 작업을 수행할 때는
     * 디버깅을 위해 명시적으로 이름을 지어주는 것이 좋습니다.
     *
     * [CoroutineName] 컨텍스트 요소는 스레드 이름과 동일한 용도로 사용됩니다.
     * 디버깅 모드가 켜질 때 이 코루틴을 실행하는 스레드 이름에 포함됩니다.
     */
    @Test
    fun `Naming coroutines for debugging`() = runBlocking<Unit> {
        log("Started main coroutine")
        val v1 = async(CoroutineName("v1coroutine")) {
            delay(500)
            log("Computing v1")
            252
        }
        val v2 = async(CoroutineName("v2coroutine")) {
            delay(1000)
            log("Computing v2")
            6
        }
        log("The answer for v1 / v2 = ${v1.await() / v2.await()}")
    }
//    [Test worker @coroutine#1] Started main coroutine
//    [Test worker @v1coroutine#2] Computing v1
//    [Test worker @v2coroutine#3] Computing v2
//    [Test worker @coroutine#1] The answer for v1 / v2 = 42

    /**
     * ## 컨텍스트 요소 합치기 (Combining context elements)
     *
     * 가끔씩 코루틴 컨텍스트에 여러 요소들을 정의해야하는 경우가 있습니다.
     * 이를 위해 `+` 연산자를 사용할 수 있습니다.
     * 예를 들면 명시적으로 지정된 디스패처와 명시적으로 지정된 이름을 가진 코루틴을 시작할 수 있습니다.
     */
    @Test
    fun `Combining context elements`() = runBlocking<Unit> {
        launch(Dispatchers.Default + CoroutineName("foobar")) {
            printlnThread("I'm working in thread")
        }
    }
//    Thread[DefaultDispatcher-worker-1 @foobar#2,5,main] ||| I'm working in thread

    /**
     * ## Coroutine scope
     *
     * context, children, job에 대해 알아봅시다.
     * 우리의 애플리케이션에 생명주기가 있는 객체가 있고 해당 객체가 코루틴은 아니라고 가정합시다.
     *
     * 예를 들어 우리가 안드로이드 애플리케이션을 만들고 안드로이드 Activity의 컨텍스트에서
     * 다양한 코루틴을 시작하여 비동기 작업을 수행하여 데이터를 가져오고 업데이트하고
     * 애니메이션을 수행하는 등의 작업을 수행합니다.
     *
     * activity가 파괴되면 메모리 누수를 방지하기 위해 모든 코루틴들은 취소되어야합니다.
     * 물론 우리는 activity의 생명주기와 그 코루틴을 묶기 위해 컨텍스트와 작업을 수동으로 조작할 수 있습니다.
     * 하지만 `kotlinx.coroutines`는 다음을 캡슐화하여 추상화된 클래스 [CoroutineScope]를 제공합니다.
     *
     * 모든 코루틴 빌더는 [CoroutineScope] 확장으로 선언되어 있으니 반드시 잘 알아두어야 합니다.
     *
     * 우리는 Activity의 생명주기에 연결된 [CoroutineScope]의 인스턴스를 만들어 코루틴의 생명주기를 관리합니다.
     * [CoroutineScope] 인스턴스는 `CoroutineScope()` 또는 `MainScope()` 팩토리 함수를 통해서 만들 수 있습니다.
     * 전자는 범용 scope를 생성하고 후자는 UI 애플리케이션에 대한 scope를 생성하며
     * [Dispatchers.Main]을 기본 디스패처로 사용합니다.
     */
    class Activity {
        private val mainScope = MainScope()

        fun destroy() {
            mainScope.cancel()
        }
        // to be continued ...
    }

    /**
     * 또는 이 Activity 클래스에서 [CoroutineScope] 인터페이스를 구현할 수 있습니다.
     * 가장 좋은 방법은 기본 팩토리 함수와 함께 위임을 사용하는 것입니다.
     * 또한 원하는 디스패처(이 예제에서는 [Dispatchers.Default])를 scope와 결합할 수도 있습니다.
     */
    class Activity2 : CoroutineScope by CoroutineScope(Dispatchers.Default) {
        // to be continued ...
    }

    class Activity3 : CoroutineScope by CoroutineScope(Dispatchers.Default) {
        // private val mainScope = MainScope()

        /**
         * 이제 컨텍스트를 명시적으로 지정하지 않고도 Activity의 범위에서 코루틴을 시작할 수 있습니다.
         * 데모를 위해 각각 다른 시간을 지연시키는 10개의 코루틴을 시작합니다.
         */
        fun doSomething() {
            repeat(10) { i ->
                launch {
                    delay((i + 1) * 200L) // variable delay 200ms, 400ms, ... etc
                    println("Coroutine $i is done")
                }
            }
        }

        fun destroy() {
            // mainScope.cancel()
            cancel()
        }
    }

    /**
     * 우리의 main 함수에서 Activity를 생성하고 `doSomething` 함수를 호출하고, 500ms 후에 Activity를 파괴해 봅시다.
     * 그러면 `doSomething`에서 시작된 모든 코루틴을 취소합니다.
     * Activity가 파괴된 후 조금 더 기다리더라도 더 이상 메시지가 출력되지 않기 때문에 알 수 있습니다.
     *
     * 결과를 보면 처음 두 코루틴만 메시지를 출력하고 나머지는 `Activity.destroy()`에서 `job.cancel()`을 한 번만 호출하면 취소됩니다.
     */
    @Test
    fun `Coroutine scope`() = runBlocking {
        val activity = Activity3()
        activity.doSomething() // run test function
        println("Launched coroutines")
        delay(500L) // delay for half a second
        println("Destroying activity!")
        activity.destroy() // cancels all coroutines
        delay(1000) // visually confirm that they don't work
    }
//    Launched coroutines
//    Coroutine 0 is done
//    Coroutine 1 is done
//    Destroying activity!

    /**
     * ## Thread-local data
     *
     * 때로는 일부 thread-local 데이터를 코루틴으로 또는 코루틴간에 전달하는 기능이 편리합니다.
     * 그러나 특정 스레드에 구속되지 않으므로 수동으로 수행하면 boilerplate로 이어질 수 있습니다.
     *
     * [ThreadLocal]의 경우에는 [asContextElement] 확장 함수가 있습니다.
     * 이 함수는 [ThreadLocal] 로부터 받은 값들을 유지하고
     * 코루틴이 컨텍스트를 전활할 때마다 이를 복구하는 컨텍스트 요소를 생성합니다.
     *
     * 다음 예제에서 [Dispatchers.Default]를 이용해 백그라운드 스레드 풀에서 새로운 코루틴을 시작했기 때문에
     * 스레드 풀과 다른 스레드에서 작동합니다.
     * 그러나 코루틴이 실행되는 스레드와 관계없이 `threadLocal.asContextElement(value = "launch")`을 사용하여
     * 지정한 스레드 로컬 변수의 값이 여전히 존재합니다.
     */
    @Test
    fun `Thread-local data`() = runBlocking<Unit> {
        val threadLocal = ThreadLocal<String>()
        threadLocal.set("main")
        printlnThread("Pre-main, thread local value: '${threadLocal.get()}'")
        val job = launch(Dispatchers.Default + threadLocal.asContextElement(value = "launch")) {
            printlnThread("Launch start, thread local value: '${threadLocal.get()}'")
            yield()
            printlnThread("After yield, thread local value: '${threadLocal.get()}'")
        }
        job.join()
        printlnThread("Post-main, thread local value: '${threadLocal.get()}'")
    }
//    Thread[Test worker @coroutine#1,5,main]                ||| Pre-main, thread local value: 'main'
//    Thread[DefaultDispatcher-worker-1 @coroutine#2,5,main] ||| Launch start, thread local value: 'launch'
//    Thread[DefaultDispatcher-worker-3 @coroutine#2,5,main] ||| After yield, thread local value: 'launch'
//    Thread[Test worker @coroutine#1,5,main]                ||| Post-main, thread local value: 'main'

    /**
     * 해당 컨텍스트 요소를 설정하는 것을 잊어버리기 쉽습니다.
     * 만약 코루틴이 동작하는 스레드가 다르다면 코루틴에서 접근한 thread-local 변수는 예상치 못한 값을 가지게 될 수 있습니다.
     * 이러한 상황을 피하려면 [ensurePresent] 메소드를 사용하여 부적절한 사용시에 빠르게 실패하는 것이 좋습니다.
     *
     * [ThreadLocal]는 first-class를 지원하며 기본 `kotlinx.coroutines`에서 제공하는 모든 것과 함께 사용할 수 있습니다.
     * 그러나 한 가지 중요한 제한사항이 있습니다.
     * 스레드 로컬이 변경되면 컨텍스트 요소가 모든 [ThreadLocal] 객체 액세스를 추적할 수 없기 때문에
     * 새로운 값이 코루틴 호출자에게 전파되지 않으며 다음 중단(suspend)시에 값을 잃게 됩니다.
     * 코루틴에서 스레드 로컬 값을 변경하려면 [withContext]를 사용하고 자세한 사항은 [asContextElement]를 참조하세요.
     *
     * 대안으로 `Counter(var i: Int)`와 같은 mutable box에 값을 저장하여 스레드 로컬 변수에 저장할 수 있습니다.
     * 하지만 이 방법은 mutable box 변수의 잠재적인 동시적인 수정에 대한 동기화를 다루어야합니다.
     *
     * MDC 로깅, 트랜잭션 컨텍스트 또는 내부적으로 데이터 전달을 위해 스레드 로컬을 사용하는
     * 다른 라이브러리와의 통합과 같은 사용법에 대해서는 [ThreadContextElement] 인터페이스 문서를 참조하세요.
     */
    fun dummy4() {}
}