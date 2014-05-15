package nl.stoux.streamingserver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import nl.stoux.streamingserver.server.RTPpacket;
import nl.stoux.streamingserver.server.ServerClient;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainActivity extends Activity {

	private long lastChanged = 0;
	private boolean send = false;
	private byte[] latest = new byte[1];
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		surface = (SurfaceView) findViewById(R.id.surface_view);
		previewHandler = new PreviewHandler();
		
		setup();
	}
	
	
	
	private class PreviewHandler implements PreviewCallback, SurfaceHolder.Callback{
		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			
			
			if (System.currentTimeMillis() - lastChanged > 39) {
				Size s = camera.getParameters().getPreviewSize();
				YuvImage yuv = new YuvImage(data, ImageFormat.NV21, s.width, s.height, null);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				yuv.compressToJpeg(new Rect(0, 0, s.width, s.height), 50, baos);
				byte[] nData = baos.toByteArray();
				
				/*
				Bitmap bitmap = BitmapFactory.decodeByteArray(nData, 0, nData.length);
				try {
					bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Test.jpg")));
				} catch (Exception e) {
					Log.w("StreamingServer", e.toString());
				}*/
				
				synchronized(latest) {
					latest = nData;
					send = false;
				}
				lastChanged = System.currentTimeMillis();
				
				Log.i("StreamingServer", "Reached - "+ nData.length);
			}
		}
		
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			
			try {
				camera.setPreviewDisplay(holder);
				camera.setPreviewCallback(this);
			} catch (IOException e){}
			
			camera.startPreview();
		}
		
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// TODO Auto-generated method stub
			
		}
	}
	
	private SurfaceView surface;
	private Camera camera;
	private PreviewHandler previewHandler;
	private Size previewSize;
	
	@Override
	protected void onResume() {
		super.onResume();
		this.camera = Camera.open();
		camera.setDisplayOrientation(90);
		
		//Set resu
		Camera.Parameters params = camera.getParameters();
		params.setPreviewFormat(ImageFormat.NV21);
		params.setPreviewFpsRange(25, 25);
		previewSize = params.getPreviewSize();
		
		SurfaceHolder holder = surface.getHolder();
		holder.setSizeFromLayout();
		holder.addCallback(previewHandler);
		
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		surface.getHolder().removeCallback(previewHandler);
		camera.release();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.exit(0);		//LOL YOLO
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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

	/*
	 ***************
	 * Server Data * 
	 *************** 
	 */
	private ConcurrentHashMap<Integer, ServerClient> clientMap; //Map containing all clients
	private ServerSocket serverSocket; //Server socket. Close this to end server
	private DatagramSocket rtpSocket; //Outgoing Datagram Socket for RTP packets
	private Timer timer; //Timer for sending packets
	
	private DatagramPacket sendablePacket; //Sendable DatagramPacket
	
	/*
	 ********************
	 * Server Functions * 
	 ******************** 
	 */
	private void setup() {
		//Create clientmap
		clientMap = new ConcurrentHashMap<>();
		
		//Create new thread to listen to incoming connections
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					//Create listening ServerSocket
					serverSocket = new ServerSocket(8080);
					
					//Create random generator
					Random random = new Random();
					
					//Start listening to IDs
					Socket client;
					while ((client = serverSocket.accept()) != null) { //While being able to accept
						//Generate a random ID
						int sessionID = 0;
						while (sessionID == 0 || clientMap.containsKey(sessionID)) {
							sessionID = random.nextInt(Integer.MAX_VALUE) + 1;
						}
						
						//Log new client
						Log.i("StreamingServer", "Wedjo! New client: " + client.getInetAddress().getHostAddress() + " (S-ID: " + sessionID + ")");
						
						//Create client
						ServerClient sClient = new ServerClient(client, sessionID);
						//Put in map
						clientMap.put(sessionID, sClient);
						//Start client
						new Thread(sClient).start();
					}
				} catch (IOException e) {
					Log.w("StreamingServer", "ServerSocket IO Exception!");
					System.exit(0);
				}
			}
		}).start();
	
		//Create new timer thread
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				//Create Datagram Socket
				try {
					rtpSocket = new DatagramSocket();
				} catch (IOException e) {
					Log.w("StreamingServer", "Failed to create DatagramSocket");
					System.exit(0);
				}
				
				//Create timer
				timer = new Timer();
				timer.scheduleAtFixedRate(new TimerTask() {
					
					@Override
					public void run() {
						if (!send) {
							send = true;
						} else {
							return;
						}
						
						//Check for any clients
						if (clientMap.isEmpty()) return; //No clients
						
						//Create new HashSet containing the clients
						HashSet<ServerClient> clients = new HashSet<>(clientMap.values());
						
						byte[] copiedData;
						synchronized (latest) {
							if (latest.length > 64000) {
								Log.w("StreamingServer", "Size: "+ latest.length);
								System.exit(0);
							}
							copiedData = latest.clone(); 
						}
						
						//Camera Fiddling
						RTPpacket packet = new RTPpacket(27, 0, 0, copiedData, copiedData.length);
						
						//Loop thru Clients
						for (ServerClient client : clients) {
							int clientState = client.getState();
							if (clientState == ServerClient.FAILED || clientState == ServerClient.TEARDOWN) {
								//Is disconnect. Remove from map
								clientMap.remove(client.getSessionID());
							} else if (clientState == ServerClient.PLAYING) {
								//If playing => Send frame
								
								//Commented out due to Camera Fiddling
								/*
								if (client.incrementImageNumber()) { //Check if frames left
									RTPpacket packet = client.createFramePacket(); //Create frame packet
									if (packet != null) { //Null check
										//Get packet bytes
										int packetLength = packet.getlength();
										byte[] packetBytes = new byte[packetLength];
										packet.getpacket(packetBytes);
										
										//Create Datagram packet
										sendablePacket = new DatagramPacket(packetBytes, packetLength, client.getClientIPAddr(), client.getRtpDestPort());
										try {
											//Send packet
											rtpSocket.send(sendablePacket);
										} catch (Exception ex) {
											Log.w("StreamingServer", "Failed to send packet to Session: " + client.getSessionID());
										}
									}
								}
								*/
								
								//Camera fiddling override send
								int packetLength = packet.getlength();
								byte[] packetBytes = new byte[packetLength];
								packet.getpacket(packetBytes);
								
								//Create Datagram packet
								sendablePacket = new DatagramPacket(packetBytes, packetLength, client.getClientIPAddr(), client.getRtpDestPort());
								try {
									//Send packet
									rtpSocket.send(sendablePacket);
									Log.i("StreamingServer", "Package send.");
								} catch (Exception ex) {
									Log.w("StreamingServer", "Failed to send packet to Session: " + client.getSessionID() + " | " + ex);
								}
								
								
								
							}
						}
					}
				}, 0, 40);
			}
		}).start();
	
	}
	
}
