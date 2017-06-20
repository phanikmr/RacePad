package com.bitsblender.racepad;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

@SuppressLint("HandlerLeak")
public class KeySendActivity extends Activity implements OnTouchListener, OnCheckedChangeListener, SensorEventListener,
		OnItemSelectedListener, OnSeekBarChangeListener {

	BluetoothSocket socket;
	Socket wifiSocket;
	// OutputStream outputStream;
	XMLParser xmlParser;
	ArrayAdapter<String> profilesList;
	ArrayAdapter<File> profiles;
	Hashtable<String, Integer> buttonIds;
	Hashtable<String, String> buttonMappings;
	Hashtable<String, String> buttonMappingsChecked, buttonMappingsNotChecked;
	Spinner profileSpinner;
	ImageButton btn_w;
	Button btn_alt_f4, btn_win_d, btn_win_t, btn_win, btn_menu, btn_esc, btn_tab, btn_enter, btn_l, btn_r, btn_u, btn_d;
	ToggleButton arrows_switch, accerlometer_switch;
	Bitmap bitmap;
	Canvas canvas;
	Paint paint;
	int mouseSensitivity;
	SensorManager sensorManager;
	Sensor accerlometer;
	OutputStream socketOutputStream;
	InputStream socketInputStream;
	BroadcastReceiver broadcastReceiver;
	SeekBar accerlometerSensitivity, mousePointerSpeed;
	Switch leftRightMotion, upDownMotion;
	SharedPreferences preferences;
	byte[] buffer;
	String gamePath;
	boolean updateRequest = false;
	private static final int LEFT = 1;
	private static final int NORMAL = 0;
	private static final int RIGHT = -1;
	private static final int UP = 1;
	private static final int DOWN = -1;
	private static Float X, Y;
	private static int lockedX = 0, lockedY = 0;
	int modeSelected;
	int previousX = NORMAL;
	int previousY = NORMAL;
	int correctedLeftValue = -5;
	int correctedRightValue = 5;
	int correctedUpValue = -2;
	int correctedDownValue = 8;
	int LockedPixels = 18;
	int flushTime;
	static Handler handler;
	ProgressDialog progressDialog;
	ReceiveingThread receiveingThread;
	static final int PROFILES_UPDATED = 0;
	static final int PROFILES_UPDATE_FAILED = 1;
	static final int PROFILES_UPDATE_STARTED = 2;
	static final int WIFI_DISCONNECTED = 3;
	InterstitialAd interstitialAd;

	@Override
	protected void onDestroy() {
		if (interstitialAd.isLoaded())
			interstitialAd.show();
		super.onDestroy();
		sensorManager.unregisterListener(this);
		try {
			if (modeSelected == com.bitsblender.racepad.Mode.BLUETOOTH_MODE) {
				socket.close();
				unregisterReceiver(broadcastReceiver);
			} else {
				wifiSocket.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void onPause() {
		if (interstitialAd.isLoaded())
			interstitialAd.show();
		super.onPause();
		sensorManager.unregisterListener(this);

	}

	@Override
	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this, accerlometer, SensorManager.SENSOR_DELAY_GAME);
		if (interstitialAd.isLoaded())
			interstitialAd.show();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent receivedIntent = getIntent();
		modeSelected = receivedIntent.getIntExtra("Selected Mode", com.bitsblender.racepad.Mode.getMode());
		interstitialAd = new InterstitialAd(this);
		AdRequest adRequest = new AdRequest.Builder().build();
		interstitialAd.setAdUnitId("ca-app-pub-1031024450684911/2676619181");
		interstitialAd.loadAd(adRequest);
		interstitialAd.setAdListener(new AdListener() {
			@Override
			public void onAdLoaded() {
				// TODO Auto-generated method stub
				interstitialAd.show();
			}
		});

		if (modeSelected == com.bitsblender.racepad.Mode.BLUETOOTH_MODE)
			monitorBluetoothService();
		else {
			WiFiMonitor wiFiMonitor = new WiFiMonitor();
			wiFiMonitor.start();
		}
		init();
		initButtonIds();
		initButtonListeners();
		initButtonMappings();
		buttonMappings = new Hashtable<>();
		buttonMappings.putAll(buttonMappingsNotChecked);
		setButtonText();
		initProgressDialog();
		if (modeSelected == com.bitsblender.racepad.Mode.BLUETOOTH_MODE) {
			socketOutputStream = StartingActivity.socketOutStream;
			socketInputStream = StartingActivity.socketInStream;
			flushTime = 100;
		} else {
			socketOutputStream = WiFiActivity.socketOutStream;
			socketInputStream = WiFiActivity.socketInStream;
			flushTime = 200;
		}
		handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch (msg.what) {
				case PROFILES_UPDATED:
					progressDialog.dismiss();
					gamePath = "";
					getProfilesList();
					profileSpinner.setAdapter(profilesList);
					pickProfile();
					initButtonMappings();
					setButtonText();
					receiveingThread = new ReceiveingThread();
					receiveingThread.start();
					break;

				case PROFILES_UPDATE_FAILED:
					progressDialog.dismiss();
					Toast.makeText(getApplicationContext(), "Profiles Update failed \n please connect again",
							Toast.LENGTH_LONG).show();
					finish();
					break;
				case PROFILES_UPDATE_STARTED:
					progressDialog.show();
					gamePath = "";
					File dir = new File(Environment.getExternalStorageDirectory() + "/RacePad");
					if (dir.exists()) {
						if (dir.isDirectory()) {
							String[] children = dir.list();
							for (int i = 0; i < children.length; i++) {
								new File(dir, children[i]).delete();
							}
						}
					}
					break;
				case WIFI_DISCONNECTED:
					Toast.makeText(getApplicationContext(), "Network gone \n please connect to Network and try again",
							Toast.LENGTH_LONG).show();
					finish();
					break;
				}
			}

		};
		receiveingThread = new ReceiveingThread();
		receiveingThread.start();
		if (interstitialAd.isLoaded()) {
			interstitialAd.show();
		}

	}

	@SuppressWarnings("deprecation")
	private void initProgressDialog() {
		progressDialog = new ProgressDialog(KeySendActivity.this, ProgressDialog.THEME_DEVICE_DEFAULT_DARK);
		progressDialog.setTitle("Please Wait");
		progressDialog.setMessage("Updating...\nPlease be patience while updating");
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setCancelable(false);
	}

	private void monitorBluetoothService() {
		broadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {

				String action = intent.getAction();
				if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
					finish();
					Toast.makeText(getApplicationContext(), "Bluetooth Switched OFF", Toast.LENGTH_SHORT).show();
				}
			}

		};
		IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(broadcastReceiver, intentFilter);
	}

	private void initButtonListeners() {
		for (Enumeration<String> keys = buttonIds.keys(); keys.hasMoreElements();) {
			String key = keys.nextElement().toString();
			Button btn = (Button) findViewById(buttonIds.get(key));
			btn.setOnTouchListener(this);
		}
	}

	private void initButtonMappings() {
		buttonMappingsNotChecked = new Hashtable<>();
		buttonMappingsChecked = new Hashtable<>();
		for (Enumeration<String> keys = buttonIds.keys(); keys.hasMoreElements();) {
			String key = keys.nextElement().toString();
			String name = xmlParser.getAttributeFromTagName(key);
			buttonMappingsNotChecked.put(key, name);
		}
		buttonMappingsChecked.putAll(buttonMappingsNotChecked);
		buttonMappingsChecked.put("btn_l", "37");
		buttonMappingsChecked.put("btn_u", "38");
		buttonMappingsChecked.put("btn_r", "39");
		buttonMappingsChecked.put("btn_d", "40");
		buttonMappings = new Hashtable<>();
		buttonMappings.putAll(buttonMappingsNotChecked);
		arrows_switch.setChecked(false);
		gamePath = xmlParser.getAttributeFromTagName("game_path");
	}

	@SuppressWarnings("deprecation")
	private void openGameDialog() {
		AlertDialog.Builder warning = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
		warning.setTitle("Launch Game...");
		warning.setCancelable(false);
		warning.setMessage("Would you like to open the Game now ?")
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						sendToServer(gamePath + "_LNK");
					}
				}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		AlertDialog dialog = warning.create();
		dialog.show();
	}

	private void initButtonIds() {
		buttonIds = new Hashtable<String, Integer>();
		buttonIds.put("btn_1", R.id.btn_1);
		buttonIds.put("btn_2", R.id.btn_2);
		buttonIds.put("btn_3", R.id.btn_3);
		buttonIds.put("btn_4", R.id.btn_4);
		buttonIds.put("btn_5", R.id.btn_5);
		buttonIds.put("btn_6", R.id.btn_6);
		buttonIds.put("btn_7", R.id.btn_7);
		buttonIds.put("btn_8", R.id.btn_8);
		buttonIds.put("btn_x", R.id.btn_x);
		buttonIds.put("btn_l", R.id.btn_l);
		buttonIds.put("btn_r", R.id.btn_r);
		buttonIds.put("btn_u", R.id.btn_u);
		buttonIds.put("btn_d", R.id.btn_d);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_actvity2, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@SuppressWarnings("deprecation")
	private void init() {
		setContentView(R.layout.key_send_activity);
		getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		getActionBar().setCustomView(R.layout.cust_title_bar);
		getProfilesList();
		File xmlFile = (File) getIntent().getExtras().get("XMLFile");
		xmlParser = new XMLParser(xmlFile);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		if (modeSelected == com.bitsblender.racepad.Mode.BLUETOOTH_MODE)
			socket = StartingActivity.socket;
		else
			wifiSocket = WiFiActivity.socket;
		/*
		 * try { outputStream = socket.getOutputStream(); } catch (IOException
		 * e) { e.printStackTrace(); }
		 */
		preferences = getSharedPreferences("preferences", Context.MODE_PRIVATE);
		mouseSensitivity = preferences.getInt("Mouse Pointer Speed", 10);
		btn_w = (ImageButton) findViewById(R.id.btn_w);
		btn_w.setOnTouchListener(this);
		btn_alt_f4 = (Button) findViewById(R.id.btn_alt_f4);
		btn_alt_f4.setOnTouchListener(this);
		btn_win_d = (Button) findViewById(R.id.btn_win_d);
		btn_win_d.setOnTouchListener(this);
		btn_win_t = (Button) findViewById(R.id.btn_win_t);
		btn_win_t.setOnTouchListener(this);
		btn_win = (Button) findViewById(R.id.btn_win);
		btn_win.setOnTouchListener(this);
		btn_menu = (Button) findViewById(R.id.btn_menu);
		btn_menu.setOnTouchListener(this);
		btn_esc = (Button) findViewById(R.id.btn_esc);
		btn_esc.setOnTouchListener(this);
		btn_tab = (Button) findViewById(R.id.btn_tab);
		btn_tab.setOnTouchListener(this);
		btn_enter = (Button) findViewById(R.id.btn_enter);
		btn_enter.setOnTouchListener(this);
		btn_l = (Button) findViewById(R.id.btn_l);
		btn_r = (Button) findViewById(R.id.btn_r);
		btn_u = (Button) findViewById(R.id.btn_u);
		btn_d = (Button) findViewById(R.id.btn_d);
		Button btn_mute = (Button) findViewById(R.id.btn_mute);
		btn_mute.setOnTouchListener(this);
		Button btn_vol_up = (Button) findViewById(R.id.btn_vol_up);
		btn_vol_up.setOnTouchListener(this);
		Button btn_vol_down = (Button) findViewById(R.id.btn_vol_down);
		btn_vol_down.setOnTouchListener(this);
		arrows_switch = (ToggleButton) findViewById(R.id.togg_arrow);
		arrows_switch.setOnCheckedChangeListener(this);
		profileSpinner = (Spinner) findViewById(R.id.profiles_spinner);
		profileSpinner.setAdapter(profilesList);
		accerlometer_switch = (ToggleButton) findViewById(R.id.accerlometer_switch);
		accerlometer_switch.setChecked(false);
		accerlometer_switch.setOnCheckedChangeListener(this);
		profileSpinner.setSelection(profiles.getPosition(xmlFile));
		profileSpinner.setOnItemSelectedListener(this);
		Display currentDisplay = getWindowManager().getDefaultDisplay();
		if ((currentDisplay.getHeight() > 900) && (currentDisplay.getWidth() > 1300))
			LockedPixels = 23;

		bitmap = Bitmap.createBitmap((int) currentDisplay.getWidth(), (int) currentDisplay.getHeight(),
				Bitmap.Config.ARGB_8888);
		canvas = new Canvas(bitmap);
		paint = new Paint();
		paint.setColor(Color.rgb(0, 160, 230));
		btn_w.setImageBitmap(bitmap);
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		accerlometer = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
		sensorManager.registerListener(this, accerlometer, SensorManager.SENSOR_DELAY_GAME);
	}

	private void setButtonText() {
		Hashtable<String, String> buttonMappings = new Hashtable<>();
		buttonMappings.putAll(buttonMappingsNotChecked);
		if (arrows_switch.isChecked()) {
			buttonMappings = new Hashtable<>();
			buttonMappings.putAll(buttonMappingsChecked);
		}
		for (Enumeration<String> keys = buttonIds.keys(); keys.hasMoreElements();) {
			String key = keys.nextElement().toString();
			String name = buttonMappings.get(key);
			Button btn = (Button) findViewById(buttonIds.get(key));
			try {
				int code = Integer.parseInt(name);
				btn.setText(KeyCodes.getKeyNormalName(code));
			} catch (Exception err) {
				btn.setText("");
			}

		}

	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int btnId = v.getId();
		// Mouse simulation
		if (btnId == R.id.btn_w) {
			int[] locations = new int[2];
			v.getLocationOnScreen(locations);
			int lMargin, rMargin, tMargin, bMargin;
			lMargin = locations[0];
			rMargin = v.getRight();
			tMargin = locations[1];
			bMargin = (locations[1] + v.getBottom());
			int action = event.getAction() & MotionEvent.ACTION_MASK;
			if (action == MotionEvent.ACTION_DOWN) {
				sendToServer("STR_MSC");
				X = event.getRawX();
				Y = event.getRawY();
				canvas.drawColor(0, Mode.CLEAR);
				canvas.drawCircle(X, Y, 17, paint);
				btn_w.invalidate();

			} else if (action == MotionEvent.ACTION_UP) {
				canvas.drawColor(0, Mode.CLEAR);
				btn_w.setBackgroundResource(R.drawable.cust_streer_wheel);
				sendToServer("STP_MSC");
			} else if (action == MotionEvent.ACTION_MOVE) {
				btn_w.setBackgroundResource(R.drawable.steer_wheel_clicked);
				Float x, y;
				x = Float.valueOf(event.getRawX());
				y = Float.valueOf(event.getRawY());
				if (x >= lMargin && x <= rMargin && y >= tMargin && y <= bMargin) {
					canvas.drawColor(0, Mode.CLEAR);
					canvas.drawCircle(x, y, 17, paint);
					btn_w.invalidate();
					x = X - x;
					y = Y - y;
					if (x >= -LockedPixels && x <= LockedPixels)
						lockedX = 0;
					else if (x > LockedPixels)
						lockedX = 1;
					else if (x < -LockedPixels)
						lockedX = -1;
					if (y >= -LockedPixels && y <= LockedPixels)
						lockedY = 0;
					else if (y > LockedPixels)
						lockedY = 1;
					else if (y < -LockedPixels)
						lockedY = -1;
				}

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (lockedX == 0 && lockedY > 0) {
					// Log.d("Moving", "Top");
					sendToServer("TP_" + Integer.toString(mouseSensitivity) + "_MSC");
				} else if (lockedX == 0 && lockedY < 0) {
					// Log.d("Moving", "Bottom");
					sendToServer("BM_" + Integer.toString(mouseSensitivity) + "_MSC");
				} else if (lockedX > 0 && lockedY == 0) {
					// Log.d("Moving", "Left");
					sendToServer("LT_" + Integer.toString(mouseSensitivity) + "_MSC");
				} else if (lockedX < 0 && lockedY == 0) {
					// Log.d("Moving", "Right");
					sendToServer("RT_" + Integer.toString(mouseSensitivity) + "_MSC");
				} else if (lockedX > 0 && lockedY > 0) {
					// Log.d("Moving", "TopLeft");
					sendToServer("TL_" + Integer.toString(mouseSensitivity) + "_MSC");
				} else if (lockedX < 0 && lockedY > 0) {
					// Log.d("Moving", "TopRight");
					sendToServer("TR_" + Integer.toString(mouseSensitivity) + "_MSC");
				} else if (lockedX > 0 && lockedY < 0) {
					// Log.d("Moving", "BottomLeft");
					sendToServer("BL_" + Integer.toString(mouseSensitivity) + "_MSC");
				} else if (lockedX < 0 && lockedY < 0) {
					// Log.d("Moving", "BottomRight");
					sendToServer("BR_" + Integer.toString(mouseSensitivity) + "_MSC");
				}

			}
		} else {
			if (buttonIds.containsValue(btnId)) {
				String key = null;
				for (Enumeration<String> keys = buttonIds.keys(); keys.hasMoreElements();) {
					key = keys.nextElement().toString();
					if (btnId == buttonIds.get(key))
						break;
				}
				int action = event.getAction();
				switch (action) {
				case MotionEvent.ACTION_DOWN:
					// Log.d("Normal key", buttonMappings.get(key) + key
					// + " is Down ");
					sendToServer(buttonMappings.get(key) + "_DN_NOM");
					break;
				case MotionEvent.ACTION_UP:
					// Log.d("Normal key", buttonMappings.get(key) + key
					// + " is Up ");
					sendToServer(buttonMappings.get(key) + "_UP_NOM");
					break;
				}
			} else {
				int action = event.getAction();
				if (action == MotionEvent.ACTION_UP) {
					switch (btnId) {
					case R.id.btn_mute:
						// Log.d("special", "btn_mute_clicked");
						sendToServer("0xAD_1_SPL");
						break;
					case R.id.btn_vol_up:
						// Log.d("Special", "Vol Up UP");
						sendToServer("0xAF_UP_NOM");
						break;
					case R.id.btn_vol_down:
						// Log.d("Special", "Vol Down UP");
						sendToServer("0xAE_UP_NOM");
						break;
					case R.id.btn_alt_f4:
						// Log.d("special", "btn_alt_f4 clicked");
						AlertDialog.Builder warning = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
						warning.setTitle("Warning !!!...");
						warning.setCancelable(false);
						warning.setMessage("Would you like to close the Game now ?")
								.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog, int which) {
										// TODO Auto-generated method stub
										sendToServer("0x120x73_2_SPL");
									}
								}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
									}
								});
						AlertDialog dialog = warning.create();
						dialog.show();
						break;
					case R.id.btn_win_d:
						// Log.d("special", "btn_win_d clicked");
						sendToServer("0x5B0x44_2_SPL");
						break;
					case R.id.btn_win_t:
						// Log.d("special", "btn_win_t clicked");
						sendToServer("0x5B0x54_2_SPL");
						break;
					case R.id.btn_win:
						// Log.d("special", "btn_win clicked");
						sendToServer("0x5B_1_SPL");
						break;
					case R.id.btn_menu:
						// Log.d("special", "btn_menu clicked");
						sendToServer("0x5D_1_SPL");
						break;
					case R.id.btn_esc:
						// Log.d("special", "btn_esc clicked");
						sendToServer("0x1B_1_SPL");
						break;
					case R.id.btn_tab:
						// Log.d("special", "btn_tab clicked");
						sendToServer("0x09_1_SPL");
						break;
					case R.id.btn_enter:
						// Log.d("special", "btn_enter clicked");
						sendToServer("0x0D_1_SPL");
						break;
					}
				} else if (action == MotionEvent.ACTION_DOWN) {
					if (btnId == R.id.btn_vol_up) {
						// Log.d("special", "Vol up Down");
						sendToServer("0xAF_DN_NOM");
					} else if (btnId == R.id.btn_vol_down) {
						// Log.d("special", "Vol down Down");
						sendToServer("0xAE_DN_NOM");
					}
				}
			}
		}
		return false;
	}

	protected void getProfilesList() {
		File profileFolder = new File(Environment.getExternalStorageDirectory() + "/RacePad");
		profilesList = new ArrayAdapter<String>(this, R.layout.spinner_view);
		profiles = new ArrayAdapter<File>(this, R.layout.spinner_view);
		FilenameFilter xmlFilter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				if (filename.endsWith(".xml"))
					return true;
				return false;
			}
		};
		File[] proFiles = profileFolder.listFiles(xmlFilter);
		for (int i = 0; i < proFiles.length; i++) {
			if (proFiles[i].isFile())
				profiles.add(proFiles[i]);
			profilesList.add(proFiles[i].getName().replaceAll(".xml", ""));
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
		case R.id.togg_arrow:
			buttonMappings = new Hashtable<>();
			if (isChecked) {
				buttonMappings.putAll(buttonMappingsChecked);
			} else {
				initButtonMappings();
			}
			setButtonText();
			Log.d("toggle", "Arrow toggled");
			break;
		case R.id.accerlometer_switch:
			if (isChecked) {
				Toast.makeText(getApplicationContext(), "Arrow keys are Disabled", Toast.LENGTH_SHORT).show();
				arrows_switch.setEnabled(false);
				arrows_switch.setTextColor(Color.parseColor("#303030"));
				if (preferences.getBoolean("Left-Right Motion", true)) {
					btn_l.setEnabled(false);
					btn_r.setEnabled(false);
				}
				if (preferences.getBoolean("Up-Down Motion", false)) {
					btn_u.setEnabled(false);
					btn_d.setEnabled(false);
				}
			} else {
				arrows_switch.setEnabled(true);
				arrows_switch.setTextColor(Color.parseColor("#FFFFFF"));
				btn_d.setEnabled(true);
				btn_u.setEnabled(true);
				btn_l.setEnabled(true);
				btn_r.setEnabled(true);
			}
			break;
		case R.id.leftRightMotion:
			SharedPreferences.Editor editor = preferences.edit();
			editor.putBoolean("Left-Right Motion", isChecked);
			editor.commit();
			if (accerlometer_switch.isChecked() && isChecked) {
				btn_l.setEnabled(false);
				btn_r.setEnabled(false);
			} else if (accerlometer_switch.isChecked() && !isChecked) {
				btn_l.setEnabled(true);
				btn_r.setEnabled(true);
			}
			break;
		case R.id.upDownMotion:
			SharedPreferences.Editor editor1 = preferences.edit();
			editor1.putBoolean("Up-Down Motion", isChecked);
			editor1.commit();
			if (accerlometer_switch.isChecked() && isChecked) {
				btn_u.setEnabled(false);
				btn_d.setEnabled(false);
			} else if (accerlometer_switch.isChecked() && !isChecked) {
				btn_u.setEnabled(true);
				btn_d.setEnabled(true);
			}
			break;
		}

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (accerlometer_switch.isChecked()) {
			float x, y;
			x = event.values[0];
			y = event.values[1];
			int currentSensitivity = preferences.getInt("Accerlometer Sensitivity", 2);
			if (preferences.getBoolean("Left-Right Motion", true)) {
				if (y <= correctedLeftValue + currentSensitivity && previousY != LEFT) {
					btn_w.setBackgroundResource(R.drawable.steer_wheel_left);
					// sendToServer("Left Down");
					sendToServer(buttonMappings.get("btn_l") + "_DN_NOM");
					previousY = LEFT;
					// Log.d("accerolmeter debug", Integer.toString((int) x)
					// + Integer.toString((int) y));
				} else if ((y > correctedLeftValue + currentSensitivity && y < correctedRightValue - currentSensitivity)
						&& (previousY != NORMAL)) {
					btn_w.setBackgroundResource(R.drawable.cust_streer_wheel);
					// sendToServer("Left or Right up");
					sendToServer(buttonMappings.get("btn_l") + "_UP_NOM");
					waitTOFlush();
					sendToServer(buttonMappings.get("btn_r") + "_UP_NOM");
					previousY = NORMAL;
					// Log.d("accerolmeter debug", Integer.toString((int) x)
					// + Integer.toString((int) y));
				} else if (y >= correctedRightValue - currentSensitivity && previousY != RIGHT) {
					btn_w.setBackgroundResource(R.drawable.steer_wheel_right);
					// sendToServer("Right Down");
					sendToServer(buttonMappings.get("btn_r") + "_DN_NOM");
					previousY = RIGHT;
					// Log.d("accerolmeter debug", Integer.toString((int) x)
					// + Integer.toString((int) y));
				}
			}
			if (preferences.getBoolean("Up-Down Motion", false)) {
				if (x > correctedUpValue + currentSensitivity && x < correctedDownValue - currentSensitivity
						&& (previousX != NORMAL)) {
					// sendToServer("Up or Down UP");
					sendToServer(buttonMappings.get("btn_u") + "_UP_NOM");
					waitTOFlush();
					sendToServer(buttonMappings.get("btn_d") + "_UP_NOM");
					previousX = NORMAL;
					// Log.d("accerolmeter debug", Integer.toString((int) x)
					// + Integer.toString((int) y));
				} else if (x <= correctedUpValue + currentSensitivity && previousX != UP) {
					// sendToServer("Up is Down");
					sendToServer(buttonMappings.get("btn_u") + "_DN_NOM");
					previousX = UP;
					// Log.d("accerolmeter debug", Integer.toString((int) x)
					// + Integer.toString((int) y));
				} else if (x >= correctedDownValue - currentSensitivity && (previousX != DOWN)) {
					// sendToServer("Down is Down");
					sendToServer(buttonMappings.get("btn_d") + "_DN_NOM");
					previousX = DOWN;
					// Log.d("accerolmeter debug", Integer.toString((int) x)
					// + Integer.toString((int) y));
				}
			}
		}
	}

	private void waitTOFlush() {
		try {
			Thread.sleep(flushTime);
		} catch (Exception err) {
			err.printStackTrace();
		}

	}

	private void sendToServer(String msg) {

		Thread t = new Thread(new SendingThread(msg));
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*
		 * try { buffer = new byte[msg.length()]; socketWriteStream.flush();
		 * buffer = msg.getBytes(); socketWriteStream.write(buffer);
		 * socketWriteStream.flush(); } catch (IOException e) {
		 * Toast.makeText(getApplicationContext(),
		 * "RacePad Server Down Send Failed", Toast.LENGTH_LONG).show();
		 * finish(); }
		 */
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

		xmlParser = new XMLParser(profiles.getItem(arg2));
		initButtonMappings();
		setButtonText();
		if (gamePath.length() != 0) {
			openGameDialog();
			// ddd.println("opened here");

		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings("deprecation")
	@Override
	public void onBackPressed() {
		AlertDialog.Builder warning = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
		warning.setTitle("Warning..!!!");
		warning.setCancelable(false);
		warning.setMessage("Connection will be Lost").setPositiveButton("Ok", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub

				if (interstitialAd.isLoaded())
					interstitialAd.show();

				finish();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();

				if (interstitialAd.isLoaded())
					interstitialAd.show();

			}
		});
		AlertDialog dialog = warning.create();
		dialog.show();
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		AlertDialog.Builder options = new AlertDialog.Builder(this);
		LayoutInflater layoutInflater = this.getLayoutInflater();
		switch (id) {
		case R.id.accerlometerOptions:
			View view = layoutInflater.inflate(R.layout.accerlometer_layout, null);
			options.setView(view);
			accerlometerSensitivity = (SeekBar) view.findViewById(R.id.accerlometerSensitivity);
			accerlometerSensitivity.setOnSeekBarChangeListener(this);
			leftRightMotion = (Switch) view.findViewById(R.id.leftRightMotion);
			leftRightMotion.setOnCheckedChangeListener(this);
			upDownMotion = (Switch) view.findViewById(R.id.upDownMotion);
			upDownMotion.setOnCheckedChangeListener(this);
			accerlometerSensitivity.setProgress(preferences.getInt("Accerlometer Sensitivity", 2));
			leftRightMotion.setChecked(preferences.getBoolean("Left-Right Motion", true));
			upDownMotion.setChecked(preferences.getBoolean("Up-Down Motion", false));
			AlertDialog dialog = options.create();
			dialog.show();
			break;
		case R.id.mousePointerSpeed:
			View view1 = layoutInflater.inflate(R.layout.mouse_layout, null);
			options.setView(view1);
			mousePointerSpeed = (SeekBar) view1.findViewById(R.id.mousePointerSpeed);
			mousePointerSpeed.setOnSeekBarChangeListener(this);
			mousePointerSpeed.setProgress(preferences.getInt("Mouse Pointer Speed", 10));
			AlertDialog dialog1 = options.create();
			dialog1.show();
			break;
		case R.id.help:
			Intent intent = new Intent("com.bitsblender.racepad.HELP");
			startActivity(intent);
			break;
		case R.id.about:
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

		return super.onOptionsItemSelected(item);
	}

	private boolean RateNowActivity(Intent aIntent) {
		try {
			startActivity(aIntent);
			return true;
		} catch (ActivityNotFoundException e) {
			return false;
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		int id = seekBar.getId();
		SharedPreferences.Editor editor = preferences.edit();
		switch (id) {
		case R.id.accerlometerSensitivity:
			editor.putInt("Accerlometer Sensitivity", progress);
			editor.commit();
			break;
		case R.id.mousePointerSpeed:
			if (progress == 0)
				progress = 1;
			editor.putInt("Mouse Pointer Speed", progress);
			editor.commit();
			mouseSensitivity = preferences.getInt("Mouse Pointer Speed", 10);

		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings("deprecation")
	public void pickProfile() {
		AlertDialog.Builder profilerPicker = new AlertDialog.Builder(KeySendActivity.this, AlertDialog.THEME_HOLO_DARK);
		profilerPicker.setTitle("Pick a profile");
		profilerPicker.setCancelable(false);
		profilerPicker.setAdapter(profilesList, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Toast.makeText(getApplicationContext(), profilesList.getItem(which), Toast.LENGTH_SHORT).show();
				xmlParser = new XMLParser(profiles.getItem(which));
				profileSpinner.setSelection(which);
				gamePath = xmlParser.getAttributeFromTagName("game_path");
			}
		});
		AlertDialog alert = profilerPicker.create();
		alert.show();
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		alert.getWindow().setLayout(size.x, size.y);
	}

	private class SendingThread implements Runnable {

		String msg;

		public SendingThread(String messaage) {
			msg = messaage+"~";
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				/*if (msg.length() > 0) {

					if (modeSelected == com.bitsblender.racepad.Mode.WIFI_MODE) {
						char[] array = new char[512];
						Arrays.fill(array, '\'');
						int size = msg.length();
						if (size < 256) {
							for (int i = 0; i < size; i++) {
								array[i + (128 - size / 2)] = msg.charAt(i);
							}
							msg = new String(array);
						}
					}
				}*/

					synchronized (ACCESSIBILITY_SERVICE) {

						buffer = new byte[msg.length()];
						socketOutputStream.flush();
						buffer = msg.getBytes();
						// System.out.println(new String(buffer));
						socketOutputStream.write(buffer);
						// if(modeSelected ==
						// com.bitsblender.racepad.Mode.BLUETOOTH_MODE)
						socketOutputStream.flush();
					}
				
			} catch (

			IOException e)

			{
				/*
				 * if (modeSelected ==
				 * com.bitsblender.racepad.Mode.BLUETOOTH_MODE)
				 * Toast.makeText(getApplicationContext(),
				 * "RacePad Server Down Send Failed", Toast.LENGTH_LONG)
				 * .show();
				 */
				e.printStackTrace();
				finish();
			}
		}

	}

	private class ReceiveingThread extends Thread {
		public void run() {

			// Keep listening to the InputStream until an exception occurs
			try {
				read();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void read() throws IOException {
			try {

				byte[] buffer; // buffer store for the stream
				int bytes = -1; // bytes returned from read()

				buffer = new byte[1024];
				// Read from the InputStream
				while (bytes < 0) {
					bytes = socketInputStream.read(buffer);
				}
				gamePath = "";
				handler.obtainMessage(PROFILES_UPDATE_STARTED).sendToTarget();
				buffer = trimBuffer(buffer, bytes);
				String fileCount = new String(buffer);
				fileCount = fileCount.replaceAll("\'", "");
				// System.out.println(fileCount);
				int count = Integer.parseInt(fileCount);
				for (int i = 0; i < count; i++) {
					buffer = new byte[1024];

					bytes = socketInputStream.read(buffer);
					// System.out.println(bytes);
					buffer = trimBuffer(buffer, bytes);
					String fileName = new String(buffer);
					fileName = fileName.replaceAll("\'", "");
					File newfile = new File(Environment.getExternalStorageDirectory() + "/RacePad/" + fileName);
					newfile.createNewFile();
					FileOutputStream fos = new FileOutputStream(newfile);
					buffer = new byte[1024];
					bytes = socketInputStream.read(buffer);
					buffer = trimBuffer(buffer, bytes);
					fos.write(new String(buffer).replaceAll("\'", "").getBytes());
					fos.close();
					// Send the obtained bytes to the UI activity
				}
				handler.obtainMessage(PROFILES_UPDATED).sendToTarget();
			}

			catch (NumberFormatException e) {
				// socket.close();
				handler.obtainMessage(PROFILES_UPDATE_FAILED).sendToTarget();
				e.printStackTrace();
			}
		}

		private byte[] trimBuffer(byte[] buffer, int bytes) {
			// System.out.println(bytes);
			if (bytes < 0)
				return buffer;
			byte[] temp = new byte[bytes];
			for (int i = 0; i < bytes; i++) {
				temp[i] = buffer[i];
			}
			return temp;
		}

	}

	public class WiFiMonitor extends Thread {

		@Override
		public void run() {
			super.run();
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

			NetworkInfo activeNetwork;
			boolean isConnected, isWiFi;
			while (true) {
				isWiFi = false;
				activeNetwork = cm.getActiveNetworkInfo();
				isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
				if (activeNetwork != null)
					isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
				if (!(isWiFi && isConnected)) {
					handler.obtainMessage(WIFI_DISCONNECTED).sendToTarget();
					break;
				}
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}
}
