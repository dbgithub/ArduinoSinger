package es.deusto.arduinosinger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class PlaceDetailsActivity extends AppCompatActivity {

    public static final String PLACE_DETAILS = "PLACE_DETAILS";
    public static final int EDIT_PLACE = 0; // ID for EditPlace Intent
    private Place tmpP;
    private boolean fieldsUpdated = false;
    private ArrayList<Contact> arraylContacts = new ArrayList<>();
    private ArrayAdapter<Contact> arrayadapContacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(es.deusto.arduinosinger.R.layout.activity_place_details);
        Toolbar toolbar = (Toolbar) findViewById(es.deusto.arduinosinger.R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(es.deusto.arduinosinger.R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent editPlaceIntent = new Intent(getBaseContext(), CreateEditPlaceActivity.class);
                editPlaceIntent.putExtra(CreateEditPlaceActivity.PLACE_EDIT, tmpP);
                startActivityForResult(editPlaceIntent, EDIT_PLACE);
            }
        });

        tmpP = (Place)getIntent().getSerializableExtra("PLACE_DETAILS");
        arraylContacts.addAll(tmpP.getlContacts());
        arrayadapContacts = new ArrayAdapter<Contact>(this, android.R.layout.simple_list_item_2, android.R.id.text1, arraylContacts) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                text1.setText(tmpP.getlContacts().get(position).getName());
                text2.setText(tmpP.getlContacts().get(position).getPhoneNumber());
                return view;
            }
        };
        ListView lv_associatedContacts = (ListView) findViewById(es.deusto.arduinosinger.R.id.listView_details);
        lv_associatedContacts.setAdapter(arrayadapContacts);

        updateFields();
    }

    @Override
    public void onBackPressed() {
        if (fieldsUpdated) {
            Log.i("INTENT!", "fieldsUpdate is TRUE, onBackPressed executed!");
            Intent myintent = new Intent();
            myintent.putExtra("place", tmpP);
            setResult(Activity.RESULT_OK, myintent);
            fieldsUpdated = false;
        }
            super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_PLACE){ // If the Intent code was "EDIT_PLACE", then we update the information on the screen
            if(resultCode == Activity.RESULT_OK){
                // Normally you'd do: "data.getStringExtra" or similar. But in this case we are retrieving a Serializable object
                tmpP = (Place) data.getSerializableExtra("place");
                updateFields();
                fieldsUpdated = true; // We indicate that an update was performed
            }
        }
    }

    /**
     * Populates the Object passed from the list of Places OR updates the information after the Place was edited.
     * In either way, all fields will be rewritten.
     */
    private void updateFields() {
        Log.i("TRAZA", "updateFields() llamado!!!");
        CollapsingToolbarLayout tb = (CollapsingToolbarLayout)findViewById(es.deusto.arduinosinger.R.id.toolbar_layout);
        TextView tv_location = (TextView) findViewById(es.deusto.arduinosinger.R.id.tv_neighborhood);
        TextView tv_description = (TextView) findViewById(es.deusto.arduinosinger.R.id.tv_description);
        TextView tv_coordinates = (TextView) findViewById(es.deusto.arduinosinger.R.id.tv_coordinates);
        tb.setTitle(tmpP.getName());
        tv_location.setText(tmpP.getNeighborhood());
        tv_description.setText(tmpP.getDescription());
        tv_coordinates.setText("("+Double.toString(tmpP.getLongitude()) + " , " + Double.toString(tmpP.getLatitude())+")");
        arraylContacts.clear();
        arraylContacts.addAll(tmpP.getlContacts()); // Be careful with REFERENCES TO lists. The normal procedure is to clear the list and add them back again (the new items).
        arrayadapContacts.notifyDataSetChanged();
    }
}
