package es.deusto.arduinosinger;

import java.io.Serializable;

/**
 * Created by aitor on 3/17/17.
 */

public class Contact implements Serializable {

    private String name;
    private String phoneNumber;

    public Contact() {}

    public Contact(String n, String num) {
        this.name = n;
        this.phoneNumber = num;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
