
package com.intelmob.trackme.util;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.intelmob.trackme.R;

public class AlertUtils {

    public static Dialog showConfirmDialog(Context context, String title, String message,
            String positiveButtonTitle,
            DialogInterface.OnClickListener onPositiveButtonClickListener,
            String negativeButtonTitle,
            DialogInterface.OnClickListener onNegativeButtonClickListener) {
        AlertDialog confirmDialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonTitle, onPositiveButtonClickListener)
                .setNegativeButton(negativeButtonTitle, onNegativeButtonClickListener)
                .create();

        confirmDialog.setOnShowListener(
                dialog -> ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_NEGATIVE)
                        .setTextColor(context.getResources().getColor(R.color.gray)));

        confirmDialog.show();
        return confirmDialog;
    }
}
