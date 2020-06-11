package com.manuelserrano.tfg;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;
import com.manuelserrano.tfg.models.Beacon;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
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
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.ColorUtils;
import com.mapbox.turf.TurfJoins;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;

/**
 * Display an indoor map of a building with toggles to switch between floor levels
 */
public class IndoorMapActivity extends AppCompatActivity {

    private GeoJsonSource indoorBuildingSource;

    private List<List<Point>> boundingBoxList;
    private View levelButtons;
    private MapView mapView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

// Mapbox access token is configured here. This needs to be called either in your application
// object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.access_token));

// This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_lab_indoor_map);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
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
                                if (mapboxMap.getCameraPosition().zoom > 16) {
                                    if (TurfJoins.inside(Point.fromLngLat(mapboxMap.getCameraPosition().target.getLongitude(),
                                            mapboxMap.getCameraPosition().target.getLatitude()), Polygon.fromLngLats(boundingBoxList))) {
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
                            }
                        });
                        indoorBuildingSource = new GeoJsonSource(
                                "indoor-building", loadJsonFromAsset("white_house_lvl_0.geojson"));
                        style.addSource(indoorBuildingSource);

// Add the building layers since we know zoom levels in range
                        loadBuildingLayer(style);


                        mapboxMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
                            @Override
                            public boolean onMapClick(@NonNull LatLng point) {
                                Log.e("POSITION", point.toString());
                                return true;
                            }
                        });


                        // create circle manager
                        CircleManager circleManager = new CircleManager(mapView, mapboxMap, style);
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
                                .withLatLng(new LatLng(38.89756475643293, -77.03659892961862))
                                .withCircleColor(ColorUtils.colorToRgbaString(Color.BLUE))
                                .withCircleRadius(12f)
                                .withDraggable(true);
                        circleManager.create(circleOptions);

                        CircleOptions circleOptions2 = new CircleOptions()
                                .withLatLng(new LatLng(38.89756599127142, -77.03648413017417))
                                .withCircleColor(ColorUtils.colorToRgbaString(Color.BLUE))
                                .withCircleRadius(12f)
                                .withDraggable(true);
                        circleManager.create(circleOptions2);

                        CircleOptions circleOptions3 = new CircleOptions()
                                .withLatLng(new LatLng(38.897640548208614, -77.03654186019196))
                                .withCircleColor(ColorUtils.colorToRgbaString(Color.BLUE))
                                .withCircleRadius(12f)
                                .withDraggable(true);
                        circleManager.create(circleOptions3);

                        Type listType = new TypeToken<List<Beacon>>() {
                        }.getType();
                        ArrayList<Beacon> beacons = new Gson().fromJson(loadJsonFromAsset("beacons.json"), listType);


                        //LatLng position = getCentralGeoCoordinate(beacons);

                        double kFilteringFactor = 0.1;

                        double rollingRssi = 0;
                        rollingRssi = (-95 * kFilteringFactor) + (rollingRssi * (1.0 - kFilteringFactor));
                        double value = calculateAccuracyWithRSSI(rollingRssi);

                        double rollingRssi2 = 0;
                        rollingRssi2 = (-200 * kFilteringFactor) + (rollingRssi2 * (1.0 - kFilteringFactor));
                        double value2 = calculateAccuracyWithRSSI(rollingRssi2);


                        Beacon a = beacons.get(0);
                        Beacon b = beacons.get(1);
                        Beacon c = beacons.get(2);


                        double R= 6371;
                        double x1 = R * cos(a.getLat()) * cos(a.getLng());
                        double y1 = R * cos(a.getLat()) * sin(a.getLng());

                        double x2 = R * cos(b.getLat()) * cos(b.getLng());
                        double y2 = R * cos(b.getLat()) * sin(b.getLng());

                        double x3 = R * cos(c.getLat()) * cos(c.getLng());
                        double y3 = R * cos(c.getLat()) * sin(c.getLng());

                        double[][] positions = new double[][] { { x1,y1}, { x2,y2  }, { x3,y3  } };
                        double[] distances = new double[] {2000, 1300.97, 3000.32};

                        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
                        LeastSquaresOptimizer.Optimum optimum = solver.solve();

                        // the answer
                        double[] centroid = optimum.getPoint().toArray();

                        //error and geometry information; may throw SingularMatrixException depending the threshold argument provided
                        RealVector standardDeviation = optimum.getSigma(0);
                        RealMatrix covarianceMatrix = optimum.getCovariances(0);


                        double z=1;
                        double lat = asin(z / R);
                        double lon = atan2(centroid[0], centroid[1]);

                        //LatLng position = new LatLng(lat,lon);

                        LatLng position = getLocationByTrilateration(a,1.0,b,8.0,c,1.0);

                        /*CircleOptions userPosition = new CircleOptions()
                                .withLatLng(new LatLng(position.getLatitude(), position.getLongitude()))
                                .withCircleColor(ColorUtils.colorToRgbaString(Color.RED))
                                .withCircleRadius(12f)
                                .withDraggable(true);
                        circleManager.create(userPosition);*/


                        LocationComponentOptions locationComponentOptions =
                                LocationComponentOptions.builder(getApplicationContext())
                                        .pulseEnabled(true)
                                        .backgroundStaleTintColor(Color.GRAY)
                                        .backgroundTintColor(Color.GRAY)
                                        .pulseInterpolator(new BounceInterpolator())
                                        .build();

                        LocationComponentActivationOptions locationComponentActivationOptions = LocationComponentActivationOptions
                                .builder(getApplicationContext(), style)
                                .locationComponentOptions(locationComponentOptions)
                                .build();

                        LocationComponent locationComponent = mapboxMap.getLocationComponent();
                        Location location = new Location("prueba");
                        location.setLatitude(position.getLatitude());
                        location.setLongitude(position.getLongitude());
                        locationComponent.activateLocationComponent(locationComponentActivationOptions);

                        locationComponent.forceLocationUpdate(location);

                        //locationComponent.setLocationComponentEnabled(true);


                    }
                });

                Button buttonSecondLevel = findViewById(R.id.second_level_button);
                buttonSecondLevel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        indoorBuildingSource.setGeoJson(loadJsonFromAsset("white_house_lvl_1.geojson"));
                    }
                });

                Button buttonGroundLevel = findViewById(R.id.ground_level_button);
                buttonGroundLevel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        indoorBuildingSource.setGeoJson(loadJsonFromAsset("white_house_lvl_0.geojson"));
                    }
                });


            }
        });
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

        FillLayer indoorBuildingLayer = new FillLayer("indoor-building-fill", "indoor-building").withProperties(
                fillColor(Color.parseColor("#eeeeee")),
// Function.zoom is used here to fade out the indoor layer if zoom level is beyond 16. Only
// necessary to show the indoor map at high zoom levels.
                fillOpacity(interpolate(exponential(1f), zoom(),
                        stop(16f, 0f),
                        stop(16.5f, 0.5f),
                        stop(17f, 1f))));

        style.addLayer(indoorBuildingLayer);

        LineLayer indoorBuildingLineLayer = new LineLayer("indoor-building-line", "indoor-building").withProperties(
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

    public LatLng getCentralGeoCoordinate(List<Beacon> geoCoordinates) {
//        if (geoCoordinates.Count == 1) {
//            return geoCoordinates.Single();
//        }
        double x = 0;
        double y = 0;
        double z = 0;

        for (Beacon coordinate : geoCoordinates) {

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


    public LatLng getCoordinateWithBeaconA( Beacon a ,Beacon b, Beacon c, double dA, double dB, double dC) {
        double W, Z, x, y, y2;
        W = dA*dA - dB*dB - a.getLat()*a.getLat() - a.getLng()*a.getLng() + b.getLat()*b.getLat() + b.getLng()*b.getLng();
        Z = dB*dB - dC*dC - b.getLat()*b.getLat() - b.getLng()*b.getLng() + c.getLat()*c.getLat() + c.getLng()*c.getLng();

        x = (W*(c.getLng()-b.getLng()) - Z*(b.getLng()-a.getLng())) / (2 * ((b.getLat()-a.getLat())*(c.getLng()-b.getLng()) - (c.getLat()-b.getLat())*(b.getLng()-a.getLng())));
        y = (W - 2*x*(b.getLat()-a.getLat())) / (2*(b.getLng()-a.getLng()));
        //y2 is a second measure of y to mitigate errors
        y2 = (Z - 2*x*(c.getLat()-b.getLat())) / (2*(c.getLng()-b.getLng()));

        y = (y + y2) / 2;
        return new LatLng(x, y);
    }


    public LatLng getCoordinateWithBeacons(Beacon beacon1,Beacon beacon2,Beacon beacon3,double distance1,double distance2,double distance3){
        double xa = beacon1.getLat();
        double ya = beacon1.getLng();
        double xb = beacon2.getLat();
        double yb = beacon2.getLng();
        double xc = beacon3.getLat();
        double yc = beacon3.getLng();
        double ra = distance1;
        double rb = distance2;
        double rc = distance3;

        double S = (pow(xc, 2.) - pow(xb, 2.) + pow(yc, 2.) - pow(yb, 2.) + pow(rb, 2.) - pow(rc, 2.)) / 2.0;
        double T = (pow(xa, 2.) - pow(xb, 2.) + pow(ya, 2.) - pow(yb, 2.) + pow(rb, 2.) - pow(ra, 2.)) / 2.0;
        double y = ((T * (xb - xc)) - (S * (xb - xa))) / (((ya - yb) * (xb - xc)) - ((yc - yb) * (xb - xa)));
        double x = ((y * (ya - yb)) - T) / (xb - xa);

        return new LatLng(x,y);
    }

    public  double calculateAccuracyWithRSSI(double rssi) {
        //formula adapted from David Young's Radius Networks Android iBeacon Code
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }


        double txPower = -70;
        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return pow(ratio,10);
        }
        else {
            double accuracy =  (0.89976) * pow(ratio,7.7095) + 0.111;
            return accuracy;
        }
    }

    public static LatLng getLocationByTrilateration(
            Beacon ponto1, double distance1,
            Beacon ponto2, double distance2,
            Beacon ponto3, double distance3){

        //DECLARACAO DE VARIAVEIS
        LatLng retorno = new LatLng();
        double[] P1   = new double[2];
        double[] P2   = new double[2];
        double[] P3   = new double[2];
        double[] ex   = new double[2];
        double[] ey   = new double[2];
        double[] p3p1 = new double[2];
        double jval  = 0;
        double temp  = 0;
        double ival  = 0;
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
            t1   = P2[i];
            t2   = P1[i];
            t    = t1 - t2;
            temp += (t*t);
        }
        d = Math.sqrt(temp);
        for (int i = 0; i < P1.length; i++) {
            t1    = P2[i];
            t2    = P1[i];
            exx   = (t1 - t2)/(Math.sqrt(temp));
            ex[i] = exx;
        }
        for (int i = 0; i < P3.length; i++) {
            t1      = P3[i];
            t2      = P1[i];
            t3      = t1 - t2;
            p3p1[i] = t3;
        }
        for (int i = 0; i < ex.length; i++) {
            t1 = ex[i];
            t2 = p3p1[i];
            ival += (t1*t2);
        }
        for (int  i = 0; i < P3.length; i++) {
            t1 = P3[i];
            t2 = P1[i];
            t3 = ex[i] * ival;
            t  = t1 - t2 -t3;
            p3p1i += (t*t);
        }
        for (int i = 0; i < P3.length; i++) {
            t1 = P3[i];
            t2 = P1[i];
            t3 = ex[i] * ival;
            eyy = (t1 - t2 - t3)/Math.sqrt(p3p1i);
            ey[i] = eyy;
        }
        for (int i = 0; i < ey.length; i++) {
            t1 = ey[i];
            t2 = p3p1[i];
            jval += (t1*t2);
        }
        xval = (Math.pow(distance1, 2) - Math.pow(distance2, 2) + Math.pow(d, 2))/(2*d);
        yval = ((Math.pow(distance1, 2) - Math.pow(distance3, 2) + Math.pow(ival, 2) + Math.pow(jval, 2))/(2*jval)) - ((ival/jval)*xval);

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



    /*@SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
// Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

// Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

// Activate with options
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, loadedMapStyle).build());

// Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

// Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

// Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }*/

}