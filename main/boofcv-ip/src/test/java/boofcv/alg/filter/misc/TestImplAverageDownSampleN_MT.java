/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.filter.misc;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

class TestImplAverageDownSampleN_MT extends CompareIdenticalFunctions
{
	int width = 640,height=480;

	protected TestImplAverageDownSampleN_MT() {
		super(ImplAverageDownSampleN_MT.class, ImplAverageDownSampleN.class);
	}

	@Test
	void performTests() {
		super.performTests(7);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class[] inputTypes = candidate.getParameterTypes();

		ImageBase input = GeneralizedImageOps.createImage(inputTypes[0],width,height,1);
		ImageBase output = GeneralizedImageOps.createImage(inputTypes[2],width/3,height/3,1);

		GImageMiscOps.fillUniform(input,rand,0,255);
		GImageMiscOps.fillUniform(output,rand,0,100);

		return new Object[][]{{input, 3, output}};
	}
}

