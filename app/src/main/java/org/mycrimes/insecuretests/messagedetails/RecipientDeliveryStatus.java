package org.mycrimes.insecuretests.messagedetails;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mycrimes.insecuretests.database.documents.IdentityKeyMismatch;
import org.mycrimes.insecuretests.database.documents.NetworkFailure;
import org.mycrimes.insecuretests.database.model.MessageRecord;
import org.mycrimes.insecuretests.recipients.Recipient;

public final class RecipientDeliveryStatus {

  enum Status {
    UNKNOWN, PENDING, SENT, DELIVERED, READ, VIEWED, SKIPPED,
  }

  private final MessageRecord       messageRecord;
  private final Recipient           recipient;
  private final Status              deliveryStatus;
  private final boolean             isUnidentified;
  private final long                timestamp;
  private final NetworkFailure      networkFailure;
  private final IdentityKeyMismatch keyMismatchFailure;

  RecipientDeliveryStatus(@NonNull MessageRecord messageRecord, @NonNull Recipient recipient, @NonNull Status deliveryStatus, boolean isUnidentified, long timestamp, @Nullable NetworkFailure networkFailure, @Nullable IdentityKeyMismatch keyMismatchFailure) {
    this.messageRecord      = messageRecord;
    this.recipient          = recipient;
    this.deliveryStatus     = deliveryStatus;
    this.isUnidentified     = isUnidentified;
    this.timestamp          = timestamp;
    this.networkFailure     = networkFailure;
    this.keyMismatchFailure = keyMismatchFailure;
  }

  public @NonNull MessageRecord getMessageRecord() {
    return messageRecord;
  }

  public @NonNull Status getDeliveryStatus() {
    return deliveryStatus;
  }

  public boolean isUnidentified() {
    return isUnidentified;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public @NonNull Recipient getRecipient() {
    return recipient;
  }

  public @Nullable NetworkFailure getNetworkFailure() {
    return networkFailure;
  }

  public @Nullable IdentityKeyMismatch getKeyMismatchFailure() {
    return keyMismatchFailure;
  }
}
