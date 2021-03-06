/**
 * Copyright 2013 Dennis Ippel
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package rajawali.materials;

import rajawali.lights.ALight;
import rajawali.math.vector.Vector3;
import android.graphics.Color;
import android.opengl.GLES20;

import com.monyetmabuk.livewallpapers.photosdof.R;


public class PhongMaterial extends AAdvancedMaterial {

	protected int muSpecularColorHandle;
	protected int muShininessHandle;

	protected float[] mSpecularColor;
	protected float mShininess;

	public PhongMaterial() {
		this(R.raw.phong_material_vertex, R.raw.phong_material_fragment);
		mSpecularColor = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		mShininess = 96.0f;
	}

	public PhongMaterial(int vertex_resID, int fragment_resID) {
		super(vertex_resID, fragment_resID);
		mSpecularColor = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		mShininess = 96.0f;
	}

	public PhongMaterial(String vertexShader, String fragmentShader) {
		super(vertexShader, fragmentShader);
		mSpecularColor = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		mShininess = 96.0f;
	}

	public PhongMaterial(float[] specularColor, float[] ambientColor, float shininess) {
		this();
		mSpecularColor = specularColor;
		mAmbientColor = ambientColor;
		mShininess = shininess;
	}

	@Override
	public void useProgram() {
		super.useProgram();
		GLES20.glUniform4fv(muSpecularColorHandle, 1, mSpecularColor, 0);
		GLES20.glUniform1f(muShininessHandle, mShininess);
	}

	public void setSpecularColor(float[] color) {
		mSpecularColor = color;
	}

	public void setSpecularColor(Vector3 color) {
		mSpecularColor[0] = (float) color.x;
		mSpecularColor[1] = (float) color.y;
		mSpecularColor[2] = (float) color.z;
		mSpecularColor[3] = 1;
	}

	public void setSpecularColor(float r, float g, float b, float a) {
		setSpecularColor(new float[] { r, g, b, a });
	}

	public void setSpecularColor(int color) {
		setSpecularColor(new float[] { Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color) });
	}

	public void setShininess(float shininess) {
		mShininess = shininess;
	}

	@Override
	public void setShaders(String vertexShader, String fragmentShader)
	{
		StringBuffer fc = new StringBuffer();
		StringBuffer vc = new StringBuffer();
		
		for(int i=0; i<mLights.size(); ++i) {
			ALight light = mLights.get(i);

			if(light.getLightType() == ALight.POINT_LIGHT) {
				vc.append("dist = distance(-vEyeVec, uLightPosition").append(i).append(");\n");
				vc.append("vAttenuation").append(i).append(" = 1.0 / (uLightAttenuation").append(i).append("[1] + uLightAttenuation").append(i).append("[2] * dist + uLightAttenuation").append(i).append("[3] * dist * dist);\n");
				fc.append("L = normalize(uLightPosition").append(i).append(" + vEyeVec);\n");
			} else if(light.getLightType() == ALight.SPOT_LIGHT) {
				vc.append("dist = distance(-vEyeVec, uLightPosition").append(i).append(");\n");
				vc.append("vAttenuation").append(i).append(" = 1.0 / (uLightAttenuation").append(i).append("[1] + uLightAttenuation").append(i).append("[2] * dist + uLightAttenuation").append(i).append("[3] * dist * dist);\n");
				fc.append("L = normalize(uLightPosition").append(i).append(" + vEyeVec);\n");
				fc.append("vec3 spotDir").append(i).append(" = normalize(-uLightDirection").append(i).append(");\n");
				fc.append("float spot_factor").append(i).append(" = dot( L, spotDir").append(i).append(" );\n");
				fc.append("if( uSpotCutoffAngle").append(i).append(" < 180.0 ) {\n");
					fc.append("if( spot_factor").append(i).append(" >= cos( radians( uSpotCutoffAngle").append(i).append(") ) ) {\n");
						fc.append("spot_factor").append(i).append(" = (1.0 - (1.0 - spot_factor").append(i).append(") * 1.0/(1.0 - cos( radians( uSpotCutoffAngle").append(i).append("))));\n");
						fc.append("spot_factor").append(i).append(" = pow(spot_factor").append(i).append(", uSpotFalloff").append(i).append("* 1.0/spot_factor").append(i).append(");\n");
					fc.append("}\n");
					fc.append("else {\n");
						fc.append("spot_factor").append(i).append(" = 0.0;\n");
					fc.append("}\n");
					fc.append("L = vec3(L.x, L.y, L.z) * spot_factor").append(i).append(";\n");
					fc.append("}\n");
			} else if(light.getLightType() == ALight.DIRECTIONAL_LIGHT) {
				vc.append("vAttenuation").append(i).append(" = 1.0;\n");
				fc.append("L = normalize(-uLightDirection").append(i).append(");\n");
			}

			fc.append("NdotL = max(dot(N, L), 0.1);\n");
			fc.append("power = uLightPower").append(i).append(" * NdotL * vAttenuation").append(i).append(";\n");
			fc.append("intensity += power;\n"); 
			fc.append("Kd.rgb += uLightColor").append(i).append(" * power;\n"); 
			fc.append("Ks += pow(NdotL, uShininess) * vAttenuation").append(i).append(" * uLightPower").append(i).append(";\n");
		}
		super.setShaders(
				vertexShader.replace("%LIGHT_CODE%", vc.toString()), 
				fragmentShader.replace("%LIGHT_CODE%", fc.toString())
				);

		muSpecularColorHandle = getUniformLocation("uSpecularColor");
		muShininessHandle = getUniformLocation("uShininess");
	}
}
