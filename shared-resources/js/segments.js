/*global $, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */

var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace segments
 */
Opencast.segments = ( function() {

  var totalPanels,
   segmentTimes,
   beforeSlide = 0,
   currentSlide = 0,
   nextSlide = 0,
   slideLength = 0;
  
  var segmentPreviews;

  function getSegmentPreview(segmentId)
  {
    return segmentPreviews[segmentId];
  }
  
  function getSecondsBeforeSlide(){
    return beforeSlide;
  }

  function getSecondsNextSlide(){
    return nextSlide;
  }
  
  function getSlideLength()
  {
	  return slideLength;
  }
  
  function setSlideLength(length)
  {
	  slideLength = length;
  }

  /**
   * @memberOf Opencast.segments
   * @description Initializes the segments view
   */
  function initialize() {

    totalPanels     = $(".scrollContainer").children().size();

    var $panels       = $('#slider .scrollContainer > div');
    var $container      = $('#slider .scrollContainer');

    if ($panels !== undefined){

      $panels.css({'float' : 'left','position' : 'relative'});

      $("#slider").data("currentlyMoving", false);

       if($panels[0] !== undefined){
          $container
            .css('width', ($panels[0].offsetWidth * $panels.length))
            .css('left', "0px");
          // Disable and grey out "Annotation" Tab
       }
      var scroll = $('#slider .scroll').css('overflow', 'hidden');

      //when the left/right arrows are clicked
      $(".right").click(function(){ change(true); }); 
      $(".left").click(function(){ change(false); });
    }

    var segmentTimes = new Array();
    var seconds;
    $('.segments-time').each( function(i) {
      seconds = $(this).html();
      segmentTimes[i] = seconds;
    });

    segmentPreviews= new Array(); 
    var url;
    $('.oc-segments-preview').each( function(i) {
      url = $(this).html();
      segmentPreviews[i] = url;
    });

    // set the slide length
    setSlideLength(segmentTimes.length);
   
    
    $('#oc_video-player-controls').css('display', 'block');

    // Hide Slide Tab, if there are no slides
    if(segmentTimes.length === 0) {
      Opencast.Player.doToggleSlides();
      $(".oc_btn-skip-backward").hide();
      $(".oc_btn-skip-forward").hide();
    }

 // set the center of the controls
    var margin = $('#oc_video-controls').width();
    var controlswith = 0;
    var playerWidth = $('#oc_video-player-controls').width();
    
  
    
    if (Opencast.segments.getSlideLength() === 0)
    {
        controlswith = 58;
        margin = ((margin - controlswith) / 2 ) - 8;
        $(".oc_btn-rewind").css("margin-left", margin + "px");
    }
    else
    {
        controlswith = 90;
        margin = ((margin - controlswith) / 2 ) - 8;
        $('#oc_btn-skip-backward').css("margin-left", (margin + "px"));
    }
    
    // set the controls visible
    $('#oc_video-player-controls').css('visibility', 'visible');
    
    
 // player size
    if( playerWidth < 460 && playerWidth >= 380)
    {
        $(".oc_btn-skip-backward").css('display','none');
        $(".oc_btn-skip-forward").css('display','none');
        $('#oc_video-controls').css('width','20%');
        $('#oc_video-cc').css('width','12%');
        $('#oc_video-time').css('width','40%');
        $('.oc_slider-volume-Rail').css('width', '45px');
        controlswith = 58;
        margin = $('#oc_video-controls').width();
        margin = ((margin - controlswith) / 2 ) - 8;
        $(".oc_btn-rewind").css("margin-left", margin + "px");
    }
    else if (playerWidth < 380 && playerWidth >= 300)
    {
        $(".oc_btn-skip-backward").css('display','none');
        $(".oc_btn-skip-forward").css('display','none');
        
        $(".oc_btn-rewind").css('display','none');
        $(".oc_btn-fast-forward").css('display','none');
        
        $("#oc_video-cc").css('display','none');
        $("#oc_video-cc").css('width','0%');
        
        
        
        $('#simpleEdit').css('font-size','0.8em');
        $('#simpleEdit').css('margin-left','1px');
        
        $('#oc_current-time').css('width','45px');
        $('#oc_edit-time').css('width','45px');
        $('#oc_duration').css('width','45px');
        $('#oc_edit-time-error').css('width','45px');
        
        
        
        $('#oc_sound').css('width','27%');
        
        $('#oc_video-controls').css('width','8%');
        
        
        $('#oc_video-time').css('width','48%');
        
        $('.oc_slider-volume-Rail').css('width', '45px');
        
        controlswith = 16;
        margin = $('#oc_video-controls').width();
        margin = ((margin - controlswith) / 2 ) - 8;
        $("#oc_btn-play-pause").css("margin-left", margin + "px");
    }



    $(document).everyTime(500, function(index) {
      var currentPosition = parseInt(Opencast.Player.getCurrentPosition());
      var last = 0;
      var cur = 0;
      var ibefore = 0;
      var lastIndex;

      // last segment
      if (segmentTimes[segmentTimes.length-1] <= currentPosition){
        if (segmentTimes[segmentTimes.length-1] <= currentPosition && currentPosition < parseInt(segmentTimes[segmentTimes.length-1]) + 3)
          lastIndex = 2;
        else
          lastIndex = 1;
        var ibefore = Math.max(segmentTimes.length-lastIndex,0);
        beforeSlide = segmentTimes[ibefore];
        currentSlide = segmentTimes[segmentTimes.length-1];
        nextSlide = currentSlide;
      } else{
        for (i in segmentTimes)
        {
          cur = segmentTimes[i];
          
          if (last <= currentPosition && currentPosition < cur){
            
            if (last <= currentPosition && currentPosition < parseInt(last) + 3)
              lastIndex = 2;
            else
              lastIndex = 1;

            ibefore = Math.max(parseInt(i)-lastIndex,0);
            beforeSlide = segmentTimes[ibefore];
            currentSlide = last;
            nextSlide = segmentTimes[i];
            break;
          }
          last = cur;
        }
      }
    }, 0);

  }
  
  /**
   * @memberOf Opencast.segments
   * @description Sets scrollContainer width
   */
  function sizeSliderContainer()
    {
      var $panels       = $('#slider .scrollContainer > div');
      var $container      = $('#slider .scrollContainer');
      if($panels[0] !== undefined) {
        $container
              .css('width', ($panels[0].offsetWidth * $panels.length))
              .css('left', "0px");
      }
    }

  /**
   * @memberOf Opencast.segments
   * @description Function initializes segments view
   * @param boolean direction true = right, false = left
   */
  function change(direction) {
    var leftValue    = parseFloat($(".scrollContainer").css("left"), 10)
    var scrollContainerWidth    = parseFloat( $(".scrollContainer").css("width"), 10);
    var sliderWidth    = parseFloat($(".scroll").css("width"), 10);
    var dif = sliderWidth - scrollContainerWidth;
    
      //if not at the first or last panel
    if((!direction && (leftValue>=0)) || (direction && (leftValue<= dif))) { return false; }  

    //if not currently moving
    if (($("#slider").data("currentlyMoving") == false)) {
      $("#slider").data("currentlyMoving", true);
      var maxMove = (scrollContainerWidth + leftValue) - sliderWidth;
      var movement   = direction ? leftValue - Math.min(sliderWidth, maxMove) : leftValue + Math.min(sliderWidth, leftValue*(-1));

      $(".scrollContainer")
        .stop()
        .animate({
          "left": movement
        }, function() {
          $("#slider").data("currentlyMoving", false);
        });
    }
  }

  return {
    getSegmentPreview : getSegmentPreview,
    getSecondsBeforeSlide : getSecondsBeforeSlide,
    getSecondsNextSlide : getSecondsNextSlide,
    getSlideLength : getSlideLength,
    initialize : initialize,
    sizeSliderContainer : sizeSliderContainer
  };
}());
