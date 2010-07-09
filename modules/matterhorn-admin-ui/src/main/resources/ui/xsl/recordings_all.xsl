<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : recordings_finished.xsl
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
          <th width="20%" id="th-StartDate" class="sortable date-column recording-Table-head">Recording Date &amp; Time</th>
          <!-- <th width="10%" class="sortable">Capture Agent</th> -->
          <th width="15%" id="th-ProcessingStatus" class="sortable recording-Table-head">Status</th>
          <th width="15%" id="th-Action" class="sortable">Action</th>
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
          <xsl:value-of select="recordingStatus" /> :
          <xsl:value-of select="processingStatus" />
      </td>
      <td>
        <xsl:choose>
            <xsl:when test="recordingStatus = 'upcoming'">
              <a title="View Recording Info">
                <xsl:attribute name="href">/admin/scheduler.html?eventId=<xsl:value-of select="id" />&amp;edit</xsl:attribute>
                Edit
              </a>
              <a title="Delete Recording">
                <xsl:attribute name="onclick">
                  <xsl:text>Recordings.removeScheduledRecording('</xsl:text>
                  <xsl:value-of select="id" />
                  <xsl:text>', '</xsl:text>
                  <xsl:value-of select="title" />
                  <xsl:text>');</xsl:text>
                </xsl:attribute>
                Delete
              </a>
            </xsl:when>
            <xsl:otherwise>
              <a title="View Recording Info">
                <xsl:attribute name="href">/admin/viewevent.html?workflow=<xsl:value-of select="id" /></xsl:attribute>
                View
              </a>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:choose>
            <xsl:when test="recordingStatus = 'hold'">
                <input type="hidden">
                </input>
                <a>
                    <xsl:attribute name="onclick">
                      <xsl:text>Recordings.displayHoldActionPanel('</xsl:text>
                      <xsl:value-of select="holdActionPanelURL" />
                      <xsl:text>','</xsl:text>
                      <xsl:value-of select="id" />
                      <xsl:text>', this);</xsl:text>
                    </xsl:attribute>
                    <xsl:value-of select="holdActionTitle" />
                </a>
            </xsl:when>
            <xsl:when test="recordingStatus = 'failed'">
                <a>
                    <xsl:attribute name="onclick">
                        <xsl:text>Recordings.retryRecording('</xsl:text>
                        <xsl:value-of select="id" />
                        <xsl:text>');</xsl:text>
                    </xsl:attribute>
                    Re-try
                </a>
		        <a>
		          <xsl:attribute name="onclick">
		            <xsl:text>Recordings.removeRecording('</xsl:text>
		            <xsl:value-of select="id" />
		            <xsl:text>');</xsl:text>
		          </xsl:attribute>
		          Delete
		        </a>
            </xsl:when>
            <xsl:otherwise></xsl:otherwise>
        </xsl:choose>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>
