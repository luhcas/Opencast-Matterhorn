/*global $, Player, Videodisplay, fluid, Scrubber*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

/**
    @namespace the global Opencast namespace
*/
var Opencast = Opencast || {};

/**
    @namespace FlashVersion
*/
Opencast.Scrubber = (function () 
{
   
    function init()
    {
        $('#draggable').bind('dragstart', function (event, ui) 
        {
            Opencast.Player.setDragging(true);

            $('#scrubber').css("filter", "alpha(opacity:0)");
            $('#scrubber').css("KHTMLOpacity", "0.00");
            $('#scrubber').css("MozOpacity", "0.00");
            $('#scrubber').css("opacity", "0.00");
            $("#scrubber").focus();
        });

        $("#draggable").draggable({ axis: 'x', containment: 'parent' });

        $('#draggable').bind('drag', function (event, ui) 
        {
            $("#scrubber").css("left", $(this).css("left"));
            var newPosition = Math.round(($("#draggable").position().left / $("#scubber-channel").width()) * Opencast.Player.getDuration());
            Videodisplay.seek(newPosition);
   
        });

        $('#draggable').bind('dragstop', function (event, ui)         {

            Opencast.Player.setDragging(false);
            $("#scrubber").css("left", $(this).css("left"));
            $("#play-progress").css("width", $(this).css("left"));
            $('#scrubber').css("filter", "alpha(opacity:100)");
            $('#scrubber').css("KHTMLOpacity", "1.00");
            $('#scrubber').css("MozOpacity", "1.00");
            $('#scrubber').css("opacity", "1.00");

            var newPosition = Math.round(($("#draggable").position().left / $("#scubber-channel").width()) * Opencast.Player.getDuration());
            Videodisplay.seek(newPosition);
        });

        $("#scubber-channel").click(function (e)
        {
           var x = e.pageX - this.offsetLeft;
        x = Math.max(4, x - 8);
        var sc_x = $("#scrubber").position().left;

        if (x < (sc_x - 8) || (sc_x + 8) < x)
        {
            $("#draggable").css("left", x);
            $("#scrubber").css("left", x);
            $("#play-progress").css("width", x);

            var newPosition = Math.round((x / $("#scubber-channel").width()) * Opencast.Player.getDuration());
            Videodisplay.seek(newPosition);
        }

        }); 
        
        $("#segment-holder-empty").click(function (e)
        {
            var x = e.pageX - this.offsetLeft;
            x = Math.max(4, x - 8);
            var sc_x = $("#scrubber").position().left;

            if (x < (sc_x - 8) || (sc_x + 8) < x)
            {
                $("#draggable").css("left", x);
                $("#scrubber").css("left", x);
                $("#play-progress").css("width", x);
    
                var newPosition = Math.round((x / $("#segment-holder-empty").width()) * Opencast.Player.getDuration());
                Videodisplay.seek(newPosition);
            }

        }); 


        $("#draggable").click(function (e)
        {
            $("#scrubber").focus();
        }); 
    }
    
    return {
        init: init
    };
}());
