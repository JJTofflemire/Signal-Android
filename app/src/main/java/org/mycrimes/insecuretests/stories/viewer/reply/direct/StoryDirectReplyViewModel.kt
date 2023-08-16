package org.mycrimes.insecuretests.stories.viewer.reply.direct

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import org.mycrimes.insecuretests.database.model.databaseprotos.BodyRangeList
import org.mycrimes.insecuretests.recipients.Recipient
import org.mycrimes.insecuretests.recipients.RecipientId
import org.mycrimes.insecuretests.util.livedata.Store

class StoryDirectReplyViewModel(
  private val storyId: Long,
  private val groupDirectReplyRecipientId: RecipientId?,
  private val repository: StoryDirectReplyRepository
) : ViewModel() {

  private val store = Store(StoryDirectReplyState())
  private val disposables = CompositeDisposable()

  val state: LiveData<StoryDirectReplyState> = store.stateLiveData

  init {
    if (groupDirectReplyRecipientId != null) {
      store.update(Recipient.live(groupDirectReplyRecipientId).liveDataResolved) { recipient, state ->
        state.copy(groupDirectReplyRecipient = recipient)
      }
    }

    disposables += repository.getStoryPost(storyId).subscribe { record ->
      store.update { it.copy(storyRecord = record) }
    }
  }

  fun sendReply(body: CharSequence, bodyRangeList: BodyRangeList?): Completable {
    return repository.send(
      storyId = storyId,
      groupDirectReplyRecipientId = groupDirectReplyRecipientId,
      body = body,
      bodyRangeList = bodyRangeList,
      isReaction = false
    )
  }

  fun sendReaction(emoji: CharSequence): Completable {
    return repository.send(
      storyId = storyId,
      groupDirectReplyRecipientId = groupDirectReplyRecipientId,
      body = emoji,
      bodyRangeList = null,
      isReaction = true
    )
  }

  override fun onCleared() {
    super.onCleared()
    disposables.clear()
  }

  class Factory(
    private val storyId: Long,
    private val groupDirectReplyRecipientId: RecipientId?,
    private val repository: StoryDirectReplyRepository
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return modelClass.cast(
        StoryDirectReplyViewModel(storyId, groupDirectReplyRecipientId, repository)
      ) as T
    }
  }
}