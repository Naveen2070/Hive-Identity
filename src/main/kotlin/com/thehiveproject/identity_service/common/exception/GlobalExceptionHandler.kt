package com.thehiveproject.identity_service.common.exception

import com.thehiveproject.identity_service.auth.exception.InvalidCredentialsException
import com.thehiveproject.identity_service.auth.exception.InvalidPasswordException
import com.thehiveproject.identity_service.auth.exception.InvalidRefreshTokenException
import com.thehiveproject.identity_service.auth.exception.TokenExpiredException
import com.thehiveproject.identity_service.user.exception.*
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.SignatureException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // 1. Handle "Not Found" (404)
    @ExceptionHandler(
        UserNotFoundException::class,
        RoleNotFoundException::class
    )
    fun handleNotFound(ex: RuntimeException, request: WebRequest): ResponseEntity<ApiErrorResponse> {
        val errorResponse = ApiErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = HttpStatus.NOT_FOUND.reasonPhrase,
            message = ex.message ?: "Resource not found",
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }

    // 2. Handle Validation Errors (400)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ApiErrorResponse> {
        val errors = ex.bindingResult.allErrors.joinToString(", ") { error ->
            error.defaultMessage ?: "Invalid value"
        }

        val errorResponse = ApiErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Validation Failed",
            message = errors,
            path = request.getDescription(false).replace("uri=", "")
        )

        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    // 3. Handle Expired JWT
    @ExceptionHandler(ExpiredJwtException::class)
    fun handleExpiredJwt(request: WebRequest): ResponseEntity<ApiErrorResponse> {
        val errorResponse = ApiErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = HttpStatus.UNAUTHORIZED.reasonPhrase,
            message = "Token has expired. Please log in again.",
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity(errorResponse, HttpStatus.UNAUTHORIZED)
    }

    // 4. Handle Invalid/Tampered JWTs
    @ExceptionHandler(
        MalformedJwtException::class,
        SignatureException::class,
        UnsupportedJwtException::class,
        IllegalArgumentException::class
    )
    fun handleInvalidJwt(ex: Exception, request: WebRequest): ResponseEntity<ApiErrorResponse> {
        logger.warn("Invalid Token Attempt: ${ex.message}")

        val errorResponse = ApiErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = HttpStatus.UNAUTHORIZED.reasonPhrase,
            message = "Invalid authentication token",
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity(errorResponse, HttpStatus.UNAUTHORIZED)
    }

    // 5. Handle General Authentication Errors (Custom + Spring Security)
    @ExceptionHandler(
        InvalidCredentialsException::class,
        TokenExpiredException::class,
        AuthenticationException::class,
        InvalidRefreshTokenException::class
    )
    fun handleAuthErrors(
        ex: RuntimeException,
        request: WebRequest
    ): ResponseEntity<ApiErrorResponse> {
        val errorResponse = ApiErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = HttpStatus.UNAUTHORIZED.reasonPhrase,
            message = ex.message ?: "Authentication failed",
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity(errorResponse, HttpStatus.UNAUTHORIZED)
    }

    // 6. Handle Conflict (409)
    @ExceptionHandler(
        UserAlreadyExistsException::class,
        UserAlreadyDeactivatedException::class,
        UserAlreadyDeletedException::class
    )
    fun handleConflict(
        ex: RuntimeException,
        request: WebRequest
    ): ResponseEntity<ApiErrorResponse> {
        val errorResponse = ApiErrorResponse(
            status = HttpStatus.CONFLICT.value(),
            error = HttpStatus.CONFLICT.reasonPhrase,
            message = ex.message ?: "Resource conflict",
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity(errorResponse, HttpStatus.CONFLICT)
    }

    // 7. Handle Forbidden (403)
    @ExceptionHandler(
        InvalidPasswordException::class
    )
    fun handleForbidden(
        ex: RuntimeException,
        request: WebRequest
    ): ResponseEntity<ApiErrorResponse> {
        val errorResponse = ApiErrorResponse(
            status = HttpStatus.FORBIDDEN.value(),
            error = HttpStatus.FORBIDDEN.reasonPhrase,
            message = ex.message ?: "Forbidden request",
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity(errorResponse, HttpStatus.CONFLICT)
    }

    // 8. Handle Everything Else (500)
    @ExceptionHandler(Exception::class)
    fun handleGlobalException(ex: Exception, request: WebRequest): ResponseEntity<ApiErrorResponse> {
        logger.error("Unexpected error", ex)

        val errorResponse = ApiErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            message = ex.message ?:"An unexpected error occurred. Please try again later.",
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}