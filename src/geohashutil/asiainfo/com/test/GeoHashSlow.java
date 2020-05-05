package geohashutil.asiainfo.com.test;

import geohashutil.asiainfo.com.BoundingBox;
import geohashutil.asiainfo.com.GeoHash;

public class GeoHashSlow {
    private static final double D360 = 360;
    private static final long DOUBLE_360 = Double.doubleToRawLongBits(GeoHashSlow.D360);
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

    private static long interleavingInsertZero(long bits) {
        long lowestOneBit = bits & -bits;
        long result = 0;
        while (lowestOneBit != 0) {
            bits ^= lowestOneBit;//remove lowestOneBit
            result |= lowestOneBit * lowestOneBit;//left shift by the number of trailing zero
            lowestOneBit = bits & -bits;
        }
        return result;
    }

    private static void CheckLatLng(double latitude, double longitude, int numberOfBits) {
        if (numberOfBits > MAX_BIT_PRECISION || numberOfBits <0) {
            throw new IllegalArgumentException("A Geohash can only be " + MAX_BIT_PRECISION + " bits long!");
        }
        if (Math.abs(latitude) > 90.0 || Math.abs(longitude) > 180.0) {
            throw new IllegalArgumentException("Can't have lat/lon values out of (-90,90)/(-180/180)");
        }
    }

    /**
     * fast pow2 using mantissa format of doulbe
     * @param exp
     * @param tmp
     * @return tmp * Math.pow(2,exp)
     */
    private static double fastDoublePow2(long exp, double tmp) {
        long raw = Double.doubleToRawLongBits(tmp);
        raw += exp <<52;
        return Double.longBitsToDouble(raw);
    }

    private static double fastDoubleDecPow2(long exp) {
        long latRaw = DOUBLE_360 - (exp <<52);
        return Double.longBitsToDouble(latRaw);
    }

    public static GeoHashSlow withBitPrecision(final double latitude, final double longitude, final int numberOfBits) {
        CheckLatLng(latitude, longitude, numberOfBits);
        final int lenY=numberOfBits>>>1;//numberOfBits/2
        final int lenX=numberOfBits-lenY;
        final double lngDelta = fastDoubleDecPow2(lenX);// == 360/ Math.pow(2, lenX);
        final double latDelta = lenX == lenY ? lngDelta /2 : lngDelta;//fastDoubleDecPow2(lenY, D180);// == 180/ Math.pow(2, lenY);
        final long latBits = (long) Math.floor((latitude+ D90)/latDelta);
        final long lngBits = (long) Math.floor((longitude+ D180)/lngDelta);
        final double minLat = latBits * latDelta - D90;
        final double maxLat = minLat + latDelta;
        final double minLng = lngBits * lngDelta - D180;
        final double maxLng = minLng + lngDelta;

        long lngInterleavingBits = interleavingInsertZero(lngBits);
        long latInterleavingBits = interleavingInsertZero(lenX == lenY ? latBits : latBits<<1);
        long bits = (lngInterleavingBits<<1) ^ latInterleavingBits;
        bits = bits << (MAX_BIT_PRECISION - (lenX<<1));
        return new GeoHashSlow(bits, (byte) numberOfBits, latBits, lngBits, minLat, maxLat, minLng, maxLng);
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
