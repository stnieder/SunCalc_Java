package suncalc;

import java.time.LocalDateTime;

public class Main {

    static LocalDateTime dateTime = LocalDateTime.of(2020, 12, 26, 0, 0, 0);    
    static double jdn = 2451604.26753472;

    public static void main(String[] args) {
        SunCalc sunCalc = new SunCalc();
        //System.out.println(sunCalc.toDays(dateTime));
        System.out.println(sunCalc.getSunTimes(dateTime, 51.5, -0.1, 1.5));
    }
    
}
