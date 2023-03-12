package alliancesheet;

import java.util.List;

public class MatchData {

    public static class AllianceData {
        int score;
        List<String> team_keys;
        List<String> surrogate_team_keys;
        List<String> dq_team_keys;
    }

    public static class BothAllianceData {
        AllianceData red, blue;
    }

    public static class Video {
        String type, key;
    }

    String key;
    String comp_level; // e.g. "qm"
    int set_number;
    int match_number;
    BothAllianceData alliances;
    String winning_alliance; // red or blue
    String event_key;
    int time;
    int actual_time;
    int predicted_time;
    int post_result_time;
    // score_breakdown
    List<Video> videos;
}
