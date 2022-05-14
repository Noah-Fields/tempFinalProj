import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import javax.management.DynamicMBean;

import java.util.ArrayList;

import org.joml.*;

import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;

public class GameServerUDP extends GameConnectionServer<UUID> 
{
	NPCcontroller npcCtrl;
	ArrayList<avatar> avList;
	avatar target = null;

	boolean waitingForCreate = false;
	double timeAtJoin = 0;
	UUID joinedClient;

	public GameServerUDP(int localPort, NPCcontroller npc) throws IOException 
	{	super(localPort, ProtocolType.UDP);
		npcCtrl = npc;
		avList = new ArrayList<avatar>();
	}

	public boolean sendCheckForTarget()
	{	try 
		{	if(target == null) return false;
			Vector3f npcLoc = npcCtrl.getNPC().getLoc();
			Vector3f dir = new Vector3f(target.getPos().x()-npcLoc.x(), target.getPos().y()-npcLoc.y(), target.getPos().z()-npcLoc.z());
			dir.normalize();
			float[] velocity = {dir.x()*30.0f, 
								dir.y()*3.0f, 
								dir.z()*30.0f};
			npcLoc.add(dir.x()*2.0f, dir.y(), dir.z()*2.0f);

			String message = new String("ball," + target.id.toString());
			message += "," + npcLoc.x();
			message += "," + npcLoc.y();
			message += "," + npcLoc.z();
			message += "," + velocity[0];
			message += "," + velocity[1];
			message += "," + velocity[2];
			sendPacketToAll(message);
		}
		catch (IOException e) 
		{	System.out.println("couldnt send ball message (sendCheckForTarget())");
				e.printStackTrace();
		}
		return true;
	}

	// ------------------  NPC SETUP ---------------
	
	/**
	 * Informs all clients of the new positions of every NPC.
	 * <p>
	 * Message Format: (mnpc,x,y,z,size) where x, y, and z represent the position.
	 * &
	 * Message Format: (dummyUpdate,id,x,y,z)
	 */
	public void sendNPCinfo()
	{	try 
		{	String message = new String("mnpc");
			message += "," + (npcCtrl.getNPC()).getX();
			message += "," + (npcCtrl.getNPC()).getY();
			message += "," + (npcCtrl.getNPC()).getZ();
			message += "," + (npcCtrl.getNPC()).getSize();
			sendPacketToAll(message);
			ArrayList<NPC> dumList = npcCtrl.getDummyList();
			for(int i = 0; i < npcCtrl.getNumDummys(); i++) {
				message = new String("dummyUpdate," + dumList.get(i).getID().toString());
				message += "," + (dumList.get(i)).getX();
				message += "," + (dumList.get(i)).getY();
				message += "," + (dumList.get(i)).getZ();
				sendPacketToAll(message);
			}
		}
		catch (IOException e) 
		{	System.out.println("clients not ready for NPCs yet");
			e.printStackTrace();
		}
	}
	
	public void sendNPCstart(UUID clientID)
	{	try 
		{	String message = new String("createNPC");
			message += "," + (npcCtrl.getNPC()).getX();
			message += "," + (npcCtrl.getNPC()).getY();
			message += "," + (npcCtrl.getNPC()).getZ();
			sendPacket(message,clientID);
			ArrayList<NPC> dumList = npcCtrl.getDummyList();
			for(int i = 0; i < npcCtrl.getNumDummys(); i++) {
				message = new String("dummyCreate," + dumList.get(i).getID().toString());
				message += "," + (dumList.get(i)).getX();
				message += "," + (dumList.get(i)).getY();
				message += "," + (dumList.get(i)).getZ();
				sendPacket(message, clientID);
			}
		} 
		catch (IOException e) 
		{	System.out.println("this client not ready for NPCs yet");
			e.printStackTrace();
		}
	}

	public class avatar {
		private Vector3f pos;
		private UUID id;
		private String color;

		public avatar(UUID id, Vector3f pos) {
			this.id = id;
			this.pos = pos;
		}

		public void setTex(String color) { this.color = color; }

		public Vector3f getPos() { return this.pos; }
		public void setPos(Vector3f pos) { this.pos = pos; }

		public UUID getID() { return this.id; }
		public void setID(UUID id) { this.id = id;}
	}

	@Override
	public void processPacket(Object o, InetAddress senderIP, int senderPort)
	{
		String message = (String)o;
		String[] messageTokens = message.split(",");
		
		if(messageTokens.length > 0)
		{	// JOIN -- Case where client just joined the server
			// Received Message Format: (join,localId)
			if(messageTokens[0].compareTo("join") == 0)
			{	try 
				{	IClientInfo ci;					
					ci = getServerSocket().createClientInfo(senderIP, senderPort);
					UUID clientID = UUID.fromString(messageTokens[1]);
					addClient(ci, clientID);  avList.add(new avatar(clientID, new Vector3f(0f, 0f, 0f)));
					System.out.println("Join request received from - " + clientID.toString());
					sendJoinedMessage(clientID, true);
					waitingForCreate = true;
					joinedClient = clientID;
					timeAtJoin = System.currentTimeMillis();
				} 
				catch (IOException e) 
				{	e.printStackTrace();
			}	}

			// Case where create didn't get received by server
			if(System.currentTimeMillis() - 1500.0 > timeAtJoin && waitingForCreate) {
				sendJoinedMessage(joinedClient, true);
				waitingForCreate = false;
			}
			
			// BYE -- Case where clients leaves the server
			// Received Message Format: (bye,localId)
			if(messageTokens[0].compareTo("bye") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				System.out.println("Exit request received from - " + clientID.toString());
				sendByeMessages(clientID);
				removeClient(clientID);
				for( avatar a: avList ) {
					if(a.getID().equals(clientID)) {
						avList.remove(a);
						break;
					}
				}
			}
			
			// CREATE -- Case where server receives a create message (to specify avatar location)
			// Received Message Format: (create,localId,x,y,z,color)
			if(messageTokens[0].compareTo("create") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]); System.out.println("\nCREATE MESSAGE");
				String[] pos = {messageTokens[2], messageTokens[3], messageTokens[4]};
				String color = messageTokens[5];
				for( avatar a: avList ) {
					if(a.getID().equals(clientID)) {
						if(color.equals("blue"))
							a.setTex("blue");
						else
							a.setTex("red");
						break;
					}
				}
				sendCreateMessages(clientID, pos, color);
				sendWantsDetailsMessages(clientID);
				if(clientID == joinedClient)
					waitingForCreate = false;

			}
			
			// DETAILS-FOR --- Case where server receives a details for message
			// Received Message Format: (dsfr,remoteId,localId,x,y,z,color)
			if(messageTokens[0].compareTo("dsfr") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				UUID remoteID = UUID.fromString(messageTokens[2]);
				String[] pos = {messageTokens[3], messageTokens[4], messageTokens[5]};
				String color = messageTokens[6];
				sendDetailsForMessage(clientID, remoteID, pos, color);
			}
			
			// MOVE --- Case where server receives a move message
			// Received Message Format: (move,localId,x,y,z)
			if(messageTokens[0].compareTo("move") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				String[] pos = {messageTokens[2], messageTokens[3], messageTokens[4]};
				sendMoveMessages(clientID, pos);

				System.out.println("avList Size: " + avList.size());

				for( avatar a: avList ) {
					if(a.getID().equals(clientID)) {
						a.setPos(new Vector3f(Float.parseFloat(pos[0]), Float.parseFloat(pos[1]), Float.parseFloat(pos[2])));
						break;
					}
				}

				target = null;
				//Set target to closest avatar in game
				float dist = 40.0f;
				Vector3f npcLoc = npcCtrl.getNPC().getLoc();
				for( avatar a : avList ) {
					if(npcLoc.distance(a.getPos()) < dist) {
						target = a;
						dist = npcLoc.distance(a.getPos());
					}
				}
			}

			// TURN --- Case where server receives a turn message
			// Received Message Format: (turn,localId,a,b,c,d)
			if(messageTokens[0].compareTo("turn") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				String[] vals = {messageTokens[2], messageTokens[3], messageTokens[4], messageTokens[5]};
				sendTurnMessages(clientID, vals);
			}

			// BALL --- Case where server receives a ball message
			// Received Message Format: (ball,remoteId,x,y,z,a,b,c)
			if(messageTokens[0].compareTo("ball") == 0)
			{	UUID clientID = UUID.fromString(messageTokens[1]);
				String[] vals = {messageTokens[2], messageTokens[3], messageTokens[4], messageTokens[5], messageTokens[6], messageTokens[7]};
				sendBallMessages(clientID, vals);
			}

			// -------- RECEIVING NPC MESSAGES SECTION --------------------

			// Case where server receives request for NPCs
			// Received Message Format: (needNPC,id)
			if(messageTokens[0].compareTo("needNPC") == 0)
			{	System.out.println("server got a needNPC message");
				UUID clientID = UUID.fromString(messageTokens[1]);
				sendNPCstart(clientID);
			}
			
			// DUMMYSWAP --- Case where the server receives notice of dummy & avatar positions swapping
			// Received Message Format: (dummySwap,dummyID,x,y,z)
			if(messageTokens[0].compareTo("dummySwap") == 0)
			{	System.out.println("server got a dummySwap message");
				UUID dummyID = UUID.fromString(messageTokens[1]);
				String[] pos = {messageTokens[2], messageTokens[3], messageTokens[4]};
				for( NPC n: npcCtrl.getDummyList() ) {
					if(n.getID().equals(dummyID))
						n.setLocation(Float.parseFloat(pos[0]), Float.parseFloat(pos[1]), Float.parseFloat(pos[2]));
				}
			}
		}	
	}

	// Informs the client who just requested to join the server if their if their 
	// request was able to be granted. 
	// Message Format: (join,success) or (join,failure)
	
	public void sendJoinedMessage(UUID clientID, boolean success)
	{	try 
		{	System.out.println("trying to confirm join");
			String message = new String("join,");
			if(success) {
				message += "success";
				System.out.println("Join Confirmed for ID: " + clientID);
			}
			else {
				message += "failure";
				System.out.println("Join Failed for ID: " + clientID);
			}
			sendPacket(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client that the avatar with the identifier remoteId has left the server. 
	// This message is meant to be sent to all client currently connected to the server 
	// when a client leaves the server.
	// Message Format: (bye,remoteId)
	
	public void sendByeMessages(UUID clientID)
	{	try 
		{	String message = new String("bye," + clientID.toString());
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client that a new avatar has joined the server with the unique identifier 
	// remoteId. This message is intended to be send to all clients currently connected to 
	// the server when a new client has joined the server and sent a create message to the 
	// server. This message also triggers WANTS_DETAILS messages to be sent to all client 
	// connected to the server. 
	// Message Format: (create,remoteId,x,y,z,color) where x, y, and z represent the position

	public void sendCreateMessages(UUID clientID, String[] position, String color)
	{	try 
		{	String message = new String("create," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + color;
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client of the details for a remote client�s avatar. This message is in response 
	// to the server receiving a DETAILS_FOR message from a remote client. That remote client�s 
	// message�s localId becomes the remoteId for this message, and the remote client�s message�s 
	// remoteId is used to send this message to the proper client. 
	// Message Format: (dsfr,remoteId,x,y,z,color) where x, y, and z represent the position.

	public void sendDetailsForMessage(UUID clientID, UUID remoteId, String[] position, String color)
	{	try 
		{	String message = new String("dsfr," + remoteId.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + color;
			sendPacket(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a local client that a remote client wants the local client�s avatar�s information. 
	// This message is meant to be sent to all clients connected to the server when a new client 
	// joins the server. 
	// Message Format: (wsds,remoteId)
	
	public void sendWantsDetailsMessages(UUID clientID)
	{	try 
		{	String message = new String("wsds," + clientID.toString());	
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs a client that a remote client�s avatar has changed position. x, y, and z represent 
	// the new position of the remote avatar. This message is meant to be forwarded to all clients
	// connected to the server when it receives a MOVE message from the remote client.   
	// Message Format: (move,remoteId,x,y,z) where x, y, and z represent the position.

	public void sendMoveMessages(UUID clientID, String[] position)
	{	try 
		{	String message = new String("move," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}

	// Informs a client that a remote client's avatar has changed it's rotation.
	// Message Format: (turn,remoteId,a,b,c,d) where a,b,c,d represent the values in the rotation matrix at
	// positions (0,0), (0,2), (2,0), (2,2)

	public void sendTurnMessages(UUID clientID, String[] vals)
	{	try 
		{	String message = new String("turn," + clientID.toString());
			message += "," + vals[0];
			message += "," + vals[1];
			message += "," + vals[2];
			message += "," + vals[3];
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}

	// Informs a client that a remote client has thrown a ball
	// Message Format: (ball,remoteId,x,y,z,a,b,c) where x,y,z are the starting location of the ball that was just thrown
	// and a,b,c is the initial velocity of the ball thrown

	public void sendBallMessages(UUID clientID, String[] vals)
	{	try 
		{	String message = new String("ball," + clientID.toString());
			message += "," + vals[0];
			message += "," + vals[1];
			message += "," + vals[2];
			message += "," + vals[3];
			message += "," + vals[4];
			message += "," + vals[5];
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
	}	}

	// ------------  SENDING NPC MESSAGES -----------------

	//Informs clients of the whereabouts of the NPCs. This message is intended to be sent
	//to any client at the time that they connect to the game.
	//Message Format: (createNPC,id,x,y,z) where x, y, and z represent the position

	public void sendCreateNPCmsg(UUID clientID, String[] position)
	{	try 
		{	System.out.println("server telling clients about an NPC");
			String message = new String("createNPC," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			forwardPacketToAll(message, clientID);
		} 
		catch (IOException e) 
		{	e.printStackTrace();
		}
	}
}
