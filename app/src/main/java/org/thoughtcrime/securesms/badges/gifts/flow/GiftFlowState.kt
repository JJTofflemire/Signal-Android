package org.mycrimes.insecuretests.badges.gifts.flow

import org.signal.core.util.money.FiatMoney
import org.mycrimes.insecuretests.badges.models.Badge
import org.mycrimes.insecuretests.recipients.Recipient
import java.util.Currency

/**
 * State maintained by the GiftFlowViewModel
 */
data class GiftFlowState(
  val currency: Currency,
  val giftLevel: Long? = null,
  val giftBadge: Badge? = null,
  val giftPrices: Map<Currency, FiatMoney> = emptyMap(),
  val stage: Stage = Stage.INIT,
  val recipient: Recipient? = null,
  val additionalMessage: CharSequence? = null
) {
  enum class Stage {
    INIT,
    READY,
    RECIPIENT_VERIFICATION,
    TOKEN_REQUEST,
    PAYMENT_PIPELINE,
    FAILURE;
  }
}
