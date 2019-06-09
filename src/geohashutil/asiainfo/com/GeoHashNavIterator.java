package geohashutil.asiainfo.com;

import java.util.Iterator;

public class GeoHashNavIterator implements Iterator<GeoHashNavIterator.GeoHashNavPair> {
    public class GeoHashNavPair{
        public final GeoHash grid;
        public final int stepFromCenter;

        public GeoHashNavPair(GeoHash grid, int stepFromCenter) {
            this.grid = grid;
            this.stepFromCenter = stepFromCenter;
        }
    }

    public final GeoHash centerGrid;
    private int latStep,lonStep,squareStep;

    public GeoHashNavIterator(GeoHash geoHash) {
        centerGrid=geoHash;
    }

    public void reset(){
        latStep=lonStep=squareStep=0;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public GeoHashNavPair next() {
        GeoHashNavPair r;
        if (squareStep == 0)
        {
            r = new GeoHashNavPair(centerGrid,0);
            squareStep++;
            latStep=lonStep=-squareStep;
        }else{
            r = new GeoHashNavPair(centerGrid.navFromSteps(latStep,lonStep),squareStep);
            if (latStep == -squareStep && lonStep == -squareStep+1){
                squareStep++;
                latStep=lonStep=-squareStep;
            }else if(lonStep == -squareStep && latStep <squareStep){
                latStep++;
            }else if (latStep == squareStep && lonStep < squareStep){
                lonStep++;
            }else if (lonStep == squareStep && latStep>-squareStep){
                latStep--;
            }else if (latStep == -squareStep && lonStep < -squareStep){
                lonStep--;
            }
        }
        return r;
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
