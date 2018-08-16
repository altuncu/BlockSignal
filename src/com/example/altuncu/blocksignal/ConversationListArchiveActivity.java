package com.example.altuncu.blocksignal;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.altuncu.blocksignal.recipients.Recipient;
import com.example.altuncu.blocksignal.util.DynamicLanguage;
import com.example.altuncu.blocksignal.util.DynamicTheme;

public class ConversationListArchiveActivity extends PassphraseRequiredActionBarActivity
    implements ConversationListFragment.ConversationSelectedListener
{

  private final DynamicTheme    dynamicTheme    = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle icicle, boolean ready) {
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(com.example.altuncu.blocksignal.R.string.AndroidManifest_archived_conversations);

    Bundle bundle = new Bundle();
    bundle.putBoolean(ConversationListFragment.ARCHIVE, true);

    initFragment(android.R.id.content, new ConversationListFragment(), dynamicLanguage.getCurrentLocale(), bundle);
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
      case com.example.altuncu.blocksignal.R.id.home: super.onBackPressed(); return true;
    }

    return false;
  }

  @Override
  public void onCreateConversation(long threadId, Recipient recipient, int distributionType, long lastSeenTime) {
    Intent intent = new Intent(this, ConversationActivity.class);
    intent.putExtra(ConversationActivity.ADDRESS_EXTRA, recipient.getAddress());
    intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, threadId);
    intent.putExtra(ConversationActivity.IS_ARCHIVED_EXTRA, true);
    intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, distributionType);
    intent.putExtra(ConversationActivity.LAST_SEEN_EXTRA, lastSeenTime);

    startActivity(intent);
    overridePendingTransition(com.example.altuncu.blocksignal.R.anim.slide_from_right, com.example.altuncu.blocksignal.R.anim.fade_scale_out);
  }

  @Override
  public void onSwitchToArchive() {
    throw new AssertionError();
  }

}
