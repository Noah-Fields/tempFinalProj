package a3;

import tage.*;
import tage.Light.LightType;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.*;
import tage.nodeControllers.BobController;
import tage.nodeControllers.OrbitController;
import tage.nodeControllers.RotationController;

import tage.audio.*;
import com.jogamp.openal.ALFactory;

import java.lang.Math;
import java.util.ArrayList;
import java.util.Random;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.*;
import java.util.UUID;
import java.net.InetAddress;

import javax.swing.*;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.joml.*;

import java.net.UnknownHostException;
import java.rmi.server.UID;
import java.sql.Struct;

import net.java.games.input.*;
import net.java.games.input.Component.Identifier.*;
import tage.networking.IGameConnection.ProtocolType;

import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.physics.PhysicsEngineFactory;
import tage.physics.JBullet.*;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.collision.dispatch.CollisionObject;

public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;
	private InputManager im;
	private GhostManager gm;

	private File initFile;
	private long fileLastModifiedTime = 0;
	ScriptEngine jsEngine;
	ScriptEngineManager factory;
	java.util.List<ScriptEngineFactory> list;

	private IAudioManager audioMgr;
	private Sound throwSound, boostSound, bgSound, bounceSound;

	private double startTime, prevTime, elapsedTime, boostStartTime, updateSentTime, timeLastMoved;
	private int elapsTimeSec, winWidth, winHeight;
	private boolean texSelected = false;

	private ArrayList<GhostDummy> dummyList;
	private ArrayList<GameObject> boostList;
	private GameObject boostUsed;
	private ArrayList<BallObject> ballList;
	private int ballIndex = 0;
	private GameObject ground, avatar, spear, x, y, z;
	private ArrayList<GhostAvatar> ghostAvatars;
	private GhostNPC ghostNPC = null;
	private AnimatedShape avatarS, ghostS;
	private ObjShape ballS, boostS, spearS, dummyS, groundS, linxS, linyS, linzS, npcS;
	private TextureImage balltx, boosttx, groundtx, avatarBluetx, avatarRedtx, speartx, ghosttx, npctx, groundHeightMap;
	private Light light1, light2;
	private int forest;

	private PhysicsEngine physicsEngine;
	private PhysicsObject groundP, avatarP, spearP;
	private ArrayList<PhysicsObject> ballPList;
	private ArrayList<PhysicsObject> dummyPList;
	private ArrayList<PhysicsObject> objsSetForRemoval;
	private float[] gravity = new float[3];
	private boolean running = true;
	private float vals[] = new float[16];
	private boolean jumping = false;
	private boolean moving = false;
	private boolean spearReady = true;
	private boolean swapCooldown = false;
	private boolean hitCooldown = false;

	private int avatarID;
	private int spearID;
	private ArrayList<UID> ballIDList;
	private ArrayList<PhysicsObject> ghostPList;

	private CameraOrbitController camOrbitController;
	private NodeController rc, bc, oc;

	private int numJumps = 3;
	private int health = 10;
	private int score = 0;
	private int numBalls = 20;
	private int numBoosts = 5;
	private Random rand = new Random();
	private boolean boost = false;
	private ArrayList<BallObject> inventoryBalls;
	private int inventorySize = 0;
	private double ballLastThrown = 0; //used as cooldown for throwing a ball
	private double spearLastThrown = 0;
	private double lastSwapTime = 0;
	private double lastTimeHit = 0;

	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false;

	private class ShowAxesAction extends AbstractInputAction {
		public ShowAxesAction() {}
		private boolean axesShown = false;

		@Override
		public void performAction(float time, Event e)
		{	
			if(axesShown) {
				x.getRenderStates().disableRendering();
				y.getRenderStates().disableRendering();
				z.getRenderStates().disableRendering();
			}
			else {
				x.getRenderStates().enableRendering();
				y.getRenderStates().enableRendering();
				z.getRenderStates().enableRendering();
			}
			axesShown = !axesShown;
		}
	}

	private class UID {
		public Integer id;

		public UID(int num) { id = num; }
	}

	public MyGame(String serverAddress, int serverPort, String protocol)
	{	super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		if (protocol.toUpperCase().compareTo("TCP") == 0)
			this.serverProtocol = ProtocolType.TCP;
		else
			this.serverProtocol = ProtocolType.UDP;
	}

	public static void main(String[] args)
	{	MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void loadShapes()
	{	ballS = new ImportedModel("Ball.obj");
		spearS = new ImportedModel("Spear.obj");
		boostS = new ManualDiamond();
		groundS = new TerrainPlane();
		dummyS = new ImportedModel("Avatar.obj");
		avatarS = new AnimatedShape("Avatar.rkm", "Avatar.rks");
		avatarS.loadAnimation("WALK", "walk.rka");
		avatarS.loadAnimation("THROW", "throw.rka");
		ghostS = new AnimatedShape("ghostAvatar.rkm", "ghostAvatar.rks");
		ghostS.loadAnimation("gWALK", "ghostWalk.rka");
		ghostS.loadAnimation("gTHROW", "ghostThrow.rka");
		linxS = new Line(new Vector3f(0f,1f,0f), new Vector3f(3f,1f,0f));
		linyS = new Line(new Vector3f(0f,1f,0f), new Vector3f(0f,4f,0f));
		linzS = new Line(new Vector3f(0f,1f,0f), new Vector3f(0f,1f,3f));
		npcS = new Cube();
	}

	@Override
	public void loadTextures()
	{	avatarBluetx = new TextureImage("AvatarBlue.png");
		avatarRedtx = new TextureImage("AvatarRed.png");
		speartx = new TextureImage("Spear.png");
		ghosttx = new TextureImage("AvatarRed.png");
		groundtx = new TextureImage("GroundTx.jpg");
		balltx = new TextureImage("Ball.png");
		boosttx = new TextureImage("BoostTx.jpg");
		groundHeightMap = new TextureImage("heightMap.png");
		npctx = new TextureImage("BoostTx.jpg");
	}

	@Override
	public void loadSkyBoxes()
	{
		forest = (engine.getSceneGraph()).loadCubeMap("Forest");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(forest);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialRotation, initialScale;

		// build spear
		spear = new GameObject(GameObject.root(), spearS, speartx);
		initialTranslation = (new Matrix4f()).translation(0f,-2f,0f);
		spear.setLocalTranslation(initialTranslation);
		initialRotation = (new Matrix4f()).rotation((float)(Math.PI/2), new Vector3f(1f,0f,0f));
		spear.setLocalRotation(initialRotation);

		// build avatar
		avatar = new GameObject(GameObject.root(), avatarS, avatarBluetx);
		initialTranslation = (new Matrix4f()).translation(-1f,1.2f,1f);
		avatar.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(0.6f, 0.6f, 0.6f);
		avatar.setLocalScale(initialScale);

		// build ground plane along XZ plane
		ground = new GameObject(GameObject.root(), groundS, groundtx);
		initialTranslation = (new Matrix4f()).translation(0,0,0);
		ground.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(150f, 15f, 150f);
		ground.setLocalScale(initialScale);
		ground.setHeightMap(groundHeightMap);
		ground.setIsTerrain(true);

		// build prizes and boosts
		ballList = new ArrayList<BallObject>();
		ballPList = new ArrayList<PhysicsObject>();
		boostList = new ArrayList<GameObject>();
		for(int i = 0; i < numBoosts; i++) {
			GameObject tempO = new GameObject(GameObject.root(), boostS, boosttx);
			initialTranslation = (new Matrix4f()).translation(((rand.nextFloat() + 1.0f) * ((float)(rand.nextInt(20) - 10))),
															  (1.0f),
															  ((rand.nextFloat() + 1.0f) * ((float)(rand.nextInt(20) - 10))));
			tempO.setLocalTranslation(initialTranslation);
			initialScale = (new Matrix4f()).scaling(0.1f);
			tempO.setLocalScale(initialScale);
			boostList.add(tempO);
		}
		for(int i = 0; i < numBalls; i++) {
			ballList.add(new BallObject(GameObject.root(), ballS, balltx));
		}
		for( BallObject o : ballList ) {
			initialTranslation = (new Matrix4f()).translation(0, -5f, 0);
			o.setLocalTranslation(initialTranslation);
			initialScale = (new Matrix4f()).scaling(0.3f);
			o.setLocalScale(initialScale);
		}

		inventoryBalls = new ArrayList<BallObject>();
		for(int i = 0; i < 5; i++) {
			BallObject tempB = new BallObject(avatar, ballS, balltx);
			tempB.setId(i);
			initialTranslation = (new Matrix4f()).translation(0, 1.0f, 0);
			tempB.setLocalTranslation(initialTranslation);
			initialScale = (new Matrix4f()).scaling(0.1f);
			tempB.setLocalScale(initialScale);
			inventoryBalls.add(tempB);
			inventorySize++;
		}

		// add X,Y,-Z axes
		x = new GameObject(GameObject.root(), linxS);
		y = new GameObject(GameObject.root(), linyS);
		z = new GameObject(GameObject.root(), linzS);
		(x.getRenderStates()).setColor(new Vector3f(1f,0f,0f));
		(y.getRenderStates()).setColor(new Vector3f(0f,1f,0f));
		(z.getRenderStates()).setColor(new Vector3f(0f,0f,1f));
	}

	@Override
	public void createViewports()
	{	(engine.getRenderSystem()).addViewport("MAIN",0,0,1f,1f);
		
		Viewport mainVp = (engine.getRenderSystem()).getViewport("MAIN");
		Camera mainCamera = mainVp.getCamera();

		mainCamera.setLocation(new Vector3f(-2,0,2));
		mainCamera.setU(new Vector3f(1,0,0));
		mainCamera.setV(new Vector3f(0,1,0));
		mainCamera.setN(new Vector3f(0,0,-1));
	}

	@Override
	public void initializeGame()
	{	setupNetworking();
		initAudio();
		prevTime = System.currentTimeMillis();
		startTime = System.currentTimeMillis();

		dummyList = protClient.getDummies();

		//----------------- adding light -----------------
		Light.setGlobalAmbient(.5f, .5f, .5f);
		
		light1 = new Light();
		light1.setLocation(new Vector3f(0f, 25f, 0f));
		(engine.getSceneGraph()).addLight(light1);

		light2 = new Light();
		light2.setLocation(new Vector3f(0f, 5f, 0f));
		light2.setAmbient(0.2f, 0.2f, 0.2f);
		light2.setDiffuse(0f, 0f, 0f);
		light2.setSpecular(0.2f, 0.2f, 0.2f);
		light2.setType(LightType.SPOTLIGHT);
		light2.setDirection(new Vector3f(avatar.getLocalLocation().x() - light2.getLocationVector().x(),
										 avatar.getLocalLocation().y() - light2.getLocationVector().y(),
										 avatar.getLocalLocation().z() - light2.getLocationVector().z()));
		(engine.getSceneGraph()).addLight(light2);
		
		//--------------------start script------------------

		factory = new ScriptEngineManager();
		list = factory.getEngineFactories();
		jsEngine = factory.getEngineByName("js");

		initFile = new File("assets/scripts/InitParams.js");
		this.runScript(initFile);

		winWidth = (int)(jsEngine.get("winWidth"));
		winHeight = (int)(jsEngine.get("winHeight"));
		(engine.getRenderSystem()).setWindowDimensions(winWidth, winHeight);
		rc = new RotationController(engine, new Vector3f(0,1,0), ((Double)(jsEngine.get("spinSpeed"))).floatValue());
		for( GameObject o : ballList ) {
			rc.addTarget(o);
		}
		rc.addTarget(spear);

		oc = new OrbitController(engine, ((Double)(jsEngine.get("radius"))).floatValue(), inventoryBalls.size());
		for( BallObject o : inventoryBalls ) {
			rc.addTarget(o);
			oc.addTarget(o);
		}
		(engine.getSceneGraph()).addNodeController(oc);
		(engine.getSceneGraph()).addNodeController(rc);
		oc.toggle();
		rc.toggle();

		//------------- PHYSICS --------------

		//     --- initialize physics system ---
		String physEngine = "tage.physics.JBullet.JBulletPhysicsEngine";
		gravity[0] = ((Double)(jsEngine.get("xGrav"))).floatValue();
		gravity[1] = ((Double)(jsEngine.get("yGrav"))).floatValue();
		gravity[2] = ((Double)(jsEngine.get("zGrav"))).floatValue();
		physicsEngine = PhysicsEngineFactory.createPhysicsEngine(physEngine);
		physicsEngine.initSystem();
		physicsEngine.setGravity(gravity);

		float mass = 1.0f;
		float up[] = {0,1,0};
		float size[] = {1.0f, 1.0f, 1.0f};
		float friction = 0.2f;
		float damping = 0.2f;
		double[] tempTransform;
		ballIDList = new ArrayList<UID>();
		ghostPList = new ArrayList<PhysicsObject>();
		dummyPList = new ArrayList<PhysicsObject>();
		objsSetForRemoval = new ArrayList<PhysicsObject>();
		/*
		for( GameObject o: ballList ) {
			tempTransform = toDoubleArray((o.getLocalTranslation()).get(vals));
			tempP = physicsEngine.addSphereObject(physicsEngine.nextUID(), mass, tempTransform, 0.75f);
			tempP.setBounciness(1.0f);
			tempP.setFriction(friction);
			tempP.setDamping(damping, damping);
			o.setPhysicsObject(tempP);
			ballPList.add(tempP);
			ballIDList.add(tempP.getUID());
		}
		*/

		health = ((int)(jsEngine.get("health")));
		float avMass = ((Double)(jsEngine.get("avMass"))).floatValue();
		tempTransform = toDoubleArray((avatar.getLocalTranslation()).get(vals));
		avatarP = physicsEngine.addCylinderObject(physicsEngine.nextUID(), avMass, tempTransform, size);
		avatarP.setBounciness(0.5f);
		avatarP.setFriction(0);
		avatarP.setDamping(0, 0);
		avatar.setPhysicsObject(avatarP);
		avatarID = avatar.getPhysicsObject().getUID();

		float groundBounciness = ((Double)(jsEngine.get("groundBounciness"))).floatValue();
		tempTransform = toDoubleArray((ground.getLocalTranslation()).get(vals));
		groundP = physicsEngine.addStaticPlaneObject(physicsEngine.nextUID(), tempTransform, up, 0.0f);
		groundP.setBounciness(groundBounciness);
		groundP.setFriction(friction);
		groundP.setDamping(damping, damping);
		ground.setPhysicsObject(groundP);

		for( GhostDummy g: dummyList ) {
			dummyPList.add(g.getPhysicsObject());
		}
		//-----------initialize obj locs, rots, etc.--------
		Matrix4f initialTranslation, initialRotation, initialScale;

		// init avatar loc and scale
		initialTranslation = (new Matrix4f()).translation(((Double)(jsEngine.get("initAvX"))).floatValue(), 
														  ((Double)(jsEngine.get("initAvY"))).floatValue(), 
														  ((Double)(jsEngine.get("initAvZ"))).floatValue());
		avatar.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(((Double)(jsEngine.get("initAvScaleX"))).floatValue(), 
												((Double)(jsEngine.get("initAvScaleY"))).floatValue(), 
												((Double)(jsEngine.get("initAvScaleZ"))).floatValue());
		avatar.setLocalScale(initialScale);

		// init ground loc and scale
		initialTranslation = (new Matrix4f()).translation(((Double)(jsEngine.get("initGroundX"))).floatValue(), 
														  ((Double)(jsEngine.get("initGroundY"))).floatValue(), 
														  ((Double)(jsEngine.get("initGroundZ"))).floatValue());
		ground.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(((Double)(jsEngine.get("initGroundScaleX"))).floatValue(), 
												((Double)(jsEngine.get("initGroundScaleY"))).floatValue(), 
												((Double)(jsEngine.get("initGroundScaleZ"))).floatValue());
		ground.setLocalScale(initialScale);

		//------------------initialize HUDS-----------------
		String hudName = "TIMER";
		String dispStr = "Time = 0";
		Vector3f hudColor = new Vector3f(1,0,0);
		(engine.getHUDmanager()).addHUD(hudName, dispStr, hudColor, 15, 15);

		hudName = "HEALTH";
		dispStr = "Health = 5";
		hudColor = new Vector3f(0,1,0);
		(engine.getHUDmanager()).addHUD(hudName, dispStr, hudColor, (int)(winWidth*0.5), 15);

		hudName = "BOOST";
		dispStr = "Boost = Empty";
		hudColor = new Vector3f(0.5f,0.4f,0.3f);
		(engine.getHUDmanager()).addHUD(hudName, dispStr, hudColor, (int)(winWidth*0.5), 45);

		(engine.getHUDmanager()).addHUD("TEX SELECT", "Click Left Arrow for Red & Right Arrow for Blue", new Vector3f(1,0,0), 600, 400);

		// ----------------- initialize camera ----------------
		im = engine.getInputManager();

		Camera c = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		if(im.getFirstGamepadName() != null)
			camOrbitController = new CameraOrbitController(c, avatar, im.getFirstGamepadName(), engine);
		else {
			System.out.println("\nPLUG IN GAMEPAD FOR FULL FUNCTIONALITY\nPLUG IN GAMEPAD FOR FULL FUNCTIONALITY\nPLUG IN GAMEPAD FOR FULL FUNCTIONALITY\n");
			System.out.println("\nGAME REQUIRES GAMEPAD FOR PROPER FUNCTIONALITY\nGAME REQUIRES GAMEPAD FOR PROPER FUNCTIONALITY\nGAME REQUIRES GAMEPAD FOR PROPER FUNCTIONALITY\n");
		}

		// ----------------- OTHER INPUTS SECTION -----------------------------
		FwdAction fwdAction = new FwdAction(this, protClient, avatarS);
		StrafeAction strafeAction = new StrafeAction(this, protClient, avatarS);
		TurnAction turnAction = new TurnAction(this, protClient);
		JumpAction jumpAction = new JumpAction(this, protClient);
		ThrowAction throwAction = new ThrowAction(this, protClient, throwSound, avatarS);
		SwapAction swapAction = new SwapAction(this, protClient, throwSound, avatarS);
		ShowAxesAction showAxesAction = new ShowAxesAction();

		// attach the action objects to keyboard and gamepad components
		ArrayList<Controller> controllers = im.getControllers();
		for (Controller contr : controllers) {
			if (contr.getType() == Controller.Type.GAMEPAD) {
				im.associateAction(contr, net.java.games.input.Component.Identifier.Axis.Y,
								   fwdAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

				im.associateAction(contr, net.java.games.input.Component.Identifier.Axis.X,
								   strafeAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

				im.associateAction(contr, net.java.games.input.Component.Identifier.Axis.RX,
								   turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

				im.associateAction(contr, net.java.games.input.Component.Identifier.Button._0,
								   jumpAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
				
				im.associateAction(contr, net.java.games.input.Component.Identifier.Button._3,
								   swapAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

				im.associateAction(contr, net.java.games.input.Component.Identifier.Button._2,
								   throwAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

				im.associateAction(contr, net.java.games.input.Component.Identifier.Button._6,
								   showAxesAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
			}
			else if (contr.getType() == net.java.games.input.Controller.Type.KEYBOARD) {
				im.associateAction(contr, net.java.games.input.Component.Identifier.Key.W,
								   fwdAction,InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

				im.associateAction(contr, net.java.games.input.Component.Identifier.Key.S,
								   fwdAction,InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				
				//im.associateAction(contr, net.java.games.input.Component.Identifier.Key.A,
				//				   turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

				//im.associateAction(contr, net.java.games.input.Component.Identifier.Key.D,
				//				   turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
			}
		}
		bgSound.play();
	}
	
	public GameObject getAvatar() { return avatar; }
	public TextureImage getRedTex() { return avatarRedtx; }
	public TextureImage getBlueTex() { return avatarBluetx; }
	public void getGhostAvatars(ArrayList<GhostAvatar> ghAvs) { ghostAvatars = ghAvs; }
	public void addGhostP(PhysicsObject p) { ghostPList.add(p); }
	public void removeGhostP(PhysicsObject p) { ghostPList.remove(p); }
	public ObjShape getNPCshape() { return npcS; }
	public TextureImage getNPCtexture() { return npctx; }
	public ObjShape getDummyShape() { return dummyS; }
	public TextureImage getDummyTexture() {
		if(rand.nextInt(2) == 1)
			return avatarBluetx;
		else
			return avatarRedtx;
	}
	public int getElapsedTime() { return elapsTimeSec; }
	public double getElapsedTimeMilli() { return System.currentTimeMillis() - startTime; }
	public boolean checkBallCooldown() { 
		if(getElapsedTimeMilli() - 500.0f > ballLastThrown)
			return false;
		 else 
		 	return true;
	}
	public boolean getBoost() { return boost; }
	public int getNumBalls() { return numBalls; }
	public PhysicsEngine getPhysEngine() { return physicsEngine; }
	public void setJumping(boolean jmp) { if(numJumps > 0) {jumping = jmp; numJumps--;} }
	public int getJumps() { return numJumps; }
	public void setMoving(boolean mv) { moving = mv; timeLastMoved = getElapsedTimeMilli(); }
	public boolean getMoving() { return moving; }
	public int getIndex() { return ballIndex; }
	public void throwBall(Vector3f pos, float[] vel) {
		BallObject tempB = ballList.get(ballIndex);

		tempB.getRenderStates().enableRendering();
		tempB.setSpawnTime(getElapsedTimeMilli());
		ballLastThrown = getElapsedTimeMilli();
		tempB.setLocalTranslation((new Matrix4f()).translation(pos.x(), pos.y(), pos.z()));
		tempB.setLocalScale((new Matrix4f()).scaling(0.3f));

		float mass = 1.0f;
		float friction = 0.2f;
		float damping = 0.2f;
		
		double[] tempTransform;
		tempTransform = toDoubleArray((tempB.getLocalTranslation()).get(vals));
		
		PhysicsObject tempP = physicsEngine.addSphereObject(physicsEngine.nextUID(), mass, tempTransform, 0.50f);
		float ballBounciness = ((Double)(jsEngine.get("ballBounciness"))).floatValue();
		tempP.setBounciness(ballBounciness);
		tempP.setFriction(friction);
		tempP.setDamping(damping, damping);
		tempB.setPhysicsObject(tempP);
		
		tempP.setLinearVelocity(vel);
		ballIDList.add(new UID(tempP.getUID()));
		ballPList.add(tempP);	System.out.println("ballPList Size: " + ballPList.size());
		ballIndex = (ballIndex + 1) % getNumBalls();
		throwSound.setLocation(pos);
		throwSound.play();
	}
	public void throwSpear(Vector3f pos, float[] vel) {
		spearReady = false;
		spear.getRenderStates().enableRendering();
		spearLastThrown = getElapsedTimeMilli();
		spear.setLocalTranslation((new Matrix4f()).translation(pos.x(), pos.y(), pos.z()));
		
		Matrix4f newRot = avatar.getLocalRotation();
		newRot.rotation((float)(-Math.PI/2), avatar.getLocalRightVector());
		spear.setLocalRotation(newRot);
		
		float mass = 0.5f;
		float friction = 0.2f;
		float damping = 0.2f;
		float[] size = {1f,1f,1f};
		
		double[] tempTransform;
		tempTransform = toDoubleArray((spear.getLocalTranslation()).get(vals));
		
		spearP = physicsEngine.addCylinderXObject(physicsEngine.nextUID(), mass, tempTransform, size);
		spearP.setBounciness(0f);
		spearP.setFriction(friction);
		spearP.setDamping(damping, damping);
		spearID = spearP.getUID();
		spear.setPhysicsObject(spearP);

		spearP.setLinearVelocity(vel);
		throwSound.setLocation(pos);
		throwSound.play();
	}
	public boolean spearReady() { return spearReady; }
	public int getInv() { return inventorySize; }
	public void setInv(int num) { inventorySize = num; }
	public void decInv() {
		if(inventorySize > 0) {
			BallObject tempB = inventoryBalls.get(inventorySize - 1);
			tempB.getRenderStates().disableRendering();
			tempB.setUsedTime(getElapsedTimeMilli());
			
			inventorySize--;
		}
	}
	public void createGhostNPC(GhostNPC gNPC) {
		ghostNPC = gNPC;
	}

	
	@Override
	public void update()
	{	if(!texSelected) {
			(engine.getHUDmanager()).setHUD("TEX SELECT", "Click Left Arrow for Red & Right Arrow for Blue", new Vector3f(1,0,0), 600, 400);
			prevTime = System.currentTimeMillis();
			return;
		}
		elapsedTime = System.currentTimeMillis() - prevTime;
		prevTime = System.currentTimeMillis();
		winWidth = (engine.getRenderSystem()).getGLCanvas().getWidth();
		winHeight = (engine.getRenderSystem()).getGLCanvas().getHeight();

		light2.setDirection(new Vector3f(avatar.getLocalLocation().x() - light2.getLocationVector().x(),
										 avatar.getLocalLocation().y() - light2.getLocationVector().y(),
										 avatar.getLocalLocation().z() - light2.getLocationVector().z()));

		this.runScript(initFile);
		running = (boolean)(jsEngine.get("running"));
		gravity[0] = ((Double)(jsEngine.get("xGrav"))).floatValue();
		gravity[1] = ((Double)(jsEngine.get("yGrav"))).floatValue();
		gravity[2] = ((Double)(jsEngine.get("zGrav"))).floatValue();
		physicsEngine.setGravity(gravity);

		//update height of avatar, prizes, and boosts based on terrain height
		Vector3f loc = avatar.getLocalLocation();
		Vector3f terrainLoc = ground.getLocalLocation();
		float height = ground.getHeight(loc.x(), loc.z());
		avatar.setLocalLocation(new Vector3f(loc.x(), height + 1.4f, loc.z()));

		dummyList = protClient.getDummies();
		for( GhostDummy g: dummyList ) {
			if(!dummyPList.contains(g.getPhysicsObject()))
				dummyPList.add(g.getPhysicsObject());
		}

		// build and set HUDs
		elapsTimeSec = Math.round((float)(System.currentTimeMillis()-startTime)/1000.0f);
		String elapsTimeStr = Integer.toString(elapsTimeSec);

		String hudName = "TIMER";
		String dispStr = "Time = " + elapsTimeStr;
		Vector3f hudColor = new Vector3f(1,0,0);
		(engine.getHUDmanager()).setHUD(hudName, dispStr, hudColor, 15, 15);

		hudName = "HEALTH";
		dispStr = "Health = " + health;
		hudColor = new Vector3f(0,1,0);
		(engine.getHUDmanager()).setHUD(hudName, dispStr, hudColor, (int)(winWidth*0.5), 15);

		// check if boost should still be active and create new boost if current one has ended
		if(elapsTimeSec - 8 > boostStartTime && boost) {
			light2.setDiffuse(0f, 0f, 0f);
			boost = !boost;
			boostUsed.getRenderStates().disableRendering();
			boostUsed = null;
			GameObject temp = new GameObject(GameObject.root(), boostS, boosttx);
			temp.setLocalTranslation((new Matrix4f()).translation(((rand.nextFloat() + 1.0f) * ((float)(rand.nextInt(20) - 10))),
															  	  (0.5f),
															  	  ((rand.nextFloat() + 1.0f) * ((float)(rand.nextInt(20) - 10)))));
			temp.setLocalScale((new Matrix4f()).scaling(0.1f));
			boostList.add(temp);
		}
		String boostReady = "Empty";
		if(boost) {
			boostReady = "Active";
			hudColor = new Vector3f(rand.nextFloat(),rand.nextFloat(),rand.nextFloat());
		}
		else { hudColor = new Vector3f(0.6f,0.2f,0.1f); }
		hudName = "BOOST";
		dispStr = "Boost = " + boostReady;
		(engine.getHUDmanager()).setHUD(hudName, dispStr, hudColor, (int)(winWidth*0.5), 45);

		//Check boost 'collision'
		for( GameObject o : boostList ) {
			if(avatar.distance(o) < 1.0f && !boost) {
				light2.setDiffuse(0.8f, 0f, 0.2f);
				boostSound.play();
				boost = true;
				boostStartTime = elapsTimeSec;
				o.setParent(avatar);
				o.applyParentRotationToPosition(true);
				o.setLocalLocation(new Vector3f(0f, 0.4f, 0.5f));
				o.setLocalScale((new Matrix4f()).scaling(0.5f));
				boostUsed = o;
				rc.addTarget(o);
			}
		}
		boostList.remove(boostUsed);

		// Check if inventory of balls needs to be incremented
		for( BallObject o : inventoryBalls ) {
			if(!o.getRenderStates().renderingEnabled() && (getElapsedTimeMilli() - 5000.0f > o.getUsedTime())) {
				o.getRenderStates().enableRendering();
				inventorySize++;
			}
		}
		int tempCount = 0;
		for( BallObject o : inventoryBalls ) {
			if(o.getRenderStates().renderingEnabled())
				tempCount++;
		}
		inventorySize = tempCount;

		// Check if spear should come off of cooldown
		if(getElapsedTimeMilli() - 2000.0 > spearLastThrown) 
			spearReady = true;
		
		if(getElapsedTimeMilli() - 2000.0 > lastSwapTime)
			swapCooldown = false;

		if(getElapsedTimeMilli() - 2000.0 > lastTimeHit)
			hitCooldown = false;

		// Check if avatar is stationary and whether the walk animation should stop
		if(getElapsedTimeMilli() - 20.0 > timeLastMoved) { setMoving(false); avatarS.stopAnimation(); }

		// 'Despawn' a ball and its physics object after 10s
		for( BallObject o : ballList ) {
			if(getElapsedTimeMilli() - 10000.0f > o.getSpawnTime() && o.getPhysicsObject() != null) {
				PhysicsObject tempP = o.getPhysicsObject();
				UID tempID = new UID(-1);
				for( UID uid: ballIDList ) {
					if(uid.id == tempP.getUID())
						tempID = uid;
				}
				ballIDList.remove(tempID);
				physicsEngine.removeObject(tempP.getUID());
				ballPList.remove(tempP);
				o.getRenderStates().disableRendering();
			}
		}

		// update physics
		if (running)
		{	Matrix4f mat = new Matrix4f();
			Matrix4f mat2 = new Matrix4f().identity();
			checkForCollisions();
			physicsEngine.update((float)elapsedTime);
			for ( GameObject go : engine.getSceneGraph().getGameObjects() )
			{	if (go.getPhysicsObject() != null && (go != avatar || jumping) && go.getClass() != GhostAvatar.class && go.getClass() != GhostDummy.class)
				{	mat.set(toFloatArray(go.getPhysicsObject().getTransform()));
					mat2.set(3,0,mat.m30()); mat2.set(3,1,mat.m31()); mat2.set(3,2,mat.m32());
					go.setLocalTranslation(mat2);

					if(go == spear) {
						Vector3f tempVec = new Vector3f(-1f,-1f,-1f);
						tempVec = mat.getTranslation(tempVec);
						//System.out.println("\nSpear Phys Obj Loc:" + tempVec.x() + ", " + tempVec.y() + ", " + tempVec.z());
					}
					
					if(go != avatar && go != spear) {
						mat2 = new Matrix4f();
						mat2.set(toFloatArray(go.getPhysicsObject().getTransform()));
						AxisAngle4f aa = new AxisAngle4f();
						mat2.getRotation(aa);
						mat = new Matrix4f();
						mat.rotation(aa);
						go.setLocalRotation(mat);
					}
				}
				if (go == avatar) {
					//go.getPhysicsObject().setTransform(toDoubleArray((avatar.getLocalTranslation()).get(vals)));
				}
			}
		}
		for( PhysicsObject o: objsSetForRemoval ) {
			physicsEngine.removeObject(o.getUID());
		}
		objsSetForRemoval = new ArrayList<PhysicsObject>();

		avatar.getPhysicsObject().setTransform(toDoubleArray((avatar.getLocalTranslation()).get(vals)));
		if(ghostAvatars != null) {
			for( GhostAvatar g: ghostAvatars ) {
				g.getPhysicsObject().setTransform(toDoubleArray((g.getLocalTranslation()).get(vals)));
				float[] tempVel = {0, 0, 0};
				g.getPhysicsObject().setLinearVelocity(tempVel);
			}
		}
		if(dummyList != null) {
			for( GhostDummy g: dummyList ) {
				g.getPhysicsObject().setTransform(toDoubleArray((g.getLocalTranslation()).get(vals)));
				float[] tempVel = {0, 0, 0};
				g.getPhysicsObject().setLinearVelocity(tempVel);
			}
		}
		if(!jumping) {
			float[] tempVel = avatar.getPhysicsObject().getLinearVelocity();
			tempVel[0] = 0;
			tempVel[1] = 0;
			tempVel[2] = 0;
			avatar.getPhysicsObject().setLinearVelocity(tempVel);
		}

		//send object updates periodically to other clients
		if(elapsTimeSec*1000.0f - 25.0f > updateSentTime) {
			updateSentTime = elapsTimeSec;
			//send updates here
			protClient.sendMoveMessage(avatar.getWorldLocation());
		}

		avatarS.updateAnimation();
		ghostS.updateAnimation();

		setEarParameters();

		// update inputs and camera
		im.update((float)elapsedTime);	
		if(im.getFirstGamepadName() != null)
			camOrbitController.updateCameraPosition();
		processNetworking((float)elapsedTime);
	}

	public AnimatedShape getGhostShape() { return ghostS; }
	public TextureImage getGhostTexture() { return ghosttx; }
	public GhostManager getGhostManager() { return gm; }
	public Engine getEngine() { return engine; }

	private void runScript(File scriptFile) {
		try { 
			FileReader fileReader = new FileReader(scriptFile); 
			jsEngine.eval(fileReader); 
			fileReader.close(); 
		} 
		catch (FileNotFoundException e1) { 
			System.out.println(scriptFile + " not found " + e1); 
		} 
		catch (IOException e2) { 
			System.out.println("IO problem with " + scriptFile + e2); 
		} 
		catch (ScriptException e3)  { 
			System.out.println("ScriptException in " + scriptFile + e3); 
		} 
		catch (NullPointerException e4) { 
			System.out.println ("Null ptr exception reading " + scriptFile + e4); 
		}
	}

	public void initAudio() {
		AudioResource resource;
		audioMgr = AudioManagerFactory.createAudioManager("tage.audio.joal.JOALAudioManager");
		if (!audioMgr.initialize())
		{	System.out.println("Audio Manager failed to initialize!");
			return;
		}
		resource = audioMgr.createAudioResource("assets/sounds/throw.wav", AudioResourceType.AUDIO_SAMPLE);
		throwSound = new Sound(resource, SoundType.SOUND_EFFECT, 50, false);
		throwSound.initialize(audioMgr);
		throwSound.setMaxDistance(10.0f);
		throwSound.setMinDistance(1f);
		throwSound.setRollOff(0.5f);

		resource = audioMgr.createAudioResource("assets/sounds/boost.wav", AudioResourceType.AUDIO_SAMPLE);
		boostSound = new Sound(resource, SoundType.SOUND_EFFECT, 1, false);
		boostSound.initialize(audioMgr);
		boostSound.setMaxDistance(10.0f);
		boostSound.setMinDistance(0.5f);
		boostSound.setRollOff(0.5f);

		resource = audioMgr.createAudioResource("assets/sounds/bgSound.wav", AudioResourceType.AUDIO_STREAM);
		bgSound = new Sound(resource, SoundType.SOUND_EFFECT, 1, true);
		bgSound.initialize(audioMgr);

		/*
		resource = audioMgr.createAudioResource("assets/sounds/bounce.wav", AudioResourceType.AUDIO_SAMPLE);
		bounceSound = new Sound(resource, SoundType.SOUND_EFFECT, 5, false);
		bounceSound.initialize(audioMgr);
		bounceSound.setMaxDistance(50.0f);
		bounceSound.setMinDistance(5f);
		bounceSound.setRollOff(10f);
		*/

		setEarParameters();
	}

	public void setEarParameters() {
		Camera camera = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		audioMgr.getEar().setLocation(avatar.getWorldLocation());
		audioMgr.getEar().setOrientation(camera.getN(), new Vector3f(0.0f, 1.0f, 0.0f));
	}
	
	private void setupNetworking()
	{	isClientConnected = false;	
		try 
		{	protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		} 	catch (UnknownHostException e) 
		{	e.printStackTrace();
		}	catch (IOException e) 
		{	e.printStackTrace();
		}
		if (protClient == null)
		{	System.out.println("missing protocol host");
		}
		else
		{	// Send the initial join message with a unique identifier for this client
			System.out.println("sending join message to protocol host");
			protClient.sendJoinMessage();
		}
	}
	
	protected void processNetworking(float elapsTime)
	{	// Process packets received by the client from the server
		if (protClient != null)
			protClient.processPackets();
	}

	public Vector3f getPlayerPosition() { return avatar.getWorldLocation(); }
	public boolean texSelected() { return texSelected; }

	public void setIsConnected(boolean value) { this.isClientConnected = value; }
	
	private class SendCloseConnectionPacketAction extends AbstractInputAction
	{	@Override
		public void performAction(float time, net.java.games.input.Event evt) 
		{	if(protClient != null && isClientConnected == true)
			{	protClient.sendByeMessage();
			}
		}
	}

	private void checkForCollisions()
	{	com.bulletphysics.dynamics.DynamicsWorld dynamicsWorld;
		com.bulletphysics.collision.broadphase.Dispatcher dispatcher;
		com.bulletphysics.collision.narrowphase.PersistentManifold manifold;
		com.bulletphysics.dynamics.RigidBody object1, object2;
		com.bulletphysics.collision.narrowphase.ManifoldPoint contactPoint;

		dynamicsWorld = ((JBulletPhysicsEngine)physicsEngine).getDynamicsWorld();
		dispatcher = dynamicsWorld.getDispatcher();
		int manifoldCount = dispatcher.getNumManifolds();
		for (int i=0; i<manifoldCount; i++)
		{	manifold = dispatcher.getManifoldByIndexInternal(i);
			object1 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody0();
			object2 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody1();
			JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
			JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);
			for (int j = 0; j < manifold.getNumContacts(); j++)
			{	contactPoint = manifold.getContactPoint(j);
				if (contactPoint.getDistance() < 0.0f)
				{	
					if( ((obj1.getUID() == avatarID && ballPList.contains((PhysicsObject)obj2)) || (obj2.getUID() == avatarID && ballPList.contains((PhysicsObject)obj1))) && !hitCooldown ) {
						System.out.println("\nBall & Avatar Hit!!\n");
						health--;
						hitCooldown = true;
						lastTimeHit = getElapsedTimeMilli();
						if(health == 0)
							gameOver();
					}
					else if( (ghostPList.contains((PhysicsObject)obj1) && ballPList.contains((PhysicsObject)obj2)) || (ghostPList.contains((PhysicsObject)obj2) && ballPList.contains((PhysicsObject)obj1)) ) {
						System.out.println("\nBall & Ghost Hit!!\n");
						for( GhostAvatar g: ghostAvatars ) {
							if(g.getPhysicsObject().getUID() == obj1.getUID() || g.getPhysicsObject().getUID() == obj2.getUID())
								g.hit();
						}
					}
					else if( (obj1.getUID() == avatarID && obj2.getUID() == groundP.getUID()) || (obj2.getUID() == avatarID && obj1.getUID() == groundP.getUID())) {
						System.out.println("\nAvatar & Ground Hit!!\n");
						setJumping(false);
						numJumps = 3;
						float vel[] = {0, 0, 0};
						avatarP.setLinearVelocity(vel);
						avatar.setLocalLocation(new Vector3f(avatar.getLocalLocation().x(), 
															 ground.getLocalLocation().y()+1.4f, 
															 avatar.getLocalLocation().z()));
					}
					/*
					else if( (obj1.getUID() == spearID && obj2.getUID() == groundP.getUID()) || (obj2.getUID() == spearID && obj1.getUID() == groundP.getUID())) {
						System.out.println("\nSpear & Ground Hit!!\n");
						objsSetForRemoval.add(spearP);
						spearID = -1;
					}
					*/
					else if( ((obj1.getUID() == spearID && dummyPList.contains(obj2)) || (obj2.getUID() == spearID && dummyPList.contains(obj1))) && !swapCooldown) {
						System.out.println("\nSpear & GhostDummy Hit!!\n");
						GhostDummy dumTemp = null;
						for( GhostDummy g: dummyList ) {
							if(g.getPhysicsObject().getUID() == obj1.getUID() || g.getPhysicsObject().getUID() == obj2.getUID())
								dumTemp = g;
						}
						if(dumTemp == null) { System.out.println("Error: Couldn't Find dummy in dummyList"); System.exit(0); }
						Matrix4f tempMat = avatar.getLocalRotation();
						Vector3f tempLoc = avatar.getLocalLocation();
						avatar.setLocalLocation(dumTemp.getLocalLocation());
						avatar.setLocalRotation(dumTemp.getLocalRotation());
						dumTemp.setLocalLocation(tempLoc);
						dumTemp.setLocalRotation(tempMat);
						objsSetForRemoval.add(spearP);
						//Send Server Notice Here
						protClient.sendDummySwapMessage(dumTemp.getID(), dumTemp.getLocalLocation());
						swapCooldown = true;
						lastSwapTime = getElapsedTimeMilli();
						spear.getRenderStates().disableRendering();
						bounceSound.setLocation(tempLoc);
						bounceSound.play();
					}
					/*
					else if( (obj1.getUID() == groundP.getUID() && ballPList.contains((PhysicsObject)obj2)) || (obj2.getUID() == groundP.getUID() && ballPList.contains((PhysicsObject)obj1))) {
						System.out.println("\nBall Hit Ground!!\n");
						Matrix4f mat = new Matrix4f().identity();
						Vector3f tempLoc = new Vector3f(0f,0f,0f);
						PhysicsObject tempB;
						if(obj1.getUID() == groundP.getUID())
							tempB = (PhysicsObject)obj2;
						else
							tempB = (PhysicsObject)obj1;
						mat.set(toFloatArray(tempB.getTransform()));
						tempLoc = mat.getTranslation(tempLoc);  System.out.println("x: " + tempLoc.x() + "  z: " + tempLoc.z());
						bounceSound.setLocation(tempLoc);
						float[] tempVel = tempB.getLinearVelocity();
						bounceSound.setVelocity(new Vector3f(tempVel[0], tempVel[1], tempVel[2]));
						bounceSound.play();
					}
					*/
					else
						//System.out.println("---- hit between " + obj1 + " and " + obj2);

					break;
				}
			}
		}
	}

	public void gameOver() {
		for(int i = 0; i < 5; i++)
			System.out.println("\n   GAME   OVER   \n");
		System.exit(0);
	}

	@Override
	public void keyPressed(KeyEvent e)
	{	switch (e.getKeyCode())
		{	case KeyEvent.VK_ESCAPE:
			{	System.out.println("Leaving Game");
				protClient.sendByeMessage();
				audioMgr.shutdown();
				break;
			}
			case KeyEvent.VK_LEFT:
			{	avatar.setTextureImage(avatarRedtx);
				texSelected = true;
				(engine.getHUDmanager()).remHUD("TEX SELECT");
				break;
			}
			case KeyEvent.VK_RIGHT:
			{	avatar.setTextureImage(avatarBluetx);
				texSelected = true;
				(engine.getHUDmanager()).remHUD("TEX SELECT");
				break;
			}
		}
		super.keyPressed(e);
	}

	// ------------------ UTILITY FUNCTIONS used by physics--------------------

	private float[] toFloatArray(double[] arr)
	{	if (arr == null) return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++)
		{	ret[i] = (float)arr[i];
		}
		return ret;
	}
 
	private double[] toDoubleArray(float[] arr)
	{	if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++)
		{	ret[i] = (double)arr[i];
		}
		return ret;
	}
}