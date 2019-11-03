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

package boofcv.factory.feature.disparity;

import boofcv.abst.feature.disparity.*;
import boofcv.abst.filter.FilterImageInterface;
import boofcv.alg.feature.disparity.DisparityBlockMatchRowFormat;
import boofcv.alg.feature.disparity.DisparitySelect;
import boofcv.alg.feature.disparity.DisparitySparseScoreSadRect;
import boofcv.alg.feature.disparity.DisparitySparseSelect;
import boofcv.alg.feature.disparity.blockmatch.BlockRowScore;
import boofcv.alg.feature.disparity.blockmatch.BlockRowScoreCensus;
import boofcv.alg.feature.disparity.blockmatch.BlockRowScoreSad;
import boofcv.alg.feature.disparity.blockmatch.impl.ImplDisparityScoreBMBestFive_F32;
import boofcv.alg.feature.disparity.blockmatch.impl.ImplDisparityScoreBMBestFive_S32;
import boofcv.alg.feature.disparity.blockmatch.impl.ImplDisparityScoreBM_F32;
import boofcv.alg.feature.disparity.blockmatch.impl.ImplDisparityScoreBM_S32;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.transform.census.FactoryCensusTransform;
import boofcv.struct.image.*;

import javax.annotation.Nullable;

import static boofcv.factory.feature.disparity.FactoryStereoDisparityAlgs.*;

/**
 * <p>
 * Creates high level interfaces for computing the disparity between two rectified stereo images.
 * Algorithms which select the best disparity for each region independent of all the others are
 * referred to as Winner Takes All (WTA) in the literature.  Dense algorithms compute the disparity for the
 * whole image while sparse algorithms do it in a per pixel basis as requested.
 * </p>
 *
 * <p>
 * Typically disparity calculations with regions will produce less erratic results, but their precision will
 * be decreased.  This is especially evident along the border of objects.  Computing a wider range of disparities
 * can better results, but is very computationally expensive.
 * </p>
 *
 * <p>
 * Dense vs Sparse.  Here dense refers to computing the disparity across the whole image at once.  Sparse refers
 * to computing the disparity for a single pixel at a time as requested by the user,
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class FactoryStereoDisparity {

	public static <T extends ImageGray<T>, DI extends ImageGray<DI>> StereoDisparity<T,DI>
	blockMatch(@Nullable ConfigureDisparityBM config , Class<T> imageType , Class<DI> dispType ) {
		if( config == null )
			config = new ConfigureDisparityBM();

		if( config.subpixel ) {
			if( dispType != GrayF32.class )
				throw new IllegalArgumentException("With subpixel on, disparity image must be GrayF32");
		} else {
			if( dispType != GrayU8.class )
				throw new IllegalArgumentException("With subpixel on, disparity image must be GrayU8");
		}

		double maxError = (config.regionRadiusX*2+1)*(config.regionRadiusY*2+1)*config.maxPerPixelError;

		DisparitySelect select;
		if( imageType == GrayU8.class || imageType == GrayS16.class ) {
			if( config.subpixel ) {
				select = selectDisparitySubpixel_S32((int) maxError, config.validateRtoL, config.texture);
			} else {
				select = selectDisparity_S32((int) maxError, config.validateRtoL, config.texture);
			}
		} else if( imageType == GrayF32.class ) {
			if( config.subpixel ) {
				select = selectDisparitySubpixel_F32((int) maxError, config.validateRtoL, config.texture);
			} else {
				select = selectDisparity_F32((int) maxError, config.validateRtoL, config.texture);
			}
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}

		switch( config.error ) {
			case SAD: {
				BlockRowScore rowScore = createScoreRowSad(imageType);
				DisparityBlockMatchRowFormat alg = createBlockMatching(config, imageType, select, rowScore);
				return new WrapDisparityBlockMatchRowFormat(alg);
			}

			case CENSUS: { // TODO support multiple census
				FilterImageInterface censusTran = FactoryCensusTransform.blockDense(3,imageType);
				Class censusType = censusTran.getOutputType().getImageClass();
				BlockRowScore rowScore;
				if (censusType == GrayU8.class) {
					rowScore = new BlockRowScoreCensus.U8();
				} else if (censusType == GrayS32.class) {
					rowScore = new BlockRowScoreCensus.S32();
				} else if (censusType == GrayS64.class) {
					rowScore = new BlockRowScoreCensus.S64();
				} else {
					throw new IllegalArgumentException("Unsupported image type");
				}

				DisparityBlockMatchRowFormat alg = createBlockMatching(config, (Class<T>) imageType, select, rowScore);
				return new WrapDisparityBlockMatchCensus<>(censusTran, alg);
			}

			default:
				throw new IllegalArgumentException("Unsupported error type "+config.error);
		}
	}

	public static <T extends ImageGray<T>, DI extends ImageGray<DI>> StereoDisparity<T,DI>
	blockMatchBest5(@Nullable ConfigureDisparityBMBest5 config , Class<T> imageType , Class<DI> dispType ) {
		if( config == null )
			config = new ConfigureDisparityBMBest5();

		if( config.subpixel ) {
			if( dispType != GrayF32.class )
				throw new IllegalArgumentException("With subpixel on, disparity image must be GrayF32");
		} else {
			if( dispType != GrayU8.class )
				throw new IllegalArgumentException("With subpixel on, disparity image must be GrayU8");
		}

		double maxError = (config.regionRadiusX*2+1)*(config.regionRadiusY*2+1)*config.maxPerPixelError;

		// 3 regions are used not just one in this case
		maxError *= 3;

		DisparitySelect select;
		if( imageType == GrayU8.class || imageType == GrayS16.class ) {
			if( config.subpixel ) {
				select = selectDisparitySubpixel_S32((int) maxError, config.validateRtoL, config.texture);
			} else {
				select = selectDisparity_S32((int) maxError, config.validateRtoL, config.texture);
			}
		} else if( imageType == GrayF32.class ) {
			if( config.subpixel ) {
				select = selectDisparitySubpixel_F32((int) maxError, config.validateRtoL, config.texture);
			} else {
				select = selectDisparity_F32((int) maxError, config.validateRtoL, config.texture);
			}
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}

		switch( config.error ) {
			case SAD: {
				BlockRowScore rowScore = createScoreRowSad(imageType);
				DisparityBlockMatchRowFormat alg = createBestFive(config, imageType, select, rowScore);
				return new WrapDisparityBlockMatchRowFormat(alg);
			}

			case CENSUS: { // TODO support multiple census
				FilterImageInterface censusTran = FactoryCensusTransform.blockDense(3,imageType);
				Class censusType = censusTran.getOutputType().getImageClass();
				BlockRowScore rowScore;
				if (censusType == GrayU8.class) {
					rowScore = new BlockRowScoreCensus.U8();
				} else if (censusType == GrayS32.class) {
					rowScore = new BlockRowScoreCensus.S32();
				} else if (censusType == GrayS64.class) {
					rowScore = new BlockRowScoreCensus.S64();
				} else {
					throw new IllegalArgumentException("Unsupported image type");
				}

				DisparityBlockMatchRowFormat alg = createBestFive(config, imageType, select, rowScore);
				return new WrapDisparityBlockMatchCensus<>(censusTran, alg);
			}

			default:
				throw new IllegalArgumentException("Unsupported error type "+config.error);
		}
	}

	public static <T extends ImageGray<T>> BlockRowScore createScoreRowSad(Class<T> imageType) {
		BlockRowScore rowScore;
		if (imageType == GrayU8.class) {
			rowScore = new BlockRowScoreSad.U8();
		} else if (imageType == GrayU16.class) {
			rowScore = new BlockRowScoreSad.U16();
		} else if (imageType == GrayS16.class) {
			rowScore = new BlockRowScoreSad.S16();
		} else if (imageType == GrayF32.class) {
			rowScore = new BlockRowScoreSad.F32();
		} else {
			throw new IllegalArgumentException("Unsupported image type "+imageType.getSimpleName());
		}
		return rowScore;
	}

	private static <T extends ImageGray<T>> DisparityBlockMatchRowFormat
	createBlockMatching(ConfigureDisparityBM config, Class<T> imageType, DisparitySelect select, BlockRowScore rowScore) {
		DisparityBlockMatchRowFormat alg;
		if (GeneralizedImageOps.isFloatingPoint(imageType)) {
			alg = new ImplDisparityScoreBM_F32<>(config.minDisparity,
					config.maxDisparity, config.regionRadiusX, config.regionRadiusY, rowScore, select);
		} else {
			alg = new ImplDisparityScoreBM_S32(config.minDisparity,
					config.maxDisparity, config.regionRadiusX, config.regionRadiusY, rowScore, select);
		}
		return alg;
	}

	private static <T extends ImageGray<T>> DisparityBlockMatchRowFormat
	createBestFive(ConfigureDisparityBM config, Class<T> imageType, DisparitySelect select, BlockRowScore rowScore) {
		DisparityBlockMatchRowFormat alg;
		if (GeneralizedImageOps.isFloatingPoint(imageType)) {
			alg = new ImplDisparityScoreBMBestFive_F32(config.minDisparity,
					config.maxDisparity, config.regionRadiusX, config.regionRadiusY, rowScore, select);
		} else {
			alg = new ImplDisparityScoreBMBestFive_S32(config.minDisparity,
					config.maxDisparity, config.regionRadiusX, config.regionRadiusY, rowScore, select);
		}
		return alg;
	}

	/**
	 * WTA algorithms that computes disparity on a sparse per-pixel basis as requested..
	 *
	 * @param minDisparity Minimum disparity that it will check. Must be &ge; 0 and &lt; maxDisparity
	 * @param maxDisparity Maximum disparity that it will calculate. Must be &gt; 0
	 * @param regionRadiusX Radius of the rectangular region along x-axis.
	 * @param regionRadiusY Radius of the rectangular region along y-axis.
	 * @param maxPerPixelError Maximum allowed error in a region per pixel.  Set to &lt; 0 to disable.
	 * @param texture Tolerance for how similar optimal region is to other region.  Closer to zero is more tolerant.
	 *                Try 0.1
	 * @param subpixelInterpolation true to turn on sub-pixel interpolation
	 * @param imageType Type of input image.
	 * @param <T> Image type
	 * @return Sparse disparity algorithm
	 */
	public static <T extends ImageGray<T>> StereoDisparitySparse<T>
	regionSparseWta( int minDisparity , int maxDisparity,
					 int regionRadiusX, int regionRadiusY ,
					 double maxPerPixelError ,
					 double texture ,
					 boolean subpixelInterpolation ,
					 Class<T> imageType ) {

		double maxError = (regionRadiusX*2+1)*(regionRadiusY*2+1)*maxPerPixelError;

		if( imageType == GrayU8.class ) {
			DisparitySparseSelect<int[]> select;
			if( subpixelInterpolation)
				select = selectDisparitySparseSubpixel_S32((int) maxError, texture);
			else
				select = selectDisparitySparse_S32((int) maxError, texture);

			DisparitySparseScoreSadRect<int[],GrayU8>
					score = scoreDisparitySparseSadRect_U8(minDisparity,maxDisparity, regionRadiusX, regionRadiusY);

			return new WrapDisparityBlockSparseSad(score,select);
		} else if( imageType == GrayF32.class ) {
			DisparitySparseSelect<float[]> select;
			if( subpixelInterpolation )
				select = selectDisparitySparseSubpixel_F32((int) maxError, texture);
			else
				select = selectDisparitySparse_F32((int) maxError, texture);

			DisparitySparseScoreSadRect<float[],GrayF32>
					score = scoreDisparitySparseSadRect_F32(minDisparity,maxDisparity, regionRadiusX, regionRadiusY);

			return new WrapDisparityBlockSparseSad(score,select);
		} else
			throw new RuntimeException("Image type not supported: "+imageType.getSimpleName() );
	}
}
