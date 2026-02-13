package com.thehiveproject.identity_service.auth.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User


/**
 * Constructs a [User] with the details required by
 * [org.springframework.security.authentication.dao.DaoAuthenticationProvider].
 *
 * @param [username] the username presented to the
 * [org.springframework.security.authentication.dao.DaoAuthenticationProvider]
 * @param password the password that should be presented to the
 * [org.springframework.security.authentication.dao.DaoAuthenticationProvider]
 * @param enabled `true` if the user is enabled
 * @param accountNonExpired `true` if the account has not expired
 * @param credentialsNonExpired `true` if the credentials have not expired
 * @param accountNonLocked `true` if the account is not locked
 * @param authorities the authorities that should be granted to the caller if they
 * present the correct username and password and the user is enabled. Must not be null.
 *
 * @throws IllegalArgumentException if a `null` value is passed either as a parameter
 * or as an element in the `GrantedAuthority` collection
 */
class CustomUserDetails(
    val id: Long,
    username: String,
    password: String,
    enabled: Boolean,
    accountNonExpired: Boolean,
    credentialsNonExpired: Boolean,
    accountNonLocked: Boolean,
    authorities: Collection<GrantedAuthority>
) : User(
    username,
    password,
    enabled,
    accountNonExpired,
    credentialsNonExpired,
    accountNonLocked,
    authorities
)