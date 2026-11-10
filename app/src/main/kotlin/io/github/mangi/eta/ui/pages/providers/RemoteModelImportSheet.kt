package io.github.mangi.eta.ui.pages.providers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.itos.coui.checkbox.Checkbox as CouiCheckbox
import com.itos.coui.checkbox.CouiCheckboxDefaults
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogPlacement
import com.itos.coui.dialog.Dialog
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.menu.rememberCouiWindowBackdrop
import com.itos.coui.theme.CouiTheme
import io.github.mangi.eta.R
import io.github.mangi.eta.data.model.AppearanceUiStyle
import io.github.mangi.eta.data.model.Model
import io.github.mangi.eta.data.repository.RemoteModelCatalog
import io.github.mangi.eta.ui.app.LocalUiStyle
import io.github.mangi.eta.ui.components.coui.CouiButton
import io.github.mangi.eta.ui.components.coui.CouiButtonRole
import io.github.mangi.eta.ui.components.coui.CouiSearchBarBox
import io.github.mangi.eta.ui.components.coui.couiPrimaryDialogAction
import io.github.mangi.eta.ui.components.coui.rememberCouiDialogMaterial
import io.github.mangi.eta.ui.model.ModelSearchQuery
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 一次拉取的待选结果；确认时保留连接和账号绑定，避免将过期目录写到另一连接。 */
internal data class RemoteModelImport(
    val catalog: RemoteModelCatalog,
    val candidates: List<Model>,
)

/** COUI 弹窗自己滚动内容并在固定操作栏放主按钮；MIUIX 底部面板仍需内容区自撑高度。 */
internal fun remoteModelImportUsesOverlaySheet(uiStyle: AppearanceUiStyle): Boolean =
    uiStyle == AppearanceUiStyle.MIUIX

internal fun toggleRemoteModelSelection(selected: Set<String>, modelId: String): Set<String> =
    if (modelId in selected) selected - modelId else selected + modelId

internal fun toggleAllVisibleRemoteModels(
    selected: Set<String>,
    visibleIds: Set<String>,
    allVisibleSelected: Boolean,
): Set<String> = if (allVisibleSelected) selected - visibleIds else selected + visibleIds

@Composable
internal fun RemoteModelImportSheet(
    pending: RemoteModelImport?,
    onDismissRequest: () -> Unit,
    onConfirm: (RemoteModelImport, Set<String>) -> Unit,
) {
    var shown by remember { mutableStateOf(pending) }
    if (pending != null) shown = pending
    var query by remember(shown) { mutableStateOf("") }
    var selected by remember(shown) { mutableStateOf(emptySet<String>()) }
    when (LocalUiStyle.current) {
        AppearanceUiStyle.COUI -> {
            val dark = CouiTheme.isDark
            val confirmText = if (selected.isNotEmpty()) {
                stringResource(R.string.remote_model_import_confirm, selected.size)
            } else {
                stringResource(R.string.remote_model_import_pick_hint)
            }
            Dialog(
                visible = pending != null,
                onDismissRequest = onDismissRequest,
                dark = dark,
                title = stringResource(R.string.remote_model_import_title),
                confirmAction = couiPrimaryDialogAction(
                    text = confirmText,
                    onClick = { shown?.let { onConfirm(it, selected) } },
                    enabled = selected.isNotEmpty(),
                    dark = dark,
                ),
                dismissAction = CouiDialogAction(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismissRequest,
                ),
                placement = CouiDialogPlacement.Bottom,
                material = rememberCouiDialogMaterial(dark),
                backdrop = rememberCouiWindowBackdrop(),
            ) {
                shown?.let { current ->
                    key(current) {
                        RemoteModelImportContent(
                            pending = current,
                            query = query,
                            onQueryChange = { query = it },
                            selected = selected,
                            onSelectedChange = { selected = it },
                        )
                    }
                }
            }
        }
        AppearanceUiStyle.MIUIX -> {
            OverlayBottomSheet(
                show = pending != null,
                title = stringResource(R.string.remote_model_import_title),
                onDismissRequest = onDismissRequest,
                insideMargin = DpSize(0.dp, 0.dp),
            ) {
                shown?.let { current ->
                    key(current) {
                        RemoteModelImportContent(
                            pending = current,
                            query = query,
                            onQueryChange = { query = it },
                            selected = selected,
                            onSelectedChange = { selected = it },
                            onConfirm = { onConfirm(current, selected) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteModelImportContent(
    pending: RemoteModelImport,
    query: String,
    onQueryChange: (String) -> Unit,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
    onConfirm: (() -> Unit)? = null,
) {
    val candidates = pending.candidates
    val visible = remember(candidates, query) {
        val search = ModelSearchQuery(query)
        candidates.filter { search.matches(it.displayName, it.modelId) }
    }
    val allVisibleSelected = visible.isNotEmpty() && visible.all { it.modelId in selected }
    val density = LocalDensity.current
    val sheetHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() } * 0.72f
    val uiStyle = LocalUiStyle.current
    val useOverlaySheet = remoteModelImportUsesOverlaySheet(uiStyle)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (useOverlaySheet) Modifier.height(sheetHeight) else Modifier)
            .then(if (uiStyle == AppearanceUiStyle.COUI) Modifier else Modifier.navigationBarsPadding()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.remote_model_import_summary, candidates.size),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.weight(1f),
            )
            val toggleAll: () -> Unit = {
                val ids = visible.mapTo(mutableSetOf()) { it.modelId }
                onSelectedChange(toggleAllVisibleRemoteModels(selected, ids, allVisibleSelected))
            }
            val toggleText = stringResource(
                if (allVisibleSelected) R.string.page_select_none_ba20eb else R.string.page_select_all_3e44b2,
            )
            if (uiStyle == AppearanceUiStyle.COUI) {
                CouiButton(
                    text = toggleText,
                    onClick = toggleAll,
                    enabled = visible.isNotEmpty(),
                    role = CouiButtonRole.Primary,
                    minHeight = 32.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                )
            } else {
                TextButton(
                    text = toggleText,
                    onClick = toggleAll,
                    enabled = visible.isNotEmpty(),
                    minWidth = 0.dp,
                    minHeight = 32.dp,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    insideMargin = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        if (uiStyle == AppearanceUiStyle.COUI) {
            CouiSearchBarBox(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = stringResource(R.string.ui_search_model_df5586),
                backgroundColor = Color.Transparent,
            )
        } else {
            InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                label = stringResource(R.string.ui_search_model_df5586),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 6.dp),
            )
        }
        if (useOverlaySheet) {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (visible.isEmpty()) {
                    item(key = "empty") {
                        RemoteModelImportEmpty()
                    }
                }
                items(visible, key = { it.modelId }) { model ->
                    val checked = model.modelId in selected
                    RemoteModelRow(
                        model = model,
                        checked = checked,
                        onToggle = {
                            onSelectedChange(toggleRemoteModelSelection(selected, model.modelId))
                        },
                    )
                }
            }
        } else {
            // COUI 弹窗自身负责滚动，普通 Column 可让内容按实际条目收缩，不再固定占满窗口高度。
            Column(modifier = Modifier.fillMaxWidth()) {
                if (visible.isEmpty()) {
                    RemoteModelImportEmpty()
                }
                visible.forEach { model ->
                    val checked = model.modelId in selected
                    RemoteModelRow(
                        model = model,
                        checked = checked,
                        onToggle = {
                            onSelectedChange(toggleRemoteModelSelection(selected, model.modelId))
                        },
                    )
                }
            }
        }
        if (onConfirm != null) {
            val confirmText = if (selected.isNotEmpty()) {
                stringResource(R.string.remote_model_import_confirm, selected.size)
            } else {
                stringResource(R.string.remote_model_import_pick_hint)
            }
            if (uiStyle == AppearanceUiStyle.COUI) {
                CouiButton(
                    text = confirmText,
                    onClick = onConfirm,
                    enabled = selected.isNotEmpty(),
                    role = CouiButtonRole.Primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 12.dp),
                )
            } else {
                TextButton(
                    text = confirmText,
                    onClick = onConfirm,
                    enabled = selected.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun RemoteModelImportEmpty() {
    Text(
        text = stringResource(R.string.page_no_matching_model_found_ae7e96),
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 48.dp),
    )
}

@Composable
private fun RemoteModelRow(
    model: Model,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val name = model.displayName.ifBlank { model.modelId }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .squircleSurface(color = Color.Transparent, cornerRadius = 12.dp)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!name.equals(model.modelId, ignoreCase = true)) {
                Text(
                    text = model.modelId,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        when (LocalUiStyle.current) {
            AppearanceUiStyle.COUI -> {
                val dark = CouiTheme.isDark
                val colors = CouiPageDefaults.themeColors(dark)
                CouiCheckbox(
                    modifier = Modifier,
                    checked = checked,
                    colors = CouiCheckboxDefaults.colors(colors.accent, dark),
                    shape = CouiCheckboxDefaults.shape,
                    borderShape = CouiCheckboxDefaults.borderShape,
                )
            }
            AppearanceUiStyle.MIUIX -> {
                Checkbox(
                    state = if (checked) ToggleableState.On else ToggleableState.Off,
                    onClick = onToggle,
                )
            }
        }
    }
}
