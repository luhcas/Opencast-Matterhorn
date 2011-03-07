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
        
        // If cashed data are available
        if(Opencast.Description_Plugin.createDescriptionFromCashe())
        {
            // Make visible
            $('#description-loading').hide();
            $('#oc-description').show();
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
                    if ((data === undefined) || (data['search-results'] === undefined) || (data['search-results'].result === undefined))
                    {
                        displayNoDescriptionAvailable("No data defined (1)");
                        return;
                    }
                    
                    // Process data
                    var defaultChar = '-';
                    // Trimpath throws (no) errors if a variable is not defined => assign default value
                    data['search-results'].result.dcCreated = data['search-results'].result.dcCreated || defaultChar;
                    data['search-results'].result.dcSeriesTitle = data['search-results'].result.mediapackage.seriestitle || defaultChar;
                    data['search-results'].result.dcContributor = data['search-results'].result.dcContributor || defaultChar;
                    data['search-results'].result.dcLanguage = data['search-results'].result.dcLanguage || defaultChar;
                    data['search-results'].result.dcViews = data['search-results'].result.dcViews || defaultChar;
                    data['search-results'].result.dcCreator = data['search-results'].result.dcCreator || defaultChar;
                    if (data['search-results'].result.dcCreated != defaultChar)
                    {
                        var timeDate = data['search-results'].result.dcCreated;
                        var sd = new Date();
                        sd.setMinutes(parseInt(timeDate.substring(14, 16), 10));
                        sd.setSeconds(parseInt(timeDate.substring(17, 19), 10));
                        data['search-results'].result.dcCreated = sd.toLocaleString();
                    }
                    
                    // Request JSONP data (Stats)
                    $.ajax(
                    {
                        url: '../../usertracking/stats.json?id=',
                        data: 'id=' + mediaPackageId,
                        dataType: 'jsonp',
                        jsonp: 'jsonp',
                        success: function (result)
                        {
                            data['search-results'].result.dcViews = result.stats.views;
                            // Create Trimpath Template
                            var descriptionSet = Opencast.Description_Plugin.addAsPlugin($('#oc-description'), data['search-results']);
                            if (!descriptionSet)
                            {
                                displayNoDescriptionAvailable("No template available (2)");
                            }
                            else
                            {
                                // Make visible
                                $('#description-loading').hide();
                                $('#oc-description').show();
                            }
                        },
                        // If no data comes back (JSONP-Call #2)
                        error: function (xhr, ajaxOptions, thrownError)
                        {
                            displayNoDescriptionAvailable("No data available (2)");
                        }
                    });
                },
                // If no data comes back (JSONP-Call #1)
                error: function (xhr, ajaxOptions, thrownError)
                {
                    displayNoDescriptionAvailable("No data available (1)");
                }
            });
        }
    }
    
    /**
     * @memberOf Opencast.Description
     * @description Displays that no Description is available
     * @param errorDesc Error Description (optional)
     */
    function displayNoDescriptionAvailable(errorDesc)
    {
        errorDesc = errorDesc || '';
        $('#description-loading').hide();
        var optError = (errorDesc != '') ? (": " + errorDesc) : '';
        $('#oc-description').html('No Description available' + optError);
        $('#oc-description').show();
        $('#scrollcontainer').hide();
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
