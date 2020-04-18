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

    public static Sharp9Square leastBoundingSlice(final BoundingBox box, final int maxLevel){
        final double centerLat = (box.minLat + box.maxLat) / 2;
        final double centerLon = (box.minLon + box.maxLon) / 2;
        final double width = box.getLongitudeSize();
        final double height = box.getLatitudeSize();
        final double halfX = width/2;
        final double halfY = height/2;
        double tx = (LOG180D - Math.log(halfX))/LOG2BASE;
        int kx = (int) (Math.floor(tx));
        double ty = (LOG180D - Math.log(halfY))/LOG2BASE;
        int ky = (int) (Math.floor(ty));
        int k = Math.min(kx,ky) *2;
        WGS84Point rt = new WGS84Point(centerLat+halfY,centerLon+halfX);
        WGS84Point lb = new WGS84Point(centerLat-halfY,centerLon-halfX);
        GeoHash p11 = GeoHash.withBitPrecision(centerLat+halfY,centerLon-halfX,k);
        GeoHash             p12 = null, p13 = null,
                p21 = null, p22 = null, p23 = null,
                p31 = null, p32 = null, p33 = null;
        if (!p11.boundingBox.contains(rt)){
            p12 = p11.getEasternNeighbour();
            if (!p12.boundingBox.contains(rt)){
                p13 = p12.getEasternNeighbour();
            }
        }
        if (!p11.boundingBox.contains(lb)){
            p21 = p11.getSouthernNeighbour();
            if (p12 != null)
                p22 = p12.getSouthernNeighbour();
            if (p13 != null)
                p23 = p13.getSouthernNeighbour();
            if (!p21.boundingBox.contains(lb)){
                p31 = p21.getSouthernNeighbour();
                if (p22 != null)
                    p32 = p22.getSouthernNeighbour();
                if (p23 != null)
                    p33 = p23.getSouthernNeighbour();
            }
        }
        return new Sharp9Square(p11,p12,p13,p21,p22,p23,p31,p32,p33);
    }

    public static List<GeoHash> convertToList(Sharp9Square slice){
        ArrayList<GeoHash> sliceLst = new ArrayList<>(9);
        sliceLst.add(slice.p11);
        if (slice.p12 != null) sliceLst.add(slice.p12);
        if (slice.p13 != null) sliceLst.add(slice.p13);
        if (slice.p21 != null) sliceLst.add(slice.p21);
        if (slice.p22 != null) sliceLst.add(slice.p22);
        if (slice.p23 != null) sliceLst.add(slice.p23);
        if (slice.p31 != null) sliceLst.add(slice.p31);
        if (slice.p32 != null) sliceLst.add(slice.p32);
        if (slice.p33 != null) sliceLst.add(slice.p33);
        return sliceLst;
    }
/*
    public static Sharp9Square<GeoHash> leastBoundingSliceMerged(final BoundingBox box, final int maxLevel){
        return mergeSlices(leastBoundingSlice(box, maxLevel));
        return null;
    }

    public static Sharp9Square<GeoHash> mergeSlices(Sharp9Square<GeoHash> slices){
        GeoHash ltg,rbg;
        ltg = slices.item1;
        rbg = slices.item4;
        long diffX = ltg.lonBits ^ rbg.lonBits;
        long diffY = ltg.latBits ^ rbg.latBits;
        if (diffX == 1 && diffY == 1){
            //01,01 -> merge into one big slice
            return new Sharp9Square<>(ltg.dropSignificantBits(2),null,null,null);
        }
        if (diffY == 1){
            //01,11 -> merge along y-axis
            return new Sharp9Square<>(ltg.dropSignificantBits(1),rbg.dropSignificantBits(1),null,null);
        }
        //all other cases cant merge
        return slices;
    }
*/
}
