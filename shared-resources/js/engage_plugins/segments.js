/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace segments
 */
Opencast.segments = (function ()
{
    var totalPanels, segmentTimes, segmentPreviews;
    var mediaPackageId, duration;
    var numberOfSegments = 0,
        beforeSlide = 0,
        currentSlide = 0,
        nextSlide = 0,
        slideLength = 0;
    var SEGMENTS = "Segments",
        SEGMENTS_HIDE = "Hide Segments";

    /**
     * @memberOf Opencast.segments
     * @description Returns the Seconds of a given Segment with the ID segmentID
     * @return the Seconds of a given Segment with the ID segmentID
     */
    function getSegmentSeconds(segmentId)
    {
        if((segmentId >= 0) && (segmentId < segmentTimes.length))
        {
            return segmentTimes[segmentId];
        }
        return 0;
    }

    /**
     * @memberOf Opencast.segments
     * @description Returns the current Segment ID
     * @return the current Segment ID
     */
    function getCurrentSlideId()
    {
        var currentPosition = parseInt(Opencast.Player.getCurrentPosition());
        for(var i = 0; i < segmentTimes.length; ++i)
        {
            if(i < (segmentTimes.length - 1))
            {
                if((currentPosition >= segmentTimes[i]) && (currentPosition < segmentTimes[i + 1]))
                {
                    return i;
                }
            } else
            {
                return i;
            }
        }
        return 0;
    }

    /**
     * @memberOf Opencast.segments
     * @description Returns the total Number of Segments
     * @return the total Number of Segments
     */
    function getNumberOfSegments()
    {
        return numberOfSegments;
    }

    /**
     * @memberOf Opencast.segments
     * @description Returns the Segments Previews of segmentID
     * @return the Segments Previews of segmentID
     */
    function getSegmentPreview(segmentId)
    {
        return segmentPreviews[segmentId];
    }
    
    /**
     * @memberOf Opencast.segments
     * @description Returns the Seconds of the Slide before
     * @return the Seconds of the Slide before
     */
    function getSecondsBeforeSlide()
    {
        updateSlides();
        return beforeSlide;
    }
    
    /**
     * @memberOf Opencast.segments
     * @description Returns the Seconds of the next Slide
     * @return the Seconds of the next Slide
     */
    function getSecondsNextSlide()
    {
        updateSlides();
        return nextSlide;
    }
    
    /**
     * 
     * @memberOf Opencast.segments
     * @description Returns the Slide Length
     * @return the Slide Length
     */
    function getSlideLength()
    {
        return slideLength;
    }
    
    /**
     * 
     * @memberOf Opencast.segments
     * @description Sets the Slide Length
     * @param length Slide Length
     */
    function setSlideLength(length)
    {
        slideLength = length;
    }
    
    /**
     * @memberOf Opencast.segments
     * @description Initializes the segments view
     */
    function initialize()
    {
        totalPanels = $(".scrollContainer").children().size();
        var $panels = $('#slider .scrollContainer > div');
        var $container = $('#slider .scrollContainer');
        if ($panels !== undefined)
        {
            $panels.css(
            {
                'float': 'left',
                'position': 'relative'
            });
            $("#slider").data("currentlyMoving", false);
            if ($panels[0] !== undefined)
            {
                $container.css('width', ($panels[0].offsetWidth * $panels.length)).css('left', "0px");
                // Disable and grey out "Annotation" Tab
                $("#oc_ui_tabs").tabs(
                {
                    disabled: [4]
                });
            }
            else
            {
                // If no Segents are available
                if ($('#segmentstable > tbody > tr > td').size() <= 1)
                {
                    // Disable and grey out "Segments" Tab
                    // Disable and grey out "Segments Text" Tab
                    $("#oc_ui_tabs").tabs(
                    {
                        disabled: [1, 2]
                    });
                }
            }
            var scroll = $('#slider .scroll').css('overflow', 'hidden');
            //when the left/right arrows are clicked
            $(".right").click(function ()
            {
                change(true);
            });
            $(".left").click(function ()
            {
                change(false);
            });
        }
        // Set Segments
        if ($('.segments-time') !== undefined)
        {
            numberOfSegments = $('.segments-time').length;
            segmentTimes = new Array(numberOfSegments);
            var seconds;
            $('.segments-time').each(function (i)
            {
                seconds = parseInt($(this).text());
                if (!isNaN(seconds))
                {
                    segmentTimes[i] = seconds;
                }
                else
                {
                    segmentTimes[i] = 0;
                }
            });
            // Set Previews
            var numberOfPreviews = 0;
            if ($('.oc-segments-preview') != undefined)
            {
                numberOfPreviews = $('.oc-segments-preview').length;
                segmentPreviews = new Array(numberOfPreviews);
                var url;
                $('.oc-segments-preview').each(function (i)
                {
                    url = $(this).html();
                    segmentPreviews[i] = url;
                });
            }
            // Set Slide Length
            setSlideLength(numberOfSegments);
            $('#oc_video-player-controls').css('display', 'block');
        }
        // Hide Slide Tab, if there are no slides
        if (numberOfSegments == 0)
        {
            hideSegments();
            $(".oc_btn-skip-backward").hide();
            $(".oc_btn-skip-forward").hide();
        }
        else
        {
            $("#oc_ui_tabs").tabs('enable', 1);
            $("#oc_ui_tabs").tabs('enable', 2);
        }
        // set the center of the controls
        var margin = $('#oc_video-controls').width();
        var controlswith = 0;
        var playerWidth = $('#oc_video-player-controls').width();
        // player size
        if (playerWidth < 460 && playerWidth >= 380)
        {
            $(".oc_btn-skip-backward").css('display', 'none');
            $(".oc_btn-skip-forward").css('display', 'none');
            $('#oc_video-controls').css('width', '20%');
            $('#oc_video-cc').css('width', '12%');
            $('#oc_video-time').css('width', '40%');
            $('.oc_slider-volume-Rail').css('width', '45px');
            controlswith = 58;
            margin = $('#oc_video-controls').width();
            margin = ((margin - controlswith) / 2) - 8;
            $(".oc_btn-rewind").css("margin-left", margin + "px");
        }
        else if (playerWidth < 380 && playerWidth >= 300)
        {
            $(".oc_btn-skip-backward").css('display', 'none');
            $(".oc_btn-skip-forward").css('display', 'none');
            $(".oc_btn-rewind").css('display', 'none');
            $(".oc_btn-fast-forward").css('display', 'none');
            $("#oc_video-cc").css('display', 'none');
            $("#oc_video-cc").css('width', '0%');
            $('#simpleEdit').css('font-size', '0.8em');
            $('#simpleEdit').css('margin-left', '1px');
            $('#oc_current-time').css('width', '45px');
            $('#oc_edit-time').css('width', '45px');
            $('#oc_duration').css('width', '45px');
            $('#oc_edit-time-error').css('width', '45px');
            $('#oc_sound').css('width', '27%');
            $('#oc_video-controls').css('width', '8%');
            $('#oc_video-time').css('width', '48%');
            $('.oc_slider-volume-Rail').css('width', '45px');
            controlswith = 16;
            margin = $('#oc_video-controls').width();
            margin = ((margin - controlswith) / 2) - 8;
            $("#oc_btn-play-pause").css("margin-left", margin + "px");
        }
    }
    
    /**
     * @memberOf Opencast.segments
     * @description Updates the Slide Seconds of the Slides 'Before', 'Current' and 'After'
     */
    function updateSlides()
    {
        if (numberOfSegments > 0)
        {
            var currentPosition = parseInt(Opencast.Player.getCurrentPosition());
            var last = 0;
            var cur = 0;
            var ibefore = 0;
            var lastIndex;
            // last segment
            if (segmentTimes[segmentTimes.length - 1] <= currentPosition)
            {
                if ((segmentTimes[segmentTimes.length - 1] <= currentPosition) && (currentPosition < parseInt(segmentTimes[segmentTimes.length - 1]) + 3))
                {
                    lastIndex = 2;
                }
                else
                {
                    lastIndex = 1;
                }
                var ibefore = Math.max(segmentTimes.length - lastIndex, 0);
                beforeSlide = segmentTimes[ibefore];
                currentSlide = segmentTimes[segmentTimes.length - 1];
                nextSlide = currentSlide;
            }
            else
            {
                for (i in segmentTimes)
                {
                    cur = segmentTimes[i];
                    if (last <= currentPosition && currentPosition < cur)
                    {
                        if ((last <= currentPosition) && (currentPosition < parseInt(last) + 3))
                        {
                            lastIndex = 2;
                        }
                        else lastIndex = 1;
                        ibefore = Math.max(parseInt(i) - lastIndex, 0);
                        beforeSlide = segmentTimes[ibefore];
                        currentSlide = last;
                        nextSlide = segmentTimes[i];
                        break;
                    }
                    last = cur;
                }
            }
        }
    }
    
    /**
     * @memberOf Opencast.segments
     * @description Sets scrollContainer width
     */
    function sizeSliderContainer()
    {
        var $panels = $('#slider .scrollContainer > div');
        var $container = $('#slider .scrollContainer');
        if ($panels[0] !== undefined)
        {
            $container.css('width', ($panels[0].offsetWidth * $panels.length)).css('left', "0px");
        }
    }
    
    /**
     * @memberOf Opencast.segments
     * @description Changes the Direction of the Slider
     * @param boolean direction true = right, false = left
     */
    function change(direction)
    {
        var leftValue = parseFloat($(".scrollContainer").css("left"), 10)
        var scrollContainerWidth = parseFloat($(".scrollContainer").css("width"), 10);
        var sliderWidth = parseFloat($(".scroll").css("width"), 10);
        var dif = sliderWidth - scrollContainerWidth;
        //if not at the first or last panel
        if ((!direction && (leftValue >= 0)) || (direction && (leftValue <= dif)))
        {
            return false;
        }
        //if not currently moving
        if (($("#slider").data("currentlyMoving") == false))
        {
            $("#slider").data("currentlyMoving", true);
            var maxMove = (scrollContainerWidth + leftValue) - sliderWidth;
            var movement = direction ? leftValue - Math.min(sliderWidth, maxMove) : leftValue + Math.min(sliderWidth, leftValue * (-1));
            $(".scrollContainer").stop().animate(
            {
                "left": movement
            }, function ()
            {
                $("#slider").data("currentlyMoving", false);
            });
        }
    }
    
    /**
     * @memberOf Opencast.segments
     * @description Displays the Segments Tab
     */
    function showSegments()
    {
        // Hide other Tabs
        Opencast.Description.hideDescription();
        Opencast.segments_text.hideSegmentsText();
        Opencast.search.hideSearch();
        // Change Tab Caption
        $('#oc_btn-slides').attr(
        {
            title: SEGMENTS_HIDE
        });
        $('#oc_btn-slides').html(SEGMENTS_HIDE);
        $("#oc_btn-slides").attr('aria-pressed', 'true');
        // Will be overwritten if the Template is ready
        $('#scrollcontainer').html('<img src="img/loading.gif" />');
        // Show a loading Image
        $('#oc_slides').show();
        $('#oc_slides').css('display', 'block');
        $('#segments-loading').show();
        $('#slider').hide();
        // Request JSONP data
        $.ajax(
        {
            url: '../../search/episode.json',
            data: 'id=' + mediaPackageId,
            dataType: 'jsonp',
            jsonp: 'jsonp',
            success: function (data)
            {
                // get rid of every '@' in the JSON data
                // data = $.parseJSON(JSON.stringify(data).replace(/@/g, ''));
                // Create Trimpath Template
                Opencast.segments_Plugin.addAsPlugin($('#scrollcontainer'), data['search-results'].result.segments);
                // Show a loading Image
                $('#oc_slides').show();
                $('#oc_slides').css('display', 'block');
                $('#segments-loading').hide();
                $('#slider').show();
                // Sets slider container width after panels are displayed
                Opencast.segments.sizeSliderContainer();
            },
            // If no data comes back
            error: function (xhr, ajaxOptions, thrownError)
            {
                $('#scrollcontainer').html('No Slides available');
                $('#scrollcontainer').hide();
            }
        });
    }
    
    /**
     * @memberOf Opencast.segments
     * @description Hides the Segments Tab
     */
    function hideSegments()
    {
        // Change Tab Caption
        $('#oc_btn-slides').attr(
        {
            title: SEGMENTS
        });
        $('#oc_btn-slides').html(SEGMENTS);
        $("#oc_btn-slides").attr('aria-pressed', 'false');
        $('#oc_slides').hide();
    }
    
    /**
     * @memberOf Opencast.segments
     * @description Toggles the Segments Tab
     */
    function doToggleSlides()
    {
        if ($('#oc_btn-slides').attr("title") === SEGMENTS)
        {
            Opencast.Description.hideDescription();
            Opencast.segments_text.hideSegmentsText();
            Opencast.search.hideSearch();
            showSegments();
        }
        else
        {
            hideSegments();
        }
    }
    
    /**
     * @memberOf Opencast.segments
     * @description Set the mediaPackageId
     * @param String mediaPackageId
     */
    function setMediaPackageId(id)
    {
        mediaPackageId = id;
    }
    
    return {
        getSegmentSeconds: getSegmentSeconds,
        getCurrentSlideId: getCurrentSlideId,
        getNumberOfSegments: getNumberOfSegments,
        getSegmentPreview: getSegmentPreview,
        getSecondsBeforeSlide: getSecondsBeforeSlide,
        getSecondsNextSlide: getSecondsNextSlide,
        getSlideLength: getSlideLength,
        initialize: initialize,
        sizeSliderContainer: sizeSliderContainer,
        showSegments: showSegments,
        hideSegments: hideSegments,
        setMediaPackageId: setMediaPackageId,
        doToggleSlides: doToggleSlides
    };
}());
