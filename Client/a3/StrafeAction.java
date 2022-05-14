package a3;

import tage.*;
import tage.shapes.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;
import tage.physics.PhysicsObject;

public class StrafeAction extends AbstractInputAction
{
	private MyGame game;
	private GameObject av;
	private AnimatedShape avS;
	private Camera cam;
	private Vector3f oldPosition, newPosition;
	private Vector4f rightDirection;
	private float vals[] = new float[16];
	private ProtocolClient protClient;

	public StrafeAction(MyGame g, ProtocolClient p, AnimatedShape avatarS)
	{	game = g;
		protClient = p;
		cam = (game.getEngine().getRenderSystem()).getViewport("MAIN").getCamera();
		avS = avatarS;
	}

	@Override
	public void performAction(float time, Event e)
	{	float keyValue = e.getValue();
		if(keyValue < .2 && keyValue > -.2) return;
		if(game.getBoost())
			keyValue *= 2;
		if(e.getComponent().toString().equals("W"))  //Reverse direction
			keyValue *= -1;
		av = game.getAvatar();
		oldPosition = av.getWorldLocation();
		rightDirection = new Vector4f(-cam.getU().x(), 0, -cam.getU().z(), 1);
		//rightDirection = new Vector4f(1f,0f,0f,1f);
		//rightDirection.mul(av.getWorldRotation());
		rightDirection.mul((-keyValue*time)/200f);
		newPosition = oldPosition.add(rightDirection.x(), rightDirection.y(), rightDirection.z());
		av.setLocalLocation(newPosition);
		av.getPhysicsObject().setTransform(toDoubleArray((av.getLocalTranslation()).get(vals)));

		if(!game.getMoving()) {
			avS.stopAnimation();
			avS.playAnimation("WALK", 0.5f, AnimatedShape.EndType.LOOP, 0);
		}
		game.setMoving(true);

		protClient.sendMoveMessage(av.getWorldLocation());
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