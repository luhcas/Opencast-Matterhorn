<?xml version="1.0" encoding="UTF-8"?>


<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns2="http://search.opencastproject.org/">
	<xsl:template match="/">
		<xsl:for-each select="ns2:search-results/result">
			<div class="oc-search-result-item">
				<div class="table-row">
					<div class="left-container13">
						<img height="54">
							<xsl:attribute name="src"><xsl:value-of
								select="cover" /></xsl:attribute>
						</img>
					</div>
					<div class="left-container23">

						<xsl:choose>
							<xsl:when test="mediapackage/media/track/mimetype[.='video/x-flv']">
								<b>
									<a>
										<xsl:attribute name="href">watch.html?id=<xsl:value-of
											select="mediapackage/@id" /></xsl:attribute>
										<xsl:value-of select='substring(dcTitle, 0, 60)' />
										<xsl:if test='string-length(dcTitle)>60'>
											...
										</xsl:if>
									</a>
								</b>

								<xsl:if test="dcCreator!=''">
									by
									<xsl:value-of select="dcCreator" />
								</xsl:if>

								<br />
								<i>
									<xsl:value-of select="dcCreated" />
								</i>
								<br />
								<xsl:value-of select='substring(dcAbstract, 0, 170)' />
								<xsl:if test='string-length(dcAbstract)>170'>
									...
								</xsl:if>
							</xsl:when>

							<xsl:otherwise>
								<b>
									<xsl:value-of select='substring(dcTitle, 0, 60)' />
									<xsl:if test='string-length(dcTitle)>60'>
										...
									</xsl:if>
								</b>

								<xsl:if test="ns2:dcCreator!=''">
									by
									<xsl:value-of select="dcCreator" />
								</xsl:if>
								<br />
							  <b>The Matterhorn Media Player cannot play this media file.</b>
                <br />
                 Alternate media files that may be playable on other players may be listed in this
                <a>
                  <xsl:attribute name="href">../../search/rest/episode?id=<xsl:value-of
                    select="mediapackage/@id" /></xsl:attribute>
                  XML file
                </a>.
							</xsl:otherwise>
						</xsl:choose>
					</div>
					<div class="right-container13">
						<xsl:value-of select="dcRightsHolder" />
						<br />
						<xsl:value-of select="dcContributor" />
					</div>
				</div>
			</div>
		</xsl:for-each>
		<div id="oc-episodes-total" style="display: none">
			<xsl:value-of select="ns2:search-results/@total" />
		</div>
		<div id="oc-episodes-limit" style="display: none">
			<xsl:value-of select="ns2:search-results/@limit" />
		</div>
		<div id="oc-episodes-offset" style="display: none">
			<xsl:value-of select="ns2:search-results/@offset" />
		</div>
	</xsl:template>
</xsl:stylesheet>
