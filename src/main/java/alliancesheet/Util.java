package alliancesheet;

import java.io.FileOutputStream;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;

/**
 * The place I put all the code I don't want to look at
 */
public class Util {

    public static boolean isRedAlliance(String teamNumber, MatchData md) {
        for(String tn : md.alliances.red.team_keys) {
            if(tn.equals("frc" + teamNumber)) return true;
        }
        return false;
    }
}
