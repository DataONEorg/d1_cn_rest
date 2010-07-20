<?xml version="1.0" ?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" omit-xml-declaration="no"/>
  <xsl:template match="/">
    <xsl:apply-templates/>
  </xsl:template>

  <!-- assuming that input stream is a contatenation (with document wrapper) of real 
       systemMetaData XML objects as described in the mule1 documentation  -->
       
  <xsl:template match="systemMetadata">
    <xsl:element name="locations"> 
      <xsl:attribute name="identifier"><xsl:value-of select="identifier"/></xsl:attribute>
      <xsl:variable name="theID" select="identifier"/>
      <!-- the authoratative member node info  -->
      <!-- the authoritative MN is represented in the replica elements so this section not needed  
      <xsl:element name="location">
	<xsl:attribute name="node">
	  <xsl:value-of select="authoritativeMemberNode"/>
	</xsl:attribute>
	<xsl:attribute name="href">http://<xsl:value-of select="authoritativeMemberNode"/>object?id=<xsl:value-of select="$theID"/>
	</xsl:attribute>
      </xsl:element>

      -->
      <!-- transcribe replica node information -->

      <xsl:for-each select="replica"> 
	<xsl:if test="replicationStatus = 'completed'">
	  <xsl:element name="location">
	    <xsl:attribute name="node">
	      <xsl:value-of select="replicaMemberNode"/>
	    </xsl:attribute>
	    <xsl:attribute name="href">http://<xsl:value-of select="replicaMemberNode"/>object?id=<xsl:value-of select="$theID"/>
	    </xsl:attribute>
	  </xsl:element>
	</xsl:if>
      </xsl:for-each>
    </xsl:element>
  </xsl:template>
</xsl:stylesheet>
