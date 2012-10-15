<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fn="http://www.w3.org/2005/02/xpath-function" 
	xmlns:d1="http://ns.dataone.org/service/types/v1"
	version="1.0">
	
	<xsl:output method="html" encoding="UTF-8" indent="yes" />
		
	<xsl:template name="queryEngineList">
		<p>
			DataONE Query Engine List
		</p>
		<hr/>
		<xsl:for-each select="*[local-name()='queryEngineList']/queryEngine">
			<table>
				<tr>
					<td>Engine Name: </td>
					<td><xsl:value-of select="."/></td>
				</tr>
			</table>
			<hr/>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>