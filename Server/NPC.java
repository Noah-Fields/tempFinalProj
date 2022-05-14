import java.util.Random;
import java.lang.Math;
import java.util.UUID;

import org.joml.*;

public class NPC
{
	UUID id;
	Random rnd = new Random();
	float locationX, locationY, locationZ;
	float radius;
	double size = 1.0;
	double rads = 2 * Math.PI * rnd.nextDouble();

	public NPC()
	{	locationX = 0.0f;
		locationY = 0.0f;
		locationZ = 0.0f;
		radius = 0.0f;
		id = UUID.randomUUID();
	}

	public void setLocation(float locX, float locY, float locZ) {
		locationX = locX;
		locationY = locY;
		locationZ = locZ;
		radius = (locX + locZ)/2;
	}

	public void randomizeLocation()
	{	locationX = ((rnd.nextFloat() + 2.0f) * ((float)(rnd.nextInt(40) - 20)));
		locationY = 1.4f;
		locationZ = ((rnd.nextFloat() + 2.0f) * ((float)(rnd.nextInt(40) - 20)));
	}

	public float getX() { return locationX; }
	public float getY() { return locationY; }
	public float getZ() { return locationZ; }
	public Vector3f getLoc() { return new Vector3f(locationX, locationY, locationZ); }

	public void getBig() { 
		size = 1.0;
	}
	public void getSmall() { size = 0.5; }
	public double getSize() { return size; }

	public UUID getID() { return id; }

	public void updateLocation() {	
		locationX = radius * (float)(Math.cos(rads));
		locationZ = radius * (float)(Math.sin(rads));
		rads = (rads + 0.005) % (2*Math.PI);
	}
}
