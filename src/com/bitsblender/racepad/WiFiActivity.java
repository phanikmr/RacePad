package com.bitsblender.racepad;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class WiFiActivity extends Activity implements android.view.View.OnClickListener {

	String androidIP;
	Button connectBtn;
	EditText selectedIPTxt, selectedPortTxt;
	ListView networkIPsView;
	ArrayAdapter<String> networkIPsAdapter;
	HashSet<String> networkIPsSet;
	WifiManager wifiManager;
	WiFiMonitor wiFiMonitor;
	String DEFAULT_PORT = "55555";
	public static Socket socket;
	ProgressDialog progressDialog;
	ArrayAdapter<String> profilesList;
	ArrayAdapter<File> profiles;
	public static InputStream socketInStream;
	public static OutputStream socketOutStream;
	static Handler handler;
	static final int IP_PINGED = 0;
	static final int SUCCESS_CONNECT = 1;
	static final int MESSAGE_READ = 2;
	static final int MESSAGE_CORRUPTED = 3;
	static final int WIFI_DISCONNECTED = 4;
	static final int CONNECTION_TIMEDOUT = 5;
	int connectionAttempts = 0;
	ConnectThread connectThread;
	Thread conectionTimer;

	@SuppressWarnings("deprecation")
	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wifi_activity);
		socket = null;
		initView();
		Intent receivedIntent = getIntent();
		androidIP = receivedIntent.getStringExtra("Android IP");
		AdView bottomAdd = (AdView) findViewById(R.id.bottomAdWiFi);
		AdRequest adRequest = new AdRequest.Builder().build();
		bottomAdd.loadAd(adRequest);
		// System.out.println(androidIP);
		String[] IPPieces = androidIP.split("\\.");
		selectedPortTxt.setText(DEFAULT_PORT);
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		// System.out.println(IPPieces[0] + "." + IPPieces[1] + "." +
		// IPPieces[2] + ".1");
		// System.out.println(wifiInfo.getSSID());
		if (wifiInfo.getSSID().equals("\"RacePadHotSpot\"")) {
			selectedIPTxt.setText(IPPieces[0] + "." + IPPieces[1] + "." + IPPieces[2] + ".1");
		} else {
			selectedIPTxt.setText(IPPieces[0] + "." + IPPieces[1] + ".");
		}
		progressDialog = new ProgressDialog(WiFiActivity.this, ProgressDialog.THEME_DEVICE_DEFAULT_DARK);
		progressDialog.setTitle("Please Wait");
		progressDialog.setMessage("Connecting...\nPlease make sure your RacePadServer is Ready");
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setCancelable(false);
		handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch (msg.what) {
				case IP_PINGED:
					networkIPsAdapter.add((String) msg.obj);
					networkIPsAdapter.notifyDataSetChanged();
					break;
				case SUCCESS_CONNECT:
					progressDialog.setTitle("Please Wait....");
					progressDialog.setMessage("Downloading Profiles...");
					progressDialog.show();
					ConnectedThread connectedThread = new ConnectedThread();
					connectedThread.start();
					break;
				case MESSAGE_READ:
					Toast.makeText(getApplicationContext(), (String) msg.obj + " Profiles Downloaded Sucessfully",
							Toast.LENGTH_SHORT).show();
					getProfilesList();
					progressDialog.dismiss();
					pickProfile();
					break;
				case MESSAGE_CORRUPTED:
					progressDialog.dismiss();

					Toast.makeText(getApplicationContext(), "Connection Failed \nPlease Trying Again",
							Toast.LENGTH_LONG).show();
					break;
				case WIFI_DISCONNECTED:
					Toast.makeText(getApplicationContext(), "Network Gone \nPlease Connect to network and Trying Again",
							Toast.LENGTH_LONG).show();
					finish();
					break;
				case CONNECTION_TIMEDOUT:
					progressDialog.dismiss();
					Toast.makeText(getApplicationContext(), "Connection Timed Out \nPlease Check Race Pad Server IP and Port and Trying Again",
							Toast.LENGTH_LONG).show();
					break;
				}
			}

		};
		wiFiMonitor = new WiFiMonitor();
		wiFiMonitor.start();
		IPPinger(IPPieces);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	private void initView() {

		connectBtn = (Button) findViewById(R.id.connectWiFiServerBtn);
		selectedIPTxt = (EditText) findViewById(R.id.editTextIP);
		selectedPortTxt = (EditText) findViewById(R.id.editTextPort);
		connectBtn.setOnClickListener(this);
		networkIPsSet = new HashSet<>();
		networkIPsAdapter = new ArrayAdapter<>(this, R.layout.list_view);
		networkIPsView = (ListView) findViewById(R.id.networkIPs);
		networkIPsView.setAdapter(networkIPsAdapter);
	}

	private void IPPinger(final String[] IPPieces) {
		new Thread(new Runnable() {

			@Override
			public void run() {

				final String partialIP = IPPieces[0] + "." + IPPieces[1] + "." + IPPieces[2] + ".";

				for (int j = 0; j <= 255; j++) {
					pingIP(partialIP + Integer.toString(j));
					// System.out.println(partialIP + Integer.toString(i) + "."
					// + Integer.toString(j)+" Pinged");

					/*
					 * try { Thread.sleep(1000); } catch (InterruptedException
					 * e) { e.printStackTrace(); }
					 */
				}

			}
		}).start();

	}

	@Override
	protected void onPause() {
		super.onPause();
		
	}

	private void pingIP(final String host) {

		Thread temp = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					String pingCmd = "ping -c 3 " + host;
					// String pingResult = "";
					Runtime r = Runtime.getRuntime();
					Process p = r.exec(pingCmd);
					// System.out.println(host);
					/*
					 * BufferedReader in = new BufferedReader(new
					 * InputStreamReader(p.getInputStream())); String inputLine;
					 * while ((inputLine = in.readLine()) != null) {
					 * System.out.println(inputLine); // text.setText(inputLine
					 * + "\n\n"); // pingResult += inputLine; //
					 * text.setText(pingResult); } in.close();
					 */
					Thread.sleep(100);
					if (isProcessPinging(p, host)) {
						p.destroy();
					}

					// System.out.println(host+ " thread exited");

				} catch (IOException e) {
					System.out.println(e);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		});
		temp.start();
		try {
			temp.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private boolean isProcessPinging(Process process, String IP) {
		try {
			if (process.exitValue() == 0) {

				// System.out.println(IP + "found");
				// networkIPsAdapter.clear();

				handler.obtainMessage(IP_PINGED, IP).sendToTarget();

				return false;
				// networkIPsAdapter.notifyDataSetChanged();
			}

		} catch (IllegalThreadStateException e) {
			return true;
		}
		return false;
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
			if (proFiles[i].isFile()) {
				profiles.add(proFiles[i]);
			}
			profilesList.add(proFiles[i].getName().replaceAll(".xml", ""));
		}
	}

	@SuppressWarnings("deprecation")
	public void pickProfile() {
		AlertDialog.Builder profilerPicker = new AlertDialog.Builder(WiFiActivity.this, AlertDialog.THEME_HOLO_DARK);
		profilerPicker.setTitle("Pick a profile");
		profilerPicker.setCancelable(false);
		profilerPicker.setAdapter(profilesList, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Toast.makeText(getApplicationContext(), profilesList.getItem(which), Toast.LENGTH_SHORT).show();
				Intent intent = new Intent("com.bitsblender.racepad.KEYSENDACTIVITY");
				intent.putExtra("XMLFile", profiles.getItem(which));
				intent.putExtra("SelectedMode", Mode.WIFI_MODE);
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

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.connectWiFiServerBtn) {
			connectionAttempts = 0;
			connectThread = new ConnectThread();
			progressDialog.show();
			connectThread.start();
			conectionTimer = new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						Thread.sleep(10000);
						if(connectThread.isAlive()){
							handler.obtainMessage(CONNECTION_TIMEDOUT).sendToTarget();
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			});
			conectionTimer.start();
		}
	}

	private class ConnectThread extends Thread {
		@Override
		public void run() {
			String dstAddress = selectedIPTxt.getText().toString();
			int dstPort = Integer.parseInt(selectedPortTxt.getText().toString());
			String response = "";
			System.out.println("Waiting for " + dstAddress + ":" + selectedPortTxt.getText().toString());
			try {
				socket = new Socket(dstAddress, dstPort);
				// ByteArrayOutputStream byteArrayOutputStream = new
				// ByteArrayOutputStream(1024);
				byte[] buffer = new byte[1024];
				// int bytesRead;
				socketInStream = socket.getInputStream();
				socketOutStream = socket.getOutputStream();
				/*
				 * notice: inputStream.read() will block if no data return
				 */
				/*
				 * while ((bytesRead = socketInStream.read(buffer)) != -1) {
				 * byteArrayOutputStream.write(buffer, 0, bytesRead); response
				 * += byteArrayOutputStream.toString("UTF-8"); }
				 */
				socketInStream.read(buffer);
				response = new String(buffer);
				System.out.println(response);
				handler.obtainMessage(SUCCESS_CONNECT).sendToTarget();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				response = "UnknownHostException: " + e.toString();
				handler.obtainMessage(CONNECTION_TIMEDOUT).sendToTarget();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				response = "IOException: " + e.toString();
				handler.obtainMessage(CONNECTION_TIMEDOUT).sendToTarget();
			}catch (Exception e) {
				handler.obtainMessage(CONNECTION_TIMEDOUT).sendToTarget();
			}

		}
	}

	private class ConnectedThread extends Thread {

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
				File dir = new File(Environment.getExternalStorageDirectory() + "/RacePad");
				System.out.println(dir.getPath());
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
					fileName = fileName.replaceAll("\'", "");
					File newfile = new File(Environment.getExternalStorageDirectory() + "/RacePad/" + fileName);
					newfile.createNewFile();
					FileOutputStream fos = new FileOutputStream(newfile);
					buffer = new byte[1024];
					bytes = socketInStream.read(buffer);
					buffer = trimBuffer(buffer, bytes);
					fos.write(new String(buffer).replaceAll("\'", "").getBytes());
					fos.close();
					// Send the obtained bytes to the UI activity
				}
				handler.obtainMessage(MESSAGE_READ, Integer.toString(count)).sendToTarget();
			}

			catch (NumberFormatException e) {
				socket.close();
				connectionAttempts++;
				handler.obtainMessage(MESSAGE_CORRUPTED).sendToTarget();
			}
		}

		private byte[] trimBuffer(byte[] buffer, int bytes) {
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
			boolean isConnected,isWiFi;
			while (true) {
				isWiFi = false;
				activeNetwork = cm.getActiveNetworkInfo();
				isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
				if(activeNetwork != null)
					isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
				if (!(isWiFi&&isConnected)){
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
