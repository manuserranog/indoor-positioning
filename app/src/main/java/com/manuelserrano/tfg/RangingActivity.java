package com.manuelserrano.tfg;

import android.app.Activity;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.RemoteException;
import android.util.Log;
import android.view.View;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Collection;

public class RangingActivity extends Activity implements BeaconConsumer, RangeNotifier {
    protected static final String TAG = "RangingActivity";
    private BeaconManager mBeaconManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranging);

        mBeaconManager = BeaconManager.getInstanceForApplication(this);

        // En este ejemplo vamos a usar el protocolo Eddystone, así que tenemos que definirlo aquí
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));

        // Bindea esta actividad al BeaconService
        mBeaconManager.bind(this);
    }


    @Override
    public void onBeaconServiceConnect() {
        // Encapsula un identificador de un beacon de una longitud arbitraria de bytes
        ArrayList<Identifier> identifiers = new ArrayList<>();

        // Asignar null para indicar que queremos buscar cualquier beacon
        identifiers.add(null);

        // Representa un criterio de campos utilizados para buscar beacons
        Region region = new Region("AllBeaconsRegion", identifiers);

        try {
            // Ordena al BeaconService empezar a buscar beacons que coincida con el objeto Region pasado
            mBeaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        // Especifica una clase que debería ser llamada cada vez que BeaconsService obtiene datos, una vez por segundo por defecto
        mBeaconManager.addRangeNotifier(this);
    }


    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        if (beacons.size() > 0) {
            Log.i(TAG, "El primer beacon detectado se encuentra a una distancia de " + beacons.iterator().next().getDistance() + " metros.");
        }
    }


}
