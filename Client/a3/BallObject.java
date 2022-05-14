package a3;

import tage.*;
import tage.shapes.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;
import java.util.ArrayList;

public class BallObject extends GameObject
{
	private double spawnTime = 0;
	private double usedTime = 0; //used to track when a ball was used from inventory
	private int id; //used by OrbitController to help determine spacing

	public BallObject() {
		super(GameObject.root(), new ImportedModel("Ball.obj"), new TextureImage("Ball.png"));
	}

	public BallObject(GameObject o, ObjShape s, TextureImage t) {
		super(o, s, t);
	}

	public double getSpawnTime() { return spawnTime; }
	public void setSpawnTime(double time) { spawnTime = time; }
	public double getUsedTime() { return usedTime; }
	public void setUsedTime(double time) { usedTime = time; }

	public int getId() { return id; }
	public void setId(int num) { id = num; }
}