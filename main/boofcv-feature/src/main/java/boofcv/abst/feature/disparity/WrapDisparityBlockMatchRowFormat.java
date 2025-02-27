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

package boofcv.abst.feature.disparity;

import boofcv.alg.feature.disparity.DisparityBlockMatchRowFormat;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
public class WrapDisparityBlockMatchRowFormat<T extends ImageGray<T>, D extends ImageGray<D>>
		implements StereoDisparity<T,D>
{
	DisparityBlockMatchRowFormat<T,D> alg;
	D disparity;

	public WrapDisparityBlockMatchRowFormat(DisparityBlockMatchRowFormat<T,D> alg) {
		this.alg = alg;
	}

	@Override
	public void process(T imageLeft, T imageRight) {
		if( disparity == null || disparity.width != imageLeft.width || disparity.height != imageLeft.height )  {
			// make sure the image borders are marked as invalid
			disparity = GeneralizedImageOps.createSingleBand(alg.getDisparityType(),imageLeft.width,imageLeft.height);
			GImageMiscOps.fill(disparity, getMaxDisparity() + 1);
		}

		alg.process(imageLeft,imageRight,disparity);
	}

	public D getDisparity() {
		return disparity;
	}

	@Override
	public int getBorderX() {
		return alg.getBorderX();
	}

	@Override
	public int getBorderY() {
		return alg.getBorderY();
	}

	@Override
	public int getMinDisparity() {
		return alg.getMinDisparity();
	}

	@Override
	public int getMaxDisparity() {
		return alg.getMaxDisparity();
	}

	@Override
	public ImageType<T> getInputType() {
		return alg.getInputType();
	}

	@Override
	public Class<D> getDisparityType() {
		return alg.getDisparityType();
	}

	public DisparityBlockMatchRowFormat<T,D> getAlg() {
		return alg;
	}
}
