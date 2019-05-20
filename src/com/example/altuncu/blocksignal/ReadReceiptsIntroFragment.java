package com.example.altuncu.blocksignal;


import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.altuncu.blocksignal.jobs.MultiDeviceReadReceiptUpdateJob;
import com.example.altuncu.blocksignal.util.TextSecurePreferences;
import com.example.altuncu.blocksignal.util.ViewUtil;

public class ReadReceiptsIntroFragment extends Fragment {

  public static ReadReceiptsIntroFragment newInstance() {
    ReadReceiptsIntroFragment fragment = new ReadReceiptsIntroFragment();
    Bundle args = new Bundle();
    fragment.setArguments(args);
    return fragment;
  }

  public ReadReceiptsIntroFragment() {}

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View         v          = inflater.inflate(com.example.altuncu.blocksignal.R.layout.experience_upgrade_preference_fragment, container, false);
    SwitchCompat preference = ViewUtil.findById(v, com.example.altuncu.blocksignal.R.id.preference);

    preference.setChecked(TextSecurePreferences.isReadReceiptsEnabled(getContext()));
    preference.setOnCheckedChangeListener((buttonView, isChecked) -> {
      TextSecurePreferences.setReadReceiptsEnabled(getContext(), isChecked);
      ApplicationContext.getInstance(getContext())
                        .getJobManager()
                        .add(new MultiDeviceReadReceiptUpdateJob(getContext(), isChecked));
    });

    return v;
  }
}
