package org.mycrimes.insecuretests.notifications.v2

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.mycrimes.insecuretests.database.model.MessageRecord
import org.mycrimes.insecuretests.database.model.MmsMessageRecord
import org.mycrimes.insecuretests.database.model.ParentStoryId

/**
 * Represents a "thread" that a notification can belong to.
 */
@Parcelize
data class ConversationId(
  val threadId: Long,
  val groupStoryId: Long?
) : Parcelable {
  companion object {
    @JvmStatic
    fun forConversation(threadId: Long): ConversationId {
      return ConversationId(
        threadId = threadId,
        groupStoryId = null
      )
    }

    @JvmStatic
    fun fromMessageRecord(record: MessageRecord): ConversationId {
      return ConversationId(record.threadId, ((record as? MmsMessageRecord)?.parentStoryId as? ParentStoryId.GroupReply)?.serialize())
    }

    @JvmStatic
    fun fromThreadAndReply(threadId: Long, groupReply: ParentStoryId.GroupReply?): ConversationId {
      return ConversationId(threadId, groupReply?.serialize())
    }
  }
}
