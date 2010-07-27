package org.dataone.cn.web;

//import java.io.IOException;
import java.io.*;
//import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class TestResolveServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void doPost(HttpServletRequest req, HttpServletResponse res)
                                throws ServletException, IOException {
		res.setContentType("text/xml");
		PrintWriter out = res.getWriter();
		out.println("<systemMetadata>");
		out.println("<identifier>12345</identifier>");
		out.println("<replica>");
		out.println("<replicaMemberNode>>MN1</replicaMemberNode>");
		out.println("<replicationStatus>completed</replicationStatus>");
		out.println("</replica>");
		out.println("</systemMetadata>");
		out.flush();
	}
}
