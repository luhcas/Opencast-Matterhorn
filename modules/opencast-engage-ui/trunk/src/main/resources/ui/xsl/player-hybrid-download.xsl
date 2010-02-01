<?xml version="1.0" encoding="UTF-8"?>


<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns2="http://searchui.opencastproject.org/">
	<xsl:template match="/">


		<xsl:choose>
			<xsl:when test="ns2:episode/videoUrl">
	
      </xsl:when>

      <xsl:otherwise>
       <b>Error: No appropriate track could be found.</b>
		</xsl:otherwise>

</xsl:choose>

    <div id="oc-video-url" style="display: none">
      <xsl:choose>
        <xsl:when test="ns2:episode/videoUrl">
            <xsl:value-of select="ns2:episode/videoUrl" />
         </xsl:when>
         <xsl:otherwise>
           No Abstract
        </xsl:otherwise>
      </xsl:choose>
    </div>
      
    <div id="oc-title" style="display: none">
      <xsl:choose>
        <xsl:when test="ns2:episode/dcTitle">
            <xsl:value-of select="ns2:episode/dcTitle" />
         </xsl:when>
         <xsl:otherwise>
           No Title
        </xsl:otherwise>
      </xsl:choose>
    </div>
    
    <div id="oc-creator" style="display: none">
      <xsl:choose>
        <xsl:when test="ns2:episode/dcCreator">
            <xsl:value-of select="ns2:episode/dcCreator" />
         </xsl:when>
         <xsl:otherwise>
           No Creator
        </xsl:otherwise>
      </xsl:choose>
    </div>
    
    <div id="oc-abstract" style="display: none">
      <xsl:choose>
        <xsl:when test="ns2:episode/dcAbstract">
            <xsl:value-of select="ns2:episode/dcAbstract" />
         </xsl:when>
         <xsl:otherwise>
           No Abstract
        </xsl:otherwise>
      </xsl:choose>
    </div>
	</xsl:template>
</xsl:stylesheet>