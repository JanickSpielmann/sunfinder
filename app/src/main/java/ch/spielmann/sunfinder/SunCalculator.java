package ch.spielmann.sunfinder;

public class SunCalculator {

    /**
     * Calculates the sun's position using the NOAA solar algorithm.
     *
     * @param lat    latitude in degrees (north positive)
     * @param lon    longitude in degrees (east positive)
     * @param timeMs UTC time in milliseconds since epoch
     * @return double[] { azimuth, elevation }
     *         azimuth:   0 = North, 90 = East, 180 = South, 270 = West
     *         elevation: degrees above horizon (negative = below horizon)
     */
    public static double[] calculate(double lat, double lon, long timeMs) {
        double JD = timeMs / 86400000.0 + 2440587.5;
        double T = (JD - 2451545.0) / 36525.0;

        double L0 = (280.46646 + T * (36000.76983 + T * 0.0003032)) % 360;
        if (L0 < 0) L0 += 360;

        double M = 357.52911 + T * (35999.05029 - 0.0001537 * T);
        double Mrad = Math.toRadians(M);

        double C = Math.sin(Mrad) * (1.914602 - T * (0.004817 + 0.000014 * T))
                + Math.sin(2 * Mrad) * (0.019993 - 0.000101 * T)
                + Math.sin(3 * Mrad) * 0.000289;

        double sunLon = L0 + C;
        double omega = 125.04 - 1934.136 * T;
        double lambda = sunLon - 0.00569 - 0.00478 * Math.sin(Math.toRadians(omega));

        double eps0 = 23 + (26 + (21.448 - T * (46.815 + T * (0.00059 - T * 0.001813))) / 60) / 60;
        double eps = eps0 + 0.00256 * Math.cos(Math.toRadians(omega));

        double lambdaRad = Math.toRadians(lambda);
        double epsRad = Math.toRadians(eps);
        double dec = Math.toDegrees(Math.asin(Math.sin(epsRad) * Math.sin(lambdaRad)));

        double y = Math.pow(Math.tan(epsRad / 2), 2);
        double L0rad = Math.toRadians(L0);
        double eot = 4 * Math.toDegrees(
                y * Math.sin(2 * L0rad)
                - 2 * 0.016708634 * Math.sin(Mrad)
                + 4 * 0.016708634 * y * Math.sin(Mrad) * Math.cos(2 * L0rad)
                - 0.5 * y * y * Math.sin(4 * L0rad)
                - 1.25 * 0.016708634 * 0.016708634 * Math.sin(2 * Mrad));

        double utcMinutes = (timeMs % 86400000L) / 60000.0;
        double trueSolarTime = (utcMinutes + eot + 4 * lon + 1440) % 1440;
        double HA = (trueSolarTime / 4) - 180;

        double latRad = Math.toRadians(lat);
        double decRad = Math.toRadians(dec);
        double HArad = Math.toRadians(HA);

        double cosZenith = clamp(
                Math.sin(latRad) * Math.sin(decRad) + Math.cos(latRad) * Math.cos(decRad) * Math.cos(HArad));
        double zenith = Math.toDegrees(Math.acos(cosZenith));
        double elevation = 90 - zenith;

        // atan2-based azimuth: no acos quadrant ambiguity, no HA-flip needed
        double eastComponent  = -Math.sin(HArad) * Math.cos(decRad);
        double northComponent =  Math.cos(latRad) * Math.sin(decRad)
                               - Math.sin(latRad) * Math.cos(decRad) * Math.cos(HArad);
        double azimuth = Math.toDegrees(Math.atan2(eastComponent, northComponent));
        if (azimuth < 0) azimuth += 360;

        return new double[]{azimuth, elevation};
    }

    private static double clamp(double v) {
        return Math.max(-1.0, Math.min(1.0, v));
    }
}
