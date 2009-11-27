<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : recordings_upcoming.xsl
    Created on : November 19, 2009, 9:18 PM
    Author     : wulff
    Description:
        Transforms output of /scheduler/rest/getUpcommingEvents to HTML table
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns1="http://scheduler.opencastproject.org" version="1.0">
    <xsl:output method="html"/>

    <xsl:template match="/">
        <table class="fl-theme-coal wu-table-list" width="100%" style="float:left;">
            <thead>
                <tr>
                    <th width="30%">Title</th>
                    <th width="20%">Presenter</th>
                    <th width="20%">Course/Series</th>
                    <th width="20%">Recording Date and Time</th>
                    <th width="10%">Capture Agent</th>
                </tr>
            </thead>
            <tbody>
                <xsl:for-each select="ns1:SchedulerEvents/ns1:SchedulerEvent">
                    <tr>
                        <td> <xsl:value-of select="metadata/item[@key='title']" /> </td>
                        <td> <xsl:value-of select="metadata/item[@key='creator']" /> </td>
                        <td> <xsl:value-of select="metadata/item[@key='series-id']" /> </td>
                        <td class="td-TimeDate"><span class="date-start"><xsl:value-of select="start" /></span> - <span class="date-end"><xsl:value-of select="enddate" /></span></td>
                        <td> <xsl:value-of select="metadata/item[@key='device']" /> </td>
                    </tr>
                </xsl:for-each>
            </tbody>
        </table>
    </xsl:template>

</xsl:stylesheet>
