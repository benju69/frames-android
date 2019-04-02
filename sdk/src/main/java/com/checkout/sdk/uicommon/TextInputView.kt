package com.checkout.sdk.uicommon

import android.content.Context
import android.support.design.widget.TextInputLayout
import android.text.Editable
import android.util.AttributeSet
import android.view.View.OnFocusChangeListener
import com.checkout.sdk.R
import com.checkout.sdk.architecture.MvpView
import com.checkout.sdk.cvvinput.*
import com.checkout.sdk.store.InMemoryStore
import com.checkout.sdk.utils.AfterTextChangedListener
import kotlinx.android.synthetic.main.view_cvv_input.view.*

/**
 * A custom EditText with validation and handling of cvv input
 */
class TextInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    TextInputLayout(context, attrs),
    MvpView<TextInputUiState> {

    val store = InMemoryStore.Factory.get()
    private val presenter: TextInputPresenter
    private val errorText: String

    init {
        inflate(context, R.layout.view_cvv_input, this)
        val textInputAttributeProperties =
            TextInputAttributeProperties.extractFromAttributes(context, attrs)
        presenter = textInputAttributeProperties.presenter
        errorText = textInputAttributeProperties.errorText
    }

    override fun onStateUpdated(uiState: TextInputUiState) {
        if (cvv_edit_text.text.toString() != uiState.cvv) {
            cvv_edit_text.setText(uiState.cvv)
        }
        if (uiState.showError) {
            error = errorText
            isErrorEnabled = true
        } else {
            isErrorEnabled = false
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        presenter.start(this)

        cvv_edit_text.addTextChangedListener(object : AfterTextChangedListener() {
            override fun afterTextChanged(s: Editable?) {
                val textInputUseCaseBuilder = TextInputUseCase.Builder(s.toString())
                presenter.inputStateChanged(textInputUseCaseBuilder)
            }
        })

        cvv_edit_text.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            val cvvFocusChangedUseCase =
                CvvFocusChangedUseCase(
                    cvv_edit_text.text.toString(),
                    hasFocus,
                    store
                )
            presenter.focusChanged(cvvFocusChangedUseCase)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        presenter.stop()
    }

    /**
     * Reset the Cvv EditText to contain no text and clear the value
     * stored in the backing storage
     */
    fun reset() {
        val cvvResetUseCase = CvvResetUseCase(InMemoryStore.Factory.get())
        presenter.reset(cvvResetUseCase)
    }

    fun showError(show: Boolean) {
        presenter.showError(show)
    }
}