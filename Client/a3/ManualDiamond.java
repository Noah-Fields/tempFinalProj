package a3;

import tage.*;
import tage.shapes.*;

public class ManualDiamond extends ManualObject
{
	private float[] vertices = new float[]
	{ -1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 2.0f, 0.0f,      //topfront
	1.0f, 0.0f, 1.0f, 1.0f, 0.0f, -1.0f, 0.0f, 2.0f, 0.0f,        //topright
	1.0f, 0.0f, -1.0f, -1.0f, 0.0f, -1.0f, 0.0f, 2.0f, 0.0f,      //topback
	-1.0f, 0.0f, -1.0f, -1.0f, 0.0f, 1.0f, 0.0f, 2.0f, 0.0f,      //topleft
	-1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, -2.0f, 0.0f,       //botfront
	1.0f, 0.0f, 1.0f, 1.0f, 0.0f, -1.0f, 0.0f, -2.0f, 0.0f,       //botright
	1.0f, 0.0f, -1.0f, -1.0f, 0.0f, -1.0f, 0.0f, -2.0f, 0.0f,     //botback
	-1.0f, 0.0f, -1.0f, -1.0f, 0.0f, 1.0f, 0.0f, -2.0f, 0.0f };   //botleft
		
	private float[] texcoords = new float[]
	{ 0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
	0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
	0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
	0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
	0.0f, 1.0f, 1.0f, 1.0f, 0.5f, 0.0f,
	0.0f, 1.0f, 1.0f, 1.0f, 0.5f, 0.0f,
	0.0f, 1.0f, 1.0f, 1.0f, 0.5f, 0.0f,
	0.0f, 1.0f, 1.0f, 1.0f, 0.5f, 0.0f };
		
	private float[] normals = new float[]
	{ 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
	1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f,
	0.0f, 1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, -1.0f,
	-1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f,
	0.0f, -1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, -1.0f, 1.0f,
	1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f,
	0.0f, -1.0f, -1.0f, 0.0f, -1.0f, -1.0f, 0.0f, -1.0f, -1.0f,
	-1.0f, -1.0f, 0.0f, -1.0f, -1.0f, 0.0f, -1.0f, -1.0f, 0.0f };

	public ManualDiamond()
	{	super();
		setNumVertices(24);
		setVertices(vertices);
		setTexCoords(texcoords);
		setNormals(normals);
		setMatAmb(Utils.silverAmbient());
		setMatDif(Utils.silverDiffuse());
		setMatSpe(Utils.silverSpecular());
		setMatShi(Utils.silverShininess());
	}
}