package com.nis.nistv

import com.nis.nistv.core.database.dao.ProfileDao
import com.nis.nistv.core.database.entity.ProfileEntity
import com.nis.nistv.core.security.ParentalPinHelper
import com.nis.nistv.data.repository.DefaultProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileRepositoryTest {

    private fun createFakeProfileDao(initialProfiles: List<ProfileEntity>): Pair<ProfileDao, MutableMap<String, ProfileEntity>> {
        val store = initialProfiles.associateBy { it.id }.toMutableMap()
        val flowState = MutableStateFlow(store.values.toList())

        val dao = object : ProfileDao {
            override fun getAllProfiles(): Flow<List<ProfileEntity>> = flowState
            override suspend fun getAllProfilesSync(): List<ProfileEntity> = store.values.toList()
            override suspend fun getProfileById(id: String): ProfileEntity? = store[id]
            override suspend fun getProfileCount(): Int = store.size
            override suspend fun insertProfile(profile: ProfileEntity) {
                store[profile.id] = profile
                flowState.value = store.values.toList()
            }
            override suspend fun updateProfile(profile: ProfileEntity) {
                store[profile.id] = profile
                flowState.value = store.values.toList()
            }
            override suspend fun deleteProfile(id: String) {
                store.remove(id)
                flowState.value = store.values.toList()
            }
            override suspend fun setHistoryPaused(id: String, paused: Boolean, updatedAt: Long) {
                store[id]?.let {
                    val updated = it.copy(historyPaused = paused, updatedAt = updatedAt)
                    store[id] = updated
                    flowState.value = store.values.toList()
                }
            }
        }
        return Pair(dao, store)
    }

    private fun createRepository(
        profileDao: ProfileDao,
        initialActiveProfileId: String = "default",
        pinHash: String = "",
        pinSalt: String = ""
    ): Pair<DefaultProfileRepository, Job> {
        val job = Job()
        val repoScope = CoroutineScope(job + Dispatchers.Default)
        val activeProfileIdFlow = MutableStateFlow(initialActiveProfileId)
        val repo = DefaultProfileRepository(
            profileDao = profileDao,
            mediaInteractionDao = null,
            programReminderDao = null,
            watchLaterDao = null,
            activeProfileIdFlow = activeProfileIdFlow,
            onSetActiveProfileId = { activeProfileIdFlow.value = it },
            parentalPinHashFlow = flowOf(pinHash),
            parentalPinSaltFlow = flowOf(pinSalt),
            preferredLanguageFlow = flowOf("Hindi"),
            preferredCountryFlow = flowOf("IN"),
            watchHistoryPausedFlow = flowOf(false),
            isBootstrapDoneFlow = flowOf(true),
            onSetBootstrapDone = {},
            playerControllerProvider = { null },
            sleepTimerManagerProvider = { null },
            parentalControlManagerProvider = { null },
            scope = repoScope
        )
        return Pair(repo, job)
    }

    @Test
    fun maxFiveProfilesRule_enforced() = runTest {
        val initialList = (1..5).map {
            ProfileEntity(id = "p_$it", name = "User $it", avatarId = "avatar_01")
        }
        val (profileDao, _) = createFakeProfileDao(initialList)
        val (repo, job) = createRepository(profileDao, "p_1")
        try {
            val result = repo.createProfile("User 6")
            assertTrue("Creating 6th profile must fail", result.isFailure)
            assertEquals("Maximum of 5 profiles reached", result.exceptionOrNull()?.message)
        } finally {
            job.cancel()
        }
    }

    @Test
    fun blankProfileName_rejected() = runTest {
        val (profileDao, _) = createFakeProfileDao(listOf(ProfileEntity(id = "p_1", name = "Me")))
        val (repo, job) = createRepository(profileDao, "p_1")
        try {
            val result = repo.createProfile("   ")
            assertTrue("Blank profile name must fail", result.isFailure)
            assertEquals("Profile name cannot be blank", result.exceptionOrNull()?.message)
        } finally {
            job.cancel()
        }
    }

    @Test
    fun duplicateProfileName_rejected() = runTest {
        val (profileDao, _) = createFakeProfileDao(listOf(ProfileEntity(id = "p_1", name = "Alex")))
        val (repo, job) = createRepository(profileDao, "p_1")
        try {
            val result = repo.createProfile("alex")
            assertTrue("Duplicate name (case-insensitive) must fail", result.isFailure)
            assertEquals("A profile with this name already exists", result.exceptionOrNull()?.message)
        } finally {
            job.cancel()
        }
    }

    @Test
    fun cannotDeleteLastProfile_enforced() = runTest {
        val (profileDao, _) = createFakeProfileDao(listOf(ProfileEntity(id = "p_1", name = "Solo")))
        val (repo, job) = createRepository(profileDao, "p_1")
        try {
            val result = repo.deleteProfile("p_1")
            assertTrue("Deleting only profile must fail", result.isFailure)
            assertEquals("Cannot delete the last remaining profile", result.exceptionOrNull()?.message)
        } finally {
            job.cancel()
        }
    }

    @Test
    fun deletingActiveProfile_switchesToAnotherFirst() = runTest {
        val p1 = ProfileEntity(id = "p_1", name = "First")
        val p2 = ProfileEntity(id = "p_2", name = "Second")
        val (profileDao, store) = createFakeProfileDao(listOf(p1, p2))
        val (repo, job) = createRepository(profileDao, "p_1")
        try {
            val result = repo.deleteProfile("p_1")
            assertTrue("Deletion must succeed", result.isSuccess)
            assertFalse("p_1 must be removed", store.containsKey("p_1"))
            assertEquals("Active profile must switch to p_2", "p_2", repo.activeProfileId.value)
        } finally {
            job.cancel()
        }
    }

    @Test
    fun canLeaveProfile_withKidsProfile_requiresPin() = runTest {
        val salt = ParentalPinHelper.generateSalt()
        val hash = ParentalPinHelper.hashPin("1234", salt)

        val kidsProfile = ProfileEntity(id = "kids_1", name = "Junior", isKids = true)
        val (profileDao, _) = createFakeProfileDao(listOf(kidsProfile))
        val (repo, job) = createRepository(profileDao, "kids_1", pinHash = hash, pinSalt = salt)
        try {
            // Switch to kids profile so active profile is kids
            repo.switchProfile("kids_1")

            assertFalse("Leaving kids profile without PIN must fail", repo.canLeaveProfile(null))
            assertFalse("Leaving kids profile with incorrect PIN must fail", repo.canLeaveProfile("9999"))
            assertTrue("Leaving kids profile with correct PIN must succeed", repo.canLeaveProfile("1234"))
        } finally {
            job.cancel()
        }
    }
}
