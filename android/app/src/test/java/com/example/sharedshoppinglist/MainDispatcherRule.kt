package com.example.sharedshoppinglist

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Replaces `Dispatchers.Main` with a [StandardTestDispatcher] for the duration of
 * each test so `viewModelScope` coroutines run on the test scheduler.
 *
 * Pass [testDispatcher] to `runTest(...)` to share the same scheduler, which is
 * what makes [kotlinx.coroutines.test.advanceUntilIdle] drive the ViewModel work.
 * A [StandardTestDispatcher] (not unconfined) is used on purpose: it keeps
 * queued work pending, so the initial `Loading` state is observable before the
 * test advances the scheduler.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
