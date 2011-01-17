/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace annotation_chapter delegate. This file contains the rest endpoint and passes the data to the annotation_chapter plugin
 */
Opencast.Annotation_Chapter = (function ()
{
    var mediaPackageId, duration;
    var annotationChapterDisplayed = false;
    var ANNOTATION_CHAPTER = "Annotation",
        ANNOTATION_CHAPTERHIDE = "Annotation off";
    var annotationDataURL = '../../annotation/rest/annotations.json'; // Test-Data at "js/engage_plugins/demodata/annotation_demo.json"
    
    /**
     * @memberOf Opencast.Annotation_Chapter
     * @description Initializes Annotation Chapter
     *              Checks whether Data are available. If not: Grey out Annotation Checkbox
     */
    function initialize()
    {
        // Request JSONP data
        $.ajax(
        {
            url: annotationDataURL,
            data: 'id=' + mediaPackageId,
            dataType: 'json',
            jsonp: 'jsonp',
            success: function (data)
            {
                var tmpData = data['annotations'];
                if ((tmpData !== undefined) && (tmpData.annotation !== undefined))
                {
                    return;
                }
                // Don't display anything + make unavailable
                $("#annotation").html("No Annotations available");
                $('#oc_checkbox-annotations').removeAttr("checked");
                $('#oc_checkbox-annotations').attr('disabled', true);
            },
            // If no data comes back
            error: function (xhr, ajaxOptions, thrownError)
            {
                // Don't display anything
                $("#annotation").html("No Annotations available");
                $('#oc_checkbox-annotations').removeAttr("checked");
                hideAnnotation_Chapter();
            }
        });
    }
    
    /**
     * @memberOf Opencast.Annotation_Chapter
     * @description Show Annotation_Chapter
     */
    function showAnnotation_Chapter()
    {
        // Request JSONP data
        $.ajax(
        {
            url: annotationDataURL,
            data: 'id=' + mediaPackageId,
            dataType: 'json',
            jsonp: 'jsonp',
            success: function (data)
            {
                var tmpData = data['annotations'];
                if ((tmpData !== undefined) && (tmpData.annotation !== undefined))
                {
                    tmpData.duration = duration;
                    // Create Trimpath Template
                    Opencast.Annotation_ChapterPlugin.addAsPlugin($('#annotation'), tmpData);
                    annotationChapterDisplayed = true;
                    var analyticsVisible = Opencast.Analytics.isVisible();
                    // If Analytics is visible: Hide it before changing
                    if (analyticsVisible)
                    {
                        Opencast.Analytics.hideAnalytics();
                    }
                    $('#segmentstable').css('segment-holder-empty', 'none');
                    $("#annotation").show();
                    $('#segmentstable1').hide();
                    $('#segmentstable2').hide();
                    // If Analytics was visible: Display it again
                    if (analyticsVisible)
                    {
                        Opencast.Analytics.showAnalytics();
                    }
                    return;
                }
                // Don't display anything
                $("#annotation").html("No Annotations available");
                $('#oc_checkbox-annotations').removeAttr("checked");
                hideAnnotation_Chapter();
            },
            // If no data comes back
            error: function (xhr, ajaxOptions, thrownError)
            {
                // Don't display anything
                $("#annotation").html("No Annotations available");
                $('#oc_checkbox-annotations').removeAttr("checked");
                hideAnnotation_Chapter();
            }
        });
    }
    
    /**
     * @memberOf Opencast.Annotation_Chapter
     * @description Hide the Annotation
     */
    function hideAnnotation_Chapter()
    {
        $("#annotation").hide();
        $('#segmentstable1').show();
        $('#segmentstable2').show();
        annotationChapterDisplayed = false;
    }
    
    /**
     * @memberOf Opencast.Analytics
     * @description Toggle Analytics
     */
    function doToggleAnnotation_Chapter()
    {
        if (!annotationChapterDisplayed)
        {
            showAnnotation_Chapter();
        }
        else
        {
            hideAnnotation_Chapter();
        }
        return true;
    }
    
    /**
     * @memberOf Opencast.Analytics
     * @description Set the mediaPackageId
     * @param String mediaPackageId
     */
    function setMediaPackageId(id)
    {
        mediaPackageId = id;
    }
    
    /**
     * @memberOf Annotation_Chapter
     * @description Set the duration
     * @param int duration
     */
    function setDuration(val)
    {
        duration = val;
    }
    
    return {
        initialize: initialize,
        hideAnnotation_Chapter: hideAnnotation_Chapter,
        showAnnotation_Chapter: showAnnotation_Chapter,
        setDuration: setDuration,
        setMediaPackageId: setMediaPackageId,
        doToggleAnnotation_Chapter: doToggleAnnotation_Chapter
    };
}());
