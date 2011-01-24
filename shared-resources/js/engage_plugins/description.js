/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace Description
 */
Opencast.Description = (function ()
{
    var mediaPackageId, duration;
    var DESCRIPTION = "Description",
        DESCRIPTION_HIDE = "Hide Description";
        
    /**
     * @memberOf Opencast.Description
     * @description Displays the Description Tab
     */
    function showDescription()
    {
        // Hide other Tabs
        Opencast.segments.hideSegments();
        Opencast.segments_text.hideSegmentsText();
        Opencast.search.hideSearch();
        // Change Tab Caption
        $('#oc_btn-description').attr(
        {
            title: DESCRIPTION_HIDE
        });
        $('#oc_btn-description').html(DESCRIPTION_HIDE);
        $("#oc_btn-description").attr('aria-pressed', 'true');
        // Show a loading Image
        $('#oc_description').show();
        $('#description-loading').show();
        $('#oc-description').hide();
        // Request JSONP data
        $.ajax(
        {
            url: '../../search/episode.json',
            data: 'id=' + mediaPackageId,
            dataType: 'jsonp',
            jsonp: 'jsonp',
            success: function (data)
            {
                var timeDate = data['search-results'].result.dcCreated;
                var sd = new Date();
                sd.setMinutes(parseInt(timeDate.substring(14, 16), 10));
                sd.setSeconds(parseInt(timeDate.substring(17, 19), 10));
                data['search-results'].result.dcCreated = sd.toLocaleString();
                // Request JSONP data (Stats)
                $.ajax(
                {
                    url: '../../usertracking/stats.json?id=' + mediaPackageId,
                    dataType: 'jsonp',
                    jsonp: 'jsonp',
                    success: function (result)
                    {
                        // If episode is part of a series: get series data    
                        if (data['search-results'].result.dcIsPartOf != '')
                        {
                            // Request JSONP data (Series)
                            $.ajax(
                            {
                                url: '../../series/' + data['search-results'].result.dcIsPartOf + ".json",
                                dataType: 'jsonp',
                                jsonp: 'jsonp',
                                success: function (res)
                                {
                                    for (var i = 0; i < res.series.additionalMetadata.metadata.length; i++)
                                    {
                                        if (res.series.additionalMetadata.metadata[i].key == 'title')
                                        {
                                            data['search-results'].result.dcSeriesTitle = res.series.additionalMetadata.metadata[i].value;
                                        }
                                    }
                                    // Create Trimpath Template
                                    Opencast.Description_Plugin.addAsPlugin($('#oc-description'), data['search-results']);
                                    // Make visible
                                    $('#oc_description').show();
                                    $('#description-loading').hide();
                                    $('#oc-description').show();
                                },
                                // If no data comes back (JSONP-Call #3)
                                error: function (xhr, ajaxOptions, thrownError)
                                {
                                }
                            });
                        }
                        else
                        {
                            // Create Trimpath Template
                            Opencast.Description_Plugin.addAsPlugin($('#oc-description'), data['search-results']);
                            // Make visible
                            $('#oc_description').show();
                            $('#description-loading').hide();
                            $('#oc-description').show();
                        }
                    },
                    // If no data comes back (JSONP-Call #2)
                    error: function (xhr, ajaxOptions, thrownError)
                    {
                        $('#scrollcontainer').html('No Description available');
                        $('#scrollcontainer').hide();
                    }
                });
            },
            // If no data comes back (JSONP-Call #1)
            error: function (xhr, ajaxOptions, thrownError)
            {
            }
        });
    }
    
    /**
     * @memberOf Opencast.Description
     * @description Hides the Description Tab
     */
    function hideDescription()
    {
        // Change Tab Caption
        $('#oc_btn-description').attr(
        {
            title: DESCRIPTION
        });
        $('#oc_btn-description').html(DESCRIPTION);
        $("#oc_btn-description").attr('aria-pressed', 'false');
        $('#oc_description').hide();
    }
    
    /**
     * @memberOf Opencast.Description
     * @description Toggles the Description Tab
     */
    function doToggleDescription()
    {
        if ($('#oc_btn-description').attr("title") === DESCRIPTION)
        {
            Opencast.segments.hideSegments();
            Opencast.segments_text.hideSegmentsText();
            Opencast.search.hideSearch();
            showDescription();
        }
        else
        {
            hideDescription();
        }
    }
    
    /**
     * @memberOf Opencast.Description
     * @description Set the mediaPackageId
     * @param String mediaPackageId
     */
    function setMediaPackageId(id)
    {
        mediaPackageId = id;
    }
    
    return {
        showDescription: showDescription,
        hideDescription: hideDescription,
        setMediaPackageId: setMediaPackageId,
        doToggleDescription: doToggleDescription
    };
}());
