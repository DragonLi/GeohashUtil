package geohashutil.asiainfo.com;

public class FloatNumUtils {

    public static final double EPSILON = 1e-12;

    public static boolean Eq(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }

    public static boolean NotEq(double a, double b) {
        return Math.abs(a - b) > EPSILON;
    }
}
