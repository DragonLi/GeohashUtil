package geohashutil.asiainfo.com;

import javafx.util.Pair;

public class GeoHashSearchUtil {
    private static final int MAX_LEVEL = 40;//40位GeoHash精度约为20米
    private static final byte[] maskPosIndex;
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
}
