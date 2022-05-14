package tage;
import static com.jogamp.opengl.GL4.*;

import java.util.ArrayList;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.gl2.GLUT;
import org.joml.*;

/**
* Manages any number of HUD strings, implemented as GLUT strings.
* This class is instantiated automatically by the engine.
* Note that this class utilizes deprectated OpenGL functionality.
* <p>
* The available fonts are:
* <ul>
* <li> GLUT.BITMAP_8_BY_13
* <li> GLUT.BITMAP_9_BY_15
* <li> GLUT.BITMAP_TIMES_ROMAN_10
* <li> GLUT.BITMAP_TIMES_ROMAN_24
* <li> GLUT.BITMAP_HELVETICA_10
* <li> GLUT.BITMAP_HELVETICA_12
* <li> GLUT.BITMAP_HELVETICA_18
* </ul>
* <p>
* This class includes a "kludge".  On many systems, GLUT strings ignore the glColor
* setting and uses the most recent color rendered.  Therefore, this HUD
* renderer first renders a single pixel at the desired HUD color at a
* distant location, before drawing the HUD.
* @author Scott Gordon
*/

public class HUDmanager
{	private GLCanvas myCanvas;
	private GLUT glut = new GLUT();
	private Engine engine;

	private class HUD {
		public int HUDfont;
		public int HUDx, HUDy;
		public int hudColorProgram;
		public float HUDcolor[];
		public String HUDString;
		public String HUDName;

		public HUD() {
			HUDfont = GLUT.BITMAP_TIMES_ROMAN_24;
			HUDx = 0; HUDy = 0;
			HUDcolor = new float[3];
			HUDString = "";
			HUDName = "";
		}
	}

	private ArrayList<HUD> HUDS = new ArrayList<HUD>();

	// The constructor is called by the engine, and should not be called by the game application.

	protected HUDmanager(Engine e)
	{	engine = e;
		HUDS = new ArrayList<HUD>();
	}
	
	protected void setGLcanvas(GLCanvas g) { myCanvas = g; }

	protected void drawHUDs(int hcp) {	
		//GL4 gl = (GL4) GLContext.getCurrentGL();
		GL4 gl4 = myCanvas.getGL().getGL4();
		GL4bc gl4bc = (GL4bc) gl4;
		for(HUD h : HUDS) {
			gl4bc.glWindowPos2d (h.HUDx, h.HUDy);
			prepHUDcolor(h.HUDcolor, hcp);
			glut.glutBitmapString(h.HUDfont, h.HUDString);
		}
	}

	/** adds HUD to list of HUDS with the specified name, text string, color, and location */
	public void addHUD(String name, String string, Vector3f color, int x, int y) {
		HUD hud = new HUD();
		hud.HUDName = name;
		hud.HUDString = string;
		hud.HUDcolor[0]=color.x(); hud.HUDcolor[1]=color.y(); hud.HUDcolor[2]=color.z();
		hud.HUDx = x;
		hud.HUDy = y;
		HUDS.add(hud);
	}

	/** updates existing HUD specified by name to the given text string, color, and location */
	public void setHUD(String name, String string, Vector3f color, int x, int y) {
		for(HUD h : HUDS) {
			if(h.HUDName == name) {
				h.HUDString = string;
				h.HUDcolor[0]=color.x(); h.HUDcolor[1]=color.y(); h.HUDcolor[2]=color.z();
				h.HUDx = x; h.HUDy = y;
			}
		}
	}

	/** removes existing HUD specified by name */
	public void remHUD(String name) {
		int index = 0;
		boolean hudFound = false;
		for(HUD h : HUDS) {
			if(h.HUDName == name) {
				index = HUDS.indexOf(h);
				hudFound = true;
			}
		}
		if(hudFound)
			HUDS.remove(index);
		else
			System.out.println("HUD named" + name + " does not exist\n");
	}

	/** sets HUD of given name to specified font - available fonts are listed above. */
	public void setHUDfont(String name, int font) { 
		for(HUD h : HUDS) {
			if(h.HUDName == name)
				h.HUDfont = font;
		}
	}

	// Kludge to ensure HUD renders with correct color - do not call from game application.
	// Draws a single dot at a distant location to set the desired HUD color.
	// Used internally by the renderer.

	private void prepHUDcolor(float[] color, int hcp)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glUseProgram(hcp);
		int hudCLoc = gl.glGetUniformLocation(hcp, "hudc");
		gl.glProgramUniform3fv(hcp, hudCLoc, 1, color, 0);
		gl.glDrawArrays(GL_POINTS,0,1);
	}
}