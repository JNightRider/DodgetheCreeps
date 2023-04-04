package com.mygame;

import com.jme3.asset.AssetManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import jMe3GL2.geometry.jMe3GL2Geometry;
import jMe3GL2.physics.control.RigidBody2D;
import jMe3GL2.scene.control.AnimatedSprite;
import jMe3GL2.scene.shape.Sprite;
import jMe3GL2.util.Converter;
import jMe3GL2.util.jMe3GL2Utils;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Interval;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

public class Player extends RigidBody2D implements ActionListener {
    
    public static final String MOVE_RIGHT = "move_right";
    public static final String MOVE_LEFT  = "move_left";
    public static final String MOVE_UP    = "move_up";
    public static final String MOVE_DOWN  = "move_down";
    
    private final double speed = 4;
    private final Camera cam;
    
    private boolean dead;
    private boolean right;
    private boolean left;
    private boolean up;
    private boolean down;

    public Player(Camera cam) {
        this.cam  = cam;
    }
    
    public static Player newInstancePlayer(AssetManager assetManager, Camera cam) {
        Sprite mesh = new Sprite(1.0F, 1.0F);
        Material mat = jMe3GL2Utils.loadMaterial(assetManager, "Textures/playerGrey_walk1.png");
        mat.setFloat("AlphaDiscardThreshold", 0.0F);
        
        Geometry geo = new Geometry("Player", mesh);
        geo.setMaterial(mat);
        geo.setQueueBucket(RenderQueue.Bucket.Transparent);
        
        AnimatedSprite as = new AnimatedSprite();
        as.addAnimation("walk", new Texture[] {
            jMe3GL2Utils.loadTexture(assetManager, "Textures/playerGrey_walk1.png"),
            jMe3GL2Utils.loadTexture(assetManager, "Textures/playerGrey_walk2.png")
        });
        as.addAnimation("up", new Texture[] {
            jMe3GL2Utils.loadTexture(assetManager, "Textures/playerGrey_up1.png"),
            jMe3GL2Utils.loadTexture(assetManager, "Textures/playerGrey_up2.png")
        });
        as.setSpeed(0.60f);
        
        Player player = new Player(cam);        
        BodyFixture bf = new BodyFixture(jMe3GL2Geometry.createCapsule(0.8, 1));      
        
        player.addFixture(bf);
        player.setMass(MassType.NORMAL);
                
        geo.addControl(player);
        geo.addControl(as);        
        return player;
    }

    @Override
    public void setEnabledPhysicsControl(boolean enabled) {
        super.setEnabledPhysicsControl(enabled);
        spatial.getControl(AnimatedSprite.class).setEnabled(enabled);
    }

    @Override
    public Spatial getUserData() {
        return (Spatial) super.getUserData();
    }
    
    public void start(Vector2 pos) {
        getTransform().setTranslation(pos);
        
        Sprite sprite = (Sprite) ((Geometry) spatial).getMesh();
        sprite.flipH(false);
        sprite.flipV(false);
        
        spatial.setLocalTranslation(Converter.toFloat(pos.x), Converter.toFloat(pos.y), 0);
        spatial.getControl(AnimatedSprite.class).playAnimation("walk", 0.15f);
        
        setEnabledPhysicsControl(false);
    }
    
    public void onPlayerBodyEntere() {
        this.dead = true;
    }
    
    @Override
    protected void controlUpdate(float tpf) {
        if (dead) {
            dead = false;
            spatial.removeFromParent();
            physicsSpace.removeBody(this);
        }
        
        setAngularVelocity(0);
        setLinearVelocity(0, 0);
        getTransform().setRotation(0);
        
        super.controlUpdate(tpf);        
        Vector2 velocity = new Vector2(0, 0);
        if ( right ) {
            velocity.x += 1;
        }
        if ( left ) {
            velocity.x -= 1;
        }
        if ( down ) {
            velocity.y -= 1;
        }
        if ( up ) {
            velocity.y += 1;
        }
        
        if (velocity.getMagnitude() > 0) {
            velocity = velocity.getNormalized().multiply(speed);
            spatial.getControl(AnimatedSprite.class).setEnabled(true);
        } else {
            spatial.getControl(AnimatedSprite.class).setEnabled(false);
        }
        
        Vector2 position = getTransform().getTranslation();
        position = position.add(velocity.multiply(tpf));
        position.x = Interval.clamp(position.x, cam.getFrustumLeft(), cam.getFrustumRight());
        position.y = Interval.clamp(position.y, cam.getFrustumBottom(), cam.getFrustumTop());        
        getTransform().setTranslation(position);
        
        Sprite sprite = (Sprite) ((Geometry) spatial).getMesh();
        if ( velocity.x != 0 ) {
            spatial.getControl(AnimatedSprite.class).playAnimation("walk", 0.15f);            
            sprite.flipV(false);
            
            sprite.flipH(velocity.x < 0);
        } else if ( velocity.y != 0 ) {
            spatial.getControl(AnimatedSprite.class).playAnimation("up", 0.15f);
            sprite.flipV(velocity.y < 0);
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if ( enabledPhysics ) {
            if (MOVE_DOWN.equals(name)) {
                down = isPressed;
            }
            if (MOVE_LEFT.equals(name)) {
                left = isPressed;
            }
            if (MOVE_RIGHT.equals(name)) {
                right = isPressed;
            }
            if (MOVE_UP.equals(name)) {
                up = isPressed;
            }
        }        
    }
}
