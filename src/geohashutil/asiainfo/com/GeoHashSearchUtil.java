package geohashutil.asiainfo.com;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class GeoHashSearchUtil {
    private static final int MAX_LEVEL = 40;//40位GeoHash精度约为20米
//    private static final byte[] maskPosIndex;
    public static final double LOG2BASE = Math.log(2);
    public static final double LOG180D = Math.log(180);
    public static final double LOG360D = Math.log(360);

    /*
    static {
        maskPosIndex = new byte[256];
        byte c = 0;//最右面的位置是0
        int carrier=1;//下一个进位的位置
        for (int i = 0; i<256; i++) {
            if (i == carrier)//到达更高一个bit的位置，相当于进位了
            {
                ++c;//到达进位位置，最高的1向左移动一位，因此加一
                carrier <<=1;
            }
            maskPosIndex[i]=c;
        }
    }
    */

    public static byte comparePrefix(long a, long b){
        long c = a ^ b;
        return (byte) (64-Long.numberOfLeadingZeros(c));
        /*
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
        */
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

    public static List<GeoHash> leastBoundingSlice(final BoundingBox box,final int maxLen){
        final double centerLat = (box.minLat + box.maxLat) / 2;
        final double centerLon = (box.minLon + box.maxLon) / 2;
        final double halfX = box.getLongitudeSize() /2;
        final double halfY = box.getLatitudeSize() /2;
        final int kx = (int) (Math.floor((LOG360D - Math.log(halfX))/LOG2BASE));
        final int ky = (int) (Math.floor((LOG180D - Math.log(halfY))/LOG2BASE));
        final int k;
        if (kx == ky || kx == ky+1){
            k = Math.min(maxLen,kx+ky);
        }else if (kx < ky){
            k = Math.min(maxLen,kx*2);
        }else {
            // kx > ky +1
            k = Math.min(maxLen,ky*2+1);
        }
        GeoHash lbg = GeoHash.withBitPrecision(centerLat-halfY,centerLon-halfX,k);
        GeoHash rtg = GeoHash.withBitPrecision(centerLat+halfY,centerLon+halfX,k);
        //matrix: (maxX - minX+1) * (maxY - minY+1)
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

    public static List<GeoHash> leastBoundingSliceMerged(final BoundingBox box,final int maxLen){
        return mergeSlices(leastBoundingSlice(box,maxLen));
    }

    private static List<GeoHash> mergeSlices(List<GeoHash> slices){
        final GeoHash lbg = slices.get(0);
        final GeoHash rtg = slices.get(slices.size()-1);
        final long maxY = rtg.latBits;
        final long minY = lbg.latBits;
        final long minX = lbg.lonBits;
        final long maxX =rtg.lonBits;
        final int N = (int) (maxX - minX + 1);
        final int M = (int) (maxY - minY + 1);
        final boolean isEvenLen = lbg.significantBits % 2 == 0;

        int mergedCount = 0;
        //matrix: (maxX - minX+1) * (maxY - minY+1)
        GeoHash[][] tmp;
        if (isEvenLen){
            tmp = new GeoHash[M][];
            for (int y = 0; y < M; ++y) {
                GeoHash[] row = new GeoHash[N];
                tmp[y] = row;
                for (int x = 0,count=y; x < N; ++x,count+=M) {
                    row[x] = slices.get(count);//count == x*M+y
                }
            }
            for (int y = 1; y < M; y++) {
                GeoHash[] row = tmp[y];
                GeoHash[] underRow = tmp[y - 1];
                for (int x = 0; x < N; ++x) {
                    //check merged along y-axis when geohash length is even
                    if (canMergedAlongYAxis(underRow[x],row[x])){
                        underRow[x]=row[x]=row[x].dropSignificantBits(1);
                        ++mergedCount;
                        //check merged along x-axis when y-axis is merged
                        if (x>0 && canMergedAlongXAxis(row[x-1],row[x])){
                            GeoHash merged = row[x].dropSignificantBits(1);
                            row[x-1]=row[x]=merged;
                            underRow[x-1]= underRow[x]=merged;
                            ++mergedCount;
                        }
                    }
                }
            }
        }else{
            tmp = new GeoHash[N][];
            for (int x = 0,count=0; x < N; ++x) {
                GeoHash[] col = new GeoHash[M];
                tmp[x] = col;
                for (int y = 0; y < M; ++y,++count) {
                    col[y] = slices.get(count);
                }
            }
            for (int x = 1; x < N; ++x) {
                GeoHash[] col = tmp[x];
                GeoHash[] leftCol = tmp[x-1];
                for (int y = 0; y < M; ++y) {
                    //check merged along x-axis when geohash length is odd
                    if (canMergedAlongXAxis(leftCol[y],col[y])){
                        leftCol[y]=col[y]=col[y].dropSignificantBits(1);
                        ++mergedCount;
                        //check merged along y-axis when x-axis is merged
                        if (y>0 && canMergedAlongYAxis(col[y-1],col[y])){
                            GeoHash merged = col[y].dropSignificantBits(1);
                            col[y-1]=col[y]=merged;
                            leftCol[y-1]= leftCol[y]=merged;
                            ++mergedCount;
                        }
                    }
                }
            }
        }
        List<GeoHash> mergedSlices = removeDuplicates(tmp,slices.size() - mergedCount);
//        if (mergedSlices.size() + mergedCount != slices.size()){
//            throw new RuntimeException("bug");
//        }
        return mergedSlices;
    }

    private static List<GeoHash> removeDuplicates(GeoHash[][] tmp, int cap) {
        List<GeoHash> result = new ArrayList<>(cap);
        for (GeoHash[] r : tmp) {
            for (GeoHash item : r) {
                if (!result.contains(item)){
                    result.add(item);
                }
            }
        }
        return result;
    }

    private static boolean canMergedAlongXAxis(GeoHash a, GeoHash b){
        return a.significantBits == b.significantBits && (a.lonBits ^ b.lonBits) == 1;
    }

    private static boolean canMergedAlongYAxis(GeoHash a, GeoHash b){
        return a.significantBits == b.significantBits && (a.latBits ^ b.latBits) == 1;
    }
}
