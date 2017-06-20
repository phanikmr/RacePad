package com.bitsblender.racepad;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class Help extends Activity {
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help_layout);
		WebView helpView = (WebView) findViewById(R.id.helpView);
		helpView.getSettings().setEnableSmoothTransition(true);
		helpView.getSettings().setJavaScriptEnabled(true);
		helpView.getSettings().setLoadWithOverviewMode(true);
		helpView.getSettings().setUseWideViewPort(true);
		helpView.getSettings().setBuiltInZoomControls(true);
		helpView.getSettings().setSupportZoom(true);
		helpView.loadUrl("file:///android_asset/help.html");
	}

}
