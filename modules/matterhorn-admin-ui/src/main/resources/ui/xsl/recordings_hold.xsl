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
          <th width="30%" class="sortable">Title</th>
          <th width="15%" class="sortable">Presenter</th>
          <th width="20%" class="sortable">Course/Series</th>
          <th width="20%" class="sortable date-column">Recording Date &amp; Time</th>
          <th width="15%" class="sortable">Status</th>
          <th width="15%" class="sortable">Action</th>
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
          <span class="time-raw" style="display:none;"><xsl:value-of select="startTime" /></span>
        </span>
      </td>
      <td>
        <xsl:value-of select="holdOperationTitle" />
      </td>
      <td>
        <a>
          <xsl:attribute name="href">
            <xsl:text>javascript:Recordings.displayHoldActionPanel('</xsl:text><xsl:value-of select="holdActionPanelURL" /><xsl:text>','</xsl:text><xsl:value-of select="id" /><xsl:text>');</xsl:text>
          </xsl:attribute>
          <xsl:value-of select="holdActionTitle" />
        </a>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>
