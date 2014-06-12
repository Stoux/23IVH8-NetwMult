package nl.stoux.peer2peerstreaming;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import nl.stoux.peer2peerstreaming.data.DiscoveryServer;
import nl.stoux.peer2peerstreaming.data.StreamingPeer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PeerListActivity extends Activity {
	
	public final static String PREFS = "nl.stoux.PeerListActivity.PREFS";
	public final static String PEER = "nl.stoux.PeerListActivity.PEER";

	//Activity Data
	private Context context;
	private PeerListAdapter adapter;
	
	//RefreshTask
	private LoadPeerListTask task;
	
	private int shortAnimTime;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_peer_list);
		
		//Set context
		this.context = this;
		
		//Set anim time
		shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
		
		//Check discovery server data
		if (DiscoveryServer.SERVER_IP == null) {
			DiscoveryServer.loadSettings(context);
		}
		
		//Set adapter
		ListView peerView = (ListView) findViewById(R.id.peer_listview);
		peerView.setAdapter(adapter = new PeerListAdapter(context));
		//=> Set on click listener
		peerView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				//Get item
				StreamingPeer peer = (StreamingPeer) parent.getItemAtPosition(pos);
				
				//Move to new Activity
				Intent peerClientIntent = new Intent(context, PeerClientActivity.class); //TODO Change to correct class
				peerClientIntent.putExtra(PEER, peer);
				startActivity(peerClientIntent);
			}
		});
		
		
		//Load data
		(task = new LoadPeerListTask()).execute((Void) null);
		showProgress(true);
	}
	
	@Override
	protected void onDestroy() {
		if (task != null) {
			task.cancel(true);
			task = null;
		}
		super.onDestroy();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (task == null) {
			(task = new LoadPeerListTask()).execute((Void) null);
			showProgress(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.peer_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch(id) {
		case R.id.action_settings:
			Intent settingsIntent = new Intent(context, DiscoveryServerActivity.class);
			startActivity(settingsIntent);
			break;
		case R.id.action_new_upload:
			//=> Start new upload
			Intent serverIntent = new Intent(context, ServerActivity.class);
			startActivity(serverIntent);
			break;
		case R.id.action_peer_refresh:
			//=> Refresh list 
			if (task != null) {
				Toast.makeText(context, "Al aan 't laden!", Toast.LENGTH_SHORT).show();
			} else {
				(task = new LoadPeerListTask()).execute((Void) null);
				showProgress(true);
			}
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	
	
	/**
	 * Show progress bar
	 * @param show
	 */
	public void showProgress(final boolean show) {
		//Get views
		final View loadingView = findViewById(R.id.loading_status);
		final View listView = findViewById(R.id.peer_list_view_container);
		final View emptyListView = findViewById(R.id.peer_list_empty_layout);
		
		//Show correct view
		loadingView.setVisibility(View.VISIBLE);
		loadingView.animate().setDuration(shortAnimTime)
				.alpha(show ? 1 : 0)
				.setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						loadingView.setVisibility(show ? View.VISIBLE
								: View.GONE);
					}
				});

		listView.setVisibility(View.VISIBLE);
		listView.animate().setDuration(shortAnimTime)
				.alpha(show ? 0 : 1)
				.setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						listView.setVisibility(show ? View.GONE
								: View.VISIBLE);
					}
				});
		
		if (show && emptyListView.getVisibility() == View.VISIBLE) {
			showEmptyList(false);
		}
	}
	
	/**
	 * Show text for an empty list
	 * @param show
	 */
	public void showEmptyList(final boolean show) {
		final View emptyListView = findViewById(R.id.peer_list_empty_layout);
						
		emptyListView.setVisibility(View.VISIBLE);
		emptyListView.animate().setDuration(shortAnimTime)
				.alpha(show ? 1 : 0)
				.setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						emptyListView.setVisibility(show ? View.VISIBLE
								: View.GONE);
					}
				});
	}
	
	
	private class LoadPeerListTask extends AsyncTask<Void, String, List<StreamingPeer>> {
		
		@Override
		protected List<StreamingPeer> doInBackground(Void... arg0) {
			//Empty list
			List<StreamingPeer> peers = new ArrayList<>();
						
			//Contact server
			Socket s = null;
			try {
				//=> Create connection
				s = new Socket();
				s.setSoTimeout(3000); //Timeout after 3 seconds
				s.connect(new InetSocketAddress(DiscoveryServer.SERVER_IP, DiscoveryServer.SERVER_PORT));
				
				//=> Reader & Writer
				BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
				BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
				
				//Create JSON container
				JSONObject json = new JSONObject();
				json.put("type", "get-peer-list");
				
				//=> Send JSON
				w.write(json.toString());
				w.newLine();
				w.flush();
				
				//Wait for response
				String jsonResponse = r.readLine();
				//=> Parse response into JsonObject
				JSONObject response = new JSONObject(jsonResponse);
				
				//Get Peers from Data
				JSONArray dataArray = response.getJSONArray("data");
				//=> Loop thru peers
				for(int i = 0; i < dataArray.length(); i++) {
					JSONObject peer = dataArray.getJSONObject(i);
					
					//Get information
					StreamingPeer sPeer = new StreamingPeer(
						peer.getString("peer-ip"),
						peer.getInt("peer-port"),
						peer.getString("content-description"),
						peer.getString("filename")
					);
					//=> Add to list
					peers.add(sPeer);
				}
			} catch (IOException e) {
				publishProgress("Verbindingsfout met server!");
			} catch (JSONException e) {
				publishProgress("Incorrect antwoord van de server!");
			} finally {
				try {
					s.close();
				} catch (Exception e) {}
			}
			
			//Return list
			return peers;
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			for (String s : values) {
				Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
			}
		}
		
		@Override
		protected void onPostExecute(List<StreamingPeer> result) {
			adapter.updateList(result);
			task = null;
			showProgress(false);
			
			if (result.isEmpty()) {
				showEmptyList(true);
			}
			
		}
		
	}
	
	private class PeerListAdapter extends ArrayAdapter<StreamingPeer> {
		
		public PeerListAdapter(Context context) {
			super(context, R.layout.activity_peer_list_item, new ArrayList<StreamingPeer>());
		}
		
		public void updateList(List<StreamingPeer> newList) {
			super.clear();
			super.addAll(newList);
			super.notifyDataSetChanged();
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Context context = super.getContext();
			//Create RowView
	    	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    	View rowView = inflater.inflate(R.layout.activity_peer_list_item, parent, false);
	    	
	    	//Get data at position
	    	StreamingPeer peer = super.getItem(position);
	    	
	    	//Get views
	    	TextView description = (TextView) rowView.findViewById(R.id.content_description_text);
	    	TextView ipadress = (TextView) rowView.findViewById(R.id.content_ip_text);
	    	//=> Fill data
	    	description.setText(peer.getContentDescription());
	    	ipadress.setText(peer.getFilename() + "@" + peer.getPeerIP() + ":" + peer.getRtspPort());
			
			return rowView;
		}
		
		
		
	}

}
