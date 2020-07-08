package com.github.galcyurio

import kotlinx.coroutines.CancellationException

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
     */

}