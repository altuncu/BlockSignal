package com.example.altuncu.blocksignal;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class BasicIntroFragment extends Fragment {

  private static final String ARG_DRAWABLE = "drawable";
  private static final String ARG_TEXT     = "text";
  private static final String ARG_SUBTEXT  = "subtext";

  private int drawable;
  private int text;
  private int subtext;

  public static BasicIntroFragment newInstance(int drawable, int text, int subtext) {
    BasicIntroFragment fragment = new BasicIntroFragment();
    Bundle args = new Bundle();
    args.putInt(ARG_DRAWABLE, drawable);
    args.putInt(ARG_TEXT, text);
    args.putInt(ARG_SUBTEXT, subtext);
    fragment.setArguments(args);
    return fragment;
  }

  public BasicIntroFragment() {}

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      drawable = getArguments().getInt(ARG_DRAWABLE);
      text     = getArguments().getInt(ARG_TEXT    );
      subtext  = getArguments().getInt(ARG_SUBTEXT );
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View v = inflater.inflate(com.example.altuncu.blocksignal.R.layout.color_fragment, container, false);

    ((ImageView)v.findViewById(com.example.altuncu.blocksignal.R.id.watermark)).setImageResource(drawable);
    ((TextView)v.findViewById(com.example.altuncu.blocksignal.R.id.blurb)).setText(text);
    ((TextView)v.findViewById(com.example.altuncu.blocksignal.R.id.subblurb)).setText(subtext);

    return v;
  }
}
