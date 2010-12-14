/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {
};
/**
 * @namespace the global Opencast namespace annotation_chapter delegate. This file contains the rest endpoint and passes the data to the annotation_chapter plugin
 */
Opencast.Description = (function ()
{
    /**
     *  variables
     */
    var mediaPackageId, duration, DESCRIPTION = "Description",
        DESCRIPTION_HIDE = "Hide Description";

    function showDescription()
    {
        // Hide other Tabs
        Opencast.segments.hideSegments();
        Opencast.segments_text.hideSegmentsText();
        Opencast.search.hideSearch();
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
            url: '../../search/rest/episode.json',
            data: 'id=' + mediaPackageId,
            dataType: 'jsonp',
            jsonp: 'jsonp',
            success: function (data)
            {
                // get rid of every '@' in the JSON data
                // data = $.parseJSON(JSON.stringify(data).replace(/@/g, ''));
                var timeDate = data['search-results'].result.dcCreated;
                var sd = new Date();
                sd.setMinutes(parseInt(timeDate.substring(14, 16), 10));
                sd.setSeconds(parseInt(timeDate.substring(17, 19), 10));
                data['search-results'].result.dcCreated = sd.toLocaleString();
                // Request JSONP data (Stats)
                $.ajax(
                {
                    url: '../../usertracking/rest/stats.json?id=' + mediaPackageId,
                    dataType: 'jsonp',
                    jsonp: 'jsonp',
                    success: function (result)
                    {
                        // get rid of every '@' in the JSON data
                        // result = $.parseJSON(JSON.stringify(result).replace(/@/g, ''));
                        data['search-results'].result.dcViews = result.stats.views;
                        // If episode is part of a series: get series data    
                        if (data['search-results'].result.dcIsPartOf != '')
                        {
                            $.ajax(
                            {
                                url: '../../series/rest/' + data['search-results'].result.dcIsPartOf + ".json",
                                dataType: 'jsonp',
                                jsonp: 'jsonp',
                                success: function (res)
                                {
                                    // get rid of every '@' in the JSON data
                                    // res = $.parseJSON(JSON.stringify(res).replace(/@/g, ''));
                                    for (var i = 0; i < res.series.metadataList.metadata.length; i++)
                                    {
                                        if (res.series.metadataList.metadata[i].key == 'title')
                                        {
                                            data['search-results'].result.dcSeriesTitle = res.series.metadataList.metadata[i].value;
                                        }
                                    }
                                    // Create Trimpath Template
                                    Opencast.Description_Plugin.addAsPlugin($('#oc-description'), data['search-results']);
                                    // Make visible
                                    $('#oc_description').show();
                                    $('#description-loading').hide();
                                    $('#oc-description').show();
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
                    error: function (xhr, ajaxOptions, thrownError)
                    {
                        $('#scrollcontainer').html('No Description available');
                        $('#scrollcontainer').hide();            
                    }
                });
            }
        });
    }

    function hideDescription()
    {
        $('#oc_btn-description').attr(
        {
            title: DESCRIPTION
        });
        $('#oc_btn-description').html(DESCRIPTION);
        $("#oc_btn-description").attr('aria-pressed', 'false');
        $('#oc_description').hide();
    }

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
     * @memberOf Opencast.Analytics
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