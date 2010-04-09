/*global $, Player, Videodisplay, fluid, Scrubber*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

/**
    @namespace the global Opencast namespace
*/
var Opencast = Opencast || {};

/**
    @namespace FlashVersion
*/
Opencast.Initialize = (function () 
{
    
    
    
    /**
        @memberOf Opencast.Player
        @description Keylistener.
     */
    function keyboardListener() {
    
        $(document).keyup(function (event) {

            if (event.altKey === true && event.ctrlKey === true) 
            {
                if (event.which === 77 || event.which === 109) // press m or M
                {
                    Opencast.Player.doToggleMute();
                }
                if (event.which === 80 || event.which === 112 || event.which === 83 || event.which === 84 || event.which === 116 || event.which === 115 || event.which === 85 || event.which === 117  || event.which === 68 || event.which === 100 || event.which === 48 || event.which === 49 || event.which === 50 || event.which === 51 || event.which === 52 || event.which === 53 || event.which === 54  || event.which === 55 || event.which === 56 || event.which === 57 || event.which === 67 || event.which === 99 || event.which === 82 || event.which === 114 || event.which === 70 || event.which === 102 || event.which === 83 || event.which === 115 || event.which === 73 || event.which === 105)
                {
                    Videodisplay.passCharCode(event.which);
                }
                event.preventDefault();
            }
        });
    }
    
    // http://javascript-array.com/scripts/jquery_simple_drop_down_menu/
    var timeout         = 200;
    var closetimer		= 0;
    var ddmenuitem      = 0;

    function dropdown_open()
    {	dropdown_canceltimer();
        dropdown_close();
    	ddmenuitem = $(this).find('ul').eq(0).css('visibility', 'visible');
    }

    function dropdown_close()
    {	if(ddmenuitem) ddmenuitem.css('visibility', 'hidden');}

    function dropdown_timer()
    {	closetimer = window.setTimeout(dropdown_close, timeout);}

    function dropdown_canceltimer()
    {	if(closetimer)
    	{	window.clearTimeout(closetimer);
    		closetimer = null;}}
    
    $(document).ready(function () {
        keyboardListener();
        var simpleEdit = fluid.inlineEdit("#simpleEdit", {
            selectors : {
                text: ".editableText",
                editContainer: "#editorContainer",
                edit: "#editField"
            },
            useTooltip : true,
            tooltipDelay : 500											
        });
        
        $('#oc_video-size-dropdown > li').bind('mouseover', dropdown_open);
        //$('#oc_video-size-dropdown > li').bind('click', dropdown_open);
    	$('#oc_video-size-dropdown > li').bind('mouseout',  dropdown_timer);
    	
    	$('#oc_volume-dropdown > li').bind('mouseover', dropdown_open);
        //$('#oc_video-size-dropdown > li').bind('click', dropdown_open);
    	$('#oc_volume-dropdown > li').bind('mouseout',  dropdown_timer);
    	
    	
    	// init the aria slider for the volume
        Opencast.ariaSlider.init();
       
        // aria roles
        $("#editorContainer").attr("className", "oc_editTime");
        $("#editField").attr("className", "oc_editTime");
        
        $("#oc_btn-cc").attr('role', 'button');
        $("#oc_btn-cc").attr('aria-pressed', 'false'); 

        $("#oc_btn-volume").attr('role', 'button');
        $("#oc_btn-volume").attr('aria-pressed', 'false');

        $("#oc_btn-play-pause").attr('role', 'button');
        $("#oc_btn-play-pause").attr('aria-pressed', 'false');

        $("#oc_btn-skip-backward").attr('role', 'button');
        $("#oc_btn-skip-backward").attr('aria-labelledby', 'Skip Backward');

        $("#oc_btn-rewind").attr('role', 'button');
        $("#oc_btn-rewind").attr('aria-labelledby', 'Rewind: Control + Alt + R');

        $("#oc_btn-fast-forward").attr('role', 'button');
        $("#oc_btn-fast-forward").attr('aria-labelledby', 'Fast Forward: Control + Alt + F');

        $("#oc_btn-skip-forward").attr('role', 'button');
        $("#oc_btn-skip-forward").attr('aria-labelledby', 'Skip Forward');

        $("#time-current").attr('role', 'timer');
        $("#time-total").attr('role', 'timer');
        
        $("#oc_btn-slides").attr('role', 'button');
        $("#oc_btn-slides").attr('aria-pressed', 'false'); 
        
        
        // Handler for .click()
        $('#oc_btn-skip-backward').click(function() 
        {
        	Opencast.Player.doSkipBackward();
        });
        $('#oc_btn-rewind').click(function()
        {
        	Opencast.Player.doRewind();
        });
        $('#oc_btn-play-pause').click(function() 
        {
        	Opencast.Player.doTogglePlayPause();
        });
        $('#oc_btn-fast-forward').click(function() 
        {
        	Opencast.Player.doFastForward();
        });
        $('#oc_btn-skip-forward').click(function() 
        {
        	Opencast.Player.doSkipForward();
        });
        $('#oc_btn-volume').click(function() 
        {
           	Opencast.Player.doToggleMute();
        });
        $('#oc_btn-cc').click(function() 
        {
            Opencast.Player.doToogleClosedCaptions();
        });
        
        // Handler for .mouseover()
        $('#oc_btn-skip-backward').mouseover(function() 
        {
        	this.className='oc_btn-skip-backward-over';  
        });
        $('#oc_btn-rewind').mouseover(function()
        {
        	this.className='oc_btn-rewind-over';
        });
        $('#oc_btn-play-pause').mouseover(function() 
        {
        	Opencast.Player.PlayPauseMouseOver();
        });
        $('#oc_btn-fast-forward').mouseover(function() 
        {
        	this.className='oc_btn-fast-forward-over';
        });
        $('#oc_btn-skip-forward').mouseover(function() 
        {
        	this.className='oc_btn-skip-forward-over';
        });
        $('#oc_btn-cc').mouseover(function() 
        {
        	if(Opencast.Player.getCaptionsBool() === false)
        	{
        		this.className='oc_btn-cc-over';
        	}
        });
        
        // Handler for .mouseout()
        $('#oc_btn-skip-backward').mouseout(function() 
        {
            this.className='oc_btn-skip-backward';         
        });
        $('#oc_btn-rewind').mouseout(function()
        {
          	this.className='oc_btn-rewind';
        });
        $('#oc_btn-play-pause').mouseout(function() 
        {
          	Opencast.Player.PlayPauseMouseOut();
        });
        $('#oc_btn-fast-forward').mouseout(function() 
        {
           	this.className='oc_btn-fast-forward';
        });
        $('#oc_btn-skip-forward').mouseout(function() 
        {
           	this.className='oc_btn-skip-forward';
        });
        $('#oc_btn-cc').mouseout(function() 
        {
        	if(Opencast.Player.getCaptionsBool() === false)
        	{
        		this.className='oc_btn-cc-off';
        	}
           	
        });
        
        // Handler for .mousedown()
        $('#oc_btn-skip-backward').mousedown(function() 
        {
        	this.className='oc_btn-skip-backward-clicked';   
        });
        $('#oc_btn-rewind').mousedown(function()
        {
           	this.className='oc_btn-rewind-clicked';
        });
        $('#oc_btn-play-pause').mousedown(function() 
        {
          	Opencast.Player.PlayPauseMouseOut();
        });
        $('#oc_btn-fast-forward').mousedown(function() 
        {
            this.className='oc_btn-fast-forward-clicked';
        });
        $('#oc_btn-skip-forward').mousedown(function() 
        {
            this.className='oc_btn-skip-forward-clicked';
        });
                               
        // Handler for .mouseup()
        $('#oc_btn-skip-backward').mouseup(function() 
        {
        	this.className='oc_btn-skip-backward-over';  
        });
        $('#oc_btn-rewind').mouseup(function()
        {
        	this.className='oc_btn-rewind-over';
        	
        	Opencast.Player.stopRewind();
        	
        });
        $('#oc_btn-play-pause').mouseup(function() 
        {
        	Opencast.Player.PlayPauseMouseOver();
        });
        $('#oc_btn-fast-forward').mouseup(function() 
        {
        	this.className='oc_btn-fast-forward-over';
        });
        $('#oc_btn-skip-forward').mouseup(function() 
        {
        	this.className='oc_btn-skip-forward-over';
        });
        
        // to calculate the embed flash height
        var iFrameHeight = document.documentElement.clientHeight;
        var otherDivHeight = 138;
        var flashHeight = iFrameHeight - otherDivHeight;
        $("#oc_flash-player").css('height',flashHeight + 'px'); 
        
       
        
        // to calculate the margin left of the video controls
        var marginleft    = 0;
            controlsWidth = 165
            flashWidth = document.documentElement.clientWidth;
            
           
    	marginleft = Math.round( (flashWidth / 2) - controlsWidth ) / 2;
    	$('.oc_btn-skip-backward').css("margin-left", marginleft + 'px');
    	
    	
    });
    
    return {
       
    };
}());

