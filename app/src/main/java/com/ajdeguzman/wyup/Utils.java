/**
 * Author: UCU Knight Coders on 9/07/2016.
 * Website: http://facebook.com/teamucuccs
 * E-mail: teamucuccs@gmail.com
 */


package com.ajdeguzman.wyup;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;

public class Utils {


    private static Context mContext;
    private static View mView;

    private Utils(){
        throw new AssertionError();
    }
    public Utils(Context mContext){
        this.mContext = mContext;
    }
    public Utils(View view){
        this.mView = view;
    }
    public static void showToast(String msg, int length){
        Toast.makeText(mContext, msg, length).show();
    }
    public static void showSnackBar(String msg, CoordinatorLayout layout, int length){
        Snackbar.make(layout, msg, length).show();
    }
    public static void showSnackBar(int msg, CoordinatorLayout layout, int length){
        Snackbar.make(layout, msg, length).show();
    }
    static String convertBitmapToBase64(Bitmap bitmap) {
        String mAttachedImage;
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bao);
        byte[] ba = bao.toByteArray();
        mAttachedImage = Base64.encodeToString(ba, Base64.DEFAULT);
        return mAttachedImage;
    }

}
