package nl.stoux.peer2peerstreaming;

import java.util.ArrayList;
import java.util.List;

import nl.stoux.peer2peerstreaming.objects.ServerClient;
import nl.stoux.peer2peerstreaming.objects.ServerRunnable;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ServerActivity extends Activity {

	//The server thread
	public static ServerRunnable serverRunnable;
	
	//The current activity
	public static ServerActivity currentActivity;
	
	//List of clients
	public static List<ServerClient> clients = new ArrayList<>();
	
	//The Context
	private Context context;
	//Adapter that shows the data
	private ClientListAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_server);
		
		//Context
		this.context = this;
		
		//Fill ListView
		ListView peerList = (ListView) findViewById(R.id.connected_peers_list);
		peerList.setAdapter(adapter = new ClientListAdapter(context));
		
		//Set correct buttonState
		refreshServerStatus();
		
		//Button listeners
		((Button) findViewById(R.id.start_server_button)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onStartButton();
			}
		});
		((Button) findViewById(R.id.stop_server_button)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onStopButton();
			}
		});
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		currentActivity = null;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		adapter.notifyDataSetChanged();
		refreshServerStatus();
		currentActivity = this;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.empty_menu, menu);
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
		
	//Start/Stop buttons
	public void onStartButton() {
		if (serverRunnable == null) {
			final EditText contentEdit = new EditText(context);
			
			//Create dialog
			AlertDialog dialog = new AlertDialog.Builder(context)
				.setTitle("Beschrijving")
				.setMessage("Beschrijving van de video")
				.setView(contentEdit)
				.setPositiveButton("Start", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String contentText = contentEdit.getText().toString();
						new Thread(serverRunnable = new ServerRunnable(contentText)).start();
						refreshServerStatus();
					}
				})
				.setNegativeButton("Annuleren", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).show();
			
			//Positive button
			final Button startButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
			contentEdit.addTextChangedListener(new TextWatcher() {
				@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
				@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				
				@Override
				public void afterTextChanged(Editable s) {
					startButton.setClickable(s.length() > 0);
				}
			});
		}
	}
	
	public void onStopButton() {
		if (serverRunnable != null) {
			serverRunnable.killServer();
			refreshServerStatus();
		}
	}
	
	private void setButtonState(int buttonID, boolean enabled) {
		//Get button
		View view = findViewById(buttonID);
		
		//Set state
		view.setEnabled(enabled);
		
		//Set background color
		int color = (enabled ? Color.WHITE : Color.GRAY);
		view.setBackgroundColor(color);
	}
	
	/**
	 * Refresh the ListView with client lists
	 */
	public void refreshClientList() {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				adapter.notifyDataSetChanged();
			}
		});
	}
	
	public void refreshServerThreadStatus() {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				refreshServerStatus();
			}
		});
	}
	
	/**
	 * Refresh the Start/Stop buttons & status
	 */
	private void refreshServerStatus() {
		boolean startButton = false;
		boolean stopButton = false;
		String status = null;
		
		if (serverRunnable == null) {
			startButton = true;
			status = "uit";
		} else if (serverRunnable.isKilled()) {
			status = "stoppen..";
		} else if (serverRunnable.isStarting()) {
			status = "starten..";
		} else {
			stopButton = true;
			status = "aan";
		}
		
		
		//Set button states
		setButtonState(R.id.start_server_button, startButton);
		setButtonState(R.id.stop_server_button, stopButton);
		if (status != null) {
			TextView statusView = (TextView) findViewById(R.id.server_status_info);
			statusView.setText("Server status: " + status);
		}
	}
	
	
	private class ClientListAdapter extends ArrayAdapter<ServerClient> {
		
		public ClientListAdapter(Context context) {
			super(context, R.layout.activity_server_peer_item, clients);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Context context = super.getContext();
			if (convertView == null) {
				//Create RowView
		    	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    	convertView = inflater.inflate(R.layout.activity_server_peer_item, parent, false);
			}
			
	    	//Get data at position
	    	ServerClient client = super.getItem(position);
	    	
	    	//Get views
	    	TextView clientIpView = (TextView) convertView.findViewById(R.id.server_list_peer_ip);
	    	TextView clientStatusView = (TextView) convertView.findViewById(R.id.server_list_peer_status);
	    	ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.server_list_peer_progress);
	    	
	    	//=> Fill data
	    	clientIpView.setText(client.getClientIpString());
	    	progressBar.setProgress(client.getImageNumber());
	    	//	=> Switch state
	    	String state;
	    	switch(client.getState()) {
	    	case ServerClient.INIT:
	    		state = "Starting";
	    		break;
	    	case ServerClient.READY:
	    		state = "Ready";
	    		break;
	    	case ServerClient.PLAYING:
	    		state = "Playing";
	    		break;
	    	case ServerClient.TEARDOWN:
	    		state = "Teardown";
	    		break;
	    	default:
	    		state = "Unkwnown";
	    	}
	    	clientStatusView.setText(state);
			
	    	//Return view
			return convertView;			
		}
		
	}
	

}
