package org.dataone.cn.web;

//import java.io.IOException;
import java.io.*;
//import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class ResolveServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void doPost(HttpServletRequest req, HttpServletResponse res)
                                throws ServletException, IOException {
//		FileReader fr = new FileReader("src/test/java/org/dataone/cn/web/sampleSysMetadata.xml");
//		BufferedReader br = new BufferedReader(fr);
		
		res.setContentType("text/xml");
		PrintWriter out = res.getWriter();

		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<d1:systemMetadata xmlns:d1=\"http://dataone.org/service/types/SystemMetadata/0.1\"");
        out.println("                   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        out.println("                   xmlns:schemaLocation=\"http://dataone.org/service/types/SystemMetadata/0.1 https://repository/dataone.org/software/cicore/trunk/schemas/systemmetadata.xsd\">");
        out.println("    <identifier>xXyYzZ12345</identifier>");
        out.println("    <objectFormat>eml://ecoinformatics.org/eml-2.0.1</objectFormat>");
        out.println("    <size>0</size>");
        out.println("    <submitter>uid=jones,o=NCEAS,dc=ecoinformatics,dc=org</submitter>");
        out.println("    <rightsHolder>uid=jones,o=NCEAS,dc=ecoinformatics,dc=org</rightsHolder>");
        out.println("    <obsoletes>Obsoletes0</obsoletes>");
        out.println("    <obsoletes>Obsoletes1</obsoletes>");
        out.println("    <obsoletedBy>ObsoletedBy0</obsoletedBy>");
        out.println("    <obsoletedBy>ObsoletedBy1</obsoletedBy>");
        out.println("    <derivedFrom>DerivedFrom0</derivedFrom>");
        out.println("    <derivedFrom>DerivedFrom1</derivedFrom>");
        out.println("    <describes>Describes0</describes>");
        out.println("    <describes>Describes1</describes>");
        out.println("    <describedBy>DescribedBy0</describedBy>");
        out.println("    <describedBy>DescribedBy1</describedBy>");
        out.println("    <checksum algorithm=\"SHA-1\">2e01e17467891f7c933dbaa00e1459d23db3fe4f</checksum>");
        out.println("    <embargoExpires>2006-05-04T18:13:51.0Z</embargoExpires>");
        out.println("    <accessRule rule=\"allow\" service=\"read\" principal=\"Principal0\"/>");
        out.println("    <accessRule rule=\"allow\" service=\"read\" principal=\"Principal1\"/>");
        out.println("    <replicationPolicy replicationAllowed=\"true\" numberReplicas=\"2\">");
        out.println("        <preferredMemberNode>MemberNode12</preferredMemberNode>");
        out.println("        <preferredMemberNode>MemberNode13</preferredMemberNode>");
        out.println("        <blockedMemberNode>MemberNode6</blockedMemberNode>");
        out.println("        <blockedMemberNode>MemberNode7</blockedMemberNode>");
        out.println("    </replicationPolicy>");
        out.println("    <dateUploaded>2006-05-04T18:13:51.0Z</dateUploaded>");
        out.println("    <dateSysMetadataModified>2006-05-04T18:13:51.0Z</dateSysMetadataModified>");
        out.println("    <originMemberNode>OriginMemberNode0</originMemberNode>");
        out.println("    <authoritativeMemberNode>AuthoritativeMemberNode0</authoritativeMemberNode>");
        out.println("    <replica>");
        out.println("        <replicaMemberNode>patternmatch2knb4now</replicaMemberNode>");
        out.println("        <replicationStatus>completed</replicationStatus>");
        out.println("        <replicaVerified>2006-05-04T18:13:51.0Z</replicaVerified>");
        out.println("    </replica>");
        out.println("    <replica>");
        out.println("        <replicaMemberNode>ReplicaMemberNode1</replicaMemberNode>");
        out.println("        <replicationStatus>queued</replicationStatus>");
        out.println("        <replicaVerified>2006-05-04T18:13:51.0Z</replicaVerified>");
        out.println("    </replica>");
        out.println("    <replica>");
        out.println("        <replicaMemberNode>http://foo.dataone.org</replicaMemberNode>");
        out.println("        <replicationStatus>completed</replicationStatus>");
        out.println("        <replicaVerified>2006-05-04T18:13:51.0Z</replicaVerified>");
        out.println("    </replica>");
        out.println("    <replica>");
        out.println("        <replicaMemberNode>http://bar.dataone.org/mn</replicaMemberNode>");
        out.println("        <replicationStatus>completed</replicationStatus>");
        out.println("        <replicaVerified>2006-05-04T18:13:51.0Z</replicaVerified>");
        out.println("    </replica>");
        out.println("    <replica>");
        out.println("        <replicaMemberNode>http://foo.dataone.org</replicaMemberNode>");
        out.println("        <replicationStatus>queued</replicationStatus>");
        out.println("        <replicaVerified>2006-05-04T18:13:51.0Z</replicaVerified>");
        out.println("    </replica>");
        out.println("</d1:systemMetadata>");
		
        out.flush();
	}
}
