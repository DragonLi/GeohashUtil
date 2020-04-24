package geohashutil.asiainfo.com.test;

import geohashutil.asiainfo.com.BoundingBox;
import geohashutil.asiainfo.com.GeoHash;

public class GeoHashSlow {
    private static final double D180 = 180;
    private static final double D90 = 90;
    private static final int MAX_BIT_PRECISION = 64;
    public static final long FIRST_BIT_FLAGGED = 0x8000000000000000L;

    public final long bits;
    public final byte significantBits;

    public final BoundingBox boundingBox;
    public final long latBits;
    public final long lonBits;

    public boolean testEquals(GeoHash other) {
        return other.significantBits == significantBits && other.bits == bits;
    }

    public static GeoHashSlow fromLongValue(final long bits, final byte significantBits) {
        double latMin = -D90, latMax = D90;
        double lonMin = -D180, lonMax = D180;
        double mid;
        boolean isEvenBit = true;
        long latBits = 0, lonBits = 0;
        long bitMask = FIRST_BIT_FLAGGED;

        for (int j = 0; j < significantBits; j++) {
            if (isEvenBit) {
                //lonBits
                lonBits <<= 1;
                mid = (lonMin + lonMax) / 2;
                if ((bits & bitMask) == 0) {
                    lonMax = mid;
                } else {
                    lonMin = mid;
                    lonBits = lonBits | 0x1;
                }
            } else {
                //latBits
                latBits <<= 1;
                mid = (latMin + latMax) / 2;
                if ((bits & bitMask) == 0) {
                    latMax = mid;
                } else {
                    latMin = mid;
                    latBits = latBits | 0x1;
                }
            }
            isEvenBit = !isEvenBit;
            bitMask >>>= 1;
        }

        return new GeoHashSlow(bits, significantBits, latBits, lonBits, latMin, latMax, lonMin, lonMax);
    }

    private GeoHashSlow(final long bits, final byte significantBits, final long lat, final long lon, final double latMin, final double latMax, final double lonMin, final double lonMax) {
        this.bits = bits;
        this.significantBits = significantBits;
        this.latBits = lat;
        this.lonBits = lon;
        this.boundingBox = new BoundingBox(latMin, latMax, lonMin, lonMax);
    }

    public GeoHashSlow(final double latitude, final double longitude, int desiredPrecision) {
        desiredPrecision = Math.min(desiredPrecision, MAX_BIT_PRECISION);
        long bits = 0;
        byte significantBits = 0;
        boolean isEvenBit = true;
        double latMin = -D90, latMax = D90;
        double lonMin = -D180, lonMax = D180;
        double mid;
        long latBits = 0, lonBits = 0;

        while (significantBits < desiredPrecision) {
            significantBits++;
            bits <<= 1;
            if (isEvenBit) {
                lonBits <<= 1;//if I can use ref variables, the logBits/latBits can be abstracted to same function
                mid = (lonMin + lonMax) / 2;
                if (longitude < mid) {
                    lonMax = mid;
                } else {
                    lonMin = mid;
                    bits = bits | 0x1;
                    lonBits = lonBits | 0x1;
                }
            } else {
                latBits <<= 1;
                mid = (latMin + latMax) / 2;
                if (latitude < mid) {
                    latMax = mid;
                } else {
                    latMin = mid;
                    bits = bits | 0x1;
                    latBits = latBits | 0x1;
                }
            }
            isEvenBit = !isEvenBit;
        }

        this.bits = bits << (MAX_BIT_PRECISION - desiredPrecision);
        this.significantBits = significantBits;
        this.latBits = latBits;
        this.lonBits = lonBits;
        boundingBox = new BoundingBox(latMin, latMax, lonMin, lonMax);
    }
}
