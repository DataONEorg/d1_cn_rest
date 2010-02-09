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
