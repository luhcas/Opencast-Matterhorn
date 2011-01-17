/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace annotation_chapter delegate. This file contains the rest endpoint and passes the data to the annotation_chapter plugin
 */
Opencast.Series = (function ()
{
    var mediaPackageId;
    var series_id;
    var series_visible = false;
    var position_set = false;
    
    /**
     * @memberOf Opencast.Series
     * @description toogle series Dropdown
     */
    function doToggleSeriesDropdown()
    {
        if (series_visible)
        {
            hideSeriesDropdown();
        }
        else
        {
            showSeriesDropdown()
            $('#oc_series').focus();
        }
    }

    /**
     * @memberOf Opencast.Series
     */
    function showSeriesPage(page)
    {
        $.ajax(
        {
            url: '../../search/rest/series.json?id=' + series_id + '&episodes=true&limit=20&offset=' + (page - 1) * 20 + '&jsonp=?',
            dataType: 'jsonp',
            jsonp: 'jsonp',
            success: function (data)
            {
                data = createDataForPlugin(data);
                data['search-results'].currentPage = page;
                //add as a plugin
                Opencast.Series_Plugin.addAsPlugin($('#oc_series'), data['search-results']);
            }
        });
    }
    
    /**
     * @memberOf Opencast.Series
     * @description hide series Dropdown
     */
    function hideSeriesDropdown()
    {
        $('#oc_series').hide();
        $('#oc_series').attr(
        {
            'aria-hidden': 'true',
            'tabindex': '-1'
        });
        series_visible = false;
    }
    
    /**
     * @memberOf Opencast.Series
     * @description show series Dropdown
     */
    function showSeriesDropdown()
    {
        $.ajax(
        {
            url: '../../search/rest/series.json?id=' + series_id + '&episodes=true&limit=20&offset=0&jsonp=?',
            dataType: 'jsonp',
            jsonp: 'jsonp',
            success: function (data)
            {
                // get rid of every '@' in the JSON data
                data = $.parseJSON(JSON.stringify(data).replace(/@/g, ''));
                data = createDataForPlugin(data);
                data['search-results'].currentPage = 1;
                //add as a plugin
                Opencast.Series_Plugin.addAsPlugin($('#oc_series'), data['search-results']);
                //set position of div and make it visible
                if (!position_set)
                {
                    $("#oc_series").position(
                    {
                        of: $("#oc_see-more-button"),
                        my: "left top",
                        at: "left bottom"
                    });
                    position_set = true;
                }
                $('#oc_series').show();
                $('#oc_series').attr(
                {
                    'aria-hidden': 'false',
                    'tabindex': '0'
                });
                series_visible = true;
            }
        });
    }
    
    /**
     * @memberOf Opencast.Series
     */
    function createDataForPlugin(data)
    {
        //set current mediapackageId for
        data['search-results'].currentMediaPackageId = mediaPackageId;
        //if there is only one episode in the series make it an array
        if (data['search-results'].result.length == undefined)
        {
            var tmp = new Array(data['search-results'].result);
            data['search-results'].result = tmp;
        }
        //reverse the array to get the results in chronical order
        data['search-results'].result.reverse();
        //change date format to MM/DD/YYYY
        //add number
        //cut title and add '...'
        for (var i = 0; i < data['search-results'].result.length; i++)
        {
            data['search-results'].result[i].dcCreated = getLocaleDate(data['search-results'].result[i].dcCreated);
            data['search-results'].result[i].dcNumber = i + 1;
            data['search-results'].result[i].dcTitleShort = data['search-results'].result[i].dcTitle.substr(0, 35) + "...";
        }
        //create pages
        data['search-results'].pages = [];
        for (var i = 1; i <= Math.ceil(data['search-results'].total / 20); i++)
        {
            data['search-results'].pages.push(i);
        }
        return data;
    }
    
    /**
     * @memberOf Opencast.Series
     * @description create date in format MM/DD/YYYY
     */
    function getLocaleDate(timeDate)
    {
        return timeDate.substring(0, 10);
    }
    
    /**
     * @memberOf Opencast.Series
     * @description Set the mediaPackageId
     * @param String mediaPackageId
     */
    function setMediaPackageId(id)
    {
        mediaPackageId = id;
        $.ajax(
        {
            url: '../../search/rest/episode.json?id=' + mediaPackageId + '&jsonp=?',
            dataType: 'jsonp',
            jsonp: 'jsonp',
            success: function (data)
            {
                series_id = data['search-results'].result.dcIsPartOf;
                if (series_id != '')
                {
                    $.ajax(
                    {
                        url: '../../search/rest/series.json?id=' + series_id + '&episodes=true&limit=20&offset=0&jsonp=?',
                        dataType: 'jsonp',
                        jsonp: 'jsonp',
                        success: function (data)
                        {
                            if (data['search-results'].result.length > 1)
                            {
                                $('#oc_player-head-see-more').show();
                            }
                        }
                    });
                }
            }
        });
    }
    
    return {
        showSeriesDropdown: showSeriesDropdown,
        hideSeriesDropdown: hideSeriesDropdown,
        setMediaPackageId: setMediaPackageId,
        showSeriesPage: showSeriesPage,
        doToggleSeriesDropdown: doToggleSeriesDropdown
    };
}());
