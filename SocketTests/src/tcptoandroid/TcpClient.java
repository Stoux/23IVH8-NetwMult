package tcptoandroid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class TcpClient {

	public TcpClient() {
		try {
			BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("CAPSIFY YO");
			
			String consoleLine = consoleReader.readLine();
			consoleReader.close();
			
			if (consoleLine.isEmpty()) {
				System.out.println("Nothing entered!");
				return;
			}
			
			
			
			Socket s = new Socket("192.168.2.20", 9001);
			System.out.println("Connected with CAPSIFY");
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
			
			//Write
			writer.write(consoleLine + "\n");
			writer.flush();
			System.out.println("Send: " + consoleLine);
			
			//Read
			String line = reader.readLine();
			System.out.println("Recieved: " + line);
			
			//Close yo
			writer.close();
			reader.close();
			s.close();
			
			System.out.println("END.");
			
		} catch (Exception e) {
			System.out.println("Woops. " + e.getMessage());
		}
	}
	
	public static void main(String[] args) {
		new TcpClient();
	}

}
