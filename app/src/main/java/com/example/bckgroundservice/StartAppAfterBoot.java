package com.example.bckgroundservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// Restart app after phone rebooting
public class StartAppAfterBoot extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals((intent.getAction()))) {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
