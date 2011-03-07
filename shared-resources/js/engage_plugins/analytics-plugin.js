/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace analyticsPlugin
 */
Opencast.AnalyticsPlugin = (function ()
{
    // The Element to put the div into
    var element;
    // Data to process
    var footprintData;
    
    /**
     * @memberOf Opencast.Analytics
     * @description Add As Plug-in
     * @param elem Element to put the Data in
     * @param data The Data to Process
     * @return true if successfully processed, false else
     */
    function addAsPlugin(elem, data)
    {
        element = elem;
        footprintData = data;
        return drawFootprints();
    }
    
    /**
     * @memberOf Opencast.Analytics
     * @description Resize Plug-in
     * @return true if successfully processed, false else
     */
    function resizePlugin()
    {
        return drawFootprints();
    }
    
    /**
     * @memberOf Opencast.Analytics
     * @description Draw footprintData into the element
     * @return true if successfully processed, false else
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
            return true;
        } else
        {
            return false;
        }
    }
    
    return {
        addAsPlugin: addAsPlugin,
        resizePlugin: resizePlugin
    };
}());
