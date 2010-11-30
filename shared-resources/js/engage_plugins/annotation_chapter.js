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
     * true if annotation chapter is displayed
     */
    var annotationChapterDisplayed = false;

    /**
     * @memberOf Opencast.Annotation_Chapter
     * @description Show Annotation_Chapter
     */
    function showAnnotation_Chapter(){
		//$.getJSON('../../annotation/rest/annotations.json?type=chapter', function(data) {
        /*
         * Load some testing demodata
         */
        $.getJSON('js/engage_plugins/demodata/annotation_demo.json', function(data){
            Opencast.Annotation_ChapterPlugin.addAsPlugin('#annotation', data, duration);
        });
        $('#segmentstable').hide();
        $("#annotation").show();
    }

    /**
     * @memberOf Opencast.Annotation_Chapter
     * @description Hide the Annotation
     */
    function hideAnnotation_Chapter(){
        $("#annotation").hide();
        $('#segmentstable').show();
    }

    /**
     * @memberOf Opencast.Analytics
     * @description Toggle Analytics
     */
    function doToggleAnnotation_Chapter(){
        if (!annotationChapterDisplayed) {
            showAnnotation_Chapter();
            annotationChapterDisplayed = true;

        }
        else {
            hideAnnotation_Chapter();
            annotationChapterDisplayed = false;
        }
        return true;
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
