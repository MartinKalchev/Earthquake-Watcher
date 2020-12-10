package com.example.earthquakewatcher.Activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.earthquakewatcher.Model.EarthQuake;
import com.example.earthquakewatcher.R;
import com.example.earthquakewatcher.UI.CustomInfoWindow;
import com.example.earthquakewatcher.Util.Constants;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMarkerClickListener  {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    public double latitude;
    public double longitude;
    private RequestQueue queue;                  // The Network requests are added here
    private AlertDialog.Builder dialogBuilder;   // Builder for the dialog window
    private AlertDialog dialog;                  // variable for the dialog window itself
    private BitmapDescriptor [] iconColors;      // array of the color markers displaying the earthquakes on the map
    private Button showListBtn;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        showListBtn = (Button) findViewById(R.id.showListBtn);

        showListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MapsActivity.this, QuakesListActivity.class));  // Displays a list of all the earthquakes displayed with markers on the map (summary) on the QuakesListActivity
            }
        });

        iconColors = new BitmapDescriptor[] {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
                //BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
        };

        queue = Volley.newRequestQueue(this);     // Instantiate the queue and get it ready for HTTP requests
        
        getEarthQuakes();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setInfoWindowAdapter(new CustomInfoWindow(getApplicationContext()));
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerClickListener(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {

            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {

            }
        };

        // Ask for permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)     //Check if the app is allowed to access precise location.
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        else {
            // we have permission
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                Log.e("TAG", "GPS is on");
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }
            LatLng latLng = new LatLng(latitude, longitude);      // Store the longitude and latitude variables
            mMap.addMarker(new MarkerOptions()                    // adding the markers on the map, the position and a title
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    .title("EarthquakeWatcher"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 3));        // Setting the zoom level of the markers on the map
        }
    }


    // Checks if the request to the network was successful
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0]
                == PackageManager.PERMISSION_GRANTED) {

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED)

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
    }

    // This function gets the earthquake data like location, coordinates and magnitude and displays it with markers on the map
    public void getEarthQuakes() {

        EarthQuake earthQuake = new EarthQuake();         // Create an instance of the earthquake class


        // Gets the contents of the url from which we are parsing the json data for the earthquakes
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, Constants.URL,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        // Here we are parsing the list of the most recent quakes which happend today
                        try {
                            JSONArray features = response.getJSONArray("features");
                            for (int i = 0; i < Constants.LIMIT; i++) {
                                JSONObject properties = features.getJSONObject(i).getJSONObject("properties");

                                //Get geometry object
                                JSONObject geometry = features.getJSONObject(i).getJSONObject("geometry");

                                // Get coordinates array
                                JSONArray coordinates = geometry.getJSONArray("coordinates");

                                double lon = coordinates.getDouble(0);
                                double lat = coordinates.getDouble(1);

                                //Log.d("Quake: ", lon + ", " + lat);
                                // Setting up the properties of the earthquake class from the json documents
                                earthQuake.setPlace(properties.getString("place"));
                                earthQuake.setType(properties.getString("type"));
                                earthQuake.setTime(properties.getLong("time"));
                                earthQuake.setMagnitude(properties.getDouble("mag"));
                                earthQuake.setDetailLink(properties.getString("detail"));

                                java.text.DateFormat dateFormat = java.text.DateFormat.getDateInstance();
                                String formattedDate = dateFormat.format(new Date(properties.getLong("time"))   // Formatting the date so it is displayed correctly and in an understandable manner
                                        .getTime());

                                MarkerOptions markerOptions = new MarkerOptions();

                                markerOptions.icon(iconColors[Constants.randomInt(iconColors.length, 0)]);  // Implementing a random color for the markers on the map
                                markerOptions.title(earthQuake.getPlace());
                                markerOptions.position(new LatLng(lat, lon));
                                markerOptions.snippet("Magnitude: " +
                                        earthQuake.getMagnitude() + "\n" +
                                        "Date: " + formattedDate);


                                // Add circle to markers if the magnitude of an earthquake is > 2
                                if (earthQuake.getMagnitude() >= 2.0) {
                                    CircleOptions circleOptions = new CircleOptions();
                                    circleOptions.center(new LatLng(lat, lon));
                                    circleOptions.radius(30000);
                                    circleOptions.strokeWidth(2.5f);
                                    circleOptions.fillColor(Color.RED);
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

                                    mMap.addCircle(circleOptions);
                                }

                                // Add the marker on the map
                                Marker marker = mMap.addMarker(markerOptions);
                                marker.setTag(earthQuake.getDetailLink());
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 1));


                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(jsonObjectRequest);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

        getQuakeDetails(marker.getTag().toString());

            //Toast.makeText(getApplicationContext(), marker.getTag().toString(), Toast.LENGTH_LONG)
                    //.show();

    }

    // Function which shows a popup window with more information about the earthquake like the date on which it occured and the magnitude
    private void getQuakeDetails(String url) {

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                String detailsUrl = "";
                JSONObject quakeObj = null;

                try {
                    JSONObject properties = response.getJSONObject("properties");
                    JSONObject products = properties.getJSONObject("products");
                    JSONArray cities = products.getJSONArray("nearby-cities");
                    JSONObject contentsObj = cities.getJSONObject(0);
                    JSONObject contents = contentsObj.getJSONObject("contents");
                    quakeObj = contents.getJSONObject("nearby-cities.json");

                    detailsUrl = quakeObj.getString("url");

                    Log.d("URL: ", detailsUrl);

                    getMoreDetails(detailsUrl);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(jsonObjectRequest);
    }


    // Function which shows a popup window with more detailed information about the earthquake like the distance to the nearest cities from it (this is shown if this property list is available in the json document)
    public void getMoreDetails(String url) {
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET,
                url, (Response.Listener<JSONArray>) response -> {

                        // Accessing the properties of the popup menu with the further details
                        dialogBuilder = new AlertDialog.Builder(MapsActivity.this);
                        View view = getLayoutInflater().inflate(R.layout.popup, null);
                        Button dismissButton = (Button) view.findViewById(R.id.dismissPop);
                        Button dismissButtonTop = (Button) view.findViewById(R.id.dismissPopTop);
                        TextView popList = (TextView) view.findViewById(R.id.popList);
                        //WebView htmlPop = (WebView) view.findViewById(R.id.htmlWebView);

                        StringBuilder stringBuilder = new StringBuilder();

                    try {

                        // Getting the properties from the json array and adding them to the string builder
                        for (int i = 0; i < response.length(); i++){
                            JSONObject citiesObj = response.getJSONObject(i);
                            stringBuilder.append("Distance: " + citiesObj.getString("distance")
                                    + "\n" + "City: " + citiesObj.getString("name")
                                    + "\n" + "Direction: " + citiesObj.getString("direction")
                                    + "\n" + "Latitude:" + citiesObj.getString("latitude")
                                    + "\n" + "Longitude" + citiesObj.getString("longitude"));

                            stringBuilder.append("\n\n");
                        }

                        // Setting up the popup window and its butons and displaying it when clicked on
                        popList.setText(stringBuilder);

                        dismissButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    dialog.dismiss();
                            }
                        });

                        dismissButtonTop.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });

                        dialogBuilder.setView(view);
                        dialog = dialogBuilder.create();
                        dialog.show();




                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(jsonArrayRequest);
    }



    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

}