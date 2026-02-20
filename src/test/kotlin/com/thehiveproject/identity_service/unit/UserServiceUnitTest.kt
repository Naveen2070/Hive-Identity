package com.thehiveproject.identity_service.unit

import com.thehiveproject.identity_service.auth.dto.CreateUserRequest
import com.thehiveproject.identity_service.auth.exception.InvalidPasswordException
import com.thehiveproject.identity_service.auth.service.RefreshTokenService
import com.thehiveproject.identity_service.user.dto.ChangePasswordRequest
import com.thehiveproject.identity_service.user.dto.UpdateProfileRequest
import com.thehiveproject.identity_service.user.dto.UserSummary
import com.thehiveproject.identity_service.user.entity.Role
import com.thehiveproject.identity_service.user.entity.User
import com.thehiveproject.identity_service.user.exception.*
import com.thehiveproject.identity_service.user.repository.RoleRepository
import com.thehiveproject.identity_service.user.repository.UserRepository
import com.thehiveproject.identity_service.user.service.UserServiceImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserServiceUnitTest {

    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var passwordEncoder: PasswordEncoder
    @Mock lateinit var refreshTokenService: RefreshTokenService
    @Mock lateinit var roleRepository: RoleRepository

    @InjectMocks
    lateinit var userService: UserServiceImpl

    private val defaultEmail = "test@test.com"
    private val defaultUserId = 1L

    // --- Helpers ---
    private fun createDummyUser(): User {
        val user = User(
            email = defaultEmail,
            passwordHash = "hashedPassword",
            fullName = "Test User",
            domainAccess = mutableSetOf("ALL")
        )
        user.id = defaultUserId
        return user
    }

    private fun createDummyRole(): Role {
        val role = Role(1,"ADMIN")
        return role
    }

    // ==========================================
    // 1. CREATE USER TESTS
    // ==========================================

    @Test
    fun `createInternalUser should save and return user when valid`() {
        val request = CreateUserRequest(
            email = defaultEmail,
            password = "password123",
            fullName = "Test User",
            domainAccess = mutableSetOf("ALL"),
            role = "ADMIN"
        )
        val role = createDummyRole()
        val savedUser = createDummyUser()

        `when`(userRepository.findByEmail(request.email)).thenReturn(Optional.empty())
        `when`(roleRepository.findByName(request.role)).thenReturn(Optional.of(role))
        `when`(passwordEncoder.encode(request.password)).thenReturn("hashedPassword")

        `when`(userRepository.save(any())).thenReturn(savedUser)

        val response = userService.createInternalUser(request)

        assertEquals(defaultEmail, response.email)
        verify(userRepository).save(check {
            assertEquals("hashedPassword", it.passwordHash)
            assertEquals("Test User", it.fullName)
        })
    }

    @Test
    fun `createInternalUser should throw exception if email already exists`() {
        val request = CreateUserRequest(
            email = defaultEmail,
            password = "pass",
            fullName = "Name",
            domainAccess = mutableSetOf("ALL"),
            role = "USER"
        )

        `when`(userRepository.findByEmail(defaultEmail)).thenReturn(Optional.of(createDummyUser()))

        assertThrows<UserAlreadyExistsException> {
            userService.createInternalUser(request)
        }
        verify(userRepository, never()).save(any())
    }
    @Test
    fun `createInternalUser should throw exception if role does not exist`() {
        val request = CreateUserRequest(defaultEmail, "password123", "Test User", mutableSetOf("ALL"), "SUPER_ADMIN")

        `when`(userRepository.findByEmail(request.email)).thenReturn(Optional.empty())
        `when`(roleRepository.findByName(request.role)).thenReturn(Optional.empty())

        assertThrows<RoleNotFoundException> {
            userService.createInternalUser(request)
        }
        verify(userRepository, never()).save(any())
    }

    // ==========================================
    // 2. READ & FETCH TESTS
    // ==========================================

    @Test
    fun `getUserProfile should return profile for existing user`() {
        `when`(userRepository.findByEmail(defaultEmail)).thenReturn(Optional.of(createDummyUser()))

        val response = userService.getUserProfile(defaultEmail)

        assertEquals(defaultEmail, response.email)
        assertEquals("Test User", response.fullName)
    }

    @Test
    fun `getAllUsers should return paginated list`() {
        val pageable = PageRequest.of(0, 10)
        val users = listOf(createDummyUser())
        val page = PageImpl(users, pageable, 1)

        // Mockito-Kotlin 'any()' handles the Specification wrapper safely
        `when`(userRepository.findAll(any<Specification<User>>(), eq(pageable))).thenReturn(page)

        val response = userService.getAllUsers(pageable, "Test")

        assertEquals(1, response.totalElements)
        assertEquals(defaultEmail, response.content[0].email)
    }

    @Test
    fun `getUserById should return user DTO when found`() {
        val user = createDummyUser()
        `when`(userRepository.findById(defaultUserId)).thenReturn(Optional.of(user))

        val response = userService.getUserById(defaultUserId)

        assertEquals(defaultEmail, response.email)
    }

    @Test
    fun `getUserSummaryById should return summary when found`() {
        val summary = mock(UserSummary::class.java)
        `when`(userRepository.findSummaryById(defaultUserId)).thenReturn(Optional.of(summary))

        val response = userService.getUserSummaryById(defaultUserId)

        assertNotNull(response)
    }

    @Test
    fun `getUserSummaryById should throw exception if not found`() {
        `when`(userRepository.findSummaryById(99L)).thenReturn(Optional.empty())

        assertThrows<UserNotFoundException> {
            userService.getUserSummaryById(99L)
        }
    }

    @Test
    fun `findBatchUserSummary should return list of summaries`() {
        val ids = listOf(1L, 2L, 3L)
        val summaries = listOf(mock(UserSummary::class.java), mock(UserSummary::class.java))

        `when`(userRepository.findByIdIn(ids)).thenReturn(summaries)

        val response = userService.findBatchUserSummary(ids)

        assertEquals(2, response.size)
    }

    // ==========================================
    // 3. PROFILE UPDATE & STATUS TESTS
    // ==========================================

    @Test
    fun `updateProfile should modify only provided fields`() {
        val user = createDummyUser()
        val request = UpdateProfileRequest(fullName = "Updated Name")

        `when`(userRepository.findByEmail(defaultEmail)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any())).thenReturn(user)

        val response = userService.updateProfile(defaultEmail, request)

        assertEquals("Updated Name", response.fullName)
        verify(userRepository).save(user)
    }

    @Test
    fun `changeUserStatus should deactivate user and revoke tokens when active is false`() {
        val user = spy(createDummyUser())

        `when`(userRepository.findById(defaultUserId)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any())).thenReturn(user)

        userService.changeUserStatus(defaultUserId, active = false)

        verify(user).deactivateUser()
        verify(refreshTokenService).revokeTokensForUser(defaultUserId)
        verify(userRepository).save(user)
    }

    @Test
    fun `changeUserStatus should activate user without revoking tokens when active is true`() {
        val user = spy(createDummyUser())

        `when`(userRepository.findById(defaultUserId)).thenReturn(Optional.of(user))
        `when`(userRepository.save(any())).thenReturn(user)

        userService.changeUserStatus(defaultUserId, active = true)

        verify(user).activateUser()
        verify(refreshTokenService, never()).revokeTokensForUser(anyLong())
        verify(userRepository).save(user)
    }

    // ==========================================
    // 4. PASSWORD CHANGE TESTS
    // ==========================================

    @Test
    fun `changePassword should save new hash if old password is correct`() {
        val user = createDummyUser()
        val request = ChangePasswordRequest("oldPass", "newPass")

        `when`(userRepository.findByEmail(defaultEmail)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches("oldPass", "hashedPassword")).thenReturn(true)
        `when`(passwordEncoder.encode("newPass")).thenReturn("newHashedPassword")

        userService.changePassword(defaultEmail, request)

        verify(userRepository).save(check {
            assertEquals("newHashedPassword", it.passwordHash)
        })
    }

    @Test
    fun `changePassword should throw exception if old password is incorrect`() {
        val user = createDummyUser()
        val request = ChangePasswordRequest("wrongOldPass", "newPass")

        `when`(userRepository.findByEmail(defaultEmail)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches("wrongOldPass", "hashedPassword")).thenReturn(false)

        assertThrows<InvalidPasswordException> {
            userService.changePassword(defaultEmail, request)
        }

        verify(userRepository, never()).save(any())
    }

    // ==========================================
    // 5. DELETION & DEACTIVATION TESTS
    // ==========================================

    @Test
    fun `deactivateAccount should successfully deactivate an active user`() {
        val user = spy(createDummyUser())
        `when`(user.isDeleted()).thenReturn(false)
        `when`(user.isInactive()).thenReturn(false)

        `when`(userRepository.findByEmail(defaultEmail)).thenReturn(Optional.of(user))

        userService.deactivateAccount(defaultEmail)

        verify(user).deactivateUser()
        verify(userRepository).save(user)
    }

    @Test
    fun `deactivateAccount should throw exception if already deleted or inactive`() {
        val user = spy(createDummyUser())
        `when`(user.isDeleted()).thenReturn(true)

        `when`(userRepository.findByEmail(defaultEmail)).thenReturn(Optional.of(user))

        assertThrows<UserAlreadyDeletedException> {
            userService.deactivateAccount(defaultEmail)
        }

        `when`(user.isDeleted()).thenReturn(false)
        `when`(user.isInactive()).thenReturn(true)

        assertThrows<UserAlreadyDeactivatedException> {
            userService.deactivateAccount(defaultEmail)
        }
    }

    @Test
    fun `deleteAccount should softly delete user`() {
        val user = spy(createDummyUser())
        `when`(user.isDeleted()).thenReturn(false)

        `when`(userRepository.findByEmail(defaultEmail)).thenReturn(Optional.of(user))

        userService.deleteAccount(defaultEmail)

        verify(user).softDeleteUser()
        verify(userRepository).save(user)
    }

    @Test
    fun `hardDeleteUser should verify existence, revoke tokens, and completely delete`() {
        `when`(userRepository.existsById(defaultUserId)).thenReturn(true)

        userService.hardDeleteUser(defaultUserId)

        verify(refreshTokenService).revokeTokensForUser(defaultUserId)
        verify(userRepository).deleteById(defaultUserId)
    }

    // ==========================================
    // 6. GHOST USER (SAD PATH) TESTS
    // ==========================================

    @Test
    fun `methods requiring email should throw UserNotFoundException if email does not exist`() {
        `when`(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty())

        assertThrows<UserNotFoundException> { userService.getUserProfile("ghost@test.com") }
        assertThrows<UserNotFoundException> { userService.updateProfile("ghost@test.com", UpdateProfileRequest()) }
        assertThrows<UserNotFoundException> { userService.changePassword("ghost@test.com", ChangePasswordRequest("old", "new")) }
        assertThrows<UserNotFoundException> { userService.deactivateAccount("ghost@test.com") }
        assertThrows<UserNotFoundException> { userService.deleteAccount("ghost@test.com") }
    }

    @Test
    fun `methods requiring ID should throw UserNotFoundException if ID does not exist`() {
        `when`(userRepository.findById(99L)).thenReturn(Optional.empty())
        `when`(userRepository.existsById(99L)).thenReturn(false)

        assertThrows<UserNotFoundException> { userService.getUserById(99L) }
        assertThrows<UserNotFoundException> { userService.changeUserStatus(99L, true) }
        assertThrows<UserNotFoundException> { userService.hardDeleteUser(99L) }
    }
}