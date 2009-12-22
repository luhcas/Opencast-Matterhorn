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
          <th width="25%">Title</th>
          <th width="15%">Presenter</th>
          <th width="20%">Course/Series</th>
          <th width="20%">Recording Date and Time</th>
          <th width="10%">Capture Agent</th>
          <th width="10%">Status</th>
        </tr>
      </thead>
      <tbody>
        <xsl:apply-templates />
      </tbody>
    </table>
  </xsl:template>

  <xsl:template match="ns1:recording">
    <tr>
      <td>
        <a>
          <xsl:attribute name="href">/scheduler/ui/viewevent.html?eventID=<xsl:value-of select="id" /></xsl:attribute>
          <xsl:value-of select="title" />
        </a>
      </td>
      <td>
        <xsl:value-of select="presenter" />
      </td>
      <td>
        <xsl:value-of select="series" />
      </td>
      <td class="td-TimeDate">
        <span class="date-start">
          <xsl:value-of select="startTime" />
        </span>
        <!-- <span class="date-end">
          <xsl:value-of select="endTime" />
        </span> -->
      </td>
      <td>
        <xsl:value-of select="captureAgent" />
      </td>
      <td>
        <xsl:value-of select="distributionStatus" />
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>