/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace segments_text_Plugin
 */
Opencast.segments_text_Plugin = (function ()
{
    // The Template to process
    var template =  '<table cellspacing="5" cellpadding="0" width="100%">' +
                        '{for s in segment}' +
                            // Accessibility Feature - Comment in if you want to display only the Segments after the current Slider-Position
                            // '{if s.durationIncludingSegment >= currentTime}' +
                                '<tr>' +
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
                            // '{/if}' +
                        '{forelse}' +
                            'No Segment Text available' +
                        '{/for}' +
                    '</table>';

    // The Element to put the div into
    var element;
    // Data to process
    var segments_data;
    // Precessed Data
    var processedTemplateData;

    /**
     * @memberOf Opencast.segments_text_Plugin
     * @description Add As Plug-in
     * @param elem Element to fill with the Data (e.g. a div)
     * @param data Data to fill the Element with
     */
    function addAsPlugin(elem, data)
    {
        element = elem;
        segments_data = data;
        createSegments();
    }

    /**
     * @memberOf Opencast.segments_text_Plugin
     * @description Processes the Data and puts it into the Element
     */
    function createSegments()
    {
        if (element !== undefined)
        {
            processedTemplateData = template.process(segments_data);
            element.html(processedTemplateData);
        }
    }

    return {
        addAsPlugin: addAsPlugin
    };
}());
