package geohashutil.asiainfo.com;

public class GridPoint {
    public final int x;
    public final int y;
    public final int level;

    public GridPoint(int x, int y, byte significantBits) {
        this.x = x;
        this.y = y;
        level = significantBits;
    }

    private double distanceWithSameLevel(int x, int y) {
        return EulerDistance(this.x,this.y,x,y);
    }

    private static double EulerDistance(int x1,int y1,int x2,int y2){
        int x = x1-x2;
        int y = y1-y2;
        int xx = x * x;
        int yy = y * y;
        return Math.sqrt(xx + yy);
    }

    public double distanceFrom(GridPoint other) {
        GridPoint large , small;
        if (other.level == level){
            return distanceWithSameLevel(other.x,other.y);
        }else if (other.level > level){
            large = other;
            small = this;
        }else {
            large = this;
            small = other;
        }
        int diff =  large.level - small.level;
        //TODO diff may not add to final result becaue in real GIS they may be shortcut between levels (jump from level 18 to 10)
        return diff + EulerDistance(large.x >> diff,large.y >> diff,small.x,small.y);
    }
}
