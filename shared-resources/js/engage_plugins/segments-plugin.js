/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {
};

/**
 * @namespace the global Opencast namespace segments Plugin
 */
Opencast.segments_Plugin = (function ()
{

    // The Element to put the div into
    var element;
    // The Template to process
    var template = '{for s in segment}' +
                        '<div id="panel_${s.index}" class="panel" style="float: left; position: relative;">' +
                            '<div class="inside" ' +
                                'onmouseover="Opencast.Watch.hoverSegment(\'segment${parseInt(s.index) + 1}\')" ' +
                                'onmouseout="Opencast.Watch.hoverOutSegment(\'segment${parseInt(s.index) + 1}\')">' +
                                    '<a href="javascript:Opencast.Watch.seekSegment(${parseInt(s.time) / 1000})">' +
                                        '<img width="111" alt="Slide ${parseInt(s.index) + 1} of ${segment.length}" ' +
                                            'src="${s.previews.preview.$}">' +
                                    '</a>' +
                            '</div>' +
                        '</div>' +
                    '{forelse}' +
                        'No Segments available' +
                    '{/for}';

    // Data to process
    var segments_data;
    // Precessed Data
    var processedTemplateData;

    /**
     * @memberOf Opencast.segments-Plugin
     * @description Add As Plug-in
     */
    function addAsPlugin(elem, data)
    {
        element = elem;
        segments_data = data;
        createSegments();
    }

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
