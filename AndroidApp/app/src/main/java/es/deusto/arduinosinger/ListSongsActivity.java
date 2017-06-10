package es.deusto.arduinosinger;

import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ListSongsActivity extends AppCompatActivity implements DialogInterface.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    // Declaring a ArrayList of Song entity:
    public ArrayList<Song> arraylSongs = new ArrayList<>();
    public static ArrayList<Song> arraylSongs_backup = new ArrayList<>(); // This list will be used to restore the previous items after a search (like a backup)
    private ArrayAdapter<Song> arrayadapSongs;
    public static final int CREATE_SONG = 0; // ID for CreateSong Intent
    public static final int EDIT_SONG = 1; // ID for CreateEditSong Intent
    private static final int  MY_PERMISSIONS_BT = 2; // 1 doesn't mean True, it's just an ID.
    private static final int  MY_PERMISSIONS_BT_ADMIN = 4; // 1 doesn't mean True, it's just an ID.
    private int edited_song_index;
    private ActionMode mActionMode = null; // Action mode for CAB (Contextual Action Bar)
    private int sortType = -1;
    public boolean isLoadDummySongs; // Boolean for loading dummy songs at the start of the application

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(es.deusto.arduinosinger.R.layout.activity_list_songs);
        Toolbar toolbar = (Toolbar) findViewById(es.deusto.arduinosinger.R.id.toolbar);
        setSupportActionBar(toolbar);
        PreferenceManager.setDefaultValues(this, es.deusto.arduinosinger.R.xml.settings_preferences, false); // Loads DEFAULT values for any item in Settings/Preferences page.
        PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).registerOnSharedPreferenceChangeListener(this); // Binds a callback listener for changes in preferences/settings page:
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        isLoadDummySongs = sharedPref.getBoolean("LoadDummySongsAtStart", true); // Indicates if dummy songs have to be loaded at the start of the app (Settings/Preferences page)
        Log.i("MYLOG", "SharedPreferences (Settings page) for 'LoadDummySongsAtStart' is: " + isLoadDummySongs );

        FloatingActionButton fab = (FloatingActionButton) findViewById(es.deusto.arduinosinger.R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                        // Ejemplo de un snackbar como mensaje emergente: Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                Intent createSonglIntent = new Intent(getBaseContext(), CreateEditSongActivity.class);
                startActivityForResult(createSonglIntent, CREATE_SONG);
            }
        });

        loadDummySongs(); // loads the data from internal storage (if any)
        arrayadapSongs = new ArrayAdapter<Song>(this, es.deusto.arduinosinger.R.layout.list_item_song, es.deusto.arduinosinger.R.id.list_item_song_tv_description, arraylSongs) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView name = (TextView) view.findViewById(es.deusto.arduinosinger.R.id.list_item_song_tv_title);
                TextView description = (TextView) view.findViewById(es.deusto.arduinosinger.R.id.list_item_song_tv_description);
                name.setText(arraylSongs.get(position).getName());
                description.setText(arraylSongs.get(position).getDescription());

                ImageView imgView = (ImageView) view.findViewById(R.id.list_item_song_img);
                String uri = "@drawable/" + arraylSongs.get(position).getImageName(); // Building the URI for every image.
                int imageResourceID = getResources().getIdentifier(uri, null, getPackageName()); // Get the ID of the image resource given the URI

                // Loading (large) bitmaps efficiently (more info at: https://developer.android.com/topic/performance/graphics/load-bitmap.html):
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeResource(getResources(),imageResourceID, options);
                imgView.setImageBitmap(decodeSampledBitmapFromResource(getResources(), imageResourceID, 100, 100));
                return view;
            }

            // It is necessary to override the getFilter method in order to implement the search feature:
            @NonNull
            @Override
            public Filter getFilter() {
                return new Filter() {
                    /**
                     * After a search, this method return a list of found items (if any)
                     * @param constraint the word or phrase being filtered
                     * @param results a set of elements containing the searched string
                     */
                    @SuppressWarnings("unchecked")
                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        arraylSongs.clear();
                        arraylSongs.addAll((ArrayList<Song>) results.values);
                        notifyDataSetChanged();
                    }

                    /**
                     * When the user searches something, this method filters the ListView according to the query string
                     * @param constraint the word or phrase being filtered
                     * @return a set of elements containing the searched string
                     */
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        ArrayList<Song> FilteredArrayNames = new ArrayList<>();

                        // Now, we look for the word we are interested in
                        constraint = constraint.toString().toLowerCase();
                        for (int i = 0; i < arraylSongs.size(); i++) {
                            Song p = arraylSongs.get(i);
                            if (p.getName().toLowerCase().contains(constraint.toString()))  {
                                FilteredArrayNames.add(p);
                            }
                        }
                        results.count = FilteredArrayNames.size();
                        results.values = FilteredArrayNames;
                        return results;
                    }
                };
            }
        };
        // LIST VIEW listing songs, if any:
        final ListView lv_songs = (ListView) findViewById(es.deusto.arduinosinger.R.id.lv_places);
        lv_songs.setAdapter(arrayadapSongs);
        lv_songs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                edited_song_index = position;

                // Checking for BT permissions:
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED) {
                    // Permission DENIED!
                    // So, we request him/her to grant the permission for that purpose:
                    ActivityCompat.requestPermissions(ListSongsActivity.this, new String[]{Manifest.permission.BLUETOOTH}, MY_PERMISSIONS_BT);
                } else if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_DENIED) {
                    // Permission DENIED!
                    // So, we request him/her to grant the permission for that purpose:
                    ActivityCompat.requestPermissions(ListSongsActivity.this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, MY_PERMISSIONS_BT_ADMIN);
                } else {
                    // Permission GRANTED! (or not needed to ask)
                    // So, we continue as planned...
                    checkBTcompatibility();
                }
            }
        });
        // We make sure Contextual Action Bar is configured as needed:
        lv_songs.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lv_songs.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode != null) {
                    return false;
                }
                // Important! mark the editing row as activated:
                lv_songs.setItemChecked(position,true);
                edited_song_index = position; // save the row position of the item to make changes later on, if needed.
                // Start CAB referencing a callback defined later in the code:
                mActionMode = ListSongsActivity.this.startSupportActionMode(myActionModeCallBack); // Watch out! If we weren't using AppCompatibilityActivity, then we would have to use: ".this.startActionMode(..)"
                return true;
            }
        });

        // If this activity is launched due to an Intent (in this case, ACTION_SEARCH), handleIntent will execute the corresponding code.
        handleIntent(getIntent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(es.deusto.arduinosinger.R.menu.menu_list_places, menu);

        // Associate searchable configuration with the SearchView:
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(es.deusto.arduinosinger.R.id.app_bar_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        // Remember that the ShareActionProvider is implemented in the ContextualActionBar's (CAB) callback
        return true;
    }

    /**
     * Callback used to response to the actions taken from CAB (Contextual Action Bar).
     * It's like CAB's own life cycle where you can define and implement specific behavior for CAB.
     * REMEMBER! if you want the CAB overlay the standard bar, then change your styles.xml adding "<item name="windowActionModeOverlay">true</item>"
     * IMPORTANT: Watch out when implementing ShareActionProvider when your activity is backwards compatible. ActionMode Callbacks should be imported with the compatibility library (..support.v7.view..).
     */
    private android.support.v7.view.ActionMode.Callback myActionModeCallBack = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(es.deusto.arduinosinger.R.menu.menu_cab_list_places, menu);
            // WATCH OUT: To integrate ShareActionProvider with backwards compatibility follow the steps at: https://developer.android.com/reference/android/support/v7/widget/ShareActionProvider.html
            // WATCH OUT: To integrate ShareActionProvider with a normal Activity (without compatibility) follow the steps at: https://developer.android.com/reference/android/widget/ShareActionProvider.html
            // WATCH OUT: More help if needed: http://stackoverflow.com/questions/24219842/getactionprovider-item-does-not-implement-supportmenuitem
            // Bind share icon with share functionality:
            MenuItem mnuShare = menu.findItem(es.deusto.arduinosinger.R.id.mnu_cab_share_song);
            ShareActionProvider shareProv = (ShareActionProvider) MenuItemCompat.getActionProvider(mnuShare); // If we were not using AppCompatActivity, we would had to use: (ShareActionProvider) mnuShare.getActionProvider();
            shareProv.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Hey! Would you fancy listening to this song?! '"+ arraylSongs.get(edited_song_index).getName()+"'\n\n"+
                    ((arraylSongs.get(edited_song_index).getDescription().length() >= 100)? arraylSongs.get(edited_song_index).getDescription().substring(0,100) : arraylSongs.get(edited_song_index).getDescription())  +"...\n\nCheck it out! (ArduinoSinger app)");
            shareProv.setShareIntent(shareIntent);
            return true;
        }

        // Called when the user enters the action mode (that is, CAB is displayed)
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            ListView lv_places = (ListView) findViewById(es.deusto.arduinosinger.R.id.lv_places);
            lv_places.setEnabled(false);
            return false;
        }

        /**
         * Implements the business logic related to what happens when the user clicks on an item on the CAB.
         * @param mode the Contextual Action Bar itself
         * @param item an item on which the user has interacted (tapped or similar)
         * @return boolean
         */
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.mnu_cab_edit_song:
                    Intent editSongIntent = new Intent(getBaseContext(), CreateEditSongActivity.class);
                    editSongIntent.putExtra(CreateEditSongActivity.SONG_EDIT, arraylSongs.get(edited_song_index));
                    startActivityForResult(editSongIntent, EDIT_SONG);
                    return false;
                case es.deusto.arduinosinger.R.id.mnu_cab_delete_song:
                    arraylSongs.remove(edited_song_index);
                    arraylSongs_backup.remove(edited_song_index);
                    arrayadapSongs.notifyDataSetChanged();
                    Toast.makeText(getApplicationContext(), "Song deleted :)", Toast.LENGTH_LONG).show();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user EXITS the action mode (that is, when CAB fades away)
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Re-enable the list after edition:
            ListView lv_places = (ListView) findViewById(es.deusto.arduinosinger.R.id.lv_places);
            lv_places.setEnabled(true);
            // Reset the CAB:
            mActionMode = null;
        }
    };

    /**
     * Overrides the behaviour of what happens when a new Intent is launched from within this Activity.
     * This current activity is prepared to receive just ACTION_SEARCH intents so as to implement the search feature
     * @param intent the Intent the Activity is capturing when it launches an Intent.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    /**
     * Executes the filtering on the ListView based on the query string.
     * @param intent the Intent the Activity is capturing
     */
    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            arrayadapSongs.getFilter().filter(query);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case es.deusto.arduinosinger.R.id.app_remove_search_filter:
                resetListView();
                break;
            case es.deusto.arduinosinger.R.id.action_settings:
                startActivity(new Intent(this, MySettingsActivity.class));
                break;
            case es.deusto.arduinosinger.R.id.app_sort_by:
                launchSortByDialog();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_BT:
                if (grantResults[0] == -1) {
                    // If the user did not grant the permission, then, booo..., back luck for you!
                } else {
                    // The user granted the permission! :)
                    // Now we check another permission:
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_DENIED) {
                        // Permission DENIED!
                        // So, we request him/her to grant the permission for that purpose:
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, MY_PERMISSIONS_BT_ADMIN);
                    } else {
                        // Permission GRANTED! (or not needed to ask)
                        // Here we continue as expected:
                        checkBTcompatibility();
                    }
                }
                break;
            case MY_PERMISSIONS_BT_ADMIN:
                if (grantResults[0] == -1) {
                    // If the user did not grant the permission, then, booo..., back luck for you!
                } else {
                    // The user granted the permission! :)
                    // Here we continue as expected:
                    checkBTcompatibility();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_SONG){ // If the Intent code was "CREATE_SONG", then add the new item and update the list
            if(resultCode == Activity.RESULT_OK){
                // Normally you'd do: "data.getStringExtra" or similar. But in this case we are retrieving a Serializable object
                arraylSongs.add((Song) data.getSerializableExtra("place"));
                arraylSongs_backup.add((Song) data.getSerializableExtra("place"));
                arrayadapSongs.notifyDataSetChanged();
            }
        } else if (requestCode == EDIT_SONG) { // This will happen when the user goes from the 'details' Activity back to the main Activity.
            if(resultCode == Activity.RESULT_OK) {
                arraylSongs.set(edited_song_index, (Song) data.getSerializableExtra("place"));
                arraylSongs_backup.set(edited_song_index, (Song) data.getSerializableExtra("place"));
                arrayadapSongs.notifyDataSetChanged();
            }
                // Reset the CAB:
                mActionMode.finish();
                mActionMode = null;
        }
    }

    /**
     * Creates and shows a dialog box with multiple choice radio buttons in order to sort the songs.
     */
    private void launchSortByDialog() {
        // A Dialog is launched in order to offer a choice of ordering:
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(ListSongsActivity.this);
        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setTitle("Sort by...")
                .setSingleChoiceItems(es.deusto.arduinosinger.R.array.sortingpreferences, sortType, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                // Sort by most played song!
                                sortType = 0;
                                break;
                            case 1:
                                // Revert back to default sorting. Show all.
                                sortType = 1;
                                break;
                            default:
                                break;
                        }
                    }
                });
                //.setMessage("Heeelloooooooooooooooooo") // setMessage is not compatible when you set multiple items to display
        // 3. Add the buttons. When launching dialogs, you decide whether passing the events back to the Dialog's host or manage it within the dialog implementation
        builder.setPositiveButton("OK", ListSongsActivity.this);
        builder.setNegativeButton("CANCEL", ListSongsActivity.this);
                    // Example of managing the event within the dialog implementation:
//                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
//                            // User clicked OK button
//                        }
//                    });
        // 4. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * When launching dialogs, you decide whether passing the events back to the Dialog's host or manage it within the dialog implementation
     * @param dialog
     * @param which
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        // -1 -> OK pressed
        // -2 -> CANCEL pressed
        if (which == -1) {
            if (sortType == 0) {
                // Sort lists by most played song.
                sortOutSongs();
            } else {
                // Revert the list back to default view. Show all places.
                resetListView();
            }
        }
    }

    /**
     * Loads persistent dummy songs. Since the data has to be persistent, we will proceed by reading a file and loading its content.
     * If there is nothing to load or the list is empty, by default we load dummy items.
     */
    private void loadDummySongs() {
        // Persistent way of loading data:
        arraylSongs = new PersistanceManager(getApplicationContext()).loadSongs();
        arraylSongs_backup = new PersistanceManager(getApplicationContext()).loadSongs();
        if (arraylSongs != null) {
            if (arraylSongs.isEmpty()) {if(isLoadDummySongs) { dumpData(false); dumpData(true);}} // We need also to check the Preferences/Settings page
        } else {
            arraylSongs = new ArrayList<>();
            if(isLoadDummySongs) {dumpData(false);} // We need also to check the Preferences/Settings page
        }
        if (arraylSongs_backup == null) {
            arraylSongs_backup = new ArrayList<>(); // We need also to check the Preferences/Settings page
            if(isLoadDummySongs) {dumpData(true);}} // We need also to check the Preferences/Settings page
    }

    /**
     * Auxiliary function to dump dummy data in the corresponding ArrayList.
     * @param isBackup
     */
    private void dumpData(boolean isBackup) {
        if (isBackup) {
            // When there is no songs, we load dummy songs as examples:
            arraylSongs_backup.add(new Song("20th Century", getResources().getString(R.string.lyric_twentycentury), getResources().getString(R.string.desc_20century), getResources().getString(R.string.song_image_twentycentury)));
            arraylSongs_backup.add(new Song("Blue", getResources().getString(R.string.lyric_blue), getResources().getString(R.string.desc_blue), getResources().getString(R.string.song_image_blue)));
            arraylSongs_backup.add(new Song("Equipo A", getResources().getString(R.string.lyric_equipoa), getResources().getString(R.string.desc_equipoa), getResources().getString(R.string.song_image_equipoa)));
            arraylSongs_backup.add(new Song("Indiana Jones", getResources().getString(R.string.lyric_indianajones), getResources().getString(R.string.desc_indianajones), getResources().getString(R.string.song_image_indianajones)));
            arraylSongs_backup.add(new Song("Los Simpsons", getResources().getString(R.string.lyric_lossimpsons), getResources().getString(R.string.desc_thesimpsons), getResources().getString(R.string.song_image_losimpsons)));
            arraylSongs_backup.add(new Song("Mario Bros", getResources().getString(R.string.lyric_mariobros), getResources().getString(R.string.desc_maribros), getResources().getString(R.string.song_image_mariobros)));
            arraylSongs_backup.add(new Song("Popeye", getResources().getString(R.string.lyric_popeye), getResources().getString(R.string.desc_popeye), getResources().getString(R.string.song_image_popeye)));
            arraylSongs_backup.add(new Song("StarWars", getResources().getString(R.string.lyric_starwars), getResources().getString(R.string.desc_starwars), getResources().getString(R.string.song_image_starwars)));
            arraylSongs_backup.add(new Song("Take on Me", getResources().getString(R.string.lyric_takeonme), getResources().getString(R.string.desc_takeonme), getResources().getString(R.string.song_image_takeonme)));
            arraylSongs_backup.add(new Song("Zelda", getResources().getString(R.string.lyric_zelda), getResources().getString(R.string.desc_zelda), getResources().getString(R.string.song_image_zelda)));
            arraylSongs_backup.add(new Song("M:I", getResources().getString(R.string.lyric_missionimpossible), getResources().getString(R.string.desc_missionimpossible), getResources().getString(R.string.song_image_missionimpossible)));
        } else {
            // When there is no songs, we load dummy songs as examples:
            arraylSongs.add(new Song("20th Century", getResources().getString(R.string.lyric_twentycentury), getResources().getString(R.string.desc_20century), getResources().getString(R.string.song_image_twentycentury)));
            arraylSongs.add(new Song("Blue", getResources().getString(R.string.lyric_blue), getResources().getString(R.string.desc_blue), getResources().getString(R.string.song_image_blue)));
            arraylSongs.add(new Song("Equipo A", getResources().getString(R.string.lyric_equipoa), getResources().getString(R.string.desc_equipoa), getResources().getString(R.string.song_image_equipoa)));
            arraylSongs.add(new Song("Indiana Jones", getResources().getString(R.string.lyric_indianajones), getResources().getString(R.string.desc_indianajones), getResources().getString(R.string.song_image_indianajones)));
            arraylSongs.add(new Song("Los Simpsons", getResources().getString(R.string.lyric_lossimpsons), getResources().getString(R.string.desc_thesimpsons), getResources().getString(R.string.song_image_losimpsons)));
            arraylSongs.add(new Song("Mario Bros", getResources().getString(R.string.lyric_mariobros), getResources().getString(R.string.desc_maribros), getResources().getString(R.string.song_image_mariobros)));
            arraylSongs.add(new Song("Popeye", getResources().getString(R.string.lyric_popeye), getResources().getString(R.string.desc_popeye), getResources().getString(R.string.song_image_popeye)));
            arraylSongs.add(new Song("StarWars", getResources().getString(R.string.lyric_starwars), getResources().getString(R.string.desc_starwars), getResources().getString(R.string.song_image_starwars)));
            arraylSongs.add(new Song("Take on Me", getResources().getString(R.string.lyric_takeonme), getResources().getString(R.string.desc_takeonme), getResources().getString(R.string.song_image_takeonme)));
            arraylSongs.add(new Song("Zelda", getResources().getString(R.string.lyric_zelda), getResources().getString(R.string.desc_zelda), getResources().getString(R.string.song_image_zelda)));
            arraylSongs.add(new Song("M:I", getResources().getString(R.string.lyric_missionimpossible), getResources().getString(R.string.desc_missionimpossible), getResources().getString(R.string.song_image_missionimpossible)));
        }
    }

    /**
     * Reset the ListView Song by loading back again the content of the backup ArrayList. In other words: list all items again.
     */
    private void resetListView() {
        arraylSongs.clear();
        arraylSongs.addAll(arraylSongs_backup);
        arrayadapSongs.notifyDataSetChanged();
        sortType = -1;
    }

    private void sortOutSongs() {
        arraylSongs.clear();
        for (Song p : arraylSongs_backup) {
//            if (p.getLongitude() != 0 && p.getLatitude() != 0) {
//                Location tmp_lo = new Location("");
//                tmp_lo.setLongitude(p.getLongitude());
//                tmp_lo.setLatitude(p.getLatitude());
//                if (location.distanceTo(tmp_lo) <= sortingDistance*1000) {
//                    Log.i("MYLOG","Esta dentro de un radio de "+sortingDistance+"KM a la redonda (distancia: "+location.distanceTo(tmp_lo)+")");
//                    arraylSongs.add(p);
//                } else {Log.i("MYLOG","Esta fuera de un radio de "+sortingDistance+"KM a la redonda");}
//            }
        }
        arrayadapSongs.notifyDataSetChanged();
    }

    // Life cycle of the activity:

    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * When the app is finishing and it is about to close and to be destroyed, then we make sure that
     * the data is saved/persisted correctly.
     */
    @Override
    protected void onStop(){
        super.onStop();
        // Clear search results (if any). Going back to initial ListView state.
        resetListView();
        (new PersistanceManager(getApplicationContext())).saveSongs(arraylSongs);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("LoadDummySongsAtStart")) {
            isLoadDummySongs = sharedPreferences.getBoolean("LoadDummySongsAtStart", false); // Indicates if dummy songs have to be loaded at the start of the app (Settings/Preferences page)
            Log.i("MYLOG", "SharedPreferences (Settings page) for 'LoadDummySongsAtStart' is: " + isLoadDummySongs );
        }
    }

    /**
     * Returns the largest sample size that the picture can be given the height and width.
     * More info at: https://developer.android.com/topic/performance/graphics/load-bitmap.html
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Returns the bitmap after resizing it
     * @param res
     * @param resId
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * Checks if the device is Bluetooth compatible!! If affirmative, we proceed!
     */
    private void checkBTcompatibility() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.i("MYLOG", "BT is not supported in this device!");
        } else {
            Log.i("MYLOG", "BT is compatible with this device!");
            Intent itemDetailIntent = new Intent(getBaseContext(), SongDetailsActivity.class);
            itemDetailIntent.putExtra(SongDetailsActivity.SONG_DETAILS, arraylSongs.get(edited_song_index));
            startActivity(itemDetailIntent);
        }
    }
}
