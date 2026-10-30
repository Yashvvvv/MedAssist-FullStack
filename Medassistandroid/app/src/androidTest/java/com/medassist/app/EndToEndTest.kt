package com.medassist.app

import android.Manifest
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import java.io.File
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * Drives the real app against a real backend seeded with demo data (CI starts both): the owner
 * bills a sale end to end, then a patient finds the demo pharmacy and places an order.
 * Saves a screenshot of each screen for the README and for review.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class EndToEndTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(GrantPermissionRule.grant(Manifest.permission.ACCESS_COARSE_LOCATION))
        .around(compose)

    private val timeout = 60_000L

    @Test
    fun a_pharmacyOwnerBillsASale() {
        logIn("Pharmacy demo")
        waitFor("Today's sales")
        shot("01-dashboard")

        tab("Sell")
        compose.onNode(hasSetTextAction()).performTextInput("dolo")
        waitFor("Dolo 650")
        compose.onAllNodesWithText("Dolo 650").onFirst().performClick()
        waitFor("Checkout")
        shot("02-pos-cart")
        compose.onNode(hasText("Checkout") and hasClickAction()).performClick()
        waitFor("Payment")
        shotAll("03-checkout")
        compose.onNode(hasText("Bill", substring = true) and hasClickAction()).performClick()
        waitFor("Taxable value")
        shot("04-invoice")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        tab("Orders")
        click("All")
        waitForAny("Online orders")
        Thread.sleep(1_500)
        shot("05-orders")

        tab("Stock")
        waitFor("Receive stock")
        Thread.sleep(1_500)
        shot("06-stock")
        click("Expiring")
        Thread.sleep(1_500)
        shot("07-expiring")

        tab("More")
        click("Reports")
        waitFor("Gross profit")
        shot("08-reports")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        logOut()
    }

    @Test
    fun b_patientFindsAPharmacyAndOrders() {
        logIn("Customer demo")
        waitFor("Pharmacies near you")
        // The emulator's GPS says Mountain View, where there are no pharmacies: use the demo area, as a real user would.
        compose.waitUntil(timeout) {
            compose.onAllNodesWithText("Sharma Medicos (Demo)").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("Use demo area").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithText("Sharma Medicos (Demo)").fetchSemanticsNodes().isEmpty()) {
            shot("09a-no-pharmacies-nearby")
            click("Use demo area")
        }
        waitFor("Sharma Medicos (Demo)")
        shot("09-explore")

        compose.onAllNodesWithText("Sharma Medicos (Demo)").onFirst().performClick()
        waitFor("Search this pharmacy")
        Thread.sleep(1_500)
        shot("10-pharmacy")
        compose.onNode(hasSetTextAction()).performTextInput("dolo")
        waitFor("Dolo 650")
        Thread.sleep(800)
        compose.onAllNodes(hasText("Add") and hasClickAction()).onFirst().performClick()
        waitFor("Review order")
        compose.onNode(hasText("Review order") and hasClickAction()).performClick()
        waitFor("Place order")
        compose.onNode(hasSetTextAction() and hasText("Mobile number")).performTextInput("9888800000")
        shotAll("11-order-checkout")
        compose.onNode(hasText("Place order") and hasClickAction()).performClick()
        waitFor("Waiting for pharmacy")
        shot("12-order-tracking")

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        tab("Scan")
        waitFor("What's this medicine?")
        shot("13-ai-scan")
        tab("Account")
        confirmLogOut()
    }

    // ------------------------------------------------------------------ helpers

    private fun logIn(demoButton: String) {
        ensureSignedOut()
        waitFor("Get started")
        shot("00-welcome")
        compose.onNode(hasText("Log in") and hasClickAction()).performClick()
        waitFor("Welcome back")
        compose.onNode(hasText(demoButton) and hasClickAction()).performScrollTo().performClick()
        compose.onNode(hasText("Log in") and hasClickAction()).performScrollTo().performClick()
    }

    private fun logOut() {
        tab("More")
        click("Account & settings")
        confirmLogOut()
    }

    private fun confirmLogOut() {
        waitFor("Log out")
        compose.onAllNodes(hasText("Log out") and hasClickAction()).onFirst().performScrollTo().performClick()
        compose.waitUntil(timeout) { compose.onAllNodesWithText("Log out?").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasText("Log out") and hasClickAction()).onLast().performClick()
        waitFor("Get started")
    }

    private fun tab(label: String) = click(label)

    private fun click(text: String) {
        waitFor(text)
        compose.onAllNodes(hasText(text) and hasClickAction()).onFirst().performClick()
        compose.waitForIdle()
    }

    private fun waitFor(text: String) {
        try {
            compose.waitUntil(timeout) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Throwable) {
            runCatching { shot("FAIL-waiting-for-${text.replace(Regex("[^A-Za-z0-9]"), "_")}") }
            throw AssertionError("Timed out waiting for \"$text\"", e)
        }
    }

    /** Tests share the app's stored session; start each one signed out, whatever the last one left behind. */
    private fun ensureSignedOut() {
        compose.waitUntil(timeout) {
            listOf("Get started", "Today's sales", "Pharmacies near you", "More", "Account").any {
                compose.onAllNodesWithText(it).fetchSemanticsNodes().isNotEmpty()
            }
        }
        if (compose.onAllNodesWithText("Get started").fetchSemanticsNodes().isNotEmpty()) return
        if (compose.onAllNodes(hasText("More") and hasClickAction()).fetchSemanticsNodes().isNotEmpty()) {
            tab("More")
            click("Account & settings")
        } else {
            tab("Account")
        }
        confirmLogOut()
    }

    private fun waitForAny(text: String) = waitFor(text)

    private fun dir(): File =
        File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "screens").apply { mkdirs() }

    private fun save(node: SemanticsNodeInteraction, name: String) {
        val bmp = node.captureToImage().asAndroidBitmap()
        File(dir(), "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** The main window. */
    private fun shot(name: String) {
        compose.waitForIdle()
        save(compose.onAllNodes(isRoot()).onFirst(), name)
    }

    /** The top-most window (bottom sheets and dialogs are windows of their own). */
    private fun shotAll(name: String) {
        compose.waitForIdle()
        Thread.sleep(600) // let the sheet finish sliding in
        save(compose.onAllNodes(isRoot()).onLast(), name)
    }
}
