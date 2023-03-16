package alliancesheet;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.WriterException;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.image.PngImageData;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;

public class App {

    public static final String TBA_API_KEY = "KYyfzxvdzhHGSE6ENeT6H7sxMJsO7Gzp0BMEi7AE3nTR7pHSsmKOSKAblMInnSfw";
    public static final int YEAR = 2023;
    public static final String API_URL = "http://www.thebluealliance.com/api/v3";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();

    public static String formatCompLevel(MatchData md) {
        switch (md.comp_level) {
            case "pm":
                return "Practice";
            case "qm":
                return "Qualifier";
            case "sf":
                return "Semi-Final";
            case "f":
                return "Final";
            default:
                return "Unknown comp_level";
        }
    }

    public static String getGoodScoreMessage(final String teamNumber, final MatchData matchData) {
        final int higherPoints = Math.max(matchData.alliances.blue.score, matchData.alliances.red.score);
        final int lowerPoints = Math.min(matchData.alliances.blue.score, matchData.alliances.red.score);
        final boolean won = Util.isRedAlliance(teamNumber, matchData) ? matchData.winning_alliance.equals("red")
                : matchData.winning_alliance.equals("blue");

        if (won) {
            return "Won " + higherPoints + "-" + lowerPoints;
        } else {
            return "Lost " + lowerPoints + "-" + higherPoints;
        }
    }

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
            con.setConnectTimeout(100000);
            con.setReadTimeout(100000);
            responseCode = con.getResponseCode();
        } while (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                || responseCode == HttpURLConnection.HTTP_MOVED_PERM);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        return content.toString();
    }

    public static void generatePDF(String teamNumber) throws Throwable {
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

        document.add(new Paragraph(String.format("win/loss ratio: %.2f", wlRatio)));
        document.add(new Paragraph(String.format("average alliance points: %.2f", avgAlliancePoints)));
        
        List<MatchData> sortedMdata = new ArrayList<MatchData>(allMatchData);

        sortedMdata.sort((o1, o2) -> o2.actual_time - o1.actual_time);

        sortedMdata = sortedMdata.stream()/*.limit(3)*/.collect(Collectors.toList());

        Table threeMatchData = new Table(4);
        for(MatchData matchData : sortedMdata) {
            threeMatchData.addCell(matchData.key);
            threeMatchData.addCell(formatCompLevel(matchData));
            threeMatchData.addCell(getGoodScoreMessage(teamNumber, matchData));
            String tbaURL = String.format("https://www.thebluealliance.com/match/%s", matchData.key); 
            //https://www.thebluealliance.com/match/2023mndu_qm79
            BufferedImage br = Util.createQRImage(tbaURL, 40);
            byte[] bArr = Util.imageToByteArray(br);
            Image qrImg = new Image(ImageDataFactory.create(bArr));
            threeMatchData.addCell(qrImg);

        }
        
        document.add(threeMatchData);

        //TODO

        // - final placing in any events
        // - last (5?) matches with scores
        // - OPR & DPR
        // - total game pieces?
        // - rank?
        
        document.close();

        System.out.println("Finished " + teamNumber);

        // to fix ratelimiting
        Thread.sleep(1000);
    }

    public static void main(String[] args) throws Throwable {
        
        String teamNumbers = JOptionPane.showInputDialog("Enter Team #");

        for(String teamNumber : teamNumbers.split(",")) {
            generatePDF(teamNumber);
        }

        
    }
}
