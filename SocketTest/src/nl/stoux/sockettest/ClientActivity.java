package nl.stoux.sockettest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import nl.stoux.sockettest.object.ClientInfo;
import android.app.Activity;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ClientActivity extends Activity {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client);
		
		//Get local wifi IP
		WifiManager wim = (WifiManager) getSystemService(WIFI_SERVICE);
		
		
		//Fill default data
		((EditText) findViewById(R.id.ip_input)).setText(Formatter.formatIpAddress(wim.getDhcpInfo().ipAddress));
		((EditText) findViewById(R.id.port_input)).setText("9001");
		((TextView) findViewById(R.id.console_view)).setText("Console output");
		
		//Add listeners
		((Button) findViewById(R.id.caps_button)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				handleCapsify();
			}
		});
	}
	
	private void handleCapsify() {
		//Check if everything is correct
		//	=> Check IP
		String ip = getTextFromView(R.id.ip_input);
		if (ip.isEmpty()) {
			makeToast("Vul een IP in!");
			return;
		}
		
		//	=> Check port
		String portString = getTextFromView(R.id.port_input);
		if (portString.isEmpty()) { //Empty check
			makeToast("Vul een portnummer in!");
			return;
		}
		int port;
		try { //Valid number
			port = Integer.parseInt(portString);
		} catch (NumberFormatException e) {
			makeToast("Ongeldig poortnummer!");
			return;
		}
		if (port < 1 || port > 65535) { //Between range
			makeToast("Portnummers: 1 t/m 65535");
			return;
		}
		
		//	=> Check message
		String message = getTextFromView(R.id.caps_input);
		if (message.isEmpty()) {
			makeToast("Vul capsify text in!");
			return;
		}
		
		//Disable inputs
		setInputs(false);
		
		//Create client
		ClientInfo info = new ClientInfo(ip, port, message);
		
		//Start task
		new ClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, info);
	}
	
	private class ClientTask extends AsyncTask<ClientInfo, String, Void> {
		
		private Socket s;
		
		@Override
		protected Void doInBackground(ClientInfo... params) {
			Log.w("SocketTest", "Starting background task (Params: " + params.length + ")");
			for (ClientInfo info : params) {
				if (isCancelled()) return null; //Check if cancelled
				try {
					//Try to connect
					publishProgress("Connecting to " + info.getIp()  + ":" + info.getPort());
					
					//	=> Create socket
					s = new Socket(info.getIp(), info.getPort());
					
				} catch (Exception e) {
					//	=> Failed to connect
					publishProgress("Failed to connect!");
					s = null;
				}
				
				//If connected
				if (s != null) {
					try {
						//Create readers
						BufferedReader fromServer = new BufferedReader(new InputStreamReader(s.getInputStream()));
						BufferedWriter toServer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
						
						//Send text
						publishProgress("Sending text: " + info.getText());
						toServer.write(info.getText() + "\n");
						toServer.flush();
						
						//Recieve in caps
						String caps = fromServer.readLine();
						publishProgress("In caps: " + caps);
						
						//Close connection
						toServer.close();
						fromServer.close();
						s.close();
					} catch (Exception e) {
						publishProgress("Woops! Connection lost!");
					}
					s = null;
				}
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			//Re-enable inputs
			setInputs(true);
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			for (String value : values) {
				addLineToConsole(value);
			}
		}
				
	}
	
	/**
	 * Set all  the inputs to enabled or disabled
	 * @param enabled their state
	 */
	private void setInputs(boolean enabled) {
		findViewById(R.id.caps_button).setEnabled(enabled);
		findViewById(R.id.caps_input).setEnabled(enabled);
		findViewById(R.id.ip_input).setEnabled(enabled);
		findViewById(R.id.port_input).setEnabled(enabled);
	}
	
	/**
	 * Get the text from a TextView
	 * @param ID the ID of the view
	 * @return the text
	 */
	private String getTextFromView(int ID) {
		return ((TextView) findViewById(ID)).getText().toString();
	}
	
	/**
	 * Make & display a toast
	 * @param text The text in the toast
	 */
	private void makeToast(String text) {
		Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * Add a line to the current console view
	 * @param line The line
	 */
	private void addLineToConsole(String line) {
		TextView console = (TextView) findViewById(R.id.console_view);
		String currentText = console.getText().toString();
		currentText += "\n" + line;
		console.setText(currentText);
		
		//Scroll down
		final ScrollView scroller = (ScrollView) console.getParent();
		scroller.post(new Runnable() {
			
			@Override
			public void run() {
				scroller.fullScroll(View.FOCUS_DOWN);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.client, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
