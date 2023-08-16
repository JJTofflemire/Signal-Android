package org.mycrimes.insecuretests.components.settings.conversation

import android.content.Context
import android.database.Cursor
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.signal.core.util.concurrent.SignalExecutors
import org.signal.core.util.logging.Log
import org.signal.storageservice.protos.groups.local.DecryptedGroup
import org.signal.storageservice.protos.groups.local.DecryptedPendingMember
import org.mycrimes.insecuretests.contacts.sync.ContactDiscovery
import org.mycrimes.insecuretests.database.CallTable
import org.mycrimes.insecuretests.database.MediaTable
import org.mycrimes.insecuretests.database.SignalDatabase
import org.mycrimes.insecuretests.database.model.GroupRecord
import org.mycrimes.insecuretests.database.model.IdentityRecord
import org.mycrimes.insecuretests.database.model.MessageRecord
import org.mycrimes.insecuretests.database.model.StoryViewState
import org.mycrimes.insecuretests.dependencies.ApplicationDependencies
import org.mycrimes.insecuretests.groups.GroupId
import org.mycrimes.insecuretests.groups.GroupProtoUtil
import org.mycrimes.insecuretests.groups.LiveGroup
import org.mycrimes.insecuretests.groups.v2.GroupAddMembersResult
import org.mycrimes.insecuretests.groups.v2.GroupManagementRepository
import org.mycrimes.insecuretests.keyvalue.SignalStore
import org.mycrimes.insecuretests.recipients.Recipient
import org.mycrimes.insecuretests.recipients.RecipientId
import org.mycrimes.insecuretests.recipients.RecipientUtil
import org.mycrimes.insecuretests.util.FeatureFlags
import java.io.IOException
import java.util.Optional

private val TAG = Log.tag(ConversationSettingsRepository::class.java)

class ConversationSettingsRepository(
  private val context: Context,
  private val groupManagementRepository: GroupManagementRepository = GroupManagementRepository(context)
) {

  fun getCallEvents(callRowIds: LongArray): Single<List<Pair<CallTable.Call, MessageRecord>>> {
    return if (callRowIds.isEmpty()) {
      Single.just(emptyList())
    } else {
      Single.fromCallable {
        val callMap = SignalDatabase.calls.getCallsByRowIds(callRowIds.toList())
        val messageIds = callMap.values.mapNotNull { it.messageId }
        SignalDatabase.messages.getMessages(messageIds).iterator().asSequence()
          .filter { callMap.containsKey(it.id) }
          .map { callMap[it.id]!! to it }
          .sortedByDescending { it.first.timestamp }
          .toList()
      }
    }
  }

  @WorkerThread
  fun getThreadMedia(threadId: Long): Optional<Cursor> {
    return if (threadId <= 0) {
      Optional.empty()
    } else {
      Optional.of(SignalDatabase.media.getGalleryMediaForThread(threadId, MediaTable.Sorting.Newest))
    }
  }

  fun getStoryViewState(groupId: GroupId): Observable<StoryViewState> {
    return Observable.fromCallable {
      SignalDatabase.recipients.getByGroupId(groupId)
    }.flatMap {
      StoryViewState.getForRecipientId(it.get())
    }.observeOn(Schedulers.io())
  }

  fun getThreadId(recipientId: RecipientId, consumer: (Long) -> Unit) {
    SignalExecutors.BOUNDED.execute {
      consumer(SignalDatabase.threads.getThreadIdIfExistsFor(recipientId))
    }
  }

  fun getThreadId(groupId: GroupId, consumer: (Long) -> Unit) {
    SignalExecutors.BOUNDED.execute {
      val recipientId = Recipient.externalGroupExact(groupId).id
      consumer(SignalDatabase.threads.getThreadIdIfExistsFor(recipientId))
    }
  }

  fun isInternalRecipientDetailsEnabled(): Boolean = SignalStore.internalValues().recipientDetails()

  fun hasGroups(consumer: (Boolean) -> Unit) {
    SignalExecutors.BOUNDED.execute { consumer(SignalDatabase.groups.getActiveGroupCount() > 0) }
  }

  fun getIdentity(recipientId: RecipientId, consumer: (IdentityRecord?) -> Unit) {
    SignalExecutors.BOUNDED.execute {
      if (SignalStore.account().aci != null && SignalStore.account().pni != null) {
        consumer(ApplicationDependencies.getProtocolStore().aci().identities().getIdentityRecord(recipientId).orElse(null))
      } else {
        consumer(null)
      }
    }
  }

  fun getGroupsInCommon(recipientId: RecipientId, consumer: (List<Recipient>) -> Unit) {
    SignalExecutors.BOUNDED.execute {
      consumer(
        SignalDatabase
          .groups
          .getPushGroupsContainingMember(recipientId)
          .asSequence()
          .filter { it.members.contains(Recipient.self().id) }
          .map(GroupRecord::recipientId)
          .map(Recipient::resolved)
          .sortedBy { gr -> gr.getDisplayName(context) }
          .toList()
      )
    }
  }

  fun getGroupMembership(recipientId: RecipientId, consumer: (List<RecipientId>) -> Unit) {
    SignalExecutors.BOUNDED.execute {
      val groupDatabase = SignalDatabase.groups
      val groupRecords = groupDatabase.getPushGroupsContainingMember(recipientId)
      val groupRecipients = ArrayList<RecipientId>(groupRecords.size)
      for (groupRecord in groupRecords) {
        groupRecipients.add(groupRecord.recipientId)
      }
      consumer(groupRecipients)
    }
  }

  fun refreshRecipient(recipientId: RecipientId) {
    SignalExecutors.UNBOUNDED.execute {
      try {
        ContactDiscovery.refresh(context, Recipient.resolved(recipientId), false)
      } catch (e: IOException) {
        Log.w(TAG, "Failed to refresh user after adding to contacts.")
      }
    }
  }

  fun setMuteUntil(recipientId: RecipientId, until: Long) {
    SignalExecutors.BOUNDED.execute {
      SignalDatabase.recipients.setMuted(recipientId, until)
    }
  }

  fun getGroupCapacity(groupId: GroupId, consumer: (GroupCapacityResult) -> Unit) {
    SignalExecutors.BOUNDED.execute {
      val groupRecord: GroupRecord = SignalDatabase.groups.getGroup(groupId).get()
      consumer(
        if (groupRecord.isV2Group) {
          val decryptedGroup: DecryptedGroup = groupRecord.requireV2GroupProperties().decryptedGroup
          val pendingMembers: List<RecipientId> = decryptedGroup.pendingMembersList
            .map(DecryptedPendingMember::getUuid)
            .map(GroupProtoUtil::uuidByteStringToRecipientId)

          val members = mutableListOf<RecipientId>()

          members.addAll(groupRecord.members)
          members.addAll(pendingMembers)

          GroupCapacityResult(Recipient.self().id, members, FeatureFlags.groupLimits(), groupRecord.isAnnouncementGroup)
        } else {
          GroupCapacityResult(Recipient.self().id, groupRecord.members, FeatureFlags.groupLimits(), false)
        }
      )
    }
  }

  fun addMembers(groupId: GroupId, selected: List<RecipientId>, consumer: (GroupAddMembersResult) -> Unit) {
    groupManagementRepository.addMembers(groupId, selected, consumer)
  }

  fun setMuteUntil(groupId: GroupId, until: Long) {
    SignalExecutors.BOUNDED.execute {
      val recipientId = Recipient.externalGroupExact(groupId).id
      SignalDatabase.recipients.setMuted(recipientId, until)
    }
  }

  fun block(recipientId: RecipientId) {
    SignalExecutors.BOUNDED.execute {
      val recipient = Recipient.resolved(recipientId)
      if (recipient.isGroup) {
        RecipientUtil.block(context, recipient)
      } else {
        RecipientUtil.blockNonGroup(context, recipient)
      }
    }
  }

  fun unblock(recipientId: RecipientId) {
    SignalExecutors.BOUNDED.execute {
      val recipient = Recipient.resolved(recipientId)
      RecipientUtil.unblock(recipient)
    }
  }

  fun block(groupId: GroupId) {
    SignalExecutors.BOUNDED.execute {
      val recipient = Recipient.externalGroupExact(groupId)
      RecipientUtil.block(context, recipient)
    }
  }

  fun unblock(groupId: GroupId) {
    SignalExecutors.BOUNDED.execute {
      val recipient = Recipient.externalGroupExact(groupId)
      RecipientUtil.unblock(recipient)
    }
  }

  @WorkerThread
  fun isMessageRequestAccepted(recipient: Recipient): Boolean {
    return RecipientUtil.isMessageRequestAccepted(context, recipient)
  }

  fun getMembershipCountDescription(liveGroup: LiveGroup): LiveData<String> {
    return liveGroup.getMembershipCountDescription(context.resources)
  }

  fun getExternalPossiblyMigratedGroupRecipientId(groupId: GroupId, consumer: (RecipientId) -> Unit) {
    SignalExecutors.BOUNDED.execute {
      consumer(Recipient.externalPossiblyMigratedGroup(groupId).id)
    }
  }
}