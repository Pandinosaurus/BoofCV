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

package boofcv.factory.transform.census;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.abst.transform.census.FilterCensusTransformD33U8;
import boofcv.abst.transform.census.FilterCensusTransformD55S32;
import boofcv.abst.transform.census.FilterCensusTransformSampleS64;
import boofcv.alg.transform.census.CensusTransform;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

/**
 * Factory for creating different types of census transforms
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class FactoryCensusTransform {
	/**
	 * Samples a dense square block
	 *
	 * @param radius Radius of the block. Width = 2*radius+1
	 * @param imageType Type of input image
	 * @param <In> Input image
	 * @param <Out> Output image
	 * @return Census Transform
	 */
	public static <In extends ImageGray<In>, Out extends ImageBase<Out>>
	FilterImageInterface<In, Out> blockDense( int radius , Class<In> imageType) {
		switch( radius ) {
			case 1:
				return new FilterCensusTransformD33U8(FactoryImageBorder.single(BorderType.ZERO,imageType), imageType);
			case 2:
				return new FilterCensusTransformD55S32(FactoryImageBorder.single(BorderType.ZERO,imageType), imageType);
			case 3: {
				FastQueue<Point2D_I32> points7x7 = CensusTransform.createBlockSamples(3);
				return new FilterCensusTransformSampleS64(points7x7,FactoryImageBorder.single(BorderType.ZERO,imageType), imageType);
			}

			default:
				throw new IllegalArgumentException("Currently only radius 1 to 3 is supported");
		}
	}
}
