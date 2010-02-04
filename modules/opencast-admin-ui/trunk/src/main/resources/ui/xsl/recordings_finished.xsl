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
    <div style="border: 1px solid #cccccc;font-size:0.9em;text-align:center;width:600px;margin-left:auto;margin-right:auto;margin-bottom:0.5em;padding:0.5em;">
      All media files on this page should appear in the Matterhorn Media Module.
    </div>
    <table id="recordingsTable" class="fl-theme-coal wu-table-list" width="100%" style="float:left;">
      <thead>
        <tr>
          <th width="30%" class="sortable">Title</th>
          <th width="17%" class="sortable">Presenter</th>
          <th width="20%" class="sortable">Course/Series</th>
          <th width="20%" class="sortable date-column">Recording Date &amp; Time</th>
          <!-- <th width="10%" class="sortable">Capture Agent</th> -->
          <th width="13%" class="sortable">Status</th>
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
        <a title="View Recording Info">
          <xsl:attribute name="href">/admin/viewevent.html?workflow=<xsl:value-of select="id" /></xsl:attribute>
          <xsl:value-of select="title" />
        </a>
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
          <span class="time-raw" style="display:none;"><xsl:value-of select="startTime" /></span>
        </span>
        <!-- <span class="date-end">
          <xsl:value-of select="endTime" />
        </span> -->
      </td>
      <!-- <td>
        <xsl:value-of select="captureAgent" />
      </td> -->
      <td class="processingStatus">
        <xsl:value-of select="processingStatus" />
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>
