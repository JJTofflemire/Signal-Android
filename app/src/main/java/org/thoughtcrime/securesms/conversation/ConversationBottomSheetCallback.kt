package org.mycrimes.insecuretests.conversation

import org.mycrimes.insecuretests.database.model.MessageRecord

/**
 * Callback interface for bottom sheets that show conversation data in a conversation and
 * want to manipulate the conversation view.
 */
interface ConversationBottomSheetCallback {
  fun getConversationAdapterListener(): ConversationAdapter.ItemClickListener
  fun jumpToMessage(messageRecord: MessageRecord)
}
