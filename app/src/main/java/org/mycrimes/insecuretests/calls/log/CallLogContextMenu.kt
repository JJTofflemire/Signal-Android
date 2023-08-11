package org.mycrimes.insecuretests.calls.log

import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import org.mycrimes.insecuretests.R
import org.mycrimes.insecuretests.calls.links.details.CallLinkDetailsActivity
import org.mycrimes.insecuretests.components.menu.ActionItem
import org.mycrimes.insecuretests.components.menu.SignalContextMenu
import org.mycrimes.insecuretests.components.settings.conversation.ConversationSettingsActivity
import org.mycrimes.insecuretests.conversation.ConversationIntents
import org.mycrimes.insecuretests.database.CallTable
import org.mycrimes.insecuretests.recipients.Recipient
import org.mycrimes.insecuretests.util.CommunicationActions

/**
 * Context menu for row items on the Call Log screen.
 */
class CallLogContextMenu(
  private val fragment: Fragment,
  private val callbacks: Callbacks
) {
  fun show(recyclerView: RecyclerView, anchor: View, call: CallLogRow.Call) {
    recyclerView.suppressLayout(true)
    anchor.isSelected = true
    SignalContextMenu.Builder(anchor, anchor.parent as ViewGroup)
      .preferredVerticalPosition(SignalContextMenu.VerticalPosition.BELOW)
      .onDismiss {
        anchor.isSelected = false
        recyclerView.suppressLayout(false)
      }
      .show(
        listOfNotNull(
          getVideoCallActionItem(call.peer),
          getAudioCallActionItem(call),
          getGoToChatActionItem(call),
          getInfoActionItem(call.peer, (call.id as CallLogRow.Id.Call).children.toLongArray()),
          getSelectActionItem(call),
          getDeleteActionItem(call)
        )
      )
  }

  fun show(recyclerView: RecyclerView, anchor: View, callLink: CallLogRow.CallLink) {
    recyclerView.suppressLayout(true)
    anchor.isSelected = true
    SignalContextMenu.Builder(anchor, anchor.parent as ViewGroup)
      .preferredVerticalPosition(SignalContextMenu.VerticalPosition.BELOW)
      .onDismiss {
        anchor.isSelected = false
        recyclerView.suppressLayout(false)
      }
      .show(
        listOfNotNull(
          getVideoCallActionItem(callLink.recipient),
          getInfoActionItem(callLink.recipient, longArrayOf()),
          getSelectActionItem(callLink),
          getDeleteActionItem(callLink)
        )
      )
  }

  private fun getVideoCallActionItem(peer: Recipient): ActionItem {
    // TODO [alex] -- Need group calling disposition to make this correct
    return ActionItem(
      iconRes = R.drawable.symbol_video_24,
      title = fragment.getString(R.string.CallContextMenu__video_call)
    ) {
      CommunicationActions.startVideoCall(fragment, peer)
    }
  }

  private fun getAudioCallActionItem(call: CallLogRow.Call): ActionItem? {
    if (call.peer.isCallLink || call.peer.isGroup) {
      return null
    }

    return ActionItem(
      iconRes = R.drawable.symbol_phone_24,
      title = fragment.getString(R.string.CallContextMenu__audio_call)
    ) {
      CommunicationActions.startVoiceCall(fragment, call.peer)
    }
  }

  private fun getGoToChatActionItem(call: CallLogRow.Call): ActionItem? {
    return when {
      call.peer.isCallLink -> null
      else -> ActionItem(
        iconRes = R.drawable.symbol_open_24,
        title = fragment.getString(R.string.CallContextMenu__go_to_chat)
      ) {
        fragment.startActivity(ConversationIntents.createBuilder(fragment.requireContext(), call.peer.id, -1L).build())
      }
    }
  }

  private fun getInfoActionItem(peer: Recipient, messageIds: LongArray): ActionItem {
    return ActionItem(
      iconRes = R.drawable.symbol_info_24,
      title = fragment.getString(R.string.CallContextMenu__info)
    ) {
      val intent = when {
        peer.isCallLink -> CallLinkDetailsActivity.createIntent(fragment.requireContext(), peer.requireCallLinkRoomId())
        else -> ConversationSettingsActivity.forCall(fragment.requireContext(), peer, messageIds)
      }
      fragment.startActivity(intent)
    }
  }

  private fun getSelectActionItem(call: CallLogRow): ActionItem {
    return ActionItem(
      iconRes = R.drawable.symbol_check_circle_24,
      title = fragment.getString(R.string.CallContextMenu__select)
    ) {
      callbacks.startSelection(call)
    }
  }

  private fun getDeleteActionItem(call: CallLogRow): ActionItem? {
    if (call is CallLogRow.Call && call.record.event == CallTable.Event.ONGOING) {
      return null
    }

    return ActionItem(
      iconRes = R.drawable.symbol_trash_24,
      title = fragment.getString(R.string.CallContextMenu__delete)
    ) {
      callbacks.deleteCall(call)
    }
  }

  interface Callbacks {
    fun startSelection(call: CallLogRow)
    fun deleteCall(call: CallLogRow)
  }
}
