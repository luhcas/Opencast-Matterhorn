/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace annotation_chapter delegate. This file contains the rest endpoint and passes the data to the annotation_chapter plugin
 * description AnnotationType can be defined according to your needs. The type can be specified as described in: ../..annotation/docs
 * this plugin is an example for annotation handling in engage;
 * it allows a user to attach chapters/segments in a video wich do not include segments from a previous media analysis
 */
Opencast.Annotation_Chapter = (function ()
{
    var mediaPackageId, duration;
    var annotationChapterDisplayed = false;
    var ANNOTATION_CHAPTER = "Annotation",
        ANNOTATION_CHAPTERHIDE = "Annotation off";
    
    var annotationType = "chapter";
    var annotationDataURL = '../../annotation/annotations.json'; // Test-Data can be found: "js/engage_plugins/demodata/annotation_demo.json"
    
    /**
     * @memberOf Opencast.Annotation_Chapter
     * @description Initializes Annotation Chapter
     *              Checks whether Data are available. If not: Hide Annotations
     */
    function initialize()
    {
        // Request JSONP data
        $.ajax(
        {
            url: annotationDataURL,
            data: 'episode=' + mediaPackageId + '&type=' + annotationType,
            dataType: 'json',
            jsonp: 'jsonp',
            success: function (data)
            {
                var tmpData = data['annotations'];
                if((tmpData !== undefined) && (tmpData.annotation !== undefined))
                {
                    // Display the controls
                    $('#oc_checkbox-annotations').show();
                    $('#oc_label-annotations').show();
                    $('#oc_video-view').show();
                    Opencast.Analytics.initialize();
                } else
                {
                    displayNoAnnotationsAvailable("No data available (undefined status, initialize)");
                }
            },
            // If no data comes back
            error: function (xhr, ajaxOptions, thrownError)
            {
                displayNoAnnotationsAvailable("No data available (1), initialize");
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
                if((data === undefined) || (data['annotations'] === undefined) || (data['annotations'].annotation === undefined))
                {
                    displayNoAnnotationsAvailable("No data defined (1)");
                } else
                {
                    tmpData.duration = duration;
                    // Create Trimpath Template
                    var annotSet = Opencast.Annotation_ChapterPlugin.addAsPlugin($('#annotation'), data['annotations'].annotation);
                    if(annotSet)
                    {
                        displayNoAnnotationsAvailable("No template available (1)");
                    } else
                    {
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
                    }
                }
            },
            // If no data comes back
            error: function (xhr, ajaxOptions, thrownError)
            {
                displayNoAnnotationsAvailable("No data available (1)");
            }
        });
    }
    
    /**
     * @memberOf Opencast.Annotation_Chapter
     * @description Displays that no Annotation is available and hides Annotations
     * @param errorDesc Error Description (optional)
     */
    function displayNoAnnotationsAvailable(errorDesc)
    {    
        errorDesc = errorDesc || '';
        var optError = (errorDesc != '') ? (": " + errorDesc) : '';
        $("#annotation").html("No Annotations available" + optError);
        $('#oc_checkbox-annotations').removeAttr("checked");
        $('#oc_checkbox-annotations').attr('disabled', true);
        $('#oc_checkbox-annotations').hide();
        $('#oc_label-annotations').hide();
        hideAnnotation_Chapter();
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
