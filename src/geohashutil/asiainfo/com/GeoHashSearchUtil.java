package geohashutil.asiainfo.com;

import javafx.util.Pair;

public class GeoHashSearchUtil {
    private static final int MAX_LEVEL = 40;//40位GeoHash精度约为20米
    private static final byte[] maskPosIndex;
    public static final double LOG2BASE = Math.log(2);

    static {
        maskPosIndex = new byte[256];
        byte c = 0;//最右面的位置是0
        int carrier=1;//下一个进位的位置
        for (int i = 0; i<256; i++) {
            if (i == carrier)//到达更高一个bit的位置，相当于进位了
            {
                ++c;//到达进位位置，最高的1向左移动一位，因此加一
                carrier += carrier;
            }
            maskPosIndex[i]=c;
        }
    }

    public static byte comparePrefix(long a, long b){
        long c = a ^ b;
        byte rShift = 64;
        byte index=64;
        for (int i = 0; i < 8; i++) {
            rShift -= 8;
            index -=8;
            int d = (int) ((c >> rShift) & 0x00000000000000FFL);
            if (d > 0)
            {
                index += maskPosIndex[d];
                break;
            }
        }
        return index;
    }

    public static Pair<GeoHash,GeoHash> leastBoundingGeoGrid(BoundingBox box){
        GeoHash leftTop = box.getUpperLeftHash(MAX_LEVEL);
        GeoHash rightBottom = box.getLowerRightHash(MAX_LEVEL);
        return leastBoundingGeoGrid(leftTop, rightBottom);
    }

    public static Pair<GeoHash, GeoHash> leastBoundingGeoGrid(GeoHash leftTop, GeoHash rightBottom) {
        byte prefixIndex = comparePrefix(leftTop.bits,rightBottom.bits);
        byte prefixAdjust;
        if (prefixIndex%2 == 0){
            if (prefixIndex >= 2){
                long testNextIndex = 1L << (prefixIndex-2);
                long cmp = leftTop.bits ^ rightBottom.bits;
                long test = cmp & testNextIndex;
                if (test > 0){
                    return new Pair<>(leftTop.fromPrefix(prefixIndex),null);
                }
            }
            prefixAdjust =2;
        }else{
            prefixAdjust=1;
        }
        prefixIndex -=prefixAdjust;
        return new Pair<>(leftTop.fromPrefix(prefixIndex),rightBottom.fromPrefix(prefixIndex));
    }

    public static Tuple4List<GeoHash> leastBoundingSliceMerged(final BoundingBox box,final int maxLevel){
        return mergeSlices(leastBoundingSlice(box, maxLevel));
    }

    public static Tuple4List<GeoHash> leastBoundingSlice(final BoundingBox box,final int maxLevel){
        final double centerLat = (box.minLat + box.maxLat) / 2;
        final double centerLon = (box.minLon + box.maxLon) / 2;
        final double width = box.getLongitudeSize();
        final double height = box.getLatitudeSize();
        final double half = Math.max(width,height)/2;
        GeoHash ltg = GeoHash.withBitPrecision(centerLat+half,centerLon-half,maxLevel);
        GeoHash rtg = GeoHash.withBitPrecision(centerLat+half,centerLon+half,maxLevel);
        long diff = rtg.lonBits - ltg.lonBits;
        int k = (int) Math.floor(Math.log(diff)/ LOG2BASE);
        if ((rtg.lonBits >> k) - (ltg.lonBits >> k) > 1){
            ++k;
        }
        k = 2*k;
        ltg = ltg.dropSignificantBits(k);
        rtg = rtg.dropSignificantBits(k);
        GeoHash lbg = ltg.getSouthernNeighbour();
        GeoHash rbg = rtg.getSouthernNeighbour();
        return new Tuple4List<>(ltg,rtg,lbg,rbg);
    }

    public static Tuple4List<GeoHash> mergeSlices(Tuple4List<GeoHash> slices){
        GeoHash ltg,rbg;
        ltg = slices.item1;
        rbg = slices.item4;
        long diffX = ltg.lonBits ^ rbg.lonBits;
        long diffY = ltg.latBits ^ rbg.latBits;
        if (diffX == 1 && diffY == 1){
            //01,01 -> merge into one big slice
            return new Tuple4List<>(ltg.dropSignificantBits(2),null,null,null);
        }
        if (diffY == 1){
            //01,11 -> merge along y-axis
            return new Tuple4List<>(ltg.dropSignificantBits(1),rbg.dropSignificantBits(1),null,null);
        }
        //all other cases cant merge
        return slices;
    }
}
