package geohashutil.asiainfo.com;

import java.io.Serializable;

public class MutableBoundingBox implements Serializable {
    private static final long serialVersionUID = -7145192134410261076L;
    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;

    /**
     * create a bounding box defined by two coordinates
     */
    public MutableBoundingBox(WGS84Point p1, WGS84Point p2) {
        this(p1.latitude, p2.latitude, p1.longitude, p2.longitude);
    }

    public MutableBoundingBox(double y1, double y2, double x1, double x2) {
        minLon = Math.min(x1, x2);
        maxLon = Math.max(x1, x2);
        minLat = Math.min(y1, y2);
        maxLat = Math.max(y1, y2);
    }

    public MutableBoundingBox(MutableBoundingBox that) {
        this(that.minLat, that.maxLat, that.minLon, that.maxLon);
    }

    public WGS84Point getUpperLeft() {
        return WGS84Point.Create(maxLat, minLon);
    }

    public WGS84Point getLowerRight() {
        return WGS84Point.Create(minLat, maxLon);
    }

    public double getLatitudeSize() {
        return maxLat - minLat;
    }

    public double getLongitudeSize() {
        return maxLon - minLon;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + HashUtil.hashCode(minLat);
        result = 37 * result + HashUtil.hashCode(maxLat);
        result = 37 * result + HashUtil.hashCode(minLon);
        result = 37 * result + HashUtil.hashCode(maxLon);
        return result;
    }

    public boolean contains(WGS84Point point) {
        return (point.latitude >= minLat) && (point.longitude >= minLon) && (point.latitude <= maxLat)
                && (point.longitude <= maxLon);
    }

    public boolean intersects(BoundingBox other) {
        return !(other.minLon > maxLon || other.maxLon < minLon || other.minLat > maxLat || other.maxLat < minLat);
    }

    public boolean intersects(MutableBoundingBox other) {
        return !(other.minLon > maxLon || other.maxLon < minLon || other.minLat > maxLat || other.maxLat < minLat);
    }

    public WGS84Point getCenterPoint() {
        return WGS84Point.Create((minLat + maxLat) / 2, (minLon + maxLon) / 2);
    }

    public void expandToInclude(BoundingBox other) {
        if (other.minLon < minLon) {
            minLon = other.minLon;
        }
        if (other.maxLon > maxLon) {
            maxLon = other.maxLon;
        }
        if (other.minLat < minLat) {
            minLat = other.minLat;
        }
        if (other.maxLat > maxLat) {
            maxLat = other.maxLat;
        }
    }

    public void expandToInclude(MutableBoundingBox other) {
        if (other.minLon < minLon) {
            minLon = other.minLon;
        }
        if (other.maxLon > maxLon) {
            maxLon = other.maxLon;
        }
        if (other.minLat < minLat) {
            minLat = other.minLat;
        }
        if (other.maxLat > maxLat) {
            maxLat = other.maxLat;
        }
    }

    public double getMinLon() {
        return minLon;
    }

    public double getMinLat() {
        return minLat;
    }

    public double getMaxLat() {
        return maxLat;
    }

    public double getMaxLon() {
        return maxLon;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MutableBoundingBox) {
            MutableBoundingBox that = (MutableBoundingBox) obj;
            return FloatNumUtils.Eq(minLat, that.minLat)
                    && FloatNumUtils.Eq(minLon, that.minLon)
                    && FloatNumUtils.Eq(maxLat, that.maxLat)
                    && FloatNumUtils.Eq(maxLon, that.maxLon);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return getUpperLeft() + " -> " + getLowerRight();
    }
}
