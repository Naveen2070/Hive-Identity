package com.thehiveproject.identity_service.auth.security

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.thehiveproject.identity_service.auth.JwtService
import com.thehiveproject.identity_service.auth.TokenBlacklistService
import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,

    private val userDetailsService: UserDetailsService,

    @param:Qualifier("handlerExceptionResolver")
    private val resolver: HandlerExceptionResolver,
    private val tokenBlacklistService: TokenBlacklistService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        val authHeader = request.getHeader("Authorization")

        // If no token or header doesn't start with Bearer, continue filter chain
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(7)

        if (tokenBlacklistService.isBlacklisted(token)) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            val body = mapOf(
                "status" to "error",
                "message" to "You have logged out, please login again"
            )
            val mapper = jacksonObjectMapper()
            response.writer.write(mapper.writeValueAsString(body))
            return
        }

        try {
            val username = jwtService.extractUsername(token)

            if (SecurityContextHolder.getContext().authentication == null) {
                val userDetails = userDetailsService.loadUserByUsername(username)

                if (jwtService.isTokenValid(token, userDetails)) {
                    val authToken = UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.authorities
                    )
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authToken
                }
            }

        } catch (ex: ExpiredJwtException) {
            resolver.resolveException(request, response, null, ex)
            return
        }
        // Continue filter chain
        filterChain.doFilter(request, response)
    }

}