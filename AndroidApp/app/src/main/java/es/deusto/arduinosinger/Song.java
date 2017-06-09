package es.deusto.arduinosinger;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by aitor on 8/6/17
 * This class represents the main Domain object which is a Song
 */

public class Song implements Serializable{
    private String name;
    private String description;
    private String lyric;
    private String imageName;

    // CONSTRUCTORS:

    public Song() {}

    public Song(String na, String lyric, String desc) {
        this.name = na;
        this.description = desc;
        this.lyric = lyric;
    }

    public Song(String na, String lyric, String desc, String imgname) {
        this.name = na;
        this.description = desc;
        this.lyric = lyric;
        this.imageName = imgname;
    }

    // METHODS:

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLyric() {
        return lyric;
    }

    public void setLyric(String lyric) {
        this.lyric = lyric;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
}
