package com.example.altuncu.blocksignal;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.cursoradapter.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.altuncu.blocksignal.database.Address;
import com.example.altuncu.blocksignal.database.loaders.BlockedContactsLoader;
import com.example.altuncu.blocksignal.mms.GlideApp;
import com.example.altuncu.blocksignal.mms.GlideRequests;
import com.example.altuncu.blocksignal.preferences.BlockedContactListItem;
import com.example.altuncu.blocksignal.recipients.Recipient;
import com.example.altuncu.blocksignal.util.DynamicLanguage;
import com.example.altuncu.blocksignal.util.DynamicTheme;

public class BlockedContactsActivity extends PassphraseRequiredActionBarActivity {

  private final DynamicTheme    dynamicTheme    = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  @Override
  public void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }


  @Override
  public void onCreate(Bundle bundle, boolean ready) {
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(com.example.altuncu.blocksignal.R.string.BlockedContactsActivity_blocked_contacts);
    initFragment(android.R.id.content, new BlockedContactsFragment());
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home: finish(); return true;
    }

    return false;
  }

  public static class BlockedContactsFragment
      extends ListFragment
      implements LoaderManager.LoaderCallbacks<Cursor>, ListView.OnItemClickListener
  {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
      return inflater.inflate(com.example.altuncu.blocksignal.R.layout.blocked_contacts_fragment, container, false);
    }

    @Override
    public void onCreate(Bundle bundle) {
      super.onCreate(bundle);
      setListAdapter(new BlockedContactAdapter(getActivity(), GlideApp.with(this), null));
      getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
      super.onActivityCreated(bundle);
      getListView().setOnItemClickListener(this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
      return new BlockedContactsLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
      if (getListAdapter() != null) {
        ((CursorAdapter) getListAdapter()).changeCursor(data);
      }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
      if (getListAdapter() != null) {
        ((CursorAdapter) getListAdapter()).changeCursor(null);
      }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      Recipient recipient = ((BlockedContactListItem)view).getRecipient();
      Intent    intent    = new Intent(getActivity(), RecipientPreferenceActivity.class);
      intent.putExtra(RecipientPreferenceActivity.ADDRESS_EXTRA, recipient.getAddress());

      startActivity(intent);
    }

    private static class BlockedContactAdapter extends CursorAdapter {

      private final GlideRequests glideRequests;

      BlockedContactAdapter(@NonNull Context context, @NonNull GlideRequests glideRequests, @Nullable Cursor c) {
        super(context, c);
        this.glideRequests = glideRequests;
      }

      @Override
      public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context)
                             .inflate(com.example.altuncu.blocksignal.R.layout.blocked_contact_list_item, parent, false);
      }

      @Override
      public void bindView(View view, Context context, Cursor cursor) {
        String    address   = cursor.getString(1);
        Recipient recipient = Recipient.from(context, Address.fromSerialized(address), true);

        ((BlockedContactListItem) view).set(glideRequests, recipient);
      }
    }

  }

}
