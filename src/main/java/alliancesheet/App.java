package alliancesheet;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;

import javax.swing.JOptionPane;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

public class App {

    public static final String TBA_API_KEY = "KYyfzxvdzhHGSE6ENeT6H7sxMJsO7Gzp0BMEi7AE3nTR7pHSsmKOSKAblMInnSfw";
    public static final int YEAR = 2023;
    public static final String API_URL = "http://www.thebluealliance.com/api/v3";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();

    public static String makeRequest(String endpoint) throws IOException {
        endpoint = API_URL + endpoint;
        URL url = new URL(endpoint);

        int responseCode = -1;
        HttpURLConnection con = null;

        do {
            con = responseCode == -1 ? (HttpURLConnection) url.openConnection()
                    : (HttpURLConnection) new URL(con.getHeaderField("Location")).openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("X-TBA-Auth-Key", TBA_API_KEY);
            con.setInstanceFollowRedirects(true);
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            responseCode = con.getResponseCode();
        } while (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                || responseCode == HttpURLConnection.HTTP_MOVED_PERM);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        return content.toString();
    }

    public static void main(String[] args) throws IOException {
        String teamNumber = JOptionPane.showInputDialog("Enter Team #");

        TeamData teamData = GSON.fromJson(makeRequest(String.format("/team/frc%s", teamNumber)), TeamData.class);
        Type type = new TypeToken<List<RobotData>>() {
        }.getType();
        // doesnt contains modern info :(
        List<RobotData> pastRobotYears = GSON.fromJson(makeRequest(String.format("/team/frc%s/robots", teamNumber)),
                type);
        List<EventData> eventData = GSON.fromJson(
                makeRequest(String.format("/team/frc%s/events/%d/simple", teamNumber, YEAR)),
                new TypeToken<List<EventData>>() {
                }.getType());

        List<MatchData> allMatchData = GSON.fromJson(makeRequest(String.format("/team/frc%s/matches/%d", teamNumber, YEAR)), new TypeToken<List<MatchData>>() {
        }.getType());

        int wins = 0;
        int totalMatches = allMatchData.size();
        int totalPoints = 0;

        for(MatchData md : allMatchData) {
            boolean won = false;
            if(Util.isRedAlliance(teamNumber, md)) {
                if(md.winning_alliance.equals("red")) won = true;
                totalPoints += md.alliances.red.score;
            } else {
                if(!md.winning_alliance.equals("red")) won = true;
                totalPoints += md.alliances.blue.score;
            }
            if(won) wins++;
        }

        String pdfDestination = teamNumber + "-data.pdf";


        PdfWriter writer = new PdfWriter(pdfDestination);

        PdfDocument pdf = new PdfDocument(writer);

        Document document = new Document(pdf);

        Paragraph headerTitle = new Paragraph("Team " + teamNumber + ": " + teamData.nickname);
        headerTitle.setTextAlignment(TextAlignment.CENTER);
        headerTitle.setFontColor(com.itextpdf.kernel.colors.Color.convertRgbToCmyk(new DeviceRgb(0, 255, 0)));
        headerTitle.setFontSize(20);
        headerTitle.setBold();

        document.add(headerTitle);

        document.add(new Paragraph(teamData.motto != null ? teamData.motto : ""));
        document.add(new Paragraph(teamData.website != null ? teamData.website : ""));
        
        document.add(new Paragraph("rookie year: " + teamData.getRookie_year()));

        float avgAlliancePoints = ((float)totalPoints / (float)totalMatches);
        float wlRatio = ((float)wins / (float)totalMatches);

        // System.out.println(avgAlliancePoints + " - " + wlRatio);
        document.add(new Paragraph(String.format("win/loss ratio: %.2f", wlRatio)));
        

        System.out.println("won " + wins + " out of " + totalMatches);
        System.out.println("total points: " + totalPoints);
        document.close();
    }
}
