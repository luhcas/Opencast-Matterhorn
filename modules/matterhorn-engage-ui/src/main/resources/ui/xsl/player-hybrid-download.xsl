<?xml version="1.0" encoding="UTF-8"?>


<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns2="http://search.opencastproject.org/">
	<xsl:template match="/">

		<div id="oc_seek-slider" >
		  <table width="100%">
		    <tbody>
		      <tr class="player-chrome">
		        <td class="progress-segment" width="100%">
		   <table class="segments" cellspacing="0" cellpadding="0">
		     <tr>
		       <xsl:for-each select="ns2:search-results/ns2:result/ns2:segments/ns2:mediaSegments">
		       <xsl:if test="(../../ns2:mediapackage/@duration) > ./@time">
		       <td 
		         class="segment-holder" 
		         style="width: 15px;" 
		         >
		         <xsl:attribute name="id">segment<xsl:value-of select="position()" /></xsl:attribute>
		         <xsl:attribute name="onmouseover">Opencast.Watch.hoverSegment('segment<xsl:value-of select="position()" />')</xsl:attribute>
		         <xsl:attribute name="onmouseout">Opencast.Watch.hoverSegment('segment<xsl:value-of select="position()" />')</xsl:attribute>
		         <xsl:attribute name="onclick">Opencast.Watch.seekSegment(<xsl:value-of select="floor(./@time div 1000)" />)</xsl:attribute>
		         <xsl:attribute name="style">width: <xsl:value-of select="./@duration div (../../ns2:mediapackage/@duration) * 100" />%;</xsl:attribute>
		       </td>
		       </xsl:if>
		       </xsl:for-each>
		     </tr>
		   </table>
		   <div class="progress-list">
		            <span class="load-progress" value="0"></span>
		            <span id="play-progress" class="play-progress" value="0" style="width: 0%;"></span>
		            <span id="scubber-channel" class="scrubber-channel">
		              <button id="scrubber" class="scrubber-button" ></button>
		              <div id="draggable" class="ui-widget-content" style="left: 0%;"></div>
		            </span>
		          </div>
		        </td>
		      </tr>
		    </tbody>
		  </table>
		</div>
		
		<div id="oc_tabs">
          <input id="oc_btn-slides" class="oc_btn-tabs" type="submit" onClick="Opencast.Player.doToggleSlides()" name="Hide Slides" value="Hide Slides" alt="Hide Slides" title="Hide Slides"></input>
          <input id="oc_btn-notes" class="oc_btn-tabs" type="submit" onClick="Opencast.Player.doToggleNotes()" name="Notes" value="Notes" alt="Notes" title="Notes"></input>
          <input id="oc_btn-search" class="oc_btn-tabs" type="submit" onClick="Opencast.Player.doToggleSearch()" name="Search" value="Search" alt="Search" title="Search"></input>
          <input id="oc_btn-shortcuts" class="oc_btn-tabs" type="submit" onClick="Opencast.Player.doToggleShortcuts()" name="Shortcuts" value="Shortcuts" alt="Shortcuts" title="Shortcuts"></input>
          <input id="oc_btn-embed" class="oc_btn-tabs" type="submit" onClick="Opencast.Player.doToggleEmbed()" name="Embed" value="Embed" alt="Embed" title="Embed"></input>
        </div>
		
		 <div id="oc_slides-sections" class="oc_slidesDisplayBlock">
		    <div id="segments-holder" class="oc-segments-holder">
		      <div class="oc-segments">
		        <table class="oc-segment-table">
		              <tr>
		          <xsl:for-each select="ns2:search-results/ns2:result/ns2:segments/ns2:mediaSegments">
		            <td class="oc-segment-td">
		              <xsl:attribute name="onmouseover">Opencast.Watch.hoverSegment('segment<xsl:value-of select="position()" />')</xsl:attribute>
		              <xsl:attribute name="onmouseout">Opencast.Watch.hoverSegment('segment<xsl:value-of select="position()" />')</xsl:attribute>
		              <a>
		                <xsl:attribute name="href">javascript:Opencast.Watch.seekSegment(<xsl:value-of
		                  select="floor(./@time div 1000)" />)</xsl:attribute>
		                <img height="83">
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
      </div> 

		<xsl:for-each
			select="ns2:search-results/ns2:result/ns2:mediapackage/media/track">

			<xsl:for-each select="tags/tag">

				<xsl:if test=".='engage'">
					<div id="oc-video-url" style="display: none">
						<xsl:value-of select="../../url" />
					</div>

				</xsl:if>
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
		
		<div id="oc-date" style="display: none">
      <xsl:choose>
        <xsl:when test="ns2:search-results/ns2:result/ns2:dcCreated">
          <xsl:value-of select="ns2:search-results/ns2:result/ns2:dcCreated" />
        </xsl:when>
        <xsl:otherwise>
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