/**
 * This work was created by participants in the DataONE project, and is
 * jointly copyrighted by participating institutions in DataONE. For 
 * more information on DataONE, see our web site at http://dataone.org.
 *
 *   Copyright ${year}
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
 * 
 * $Id$
 */

package org.dataone.cn.rest.util;

import java.io.InputStream;
import java.io.OutputStream;

public class ControllerUtilities {

	static public void writeByteOutput (InputStream inputStream, OutputStream outputStream) throws Exception {
		byte[] buffer = new byte[1024];
		int r = 0;
		try {
			while ((r = inputStream.read(buffer, 0, buffer.length)) != -1) {
				outputStream.write(buffer, 0, r);
			}
		} finally {
			if (outputStream != null) {
				outputStream.flush();
			}
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}
	
}
