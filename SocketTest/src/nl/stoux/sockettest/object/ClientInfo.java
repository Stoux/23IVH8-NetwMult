package nl.stoux.sockettest.object;

public class ClientInfo {

	private String ip;
	private int port;
	
	private String text;
	
	public ClientInfo(String ip, int port, String text) {
		this.ip = ip;
		this.port = port;
		this.text = text;
	}
	
	public String getIp() {
		return ip;
	}
	
	public int getPort() {
		return port;
	}
	
	public String getText() {
		return text;
	}
	
}
