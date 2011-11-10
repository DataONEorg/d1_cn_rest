<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fn="http://www.w3.org/2005/02/xpath-function" 
	xmlns:d1="http://ns.dataone.org/service/types/v1"
	version="1.0">
	
	<xsl:output method="html" encoding="UTF-8" indent="yes" />
		
	<xsl:template name="nodeList">
		<h1>Node List</h1>
		<xsl:for-each select="*[local-name()='nodeList']/node">
			<xsl:call-template name="node" />
			<hr/>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="node">
		<xsl:for-each select=".">
			<table>
				<tr>
					<td>ID: </td>
					<td><xsl:value-of select="identifier"/></td>
				</tr>
				<tr>
					<td>Type: </td>
					<td><xsl:value-of select="@type"/></td>
				</tr>
				<tr>
					<td>Name: </td>
					<td><xsl:value-of select="name"/></td>
				</tr>
				<tr>
					<td>Description: </td>
					<td><xsl:value-of select="description"/></td>
				</tr>
				<tr>
					<td>Base URL: </td>
					<td><xsl:value-of select="baseURL"/></td>
				</tr>
				<tr>
					<td>Subject[s]: </td>
					<td>
						<xsl:for-each select="subject">
							<xsl:value-of select="."/>
							<br/>
						</xsl:for-each>	
					</td>
				</tr>
				<tr>
					<td>Services: </td>
					<td>
						<xsl:for-each select="services">
							<xsl:call-template name="services" />
						</xsl:for-each>	
					</td>
				</tr>
			</table>		
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="services">
		<table>
			<tr>
				<td>Name</td>
				<td>Version</td>
				<td>Available</td>
			</tr>
			<xsl:for-each select="service">		
				<tr>
					<td><xsl:value-of select="@name"/></td>
					<td><xsl:value-of select="@version"/></td>
					<td><xsl:value-of select="@available"/></td>
				</tr>
			</xsl:for-each>	
		</table>
	</xsl:template>			
	
</xsl:stylesheet>