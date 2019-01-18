package com.example.altuncu.blocksignal.components.identity;


import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import com.example.altuncu.blocksignal.R;
import com.example.altuncu.blocksignal.database.DatabaseFactory;
import com.example.altuncu.blocksignal.database.IdentityDatabase;
import com.example.altuncu.blocksignal.database.IdentityDatabase.IdentityRecord;

import java.util.List;

import static org.whispersystems.libsignal.SessionCipher.SESSION_LOCK;

public class UntrustedSendDialog extends AlertDialog.Builder implements DialogInterface.OnClickListener {

  private final List<IdentityRecord> untrustedRecords;
  private final ResendListener       resendListener;

  public UntrustedSendDialog(@NonNull Context context,
                             @NonNull String message,
                             @NonNull List<IdentityRecord> untrustedRecords,
                             @NonNull ResendListener resendListener)
  {
    super(new android.view.ContextThemeWrapper(context,R.style.AlertDialogCustom));
    this.untrustedRecords = untrustedRecords;
    this.resendListener   = resendListener;

    setTitle(R.string.UntrustedSendDialog_send_message);
    setIconAttribute(R.attr.dialog_alert_icon);
    setMessage(message);
    setPositiveButton(R.string.UntrustedSendDialog_send, this);
    setNegativeButton(android.R.string.cancel, null);
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    final IdentityDatabase identityDatabase = DatabaseFactory.getIdentityDatabase(getContext());

    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        synchronized (SESSION_LOCK) {
          for (IdentityRecord identityRecord : untrustedRecords) {
            identityDatabase.setApproval(identityRecord.getAddress(), true);
          }
        }

        return null;
      }

      @Override
      protected void onPostExecute(Void result) {
        resendListener.onResendMessage();
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  public interface ResendListener {
    public void onResendMessage();
  }
}
