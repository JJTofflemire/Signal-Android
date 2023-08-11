package org.mycrimes.insecuretests.components.settings.models

import android.view.View
import android.widget.TextView
import com.google.android.material.materialswitch.MaterialSwitch
import org.mycrimes.insecuretests.R
import org.mycrimes.insecuretests.components.settings.DSLSettingsText
import org.mycrimes.insecuretests.util.adapter.mapping.LayoutFactory
import org.mycrimes.insecuretests.util.adapter.mapping.MappingAdapter
import org.mycrimes.insecuretests.util.adapter.mapping.MappingModel
import org.mycrimes.insecuretests.util.adapter.mapping.MappingViewHolder

object OutlinedSwitch {
  fun register(mappingAdapter: MappingAdapter) {
    mappingAdapter.registerFactory(Model::class.java, LayoutFactory(::ViewHolder, R.layout.outlined_switch))
  }

  class Model(
    val key: String = "OutlinedSwitch",
    val text: DSLSettingsText,
    val isChecked: Boolean,
    val isEnabled: Boolean,
    val onClick: (Model) -> Unit
  ) : MappingModel<Model> {
    override fun areItemsTheSame(newItem: Model): Boolean = newItem.key == key

    override fun areContentsTheSame(newItem: Model): Boolean {
      return areItemsTheSame(newItem) &&
        text == newItem.text &&
        isChecked == newItem.isChecked &&
        isEnabled == newItem.isEnabled
    }
  }

  class ViewHolder(itemView: View) : MappingViewHolder<Model>(itemView) {

    private val text: TextView = findViewById(R.id.outlined_switch_control_text)
    private val switch: MaterialSwitch = findViewById(R.id.outlined_switch_switch)

    override fun bind(model: Model) {
      text.text = model.text.resolve(context)
      switch.isChecked = model.isChecked
      switch.setOnClickListener { model.onClick(model) }
      itemView.isEnabled = model.isEnabled
    }
  }
}
