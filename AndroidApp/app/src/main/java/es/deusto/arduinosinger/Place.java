package es.deusto.arduinosinger;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by aitor on 3/9/17. **UPDATED ON: 19/4/17
 * This class represents the main Domain object which is a Place
 */

public class Place implements Serializable{
    private String name;
    private String neighborhood;
    private String description;
    private double longitude, latitude;
    private ArrayList<Contact> lContacts;

    // CONSTRUCTORS:

    public Place() {}

    public Place(String na, String neighborhood, String desc) {
        this.name = na;
        this.neighborhood = neighborhood;
        this.description = desc;
        this.lContacts = new ArrayList<>();
    }

    public Place(String na, String neighborhood, String desc, double lon, double lat) {
        this.name = na;
        this.neighborhood = neighborhood;
        this.description = desc;
        this.lContacts = new ArrayList<>();
        this.longitude = lon;
        this.latitude = lat;
    }

    public Place(String na, String neighborhood, String desc, ArrayList<Contact> contacts) {
        this.name = na;
        this.neighborhood = neighborhood;
        this.description = desc;
        this.lContacts = contacts;
    }

    public Place(String na, String neighborhood, String desc, ArrayList<Contact> contacts, double lon, double lat) {
        this.name = na;
        this.neighborhood = neighborhood;
        this.description = desc;
        this.lContacts = contacts;
        this.longitude = lon;
        this.latitude = lat;
    }

    // METHODS:

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<Contact> getlContacts() {
        return lContacts;
    }

    public void setlContacts(ArrayList<Contact> lContacts) {
        this.lContacts = lContacts;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
}
