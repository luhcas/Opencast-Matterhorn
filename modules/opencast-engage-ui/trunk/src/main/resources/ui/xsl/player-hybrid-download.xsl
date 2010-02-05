<?xml version="1.0" encoding="UTF-8"?>


<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns2="http://search.opencastproject.org/">
	<xsl:template match="/">

		<xsl:for-each
			select="ns2:search-results/ns2:result/ns2:mediapackage/media/track">

			<xsl:for-each select="tags/tag">

				<xsl:if test=".='engage'">
					<div id="oc-video-url" style="display: none">
						<xsl:value-of select="../../url" />
					</div>

				</xsl:if>
				<br />
			</xsl:for-each>

		</xsl:for-each>

		<div id="oc-title" style="display: none">
			<xsl:choose>
				<xsl:when test="ns2:search-results/ns2:result/ns2:dcTitle">
					<xsl:value-of select="ns2:search-results/ns2:result/ns2:dcTitle" />
				</xsl:when>
				<xsl:otherwise>
					No Title
				</xsl:otherwise>
			</xsl:choose>
		</div>

		<div id="oc-creator" style="display: none">
			<xsl:choose>
				<xsl:when test="ns2:search-results/ns2:result/ns2:dcCreator">
					<xsl:value-of select="ns2:search-results/ns2:result/ns2:dcCreator" />
				</xsl:when>
				<xsl:otherwise>
					No Creator
				</xsl:otherwise>
			</xsl:choose>
		</div>

		<div id="oc-abstract" style="display: none">
			<xsl:choose>
				<xsl:when test="ns2:search-results/ns2:result/ns2:dcAbstract">
					<xsl:value-of select="ns2:search-results/ns2:result/ns2:dcAbstract" />
				</xsl:when>
				<xsl:otherwise>
					No Abstract
				</xsl:otherwise>
			</xsl:choose>
		</div>
	</xsl:template>
</xsl:stylesheet>