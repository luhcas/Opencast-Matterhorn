/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace annotation_chapter delegate. This file contains the rest endpoint and passes the data to the annotation_chapter plugin
 */
Opencast.Annotation_Chapter = (function(){

    /**
     *  variables
     */
    var mediaPackageId, duration, ANNOTATION_CHAPTER = "Annotation", ANNOTATION_CHAPTERHIDE = "Annotation off";
    
    /**
     * @memberOf Opencast.Annotation_Chapter
     * @description Show Annotation_Chapter
     */
    function showAnnotation_Chapter(){
        $("#oc_btn-annotation_chapter").attr({
            alt: ANNOTATION_CHAPTERHIDE,
            title: ANNOTATION_CHAPTERHIDE,
            value: ANNOTATION_CHAPTERHIDE
        });
        $("#oc_btn-annotation_chapter").attr('aria-pressed', 'true');
		//$.getJSON('../../usertracking/rest/annotations.json?key=chapter', function(data) {
        /*
         * Load some testing demodata
         */
        $.getJSON('js/engage_plugins/demodata/annotation_demo.json', function(data){
            Opencast.Annotation_ChapterPlugin.addAsPlugin('#annotation', data, duration);
        });
        $("#annotation").show();
    }
    
    /**
     * @memberOf Opencast.Annotation_Chapter
     * @description Hide the Annotation
     */
    function hideAnnotation_Chapter(){
        $("#oc_btn-annotation_chapter").attr({
            alt: ANNOTATION_CHAPTER,
            title: ANNOTATION_CHAPTER,
            value: ANNOTATION_CHAPTER
        });
        $("#oc_btn-annotation_chapter").attr('aria-pressed', 'false');
        $("#annotation").hide();
    }
    
    /**
     * @memberOf Opencast.Analytics
     * @description Toggle Analytics
     */
    function doToggleAnnotation_Chapter(){
        if ($("#oc_btn-annotation_chapter").attr("title") === ANNOTATION_CHAPTER) {
            showAnnotation_Chapter()
        }
        else {
            hideAnnotation_Chapter()
        }
    }
    
    /**
     * @memberOf Opencast.Analytics
     * @description Set the mediaPackageId
     * @param String mediaPackageId
     */
    function setMediaPackageId(id){
        mediaPackageId = id;
    }
    
    /**
     * @memberOf Annotation_Chapter
     * @description Set the duration
     * @param int duration
     */
    function setDuration(val){
        duration = val;
    }
    
    
    return {
        hideAnnotation_Chapter: hideAnnotation_Chapter,
        showAnnotation_Chapter: showAnnotation_Chapter,
        setDuration: setDuration,
        setMediaPackageId: setMediaPackageId,
        doToggleAnnotation_Chapter: doToggleAnnotation_Chapter
    };
}());
