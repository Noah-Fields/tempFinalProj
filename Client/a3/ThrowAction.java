package a3;

import tage.*;
import tage.audio.*;
import tage.shapes.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import java.util.ArrayList;

public class ThrowAction extends AbstractInputAction
{
	private MyGame game;
	private GameObject av;
	private AnimatedShape avS;
	private Sound throwSound;
	private Camera cam;
	private Vector3f loc;
	private Vector3f fwdDirection;
	private ProtocolClient protClient;

	public ThrowAction(MyGame g, ProtocolClient p, Sound throwAudio, AnimatedShape avatarS)
	{	game = g;
		protClient = p;
		cam = (game.getEngine().getRenderSystem()).getViewport("MAIN").getCamera();
		avS = avatarS;
		throwSound = throwAudio;
	}

	@Override
	public void performAction(float time, Event e)
	{	float keyValue = e.getValue();
		if(keyValue < .2 && keyValue > -.2) return;
		if(game.getInv() < 1) return;
		if(game.checkBallCooldown()) return;
		if(game.getBoost())
			keyValue *= 2;
		av = game.getAvatar();
		game.setMoving(false);
		avS.stopAnimation();
		avS.playAnimation("THROW", 0.5f, AnimatedShape.EndType.STOP, 0);
		loc = av.getWorldLocation();
		fwdDirection = cam.getN();
		float startX = 2.0f;
		float startZ = 2.0f;
		if(fwdDirection.x() < 0) startX = -2.0f;
		if(fwdDirection.z() < 0) startZ = -2.0f;
		loc.add(fwdDirection.x()*2.0f, fwdDirection.y(), fwdDirection.z()*2.0f);
		float[] avVel = av.getPhysicsObject().getLinearVelocity();
		float[] velocity = {fwdDirection.x()*30.0f, 
							fwdDirection.y()*3.0f + 5.0f, 
							fwdDirection.z()*30.0f};

		game.throwBall(loc, velocity);
		game.decInv();
		protClient.sendBallMessage(loc, velocity);
	}
}