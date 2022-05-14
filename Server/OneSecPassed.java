import tage.ai.behaviortrees.BTCondition;

public class OneSecPassed extends BTCondition
{
	NPC npc;
	NPCcontroller npcc;
	long lastUpdateTime;
  
	public OneSecPassed(NPCcontroller c, NPC n, boolean toNegate)
	{	super(toNegate);
		npcc = c;
		npc = n;
		lastUpdateTime = System.nanoTime();
	}	

	protected boolean check()
	{	float elapsedMilliSecs = (System.nanoTime()-lastUpdateTime)/(1000000.0f);
		if ((elapsedMilliSecs >= 3000.0f) && (npc.getSize()==0.5))
		{	lastUpdateTime = System.nanoTime();
			return true;
		}
		else return false;
	}
}
