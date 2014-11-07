package com.jdsu.ranadvisor.geotagging;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends Activity implements 
	LocationListener, ConnectionCallbacks, OnConnectionFailedListener {
	
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            1000 * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            1000 * FASTEST_INTERVAL_IN_SECONDS;
    
	private GoogleMap map;
	private LatLng currentLoc = null;
	private Marker gpsMarker;
	private boolean mStopped = false;
	private boolean gpson = true;
	private int mapType = GoogleMap.MAP_TYPE_NORMAL;
	private List<Marker> cellsiteMarkers;
	private Thread gpsAnimationThread;
	private GoogleApiClient mLocationClient;
	private LocationRequest mLocationRequest;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mLocationClient = new GoogleApiClient.Builder(getApplicationContext())
        	.addApi(LocationServices.API).addConnectionCallbacks(this)
        	.addOnConnectionFailedListener(this).build();
	    
		map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		
		final ImageButton ibtn = (ImageButton)findViewById(R.id.mapTypeBtn);
		ibtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if(mapType == GoogleMap.MAP_TYPE_NORMAL) {
					mapType = GoogleMap.MAP_TYPE_SATELLITE;
					ibtn.setImageResource(R.drawable.satellite);
				}
				else {
					mapType = GoogleMap.MAP_TYPE_NORMAL;
					ibtn.setImageResource(R.drawable.map);
				}
				
				map.setMapType(mapType);
			}
			
		});
		
		final ImageButton gbtn = (ImageButton)findViewById(R.id.gpsBtn);
		gbtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				gpson = !gpson;
				if(gpson) {
					gbtn.setImageResource(R.drawable.gpson);
					if(gpsMarker!=null) 
						gpsMarker.remove();
					gpsMarker = map.addMarker(new MarkerOptions().position(currentLoc)
							.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_89a_crosshair)));
				}
				else {
					gpsAnimationThread.interrupt();
					gbtn.setImageResource(R.drawable.gpsoff);
					gpsMarker.setDraggable(true);
				}
			}
			
		});
		
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
		
        startGpsAnimationThread();
    }
	
    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mLocationClient.connect();
    }
    
    @Override
    protected void onStop() {

        if (mLocationClient.isConnected()) {
        	LocationServices.FusedLocationApi.removeLocationUpdates(mLocationClient, this);
        }

        mLocationClient.disconnect();
        super.onStop();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CONNECTION_FAILURE_RESOLUTION_REQUEST :
            /*
             * If the result code is Activity.RESULT_OK, try
             * to connect again
             */
                switch (resultCode) {
                    case Activity.RESULT_OK :
                    	mLocationClient.connect();
                    break;
                }

        }
     }
    
    @Override
	public void onLocationChanged(Location loc) {
		if(!gpson || loc==null)
			return;
		
		if(currentLoc == null) {
			currentLoc = new LatLng(loc.getLatitude(), loc.getLongitude());
			
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLoc, 17));
		}
		else {
			currentLoc = new LatLng(loc.getLatitude(), loc.getLongitude());
		}
		
		if(gpsMarker!=null) 
			gpsMarker.remove();

		gpsMarker = map.addMarker(new MarkerOptions().position(currentLoc)
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_89a_crosshair)));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	private void loadCellSites() {
		String loc = Environment.getExternalStorageDirectory().getAbsolutePath() 
				+ File.separator + "OutDoor" + File.separator + "cellsites" + File.separator;
		File folder = new File(loc);
		if(!folder.exists())
			folder.mkdirs();
		
		File sf = new File(loc);    
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".xml");
			}
			
		};
		final File[] files = sf.listFiles(filter);
		String[] items = new String[files.length];

	    for (int i=0; i < files.length; i++)
	    	items[i] = files[i].getName();
	    
	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    if(items.length > 0) {
	        builder.setTitle("Load Cellsite")
	        	.setAdapter(new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, items), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface arg0, int id) {
						loadCellsiteFile(files[id]);
					}
				});
	    }
	    else {
	        builder.setTitle("Load Cellsite")
	        	.setMessage("No cellsite file found!");

	    }

	    builder.create().show();
	}
	
	private void loadCellsiteFile(File file) {
		CellsiteXmlParser xmlParser = new CellsiteXmlParser();
		List<Cellsite> cellsites = xmlParser.parse(file);
		
		if(cellsiteMarkers == null)
			cellsiteMarkers = new ArrayList<Marker>();
		
		if(cellsites!=null) {
			for(Cellsite cs:cellsites) {
				LatLng pos = new LatLng(cs.lat, cs.lon);
				Marker csm = map.addMarker(new MarkerOptions().position(pos)
						.title(cs.tech + " channel:" + cs.channelValue + " code:" + cs.channelCode)
						.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_50_macro_tower_3)));
				cellsiteMarkers.add(csm);
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.settings_load_cellsites:
			loadCellSites();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDestroy() {
		mStopped = true;

		super.onDestroy();
	}

	private void startGpsAnimationThread() {
		final ArrayList<BitmapDescriptor> pointStyles = new ArrayList<BitmapDescriptor>();
		pointStyles.add(BitmapDescriptorFactory.fromResource(R.drawable.ic_89a_crosshair));
		pointStyles.add(BitmapDescriptorFactory.fromResource(R.drawable.ic_89b_crosshair));
		pointStyles.add(BitmapDescriptorFactory.fromResource(R.drawable.ic_89c_crosshair));
		pointStyles.add(BitmapDescriptorFactory.fromResource(R.drawable.ic_89d_crosshair));
		
		gpsAnimationThread = new Thread() {
			int crosshairIndex = 0;
			
			public void run() {
				while (!mStopped) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
									
							if(gpson && crosshairIndex<pointStyles.size())
								if(gpsMarker!=null)
									gpsMarker.setIcon(pointStyles.get(crosshairIndex));
							
						}
					});

					try {
						Thread.sleep(1000 / 5); // target fps
					} 
					catch (InterruptedException e) { }
					
					if(crosshairIndex >= pointStyles.size()-1)
						crosshairIndex = 0;
					else
						crosshairIndex++;
				}
			}
		};
		
		gpsAnimationThread.start();
	}

	@Override
	public void onConnected(Bundle arg0) {
		onLocationChanged(LocationServices.FusedLocationApi.getLastLocation(mLocationClient));
		LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, mLocationRequest, this);
	}


	@Override
	public void onConnectionSuspended(int arg0) {

	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            showErrorDialog(connectionResult.getErrorCode());
        }
	}
	
	private void showErrorDialog(int errorcode) {
        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
        		errorcode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {
            // Create a new DialogFragment for the error dialog
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();
            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);
            // Show the error dialog in the DialogFragment
            errorFragment.show(getFragmentManager(),
                    "Location Updates");
        }
	}
	
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;
        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }
        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }
        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
}