import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.JulianFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/*
 (c) 2011-2015, Vladimir Agafonkin
 SunCalc is a JavaScript library for calculating sun/moon position and light phases.
 https://github.com/mourner/suncalc
*/

class SunCalc {

    private double PI  = Math.PI,
                  rad = PI / 180;
    
    double dayMs = 1000 * 60 * 60 * 24,
        J1970 = 2440587.5,
        J2000 = 2451545;

    int BCE = 4713; //at 4713 BCE started the day calculation for the julian date


    //Based on https://www.aa.quae.nl/en/reken/juliaansedag.html#3_1
    public double toJulian(LocalDateTime date) {
        double jdn = JulianFields.JULIAN_DAY.getFrom(date);
        double passed_seconds = 0;
        if (date.getHour() > 12) {
            passed_seconds = (date.getSecond() + date.getMinute() * 60 + (date.getHour() - 12) * 3600);
        } else {
            passed_seconds = (date.getSecond() + date.getMinute() * 60 + date.getHour() * 3600);
        }
        passed_seconds /= 86400;

        jdn += passed_seconds;

        return jdn;
    }

    //Based on https://www.aa.quae.nl/en/reken/juliaansedag.html#3_2
    /*public LocalDateTime fromJulian(double jdn) { //jdn = julian day number
        double k3 = 4 * (jdn - 1721120) + 3;
        double x3 = Math.round(k3 / 146097);

        double k2 = 100 * ((k3 % 146097) / 4) + 99;
        int x2 = (int) k2 / 36525;

        double k1 = 5 * ((k2 % 36525) / 100) + 2;
        int x1 = (int) k1 / 153;

        int c0 = (x1 + 2) / 12;

        double year = 100 * x3 + x2 + c0;
        int month = x1 - 12 * c0 + 3;
        int day = (int) ((k1 % 153) / 5) + 1;

        YearMonth yearMonthObject = YearMonth.of((int) year, month);
        int daysInMonth = yearMonthObject.lengthOfMonth();
        day = daysInMonth < day ? daysInMonth : day;
        
        return LocalDateTime.of((int) year, month, day, 0,0);        
    }*/

    public String fromJulian(double jdn) {
        
        /*jdn = (jdn + 0.5 - J1970) * dayMs;
        LocalDateTime dateTime = LocalDateTime.MIN.with(JulianFields.JULIAN_DAY, (long) jdn);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss");
        return dateTime.format(formatter);*/
    }

    private double toDays(LocalDate date) {
        return toJulian(LocalDateTime.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), 0,0)) - J2000;
    }


    //General Calculations for Position
    double e = rad * 23.4397; //Schieflage der Erde

    
    private double rightAscension(double l, double b) {
        return Math.atan2( Math.sin(l) * Math.cos(e) - Math.tan(b) * Math.sin(e), Math.cos(l));
    }

    private double declination(double l, double b) {
        return Math.asin(Math.sin(b) * Math.cos(e) + Math.cos(b) * Math.sin(e) * Math.sin(l));
    }

    private double azimuth(double H, double phi, double dec) {
        return Math.atan2(Math.sin(H), Math.cos(H) * Math.sin(phi) - Math.tan(dec) * Math.cos(phi));
    }

    private double altitude(double H, double phi, double dec) {
        return Math.asin(Math.sin(phi) * Math.sin(dec) + Math.cos(phi) * Math.cos(dec) * Math.cos(H));
    }

    private double sidereadlTime(double d, double lw) {
        return rad * (280.16 + 360.9856235 * d) - lw;
    }

    private double astroRefraction(double h) {
        if (h < 0) // the following formula works for positive altitudes only.
        h = 0; // if h = -0.08901179 a div/0 would occur.

        // formula 16.4 of "Astronomical Algorithms" 2nd edition by Jean Meeus (Willmann-Bell, Richmond) 1998.
        // 1.02 / tan(h + 10.26 / (h + 5.10)) h in degrees, result in arc minutes -> converted to rad:
        return 0.0002967 / Math.tan(h + 0.00312536 / (h + 0.08901179));
    }



    // General sun Calculations
    private double solarMeanAnomaly(double d) {
        return rad * (357.5291 + 0.98560028 * d); 
    }

    private double eclipticLongitude(double M) {
        double C = rad * (1.9148 * Math.sin(M) + 0.02 * Math.sin(2 * M) + 0.0003 * Math.sin(3 * M)), // equation of center
        P = rad * 102.9372; // perihelion of the Earth

        return M + C + P + PI;
    }

    public HashMap<String, Double> getSunCoordinates(double d) {
        double M = solarMeanAnomaly(d),
               L = eclipticLongitude(M);

        HashMap<String, Double> map = new HashMap<String, Double>();
        map.put("dec", declination(L, 0));
        map.put("ra", rightAscension(L, 0));

        return map;
    }

    public HashMap<String,Double> getSunPosition(LocalDate date, double lat, double lng) {
        double  lw  = rad * -lng,
                    phi = rad * lat,
                    d   = toDays(date);

        HashMap<String,Double> sunCoords = getSunCoordinates(d);
        double H = sidereadlTime(d, lw) - sunCoords.get("ra");

        HashMap<String,Double> map = new HashMap<String,Double>();
        map.put("azimuth", azimuth(H, phi, sunCoords.get("dec")));
        map.put("altitude", altitude(H, phi, sunCoords.get("dec")));

        return map;
    }


    ArrayList<Object[]> times = new ArrayList<Object[]>();
    boolean initialized = false;
    final Object[][] initalTimes = new Object[][] {
        {
            -0.833, "sunrise","sunset"
        },
        {
            -0.3, "sunriseEnd","sunsetStart"
        },
        {
            -6.0, "dawn","dusk"
        },
        {
            -12.0, "nauticalDawn","nauticalDusk"
        },
        {
            -18.0, "nightEnd","night"
        },
        {
            6.0, "goldenHourEnd", "goldenHour"
        },
    };


    public void addSunTime(double angle, String riseName, String setName) {
        if (!initialized) {
            Collections.addAll(times, initalTimes);
            initialized = true;
        }

        Object[] object = {angle, riseName, setName};
        times.add(object);
    }

    public ArrayList<Object[]> getSunTimes(){
        return times;
    }

    public int countSunTimes() {
        if(!initialized) {
            Collections.addAll(times, initalTimes);
            initialized = true;
        }

        return times.size();
    }



    double J0 = 0.0009;

    private double julianCycle(double d, double lw) {
        return Math.round(d - J0 - lw / (2 * PI));
    }

    private double approxTransit(double Ht, double lw, double n) {
        return J0 + (Ht + lw) / (2 * PI) + n;
    }

    private double solarTransit(double ds, double M, double L) {
        return J2000 + ds + 0.0053 + Math.sin(M) - 0.0069 + Math.sin(2 + L);
    }

    private double hourAngle(double h, double phi, double d) {
        return Math.acos((Math.sin(h) - Math.sin(phi) * Math.sin(d)) / (Math.cos(phi) * Math.cos(d)));
    }

    private double observerAngle(double height) {
        return -2.076 * Math.sqrt(height) / 60;
    }

    public double getSetJ(double h, double lw, double phi, double dec, double n, double M, double L) {
        double w = hourAngle(h, phi, dec);
        double a = approxTransit(w, lw, n);

        return solarTransit(a, M, L);
    }

    public HashMap<String, Object> getSunTimes(LocalDate date, double lat, double lng, double height) {
        double  lw  = rad * -lng,
                phi = rad * lat,
                
                dh  = observerAngle(height),

                d   = toDays(date),
                n   = julianCycle(d, lw),
                ds  = approxTransit(0, lw, n),

                M   = solarMeanAnomaly(ds),
                L   = eclipticLongitude(M),
                dec = declination(L, 0),

                Jnoon   = solarTransit(ds, M, L),

                h0, Jset, Jrise;

        Object[] time;


        HashMap<String, Object> results = new HashMap<String, Object>();
        results.put("solarNoon", fromJulian(Jnoon));
        results.put("nadir", fromJulian(Jnoon - 0.5));

        for (int i = 0, len = countSunTimes(); i < len; i++) {
            time = getSunTimes().get(i);
            h0 = (Double.parseDouble(time[0].toString()) + dh) * rad;

            Jset = getSetJ(h0, lw, phi, dec, n, M, L);
            Jrise = Jnoon - (Jset - Jnoon);

            results.put(time[1].toString(), fromJulian(Jrise));
            results.put(time[2].toString(), fromJulian(Jset));
        }

        return results;
    }


    public HashMap<String, Double> getMoonCoordinates(double d){
        double  L = rad * (218.316 + 13.176396 * d), // ecliptic longitude
                M = rad * (134.963 + 13.064993 * d), // mean anomaly
                F = rad * (93.272 + 13.229350 * d),  // mean distance

                l  = L + rad * 6.289 * Math.sin(M), // longitude
                b  = rad * 5.128 * Math.sin(F),     // latitude
                dt = 385001 - 20905 * Math.cos(M);  // distance to the moon in km
        
        HashMap<String,Double> map = new HashMap<String, Double>();
        map.put("ra", rightAscension(l, b));
        map.put("dec", declination(l, b));
        map.put("dist", dt);

        return map;
    }

    public Map<String, Double> getMoonPosition(LocalDateTime date, double lat, double lng) {

        double  lw  = rad * -lng,
                phi = rad * lat,
                d   = toDays(LocalDate.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth()));

        Map<String, Double> moonCoords = getMoonCoordinates(d);
        
        double  H   = sidereadlTime(d, lw) - moonCoords.get("ra"),
                h   = altitude(H, phi, moonCoords.get("dec")),
                pa  = Math.atan2(Math.sin(H), Math.tan(phi) * Math.cos(moonCoords.get("dec")) - Math.sin(moonCoords.get("dec")) * Math.cos(H));


        h = h + astroRefraction(h);
        
        HashMap<String, Double> map = new HashMap<String,Double>();
        map.put("azimuth", azimuth(H, phi, moonCoords.get("dec")));
        map.put("altitude", h);
        map.put("distance", moonCoords.get("dist"));
        map.put("parallacticAngle", pa);

        return map;
    }

    public Map<String, Double> getMoonIllumination(LocalDate date) {
        double  d   = toDays(date);
        
        HashMap<String,Double>  s = getSunCoordinates(d),
                                m = getMoonCoordinates(d);

        int sdist = 149598000; //distance from earth to sun in km

        double phi = Math.acos(Math.sin(s.get("dec")) * Math.sin(m.get("dec")) + Math.cos(s.get("dec")) * Math.cos(m.get("dec")) * Math.cos(s.get("ra") - m.get("ra"))),
               inc = Math.atan2(sdist * Math.sin(phi), m.get("dist") - sdist * Math.cos(phi)),
               angle = Math.atan2(Math.cos(s.get("dec")) * Math.sin(s.get("ra") - m.get("ra")), Math.sin(s.get("dec")) * Math.cos(m.get("dec")) -
               Math.cos(s.get("dec")) * Math.sin(m.get("dec")) * Math.cos(s.get("ra") - m.get("ra")));

        HashMap<String,Double> map = new HashMap<String, Double>();
        map.put("fraction", (1 + Math.cos(inc)) / 2);
        map.put("phase", 0.5 + 0.5 * inc * (angle < 0 ? -1 : 1) / Math.PI);
        map.put("angle", angle);


        return map;
    }

    public static LocalDateTime hoursLater(LocalDate date, double hours) {
        return LocalDateTime.of(
            date.getYear(), 
            date.getMonth(), 
            date.getDayOfMonth(), 
            (int) hours, 
            0
        );
    }

    public HashMap<String,Object> getMoonTimes(LocalDate date, double lat, double lng, boolean inUTC) {
        LocalDate localDate = date;
        LocalDateTime localDateTime;

        if (inUTC) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            localDate.atTime(0,0,0);
            localDateTime = LocalDateTime.parse(sdf.format(localDate));

        } else {
            localDateTime = LocalDateTime.of(
                localDate.getYear(),
                localDate.getMonthValue(),
                localDate.getDayOfMonth(),
                0,
                0,
                0
            );
        }

        double  hc  = 0.133 * rad,
                h0  = getMoonPosition(localDateTime, lat, lng).get("altitude") - hc,
                h1, h2, rise = Double.NaN, set = Double.NaN, a, b, xe, ye = Double.NaN, d, roots, x1 = 0, x2 = 0, dx;

        for (int i = 0; i <= 24; i++) {
            h1 = getMoonPosition(hoursLater(localDate, i), lat, lng).get("altitude") - hc;
            h2 = getMoonPosition(hoursLater(localDate, i + 1), lat, lng).get("altitude") - hc;

            a = (h0 + h2) / 2 - h1;
            b = (h2 - h0) / 2;
            xe = -b / (2 * a);
            ye = (a * xe + b) * xe + h1;
            d = b * b - 4 * a * h1;
            roots = 0;

            if (d >= 0) {
                dx = Math.sqrt(d) / (Math.abs(a) * 2);
                x1 = xe - dx;
                x2 = xe + dx;
                if (Math.abs(x1) <= 1) roots++;
                if (Math.abs(x2) <= 1) roots++;
                if (x1 < -1) x1 = x2;
            }

            if (roots == 1) {
                if (h0 < 0) rise = i + x1;
                else set = i + x1;

            } else if (roots == 2) {
                rise = i + (ye < 0 ? x2 : x1);
                set = i + (ye < 0 ? x1 : x2);
            }

            if (!Double.isNaN(rise) && !Double.isNaN(set)) break;

            h0 = h2;
        }

        HashMap<String, Object> result = new HashMap<String, Object>();

        if (!Double.isNaN(rise)) 
            result.put("rise", hoursLater(localDateTime.toLocalDate(), rise));

        if (!Double.isNaN(set)) 
            result.put("set", hoursLater(localDateTime.toLocalDate(), set));

        if (Double.isNaN(rise) && Double.isNaN(set))
            result.put(ye > 0 ? "alwaysUp" : "alwaysDown", true);

        return result;
    }
}