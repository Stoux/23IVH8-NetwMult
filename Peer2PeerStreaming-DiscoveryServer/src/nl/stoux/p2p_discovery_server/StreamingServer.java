package nl.stoux.p2p_discovery_server;

public class StreamingServer {

	private String IP;
	private int RTSP_port;
	private String filename;
	private String contentDescription;
	private long sessionID;
	
	private long lastRefresh;
	
	public StreamingServer(String IP, int RTSPPort, String filename, String contentDescription, long sessionID) {
		this.IP = IP;
		this.RTSP_port = RTSPPort;
		this.filename = filename;
		this.contentDescription = contentDescription;
		this.sessionID = sessionID;
		lastRefresh = System.currentTimeMillis();
	}
	
	public long getLastRefresh() {
		return lastRefresh;
	}
	
	public void refreshed() {
		lastRefresh = System.currentTimeMillis();
	}
	
	public String getIP() {
		return IP;
	}
	
	public String getContentDescription() {
		return contentDescription;
	}
	
	public String getFilename() {
		return filename;
	}
	
	public long getSessionID() {
		return sessionID;
	}
	
	public int getRTSP_port() {
		return RTSP_port;
	}

}
