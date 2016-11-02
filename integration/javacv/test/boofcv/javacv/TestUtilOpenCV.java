/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.javacv;

import boofcv.struct.calib.CameraPinholeRadial;
import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestUtilOpenCV {
	@Test
	public void loadCalibration() {
		URL url = TestUtilOpenCV.class.getResource("pinhole_distorted.yml");
		CameraPinholeRadial model = UtilOpenCV.loadCalibration(url.getFile());
		fail("Implement");
	}
}