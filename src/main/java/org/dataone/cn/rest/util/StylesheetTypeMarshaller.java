package org.dataone.cn.rest.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import org.dataone.service.util.TypeMarshaller;
import org.jibx.runtime.JiBXException;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;

public class StylesheetTypeMarshaller implements Marshaller {

	// configure in the bean
	private String stylesheet;

	public String getStylesheet() {
		return stylesheet;
	}

	public void setStylesheet(String stylesheet) {
		this.stylesheet = stylesheet;
	}

	@Override
	public void marshal(Object typeObject, Result result) throws IOException,
			XmlMappingException {
		
		// marshal to byte stream
		OutputStream os = new ByteArrayOutputStream();
		try {
			TypeMarshaller.marshalTypeToOutputStream(typeObject, os, stylesheet);
		} catch (JiBXException e) {
			// throw it up the stack
			e.printStackTrace();
			throw new IOException(e.getMessage(), e);
		}
		// use the outputstream as the result
		StreamResult streamResult = new StreamResult(os);
		result = streamResult;
	}

	@Override
	public boolean supports(Class<?> arg0) {
		//should restrict to dataone types
		return true;
	}

}
