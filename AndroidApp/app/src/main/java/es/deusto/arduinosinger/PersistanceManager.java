package es.deusto.arduinosinger;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;

/**
 * This class ensures that the data is persistent whenever the user closes the app and reopens it.
 * Or just when even the app is brought to background and/or foreground
 * Created by aitor on 08/06/17.
 */

public class PersistanceManager {

    private static final String FILENAME = "Songs.data";
    private Context currentContext;

    public PersistanceManager(Context c) {
        currentContext = c;
    }

    public ArrayList<Song> loadSongs() {
        try {
            FileInputStream fis = currentContext.openFileInput(FILENAME);
            ObjectInputStream ois = new ObjectInputStream(fis);
            //@SuppressWarnings("unchecked")
            ArrayList<Song> arr = (ArrayList<Song>) ois.readObject();
            ois.close();
            fis.close();
            return arr;
        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (OptionalDataException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveSongs(ArrayList<Song> lp) {
        try {
            FileOutputStream fos = currentContext.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(lp);
            oos.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
