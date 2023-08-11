package org.mycrimes.insecuretests.components.reminder;

import android.content.Context;

import org.mycrimes.insecuretests.R;
import org.mycrimes.insecuretests.keyvalue.SignalStore;
import org.mycrimes.insecuretests.registration.RegistrationNavigationActivity;

public class PushRegistrationReminder extends Reminder {

  public PushRegistrationReminder(final Context context) {
    super(R.string.reminder_header_push_title, R.string.reminder_header_push_text);

    setOkListener(v -> context.startActivity(RegistrationNavigationActivity.newIntentForReRegistration(context)));
  }

  @Override
  public boolean isDismissable() {
    return false;
  }

  public static boolean isEligible() {
    return !SignalStore.account().isRegistered();
  }
}
