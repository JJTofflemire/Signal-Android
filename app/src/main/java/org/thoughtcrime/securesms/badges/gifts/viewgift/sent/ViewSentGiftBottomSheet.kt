package org.mycrimes.insecuretests.badges.gifts.viewgift.sent

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.signal.core.util.DimensionUnit
import org.signal.core.util.concurrent.LifecycleDisposable
import org.signal.core.util.getParcelableCompat
import org.mycrimes.insecuretests.R
import org.mycrimes.insecuretests.badges.gifts.viewgift.ViewGiftRepository
import org.mycrimes.insecuretests.badges.models.BadgeDisplay112
import org.mycrimes.insecuretests.components.settings.DSLConfiguration
import org.mycrimes.insecuretests.components.settings.DSLSettingsAdapter
import org.mycrimes.insecuretests.components.settings.DSLSettingsBottomSheetFragment
import org.mycrimes.insecuretests.components.settings.DSLSettingsText
import org.mycrimes.insecuretests.components.settings.configure
import org.mycrimes.insecuretests.database.model.MmsMessageRecord
import org.mycrimes.insecuretests.database.model.databaseprotos.GiftBadge
import org.mycrimes.insecuretests.recipients.RecipientId
import org.mycrimes.insecuretests.util.BottomSheetUtil

/**
 * Handles all interactions for received gift badges.
 */
class ViewSentGiftBottomSheet : DSLSettingsBottomSheetFragment() {

  companion object {
    private const val ARG_GIFT_BADGE = "arg.gift.badge"
    private const val ARG_SENT_TO = "arg.sent.to"

    @JvmStatic
    fun show(fragmentManager: FragmentManager, messageRecord: MmsMessageRecord) {
      ViewSentGiftBottomSheet().apply {
        arguments = Bundle().apply {
          putParcelable(ARG_SENT_TO, messageRecord.toRecipient.id)
          putByteArray(ARG_GIFT_BADGE, messageRecord.giftBadge!!.toByteArray())
        }
        show(fragmentManager, BottomSheetUtil.STANDARD_BOTTOM_SHEET_FRAGMENT_TAG)
      }
    }
  }

  private val sentTo: RecipientId
    get() = requireArguments().getParcelableCompat(ARG_SENT_TO, RecipientId::class.java)!!

  private val giftBadge: GiftBadge
    get() = GiftBadge.parseFrom(requireArguments().getByteArray(ARG_GIFT_BADGE))

  private val lifecycleDisposable = LifecycleDisposable()

  private val viewModel: ViewSentGiftViewModel by viewModels(
    factoryProducer = { ViewSentGiftViewModel.Factory(sentTo, giftBadge, ViewGiftRepository()) }
  )

  override fun bindAdapter(adapter: DSLSettingsAdapter) {
    BadgeDisplay112.register(adapter)

    lifecycleDisposable.bindTo(viewLifecycleOwner)
    lifecycleDisposable += viewModel.state.observeOn(AndroidSchedulers.mainThread()).subscribe { state ->
      adapter.submitList(getConfiguration(state).toMappingModelList())
    }
  }

  private fun getConfiguration(state: ViewSentGiftState): DSLConfiguration {
    return configure {
      noPadTextPref(
        title = DSLSettingsText.from(
          stringId = R.string.ViewSentGiftBottomSheet__thanks_for_your_support,
          DSLSettingsText.CenterModifier,
          DSLSettingsText.TitleLargeModifier
        )
      )

      space(DimensionUnit.DP.toPixels(8f).toInt())

      if (state.recipient != null) {
        noPadTextPref(
          title = DSLSettingsText.from(
            charSequence = getString(R.string.ViewSentGiftBottomSheet__youve_made_a_donation_to_signal, state.recipient.getDisplayName(requireContext())),
            DSLSettingsText.CenterModifier
          )
        )

        space(DimensionUnit.DP.toPixels(30f).toInt())
      }

      if (state.badge != null) {
        customPref(
          BadgeDisplay112.Model(
            badge = state.badge
          )
        )
      }
    }
  }
}
