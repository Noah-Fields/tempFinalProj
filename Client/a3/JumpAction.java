package a3;

import tage.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;

public class JumpAction extends AbstractInputAction
{
	private MyGame game;
	private GameObject av;
	private Vector3f oldPosition, newPosition;
	private Vector3f fwdDirection;
	private ProtocolClient protClient;

	public JumpAction(MyGame g, ProtocolClient p)
	{	game = g;
		protClient = p;
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
		fwdDirection = av.getWorldForwardVector();
		if(game.getJumps() > 0) {
			game.setJumping(true);
			float velocity[] = {fwdDirection.x()*3.0f, 2f*3.0f, fwdDirection.z()*3.0f};
			av.getPhysicsObject().setLinearVelocity(velocity);	
		}
		
		protClient.sendMoveMessage(av.getWorldLocation());
	}
}