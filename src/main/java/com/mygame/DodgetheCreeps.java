package com.mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioKey;
import com.jme3.audio.AudioNode;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.style.BaseStyles;
import com.simsilica.lemur.style.ElementId;
import jMe3GL2.physics.Dyn4jAppState;
import jMe3GL2.physics.PhysicsSpace;
import jMe3GL2.physics.ThreadingType;
import jMe3GL2.physics.control.AbstractBody;
import jMe3GL2.renderer.Camera2DState;
import jMe3GL2.util.Timer;
import jMe3GL2.util.TimerTask;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.dyn4j.dynamics.TimeStep;
import org.dyn4j.dynamics.contact.ContactConstraint;
import org.dyn4j.geometry.Vector2;
import org.dyn4j.world.PhysicsWorld;
import org.dyn4j.world.World;
import org.dyn4j.world.listener.StepListenerAdapter;
import org.jnightride.jgui.RootPane;
import org.jnightride.jgui.core.Dock;
import org.jnightride.jgui.core.DockControl;
import org.jnightride.jgui.core.DynamicLayout;

/**
 * Dodge the Creeps!
 */
@SuppressWarnings(value = {"unchecked"})
public final class DodgetheCreeps extends SimpleApplication {

    public static void main(String[] args) {
        DodgetheCreeps app = new DodgetheCreeps();
        AppSettings as = new AppSettings(true);
        
        as.setResolution(480, 720);
        as.setGammaCorrection(false);
        as.setTitle("Dodge the Creeps!");
        
        app.setShowSettings(false); //Settings dialog not supported on mac
        app.setSettings(as);
        app.start();
    }
    
    private Dyn4jAppState<AbstractBody> dyn4jAppState;
    private PhysicsSpace<AbstractBody> physicsSpace;
    private Camera2DState camera2DState;
    
    private Player player;
    private MobPath mobPath;
    
    private Container container;
    private Button startButton;
    private Label message;
    private Label scoreLabel;
    
    private int score = 0;
    private boolean game_over = false;
    
    private Timer mobTimer     = new Timer(0.5f);
    private Timer scoreTimer   = new Timer(0.75f);
    private Timer startTimer   = new Timer(1.05f);
    private Timer messageTimer = new Timer(1.0f);
    
    private AudioNode deathSound;
    private AudioNode music;
        
    @Override
    public void simpleInitApp() {
        setDisplayStatView(false);
        setDisplayFps(false);
        
        dyn4jAppState = new Dyn4jAppState<>(ThreadingType.PARALLEL);
        stateManager.attach(dyn4jAppState);
        
        camera2DState = new Camera2DState(5f, 0.01f);
        stateManager.attach(camera2DState);
        
        physicsSpace = dyn4jAppState.getPhysicsSpace();
        
        mobPath = new MobPath();
        mobPath.setGScreen(cam);
        
        mobTimer.addTask(_on_MobTimer_timeout);
        scoreTimer.addTask(_on_ScoreTimer_timeout);
        startTimer.addTask(_on_StartTimer_timeou);
        messageTimer.addTask(_on_MessageTimer_timeout);
        
        music = new AudioNode(assetManager.loadAudio("Sounds/House_In_a_Forest_Loop.ogg"), new AudioKey("Music", true));
        music.setPositional(false);
        music.setDirectional(false);
        music.setLooping(true);
        
        deathSound = new AudioNode(assetManager.loadAudio("Sounds/gameover.wav"), new AudioKey("DeadthSound", false));
        deathSound.setPositional(false);
        deathSound.setDirectional(false);
        deathSound.setLooping(false);
        
        player = Player.newInstancePlayer(assetManager, cam);
        
        setupInputMap(player);
        World<AbstractBody> world = physicsSpace.getPhysicsWorld();
        world.setGravity(0, 0);
        
        world.addStepListener(new StepListenerAdapter<>() {
            @Override
            public void begin(TimeStep step, PhysicsWorld<AbstractBody, ?> world) {
                List<ContactConstraint<AbstractBody>> contacts = world.getContacts(player);
                for (final ContactConstraint<AbstractBody> cc : contacts) {
                    if (isMob(cc.getOtherBody(player))) {                        
                        gameOver();
                    }
                }
            } 
        });
        
        headsUpDisplay();
    }
    
    private boolean isMob(AbstractBody body) {
	Object userData = body.getUserData();
        if (userData != null 
                && (userData instanceof Spatial)) {
            if ("Mob".equals(((Spatial) userData).getName())) {
                return true;
            }
        }
        return false;
    }
    
    private void headsUpDisplay() {
        GuiGlobals.initialize(this);
            
        // Load the 'glass' style
        BaseStyles.loadGlassStyle();
        LemurGuiStyle.loadAppStyle(assetManager);
        
        // Set 'glass' as the default style when not specified
        GuiGlobals.getInstance().getStyles().setDefaultStyle("glass");
        
        final RootPane rootPane = new RootPane(this);
        rootPane.setName("rootPane");
        rootPane.setPreferredSize(new Vector3f(480, 720, 0));
        guiNode.attachChild(rootPane);
        
        container = rootPane.addChild(new Container(new DynamicLayout(rootPane)), false, Dock.Center);
        container.setPreferredSize(rootPane.getPreferredSize().clone());
        container.setBackground(null);
        
        startButton = container.addChild(new Button("Start", new ElementId("StartButton")), true, Dock.CenterBottom);
        startButton.setTextHAlignment(HAlignment.Center);
        startButton.setTextVAlignment(VAlignment.Center);        
        startButton.setFont(assetManager.loadFont("Interface/Fonts/Xolonium.fnt"));
        startButton.getControl(DockControl.class).setPosition(0, 100);
        startButton.getControl(DockControl.class).setFontSize(50);
        startButton.setPreferredSize(new Vector3f(200, 100, 0));
        startButton.addClickCommands((Button source) -> {
            newGame();
        });
                
        message = container.addChild(new Label("Dodge the\nCreeps!"), true, Dock.Center);
        message.setTextHAlignment(HAlignment.Center);
        message.setTextVAlignment(VAlignment.Center);        
        message.setFont(assetManager.loadFont("Interface/Fonts/Xolonium.fnt"));
        message.setColor(new ColorRGBA(1, 1, 1, 1));
        message.getControl(DockControl.class).setFontSize(55);
        message.setPreferredSize(new Vector3f(400, 200, 0));
        
        scoreLabel = container.addChild(new Label("0"), true, Dock.CenterTop);
        scoreLabel.setTextHAlignment(HAlignment.Center);
        scoreLabel.setTextVAlignment(VAlignment.Center);        
        scoreLabel.setFont(assetManager.loadFont("Interface/Fonts/Xolonium.fnt"));
        scoreLabel.setColor(new ColorRGBA(1, 1, 1, 1));
        scoreLabel.getControl(DockControl.class).setPosition(0, 10);
        scoreLabel.getControl(DockControl.class).setFontSize(45);
        scoreLabel.setPreferredSize(new Vector3f(300, 45, 0));
        
        viewPort.setBackgroundColor(new ColorRGBA(0.224f, 0.427f, 0.439f, 1.0f));
    }
        
    private final TimerTask _on_MobTimer_timeout = () -> {
        Mob body = Mob.newInstanceMob(assetManager, cam);
        Spatial geo = body.getUserData();
        
        Vector2 position = mobPath.randiPath();
        
        double direction = position.getDirection() + Math.PI;
        direction += ThreadLocalRandom.current().nextDouble(-Math.PI / 4, Math.PI / 4);   
        
        body.getTransform().rotate(direction);
        body.getTransform().setTranslation(position);
        
        Vector2 velocity = new Vector2(ThreadLocalRandom.current().nextDouble(1.50, 2.50), 0.0);
        body.setLinearVelocity(velocity.rotate(direction));
        
        physicsSpace.addBody(body);
        rootNode.attachChild(geo);
        
        mobTimer.reset();
    };
    
    private final TimerTask _on_ScoreTimer_timeout = () -> {
        score += 1;
        scoreLabel.setText(String.valueOf(score));
        scoreTimer.reset();
    };
    
    private final TimerTask _on_StartTimer_timeou = () -> {
        player.setEnabledPhysicsControl(true);
        container.removeChild(message);
        
        mobTimer.start();
        scoreTimer.start();
        startTimer.stop();
    };
    
    private final TimerTask _on_MessageTimer_timeout = () -> {
        message.setText("Dodge the\nCreeps!");
        scoreLabel.setText("0");
        
        container.addChild(startButton, true, Dock.CenterBottom);
        startButton.getControl(DockControl.class).setPosition(0, 100);
        
        physicsSpace.getPhysicsWorld().removeAllBodies();
        rootNode.detachAllChildren();
        
        messageTimer.stop();
    };

    private void setupInputMap(ActionListener al) {
        inputManager.addMapping(Player.MOVE_DOWN, new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping(Player.MOVE_UP, new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping(Player.MOVE_LEFT,new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping(Player.MOVE_RIGHT, new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addListener(al, new String[] {
            Player.MOVE_DOWN, Player.MOVE_LEFT, Player.MOVE_RIGHT, Player.MOVE_UP
        });
    }
    
    private void newGame() {
        score = 0;
        game_over = false;
        
        physicsSpace.addBody(player);
        rootNode.attachChild(player.getUserData());
        
        player.start(new Vector2(0, -1.5));
        startTimer.start();
        
        message.setText("Get Ready!");
        container.removeChild(startButton);
        
        music.play();
        deathSound.stop();
    }
    
    private void gameOver() {
        player.onPlayerBodyEntere();
        game_over = true;
        mobTimer.stop();
        scoreTimer.stop();
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        mobTimer.update(tpf, 0.60f);
        scoreTimer.update(tpf, 0.60f);
        startTimer.update(tpf, 0.60f);
        messageTimer.update(tpf, 0.60f);
    }

    @Override
    public void simpleRender(RenderManager rm) {
        if (game_over) {
            game_over = false;
            music.stop();
            deathSound.play();
            
            message.setText("Game Over");
            container.addChild(message, true, Dock.Center);
            messageTimer.start();
        }
    }
}
