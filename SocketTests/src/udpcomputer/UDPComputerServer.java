package udpcomputer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPComputerServer {

	boolean running = true;
	
	public UDPComputerServer() {
		
		try {
			//Create socket
			DatagramSocket socket = new DatagramSocket(9876);
			
			//Byte arrays
			byte[] recievedData;
			byte[] sendData;
			
			while (running) {
				//Reset arrays
				sendData = new byte[1024];
				recievedData = new byte[1024];
				
				//Recieve package
				DatagramPacket recievedPacket = new DatagramPacket(recievedData, recievedData.length);
				socket.receive(recievedPacket);
				
				//Read content
				String content = new String(recievedPacket.getData());
				//TO CAPS
				String caps = content.toUpperCase();
				//To bytes
				sendData = caps.getBytes();
				
				//Create return package
				//	=> Get address & port
				int port = recievedPacket.getPort();
				InetAddress inet = recievedPacket.getAddress();
				DatagramPacket returnPacket = new DatagramPacket(sendData, sendData.length, inet, port);
				
				//	=> Send
				socket.send(returnPacket);
			}
			
			socket.close();
		} catch (Exception e) {
			
		}
		
	}
	
	public static void main(String[] args) {
		new UDPComputerServer();
	}

}
