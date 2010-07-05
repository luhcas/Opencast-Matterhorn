/*global $*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace search
 */
Opencast.search = (function () {

    /**
     * @memberOf Opencast.search
     * @description Comparator
     */
    function Numsort(a, b)
    {
        return a - b;
    }

    /**
     * @memberOf Opencast.search
     * @description Does the search
     */
    function showResult(value)
    {
        // url: "../../search/rest/episode", // Production
        // url: "episode-segments.xml",      // For Testing
        var mediaPackageId = Opencast.engage.getMediaPackageId();
        $.ajax(
        {
            type: 'GET',
            contentType: 'text/xml',
            url: "../../search/rest/episode",
            data: "id=" + mediaPackageId + "&q=" + escape(value),
            dataType: 'xml',
            success: function (xml) 
            {
                var rows = new Array();
                var ids = new Array();
                var sortedIds = new Array();
                var index = 0;

                var title = "";
                
                if (value !== "")
                {
                    title = "Results for &quot;" + unescape(value) + "&quot;<br/>";
                }
                $('#oc_slidetext-left').html(title);

                $(xml).find('segment').each(function ()
                {
                    var relevance = parseInt($(this).attr('relevance'), 10);
                    // Select only item with a relevanve value greater than zero
                    if (relevance !== 0 || value === "")
                    {
                        var text = $(this).find('text').text();
                        var seconds = parseInt($(this).attr('time'), 10) / 1000;
                        var id = relevance * 1000 + index;
                        var row = "";

                        text = text.replace(new RegExp(value, 'g'), '<span class="marked">' + value + '</span>');

                        row += '<tr>';
                        row += '<td class="oc-segments-time">';
                        row += '<a onclick="Opencast.Watch.seekSegment(' + seconds + ')" class="segments-time">';
                        row += Opencast.engage.formatSeconds(seconds);
                        row += "</a>";
                        row += "</td>";
                        row += "<td>";
                        row += text;
                        row += "</td>";
                        row += "</tr>";

                        sortedIds[index] = id;
                        ids[index] = id;
                        rows[index] = row;
                        index++;
                    }
                });

                // Sort array
                if (value !== "")
                {
                    sortedIds.sort(Numsort).reverse();
                }
                // Generate Table
                var table = "";
                table += '<table cellspacing="0" cellpadding="0"><tbody>';
                for (var i = 0; i < sortedIds.length; i++)
                {
                    for (var j = 0; j < ids.length; j++)
                    {
                        if (tedIds[i] === ids[j])
                        {
                            table += rows[j];
                        }
                    }
                }
                table += "</tbody></table>";

                // Append Table
                $('#oc_slidetext-left').append(table);

            },
            error: function (a, b, c) 
            {
            // Some error while trying to get the search result
            }
        }); //close ajax(
    }

    /**
     * @memberOf Opencast.search
     * @description Initializes the search view
     */
    function initialize()
    {
        // initialize
    }

    

    return {
        showResult : showResult,
        initialize : initialize
    };
}());