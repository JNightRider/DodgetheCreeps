package com.mygame;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import jMe3GL2.geometry.jMe3GL2Geometry;
import jMe3GL2.physics.control.RigidBody2D;
import jMe3GL2.scene.control.AnimatedSprite;
import jMe3GL2.scene.control.ScaleType;
import jMe3GL2.scene.shape.Sprite;
import jMe3GL2.util.jMe3GL2Utils;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

public class Mob extends RigidBody2D {
    
    private final Camera cam;

    public Mob(Camera cam) {
        this.cam = cam;
    }
    
    public static Mob newInstanceMob(AssetManager assetManager, Camera cam) {
        Sprite mesh = new Sprite(1F, 1F);
        Material mat = jMe3GL2Utils.loadMaterial(assetManager, "Textures/enemyWalking_1.png");
        mat.setFloat("AlphaDiscardThreshold", 0.0F);
        
        Geometry geo = new Geometry("Mob", mesh);
        geo.setMaterial(mat);
        geo.setQueueBucket(RenderQueue.Bucket.Transparent);
        
        AnimatedSprite as = new AnimatedSprite();
        as.addAnimation("fly", new Texture[] {
            jMe3GL2Utils.loadTexture(assetManager, "Textures/enemyFlyingAlt_1.png"),
            jMe3GL2Utils.loadTexture(assetManager, "Textures/enemyFlyingAlt_2.png")
        });
        as.addAnimation("swim", new Texture[] {
            jMe3GL2Utils.loadTexture(assetManager, "Textures/enemySwimming_1.png"),
            jMe3GL2Utils.loadTexture(assetManager, "Textures/enemySwimming_2.png")
        });
        as.addAnimation("walk", new Texture[] {
            jMe3GL2Utils.loadTexture(assetManager, "Textures/enemyWalking_1.png"),
            jMe3GL2Utils.loadTexture(assetManager, "Textures/enemyWalking_2.png")
        });
        
        String[] names = as.getAnimations().toArray(new String[3]);
        int rand = (int) (Math.random() * names.length);
        
        as.setSpeed(0.50f);
        as.setDynamic(true);
        as.setScaleType(ScaleType.GL2_MAX);
        
        Mob mob = new Mob(cam);
        Convex convex;
        switch (rand) {
            case 0:
                convex =  jMe3GL2Geometry.createCapsule(0.66, 1);
                break;
            case 1:
            default:
                convex = jMe3GL2Geometry.createCapsule(1, 0.71);
                break;
        }
        
        BodyFixture bf = new BodyFixture(convex);
        
        mob.addFixture(bf);
        mob.setMass(MassType.INFINITE);
        
        geo.addControl(mob);
        geo.addControl(as);
        
        as.playAnimation(names[rand], 0.15F);
        return mob;
    }
    
    

    @Override
    public Spatial getUserData() {
        return (Spatial) super.getUserData();
    }

    public void onVisibilityNotifierScreenExited() {
        Vector2 pos = getTransform().getTranslation();
        double x = pos.x, y = pos.y;
        
        if (x < (cam.getFrustumLeft() - 1) 
                || x > (cam.getFrustumRight() + 1)) {
            
            physicsSpace.removeBody(this);
            spatial.removeFromParent();
        } else if (y < (cam.getFrustumBottom() - 1) 
                        || y > (cam.getFrustumTop() + 1)) {
            
            physicsSpace.removeBody(this);
            spatial.removeFromParent();
        }
    }
    
    @Override
    protected void controlUpdate(float tpf) {
        super.controlUpdate(tpf);
        onVisibilityNotifierScreenExited();
    }
}
