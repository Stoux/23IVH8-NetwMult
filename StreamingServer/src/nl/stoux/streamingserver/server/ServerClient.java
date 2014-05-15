package nl.stoux.streamingserver.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.StringTokenizer;

import android.os.Environment;
import android.util.Log;

public class ServerClient implements Runnable {
	
	//Final statics
	//	=> States
	public final static int INIT = 0;
	public final static int READY = 1;
	public final static int PLAYING = 2;
	//	=> RTSP message types
	public final static int SETUP = 3;
	public final static int PLAY = 4;
	public final static int PAUSE = 5;
	public final static int TEARDOWN = 6;
	public final static int FAILED = 7;
	//	=> Others
	private final static String CRLF = "\r\n";
	private final static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
	
	
	//Client connection
	private Socket rtspSocket;
	private InetAddress clientIPAddr;
	private int rtpDestPort;
	private int sessionID;
	
	//Video information
	private int imageNumber = 0; //Image number currently being send
	private int videoLength = 500; //Assuming video = movie.Mjpeg | Should be able to do this dynamic?
	private String videoFileName; //Name of video
	private VideoStream video; //The VideoStream to access frames
	
	//State
	private int state = 0;
	private int rtspSeqNumber = 0;
	private byte[] buffer; //Video buffer
	
	//SocketReader & Writer
	private BufferedReader rtspReader;
	private BufferedWriter rtspWriter;
	
	public ServerClient(Socket rtspSocket, int sessionID) {
		//Set this
		this.rtspSocket = rtspSocket;
		this.sessionID = sessionID;
		
		//Retrieve targetIPAddress
		clientIPAddr = rtspSocket.getInetAddress();
		
		//Set state
		state = INIT;
	}
	
	@Override
	public void run() {
		//Initialize
		buffer = new byte[15000];
		
		//Create reader & writer
		try {
			rtspReader = new BufferedReader(new InputStreamReader(rtspSocket.getInputStream()));
			rtspWriter = new BufferedWriter(new OutputStreamWriter(rtspSocket.getOutputStream()));
		} catch (IOException e) {
			Log.w("StreamingServer", "Failed to create Reader & Writer");
			state = FAILED;
			return;
		}
		
		//Wait for setup message
		int requestType;
		boolean done = false;
		while (!done) {
			requestType = parseRtspResponse();
			if (requestType == SETUP) {
				done = true;
				
				//Update state
				state = READY;
				Log.i("StreamingServer", "[Session: " + sessionID + "] State: READY");
				
				//Send response
				sendRtspResponse();
				
				//Create videostream
				try {
					//Fully ignore videoFileName => We're always going to return movie.Mjpeg
					String path = Environment.getExternalStorageDirectory().getAbsolutePath();
					video = new VideoStream(path + File.separator + videoFileName);
				} catch (Exception e) {
					Log.w("StreamingServer", "[Session: " + sessionID + "] Failed to open VideoStream!");
					state = FAILED;
					return;
				}
			}			
		}
		
		//Wait for incoming requests
		while (true && state != FAILED && state != TEARDOWN) {
			requestType = parseRtspResponse();
			
			if (requestType == PLAY && state == READY) { //Ready to play
				sendRtspResponse(); //Respond
				
				//Update state
				state = PLAYING;
				Log.i("StreamingServer", "[Session: " + sessionID + "] State: PLAYING");				
			} else if (requestType == PAUSE && state == PLAYING) {
				sendRtspResponse(); //Respond
				
				//Update state
				state = READY;
				Log.i("StreamingServer", "[Session: " + sessionID + "] State: READY");
			} else if (requestType == TEARDOWN) {
				sendRtspResponse(); //Respond

				//Update state
				state = TEARDOWN;
				Log.i("StreamingServer", "[Session: " + sessionID + "] State: PLAYING");
				
				//Close client
				try {
					rtspReader.close();
					rtspWriter.close();
					rtspSocket.close();
				} catch (Exception e) {
					//Closing error. Ignore.
				}
			}
		}
	}
	
	
	//------------------------------------
	//Parse RTSP Request
	//------------------------------------
	private int parseRtspResponse() {
		int requestType = -1;
	    try{
	    	//parse request line and extract the request_type:
	    	String requestLine = rtspReader.readLine();
	    	//System.out.println("RTSP Server - Received from Client:");
	    	System.out.println(requestLine);

	    	StringTokenizer tokens = new StringTokenizer(requestLine);
	    	String requestTypeString = tokens.nextToken();

	    	switch (requestTypeString) {
	    		case "SETUP": 		requestType = SETUP;	break;
	    		case "PLAY":		requestType = PLAY;		break;
	    		case "PAUSE":		requestType = PAUSE;	break;
	    		case "TEARDOWN":	requestType = TEARDOWN;	break;
	    	}

	    	if (requestType == SETUP) {
	    		//extract VideoFileName from RequestLine
	    		videoFileName = tokens.nextToken();
	    	}

	    	//parse the SeqNumLine and extract CSeq field
	    	String seqNumLine = rtspReader.readLine();
	    	System.out.println(seqNumLine);
	    	tokens = new StringTokenizer(seqNumLine);
	    	tokens.nextToken();
	    	rtspSeqNumber = Integer.parseInt(tokens.nextToken());
		
	    	//get LastLine
	    	String lastLine = rtspReader.readLine();
	    	System.out.println(lastLine);

	    	if (requestType == SETUP) {
	    		//extract RTP_dest_port from LastLine
	    		tokens = new StringTokenizer(lastLine);
	    		for (int i=0; i<3; i++) {
	    			tokens.nextToken(); //skip unused stuff
	    		}
	    		rtpDestPort = Integer.parseInt(tokens.nextToken());
	    	}
	    	//else LastLine will be the SessionId line ... do not check for now.
	    } catch(Exception ex) {
	    	System.out.println("Exception caught: "+ex);
	    	System.exit(0);
	    }
	    return(requestType);
	  }

	  //------------------------------------
	  //Send RTSP Response
	  //------------------------------------
	private void sendRtspResponse() {
		try {
			rtspWriter.write("RTSP/1.0 200 OK"+CRLF);
			rtspWriter.write("CSeq: "+rtspSeqNumber+CRLF);
			rtspWriter.write("Session: "+sessionID+CRLF);
			rtspWriter.flush();
			//System.out.println("RTSP Server - Sent response to Client.");
		} catch(Exception ex) {
			System.out.println("Exception caught: "+ex);
			System.exit(0);
		}
	}
	
	
	/*
	 * Other stuff
	 */
	/**
	 * Create a frame packet
	 * @return The packet or null
	 */
	public RTPpacket createFramePacket() {
		try {
			int imageLength = video.getnextframe(buffer);
			RTPpacket packet = new RTPpacket(MJPEG_TYPE, imageNumber, imageNumber * 40 /* Frame Speed */, buffer, imageLength);
			return packet;
		} catch (Exception e) {
			state = FAILED;
			return null;
		}
		
	}
	
	
	/**
	 * Get the state of the client
	 * @return the state
	 */
	public int getState() {
		return state;
	}
	
	/**
	 * Get the Session ID of the client
	 * @return the ID
	 */
	public int getSessionID() {
		return sessionID;
	}
	
	/**
	 * Increment the Image Number
	 * @return is frame left
	 */
	public boolean incrementImageNumber() {
		if (imageNumber < videoLength) {
			imageNumber++;
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Get the client IP address
	 * @return the address
	 */
	public InetAddress getClientIPAddr() {
		return clientIPAddr;
	}
	
	/**
	 * Get the destination port
	 * @return portnumber
	 */
	public int getRtpDestPort() {
		return rtpDestPort;
	}
	
}
