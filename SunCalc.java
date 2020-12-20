import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/*
 (c) 2011-2015, Vladimir Agafonkin
 SunCalc is a JavaScript library for calculating sun/moon position and light phases.
 https://github.com/mourner/suncalc
*/

class SunCalc {

    public double PI  = Math.PI,
                  rad = PI / 180;

    public int J2000 = 2451545;


    //Based on https://www.aa.quae.nl/en/reken/juliaansedag.html#3_1
    public long toJulian(LocalDate date) {
        int year = date.getYear(),
            month = date.getMonthValue(),
            day = date.getDayOfMonth();

        System.out.println("Year: " + year + "; month: " + month + "; day: "+ day);

        double c  = Math.floor((month - 3) / 12.0);
        double x4 = year + c;
        int x3 = (int) x4 / 100;
        int x2 = (int) x4 % 100;
        double x1 = month - 12 * c - 3;

        System.out.println("c: " + c + "; x4: " + x4 + "; x3: " + x3 + "; x2: " + x2 + "; x1: " + x1);
        
        return (long) Math.round(((146097 * x3) / 4) + ((36525 * x2) / 100) + ((153 * x1 + 2) / 5) + day + 1721119);
    }

    //Based on https://www.aa.quae.nl/en/reken/juliaansedag.html#3_2
    public LocalDate fromJulian(long jdn) { //jdn = julian day number
        long k3 = 4 * (jdn - 1721120) + 3;
        int x3 = Math.round(k3 / 146097);

        long k2 = 100 * ((k3 % 146097) / 4) + 99;
        int x2 = (int) k2 / 36525;

        long k1 = 5 * ((k2 % 36525) / 100) + 2;
        int x1 = (int) k1 / 153;

        int c0 = (x1 + 2) / 12;

        int year = 100 * x3 + x2 + c0;
        int month = x1 - 12 * c0 + 3;
        int day = (int) ((k1 % 153) / 5) + 1;

        LocalDate date = LocalDate.of(year, month, day);
        return date;
    }

    public long toDays(LocalDate date) {
        return toJulian(date) - J2000;
    }


    //General Calculations for Position
    double e = rad * 23.4397; //Schieflage der Erde

    
    public double rightAscension(double l, int b) {
        return Math.atan2( Math.sin(l) * Math.cos(e) - Math.tan(b) * Math.sin(e), Math.cos(l));
    }

    public double declination(double l, int b) {
        return Math.asin(Math.sin(b) * Math.cos(e) + Math.cos(b) * Math.sin(e) * Math.sin(l));
    }

    public double azimuth(int H, int phi, int dec) {
        return Math.atan2(Math.sin(H), Math.cos(H) * Math.sin(phi) - Math.tan(dec) * Math.cos(phi));
    }

    public double altitude(int H, int phi, int dec) {
        return Math.asin(Math.sin(phi) * Math.sin(dec) + Math.cos(phi) * Math.cos(dec) * Math.cos(H));
    }

    public double sidereadlTime(int d, int lw) {
        return rad * (280.16 + 360.9856235 * d) - lw;
    }

    public double astroRefraction(int h) {
        if (h < 0) // the following formula works for positive altitudes only.
        h = 0; // if h = -0.08901179 a div/0 would occur.

        // formula 16.4 of "Astronomical Algorithms" 2nd edition by Jean Meeus (Willmann-Bell, Richmond) 1998.
        // 1.02 / tan(h + 10.26 / (h + 5.10)) h in degrees, result in arc minutes -> converted to rad:
        return 0.0002967 / Math.tan(h + 0.00312536 / (h + 0.08901179));
    }



    // General sun Calculations
    public double solarMeanAnomaly(int d) {
        return rad * (357.5291 + 0.98560028 * d); 
    }

    public double eclipticLongitude(double M) {
        double C = rad * (1.9148 * Math.sin(M) + 0.02 * Math.sin(2 * M) + 0.0003 * Math.sin(3 * M)), // equation of center
        P = rad * 102.9372; // perihelion of the Earth

        return M + C + P + PI;
    }

    public HashMap<String,Double> sunCoords(int d) {
        double M = solarMeanAnomaly(d),
            L = eclipticLongitude(M);

        HashMap<String, Double> map = new HashMap<String, Double>();
        map.put("dec", declination(L, 0));
        map.put("ra", rightAscension(L, 0));

        return map;
    }
}