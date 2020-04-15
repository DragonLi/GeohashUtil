package geohashutil.asiainfo.com;

import java.io.Serializable;

//reference: geohash-java project
public final class WGS84Point implements Serializable {
    private static final long serialVersionUID = 7457963026513014856L;
    /**
     * 经度,x
     */
    public final double longitude;
    /**
     * 纬度,y
     */
    public final double latitude;

    public WGS84Point(final double latitude, final double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
            throw new IllegalArgumentException("The supplied coordinates " + this + " are out of range.");
        }
    }

    @Override
    public int hashCode() {
        //TODO maybe cached
        return 31 * (31 * 42 + HashUtil.hashCode(latitude)) + HashUtil.hashCode(longitude);
    }

    public static WGS84Point Create(double latitude, double longitude) {
        return new WGS84Point(latitude, longitude);
    }

    public static WGS84Point Create(WGS84Point other) {
        return new WGS84Point(other.latitude, other.longitude);
    }

    @Override
    public String toString() {
        return String.format("(" + latitude + "," + longitude + ")");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WGS84Point) {
            WGS84Point other = (WGS84Point) obj;
            return FloatNumUtils.Eq(latitude, other.latitude)
                    && FloatNumUtils.Eq(longitude, other.longitude);
        }
        return false;
    }
}
