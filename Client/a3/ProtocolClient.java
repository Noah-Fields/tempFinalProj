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

import tage.*;
import tage.networking.client.GameConnectionClient;

public class ProtocolClient extends GameConnectionClient
{
	private MyGame game;
	private String color;
	private PhysicsEngine physEng;
	private ArrayList<PhysicsObject> dummyPList;
	private GhostManager ghostManager;
	private UUID id;
	private GhostNPC ghostNPC;
	private ArrayList<GhostDummy> dummyList;
	
	public ProtocolClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, MyGame game) throws IOException 
	{	super(remoteAddr, remotePort, protocolType);
		this.game = game;
		this.id = UUID.randomUUID();
		ghostManager = game.getGhostManager();
		dummyList = new ArrayList<GhostDummy>();
		dummyPList = new ArrayList<PhysicsObject>();
	}
	
	public UUID getID() { return id; }
	public ArrayList<GhostDummy> getDummies() { return dummyList; }

	// ------------- GHOST NPC SECTION --------------

	private void createGhostNPC(Vector3f position) throws IOException
	{	if (ghostNPC == null) {
			ghostNPC = new GhostNPC(0, game.getNPCshape(), game.getNPCtexture(), position, game);
			game.createGhostNPC(ghostNPC);
		}
	}

	private void createGhostDummy(UUID id, Vector3f position) throws IOException {
		GhostDummy tempD = new GhostDummy(id, game.getDummyShape(), game.getDummyTexture(), position, game);

		if(physEng == null)
			physEng = game.getPhysEngine();
		float mass = 2.0f;
		float size[] = {1.0f, 1.0f, 1.0f};
		float vals[] = new float[16];
		double[] tempTransform = toDoubleArray((tempD.getLocalTranslation()).get(vals));
		PhysicsObject tempP = physEng.addCylinderObject(physEng.nextUID(), mass, tempTransform, size);
		tempD.setPhysicsObject(tempP);
		dummyPList.add(tempP);
		dummyList.add(tempD);
	}

	private void updateGhostNPC(Vector3f position, float gsize)
	{	boolean gs;
		if (ghostNPC == null)
		{	try
			{	createGhostNPC(position);
			}	catch (IOException e)
			{	System.out.println("error creating ghost npc at update");
			}
		}
		ghostNPC.setPosition(position);
		ghostNPC.setSize(gsize);
	}

	private void updateGhostDummy(UUID id, Vector3f position)
	{	
		for( GhostDummy g: dummyList ) {
			if(g.getID().compareTo(id) == 0) {
				g.setLocalLocation(position);
			}
		}
	}

	public GhostNPC getGhostNPC() { return ghostNPC; }

	// ------------  PROCESS INCOMING PACKETS SECTION ----------------
	
	@Override
	protected void processPacket(Object message)
	{	if(message == null) return;
		String strMessage = (String)message;
		//System.out.println("message received -->" + strMessage);
		String[] messageTokens = strMessage.split(",");

		// Game specific protocol to handle the message
		if(messageTokens.length > 0)
		{
			// Handle JOIN message
			// Format: (join,success) or (join,failure)
			if(messageTokens[0].compareTo("join") == 0)
			{	if(messageTokens[1].compareTo("success") == 0)
				{	System.out.println("join success confirmed");
					game.setIsConnected(true);
					color = "blue";
					if(game.getAvatar().getTextureImage().equals(game.getRedTex()))
						color = "red";
					sendCreateMessage(game.getPlayerPosition(), color);
					System.out.println("client asking for NPC info");
					askForNPCinfo();
				}
				if(messageTokens[1].compareTo("failure") == 0)
				{	System.out.println("join failure confirmed");
					game.setIsConnected(false);
			}	}
			
			// Handle BYE message
			// Format: (bye,remoteId)
			if(messageTokens[0].compareTo("bye") == 0)
			{	// remove ghost avatar with id = remoteId
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				ghostManager.removeGhostAvatar(ghostID);
			}
			
			// Handle CREATE message
			// Format: (create,remoteId,x,y,z,color)
			// AND
			// Handle DETAILS FOR message
			// Format: (dsfr,remoteId,x,y,z,color)
			if (messageTokens[0].compareTo("create") == 0 || messageTokens[0].compareTo("dsfr") == 0)
			{	// create a new ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
				
				String ghostColor = messageTokens[5];

				try
				{	ghostManager.createGhostAvatar(ghostID, ghostPosition, ghostColor);
				}	catch (IOException e)
				{	System.out.println("error creating ghost avatar");
				}
			}
			
			// Handle WANTS_DETAILS message
			// Format: (wsds,remoteId)
			if (messageTokens[0].compareTo("wsds") == 0)
			{
				// Send the local client's avatar's information
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				sendDetailsForMessage(ghostID, game.getPlayerPosition());
			}
			
			// Handle MOVE message
			// Format: (move,remoteId,x,y,z)
			if (messageTokens[0].compareTo("move") == 0)
			{
				// move a ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
				
				ghostManager.updateGhostAvatarPosition(ghostID, ghostPosition);
			}

			// Handle TURN message
			// Format: (turn,localId,a,b,c,d)
			if (messageTokens[0].compareTo("turn") == 0)
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);
				float val1 = Float.parseFloat(messageTokens[2]);
				float val2 = Float.parseFloat(messageTokens[3]);
				float val3 = Float.parseFloat(messageTokens[4]);
				float val4 = Float.parseFloat(messageTokens[5]);
				ghostManager.updateGhostAvatarOrientation(ghostID, val1, val2, val3, val4);
			}

			// Handle BALL message
			// Format: (ball,localId,x,y,z,a,b,c)
			if (messageTokens[0].compareTo("ball") == 0)
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f ballPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
				
				float[] velocity = {Float.parseFloat(messageTokens[5]), 
									Float.parseFloat(messageTokens[6]), 
									Float.parseFloat(messageTokens[7])};

				game.throwBall(ballPosition, velocity);
			}

			// ------------- HANDLE NPC MESSAGES ---------------

			// Handle CREATE_NPC message
			// Format: (createNPC,id,x,y,z,state)
			if (messageTokens[0].compareTo("createNPC") == 0)
			{	// create a new ghost NPC
				// Parse out the position
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[1]),
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]));
				try
				{	createGhostNPC(ghostPosition); System.out.println("client creating a ghost NPC");
				}	catch (IOException e)
				{	System.out.println("error creating ghost NPC");
				}
			}

			// Handle DUMMY CREATE message
			// Format: (dummyCreate,id,x,y,z)
			if (messageTokens[0].compareTo("dummyCreate") == 0)
			{	// create a list of dummies
				// Parse out the position
				UUID id = UUID.fromString(messageTokens[1]);
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
				try
				{	createGhostDummy(id, ghostPosition); System.out.println("client creating a ghost Dummy");
				}	catch (IOException e)
				{	System.out.println("error creating ghost Dummy");
				}
			}

			// Handle MOVE NPC message
			// Format: (mnpc,x,y,z,size)
			if(messageTokens[0].compareTo("mnpc") == 0)
			{	// move a ghost npc
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[1]),
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]));
				float gSize = Float.parseFloat(messageTokens[4]);
				updateGhostNPC(ghostPosition, gSize);
			}

			// Handle DUMMY UPDATE message
			// Format: (dummyUpdate,id,x,y,z)
			if(messageTokens[0].compareTo("dummyUpdate") == 0)
			{	// update dummy positions
				// Parse out the position into a Vector3f
				UUID id = UUID.fromString(messageTokens[1]);
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
				updateGhostDummy(id, ghostPosition);
			}

			// -------------------------------------------------------------
			
		}	
	}

	// ----------------------Utility functions---------------------
	public boolean checkRange(float numToCheck, float num1, float num2) {
		boolean retVal;
		System.out.println("\n\nTEST\n\n");
		if(num1 < num2) {
			if(numToCheck > num1 && numToCheck < num2)
				retVal = true;
			else
				retVal = false;
		}
		else if(num2 < num1) {
			if(numToCheck > num2 && numToCheck < num1)
				retVal = true;
			else
				retVal = false;
		}
		else
			retVal = false;
		
		return retVal;
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
	
	// The initial message from the game client requesting to join the 
	// server. localId is a unique identifier for the client. Recommend 
	// a random UUID.
	// Message Format: (join,localId)
	
	public void sendJoinMessage()
	{	try 
		{	sendPacket(new String("join," + id.toString()));
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server that the client is leaving the server. 
	// Message Format: (bye,localId)

	public void sendByeMessage()
	{	try 
		{	sendPacket(new String("bye," + id.toString()));
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server of the client's Avatar's position. The server 
	// takes this message and forwards it to all other clients registered 
	// with the server.
	// Message Format: (create,localId,x,y,z) where x, y, and z represent the position

	public void sendCreateMessage(Vector3f position, String color)
	{	try 
		{	String message = new String("create," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + color;
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server of the local avatar's position. The server then 
	// forwards this message to the client with the ID value matching remoteId. 
	// This message is generated in response to receiving a WANTS_DETAILS message 
	// from the server.
	// Message Format: (dsfr,remoteId,localId,x,y,z) where x, y, and z represent the position.

	public void sendDetailsForMessage(UUID remoteId, Vector3f position)
	{	try 
		{	String message = new String("dsfr," + remoteId.toString() + "," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + color;
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server that the local avatar has changed position.  
	// Message Format: (move,localId,x,y,z) where x, y, and z represent the position.

	public void sendMoveMessage(Vector3f position)
	{	try 
		{	String message = new String("move," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}

	// Informs the server that the local avatar has changed rotation.  
	// Message Format: (turn,localId,a,b,c,d) where a,b,c,d represent the values in the rotation matrix at
	// positions (0,0), (0,2), (2,0), (2,2)

	public void sendTurnMessage(float val1, float val2, float val3, float val4)
	{
		try
		{	String message = new String("turn," + id.toString());
			message += "," + val1;
			message += "," + val2;
			message += "," + val3;
			message += "," + val4;

			sendPacket(message);
		} 
		catch (IOException e) {	
			e.printStackTrace();
		}
	}

	// Informs the server that the local avatar has thrown a ball.
	// Message Format: (ball,localId,x,y,z,a,b,c) where x,y,z are the starting location of the ball that was just thrown
	// and a,b,c is the initial velocity of the ball thrown

	public void sendBallMessage(Vector3f position, float[] velocity)
	{
		try
		{	String message = new String("ball," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();

			message += "," + velocity[0];
			message += "," + velocity[1];
			message += "," + velocity[2];

			sendPacket(message);
		} 
		catch (IOException e) {	
			e.printStackTrace();
		}
	}

	// --------------- NPC SECTION --------------------

	public void askForNPCinfo()
	{	try
		{	sendPacket(new String("needNPC," + id.toString()));
		}
		catch (IOException e)
		{	e.printStackTrace();
		}
	}

	public void sendDummySwapMessage(UUID dummyID, Vector3f dummyLoc) {
		try
		{	String message = new String("dummySwap," + dummyID.toString());
			message += "," + dummyLoc.x();
			message += "," + dummyLoc.y();
			message += "," + dummyLoc.z();

			sendPacket(message);
		} 
		catch (IOException e) {	
			e.printStackTrace();
		}
	}

}
