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
		       <xsl:for-each select="ns2:search-results/result/segments/segment">
		       <xsl:if test="(../../mediapackage/@duration) > ./@time">
		       <td 
		         class="segment-holder" 
		         style="width: 15px;" 
		         >
		         <xsl:attribute name="id">segment<xsl:value-of select="position()" /></xsl:attribute>
		         <xsl:attribute name="onmouseover">Opencast.Watch.hoverSegment('segment<xsl:value-of select="position()" />')</xsl:attribute>
		         <xsl:attribute name="onmouseout">Opencast.Watch.hoverSegment('segment<xsl:value-of select="position()" />')</xsl:attribute>
		         <xsl:attribute name="alt">Slide <xsl:value-of select="position()" /> of <xsl:value-of select="last()" /></xsl:attribute>
		         <xsl:attribute name="onclick">Opencast.Watch.seekSegment(<xsl:value-of select="floor(./@time div 1000)" />)</xsl:attribute>
		         <xsl:attribute name="style">width: <xsl:value-of select="./@duration div (../../mediapackage/@duration) * 100" />%;</xsl:attribute>
		       </td>
		       </xsl:if>
		       </xsl:for-each>
		     </tr>
		   </table>
		   <div class="progress-list">
		            <span class="load-progress" value="0"></span>
		            <span id="play-progress" class="play-progress" value="0" style="width: 0%;"></span>
		            <span id="scubber-channel" class="scrubber-channel">
		              <input id="scrubber" type="submit" class="scrubber-button" role="slider"/>
		              <div id="draggable" class="ui-widget-content" style="left: 0%;"></div>
		            </span>
		          </div>
		        </td>
		      </tr>
		    </tbody>
		  </table>
		</div>
		
		
    <xsl:for-each
      select="ns2:search-results/result/mediapackage/media/track">

          <xsl:if test="@type='presenter/delivery'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='http'">
           <div id="oc-video-presenter-delivery-x-flv-http" style="display: none">
              <xsl:value-of select="url" />
            </div>
            <div id="oc-mimetype-presenter-delivery-x-flv-http" style="display: none">
              <xsl:value-of select="mimetype" />
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>
      
       <xsl:if test="@type='presenter/delivery' and not(preceding-sibling::track[@type='presentation/delivery']/mimetype[.='video/...']) and not(following-sibling::track[@type='presentation/delivery']/mimetype[.='video/...'])">
        <xsl:if test="./mimetype='audio/x-adpcm'">
          <xsl:if test="substring(url, 1, 4)='http'">
           <div id="oc-video-presenter-delivery-x-flv-http" style="display: none">
              <xsl:value-of select="url" />
            </div>
            <div id="oc-mimetype-presenter-delivery-x-flv-http" style="display: none">
              <xsl:value-of select="mimetype" />
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>

      <xsl:if test="@type='presentation/delivery'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='http'">
           <div id="oc-video-presentation-delivery-x-flv-http" style="display: none">
              <xsl:value-of select="url" />
            </div>
            <div id="oc-mimetype-presentation-delivery-x-flv-http" style="display: none">
              <xsl:value-of select="mimetype" />
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>

      <xsl:if test="@type='presenter/delivery'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='rtmp'">
           <div id="oc-video-presenter-delivery-x-flv-rtmp" style="display: none">
              <xsl:value-of select="url" />
            </div>
            <div id="oc-mimetype-presenter-delivery-x-flv-rtmp" style="display: none">
              <xsl:value-of select="mimetype" />
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>

      <xsl:if test="@type='presentation/delivery'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='rtmp'">
           <div id="oc-video-presentation-delivery-x-flv-rtmp" style="display: none">
              <xsl:value-of select="url" />
            </div>
            <div id="oc-mimetype-presentation-delivery-x-flv-rtmp" style="display: none">
              <xsl:value-of select="mimetype" />
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>

    </xsl:for-each>

    <xsl:for-each select="ns2:search-results/result/mediapackage/attachments/attachment">
      <xsl:choose>
        <xsl:when test="@type='presenter/player+preview'">
          <div id="oc-cover-presenter" style="display: none">
            <xsl:value-of select="url"/>
          </div>
        </xsl:when>
        <xsl:when test="@type='presentation/player+preview'">
          <div id="oc-cover-presentation" style="display: none">
            <xsl:value-of select="url"/>
          </div>
        </xsl:when>
      </xsl:choose>
    </xsl:for-each>

		<div id="oc-title" style="display: none">
			<xsl:choose>
				<xsl:when test="ns2:search-results/result/dcTitle">
					<xsl:value-of select="ns2:search-results/result/dcTitle" />
				</xsl:when>
				<xsl:otherwise>
					No Title
				</xsl:otherwise>
			</xsl:choose>
		</div>

		<div id="oc-creator" style="display: none">
			<xsl:choose>
				<xsl:when test="ns2:search-results/result/dcCreator">
					<xsl:value-of select="ns2:search-results/result/dcCreator" />
				</xsl:when>
				<xsl:otherwise>
					No Creator
				</xsl:otherwise>
			</xsl:choose>
		</div>
		
		<div id="oc-date" style="display: none">
      <xsl:choose>
        <xsl:when test="ns2:search-results/result/dcCreated">
          <xsl:value-of select="ns2:search-results/result/dcCreated" />
        </xsl:when>
        <xsl:otherwise>
        </xsl:otherwise>
      </xsl:choose>
    </div>

		<div id="oc-abstract" style="display: none">
			<xsl:choose>
				<xsl:when test="ns2:search-results/result/dcAbstract">
					<xsl:value-of select="ns2:search-results/result/dcAbstract" />
				</xsl:when>
				<xsl:otherwise>
					No Abstract
				</xsl:otherwise>
			</xsl:choose>
		</div>
		
		<div id="oc-segments-text" style="display: none">
      <table cellspacing="0" cellpadding="0">
        <xsl:for-each select="ns2:search-results/result/segments/segment">
          <xsl:if test="(../../mediapackage/@duration) &gt; ./@time">
            <tr>
              <td class="oc-segments-preview">
               <xsl:value-of select="./previews/preview" />
              </td>
              <td class="oc-segments-time">
                <a class="segments-time">
                  <xsl:attribute name="onclick">Opencast.Watch.seekSegment(<xsl:value-of select="floor(./@time div 1000)"/>)</xsl:attribute>
                  <xsl:value-of select="floor(./@time div 1000)"/>
                </a>
              </td>
              <td>
                <xsl:value-of select="text"/>
              </td>
            </tr>
          </xsl:if>
        </xsl:for-each>
      </table>
    </div>
			</xsl:template>
</xsl:stylesheet>