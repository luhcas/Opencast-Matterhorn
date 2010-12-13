/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {
};

// Variable for the storage of the processed jsonp-Data
var dataStor;
var staticImg = 'url("../img/jquery/ui-bg_flat_75_fde7ce_40x100.png") repeat-x scroll 50% 50% #FDE7CE';
var SEARCH = 'Search this Recording';
var staticInputElem;

/**
 * @namespace the global Opencast namespace search
 */
Opencast.search = (function ()
{
    /**
     * @memberOf Opencast.search
     * @description Initializes the search view
     */
    function initialize()
    {
        $('#oc_search-lect').html('');
        $('#oc-search-text').html('');
    }

    /**
     *  variables
     */
    var mediaPackageId;

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

    /**
     * @memberOf Opencast.search
     * @description Set the mediaPackageId
     * @param String mediaPackageId
     */
    function setMediaPackageId(id)
    {
        mediaPackageId = id;
    }

    /**
     * @description Prepares the Data:
     * - Adds a background color correlating to the relevance
     * - Highlights the search values inside the text segments
     * - Highlights the search values inside the scrubber colored
     * @param value The search value
     */
    function prepareData(value)
    {
        // Go through all segments to get the max relevance
        var maxRelevance = 0;
        $(dataStor['search-results'].result.segments.segment).each(function (i)
        {
            var rel = parseInt(this.relevance);
            if (maxRelevance < rel)
            {
                maxRelevance = rel;
            }
        });

        // Prepare each segment
        $(dataStor['search-results'].result.segments.segment).each(function (i)
        {
            var bgColor = 'none';
            var text = this.text + '';
            // Remove previously marked Text
            text = text.replace(new RegExp('<span class=\'marked\'>', 'g'), '');
            text = text.replace(new RegExp('</span>', 'g'), '');

            var relevance = parseInt(this.relevance);

            // if no search value exists
            if (value === '')
            {
                this.display = true;
            }
            // If the relevance is greater than zero and a search value exists
            else if (relevance > 0)
            {
                this.display = true;
                // Add new Markers
                text = text.replace(new RegExp(value, 'gi'), '<span class=\'marked\'>' + value + '</span>');

                // Set the background color correlated to the relevance
                if (relevance < Math.round(maxRelevance * 30 / 100))
                {
                    bgColor = "#C0C0C0";
                }
                else if (relevance < Math.round(maxRelevance * 70 / 100))
                {
                    bgColor = "#ADD8E6";
                }
                else
                {
                    bgColor = "#90EE90";
                }
            }
            // if the relevance is too small but a search value exists
            else
            {
                this.display = false;
            }

            // Set background of the table tr
            this.backgroundColor = bgColor;
            // Set background of the scrubber elements
            var segment = 'td#segment' + (i + 1);
            if (bgColor !== 'none')
            {
                // The image from jquery ui overrides the background-color, so: remove it
                $(segment).css('background', 'none');
            }
            else
            {
                // Restore the image from jquery ui
                $(segment).css('background', staticImg);
            }
            $(segment).css('backgroundColor', bgColor);

            // Set processed text
            this.text = text;
        });
    }

    /**
     * @memberOf Opencast.search
     * @description Does the search
     * @param searchValue The search value
     */
    function showResult(elem, searchValue)
    {
        staticInputElem = elem;

        // Don't search for the default value
        if ((searchValue === SEARCH) || ($(staticInputElem).val() === SEARCH))
        {
            searchValue = '';
            $(staticInputElem).val('');
        }

        // Hide other Tabs
        Opencast.Description.hideDescription();
        Opencast.segments.hideSegments();
        Opencast.segments_text.hideSegmentsText();

        $("#oc_btn-lecturer-search").attr('aria-pressed', 'true');

        var mediaPackageId = Opencast.engage.getMediaPackageId();

        // Request JSONP data
        $.ajax(
        {
            url: "../../search/rest/episode.json",
            data: "id=" + mediaPackageId + "&q=" + escape(searchValue),
            dataType: 'jsonp',
            jsonp: 'jsonp',
            success: function (data)
            {
                // get rid of every '@' in the JSON data
                // dataStor = $.parseJSON(JSON.stringify(data).replace(/@/g, ''));
                
                dataStor = data;
                if (dataStor['search-results'] && dataStor['search-results'].result)
                {
                    dataStor['search-results'].result.segments.currentTime = getTimeInMilliseconds(Opencast.Player.getCurrentTime());

                    // Set Duration until this Segment ends
                    var completeDuration = 0;
                    $.each(dataStor['search-results'].result.segments.segment, function (i, value)
                    {
                        // Set a Duration until the Beginning of this Segment
                        dataStor['search-results'].result.segments.segment[i].durationExcludingSegment = completeDuration;
                        completeDuration += parseInt(dataStor['search-results'].result.segments.segment[i].duration);
                        // Set a Duration until the End of this Segment
                        dataStor['search-results'].result.segments.segment[i].durationIncludingSegment = completeDuration;
                    });

                    // Prepare the Data
                    prepareData(searchValue);
                    // Process
                    addAsPluginAndMakeVisible(searchValue);
                }
            }
        });
    }

    /**
     * Hides the whole Search
     */
    function hideSearch()
    {
        $("#oc_btn-lecturer-search").attr('aria-pressed', 'false');
        $('#oc_search-lect').hide();
        $('#oc_search-left').hide();
        $('#oc-search').hide();

        $('div#oc_search-lect').css('display', 'none');
        $('div#oc_search-left').css('display', 'none');
        $('div#oc-search').css('display', 'none');

        // Write the default value if no search value has been given
        if ($(staticInputElem).val() === '')
        {
            $(staticInputElem).val(SEARCH);
        }
    }

    /**
     * Calls the Plugin-Call and makes the search visible
     * @param searchValue The search value
     */
    function addAsPluginAndMakeVisible(searchValue)
    {
        // Create Trimpath Template
        Opencast.search_Plugin.addAsPlugin($('div#oc-search'), dataStor['search-results'].result.segments, searchValue);

        // Make visible
        $('#oc_search-lect').show();
        $('#oc_search-left').show();
        $('#oc-search').show();

        $('div#oc_search-lect').css('display', 'block');
        $('div#oc_search-left').css('display', 'block');
        $('div#oc-search').css('display', 'block');
        $('.oc-segments-preview').css('display', 'block');
    }

    /**
     * @memberOf Opencast.search
     * @description Initializes the search view
     */
    function initialize()
    {
        // DO nothing in here
    }

    return {
        initialize: initialize,
        showResult: showResult,
        hideSearch: hideSearch,
        setMediaPackageId: setMediaPackageId
    };
}());
