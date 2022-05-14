package a3;

import java.util.UUID;

import tage.*;
import tage.shapes.AnimatedShape;

import org.joml.*;

// A ghost MUST be connected as a child of the root,
// so that it will be rendered, and for future removal.
// The ObjShape and TextureImage associated with the ghost
// must have already been created during loadShapes() and
// loadTextures(), before the game loop is started.

public class GhostAvatar extends GameObject
{
	MyGame game;
	AnimatedShape ghostS;
	UUID uuid;
	double timeLastMoved;
	double timeLastHit;
	int health = 5;
	boolean readyToHit = true;
	boolean moving = false;

	public GhostAvatar(UUID id, AnimatedShape s, TextureImage t, Vector3f p, MyGame gm) 
	{	super(GameObject.root(), s, t);
		ghostS = s;
		uuid = id;
		setPosition(p);
		game = gm;
	}

	public void hit() { health--; timeLastHit = game.getElapsedTimeMilli(); readyToHit = false; }
	
	public UUID getID() { return uuid; }
	public void setPosition(Vector3f m) { setLocalLocation(m); }
	public Vector3f getPosition() { return getWorldLocation(); }

	public void setMoving(boolean mv) {
		if(mv && !moving)
			ghostS.playAnimation("gWALK", 0.5f, AnimatedShape.EndType.LOOP, 0);
		moving = mv; 
		timeLastMoved = game.getElapsedTimeMilli(); 
	}
	public boolean getMoving() { return moving; }
	public void stopAnim() { ghostS.stopAnimation(); }
}
