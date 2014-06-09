package nl.stoux.peer2peerstreaming.objects;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import android.widget.Toast;
import nl.stoux.peer2peerstreaming.ServerActivity;
import nl.stoux.peer2peerstreaming.data.DiscoveryServer;

public class ServerRunnable implements Runnable {

	//Data info
	private String file = "movie.Mjpeg";
	private String contentDescription;
	
	//Server info
	private int serverPort = 9889;
	private int clientSessionID = 1;
	private ServerSocket sSocket;
	
	//Discovery server info
	private long serverSessionID;
	private long refreshInterval; //Interval 
	private Timer timer;
	
	//Status info
	private boolean starting = true;
	private boolean killed = false;
	
	public ServerRunnable(String contentDescription) {
		this.contentDescription = contentDescription;
	}
		
	@Override
	public void run() {
		//Make a ServerSocket for RTSP connections
		try {
			sSocket = new ServerSocket(serverPort);
		} catch (IOException e) {
			ServerActivity.serverRunnable = null;
			notifyUI();
			toastUI("Kon geen server aanmaken!");
			return;
		}
		
		//Register with the DiscoveryServer
		try {
			Socket discoverySocket = new Socket(DiscoveryServer.SERVER_IP, DiscoveryServer.SERVER_PORT);
			BufferedReader reader = new BufferedReader(new InputStreamReader(discoverySocket.getInputStream()));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(discoverySocket.getOutputStream()));
			
			//Create JSON
			JSONObject sendable = new JSONObject();
			sendable.put("type", "register");
			sendable.put("file", file);
			sendable.put("content-description", contentDescription);
			sendable.put("rtsp-port", serverPort);
			
			//=> Send JSON
			writer.write(sendable.toString());
			writer.newLine();
			writer.flush();
			
			
			//Wait for response
			String jsonResponse = reader.readLine();
			JSONObject response = new JSONObject(jsonResponse);
			
			//=> Get data
			if (!response.getBoolean("accepted")) {
				discoverySocket.close();
				throw new Exception();
			}
			serverSessionID = response.getLong("session-id");
			refreshInterval = response.getLong("refresh-interval");
			
			//Close resources
			writer.close();
			reader.close();
			discoverySocket.close();
		} catch (Exception e) {
			try {
				sSocket.close();
			} catch (Exception ex) {}
			ServerActivity.serverRunnable = null;
			notifyUI();
			toastUI("Kon niet registreren!");
			return;
		}
		
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					Socket discoverySocket = new Socket(DiscoveryServer.SERVER_IP, DiscoveryServer.SERVER_PORT);
					BufferedReader reader = new BufferedReader(new InputStreamReader(discoverySocket.getInputStream()));
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(discoverySocket.getOutputStream()));
					
					//Create JSON
					JSONObject json = new JSONObject();
					json.put("type", "server-refresh");
					json.put("session-id", serverSessionID);
					
					//=> Send JSON
					writer.write(json.toString());
					writer.newLine();
					writer.flush();
					
					//Wait for response
					String jsonResponse = reader.readLine();
					JSONObject response = new JSONObject(jsonResponse);
					if (!response.getBoolean("accepted")) { //Not accepted -> Kill server
						discoverySocket.close();
						throw new Exception();
					}
					
					//Close resources
					writer.close();
					reader.close();
					discoverySocket.close();
				} catch (Exception e) {
					toastUI("Fout met discovery server!");
					killServer();
				}
			}
		}, refreshInterval, refreshInterval);
		
		
		starting = false;
		notifyUI();
		try {
			Socket incomingSocket;
			while ((incomingSocket = sSocket.accept()) != null) {
				ServerClient client = new ServerClient(incomingSocket, clientSessionID++);
				synchronized (ServerActivity.clients) {
					ServerActivity.clients.add(client);
				}
				new Thread(client).start();
			}
		} catch (Exception e) {
			
		}
		
		//Try to notify discovery server of stopping
		try {
			Socket discoverySocket = new Socket(DiscoveryServer.SERVER_IP, DiscoveryServer.SERVER_PORT);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(discoverySocket.getOutputStream()));
			
			//Create JSON
			JSONObject json = new JSONObject();
			json.put("type", "server-disconnect");
			json.put("session-id", serverSessionID);
			//=> Send JSON
			writer.write(json.toString());
			writer.newLine();
			writer.flush();
			
			//CBA about a response
			
			//Close resources
			writer.close();
			discoverySocket.close();
		} catch (Exception e) {
			//Not much we can do if it fails
		}
		
		//Kill any clients
		HashSet<ServerClient> clients = new HashSet<>(ServerActivity.clients);
		for (ServerClient client : clients) {
			client.killClient();
		}
		
		//Set ServerThread/Runnable to null
		ServerActivity.serverRunnable = null;
		
		notifyUI();
	}
	
	private void toastUI(final String text) {
		if (ServerActivity.currentActivity != null) {
			ServerActivity.currentActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(ServerActivity.currentActivity, text, Toast.LENGTH_SHORT).show();
				}
			});
		}
	}
	
	private void notifyUI() {
		if (ServerActivity.currentActivity != null) {
			ServerActivity.currentActivity.refreshServerThreadStatus();
		}
	}
	
	public void killServer() {
		killed = true;
		//Kill the ServerSocket
		try { sSocket.close(); } catch (Exception e) {}
		//Stop the timer
		try { timer.cancel(); } catch (Exception e) {}
		
	}
	
	public boolean isKilled() {
		return killed;
	}
	
	public boolean isStarting() {
		return starting;
	}
	
	
}
