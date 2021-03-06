package com.example.altuncu.blocksignal.push;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.security.ProviderInstaller;

import com.example.altuncu.blocksignal.BuildConfig;
import com.example.altuncu.blocksignal.util.TextSecurePreferences;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;

import java.io.IOException;

public class AccountManagerFactory {

  private static final String TAG = AccountManagerFactory.class.getName();

  public static SignalServiceAccountManager createManager(Context context) {
    return new SignalServiceAccountManager(new SignalServiceNetworkAccess(context).getConfiguration(context),
            TextSecurePreferences.getLocalNumber(context),
            TextSecurePreferences.getPushServerPassword(context),
            BuildConfig.USER_AGENT);
  }

  public static SignalServiceAccountManager createManager(final Context context, String number, String password) {
    if (new SignalServiceNetworkAccess(context).isCensored(number)) {
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          try {
            ProviderInstaller.installIfNeeded(context);
          } catch (Throwable t) {
            Log.w(TAG, t);
          }
          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    return new SignalServiceAccountManager(new SignalServiceNetworkAccess(context).getConfiguration(number),
                                           number, password, BuildConfig.USER_AGENT);
  }

  public static SignalServiceAccountManager createManager(final Context context, String gaiaHubURL, String number, String password) throws IOException {
    if (new SignalServiceNetworkAccess(context).isCensored(number)) {
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          try {
            ProviderInstaller.installIfNeeded(context);
          } catch (Throwable t) {
            Log.w(TAG, t);
          }
          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    return new SignalServiceAccountManager(gaiaHubURL, new SignalServiceNetworkAccess(context).getConfiguration(number),
            password, BuildConfig.USER_AGENT);
  }

}
