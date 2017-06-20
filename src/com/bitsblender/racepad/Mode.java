package com.bitsblender.racepad;


import java.io.File;
import java.util.List;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

@SuppressLint({ "DefaultLocale", "HandlerLeak" })
public class Mode extends Activity implements OnClickListener {
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	public static final int WIFI_MODE = 0;
	public static final int BLUETOOTH_MODE = 1;
	static int mode;
	int racePadNetworkID;
	WifiManager wifiManager;
	ConnectivityManager connectivityManager;
	BroadcastReceiver wifiScanner;
	BroadcastReceiver networkScanner;
	ProgressDialog netStatus;
	static int CONNECTION_TIMEOUT = 90;
	public static final int CONNECTION_TIMED_OUT = 0;
	public static final int CONNECTION_SUCESS = 1;
	static Handler connectionHandler;

	public static void setMode(int m) {
		if (m >= WIFI_MODE && m <= BLUETOOTH_MODE)
			mode = m;
		else
			Log.e("Unkown Mode", "Unknown Mode called");
	}

	public static int getMode() {
		return mode;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mode_chooser);
		initBtns();
		createProfilePath();
		createPrefrences();
		AdView bottomAdd = (AdView) findViewById(R.id.modeBannerAd);
		AdRequest adRequest = new AdRequest.Builder().build();
		bottomAdd.loadAd(adRequest);
		connectionHandler = new Handler() {

			@SuppressWarnings("deprecation")
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);

				switch (msg.what) {
				case CONNECTION_TIMED_OUT:
					Toast.makeText(getApplicationContext(),
							"Connection TIMED OUT trying again to connect to Available HotSpot", Toast.LENGTH_LONG)
							.show();
					NetworkInfo netInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					if (!netInfo.isConnected()) {
						CONNECTION_TIMEOUT += 10;
						List<WifiConfiguration> currentConifgs = wifiManager.getConfiguredNetworks();
						if (currentConifgs == null)
							Toast.makeText(getApplicationContext(), "Wifi is Turned Off \n please Try Again",
									Toast.LENGTH_LONG).show();
						else {
							int availableID = racePadNetworkID;
							List<ScanResult> recentHotSpots = wifiManager.getScanResults();
							if (recentHotSpots.size() == 0)
								Toast.makeText(getApplicationContext(),
										"No HotSpots Available.\n Please Enable RacePad Network in RacePadServer",
										Toast.LENGTH_LONG).show();
							else {
								for (WifiConfiguration config : currentConifgs) {
									for (ScanResult results : recentHotSpots) {
										if (config.SSID.substring(1, config.SSID.length() - 1).equals(results.SSID)
												&& config.networkId != racePadNetworkID) {
											availableID = config.networkId;
											// Log.d("Selected HotSpot",
											// results.SSID);
										}
									}
								}

								netStatus.setTitle("Connecting to available HotSpot");
								netStatus.show();
								connectToRacePadHost(availableID, false);
							}
						}

					}
					break;

				case CONNECTION_SUCESS:
					Toast.makeText(getApplicationContext(), "Sucessfully Connected to network", Toast.LENGTH_SHORT)
							.show();
					WifiInfo info = wifiManager.getConnectionInfo();
					int ip = info.getIpAddress();
					String ipString = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff),
							(ip >> 24 & 0xff));
					Intent intent = new Intent("com.bitsblender.racepad.WIFIACTIVITY");
					intent.putExtra("Android IP", ipString);
					//System.out.println(ipString);
					startActivity(intent);
					break;
				}

			}

		};
		wifiScanner = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				netStatus.dismiss();
				String action = intent.getAction();
				if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
					List<ScanResult> scanResults = wifiManager.getScanResults();
					boolean isRacePadHotSpotAvailable = false;
					for (ScanResult result : scanResults) {
						Log.d("Available Connections", result.SSID);
						if (result.SSID.equals("RacePadHotSpot")) {
							isRacePadHotSpotAvailable = true;
							netStatus.setTitle("Connecting to Race Pad Network");
							netStatus.show();
							connectToRacePadHost(racePadNetworkID, isRacePadHotSpotAvailable);
							break;
						}
					}
					if (!isRacePadHotSpotAvailable) {
						netStatus.setTitle("Connecting to Available HotSpot");
						netStatus.show();
						connectToRacePadHost(racePadNetworkID, isRacePadHotSpotAvailable);
					}
					Log.d("NULL", " ");
					unregisterReceiver(wifiScanner);
				}
			}
		};
		networkScanner = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

				}

			}

		};
	}

	protected void connectToRacePadHost(final int netID, final boolean disallowOtherNetworksToConnect) {
		new Thread(new Runnable() {

			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				wifiManager.enableNetwork(netID, disallowOtherNetworksToConnect);
				int timeOut = 0;
				while (true) {
					if (timeOut > CONNECTION_TIMEOUT) {
						netStatus.dismiss();
						connectionHandler.obtainMessage(CONNECTION_TIMED_OUT).sendToTarget();
						break;
					}
					NetworkInfo netInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					if (netInfo.isConnected()) {
						netStatus.dismiss();
						connectionHandler.obtainMessage(CONNECTION_SUCESS).sendToTarget();
						break;
					}
					try {
						Thread.sleep(1000);
						timeOut++;
						// Log.d("Time Out", Integer.toString(timeOut));
					} catch (InterruptedException e) {

						e.printStackTrace();
					}
				}

			}

		}).start();

	}

	protected void messageToaster(String msg) {
		Toast.makeText(Mode.this, msg, Toast.LENGTH_LONG).show();
	}

	

	private void initBtns() {
		Button wifiBtn = (Button) findViewById(R.id.wifiBtn);
		wifiBtn.setOnClickListener(this);
		Button bluetoothBtn = (Button) findViewById(R.id.bluetoothBtn);
		bluetoothBtn.setOnClickListener(this);
		Button helpBtn = (Button) findViewById(R.id.helpBtn);
		helpBtn.setOnClickListener(this);
		Button aboutBtn = (Button) findViewById(R.id.aboutBtn);
		aboutBtn.setOnClickListener(this);

	}

	@SuppressLint("SdCardPath")
	private void createPrefrences() {
		File prefrencesFile = new File("/data/data/com.bitsblender.racepad/shared_prefs/preferences.xml");
		if (!prefrencesFile.exists()) {
			// Toast.makeText(getApplicationContext(), "created Default
			// Preferences", Toast.LENGTH_SHORT).show();
			SharedPreferences preferences = getSharedPreferences("preferences", Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("Left-Right Motion", true);
			editor.putBoolean("Up-Down Motion", false);
			editor.putInt("Accerlometer Sensitivity", 2);
			editor.putInt("Mouse Pointer Speed", 3);
			editor.commit();
		}

	}

	private void createProfilePath() {
		File profileFolder = new File(Environment.getExternalStorageDirectory() + "/RacePad");
		if (!profileFolder.exists()) {

			profileFolder.mkdir();
		}
	}

	private boolean RateNowActivity(Intent aIntent) {
		try {
			startActivity(aIntent);
			return true;
		} catch (ActivityNotFoundException e) {
			return false;
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onClick(View v) {
		Intent intent;
		switch (v.getId()) {
		case R.id.wifiBtn:
			setMode(WIFI_MODE);

			wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (!networkInfo.isConnected()) {

				if (!wifiManager.isWifiEnabled()) {
					wifiManager.setWifiEnabled(true);
					Toast.makeText(this, "Turning ON Wifi", Toast.LENGTH_SHORT).show();
				}

				addRacePadNetwork();

				registerReceiver(wifiScanner, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
				netStatus = ProgressDialog.show(Mode.this, "Searching for Networks			", "Please Wait.....",
						false);
				wifiManager.startScan();
			} else {
				WifiInfo info = wifiManager.getConnectionInfo();
				int ip = info.getIpAddress();
				String ipString = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff),
						(ip >> 24 & 0xff));
				intent = new Intent("com.bitsblender.racepad.WIFIACTIVITY");
				intent.putExtra("Android IP", ipString);
				startActivity(intent);
			}
			

			break;
		case R.id.bluetoothBtn:
			setMode(BLUETOOTH_MODE);
			intent = new Intent("com.bitsblender.racepad.STARTINGACTIVITY");
			intent.putExtra("ModeSelected", getMode());
			startActivity(intent);
			break;
		case R.id.helpBtn:
			intent = new Intent("com.bitsblender.racepad.HELP");
			startActivity(intent);
			break;
		case R.id.aboutBtn:
			LayoutInflater layoutInflater = this.getLayoutInflater();
			AlertDialog.Builder about = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			View view2 = layoutInflater.inflate(R.layout.about_layout, null);
			about.setView(view2).setPositiveButton("Rate Now", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse("market://details?id=com.bitsblender.racepad"));
					if (!RateNowActivity(intent)) {
						intent.setData(
								Uri.parse("https://play.google.com/store/apps/details?id=com.bitsbleder.racepad"));

						if (!RateNowActivity(intent)) {
							Toast.makeText(getApplicationContext(), "Could not open Android Market", Toast.LENGTH_SHORT)
									.show();
						}
					}
				}
			}).setNegativeButton("Share Now", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent sharingIntent = new Intent(Intent.ACTION_SEND);
					sharingIntent.setType("text/plain");
					String shareBody = "https://play.google.com/store/apps/details?id=com.bitsbleder.racepad http://bitsblender.wix.com/racepad";
					sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Install RacePad Now");
					sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
					startActivity(Intent.createChooser(sharingIntent, "Share via"));
				}
			});

			AlertDialog dialog2 = about.create();
			dialog2.show();
			break;
		}

	}

	private void addRacePadNetwork() {

		List<WifiConfiguration> networksList = wifiManager.getConfiguredNetworks();
		boolean isRacePadNetworkConfigured = false;

		for (WifiConfiguration currentNetwork : networksList) {
			if (currentNetwork.SSID.equals("RacePadHotSpot")) {
				isRacePadNetworkConfigured = true;
				break;
			}
		}

		if (!isRacePadNetworkConfigured) {
			WifiConfiguration wifiConfig = new WifiConfiguration();
			wifiConfig.SSID = "\"" + "RacePadHotSpot" + "\"";
			wifiConfig.preSharedKey = "\"" + "h1a2r3d4" + "\"";
			wifiConfig.status = WifiConfiguration.Status.ENABLED;
			wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			racePadNetworkID = wifiManager.addNetwork(wifiConfig);
			wifiManager.saveConfiguration();
			//Log.d("Network Test", "RacePad Network Added");
		}
	}
}
