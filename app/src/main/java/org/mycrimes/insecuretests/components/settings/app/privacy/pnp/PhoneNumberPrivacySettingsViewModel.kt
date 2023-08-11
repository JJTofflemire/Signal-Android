package org.mycrimes.insecuretests.components.settings.app.privacy.pnp

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import org.mycrimes.insecuretests.database.SignalDatabase
import org.mycrimes.insecuretests.dependencies.ApplicationDependencies
import org.mycrimes.insecuretests.jobs.RefreshAttributesJob
import org.mycrimes.insecuretests.jobs.RefreshOwnProfileJob
import org.mycrimes.insecuretests.keyvalue.PhoneNumberPrivacyValues.PhoneNumberListingMode
import org.mycrimes.insecuretests.keyvalue.PhoneNumberPrivacyValues.PhoneNumberSharingMode
import org.mycrimes.insecuretests.keyvalue.SignalStore
import org.mycrimes.insecuretests.recipients.Recipient
import org.mycrimes.insecuretests.storage.StorageSyncHelper

class PhoneNumberPrivacySettingsViewModel : ViewModel() {

  private val _state = mutableStateOf(
    PhoneNumberPrivacySettingsState(
      seeMyPhoneNumber = SignalStore.phoneNumberPrivacy().phoneNumberSharingMode,
      findMeByPhoneNumber = SignalStore.phoneNumberPrivacy().phoneNumberListingMode
    )
  )

  val state: State<PhoneNumberPrivacySettingsState> = _state

  fun setNobodyCanSeeMyNumber() {
    setPhoneNumberSharingMode(PhoneNumberSharingMode.NOBODY)
  }

  fun setEveryoneCanSeeMyNumber() {
    setPhoneNumberSharingMode(PhoneNumberSharingMode.EVERYONE)
    setPhoneNumberListingMode(PhoneNumberListingMode.LISTED)
  }

  fun setNobodyCanFindMeByMyNumber() {
    setPhoneNumberListingMode(PhoneNumberListingMode.UNLISTED)
  }

  fun setEveryoneCanFindMeByMyNumber() {
    setPhoneNumberListingMode(PhoneNumberListingMode.LISTED)
  }

  private fun setPhoneNumberSharingMode(phoneNumberSharingMode: PhoneNumberSharingMode) {
    SignalStore.phoneNumberPrivacy().phoneNumberSharingMode = phoneNumberSharingMode
    SignalDatabase.recipients.markNeedsSync(Recipient.self().id)
    StorageSyncHelper.scheduleSyncForDataChange()
    refresh()
  }

  private fun setPhoneNumberListingMode(phoneNumberListingMode: PhoneNumberListingMode) {
    SignalStore.phoneNumberPrivacy().phoneNumberListingMode = phoneNumberListingMode
    StorageSyncHelper.scheduleSyncForDataChange()
    ApplicationDependencies.getJobManager().startChain(RefreshAttributesJob()).then(RefreshOwnProfileJob()).enqueue()
    refresh()
  }

  fun refresh() {
    _state.value = PhoneNumberPrivacySettingsState(
      seeMyPhoneNumber = SignalStore.phoneNumberPrivacy().phoneNumberSharingMode,
      findMeByPhoneNumber = SignalStore.phoneNumberPrivacy().phoneNumberListingMode
    )
  }
}
