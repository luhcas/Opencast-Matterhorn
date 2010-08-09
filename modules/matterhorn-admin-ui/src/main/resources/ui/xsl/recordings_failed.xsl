<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : recordings_failed.xsl
    Created on : November 19, 2009, 9:18 PM
    Author     : wulff
    Description:
        Transforms output of /admin/rest/recordings/finished to HTML table
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns1="http://adminui.opencastproject.org/" version="1.0">
  <xsl:output method="html"/>

  <xsl:template match="ns1:recordingLists">
    <table id="recordingsTable" class="fl-theme-coal wu-table-list" width="100%" style="float:left;">
      <thead>
        <tr>
          <th width="30%" id="th-Title" class="sortable recording-Table-head">Title</th>
          <th width="15%" id="th-Presenter" class="sortable recording-Table-head">Presenter</th>
          <th width="20%" id="th-Series" class="sortable recording-Table-head">Course/Series</th>
          <th width="20%" id="th-StartDate" class="sortable date-column recording-Table-head">Upload / Recording Time</th>
          <!-- <th width="10%" class="sortable">Capture Agent</th> -->
          <th width="15%" id="th-ProcessingStatus" class="sortable recording-Table-head">Status</th>
          <th width="15%" id="th-Action" class="recording-Table-head">Actions</th>
        </tr>
      </thead>
      <tbody>
        <xsl:apply-templates />
      </tbody>
    </table>
  </xsl:template>

  <xsl:template match="ns1:recording">
    <tr class="highlightable">
      <td>
        <xsl:value-of select="title" />
      </td>
      <td>
        <xsl:value-of select="presenter" />
      </td>
      <td>
        <xsl:value-of select="seriestitle" />
      </td>
      <td class="td-TimeDate">
        <span class="date-start">
          <xsl:value-of select="startTime" />
        </span>
        <span class="date-end">
          <xsl:value-of select="endTime" />
        </span>
      </td>
      <!-- <td>
        <xsl:value-of select="captureAgent" />
      </td> -->
      <td class="processingStatus">
        <xsl:choose>
        <xsl:when test="errors!=''">
            <xsl:value-of select="failedOperation" /><br/>
              <a style="font-size:x-small;">
                <xsl:attribute name="onclick">
                   $(this).text(($(this).text() == 'Show Details') ? 'Hide Details' : 'Show Details');
                   <xsl:text>$('#</xsl:text>
                        <xsl:value-of select="id" />
                        <xsl:text>_error').toggleClass('hidden');
                  </xsl:text>
                </xsl:attribute>
                <xsl:attribute name="onmouseover">
                     <xsl:text>this.className='cursor';</xsl:text>
                </xsl:attribute>
                <xsl:text>Show Details</xsl:text>
              </a>
              <div>
                <xsl:attribute name="class">hidden</xsl:attribute>
                <xsl:attribute name="id">
                  <xsl:value-of select="id" />
                  <xsl:text>_error</xsl:text>
                </xsl:attribute>
                <xsl:for-each select="errors">
                  <br/>
                  <xsl:value-of select="error" />
                </xsl:for-each>
              </div>
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="recordingStatus" /> :
            <xsl:value-of select="processingStatus" />
        </xsl:otherwise>
        </xsl:choose>
      </td>
      <td class="td-Action">
        <!-- a>
          <xsl:attribute name="onclick">
            <xsl:text>Recordings.retryRecording('</xsl:text>
            <xsl:value-of select="id" />
            <xsl:text>');</xsl:text>
          </xsl:attribute>
          Re-try
        </a -->
        <a title="View Recording Info">
            <xsl:attribute name="href">/admin/viewevent.html?id=<xsl:value-of select="id" /><xsl:text>&amp;type=</xsl:text><xsl:value-of select="itemType" /></xsl:attribute>
          View Info
          </a>
          <br />
        <xsl:choose>
          <xsl:when test="itemType='SCHEDULER_EVENT'">
            <a>
              <xsl:attribute name="onclick">
                <xsl:text>Recordings.removeScheduledEvent('</xsl:text>
                <xsl:value-of select="id" />
                <xsl:text>','</xsl:text>
                <xsl:value-of select="title" />
                <xsl:text>');</xsl:text>
              </xsl:attribute>
              Delete
            </a>
          </xsl:when>
          <xsl:when test="itemType='WORKFLOW'">
            <xsl:if test="zip!=''">
              <a>
                <xsl:attribute name="href">
                  <xsl:value-of select="zip" />
                </xsl:attribute>Download
              </a>
              <br/>
            </xsl:if>
            <a>
              <xsl:attribute name="onclick">
                <xsl:text>Recordings.removeRecording('</xsl:text>
                <xsl:value-of select="id" />
                <xsl:text>');</xsl:text>
              </xsl:attribute>
              <xsl:attribute name="onmouseover">
                   <xsl:text>this.className='cursor';</xsl:text>
              </xsl:attribute>
              Delete
            </a>
          </xsl:when>
        </xsl:choose>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>
