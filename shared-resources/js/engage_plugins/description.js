/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace annotation_chapter delegate. This file contains the rest endpoint and passes the data to the annotation_chapter plugin
 */
Opencast.Description = (function(){

    /**
     *  variables
     */
    var mediaPackageId, duration, DESCRIPTION = "Description", DESCRIPTION_HIDE = "Hide Description";

    function showDescription()
    {
        $('#oc_btn-description').attr({ title: DESCRIPTION_HIDE});
        $('#oc_btn-description').html(DESCRIPTION_HIDE);
        $("#oc_btn-description").attr('aria-pressed', 'true');
        
        $.getJSON('../../search/rest/episode.json?id=' + mediaPackageId, function(data){
            var timeDate = data['search-results'].result.dcCreated;
            var sd = new Date();
            sd.setFullYear(parseInt(timeDate.substring(0, 4), 10));
            sd.setMonth(parseInt(timeDate.substring(5, 7), 10) - 1);
            sd.setDate(parseInt(timeDate.substring(8, 10), 10));
            sd.setHours(parseInt(timeDate.substring(11, 13), 10));
            sd.setMinutes(parseInt(timeDate.substring(14, 16), 10));
            sd.setSeconds(parseInt(timeDate.substring(17, 19), 10));
            data['search-results'].result.dcCreated = sd.toLocaleString();
            $.getJSON('../../usertracking/rest/stats.json?id=' + mediaPackageId, function(result) {
                data['search-results'].result.dcViews = result.stats.views;
                Opencast.Description_Plugin.addAsPlugin('#oc_description', data['search-results']);
            });
            
        });

        $('#oc_description').show();
    }

    function hideDescription()
    {
        $('#oc_btn-description').attr({ title: DESCRIPTION});
        $('#oc_btn-description').html(DESCRIPTION);
        $("#oc_btn-description").attr('aria-pressed', 'false');
        $('#oc_description').hide();
    }

    function doToggleDescription()
    {
        if ($('#oc_btn-description').attr("title") === DESCRIPTION) {
            showDescription()
        }
        else {
            hideDescription()
        }
    }

    /**
     * @memberOf Opencast.Analytics
     * @description Set the mediaPackageId
     * @param String mediaPackageId
     */
    function setMediaPackageId(id){
        mediaPackageId = id;
    }

    return {
        showDescription: showDescription,
        hideDescription: hideDescription,
        setMediaPackageId: setMediaPackageId,
        doToggleDescription: doToggleDescription
    };

}());
