package geohashutil.asiainfo.com;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public final class GeoHash implements Comparable<GeoHash>, Serializable {
    private static final int MAX_BIT_PRECISION = 64;
    private static final int MAX_CHARACTER_PRECISION = 12;

    private static final long serialVersionUID = -8553214249630252175L;
    private static final int[] BITS = {16, 8, 4, 2, 1};
    private static final int BASE32_BITS = 5;
    public static final long FIRST_BIT_FLAGGED = 0x8000000000000000l;
    private static final char[] base32 = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

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
    /**
     * 纬度,y
     */
    public final long latBits;
    /**
     * 经度,x
     */
    public final long lonBits;

    private String base32Encoding;
//end cache

    public GeoHash fromPrefix(byte prefixIndex){
        if (prefixIndex <0 || prefixIndex >64)
            return null;
        long tmp = -1L << prefixIndex;
        return GeoHash.fromLongValue(bits & tmp, (byte) (64 - prefixIndex));
    }

    public GridPoint getGridPoint(){
        //the four-branching tree geohash represented is a number based on 4
        //our max bits is 64, so unsigned int is enough to store,max = 2^32-1

        //must scan every two bits,
        int level = (significantBits+1)/2;
        //odd length must pad, and I choose 0 to pad
        long y = (significantBits & 0x01) == 0 ? latBits : latBits<<1;
        return new GridPoint((int) lonBits, (int) y, (byte) level);
    }

    public double distanceByGrid(GridPoint p){
        return p.distanceFrom(getGridPoint());
    }

    public double distanceByGrid(GeoHash geoHash){
        return this.getGridPoint().distanceFrom(geoHash.getGridPoint());
    }

    private GeoHash(final double latitude, final double longitude, int desiredPrecision) {
        desiredPrecision = Math.min(desiredPrecision, MAX_BIT_PRECISION);
        long bits = 0;
        byte significantBits = 0;
        boolean isEvenBit = true;
        double latMin = -90, latMax = 90;
        double lonMin = -180, lonMax = 180;
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

    private GeoHash(final long bits, final byte significantBits, final long lat, final long lon, final double latMin, final double latMax, final double lonMin, final double lonMax) {
        this.bits = bits;
        this.significantBits = significantBits;
        this.latBits = lat;
        this.lonBits = lon;
        this.boundingBox = new BoundingBox(latMin, latMax, lonMin, lonMax);
    }

    public String toBase32() {
        if (base32Encoding != null)
            return base32Encoding;

        if (significantBits % 5 != 0) {
            throw new IllegalStateException("Cannot convert a geohash to base32 if the precision is not a multiple of 5.");
        }

        StringBuilder buf = new StringBuilder();
        final long firstFiveBitsMask = 0xf800000000000000l;
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

    protected GeoHash recombineLatLonBitsToHash(final long latBits, final long lonBits) {
        long bits = 0;
        final byte significantBits = this.significantBits;
        boolean isEvenBit = false;
        double latMin = -90, latMax = 90;
        double lonMin = -180, lonMax = 180;
        double mid;

        long latBitMask = 1 << (significantBits / 2);
        long lonBitMasK = 1 << (significantBits / 2 + significantBits % 2);

        for (int i = 0; i < significantBits; i++) {
            bits <<= 1;
            if (isEvenBit) {
                mid = (latMin + latMax) / 2;
                latBitMask >>= 1;
                if ((latBits & latBitMask) == 0) {
                    latMax = mid;
                } else {
                    bits = bits | 0X1;
                    latMin = mid;
                }
            } else {
                mid = (lonMin + lonMax) / 2;
                lonBitMasK >>= 1;
                if ((lonBits & lonBitMasK) == 0) {
                    lonMax = mid;
                } else {
                    bits = bits | 0X1;
                    lonMin = mid;
                }
            }
            isEvenBit = !isEvenBit;
        }

        bits <<= (MAX_BIT_PRECISION - significantBits);
        return new GeoHash(bits, significantBits, latBits, lonBits, latMin, latMax, lonMin, lonMax);
    }

    public GeoHashNavIterator NavigateSquare(){
        return new GeoHashNavIterator(this);
    }

    public GeoHash navFromSteps(int latitudeStep,int longtitudeStep){
        return recombineLatLonBitsToHash(latBits + latitudeStep, lonBits+longtitudeStep);
    }

    public GeoHash getNorthernNeighbour() {
        return recombineLatLonBitsToHash(latBits + 1, lonBits);
    }

    public GeoHash getSouthernNeighbour() {
        return recombineLatLonBitsToHash(latBits - 1, lonBits);
    }

    public GeoHash getEasternNeighbour() {
        return recombineLatLonBitsToHash(latBits, lonBits + 1);
    }

    public GeoHash getWesternNeighbour() {
        return recombineLatLonBitsToHash(latBits, lonBits - 1);
    }

    public GeoHash getNorthernEastNeighbour() {
        return recombineLatLonBitsToHash(latBits + 1, lonBits + 1);
    }

    public GeoHash getNorthernWestNeighbour() {
        return recombineLatLonBitsToHash(latBits + 1, lonBits - 1);
    }

    public GeoHash getSouthernEastNeighbour() {
        return recombineLatLonBitsToHash(latBits - 1, lonBits + 1);
    }

    public GeoHash getSouthernWestNeighbour() {
        return recombineLatLonBitsToHash(latBits - 1, lonBits - 1);
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
        return new GeoHash[]{northern, getNorthernEastNeighbour(), eastern, getSouthernEastNeighbour(),
                southern,
                getSouthernWestNeighbour(), western, getNorthernWestNeighbour()};
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof GeoHash) {
            GeoHash other = (GeoHash) obj;
            if (other.significantBits == significantBits && other.bits == bits) {
                return true;
            }
        }
        return false;
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

    public static GeoHash withBitPrecision(final double latitude, final double longitude, int numberOfBits) {
        if (numberOfBits > MAX_BIT_PRECISION) {
            throw new IllegalArgumentException("A Geohash can only be " + MAX_BIT_PRECISION + " bits long!");
        }
        if (Math.abs(latitude) > 90.0 || Math.abs(longitude) > 180.0) {
            throw new IllegalArgumentException("Can't have lat/lon values out of (-90,90)/(-180/180)");
        }
        return new GeoHash(latitude, longitude, numberOfBits);
    }

    /**
     * This method uses the given number of characters as the desired precision
     * value. The hash can only be 64bits long, thus a maximum precision of 12
     * characters can be achieved.
     */
    public static GeoHash withCharacterPrecision(final double latitude, final double longitude, final int numberOfCharacters) {
        if (numberOfCharacters > MAX_CHARACTER_PRECISION) {
            throw new IllegalArgumentException("A geohash can only be " + MAX_CHARACTER_PRECISION + " character long.");
        }
        int desiredPrecision = (numberOfCharacters * 5 <= 60) ? numberOfCharacters * 5 : 60;
        return new GeoHash(latitude, longitude, desiredPrecision);
    }

    public static GeoHash fromLongValue(final long bits, final byte significantBits) {
        double latMin = -90, latMax = 90;
        double lonMin = -180, lonMax = 180;
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

        return new GeoHash(bits, significantBits, latBits, lonBits, latMin, latMax, lonMin, lonMax);
    }

    /**
     * build a new {@link GeoHash} from a base32-encoded {@link String}.<br>
     * This will also set up the hashes bounding box and other values, so it can
     * also be used with functions like within().
     */
    public static GeoHash fromGeohashString(String geohash) {
        double latMin = -90, latMax = 90;
        double lonMin = -180, lonMax = 180;
        double mid;
        boolean isEvenBit = true;
        long latBits = 0, lonBits = 0;
        long bits = 0;
        byte significantBits = 0;

        for (int i = 0; i < geohash.length(); i++) {
            int cd = decodeMap.get(geohash.charAt(i));
            for (int j = 0; j < BASE32_BITS; j++) {
                int mask = BITS[j];
                significantBits++;
                bits <<= 1;
                if (isEvenBit) {
                    //lonBits
                    lonBits <<= 1;
                    mid = (lonMin + lonMax) / 2;
                    if ((cd & mask) == 0) {
                        lonMax = mid;
                    } else {
                        lonMin = mid;
                        bits = bits | 0x1;
                        lonBits = lonBits | 0x1;
                    }
                } else {
                    //latBits
                    latBits <<= 1;
                    mid = (latMin + latMax) / 2;
                    if ((cd & mask) == 0) {
                        latMax = mid;
                    } else {
                        latMin = mid;
                        bits = bits | 0x1;
                        latBits = latBits | 0x1;
                    }
                }
                isEvenBit = !isEvenBit;
            }
        }

        return new GeoHash(bits << (MAX_BIT_PRECISION - significantBits), significantBits, latBits, lonBits, latMin, latMax, lonMin, lonMax);
    }

    public static final int commonPrefixLength(long a, long b) {
        int result = 0;
        while (result < 64 && (a & FIRST_BIT_FLAGGED) == (b & FIRST_BIT_FLAGGED)) {
            result++;
            a <<= 1;
            b <<= 1;
        }
        return result;
    }

    @Override
    public String toString() {
        if (significantBits % 5 == 0) {
            return String.format("%s -> %s -> %s", Long.toBinaryString(bits), boundingBox, toBase32());
        } else {
            return String.format("%s -> %s, bits: %d", Long.toBinaryString(bits), boundingBox, significantBits);
        }
    }
    //functions that have no use case for now

    /**
     * return a long mask for this hashes significant bits.
     */
    private long mask() {
        if (significantBits == 0) {
            return 0;
        } else {
            long value = FIRST_BIT_FLAGGED;
            value >>= (significantBits - 1);
            return value;
        }
    }

    /**
     * returns true iff this is within the given geohash bounding box.
     */
    public boolean within(GeoHash boundingBox) {
        return (bits & boundingBox.mask()) == boundingBox.bits;
    }

    /**
     * This method uses the given number of characters as the desired precision
     * value. The hash can only be 64bits long, thus a maximum precision of 12
     * characters can be achieved.
     */
    public static String geoHashStringWithCharacterPrecision(double latitude, double longitude, int numberOfCharacters) {
        GeoHash hash = withCharacterPrecision(latitude, longitude, numberOfCharacters);
        return hash.toBase32();
    }

    public int getNumLatBits() {
        return significantBits/2;
    }

    public int getNumLonBits() {
        return significantBits/2+significantBits%2;
    }

    public GeoHash next(int step) {
        return fromOrd(ord() + step, significantBits);
    }

    public GeoHash next() {
        return next(1);
    }

    public GeoHash prev() {
        return next(-1);
    }

    public long ord() {
        int insignificantBits = MAX_BIT_PRECISION - significantBits;
        return bits >>> insignificantBits;
    }

    public static GeoHash fromOrd(long ord, byte significantBits) {
        int insignificantBits = MAX_BIT_PRECISION - significantBits;
        return fromLongValue(ord << insignificantBits, significantBits);
    }

    /**
     * Returns the number of characters that represent this hash.
     *
     * @throws IllegalStateException
     *             when the hash cannot be encoded in base32, i.e. when the
     *             precision is not a multiple of 5.
     */
    public int getCharacterPrecision() {
        if (significantBits % 5 != 0) {
            throw new IllegalStateException(
                    "precision of GeoHash is not divisble by 5: " + this);
        }
        return significantBits / 5;
    }

    /**
     * Counts the number of geohashes contained between the two (ie how many
     * times next() is called to increment from one to two) This value depends
     * on the number of significant bits.
     *
     * @param one
     * @param two
     * @return number of steps
     */
    public static long stepsBetween(GeoHash one, GeoHash two) {
        if (one.significantBits != two.significantBits) {
            throw new IllegalArgumentException(
                    "It is only valid to compare the number of steps between two hashes if they have the same number of significant bits");
        }
        return two.ord() - one.ord();
    }
}
