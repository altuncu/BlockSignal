package com.example.altuncu.blocksignal.jobs;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import android.util.Log;

import com.example.altuncu.blocksignal.crypto.MasterSecret;
import com.example.altuncu.blocksignal.database.DatabaseFactory;
import com.example.altuncu.blocksignal.database.GroupDatabase;
import com.example.altuncu.blocksignal.database.GroupDatabase.GroupRecord;
import com.example.altuncu.blocksignal.dependencies.InjectableType;
import com.example.altuncu.blocksignal.jobs.requirements.MasterSecretRequirement;
import com.example.altuncu.blocksignal.mms.AttachmentStreamUriLoader.AttachmentModel;
import com.example.altuncu.blocksignal.util.BitmapDecodingException;
import com.example.altuncu.blocksignal.util.BitmapUtil;
import com.example.altuncu.blocksignal.util.GroupUtil;
import com.example.altuncu.blocksignal.util.Hex;
import org.whispersystems.jobqueue.JobParameters;
import org.whispersystems.jobqueue.requirements.NetworkRequirement;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachmentPointer;
import org.whispersystems.signalservice.api.push.exceptions.NonSuccessfulResponseCodeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

public class AvatarDownloadJob extends MasterSecretJob implements InjectableType {

  private static final int MAX_AVATAR_SIZE = 20 * 1024 * 1024;
  private static final long serialVersionUID = 1L;

  private static final String TAG = AvatarDownloadJob.class.getSimpleName();

  @Inject transient SignalServiceMessageReceiver receiver;

  private final byte[] groupId;

  public AvatarDownloadJob(Context context, @NonNull byte[] groupId) {
    super(context, JobParameters.newBuilder()
                                .withRequirement(new MasterSecretRequirement(context))
                                .withRequirement(new NetworkRequirement(context))
                                .withPersistence()
                                .create());

    this.groupId = groupId;
  }

  @Override
  public void onAdded() {}

  @Override
  public void onRun(MasterSecret masterSecret) throws IOException {
    String                encodeId   = GroupUtil.getEncodedId(groupId, false);
    GroupDatabase         database   = DatabaseFactory.getGroupDatabase(context);
    Optional<GroupRecord> record     = database.getGroup(encodeId);
    File                  attachment = null;

    try {
      if (record.isPresent()) {
        long             avatarId    = record.get().getAvatarId();
        String           contentType = record.get().getAvatarContentType();
        byte[]           key         = record.get().getAvatarKey();
        String           relay       = record.get().getRelay();
        Optional<byte[]> digest      = Optional.fromNullable(record.get().getAvatarDigest());
        Optional<String> fileName    = Optional.absent();

        if (avatarId == -1 || key == null) {
          return;
        }

        if (digest.isPresent()) {
          Log.w(TAG, "Downloading group avatar with digest: " + Hex.toString(digest.get()));
        }

        attachment = File.createTempFile("avatar", "tmp", context.getCacheDir());
        attachment.deleteOnExit();

        SignalServiceAttachmentPointer pointer     = new SignalServiceAttachmentPointer(avatarId, contentType, key, relay, Optional.of(0), Optional.absent(), 0, 0, digest, fileName, false);
        InputStream                    inputStream = receiver.retrieveAttachment(pointer, attachment, MAX_AVATAR_SIZE);
        Bitmap                         avatar      = BitmapUtil.createScaledBitmap(context, new AttachmentModel(attachment, key, 0, digest), 500, 500);

        database.updateAvatar(encodeId, avatar);
        inputStream.close();
      }
    } catch (BitmapDecodingException | NonSuccessfulResponseCodeException | InvalidMessageException e) {
      Log.w(TAG, e);
    } finally {
      if (attachment != null)
        attachment.delete();
    }
  }

  @Override
  public void onCanceled() {}

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    if (exception instanceof IOException) return true;
    return false;
  }

}
