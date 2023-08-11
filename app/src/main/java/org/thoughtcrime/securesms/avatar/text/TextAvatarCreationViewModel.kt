package org.mycrimes.insecuretests.avatar.text

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.mycrimes.insecuretests.avatar.Avatar
import org.mycrimes.insecuretests.avatar.Avatars
import org.mycrimes.insecuretests.util.livedata.Store

class TextAvatarCreationViewModel(initialText: Avatar.Text) : ViewModel() {

  private val store = Store(TextAvatarCreationState(initialText))

  val state: LiveData<TextAvatarCreationState> = Transformations.distinctUntilChanged(store.stateLiveData)

  fun setColor(colorPair: Avatars.ColorPair) {
    store.update { it.copy(currentAvatar = it.currentAvatar.copy(color = colorPair)) }
  }

  fun setText(text: String) {
    store.update {
      if (it.currentAvatar.text == text) {
        it
      } else {
        it.copy(currentAvatar = it.currentAvatar.copy(text = text))
      }
    }
  }

  fun getCurrentAvatar(): Avatar.Text {
    return store.state.currentAvatar
  }

  class Factory(private val initialText: Avatar.Text) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return requireNotNull(modelClass.cast(TextAvatarCreationViewModel(initialText)))
    }
  }
}
