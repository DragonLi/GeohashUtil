package geohashutil.asiainfo.com;

import java.io.Serializable;

public class BoundingBox implements Serializable {
    /**
     * 纬度,y
     */
    public final double minLat;
    /**
     * 纬度,y
     */
    public final double maxLat;
    /**
     * 经度,x
     */
    public final double minLon;
    /**
     * 经度,x
     */
    public final double maxLon;

    /**
     * create a bounding box defined by two coordinates
     */
    public BoundingBox(WGS84Point p1, WGS84Point p2) {
        this(p1.latitude, p2.latitude, p1.longitude, p2.longitude);
    }

    public BoundingBox(double y1, double y2, double x1, double x2) {
        minLon = Math.min(x1, x2);
        maxLon = Math.max(x1, x2);
        minLat = Math.min(y1, y2);
        maxLat = Math.max(y1, y2);
    }

    public BoundingBox(BoundingBox that) {
        this(that.minLat, that.maxLat, that.minLon, that.maxLon);
    }

    //TODO might need cached
    public WGS84Point getUpperRight() {
        return WGS84Point.Create(maxLat, maxLon);
    }
    public GeoHash getUpperRightHash(int level){return GeoHash.withBitPrecision(maxLat,maxLon,level);}

    public WGS84Point getLowerLeft() {
        return WGS84Point.Create(minLat, minLon);
    }
    public GeoHash getLowerLeftHash(int level){return GeoHash.withBitPrecision(minLat,minLon,level);}

    public WGS84Point getUpperLeft() {
        return WGS84Point.Create(maxLat, minLon);
    }
    public GeoHash getUpperLeftHash(int level){return GeoHash.withBitPrecision(maxLat,minLon,level);}

    public WGS84Point getLowerRight() {
        return WGS84Point.Create(minLat, maxLon);
    }
    public GeoHash getLowerRightHash(int level) {return GeoHash.withBitPrecision(minLat, maxLon,level);}

    public WGS84Point getCenterPoint() {
        return WGS84Point.Create((minLat + maxLat) / 2, (minLon + maxLon) / 2);
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

    /**
     * 纬度,y
     * @return 高度
     */
    public double getLatitudeSize() {
        return maxLat - minLat;
    }

    /**
     * 经度,x
     * @return 宽度
     */
    public double getLongitudeSize() {
        return maxLon - minLon;
    }
//TODO end might need cached

    public boolean contains(WGS84Point point) {
        return (point.latitude >= minLat) && (point.longitude >= minLon) && (point.latitude <= maxLat)
                && (point.longitude <= maxLon);
    }

    public boolean intersects(MutableBoundingBox other) {
        return !(other.getMinLon() > maxLon || other.getMaxLon() < minLon || other.getMinLat() > maxLat || other.getMaxLat() < minLat);
    }

    public boolean intersects(BoundingBox other) {
        return !(other.minLon > maxLon || other.maxLon < minLon || other.minLat > maxLat || other.maxLat < minLat);
    }

    public BoundingBox expandToInclude(BoundingBox other) {
        double minLon = Math.min(other.minLon, this.minLon);
        double maxLon = Math.max(other.maxLon, this.maxLon);
        double minLat = Math.min(other.minLat, this.minLat);
        double maxLat = Math.max(other.maxLat, this.maxLat);
        return new BoundingBox(minLat, maxLat, minLon, maxLon);
    }

    public BoundingBox expandToInclude(MutableBoundingBox other) {
        double minLon = Math.min(other.getMinLon(), this.minLon);
        double maxLon = Math.max(other.getMaxLon(), this.maxLon);
        double minLat = Math.min(other.getMinLat(), this.minLat);
        double maxLat = Math.max(other.getMaxLat(), this.maxLat);
        return new BoundingBox(minLat, maxLat, minLon, maxLon);
    }

    public BoundingBoxNavIterator navigate() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BoundingBox) {
            BoundingBox that = (BoundingBox) obj;
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
        return getLowerLeft() + " -> " + getUpperRight();
    }
}
