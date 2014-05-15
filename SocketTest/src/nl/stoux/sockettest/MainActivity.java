package nl.stoux.sockettest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

	//Console text
	private String consoleText = "Console output";
	
	//Server task
	private ServerTask task;
	
	//Buttons
	private Button startButton;
	private Button stopButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Button listeners
		//	=> Start button
		startButton = (Button) findViewById(R.id.start_button);
		startButton.setEnabled(true);
		startButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//Change buttons
				startButton.setEnabled(false);
				stopButton.setEnabled(true);
				
				//Create task
				task = new ServerTask();
				//	=> Start
				task.execute();
			}
		});
		
		//	=> Stop button
		stopButton = (Button) findViewById(R.id.stop_button);
		stopButton.setEnabled(false);
		stopButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//Change buttons
				startButton.setEnabled(true);
				stopButton.setEnabled(false);
				 
				//Stop task
				killServer();
			}
		});
		
		//Set text
		((TextView) findViewById(R.id.console_view)).setText(consoleText);
	}
	
	
	private class ServerTask extends AsyncTask<Void, String, Void> {
		
		private ServerSocket ss;
		
		@Override
		protected Void doInBackground(Void... params) {
			//Socket
			Log.w("SocketTest", "Background yo");
			try {
				ss = new ServerSocket(9001);
				publishProgress("Server online!");
				Log.w("SocketTest", "Socket created");
				//Accept connections
				Socket s;
				while (!isCancelled() && (s = ss.accept()) != null) {
					Log.w("SocketTest", "New client");
					//Publish
					publishProgress("New client! " + s.getInetAddress().getHostAddress() + ":" + s.getPort());
						
					//Create reader and writer
					BufferedReader fromClient = new BufferedReader(new InputStreamReader(s.getInputStream()));
					BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
					
					//Read line & Return in caps
					String line = fromClient.readLine();
					//	=> Publish
					publishProgress("Received line: " + line);
					
					//	=> To caps
					String caps = line.toUpperCase();
					
					//	=> Return
					toClient.write(caps + "\n");
					toClient.flush();
					
					//	=> Publish
					publishProgress("Returned in caps: " + caps, "DOEI");
					
					//Close connection
					fromClient.close();
					toClient.close();
					s.close();
				}
				ss.close();
			} catch (Exception e) {
				publishProgress("Server crashed!");
			}
			return null;
		}
		
		@Override
		protected void onCancelled() {
			super.onCancelled();
			Log.w("SocketTest", "Stopped.");
			addLine("Server force stopped!");
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			//Add text
			for (String line : values) {
				addLine(line);
			}
		}
		
		public void closeSocket() {
			try {
				ss.close();
			} catch (Exception e) {}
		}
		
	}
	
	
	
	/**
	 * Add a line to the text view
	 * @param line the line
	 */
	private void addLine(String line) {
		TextView text = (TextView) findViewById(R.id.console_view);
		consoleText += "\n" + line;
		text.setText(consoleText);
		
		//Set focus
		final ScrollView scroller = (ScrollView) text.getParent();
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
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.start_client) {
			//Start client activity
			Intent clientIntent = new Intent(this, ClientActivity.class);
			startActivity(clientIntent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		killServer();
	}
	
	/**
	 * Kill the server
	 */
	private void killServer() {
		if (task instanceof ServerTask && task != null) {
			task.cancel(true);
			task.closeSocket();
		}
		task = null;
	}

}
