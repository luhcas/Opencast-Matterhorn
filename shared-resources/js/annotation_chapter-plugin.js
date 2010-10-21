/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace Annotation Plugin
 */
Opencast.Annotation_ChapterPlugin = ( function() {

  /**
   *  Variables
   *  template: trimmpath render template
   */
  var element; //place to render the data
  var template ="<table cellspacing=\"0\" cellpadding=\"0\" style=\"opacity: 0.65;\" class=\"segments\"><tbody><tr>{for a in annotations.annotation}<td onclick=\"Opencast.Watch.seekSegment(${a.inpoint})\" alt=\"Slide 1 of 2\" onmouseout=\"Opencast.Watch.hoverOut('segment-${a.annotationId}','${a.value}')\" onmouseover=\"Opencast.Watch.hoverDescription('segment-${a.annotationId}','${a.value}')\" id=\"segment-${a.annotationId}\" style=\"width: ${(a.length*100/duration)}%;\" class=\"segment-holder-over\"></td>{/for}</tr></tbody></table>";
  var annotation_chapterData;
  var processedTemplateData;
 
  /**
   * @memberOf Opencast.Annotation_ChapterPlugin
   * @description Add As Plug-in
   */
  function addAsPlugin (elem, data, mediaduration) {
      element = elem;
      annotation_chapterData = data;
	  duration = mediaduration;
      drawAnnotation_Chapter();
  }

  /**
   * @memberOf Opencast.Annotation_ChapterPlugin
   * @description Resize Plug-in
   */
  function resizePlugin () {
    drawAnnotation_Chapter();
  }

  /**
   * @memberOf Opencast.Analytics
   * @description Draw footprintData into the element
   * processing the template with service data
   */
  function drawAnnotation_Chapter() {
    if(element !== undefined) {	
    processedTemplateData = template.process(annotation_chapterData);
    document.getElementById('annotation').innerHTML=processedTemplateData;
    $('#segmentstable').css('display','none');
    $('#segmentstable').css('segment-holder-empty','none');
    }
    else{
    alert("target element is not defined.");
    }
  }

  return {
    addAsPlugin : addAsPlugin,
    resizePlugin : resizePlugin
  };
}());