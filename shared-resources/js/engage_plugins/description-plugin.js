/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace Description Plugin
 */
Opencast.Description_Plugin = (function(){

    /**
     *  Variables
     *  template: trimmpath render template
     */
    var element; //place to render the data in the html
    var template =  '<div style="float: left;">' +
                        '<b>Date:</b> ${result.dcCreated}<br />' +
                        '<b>Sponsoring Department:</b> ${result.dcContributor}<br />' +
                        '<b>Language:</b> ${result.dcLanguage}<br />' +
                        '<b>Views:</b> ${result.dcViews}<br />' +
                    '</div>' +
                    '<div style="float: right; margin-right: 300px;">' +
                        '<b>See related Video:</b> <br />' +
                        '<b>Series:</b> <br />' +
                        '<b>Presenter:</b> ${result.dcCreator}<br />' +
                        '<b>Contents:</b> <br />' +
                    '</div>' +
                    '<div style="clear: both"></div>';
    var description_data;
    var processedTemplateData;

    /**
     * @memberOf Opencast.Description_Plugin
     * @description Add As Plug-in
     */
    function addAsPlugin(elem, data){
        element = elem;
        description_data = data;
        createDescription();
    }

    function createDescription(){
        if (element !== undefined) {
            processedTemplateData = template.process(description_data);
            $('div#oc_description').html( processedTemplateData);
        }
    }
    
    return {
        addAsPlugin: addAsPlugin
    };
}());
