package it.spielmann.sunfinder;

public class MoonCalculator {

    private static final double SYNODIC_MONTH_DAYS = 29.530588853;
    private static final double KNOWN_NEW_MOON_JD = 2451550.1; // 2000-01-06 18:14 UTC

    /**
     * Calculates the Moon's position and illumination using low-precision formulas
     * (Meeus, "Astronomical Algorithms", abbreviated series).
     *
     * @param lat    latitude in degrees (north positive)
     * @param lon    longitude in degrees (east positive)
     * @param timeMs UTC time in milliseconds since epoch
     * @return double[] { azimuth, elevation, illuminatedFraction, waxing }
     *         azimuth:            0 = North, 90 = East, 180 = South, 270 = West
     *         elevation:          degrees above horizon (negative = below horizon)
     *         illuminatedFraction: 0 (new moon) .. 1 (full moon)
     *         waxing:              1.0 = waxing (growing), -1.0 = waning (shrinking)
     */
    public static double[] calculate(double lat, double lon, long timeMs) {
        double JD = timeMs / 86400000.0 + 2440587.5;
        double d = JD - 2451545.0; // days since J2000

        double L = AstroMath.normalize360(218.316 + 13.176396 * d); // mean longitude
        double M = 134.963 + 13.064993 * d;                          // mean anomaly
        double F = 93.272 + 13.229350 * d;                           // argument of latitude

        double eclLonRad = Math.toRadians(L + 6.289 * Math.sin(Math.toRadians(M)));
        double eclLatRad = Math.toRadians(5.128 * Math.sin(Math.toRadians(F)));
        double epsRad = Math.toRadians(23.439 - 0.0000004 * d);

        double ra = Math.toDegrees(Math.atan2(
                Math.sin(eclLonRad) * Math.cos(epsRad) - Math.tan(eclLatRad) * Math.sin(epsRad),
                Math.cos(eclLonRad)));
        double dec = Math.toDegrees(Math.asin(
                Math.sin(eclLatRad) * Math.cos(epsRad) + Math.cos(eclLatRad) * Math.sin(epsRad) * Math.sin(eclLonRad)));

        double gmst = AstroMath.normalize360(280.16 + 360.9856235 * d);
        double lst = AstroMath.normalize360(gmst + lon);
        double HA = lst - ra;
        if (HA > 180) HA -= 360;
        if (HA < -180) HA += 360;

        double[] horiz = AstroMath.horizontal(Math.toRadians(lat), Math.toRadians(dec), Math.toRadians(HA));

        double daysSinceNewMoon = (JD - KNOWN_NEW_MOON_JD) % SYNODIC_MONTH_DAYS;
        if (daysSinceNewMoon < 0) daysSinceNewMoon += SYNODIC_MONTH_DAYS;
        double ageFraction = daysSinceNewMoon / SYNODIC_MONTH_DAYS; // 0 = new, 0.5 = full

        double illuminatedFraction = (1 - Math.cos(2 * Math.PI * ageFraction)) / 2;
        double waxing = ageFraction < 0.5 ? 1.0 : -1.0;

        return new double[]{horiz[0], horiz[1], illuminatedFraction, waxing};
    }
}
