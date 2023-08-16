package org.mycrimes.insecuretests.mediasend.v2.text

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.drawToBitmap
import androidx.core.view.postDelayed
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import org.signal.core.util.concurrent.LifecycleDisposable
import org.mycrimes.insecuretests.R
import org.mycrimes.insecuretests.contacts.paged.ContactSearchKey
import org.mycrimes.insecuretests.conversation.mutiselect.forward.MultiselectForwardFragmentArgs
import org.mycrimes.insecuretests.databinding.StoriesTextPostCreationFragmentBinding
import org.mycrimes.insecuretests.linkpreview.LinkPreview
import org.mycrimes.insecuretests.linkpreview.LinkPreviewRepository
import org.mycrimes.insecuretests.linkpreview.LinkPreviewViewModel
import org.mycrimes.insecuretests.linkpreview.LinkPreviewViewModel.LinkPreviewState
import org.mycrimes.insecuretests.mediasend.CameraDisplay
import org.mycrimes.insecuretests.mediasend.v2.HudCommand
import org.mycrimes.insecuretests.mediasend.v2.MediaSelectionViewModel
import org.mycrimes.insecuretests.mediasend.v2.stories.StoriesMultiselectForwardActivity
import org.mycrimes.insecuretests.mediasend.v2.text.send.TextStoryPostSendRepository
import org.mycrimes.insecuretests.mediasend.v2.text.send.TextStoryPostSendResult
import org.mycrimes.insecuretests.safety.SafetyNumberBottomSheet
import org.mycrimes.insecuretests.stories.Stories
import org.mycrimes.insecuretests.util.livedata.LiveDataUtil
import org.mycrimes.insecuretests.util.visible
import java.util.Optional

class TextStoryPostCreationFragment : Fragment(R.layout.stories_text_post_creation_fragment), TextStoryPostTextEntryFragment.Callback, SafetyNumberBottomSheet.Callbacks {

  private var _binding: StoriesTextPostCreationFragmentBinding? = null
  private val binding: StoriesTextPostCreationFragmentBinding get() = _binding!!

  private val sharedViewModel: MediaSelectionViewModel by viewModels(
    ownerProducer = {
      requireActivity()
    }
  )

  private val viewModel: TextStoryPostCreationViewModel by viewModels(
    ownerProducer = {
      requireActivity()
    },
    factoryProducer = {
      TextStoryPostCreationViewModel.Factory(TextStoryPostSendRepository())
    }
  )

  private val linkPreviewViewModel: LinkPreviewViewModel by viewModels(
    ownerProducer = {
      requireActivity()
    },
    factoryProducer = {
      LinkPreviewViewModel.Factory(LinkPreviewRepository(), true)
    }
  )

  private val lifecycleDisposable = LifecycleDisposable()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    _binding = StoriesTextPostCreationFragmentBinding.bind(view)

    binding.storyTextPost.showCloseButton()

    lifecycleDisposable.bindTo(viewLifecycleOwner)
    lifecycleDisposable += sharedViewModel.hudCommands.subscribe {
      if (it == HudCommand.GoToCapture) {
        findNavController().popBackStack()
      }
    }

    viewModel.typeface.observe(viewLifecycleOwner) { typeface ->
      binding.storyTextPost.setTypeface(typeface)
    }

    viewModel.state.observe(viewLifecycleOwner) { state ->
      binding.backgroundSelector.background = state.backgroundColor.chatBubbleMask
      binding.storyTextPost.bindFromCreationState(state)

      if (state.linkPreviewUri != null) {
        linkPreviewViewModel.onTextChanged(requireContext(), state.linkPreviewUri, 0, state.linkPreviewUri.lastIndex)
      } else {
        linkPreviewViewModel.onSend()
      }

      val canSend = state.body.isNotEmpty() || !state.linkPreviewUri.isNullOrEmpty()
      binding.send.alpha = if (canSend) 1f else 0.5f
      binding.send.isEnabled = canSend
    }

    LiveDataUtil.combineLatest(viewModel.state, linkPreviewViewModel.linkPreviewState) { viewState, linkState ->
      Pair(viewState.body.isBlank(), linkState)
    }.observe(viewLifecycleOwner) { (useLargeThumb, linkState) ->
      binding.storyTextPost.bindLinkPreviewState(linkState, View.GONE, useLargeThumb)
      binding.storyTextPost.postAdjustLinkPreviewTranslationY()
    }

    binding.storyTextPost.setTextViewClickListener {
      binding.storyTextPost.hidePostContent()
      binding.storyTextPost.isEnabled = false
      TextStoryPostTextEntryFragment().show(childFragmentManager, null)
    }

    binding.backgroundProtection.setOnClickListener {
      viewModel.cycleBackgroundColor()
    }

    binding.addLinkProtection.setOnClickListener {
      TextStoryPostLinkEntryFragment().show(childFragmentManager, null)
    }

    binding.storyTextPost.setLinkPreviewCloseListener {
      viewModel.setLinkPreview("")
    }

    val launcher = registerForActivityResult(StoriesMultiselectForwardActivity.SelectionContract()) {
      if (it.isNotEmpty()) {
        performSend(it.toSet())
      } else {
        binding.send.isClickable = true
        binding.sendInProgressIndicator.visible = false
      }
    }

    binding.send.setOnClickListener {
      binding.send.isClickable = false
      binding.sendInProgressIndicator.visible = true

      binding.storyTextPost.hideCloseButton()

      val contacts = (sharedViewModel.destination.getRecipientSearchKeyList() + sharedViewModel.destination.getRecipientSearchKey())
        .filterIsInstance(ContactSearchKey::class.java)
        .toSet()

      if (contacts.isEmpty()) {
        val bitmap = binding.storyTextPost.drawToBitmap()
        viewModel.compressToBlob(bitmap).observeOn(AndroidSchedulers.mainThread()).subscribe { uri ->
          launcher.launch(
            StoriesMultiselectForwardActivity.Args(
              MultiselectForwardFragmentArgs(
                title = R.string.MediaReviewFragment__send_to,
                canSendToNonPush = false,
                storySendRequirements = Stories.MediaTransform.SendRequirements.VALID_DURATION,
                isSearchEnabled = false
              ),
              listOf(uri)
            )
          )
        }
      } else if (sharedViewModel.isAddToGroupStoryFlow) {
        MaterialAlertDialogBuilder(requireContext())
          .setMessage(getString(R.string.MediaReviewFragment__add_to_the_group_story, sharedViewModel.state.value!!.recipient!!.getDisplayName(requireContext())))
          .setPositiveButton(R.string.MediaReviewFragment__add_to_story) { _, _ -> performSend(contacts) }
          .setNegativeButton(android.R.string.cancel) { _, _ -> }
          .show()
      } else {
        performSend(contacts)
      }
    }

    initializeScenePositioning()
  }

  override fun onResume() {
    super.onResume()
    binding.storyTextPost.showCloseButton()
    requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
  }

  override fun onDestroy() {
    super.onDestroy()
    _binding = null
  }

  override fun onTextStoryPostTextEntryDismissed() {
    binding.storyTextPost.postDelayed(resources.getInteger(R.integer.text_entry_exit_duration).toLong()) {
      binding.storyTextPost.showPostContent()
      binding.storyTextPost.isEnabled = true
    }
  }

  private fun initializeScenePositioning() {
    val cameraDisplay = CameraDisplay.getDisplay(requireActivity())

    if (!cameraDisplay.roundViewFinderCorners) {
      binding.storyTextPostCard.radius = 0f
    }

    binding.send.updateLayoutParams<ConstraintLayout.LayoutParams> {
      bottomMargin = cameraDisplay.getToggleBottomMargin()
    }

    listOf(binding.backgroundProtection, binding.addLinkProtection).forEach {
      it.updateLayoutParams<ConstraintLayout.LayoutParams> {
        bottomMargin += cameraDisplay.getCameraCaptureMarginBottom(resources)
      }
    }

    if (cameraDisplay.getCameraViewportGravity() == CameraDisplay.CameraViewportGravity.CENTER) {
      val constraintSet = ConstraintSet()
      constraintSet.clone(binding.scene)
      constraintSet.connect(R.id.story_text_post_card, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
      constraintSet.applyTo(binding.scene)
    } else {
      binding.storyTextPostCard.updateLayoutParams<ConstraintLayout.LayoutParams> {
        bottomMargin = cameraDisplay.getCameraViewportMarginBottom()
      }
    }
  }

  private fun performSend(contacts: Set<ContactSearchKey>) {
    lifecycleDisposable += viewModel.send(
      contacts = contacts,
      getLinkPreview()
    ).observeOn(AndroidSchedulers.mainThread()).subscribe { result ->
      when (result) {
        TextStoryPostSendResult.Success -> {
          Toast.makeText(requireContext(), R.string.TextStoryPostCreationFragment__sent_story, Toast.LENGTH_SHORT).show()
          requireActivity().finish()
        }
        TextStoryPostSendResult.Failure -> {
          Toast.makeText(requireContext(), R.string.TextStoryPostCreationFragment__failed_to_send_story, Toast.LENGTH_SHORT).show()
          requireActivity().finish()
        }
        is TextStoryPostSendResult.UntrustedRecordsError -> {
          binding.send.isClickable = true
          binding.sendInProgressIndicator.visible = false

          SafetyNumberBottomSheet
            .forIdentityRecordsAndDestinations(result.untrustedRecords, contacts.toList())
            .show(childFragmentManager)
        }
      }
    }
  }

  private fun getLinkPreview(): LinkPreview? {
    val linkPreviewState: LinkPreviewState = linkPreviewViewModel.linkPreviewState.value ?: return null

    return if (linkPreviewState.linkPreview.isPresent) {
      linkPreviewState.linkPreview.get()
    } else if (!linkPreviewState.activeUrlForError.isNullOrEmpty()) {
      LinkPreview(linkPreviewState.activeUrlForError!!, linkPreviewState.activeUrlForError!!, "", 0L, Optional.empty())
    } else {
      null
    }
  }

  override fun sendAnywayAfterSafetyNumberChangedInBottomSheet(destinations: List<ContactSearchKey.RecipientSearchKey>) {
    performSend(destinations.toSet())
  }

  override fun onMessageResentAfterSafetyNumberChangeInBottomSheet() {
    error("Unsupported, we do not hand in a message id.")
  }

  override fun onCanceled() = Unit
}