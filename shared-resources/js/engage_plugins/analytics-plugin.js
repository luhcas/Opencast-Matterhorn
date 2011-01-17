/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace analyticsPlugin
 */
Opencast.AnalyticsPlugin = (function ()
{
    /**
     *  Variables
     */
    var element;
    var footprintData;
    
    /**
     * @memberOf Opencast.Analytics
     * @description Add As Plug-in
     * @param elem Element to put the Data in
     * @param data The Data to Process
     */
    function addAsPlugin(elem, data)
    {
        element = elem;
        footprintData = data;
        drawFootprints();
    }
    
    /**
     * @memberOf Opencast.Analytics
     * @description Resize Plug-in
     */
    function resizePlugin()
    {
        drawFootprints();
    }
    
    /**
     * @memberOf Opencast.Analytics
     * @description Draw footprintData into the element
     */
    function drawFootprints()
    {
        if (element !== undefined)
        {
            element.sparkline(footprintData, {
                type: 'line',
                spotRadius: '0',
                width: '100%',
                height: '25px'
            });
        }
    }
    
    return {
        addAsPlugin: addAsPlugin,
        resizePlugin: resizePlugin
    };
}());
