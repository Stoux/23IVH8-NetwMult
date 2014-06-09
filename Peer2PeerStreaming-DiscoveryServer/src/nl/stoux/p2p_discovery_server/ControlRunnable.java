package nl.stoux.p2p_discovery_server;

import java.util.HashSet;

public class ControlRunnable implements Runnable {

	private static final long CONTROL_INTERVAL = 70000; //Max 70 seconds
	public static final long REFRESH_INTERVAL = 60000; //Every 60 seconds
	
	private boolean checking = true;
	
	@Override
	public void run() {
		while(checking) {
			HashSet<StreamingServer> servers = new HashSet<StreamingServer>(Main.servers.values());
			for (StreamingServer server : servers) {
				if (System.currentTimeMillis() - server.getLastRefresh() > CONTROL_INTERVAL) {
					Main.servers.remove(server.getSessionID());
				}
			}
			synchronized (this) {
				try {
					this.wait(5000);
				} catch (InterruptedException e) {}
			}
		}
	}
	
	public void setChecking(boolean checking) {
		this.checking = checking;
	}

}
