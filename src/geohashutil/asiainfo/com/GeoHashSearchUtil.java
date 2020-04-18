package geohashutil.asiainfo.com;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class GeoHashSearchUtil {
    private static final int MAX_LEVEL = 40;//40位GeoHash精度约为20米
    private static final byte[] maskPosIndex;
    public static final double LOG2BASE = Math.log(2);
    public static final double LOG180D = Math.log(180);

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

    public static List<GeoHash> leastBoundingSlice(final BoundingBox box){
        final double centerLat = (box.minLat + box.maxLat) / 2;
        final double centerLon = (box.minLon + box.maxLon) / 2;
        final double width = box.getLongitudeSize();
        final double height = box.getLatitudeSize();
        final double halfX = width/2;
        final double halfY = height/2;
        final double tx = (LOG180D - Math.log(halfX))/LOG2BASE;
        final int kx = (int) (Math.floor(tx));
        final double ty = (LOG180D - Math.log(halfY))/LOG2BASE;
        final int ky = (int) (Math.floor(ty));
        final int k = Math.min(kx,ky) *2;
        GeoHash lbg = GeoHash.withBitPrecision(centerLat-halfY,centerLon-halfX,k);
        GeoHash rtg = GeoHash.withBitPrecision(centerLat+halfY,centerLon+halfX,k);
        List<GeoHash> result = new ArrayList<>(9);
        final long startX = lbg.lonBits;
        final long startY = lbg.latBits;
        final long endX = rtg.lonBits;
        final long endY = rtg.latBits;
        GeoHash curX = lbg;
        for (long x = startX; x <=endX ; ++x) {
            GeoHash curY = curX;
            if (x < endX)
                curX = curX.getEasternNeighbour();
            for (long y = startY; y <= endY; ++y) {
                if (x == endX && y == endY){
                    result.add(rtg);
                }else {
                    result.add(curY);
                }
                if (y < endY)
                    curY = curY.getNorthernNeighbour();
            }
        }
        return result;
    }

    public static List<GeoHash> leastBoundingSliceMerged(final BoundingBox box){
        return mergeSlices(leastBoundingSlice(box));
    }

    private static List<GeoHash> mergeSlices(List<GeoHash> slices){
        final GeoHash lbg = slices.get(0);
        final GeoHash rtg = slices.get(slices.size()-1);
        final long maxY = rtg.latBits;
        final long minY = lbg.latBits;
        if (maxY - minY == 1){
            if ((maxY ^ minY) == 1){
                //merge along y-axis
            }
        }else if (maxY - minY == 2){
            //test 2 combination
        }
        return slices;
    }

}
