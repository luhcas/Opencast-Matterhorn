<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns1="http://capture.admin.opencastproject.org" version="1.0">
    <xsl:output method="html"/>

    <xsl:template match="/">
        <table id="seriesTable" class="fl-theme-coal wu-table-list" width="100%" style="float:left;">
            <thead>
                <tr>
                    <th width="25%" class="sortable">Title</th>
                    <th width="25%" class="sortable">Organizer</th>
                    <th width="25%" class="sortable">Department</th>
                    <!--<th width="25%">Action</th>-->
                </tr>
            </thead>
            <tbody>
                <xsl:for-each select="seriesLists/series">
                    <tr class="highlightable">
                        <td>
                          <xsl:value-of select="title" />
                        </td>
                        <td>
                          <xsl:value-of select="contributor" />
                        </td>
                        <td>
                          <xsl:value-of select="creator" />
                        </td>
                    </tr>
                </xsl:for-each>
            </tbody>
        </table>
    </xsl:template>

</xsl:stylesheet>
