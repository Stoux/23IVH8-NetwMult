package nl.stoux.p2p_discovery_server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

	public static ConcurrentHashMap<Long, StreamingServer> servers;
	private static ControlRunnable control;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//Thread pool
		ExecutorService executor = Executors.newFixedThreadPool(5);
		
		//Set with servers
		servers = new ConcurrentHashMap<Long, StreamingServer>();
		
		//Start control thread
		new Thread(control = new ControlRunnable()).start();
		
		try {
			//Create server socket to listen to new requests
			ServerSocket sSocket = new ServerSocket(34404);
			System.out.println("Socket running!");
			Socket incomingSocket;
			while ((incomingSocket = sSocket.accept()) != null) {
				System.out.println("New one: " + incomingSocket.getInetAddress().toString());
				executor.execute(new RequestHandler(incomingSocket));
			}
			sSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		control.setChecking(false);
		
	}

}
