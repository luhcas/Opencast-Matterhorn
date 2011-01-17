/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace search_Plugin
 */
Opencast.search_Plugin = (function ()
{
    // The Element to put the div into
    var element;
    // The Template to process
    var template =  '<table cellspacing="5" cellpadding="0" width="100%">' +
                    '{for s in segment}' +
                        '{if s.display}' +
                            '<tr style="background-color:${s.backgroundColor};">' +
                                '<td width="15%" class="oc-segments-preview" style="cursor:pointer;cursor:hand;">' +
                                    '<a onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})"><img width="111" alt="Slide ${parseInt(s.index) + 1} of ${segment.length}" src="${s.previews.preview.$}"></a>' +
                                '</td>' +
                                '<td width="85%" align="left" onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})" style="cursor:pointer;cursor:hand;">' +
                                    '&nbsp;<a class="segments-time"' +
                                        'onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})">' +
                                        '${Opencast.engage.formatSeconds(Math.floor(parseInt(s.time) / 1000))}' +
                                    '</a>' +
                                    '&nbsp;<a onclick="Opencast.Watch.seekSegment(${Math.floor(parseInt(s.time) / 1000)})">${s.text}</a>' +
                                '</td>' +
                            '</tr>' +
                        '{/if}' +
                    '{forelse}' +
                        'No Segment Text available' +
                    '{/for}' +
                    '</table>';

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
     */
    function addAsPlugin(elem, data, value)
    {
        element = elem;
        search_data = data;
        
        search_value = value;
        createSearch();
    }

    /**
     * @memberOf Opencast.search_Plugin
     * @description Processes the Data and puts it into the Element
     */
    function createSearch()
    {
        if (element !== undefined)
        {
            if (search_value !== '')
            {
                var newTemplate = 'Results for &quot;' + unescape(search_value) + '&quot;<br/>' + template;
                processedTemplate = newTemplate.process(search_data);
            }
            else
            {
                processedTemplate = template.process(search_data);
            }
            element.html(processedTemplate);
        }
    }

    return {
        addAsPlugin: addAsPlugin
    };
}());
