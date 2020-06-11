package com.manuelserrano.tfg;

import android.app.PendingIntent;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;

public class CustomLocationEngine implements LocationEngine {
    @Override
    public void getLastLocation(@NonNull LocationEngineCallback<LocationEngineResult> callback) throws SecurityException {

    }

    @Override
    public void requestLocationUpdates(@NonNull LocationEngineRequest request, @NonNull LocationEngineCallback<LocationEngineResult> callback, @Nullable Looper looper) throws SecurityException {

    }

    @Override
    public void requestLocationUpdates(@NonNull LocationEngineRequest request, PendingIntent pendingIntent) throws SecurityException {

    }

    @Override
    public void removeLocationUpdates(@NonNull LocationEngineCallback<LocationEngineResult> callback) {

    }

    @Override
    public void removeLocationUpdates(PendingIntent pendingIntent) {

    }
}
