package tcpcomputer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPComputerServer {

	public TCPComputerServer() {
		try { 
			//Create ServerSocket
			ServerSocket ss = new ServerSocket(9001);
			System.out.println("Listening!");
			Socket s;
			while ((s = ss.accept()) != null) {
				//Create reader and writer
				BufferedReader fromClient = new BufferedReader(new InputStreamReader(s.getInputStream()));
				BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
				
				System.out.println("New client!");
				
				//Read line
				String line = fromClient.readLine();
				System.out.println("C: " + line);
				
				if (!line.equalsIgnoreCase("Knock Knock")) {
					//Write go away
					toClient.write("Go away!" + "\n");
					toClient.flush();
					System.out.println("S: Go away!");
					
				} else {
					//Write question
					toClient.write("Who's there?" + "\n");
					toClient.flush();
					System.out.println("S: Who's there?");
					
					//Recieve response
					String responseLine = fromClient.readLine();
					System.out.println("C: " + responseLine);
					
					//Bid farewell
					toClient.write("You're not allowed in " + responseLine + ". Goodbye" + "\n");
					toClient.flush();
					System.out.println("S: You're not allowed in " + responseLine + ". Goodbye");
				}
				//Close
				toClient.close();
				fromClient.close();
				s.close();
				System.out.println("\n\n");
			}
			
			ss.close();
		} catch (Exception e) {
			System.err.println("Woops! " + e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public static void main(String[] args) {
		new TCPComputerServer();
	}

}
