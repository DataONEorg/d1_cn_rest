<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fn="http://www.w3.org/2005/02/xpath-function" 
	xmlns:d1="http://ns.dataone.org/service/types/v1"
	version="1.0">
	
	<!-- these are the types we want to render -->
	<xsl:import href="nodeList.v1.xsl"/>
	<!--
	<xsl:import href="objectList.v1.xsl"/>
	<xsl:import href="objectFormatList.v1.xsl"/>
	-->

	<xsl:output method="html" encoding="UTF-8" indent="yes" />
	
	<xsl:param name="something">something</xsl:param>
		
	<xsl:template match="/">
		<html>
		
			<xsl:call-template name="documenthead"/>
			
			<body>
		
				<xsl:call-template name="bodyheader"/>
				    
					<div id="content">
					                        
                   		<xsl:if test="*[local-name()='nodeList']">
                            <xsl:call-template name="nodeList"/>
                        </xsl:if>
                        <!-- 
                        <xsl:if test="*[local-name()='objectList']">     	
                            <xsl:call-template name="objectList"/>
                        </xsl:if>
                        
                        <xsl:if test="*[local-name()='objectFormatList']">     	
                            <xsl:call-template name="objectFormatList"/>
                        </xsl:if>
                        -->
					</div>
                        
				<xsl:call-template name="bodyfooter"/>
					
			</body>
			
		</html>
		
	</xsl:template>
	
	
	<xsl:template name="documenthead">
		<head>
			<!-- 
			<link rel="stylesheet" type="text/css" href="dataone.css" />
			-->
		</head>
	</xsl:template>
	
	<xsl:template name="bodyheader">
		<!-- header -->
	</xsl:template>
	
	<xsl:template name="bodyfooter">
		<!-- footer -->
	</xsl:template>
	
	
</xsl:stylesheet>