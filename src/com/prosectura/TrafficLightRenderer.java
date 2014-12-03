package com.prosectura;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.opengl.GLSurfaceView.Renderer;
import android.os.SystemClock;


public class TrafficLightRenderer implements Renderer {
	/*Store our model data in a float buffer*/
	private final FloatBuffer mTriangle1Vertices;
	
	private float[] mViewMatrix = new float[16];

	/*How many bytes per float*/
	private final int mBytesPerFloat = 4;

	final String vertexShader =
			 "uniform mat4 u_MVPMatrix;\n" //A constant representing the combined model/view/projection matrix
			+"attribute vec4 a_Position;\n" //Per-vertex position information we will pass in.
			+"attribute vec4 a_Color;\n" //Per-vertex color information we will pass in.
			+"varying vec4 v_Color;\n" //This will be passed into the fragment shader.
			+"void main()\n"  //The entry point for our vertex shader.
			+"{\n"
			+"  v_Color = a_Color;\n" //Pass the color through to the fragment shader. It will be interpolated across the triangle.
			+"  gl_Position = u_MVPMatrix * a_Position;\n" //gl_Position is a special variable used to store the final position. Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
			+"}\n";
			
	final String fragmentShader =
			 "precision mediump float;\n" //Set the default precision to medium. We don't need as high of a precision in the fragment shader.
			+"varying vec4 v_Color;\n" //This is the color from the vertex shader interpolated across the triangle per fragment.
			+"void main()\n"
			+"{\n"
			+"  gl_FragColor = v_Color;\n" //Pass the color directly through the pipeline.
			+"}\n";
	
	
	
	
	
	
	
	
	
	//This will be used to pass in the transformation matrix.
	private int mMVPMatrixHandle;
	//This will be used to pass in model position information.
	private int mPositionHandle;
	//This will be used to pass in model color information.
	private int mColorHandle;
	
	//Store the projection matrix. This is used to project the scene onto a 2D viewport.
	private float[] mProjectionMatrix = new float[16];
	
	//Store the model matrix. This matrix is used to move models from object space (where each model can be thought of being located at the center of the universe) to world space.
	private float[] mModelMatrix = new float[16];
	
	//Allocate storage for the final combined matrix. This will be passed into the shader program.
	private float[] mMVPMatrix = new float[16];
	
	//How many elements per vertex
	private final int mStrideBytes = 7 * mBytesPerFloat;
	
	//Offset of the position date
	private final int mPositionOffset = 0;
	
	//Size of the position data in elements.
	private final int mPositionDataSize = 3;
	
	//Offset of the color data.
	private final int mColorOffset = 3;
	
	//Size of the color data in elements.
	private final int mColorDataSize = 4;
	
	public TrafficLightRenderer() {
		final float[] triangle1VerticesData = {
				//X,Y,Z,  R,G,B,A
				-0.5f, -0.25f, 0.0f,  1.0f, 0.0f, 0.0f, 1.0f,
				 0.5f, -0.25f, 0.0f,  0.0f, 0.0f, 1.0f, 1.0f,
				 0.0f, 0.559016994f, 0.0f,  0.0f, 1.0f, 0.0f, 1.0f
		};
		mTriangle1Vertices = ByteBuffer.allocateDirect(triangle1VerticesData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
		mTriangle1Vertices.put(triangle1VerticesData).position(0);
	}
	
	private int LoadShader(final String shaderSource, final int shaderType)
	{
		int shaderHandle = GLES20.glCreateShader(shaderType);
		
		if (shaderHandle != 0)
		{
			//Pass in the shader source.
			GLES20.glShaderSource(shaderHandle, shaderSource);
			
			//Compile the shader.
			GLES20.glCompileShader(shaderHandle);
			
			//Get the compilation status.
			final int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
			
			//If the compilation failed, delete the shader.
			if (compileStatus[0] == 0)
			{
				GLES20.glDeleteShader(shaderHandle);
				shaderHandle = 0;
			}
		}

		if (shaderHandle == 0)
			throw new RuntimeException("Error creating vertex shader.");
		
		return shaderHandle;
	}
	
	@Override
	public void onSurfaceCreated(GL10 notUsed, EGLConfig config) {
		//Set the background color to gray.
		GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
		
		//Position the eye behind the origin.
		final float eyeX = 0.0f;
		final float eyeY = 0.0f;
		final float eyeZ = 1.5f;
		
		//We are looking toward the distance
		final float centerX = 0.0f;
		final float centerY = 0.0f;
		final float centerZ = -0.5f;
		
		//Set our up vector. This is where our head would be pointing were we holding the camera.
		final float upX = 0.0f;
		final float upY = 1.0f;
		final float upZ = 0.0f;
		
		//Set the view matrix. This matrix can be said to represent the camera position.
		//NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
		Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
		
		//Load in the vertex shader.
		int vertexShaderHandle = 0;
		int fragmentShaderHandle = 0;
		try {
			vertexShaderHandle = LoadShader(vertexShader, GLES20.GL_VERTEX_SHADER);
			fragmentShaderHandle = LoadShader(fragmentShader, GLES20.GL_FRAGMENT_SHADER);
		} catch (RuntimeException e) {
			throw e;
		}
		
		//Create a program object and store the handle to it.
		int programHandle = GLES20.glCreateProgram();
		
		if (programHandle != 0)
		{
			//Bind the vertex shader to the program.
			GLES20.glAttachShader(programHandle, vertexShaderHandle);
			//Bind the fragment shader to the program.
			GLES20.glAttachShader(programHandle, fragmentShaderHandle);
			
			//Bind attributes
			GLES20.glBindAttribLocation(programHandle, 0, "a_Position");
			GLES20.glBindAttribLocation(programHandle, 1, "a_Color");
			
			//Link the two shaders together into a program.
			GLES20.glLinkProgram(programHandle);
			
			//Get the link status
			final int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
			
			//If the link failed, delete the program.
			if (linkStatus[0] == 0)
			{
				GLES20.glDeleteProgram(programHandle);
				programHandle = 0;
			}
		}
		
		if (programHandle == 0)
			throw new RuntimeException("Error creating program");
		
		// Set program handles. These will later be used to pass in values to the program.
		mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");
		mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");
		mColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color");
		
		//Tell OpenGL to use this program when rendering.
		GLES20.glUseProgram(programHandle);
	}
	

	@Override
	public void onSurfaceChanged(GL10 notUsed, int width, int height) {
		//Set the OpenGL viewport to the same size as the surface
		GLES20.glViewport(0, 0, width, height);
		
		//Create a new perspective projection matrix. The height will stay the same while the width will vary as per aspect ratio.
		final float ratio = (float)width / height;
		final float left = -ratio;
		final float right = ratio;
		final float bottom = -1.0f;
		final float top = 1.0f;
		final float near = 1.0f;
		final float far = 10.0f;
		
		Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
	}

	@Override
	public void onDrawFrame(GL10 notUsed) {
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
		
		//Do a complete rotation every 10 seconds.
		long time = SystemClock.uptimeMillis() % 10000L;
		float angleInDegrees = (360.0f / 10000.0f) * (int)time;
		
		//Draw the triangle facing straight on.
		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 0.0f, 1.0f);
		
		drawTriangle(mTriangle1Vertices);
	}
	
	/**
	 * Draws a triangle from a given vertex data.
	 * 
	 * @param aTriangleBUffer The buffer containing the vertex data.
	 */
	private void drawTriangle(final FloatBuffer aTriangleBuffer)
	{
		//Pass in the position information
		aTriangleBuffer.position(mPositionOffset);
		GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false, mStrideBytes, aTriangleBuffer);
		GLES20.glEnableVertexAttribArray(mPositionHandle);
		
		//Pass in the color information
		aTriangleBuffer.position(mColorOffset);
		GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false, mStrideBytes, aTriangleBuffer);
		GLES20.glEnableVertexAttribArray(mColorHandle);
		
		//This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix (which currently contains model*view)
		Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
		
		//This multiplies the modelview by the projection matrix, and stores the result in the MVP matrix (which now contains model*view*projection).
		Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
		
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
	}

}
