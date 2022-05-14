package a3;

import tage.*;
import org.joml.*;

public class GhostNPC extends GameObject
{
	private int id;
	private MyGame game;
	private float curSize;
	
	public GhostNPC(int id, ObjShape s, TextureImage t, Vector3f p, MyGame game) 
	{	super(GameObject.root(), s, t);
		this.id = id;
		setPosition(p);
		this.game = game;
	}

	public float getSize() { return curSize; }
	
	public void setSize(float size)
	{	this.setLocalScale((new Matrix4f()).scaling(size)); curSize = size;
		/*
		if (small) { this.setLocalScale((new Matrix4f()).scaling(0.5f)); curSize = 0.5f; }
		else { 
			this.setLocalScale((new Matrix4f()).scaling(1.0f));
			curSize = 1.0f;
		}
		*/
	}
	
	public int getID() { return id; }
	public void setPosition(Vector3f p) { this.setLocalLocation(p); }
	public Vector3f getPosition() { return this.getWorldLocation(); }
}
