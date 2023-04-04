package com.mygame;

import com.jme3.renderer.Camera;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.dyn4j.geometry.Vector2;

public final 
class MobPath {

    
    private final List<GLine> path 
                        = new ArrayList<>();
    private Camera cam;
    
    public MobPath() {
    }
    
    public void setGScreen(Camera cam) {
        this.cam = cam;
    }
    
    
    private void setup() {
        path.clear();
        path.add(new GLine(new Vector2(cam.getFrustumLeft(), cam.getFrustumTop()), 
                           new Vector2(cam.getFrustumRight(), cam.getFrustumTop())));
        
        path.add(new GLine(new Vector2(cam.getFrustumRight(), cam.getFrustumTop()), 
                           new Vector2(cam.getFrustumRight(), cam.getFrustumBottom())));
        
        path.add(new GLine(new Vector2(cam.getFrustumRight(), cam.getFrustumBottom()), 
                           new Vector2(cam.getFrustumLeft(), cam.getFrustumBottom())));
        
        path.add(new GLine(new Vector2(cam.getFrustumLeft(), cam.getFrustumBottom()), 
                           new Vector2(cam.getFrustumLeft(), cam.getFrustumTop())));
    }
    
    public Vector2 randiPath() {
        setup();
        GLine rand = path.get((int) (Math.random() * path.size()));
        Vector2 start = rand.getStart(),
                end   = rand.getEnd();
        
        double x, y;
        if (start.y == end.y) {
            x = ThreadLocalRandom.current().nextDouble(Math.min(start.x, end.x), Math.max(start.x, end.x));
            y = start.y;            
        } else {
            x = start.x;
            y = ThreadLocalRandom.current().nextDouble(Math.min(start.y, end.y), Math.max(start.y, end.y));
        }
        return new Vector2(x, y);
    }
    
    private static 
    final class GLine {
        
        private final Vector2 start;
        private final Vector2 end;

        public GLine(Vector2 start, Vector2 end) {
            this.start = start;
            this.end = end;
        }

        public Vector2 getStart() {
            return start;
        }

        public Vector2 getEnd() {
            return end;
        }
    }
}
