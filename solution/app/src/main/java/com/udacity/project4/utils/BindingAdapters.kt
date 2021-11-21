package com.udacity.project4.utils

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.slider.Slider
import com.udacity.project4.R
import com.udacity.project4.base.BaseRecyclerViewAdapter

object BindingAdapters {

    /**
     * Use binding adapter to set the recycler view data using livedata object
     */
    @Suppress("UNCHECKED_CAST")
    @BindingAdapter("android:liveData")
    @JvmStatic
    fun <T> setRecyclerViewData(recyclerView: RecyclerView, items: LiveData<List<T>>?) {
        items?.value?.let { itemList ->
            (recyclerView.adapter as? BaseRecyclerViewAdapter<T>)?.apply {
                clear()
                addData(itemList)
            }
        }
    }

    /**
     * Use this binding adapter to show and hide the views using boolean variables
     */
    @BindingAdapter("android:fadeVisible")
    @JvmStatic
    fun setFadeVisible(view: View, visible: Boolean? = true) {
        if (view.tag == null) {
            view.tag = true
            view.visibility = if (visible == true) View.VISIBLE else View.GONE
        } else {
            view.animate().cancel()
            if (visible == true) {
                if (view.visibility == View.GONE)
                    view.fadeIn()
            } else {
                if (view.visibility == View.VISIBLE)
                    view.fadeOut()
            }
        }
    }

    @InverseBindingAdapter(attribute = "android:value")
    @JvmStatic
    fun getSliderValue(slider: Slider) = slider.value

    @BindingAdapter("android:valueAttrChanged")
    @JvmStatic
    fun setSliderListeners(slider: Slider, attrChange: InverseBindingListener) {
        slider.addOnChangeListener { _, _, _ ->
            attrChange.onChange()
        }
    }

    @BindingAdapter("app:radiusButtonIcon")
    @JvmStatic
    fun setRadiusButtonIcon(view: ExtendedFloatingActionButton, isOpen: Boolean = false) {
        view.setIconResource(if (isOpen) R.drawable.ic_arrow_drop_up else R.drawable.ic_arrow_drop_down)
    }
}