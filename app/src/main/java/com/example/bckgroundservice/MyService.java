package com.example.bckgroundservice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.util.HashMap;
import java.util.Map;

public class MyService extends Service implements LocationListener {
    public static final String API_URL = "https://admin.jahajibd.com/api_req/Ship/GpsTracking";
//    public static final String API_URL = "http://192.168.0.9/shipTracking/post.php";

    private static final int NOTIF_ID = 14;
    private static final String NOTIF_CHANNEL_ID = "CHANNEL_14";
    private Context context;

    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    protected LocationManager locationManager;


    private final Handler handler = new Handler();
    private final Runnable refresher = new Runnable() {
        public void run() {

            startNotification();
            Location location = getCurrentLocation();
            boolean connected = false;
            ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                    connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
                //we are connected to a network
                connected = true;
            }
            else{
                connected = false;
            }

            if (location == null){
                Toast.makeText(context, "Location Not Received.", Toast.LENGTH_LONG).show();
            } else if (!connected){
                Toast.makeText(context, "No Internet Connection.", Toast.LENGTH_LONG).show();
            } else {
                String latitude = String.valueOf(location.getLatitude());
                String longitude = String.valueOf(location.getLongitude());

                String heading = "0.0";

                // getting charge level.
                IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = context.registerReceiver(null, intentFilter);
                int level = 0;
                if (batteryStatus != null) {
                    level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                }

                String battery_level = String.valueOf(level);

                postToServer(context, latitude, longitude, heading, battery_level);
            }

            handler.postDelayed(this, 1000 * 20);

        }
    };

    public MyService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.context = this;

        handler.post(refresher);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }


    @Override
    public void onDestroy() {
        handler.removeCallbacks(refresher);
        if(locationManager != null){
            locationManager.removeUpdates(this);
        }
        super.onDestroy();
    }

    private void startNotification() {
        createNotificationChannel();

        Intent notificationIntent = new Intent(this, MainActivity.class);
//        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        startForeground(NOTIF_ID, new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_directions_boat_black_24dp)
                .setContentTitle("Ship Tracker")
                .setContentText("Tracking is Running in Background...")
//                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_NAME = "My Channel";

            NotificationChannel notificationChannel = new NotificationChannel(NOTIF_CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setLightColor(R.color.colorPrimary);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
//            latitude = location.getLatitude();
//            longitude = location.getLongitude();
        } else {
            Toast.makeText(context, "Location Not Received.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public Location getCurrentLocation() {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        Location location = null;

        if (!isGPSEnabled && !isNetworkEnabled) {
            Toast.makeText(context, "Enable GPS and Inernet Connection", Toast.LENGTH_LONG).show();
        } else {
            if (isGPSEnabled) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(context, "Location Permission Not Granted.", Toast.LENGTH_LONG).show();
                } else {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2, 0, this);

                    if (locationManager != null) {
                        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                        if (location != null) {
//                            latitude = location.getLatitude();
//                            longitude = location.getLongitude();
//                            Toast.makeText(context, "Lat"+latitude+"lon"+longitude,Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Location is Empty.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(context, "LocationManager is Empty.", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Toast.makeText(context, "Please, Enable GPS.", Toast.LENGTH_LONG).show();
            }
            if (isNetworkEnabled && location == null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(context, "Permission Not Granted.", Toast.LENGTH_LONG).show();
                } else {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
                    if (locationManager != null) {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
//                            double latitude = location.getLatitude();
//                            double longitude = location.getLongitude();
//                            Toast.makeText(context, "DONE", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Location is Empty.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(context, "LocationManager is Empty.", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                if (!isNetworkEnabled){
                    Toast.makeText(context, "Please, Enable Internet Connection.", Toast.LENGTH_LONG).show();
                }
            }
        }

        return location;
    }

    private  void postToServer(final Context context, final String latitude, final String longitude, final String heading, final String battery_level){
        StringRequest stringRequest = new StringRequest(Request.Method.POST, MyService.API_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, error+"", Toast.LENGTH_LONG).show();
            }
        }){
            @Override
            protected Map<String, String> getParams(){
                Map<String, String> params = new HashMap<>();

                @SuppressLint("HardwareIds")
                String androidID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

                params.put("serial", androidID);
                params.put("lat", latitude);
                params.put("lng", longitude);
                params.put("heading", heading);
                params.put("crg", battery_level);

                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(60000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(stringRequest);
    }

}
