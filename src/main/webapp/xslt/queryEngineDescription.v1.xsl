<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fn="http://www.w3.org/2005/02/xpath-function" 
	xmlns:d1="http://ns.dataone.org/service/types/v1"
	version="1.0">
	
	<xsl:output method="html" encoding="UTF-8" indent="yes" />
		
	<xsl:template name="queryEngineDescription">
		<p>
			DataONE Query Engine Description 
			<br></br>
			<br></br>
			<table>
				<tr>
					<td>Query Engine</td>
					<td><xsl:value-of select="*[local-name()='queryEngineDescription']/name"/></td>
				</tr>
				<tr>
					<td>Engine Version</td>
					<td><xsl:value-of select="*[local-name()='queryEngineDescription']/queryEngineVersion"/></td>
				</tr>
				<tr>
					<td>Schema Version</td>
					<td><xsl:value-of select="*[local-name()='queryEngineDescription']/querySchemaVersion"/></td>
				</tr>
				<tr>
					<td>Additional Info:</td>
					<td><xsl:value-of select="*[local-name()='queryEngineDescription']/additionalInfo"/></td>
				</tr>
			</table>
		</p>
		<hr/>
		<xsl:for-each select="*[local-name()='queryEngineDescription']/queryField">
			<xsl:call-template name="queryField" />
			<hr/>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="queryField">
		<xsl:for-each select=".">
			<table>
				<tr>
					<td>Field Name: </td>
					<td><xsl:value-of select="name"/></td>
				</tr>
				<tr>
					<td>Description: </td>
					<td><xsl:value-of select="description"/></td>
				</tr>
				<tr>
					<td>Type: </td>
					<td><xsl:value-of select="type"/></td>
				</tr>
				<tr>
					<td>Searchable: </td>
					<td><xsl:value-of select="searchable"/></td>
				</tr>
				<tr>
					<td>Returnable: </td>
					<td><xsl:value-of select="returnable"/></td>
				</tr>
				<tr>
					<td>Multivalued: </td>
					<td><xsl:value-of select="multivalued"/></td>
				</tr>
				<tr>
					<td>Sortable: </td>
					<td><xsl:value-of select="sortable"/></td>
				</tr>
			</table>		
		</xsl:for-each>
	</xsl:template>	
	
</xsl:stylesheet>