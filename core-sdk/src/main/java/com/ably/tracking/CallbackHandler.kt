package com.ably.tracking

interface CallbackHandler {
    fun onSucces()
    fun onError(exception: Exception)
}
