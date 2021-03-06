package com.example.altuncu.blocksignal.preferences;


import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import android.view.View;

import com.example.altuncu.blocksignal.components.CustomDefaultPreference;
import com.example.altuncu.blocksignal.preferences.widgets.ColorPickerPreference;
import com.example.altuncu.blocksignal.preferences.widgets.ColorPickerPreferenceDialogFragmentCompat;

public abstract class CorrectedPreferenceFragment extends PreferenceFragmentCompat {

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    View lv = getView().findViewById(android.R.id.list);
    if (lv != null) lv.setPadding(0, 0, 0, 0);
  }

  @Override
  public void onDisplayPreferenceDialog(Preference preference) {
    DialogFragment dialogFragment = null;

    if (preference instanceof ColorPickerPreference) {
      dialogFragment = ColorPickerPreferenceDialogFragmentCompat.newInstance(preference.getKey());
    } else if (preference instanceof CustomDefaultPreference) {
      dialogFragment = CustomDefaultPreference.CustomDefaultPreferenceDialogFragmentCompat.newInstance(preference.getKey());
    }

    if (dialogFragment != null) {
      dialogFragment.setTargetFragment(this, 0);
      dialogFragment.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
    } else {
      super.onDisplayPreferenceDialog(preference);
    }
  }


}
