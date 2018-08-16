package com.example.altuncu.blocksignal.service;


import android.content.Context;
import android.content.Intent;

import com.example.altuncu.blocksignal.ApplicationContext;
import com.example.altuncu.blocksignal.jobs.DirectoryRefreshJob;
import com.example.altuncu.blocksignal.util.TextSecurePreferences;

import java.util.concurrent.TimeUnit;

public class DirectoryRefreshListener extends PersistentAlarmManagerListener {

  private static final long INTERVAL = TimeUnit.HOURS.toMillis(12);

  @Override
  protected long getNextScheduledExecutionTime(Context context) {
    return TextSecurePreferences.getDirectoryRefreshTime(context);
  }

  @Override
  protected long onAlarm(Context context, long scheduledTime) {
    if (scheduledTime != 0 && TextSecurePreferences.isPushRegistered(context)) {
      ApplicationContext.getInstance(context)
                        .getJobManager()
                        .add(new DirectoryRefreshJob(context, true));
    }

    long newTime = System.currentTimeMillis() + INTERVAL;
    TextSecurePreferences.setDirectoryRefreshTime(context, newTime);

    return newTime;
  }

  public static void schedule(Context context) {
    new DirectoryRefreshListener().onReceive(context, new Intent());
  }
}
