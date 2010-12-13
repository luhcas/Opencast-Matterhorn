/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {
};

/**
 * @namespace the global Opencast namespace Description Plugin
 */
Opencast.Description_Plugin = (function ()
{
    // The Element to put the div into
    var element;
    // The Template to process
    var template =  '<div style="float: left;">' +
                        'Date: <span style="color:grey;">${result.dcCreated}</span><br />' +
                        'Sponsoring Department: <span style="color:grey;">${result.dcContributor}</span><br />' +
                        'Language: <span style="color:grey;">${result.dcLanguage}</span><br />' +
                        'Views: <span style="color:grey;">${result.dcViews}</span><br />' +
                    '</div>' +
                    '<div style="float: right; margin-right: 300px;">' +
                        'See related Videos: <span style="color:grey;"></span><br />' +
                        'Series: <span style="color:grey;">${result.dcSeriesTitle}</span><br />' +
                        'Presenter: <span style="color:grey;"><a href="../../engage/ui/index.html?q=${result.dcCreator}">${result.dcCreator}</a></span><br />' +
                        'Contents: <span style="color:grey;"></span><br />' +
                    '</div>' +
                    '<div style="clear: both">' + 
                    '</div>';

    // Data to process
    var description_data;
    // Precessed Data
    var processedTemplateData;

    /**
     * @memberOf Opencast.Description_Plugin
     * @description Add As Plug-in
     */
    function addAsPlugin(elem, data)
    {
        element = elem;
        description_data = data;
        createDescription();
    }

    function createDescription()
    {
        if (element !== undefined)
        {
            processedTemplateData = template.process(description_data);
            element.html(processedTemplateData);
        }
    }

    return {
        addAsPlugin: addAsPlugin
    };
}());
