/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace segments_ui
 */
Opencast.segments_ui = (function ()
{   
    /**
     * @memberOf Opencast.segments_ui
     * @description Toggles the class segment-holder-over
     * @param Integer
     *          segmentId the id of the segment
     */
    function hoverSegment(segmentId)
    {
        $("#segment" + segmentId).toggleClass("segment-holder");
        $("#segment" + segmentId).toggleClass("segment-holder-over");
        $("#segment" + segmentId).toggleClass("ui-state-hover");
        $("#segment" + segmentId).toggleClass("ui-corner-all");
        var index = segmentId;
        var imageHeight = 120;
        var nrOfSegments = Opencast.segments.getNumberOfSegments();
        $("#segment-tooltip").html('<img src="' + Opencast.segments.getSegmentPreview(index) +
                                   '" height="' + imageHeight +
                                   '" alt="Slide ' + (segmentId + 1) + ' of ' + nrOfSegments + '"/>');
        if (($("#segment" + segmentId).offset() != null) &&
            ($("#segment" + segmentId).offset() != null) &&
            ($("#segment" + segmentId).width() != null) &&
            ($("#segment-tooltip").width() != null))
        {
            var eps = 4;
            var segment0Left = $("#segment0").offset().left + eps;
            var segmentLastRight = $("#segment" + (nrOfSegments - 1)).offset().left + $("#segment" + (nrOfSegments - 1)).width() - eps;
            var segmentLeft = $("#segment" + segmentId).offset().left;
            var segmentTop = $("#segment" + segmentId).offset().top;
            var segmentWidth = $("#segment" + segmentId).width();
            var tooltipWidth = $("#segment-tooltip").width();
            if(tooltipWidth == 0)
            {
                tooltipWidth = 160;
            }
            var pos = segmentLeft + segmentWidth / 2 - tooltipWidth / 2;
            // Check overflow on left Side
            if(pos < segment0Left)
            {
                pos = segment0Left;
            }
            // Check overflow on right Side
            if((pos + tooltipWidth) > segmentLastRight)
            {
                pos -= (pos + tooltipWidth) - segmentLastRight;
            }
            $("#segment-tooltip").css("left", pos + "px");
            $("#segment-tooltip").css("top", segmentTop - (imageHeight + 6) + "px");
            $("#segment-tooltip").show();
        }
    }

    /**
     * @memberOf Opencast.segments_ui
     * @description Toggles the class segment-holder-over
     * @param Integer
     *          segmentId the id of the segment
     */
    function hoverOutSegment(segmentId)
    {
        $("#segment" + segmentId).toggleClass("segment-holder");
        $("#segment" + segmentId).toggleClass("segment-holder-over");
        $("#segment" + segmentId).toggleClass("ui-state-hover");
        $("#segment" + segmentId).toggleClass("ui-corner-all");
        $("#segment-tooltip").hide();
    }

    /**
     * @memberOf Opencast.segments_ui
     * @description Toggles the class segment-holder-over
     * @param String
     *          segmentId the id of the segment
     */
    function hoverDescription(segmentId, description)
    {
        $("#" + segmentId).toggleClass("segment-holder");
        $("#" + segmentId).toggleClass("segment-holder-over");
        $("#" + segmentId).toggleClass("ui-state-hover");
        $("#" + segmentId).toggleClass("ui-corner-all");
        var index = parseInt(segmentId.substr(7)) - 1;
        var imageHeight = 25;
        var text = description;
        $("#segment-tooltip").html(text);
        var segmentLeft = $("#" + segmentId).offset().left;
        var segmentTop = $("#" + segmentId).offset().top;
        var segmentWidth = $("#" + segmentId).width();
        var tooltipWidth = $("#segment-tooltip").width();
        $("#segment-tooltip").css("left", (segmentLeft + segmentWidth / 2 - tooltipWidth / 2) + "px");
        $("#segment-tooltip").css("top", segmentTop - (imageHeight + 7) + "px");
        $("#segment-tooltip").show();
    }

    /**
     * @memberOf Opencast.segments_ui
     * @description Toggles the class segment-holder-over
     * @param String
     *          segmentId the id of the segment
     */
    function hoverOutDescription(segmentId, description)
    {
        $("#" + segmentId).toggleClass("segment-holder");
        $("#" + segmentId).toggleClass("segment-holder-over");
        $("#" + segmentId).toggleClass("ui-state-hover");
        $("#" + segmentId).toggleClass("ui-corner-all");
        $("#segment-tooltip").hide();
    }

    /**
     * @memberOf Opencast.segments_ui
     * @description Initializes the segments ui view
     */
    function initialize()
    {
        // Request JSONP data
        $.ajax(
        {
            url: '../../search/episode.json',
            data: 'id=' + mediaPackageId,
            dataType: 'jsonp',
            jsonp: 'jsonp',
            success: function (data)
            {
                if((data !== undefined) && (data['search-results'] !== undefined) && (data['search-results'].result !== undefined))
                {
                    // Streaming Mode is default true
                    var videoModeStream = true;
                    // Check whether a Videomode has been selected
                    var urlParamProgStream = Opencast.Utils.getURLParameter('videomode') || Opencast.Utils.getURLParameter('vmode');
                    // If such an URL Parameter exists (if parameter doesn't exist, the return value is null)
                    if(urlParamProgStream !== null)
                    {
                        // If current Videomode == progressive && URL Parameter == streaming
                        if(urlParamProgStream == 'streaming')
                        {
                            videoModeStream = true;
                        }
                        // If current Videomode == streaming && URL Parameter == progressive
                        else if(urlParamProgStream == 'progressive')
                        {
                            videoModeStream = false;
                        }
                    }
                    
                    // Check if Segments + Segments Text is available
                    var segmentsAvailable = (data['search-results'].result !== undefined) && (data['search-results'].result.segments !== undefined) && (data['search-results'].result.segments.segment.length > 0);
                    if (segmentsAvailable)
                    {
                        // get rid of every '@' in the JSON data
                        // data = $.parseJSON(JSON.stringify(data).replace(/@/g, ''));
                        data['search-results'].result.segments.currentTime = Opencast.Utils.getTimeInMilliseconds(Opencast.Player.getCurrentTime());
                        // Get the complete Track Duration // TODO: handle more clever
                        var complDur = 0;
                        $.each(data['search-results'].result.segments.segment, function (i, value)
                        {
                            complDur += parseInt(data['search-results'].result.segments.segment[i].duration);
                        });
                        // Set Duration until this Segment ends
                        var completeDuration = 0;
                        $.each(data['search-results'].result.segments.segment, function (i, value)
                        {
                            data['search-results'].result.segments.segment[i].completeDuration = complDur;
                            // Set a Duration until the Beginning of this Segment
                            data['search-results'].result.segments.segment[i].durationExcludingSegment = completeDuration;
                            completeDuration += parseInt(data['search-results'].result.segments.segment[i].duration);
                            // Set a Duration until the End of this Segment
                            data['search-results'].result.segments.segment[i].durationIncludingSegment = completeDuration;
                        });
                    }
                    // Check if any Media.tracks are available
                    if((data['search-results'].result.mediapackage.media !== undefined) && (data['search-results'].result.mediapackage.media.track.length > 0))
                    {
                        // Set whether prefer streaming of progressive
                        data['search-results'].result.mediapackage.media.preferStreaming = videoModeStream;
                            
                        // Check if the File is a Video
                        var isVideo = false;
                        var rtmpAvailable = false;
                        $.each(data['search-results'].result.mediapackage.media.track, function (i, value)
                        {
                            if(!isVideo && Opencast.Utils.startsWith(this.mimetype, 'video'))
                            {
                                isVideo = true;
                            }
                            if(data['search-results'].result.mediapackage.media.track[i].url.substring(0, 4) == "rtmp")
                            {
                                rtmpAvailable = true;
                            }
                            // If both Values have been set
                            if(isVideo && rtmpAvailable)
                            {
                                // Jump out of $.each
                                return false;
                            }
                        });
                        data['search-results'].result.mediapackage.media.isVideo = isVideo;
                        data['search-results'].result.mediapackage.media.rtmpAvailable = rtmpAvailable;
                    }
                    
                    // Create Trimpath Template
                    Opencast.segments_ui_Plugin.addAsPlugin($('#segmentstable1'),
                                                            $('#segments_ui-media1'),
                                                            $('#data1'),
                                                            $('#segments_ui-mediapackagesAttachments'),
                                                            $('#data2'),
                                                            $('#segments_ui-mediapackagesCatalog'),
                                                            $('#segmentstable2'),
                                                            data['search-results'].result,
                                                            segmentsAvailable);
                    Opencast.segments_ui_slider_Plugin.addAsPlugin($('#tableData1'),
                                                                   $('#segments_ui_slider_data1'),
                                                                   $('#segments_ui_slider_data2'),
                                                                   data['search-results'].result,
                                                                   segmentsAvailable);
                    Opencast.Watch.continueProcessing();
                } else
                {
                    Opencast.Watch.continueProcessing(true);
                }
            },
            // If no data comes back
            error: function (xhr, ajaxOptions, thrownError)
            {
                $('#data').html('No Segment UI available');
                $('#data').hide();
                Opencast.Watch.continueProcessing(true);
            }
        });
    }

    /**
     * @memberOf Opencast.segments_ui
     * @description Set the mediaPackageId
     * @param String mediaPackageId
     */
    function setMediaPackageId(id)
    {
        mediaPackageId = id;
    }

    return {
        hoverSegment: hoverSegment,
        hoverOutSegment: hoverOutSegment,
        hoverDescription: hoverDescription,
        hoverOutDescription: hoverOutDescription,
        initialize: initialize,
        setMediaPackageId: setMediaPackageId
    };
}());
