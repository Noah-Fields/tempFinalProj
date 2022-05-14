package a3;

import tage.*;
import java.util.UUID;
import org.joml.*;

public class GhostDummy extends GameObject
{
	private UUID id;
	private MyGame game;
	
	public GhostDummy(UUID id, ObjShape s, TextureImage t, Vector3f p, MyGame game) 
	{	super(GameObject.root(), s, t);
		this.id = id;
		setPosition(p);
		this.game = game;
		this.setLocalScale((new Matrix4f()).scaling(0.6f, 0.6f, 0.6f));
		this.lookAt(new Vector3f(0f,1.4f,0f));
	}
	
	public UUID getID() { return id; }
	public void setPosition(Vector3f p) { this.setLocalLocation(p); }
	public Vector3f getPosition() { return this.getWorldLocation(); }
}
