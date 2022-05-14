package tage.nodeControllers;
import tage.*;
import org.joml.*;

import a3.BallObject;

import java.lang.Math;

/**
* An OrbitController is a node controller that, when enabled, causes any object
* it is attached to to bob up and down in place.
*/
public class OrbitController extends NodeController
{
	private float orbitRadius = 1.0f;
	private float rads = 0;
	private int orbitSpacing = 1; //This value helps to create an equal spacing between objects orbitting with each other (usually number of objects orbitting together)
	private Vector3f curLocation;
	private Engine engine;

	/** Creates an orbit controller */
	public OrbitController() { super(); }

	/** Creates an orbit controller with radius & spacing as specified. */
	public OrbitController(Engine e, float radius, int spacing)
	{	super();
		orbitRadius = radius;
		orbitSpacing = spacing;
		engine = e;
	}

	/** sets the orbit radius or spacing when the controller is enabled */
	public void setRadius(float r) { orbitRadius = r; }
	public void setSpacing(int s) { orbitSpacing = s; }

	/** This is called automatically by the RenderSystem (via SceneGraph) once per frame
	*   during display().  It is for engine use and should not be called by the application.
	*/
	public void apply(GameObject go)
	{	float elapsedTimeStart = super.getElapsedTimeTotal();
		float elapsedTime = super.getElapsedTime();
		curLocation = go.getLocalLocation();
		BallObject tempB = (BallObject)go;

		Vector3f newLocation = new Vector3f((float)Math.cos(rads + (((float)(2 * Math.PI))/orbitSpacing) * tempB.getId()) * orbitRadius, 
											curLocation.y(), 
											(float)Math.sin(rads + (((float)(2 * Math.PI))/orbitSpacing) * tempB.getId()) * orbitRadius);
		
		rads = (float)((rads + 0.001) % ((float)(2 * Math.PI)));
		go.setLocalLocation(newLocation);
	}
}