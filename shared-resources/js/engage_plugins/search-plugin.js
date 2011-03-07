/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace search_Plugin
 */
Opencast.search_Plugin = (function ()
{
    // The Template to process
    var template =  '<table cellspacing="5" cellpadding="0" style="table-layout:fixed; empty-cells:hide">' +
                    '{for s in segment}' +
                        '{if s.display}' +
                            '<tr style="background-color:${s.backgroundColor};cursor:pointer;cursor:hand;">' +
                                '<td style="width:115px" class="oc-segments-preview">' +
                                    '<a onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})"><img width="111" alt="Slide ${parseInt(s.index) + 1} of ${segment.length}" src="${s.previews.preview.$}"></a>' +
                                '</td>' +
                                '<td style="width:90px; text-align:center;" onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})">' +
                                    '<a class="segments-time"' +
                                        'onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})">' +
                                        '${Opencast.Utils.formatSeconds(Math.floor(parseInt(s.time) / 1000))}' +
                                    '</a>' +
                                '</td>' +
                                '<td style="text-align:left;" onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})">' +
                                    '<a onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})">${s.text}</a>' +
                                '</td>' +
                            '</tr>' +
                        '{/if}' +
                    '{forelse}' +
                        'No Segment Text available' +
                    '{/for}' +
                    '</table>';
    
    function getHeader(searchValue)
    {
        var ret = '<div id="searchValueDisplay" style="float:left">' +
                      'Results for &quot;' + unescape(searchValue) + '&quot;' +
                  '</div>';
        ret +=  '<div id="relevance-overview">' +
                    '<div style="text-align: center; margin-left: 1px; margin-right: 1px; border: 0px solid black;width: 60px; height: 15px; background-color: ' + Opencast.search.getThirdColor() + '; float: right">' +
                        '&gt; 70&#37;' +
                    '</div>' +
                    '<div style="text-align: center; margin-left: 1px; margin-right: 1px; border: 0px solid black;width: 60px; height: 15px; background-color: ' + Opencast.search.getSecondColor() + '; float: right">' +
                        '&lt; 70&#37;' +
                    '</div>' +
                    '<div style="text-align: center; margin-left: 1px; margin-right: 1px; border: 0px solid black;width: 60px; height: 15px; background-color: ' + Opencast.search.getFirstColor() + '; float: right">' +
                        '&lt; 30&#37;' +
                    '</div>' +
                    '<div style="float:right">' +
                        'Search Relevance:&nbsp;' +
                    '</div>' +
                '</div>';
        return ret;
    }

    // The Element to put the div into
    var element;
    // Data to process
    var search_data;
    // Precessed Data
    var processedTemplate = '';
    // Search Value
    var search_value = '';

    /**
     * @memberOf Opencast.search_Plugin
     * @description Add As Plug-in
     * @param elem Element to fill with the Data (e.g. a div)
     * @param data Data to fill the Element with
     * @return true if successfully processed, false else
     */
    function addAsPlugin(elem, data, value)
    {
        element = elem;
        search_data = data;
        
        search_value = value;
        return createSearch();
    }

    /**
     * @memberOf Opencast.search_Plugin
     * @description Processes the Data and puts it into the Element
     * @return true if successfully processed, false else
     */
    function createSearch()
    {
        if (element !== undefined)
        {
            if (search_value !== '')
            {
                var newTemplate = getHeader(search_value) + '<br />' + template;
                processedTemplate = newTemplate.process(search_data);
            }
            else
            {
                processedTemplate = template.process(search_data);
            }
            element.html(processedTemplate);
            return true;
        } else
        {
            return false;
        }
    }

    return {
        addAsPlugin: addAsPlugin
    };
}());
