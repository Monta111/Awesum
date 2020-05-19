package com.monta.awesum.ultility;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.monta.awesum.AwesumApp;

public class Ultility {

    //Make needed data always fresh
    public static void keepSyncData(String id) {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        db.child(AwesumApp.DB_POSTMAIN).child(id).keepSynced(true);
        db.child(AwesumApp.DB_USER).child(id).keepSynced(true);
        db.child(AwesumApp.DB_NOTIFICATION).child(id).keepSynced(true);
        db.child(AwesumApp.DB_FOLLOW).child(id).keepSynced(true);
        db.child(AwesumApp.DB_SAVE).child(id).keepSynced(true);
        db.child(AwesumApp.DB_STORYMAIN).child(id).keepSynced(true);
    }

    //Set selected background to view
    public static void setSelectedBackground(View view, Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
        if (typedValue.resourceId != 0)
            view.setBackgroundResource(typedValue.resourceId);
        else
            view.setBackgroundColor(typedValue.data);
    }

    //Get width and height (pixel) of screen
    public static int[] getDisplayMetric(AppCompatActivity activity) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        int width = displaymetrics.widthPixels;
        return new int[]{height, width};
    }

    //Show keyboard
    public static void showKeyboard(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    //Hide keyboard
    public static void hideKeyboard(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        AppCompatActivity activity = (AppCompatActivity) context;
        if (activity.getCurrentFocus() != null && imm != null)
            imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }
}
