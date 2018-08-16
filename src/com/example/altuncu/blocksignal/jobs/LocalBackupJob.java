package com.example.altuncu.blocksignal.jobs;


import android.Manifest;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.altuncu.blocksignal.R;
import com.example.altuncu.blocksignal.backup.FullBackupExporter;
import com.example.altuncu.blocksignal.crypto.AttachmentSecretProvider;
import com.example.altuncu.blocksignal.database.DatabaseFactory;
import com.example.altuncu.blocksignal.database.NoExternalStorageException;
import com.example.altuncu.blocksignal.permissions.Permissions;
import com.example.altuncu.blocksignal.service.GenericForegroundService;
import com.example.altuncu.blocksignal.util.BackupUtil;
import com.example.altuncu.blocksignal.util.StorageUtil;
import com.example.altuncu.blocksignal.util.TextSecurePreferences;
import org.whispersystems.jobqueue.JobParameters;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class LocalBackupJob extends ContextJob {

  private static final String TAG = LocalBackupJob.class.getSimpleName();

  public LocalBackupJob(@NonNull Context context) {
    super(context, JobParameters.newBuilder()
                                .withGroupId("__LOCAL_BACKUP__")
                                .withWakeLock(true, 10, TimeUnit.SECONDS)
                                .create());
  }

  @Override
  public void onAdded() {}

  @Override
  public void onRun() throws NoExternalStorageException, IOException {
    Log.w(TAG, "Executing backup job...");

    if (!Permissions.hasAll(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
      throw new IOException("No external storage permission!");
    }

    GenericForegroundService.startForegroundTask(context,
                                                 context.getString(R.string.LocalBackupJob_creating_backup));

    try {
      String backupPassword  = TextSecurePreferences.getBackupPassphrase(context);
      File   backupDirectory = StorageUtil.getBackupDirectory(context);
      String timestamp       = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(new Date());
      String fileName        = String.format("signal-%s.backup", timestamp);
      File   backupFile      = new File(backupDirectory, fileName);

      if (backupFile.exists()) {
        throw new IOException("Backup file already exists?");
      }

      if (backupPassword == null) {
        throw new IOException("Backup password is null");
      }

      File tempFile = File.createTempFile("backup", "tmp", StorageUtil.getBackupCacheDirectory(context));

      FullBackupExporter.export(context,
                                AttachmentSecretProvider.getInstance(context).getOrCreateAttachmentSecret(),
                                DatabaseFactory.getBackupDatabase(context),
                                tempFile,
                                backupPassword);

      if (!tempFile.renameTo(backupFile)) {
        tempFile.delete();
        throw new IOException("Renaming temporary backup file failed!");
      }

      BackupUtil.deleteOldBackups(context);
    } finally {
      GenericForegroundService.stopForegroundTask(context);
    }
  }

  @Override
  public boolean onShouldRetry(Exception e) {
    return false;
  }

  @Override
  public void onCanceled() {

  }
}
