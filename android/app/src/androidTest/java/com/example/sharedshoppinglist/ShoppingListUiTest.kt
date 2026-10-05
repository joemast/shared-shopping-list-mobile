package com.example.sharedshoppinglist

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sharedshoppinglist.data.ApiClient
import com.example.sharedshoppinglist.data.dto.ShoppingItemDto
import com.example.sharedshoppinglist.data.dto.ShoppingListDto
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI smoke suite — **UI-01..UI-05** (AC20/AC38).
 *
 * It drives the *real* Compose UI through the *real* Retrofit + kotlinx.serialization
 * path, but with no live backend: at suite startup the [AppContainer] seam is
 * overridden with a [MockWebServerRepository] that replays canned list/item
 * responses (OQ-15). The production `10.0.2.2:<port>` wiring is never exercised here.
 *
 * Execution boundary (AC42): this suite is **compiled locally**
 * (`./gradlew assembleDebugAndroidTest` in the `android-build` container) and
 * **executed only in CI** on the Ubuntu + KVM emulator job
 * (`./gradlew connectedDebugAndroidTest`). Docker Desktop on macOS has no KVM, so
 * a local execution attempt is unsupported — not a skipped pass.
 *
 * Determinism: responses are scripted per test with [MockWebServerRepository.enqueueJson];
 * the UI is driven by semantics/test tags and observed via Compose idling
 * (`waitUntil`) — there is no `Thread.sleep()`.
 */
@RunWith(AndroidJUnit4::class)
class ShoppingListUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /**
     * Each test scripts its own FIFO responses before the Activity's launch-time
     * `GET /lists` is served. Dropping the script afterwards keeps tests isolated.
     */
    @After
    fun resetResponseScript() {
        repository.resetQueue()
    }

    // ---- UI-01 -----------------------------------------------------------------

    @Test
    fun ui01_createList_showsNewList() {
        enqueueLists() // GET /lists (launch) -> []
        repository.enqueueJson(code = 201, body = listJson(listDto())) // POST /lists
        enqueueLists(listDto()) // GET /lists (reload) -> [list]

        waitForText("No lists yet")
        composeRule.onNodeWithContentDescription("Add list").performClick()
        composeRule.onNodeWithTag("nameField").performTextInput(LIST_NAME)
        composeRule.onNodeWithText("Save").performClick()

        waitForText(LIST_NAME)
        composeRule.onNodeWithText(LIST_NAME).assertIsDisplayed()
    }

    // ---- UI-02 -----------------------------------------------------------------

    @Test
    fun ui02_addItemWithQuantity_rendersItemAndQuantity() {
        enqueueLists(listDto())
        enqueueList(listDto(items = emptyList()))
        repository.enqueueJson(code = 201, body = itemJson(itemDto("milk", "2 bottles")))
        enqueueList(listDto(items = listOf(itemDto("milk", "2 bottles"))))

        openSeededList(awaitContent = "No items yet")
        composeRule.onNodeWithContentDescription("Add item").performClick()
        composeRule.onNodeWithTag("itemNameField").performTextInput("milk")
        composeRule.onNodeWithTag("itemQuantityField").performTextInput("2 bottles")
        composeRule.onNodeWithText("Save").performClick()

        waitForText("milk")
        composeRule.onNodeWithText("milk").assertIsDisplayed()
        composeRule.onNodeWithText("2 bottles").assertIsDisplayed()
    }

    @Test
    fun ui02b_addItemWithoutQuantity_omitsQuantity() {
        enqueueLists(listDto())
        enqueueList(listDto(items = emptyList()))
        repository.enqueueJson(code = 201, body = itemJson(itemDto("bread"))) // quantity omitted
        enqueueList(listDto(items = listOf(itemDto("bread"))))

        openSeededList(awaitContent = "No items yet")
        composeRule.onNodeWithContentDescription("Add item").performClick()
        composeRule.onNodeWithTag("itemNameField").performTextInput("bread")
        composeRule.onNodeWithText("Save").performClick()

        waitForText("bread")
        composeRule.onNodeWithText("bread").assertIsDisplayed()
        // The optional-quantity boundary: no quantity text is rendered.
        composeRule.onAllNodesWithTag("itemQuantityText", useUnmergedTree = true)
            .assertCountEquals(0)
    }

    // ---- UI-03 -----------------------------------------------------------------

    @Test
    fun ui03_toggleBought_reflectsBoughtState() {
        enqueueLists(listDto(items = listOf(itemDto("milk", "2 bottles", bought = false))))
        enqueueList(listDto(items = listOf(itemDto("milk", "2 bottles", bought = false))))
        repository.enqueueJson(body = itemJson(itemDto("milk", "2 bottles", bought = true)))
        enqueueList(listDto(items = listOf(itemDto("milk", "2 bottles", bought = true))))

        openSeededList(awaitContent = "milk")
        composeRule.onNodeWithTag("itemBoughtToggle").performClick()
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodes(isOn()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("itemBoughtToggle").assertIsOn()
    }

    // ---- UI-04 -----------------------------------------------------------------

    @Test
    fun ui04_renameAndUpdateQuantity_rendersUpdatedValues() {
        enqueueLists(listDto(items = listOf(itemDto("milk", "2 bottles"))))
        enqueueList(listDto(items = listOf(itemDto("milk", "2 bottles"))))
        repository.enqueueJson(body = itemJson(itemDto("whole milk", "1 bottle")))
        enqueueList(listDto(items = listOf(itemDto("whole milk", "1 bottle"))))

        openSeededList(awaitContent = "milk")
        composeRule.onNodeWithContentDescription("Edit milk").performClick()
        composeRule.onNodeWithTag("itemNameField").performTextReplacement("whole milk")
        composeRule.onNodeWithTag("itemQuantityField").performTextReplacement("1 bottle")
        composeRule.onNodeWithText("Save").performClick()

        waitForText("whole milk")
        composeRule.onNodeWithText("whole milk").assertIsDisplayed()
        composeRule.onNodeWithText("1 bottle").assertIsDisplayed()
    }

    // ---- UI-05 -----------------------------------------------------------------

    @Test
    fun ui05_deleteItem_removesItem() {
        enqueueLists(listDto(items = listOf(itemDto("milk", "2 bottles"))))
        enqueueList(listDto(items = listOf(itemDto("milk", "2 bottles"))))
        repository.enqueueEmpty(code = 204) // DELETE /items/{id}
        enqueueList(listDto(items = emptyList())) // GET /lists/{id} (reload) -> empty

        openSeededList(awaitContent = "milk")
        composeRule.onNodeWithContentDescription("Delete milk").performClick()

        waitForTextToDisappear("milk")
        waitForText("No items yet")
    }

    // ---- Helpers ---------------------------------------------------------------

    /** Waits on Compose idling (no sleeps) until [text] is present. */
    private fun waitForText(text: String) {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits on Compose idling (no sleeps) until [text] is gone. */
    private fun waitForTextToDisappear(text: String) {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
    }

    /** Opens the single seeded list and waits for [awaitContent] on the details screen. */
    private fun openSeededList(awaitContent: String) {
        waitForText(LIST_NAME)
        composeRule.onNodeWithTag("listRow").performClick()
        waitForText(awaitContent)
    }

    private fun enqueueLists(vararg lists: ShoppingListDto) {
        repository.enqueueJson(body = ApiClient.json.encodeToString(lists.toList()))
    }

    private fun enqueueList(list: ShoppingListDto) {
        repository.enqueueJson(body = listJson(list))
    }

    private fun listJson(list: ShoppingListDto): String = ApiClient.json.encodeToString(list)

    private fun itemJson(item: ShoppingItemDto): String = ApiClient.json.encodeToString(item)

    private fun listDto(
        items: List<ShoppingItemDto> = emptyList(),
        name: String = LIST_NAME,
    ): ShoppingListDto = ShoppingListDto(
        id = LIST_ID,
        name = name,
        createdAt = TIMESTAMP,
        updatedAt = TIMESTAMP,
        items = items,
    )

    private fun itemDto(
        name: String,
        quantity: String? = null,
        bought: Boolean = false,
    ): ShoppingItemDto = ShoppingItemDto(
        id = ITEM_ID,
        listId = LIST_ID,
        name = name,
        quantity = quantity,
        bought = bought,
        createdAt = TIMESTAMP,
        updatedAt = TIMESTAMP,
    )

    companion object {
        private const val TIMEOUT_MILLIS = 15_000L
        private const val LIST_ID = "list-1"
        private const val ITEM_ID = "item-1"
        private const val LIST_NAME = "Weekend groceries"
        private const val TIMESTAMP = "2026-10-05T12:00:00Z"

        /**
         * Suite-level stub server, installed into the `AppContainer` seam before any
         * Activity is created. One server for the class; each test scripts its own
         * FIFO responses.
         */
        private lateinit var repository: MockWebServerRepository

        @JvmStatic
        @BeforeClass
        fun installTestSeam() {
            repository = MockWebServerRepository()
            AppContainer.installTestOverride(
                AppContainer(baseUrl = repository.baseUrl, repository = repository),
            )
        }

        @JvmStatic
        @AfterClass
        fun removeTestSeam() {
            AppContainer.clearTestOverride()
            repository.shutdown()
        }
    }
}
