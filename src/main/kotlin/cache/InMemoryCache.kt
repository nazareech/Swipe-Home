package com.swipehome.cache

import com.swipehome.features.register.RegisterReceivedRemote
data class TokenCache(
    val login: String,
    val token: String
)

object InMemoryCache {
    val userList: MutableList<RegisterReceivedRemote> = mutableListOf()
    val token: MutableList<TokenCache> = mutableListOf()
}