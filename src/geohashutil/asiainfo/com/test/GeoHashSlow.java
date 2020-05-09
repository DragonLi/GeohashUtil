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

    private static final double[] deltaCached;// ==360/2^(index+1)
    static {
        deltaCached = new double[64];
        double tmp = D360;
        for (int i = 0; i < 64; i++) {
            deltaCached[i] = tmp = tmp/2.0d;
        }
    }
    public boolean testEquals(GeoHash other) {
        return other.significantBits == significantBits && other.bits == bits;
    }

    private static long interleavingInsertZero(long bits) {
        bits |= bits << 16; bits &= 0x0000ffff0000ffffL;
        bits |= bits << 8;  bits &= 0x00ff00ff00ff00ffL;
        bits |= bits << 4;  bits &= 0x0f0f0f0f0f0f0f0fL;
        bits |= bits << 2;  bits &= 0x3333333333333333L;
        bits |= bits << 1;  bits &= 0x5555555555555555L;
        return bits;
        /*long lowestOneBit = bits & -bits;
        long result = 0;
        while (lowestOneBit != 0) {
            bits ^= lowestOneBit;//remove lowestOneBit
            result |= lowestOneBit * lowestOneBit;//left shift by the number of trailing zero
            lowestOneBit = bits & -bits;
        }
        return result;*/
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

    @Override
    public String toString() {
        return String.format("%s -> %s, bits: %d", Long.toBinaryString(bits), boundingBox, significantBits);
    }

    /**
     * this version is adapted from
     * https://mmcloughlin.com/posts/geohash-assembly
     * key idea: when y in (0,1], y+1 in (1,2], y+1 = (-1)^s*2^(e-1023)*(1.0+B / 2^52)
     * where s--e--B is the binary representation of float value (y+1) according IEEE standard
     * so: Floor[(lat+90)/180 * 2^52] >>> 20 = B == Floor[(lat+90)/180 * 2^32]!
     * where y = lat/180+0.5
     */
    public static GeoHashSlow v4(final double latitude, final double longitude, final int numberOfBits) {
        CheckLatLng(latitude, longitude, numberOfBits);
        final int lenY=numberOfBits>>>1;//numberOfBits/2
        final int lenX=numberOfBits-lenY;
        final double lngDelta = lenX==0? D360 : deltaCached[lenX-1];// == 360/ Math.pow(2, lenX);
        final double latDelta = lenX == lenY ? lngDelta /2 : lngDelta;//fastDoubleDecPow2(lenY, D180);// == 180/ Math.pow(2, lenY);
        double x = latitude / D180 + 1.5;
        double y = longitude/D360 + 1.5;
        final long lowMask = 0xFFFFFFFFFFFFFL;
        final long latBits = (Double.doubleToLongBits(x) & lowMask) >>> (52-lenY);
        final long lngBits = (Double.doubleToLongBits(y) & lowMask) >>> (52-lenX);
        final double minLat = Double.longBitsToDouble(Double.doubleToRawLongBits((double) latBits)+((long) lenY <<52)) - D90;
        final double maxLat = minLat + latDelta;
        final double minLng = Double.longBitsToDouble(Double.doubleToRawLongBits((double) lngBits)+((long) lenX <<52)) - D180;
        final double maxLng = minLng + lngDelta;

        long lngInterleavingBits = interleavingInsertZero(lngBits);
        long latInterleavingBits = interleavingInsertZero(lenX == lenY ? latBits : latBits<<1);
        long bits = (lngInterleavingBits<<1) ^ latInterleavingBits;
        bits = bits << (MAX_BIT_PRECISION - (lenX<<1));
        return new GeoHashSlow(bits, (byte) numberOfBits, latBits, lngBits, minLat, maxLat, minLng, maxLng);
    }

    public static GeoHashSlow v3(final double latitude, final double longitude, final int numberOfBits) {
        CheckLatLng(latitude, longitude, numberOfBits);
        final int lenY=numberOfBits>>>1;//numberOfBits/2
        final int lenX=numberOfBits-lenY;
        final double lngDelta = lenX==0? D360 : deltaCached[lenX-1];// == 360/ Math.pow(2, lenX);
        final double latDelta = lenX == lenY ? lngDelta /2 : lngDelta;//fastDoubleDecPow2(lenY, D180);// == 180/ Math.pow(2, lenY);
        final long latBits = (long) ((latitude + D90) * (1L << lenY) / D180);
        final long lngBits = (long) ((longitude + D180) * (1L << lenX) / D360);
        final double minLat = Double.longBitsToDouble(Double.doubleToRawLongBits((double) latBits)+((long) lenY <<52)) - D90;//TODO inline fastDoublePow2
        final double maxLat = minLat + latDelta;
        final double minLng = Double.longBitsToDouble(Double.doubleToRawLongBits((double) lngBits)+((long) lenX <<52)) - D180;
        final double maxLng = minLng + lngDelta;

        long lngInterleavingBits = interleavingInsertZero(lngBits);
        long latInterleavingBits = interleavingInsertZero(lenX == lenY ? latBits : latBits<<1);
        long bits = (lngInterleavingBits<<1) ^ latInterleavingBits;
        bits = bits << (MAX_BIT_PRECISION - (lenX<<1));
        return new GeoHashSlow(bits, (byte) numberOfBits, latBits, lngBits, minLat, maxLat, minLng, maxLng);
    }

    public static GeoHashSlow v2(final double latitude, final double longitude, final int numberOfBits) {
        CheckLatLng(latitude, longitude, numberOfBits);
        final int lenY=numberOfBits>>>1;//numberOfBits/2
        final int lenX=numberOfBits-lenY;
        final double lngDelta = lenX==0? D360 : deltaCached[lenX-1];// == 360/ Math.pow(2, lenX);
        final double latDelta = lenX == lenY ? lngDelta /2 : lngDelta;//fastDoubleDecPow2(lenY, D180);// == 180/ Math.pow(2, lenY);
        final long latBits = (long) ((latitude+ D90)/latDelta);
        final long lngBits = (long) ((longitude+ D180)/lngDelta);
        final double minLat = Double.longBitsToDouble(Double.doubleToRawLongBits((double) latBits)+((long) lenY <<52)) - D90;//TODO inline fastDoublePow2
        final double maxLat = minLat + latDelta;
        final double minLng = Double.longBitsToDouble(Double.doubleToRawLongBits((double) lngBits)+((long) lenX <<52)) - D180;
        final double maxLng = minLng + lngDelta;

        long lngInterleavingBits = interleavingInsertZero(lngBits);
        long latInterleavingBits = interleavingInsertZero(lenX == lenY ? latBits : latBits<<1);
        long bits = (lngInterleavingBits<<1) ^ latInterleavingBits;
        bits = bits << (MAX_BIT_PRECISION - (lenX<<1));
        return new GeoHashSlow(bits, (byte) numberOfBits, latBits, lngBits, minLat, maxLat, minLng, maxLng);
    }

    public static GeoHashSlow v1(final double latitude, final double longitude, final int numberOfBits){
        if (numberOfBits > MAX_BIT_PRECISION) {
            throw new IllegalArgumentException("A Geohash can only be " + MAX_BIT_PRECISION + " bits long!");
        }
        if (Math.abs(latitude) > 90.0 || Math.abs(longitude) > 180.0) {
            throw new IllegalArgumentException("Can't have lat/lon values out of (-90,90)/(-180/180)");
        }
        return new GeoHashSlow(latitude, longitude, numberOfBits);
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
