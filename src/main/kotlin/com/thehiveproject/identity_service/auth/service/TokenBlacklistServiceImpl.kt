package com.thehiveproject.identity_service.auth.service

import com.google.common.cache.CacheBuilder
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class TokenBlacklistServiceImpl : TokenBlacklistService {

    // Configure the cache to expire entries after 30 minutes.
    // This is safe because your Access Tokens only last 15 minutes.
    private val blacklist = CacheBuilder.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .build<String, Boolean>()

    override fun blacklistToken(token: String) {
        blacklist.put(token, true)
    }

    override fun isBlacklisted(token: String): Boolean {
        return blacklist.getIfPresent(token) != null
    }
}