/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.blockmatch.impl;

import boofcv.alg.feature.disparity.DisparitySelect;
import boofcv.struct.image.GrayU8;

/**
 * @author Peter Abeles
 */
public class TestImplSelectRectBasicWta_F32_U8 extends BasicDisparitySelectRectTests<float[],GrayU8> {

	public TestImplSelectRectBasicWta_F32_U8() {
		super(float[].class,GrayU8.class);
	}

	@Override
	public DisparitySelect<float[],GrayU8> createAlg() {
		return new ImplSelectRectBasicWta_F32_U8();
	}
}
