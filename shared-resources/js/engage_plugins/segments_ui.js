/**
 *  Copyright 2009-2011 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace segments_ui
 */
Opencast.segments_ui = (function ()
{
    var imgURLs = [],                 // segment image URLs  (Array)
        newSegments = [],             // segments            (Array)
        retSegments,                  // segment object      (Object)
        minSegmentPixels = 8,
        segmentsNrs = [],             // segments number     (Array)
        resizeEndTimeoutRunning = false,
        waitForMove = 150,
        onceLoaded = false;
        
    /**
     * @memberOf Opencast.segments_ui
     * @description Returns the segment object given by the JSONP-Ajax-Call and afterwards processed
     * @return the segment object given by the JSONP-Ajax-Call and afterwards processed
     */
    function getSegments()
    {
        return retSegments;
    }
     
    /**
     * @memberOf Opencast.segments_ui
     * @description Returns the old and new segment numbers mapped (e.g. getSegmentNumbers[oldNumber] = newNumber), -1 if no new value is available
     * @return the old and new segment numbers mapped (e.g. getSegmentNumbers[oldNumber] = newNumber), -1 if no new value is available
     */
    function getSegmentNumbers()
    {
        return segmentsNrs;
    }
     
    /**
     * @memberOf Opencast.segments_ui
     * @description Returns the new segment number (e.g. getSegmentNumbers[oldNumber] = newNumber), -1 if no new value is available
     * @param oldNr the old segment number
     * @return the new segment number mapped (e.g. getSegmentNumbers[oldNumber] = newNumber), -1 if no new value is available
     */
    function getSegmentNumber(oldNr)
    {
        if((oldNr >= 0) && (oldNr < segmentsNrs.length))
        {
            return segmentsNrs[oldNr];
        } else
        {
            return -1;
        }
    }
    
    /**
     * @memberOf Opencast.segments_ui
     * @description Returns an array with segment image URLs
     * @return an array with segment image URLs
     */
    function getImgURLArray()
    {
        return imgURLs;
    }
    
    /**
     * @memberOf Opencast.segments_ui
     * @description Toggles the class segment-holder-over
     * @param segmentId segmentId the id of the segment image
     * @param slideNr actual slide number
     */
    function hoverSegment(segmentId, slideNr)
    {
        slideNr = slideNr||segmentId;
        $("#segment" + slideNr).toggleClass("segment-holder");
        $("#segment" + slideNr).toggleClass("segment-holder-over");
        $("#segment" + slideNr).toggleClass("ui-state-hover");
        $("#segment" + slideNr).toggleClass("ui-corner-all");
        var imageHeight = 120;
        var nrOfSegments = segmentsNrs[segmentsNrs.length - 1]; // get length of new segments
        $("#segment-tooltip").html('<img src="' + imgURLs[segmentId] + '" height="' + imageHeight + '" alt="Slide ' + (slideNr + 1) + ' of ' + nrOfSegments + '"/>');
        if (($("#segment" + slideNr).offset() != null) && ($("#segment" + slideNr).offset() != null) && ($("#segment" + slideNr).width() != null) && ($("#segment-tooltip").width() != null))
        {
            var eps = 4;
            var tooltipWidth = $("#segment-tooltip").width();
            if (tooltipWidth == 0)
            {
                tooltipWidth = 160;
            }            
            var segment0Left = parseInt($("#segment0").offset().left + eps);
            var segmentLastRight = parseInt($('#segmentstable1').width()) + segment0Left - 3 * eps;
            
            var segmentLeft = $("#segment" + slideNr).offset().left;
            var segmentTop = $("#segment" + slideNr).offset().top;
            var segmentWidth = $("#segment" + slideNr).width();
            var pos = segmentLeft + segmentWidth / 2 - tooltipWidth / 2;
            // Check overflow on left Side
            if (pos < segment0Left)
            {
                pos = segment0Left;
            }
            // Check overflow on right Side
            if ((pos + tooltipWidth) > segmentLastRight)
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
                imgURLs = [];
                newSegments = [];
                segmentsNrs = [];
                
                Opencast.Utils.log("Segments UI AJAX call: Requesting data succeeded");
                if ((data !== undefined) && (data['search-results'] !== undefined) && (data['search-results'].result !== undefined))
                {
                    Opencast.Utils.log("Segments UI AJAX call: Data available");
                    // Streaming Mode is default true
                    var videoModeStream = true;
                    // Check whether a Videomode has been selected
                    var urlParamProgStream = Opencast.Utils.getURLParameter('videomode') || Opencast.Utils.getURLParameter('vmode');
                    // If such an URL Parameter exists (if parameter doesn't exist, the return value is null)
                    if (urlParamProgStream !== null)
                    {
                        // If current Videomode == progressive && URL Parameter == streaming
                        if (urlParamProgStream == 'streaming')
                        {
                            videoModeStream = true;
                        }
                        // If current Videomode == streaming && URL Parameter == progressive
                        else if (urlParamProgStream == 'progressive')
                        {
                            videoModeStream = false;
                        }
                        Opencast.Utils.log("Videomode manually set to: " + (videoModeStream ? "Streaming" : "Progressive"));
                    }
                    Opencast.Utils.log("Videomode: " + (videoModeStream ? "Streaming if possible" : "Progressive"));
                    // Check if Segments + Segments Text is available
                    var segmentsAvailable = (data['search-results'].result !== undefined) && (data['search-results'].result.segments !== undefined) && (data['search-results'].result.segments.segment.length > 0);
                    if (segmentsAvailable)
                    {
                        Opencast.Utils.log("Segments available");
                        // get rid of every '@' in the JSON data
                        // data = $.parseJSON(JSON.stringify(data).replace(/@/g, ''));
                        data['search-results'].result.segments.currentTime = Opencast.Utils.getTimeInMilliseconds(Opencast.Player.getCurrentTime());
                        // Get the complete Track Duration // TODO: handle more clever
                        var complDur = 0;
                        $.each(data['search-results'].result.segments.segment, function (i, value)
                        {
                            complDur += parseInt(data['search-results'].result.segments.segment[i].duration);
                        });
                        var completeDuration = 0;
                        var length = data['search-results'].result.segments.segment.length;
                        
                        // Calculate the minimal segment duration
                        // time / (scrubberLength / minPixel)
                        var scrubberLength = parseInt($('#oc_body').width());
                        var minPixel= Math.max(minSegmentPixels, 5);
                        var minSegmentLen = complDur / (scrubberLength / minPixel);
                        Opencast.Utils.log("Min. scrubber length: " + scrubberLength);
                        Opencast.Utils.log("Min. segment pixel: " + minPixel);
                        Opencast.Utils.log("Min. segment length: " + minSegmentLen);
                        var newSegmentsIndex = 0;
                        var hiddenSegmentsNr = 0;
                        var hiddenSegmentsStr = '';
                        
                        // Filter segments with a too small duration
                        $.each(data['search-results'].result.segments.segment, function (i, value)
                        {
                            // Save the image URL
                            imgURLs[i] = data['search-results'].result.segments.segment[i].previews.preview.$;
                            var curr = data['search-results'].result.segments.segment[i];
                            var currDur = parseInt(curr.duration);
                            if(currDur > 0)
                            {
                                curr.completeDuration = complDur;
                                // Set a Duration until the Beginning of this Segment
                                curr.durationExcludingSegment = completeDuration;
                                completeDuration += currDur;
                                // Set a Duration until the End of this Segment
                                curr.durationIncludingSegment = completeDuration;
                                
                                var timeToAdd = 0;
                                
                                segmentsNrs[i] = i - hiddenSegmentsNr;
                                var currHid = 0;
                                // loop through following segments
                                for(var j = i + 1; j < length; ++j)
                                {
                                    var currJ = data['search-results'].result.segments.segment[j];
                                    var currJDur = parseInt(currJ.duration);
                                    // if a following segment does not has the minimal length
                                    if(currJDur < minSegmentLen)
                                    {
                                        ++currHid;
                                        // map the old and new segment numbers
                                        segmentsNrs[j] = i - hiddenSegmentsNr;
                                        hiddenSegmentsStr += ' ' + j + ' ';
                                        // save duration
                                        timeToAdd += currJDur;
                                        // set duration to 0 for not displaying the segment
                                        currJ.duration = 0;
                                    } else
                                    {
                                        break;
                                    }
                                }
                                hiddenSegmentsNr += currHid;
                                // if some following segment(s) didn't have the minimal length as well
                                if(timeToAdd > 0)
                                {
                                    // put the segments with the current segment together
                                    curr.duration = parseInt(curr.duration) + timeToAdd;
                                    curr.durationIncludingSegment = parseInt(curr.durationIncludingSegment) + timeToAdd;
                                    timeToAdd = 0;
                                }
                            }
                        });
                        /*
                        $.each(segmentsNrs, function (i, value)
                        {
                            Opencast.Utils.log("segmentsNrs[" + i + "] = " + segmentsNrs[i]);
                        });
                        */
                        $.each(data['search-results'].result.segments.segment, function (i, value)
                        {
                            var dur = parseInt(data['search-results'].result.segments.segment[i].duration);
                            if(dur > 0)
                            {
                                newSegments[newSegmentsIndex] = data['search-results'].result.segments.segment[i];
                                newSegments[newSegmentsIndex].hoverSegmentIndex = i;
                                newSegments[newSegmentsIndex].index = newSegmentsIndex;
                                newSegments[newSegmentsIndex].previews.preview.$ = data['search-results'].result.segments.segment[i].previews.preview.$;
                                ++newSegmentsIndex;
                            }
                        });
                        var oldLength = data['search-results'].result.segments.segment.length;
                        data['search-results'].result.segments.segment = newSegments;
                        retSegments = data['search-results'].result.segments;
                        Opencast.Utils.log("Removed " + (oldLength - newSegments.length) + "/" + oldLength + " Segments due to being too small in relation to the scrubber length:" + hiddenSegmentsStr);
                        initResizeEnd();
                    } else
                    {
                        Opencast.Utils.log("Segments not available");
                    }
                    // Check if any Media.tracks are available
                    if ((data['search-results'].result.mediapackage.media !== undefined) && (data['search-results'].result.mediapackage.media.track.length > 0))
                    {
                        Opencast.Utils.log("Media tracks available");
                        // Set whether prefer streaming of progressive
                        data['search-results'].result.mediapackage.media.preferStreaming = videoModeStream;
                        // Check if the File is a Video
                        var isVideo = false;
                        var rtmpAvailable = false;
                        $.each(data['search-results'].result.mediapackage.media.track, function (i, value)
                        {
                            if (!isVideo && Opencast.Utils.startsWith(this.mimetype, 'video'))
                            {
                                isVideo = true;
                            }
                            if (data['search-results'].result.mediapackage.media.track[i].url.substring(0, 4) == "rtmp")
                            {
                                rtmpAvailable = true;
                            }
                            // If both Values have been set
                            if (isVideo && rtmpAvailable)
                            {
                                // Jump out of $.each
                                return false;
                            }
                        });
                        data['search-results'].result.mediapackage.media.isVideo = isVideo;
                        data['search-results'].result.mediapackage.media.rtmpAvailable = rtmpAvailable;
                        Opencast.Utils.log(isVideo ? "Media is a Video" : "Media is not a Video");
                        Opencast.Utils.log(rtmpAvailable ? "Streaming (rtmp) is available" : "Streaming (rtmp) is not available");
                    } else
                    {
                        Opencast.Utils.log("Media tracks not available");
                    }
                    // Create Trimpath Template
                    Opencast.segments_ui_Plugin.addAsPlugin($('#segmentstable1'), $('#segments_ui-media1'), $('#data1'), $('#segments_ui-mediapackagesAttachments'), $('#data2'), $('#segments_ui-mediapackagesCatalog'), $('#segmentstable2'), data['search-results'].result, segmentsAvailable);
                    Opencast.segments_ui_slider_Plugin.addAsPlugin($('#tableData1'), $('#segments_ui_slider_data1'), $('#segments_ui_slider_data2'), data['search-results'].result, segmentsAvailable);
                    if(!onceLoaded)
                    {
                        Opencast.Watch.continueProcessing();
                        onceLoaded = true;
                    }
                }
                else
                {
                    Opencast.Utils.log("Ajax call: Data not available");
                    Opencast.Watch.continueProcessing(true);
                }
            },
            // If no data comes back
            error: function (xhr, ajaxOptions, thrownError)
            {
                Opencast.Utils.log("Segments UI Ajax call: Requesting data failed");
                $('#data').html('No Segment UI available');
                $('#data').hide();
                Opencast.Watch.continueProcessing(true);
            }
        });
    }
        
    /**
     * @memberOf Opencast.segments_ui
     * @description Binds the Window-Resize-Event
     */
    function initResizeEnd()
    {
        if(!resizeEndTimeoutRunning)
        {
            $(window).resize(function ()
            {
                dateIn = new Date();
                if (resizeEndTimeoutRunning === false)
                {
                    resizeEndTimeoutRunning = true;
                    resizeEndTimeoutRunning = setTimeout(resizeEnd, waitForMove);
                }
            });
        }
        // If window has been resized
        if(resizeEndTimeoutRunning == true)
        {
            resizeEndTimeoutRunning = false;
            Opencast.segments.clearCashe();
            Opencast.segments.initialize();
            if(Opencast.search)
            {
                Opencast.search.initialize();
                if(Opencast.search.isOpen())
                {
                    Opencast.search.showResult(Opencast.search.getCurrentInputElement(), Opencast.search.getCurrentSearchString());
                }
            }
        }
    }
    
    /**
     * @memberOf Opencast.segments_ui
     * @description Checks if resize is over
     */
    function resizeEnd()
    {
        var dateOut = new Date();
        // if the Resize-Event is not over yet: set new timeout
        if ((dateOut - dateIn) < waitForMove)
        {
            setTimeout(resizeEnd, waitForMove);
        }
        else
        {
            // Set this flag to true to show that after the asynchronous AJAX-call other segments have to be re-initialized
            resizeEndTimeoutRunning = true;
            initialize();
        }
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
        getSegments: getSegments,
        getSegmentNumbers: getSegmentNumbers,
        getSegmentNumber: getSegmentNumber,
        getImgURLArray: getImgURLArray,
        hoverSegment: hoverSegment,
        hoverOutSegment: hoverOutSegment,
        hoverDescription: hoverDescription,
        hoverOutDescription: hoverOutDescription,
        initialize: initialize,
        setMediaPackageId: setMediaPackageId
    };
}());
