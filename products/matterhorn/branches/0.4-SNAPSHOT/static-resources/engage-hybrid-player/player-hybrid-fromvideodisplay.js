/*global $, Videodisplay, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */


/**
    @namespace the global Opencast namespace FromVideodisplay
*/
Opencast.FromVideodisplay = (function () 
{
    var playing = "playing",
        pausing = "pausing",
        play    = "Play",
        pause   = "Pause",
        unmute  = "Unmute",
        mute    = "Mute",
        ccon    = "Closed Caption On",
        ccoff   = "Closed Caption Off",
        sliderVolume = 'slider_volume_Thumb',
        sliderSeek = 'slider_seek_Thumb';
    
    /**
        @memberOf Opencast.FromVideodisplay
        @description Set the new position of the seek slider.
        @param Number newPosition 
    */
    function setPlayhead(newPosition) 
    {
        Opencast.ariaSlider.changeValueFromVideodisplay(Opencast.ariaSlider.getElementId(sliderSeek), newPosition);
    }
    
    /**
        @memberOf Opencast.FromVideodisplay
        @description Change the css style of the mute/unmute button.
    */
    function setDoUnmute() 
    {
        if ($("#btn_volume").attr("value") === mute) 
        {  
            $("#btn_volume").attr({ 
                value: unmute,
                alt: unmute,
                title: unmute
            });
            $("#btn_volume").attr("className", "oc-btn-volume-high");
        }
    }

    /**
        @memberOf Opencast.FromVideodisplay
        @description Set the new position of the volume slider.
        @param Number newVolume 
    */
    function setVolume(newVolume) 
    {
        if (newVolume !== 0)
        {
            setDoUnmute();
        }
        Opencast.ariaSlider.changeValueFromVideodisplay(Opencast.ariaSlider.getElementId(sliderVolume), newVolume);
    }

    /**
        @memberOf Opencast.FromVideodisplay
        @description Set the current time of the video.
        @param String text 
    */
    function setCurrentTime(text) 
    {
        $("#time-current").text(text);
        $("#time-current").attr("value", text);
        $("#slider_seek_Rail").attr("title", "Time " + text);
    }

    /**
        @memberOf Opencast.FromVideodisplay
        @description Set the total time of the video.
        @param String text 
    */
    function setTotalTime(text) 
    {
        $("#time-total").text(text);
    }

    /**
        @memberOf Opencast.FromVideodisplay
        @description Set the slider max time.
        @param Number time 
    */
    function setDuration(time) 
    {
        $('#slider').slider('option', 'max', time);
        Opencast.ariaSlider.getElementId(sliderSeek).setAttribute('aria-valuemax', time);
    }
    
    /**
        @memberOf Opencast.FromVideodisplay
        @description Set the with of the progress bar.
        @param Number value 
    */
    function setProgress(value) 
    {
        $('.matterhorn-progress-bar').css("width", (value + "%"));
    }

    /**
        @memberOf Opencast.FromVideodisplay
        @description Toggle the closed caption button between on or off and change the css style from the closed captions button.
        @param Boolean bool 
    */
    function setCaptionsButton(bool) 
    {
        if (bool === true)
        {
            $("#btn_cc").attr({ 
                value: ccon,
                alt: ccon,
                title: ccon
            });
            $("#btn_cc").attr("className", "oc-btn-cc-on");
            $("#btn_cc").attr('aria-pressed', 'true');
            Opencast.ToVideodisplay.setccBool(true);
        }
        else
        {
            $("#btn_cc").attr({ 
                value: ccoff,
                alt: ccoff,
                title: ccoff
            });
            $("#btn_cc").attr("className", "oc-btn-cc-off");
            $("#btn_cc").attr('aria-pressed', 'false');
            Opencast.ToVideodisplay.setccBool(false);
        }
    }
    
    /**
        @memberOf Opencast.FromVideodisplay
        @description Set the play/pause state and change the css style of the play/pause button.
        @param String state 
    */
    function setPlayPauseState(state) {
        if (state === playing) {
            $("#btn_play_pause").attr({ 
                value: play,
                alt: play,
                title: play
            });
           
            $("#btn_play_pause").attr('className', 'btn_play_out');
            $("#btn_play_pause").attr('aria-pressed', 'false');
            
            if (Opencast.mouseOverBool === true)
            {
                $("#btn_play_pause").attr("className", "btn_play_over");
            }
            Opencast.ToVideodisplay.doSetCurrentPlayPauseState(pausing);
        } 
        else {
            $("#btn_play_pause").attr({ 
                value: pause,
                alt: pause,
                title: pause
            });
           
            $("#btn_play_pause").attr("className", "btn_pause_out");
            $("#btn_play_pause").attr('aria-pressed', 'true');
            
            if (Opencast.mouseOverBool === true)
            {
                $("#btn_play_pause").attr("className", "btn_pause_over");
            }
            
            Opencast.ToVideodisplay.doSetCurrentPlayPauseState(playing);
        }
    }
    
    /**
        @memberOf Opencast.FromVideodisplay
        @description Toggle the volume between mute or unmute.
    */
    function setDoMute() {
     
        if (Videodisplay.getVolume() !== 0)
        {
            Opencast.volume = Videodisplay.getVolume();
        }
        Opencast.ToVideodisplay.doToggleVolume();
    }
    
    /**
        @memberOf Opencast.FromVideodisplay
        @description Set the captions in html
        @param Html text 
    */
    function setCaptions(text) {
        var elm = document.createElement('li');
        elm.innerHTML = text;        
        $('#captions').empty().append(elm);
    }
    
    /**
        @memberOf Opencast.FromVideodisplay
        @description Go to the function Opencast.global.addAlert(alertMessage).
        @param Html text 
    */
    function hearTimeInfo(alertMessage)
    {
        Opencast.global.addAlert(alertMessage);
    }
    
    /**
        @memberOf Opencast.FromVideodisplay
        @description Go to the function Opencast.global.toggleInfo().
    */
    function toggleInfo()
    {
        Opencast.global.toggleInfo();
     
    }
    
    return {
        setPlayhead : setPlayhead,
        setVolume : setVolume,
        setCurrentTime : setCurrentTime,
        setTotalTime: setTotalTime,
        setDuration: setDuration,
        setProgress : setProgress,
        setPlayPauseState : setPlayPauseState,
        setCaptionsButton : setCaptionsButton,
        setDoUnmute : setDoUnmute,
        setCaptions : setCaptions,
        setDoMute : setDoMute,
        hearTimeInfo : hearTimeInfo,
        toggleInfo : toggleInfo
    };
}());
