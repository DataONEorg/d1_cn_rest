<?xml version="1.0" ?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="text" omit-xml-declaration="yes"/>
  <xsl:template match="/">
    <xsl:apply-templates/>
  </xsl:template>
  <xsl:template match="systemMetadata"><xsl:variable name="theID" select="identifier"/>
{
 'identifier':'<xsl:value-of select="$theID"/>'
 'locations': [<xsl:for-each select="replica[replicationStatus = 'completed']">
   ['<xsl:value-of select="replicaMemberNode"/>', 'http://<xsl:value-of select="replicaMemberNode"/>object?id=<xsl:value-of select="$theID"/>']<xsl:if test="position() != last()">,</xsl:if></xsl:for-each> ]
}
</xsl:template>
</xsl:stylesheet>
