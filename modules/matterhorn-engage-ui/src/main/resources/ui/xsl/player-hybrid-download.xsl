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
		
		<div id="segments-holder" class="oc-segments-holder">
			<div class="oc-segments">
				<table class="oc-segment-table">
				      <tr>
					<xsl:for-each select="ns2:search-results/ns2:result/ns2:segments/ns2:mediaSegments">
			      <td class="oc-segment-td">
			        <a>
	              <xsl:attribute name="href">javascript:Videodisplay.seek(<xsl:value-of
	                select="./@time div 1000" />)</xsl:attribute>
	                <xsl:if test='10>floor((./@time div 1000) div 3600)'>0</xsl:if><xsl:value-of select="floor((./@time div 1000) div 3600)" />:<xsl:if test='10>floor(((./@time div 1000) - ((floor((./@time div 1000) div 3600))*3600)) div 60)'>0</xsl:if><xsl:value-of select="floor(((./@time div 1000) - ((floor((./@time div 1000) div 3600))*3600)) div 60)" />:<xsl:if test='10>(./@time div 1000) - ((floor((./@time div 1000) div 3600))*3600) - ((floor(((./@time div 1000) - ((floor((./@time div 1000) div 3600))*3600)) div 60))*60)'>0</xsl:if><xsl:value-of select="(./@time div 1000) - ((floor((./@time div 1000) div 3600))*3600) - ((floor(((./@time div 1000) - ((floor((./@time div 1000) div 3600))*3600)) div 60))*60)" />
		          </a>
			        <a>
								<xsl:attribute name="href">javascript:Videodisplay.seek(<xsl:value-of
								  select="./@time div 1000" />)</xsl:attribute>
			          <img>
			            <xsl:attribute name="src"><xsl:value-of
			              select="./ns2:previews/ns2:preview[@type='presentation']" /></xsl:attribute>
			          </img>
		          </a>
			      </td>
			    </xsl:for-each>
			    </tr>
				</table>
			</div>
	  </div>
	</xsl:template>
</xsl:stylesheet>