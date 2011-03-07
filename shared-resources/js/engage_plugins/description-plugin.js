/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace Description Plugin
 */
Opencast.Description_Plugin = (function ()
{
    // The Template to process
    var template =  '<div style="float: left;">' +
                        'Date: <span style="color:grey;">${result.dcCreated}</span><br />' +
                        'Department: <span style="color:grey;">${result.dcContributor}</span><br />' +
                        'Language: <span style="color:grey;">${result.dcLanguage}</span><br />' +
                        'Views: <span style="color:grey;">${result.dcViews}</span><br />' +
                    '</div>' +
                    '<div style="float: right; margin-right: 300px;">' +
                        // 'See related Videos: <span style="color:grey;"></span><br />' +
                        'Series: ${result.dcSeriesTitle}<br />' +
                        'Presenter: <a href="../../engage/ui/index.html?q=${result.dcCreator}">${result.dcCreator}</a></span><br />' +
                        // 'Contents: <br />' +
                    '</div>' +
                    '<div style="clear: both">' + 
                    '</div>';

    // The Element to put the div into
    var element;
    // Data to process
    var description_data;
    // Precessed Data
    var processedTemplateData = false;

    /**
     * @memberOf Opencast.Description_Plugin
     * @description Add As Plug-in
     * @param elem Element to fill with the Data (e.g. a div)
     * @param data Data to fill the Element with
     * @return true if successfully processed, false else
     */
    function addAsPlugin(elem, data)
    {
        element = elem;
        description_data = data;
        return createDescription();
    }
    
    /**
     * @memberOf Opencast.Description_Plugin
     * @description Tries to work with the cashed data
     * @return true if successfully processed, false else
     */
    function createDescriptionFromCashe()
    {
        if((processedTemplateData !== false) && (element !== undefined) && (description_data !== undefined))
        {
            element.html(processedTemplateData);
            return true;
        } else
        {
            return false;
        }
    }

    /**
     * @memberOf Opencast.Description_Plugin
     * @description Processes the Data and puts it into the Element
     * @return true if successfully processed, false else
     */
    function createDescription()
    {
        if ((element !== undefined) && (description_data !== undefined))
        {
            processedTemplateData = template.process(description_data);
            element.html(processedTemplateData);
            return true;
        } else
        {
            return false;
        }
    }

    return {
        createDescriptionFromCashe: createDescriptionFromCashe,
        addAsPlugin: addAsPlugin
    };
}());
