<?xml version="1.0" ?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:d1="http://dataone.org/service/types/SystemMetadata/0.1">
  <xsl:output method="text" omit-xml-declaration="yes"/>
  <xsl:template match="/">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="d1:systemMetadata"><xsl:variable name="theID" select="identifier"/>
#<xsl:value-of select="$theID"/>
node,url<xsl:for-each select="replica[replicationStatus = 'completed']">
'<xsl:value-of select="replicaMemberNode"/>', '<xsl:value-of select="replicaMemberNode"/>/mn/object/=<xsl:value-of select="$theID"/>'</xsl:for-each>
  </xsl:template>

</xsl:stylesheet>
