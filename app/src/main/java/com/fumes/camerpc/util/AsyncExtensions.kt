package com.fumes.camerpc.util


import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> ListenableFuture<T>.await(): T {
    return suspendCancellableCoroutine { cont ->
        addListener(
            {
                try {
                    cont.resume(get())
                } catch (e: ExecutionException) {
                    cont.resumeWithException(e.cause ?: e)
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            },
            { command -> command.run() } // Direct executor for simplicity
        )
        
        cont.invokeOnCancellation {
            cancel(false)
        }
    }
}
