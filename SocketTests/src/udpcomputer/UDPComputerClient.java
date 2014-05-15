package udpcomputer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPComputerClient {

	public UDPComputerClient() {
		try {
			//Create socket
			DatagramSocket socket = new DatagramSocket();
			
			//Get IP
			InetAddress toIP = InetAddress.getByName("localhost");
			
			//Arrays
			byte[] sendData;
			byte[] recievedData;
			
			//Create input reader
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			
			//Start
			System.out.println("PREPARE FOR CAPS");
			
			String line;
			while ((line = reader.readLine()) != null) {
				//Check if stopping
				if (line.equalsIgnoreCase("stop") || line.equalsIgnoreCase("quit")) {
					System.exit(0); //Stop app
				}
				
				//Reset arrays
				sendData = new byte[1024];
				recievedData = new byte[1024];
				
				//Load senddata
				sendData = line.getBytes();
				
				//Create & send packet
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, toIP, 9876);
				socket.send(sendPacket);
				
				//Prepare return packet
				DatagramPacket returnPacket = new DatagramPacket(recievedData, recievedData.length);
				socket.receive(returnPacket);
				
				System.out.println("IN CAPS: " + new String(returnPacket.getData()));				
			}
			
			reader.close();
			socket.close();			
		} catch (Exception e) {
			System.out.println("Woops!");
		}		
	}
	
	public static void main(String[] args) {
		new UDPComputerClient();
	}

}
