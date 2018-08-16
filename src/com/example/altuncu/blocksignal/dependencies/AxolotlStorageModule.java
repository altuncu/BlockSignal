package com.example.altuncu.blocksignal.dependencies;

import android.content.Context;

import com.example.altuncu.blocksignal.crypto.storage.SignalProtocolStoreImpl;
import com.example.altuncu.blocksignal.jobs.CleanPreKeysJob;
import org.whispersystems.libsignal.state.SignedPreKeyStore;

import dagger.Module;
import dagger.Provides;

@Module (complete = false, injects = {CleanPreKeysJob.class})
public class AxolotlStorageModule {

  private final Context context;

  public AxolotlStorageModule(Context context) {
    this.context = context;
  }

  @Provides SignedPreKeyStoreFactory provideSignedPreKeyStoreFactory() {
    return new SignedPreKeyStoreFactory() {
      @Override
      public SignedPreKeyStore create() {
        return new SignalProtocolStoreImpl(context);
      }
    };
  }

  public static interface SignedPreKeyStoreFactory {
    public SignedPreKeyStore create();
  }
}
