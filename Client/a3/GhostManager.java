package a3;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import java.util.ArrayList;
import org.joml.*;

import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.shapes.AnimatedShape;
import tage.*;

public class GhostManager
{
	private MyGame game;
	private ArrayList<GhostAvatar> ghostAvatars = new ArrayList<GhostAvatar>();
	private PhysicsEngine physEng;
	private float vals[] = new float[16];

	public GhostManager(VariableFrameRateGame vfrg)
	{	game = (MyGame)vfrg;
	}
	
	public void createGhostAvatar(UUID id, Vector3f position, String color) throws IOException
	{	if(findAvatar(id) != null) return;
		System.out.println("adding ghost with ID --> " + id);
		AnimatedShape s = game.getGhostShape();
		TextureImage t = game.getRedTex();
		if(color.equals("blue"))
			t = game.getBlueTex();
		GhostAvatar newAvatar = new GhostAvatar(id, s, t, position, game);
		Matrix4f initialScale = (new Matrix4f()).scaling(0.5f);
		newAvatar.setLocalScale(initialScale);
		if(physEng == null) physEng = game.getPhysEngine();

		double[] tempTransform = toDoubleArray((newAvatar.getLocalTranslation()).get(vals));
		float size[] = {1.0f, 1.0f, 1.0f};
		PhysicsObject tempP = physEng.addCylinderObject(physEng.nextUID(), 2.0f, tempTransform, size);
		tempP.setBounciness(0.5f);
		tempP.setFriction(0);
		tempP.setDamping(0, 0);
		newAvatar.setPhysicsObject(tempP);
		ghostAvatars.add(newAvatar);
		game.addGhostP(tempP);
		game.getGhostAvatars(ghostAvatars);
	}
	
	public void removeGhostAvatar(UUID id)
	{	GhostAvatar ghostAvatar = findAvatar(id);
		if(ghostAvatar != null)
		{	game.getEngine().getSceneGraph().removeGameObject(ghostAvatar);
			game.removeGhostP(ghostAvatar.getPhysicsObject());
			physEng.removeObject(ghostAvatar.getPhysicsObject().getUID());
			ghostAvatars.remove(ghostAvatar);
		}
		else
		{	System.out.println("tried to remove, but unable to find ghost in list");
		}
	}

	private GhostAvatar findAvatar(UUID id)
	{	GhostAvatar ghostAvatar;
		Iterator<GhostAvatar> it = ghostAvatars.iterator();
		while(it.hasNext())
		{	ghostAvatar = it.next();
			if(ghostAvatar.getID().compareTo(id) == 0)
			{	return ghostAvatar;
			}
		}		
		return null;
	}
	
	public void updateGhostAvatarPosition(UUID id, Vector3f position)
	{	GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null)
		{	
			if(!position.equals(ghostAvatar.getPosition(), 0.05f))
				ghostAvatar.setMoving(true);
			
			if(game.getElapsedTimeMilli() - 50.0 > ghostAvatar.timeLastMoved) {
				ghostAvatar.setMoving(false);
				ghostAvatar.stopAnim();
			}
			
			ghostAvatar.setPosition(position);
			ghostAvatar.getPhysicsObject().setTransform(toDoubleArray((ghostAvatar.getLocalTranslation()).get(vals)));
		}
		else
		{	System.out.println("tried to update ghost avatar position, but unable to find ghost in list");
		}
	}

	public void updateGhostAvatarOrientation(UUID id, float val1, float val2, float val3, float val4)
	{	GhostAvatar ghostAvatar = findAvatar(id);
		if (ghostAvatar != null)
		{	Matrix4f tempRot = ghostAvatar.getWorldRotation();
			float[] oldRot = new float[16];
			tempRot.get(oldRot);
			oldRot[0] = val1;
			oldRot[2] = val2;
			oldRot[8] = val3;
			oldRot[10] = val4;
			ghostAvatar.setLocalRotation(new Matrix4f().set(oldRot));
		}
		else
		{	System.out.println("tried to update ghost avatar position, but unable to find ghost in list");
		}
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
