package nl.stoux.peer2peerstreaming;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import nl.stoux.peer2peerstreaming.data.StreamingPeer;
import nl.stoux.peer2peerstreaming.objects.RTPpacket;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class PeerClientActivity extends Activity {

	//Context
	private Context context;
	
	//ImageView for Video
	private ImageView videoView;
	
	//Peer
	private StreamingPeer peer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_peer_client);
		
		this.context = this;
		
		//Get peer
		peer = getIntent().getExtras().getParcelable(PeerListActivity.PEER);
		videoFileName = peer.getFilename();
		
		//Fill data
		TextView fileView = (TextView) findViewById(R.id.filename_at_ip_text);
		fileView.setText(peer.getFilename() + "@" + peer.getPeerIP() + ":" + peer.getRtspPort());
		
		//Get ImageView for Video
		videoView = (ImageView) findViewById(R.id.movie_stream);
		
		//Set buttons
		setButtonState(R.id.pause_button, false);
		setButtonState(R.id.start_button, false);
		setButtonState(R.id.teardown_button, false);
		setButtonState(R.id.setup_button, false);
		
		//Setup
		setup();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.empty_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
			
	@Override
	public void onBackPressed() {
		stop();
	}	
	
	private void setButtonState(int buttonID, boolean enabled) {
		//Get button
		View view = findViewById(buttonID);
		
		//Set state
		view.setEnabled(enabled);
		
		//Set background color
		int color = (enabled ? Color.WHITE : Color.GRAY);
		view.setBackgroundColor(color);
	}
	
	/**
	 * Set the button state on the UI thread.
	 * Use this when called on another thread.
	 * @param buttonID The ID of the button
	 * @param enabled The new state of the button
	 */
	private void postButtonState(final int buttonID, final boolean enabled) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Log.i("StreamingTest", "Posting button state. ID: " + buttonID + " | En: "+ enabled);
				setButtonState(buttonID, enabled);
			}
		});
	}
	
	/**
	 * Change the State Text
	 * @param onUI Needs to be run on UI thread
	 * @param text The text
	 */
	private void setStateText(boolean onUI, final String text) {
		if (onUI) {
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					((TextView) findViewById(R.id.state_text)).setText("Status: " + text);
				}
			});
		} else {
			((TextView) findViewById(R.id.state_text)).setText("Status: " + text);
		}
	}
	
	/*
	 * Button reacts
	 */
	
	public void onSetupButton(View view) {
		Log.i("StreamingTest", "ID Check: " + (R.id.setup_button == view.getId()));
		if (state == INIT) {
			setButtonState(R.id.setup_button, false); //Disable this button
			rtspHandler.sendMessage(createMessage("SETUP")); //Send SETUP message
		}
	}
	
	public void onStartButton(View view) {
		if (state == READY) {
			setButtonState(R.id.start_button, false); //Disable this button
			rtspHandler.sendMessage(createMessage("PLAY")); //Send PLAY message
		}
	}
	
	public void onPauseButton(View view) {
		if (state == PLAYING) {
			setButtonState(R.id.pause_button, false); //Disable this button
			rtspHandler.sendMessage(createMessage("PAUSE")); //Send PAUSE message
		}
	}
	
	public void onTeardownButton(View view) {
		if (state == READY || state == PLAYING) {
			setButtonState(R.id.teardown_button, false); //Disable this button
			rtspHandler.sendMessage(createMessage("TEARDOWN")); //Send TEARDOWN message
		}
	}
	
	
	
	/**
	 * Create a message.
	 * Add requestType as obj.
	 * @param requestType The requestType (SETUP, PLAY, PAUSE, TEARDOWN)
	 * @return The message
	 */
	private Message createMessage(String requestType) {
		Message message = new Message();
		message.obj = requestType;
		return message;
	}
	
	
	
	/*
	 ***************
	 * Client data *
	 ***************
	 */
	//	=> RTP Vars
	private DatagramPacket recievedPacket;
	private DatagramSocket rtpSocket;
	private byte[] buf; //buffer used to store data received from the server 
	
	
	//	=> Constants
	private final static int RTP_RCV_PORT = 8080; //Recieving port
	//		=> RTSP States
	private final static int INIT = 0;
	private final static int READY = 1;
	private final static int PLAYING = 2;
	
		
	//	=> RTSP Connection
	private Socket rtspSocket;
	private BufferedReader rtspBufferedReader;
	private BufferedWriter rtspBufferedWriter;
	
	//	=> RTSP Vars
	private String videoFileName; //Requested file name
	private int rtspSeqNumber; //Sequence number of RTSP messages
	private int rtspID; //ID of RTSP session
	private int state = -1;
	
	
	//	=> Threads & Handlers
	private HandlerThread tcpThread; //<= For sending & incoming RTSP messages
	private Handler rtspHandler; //<= Handler for tcpThread
	private Thread udpThread; //<= For receiving RTP Packets
	private RTPRunnable rtpRunnable;
	
	/*
	 ********************
	 * Client Functions *
	 ********************
	 */
	
	private void stop() {
		//Try to close sockets & stop threads
		try { rtspSocket.close(); } catch (Exception e) {}
		try { rtpSocket.close(); } catch (Exception e) {}
		try { udpThread.interrupt(); } catch (Exception e) {}
		try { tcpThread.interrupt(); } catch (Exception e) {}
		try { rtpRunnable.timer.cancel(); } catch (Exception e) {}
		
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				finish();
			}
		});
	}
		
	/**
	 * Setup the client
	 */
	private void setup() {
		
	    //allocate enough memory for the buffer used to receive data from the server
	    buf = new byte[15000];    
	    	   		
		//Create thread to setup TCP connection
		new Thread(new Runnable() {
			@Override
			public void run() {
				int rtspPort = peer.getRtspPort();
				String host = peer.getPeerIP();
				Log.i("StreamingTest", "RTSP Socket setup thread started!");
				
				try {
					//Create socket
					rtspSocket = new Socket(host, rtspPort);
					Log.i("StreamingTest", "RTSP Socket created!");
					
					//Create reader & writer
					rtspBufferedReader = new BufferedReader(new InputStreamReader(rtspSocket.getInputStream()));
					rtspBufferedWriter = new BufferedWriter(new OutputStreamWriter(rtspSocket.getOutputStream()));
					Log.i("StreamingTest", "RTSP Socket reader & writer created!");
				} catch (Exception e) {
					Log.w("StreamingTest", "Failed to setup socket.");
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							Toast.makeText(context, "Kon geen verbinding maken!", Toast.LENGTH_LONG).show();
						}
					});
					stop();
				}
								
				Log.i("StreamingTest", "RTSP Socket setup!");
			}
		}).start();
				
		
		//Create TCP thread
		tcpThread = new HandlerThread("RTSP Thread");
		//	=> Start TCP thread
		tcpThread.start();
		Log.i("StreamingTest", "Started RTSP handler thread.");
		//	=> Get handler from TCP thread
		rtspHandler = new Handler(tcpThread.getLooper()){
			@Override
			public void handleMessage(Message msg) {
				Log.i("StreamingTest", "[RTSP Handler] Handling message: " + msg.obj);
				rtspSeqNumber++; //Increase number
				
				//Get requestType from message
				String requestType = (String) msg.obj;
				
				//Send RTSP request
				send_RTSP_request(requestType);
				
				//Wait for a response
				if (parse_server_response() != 200) {
					Log.w("StreamingTest", "Invalid server response.");
				} else {
					switch (requestType) {
					case "SETUP":
						state = READY;
						Log.w("StreamingTest", "State: READY");
						setStateText(true, "Klaar om af te spelen");
						
						//Change button states
						postButtonState(R.id.teardown_button, true);
						postButtonState(R.id.start_button, true);
						break;
						
					case "PLAY":
						state = PLAYING;
						Log.w("StreamingTest", "State: PLAYING");
						setStateText(true, "Speelt af");
						
						//Unpause the timer
						rtpRunnable.setPaused(false);
						
						//Change button state
						postButtonState(R.id.pause_button, true);
						break;
					 
					case "PAUSE":
						state = READY;
						Log.w("StreamingTest", "State: READY");
						setStateText(true, "Gepauzeerd");
						
						//Pause the timer
						rtpRunnable.setPaused(true);
						
						//Change button state
						postButtonState(R.id.start_button, true);
						break;
						
					case "TEARDOWN":
						state = -1;
						Log.w("StreamingTest", "State: TEARDOWN");
						
						//Full stop
						setResult(RESULT_OK);
						stop();
						break;
					}
				}
				
			}
			
			  private void send_RTSP_request(String request_type) {
				  try {
				      //Use the RTSPBufferedWriter to write to the RTSP socket
				
				      //write the request line:
				      rtspBufferedWriter.write(request_type + " " + videoFileName + " RTSP/1.0" + "\n");
				
				      //write the CSeq line: 
				      rtspBufferedWriter.write("CSeq: " + rtspSeqNumber + "\n");
				
				      //check if request_type is equal to "SETUP" and in this case write the Transport: line advertising to the server the port used to receive the RTP packets RTP_RCV_PORT
				      rtspBufferedWriter.write((request_type.equals("SETUP") ? "Transport: RTP/UDP; client_port= " + RTP_RCV_PORT : "Session: " + rtspID) + "\n");
				
				      //Flush written text
				      rtspBufferedWriter.flush();
				  }	catch(Exception ex) {
					  System.out.println("Exception caught: "+ex);
					  runOnUiThread(new Runnable() {
							
						@Override
						public void run() {
							Toast.makeText(context, "En toen was die kapot.", Toast.LENGTH_LONG).show();
						}
					  });
					  stop();
			      }
			  }
			
			  private int parse_server_response() {
				  int reply_code = 0;
				  try {
					  //parse status line and extract the reply_code:
				      String StatusLine = rtspBufferedReader.readLine();
				      //System.out.println("RTSP Client - Received from Server:");
				      System.out.println(StatusLine);
				    
				      StringTokenizer tokens = new StringTokenizer(StatusLine);
				      tokens.nextToken(); //skip over the RTSP version
				      reply_code = Integer.parseInt(tokens.nextToken());
				      
				      //if reply code is OK get and print the 2 other lines
				      if (reply_code == 200) {
						  String SeqNumLine = rtspBufferedReader.readLine();
						  System.out.println(SeqNumLine);
						  
						  String SessionLine = rtspBufferedReader.readLine();
						  System.out.println(SessionLine);
						
						  //if state == INIT gets the Session Id from the SessionLine
						  tokens = new StringTokenizer(SessionLine);
						  tokens.nextToken(); //skip over the Session:
						  rtspID = Integer.parseInt(tokens.nextToken());
				      }
				  } catch(Exception ex) {
					  System.out.println("Exception caught: "+ex);
					  runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
							Toast.makeText(context, "En toen was die kapot.", Toast.LENGTH_LONG).show();
						}
					  });
					  stop();
			      }
				  return(reply_code);
			  }
			
		};
		
		
		//Create UDP thread
		udpThread = new Thread(rtpRunnable = new RTPRunnable());
		udpThread.start();
		
		//Create Control thread
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				Log.i("StreamingTest", "[Control Thread] Waiting for setup.");
				//Infinite loop wait until fully setup
				while(videoFileName == null || !rtpRunnable.isSetup()) {
					//Wait
				}
				Log.i("StreamingTest", "[Control Thread] Setup!");
				
				//Set state
				state = INIT;
				setStateText(true, "Verbonden");
				
				//Enable setup button
				postButtonState(R.id.setup_button, true);
				Log.i("StreamingTest", "[Control Thread] Enabled setup button.");
			}
		}).start();
		
	}
	
	/**
	 * Class for running RTP thread
	 */
	private class RTPRunnable implements Runnable {
		
		public boolean setup = false;
		public boolean paused = true;
		private Timer timer;
		
		@Override
		public void run() {
			//Create RTP Socket
			try {
				Log.i("StreamingTest", "[RTP] Creating Socket.");
				rtpSocket = new DatagramSocket(RTP_RCV_PORT); //Create socket
				rtpSocket.setSoTimeout(5); //Set timeout
				Log.i("StreamingTest", "[RTP] Socket created.");
			} catch (SocketException e) {
				Log.w("StreamingTest", "Failed to create UDP socket");
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						Toast.makeText(context, "Kon geen verbinding maken!", Toast.LENGTH_LONG).show();
					}
				  });
				stop();
			}
			
			//init seq number
			rtspSeqNumber = 1;
			
			//Create timer
			Log.i("StreamingTest", "[RTP] Creating timer");
			timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask() {
									
				@Override
				public void run() {
					//Do nothing if paused
					if (paused) return;
					
					//Create new packet to receive data with
					recievedPacket = new DatagramPacket(buf, buf.length);
					
					try {
						//Receive packet
						rtpSocket.receive(recievedPacket);
						
						//Create RTPPacket from DP
						RTPpacket rtpPacket = new RTPpacket(recievedPacket.getData(), recievedPacket.getLength());
						
						//Get payload
						final int payloadLength = rtpPacket.getpayload_length();
						final byte[] payload = new byte[payloadLength];
						rtpPacket.getpayload(payload);
						
						//Post the image
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								//Create bitmap from payload
								Bitmap bmp = BitmapFactory.decodeByteArray(payload, 0, payloadLength);
								//Set image as current
								videoView.setImageBitmap(bmp);
							}
						});
					} catch (InterruptedIOException e) {
						//Nothing to read
					} catch (IOException e) {
						Log.w("StreamingTest", "Exception caught recieving mesage! " + e);
					}
				}
			}, 20, 20);
			
			
			setup = true;
		}
		
		/**
		 * Thread is fully setup
		 * @return isSetup
		 */
		public boolean isSetup() {
			return setup;
		}
		
		/**
		 * Set paused
		 * @param paused
		 */
		public void setPaused(boolean paused) {
			this.paused = paused;
		}
		
	}

}
