package nl.stoux.peer2peerstreaming.data;

import android.os.Parcel;
import android.os.Parcelable;

public class StreamingPeer implements Parcelable {

	//Connection wise
	private String peerIP; //The IP for the peer
	private int rtspPort; //Port for the TCP protocol
	
	//Content
	private String contentDescription;
	private String filename;
	
	public StreamingPeer(String peerIP, int rtspPort, String contentDescription, String filename) {
		this.peerIP = peerIP;
		this.rtspPort = rtspPort;
		this.contentDescription = contentDescription;
		this.filename = filename;
	}
	
	public String getPeerIP() {
		return peerIP;
	}
	
	public String getContentDescription() {
		return contentDescription;
	}
	
	public String getFilename() {
		return filename;
	}
	
	public int getRtspPort() {
		return rtspPort;
	}
	
	
	
	//Parcable	
	public StreamingPeer(Parcel parcel) {
		this.peerIP = parcel.readString();
		this.rtspPort = parcel.readInt();
		this.contentDescription = parcel.readString();
		this.filename = parcel.readString();
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(peerIP);
		dest.writeInt(rtspPort);
		dest.writeString(contentDescription);
		dest.writeString(filename);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	
	   public static final Parcelable.Creator<StreamingPeer> CREATOR = new Parcelable.Creator<StreamingPeer>() {
	        public StreamingPeer createFromParcel(Parcel in) {
	            return new StreamingPeer(in);
	        }

	        public StreamingPeer[] newArray(int size) {
	            return new StreamingPeer[size];
	        }
	    };
	
	
		
}
