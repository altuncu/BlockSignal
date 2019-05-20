package com.example.altuncu.blocksignal.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.example.altuncu.blocksignal.contacts.ContactsSyncAdapter;

public class ContactsSyncAdapterService extends Service {

  private static ContactsSyncAdapter syncAdapter;

  @Override
  public synchronized void onCreate() {
    if (syncAdapter == null) {
      syncAdapter = new ContactsSyncAdapter(this, true);
    }
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return syncAdapter.getSyncAdapterBinder();
  }
}
