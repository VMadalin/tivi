/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.tivi.episodedetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import app.tivi.DaggerMvRxFragment
import app.tivi.common.epoxy.SwipeAwayCallbacks
import app.tivi.episodedetails.databinding.FragmentEpisodeDetailsBinding
import app.tivi.extensions.resolveThemeColor
import com.airbnb.epoxy.EpoxyTouchHelper
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import javax.inject.Inject

class EpisodeDetailsFragment : DaggerMvRxFragment(), EpisodeDetailsViewModel.FactoryProvider {
    companion object {
        @JvmStatic
        fun create(id: Long): EpisodeDetailsFragment {
            return EpisodeDetailsFragment().apply {
                arguments = bundleOf(MvRx.KEY_ARG to EpisodeDetailsArguments(id))
            }
        }
    }

    private val viewModel: EpisodeDetailsViewModel by fragmentViewModel()
    @Inject lateinit var episodeDetailsViewModelFactory: EpisodeDetailsViewModel.Factory

    override fun provide(): EpisodeDetailsViewModel.Factory = episodeDetailsViewModelFactory

    @Inject lateinit var controller: EpisodeDetailsEpoxyController
    @Inject lateinit var textCreator: EpisodeDetailsTextCreator

    private lateinit var binding: FragmentEpisodeDetailsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentEpisodeDetailsBinding.inflate(layoutInflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.textCreator = textCreator
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.epDetailsRv.setController(controller)

        binding.epDetailsFab.setOnClickListener {
            withState(viewModel) { state ->
                when (state.action) {
                    EpisodeDetailsViewState.Action.WATCH -> viewModel.markWatched()
                    EpisodeDetailsViewState.Action.UNWATCH -> viewModel.markUnwatched()
                }
            }
        }

        val context = requireContext()
        val swipeCallback = object : SwipeAwayCallbacks<EpDetailsWatchItemBindingModel_>(
                context.getDrawable(R.drawable.ic_eye_off_24dp)!!,
                context.resources.getDimensionPixelSize(R.dimen.spacing_large),
                context.getColor(R.color.swipe_away_background),
                context.resolveThemeColor(R.attr.colorSecondary)
        ) {
            override fun onSwipeCompleted(
                model: EpDetailsWatchItemBindingModel_,
                itemView: View,
                position: Int,
                direction: Int
            ) {
                model.watch().also(viewModel::removeWatchEntry)
            }

            override fun isSwipeEnabledForModel(model: EpDetailsWatchItemBindingModel_): Boolean {
                return model.watch() != null
            }
        }

        EpoxyTouchHelper.initSwiping(binding.epDetailsRv)
                .let { if (view.layoutDirection == View.LAYOUT_DIRECTION_RTL) it.right() else it.left() }
                .withTarget(EpDetailsWatchItemBindingModel_::class.java)
                .andCallbacks(swipeCallback)
    }

    override fun invalidate() {
        withState(viewModel) { state ->
            binding.state = state
            controller.setData(state)
        }
    }
}