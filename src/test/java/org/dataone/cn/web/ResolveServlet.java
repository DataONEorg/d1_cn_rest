package org.dataone.cn.web;

//import java.io.IOException;
import java.io.*;
import java.util.Scanner;
//import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class ResolveServlet extends HttpServlet {

	/**
	 * For unit testing, build a 2 member filter chain consisting of ResolveFilter
	 * and this servlet (the endpoint).  This replaces the urlrewrite to metacat chain 
	 * that comprise the inner layers of the calling chain.
	 * Returns a systemMetadata xml file.
	 */
	private static final long serialVersionUID = 1L;
	private BufferedReader br;
	private InputStream is;
	private FileInputStream byteInput;
	
	public void setOutput(String file) throws FileNotFoundException, UnsupportedEncodingException {
		this.is = this.getClass().getResourceAsStream("/resolveTesting/" + file);
	}
	
	
	public void doPost(HttpServletRequest req, HttpServletResponse res)
                                throws ServletException, IOException {

		
		if (this.is == null) {
			throw new ServletException("output not set.  call setOutput(file) before doPost( )");
		}
		
		res.setContentType("text/xml");
		res.setCharacterEncoding("UTF-8");

		ServletOutputStream out = res.getOutputStream();
		
		byte[] inBytes = null;
		boolean eof = false;
		int count = 0;
		while (!eof) {
			int input = is.read();
	        if (input == -1)
	          eof = true;
	        else 
	        	out.write(input);
	        	count++;
		}
		is.close();
		
		out.flush();
		out.close();
	}
}
