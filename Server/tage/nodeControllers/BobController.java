package tage.nodeControllers;
import tage.*;
import org.joml.*;

/**
* A BobController is a node controller that, when enabled, causes any object
* it is attached to to bob up and down in place.
*/
public class BobController extends NodeController
{
	private float bobSpeed = 1.0f;
    private float timePassedSinceSwitch = 0;
	private Vector3f curLocation;
	private Engine engine;

	/** Creates a bob controller with speed=1.0. */
	public BobController() { super(); }

	/** Creates a bob controller with speed as specified. */
	public BobController(Engine e, float speed)
	{	super();
		bobSpeed = speed;
		engine = e;
	}

	/** sets the bob speed when the controller is enabled */
	public void setSpeed(float s) { bobSpeed = s; }

	/** This is called automatically by the RenderSystem (via SceneGraph) once per frame
	*   during display().  It is for engine use and should not be called by the application.
	*/
	public void apply(GameObject go)
	{	float elapsedTimeStart = super.getElapsedTimeTotal();
		float elapsedTime = super.getElapsedTime();
		curLocation = go.getLocalLocation();
        if(elapsedTimeStart - timePassedSinceSwitch > 500.0f) {
            bobSpeed *= -1;
            timePassedSinceSwitch = elapsedTimeStart;
        }
		float bobAmt = elapsedTime * bobSpeed;
		Vector3f newLocation = new Vector3f(curLocation.x(), curLocation.y()+bobAmt, curLocation.z());
		go.setLocalLocation(newLocation);
	}
}