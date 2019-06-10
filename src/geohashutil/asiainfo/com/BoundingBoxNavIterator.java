package geohashutil.asiainfo.com;

import java.util.Iterator;

public class BoundingBoxNavIterator implements Iterator<GeoHash>{

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public GeoHash next() {
        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
