package org.mycrimes.insecuretests.migrations

import org.mycrimes.insecuretests.database.SignalDatabase
import org.mycrimes.insecuretests.database.SignalDatabase.Companion.recipients
import org.mycrimes.insecuretests.jobmanager.Job
import org.mycrimes.insecuretests.keyvalue.SignalStore
import org.mycrimes.insecuretests.recipients.Recipient
import org.mycrimes.insecuretests.storage.StorageSyncHelper

/**
 * Added to initialize whether the user has seen the onboarding story
 */
internal class StoryReadStateMigrationJob(
  parameters: Parameters = Parameters.Builder().build()
) : MigrationJob(parameters) {

  companion object {
    const val KEY = "StoryReadStateMigrationJob"
  }

  override fun getFactoryKey(): String = KEY

  override fun isUiBlocking(): Boolean = false

  override fun performMigration() {
    if (!SignalStore.storyValues().hasUserOnboardingStoryReadBeenSet()) {
      SignalStore.storyValues().userHasReadOnboardingStory = SignalStore.storyValues().userHasReadOnboardingStory
      SignalDatabase.messages.markOnboardingStoryRead()

      if (SignalStore.account().isRegistered) {
        recipients.markNeedsSync(Recipient.self().id)
        StorageSyncHelper.scheduleSyncForDataChange()
      }
    }
  }

  override fun shouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<StoryReadStateMigrationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): StoryReadStateMigrationJob {
      return StoryReadStateMigrationJob(parameters)
    }
  }
}