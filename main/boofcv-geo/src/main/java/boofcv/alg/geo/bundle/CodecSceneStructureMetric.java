/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.bundle;

import boofcv.abst.geo.bundle.BundleAdjustmentSchur;
import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.bundle.jacobians.JacobianSo3;
import boofcv.alg.geo.bundle.jacobians.JacobianSo3Rodrigues;
import boofcv.misc.BoofMiscOps;
import org.ejml.UtilEjml;

/**
 * Encodes and decodes the values in a {@link SceneStructureMetric} using the following
 * parameterization:<br>
 * <pre>
 *     RT = (rodX rodY rodZ Tx Ty Tz)
 * [ (X Y Z)*M ][  RT*len(rigid)  ][  RT*len(views) ][ intrinsic*O ]
 * [ features  ][    rigid        ][    views       ][ camera      ]
 * </pre>
 *
 * Default encoding for rotation matrix is {@link JacobianSo3Rodrigues}
 *
 * @author Peter Abeles
 */
public class CodecSceneStructureMetric implements BundleAdjustmentSchur.Codec<SceneStructureMetric> {
	/**
	 * Specifies encoding/decoding of rotation for bundle adjustment. Default is {@link JacobianSo3Rodrigues}
	 */
	public JacobianSo3 rotation = new JacobianSo3Rodrigues();

	public CodecSceneStructureMetric() {}

	public CodecSceneStructureMetric( JacobianSo3 rotation ) {
		this.rotation = rotation;
	}

	@Override
	public void decode( double[] input, SceneStructureMetric structure ) {
		int index = 0;

		for (int i = 0; i < structure.points.size; i++) {
			SceneStructureCommon.Point p = structure.points.data[i];
			p.coordinate[0] = input[index++];
			p.coordinate[1] = input[index++];
			p.coordinate[2] = input[index++];
			if (structure.isHomogeneous())
				p.coordinate[3] = input[index++];
		}

		for (int rigidIndex = 0; rigidIndex < structure.rigids.size; rigidIndex++) {
			SceneStructureMetric.Rigid rigid = structure.rigids.data[rigidIndex];
			// Decode the rigid body transform from object to world
			if (rigid.known)
				continue;
			rotation.setParameters(input, index);
			rigid.object_to_world.R.setTo(rotation.getRotationMatrix());
			index += rotation.getParameterLength();

			rigid.object_to_world.T.x = input[index++];
			rigid.object_to_world.T.y = input[index++];
			rigid.object_to_world.T.z = input[index++];
		}

		for (int motionIndex = 0; motionIndex < structure.motions.size; motionIndex++) {
			SceneStructureMetric.Motion motion = structure.motions.data[motionIndex];
			// Decode the rigid body transform from world to view
			if (motion.known)
				continue;
			rotation.setParameters(input, index);
			motion.parent_to_view.R.setTo(rotation.getRotationMatrix());
			index += rotation.getParameterLength();

			motion.parent_to_view.T.x = input[index++];
			motion.parent_to_view.T.y = input[index++];
			motion.parent_to_view.T.z = input[index++];
		}

		for (int i = 0; i < structure.cameras.size; i++) {
			SceneStructureCommon.Camera camera = structure.cameras.data[i];
			if (camera.known)
				continue;
			camera.model.setIntrinsic(input, index);
			index += camera.model.getIntrinsicCount();
		}
	}

	@Override
	public void encode( SceneStructureMetric structure, double[] output ) {
		int index = 0;

		for (int i = 0; i < structure.points.size; i++) {
			SceneStructureCommon.Point p = structure.points.data[i];
			output[index++] = p.coordinate[0];
			output[index++] = p.coordinate[1];
			output[index++] = p.coordinate[2];
			if (structure.isHomogeneous())
				output[index++] = p.coordinate[3];

			// Make sure nothing blew up
			for (int checkIdx = index - p.coordinate.length; checkIdx < index; checkIdx++) {
				BoofMiscOps.checkTrue(!UtilEjml.isUncountable(output[checkIdx]), "Bad point");
			}
		}

		for (int rigidIndex = 0; rigidIndex < structure.rigids.size; rigidIndex++) {
			SceneStructureMetric.Rigid rigid = structure.rigids.data[rigidIndex];
			// Decode the rigid body transform from object to world
			if (rigid.known)
				continue;

			int indexBefore = index;
			rotation.getParameters(rigid.object_to_world.R, output, index);
			index += rotation.getParameterLength();

			output[index++] = rigid.object_to_world.T.x;
			output[index++] = rigid.object_to_world.T.y;
			output[index++] = rigid.object_to_world.T.z;

			// Make sure nothing blew up
			for (int checkIdx = indexBefore; checkIdx < index; checkIdx++) {
				BoofMiscOps.checkTrue(!UtilEjml.isUncountable(output[checkIdx]), "Bad rigid");
			}
		}

		for (int motionIndex = 0; motionIndex < structure.motions.size; motionIndex++) {
			SceneStructureMetric.Motion motion = structure.motions.data[motionIndex];
			// Decode the rigid body transform from world to view
			if (motion.known)
				continue;

			int indexBefore = index;
			rotation.getParameters(motion.parent_to_view.R, output, index);
			index += rotation.getParameterLength();

			output[index++] = motion.parent_to_view.T.x;
			output[index++] = motion.parent_to_view.T.y;
			output[index++] = motion.parent_to_view.T.z;

			// Make sure nothing blew up
			for (int checkIdx = indexBefore; checkIdx < index; checkIdx++) {
				BoofMiscOps.checkTrue(!UtilEjml.isUncountable(output[checkIdx]), "Bad rotation");
			}
		}

		for (int i = 0; i < structure.cameras.size; i++) {
			SceneStructureCommon.Camera camera = structure.cameras.data[i];
			if (camera.known)
				continue;

			int indexBefore = index;
			camera.model.getIntrinsic(output, index);
			index += camera.model.getIntrinsicCount();

			// Make sure nothing blew up
			for (int checkIdx = indexBefore; checkIdx < index; checkIdx++) {
				BoofMiscOps.checkTrue(!UtilEjml.isUncountable(output[checkIdx]), "Bad camera");
			}
		}
	}
}
