package com.wpd.erez.walkingpatterndetector;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import static android.support.v4.app.ActivityCompat.startActivity;

/**
 * Created by erez on 08/06/2016.
 */
public class DiscliamerActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "user_conditions";
    Context context;
    // Check whether the user has already accepted
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean accepted = settings.getBoolean("accepted", false);
        if (accepted) {
            // do what ever you want... for instance:
            startActivity(new Intent(this, MainActivity.class));
        } else {
            // show disclaimer....
            // for example, you can show a dialog box... and,
            // if the user accept, you can execute something like this:

            // We need an Editor object to make preference changes.
            // All objects are from android.context.Context
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.disclaimer)
                    .setTitle("DISCLAIMER")
                    .setCancelable(false)
                    .setPositiveButton("Agree", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // and, if the user accept, you can execute something like this:
                            // We need an Editor object to make preference changes.
                            // All objects are from android.context.Context
                            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putBoolean("accepted", true);
                            // Commit the edits!
                            editor.commit();
                            Intent intent = new Intent(DiscliamerActivity.this, MainActivity.class);
                            intent.addCategory(Intent.CATEGORY_HOME);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//***Change Here***
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("Disagree", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //nm.cancel(R.notification.running); // cancel the NotificationManager (icon)
                            System.exit(0);
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

}
