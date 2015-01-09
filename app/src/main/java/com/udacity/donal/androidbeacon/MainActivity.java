package com.udacity.donal.androidbeacon;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.worklight.wlclient.api.WLClient;
import com.worklight.wlclient.api.WLFailResponse;
import com.worklight.wlclient.api.WLProcedureInvocationData;
import com.worklight.wlclient.api.WLRequestOptions;
import com.worklight.wlclient.api.WLResponse;
import com.worklight.wlclient.api.WLResponseListener;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;


public class MainActivity extends Activity implements BeaconConsumer {

    protected static final String TAG = MainActivity.class.getName();
    private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
    // todo - validation of beacon id will be required throu mfps
    final String BEACON1_ID = "f0018b9b-7509-4c31-a905-1a27d39c003c";
    JSONObject wlBeacons = new JSONObject();
    boolean BEACON_EXISTS = false;
    private WLClient client;
    JSONObject triggerActionPayload;

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
        client = WLClient.createInstance(this);
//        WLProcedureInvocationData invocationData = new WLProcedureInvocationData("HelloWorld", "hello");
        WLProcedureInvocationData invocationData = new WLProcedureInvocationData("BeaconsAndTriggers", "getAll");
        WLRequestOptions options = new WLRequestOptions();
        options.setTimeout(30000);
        Object[] parameters = new Object[] {"AndroidiBeacon"};
        invocationData.setParameters(parameters);
        WLResponseListener myListerner = new WLResponseListener() {
            @Override
            public void onSuccess(WLResponse wlResponse) {
//                Toast.makeText(getApplicationContext(), wlResponse.toString(), Toast.LENGTH_LONG);
                wlBeacons = wlResponse.getResponseJSON();
//                toastOnUI(wlResponse.toString());
            }

            @Override
            public void onFailure(WLFailResponse wlFailResponse) {
//                Toast.makeText(getApplicationContext(), wlFailResponse.toString(), Toast.LENGTH_LONG);
                toastOnUI(wlFailResponse.toString());

            }
        };
        client.invokeProcedure(invocationData, myListerner, options);


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
                    String beaconUuid = beacon.getId1().toString();
                    BEACON_EXISTS = searchBeacons(beaconUuid);
                    if (BEACON_EXISTS) {
                        Log.v(TAG, "Saving beacon in the map");

                        // now i know hte beacon exists so check the trigger
                        String messageFromServer =  searchTriggers(beaconUuid);
//                        toastOnUI(messageFromServer);
                        String messageFromBeacon = "Hello iBeacon with ID - "+beacon.getId1().toString()
                                +" in region "+ region.getUniqueId()+" with distance of " +beacon.getDistance();
//                        Toast.makeText(getApplicationContext(), "Found the right beacon " +beacon.getId1().toString(), Toast.LENGTH_SHORT).show();
//                        toastOnUI("Found the right beacon in region" +beacon.getId1().toString());
                        updateUI(messageFromBeacon, messageFromServer);
                    }

                } else {
                    // beacon not in collection
                    updateUI("No iBeacon in Range", "No Server Messages");

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

    private String searchTriggers(String uuid) {
        try {
            //get the trigger name
            JSONArray beaconTriggerAssociationsArray = wlBeacons.getJSONArray("beaconTriggerAssociations");
            String triggerName = "";
            for (int i = 0; i < beaconTriggerAssociationsArray.length(); i++) {
                JSONObject beaconObj = beaconTriggerAssociationsArray.getJSONObject(i);
                if (beaconObj.getString("uuid").equals(uuid)) {
                    Log.v(TAG, "found beacon matching server list on uuid "+uuid);
                    triggerName = beaconObj.getString("triggerName");
                }
                else return "No Trigger Association Found";
            }
            // get the trigger event from the associated trigger
            JSONArray beaconTriggersArray = wlBeacons.getJSONArray("beaconTriggers");
            for (int j = 0; j < beaconTriggersArray.length(); j++) {
                JSONObject beaconObj = beaconTriggersArray.getJSONObject(j);
                if (beaconObj.getString("triggerName").equals(triggerName)) {
                    Log.v(TAG, "found beacon trigger named "+triggerName);
                    String actionPayload = beaconObj.getJSONObject("actionPayload").getString("alert");
                    Log.v(TAG, "action payload is  "+actionPayload);
                    return  actionPayload;
                }
                else return "No Trigger Action Found";

            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.v(TAG, "JSON beacon triggers array issue");
        }
        return "some other non trigger error";

    };

    @Override
    protected void onResume() {
        super.onResume();
        // handle adapter call
    }

    private void toastOnUI (final String message){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });

    };

    private boolean searchBeacons(String uuid){

        try {
            JSONArray beaconArray = wlBeacons.getJSONArray("beacons");
            for (int i = 0; i < beaconArray.length(); i++) {
                    JSONObject beaconObj = beaconArray.getJSONObject(i);
                    if (beaconObj.getString("uuid").equals(uuid)) {
                        Log.v(TAG, "found beacon matching server list on uuid "+uuid);
                        return true;
                    }
                    else return false;
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.v(TAG, "JSON beacons array issue");
        }
        return false;
    };

    private void updateUI (final String beaconMessage, final String serverMessage){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                TextView serverMessageTextView  = (TextView) findViewById(R.id.main_activity_server_message);
//                serverMessageTextView.setText("THIS IS A TEST");
                serverMessageTextView.setText(serverMessage);

                TextView beaconMessageTextView = (TextView) findViewById(R.id.main_activity_text_view);
                beaconMessageTextView.setText(beaconMessage);

            }
        });
    };
}
