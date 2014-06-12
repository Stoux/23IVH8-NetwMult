package nl.stoux.peer2peerstreaming;

import nl.stoux.peer2peerstreaming.data.DiscoveryServer;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class DiscoveryServerActivity extends Activity {

	private Context context;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_discovery_server);
		
		this.context = this;
		
		//Get views
		final EditText ipEdit = (EditText) findViewById(R.id.settings_ip_adress_input);
		final EditText portEdit = (EditText) findViewById(R.id.settings_port_input);
		Button saveButton = (Button) findViewById(R.id.settings_save_button);
		
		//Set data
		ipEdit.setText(DiscoveryServer.SERVER_IP);
		portEdit.setText("" + DiscoveryServer.SERVER_PORT);
		
		//Button listener
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (ipEdit.getText().length() == 0 || portEdit.getText().length() == 0) { //Check if filled in
					Toast.makeText(context, "Vul alle velden in!", Toast.LENGTH_LONG).show();
				} else {
					//Save data
					DiscoveryServer.SERVER_IP = ipEdit.getText().toString();
					DiscoveryServer.SERVER_PORT = Integer.parseInt(portEdit.getText().toString());
					
					//=> Save
					DiscoveryServer.saveSettings(context);
					
					//Notify user
					Toast.makeText(context, "Opgeslagen!", Toast.LENGTH_LONG).show();
					
					//Finish
					finish(); 
				}
			}
		});
		
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
		return super.onOptionsItemSelected(item);
	}

}
