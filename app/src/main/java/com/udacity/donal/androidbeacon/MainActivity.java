package com.udacity.donal.androidbeacon;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;


public class MainActivity extends Activity implements BeaconConsumer {

    protected static final String TAG = MainActivity.class.getName();
    private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
    // todo - validation of beacon id will be required throu mfps
    final String BEACON1_ID = "f0018b9b-7509-4c31-a905-1a27d39c003c";

    // perhaps add the wl call for list of beacons inside on resume hense its updated when /
    // the activity comes back into play

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        beaconManager
                .getBeaconParsers()
                .add(new BeaconParser()
                        // this is a specific parser for the manuf of the ibeacon - look at docsø
                        .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        beaconManager.bind(this);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBeaconServiceConnect() {

        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
//                toastOnUI("didRangeBeaconsInRegion");
                // if the collection contains a beacon
                if (beacons.size() > 0) {
                    Beacon beacon = beacons.iterator().next();
                    Log.v(TAG,
                            "The first beacon I see is about "
                                    + beacon.getDistance()
                                    + " meters away. Region is "
                                    + region.getUniqueId());
                    // at this point iterate over the list of beacons stored as some kinda collection
                    // ie an array list and check if it matches
                    if (beacon.getId1().toString().equals(BEACON1_ID)) {
                        Log.v(TAG, "Saving beacon in the map");
//                        Toast.makeText(getApplicationContext(), "Found the right beacon " +beacon.getId1().toString(), Toast.LENGTH_SHORT).show();
//                        toastOnUI("Found the right beacon in region" +beacon.getId1().toString());
                        updateUI("Hello iBeacon with ID - "+beacon.getId1().toString()+" in region "+ region.getUniqueId()
                                +" with distance of " +beacon.getDistance() );
                    }

                } else {
                    // beacon not in collection
                    updateUI("No iBeacon in Range ");

                };

            }
        });


        beaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                Log.v(TAG, "I JUST didEnterRegion");

//                Toast.makeText(getApplicationContext(), "You have entered the region", Toast.LENGTH_SHORT).show();
                toastOnUI("You have entered the region");
//                updateUI("Hello iBeacon - in region"+region.getUniqueId());


            }

            @Override
            public void didExitRegion(Region region) {

                Log.v(TAG, "I didExitRegion");

//              Toast.makeText(getApplicationContext(), "You have exited the region", Toast.LENGTH_SHORT).show();
                toastOnUI("You have exited the region");
//                updateUI("Goodbye iBeacon - in region"+region.getUniqueId());

            }

            @Override   //
            public void didDetermineStateForRegion(int state, Region region) {

                Log.v(TAG, "I didDetermineStateForRegion");
                if (MonitorNotifier.INSIDE == state) {
//                    Toast.makeText(getApplicationContext(), "You have switched state to (INSIDE) "+state, Toast.LENGTH_SHORT).show();
//                    toastOnUI("You have switched state to (INSIDE) "+state);
                } else {
//                    Toast.makeText(getApplicationContext(), "You have switched state to (NOT-INSIDE) "+state, Toast.LENGTH_SHORT).show();
//                    toastOnUI("You have switched state to (NOT-INSIDE) "+state);
                }

            }
        });

        Region region = new Region("com.udacity.donal.androidbeacon", Identifier.parse(BEACON1_ID), null, null);
        try {
            beaconManager.startMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void toastOnUI (final String message){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });

    };

    private void updateUI (final String message){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) findViewById(R.id.main_activity_text_view);
                textView.setText(message);
            }
        });
    };
}
