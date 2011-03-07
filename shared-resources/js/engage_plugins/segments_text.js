/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace segments_text
 */
Opencast.segments_text = (function ()
{
    var mediaPackageId;
    var staticBool_hide = true;
    var SEGMENTS_TEXT = "Segment Text",
        SEGMENTS_TEXT_HIDE = "Hide Segment Text";
        
    /**
     * @memberOf Opencast.segments_text
     * @description Initializes the Segments Text Tab
     */
    function initialize()
    {
        // Do nothing in here
    }
    
    /**
     * @memberOf Opencast.segments_text
     * @description Shows the Segments Text Tab
     */
    function showSegmentsText()
    {
        // Hide other Tabs
        Opencast.Description.hideDescription();
        Opencast.segments.hideSegments();
        Opencast.search.hideSearch();
        // Change Tab Caption
        $('#oc_btn-slidetext').attr(
        {
            title: SEGMENTS_TEXT_HIDE
        });
        $('#oc_btn-slidetext').html(SEGMENTS_TEXT_HIDE);
        $("#oc_btn-slidetext").attr('aria-pressed', 'true');
        // Show a loading Image
        $('#oc_slidetext').show();
        $('#segments_text-loading').show();
        $('#oc-segments_text').hide();
        $('.oc-segments-preview').css('display', 'block');
        
        // If cashed data are available
        if(Opencast.segments_text_Plugin.createSegmentsTextFromCashe())
        {
            // Make visible
            $('#oc_slidetext').show();
            $('#segments_text-loading').hide();
            $('#oc-segments_text').show();
            $('.oc-segments-preview').css('display', 'block');
        } else
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
                    // get rid of every '@' in the JSON data
                    // data = $.parseJSON(JSON.stringify(data).replace(/@/g, ''));
                    data['search-results'].result.segments.currentTime = Opencast.Utils.getTimeInMilliseconds(Opencast.Player.getCurrentTime());
                    // Set Duration until this Segment ends
                    var completeDuration = 0;
                    $.each(data['search-results'].result.segments.segment, function (i, value)
                    {
                        // Set a Duration until the Beginning of this Segment
                        data['search-results'].result.segments.segment[i].durationExcludingSegment = completeDuration;
                        completeDuration += parseInt(data['search-results'].result.segments.segment[i].duration);
                        // Set a Duration until the End of this Segment
                        data['search-results'].result.segments.segment[i].durationIncludingSegment = completeDuration;
                    });
                    // Create Trimpath Template
                    Opencast.segments_text_Plugin.addAsPlugin($('#oc-segments_text'), data['search-results'].result.segments);
                    // Make visible
                    $('#oc_slidetext').show();
                    $('#segments_text-loading').hide();
                    $('#oc-segments_text').show();
                    $('.oc-segments-preview').css('display', 'block');
                },
                // If no data comes back
                error: function (xhr, ajaxOptions, thrownError)
                {
                    $('#oc-segments_text').html('No Segment Text available');
                    $('#oc-segments_text').hide();
                }
            });
        }
    }
    
    /**
     * @memberOf Opencast.segments_text
     * @description Hides the Segments Text Tab
     */
    function hideSegmentsText()
    {
        // Change Tab Caption
        $('#oc_btn-slidetext').attr(
        {
            title: SEGMENTS_TEXT
        });
        $('#oc_btn-slidetext').html(SEGMENTS_TEXT);
        $("#oc_btn-slidetext").attr('aria-pressed', 'false');
        $('#oc_slidetext').hide();
    }
    
    /**
     * @memberOf Opencast.segments_text
     * @description Toggles the Segments Text Tab
     */
    function doToggleSegmentsText()
    {
        if ($('#oc_btn-slidetext').attr("title") === SEGMENTS_TEXT)
        {
            Opencast.Description.hideDescription();
            Opencast.segments.hideSegments();
            showSegmentsText();
        }
        else if (staticBool_hide)
        {
            hideSegmentsText();
        }
    }
    
    /**
     * @memberOf Opencast.segments_text
     * @description Sets the mediaPackageId
     * @param String mediaPackageId
     */
    function setMediaPackageId(id)
    {
        mediaPackageId = id;
    }
    
    return {
        initialize: initialize,
        showSegmentsText: showSegmentsText,
        hideSegmentsText: hideSegmentsText,
        setMediaPackageId: setMediaPackageId,
        doToggleSegmentsText: doToggleSegmentsText
    };
}());
