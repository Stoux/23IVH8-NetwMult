package nl.stoux.p2p_discovery_server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashSet;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class RequestHandler implements Runnable {

	private static long sessionID = 1;
	
	private Socket s;
	
	public RequestHandler(Socket s) {
		this.s = s;
	}

	@Override
	public void run() {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
			
			//Read JSON
			String jsonString = reader.readLine();
			System.out.println("Got json: " + jsonString);
			JsonObject json = JsonObject.readFrom(jsonString);
			
			//Get type & switch
			String type = json.get("type").asString();
			switch(type.toLowerCase()) {
			case "register":
				//Parse server
				StreamingServer newServer = new StreamingServer(
					s.getInetAddress().getHostAddress(),
					json.get("rtsp-port").asInt(),
					json.get("file").asString(),
					json.get("content-description").asString(),
					sessionID++
				);
				
				//Add new server
				Main.servers.put(newServer.getSessionID(), newServer);
				
				//Respond
				JsonObject registerResponse = new JsonObject();
				registerResponse.add("accepted", true);
				registerResponse.add("session-id", newServer.getSessionID());
				registerResponse.add("refresh-interval", ControlRunnable.REFRESH_INTERVAL);
				
				//=> Write JSON
				writer.write(registerResponse.toString());
				writer.newLine();
				writer.flush();
				break;
				
			case "server-refresh":
				long sessionID = json.get("session-id").asLong();
				StreamingServer refreshServer = Main.servers.get(sessionID);
				boolean accepted;
				if (refreshServer == null) {
					accepted = false;
				} else {
					accepted = true;
					refreshServer.refreshed();
				}
				
				//Respond
				JsonObject refreshResponse = new JsonObject();
				refreshResponse.add("accepted", accepted);
				
				//=> Write
				writer.write(refreshResponse.toString());
				writer.newLine();
				writer.flush();
				break;
				
			case "server-disconnect":
				long disconnectingID = json.get("session-id").asLong();
				Main.servers.remove(disconnectingID);
				break;
				
			case "get-peer-list":
				HashSet<StreamingServer> servers = new HashSet<StreamingServer>(Main.servers.values());
				JsonObject response = new JsonObject();
				JsonArray data = new JsonArray();
				for (StreamingServer server : servers) {
					JsonObject sObject = new JsonObject();
					sObject.add("peer-ip", server.getIP());
					sObject.add("peer-port", server.getRTSP_port());
					sObject.add("content-description", server.getContentDescription());
					sObject.add("filename", server.getFilename());
					data.add(sObject);
				}
				response.add("data", data);
				
				//Response JSON
				writer.write(response.toString());
				writer.newLine();
				writer.flush();
				break;
				
			}
			
			writer.close();
			reader.close();
			s.close();
		} catch (Exception e) {
			
		}
	}

}
