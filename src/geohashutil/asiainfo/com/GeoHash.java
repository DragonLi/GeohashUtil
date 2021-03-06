package geohashutil.asiainfo.com;

import java.util.HashMap;
import java.util.Map;

public final class GeoHash implements Comparable<GeoHash>{
    private static final int MAX_BIT_PRECISION = 64;
    private static final int MAX_CHARACTER_PRECISION = 12;

    private static final int[] BITS = {16, 8, 4, 2, 1};
    private static final int BASE32_BITS = 5;
    public static final long FIRST_BIT_FLAGGED = 0x8000000000000000L;
    private static final char[] base32 = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

    private final static Map<Character, Integer> decodeMap = new HashMap<>();
    private static final double D180 = 180;
    private static final double D360 = 360;
    private static final double D90 = 90;
    private static final double[] deltaCached;//deltaCached[i]==360/(2^(i+1))==180/(2^i)
    private static final int[] doubleOneBitMask;
    public static final long EvenBitMask = 0XAA_AA_AA_AA_AA_AA_AA_AAL;
    public static final long OddBitMask = 0X55_55_55_55_55_55_55_55L;

    static {
        int sz = base32.length;
        for (int i = 0; i < sz; i++) {
            decodeMap.put(base32[i], i);
        }
        deltaCached = new double[64];
        double tmp = D360;
        for (int i = 0; i < 64; i++) {
            deltaCached[i] = tmp = tmp/2.0d;
        }
        doubleOneBitMask = new int[256];
        for (int i = 0; i < 256; i++) {
            doubleOneBitMask[i] = interleavingInsertZeroHelper(i);
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

    public GeoHash dropSignificantBits(int k) {
        if (k >= significantBits)
            return null;
        if (k <=0)
            return this;
        int numberOfBits = significantBits - k;
        int prefixIndex = MAX_BIT_PRECISION - numberOfBits;
        long tmp = -1L << prefixIndex;
        long bits = this.bits & tmp;
        int lenY = numberOfBits / 2;
        int lenX = numberOfBits - lenY;
        final double lngDelta = lenX==0? D360 : deltaCached[lenX-1];// == 360/ Math.pow(2, lenX);
        final double latDelta = lenX == lenY ? lngDelta /2 : lngDelta;//fastDoubleDecPow2(lenY, D180);// == 180/ Math.pow(2, lenY);
        final long latBits = this.latBits >>> (this.getNumLatBits()-lenY);
        final long lngBits = this.lonBits >>> (this.getNumLonBits()-lenX);
        final double minLat = latBits * latDelta - D90;
        final double maxLat = minLat + latDelta;
        final double minLng = lngBits * lngDelta - D180;
        final double maxLng = minLng + lngDelta;
        return new GeoHash(bits, (byte) numberOfBits, latBits, lngBits, minLat, maxLat, minLng, maxLng);
    }

    public GeoHash fromPrefix(byte prefixIndex){
        if (prefixIndex <0 || prefixIndex >MAX_BIT_PRECISION)
            return null;
        byte numberOfBits = (byte) (MAX_BIT_PRECISION - prefixIndex);
        long tmp = -1L << prefixIndex;
        long bits = this.bits & tmp;
        int lenY = numberOfBits / 2;
        int lenX = numberOfBits - lenY;
        final double lngDelta = lenX==0? D360 : deltaCached[lenX-1];// == 360/ Math.pow(2, lenX);
        final double latDelta = lenX == lenY ? lngDelta /2 : lngDelta;//fastDoubleDecPow2(lenY, D180);// == 180/ Math.pow(2, lenY);
        final long latBits = this.latBits >>> (this.getNumLatBits()-lenY);
        final long lngBits = this.lonBits >>> (this.getNumLonBits()-lenX);
        final double minLat = latBits * latDelta - D90;
        final double maxLat = minLat + latDelta;
        final double minLng = lngBits * lngDelta - D180;
        final double maxLng = minLng + lngDelta;
        return new GeoHash(bits, numberOfBits, latBits, lngBits, minLat, maxLat, minLng, maxLng);
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
        final long firstFiveBitsMask = 0xf800000000000000L;
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
        final byte significantBits = this.significantBits;
        return recombineLatLonBitsToHash(latBits, lonBits, significantBits);
    }

    public GeoHashNavIterator NavigateSquare(){
        return new GeoHashNavIterator(this);
    }

    public GeoHash navFromSteps(int latitudeStep,int longitudeStep){
        return recombineLatLonBitsToHash(latBits + latitudeStep, lonBits+longitudeStep);
    }

    public GeoHash getNorthernNeighbour() {
        int lenY = significantBits>>>1;
        long otherBits = bits& EvenBitMask |
                ((bits | EvenBitMask)+(1L<< MAX_BIT_PRECISION-(lenY<<1)) & OddBitMask);
        double deltaY=deltaCached[lenY];//== 180/ Math.pow(2, lenY);
        double otherMinLat=boundingBox.maxLat;
        double otherMaxLat=otherMinLat+deltaY;
        return new GeoHash(otherBits, significantBits, latBits+1, lonBits, otherMinLat, otherMaxLat, boundingBox.minLon, boundingBox.maxLon);
    }

    public GeoHash getSouthernNeighbour() {
        int lenY = significantBits>>>1;
        long otherBits = (bits & EvenBitMask) |
                ((bits & OddBitMask) - (1L << MAX_BIT_PRECISION - (lenY<<1)) & OddBitMask);
        double deltaY=deltaCached[lenY];//== 180/ Math.pow(2, lenY);
        double otherMaxLat=boundingBox.minLat;
        double otherMinLat=otherMaxLat-deltaY;
        return new GeoHash(otherBits, significantBits, latBits-1, lonBits, otherMinLat, otherMaxLat, boundingBox.minLon, boundingBox.maxLon);
    }

    public GeoHash getEasternNeighbour() {
        int lenX = significantBits - (significantBits >>1);
        long otherBits = (bits& OddBitMask)|
                (bits |OddBitMask)+(1L<< MAX_BIT_PRECISION-((lenX<<1)-1)) & EvenBitMask;
        double deltaX=deltaCached[lenX-1];//== 360/ Math.pow(2, lenX);
        double otherMinLng=boundingBox.maxLon;
        double otherMaxLng=otherMinLng+deltaX;
        return new GeoHash(otherBits, significantBits, latBits, lonBits + 1, boundingBox.minLat, boundingBox.maxLat, otherMinLng, otherMaxLng);
    }

    public GeoHash getWesternNeighbour() {
        int lenX = significantBits - (significantBits >>1);
        long otherBits = (bits& OddBitMask)|
                (bits &EvenBitMask)-(1L<< MAX_BIT_PRECISION-((lenX<<1)-1)) & EvenBitMask;
        double deltaX=deltaCached[lenX-1];//== 360/ Math.pow(2, lenX);
        double otherMaxLng=boundingBox.minLon;
        double otherMinLng=otherMaxLng-deltaX;
        return new GeoHash(otherBits, significantBits, latBits, lonBits - 1, boundingBox.minLat, boundingBox.maxLat, otherMinLng, otherMaxLng);
    }

    public GeoHash getNorthernEastNeighbour() {
        int lenY = significantBits>>>1;
        int lenX = significantBits - lenY;
        long otherBits =
                ((bits |OddBitMask)+(1L<< MAX_BIT_PRECISION-((lenX<<1)-1)) & EvenBitMask)
                | ((bits | EvenBitMask)+(1L<< MAX_BIT_PRECISION-(lenY<<1)) & OddBitMask);
        double deltaY=deltaCached[lenY];//== 180/ Math.pow(2, lenY);
        double otherMinLat=boundingBox.maxLat;
        double otherMaxLat=otherMinLat+deltaY;
        double deltaX=deltaCached[lenX-1];//== 360/ Math.pow(2, lenX);
        double otherMinLng=boundingBox.maxLon;
        double otherMaxLng=otherMinLng+deltaX;
        return new GeoHash(otherBits, significantBits, latBits+1, lonBits+1, otherMinLat, otherMaxLat, otherMinLng, otherMaxLng);
    }

    public GeoHash getNorthernWestNeighbour() {
        int lenY = significantBits>>>1;
        int lenX = significantBits - lenY;
        long otherBits =
                ((bits &EvenBitMask)-(1L<< MAX_BIT_PRECISION-((lenX<<1)-1)) & EvenBitMask)
                        | ((bits | EvenBitMask)+(1L<< MAX_BIT_PRECISION-(lenY<<1)) & OddBitMask);
        double deltaY=deltaCached[lenY];//== 180/ Math.pow(2, lenY);
        double otherMinLat=boundingBox.maxLat;
        double otherMaxLat=otherMinLat+deltaY;
        double deltaX=deltaCached[lenX-1];//== 360/ Math.pow(2, lenX);
        double otherMaxLng=boundingBox.minLon;
        double otherMinLng=otherMaxLng-deltaX;
        return new GeoHash(otherBits, significantBits, latBits+1, lonBits-1, otherMinLat, otherMaxLat, otherMinLng, otherMaxLng);
    }

    public GeoHash getSouthernEastNeighbour() {
        int lenY = significantBits>>>1;
        int lenX = significantBits - lenY;
        long otherBits =
                ((bits |OddBitMask)+(1L<< MAX_BIT_PRECISION-((lenX<<1)-1)) & EvenBitMask)
                        | ((bits & OddBitMask) - (1L << MAX_BIT_PRECISION - (lenY<<1)) & OddBitMask);
        double deltaY=deltaCached[lenY];//== 180/ Math.pow(2, lenY);
        double otherMaxLat=boundingBox.minLat;
        double otherMinLat=otherMaxLat-deltaY;
        double deltaX=deltaCached[lenX-1];//== 360/ Math.pow(2, lenX);
        double otherMinLng=boundingBox.maxLon;
        double otherMaxLng=otherMinLng+deltaX;
        return new GeoHash(otherBits, significantBits, latBits-1, lonBits+1, otherMinLat, otherMaxLat, otherMinLng, otherMaxLng);
    }

    public GeoHash getSouthernWestNeighbour() {

        int lenY = significantBits>>>1;
        int lenX = significantBits - lenY;
        long otherBits =
                ((bits &EvenBitMask)-(1L<< MAX_BIT_PRECISION-((lenX<<1)-1)) & EvenBitMask)
                        | ((bits & OddBitMask) - (1L << MAX_BIT_PRECISION - (lenY<<1)) & OddBitMask);
        double deltaY=deltaCached[lenY];//== 180/ Math.pow(2, lenY);
        double otherMaxLat=boundingBox.minLat;
        double otherMinLat=otherMaxLat-deltaY;
        double deltaX=deltaCached[lenX-1];//== 360/ Math.pow(2, lenX);
        double otherMaxLng=boundingBox.minLon;
        double otherMinLng=otherMaxLng-deltaX;
        return new GeoHash(otherBits, significantBits, latBits-1, lonBits-1, otherMinLat, otherMaxLat, otherMinLng, otherMaxLng);
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

    private static GeoHash recombineLatLonBitsToHash(final long latBits, final long lngBits
            , final byte numberOfBits) {
        final int lenY=numberOfBits>>>1;//numberOfBits/2
        final int lenX=numberOfBits-lenY;
        final double lngDelta = lenX==0? D360 : deltaCached[lenX-1];// == 360/ Math.pow(2, lenX);
        final double latDelta = lenX == lenY ? lngDelta /2 : lngDelta;//fastDoubleDecPow2(lenY, D180);// == 180/ Math.pow(2, lenY);
        final double minLat = latBits * latDelta - D90;
        final double maxLat = minLat + latDelta;
        final double minLng = lngBits * lngDelta - D180;
        final double maxLng = minLng + lngDelta;
        long lngInterleavingBits = interleavingInsertZero((int) lngBits);
        long latInterleavingBits = interleavingInsertZero((int) (lenX == lenY ? latBits : latBits<<1));
        long bits = (lngInterleavingBits<<1) ^ latInterleavingBits;
        bits = bits << (MAX_BIT_PRECISION - (lenX<<1));

        return new GeoHash(bits, numberOfBits, latBits, lngBits, minLat, maxLat, minLng, maxLng);
    }

    public static GeoHash withBitPrecision(final double latitude, final double longitude, final int numberOfBits) {
        CheckLatLng(latitude, longitude, numberOfBits);
        final int lenY=numberOfBits>>>1;//numberOfBits/2
        final int lenX=numberOfBits-lenY;
        final double lngDelta = lenX==0? D360 : deltaCached[lenX-1];// == 360/ Math.pow(2, lenX);
        final double latDelta = lenX == lenY ? lngDelta /2 : lngDelta;// == 180/ Math.pow(2, lenY);
        final long latBits = (long) ((latitude+ D90)/latDelta);
        final long lngBits = (long) ((longitude+ D180)/lngDelta);
        final double minLat = latBits * latDelta - D90;
        final double maxLat = minLat + latDelta;
        final double minLng = lngBits * lngDelta - D180;
        final double maxLng = minLng + lngDelta;
        long lngInterleavingBits = interleavingInsertZero((int) lngBits);
        long latInterleavingBits = interleavingInsertZero((int) (lenX == lenY ? latBits : latBits<<1));
        long bits = (lngInterleavingBits<<1) ^ latInterleavingBits;
        bits = bits << (MAX_BIT_PRECISION - (lenX<<1));
        return new GeoHash(bits, (byte) numberOfBits, latBits, lngBits, minLat, maxLat, minLng, maxLng);
    }

    private static long interleavingInsertZero(int bits) {
        long result = doubleOneBitMask[(bits & 0xff)];
        result += (long)(doubleOneBitMask[(bits>>>8) & 0xff]) << 16;
        result += (long)(doubleOneBitMask[(bits>>>16) & 0xff]) << 32;
        result += (long)(doubleOneBitMask[(bits>>>24) & 0xff]) << 48;
        return result;
    }

    private static int interleavingInsertZeroHelper(int bits) {
        bits |= bits << 16; bits &= 0x0000ffff0000ffffL;
        bits |= bits << 8;  bits &= 0x00ff00ff00ff00ffL;
        bits |= bits << 4;  bits &= 0x0f0f0f0f0f0f0f0fL;
        bits |= bits << 2;  bits &= 0x3333333333333333L;
        bits |= bits << 1;  bits &= 0x5555555555555555L;
        return bits;
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
     * This method uses the given number of characters as the desired precision
     * value. The hash can only be 64bits long, thus a maximum precision of 12
     * characters can be achieved.
     */
    public static GeoHash withCharacterPrecision(final double latitude, final double longitude, final int numberOfCharacters) {
        if (numberOfCharacters > MAX_CHARACTER_PRECISION) {
            throw new IllegalArgumentException("A geohash can only be " + MAX_CHARACTER_PRECISION + " character long.");
        }
        int desiredPrecision = Math.min(numberOfCharacters * 5, 60);
        return withBitPrecision(latitude, longitude, desiredPrecision);
    }

    public static GeoHash fromLongValue(final long bits, final byte significantBits) {
        final int lenY = significantBits/2;
        final int lenX = significantBits - lenY;
        long tmp = bits>>>(MAX_BIT_PRECISION - (lenX<<1));
        long latBits = 0, lngBits = 0;

        long lowestOneBit = tmp & -tmp;
        while (lowestOneBit != 0){
            int lowestIndex = Long.numberOfTrailingZeros(lowestOneBit);
            long bitSetIndex = 1L << (lowestIndex>>1);
            if ((OddBitMask & lowestOneBit) != 0){
                latBits |= bitSetIndex;
            }else{
                lngBits |= bitSetIndex;
            }
            tmp ^= lowestOneBit;
            lowestOneBit = tmp & -tmp;
        }
        latBits = lenX == lenY ?latBits:latBits>>>1;

        final double lngDelta = lenX==0? D360 : deltaCached[lenX-1];// == 360/ Math.pow(2, lenX);
        final double latDelta = lenX == lenY ? lngDelta /2 : lngDelta;//fastDoubleDecPow2(lenY, D180);// == 180/ Math.pow(2, lenY);
        final double minLat = latBits * latDelta - D90;
        final double maxLat = minLat + latDelta;
        final double minLng = lngBits * lngDelta - D180;
        final double maxLng = minLng + lngDelta;
        return new GeoHash(bits, significantBits, latBits, lngBits, minLat, maxLat, minLng, maxLng);
    }

    //TODO optimized using numberOfLeadingZeros
    /**
     * build a new {@link GeoHash} from a base32-encoded {@link String}.<br>
     * This will also set up the hashes bounding box and other values, so it can
     * also be used with functions like within().
     */
    public static GeoHash fromGeohashString(String geohash) {
        double latMin = -D90, latMax = D90;
        double lonMin = -D180, lonMax = D180;
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
        long c = a ^ b;
        return Long.numberOfLeadingZeros(c);
    }

    public String toBinaryStringOnlySignificantBits(){
        return Long.toBinaryString(bits>>>(MAX_BIT_PRECISION - significantBits));
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
     * that is boundingBox contains this grid
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
        return significantBits-significantBits/2;
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
