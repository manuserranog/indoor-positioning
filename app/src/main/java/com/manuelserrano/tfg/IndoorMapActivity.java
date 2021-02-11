package com.manuelserrano.tfg;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.manuelserrano.tfg.models.BeaconBuilding;
import com.manuelserrano.tfg.models.Building1;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.Circle;
import com.mapbox.mapboxsdk.plugins.annotation.CircleManager;
import com.mapbox.mapboxsdk.plugins.annotation.CircleOptions;
import com.mapbox.mapboxsdk.plugins.annotation.OnCircleClickListener;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.ColorUtils;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;

public class IndoorMapActivity extends AppCompatActivity implements BeaconConsumer, RangeNotifier {

    private String BASE_URL = "https://mysterious-meadow-52644.herokuapp.com";

    private GeoJsonSource geoJsonIndoorBuilding;

    private List<List<Point>> boundingBoxList;
    private View levelButtons;
    private MapView mapView;
    BeaconManager beaconManager;
    public BluetoothManager bluetoothState;
    BluetoothAdapter mBluetoothAdapter;

    private LocationComponent locationComponent;
    private LocationComponentActivationOptions locationComponentActivationOptions;

    private List<BeaconBuilding> beaconBuildings = new ArrayList<>();


    private Building1 nearestBuilding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Mapbox.getInstance(this, getString(R.string.access_token));

        setContentView(R.layout.activity_lab_indoor_map);
        checkPermissions(IndoorMapActivity.this, this);
        requestPermissions();

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        calculateNearBuilding();


    }

    private void initMap() {
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull final MapboxMap mapboxMap) {
                mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        levelButtons = findViewById(R.id.floor_level_buttons);

                        final List<Point> boundingBox = new ArrayList<>();
                        boundingBox.add(Point.fromLngLat(-77.03791, 38.89715));
                        boundingBox.add(Point.fromLngLat(-77.03791, 38.89811));
                        boundingBox.add(Point.fromLngLat(-77.03532, 38.89811));
                        boundingBox.add(Point.fromLngLat(-77.03532, 38.89708));
                        boundingBoxList = new ArrayList<>();
                        boundingBoxList.add(boundingBox);

                        mapboxMap.addOnCameraMoveListener(new MapboxMap.OnCameraMoveListener() {
                            @Override
                            public void onCameraMove() {
                                /*
                                if (mapboxMap.getCameraPosition().zoom > 16) {
                                    if (TurfJoins.inside(Point.fromLngLat(mapboxMap.getCameraPosition().target.getLongitude(),
                                            mapboxMap.getCameraPosition().target.getLatitude()),
                                            Polygon.fromLngLats(boundingBoxList))) {
                                        if (levelButtons.getVisibility() != View.VISIBLE) {
                                            showLevelButton();
                                        }
                                    } else {
                                        if (levelButtons.getVisibility() == View.VISIBLE) {
                                            hideLevelButton();
                                        }
                                    }
                                } else if (levelButtons.getVisibility() == View.VISIBLE) {
                                    hideLevelButton();
                                }

                                 */
                            }
                        });


                        /*GeoJsonSource geoJsonindoorBuilding = new GeoJsonSource(
                                "indoor-building", loadJsonFromAsset("white_house_lvl_1.geojson"));*/
                        style.addSource(geoJsonIndoorBuilding);


                        // Add the building layers since we know zoom levels in range
                        loadBuildingLayer(style);


                        mapboxMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
                            @Override
                            public boolean onMapClick(@NonNull LatLng point) {
                                Log.e("POSITION", point.toString());
                                return true;
                            }
                        });

                        loadCircleBeacons(style, mapboxMap);

                        LocationComponentOptions locationComponentOptions =
                                LocationComponentOptions.builder(getApplicationContext())
                                        .pulseEnabled(true)
                                        .backgroundStaleTintColor(Color.GRAY)
                                        .backgroundTintColor(Color.GRAY)
                                        .pulseInterpolator(new BounceInterpolator())
                                        .build();

                        locationComponentActivationOptions =
                                LocationComponentActivationOptions
                                        .builder(getApplicationContext(), style)
                                        .locationComponentOptions(locationComponentOptions)
                                        .build();

                        locationComponent = mapboxMap.getLocationComponent();

                    }
                });

                Button buttonSecondLevel = findViewById(R.id.second_level_button);
                buttonSecondLevel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        loadFloor(0);
                        //geoJsonIndoorBuilding.setGeoJson(loadJsonFromAsset("white_house_lvl_1.geojson"));
                    }
                });

                Button buttonGroundLevel = findViewById(R.id.ground_level_button);
                buttonGroundLevel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        //geoJsonIndoorBuilding.setGeoJson(loadJsonFromAsset("white_house_lvl_0.geojson"));
                    }
                });
            }
        });
    }

    private void calculateNearBuilding() {

        LatLng position = new LatLng(37.358507, -5.986327);

        String url = BASE_URL+"/v1/buildings?latitude=" + position.getLatitude() + "&longitude=" + position.getLongitude();
        RequestQueue queue = Volley.newRequestQueue(this);
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i("JSON", response.toString());

                        nearestBuilding = new Gson().fromJson(response, Building1.class);


                        initBeacons();

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("ERROR", error.toString());
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void initBeacons() {
    /*Type listType = new TypeToken<List<BeaconBuilding>>()
    }.getType();
    beaconBuildings = new Gson().fromJson(loadJsonFromAsset("beacons.json"), listType);*/

        String url = BASE_URL+"/v1/beacons?buildingId=" + nearestBuilding.getId();
        RequestQueue queue = Volley.newRequestQueue(this);
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i("JSON", response.toString());

                        //BeaconPage beaconPage = new Gson().fromJson(response, BeaconPage.class);
                        //beaconBuildings = beaconPage.getResults();

                        Type listType = new TypeToken<List<BeaconBuilding>>() {
                        }.getType();
                        beaconBuildings = new Gson().fromJson(response, listType);


                        initBeaconScanner();

                        initGeoJson();

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("ERROR", error.toString());
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void initGeoJson() {

        String url = BASE_URL+"/v1/floors?buildingId=" + nearestBuilding.getId() + "&floorNumber=0";
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i("JSON", response.toString());

                        //FeatureCollection featureCollection = FeatureCollection.fromJson(response);

                        geoJsonIndoorBuilding = new GeoJsonSource("indoor-building", response);

                        //geoJsonIndoorBuilding = new GeoJsonSource("indoor-building", featureCollection);

                        //FeatureCollection featureCollection = new FeatureCollection();
                        //BuildingPage buildingPage = new Gson().fromJson(response, BuildingPage.class);

                        initMap();

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("ERROR", error.toString());
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }


    private void loadFloor(int i) {

        String url = BASE_URL+"/v1/buildings/5f678172e020a41391d2cfd9";
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i("JSON", response.toString());

                        FeatureCollection featureCollection = FeatureCollection.fromJson(response);

                        //geoJsonIndoorBuilding = new GeoJsonSource("indoor-building", response);

                        //geoJsonIndoorBuilding = new GeoJsonSource("indoor-building", featureCollection);

                        //FeatureCollection featureCollection = new FeatureCollection();
                        //BuildingPage buildingPage = new Gson().fromJson(response, BuildingPage.class);

                        initMap();

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("ERROR", error.toString());
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void loadCircleBeacons(@NonNull Style style, @NonNull MapboxMap mapboxMap) {
        // create circle manager
        /* CircleManager circleManager = new CircleManager(mapView, mapboxMap, style);
        circleManager.addClickListener(new OnCircleClickListener() {
            @Override
            public void onAnnotationClick(Circle circle) {
                Toast.makeText(IndoorMapActivity.this,
                        String.format("Circle clicked %s", circle.getId()),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // create a fixed circle
        CircleOptions circleOptions = new CircleOptions()
                .withLatLng(new LatLng(37.358140319763294, -5.987024166876807))
                .withCircleColor(ColorUtils.colorToRgbaString(Color.BLUE))
                .withCircleRadius(12f)
                .withDraggable(true);
        circleManager.create(circleOptions);

        CircleOptions circleOptions2 = new CircleOptions()
                .withLatLng(new LatLng(37.35826293615726, -5.9870105230474735))
                .withCircleColor(ColorUtils.colorToRgbaString(Color.BLUE))
                .withCircleRadius(12f)
                .withDraggable(true);
        circleManager.create(circleOptions2);

        CircleOptions circleOptions3 = new CircleOptions()
                .withLatLng(new LatLng(37.35820630661975, -5.986862294281593))
                .withCircleColor(ColorUtils.colorToRgbaString(Color.BLUE))
                .withCircleRadius(12f)
                .withDraggable(true);
        circleManager.create(circleOptions3);

*/

        mapboxMap.getStyle().addImage("ICON_ID", BitmapFactory.decodeResource(
                IndoorMapActivity.this.getResources(), R.drawable.mapbox_marker_icon_default));


        GeoJsonSource geoJsonindoorBuilding2 = new GeoJsonSource(
                "SOURCE_ID", loadJsonFromAsset("points.geojson"));
        mapboxMap.getStyle().addSource(geoJsonindoorBuilding2);

        mapboxMap.getStyle().addLayer(new SymbolLayer("LAYER_ID", "SOURCE_ID")
                .withProperties(
                        iconImage("ICON_ID"),
                        iconAllowOverlap(true),
                        iconIgnorePlacement(true)
                ));

/*
        mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(37.35820630661975, -5.986862294281593))
                .title("Eiffel Tower"));
                */

    }

    private void initBeaconScanner() {
        String IBEACON_LAYOUT = "m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24";
        String EDDYSTONE_UID_LAYOUT = BeaconParser.EDDYSTONE_UID_LAYOUT;
        String EDDYSTONE_URL_LAYOUT = BeaconParser.EDDYSTONE_URL_LAYOUT;
        String EDDYSTONE_TLM_LAYOUT = BeaconParser.EDDYSTONE_TLM_LAYOUT;

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(EDDYSTONE_UID_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(EDDYSTONE_URL_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(EDDYSTONE_TLM_LAYOUT));

        bluetoothState = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothState.getAdapter();
        startScan();
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(IndoorMapActivity.this,
                Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(IndoorMapActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_COARSE_LOCATION},
                    0);
        } else {
            Log.d("", "GRANTED");
        }
    }

    private void updatePosition() {

        LatLng position = getLocationByTrilateration(beaconBuildings.get(0), beaconBuildings.get(0).getDistance(),
                beaconBuildings.get(1), beaconBuildings.get(1).getDistance(), beaconBuildings.get(2),
                beaconBuildings.get(2).getDistance());

        Location location = new Location("prueba");
        location.setLatitude(position.getLatitude());
        location.setLongitude(position.getLongitude());
        locationComponent.activateLocationComponent(locationComponentActivationOptions);


        locationComponent.forceLocationUpdate(location);
        //locationComponent.setLocationComponentEnabled(true);


    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    private void hideLevelButton() {
// When the user moves away from our bounding box region or zooms out far enough the floor level
// buttons are faded out and hidden.
        AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
        animation.setDuration(500);
        levelButtons.startAnimation(animation);
        levelButtons.setVisibility(View.GONE);
    }

    private void showLevelButton() {
// When the user moves inside our bounding box region or zooms in to a high enough zoom level,
// the floor level buttons are faded out and hidden.
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(500);
        levelButtons.startAnimation(animation);
        levelButtons.setVisibility(View.VISIBLE);
    }

    private void loadBuildingLayer(@NonNull Style style) {
// Method used to load the indoor layer on the map. First the fill layer is drawn and then the
// line layer is added.

        FillLayer indoorBuildingLayer = new FillLayer("indoor-building-fill",
                "indoor-building").withProperties(
                fillColor(Color.parseColor("#eeeeee")),
// Function.zoom is used here to fade out the indoor layer if zoom level is beyond 16. Only
// necessary to show the indoor map at high zoom levels.
                fillOpacity(interpolate(exponential(1f), zoom(),
                        stop(16f, 0f),
                        stop(16.5f, 0.5f),
                        stop(17f, 1f))));

        style.addLayer(indoorBuildingLayer);

        LineLayer indoorBuildingLineLayer = new LineLayer("indoor-building-line",
                "indoor-building").withProperties(
                lineColor(Color.parseColor("#50667f")),
                lineWidth(0.5f),
                lineOpacity(interpolate(exponential(1f), zoom(),
                        stop(16f, 0f),
                        stop(16.5f, 0.5f),
                        stop(17f, 1f))));
        style.addLayer(indoorBuildingLineLayer);
    }

    private String loadJsonFromAsset(String filename) {
// Using this method to load in GeoJSON files from the assets folder.
        try {
            InputStream is = getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, Charset.forName("UTF-8"));

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }


    public double calculateAccuracyWithRSSI(double rssi) {
        //formula adapted from David Young's Radius Networks Android iBeacon Code
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        double txPower = -70;
        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return pow(ratio, 10);
        } else {
            double accuracy = (0.89976) * pow(ratio, 7.7095) + 0.111;
            return accuracy;
        }
    }

    public static LatLng getLocationByTrilateration(
            BeaconBuilding ponto1, double distance1,
            BeaconBuilding ponto2, double distance2,
            BeaconBuilding ponto3, double distance3) {

        LatLng retorno = new LatLng();
        double[] P1 = new double[2];
        double[] P2 = new double[2];
        double[] P3 = new double[2];
        double[] ex = new double[2];
        double[] ey = new double[2];
        double[] p3p1 = new double[2];
        double jval = 0;
        double temp = 0;
        double ival = 0;
        double p3p1i = 0;
        double triptx;
        double xval;
        double yval;
        double t1;
        double t2;
        double t3;
        double t;
        double exx;
        double d;
        double eyy;

        //TRANSFORMA OS PONTOS EM VETORES
        //PONTO 1
        P1[0] = ponto1.getLat();
        P1[1] = ponto1.getLng();
        //PONTO 2
        P2[0] = ponto2.getLat();
        P2[1] = ponto2.getLng();
        //PONTO 3
        P3[0] = ponto3.getLat();
        P3[1] = ponto3.getLng();

        //TRANSFORMA O VALOR DE METROS PARA A UNIDADE DO MAPA
        //DISTANCIA ENTRE O PONTO 1 E A MINHA LOCALIZACAO
        distance1 = (distance1 / 100000);
        //DISTANCIA ENTRE O PONTO 2 E A MINHA LOCALIZACAO
        distance2 = (distance2 / 100000);
        //DISTANCIA ENTRE O PONTO 3 E A MINHA LOCALIZACAO
        distance3 = (distance3 / 100000);

        for (int i = 0; i < P1.length; i++) {
            t1 = P2[i];
            t2 = P1[i];
            t = t1 - t2;
            temp += (t * t);
        }
        d = Math.sqrt(temp);
        for (int i = 0; i < P1.length; i++) {
            t1 = P2[i];
            t2 = P1[i];
            exx = (t1 - t2) / (Math.sqrt(temp));
            ex[i] = exx;
        }
        for (int i = 0; i < P3.length; i++) {
            t1 = P3[i];
            t2 = P1[i];
            t3 = t1 - t2;
            p3p1[i] = t3;
        }
        for (int i = 0; i < ex.length; i++) {
            t1 = ex[i];
            t2 = p3p1[i];
            ival += (t1 * t2);
        }
        for (int i = 0; i < P3.length; i++) {
            t1 = P3[i];
            t2 = P1[i];
            t3 = ex[i] * ival;
            t = t1 - t2 - t3;
            p3p1i += (t * t);
        }
        for (int i = 0; i < P3.length; i++) {
            t1 = P3[i];
            t2 = P1[i];
            t3 = ex[i] * ival;
            eyy = (t1 - t2 - t3) / Math.sqrt(p3p1i);
            ey[i] = eyy;
        }
        for (int i = 0; i < ey.length; i++) {
            t1 = ey[i];
            t2 = p3p1[i];
            jval += (t1 * t2);
        }
        xval = (Math.pow(distance1, 2) - Math.pow(distance2, 2) + Math.pow(d, 2)) / (2 * d);
        yval = ((Math.pow(distance1, 2) - Math.pow(distance3, 2) + Math.pow(ival, 2)
                + Math.pow(jval, 2)) / (2 * jval)) - ((ival / jval) * xval);

        t1 = ponto1.getLat();
        t2 = ex[0] * xval;
        t3 = ey[0] * yval;
        triptx = t1 + t2 + t3;
        retorno.setLatitude(triptx);
        t1 = ponto1.getLng();
        t2 = ex[1] * xval;
        t3 = ey[1] * yval;
        triptx = t1 + t2 + t3;
        retorno.setLongitude(triptx);

        return retorno;
    }

    int RC_COARSE_LOCATION = 1;

    private void startScan() {
        mBluetoothAdapter.enable();
        if (mBluetoothAdapter.isEnabled()) {
            if (beaconManager.isBound(this) != true) {
                beaconManager.bind(this);
            }
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        Log.d("", "beaconManager is bound, ready to start scanning");


        // Encapsula un identificador de un beacon de una longitud arbitraria de bytes
        ArrayList<Identifier> identifiers = new ArrayList<>();

        // Asignar null para indicar que queremos buscar cualquier beacon
        identifiers.add(null);
        // Representa un criterio de campos utilizados para buscar beacons
        Region region = new Region("com.manuelserrano.tfg", null, null, null);
        try {
            // Ordena al BeaconService empezar a buscar beacons que coincida con el objeto Region pasado
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        // Especifica una clase que debería ser llamada cada vez que BeaconsService obtiene datos, una vez por segundo por defecto
        beaconManager.addRangeNotifier(this);
    }

    private List<Beacon> allBeacons = new ArrayList<>();

    private int resetDistances = 0;

    @Override
    public void didRangeBeaconsInRegion(Collection<org.altbeacon.beacon.Beacon> beacons, Region region) {
        if (beacons.size() > 0) {
            storeBeaconsAround(beacons);
//            Log.i("", "El primer beacon detectado se encuentra a una distancia de " +
//                    beacons.iterator().next().getBluetoothAddress() + " metros.");
            //Log.i("Nuevo",beacons.iterator().next().getBluetoothAddress().toString());
        }
    }


    private void storeBeaconsAround(Collection<Beacon> beacons) {

        if (resetDistances == 5) {
            resetDistances = 0;

            for (BeaconBuilding beaconBuilding : beaconBuildings) {
                beaconBuilding.setDistance(10.0);

            }
            allBeacons = new ArrayList<>();

        }

        for (Beacon newBeacon : beacons) {
            if (!allBeacons.contains(newBeacon)) {
                allBeacons.add(newBeacon);
            } else {
                allBeacons.set(allBeacons.indexOf(newBeacon), newBeacon);
            }
        }

        for (Beacon b1 : allBeacons) {
            BeaconBuilding beaconFound = beaconBuildings.stream().filter(beaconBuilding -> beaconBuilding.getBluetoothAddress()
                    .equals(b1.getBluetoothAddress())).findFirst().orElse(null);

            if (beaconFound != null) {
                beaconFound.setDistance(b1.getDistance());
                beaconBuildings.set(beaconBuildings.indexOf(beaconFound), beaconFound);
            }
        }

        //Log.i("Encontrados",beacons.toString());
        Log.i("Todos", beaconBuildings.toString());

        updatePosition();
        resetDistances++;
    }

    public static void checkPermissions(Activity activity, Context context) {
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_PRIVILEGED,
        };

        if (!hasPermissions(context, PERMISSIONS)) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS, PERMISSION_ALL);
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    public LatLng getCentralGeoCoordinate(List<BeaconBuilding> geoCoordinates) {
//        if (geoCoordinates.Count == 1) {
//            return geoCoordinates.Single();
//        }
        double x = 0;
        double y = 0;
        double z = 0;

        for (BeaconBuilding coordinate : geoCoordinates) {

            double latitude = coordinate.getLat() * Math.PI / 180;
            double longitude = coordinate.getLng() * Math.PI / 180;

            x += cos(latitude) * cos(longitude);
            y += cos(latitude) * sin(longitude);
            z += sin(latitude);
        }

        int total = geoCoordinates.size();

        x = x / total;
        y = y / total;
        z = z / total;

        double centralLongitude = atan2(y, x);
        double centralSquareRoot = Math.sqrt(x * x + y * y);
        double centralLatitude = atan2(z, centralSquareRoot);

        return new LatLng(centralLatitude * 180 / Math.PI, centralLongitude * 180 / Math.PI);
    }


    public LatLng getCoordinateWithBeaconA(BeaconBuilding a, BeaconBuilding b, BeaconBuilding c, double dA, double dB, double dC) {
        double W, Z, x, y, y2;
        W = dA * dA - dB * dB - a.getLat() * a.getLat() - a.getLng() * a.getLng() + b.getLat() * b.getLat() + b.getLng() * b.getLng();
        Z = dB * dB - dC * dC - b.getLat() * b.getLat() - b.getLng() * b.getLng() + c.getLat() * c.getLat() + c.getLng() * c.getLng();

        x = (W * (c.getLng() - b.getLng()) - Z * (b.getLng() - a.getLng())) / (2 * ((b.getLat() - a.getLat()) * (c.getLng() - b.getLng())
                - (c.getLat() - b.getLat()) * (b.getLng() - a.getLng())));
        y = (W - 2 * x * (b.getLat() - a.getLat())) / (2 * (b.getLng() - a.getLng()));
        //y2 is a second measure of y to mitigate errors
        y2 = (Z - 2 * x * (c.getLat() - b.getLat())) / (2 * (c.getLng() - b.getLng()));

        y = (y + y2) / 2;
        return new LatLng(x, y);
    }


    public LatLng getCoordinateWithBeacons(BeaconBuilding beaconBuilding1,
                                           BeaconBuilding beaconBuilding2, BeaconBuilding beaconBuilding3, double distance1,
                                           double distance2, double distance3) {
        double xa = beaconBuilding1.getLat();
        double ya = beaconBuilding1.getLng();
        double xb = beaconBuilding2.getLat();
        double yb = beaconBuilding2.getLng();
        double xc = beaconBuilding3.getLat();
        double yc = beaconBuilding3.getLng();
        double ra = distance1;
        double rb = distance2;
        double rc = distance3;

        double S = (pow(xc, 2.) - pow(xb, 2.) + pow(yc, 2.) - pow(yb, 2.) + pow(rb, 2.) - pow(rc, 2.)) / 2.0;
        double T = (pow(xa, 2.) - pow(xb, 2.) + pow(ya, 2.) - pow(yb, 2.) + pow(rb, 2.) - pow(ra, 2.)) / 2.0;
        double y = ((T * (xb - xc)) - (S * (xb - xa))) / (((ya - yb) * (xb - xc)) - ((yc - yb) * (xb - xa)));
        double x = ((y * (ya - yb)) - T) / (xb - xa);

        return new LatLng(x, y);
    }

}