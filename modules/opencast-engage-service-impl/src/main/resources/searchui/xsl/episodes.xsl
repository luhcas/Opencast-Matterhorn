<?xml version="1.0" encoding="UTF-8"?>


<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:ns2="http://searchui.opencastproject.org/">
<xsl:template match="/">
	 <xsl:for-each select="ns2:episodeListResult/searchui-episodes/searchui-episode">
			<div class="oc-search-result-item">
			     <div class="table-row">
			        <div class="left-container13">
			           <img src="img/thumbnail.png" />
			        </div>
			        <div class="left-container23">
			            <b> 
			              <a>
                      <xsl:attribute name="href">../watch/<xsl:value-of select="mediaPackageId" /></xsl:attribute>
                      <xsl:value-of select="title" />
                    </a>
                  </b> by Jenny Baltz<br/>
			            <i>25/11/2009</i> <br/>
			            Lorem ipsum dolor sit amet, consectetur adipiscing elit.
			       </div>
			       <div class="right-container13">
			            UC Berkeley <br/>
			            Computer Science Division
			       </div>
			    </div>
			</div>
	 </xsl:for-each>
	 <div id="oc-maxpage" style="display: none"><xsl:value-of select="ns2:episodeListResult/searchui-pagemax" /></div>
	 <div id="oc-from-index" style="display: none"><xsl:value-of select="ns2:episodeListResult/searchui-from-index" /></div>
	 <div id="oc-to-index" style="display: none"><xsl:value-of select="ns2:episodeListResult/searchui-to-index" /></div>
	 <div id="oc-episodes-max" style="display: none"><xsl:value-of select="ns2:episodeListResult/searchui-episodes-max" /></div>
</xsl:template>
</xsl:stylesheet>
