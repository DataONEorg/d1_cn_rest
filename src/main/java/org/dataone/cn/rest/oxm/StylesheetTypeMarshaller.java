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

package org.dataone.cn.rest.oxm;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.dataone.service.util.TypeMarshaller;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;

public class StylesheetTypeMarshaller implements Marshaller {

    private static TransformerFactory transformerFactory = TransformerFactory.newInstance();

	// configure in the bean
	private String stylesheet;

	public String getStylesheet() {
		return stylesheet;
	}

	public void setStylesheet(String stylesheet) {
		this.stylesheet = stylesheet;
	}

	@Override
	public void marshal(final Object typeObject, Result result) throws IOException,
			XmlMappingException {
		
		// marshal to byte stream
		PipedInputStream pis = new PipedInputStream();
		final PipedOutputStream pos = new PipedOutputStream(pis);
		
		// now write to it in a thread
		Runnable writingThread = new Runnable() {
			@Override
			public void run() {
				try {
					TypeMarshaller.marshalTypeToOutputStream(typeObject, pos, stylesheet);
				} catch (Exception e) {
					// throw it up the stack
					//e.printStackTrace();
					throw new RuntimeException(e.getMessage(), e);
				} finally {
					try {
						pos.close();
					} catch (IOException e) {
						throw new RuntimeException(e.getMessage(), e);
					}
				}
			}
		};
		// execute the thread
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.execute(writingThread);		
		executor.shutdown();
		
		// make a source from the input stream that is connected to the outputstream
		Source xmlSource = new StreamSource(pis);
		try {
			transformerFactory.newTransformer().transform(xmlSource, result);
		} catch (TransformerConfigurationException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TransformerException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	@Override
	public boolean supports(Class<?> arg0) {
		//should restrict to dataone types
		return true;
	}

}
