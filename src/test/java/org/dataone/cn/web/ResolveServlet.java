package org.dataone.cn.web;

//import java.io.IOException;
import java.io.*;
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

	public void setOutput(String file) throws FileNotFoundException {
		
		FileReader fr = new FileReader("src/test/resources/resolveTesting/" + file);
		this.br = new BufferedReader(fr);
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse res)
                                throws ServletException, IOException {

		
		if (this.br == null) {
			throw new ServletException("output not set.  call setOutput(file) before doPost( )");
		}
		
		res.setContentType("text/xml");
		PrintWriter out = res.getWriter();
		
		String line = br.readLine();
		while (line != null) {
			out.println(line);
			line = br.readLine();
		}
		out.flush();
		out.close();

	}
}
