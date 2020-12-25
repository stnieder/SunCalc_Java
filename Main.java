import java.time.LocalDate;

import SunCalc;

public class Main {

    public static void main(String[] args) {
        SunCalc sunCalc = new SunCalc();        
        System.out.println(
            sunCalc.getSunTimes(LocalDate.now(), 51.5, -0.1, 1.5)
        );
    }
    
}
