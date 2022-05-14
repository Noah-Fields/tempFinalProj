package a3;

import tage.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import org.joml.*;

public class TurnAction extends AbstractInputAction
{
	private MyGame game;
	private GameObject av;
	private ProtocolClient protClient;

	public TurnAction(MyGame g, ProtocolClient p)
	{	game = g;
		protClient = p;
	}

	@Override
	public void performAction(float time, Event e)
	{	float keyValue = e.getValue();
		if (keyValue > -.2 && keyValue < .2) return;  // deadzone
		if(e.getComponent().toString().equals("A"))
			keyValue *= -1;
		av = game.getAvatar();

		av.yaw(keyValue, time);
		protClient.sendTurnMessage(av.getWorldRotation().m00(),
								   av.getWorldRotation().m02(),
								   av.getWorldRotation().m20(),
								   av.getWorldRotation().m22());
	}
}