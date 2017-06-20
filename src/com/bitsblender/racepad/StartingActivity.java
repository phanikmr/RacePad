package com.bitsblender.racepad;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import com.bitsblender.racepad.Mode;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

@SuppressLint({ "HandlerLeak", "SdCardPath" })
public class StartingActivity extends Activity implements OnItemClickListener, View.OnClickListener {

	ListView pairedDevicesListView;
	ArrayAdapter<String> pairedDevicesListAdapter, profilesList;
	ArrayAdapter<File> profiles;
	BluetoothAdapter bluetoothAdapter;
	Set<BluetoothDevice> pairedDevicesSet;
	ArrayAdapter<BluetoothDevice> pairedDevicesAdapter;
	Intent bluetoothEnableIntent;
	BroadcastReceiver broadcastReceiver;
	ProgressDialog progressDialog;
	ConnectThread connectThread;
	public static BluetoothSocket socket;
	int modeSelected;
	private static final UUID MY_UUID = UUID.fromString("44361e26-3245-415a-8085-5f8944ef9b78");
	public static final int SUCESS_CONNECT = 0;
	public static final int MESSAGE_READ = 1;
	static final int MESSAGE_CORRUPTED = 3;
	static final int CONNECTION_TIMEDOUT = 4;
	public static InputStream socketInStream;
	public static OutputStream socketOutStream;
	static Handler mHandler;
	private Thread conectionTimer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent receivedIntent = getIntent();
		modeSelected = receivedIntent.getIntExtra("ModeSelected", Mode.BLUETOOTH_MODE);
		setContentView(R.layout.activity_startting);
		init();
		checkAdapterStatus();
		AdView bottomAdd = (AdView) findViewById(R.id.bottomAdd);
		AdRequest adRequest = new AdRequest.Builder().build();
		bottomAdd.loadAd(adRequest);
		mHandler = new Handler() {

	@Override
	public void handleMessage(Message msg) {
		super.handleMessage(msg);
		switch (msg.what) {
		case SUCESS_CONNECT:
			Toast.makeText(getApplicationContext(), "connected", Toast.LENGTH_SHORT).show();
			ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket) msg.obj);
			socket = (BluetoothSocket) msg.obj;
			connectedThread.start();
			// connectedThread.write("Connected
			// Sucessfully".getBytes());
			break;

		case MESSAGE_READ:
			progressDialog.dismiss();
			Toast.makeText(getApplicationContext(), "connected", Toast.LENGTH_SHORT).show();
			getProfilesList();
			// byte[] message = (byte[])msg.obj;
			// String string = new String(message);
			// Toast.makeText(getApplicationContext(),
			// "Message read recieved"+string,
			// Toast.LENGTH_SHORT).show();
			pickProfile();
			break;
		case MESSAGE_CORRUPTED:
			progressDialog.dismiss();
			Toast.makeText(getApplicationContext(), "Connection Failed\n Please Try Again", Toast.LENGTH_LONG).show();
			break;
		case CONNECTION_TIMEDOUT:
			progressDialog.dismiss();
			connectThread.cancel();
			Toast.makeText(getApplicationContext(), "Connection TIMEDOUT\nPlease Try Again", Toast.LENGTH_LONG).show();
			break;
		}
	}

	};

	/*
	 * String networkSSID = "RacePadServer"; String networkPass =
	 * "hardpasscode";
	 * 
	 * WifiConfiguration conf = new WifiConfiguration(); conf.SSID = "\"" +
	 * networkSSID + "\""; // Please note the quotes. String should contain ssid
	 * in quotes
	 * 
	 * conf.preSharedKey = "\""+ networkPass +"\"";
	 */

	}

	/*
	 * public void pickmode() { String[] modesOfConnection = { "Wifi",
	 * "Bluetooth" }; ArrayAdapter<String> modesAdapter = new
	 * ArrayAdapter<>(this, R.layout.list_view, modesOfConnection);
	 * AlertDialog.Builder modeChooser = new AlertDialog.Builder(this,
	 * AlertDialog.THEME_HOLO_DARK); AlertDialog modeCreator;
	 * modeChooser.setTitle("Choose mode of conection");
	 * modeChooser.setCancelable(false); modeChooser.setAdapter(modesAdapter,
	 * new OnClickListener() {
	 * 
	 * @Override public void onClick(DialogInterface dialog, int which) { if
	 * (which == 0) { mode.setMode(Mode.WIFI_MODE);
	 * 
	 * } else { mode.setMode(Mode.BLUETOOTH_MODE);
	 * 
	 * }
	 * 
	 * } }); modeChooser.setNegativeButton("Close", new OnClickListener() {
	 * 
	 * @Override public void onClick(DialogInterface dialog, int which) {
	 * finish(); } });
	 * 
	 * modeCreator = modeChooser.create(); modeCreator.show();
	 * 
	 * }
	 */

	@Override
	public void finish() {
		super.finish();
	}

	@SuppressWarnings("deprecation")
	public void pickProfile() {
		AlertDialog.Builder profilerPicker = new AlertDialog.Builder(StartingActivity.this,
				AlertDialog.THEME_HOLO_DARK);
		profilerPicker.setTitle("Pick a profile");
		profilerPicker.setCancelable(false);
		profilerPicker.setAdapter(profilesList, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Toast.makeText(getApplicationContext(), profilesList.getItem(which), Toast.LENGTH_SHORT).show();
				Intent intent = new Intent("com.bitsblender.racepad.KEYSENDACTIVITY");
				intent.putExtra("XMLFile", profiles.getItem(which));
				intent.putExtra("SelectedMode", modeSelected);
				startActivity(intent);
			}
		});
		AlertDialog alert = profilerPicker.create();
		alert.show();
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		alert.getWindow().setLayout(size.x, size.y);
	}

	protected void getProfilesList() {
		File profileFolder = new File(Environment.getExternalStorageDirectory() + "/RacePad");
		profilesList = new ArrayAdapter<>(this, R.layout.list_view);
		profiles = new ArrayAdapter<>(this, R.layout.list_view);
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

	@SuppressWarnings("deprecation")
	private void checkAdapterStatus() {
		if (bluetoothAdapter == null) {
			Toast.makeText(getApplicationContext(), "Your Device Not Supports Bluetooth", Toast.LENGTH_LONG).show();
			finish();
		} else {
			AlertDialog.Builder message = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			message.setMessage("Please make sure to pair your PC Bluetooth Device with this Device to show in list.\n"
					+ "Starting RacePadServer in your PC before client is Recommended.");
			message.setCancelable(true);
			AlertDialog alert = message.create();
			alert.show();
			if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
				startActivityForResult(bluetoothEnableIntent, 0);
			} else {
				addPairedDevicestoList();
			}

		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_CANCELED) {
			Toast.makeText(getApplicationContext(), "You Should Enable Bluetooth to continue ", Toast.LENGTH_LONG)
					.show();
			finish();
		} else {
			addPairedDevicestoList();
		}
	}

	private void addPairedDevicestoList() {
		pairedDevicesListAdapter = new ArrayAdapter<>(this, R.layout.list_view);
		pairedDevicesAdapter = new ArrayAdapter<>(this, R.layout.list_view);
		pairedDevicesSet = bluetoothAdapter.getBondedDevices();
		if (pairedDevicesSet.size() > 0) {
			for (BluetoothDevice device : pairedDevicesSet) {
				BluetoothClass deviceClass = device.getBluetoothClass();
				if (deviceClass.getMajorDeviceClass() == BluetoothClass.Device.Major.COMPUTER) {
					pairedDevicesListAdapter.add(device.getName() + "\n" + device.getAddress());
					pairedDevicesAdapter.add(device);
				}
			}
		}
		pairedDevicesListView.setAdapter(pairedDevicesListAdapter);

	}

	private void init() {
		pairedDevicesListView = (ListView) findViewById(R.id.pairedDevicesListView);
		Button addBTDevice = (Button) findViewById(R.id.addBTDevice);
		addBTDevice.setOnClickListener(this);
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		bluetoothEnableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		pairedDevicesListView.setOnItemClickListener(this);
		broadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
					if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF)
						startActivityForResult(bluetoothEnableIntent, 0);
				}
			}
		};
		IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(broadcastReceiver, intentFilter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_activity1, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.help1) {
			Intent intent = new Intent("com.bitsblender.racepad.HELP");
			startActivity(intent);
		} else if (id == R.id.about1) {
			AlertDialog.Builder about = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			LayoutInflater layoutInflater = this.getLayoutInflater();
			View view = layoutInflater.inflate(R.layout.about_layout, null);
			about.setView(view).setPositiveButton("Rate Now", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
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

			AlertDialog dialog = about.create();
			dialog.show();
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
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(broadcastReceiver);
		try {
			connectThread.cancel();
		} catch (Exception e) {

		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// Toast.makeText(getApplicationContext(),
		// pairedDevicesAdapter.getItem(arg2).getName(),
		// Toast.LENGTH_SHORT).show();
		progressDialog = new ProgressDialog(StartingActivity.this, ProgressDialog.THEME_DEVICE_DEFAULT_DARK);
		progressDialog.setTitle("Please Wait");
		progressDialog.setMessage("Connecting...\nPlease make sure your RacePadServer is Ready");
		progressDialog.setButton("Cancel", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				try {
					connectThread.cancel();
					progressDialog.dismiss();
				} catch (Exception e) {

				}
			}
		});
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setCancelable(false);
		progressDialog.show();
		connectThread = new ConnectThread(pairedDevicesAdapter.getItem(arg2));
		connectThread.start();
		conectionTimer = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					System.out.println("connection timer tit tic");
					Thread.sleep(10000);
					if(connectThread.isAlive()){
						mHandler.obtainMessage(CONNECTION_TIMEDOUT).sendToTarget();
						System.out.println("Connection Timed Out");
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		});
		conectionTimer.start();
	}

	private class ConnectThread extends Thread {
		public final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;
			mmDevice = device;

			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				// MY_UUID is the app's UUID string, also used by the server
				// code
				tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
			}
			mmSocket = tmp;
		}

		public void run() {
			// Cancel discovery because it will slow down the connection
			bluetoothAdapter.cancelDiscovery();

			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				mmSocket.connect();
				mHandler.obtainMessage(SUCESS_CONNECT, mmSocket).sendToTarget();
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				try {
					mmSocket.close();
				} catch (IOException closeException) {
					mHandler.obtainMessage(MESSAGE_CORRUPTED).sendToTarget();
				}
				return;
			}

			// Do work to manage the connection (in a separate thread)
			// manageConnectedSocket(mmSocket);
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
			}
		}
	}

	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		// private final InputStream mmInStream;
		// private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			mmSocket = socket;
			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				socketInStream = mmSocket.getInputStream();
				socketOutStream = mmSocket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {

			// Keep listening to the InputStream until an exception occurs
		
				read();

		}

		public void read() {
			try {
				File dir = new File(Environment.getExternalStorageDirectory() + "/RacePad");
				if (dir.exists()) {
					if (dir.isDirectory()) {
						String[] children = dir.list();
						for (int i = 0; i < children.length; i++) {
							new File(dir, children[i]).delete();
						}
					}
				}

				byte[] buffer; // buffer store for the stream
				int bytes; // bytes returned from read()

				buffer = new byte[1024];
				// Read from the InputStream
				bytes = socketInStream.read(buffer);
				buffer = trimBuffer(buffer, bytes);
				String fileCount = new String(buffer);
				fileCount = fileCount.replaceAll("\'", "");
				System.out.println(fileCount);
				int count = Integer.parseInt(fileCount);
				for (int i = 0; i < count; i++) {
					buffer = new byte[1024];
					bytes = socketInStream.read(buffer);
					// System.out.println(bytes);
					buffer = trimBuffer(buffer, bytes);
					String fileName = new String(buffer);
					// fileName = fileName.replaceAll("\'", "");
					File newfile = new File(Environment.getExternalStorageDirectory() + "/RacePad/" + fileName);
					newfile.createNewFile();
					FileOutputStream fos = new FileOutputStream(newfile);
					buffer = new byte[1024];
					bytes = socketInStream.read(buffer);
					buffer = trimBuffer(buffer, bytes);
					fos.write(buffer);
					fos.close();
					// Send the obtained bytes to the UI activity
				}
				mHandler.obtainMessage(MESSAGE_READ, Integer.toString(count)).sendToTarget();
			}

			catch (Exception e) {
				try{
				socket.close();
				}
				catch (Exception er)
				{
					er.printStackTrace();
				}
				mHandler.obtainMessage(MESSAGE_CORRUPTED).sendToTarget();
			}
		}

		private byte[] trimBuffer(byte[] buffer, int bytes) {
			byte[] temp = new byte[bytes];
			for (int i = 0; i < bytes; i++) {
				temp[i] = buffer[i];
			}
			return temp;
		}

		/* Call this from the main activity to send data to the remote device */
		public void write(byte[] bytes) {
			try {
				socketOutStream.write(bytes);
			} catch (IOException e) {
			}
		}

	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.addBTDevice) {
			startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
		}
	}
}
