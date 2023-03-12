package alliancesheet;

import com.google.gson.annotations.SerializedName;

public class EventData {
    private static class DistrictData {
        @SerializedName("abbreviation")
        String abbr;
        String display_name;
        String key;
        int year;
    }
    String key;
    String name;
    String event_code;
    int event_type; // TODO: find out what this is
    DistrictData district;
    String city;
    String state_prov;
    String country;
    String start_date, end_date;
    int year;
}
