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
		      <xsl:choose>
        <xsl:when test="ns2:search-results/result/segments/segment">
           <xsl:for-each select="ns2:search-results/result/segments/segment">
           <xsl:if test="(../../mediapackage/@duration) > ./@time">
           <td 
             class="segment-holder" 
             style="width: 15px;" 
             >
             <xsl:attribute name="id">segment<xsl:value-of select="position()" /></xsl:attribute>
             <xsl:attribute name="onmouseover">Opencast.Watch.hoverSegment('segment<xsl:value-of select="position()" />')</xsl:attribute>
             <xsl:attribute name="onmouseout">Opencast.Watch.hoverSegment('segment<xsl:value-of select="position()" />')</xsl:attribute>
             <xsl:attribute name="onclick">Opencast.Watch.seekSegment(<xsl:value-of select="floor(./@time div 1000)" />)</xsl:attribute>
             <xsl:attribute name="style">width: <xsl:value-of select="./@duration div (../../mediapackage/@duration) * 100" />%;</xsl:attribute>
           </td>
           </xsl:if>
           </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
		      <td style="width: 100%;" id="segment-holder-empty"  class="segment-holder" ></td>
		    </xsl:otherwise>
      </xsl:choose>
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
		
		
		<div id="oc-segments" style="display: none">
		<table class="oc-segment-table">
                  <tr>
              <xsl:for-each select="ns2:search-results/result/segments/segment">
                <td class="oc-segment-td">
                  <xsl:attribute name="onmouseover">Opencast.Watch.hoverSegment('segment<xsl:value-of select="position()" />')</xsl:attribute>
                  <xsl:attribute name="onmouseout">Opencast.Watch.hoverSegment('segment<xsl:value-of select="position()" />')</xsl:attribute>
                  <a>
                    <xsl:attribute name="href">javascript:Opencast.Watch.seekSegment(<xsl:value-of
                      select="floor(./@time div 1000)" />)</xsl:attribute>
                    <img height="83">
                      <xsl:attribute name="src"><xsl:value-of
                        select="./previews/preview" /></xsl:attribute>
                    </img>
                  </a>
                </td>
              </xsl:for-each>
              </tr>
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
          </xsl:if>
        </xsl:if>
      </xsl:if>

      <xsl:if test="@type='presentation/delivery'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='http'">
           <div id="oc-video-presentation-delivery-x-flv-http" style="display: none">
              <xsl:value-of select="url" />
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
          </xsl:if>
        </xsl:if>
      </xsl:if>

      <xsl:if test="@type='presentation/delivery'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='rtmp'">
           <div id="oc-video-presentation-delivery-x-flv-rtmp" style="display: none">
              <xsl:value-of select="url" />
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>



      <xsl:if test="@type='presenter/source'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='http'">
           <div id="oc-video-presenter-source-x-flv-http" style="display: none">
              <xsl:value-of select="url" />
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>

      <xsl:if test="@type='presentation/source'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='http'">
           <div id="oc-video-presentation-source-x-flv-http" style="display: none">
              <xsl:value-of select="url" />
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>

      <xsl:if test="@type='presenter/source'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='rtmp'">
           <div id="oc-video-presenter-source-x-flv-rtmp" style="display: none">
              <xsl:value-of select="url" />
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>

      <xsl:if test="@type='presentation/source'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='rtmp'">
           <div id="oc-video-presentation-source-x-flv-rtmp" style="display: none">
              <xsl:value-of select="url" />
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>

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
		
			</xsl:template>
</xsl:stylesheet>