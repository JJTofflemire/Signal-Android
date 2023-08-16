/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.mycrimes.insecuretests.components.settings.app.help

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.findNavController
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.signal.core.ui.Scaffolds
import org.signal.core.util.logging.Log
import org.mycrimes.insecuretests.R
import org.mycrimes.insecuretests.compose.ComposeFragment

class LicenseFragment : ComposeFragment() {
  private val TAG = Log.tag(LicenseFragment::class.java)

  @Composable
  override fun FragmentContent() {
    val textState: State<String> = Single.fromCallable {
      requireContext().resources.openRawResource(R.raw.third_party_licenses).bufferedReader().use { it.readText() }
    }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeAsState(initial = "")
    Scaffolds.Settings(
      title = stringResource(id = R.string.HelpSettingsFragment__licenses),
      onNavigationClick = findNavController()::popBackStack,
      navigationIconPainter = painterResource(id = R.drawable.ic_arrow_left_24),
      navigationContentDescription = stringResource(id = R.string.Material3SearchToolbar__close)
    ) {
      LicenseScreen(licenseText = textState.value, modifier = Modifier.padding(it))
    }
  }
}

@Composable
fun LicenseScreen(licenseText: String, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .padding(horizontal = 24.dp)
      .verticalScroll(rememberScrollState())
  ) {
    Text(
      text = licenseText,
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.padding(vertical = 16.dp)
    )
  }
}

@Preview
@Composable
fun LicenseFragmentPreview() {
  LicenseScreen("Lorem ipsum")
}