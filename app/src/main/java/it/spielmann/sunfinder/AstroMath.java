package it.spielmann.sunfinder;

final class AstroMath {

    private AstroMath() {}

    /**
     * Converts equatorial coordinates (declination + local hour angle) to horizontal
     * coordinates (azimuth + elevation), given the observer's latitude.
     *
     * @return double[] { azimuth, elevation } — azimuth: 0=North, 90=East, 180=South, 270=West
     */
    static double[] horizontal(double latRad, double decRad, double hourAngleRad) {
        double cosZenith = clamp(
                Math.sin(latRad) * Math.sin(decRad) + Math.cos(latRad) * Math.cos(decRad) * Math.cos(hourAngleRad));
        double zenith = Math.toDegrees(Math.acos(cosZenith));
        double elevation = 90 - zenith;

        double eastComponent  = -Math.sin(hourAngleRad) * Math.cos(decRad);
        double northComponent =  Math.cos(latRad) * Math.sin(decRad)
                               - Math.sin(latRad) * Math.cos(decRad) * Math.cos(hourAngleRad);
        double azimuth = Math.toDegrees(Math.atan2(eastComponent, northComponent));
        if (azimuth < 0) azimuth += 360;

        return new double[]{azimuth, elevation};
    }

    static double normalize360(double deg) {
        double v = deg % 360;
        return v < 0 ? v + 360 : v;
    }

    private static double clamp(double v) {
        return Math.max(-1.0, Math.min(1.0, v));
    }
}
