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
package org.opencast.engage.videodisplay.control.command
{
    import bridge.ExternalFunction;

    import flash.events.TimerEvent;
    import flash.external.ExternalInterface;
    import flash.utils.Timer;

    import mx.core.Application;

    import org.opencast.engage.videodisplay.control.event.DisplayCaptionEvent;
    import org.opencast.engage.videodisplay.control.event.InitPlayerEvent;
    import org.opencast.engage.videodisplay.control.event.VideoControlEvent;
    import org.opencast.engage.videodisplay.control.util.TimeCode;
    import org.opencast.engage.videodisplay.model.VideodisplayModel;
    import org.opencast.engage.videodisplay.state.MediaState;
    import org.opencast.engage.videodisplay.state.PlayerState;
    import org.osmf.audio.AudioElement;
    import org.osmf.audio.SoundLoader;
    import org.osmf.display.ScaleMode;
    import org.osmf.events.AudioEvent;
    import org.osmf.events.MediaErrorEvent;
    import org.osmf.events.TimeEvent;
    import org.osmf.media.MediaElement;
    import org.osmf.media.URLResource;
    import org.osmf.net.NetLoadedContext;
    import org.osmf.net.NetLoader;
    import org.osmf.traits.LoadTrait;
    import org.osmf.traits.MediaTraitType;
    import org.osmf.utils.FMSURL;
    import org.osmf.utils.URL;
    import org.osmf.video.VideoElement;
    import org.swizframework.Swiz;

    public class InitPlayerCommand
    {
        [Autowire]
        public var model:VideodisplayModel;

        private var bytesLoadedTimer:Timer;
        private var mediaElement:MediaElement = new MediaElement();
        private var _time:TimeCode;
        private const TIMER_INTERVAL_BYTES_LOADED:int = 250;
        private var currentDurationString:String = "00:00:00";
        private var lastNewPositionString:String = "00:00:00";

        /** Constructor */
        public function InitPlayerCommand()
        {
            Swiz.autowire( this );
        }

        /** execute
         *
         * init the video player.
         *
         * @eventType event:InitPlayerEvent
         * */
        public function execute( event:InitPlayerEvent ):void
        {
            _time = new TimeCode();

            bytesLoadedTimer = new Timer( TIMER_INTERVAL_BYTES_LOADED );
            bytesLoadedTimer.addEventListener( TimerEvent.TIMER, progress );
            bytesLoadedTimer.start();

            var stream:String = "rtmp://freecom.serv.uni-osnabrueck.de/oflaDemo/algorithmen08_2008_11_11_13_59__131_173_10_32.flv";

            if( model.mediaURL != '' )
            {
                var pos:int = model.mediaURL.lastIndexOf( "." );
                var fileType:String = model.mediaURL.substring( pos + 1 );

                switch ( fileType )
                {
                    case "flv":
                        mediaElement = new VideoElement( new NetLoader(), new URLResource( new URL( model.mediaURL ) ) );
                        ExternalInterface.call( ExternalFunction.SETVOLUME, 100, model.playerId );
                        model.mediaState = MediaState.VIDEO;
                        break;

                    case "mp4":
                        mediaElement = new VideoElement( new NetLoader(), new URLResource( new URL( model.mediaURL ) ) );
                        ExternalInterface.call( ExternalFunction.SETVOLUME, 100, model.playerId );
                        model.mediaState = MediaState.VIDEO;
                        break;

                    case "mp3":
                        mediaElement = new AudioElement( new SoundLoader(), new URLResource( new URL( model.mediaURL ) ) );
                        ExternalInterface.call( ExternalFunction.SETVOLUME, 100, model.playerId );
                        model.mediaState = MediaState.AUDIO;
                        var position:int = model.mediaURL.lastIndexOf( '/' );
                        model.audioURL = model.mediaURL.substring( position + 1 );
                        break;

                    default:
                        errorMessage( "Error", "TRACK COULD NOT BE FOUND" );
                        break;
                }
            }
            else
            {
                errorMessage( "Error", "TRACK COULD NOT BE FOUND" );
            }

            if ( Application.application.parameters.playerId != undefined )
            {
                model.playerId = Application.application.parameters.playerId;
            }

            // When the flash vars autoplay ist not undifined
            if ( Application.application.parameters.autoplay != undefined )
            {
                if ( Application.application.parameters.autoplay == "true" )
                {
                    model.player.autoPlay = true;
                    model.currentPlayerState = PlayerState.PLAYING;
                    ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, PlayerState.PAUSING );
                }
                else
                {
                    model.player.autoPlay = true;
                    model.player.volume = 0.0;
                    model.mediaPlayerWrapper.visible = false;
                }
            }
            else
            {
                model.player.autoPlay = true;
                model.player.volume = 0.0;
                model.mediaPlayerWrapper.visible = false;
            }

            // Set up the MediaPlayer.
            model.player.autoRewind = true;

            model.mediaPlayerWrapper.scaleMode = ScaleMode.LETTERBOX;
            model.mediaPlayerWrapper.addEventListener( TimeEvent.DURATION_CHANGE, onDurationChange );
            model.mediaPlayerWrapper.addEventListener( AudioEvent.VOLUME_CHANGE, volumeChange );
            model.mediaPlayerWrapper.addEventListener( TimeEvent.CURRENT_TIME_CHANGE, onCurrentTimeChange );
            model.mediaPlayerWrapper.addEventListener( TimeEvent.DURATION_REACHED, onDurationReached );
            model.mediaPlayerWrapper.addEventListener( MediaErrorEvent.MEDIA_ERROR, onMediaError );

            model.mediaPlayerWrapper.element = mediaElement;
        }

        /**
         * progress
         *
         * @eventType event:TimerEvent
         *
         * */
        private function progress( event:TimerEvent ):void
        {
            if ( model.mediaState == MediaState.VIDEO )
            {
                var loadableVideo:LoadTrait = mediaElement.getTrait( MediaTraitType.LOAD ) as LoadTrait;

                if ( loadableVideo )
                {
                    var context:NetLoadedContext = NetLoadedContext( loadableVideo.loadedContext );
                    var progressVideo:Number = 0;

                    try
                    {
                        progressVideo = Math.round( context.stream.bytesLoaded / context.stream.bytesTotal * 100 );
                        ExternalInterface.call( ExternalFunction.SETPROGRESS, progressVideo, model.playerId );
                        model.progressBar.setProgress( progressVideo, 100 );
                    }
                    catch ( e:TypeError )
                    {
                        // ignore
                    }

                    if ( progressVideo >= 100 )
                    {
                        bytesLoadedTimer.stop();
                    }
                }
            }
            else
            {
                var loadableAudio:LoadTrait = mediaElement.getTrait( MediaTraitType.LOAD ) as LoadTrait;

                if ( loadableAudio )
                {
                    var audioProgress:Number = 0;

                    try
                    {
                        audioProgress = Math.round( loadableAudio.bytesLoaded / loadableAudio.bytesTotal * 100 );
                        ExternalInterface.call( ExternalFunction.SETPROGRESS, audioProgress, model.playerId );
                        model.progressBar.setProgress( audioProgress, 100 );
                    }
                    catch ( e:TypeError )
                    {
                        // ignore
                    }

                    if ( audioProgress >= 100 )
                    {
                        bytesLoadedTimer.stop();

                    }
                }
            }
        }

        /**
         * autoPlayOff
         *
         * Stop the video at the beginning.
         *
         * */
        private function autoPlayOff():void
        {
            Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.STOP ) );
            model.mediaPlayerWrapper.mediaPlayer.volume = 1.0;
            model.mediaPlayerWrapper.visible = true;
        }

        /**
         * onDurationChange
         *
         * @eventType event:TimeEvent
         *
         * */
        private function onDurationChange( event:TimeEvent ):void
        {
            // Store new duration as current duration in the videodisplay model
            model.currentDuration = event.time;
            currentDurationString = _time.getTC( model.currentDuration );
            ExternalInterface.call( ExternalFunction.SETDURATION, event.time, model.playerId );
            ExternalInterface.call( ExternalFunction.SETTOTALTIME, currentDurationString, model.playerId );
            autoPlayOff();
        }

        /**
         * volumeChange
         *
         * When the volume is change in the video
         *
         * */
        private function volumeChange( event:AudioEvent ):void
        {
            ExternalInterface.call( ExternalFunction.SETVOLUME, event.volume * 100, model.playerId );
            model.videoVolume = event.volume;
        }

        /**
         * onCurrentTimeChange
         *
         * @eventType event:TimeEvent
         *
         * */
        private function onCurrentTimeChange( event:TimeEvent ):void
        {
            var newPositionString:String = _time.getTC( event.time );

            if ( newPositionString != lastNewPositionString )
            {
                ExternalInterface.call( ExternalFunction.SETCURRENTTIME, newPositionString, model.playerId );
                lastNewPositionString = newPositionString;
            }

            if ( !model.player.seeking )
            {
                ExternalInterface.call( ExternalFunction.SETPLAYHEAD, event.time, model.playerId );
            }
            
            if ( model.captionsURL != null )
            {
                Swiz.dispatchEvent( new DisplayCaptionEvent( event.time ) );
            }

            model.currentPlayhead = event.time;
        }

        /**
         * onDurationReached
         *
         * @eventType event:TimeEvent
         *
         * */

        private function onDurationReached( event:TimeEvent ):void
        {
            model.player.pause();
            model.currentPlayerState = PlayerState.PAUSING;
            ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, PlayerState.PLAYING );
        }

        /**
         * onMediaError
         *
         * When the media file ist nocht available.
         *
         * */
        private function onMediaError( event:MediaErrorEvent ):void
        {
            model.error = new Error();
            model.error.name = event.error.description;
            model.error.message = event.error.detail;
        }

        /**
         * errorMessage
         *
         * Set the error Message and switch the stage.
         *
         * */
        private function errorMessage( name:String, message:String ):void
        {
            model.error = new Error();
            model.error.name = name;
            model.error.message = message;
            model.mediaState = MediaState.ERROR;
        }
    }
}