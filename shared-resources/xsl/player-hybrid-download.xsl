<!--
<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns2="http://search.opencastproject.org/" version="1.0">
  <xsl:template match="/">
    <div id="oc_seek-slider">
    <label id="skip" for="srubber">Time Slider</label>
      <div class="progress-list">
        <span class="load-progress" value="0"/>
        <span id="play-progress" class="play-progress" value="0" style="width: 0%;"/>
        <span id="scubber-channel" class="scrubber-channel">
          <input id="scrubber" type="image" src="img/space.png" class="scrubber-button" role="slider"/>
          <div id="draggable" class="ui-widget-content" style="left: 0%;" value="test"/>
        </span>
      </div>

      <div style="height: 25px">
              <span id="analytics" class="analytics" style="display: none;">Loading..</span>
              <span id="annotation" class="annotation" style="display: none;">Annotation..</span>
              <table class="segments" id="segmentstable" cellspacing="0" cellpadding="0" style="opacity:0.65; filter: alpha(opacity=65);">
                <tr>
                  <xsl:choose>
                    <xsl:when test="ns2:search-results/result/segments/segment">
                      <xsl:for-each select="ns2:search-results/result/segments/segment">
                        <xsl:if test="(../../mediapackage/@duration) &gt; ./@time">
                          <td class="segment-holder ui-widget ui-widget-content" style="width: 15px;">
                            <xsl:attribute name="id">segment<xsl:value-of select="position()"/></xsl:attribute>
                            <xsl:attribute name="onmouseover">Opencast.segments_ui.hoverSegment('segment<xsl:value-of select="position()"/>')</xsl:attribute>
                            <xsl:attribute name="onmouseout">Opencast.segments_ui.hoverOutSegment('segment<xsl:value-of select="position()"/>')</xsl:attribute>
                            <xsl:attribute name="alt">Slide <xsl:value-of select="position()"/> of <xsl:value-of select="last()"/></xsl:attribute>
                            <xsl:attribute name="onclick">Opencast.Watch.seekSegment(<xsl:value-of select="floor(./@time div 1000)"/>)</xsl:attribute>
                            <xsl:attribute name="style">width: <xsl:value-of select="./@duration div (../../mediapackage/@duration) * 100"/>%;</xsl:attribute>
                          </td>
                        </xsl:if>
                      </xsl:for-each>
                    </xsl:when>
                    <xsl:otherwise>
                      <td style="width: 100%;" id="segment-holder-empty" class="segment-holder"/>
                    </xsl:otherwise>
                  </xsl:choose>
                </tr>
              </table>

      </div>

    </div>
    <xsl:for-each select="ns2:search-results/result/mediapackage/media/track">
      <xsl:if test="@type='presenter/delivery'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='http'">
            <div id="oc-video-presenter-delivery-x-flv-http" style="display: none">
              <xsl:value-of select="url"/>
            </div>
            <div id="oc-resolution-presenter-delivery-x-flv-http" style="display: none">
              <xsl:value-of select="video/resolution"/>
            </div>
            <div id="oc-mimetype-presenter-delivery-x-flv-http" style="display: none">
              <xsl:value-of select="mimetype"/>
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>
       <xsl:if test="@type='presenter/delivery' and not(preceding-sibling::track[@type='presentation/delivery']/mimetype[.='video/...']) and not(following-sibling::track[@type='presentation/delivery']/mimetype[.='video/...'])">
        <xsl:if test="./mimetype='audio/x-adpcm'">
          <xsl:if test="substring(url, 1, 4)='http'">
            <div id="oc-video-presenter-delivery-x-flv-http" style="display: none">
              <xsl:value-of select="url"/>
            </div>
            <div id="oc-resolution-presenter-delivery-x-flv-http" style="display: none">
              <xsl:value-of select="video/resolution"/>
            </div>
            <div id="oc-mimetype-presenter-delivery-x-flv-http" style="display: none">
              <xsl:value-of select="mimetype"/>
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>
      <xsl:if test="@type='presentation/delivery'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='http'">
            <div id="oc-video-presentation-delivery-x-flv-http" style="display: none">
              <xsl:value-of select="url"/>
            </div>
            <div id="oc-resolution-presentation-delivery-x-flv-http" style="display: none">
              <xsl:value-of select="video/resolution"/>
            </div>
            <div id="oc-mimetype-presentation-delivery-x-flv-http" style="display: none">
              <xsl:value-of select="mimetype"/>
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>

      <xsl:if test="@type='presenter/delivery'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='rtmp'">
            <div id="oc-video-presenter-delivery-x-flv-rtmp" style="display: none">
              <xsl:value-of select="url"/>
            </div>
            <div id="oc-resolution-presenter-delivery-x-flv-rtmp" style="display: none">
              <xsl:value-of select="video/resolution"/>
            </div>
            <div id="oc-mimetype-presenter-delivery-x-flv-rtmp" style="display: none">
              <xsl:value-of select="mimetype"/>
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>
      <xsl:if test="@type='presentation/delivery'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='rtmp'">
            <div id="oc-video-presentation-delivery-x-flv-rtmp" style="display: none">
              <xsl:value-of select="url"/>
            </div>
            <div id="oc-resolution-presentation-delivery-x-flv-rtmp" style="display: none">
              <xsl:value-of select="video/resolution"/>
            </div>
            <div id="oc-mimetype-presentation-delivery-x-flv-rtmp" style="display: none">
              <xsl:value-of select="mimetype"/>
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>
      <xsl:if test="@type='presenter/source'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='http'">
            <div id="oc-video-presenter-source-x-flv-http" style="display: none">
              <xsl:value-of select="url"/>
            </div>
            <div id="oc-resolution-presenter-source-x-flv-http" style="display: none">
              <xsl:value-of select="video/resolution"/>
            </div>
            <div id="oc-mimetype-presenter-source-x-flv-http" style="display: none">
              <xsl:value-of select="mimetype"/>
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>
      <xsl:if test="@type='presentation/source'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='http'">
            <div id="oc-video-presentation-source-x-flv-http" style="display: none">
              <xsl:value-of select="url"/>
            </div>
            <div id="oc-resolution-presentation-source-x-flv-http" style="display: none">
              <xsl:value-of select="video/resolution"/>
            </div>
            <div id="oc-mimetype-presentation-source-x-flv-http" style="display: none">
              <xsl:value-of select="mimetype"/>
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>
      <xsl:if test="@type='presenter/source'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='rtmp'">
            <div id="oc-video-presenter-source-x-flv-rtmp" style="display: none">
              <xsl:value-of select="url"/>
            </div>
            <div id="oc-resolution-presenter-source-x-flv-rtmp" style="display: none">
              <xsl:value-of select="video/resolution"/>
            </div>
            <div id="oc-mimetype-presenter-source-x-flv-rtmp" style="display: none">
              <xsl:value-of select="mimetype"/>
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>
      <xsl:if test="@type='presentation/source'">
        <xsl:if test="./mimetype='video/x-flv'">
          <xsl:if test="substring(url, 1, 4)='rtmp'">
            <div id="oc-video-presentation-source-x-flv-rtmp" style="display: none">
              <xsl:value-of select="url"/>
            </div>
            <div id="oc-resolution-presentation-source-x-flv-rtmp" style="display: none">
              <xsl:value-of select="video/resolution"/>
            </div>
            <div id="oc-mimetype-presentation-source-x-flv-rtmp" style="display: none">
              <xsl:value-of select="mimetype"/>
            </div>
          </xsl:if>
        </xsl:if>
      </xsl:if>
    </xsl:for-each>
    <div id="oc-title" style="display: none">
      <xsl:choose>
        <xsl:when test="ns2:search-results/result/dcTitle">
          <xsl:value-of select="ns2:search-results/result/dcTitle"/>
        </xsl:when>
        <xsl:otherwise>
					No Title
				</xsl:otherwise>
      </xsl:choose>
    </div>
    <div id="dc-extent" style="display: none">
      <xsl:choose>
        <xsl:when test="ns2:search-results/result/dcExtent">
          <xsl:value-of select="ns2:search-results/result/ dcExtent"/>
        </xsl:when>
        <xsl:otherwise>
          0
        </xsl:otherwise>
      </xsl:choose>
    </div>
    <div id="oc-creator" style="display: none">
      <xsl:choose>
        <xsl:when test="ns2:search-results/result/dcCreator">
          <xsl:value-of select="ns2:search-results/result/dcCreator"/>
        </xsl:when>
        <xsl:otherwise>
          No Creator
        </xsl:otherwise>
      </xsl:choose>
    </div>
    <div id="oc-date" style="display: none">
      <xsl:choose>
        <xsl:when test="ns2:search-results/result/dcCreated">
          <xsl:value-of select="ns2:search-results/result/dcCreated"/>
        </xsl:when>
        <xsl:otherwise>
        </xsl:otherwise>
      </xsl:choose>
    </div>
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
    <div id="dc-subject" style="display: none">
      <xsl:choose>
        <xsl:when test="ns2:search-results/result/dcSubject">
          <xsl:value-of select="ns2:search-results/result/dcSubject"/>
        </xsl:when>
        <xsl:otherwise>
          No Subject
        </xsl:otherwise>
      </xsl:choose>
    </div>
    <div id="dc-contributor" style="display: none">
      <xsl:choose>
        <xsl:when test="ns2:search-results/result/dcContributor">
          <xsl:value-of select="ns2:search-results/result/dcContributor"/>
        </xsl:when>
        <xsl:otherwise>
          No Department
        </xsl:otherwise>
      </xsl:choose>
    </div>
    <div id="dc-description" style="display: none">
      <xsl:choose>
        <xsl:when test="ns2:search-results/result/dcDescription">
          <xsl:value-of select="ns2:search-results/result/dcDescription"/>
        </xsl:when>
        <xsl:otherwise>
          No Description
        </xsl:otherwise>
      </xsl:choose>
    </div>
    <div id="dc-language" style="display: none">
      <xsl:choose>
        <xsl:when test="ns2:search-results/result/dcLanguage">
          <xsl:value-of select="ns2:search-results/result/dcLanguage"/>
        </xsl:when>
        <xsl:otherwise>
          No Language
        </xsl:otherwise>
      </xsl:choose>
    </div>
    <xsl:for-each select="ns2:search-results/result/mediapackage/metadata/catalog">
      <xsl:if test="@type='captions/timedtext'">
        <xsl:if test="./mimetype='text/xml'">
          <div id="oc-captions" style="display: none">
            <xsl:value-of select="url"/>
          </div>
        </xsl:if>
      </xsl:if>
    </xsl:for-each>
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
-->