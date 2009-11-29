/*global $, Videodisplay, Opencast*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */


/**
    @namespace the global Opencast namespace FromVideodisplay
*/
Opencast.FromVideodisplay = (function () {

    var playing = "playing",
        pausing = "pausing",
        play    = "Play",
        pause   = "Pause",
        unmute  = "Unmute",
        mute    = "Mute",
        ccon    = "Closed Caption On",
        ccoff   = "Closed Caption Off";
    
    /**
        @memberOf Opencast.FromVideodisplay
        @description Set the new position of the seek slider.
        @param number newPosition 
    */
    function setPlayhead(newPosition) {
        $('#slider').slider('value', newPosition);
    }

    /**
        @memberOf Opencast.FromVideodisplay
        @description Set the new position of the volume slider.
        @param number newVolume 
    */
    function setVolume(newVolume) {
        $('#volume_slider').slider('value', newVolume);
        
        if (newVolume !== 0)
        {
            Opencast.FromVideodisplay.setDoUnmute();
        }
    }

    /**
        @memberOf Opencast.FromVideodisplay
        @description Set the current time of the video.
        @param string text 
    */
    function setCurrentTime(text) {
        $("#time-current").text(text);
    }

    /**
        @memberOf Opencast.FromVideodisplay
        @description Set the total time of the video.
        @param string text 
    */
    function setTotalTime(text) {
        $("#time-total").text(text);
    }

    /**
        @memberOf Opencast.FromVideodisplay
        @description Set the slider max time.
        @param number time 
    */
    function setDuration(time) {
        $('#slider').slider('option', 'max', time);
    }
    
    /**
        @memberOf Opencast.FromVideodisplay
        @description Set the with of the progress bar.
        @param number value 
    */
    function setProgress(value) {
        $('.matterhorn-progress-bar').css("width", (value + "%"));
    }

    /**
        @memberOf Opencast.FromVideodisplay
        @description Toggle the cc button between on or off.
        @param boolean bool 
    */
    function setCaptionsButton(bool) {
        if (bool === true)
        {
            $("#btn_cc").attr({ 
                value: ccoff,
                alt: ccoff,
                title: ccoff
            });
            $("#btn_cc").attr("className", "oc-btn-cc-on");
        }
        else
        {
            $("#btn_cc").attr({ 
                value: ccon,
                alt: ccon,
                title: ccon
            });
            $("#btn_cc").attr("className", "oc-btn-cc-off");
        }
    }
    
    /**
        @memberOf Opencast.FromVideodisplay
        @description Toogle between play and pause.
        @param sting state 
    */
    function setPlayPauseState(state) {
        if (state === playing) {
            $("#btn_play_pause").attr({ 
                value: play,
                alt: play,
                title: play
            });
           
            $("#btn_play_pause").attr("className", "btn_play_out");
            
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
            
            if (Opencast.mouseOverBool === true)
            {
                $("#btn_play_pause").attr("className", "btn_pause_over");
            }
            
            Opencast.ToVideodisplay.doSetCurrentPlayPauseState(playing);
        }
    }
    
    /**
        @memberOf Opencast.FromVideodisplay
        @description Mute the player.
    */
    function setDoMute() {
        $("#btn_volume").attr({ 
            value: unmute,
            alt: unmute,
            title: unmute
        });
        $("#btn_volume").attr("className", "oc-btn-volume-mute");
    }
    
    
        /**
           @memberOf Opencast.FromVideodisplay
           @description Unmute the player.
        */
    function setDoUnmute() {
        $("#btn_volume").attr({ 
                value: mute,
                alt: mute,
                title: mute
            });
    }
    
    function setCaptions(text) {
        var elm = document.createElement('li');
        elm.innerHTML = text;        
        $('#captions').empty().append(elm);
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
        setDoMute : setDoMute,
        setDoUnmute : setDoUnmute,
        setCaptions : setCaptions
    };
}());
