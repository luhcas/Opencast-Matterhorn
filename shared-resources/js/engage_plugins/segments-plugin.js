/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace segments_Plugin
 */
Opencast.segments_Plugin = (function ()
{
    // The Template to process
    var template = '{for s in segment}' +
                        '<div id="panel_${s.index}" class="panel" style="float: left; position: relative;">' +
                            '<div role="button" class="inside" ' +
                                'onmouseover="Opencast.segments_ui.hoverSegment(${parseInt(s.index)})" ' +
                                'onmouseout="Opencast.segments_ui.hoverOutSegment(${parseInt(s.index)})">' +
                                    '<a href="javascript:Opencast.Watch.seekSegment(${parseInt(s.time) / 1000})">' +
                                        '<img width="111" alt="Slide ${parseInt(s.index) + 1} of ${segment.length}" ' +
                                            'src="${s.previews.preview.$}">' +
                                    '</a>' +
                            '</div>' +
                        '</div>' +
                    '{forelse}' +
                        'No Segments available' +
                    '{/for}';

    // The Element to put the div into
    var element;
    // Data to process
    var segments_data;
    // Processed Data
    var processedTemplateData;

    /**
     * @memberOf Opencast.segments_Plugin
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
     * @memberOf Opencast.segments_Plugin
     * @description Processes the Data and puts it into the Element
     */
    function createSegments()
    {
        if ((element !== undefined) && (segments_data.segment !==  undefined) && (segments_data.segment.length > 0))
        {
            processedTemplateData = template.process(segments_data);
            element.html(processedTemplateData);
        }
    }

    return {
        addAsPlugin: addAsPlugin
    };
}());
