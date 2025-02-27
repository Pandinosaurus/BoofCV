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

import boofcv.alg.feature.disparity.DisparityBlockMatch;
import boofcv.alg.feature.disparity.DisparitySparseScoreSadRect;
import boofcv.alg.feature.disparity.blockmatch.BlockRowScore;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;

/**
 * @author Peter Abeles
 */
public class TestImplDisparitySparseScoreSadRect_S16 extends ChecksImplDisparitySparseScoreSadRect<GrayS16,int[]>{

	public TestImplDisparitySparseScoreSadRect_S16() {
		super(GrayS16.class);
	}

	@Override
	public DisparityBlockMatch<GrayS16, GrayU8> createDense(int minDisparity, int maxDisparity,
															int radiusX, int radiusY, BlockRowScore scoreRow) {
		return new ImplDisparityScoreBM_S32<>(minDisparity,maxDisparity,radiusX,radiusY,scoreRow,
				new ImplSelectRectBasicWta_S32_U8());
	}

	@Override
	public DisparitySparseScoreSadRect<int[], GrayS16> createSparse(int minDisparity, int maxDisparity,
																	int radiusX, int radiusY) {
		return new ImplDisparitySparseScoreBM_SAD_S16(minDisparity,maxDisparity,radiusX,radiusY);
	}
}
