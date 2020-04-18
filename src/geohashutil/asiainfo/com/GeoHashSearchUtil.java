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
        GeoHash ltg = GeoHash.withBitPrecision(centerLat+halfY,centerLon-halfX,k);
        GeoHash rbg = GeoHash.withBitPrecision(centerLat-halfY,centerLon+halfX,k);
        List<GeoHash> result = new ArrayList<>(9);
        final long startX = ltg.lonBits;
        final long endX = rbg.lonBits;
        final long startY = ltg.latBits;
        final long endY = rbg.latBits;
        for (long x = startX; x <=endX ; ++x) {
            for (long y = endY; y <= startY; ++y) {
                if (x == startX && y == startY){
                    result.add(ltg);
                }else if (x == endX && y == endY){
                    result.add(rbg);
                }else{
                    result.add(GeoHash.recombineLatLonBits(x,y,k));
                }
            }
        }
        return result;
    }

    public static List<GeoHash> leastBoundingSliceMerged(final BoundingBox box){
        return mergeSlices(leastBoundingSlice(box));
    }

    private static List<GeoHash> mergeSlices(List<GeoHash> slices){
        final GeoHash lbg = slices.get(0);
        final GeoHash rtg=slices.get(slices.size());
        for (long x = lbg.lonBits; x <=rtg.lonBits ; ++x) {
            for (long y = lbg.latBits; y <= rtg.latBits; ++y) {
            }
        }
        long diffX = lbg.lonBits ^ rtg.lonBits;
        long diffY = lbg.latBits ^ rtg.latBits;
        if (diffX == 1 && diffY == 1){
            //01,01 -> merge into one big slice
        }
        if (diffY == 1){
            //01,11 -> merge along y-axis
        }
        //all other cases cant merge
        return slices;
    }

}
