package edu.uw.spacescout_android;

import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.PreCachingAlgorithmDecorator;
import com.google.maps.android.ui.IconGenerator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import edu.uw.spacescout_android.TouchableWrapper.UpdateMapAfterUserInteraction;
import edu.uw.spacescout_android.model.Building;
import edu.uw.spacescout_android.model.Space;
import edu.uw.spacescout_android.model.Spaces;

/**
 * Created by ajay alfred on 11/5/13.
 * Modified by azri92.
 *
 * This class displays spaces on a Google map and its markers/clusters.
 * Extends a fragment that is embedded onto MainActivity.
 * Implements UpdateMapAfterUserInteraction for callback ethod for Map touch or zoom change.
 * Implements OnMapReadyCallback for callback method for preparing the map.
 *
 */

public class SpaceMapFragment extends Fragment implements OnMapReadyCallback {
    private final String TAG = "SpaceMap";

    // TODO: Should change based on User's preference (campus)
    private LatLng campusCenter;
    private String baseUrl;

    private GoogleMap map;
    private SupportMapFragment mapFragment;
    private View view;

    public TouchableWrapper mTouchView;
    public static boolean mMapIsTouched = false;
    public IconGenerator tc;
    public ClusterManager<Space> mClusterManager;

    public PolylineOptions line;

    public SpaceMapFragment() {
        ///empty constructor required for fragment subclasses
    }

    // This is the default method needed for Android Fragments
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if(mapFragment == null) { // azri92: not sure if this actually helps
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        campusCenter = new LatLng(Float.parseFloat(getActivity().getResources().getString(R.string.default_center_latitude)),
                Float.parseFloat(getActivity().getResources().getString(R.string.default_center_longitude)));
        baseUrl = getResources().getString(R.string.baseUrl);

        tc = new IconGenerator(getActivity());
    }

    @Override
    // Map zoom controls and rotation gesture disabled.
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setZoomControlsEnabled(false);
        uiSettings.setRotateGesturesEnabled(false);
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(campusCenter, 17.2f));

        // this will only be called before overriden by new listener for clustering
        map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
                Log.d(TAG, "gesture detected by onCameraChange");

                spotSearch();
                // Setting up cluster manager with the CustomClusteringAlgorithm Class
                setUpClusterer();
            }
        });
    }

    private void spotSearch() {
        Log.d(TAG, "spotSearch() called");
        // Get the latlng bounds to calculate radius distance
        // TODO: All this code can probably be shortened
        VisibleRegion vr = map.getProjection().getVisibleRegion();
        double rightLat = vr.latLngBounds.northeast.latitude;
        double rightLon = vr.latLngBounds.northeast.longitude;
        Location topRightCorner = new Location("topRightCorner");
        topRightCorner.setLatitude(rightLat);
        topRightCorner.setLongitude(rightLon);
        Location center = new Location("center");
        center.setLatitude(map.getCameraPosition().target.latitude);
        center.setLongitude(map.getCameraPosition().target.longitude);
        int radius = Math.round(center.distanceTo(topRightCorner));
        Log.d(TAG, "radius: " + radius);

        String url = baseUrl + "spot/?center_latitude=" + center.getLatitude() +
                "&center_longitude=" + center.getLongitude() + "&distance=" +
                (radius - Math.round(0.045*radius)) + "&limit=0";
        ((MainActivity) getActivity()).connectToServer(url, "spaces");

        // this is how you draw a line from point to point
//        line = new PolylineOptions().add(new LatLng(rightLat, rightLon),
//                new LatLng(center.getLatitude(), center.getLongitude()))
//                .width(5).color(Color.RED);
//        googleMap.addPolyline(line);
    }

    // This method is being implemented as part of the interface UpdateMapAfterUserInteraction
    // This is essentially a callback for touch event on the map
    // TODO: send new request to server when the user moves around the map
    public void updateMap() {
//        ((MainActivity) getActivity()).connectToServer();
        Log.d(TAG, "gesture detected by onUpdateMapAfterUserInteraction");
        spotSearch();
    }

    // This is the default method needed for Android Frafments
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle bundle) {
        super.onCreate(bundle);
        if(view == null)
            view = inflater.inflate(R.layout.fragment_space_map, container, false);

        mTouchView = new TouchableWrapper(getActivity());
        // TODO: need to avoid restarting fragment on backpress
        if(view.getParent() != null) {
            ((ViewGroup)view.getParent()).removeView(view);
        }
        mTouchView.addView(view);
        return mTouchView;
    }

    // This is the default method needed for Android Fragments
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
//        setUpMap();
    }

//    // Setting up a Map
//    private void setUpMap() {
//        if (map != null)
//            return;
//        map = mapFragment.getMap();
//        if(map == null)
//            return;
//    }

    // TODO: save current conditions here to be reloaded
    @Override
    public void onSaveInstanceState (Bundle savedInstanceState) {

    }

    // This method displays the clusters on the map by clustering by distance
    // It takes the json data as parameter
    public void DisplayClustersByDistance(Spaces spaces){
        //TODO: Use CustomRenderer to set minimum cluster size
//        mClusterManager.setRenderer(new CustomeRenderer<>(this, mMap, mClusterManager));
        mClusterManager.clearItems();

        // Use to create a minimum bound based on a set of LatLng points.
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        // Looping through Spaces model to obtain each Space
        for(int i = 0; i < spaces.size(); i++){
            mClusterManager.addItem(spaces.get(i));
        }
    }

    // Setting up the ClusterManager which would contain all the clusters
    // This is only used by DisplayClustersByDistance() method
    public void setUpClusterer() {

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        mClusterManager = new ClusterManager<>(this.getActivity(), map);

        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        map.setOnCameraChangeListener(mClusterManager);
        map.setOnMarkerClickListener(mClusterManager);

        mClusterManager.setAlgorithm(new PreCachingAlgorithmDecorator<>(new CustomClusteringAlgorithm<Space>()));
    }

    // This method displays the clusters on the map by clustering by Building Names
    // It takes the json data as parameter
    public void DisplayClustersByBuilding(JSONArray mJson){

        // HashMap to keep track of all buildings with their building objects
        HashMap<String, Building> building_cluster = new HashMap<String, Building>();
        try {

            // Builder object to build bound for all clusters/markers
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            // Looping through all json data
            for(int i = 0; i < mJson.length(); i++){
                JSONObject curr = mJson.getJSONObject(i);
                JSONObject info = curr.getJSONObject("extended_info");

                JSONObject location = curr.getJSONObject("location");
                double lng = Double.parseDouble(location.getString("longitude"));
                double lat = Double.parseDouble(location.getString("latitude"));
                String name = curr.getString("name");
                String building_name = location.getString("building_name");
                String campus = info.getString("campus");

                if(campus.equals("seattle") && (!building_cluster.containsKey(building_name))){
                    LatLng currLoc = new LatLng(lat, lng);

                    // Creating Building Objects with location, building name
                    Building buil = new Building(building_name);
                    building_cluster.put(building_name, buil);
                    builder.include(currLoc);
                }else if(building_cluster.containsKey(building_name)){

                    // Increasing the number of spots in the current building
                    Building temp = building_cluster.get(building_name);
                    temp.increaseSpots();
                }
            }

            // Iterating through the hashmap of all buildings to add to the maps
            for (String key : building_cluster.keySet()) {
                Building b = building_cluster.get(key);
                addMarkerToMap(b.getPosition(), b.getSpots());
            }

            LatLngBounds bounds = builder.build();
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 70));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // This adds a marker to the map with IconGenerator class
    // The method takes the LatLng object location and text to be put on the marker/cluster as an Integer
    protected void addMarkerToMap(LatLng loc, int text) {
        IconGenerator iconFactory = new IconGenerator(getActivity());
        addIcon(iconFactory, Integer.toString(text), loc);
    }

    // This is the helper method for adding a marker to the map
    // This is invoked by addMarkerToMap
    private void addIcon(IconGenerator iconFactory, String text, LatLng position) {
        iconFactory.setStyle(IconGenerator.STYLE_PURPLE);
        Bitmap bmp = iconFactory.makeIcon(text);
        MarkerOptions markerOptions = new MarkerOptions().
                icon(BitmapDescriptorFactory.fromBitmap(bmp)).
                position(position).
                anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());

        map.addMarker(markerOptions);
    }

}
