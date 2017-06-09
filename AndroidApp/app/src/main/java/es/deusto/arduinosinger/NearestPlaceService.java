package es.deusto.arduinosinger;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.location.LocationListener;

import java.util.ArrayList;

/**
 * This service looks for the nearest place every minute and displays a notification when the latter changes.
 */

public class NearestPlaceService extends Service implements LocationListener {

    private LocationManager lm;
    private ArrayList<Song> arraylSongs;
    private long waiting_interval = 5; // in seconds!!
    private float minDistance = 20f; // Minimum distance between location updates, in meters
    public static double nearestPlaceLongitude = 0;
    public static double  nearestPlaceLatitude = 0;
    private double distanceToPlace = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null;}

    @SuppressWarnings("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { // No need to access intent for the moment.
        // Display a notification or so...
        Log.i("MYLOG", "NearestPlace SERVICE HAS STARTED!!!!!!");

        // Activate location gathering to check for the nearest place:
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Every minute the app will check whether the nearest place/location has changed, looking among the nearest places and choosing the nearest one for comparison.
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,waiting_interval*1000,minDistance, this); // elapsed time should be specified in milliseconds; distance in meters
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i("MYLOG", "NearestPlace SERVICE HAS STOPPED!");
//        lm.removeUpdates(this);
        super.onDestroy();
    }

    /**
     * Looks the nearest place based on user's location
     * @return the Song which is nearest.
     */
    public Song getNearestPlace(Location location) {
        // First of all, the nearest distance limit value set by the user is retrieved:
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        float shortestDistance = Float.valueOf(sharedPref.getString("locationDistance", "200")); // This refers to the radius distance where the nearest place can be
        Song nearestSong = null;
        for (Song p : ListSongsActivity.arraylSongs_backup) { // The list of places is directly obtained from ListSongsActivity
//            if (p.getLongitude() != 0 && p.getLatitude() != 0) {
//                Location tmp_lo = new Location("");
//                tmp_lo.setLongitude(p.getLongitude());
//                tmp_lo.setLatitude(p.getLatitude());
//                Log.i("MYLOG", "distanceTo: " + location.distanceTo(tmp_lo));
//                if (location.distanceTo(tmp_lo) <= shortestDistance) {
//                    nearestSong = new Song(p.getName(),p.getNeighborhood(),p.getDescription(),p.getlContacts(),p.getLongitude(),p.getLatitude());
//                    distanceToPlace = location.distanceTo(tmp_lo);
//                }
//            }
        }
        return nearestSong;
    }

    /**
     * What to do when user's location has changed. Now, it is necessary to check whether the user is still next to previous
     * nearest place or the latter has changed.
     * @param location user's current location
     */
    @Override
    public void onLocationChanged(Location location) {
        // The nearest Song is obtained
        Song tmpPl = getNearestPlace(location);
        // Obtain its Longitude and Latitude values. Depending on whether they are different from the previous nearest place,
        // then, we should display a notification.
        if (tmpPl != null) {
//            if (tmpPl.getLongitude() != nearestPlaceLongitude || tmpPl.getLatitude() != nearestPlaceLatitude) {
//                // The nearest place changed!!
//                // So, we need to notify the user:
//                    // Creating the notification:
//                    NotificationCompat.Builder nBuilder =
//                            new NotificationCompat.Builder(getApplicationContext())
//                                    .setSmallIcon(es.deusto.arduinosinger.R.drawable.ic_location_drop_on_black_24dp)
//                                    .setContentTitle("You are next to one of your favourite places!")
//                                    .setContentText(tmpPl.getName() + " is approx. " + Math.round(distanceToPlace) + "m from you! Would you fancy visiting? :)");
//                    Notification notif = nBuilder.build();
//
//                    // Displaying the notification:
//                    NotificationManager mNotificationManager =
//                            (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
//                    mNotificationManager.notify(0, notif);
//
//                // Assigning (updating) now the new coordinate values for the place:
//                nearestPlaceLongitude = tmpPl.getLongitude();
//                nearestPlaceLatitude = tmpPl.getLatitude();
//            }
        }
    }

    // THE FOLLOWING METHODS HAD TO BE IMPLEMENTED BECAUSE OF THE IMPLEMENTATION OF THE INTERFACE:
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    /**
     * Removes the notification with id 0 from the notification area
     * @param context current context
     */
    private void removeNotification(Context context){
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(0);
    }
}
