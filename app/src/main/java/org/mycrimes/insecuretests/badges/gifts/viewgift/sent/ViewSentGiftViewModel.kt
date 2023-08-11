package org.mycrimes.insecuretests.badges.gifts.viewgift.sent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import org.mycrimes.insecuretests.badges.gifts.viewgift.ViewGiftRepository
import org.mycrimes.insecuretests.database.model.databaseprotos.GiftBadge
import org.mycrimes.insecuretests.recipients.Recipient
import org.mycrimes.insecuretests.recipients.RecipientId
import org.mycrimes.insecuretests.util.rx.RxStore

class ViewSentGiftViewModel(
  sentFrom: RecipientId,
  giftBadge: GiftBadge,
  repository: ViewGiftRepository
) : ViewModel() {

  private val store = RxStore(ViewSentGiftState())
  private val disposables = CompositeDisposable()

  val state: Flowable<ViewSentGiftState> = store.stateFlowable

  init {
    disposables += Recipient.observable(sentFrom).subscribe { recipient ->
      store.update { it.copy(recipient = recipient) }
    }

    disposables += repository.getBadge(giftBadge).subscribe { badge ->
      store.update {
        it.copy(
          badge = badge
        )
      }
    }
  }

  override fun onCleared() {
    disposables.dispose()
    store.dispose()
  }

  class Factory(
    private val sentFrom: RecipientId,
    private val giftBadge: GiftBadge,
    private val repository: ViewGiftRepository
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return modelClass.cast(ViewSentGiftViewModel(sentFrom, giftBadge, repository)) as T
    }
  }
}
