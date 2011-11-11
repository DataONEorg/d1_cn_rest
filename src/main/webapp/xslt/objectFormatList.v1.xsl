<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fn="http://www.w3.org/2005/02/xpath-function" 
	xmlns:d1="http://ns.dataone.org/service/types/v1"
	version="1.0">
	
	<xsl:output method="html" encoding="UTF-8" indent="yes" />
		
	<xsl:template name="objectFormatList">
		<p>
			Registered DataONE format types 
			(displaying 
			<xsl:value-of select="*[local-name()='objectFormatList']/@start"/>
			<xsl:text>-</xsl:text>
			<xsl:value-of select="*[local-name()='objectFormatList']/@count"/> 
			of 
			<xsl:value-of select="*[local-name()='objectFormatList']/@total"/>
			total).
		</p>
		<hr/>
		<xsl:for-each select="*[local-name()='objectFormatList']/objectFormat">
			<xsl:call-template name="objectFormat" />
			<hr/>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="objectFormat">
		<xsl:for-each select=".">
			<table>
				<tr>
					<td>Type: </td>
					<td><xsl:value-of select="formatType"/></td>
				</tr>
				<tr>
					<td>Id: </td>
					<td><xsl:value-of select="formatId"/></td>
				</tr>
				<tr>
					<td>Name: </td>
					<td><xsl:value-of select="formatName"/></td>
				</tr>
			</table>		
		</xsl:for-each>
	</xsl:template>	
	
</xsl:stylesheet>