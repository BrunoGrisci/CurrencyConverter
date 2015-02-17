/*Bruno Iochins Grisci ID 1497778
Mobile and Ubiquitous Computing
University of Birmingham 2015/1 */

package com.bruno.currencyconverter;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * Created by bruno on 11/02/15.
 */
public class EditTextCurrency extends EditText{

    public EditTextCurrency(Context context) {
        super(context);
        init();
    }

    public EditTextCurrency(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EditTextCurrency(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init () {

    }

    public boolean isEmpty() {
        return getText().toString().trim().length() == 0;
    }

    public double getDoubleValue() {
        try {
            double d = Double.valueOf(getText().toString().trim()).doubleValue();
            return d;
        } catch (NumberFormatException nfe) {
            System.out.println("NumberFormatException: " + nfe.getMessage());
            return 0.0;
        }
    }
}
