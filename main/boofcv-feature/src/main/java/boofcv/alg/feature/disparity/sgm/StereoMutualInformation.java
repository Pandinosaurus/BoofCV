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

package boofcv.alg.feature.disparity.sgm;

import boofcv.alg.misc.ImageStatistics;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;

import java.util.Arrays;

/**
 * <p></p>Computes the Mutual Information error metric from a rectified stereo pair. Mutual information
 * between two images is defined as: MI(I1,I2) = H<sub>I1</sub> + H<sub>I2</sub> + H<sub>I1,I2</sub>.
 * Where H is an entropy function, e.g. H<sub>I</sub> = -sum_i P<sub>I</sub>(i)log(P<sub>I</sub>(i)),
 * where P<sub>I</sub>(i) is the probability of a pixel in image 'I' having that intensity. See [1]
 * for details.</p>
 *
 * <p>This implementation has been designed to handle images with pixel intensity greater than 8-bit.
 * That was done by allowing the number of bins in a histogram to be specified.
 * High dynamic range images are problematic because the joint entropy instead of being a 256x256 image might now be a
 * 4096x4096 image. The PDF for each image might also appear to be too flat.</p>
 *
 * <p>[1] Hirschmuller, Heiko. "Stereo processing by semiglobal matching and mutual information."
 * IEEE Transactions on pattern analysis and machine intelligence 30.2 (2007): 328-341.</p>
 *
 * @author Peter Abeles
 */
public class StereoMutualInformation {

	// Kernel that's used to apply smoothing to 1D and 2D
	Kernel1D_F32 smoothKernel;

	// Maximum intensity value a pixel can have
	int maxIntensityValue;
	// Storage for pixel intensity histogram after scaling
	int[] histogramIntensity;
	float[] probLeft;
	float[] probRight;
	float[] probSmoothed;

	float[] entropyLeft, entropyRight;

	GrayS32 histJoint = new GrayS32(1,1);
	GrayF32 probJoint = new GrayF32(1,1);

	public StereoMutualInformation() {
		// this is a reasonable default for 8-bit images
		configureHistogram(255,256);

		configureSmoothing(3);
	}

	public void configureHistogram(int maxIntensityValue , int intensityBins ) {
		probJoint.reshape(intensityBins,intensityBins);

		this.maxIntensityValue = maxIntensityValue;
		histogramIntensity = new int[intensityBins];
		probLeft = new float[intensityBins];
		probRight = new float[intensityBins];
		probSmoothed = new float[intensityBins];
		entropyLeft = new float[intensityBins];
		entropyRight = new float[intensityBins];
	}

	public void configureSmoothing( int radius ) {
		smoothKernel = FactoryKernelGaussian.gaussian(1,true,32,-1,radius);
	}

	public void process( GrayU8 left , GrayU8 right , GrayU8 disparity , int invalid ) {
		if( left.isSubimage() || right.isSubimage() || disparity.isSubimage() )
			throw new IllegalArgumentException("Can't process sub images. Is this a major issue? Could be fixed");

		computeJointHistogram(left, right, disparity, invalid);
		computeProbabilities();

		// TODO compute entropy
	}

	/**
	 * Computes the joint histogram of pixel intensities (2D histogram) while skipping over pixels with
	 * no correspondences
	 */
	private void computeJointHistogram(GrayU8 left, GrayU8 right, GrayU8 disparity, int invalid) {
		final int N = histogramIntensity.length;

		// Compute the joint histogram
		for (int row = 0; row < left.height; row++) {
			int idx = row*left.width;
			for (int col = 0; col < left.width; col++, idx++ ) {
				int d = disparity.data[idx]&0xFF;
				// Don't consider pixels without correspondences
				if( d != invalid ) {
					int leftValue = left.data[idx];     // I(x,y)
					int rightValue = right.data[idx-d]; // I(x-d,y)

					// scale the pixel intensity for the histogram
					leftValue = N*leftValue/maxIntensityValue;
					rightValue = N*rightValue/maxIntensityValue;

					// increment the histogram
					histJoint.data[leftValue*N+rightValue]++; // H(L,R) += 1
				}
			}
		}
	}

	/**
	 * Computes the joint and image specific probabilities using the joint histogram.
	 */
	private void computeProbabilities() {
		// Convert joint histogram into a joint probability
		float totalPixels = ImageStatistics.sum(histJoint);
		int histN = histJoint.width*histJoint.height;
		for (int i = 0; i < histN; i++) {
			probJoint.data[i] = histJoint.data[i]/totalPixels;
		}

		// Compute probabilities for left and right images by summing rows and columns of the joint probability
		Arrays.fill(probLeft,0.0f);
		Arrays.fill(probRight,0.0f);

		for (int row = 0; row < probJoint.height; row++) {
			int idx = row * probJoint.width;
			float sumRow = 0;
			for (int col = 0; col < probJoint.width; col++, idx++) {
				float v = probJoint.data[idx];
				sumRow += v;
				probRight[col] += v;
			}
			probLeft[row] = sumRow;
		}
	}
}
