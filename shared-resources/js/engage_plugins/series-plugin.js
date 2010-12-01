/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace Description Plugin
 */
Opencast.Series_Plugin = (function(){

     /**
     *  Variables
     *  template: trimmpath render template
     */
    var element; //place to render the data in the html
    var template =  '{if pages.length != 1}' +
                        '<div style="float: right; padding: 5px;">' +
                            '{for p in pages}' +
                                '{if p == currentPage}' +
                                    '<span>${p}</span>&nbsp;' +
                                '{else}' +
                                    '<a href="javascript:" onclick="Opencast.Series.showSeriesPage(${p})">${p}</a>&nbsp;' +
                                '{/if}' +
                            '{/for}' +
                        '</div>' +
                    '{/if}' +
                    '<div style="padding: {if pages.length != 1}30px{else}10px{/if} 10px 10px 10px;">' +
                        '{for episode in result}' +
                            '<span style="font-weight: bold;">${episode.dcCreated}</span> ' +
                            '{if episode.id == currentMediaPackageId}' +
                                '<span title="${episode.dcTitle}"> ${episode.dcNumber}: ${episode.dcTitleShort} ' +
                                '${episode.dcCreator}</span><br />' +
                            '{else}' +
                                ' <a href="watch.html?id=${episode.id}" title="${episode.dcTitle}">${episode.dcNumber}: ${episode.dcTitleShort}</a> ' +
                                '<span>${episode.dcCreator}</span><br />' +
                            '{/if}' +
                        '{/for}' +
                    '</div>';
    var series_data;
    var processedTemplateData;

    function addAsPlugin(elem, data){
        element = elem;
        series_data = data;
        createSeriesDropdown();
    }

    function createSeriesDropdown()
    {
        if (element !== undefined) 
        {
            processedTemplateData = template.process(series_data);
            element.html(processedTemplateData);
        }
    }

    return {
        addAsPlugin: addAsPlugin
    };
}());
