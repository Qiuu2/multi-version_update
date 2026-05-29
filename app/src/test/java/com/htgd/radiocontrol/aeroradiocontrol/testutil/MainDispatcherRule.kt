package com.htgd.radiocontrol.aeroradiocontrol.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit rule that swaps `Dispatchers.Main` for a [TestDispatcher] around each
 * test, so code that launches into `viewModelScope` (which uses Main) runs
 * deterministically under `runTest`.
 *
 * Defaults to [UnconfinedTestDispatcher] so coroutines launched in a ViewModel's
 * constructor / on a call execute eagerly up to their first suspension — handy
 * for asserting state right after an action without manually advancing time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
