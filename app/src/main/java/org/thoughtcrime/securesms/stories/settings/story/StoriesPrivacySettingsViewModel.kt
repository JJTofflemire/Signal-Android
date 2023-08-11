package org.mycrimes.insecuretests.stories.settings.story

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import org.signal.paging.PagedData
import org.signal.paging.PagingConfig
import org.signal.paging.ProxyPagingController
import org.mycrimes.insecuretests.contacts.paged.ContactSearchConfiguration
import org.mycrimes.insecuretests.contacts.paged.ContactSearchKey
import org.mycrimes.insecuretests.contacts.paged.ContactSearchPagedDataSource
import org.mycrimes.insecuretests.contacts.paged.ContactSearchPagedDataSourceRepository
import org.mycrimes.insecuretests.keyvalue.SignalStore
import org.mycrimes.insecuretests.recipients.RecipientId
import org.mycrimes.insecuretests.stories.Stories
import org.mycrimes.insecuretests.util.rx.RxStore

class StoriesPrivacySettingsViewModel(
  contactSearchPagedDataSourceRepository: ContactSearchPagedDataSourceRepository
) : ViewModel() {

  private val repository = StoriesPrivacySettingsRepository()

  private val store = RxStore(
    StoriesPrivacySettingsState(
      areStoriesEnabled = Stories.isFeatureEnabled(),
      areViewReceiptsEnabled = SignalStore.storyValues().viewedReceiptsEnabled
    )
  )

  private val pagingConfig = PagingConfig.Builder()
    .setBufferPages(1)
    .setPageSize(20)
    .setStartIndex(0)
    .build()

  private val disposables = CompositeDisposable()

  val state: Flowable<StoriesPrivacySettingsState> = store.stateFlowable.observeOn(AndroidSchedulers.mainThread())
  val userHasActiveStories: Boolean get() = store.state.userHasStories
  val pagingController = ProxyPagingController<ContactSearchKey>()

  init {
    val configuration = ContactSearchConfiguration.build {
      addSection(
        ContactSearchConfiguration.Section.Stories(
          includeHeader = false
        )
      )
    }

    val pagedDataSource = ContactSearchPagedDataSource(configuration, contactSearchPagedDataSourceRepository)
    val observablePagedData = PagedData.createForObservable(pagedDataSource, pagingConfig)

    pagingController.set(observablePagedData.controller)

    updateUserHasStories()

    disposables += store.update(observablePagedData.data.toFlowable(BackpressureStrategy.LATEST)) { data, state ->
      state.copy(storyContactItems = data)
    }
  }

  override fun onCleared() {
    disposables.clear()
    store.dispose()
  }

  fun setStoriesEnabled(isEnabled: Boolean) {
    store.update { it.copy(isUpdatingEnabledState = true) }
    disposables += repository.setStoriesEnabled(isEnabled).subscribe {
      store.update {
        it.copy(
          isUpdatingEnabledState = false,
          areStoriesEnabled = Stories.isFeatureEnabled()
        )
      }
      updateUserHasStories()
    }
  }

  fun toggleViewReceipts() {
    SignalStore.storyValues().viewedReceiptsEnabled = !SignalStore.storyValues().viewedReceiptsEnabled
    store.update { it.copy(areViewReceiptsEnabled = SignalStore.storyValues().viewedReceiptsEnabled) }
    repository.onSettingsChanged()
  }

  fun displayGroupsAsStories(recipientIds: List<RecipientId>) {
    disposables += repository.markGroupsAsStories(recipientIds).subscribe {
      pagingController.onDataInvalidated()
    }
  }

  private fun updateUserHasStories() {
    disposables += repository.userHasOutgoingStories().subscribe { userHasActiveStories ->
      store.update { it.copy(userHasStories = userHasActiveStories) }
    }
  }

  class Factory(
    private val contactSearchPagedDataSourceRepository: ContactSearchPagedDataSourceRepository
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return modelClass.cast(StoriesPrivacySettingsViewModel(contactSearchPagedDataSourceRepository)) as T
    }
  }
}
