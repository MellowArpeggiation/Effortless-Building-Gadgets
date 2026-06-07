package net.mellow.effortless.util;

public class MathUtil {

    public static String getShortNumber(long l) {
        double res;
        String magnitude_letter = "";

        if (Math.abs(l) >= Math.pow(10, 18)) {
            res = l / Math.pow(10, 18);
            magnitude_letter = "E";
        } else if (Math.abs(l) >= Math.pow(10, 15)) {
            res = l / Math.pow(10, 15);
            magnitude_letter = "P";
        } else if (Math.abs(l) >= Math.pow(10, 12)) {
            res = l / Math.pow(10, 12);
            magnitude_letter = "T";
        } else if (Math.abs(l) >= Math.pow(10, 9)) {
            res = l / Math.pow(10, 9);
            magnitude_letter = "G";
        } else if (Math.abs(l) >= Math.pow(10, 6)) {
            res = l / Math.pow(10, 6);
            magnitude_letter = "M";
        } else if (Math.abs(l) >= Math.pow(10, 3)) {
            res = l / Math.pow(10, 3);
            magnitude_letter = "k";
        } else {
            return Long.toString(l);
        }

        // Edgecase: a negative triple digit number would result in a 8 character long result so we will loose one decimal place
        if (res <= -100.0) {
            res = Math.round(res * 10.0) / 10.0;
        } else {
            res = Math.round(res * 100.0) / 100.0;
        }

        return res + magnitude_letter;
    }
    
}
