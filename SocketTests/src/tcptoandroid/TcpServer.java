package tcptoandroid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServer {

	public TcpServer() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		try {
			ServerSocket ss = new ServerSocket(80);
			Socket s;
			while ((s = ss.accept()) != null) {
				BufferedReader fromClient = new BufferedReader(new InputStreamReader(s.getInputStream()));
				BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
				
				System.out.println("Ik ben niet meer lonely");
				String incoming = fromClient.readLine();
				toClient.write(incoming.toUpperCase() + "\n");
				toClient.flush();
				
				fromClient.close();
				toClient.close();
				s.close();
			}
			ss.close();
		} catch (Exception e) {}
		
	}

}
