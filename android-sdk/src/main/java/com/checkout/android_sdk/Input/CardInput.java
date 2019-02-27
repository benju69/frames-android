package com.checkout.android_sdk.Input;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;

import com.checkout.android_sdk.Store.DataStore;
import com.checkout.android_sdk.UseCase.CardInputUseCase;
import com.checkout.android_sdk.Utils.CardUtils;

/**
 * <h1>CardInput class</h1>
 * The CardInput class has the purpose extending an AppCompatEditText and provide validation
 * and formatting for the user's card details.
 * <p>
 * This class will validate on the "afterTextChanged" event and display a card icon on the right
 * side based on  the users input. It will also span spaces following the {@link CardUtils} details.
 */
public class CardInput extends android.support.v7.widget.AppCompatEditText implements CardInputUseCase.Callback {

    @Override
    public void onCardInputResult(@NonNull CardInputUseCase.CardInputResult cardInputResult) {
        // Save State
        mDataStore.setCardNumber(cardInputResult.getCardNumber());
        mDataStore.setCvvLength(cardInputResult.getCardType().maxCvvLength);
        // Get Card type
        setFilters(new InputFilter[]{new InputFilter.LengthFilter(cardInputResult.getCardType().maxCardLength)});
        // Set the CardInput icon based on the type of card
        setCardTypeIcon(cardInputResult.getCardType());
        if (mCardInputListener != null && cardInputResult.getInputFinished()) {
            mCardInputListener.onCardInputFinish(cardInputResult.getCardNumber());
        }
    }

    /**
     * An interface needed to communicate with the parent once the field is successfully completed
     */
    public interface Listener {
        void onCardInputFinish(String number);

        void onCardError();

        void onClearCardError();
    }

    private @Nullable
    CardInput.Listener mCardInputListener;
    Context mContext;
    DataStore mDataStore = DataStore.getInstance();
    final CardUtils mCardUtils = new CardUtils();

    public CardInput(Context context) {
        this(context, null);
    }

    public CardInput(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    /**
     * The UI initialisation
     * <p>
     * Used to initialise element as well as setting up appropriate listeners
     */
    private void init() {

        // Add listener for text input
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable text) {
                // Remove error if the user is typing - TODO: This should do in the UseCase
                if (mCardInputListener != null) {
                    mCardInputListener.onClearCardError();
                }
                new CardInputUseCase(CardInput.this, text).execute();
            }
        });

        // Add listener for focus

        // When the CardInput loses focus check if the card number is not valid and trigger an error
        setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (mCardInputListener != null && !mCardUtils.isValidCard(mDataStore.getCardNumber())) {
                        mCardInputListener.onCardError();
                    }
                } else {
                    // Clear the error message until the field loses focus
                    if (mCardInputListener != null) {
                        mCardInputListener.onClearCardError();
                    }
                }
            }
        });
    }

    /**
     * This method is used to validate the card number
     */
    public void checkIfCardIsValid(String number, CardUtils.Cards cardType) {
        boolean hasDesiredLength = false;
        for (int i : cardType.cardLength) {
            if (i == number.length()) {
                hasDesiredLength = true;
                break;
            }
        }
        if (mCardUtils.isValidCard(number) && hasDesiredLength) {
            if (mCardInputListener != null) {
                mCardInputListener.onCardInputFinish(sanitizeEntry(number));
            }
            mDataStore.setCvvLength(cardType.maxCvvLength);
        }
    }

    /**
     * This method will display a card icon associated to the specific card scheme
     */
    public void setCardTypeIcon(CardUtils.Cards type) {
        Drawable img;
        if (type.resourceId != 0) {
            img = getContext().getResources().getDrawable(type.resourceId);
            img.setBounds(0, 0, 68, 68);
            setCompoundDrawables(null, null, img, null);
            setCompoundDrawablePadding(5);
        } else {
            setCompoundDrawables(null, null, null, null);
        }
    }

    /**
     * This method will clear the whitespace in a number string
     */
    public static String sanitizeEntry(String entry) {
        return entry.replaceAll("\\D", "");
    }

    /**
     * Used to set the callback listener for when the card input is completed
     */
    public void setCardListener(Listener listener) {
        this.mCardInputListener = listener;
    }
}
