/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {
};

var staticBool_hide = true;

/**
 * @namespace the global Opencast namespace segments_text
 */
Opencast.segments_text = (function ()
{
    /**
     * @memberOf Opencast.segments
     * @description Initializes the segments_text view
     */
    function initialize()
    {
        $('#oc_slidetext-left').html('');
        $('#oc_slidetext-right').html('');
        $('#oc-segments-text').html('');
    }

    /**
     *  variables
     */
    var mediaPackageId, SEGMENTS_TEXT = "Segment Text",
        SEGMENTS_TEXT_HIDE = "Hide Segment Text";

    /**
     * Returns the Input Time in Milliseconds -- TODO: put it in a utils-class
     * @param data Data in the Format ab:cd:ef
     * @return Time from the Data in Milliseconds
     */
    function getTimeInMilliseconds(data)
    {
        var values = data.split(':');

        // If the Format is correct
        if (values.length == 3)
        {
            // Try to convert to Numbers
            var val0 = values[0] * 1;
            var val1 = values[1] * 1;
            var val2 = values[2] * 1;
            // Check and parse the Seconds
            if (!isNaN(val0) && !isNaN(val1) && !isNaN(val2))
            {
                // Convert Hours, Minutes and Seconds to Milliseconds
                val0 *= 60 * 60 * 1000; // 1 Hour = 60 Minutes = 60 * 60 Seconds = 60 * 60 * 1000 Milliseconds
                val1 *= 60 * 1000; // 1 Minute = 60 Seconds = 60 * 1000 Milliseconds
                val2 *= 1000; // 1 Second = 1000 Milliseconds
                // Add the Milliseconds and return it
                return val0 + val1 + val2;
            }
            else
            {
                return 0;
            }
        }
        else
        {
            return 0;
        }
    }
    
    function showSegmentsText(searchValue)
    {
        // Hide other Tabs
        Opencast.Description.hideDescription();
        Opencast.segments.hideSegments();
        Opencast.search.hideSearch();

        $('#oc_btn-slidetext').attr(
        {
            title: SEGMENTS_TEXT_HIDE
        });
        $('#oc_btn-slidetext').html(SEGMENTS_TEXT_HIDE);
        $("#oc_btn-slidetext").attr('aria-pressed', 'true');

        // Request JSONP data
        $.ajax(
        {
            url: '../../search/rest/episode.json',
            data: 'id=' + mediaPackageId,
            dataType: 'jsonp',
            jsonp: 'jsonp',
            success: function (data)
            {
                // get rid of every '@' in the JSON data
                // data = $.parseJSON(JSON.stringify(data).replace(/@/g, ''));

                data['search-results'].result.segments.currentTime = getTimeInMilliseconds(Opencast.Player.getCurrentTime());
                
                // Set Duration until this Segment ends
                var completeDuration = 0;
                $.each(data['search-results'].result.segments.segment, function(i, value)
                {
                    // Set a Duration until the Beginning of this Segment
                    data['search-results'].result.segments.segment[i].durationExcludingSegment = completeDuration;
                    completeDuration += parseInt(data['search-results'].result.segments.segment[i].duration);
                    // Set a Duration until the End of this Segment
                    data['search-results'].result.segments.segment[i].durationIncludingSegment = completeDuration;
                });

                // Create Trimpath Template
                Opencast.segments_text_Plugin.addAsPlugin($('div#oc_slidetext-left'), data['search-results'].result.segments);

                // Make visible
                $('#oc_slidetext').show();
                $('.oc-segments-preview').css('display', 'block');
            }
        });
    }

    function hideSegmentsText()
    {
        $('#oc_btn-slidetext').attr(
        {
            title: SEGMENTS_TEXT
        });
        $('#oc_btn-slidetext').html(SEGMENTS_TEXT);
        $("#oc_btn-slidetext").attr('aria-pressed', 'false');
        $('#oc_slidetext').hide();
    }

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
     * @description Set the mediaPackageId
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
