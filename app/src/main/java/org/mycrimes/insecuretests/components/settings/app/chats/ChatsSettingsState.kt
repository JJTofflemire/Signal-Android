package org.mycrimes.insecuretests.components.settings.app.chats

import org.mycrimes.insecuretests.components.settings.app.chats.sms.SmsExportState

data class ChatsSettingsState(
  val generateLinkPreviews: Boolean,
  val useAddressBook: Boolean,
  val keepMutedChatsArchived: Boolean,
  val useSystemEmoji: Boolean,
  val enterKeySends: Boolean,
  val chatBackupsEnabled: Boolean,
  val useAsDefaultSmsApp: Boolean,
  val smsExportState: SmsExportState = SmsExportState.FETCHING
)
