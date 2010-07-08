/*global $, Player, Videodisplay, fluid, Scrubber*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

/**
 *  Copyright 2009 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

/**
    @namespace the global Opencast namespace
*/
var Opencast = Opencast || {};

/**
    @namespace FlashVersion
*/
Opencast.Scrubber = (function () 
{
    var locked             = false,
    EMBED                  = 'embed';
    currentScrubberPositon = 0,
    tooltipTop             = 0,
    offsetX                = 25,
    offsetY                = 35;
    

    /**
        @memberOf Opencast.Scrubber
        @description Create a tooltip div
     */
    function oc_tooltip()
    {
        var $tooltip = $('<div id="divToolTip" value="00:00:00">00:00:00</div>');
        $('body').append($tooltip);
        $tooltip.hide();
    }
    
    /**
        @memberOf Opencast.Scrubber
        @description Initialize the scrubber
     */
    function init(player)
    {
        var _player = player;
    	
    	$('#draggable').bind('dragstart', function (event, ui) 
        {
            tooltipTop = event.pageY - offsetY;
            $("#divToolTip").css('top', tooltipTop);
            $("#divToolTip").css('left', event.pageX - offsetX);
            $("#divToolTip").fadeIn();
        
            Opencast.Player.setDragging(true);
            
            currentScrubberPositon = parseInt($(this).css("left").replace(/[^0-9:]/g, ''), 10);
            
            if (currentScrubberPositon >= $("#scubber-channel").width())
            {
                currentScrubberPositon = $('.load-progress').width();
            }
            
            $('#scrubber').css("filter", "alpha(opacity:0)");
            $('#scrubber').css("KHTMLOpacity", "0.00");
            $('#scrubber').css("MozOpacity", "0.00");
            $('#scrubber').css("opacity", "0.00");
            $("#scrubber").focus();
        });

        $("#draggable").draggable({ axis: 'x', containment: 'parent' });

        $('#draggable').bind('drag', function (event, ui) 
        {
            $("#divToolTip").css('top', tooltipTop);
            
            var dragLeft = event.pageX - offsetX + $("#divToolTip").width();
            
            if(dragLeft > $('#oc_flash-player').width() - 20 && _player === EMBED)
            {
            	$("#divToolTip").css('left', $('#oc_flash-player').width() - $("#divToolTip").width() - 10);
            }
            else if(event.pageX - offsetX < 0 && _player === EMBED)
            {
            	$("#divToolTip").css('left', 5);
            }
            else
            {
            	$("#divToolTip").css('left', event.pageX - offsetX);
            }
           
            if (!locked)
            {
                locked = true;
                setTimeout(function ()
                {  
                    locked = false;
                }, 200);
              
                var position =  $(this).css("left").replace(/[^0-9:]/g, ''); 
                var positionInt = parseInt(position, 10);
                var newPosition = 0;
              
                if (positionInt <= $('.load-progress').width() && Opencast.Player.getHtmlBool() === true)
                {
                    $("#scrubber").css("left", $(this).css("left"));
                    newPosition = Math.round(($("#draggable").position().left / $("#scubber-channel").width()) * Opencast.Player.getDuration());
                    Videodisplay.seek(newPosition);
                }
                else if (Opencast.Player.getHtmlBool() === false)
                {
                    $("#scrubber").css("left", $(this).css("left"));
                    newPosition = Math.round(($("#draggable").position().left / $("#scubber-channel").width()) * Opencast.Player.getDuration());
                    Videodisplay.seek(newPosition);
                }
            }
        });

        $('#draggable').bind('dragstop', function (event, ui)         
        {
            var position =  $(this).css("left").replace(/[^0-9:]/g, ''); 
            var positionInt = parseInt(position, 10);
            var newPosition = 0;
        
            if (positionInt <= $('.load-progress').width() && Opencast.Player.getHtmlBool() === true)
            {
                Opencast.Player.setDragging(false);
                $("#scrubber").css("left", $(this).css("left"));
                $("#play-progress").css("width", $(this).css("left"));
                $('#scrubber').css("filter", "alpha(opacity:100)");
                $('#scrubber').css("KHTMLOpacity", "1.00");
                $('#scrubber').css("MozOpacity", "1.00");
                $('#scrubber').css("opacity", "1.00");

                newPosition = Math.round(($("#draggable").position().left / $("#scubber-channel").width()) * Opencast.Player.getDuration());
                Videodisplay.seek(newPosition);
            }
            else if (positionInt > $('.load-progress').width() && Opencast.Player.getHtmlBool() === true)
            {
                Opencast.Player.setDragging(false);
                $("#scrubber").css("left", currentScrubberPositon + 'px');
                $("#play-progress").css("width", currentScrubberPositon + 'px');
                $('#scrubber').css("filter", "alpha(opacity:100)");
                $('#scrubber').css("KHTMLOpacity", "1.00");
                $('#scrubber').css("MozOpacity", "1.00");
                $('#scrubber').css("opacity", "1.00");
                $('#draggable').css("left", currentScrubberPositon + 'px');
                newPosition = Math.round((currentScrubberPositon / $("#scubber-channel").width()) * Opencast.Player.getDuration());
                Videodisplay.seek(newPosition);
            }
            else if (Opencast.Player.getHtmlBool() === false)
            {
                Opencast.Player.setDragging(false);
                $("#scrubber").css("left", $(this).css("left"));
                $("#play-progress").css("width", $(this).css("left"));
                $('#scrubber').css("filter", "alpha(opacity:100)");
                $('#scrubber').css("KHTMLOpacity", "1.00");
                $('#scrubber').css("MozOpacity", "1.00");
                $('#scrubber').css("opacity", "1.00");
                newPosition = Math.round(($("#draggable").position().left / $("#scubber-channel").width()) * Opencast.Player.getDuration());
                Videodisplay.seek(newPosition);
            }
        
            // hide tooltip
            $("#divToolTip").fadeOut();
        
        });

        $("#scubber-channel").click(function (e)
        {
            var newPosition = 0;
            var x = e.pageX - $("#scubber-channel").offset().left;
            x = Math.max(4, x - 8);
            var sc_x = $("#scrubber").position().left;

            if (x < (sc_x - 8) || (sc_x + 8) < x)
            {
                if ($('.load-progress').width() >= x && Opencast.Player.getHtmlBool() === true)
                {
                    $("#draggable").css("left", x);
                    $("#scrubber").css("left", x);
                    $("#play-progress").css("width", x);

                    newPosition = Math.round((x / $("#scubber-channel").width()) * Opencast.Player.getDuration());
                    Videodisplay.seek(newPosition);
                }
                else if (Opencast.Player.getHtmlBool() === false)
                {
                    $("#draggable").css("left", x);
                    $("#scrubber").css("left", x);
                    $("#play-progress").css("width", x);

                    newPosition = Math.round((x / $("#scubber-channel").width()) * Opencast.Player.getDuration());
                    Videodisplay.seek(newPosition);
                }
            }
        }); 
        
        $("#segment-holder-empty").click(function (e)
        {
            var x = e.pageX - $("#segment-holder-empty").offset().left;
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
        
        // create tooltip
        oc_tooltip();
    }
    

    return {
        init: init
    };
}());

