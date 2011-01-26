<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html" omit-xml-declaration="yes"/>
  <!-- Because of an issue with XPath we have to use &#x2D; to represent '-'
   otherwise it tries to evaluate hyphen-minus as a subtraction operator in firefox. -->
  <xsl:template match="event">
    <div class="fl-container-800">
      <center>
        <h2>View Recording Info</h2>
      </center>
    </div>
    <div class="fl-container-800 fl-theme-coal">
      <div class="fl-widget">
        <div class="fl-widget-content form-box-container">
          <ul class="fl-controls-right form-list">
            <li>
              <label class="fl-label">Title:</label>
              <!-- Title -->
              <xsl:value-of select="title" />
            </li>
            <li>
              <label class="fl-label">Series:</label>
              <!-- Series -->
              <xsl:value-of select="isPartOf" />
            </li>
            <li>
              <label class="fl-label">Presenter:</label>
              <!-- Presenter -->
              <xsl:value-of select="creator" />
            </li>
          </ul>
        </div>
      </div>
    </div>
    
    <div class="fl-container-800 fl-theme-coal">
      <div class="fl-widget">
        <div class="fl-widget-titlebar folder-head">
          <span class="fl-icon icon-arrow-right folder-icon">Additional Content</span>
          <b style="verticle-align: middle;">Additional Description</b>
        </div>
        <div class="fl-widget-content folder-content">
          <ul class="fl-controls-right form-list">
            <li class="additionalMeta">
              <label class="fl-label">Department:</label>
              <!-- Department -->
              <xsl:value-of select="contributor" />
            </li>
            <li class="additionalMeta">
              <label class="fl-label">Subject:</label>
              <!-- Subject -->
              <xsl:value-of select="subject" />
            </li>
            <li class="additionalMeta">
              <label class="fl-label">Language:</label>
              <!-- Language -->
              <xsl:value-of select="language" />
            </li>
            <li class="additionalMeta">
              <label class="fl-label">Description:</label>
              <!-- Description -->
              <span style="display:table-cell">
                <xsl:value-of select="description" />
              </span>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <xsl:choose>
      <xsl:when test="string-length(startdate) &gt; 0">
      <xsl:if test="string-length(agent) &gt; 0">
        <div class="fl-container-800 fl-theme-coal" id="captureform">
          <div class="fl-widget">
            <div class="fl-widget-titlebar form-box-title">
              <b>Capture</b>
            </div>
            <div class="fl-widget-content form-box-container">
              <ul class="fl-controls-right form-list">
                <li>
                  <label class="fl-label">Recording Start Time:</label>
                <!-- Recording Date/Time -->
                  <xsl:value-of select="startdate" />
                </li>
                <li>
                  <label class="fl-label">Duration:</label>
                <!-- Duration -->
                  <xsl:value-of select="duration" />
                </li>
                <li>
                  <label class="fl-label">Capture Agent:</label>
                <!-- Capture Agent -->
                  <xsl:value-of select="agent" />
                </li>
                <li>
                  <label class="fl-label">Input(s):</label>
                <!-- Inputs -->
                  <xsl:value-of select="inputs" />
                </li>
              </ul>
            </div>
          </div>
        </div>
      </xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <div class="fl-container-800 fl-theme-coal">
          <div class="fl-widget">
            <div class="fl-widget-titlebar form-box-title">
              <b>File Upload</b>
            </div>
            <div class="fl-widget-content form-box-container">
              <ul class="fl-controls-right form-list">
                <li>
                  <label class="fl-label">File Uploaded:</label>
              <!-- filename -->
                  <xsl:value-of select="filename" />
                </li>
              </ul>
            </div>
          </div>
        </div>
      </xsl:otherwise>
    </xsl:choose>
    
    <div class="fl-container-800 fl-theme-coal">
      <div class="fl-widget">
        <div class="fl-widget-titlebar form-box-title">
          <b>Distribution</b>
        </div>
        <div class="fl-widget-content form-box-container">
          <ul class="fl-controls-right form-list">
            <li>
              <label class="fl-label">Distribution Channel(s):</label>
              <!-- Distribution Channels -->
              Matterhorn Media Module
              <xsl:value-of select="distribution" />
            </li>
            <li>
              <label class="fl-label">License:</label>
              <!-- license -->
              <xsl:value-of select="license" />
            </li>
            <li>
              <label class="fl-label">Media File:</label>
              <!-- filename -->
              <xsl:value-of select="filename" />
            </li>
          </ul>
        </div>
      </div>
    </div>
    <div style="margin-left: 31%">
      <a href="javascript:history.back();">&lt;&lt; Back to Recordings</a>
    </div>
  </xsl:template>
</xsl:stylesheet>
