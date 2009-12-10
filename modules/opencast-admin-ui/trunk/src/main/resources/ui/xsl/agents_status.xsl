<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : agents_status.xsl
    Created on : November 19, 2009, 9:18 PM
    Author     : wulff
    Description:
        Transforms the output of /capture-admin/rest/GetKnownAgents into an HTML table
    TODO: add capability field (waiting for capture-admin-service)
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns1="http://capture.admin.opencastproject.org" version="1.0">
    <xsl:output method="html"/>

    <xsl:template match="/">
        <table class="fl-theme-coal wu-table-list" width="100%" style="float:left;">
            <thead>
                <tr>
                    <th width="33%">Agent Name</th>
                    <th width="33%">Capabilities</th>
                    <th width="33%">Status</th>
                </tr>
            </thead>
            <tbody>
                <xsl:for-each select="ns1:agent-state-updates/ns1:agent-state-update">
                    <tr>
                        <td>
                            <xsl:value-of select="name" />
                        </td>
                        <td>
                          Audio, Video, Screen
                        </td>
                        <td>
                            <span>
                                <xsl:attribute name="class">icon icon-<xsl:value-of select="state" /></xsl:attribute>
                            </span>
                            <span style="margin-left:3px;"><xsl:value-of select="state" /></span>
                        </td>
                    </tr>
                </xsl:for-each>
            </tbody>
        </table>
    </xsl:template>

</xsl:stylesheet>
