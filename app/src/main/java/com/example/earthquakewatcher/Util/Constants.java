package com.example.earthquakewatcher.Util;

import java.util.Random;

public class Constants {
    public static final String URL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_week.geojson";
    public static final int LIMIT = 30;   // Limits the number of earthquakes displayed to 30



    // a custom function for displaying different marker colors on the map
    public static int randomInt(int max, int min){
        return new Random().nextInt(max - min) + min;

    }
}
