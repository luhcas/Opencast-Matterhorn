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
        $("#segment-tooltip").html('<img src="' + Opencast.segments.getSegmentPreview(index) + '" height="' + imageHeight + '"/>');
        if (($("#segment" + segmentId).offset() != null) &&
            ($("#segment" + segmentId).offset() != null) &&
            ($("#segment" + segmentId).width() != null) &&
            ($("#segment-tooltip").width() != null))
        {
            var segmentLeft = $("#segment" + segmentId).offset().left;
            var segmentTop = $("#segment" + segmentId).offset().top;
            var segmentWidth = $("#segment" + segmentId).width();
            var tooltipWidth = $("#segment-tooltip").width();
            $("#segment-tooltip").css("left", (segmentLeft + segmentWidth / 2 - tooltipWidth / 2) + "px");
            $("#segment-tooltip").css("top", segmentTop - (imageHeight + 7) + "px");
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
                // Check if Segments + Segments Text is available
                var segmentsAvailable = (data['search-results'].result.segments !== undefined) && (data['search-results'].result.segments.segment.length > 0);
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
                if((data['search-results'].result.mediapackage.media !== undefined) && (data['search-results'].result.mediapackage.media.track.length > 0))
                {
                    $.each(data['search-results'].result.mediapackage.media.track, function (i, value)
                    {
                        // Set preceding Sibling's Type and Mimetype
                        if(i > 0)
                        {
                            this.precedingSiblingType = data['search-results'].result.mediapackage.media.track[i - 1].type;
                            this.precedingSiblingMimetypeIsVideo = Opencast.Utils.startsWith(data['search-results'].result.mediapackage.media.track[i - 1].mimetype, "video");
                        } else
                        {
                            this.precedingSiblingType = 'none';
                            this.precedingSiblingMimetypeIsVideo = false;
                        }
                        // Set following Sibling's Type and Mimetype
                        if(i < (data['search-results'].result.mediapackage.media.track.length - 1))
                        {
                            this.followingSiblingType = data['search-results'].result.mediapackage.media.track[i + 1].type;
                            this.followingSiblingMimetypeIsVideo = Opencast.Utils.startsWith(data['search-results'].result.mediapackage.media.track[i + 1].mimetype, "video");
                        } else
                        {
                            this.followingSiblingType = 'none';
                            this.followingSiblingMimetypeIsVideo = false;
                        }
                    });
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
            },
            // If no data comes back
            error: function (xhr, ajaxOptions, thrownError)
            {
                $('#data').html('No Segment UI available');
                $('#data').hide();
                Opencast.Watch.continueProcessing();
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
