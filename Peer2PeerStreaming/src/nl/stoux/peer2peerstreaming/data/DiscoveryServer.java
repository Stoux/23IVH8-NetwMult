package nl.stoux.peer2peerstreaming.data;

import nl.stoux.peer2peerstreaming.PeerListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class DiscoveryServer {

	private static final String DEFAULT_IP = "192.168.2.45";
	public static String SERVER_IP;
	private static final int DEFAULT_PORT = 34404;
	public static int SERVER_PORT;
	
	private static final String PREFS_IP = "SERVER_IP";
	private static final String PREFS_PORT = "SERVER_PORT";
	
	/**
	 * Load the settings from the Preferences
	 * @param context The context
	 */
	public static void loadSettings(Context context) {
		//Preferences
		SharedPreferences settings = context.getSharedPreferences(PeerListActivity.PREFS, 0);
		//Get data
		SERVER_IP = settings.getString(PREFS_IP, DEFAULT_IP);
		SERVER_PORT = settings.getInt(PREFS_PORT, DEFAULT_PORT);
		
		//Logging
		Log.d("discovery", "IP: " + SERVER_IP + " | Port: " + SERVER_PORT);
	}
	
	/**
	 * Save the current IP & Port settings
	 * @param context The context
	 */
	public static void saveSettings(Context context) {
		//Get preferences
		SharedPreferences settings = context.getSharedPreferences(PeerListActivity.PREFS, 0);
		//Start edit
		Editor editor = settings.edit();
		
		//Set edits
		editor.putString(PREFS_IP, SERVER_IP);
		editor.putInt(PREFS_PORT, SERVER_PORT);
		
		//Commit
		editor.commit();
	}
	
}
