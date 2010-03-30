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
    import org.opencast.engage.videodisplay.control.event.InitMultiPlayerEvent;
    import org.opencast.engage.videodisplay.control.event.VideoControlEvent;
    import org.opencast.engage.videodisplay.control.util.TimeCode;
    import org.opencast.engage.videodisplay.model.VideodisplayModel;
    import org.opencast.engage.videodisplay.state.MediaState;
    import org.opencast.engage.videodisplay.state.PlayerState;
    import org.osmf.display.ScaleMode;
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

    public class InitMultiPlayerCommand
    {
        [Autowire]
        public var model:VideodisplayModel;

       
        private var mediaElementOne:MediaElement = new MediaElement();
        private var mediaElementTwo:MediaElement = new MediaElement();
        private var _time:TimeCode;
        private var currentDurationString:String = "00:00:00";
        private var lastNewPositionString:String = "00:00:00";

        /** Constructor */
        public function InitMultiPlayerCommand()
        {
            Swiz.autowire( this );
        }

        /** execute
         *
         * init the video player.
         *
         * @eventType event:InitPlayerEvent
         * */
        public function execute( event:InitMultiPlayerEvent ):void
        {
			 _time = new TimeCode();            

			if( model.mediaURLOne != '' && model.mediaURLTwo != '' )
            {
            	mediaElementOne =  new VideoElement ( new URLResource( model.mediaURLOne ) );
            	setMediaElementOne( mediaElementOne );
            	mediaElementTwo =  new VideoElement ( new URLResource( model.mediaURLTwo ) );
            	setMediaElementTwo( mediaElementTwo );
            }
            else
            {
                errorMessage( "Error", "TRACKS COULD NOT BE FOUND" );
            }

            // When the flash vars autoplay ist not undifined
            if ( Application.application.parameters.autoplay != undefined )
            {
                if ( Application.application.parameters.autoplay == "true" )
                {
                    model.mediaPlayerOne.autoPlay = true;
                    model.mediaPlayerTwo.autoPlay = true;
                    model.currentPlayerState = PlayerState.PLAYING;
                    ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, PlayerState.PAUSING );
                }
                else
                {
                    model.mediaPlayerOne.autoPlay = true;
                    model.mediaPlayerTwo.autoPlay = true;
                    model.mediaPlayerOne.volume = 1.0;
                    model.mediaPlayerTwo.volume = 0;
                   
                }
            }
            else
            {
                model.mediaPlayerOne.autoPlay = true;
                model.mediaPlayerTwo.autoPlay = true;
                model.mediaPlayerOne.volume = 1.0;
                model.mediaPlayerTwo.volume = 0;
              
            }
            
            // Set up the MediaPlayer.
            model.mediaPlayerOne.addEventListener( TimeEvent.DURATION_CHANGE, onDurationChange);
            model.mediaPlayerOne.addEventListener( AudioEvent.MUTED_CHANGE, muteChange );
			model.mediaPlayerOne.addEventListener( AudioEvent.VOLUME_CHANGE, volumeChange );
            model.mediaPlayerOne.addEventListener( TimeEvent.CURRENT_TIME_CHANGE, onCurrentTimeChange );
            model.mediaPlayerOne.addEventListener( TimeEvent.DURATION_CHANGE, onDurationReached);
            model.mediaPlayerOne.addEventListener( MediaErrorEvent.MEDIA_ERROR, onMediaError);
            
            model.mediaPlayerOne.addEventListener( BufferEvent.BUFFERING_CHANGE, onBufferingChange);
		}
        
        /**
         * setMediaElementOne
         * 
         * Set the media element.
         *
         * @eventType event:MediaElement
         *
         * */
        private function setMediaElementOne(value:MediaElement):void
		{
			if (value != null)
			{
				// If there's no explicit layout metadata, center the content. 
				model.layoutMetadataOne = value.getMetadata(LayoutMetadata.LAYOUT_NAMESPACE) as LayoutMetadata;
				if (model.layoutMetadataOne == null)
				{
					model.layoutMetadataOne = new LayoutMetadata();
					model.layoutMetadataOne.scaleMode = ScaleMode.LETTERBOX;
					model.layoutMetadataOne.horizontalAlign = HorizontalAlign.CENTER;
					model.layoutMetadataOne.verticalAlign = VerticalAlign.MIDDLE;
					model.layoutMetadataOne.percentHeight = 100;
					model.layoutMetadataOne.percentWidth = 100;
					value.addMetadata(LayoutMetadata.LAYOUT_NAMESPACE, model.layoutMetadataOne);
				}
				model.containerOne.addMediaElement(value);
			}
			model.mediaPlayerOne.media = value;
			ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, 100 );
		}
		
		/**
         * setMediaElementTwo
         * 
         * Set the media element.
         *
         * @eventType event:MediaElement
         *
         * */
        private function setMediaElementTwo(value:MediaElement):void
		{
			if (value != null)
			{
				// If there's no explicit layout metadata, center the content. 
				model.layoutMetadataTwo = value.getMetadata(LayoutMetadata.LAYOUT_NAMESPACE) as LayoutMetadata;
				if (model.layoutMetadataTwo == null)
				{
					model.layoutMetadataTwo = new LayoutMetadata();
					model.layoutMetadataTwo.scaleMode = ScaleMode.LETTERBOX;
					model.layoutMetadataTwo.horizontalAlign = HorizontalAlign.CENTER;
					model.layoutMetadataTwo.verticalAlign = VerticalAlign.MIDDLE;
					model.layoutMetadataTwo.percentHeight = 100;
					model.layoutMetadataTwo.percentWidth = 100;
					value.addMetadata(LayoutMetadata.LAYOUT_NAMESPACE, model.layoutMetadataTwo);
				}
				
				model.containerTwo.addMediaElement(value);
			}
			model.mediaPlayerTwo.media = value;
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
            model.mediaPlayerOne.volume = 1.0;
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
           if( model.mediaPlayerOne.muted == true )
           {
                model.mediaPlayerOne.muted = false;
           }
           if( model.mediaPlayerOne.volume > 0.50 )
           {
            	ExternalInterface.call( ExternalFunction.HIGHSOUND, '' );
           }
           
           if( model.mediaPlayerOne.volume <= 0.50 )
           {
                ExternalInterface.call( ExternalFunction.LOWSOUND, '' );
           }
           
           if( model.mediaPlayerOne.volume == 0 )
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
           		ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, model.mediaPlayerOne.volume * 100 );	
           		model.playerVolume = model.mediaPlayerOne.volume;
           		
           		if( model.mediaPlayerOne.volume > 0.50 )
                {
                    ExternalInterface.call( ExternalFunction.HIGHSOUND, '' );
                }
           
                if( model.mediaPlayerOne.volume <= 0.50 )
                {
                    ExternalInterface.call( ExternalFunction.LOWSOUND, '' );
                }
           
                if( model.mediaPlayerOne.volume == 0 )
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

            if ( !model.mediaPlayerOne.seeking )
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
            model.mediaPlayerOne.pause();
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