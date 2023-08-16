/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.mycrimes.insecuretests.components.settings.app.internal.search

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.mycrimes.insecuretests.database.RecipientTable
import org.mycrimes.insecuretests.database.SignalDatabase
import org.mycrimes.insecuretests.database.model.RecipientRecord
import java.util.concurrent.TimeUnit

class InternalSearchViewModel : ViewModel() {

  private val _results: MutableState<ImmutableList<InternalSearchResult>> = mutableStateOf(persistentListOf())
  val results: State<ImmutableList<InternalSearchResult>> = _results

  private val _query: MutableState<String> = mutableStateOf("")
  val query: State<String> = _query

  private val disposable: CompositeDisposable = CompositeDisposable()

  private val querySubject: BehaviorSubject<String> = BehaviorSubject.create()

  init {
    disposable += querySubject
      .distinctUntilChanged()
      .debounce(250, TimeUnit.MILLISECONDS, Schedulers.io())
      .observeOn(Schedulers.io())
      .map { query ->
        SignalDatabase.recipients.queryByInternalFields(query)
          .map { record ->
            InternalSearchResult(
              id = record.id,
              name = record.displayName(),
              aci = record.serviceId?.toString(),
              pni = record.pni.toString(),
              groupId = record.groupId
            )
          }
          .toImmutableList()
      }
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe { results ->
        _results.value = results
      }
  }

  fun onQueryChanged(value: String) {
    _query.value = value
    querySubject.onNext(value)
  }

  override fun onCleared() {
    disposable.clear()
  }

  private fun RecipientRecord.displayName(): String {
    return when {
      this.groupType == RecipientTable.GroupType.SIGNAL_V1 -> "GV1::${this.groupId}"
      this.groupType == RecipientTable.GroupType.SIGNAL_V2 -> "GV2::${this.groupId}"
      this.groupType == RecipientTable.GroupType.MMS -> "MMS_GROUP::${this.groupId}"
      this.groupType == RecipientTable.GroupType.DISTRIBUTION_LIST -> "DLIST::${this.distributionListId}"
      this.systemDisplayName?.isNotBlank() == true -> this.systemDisplayName
      this.signalProfileName.toString().isNotBlank() -> this.signalProfileName.serialize()
      this.e164 != null -> this.e164
      else -> "Unknown"
    }
  }
}