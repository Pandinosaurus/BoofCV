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

package boofcv.alg.transform.census;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.transform.census.impl.ImplCensusTransformBorder;
import boofcv.alg.transform.census.impl.ImplCensusTransformInner;
import boofcv.alg.transform.census.impl.ImplCensusTransformInner_MT;
import boofcv.concurrency.BoofConcurrency;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import javax.annotation.Nullable;

/**
 * <p>The Census Transform [1] computes a bit mask for each pixel in the image. If a neighboring pixel is greater than the
 * center pixel in a region that bit is set to 1. A 3x3 region 9radius=1) is encoded in 8-bits and a 5x5 region
 * (radius=2) in 24-bits. To compute the error between two pixels simply compute the hamming distance. The
 * hamming distance for an input can be computed using DescriptorDistance.hamming().</p>
 *
 * <p>DEVELOPMENT NOTE: See if this can be speed up by only comparing each pixel with another once.
 * Code will be complex</p>
 *
 * <p>
 * [1] Zabih, Ramin, and John Woodfill. "Non-parametric local transforms for computing visual correspondence."
 * European conference on computer vision. Springer, Berlin, Heidelberg, 1994.
 * </p>
 *
 * @author Peter Abeles
 */
public class CensusTransform {

//	public int error( short[] values , int offset , int length ) {
//		int total = 0;
//		for (int i = 0; i < length; i++) {
//			total +=
//		}
//	}

	public static FastQueue<Point2D_I32> createBlockSamples( int radius ) {
		FastQueue<Point2D_I32> samples = new FastQueue<>(Point2D_I32.class,true);
		int w = radius*2+1;
		samples.growArray(w*w-1);

		for (int y = -radius; y <= radius; y++) {
			for (int x = -radius; x <= radius; x++) {
				samples.grow().set(x,y);
			}
		}

		return samples;
	}

	public static<T extends ImageGray<T>> void dense3x3(final T input , final GrayU8 output ,
														@Nullable ImageBorder<T> border )
	{
		if( input.getClass() == GrayU8.class ) {
			dense3x3_U8((GrayU8)input,output,(ImageBorder_S32)border);
		}
	}

	public static<T extends ImageGray<T>> void dense5x5(final T input , final GrayS32 output ,
														@Nullable ImageBorder<T> border )
	{
		if( input.getClass() == GrayU8.class ) {
			dense5x5_U8((GrayU8)input,output,(ImageBorder_S32)border);
		}
	}

	public static<T extends ImageGray<T>> void sample(final T input , final FastQueue<Point2D_I32> sample,
													  final GrayS64 output ,
													  @Nullable ImageBorder<T> border, @Nullable GrowQueue_I32 workSpace )
	{
		if( input.getClass() == GrayU8.class ) {
			sample_S64((GrayU8)input,sample,output,(ImageBorder_S32)border,workSpace);
		}
	}

	public static<T extends ImageGray<T>> void sample(final T input , final FastQueue<Point2D_I32> sample,
													  final InterleavedU16 output ,
													  @Nullable ImageBorder<T> border, @Nullable GrowQueue_I32 workSpace )
	{
		if( input.getClass() == GrayU8.class ) {
			sample_U8((GrayU8)input,sample,output,(ImageBorder_S32)border,workSpace);
		}
	}

	/**
	 * Census transform for local 3x3 region around each pixel.
	 *
	 * @param input Input image
	 * @param output Census transformed output image
	 * @param border (Nullable) How the border is handled
	 */
	public static void dense3x3_U8(final GrayU8 input , final GrayU8 output , @Nullable ImageBorder_S32<GrayU8> border ) {
		InputSanityCheck.checkReshape(input,output);

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplCensusTransformInner_MT.dense3x3_U8(input,output);
		} else {
			ImplCensusTransformInner.dense3x3_U8(input,output);
		}

		if( border != null ) {
			border.setImage(input);
			ImplCensusTransformBorder.dense3x3_U8(border,output);
		}
	}

	/**
	 * Census transform for local 5x5 region around each pixel.
	 *
	 * @param input Input image
	 * @param output Census transformed output image
	 * @param border (Nullable) How the border is handled
	 */
	public static void dense5x5_U8(final GrayU8 input , final GrayS32 output , @Nullable ImageBorder_S32<GrayU8> border ) {
		InputSanityCheck.checkReshape(input,output);

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplCensusTransformInner_MT.dense5x5_U8(input,output);
		} else {
			ImplCensusTransformInner.dense5x5_U8(input,output);
		}

		if( border != null ) {
			border.setImage(input);
			ImplCensusTransformBorder.dense5x5_U8(border,output);
		}
	}

	/**
	 * Census transform for an arbitrary region specified by the provided sample points
	 *
	 * @param input Input image
	 * @param sample Relative coordinates that are sampled when computing the
	 * @param output Census transformed output image
	 * @param border (Nullable) How the border is handled
	 */
	public static void sample_S64(final GrayU8 input , final FastQueue<Point2D_I32> sample,
								 final GrayS64 output , @Nullable ImageBorder_S32<GrayU8> border ,
								 @Nullable GrowQueue_I32 workSpace ) {
		output.reshape(input.width,input.height);

		// The farthest away
		int radius = 0;
		// Precompute the offset in array indexes for the sample points
		if( workSpace == null )
			workSpace = new GrowQueue_I32();
		workSpace.resize(sample.size);
		for (int i = 0; i < sample.size; i++) {
			Point2D_I32 p = sample.get(i);
			workSpace.data[i] = p.y*input.stride + p.x;
			radius = Math.max(radius,Math.abs(p.x));
			radius = Math.max(radius,Math.abs(p.y));
		}

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplCensusTransformInner_MT.sample_S64(input,radius,workSpace,output);
		} else {
			ImplCensusTransformInner.sample_S64(input,radius,workSpace,output);
		}

		if( border != null ) {
			border.setImage(input);
			ImplCensusTransformBorder.sample_S64(border,radius,sample,output);
		}
	}

	/**
	 * Census transform for an arbitrary region specified by the provided sample points
	 *
	 * @param input Input image
	 * @param sample Relative coordinates that are sampled when computing the
	 * @param output Census transformed output image
	 * @param border (Nullable) How the border is handled
	 */
	public static void sample_U8(final GrayU8 input , final FastQueue<Point2D_I32> sample,
								 final InterleavedU16 output , @Nullable ImageBorder_S32<GrayU8> border ,
								 @Nullable GrowQueue_I32 workSpace ) {
		// Compute the number of 16-bit values that are needed to store
		int numBlocks = BoofMiscOps.bitsToWords(sample.size,16);
		output.reshape(input.width,input.height,numBlocks);

		// The farthest away
		int radius = 0;
		// Precompute the offset in array indexes for the sample points
		if( workSpace == null )
			workSpace = new GrowQueue_I32();
		workSpace.resize(sample.size);
		for (int i = 0; i < sample.size; i++) {
			Point2D_I32 p = sample.get(i);
			workSpace.data[i] = p.y*input.stride + p.x;
			radius = Math.max(radius,Math.abs(p.x));
			radius = Math.max(radius,Math.abs(p.y));
		}

		if( BoofConcurrency.USE_CONCURRENT ) {
			ImplCensusTransformInner_MT.sample_IU16(input,radius,workSpace,output);
		} else {
			ImplCensusTransformInner.sample_IU16(input,radius,workSpace,output);
		}

		if( border != null ) {
			border.setImage(input);
			ImplCensusTransformBorder.sample_IU16(border,radius,sample,output);
		}
	}
}
