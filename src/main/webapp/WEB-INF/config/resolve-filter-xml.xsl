<?xml version="1.0" ?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:d1="http://dataone.org/service/types/SystemMetadata/0.1">
  <xsl:output method="xml" omit-xml-declaration="no"/>
  <xsl:template match="/">
    <xsl:apply-templates/>
  </xsl:template>
       
  <xsl:template match="d1:systemMetadata">
    <xsl:element name="d1:objectLocationList" xmlns:d1="http://dataone.org/service/types/ObjectLocationList/0.1"
 		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 		xsi:schemaLocation="http://dataone.org/service/types/ObjectLocationList/0.1 https://repository.dataone.org/software/cicore/trunk/schemas/objectlocationlist.xsd"> 
      <xsl:variable name="theID" select="identifier"/>
      <xsl:element name="identifier"><xsl:value-of select="$theID"/></xsl:element>

      <!-- transcribe replica node information -->
      <xsl:for-each select="replica[replicationStatus = 'completed']"> 
	  	<xsl:element name="objectLocation">
	  		<xsl:variable name="theNode" select="replicaMemberNode"/>
	   		
	   		<!-- TODO: (Major) We don't want to have hardcoded urls to handle odd cases.  This choose 
	   		   structure is a temporary workaround and needs to be removed once the node information
	   		   coming in on the systemMetadata is harmonized.  In future, we expect the replicaMemberNode element
	   		   to contain a (registered) node identifier, and this code to invoke an external function
	   		   that fetches the base-url from the node registry.  see http://www.javaworld.com/javaworld/jw-12-2001/jw-1221-xslt.html
	   		   for how-to do this with a Java extension.  A Java extension requires Xalan xslt processor, so reduces portability.
	   		   Other possible techniques include embedded JavaScript, Jython, or other interpretted languages.  -->  
	   		   
	   		<xsl:choose>
	  			<xsl:when test="contains($theNode,'knb')">
	  				<xsl:element name="nodeIdentifier">http://knb-mn.ecoinformatics.org/knb</xsl:element>
		    		<xsl:element name="url">http://knb-mn.ecoinformatics.org/knb/object/<xsl:value-of select="$theID"/></xsl:element>
		    	</xsl:when>
		    	<xsl:when test="contains($theNode,'KNB')">
	  				<xsl:element name="nodeIdentifier">http://knb-mn.ecoinformatics.org/knb</xsl:element>
		    		<xsl:element name="url">http://knb-mn.ecoinformatics.org/knb/object/<xsl:value-of select="$theID"/></xsl:element>
		    	</xsl:when>
		    	<xsl:when test="contains($theNode,'Knb')">
	  				<xsl:element name="nodeIdentifier">http://knb-mn.ecoinformatics.org/knb</xsl:element>
		    		<xsl:element name="url">http://knb-mn.ecoinformatics.org/knb/object/<xsl:value-of select="$theID"/></xsl:element>
		    	</xsl:when>
	    		<xsl:otherwise>
	    			<xsl:element name="nodeIdentifier"><xsl:value-of select="replicaMemberNode"/>/mn</xsl:element>
	    			<xsl:element name="url"><xsl:value-of select="replicaMemberNode"/>/mn/object/<xsl:value-of select="$theID"/></xsl:element>
	  			</xsl:otherwise>
	  		</xsl:choose>
	  	</xsl:element>
	  	
      </xsl:for-each>
    </xsl:element>
  </xsl:template>
 </xsl:stylesheet>
