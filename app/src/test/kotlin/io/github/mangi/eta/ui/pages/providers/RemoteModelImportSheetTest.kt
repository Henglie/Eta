package io.github.mangi.eta.ui.pages.providers

import io.github.mangi.eta.data.model.AppearanceUiStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteModelImportSheetTest {
    @Test
    fun couiUsesDialogActionBarInsteadOfOverlaySheet() {
        assertFalse(remoteModelImportUsesOverlaySheet(AppearanceUiStyle.COUI))
        assertTrue(remoteModelImportUsesOverlaySheet(AppearanceUiStyle.MIUIX))
    }

    @Test
    fun toggleSelectionAddsAndRemovesOnlyRequestedModel() {
        val selected = toggleRemoteModelSelection(emptySet(), "alpha")
        assertEquals(setOf("alpha"), selected)

        val cleared = toggleRemoteModelSelection(selected, "alpha")
        assertTrue(cleared.isEmpty())
    }

    @Test
    fun toggleAllVisibleKeepsSelectionsOutsideSearchResults() {
        val selected = setOf("alpha", "hidden")
        val visibleIds = setOf("alpha", "beta")

        val allSelected = toggleAllVisibleRemoteModels(selected, visibleIds, allVisibleSelected = false)
        assertEquals(setOf("alpha", "hidden", "beta"), allSelected)

        val visibleCleared = toggleAllVisibleRemoteModels(allSelected, visibleIds, allVisibleSelected = true)
        assertEquals(setOf("hidden"), visibleCleared)
    }
}
