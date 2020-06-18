import kotlinx.coroutines.runBlocking
import okhttp3.*
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// 함수의 매개변수로 콜백함수를 받는 경우

//===========================================================================
//  ~ Before Migration
//===========================================================================

private fun requestUsers(
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .get()
        .url("https://api.github.com/users")
        .build()
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onFailure(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (!response.isSuccessful) {
                onFailure(IllegalStateException("request failed"))
                return
            }

            val body = response.body?.string()
            if (body == null) {
                onFailure(IllegalStateException("null response body"))
            } else {
                onSuccess(body)
            }
        }
    })
}

private fun main1() {
    requestUsers(
        onSuccess = { /* 성공 처리 */ println(it) },
        onFailure = { /* 실패 처리 */ }
    )
}

//===========================================================================
//  ~ After Migration
//===========================================================================

private suspend fun requestUsersSuspend(): String? = suspendCoroutine { continuation ->
    requestUsers(
        onSuccess = { continuation.resume(it) },
        onFailure = { continuation.resume(null) }
    )
}

private fun main2() = runBlocking {
    val users = requestUsersSuspend()
    println(users)
}