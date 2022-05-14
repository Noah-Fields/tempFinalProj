import java.util.Random;
import java.util.UUID;
import java.util.ArrayList;
import tage.ai.behaviortrees.*;

public class NPCcontroller
{
	private NPC npc;
	private ArrayList<NPC> dummyList;
	private int numDummys = 10;
	Random rnd = new Random();
	BehaviorTree bt = new BehaviorTree(BTCompositeType.SELECTOR);
	long thinkStartTime, tickStartTime, lastThinkUpdateTime, lastTickUpdateTime;
	GameServerUDP server;
	float criteria = 2.0f;

	public void updateNPCs()
	{	npc.updateLocation();
	}

	public void start(GameServerUDP s)
	{	thinkStartTime = System.nanoTime();
		tickStartTime = System.nanoTime();
		lastThinkUpdateTime = thinkStartTime;
		lastTickUpdateTime = tickStartTime;
		server = s;
		setupNPCs();
		setupBehaviorTree();
		npcLoop();
	}

	public NPC getNPC() { return npc; }

	public void setupNPCs() 
	{	npc = new NPC();
		dummyList = new ArrayList<NPC>();
		npc.setLocation(rnd.nextInt(20) + 10, 5, rnd.nextInt(20) + 10);
		for(int i = 0; i < numDummys; i++) {
			NPC tempNpc = new NPC();
			tempNpc.randomizeLocation();
			dummyList.add(tempNpc);
		}
	}

	public int getNumDummys() { return numDummys; }
	public ArrayList<NPC> getDummyList() { return dummyList; }

	// NPC moves in circle around center of arena, every 1.25 seconds the behavior tree is evaluated.
	// if 3 seconds have passed, 

	public void npcLoop()
	{	while (true)
		{	long currentTime = System.nanoTime();
			float elapsedThinkMilliSecs = (currentTime-lastThinkUpdateTime)/(1000000.0f);
			float elapsedTickMilliSecs = (currentTime-lastTickUpdateTime)/(1000000.0f);

			if (elapsedTickMilliSecs >= 25.0f)
			{	lastTickUpdateTime = currentTime;
				npc.updateLocation();
				server.sendNPCinfo();
				//System.out.println("tick");
			}
	  
			if (elapsedThinkMilliSecs >= 1250.0f)
			{	lastThinkUpdateTime = currentTime;
				bt.update(elapsedThinkMilliSecs);
				//System.out.println("----------- THINK ------------");
			}
			Thread.yield();
		}
	}

	public void setupBehaviorTree()
	{	bt.insertAtRoot(new BTSequence(10));
		bt.insertAtRoot(new BTSequence(20));
		bt.insert(10, new OneSecPassed(this,npc,false));
		bt.insert(10, new GetBig(npc));
		bt.insert(20, new EvalTarget(server,this,npc,false));
		bt.insert(20, new GetSmall(npc));
	}
}
