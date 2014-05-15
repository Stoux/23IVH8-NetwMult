package tcpcomputer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class TCPComputerClient {
	
	private BufferedWriter writer;

	public TCPComputerClient() {
		try {
			//Create connection
			Socket s = new Socket("localhost", 9001);
			
			//Readers
			BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
			writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
			
			Thread inputThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
						String line;
						while ((line = reader.readLine()) != null) {
							write(line);
						}
					} catch (Exception e) {
						System.out.println("Failed to read!");
					}
				}
			});
			inputThread.start();
			
			//Read line
			String incoming;
			while ((incoming = reader.readLine()) != null) {
				System.out.println("Server: " + incoming);
			}
			
			System.out.println("And it's closed.");
			inputThread.interrupt();
			reader.close();
			writer.close();
			s.close();
		} catch (Exception e) {
			System.out.println("And it's gone.");
		}
		System.exit(0);
	}
	
	private void write(String message) {
		try {
			writer.write(message + "\n");
			writer.flush();
		} catch (Exception e) {
			System.out.println("Failed to write!");
		}
	}
	
	public static void main(String[] args) {
		new TCPComputerClient();
	}

}
