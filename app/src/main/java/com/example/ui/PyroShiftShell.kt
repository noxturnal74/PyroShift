package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.ui.ai.AiChatScreen
import com.example.ui.theme.PyroOrange

enum class PyroTab(
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
) {
  WEB(R.string.tab_web, Icons.Rounded.Language, Icons.Outlined.Language, "tab_web"),
  AI(R.string.tab_ai, Icons.Rounded.TravelExplore, Icons.Outlined.TravelExplore, "tab_ai"),
}

@Composable
fun PyroShiftShell() {
  var selectedTab by rememberSaveable { mutableStateOf(PyroTab.WEB) }
  var isWebFullscreen by rememberSaveable { mutableStateOf(false) }
  var pendingWebUrl by rememberSaveable { mutableStateOf<String?>(null) }

  val showChrome = !(selectedTab == PyroTab.WEB && isWebFullscreen)

  BoxWithConstraints(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
  ) {
    val useRail = maxWidth >= 600.dp

    if (useRail) {
      Row(modifier = Modifier.fillMaxSize()) {
        if (showChrome) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier =
              Modifier.fillMaxHeight()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
          ) {
            PyroTab.entries.forEach { tab ->
              val selected = selectedTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = { selectedTab = tab },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = stringResource(tab.labelRes),
                  )
                },
                label = { Text(stringResource(tab.labelRes)) },
                colors =
                  NavigationRailItemDefaults.colors(
                    selectedIconColor = PyroOrange,
                    selectedTextColor = PyroOrange,
                    indicatorColor = PyroOrange.copy(alpha = 0.16f),
                  ),
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }

        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .let { mod ->
                if (showChrome) {
                  mod.windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                } else {
                  mod
                }
              }
        ) {
          ShellContent(
            selectedTab = selectedTab,
            pendingWebUrl = pendingWebUrl,
            onPendingUrlConsumed = { pendingWebUrl = null },
            onFullscreenChanged = { isWebFullscreen = it },
            onOpenWebUrl = { url ->
              pendingWebUrl = url
              selectedTab = PyroTab.WEB
            },
          )
        }
      }
    } else {
      Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
          if (showChrome) {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              windowInsets = WindowInsets.navigationBars,
            ) {
              PyroTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                NavigationBarItem(
                  selected = selected,
                  onClick = { selectedTab = tab },
                  icon = {
                    Icon(
                      imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = stringResource(tab.labelRes),
                    )
                  },
                  label = { Text(stringResource(tab.labelRes), maxLines = 1) },
                  colors =
                    NavigationBarItemDefaults.colors(
                      selectedIconColor = PyroOrange,
                      selectedTextColor = PyroOrange,
                      indicatorColor = PyroOrange.copy(alpha = 0.16f),
                    ),
                  modifier = Modifier.testTag(tab.testTag),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Box(
          modifier =
            Modifier.fillMaxSize()
              .padding(innerPadding)
              .let { mod ->
                if (showChrome) mod.windowInsetsPadding(WindowInsets.statusBars).imePadding()
                else mod
              }
        ) {
          ShellContent(
            selectedTab = selectedTab,
            pendingWebUrl = pendingWebUrl,
            onPendingUrlConsumed = { pendingWebUrl = null },
            onFullscreenChanged = { isWebFullscreen = it },
            onOpenWebUrl = { url ->
              pendingWebUrl = url
              selectedTab = PyroTab.WEB
            },
          )
        }
      }
    }
  }
}

@Composable
private fun ShellContent(
  selectedTab: PyroTab,
  pendingWebUrl: String?,
  onPendingUrlConsumed: () -> Unit,
  onFullscreenChanged: (Boolean) -> Unit,
  onOpenWebUrl: (String) -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize()) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .zIndex(if (selectedTab == PyroTab.WEB) 1f else 0f)
          .alpha(if (selectedTab == PyroTab.WEB) 1f else 0f)
    ) {
      PyroShiftScreen(
        isTabActive = selectedTab == PyroTab.WEB,
        pendingUrlToLoad = pendingWebUrl,
        onPendingUrlConsumed = onPendingUrlConsumed,
        onFullscreenChanged = onFullscreenChanged,
      )
    }

    if (selectedTab == PyroTab.AI) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .zIndex(2f)
            .background(MaterialTheme.colorScheme.background)
      ) {
        AiChatScreen(onOpenWebUrl = onOpenWebUrl)
      }
    }
  }
}
