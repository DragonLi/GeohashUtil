package geohashutil.asiainfo.com;

public class HashUtil {
    public static int hashCode(double x) {
        long f = Double.doubleToLongBits(x);
        return (int) (f ^ (f >>> 32));
    }
}
