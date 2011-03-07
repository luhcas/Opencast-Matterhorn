/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace Annotation Plugin
 */
Opencast.Annotation_ChapterPlugin = (function(){
    
    //place to render the data in the html
    var template = '<table ' +
                      'id="annotation_holder" ' +
                      'cellspacing="0" ' +
                      'cellpadding="0" ' +
                      'style="float:left;opacity: 0.65;" ' +
                      'class="segments">' +
                      '<tbody>' +
                           '<tr>' +
                               '{for a in annotation}' +
                                   '<td ' +
                                     'onclick="Opencast.Watch.seekSegment(${a.inpoint})" ' +
                                     'alt="Slide ${a.annotationId} of ${total}" ' +
                                     'onmouseout="Opencast.segments_ui.hoverOutDescription(\'segment-${a.annotationId}\',\'${a.value}\')" ' +
                                     'onmouseover="Opencast.segments_ui.hoverDescription(\'segment-${a.annotationId}\',\'${a.value}\')" ' +
                                     'id="segment-${a.annotationId}" ' +
                                     'style="width: ${parseInt(a.length) / parseInt(duration) * 100}%;" ' +
                                     'class="segment-holder-over ui-widget ui-widget-content">' +
                                   '</td>' +
                                 '{/for}' +
                            '</tr>' +
                        '</tbody>' +
                    '</table>';
    
    // The Element to put the div into
    var element;
    // Data to process
    var annotation_chapterData;
    // Processed Data
    var processedTemplateData;

    /**
     * @memberOf Opencast.Annotation_ChapterPlugin
     * @description Add As Plug-in
     * @param elem Element to put the Data into
     * @param data The Data to process
     * @return true if successfully processed, false else
     */
    function addAsPlugin(elem, data){
        element = elem;
        annotation_chapterData = data;
        return drawAnnotation_Chapter();
    }

    /**
     * @memberOf Opencast.Annotation_ChapterPlugin
     * @description Resize Plug-in
     * @return true if successfully processed, false else
     */
    function resizePlugin(){
        return drawAnnotation_Chapter();
    }

    /**
     * @memberOf Opencast.Annotation_ChapterPlugin
     * @description Add annotations into template element
     * processing the template with service data
     * @return true if successfully processed, false else
     */
    function drawAnnotation_Chapter(){
        if((element !== undefined) && (annotation_chapterData.annotation !== undefined) && (annotation_chapterData.annotation.length > 0))
        {
            processedTemplateData = template.process(annotation_chapterData);
            element.html(processedTemplateData);
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
