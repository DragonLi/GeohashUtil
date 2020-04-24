package geohashutil.asiainfo.com.test;

import geohashutil.asiainfo.com.BoundingBox;
import geohashutil.asiainfo.com.GeoHash;
import geohashutil.asiainfo.com.GeoHashSearchUtil;
import geohashutil.asiainfo.com.WGS84Point;
import javafx.util.Pair;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static geohashutil.asiainfo.com.GeoHash.commonPrefixLength;
import static org.junit.Assert.*;

public class GeoHashTest {

    private void assertWithin(GeoHash hash, GeoHash bbox) {
        assertTrue(hash + " should be within " + bbox, hash.within(bbox));
    }

    private void printBoundingBox(GeoHash hash) {
        System.out.println("Bounding Box: \ncenter =" + hash.boundingBox.getCenterPoint());
        System.out.print("corners=");
        System.out.println(hash.boundingBox);
    }

    @Test
    public void testSameNumbersHave64BitsPrefix() {
        for (long a = 0; a < 120000000; a += 101) {
            long b = a;
            assertPrefixLength(64,a,b);
        }
    }

    @Test
    public void testFromLongFast() {
        final int testCount = 100000;
        final long seed = System.nanoTime();
        java.util.Random r = new java.util.Random(seed);
        for (int i = 0; i < testCount; i++) {
            long bits = r.nextLong();
            for (byte numberOfBits = 0; numberOfBits <= 64; numberOfBits++) {
                GeoHash test = GeoHash.fromLongValue(bits, numberOfBits);
                GeoHashSlow old = GeoHashSlow.fromLongValue(bits, numberOfBits);
                boolean ass = old.testEquals(test);
                if (!ass){
                    System.out.println(Long.toBinaryString(bits));
                    System.out.println(old);
                    System.out.println(test);
                }
                assertTrue(ass);
            }
        }

        r = new java.util.Random(seed);
        long elapsedFast = 0;
        for (int i = 0; i < testCount; i++) {
            long bits = r.nextLong();
            for (byte numberOfBits = 0; numberOfBits <= 64; numberOfBits++) {
                long start = System.nanoTime();
                GeoHash hash = GeoHash.fromLongValue(bits, numberOfBits);
                elapsedFast += System.nanoTime() - start;
            }
        }

        r = new java.util.Random(seed);
        long elapsedOld = 0;
        for (int i = 0; i < testCount; i++) {
            long bits = r.nextLong();
            for (byte numberOfBits = 0; numberOfBits <= 64; numberOfBits++) {
                long start = System.nanoTime();
                GeoHashSlow hash = GeoHashSlow.fromLongValue(bits, numberOfBits);
                elapsedOld += System.nanoTime() - start;
            }
        }

        System.out.println(elapsedFast);
        System.out.println(elapsedOld);
        double delta = (double)elapsedOld - (double)elapsedFast;
        System.out.println(delta/(double)elapsedOld);
    }

    @Test
    public void testKnownPrefixLenghts() {
        char t = 'a';
        System.out.println(t);
        System.out.println((int)t);
        t='9';
        System.out.println(t);
        System.out.println((int)t);

        long a = 0x8f00000000000000l;
        long b = 0x8000000000000000l;
        long c = 0x8800000000000000l;
        assertPrefixLength(4, a, b);
        assertPrefixLength(4, b, c);
        assertPrefixLength(5, a, c);
        assertPrefixLength(0, 0x0, a);
        assertPrefixLength(16, 0x8888300000000000l, 0x8888c00000000000l);
    }

    private void assertPrefixLength(int length, long a, long b) {
        assertEquals(length, commonPrefixLength(a, b));
    }

    @Test
    public void testFromPrefix(){
        double lng=112.1213;
        double lat=32.214;
        GeoHash hash = GeoHash.withBitPrecision(lat, lng, 40);
        //1110010011111011110110111000101001010000000000000000000000000000
        GeoHash prefix = hash.fromPrefix((byte) 58);
        System.out.println(Long.toBinaryString(hash.bits));
        System.out.println(Long.toBinaryString(prefix.bits));
        assertEquals(Long.toBinaryString(prefix.bits),"1110010000000000000000000000000000000000000000000000000000000000");
    }

    @Test
    public void testComparePrefix(){
        double lng=112.1213;
        double lat=32.214;
        GeoHash a = GeoHash.withBitPrecision(lat, lng, 40);
        //1110010011111011110110111000101001010000000000000000000000000000
        GeoHash b = GeoHash.withBitPrecision(lat+0.01, lng+0.01, 40);
        //1110010011111011110110111011000010111100000000000000000000000000
        System.out.println(Long.toBinaryString(a.bits));
        System.out.println(Long.toBinaryString(b.bits));
        byte index = GeoHashSearchUtil.comparePrefix(a.bits,b.bits);
        System.out.println(index);
        GeoHash prefix = a.fromPrefix(index);
        System.out.println(Long.toBinaryString(prefix.bits));
        assertEquals(index,64-"11100100111110111101101110".length());
        assertEquals(Long.toBinaryString(prefix.bits),"1110010011111011110110111000000000000000000000000000000000000000");
    }

    @Test
    public void testFastGeohash(){
        final int testCount = 100000;
        final long seed = System.nanoTime();
        java.util.Random r = new java.util.Random(seed);

        for (int i = 0; i < testCount; i++) {
            for (int numberOfBits = 0; numberOfBits <= 64; numberOfBits++) {
                double lng=Math.random()*360-180;
                double lat=Math.random()*180 -90;
                GeoHashSlow old = new GeoHashSlow(lat,lng, numberOfBits);
                GeoHash test = GeoHash.withBitPrecision(lat,lng, numberOfBits);
                boolean ass = old.testEquals(test);
                if (!ass){
                    System.out.println(lng);
                    System.out.println(lat);
                    System.out.println(test);
                    System.out.println(old);
                }
                assertTrue(ass);
            }
        }

        r = new java.util.Random(seed);
        long elapsedFast = 0;
        for (int i = 0; i < testCount; i++) {
            for (int numberOfBits = 0; numberOfBits <= 64; numberOfBits++) {
                double lng=r.nextDouble()*360-180;
                double lat=r.nextDouble()*180 -90;
                long start = System.nanoTime();
                GeoHash hash = GeoHash.withBitPrecision(lat,lng, numberOfBits);
                elapsedFast += System.nanoTime() - start;
            }
        }

        r = new java.util.Random(seed);
        long elapsedOld = 0;
        for (int i = 0; i < testCount; i++) {
            for (int numberOfBits = 0; numberOfBits <= 64; numberOfBits++) {
                double lng=r.nextDouble()*360-180;
                double lat=r.nextDouble()*180 -90;
                long start = System.nanoTime();
                GeoHashSlow test = new GeoHashSlow(lat,lng, numberOfBits);
                elapsedOld += System.nanoTime() - start;
            }
        }

        System.out.println(elapsedFast);
        System.out.println(elapsedOld);
        double delta = (double)elapsedOld - (double)elapsedFast;
        System.out.println(delta/(double)elapsedOld);
    }

    @Test
    public void testLeastBoundingSliceEff(){
        double eff = 0;
        int sliceNumAvg = 0;
        final int testCounts = 100000;
        for (int i = 0; i < testCounts; i++) {
            BoundingBox box = prepareBoundingBox();
            List<GeoHash> slices = GeoHashSearchUtil.leastBoundingSliceMerged(box,36);
            sliceNumAvg += slices.size();
            double total = 0;
            for (GeoHash slice : slices) {
                BoundingBox boundingBox = slice.boundingBox;
                total += boundingBox.getLatitudeSize()* boundingBox.getLongitudeSize();
            }
            eff += box.getLatitudeSize()*box.getLongitudeSize()/total;
        }
        System.out.println(eff/ testCounts);
        System.out.println(sliceNumAvg/(float)testCounts);
    }

    @Test
    public void testLeastBoundingSlice(){
        BoundingBox box = prepareBoundingBox();
        List<GeoHash> slices = GeoHashSearchUtil.leastBoundingSlice(box,36);
        testSliceCoverBox(box, slices);
        GeoHash p = slices.get(0);
        final byte len = p.significantBits;
        final int ky = len / 2;
        final int kx = len - ky;
        final double width = box.getLongitudeSize();
        final double height = box.getLatitudeSize();
        assertTrue((180 / Math.pow(2, ky)) >= height / 2);
        assertTrue((360 / Math.pow(2, kx)) >= width / 2);
        assertTrue((180 / Math.pow(2, ky + 1)) < height / 2 || (360 / Math.pow(2, kx + 1)) < width / 2 || len == 36);
    }

    @Test
    public void testLeastBoundingSliceMerged(){
        BoundingBox box = prepareBoundingBox();
        List<GeoHash> slices = GeoHashSearchUtil.leastBoundingSliceMerged(box,36);
        testSliceCoverBox(box, slices);
        int size = slices.size();
        assertTrue(size <= 6);
        for (int i = 0; i < size; i++) {
            GeoHash item = slices.get(i);
            for (int j = i+1; j < size; j++) {
                assertTrue(!item.equals(slices.get(j)));
            }
        }
    }

    private BoundingBox prepareBoundingBox(){
        double lng=112.1213;
        double lat=32.214;
        double width = Math.random()*0.5+0.01;
        double height = width * (Math.random()*0.6+0.7);
        BoundingBox box = new BoundingBox(lat-height,lat+height,lng-width,lng+width);
        return box;
    }

    private void testSliceCoverBox(BoundingBox box, List<GeoHash> slices) {
        WGS84Point upperLeft = box.getUpperLeft();
        testSliceCoverCorner(box, slices, upperLeft);
        WGS84Point lowerRight = box.getLowerRight();
        testSliceCoverCorner(box, slices, lowerRight);
        WGS84Point upperRight = box.getUpperRight();
        testSliceCoverCorner(box, slices, upperRight);
        WGS84Point lowerLeft = box.getLowerLeft();
        testSliceCoverCorner(box, slices, lowerLeft);
        assertTrue(slices.size() <= 9);
    }

    private void testSliceCoverCorner(BoundingBox box, List<GeoHash> slices, WGS84Point lowerRight) {
        boolean isCovered = false;
        for (GeoHash geoHash : slices) {
            if (geoHash.boundingBox.contains(lowerRight)) {
                isCovered = true;
                break;
            }
        }
        assertTrue( isCovered);
    }

    private void debugSliceBox(BoundingBox box, List<GeoHash> slices) {
        System.out.println(box);
        for (GeoHash geoHash : slices) {
            System.out.println(geoHash);
        }
    }

    @Test
    public void testLeastBoundingGeoGrid(){
        double lng=112.1213;
        double lat=32.214;
        WGS84Point a = WGS84Point.Create(lat,lng);
        WGS84Point b = WGS84Point.Create(lat+0.01,lng+0.01);
        BoundingBox box = new BoundingBox(a,b);
        Pair<GeoHash,GeoHash> pair = GeoHashSearchUtil.leastBoundingGeoGrid(box);
        GeoHash grid1 = pair.getKey();
        GeoHash grid2 = pair.getValue();
        assertEquals(Long.toBinaryString(grid1.bits),"1110010011111011110110111000000000000000000000000000000000000000");
        assertEquals(grid1.significantBits,26);
        assertEquals(grid2,null);

        //00 , 10
        GeoHash h1 = GeoHash.fromLongValue(0B1110010011111011110110110011000000000000000000000000000000000000L, (byte) 28);
        GeoHash h2 = GeoHash.fromLongValue(0B1110010011111011110110111001000000000000000000000000000000000000L, (byte) 28);
        pair = GeoHashSearchUtil.leastBoundingGeoGrid(h1,h2);
        grid1 = pair.getKey();
        grid2 = pair.getValue();
        assertEquals(Long.toBinaryString(grid1.bits),"1110010011111011110110110000000000000000000000000000000000000000");
        assertEquals(grid1.significantBits,26);
        assertEquals(Long.toBinaryString(grid2.bits),"1110010011111011110110111000000000000000000000000000000000000000");
        assertEquals(grid2.significantBits,26);

        //01 , 10
        h1 = GeoHash.fromLongValue(0B1110010011111011110110110111000000000000000000000000000000000000L, (byte) 28);
        h2 = GeoHash.fromLongValue(0B1110010011111011110110111001000000000000000000000000000000000000L, (byte) 28);
        pair = GeoHashSearchUtil.leastBoundingGeoGrid(h1,h2);
        grid1 = pair.getKey();
        grid2 = pair.getValue();
        assertEquals(Long.toBinaryString(grid1.bits),"1110010011111011110110110000000000000000000000000000000000000000");
        assertEquals(grid1.significantBits,24);
        assertEquals(grid2,null);

        //01 , 11
        h1 = GeoHash.fromLongValue(0B1110010011111011110110110111000000000000000000000000000000000000L, (byte) 28);
        h2 = GeoHash.fromLongValue(0B1110010011111011110110111101000000000000000000000000000000000000L, (byte) 28);
        pair = GeoHashSearchUtil.leastBoundingGeoGrid(h1,h2);
        grid1 = pair.getKey();
        grid2 = pair.getValue();
        assertEquals(Long.toBinaryString(grid1.bits),"1110010011111011110110110100000000000000000000000000000000000000");
        assertEquals(grid1.significantBits,26);
        assertEquals(Long.toBinaryString(grid2.bits),"1110010011111011110110111100000000000000000000000000000000000000");
        assertEquals(grid2.significantBits,26);

        //11 , 10
        h1 = GeoHash.fromLongValue(0B1110010011111011110110111111000000000000000000000000000000000000L, (byte) 28);
        h2 = GeoHash.fromLongValue(0B1110010011111011110110111001000000000000000000000000000000000000L, (byte) 28);
        pair = GeoHashSearchUtil.leastBoundingGeoGrid(h1,h2);
        grid1 = pair.getKey();
        grid2 = pair.getValue();
        assertEquals(Long.toBinaryString(grid1.bits),"1110010011111011110110111100000000000000000000000000000000000000");
        assertEquals(grid1.significantBits,26);
        assertEquals(Long.toBinaryString(grid2.bits),"1110010011111011110110111000000000000000000000000000000000000000");
        assertEquals(grid2.significantBits,26);

        //01 , 00
        h1 = GeoHash.fromLongValue(0B1110010011111011110110110111000000000000000000000000000000000000L, (byte) 28);
        h2 = GeoHash.fromLongValue(0B1110010011111011110110110001000000000000000000000000000000000000L, (byte) 28);
        pair = GeoHashSearchUtil.leastBoundingGeoGrid(h1,h2);
        grid1 = pair.getKey();
        grid2 = pair.getValue();
        assertEquals(Long.toBinaryString(grid1.bits),"1110010011111011110110110100000000000000000000000000000000000000");
        assertEquals(grid1.significantBits,26);
        assertEquals(Long.toBinaryString(grid2.bits),"1110010011111011110110110000000000000000000000000000000000000000");
        assertEquals(grid2.significantBits,26);
    }

    @Test
    public void itShouldCreateAHashWithMaximumPrecisionOf64Bits() {
        GeoHash.withBitPrecision(10.0, 10.0, 64);
    }

    @Test(expected = IllegalArgumentException.class)
    public void itShouldThrowWhenTheBitPrecisionIsMoreThan64Bits() {
        GeoHash.withBitPrecision(46.0, 8.0, 70);
    }

    @Test(expected = IllegalArgumentException.class)
    public void itShouldThrowWhenTheCharacterPrecisionIsTooBig() {
        GeoHash.withCharacterPrecision(10.0, 120.0, 14);
    }

    @Test
    public void testToBase32() {
        GeoHash hash = GeoHash.fromLongValue(0x6ff0414000000000l, (byte) 25);
        String base32 = hash.toBase32();
        assertEquals("ezs42", base32);
    }

    @Test(expected = IllegalStateException.class)
    public void toBase32ShouldThrowWhenPrecisionIsNotAMultipleOf5() {
        GeoHash hash = GeoHash.fromLongValue(0x6ff0413000000000l, (byte) 24);
        hash.toBase32();
    }

    @Test
    public void testDecode() {
        // for all lat/lon pairs check decoded point is in the same bbox as the
        // geohash formed by encoder
        for (GeoHash gh : RandomGeohashes.fullRange()) {
            BoundingBox bbox = gh.boundingBox;
            GeoHash decodedHash = GeoHash.fromGeohashString(gh.toBase32());
            WGS84Point decodedCenter = decodedHash.boundingBox.getCenterPoint();

            assertTrue("bbox " + bbox + " should contain the decoded center value " + decodedCenter, bbox
                    .contains(decodedCenter));
            BoundingBox decodedBoundingBox = decodedHash.boundingBox;
            assertEquals(bbox, decodedBoundingBox);
            assertEquals(gh, decodedHash);
            assertEquals(gh.toBase32(), decodedHash.toBase32());
        }
    }

    @Test
    public void testWithin() {
        GeoHash hash = GeoHash.fromLongValue(0x6ff0414000000000l, (byte) 25);
        System.out.println(hash.toBase32());
        assertEquals("ezs42", hash.toBase32());

        GeoHash bbox = GeoHash.fromLongValue(0x6ff0000000000000l, (byte) 12);
        assertWithin(hash, bbox);
    }

    @Test
    public void testNotWithin() {
        GeoHash hash = GeoHash.fromLongValue(0x6ff0414000000000l, (byte) 25);
        assertEquals("ezs42", hash.toBase32());

        GeoHash bbox = GeoHash.fromLongValue(0x6fc0000000000000l, (byte) 12);
        assertFalse(hash + " should NOT be within " + bbox, hash.within(bbox));
    }

    @Test
    public void testConstructorWithBitPrecision() {
        GeoHash hash1 = GeoHash.withBitPrecision(45, 120, 20);
        assertEquals(hash1.significantBits, 20);
        System.out.println(hash1);
        System.out.println(hash1.toBase32());

        GeoHash hash2 = GeoHash.withBitPrecision(45, 120, 55);
        assertEquals(hash2.significantBits, 55);
        System.out.println(hash2);
        System.out.println(hash2.toBase32());

        assertTrue(hash2.within(hash1));

        // this should match Dave Troys Codebase. This is also his maximum
        // accuracy (12 5-nibbles).
        GeoHash hash3 = GeoHash.withBitPrecision(20, 31, 60);
        assertEquals("sew1c2vs2q5r", hash3.toBase32());
    }

    @Test
    public void testLatLonBoundingBoxes() {
        GeoHash hash = GeoHash.withBitPrecision(40, 120, 10);
        System.out.println(hash.toBase32());
        printBoundingBox(hash);
    }

    @Test
    public void testByCharacterPrecision() {
        assertEncodingWithCharacterPrecision(new WGS84Point(20, 31), 12, "sew1c2vs2q5r");
        assertEncodingWithCharacterPrecision(new WGS84Point(-20, 31), 12, "ksqn1rje83g2");
        assertEncodingWithCharacterPrecision(new WGS84Point(-20.783236276, 31.9867127312312), 12, "ksq9zbs0b7vw");

        WGS84Point point = new WGS84Point(-76.5110040642321, 39.0247389581054);
        String fullStringValue = "hf7u8p8gn747";
        for (int characters = 12; characters > 1; characters--) {
            assertEncodingWithCharacterPrecision(point, characters, fullStringValue.substring(0, characters));
        }

        assertEncodingWithCharacterPrecision(new WGS84Point(39.0247389581054, -76.5110040642321), 12, "dqcw4bnrs6s7");

        String geoHashString = GeoHash.geoHashStringWithCharacterPrecision(point.latitude, point.longitude, 12);
        assertEquals(fullStringValue, geoHashString);
    }

    private void assertEncodingWithCharacterPrecision(WGS84Point point, int numberOfCharacters, String stringValue) {
        GeoHash hash = GeoHash.withCharacterPrecision(point.latitude, point.longitude, numberOfCharacters);
        assertEquals(stringValue, hash.toBase32());
    }

    @Test
    public void testGetLatitudeBits() {
        GeoHash hash = GeoHash.withBitPrecision(30, 30, 16);
        assertEquals(0xaal, hash.latBits);
        assertEquals(8, hash.getNumLatBits());
    }

    @Test
    public void testGetLongitudeBits() {
        GeoHash hash = GeoHash.withBitPrecision(30, 30, 16);
        assertEquals(0x95l, hash.lonBits);
        assertEquals(8, hash.getNumLonBits());
    }

    @Test
    public void testNeighbourLocationCode() {
        // set up corner case
        GeoHash hash = GeoHash.fromLongValue(0xc400000000000000l, (byte) 7);

        assertEquals(0x8, hash.lonBits);
        assertEquals(4, hash.getNumLonBits());
        assertEquals(0x5, hash.latBits);
        assertEquals(3, hash.getNumLatBits());

        GeoHash north = hash.getNorthernNeighbour();
        assertEquals(0xd000000000000000l, north.bits);
        assertEquals(7, north.significantBits);

        GeoHash south = hash.getSouthernNeighbour();
        assertEquals(0xc000000000000000l, south.bits);
        assertEquals(7, south.significantBits);

        GeoHash east = hash.getEasternNeighbour();
        assertEquals(0xc600000000000000l, east.bits);

        // NOTE: this is actually a corner case!
        GeoHash west = hash.getWesternNeighbour();
        assertEquals(0x6e00000000000000l, west.bits);

        // NOTE: and now, for the most extreme corner case in 7-bit geohash-land
        hash = GeoHash.fromLongValue(0xfe00000000000000l, (byte) 7);

        east = hash.getEasternNeighbour();
        assertEquals(0x5400000000000000l, east.bits);

    }

    @Test
    public void testEqualsAndHashCode() {
        GeoHash hash1 = GeoHash.withBitPrecision(30, 30, 24);
        GeoHash hash2 = GeoHash.withBitPrecision(30, 30, 24);
        GeoHash hash3 = GeoHash.withBitPrecision(30, 30, 10);

        assertTrue(hash1.equals(hash2) && hash2.equals(hash1));
        assertFalse(hash1.equals(hash3) && hash3.equals(hash1));

        assertEquals(hash1.hashCode(), hash2.hashCode());
        assertFalse(hash1.hashCode() == hash3.hashCode());
    }

    @Test
    public void testAdjacentHashes() {
        GeoHash[] adjacent = GeoHash.fromGeohashString("dqcw4").getAdjacent();
        assertEquals(8, adjacent.length);
    }

    @Test
    public void testMovingInCircle() {
        // moving around hashes in a circle should be possible
        checkMovingInCircle(34.2, -45.123);
        // this should also work at the "back" of the earth
        checkMovingInCircle(45, 180);
        checkMovingInCircle(90, 180);
        checkMovingInCircle(0, -180);
    }

    private void checkMovingInCircle(double latitude, double longitude) {
        GeoHash start;
        GeoHash end;
        start = GeoHash.withCharacterPrecision(latitude, longitude, 12);
        end = start.getEasternNeighbour();
        end = end.getSouthernNeighbour();
        end = end.getWesternNeighbour();
        end = end.getNorthernNeighbour();
        assertEquals(start, end);
        assertEquals(start.boundingBox, end.boundingBox);
    }

    @Test
    public void testMovingAroundWorldOnHashStrips() throws Exception {
        String[] directions = { "Northern", "Eastern", "Southern", "Western" };
        for (String direction : directions) {
            checkMoveAroundStrip(direction);
        }
    }

    private void checkMoveAroundStrip(String direction) throws Exception {
        for (int bits = 2; bits < 16; bits++) {

            GeoHash hash = RandomGeohashes.createWithPrecision(bits);
            Method method = hash.getClass().getDeclaredMethod("get" + direction + "Neighbour");
            GeoHash result = hash;

            // moving this direction 2^bits times should yield the same hash
            // again
            for (int i = 0; i < Math.pow(2, bits); i++) {
                result = (GeoHash) method.invoke(result);
            }
            assertEquals(hash, result);
        }
    }

    @Test
    public void testKnownNeighbouringHashes() {
        GeoHash h1 = GeoHash.fromGeohashString("u1pb");
        assertEquals("u0zz", h1.getSouthernNeighbour().toBase32());
        assertEquals("u1pc", h1.getNorthernNeighbour().toBase32());
        assertEquals("u300", h1.getEasternNeighbour().toBase32());
        assertEquals("u302", h1.getEasternNeighbour().getEasternNeighbour().toBase32());
        assertEquals("u1p8", h1.getWesternNeighbour().toBase32());

        assertEquals("sp2j", GeoHash.withCharacterPrecision(41.7, 0.08, 4).toBase32());
    }

    @Test
    public void testKnownAdjacentNeighbours() {
        String center = "dqcjqc";
        String[] adjacent = new String[] { "dqcjqf", "dqcjqb", "dqcjr1", "dqcjq9", "dqcjqd", "dqcjr4", "dqcjr0",
                "dqcjq8" };
        assertAdjacentHashesAre(center, adjacent);

        center = "u1x0dfg";
        adjacent = new String[] { "u1x0dg4", "u1x0dg5", "u1x0dgh", "u1x0dfu", "u1x0dfs", "u1x0dfe", "u1x0dfd",
                "u1x0dff" };
        assertAdjacentHashesAre(center, adjacent);

        center = "sp2j";
        adjacent = new String[] { "ezry", "sp2n", "sp2q", "sp2m", "sp2k", "sp2h", "ezru", "ezrv" };
        assertAdjacentHashesAre(center, adjacent);
    }

    @Test
    public void testThatAdjacentHashesHavePointInitialized() {
        String center = "dqcjqc";
        GeoHash geohash = GeoHash.fromGeohashString(center);
        GeoHash[] adjacentHashes = geohash.getAdjacent();
        for (GeoHash adjacentHash : adjacentHashes) {
            assertNotNull(adjacentHash.boundingBox);
            assertNotNull(adjacentHash.boundingBox.getCenterPoint());
        }
    }

    private void assertAdjacentHashesAre(String centerString, String[] adjacentStrings) {
        GeoHash center = GeoHash.fromGeohashString(centerString);
        GeoHash[] adjacent = center.getAdjacent();
        for (String check : adjacentStrings) {
            assertArrayContainsGeoHash(check, adjacent);
        }
    }

    private void assertArrayContainsGeoHash(String check, GeoHash[] hashes) {
        boolean found = false;
        for (GeoHash hash : hashes) {
            if (hash.toBase32().equals(check)) {
                found = true;
                break;
            }
        }
        assertTrue("Array should contain " + check, found);
    }

    @Test
    public void testNeibouringHashesNearMeridian() {
        GeoHash hash = GeoHash.fromGeohashString("sp2j");
        GeoHash west = hash.getWesternNeighbour();
        assertEquals("ezrv", west.toBase32());
        west = west.getWesternNeighbour();
        assertEquals("ezrt", west.toBase32());
    }

    @Test
    public void testIssue1() {
        double lat = 40.390943;
        double lon = -75.9375;
        GeoHash hash = GeoHash.withCharacterPrecision(lat, lon, 12);

        String base32 = "dr4jb0bn2180";
        GeoHash fromRef = GeoHash.fromGeohashString(base32);
        assertEquals(hash, fromRef);
        assertEquals(base32, hash.toBase32());
        assertEquals(base32, fromRef.toBase32());

        hash = GeoHash.withCharacterPrecision(lat, lon, 10);
        assertEquals("dr4jb0bn21", hash.toBase32());
    }

    @Test
    public void testSimpleWithin() {
        GeoHash hash = GeoHash.withBitPrecision(70, -120, 8);
        GeoHash inside = GeoHash.withBitPrecision(74, -130, 64);
        assertWithin(inside, hash);
    }

    @Test
    public void testToLongAndBack() {
        double lat = 40.390943;
        double lon = -75.9375;
        GeoHash hash = GeoHash.withCharacterPrecision(lat, lon, 10);
        long lv = hash.bits;
        assertEquals(lv + (1 << (64 - hash.significantBits)), hash.next().bits);
        GeoHash hashFromLong = GeoHash.fromLongValue(lv, hash.significantBits);
        assertEquals("dr4jb0bn21", hashFromLong.toBase32());
        assertEquals(hash, hashFromLong);
    }

    @Test
    public void testNext() {
        double lat = 37.7;
        double lon = -122.52;
        GeoHash hash = GeoHash.withBitPrecision(lat, lon, 10);
        GeoHash next = hash.next();
        assertTrue(hash.compareTo(next) < 0);
    }

    @Test
    public void testNextPrev() {
        double lat = 37.7;
        double lon = -122.52;
        GeoHash hash = GeoHash.withBitPrecision(lat, lon, 35);
        GeoHash next = hash.next(2);
        assertTrue(hash.compareTo(next) < 0);
        GeoHash prev1 = next.prev();
        GeoHash prev2 = prev1.next(-1);
        assertTrue(prev1.compareTo(next) < 0);
        System.out.println("hash: " + hash.toBase32());
        System.out.println("next: " + next.toBase32());
        System.out.println("prev1: " + prev1.toBase32());
        System.out.println("prev2: " + prev2.toBase32());

        assertTrue(prev2.compareTo(prev1) < 0);
        assertTrue(prev2.compareTo(hash) == 0);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetCharacterPrecisionThrows() throws Exception {
        GeoHash hash = GeoHash.withBitPrecision(37.7, -122.52, 32);
        hash.getCharacterPrecision();
    }

    @Test
    public void testGetCharacterPrecisionWorksWhenPrecisionIsMultipleOfFive() throws Exception {
        GeoHash hash = GeoHash.withBitPrecision(37.7, -122.52, 60);
        int precision = hash.getCharacterPrecision();
        assertEquals(precision, 12);
    }

    @Test
    public void testStepsBetween() {
        GeoHash bl = GeoHash.withBitPrecision(37.7, -122.52, 35);
        GeoHash ur = GeoHash.withBitPrecision(37.84, -122.35, 35);

        long steps = GeoHash.stepsBetween(bl, bl);
        assertEquals(steps, 0);

        steps = GeoHash.stepsBetween(bl, bl.next(4));
        assertEquals(steps, 4);
    }

    @Test
    public void testCompareTo() {
        GeoHash prevHash = null;
        for (int i = 0; i < 10000; i++) {
            GeoHash hash = RandomGeohashes.createWith5BitsPrecision();
            if (i >= 1) {
                String prevHashBase32 = prevHash.toBase32();
                String hashBase32 = hash.toBase32();
                String errorMessage = String.format("prev: %s, cur: %s", prevHashBase32, hashBase32);
                if (prevHashBase32.compareTo(hashBase32) < 0) {
                    assertTrue(errorMessage, prevHash.compareTo(hash) < 0);
                } else if (prevHashBase32.compareTo(hashBase32) > 0) {
                    assertTrue(errorMessage, prevHash.compareTo(hash) > 0);
                } else {
                    assertTrue(errorMessage, prevHash.compareTo(hash) == 0);
                }
            }
            prevHash = hash;
        }
    }

    @Test
    public void testOrdIsPositive() {
        double lat = 40.390943;
        double lon = 75.9375;
        GeoHash hash = GeoHash.withCharacterPrecision(lat, lon, 12);
        assertEquals(0xcf6915015410500l, hash.ord());
        assertTrue(hash.ord() >= 0);
    }

    @Test
    public void testSecondCaseWhereOrdMustBePositive() {
        GeoHash hash = GeoHash.withCharacterPrecision(-36.919550434870125,174.71024582237604,7);
        assertTrue(hash.ord() > 0);
    }}
