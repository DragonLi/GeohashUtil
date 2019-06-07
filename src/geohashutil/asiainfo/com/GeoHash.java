package geohashutil.asiainfo.com;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public final class GeoHash implements Comparable<GeoHash>, Serializable {
    private static final int MAX_BIT_PRECISION = 64;
    private static final int MAX_CHARACTER_PRECISION = 12;

    private static final long serialVersionUID = -8553214249630252175L;
    private static final int[] BITS = { 16, 8, 4, 2, 1 };
    private static final int BASE32_BITS = 5;
    public static final long FIRST_BIT_FLAGGED = 0x8000000000000000l;
    private static final char[] base32 = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

    private final static Map<Character, Integer> decodeMap = new HashMap<>();

    static {
        int sz = base32.length;
        for (int i = 0; i < sz; i++) {
            decodeMap.put(base32[i], i);
        }
    }

    public final long bits;
    public final byte significantBits;

//cached computation results
    public final BoundingBox boundingBox;
    public final long latBits;
    public final long lonBits;

    private String base32Encoding;
//end cache

    private GeoHash(double latitude, double longitude, int desiredPrecision) {
        desiredPrecision = Math.min(desiredPrecision, MAX_BIT_PRECISION);
        long bits=0;
        byte significantBits = 0;
        boolean isEvenBit = true;
        double latMin = -90;
        double latMax = 90;
        double lonMin = -180;
        double lonMax = 180;
        double mid;
        long latBits=0,lonBits=0;

        while (significantBits < desiredPrecision) {
            significantBits++;
            bits <<= 1;
            if (isEvenBit) {
                lonBits <<= 1;
                mid = (lonMin+lonMax)/2;
                if (longitude >= mid)
                {
                    lonMin=mid;
                    bits = bits | 0x1;
                    lonBits = lonBits | 0x1;
                }else{
                    lonMax=mid;
                }
            } else {
                latBits <<= 1;
                mid = (latMin+latMax)/2;
                if (latitude >= mid)
                {
                    latMin=mid;
                    bits = bits | 0x1;
                    latBits = latBits | 0x1;
                }else{
                    latMax=mid;
                }
            }
            isEvenBit = !isEvenBit;
        }

        this.bits = bits << (MAX_BIT_PRECISION - desiredPrecision);
        this.significantBits=significantBits;
        int latNum = desiredPrecision / 2;
        this.latBits = latBits << (MAX_BIT_PRECISION - latNum);
        this.lonBits = lonBits << (MAX_BIT_PRECISION - latNum + significantBits % 2);
        boundingBox = new BoundingBox(latMin,latMax,lonMin,lonMax);
    }

    public String toBase32() {
        if (significantBits % 5 != 0) {
            throw new IllegalStateException("Cannot convert a geohash to base32 if the precision is not a multiple of 5.");
        }
        if (base32Encoding != null)
            return base32Encoding;

        StringBuilder buf = new StringBuilder();

        long firstFiveBitsMask = 0xf800000000000000l;
        long bitsCopy = bits;
        int partialChunks = (int) Math.ceil(((double) significantBits / 5));

        for (int i = 0; i < partialChunks; i++) {
            int pointer = (int) ((bitsCopy & firstFiveBitsMask) >>> 59);
            buf.append(base32[pointer]);
            bitsCopy <<= 5;
        }
        base32Encoding = buf.toString();
        return base32Encoding;
    }

    protected int getNumLatBits(){
        return significantBits / 2;
    }

    protected int getNumLonBits(){
        return significantBits / 2+significantBits % 2;
    }

    private long maskLastNBits(long value, long n) {
        long mask = 0xffffffffffffffffl;
        mask >>>= (MAX_BIT_PRECISION - n);
        return value & mask;
    }

    private static void divideRangeDecode(GeoHash hash, double[] range, boolean b) {
        double mid = (range[0] + range[1]) / 2;
        if (b) {
            hash.addOnBitToEnd();
            range[0] = mid;
        } else {
            hash.addOffBitToEnd();
            range[1] = mid;
        }
    }

    protected GeoHash recombineLatLonBitsToHash(long lat, long lon) {
        GeoHash hash = new GeoHash();
        boolean isEvenBit = false;
        latBits[0] <<= (MAX_BIT_PRECISION - latBits[1]);
        lonBits[0] <<= (MAX_BIT_PRECISION - lonBits[1]);
        double[] latitudeRange = { -90.0, 90.0 };
        double[] longitudeRange = { -180.0, 180.0 };

        for (int i = 0; i < latBits[1] + lonBits[1]; i++) {
            if (isEvenBit) {
                divideRangeDecode(hash, latitudeRange, (latBits[0] & FIRST_BIT_FLAGGED) == FIRST_BIT_FLAGGED);
                latBits[0] <<= 1;
            } else {
                divideRangeDecode(hash, longitudeRange, (lonBits[0] & FIRST_BIT_FLAGGED) == FIRST_BIT_FLAGGED);
                lonBits[0] <<= 1;
            }
            isEvenBit = !isEvenBit;
        }
        hash.bits <<= (MAX_BIT_PRECISION - hash.significantBits);
        setBoundingBox(hash, latitudeRange, longitudeRange);
        hash.point = hash.boundingBox.getCenterPoint();
        return hash;
    }

    public GeoHash getNorthernNeighbour() {
        long lat = latBits +1;
        lat = maskLastNBits(lat, getNumLatBits());
        return recombineLatLonBitsToHash(lat, lonBits);
    }

    public GeoHash getSouthernNeighbour() {
        long[] latitudeBits = getRightAlignedLatitudeBits();
        long[] longitudeBits = getRightAlignedLongitudeBits();
        latitudeBits[0] -= 1;
        latitudeBits[0] = maskLastNBits(latitudeBits[0], latitudeBits[1]);
        return recombineLatLonBitsToHash(latitudeBits, longitudeBits);
    }

    public GeoHash getEasternNeighbour() {
        long[] latitudeBits = getRightAlignedLatitudeBits();
        long[] longitudeBits = getRightAlignedLongitudeBits();
        longitudeBits[0] += 1;
        longitudeBits[0] = maskLastNBits(longitudeBits[0], longitudeBits[1]);
        return recombineLatLonBitsToHash(latitudeBits, longitudeBits);
    }

    public GeoHash getWesternNeighbour() {
        long[] latitudeBits = getRightAlignedLatitudeBits();
        long[] longitudeBits = getRightAlignedLongitudeBits();
        longitudeBits[0] -= 1;
        longitudeBits[0] = maskLastNBits(longitudeBits[0], longitudeBits[1]);
        return recombineLatLonBitsToHash(latitudeBits, longitudeBits);
    }

    /**
     * returns the 8 adjacent hashes for this one. They are in the following
     * order:<br>
     * N, NE, E, SE, S, SW, W, NW
     */
    public GeoHash[] getAdjacent() {
        GeoHash northern = getNorthernNeighbour();
        GeoHash eastern = getEasternNeighbour();
        GeoHash southern = getSouthernNeighbour();
        GeoHash western = getWesternNeighbour();
        return new GeoHash[] { northern, northern.getEasternNeighbour(), eastern, southern.getEasternNeighbour(),
                southern,
                southern.getWesternNeighbour(), western, northern.getWesternNeighbour() };
    }

    @Override
    public int hashCode() {
        int f = 31 * 17 + (int) (bits ^ (bits >>> 32));
        f = 31 * f + significantBits;
        return f;
    }

    @Override
    public int compareTo(GeoHash o) {
        int bitsCmp = Long.compare(bits ^ FIRST_BIT_FLAGGED, o.bits ^ FIRST_BIT_FLAGGED);
        if (bitsCmp != 0) {
            return bitsCmp;
        } else {
            return Integer.compare(significantBits, o.significantBits);
        }
    }
}
