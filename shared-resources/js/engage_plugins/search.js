/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace search
 */
Opencast.search = (function ()
{
    // Variable for the storage of the processed jsonp-Data
    var dataStor, staticInputElem, mediaPackageId;
    var staticImg = 'url("../img/jquery/ui-bg_flat_75_fde7ce_40x100.png") repeat-x scroll 50% 50% #FDE7CE';
    var SEARCH = 'Search this Recording';
    var colorFirst = '#C0C0C0';
    var colorSecond = '#ADD8E6';
    var colorThird = '#90EE90';
    
    /**
     * @memberOf Opencast.search
     * @description Returns the first Color - Colors for Segments < 30% Relevance
     * @return the first Color - Colors for Segments < 30% Relevance
     */
    function getFirstColor()
    {
        return colorFirst;
    }
    
    /**
     * @memberOf Opencast.search
     * @description Returns the second Color - Colors for Segments < 70% Relevance
     * @return the second Color - Colors for Segments < 70% Relevance
     */
    function getSecondColor()
    {
        return colorSecond;
    }
    
    /**
     * @memberOf Opencast.search
     * @description Returns the third Color - Colors for Segments >= 70% Relevance
     * @return the third Color - Colors for Segments >= 70% Relevance
     */
    function getThirdColor()
    {
        return colorThird;
    }
    
    /**
     * @memberOf Opencast.search
     * @description Initializes the search view
     */
    function initialize()
    {
        // Do nothing in here
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
     * @memberOf Opencast.search
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
                    bgColor = colorFirst;
                }
                else if (relevance < Math.round(maxRelevance * 70 / 100))
                {
                    bgColor = colorSecond;
                }
                else
                {
                    bgColor = colorThird;
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
            var segment = 'td#segment' + i;
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
     * @param elem The Input ELement (currently a workaround)
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
        // Show a loading Image
        $('#oc_search-segment').show();
        $('#search-loading').show();
        $('#oc-search-result').hide();
        $('.oc-segments-preview').css('display', 'block');
        var mediaPackageId = Opencast.engage.getMediaPackageId();
        // Request JSONP data
        $.ajax(
        {
            url: "../../search/episode.json",
            data: "id=" + mediaPackageId + "&q=" + escape(searchValue),
            dataType: 'jsonp',
            jsonp: 'jsonp',
            success: function (data)
            {
                // get rid of every '@' in the JSON data
                // dataStor = $.parseJSON(JSON.stringify(data).replace(/@/g, ''));
                dataStor = data;
                // Check if Segments + Segments Text is available
                var segmentsAvailable = (dataStor['search-results'].result.segments !== undefined) && (dataStor['search-results'].result.segments.segment.length > 0);
                if (segmentsAvailable) // dataStor['search-results'] && dataStor['search-results'].result
                {
                    dataStor['search-results'].result.segments.currentTime = Opencast.Utils.getTimeInMilliseconds(Opencast.Player.getCurrentTime());
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
                    // Create Trimpath Template nd add it to the HTML
                    Opencast.search_Plugin.addAsPlugin($('#oc-search-result'), dataStor['search-results'].result.segments, searchValue);
                    // Make visible
                    $('.oc-segments-preview').css('display', 'block');
                }
                else
                {
                    // If no Segment Data is available
                    $('#oc-search-result').html('No Segment Data available');
                }
                $('#oc_search-segment').show();
                $('#search-loading').hide();
                $('#oc-search-result').show();
            },
            // If no data comes back
            error: function (xhr, ajaxOptions, thrownError)
            {
                $('#oc-search-result').html('No Segment Data available');
                $('#search-loading').hide();
                $('#oc-search-result').show();
            }
        });
        // If the Search Result Field contains nothing: Clear and display a Message
        if($('#oc-search-result').empty)
        {
            $('#oc-search-result').html('No Search Result available');
            $('#oc_search-segment').show();
            $('#search-loading').hide();
            $('#oc-search-result').show();
        }
    }
    
    /**
     * @memberOf Opencast.search
     * @description Hides the whole Search
     */
    function hideSearch()
    {
        $("#oc_btn-lecturer-search").attr('aria-pressed', 'false');
        $('#oc_search-segment').hide();
        // Write the default value if no search value has been given
        if ($(staticInputElem).val() === '')
        {
            $(staticInputElem).val(SEARCH);
        }
    }
    
    /**
     * @memberOf Opencast.search
     * @description Initializes the search view
     */
    function initialize()
    {
        // Do nothing in here
    }
    
    return {
        getFirstColor: getFirstColor,
        getSecondColor: getSecondColor,
        getThirdColor: getThirdColor,
        initialize: initialize,
        showResult: showResult,
        hideSearch: hideSearch,
        setMediaPackageId: setMediaPackageId
    };
}());
