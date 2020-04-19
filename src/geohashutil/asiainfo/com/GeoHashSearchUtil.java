package geohashutil.asiainfo.com;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class GeoHashSearchUtil {
    private static final int MAX_LEVEL = 40;//40位GeoHash精度约为20米
    private static final byte[] maskPosIndex;
    public static final double LOG2BASE = Math.log(2);
    public static final double LOG180D = Math.log(180);
    public static final double LOG90D = Math.log(90);

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
        final double halfX = box.getLongitudeSize() /2;
        final double halfY = box.getLatitudeSize() /2;
        final int kx = (int) (Math.floor((LOG180D - Math.log(halfX))/LOG2BASE +1));
        final int ky = (int) (Math.floor((LOG90D - Math.log(halfY))/LOG2BASE +1));
        final int k;
        if (kx == ky || kx == ky+1){
            k = kx+ky;
        }else if (kx < ky){
            k = kx*2;
        }else {
            // kx > ky +1
            k = ky*2+1;
        }
        GeoHash lbg = GeoHash.withBitPrecision(centerLat-halfY,centerLon-halfX,k);
        GeoHash rtg = GeoHash.withBitPrecision(centerLat+halfY,centerLon+halfX,k);
        int N = (int) (rtg.lonBits - lbg.lonBits + 1);
        int M = (int) (rtg.latBits - lbg.latBits + 1);
        int cap = N * M;
        List<GeoHash> result = new ArrayList<>(cap);
        GeoHash curX = lbg;
        for (int x = 0; x < N ; ++x) {
            GeoHash curY = curX;
            if (x < N -1)
                curX = curX.getEasternNeighbour();
            for (int y = 0; y < M; ++y) {
                if (x == N -1 && y == M -1){
                    result.add(rtg);
                }else {
                    result.add(curY);
                }
                if (y < M -1)
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
        final long minX = lbg.lonBits;
        final long maxX =rtg.lonBits;
        //matrix: (maxX - minX+1) * (maxY - minY+1)
        int N = (int) (maxX - minX + 1);
        int M = (int) (maxY - minY + 1);
        int count = 0;
        GeoHash[][] tmp = new GeoHash[N][];
        for (int i = 0; i < N; i++) {
            GeoHash[] cur = new GeoHash[M];
            tmp[i] = cur;
            for (int j = 0; j < M; j++) {
                cur[j] = slices.get(count);
                ++count;
                if (j>0 && cur[j-1].significantBits == cur[j].significantBits
                        && cur[j].significantBits%2 ==0){
                    long diff = cur[j-1].latBits ^ cur[j].latBits;
                    if (diff == 1){
                        GeoHash merged = cur[j].dropSignificantBits(1);
                        cur[j-1] = cur[j] = merged;
                        if (i>0 && tmp[i-1][j-1].significantBits == cur[j-1].significantBits){
                            diff = tmp[i-1][j-1].lonBits ^ cur[j-1].lonBits;
                            if (diff == 1){
                                merged = cur[j-1].dropSignificantBits(1);

                            }
                        }
                    }
                }
            }
        }
        return slices;
    }

}
