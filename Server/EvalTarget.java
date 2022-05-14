import tage.ai.behaviortrees.BTCondition;

public class EvalTarget extends BTCondition
{
	NPC npc;
	NPCcontroller npcc;
	GameServerUDP server;
  
	public EvalTarget(GameServerUDP s, NPCcontroller c, NPC n, boolean toNegate)
	{	super(toNegate);
		server = s;
		npcc = c;
		npc = n;
	}

	protected boolean check()
	{	
		if(npc.getSize()==1.0)
			return server.sendCheckForTarget();
		else
			return false;
	}
}
