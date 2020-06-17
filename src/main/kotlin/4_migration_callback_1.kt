
import kotlinx.coroutines.runBlocking
import okhttp3.*
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// 콜백함수를 등록할 수 있는 Future 타입을 반환하는 경우

//===========================================================================
//  ~ Before Migration
//===========================================================================

private fun requestUsers(): Call {
    val client = OkHttpClient()
    val request = Request.Builder()
        .get()
        .url("https://api.github.com/users")
        .build()
    return client.newCall(request)
}

private fun main1() {
    requestUsers().enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            // 예외 처리
        }

        override fun onResponse(call: Call, response: Response) {
            if (!response.isSuccessful) {
                // 요청 실패 처리
            }

            val body = response.body?.string()
            println(body) // 응답 처리
        }
    })
    // 참고: OkHttp 쓰레드 풀이 남아서 자동 종료 되지 않음
}

//===========================================================================
//  ~ After Migration
//===========================================================================

private suspend fun Call.await(): String? = suspendCoroutine { continuation ->
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            continuation.resume(null)
        }

        override fun onResponse(call: Call, response: Response) {
            val body = response.body?.string()
            continuation.resume(body)
        }
    })
}

// 프로덕션 코드에서 runBlocking 사용하지 말 것
private fun main2() = runBlocking {
    val body = requestUsers().await()
    if (body == null) {
        // 요청 실패 처리
    } else {
        // 성공 처리
        println(body)
    }
}