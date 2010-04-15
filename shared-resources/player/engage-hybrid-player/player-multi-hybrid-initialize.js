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
    
    var myWidth           = 0,
    myHeight = 0;
    
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
        init();
       
        $('#wysiwyg').wysiwyg({
            controls: {
              strikeThrough : { visible : true },
              underline     : { visible : true },
              
              separator00 : { visible : true },
              
              justifyLeft   : { visible : true },
              justifyCenter : { visible : true },
              justifyRight  : { visible : true },
              justifyFull   : { visible : true },
              
              separator01 : { visible : true },
              
              indent  : { visible : true },
              outdent : { visible : true },
              
              separator02 : { visible : true },
              
              subscript   : { visible : true },
              superscript : { visible : true },
              
              separator03 : { visible : true },
              
              undo : { visible : true },
              redo : { visible : true },
              
              separator04 : { visible : true },
              
              insertOrderedList    : { visible : true },
              insertUnorderedList  : { visible : true },
              insertHorizontalRule : { visible : true },

              separator07 : { visible : true },
              
              cut   : { visible : true },
              copy  : { visible : true },
              paste : { visible : true }
            }
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

        $("#oc_current-time").attr('role', 'timer');
        $("#oc_edit-time").attr('role', 'timer');
        
        $("#oc_btn-slides").attr('role', 'button');
        $("#oc_btn-slides").attr('aria-pressed', 'false'); 
        
        
        // Handler for .click()
        $('#oc_btn-skip-backward').click(function() 
        {
        	Opencast.Player.doSkipBackward();
        });
        $('#oc_btn-play-pause').click(function() 
        {
        	Opencast.Player.doTogglePlayPause();
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
        $('#oc_current-time').click(function() 
        {
        	Opencast.Player.showEditTime();
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
           	Opencast.Player.doRewind();
        });
        
        $('#oc_btn-play-pause').mousedown(function() 
        {
          	Opencast.Player.PlayPauseMouseOut();
        });
        $('#oc_btn-fast-forward').mousedown(function() 
        {
            this.className='oc_btn-fast-forward-clicked';
            Opencast.Player.doFastForward();
            
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
        	Opencast.Player.stopFastForward();
        });
        $('#oc_btn-skip-forward').mouseup(function() 
        {
        	this.className='oc_btn-skip-forward-over';
        });
              
        // Handler onBlur
        $('#oc_edit-time').blur(function() 
        {
        	Opencast.Player.hideEditTime(); 	
        });
        
        // Handler keypress
        $('#oc_edit-time').keypress(function(event) 
        {
        	if (event.keyCode == '13') 
        	{
        		Opencast.Player.editTime();
        	};	
        });
    });
    
    
    /*
     * 
     * http://www.roytanck.com
     * Roy Tanck
     * http://www.this-play.nl/tools/resizer.html
     * 
     * */
    function reportSize() 
    {
        myWidth = 0; 
        myHeight = 0;
        if (typeof (window.innerWidth) === 'number') 
        {
            //Non-IE
            myWidth = window.innerWidth;
            myHeight = window.innerHeight;
        } 
        else 
        {
            if (document.documentElement && (document.documentElement.clientWidth || document.documentElement.clientHeight)) 
            {
                //IE 6+ in 'standards compliant mode'
                myWidth = document.documentElement.clientWidth;
                myHeight = document.documentElement.clientHeight;
            } 
            else 
            {
                if (document.body && (document.body.clientWidth || document.body.clientHeight)) 
                {
                    //IE 4 compatible
                    myWidth = document.body.clientWidth;
                    myHeight = document.body.clientHeight;
                }
            }
        }
    }
    
    function doTest()
    {
      
    	reportSize();
        if (myHeight > 600)
        {
           //$('#oc_body').css("height", (myHeight - 50 + "px"));
        	//$('#oc_flash-player').css("height", (myHeight - 180 + "px"));
        	
           
           
           if (Opencast.Player.getShowSections() === false)
            {
             $('#oc_flash-player').css("height", (myHeight - 180 + "px"));
            }
            else
            {
              $('#oc_flash-player').css("height", (myHeight - 288 + "px"));
            }
        }
        //
        var margin= 0;
        margin = $('#oc_video-controls').width();
        margin = (margin - 165 ) /2;
        $('#oc_btn-skip-backward').css("margin-left", (margin +"px") );
    }

    function init()
    {
        window.onresize = doTest;
        doTest();
    }
    
    
    

    return {
        doTest : doTest
    };
}());

