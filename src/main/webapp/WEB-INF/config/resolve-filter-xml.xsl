<?xml version="1.0" ?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:d1="http://dataone.org/service/types/SystemMetadata/0.1">
  <xsl:output method="xml" omit-xml-declaration="no"/>
  <xsl:template match="/">
    <xsl:apply-templates/>
  </xsl:template>

  <!-- assuming that input stream is a contatenation (with document wrapper) of real 
       systemMetaData XML objects as described in the mule1 documentation  -->
       
  <xsl:template match="d1:systemMetadata">
    <xsl:element name="d1:objectLocationList" xmlns:d1="http://dataone.org/service/types/ObjectLocationList/0.1"
 		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 		xsi:schemaLocation="http://dataone.org/service/types/ObjectLocationList/0.1 https://repository.dataone.org/software/cicore/trunk/schemas/objectlocationlist.xsd"> 
      <xsl:variable name="theID" select="identifier"/>
      <xsl:attribute name="identifier"><xsl:value-of select="$theID"/></xsl:attribute>

      <!-- transcribe replica node information -->
      <xsl:for-each select="replica[replicationStatus = 'completed']"> 
	  	<xsl:element name="objectLocation">
	    	<xsl:element name="nodeIdentifier"><xsl:value-of select="replicaMemberNode"/></xsl:element>
	    	<xsl:element name="url"><xsl:value-of select="replicaMemberNode"/>/mn/object/<xsl:value-of select="$theID"/>
	    	</xsl:element>
	  	</xsl:element>
      </xsl:for-each>
    </xsl:element>
  </xsl:template>
</xsl:stylesheet>
