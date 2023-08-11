package org.mycrimes.insecuretests.payments.preferences;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import org.signal.core.util.concurrent.SignalExecutors;
import org.signal.core.util.logging.Log;
import org.mycrimes.insecuretests.dependencies.ApplicationDependencies;
import org.mycrimes.insecuretests.jobs.PaymentLedgerUpdateJob;
import org.mycrimes.insecuretests.jobs.ProfileUploadJob;
import org.mycrimes.insecuretests.jobs.SendPaymentsActivatedJob;
import org.mycrimes.insecuretests.keyvalue.SignalStore;
import org.mycrimes.insecuretests.util.AsynchronousCallback;
import org.mycrimes.insecuretests.util.ProfileUtil;
import org.whispersystems.signalservice.api.push.exceptions.NonSuccessfulResponseCodeException;
import org.whispersystems.signalservice.internal.push.exceptions.PaymentsRegionException;

import java.io.IOException;

public class PaymentsHomeRepository {

  private static final String TAG = Log.tag(PaymentsHomeRepository.class);

  public void activatePayments(@NonNull AsynchronousCallback.WorkerThread<Void, Error> callback) {
    SignalExecutors.BOUNDED.execute(() -> {
      SignalStore.paymentsValues().setMobileCoinPaymentsEnabled(true);
      try {
        ProfileUtil.uploadProfile(ApplicationDependencies.getApplication());
        ApplicationDependencies.getJobManager()
                               .startChain(PaymentLedgerUpdateJob.updateLedger())
                               .then(new SendPaymentsActivatedJob())
                               .enqueue();
        callback.onComplete(null);
      } catch (PaymentsRegionException e) {
        SignalStore.paymentsValues().setMobileCoinPaymentsEnabled(false);
        Log.w(TAG, "Problem enabling payments in region", e);
        callback.onError(Error.RegionError);
      } catch (NonSuccessfulResponseCodeException e) {
        SignalStore.paymentsValues().setMobileCoinPaymentsEnabled(false);
        Log.w(TAG, "Problem enabling payments", e);
        callback.onError(Error.NetworkError);
      } catch (IOException e) {
        SignalStore.paymentsValues().setMobileCoinPaymentsEnabled(false);
        Log.w(TAG, "Problem enabling payments", e);
        tryToRestoreProfile();
        callback.onError(Error.NetworkError);
      }
    });
  }

  private void tryToRestoreProfile() {
    try {
      ProfileUtil.uploadProfile(ApplicationDependencies.getApplication());
      Log.i(TAG, "Restored profile");
    } catch (IOException e) {
      Log.w(TAG, "Problem uploading profile", e);
    }
  }

  public void deactivatePayments(@NonNull Consumer<Boolean> consumer) {
    SignalExecutors.BOUNDED.execute(() -> {
      SignalStore.paymentsValues().setMobileCoinPaymentsEnabled(false);
      ApplicationDependencies.getJobManager().add(new ProfileUploadJob());
      consumer.accept(!SignalStore.paymentsValues().mobileCoinPaymentsEnabled());
    });
  }

  public enum Error {
    NetworkError,
    RegionError
  }
}
