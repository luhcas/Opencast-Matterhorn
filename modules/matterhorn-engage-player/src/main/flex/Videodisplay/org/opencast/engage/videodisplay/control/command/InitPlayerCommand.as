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
    
    import flash.external.ExternalInterface;
    
    import mx.controls.Alert;
    import mx.core.Application;
    
    import org.opencast.engage.videodisplay.control.event.DisplayCaptionEvent;
    import org.opencast.engage.videodisplay.control.event.InitPlayerEvent;
    import org.opencast.engage.videodisplay.control.event.VideoControlEvent;
    import org.opencast.engage.videodisplay.control.util.TimeCode;
    import org.opencast.engage.videodisplay.model.VideodisplayModel;
    import org.opencast.engage.videodisplay.state.MediaState;
    import org.opencast.engage.videodisplay.state.PlayerState;
    import org.osmf.display.ScaleMode;
    import org.osmf.elements.AudioElement;
    import org.osmf.elements.VideoElement;
    import org.osmf.events.AudioEvent;
    import org.osmf.events.BufferEvent;
    import org.osmf.events.LoadEvent;
    import org.osmf.events.MediaErrorEvent;
    import org.osmf.events.TimeEvent;
    import org.osmf.layout.HorizontalAlign;
    import org.osmf.layout.LayoutMetadata;
    import org.osmf.layout.VerticalAlign;
    import org.osmf.media.MediaElement;
    import org.osmf.media.URLResource;
    import org.swizframework.Swiz;

    public class InitPlayerCommand
    {
        [Autowire]
        public var model:VideodisplayModel;

       
        private var mediaElement:MediaElement = new MediaElement();
        private var _time:TimeCode;
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

			if( model.mediaURLOne != '' )
            {
                var pos:int = model.mediaURLOne.lastIndexOf( "." );
                var fileType:String = model.mediaURLOne.substring( pos + 1 );

                switch ( fileType )
                {
                    case "flv":
                    case "mp4":
                    	mediaElement =  new VideoElement ( new URLResource( model.mediaURLOne ) );
                    	setMediaElement( mediaElement );
                        model.mediaState = MediaState.VIDEO;
                        break;

                    case "mp3":
                        mediaElement = new AudioElement( new URLResource( model.mediaURLOne ) );
                        setMediaElement( mediaElement );
                        model.mediaState = MediaState.AUDIO;
                        var position:int = model.mediaURLOne.lastIndexOf( '/' );
                        model.audioURL = model.mediaURLOne.substring( position + 1 );
                        Application.application.bx_audio.startVisualization();
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

            // When the flash vars autoplay ist not undifined
            if ( Application.application.parameters.autoplay != undefined )
            {
                if ( Application.application.parameters.autoplay == "true" )
                {
                    model.mediaPlayerSingle.autoPlay = true;
                    model.currentPlayerState = PlayerState.PLAYING;
                    ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, PlayerState.PAUSING );
                }
                else
                {
                    model.mediaPlayerSingle.autoPlay = true;
                    model.mediaPlayerSingle.volume = 1.0;
                 }
            }
            else
            {
                model.mediaPlayerSingle.autoPlay = true;
                model.mediaPlayerSingle.volume = 1.0;
            }
            
            // Set up the MediaPlayer.
            model.mediaPlayerSingle.addEventListener( TimeEvent.DURATION_CHANGE, onDurationChange);
            model.mediaPlayerSingle.addEventListener( AudioEvent.MUTED_CHANGE, muteChange );
			model.mediaPlayerSingle.addEventListener( AudioEvent.VOLUME_CHANGE, volumeChange );
            model.mediaPlayerSingle.addEventListener( TimeEvent.CURRENT_TIME_CHANGE, onCurrentTimeChange );
            model.mediaPlayerSingle.addEventListener( TimeEvent.DURATION_CHANGE, onDurationReached);
            model.mediaPlayerSingle.addEventListener( MediaErrorEvent.MEDIA_ERROR, onMediaError);
            
            model.mediaPlayerSingle.addEventListener( LoadEvent.BYTES_TOTAL_CHANGE, onBytesTotalChange );
			model.mediaPlayerSingle.addEventListener( LoadEvent.BYTES_LOADED_CHANGE, onBytesLoadedChange);
			
			model.mediaPlayerSingle.addEventListener( BufferEvent.BUFFERING_CHANGE, onBufferingChange);
		}
        
        /**
         * setMediaElement
         * 
         * Set the media element.
         *
         * @eventType event:MediaElement
         *
         * */
        private function setMediaElement(value:MediaElement):void
		{
			if (value != null)
			{
				// If there's no explicit layout metadata, center the content. 
				var layoutMetadata:LayoutMetadata = value.getMetadata(LayoutMetadata.LAYOUT_NAMESPACE) as LayoutMetadata;
				if (layoutMetadata == null)
				{
					layoutMetadata = new LayoutMetadata();
					layoutMetadata.scaleMode = ScaleMode.LETTERBOX;
					layoutMetadata.horizontalAlign = HorizontalAlign.CENTER;
					layoutMetadata.verticalAlign = VerticalAlign.BOTTOM;
					layoutMetadata.percentHeight = 100;
					layoutMetadata.percentWidth = 100;
					value.addMetadata(LayoutMetadata.LAYOUT_NAMESPACE, layoutMetadata);
				}
				model.containerSingle.addMediaElement(value);
			}
			model.mediaPlayerSingle.media = value;
			ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, 100 );
		}
        
        /**
         * onDurationChange
         *
         * @eventType event:TimeEvent
         *
         * */
        private function onDurationChange(event:TimeEvent):void
		{
			// Store new duration as current duration in the videodisplay model
            model.currentDuration = event.time;
            currentDurationString = _time.getTC( event.time );
            ExternalInterface.call( ExternalFunction.SETDURATION, event.time );
            ExternalInterface.call( ExternalFunction.SETTOTALTIME, currentDurationString );
            autoPlayOff();
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
            model.mediaPlayerSingle.volume = 1.0;
        }
        
        /**
         * volumeChange
         *
         * When the volume is change in the video
         * 
         * @eventType event:AudioEvent
         *
         * */
        private function volumeChange( event:AudioEvent ):void
        {
           if( model.mediaPlayerSingle.muted == true )
           {
                model.mediaPlayerSingle.muted = false;
           }
           if( model.mediaPlayerSingle.volume > 0.50 )
           {
            	ExternalInterface.call( ExternalFunction.HIGHSOUND, '' );
           }
           
           if( model.mediaPlayerSingle.volume <= 0.50 )
           {
                ExternalInterface.call( ExternalFunction.LOWSOUND, '' );
           }
           
           if( model.mediaPlayerSingle.volume == 0 )
           {
                ExternalInterface.call( ExternalFunction.NONESOUND, '' );
           }
           
           if( model.ccButtonBoolean == false && model.ccBoolean == true )
           {
                model.ccBoolean = false;
                ExternalInterface.call( ExternalFunction.SETCCICONOFF, '' );
           }
        }
        
        /**
         * muteChange
         *
         * When the player is mute or unmute
         * 
         * @eventType event:AudioEvent
         *
         * */
        private function muteChange( event:AudioEvent ):void
        {
           	if( event.muted )
           	{
           		ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, 0 );
           		model.playerVolume = 0;
           		ExternalInterface.call( ExternalFunction.MUTESOUND, '' );
           		if( model.ccButtonBoolean == false )
           		{
           			model.ccBoolean = true;
                    ExternalInterface.call( ExternalFunction.SETCCICONON, '' );
           		}
           		
           	}
           	else
           	{
           		ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, model.mediaPlayerSingle.volume * 100 );	
           		model.playerVolume = model.mediaPlayerSingle.volume;
           		
           		if( model.mediaPlayerSingle.volume > 0.50 )
                {
                    ExternalInterface.call( ExternalFunction.HIGHSOUND, '' );
                }
           
                if( model.mediaPlayerSingle.volume <= 0.50 )
                {
                    ExternalInterface.call( ExternalFunction.LOWSOUND, '' );
                }
           
                if( model.mediaPlayerSingle.volume == 0 )
                {
                    ExternalInterface.call( ExternalFunction.NONESOUND, '' );
                }
                
                if( model.ccButtonBoolean == false )
                {
                    model.ccBoolean = false;
                    ExternalInterface.call( ExternalFunction.SETCCICONOFF, '' );
                }
                
           	}
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
                ExternalInterface.call( ExternalFunction.SETCURRENTTIME, newPositionString );
                lastNewPositionString = newPositionString;
            }

            if ( !model.mediaPlayerSingle.seeking )
            {
                ExternalInterface.call( ExternalFunction.SETPLAYHEAD, event.time );
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
            model.mediaPlayerSingle.pause();
            model.currentPlayerState = PlayerState.PAUSING;
            ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, PlayerState.PLAYING );
        }
        
        /**
         * onMediaError
         *
         * When the media file ist nocht available.
         * 
         * @eventType event:MediaErrorEvent
         *
         * */
        private function onMediaError( event:MediaErrorEvent ):void
        {
            model.error = new Error();
            model.error.name = event.error.message;
            model.error.message = event.error.detail;
        }

        /**
         * errorMessage
         *
         * Set the error Message and switch the stage.
         * 
         * 
         *
         * */
        private function errorMessage( name:String, message:String ):void
        {
            model.error = new Error();
            model.error.name = name;
            model.error.message = message;
            model.mediaState = MediaState.ERROR;
        }
        
        
        /**
         * onBytesTotalChange
         *
         * Save the total bytes of the video
         * 
         * @eventType event:LoadEvent
         *
         * */
        private function onBytesTotalChange( event:LoadEvent ):void
		{
			model.bytesTotal = event.bytes;
		}
		
		/**
         * onBytesLoadedChange
         *
         * Set the progress bar.
         * 
         * @eventType event:LoadEvent
         *
         * */
		private function onBytesLoadedChange( event:LoadEvent ):void
		{
			var progress:Number = 0;
            
            try
            {
            	progress = Math.round( event.bytes / model.bytesTotal * 100 );
                ExternalInterface.call( ExternalFunction.SETPROGRESS, progress );
                model.progressBar.setProgress( progress, 100 );
            }
            catch ( e:TypeError )
            {
                // ignore
            }
        }
        
        /**
         * onBufferingChange
         *
		 * @eventType event:BufferEvent
         *
         * */
        private function onBufferingChange( event:BufferEvent ):void
		{
			
		}
    }
}