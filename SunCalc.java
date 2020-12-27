import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.JulianFields;
import java.util.ArrayList;
import java.util.Calendar;
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
        J1970 = 2440588,
        J2000 = 2451545,
        HALFSECOND = 0.5;
    
    int JGREG= 15 + 31*(10+12*1582);


    public double toJulian(LocalDateTime date) {
        ZonedDateTime zdt = date.atZone(ZoneId.of("Europe/Vienna"));
        double result = zdt.toInstant().toEpochMilli(); //milliseconds since epoch
        result = result / dayMs - 0.5 + J1970;

        return result;
    }

    public String fromJulian(double jdn) {

        //wrong calculations

        System.out.println("fromJulian.jdn: " + jdn);

        int jalpha,ja,jb,jc,jd,je,year,month,day;
        double  julian = jdn,
                decimal= jdn % 1;
        ja = (int) julian;
        if (ja>= JGREG) {
            jalpha = (int) (((ja - 1867216) - 0.25) / 36524.25);
            ja = ja + 1 + jalpha - jalpha / 4;
        }

        jb = ja + 1524;
        jc = (int) (6680.0 + ((jb - 2439870) - 122.1) / 365.25);
        jd = 365 * jc + jc / 4;
        je = (int) ((jb - jd) / 30.6001);
        day = jb - jd - (int) (30.6001 * je);
        month = je - 1;
        if (month > 12) month = month - 12;
        year = jc - 4715;
        if (month > 2) year--;
        if (year <= 0) year--;

        double dhour = decimal * 24;
        int hour = (int) Math.round(dhour);

        decimal = dhour % 1;

        double dminute = decimal * 60;
        int minute = (int) Math.round(dminute);

        decimal = dminute % 1;

        double dsecond = decimal * 60;
        int second = (int) Math.round(dsecond);

        LocalDateTime ldt = LocalDateTime.of(
            year,
            month,
            day,
            hour,
            minute,
            second
        );
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        ZonedDateTime zdt = ZonedDateTime.of(ldt, ZoneId.of("UTC"));

        return dtf.format(zdt);
    }

    public double toDays(LocalDateTime date) {
        return toJulian(date) - J2000;
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

    public HashMap<String,Double> getSunPosition(LocalDateTime date, double lat, double lng) {
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

    public ArrayList<Object[]> getAllSunTimes(){
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

    private double approxTransitJ(double Ht, double lw, double n) {
        return J0 + (Ht + lw) / (2 * PI) + n;
    }

    private double solarTransit(double ds, double M, double L) {
        return J2000 + ds + 0.0053 * Math.sin(M) - 0.0069 * Math.sin(2 * L);
    }

    private double hourAngle(double h, double phi, double d) {
        return Math.acos((Math.sin(h) - Math.sin(phi) * Math.sin(d)) / (Math.cos(phi) * Math.cos(d)));
    }

    private double observerAngle(double height) {
        return -2.076 * Math.sqrt(height) / 60;
    }

    public double getSetJ(double h, double lw, double phi, double dec, double n, double M, double L) {
        double w = hourAngle(h, phi, dec);
        double a = approxTransitJ(w, lw, n);

        return solarTransit(a, M, L);
    }

    public HashMap<String, Object> getSunTimes(LocalDateTime date, double lat, double lng, double height) {
        double  lw  = rad * (lng * -1),
                phi = rad * lat,
                
                dh  = observerAngle(height);

        double  d   = toDays(date);
        double  n   = julianCycle(d, lw),
                ds  = approxTransitJ(0, lw, n),

                M   = solarMeanAnomaly(ds),
                L   = eclipticLongitude(M),
                dec = declination(L, 0),

                Jnoon   = solarTransit(ds, M, L),

                h0, Jset, Jrise;

        System.out.println("getSunTimes.Jnoon: " + Jnoon);

        Object[] time;

        HashMap<String, Object> results = new HashMap<String, Object>();
        results.put("solarNoon", fromJulian(Jnoon));
        results.put("nadir", fromJulian(Jnoon - 0.5));

        for (int i = 0, len = countSunTimes(); i < len; i++) {
            time = getAllSunTimes().get(i);
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
                d   = toDays(LocalDateTime.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), date.getHour(), date.getMinute(), date.getSecond()));

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

    public Map<String, Double> getMoonIllumination(LocalDateTime date) {
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