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
package org.opencast.engage.videodisplay.business
{
    import bridge.ExternalFunction;
    
    import flash.events.KeyboardEvent;
    import flash.external.ExternalInterface;
    
    import org.opencast.engage.videodisplay.control.event.ClosedCaptionsEvent;
    import org.opencast.engage.videodisplay.control.event.InitMediaPlayerEvent;
    import org.opencast.engage.videodisplay.control.event.LoadDFXPXMLEvent;
    import org.opencast.engage.videodisplay.control.event.SetVolumeEvent;
    import org.opencast.engage.videodisplay.control.event.VideoControlEvent;
    import org.opencast.engage.videodisplay.control.util.TimeCode;
    import org.opencast.engage.videodisplay.model.VideodisplayModel;
    import org.opencast.engage.videodisplay.state.CoverState;
    import org.opencast.engage.videodisplay.state.VideoSizeState;
    import org.swizframework.Swiz;

    public class FlexAjaxBridge
    {
        [Autowire]
        [Bindable]
        public var model:VideodisplayModel;
        
        private var _time:TimeCode;

        /** Constructor */
        public function FlexAjaxBridge()
        {
            Swiz.autowire( this );
        }

        /**
         * play
         *
         * When the learnder click on the play button
         * The return value is available in the calling javascript
         * */
        public function play():String
        {
            Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.PLAY ) );
            return model.currentPlayerState;
        }

        /**
         * pause
         *
         * When the learnder click on the pause button
         * The return value is available in the calling javascript
         * */
        public function pause():String
        {
            Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.PAUSE ) );
            return model.currentPlayerState;
        }

        /**
         * stop
         *
         * When the learnder click on the stop button
         * */
        public function stop():void
        {
            Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.STOP ) );
        }

        /**
         * skipBackward
         *
         * When the learnder click on the skip backward button
         * */
        public function skipBackward():void
        {
            Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SKIPBACKWARD ) );
        }

        /**
         * rewind
         *
         * When the learnder click on the rewind button
         * */
        public function rewind():void
        {
            Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.REWIND ) );
        }

        /**
         * fastForward
         *
         * When the learnder click on the fast forward button
         * */
        public function fastForward():void
        {
            Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.FASTFORWARD ) );
        }

        /**
         * skipForward
         *
         * When the learnder click on the skip forward button
         * */
        public function skipForward():void
        {
            Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SKIPFORWARD ) );
        }

        /**
         * seek
         *
         * When the learner seek the video
         * 
         * @param Number time
         * */
        public function seek( time:Number ):Number
        {
            if( model.startPlay == false )
            {
                model.startSeek = time;
                _time = new TimeCode();
                var newPositionString:String = _time.getTC( time );
                ExternalInterface.call( ExternalFunction.SETCURRENTTIME, newPositionString );
            }   
            model.mediaPlayer.seek( time );
            
            return time;
        }
        
        /**
         * mute
         *
         * Wehn the learner mutes the video
         * The return value is available in the calling javascript
         * */
        public function mute():Number
        {
            Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.MUTE ) );
            return model.playerVolume;
        }
        
        /**
         * setVolumeSlider
         *
         * Set the volume slider
         * 
         * @param Number newVolume
         * */
        public function setVolumePlayer( newVolume:Number ):void
        {
            Swiz.dispatchEvent( new SetVolumeEvent( newVolume ));
        }
        
        /**
         * setClosedCaptions
         *
         * To see the captions
         * */
        public function closedCaptions():void
        {
            Swiz.dispatchEvent( new ClosedCaptionsEvent() );
        }

        /**
         * setCaptionsURL
         *
         * Set captions URL and load the file.
         * 
         * @param String captionsURL
         */
        public function setCaptionsURL( captionsURL:String ):void
        {            
            if ( captionsURL != model.captionsURL )
            {
                model.captionsURL = captionsURL;
                Swiz.dispatchEvent( new LoadDFXPXMLEvent( captionsURL ));
            }
        }
        
        /**
         * setMediaURL
         *
         * Set media URL and init the player.
         * 
         * @param String mediaURLOne, String mediaURLTwo
         */
        public function setMediaURL(coverURLOne:String, coverURLTwo:String, mediaURLOne:String, mediaURLTwo:String, mimetypeOne:String, mimetypeTwo:String, playerMode:String ):void
        {
            Swiz.dispatchEvent( new InitMediaPlayerEvent(coverURLOne, coverURLTwo, mediaURLOne, mediaURLTwo, mimetypeOne, mimetypeTwo, playerMode ) );
        }
        
        /**
         * videoSizeControl
         *
         * When the learner press the video size control button.
         * 
         * @param Number sizeLeft, Number sizeRight
         */
        public function videoSizeControl( sizeLeft:Number, sizeRight:Number ):void
        {
            if(sizeLeft == sizeRight)
			{
			    model.videoSizeState = VideoSizeState.CENTER;
			    model.coverState = CoverState.TWOCOVERS;
			}
			else if(sizeLeft > sizeRight && sizeRight > 0)
			{
			    model.videoSizeState = VideoSizeState.BIGLEFT;
			    model.coverState = CoverState.TWOCOVERS;
			}
			else if(sizeLeft < sizeRight && sizeLeft > 0)
            {
                model.videoSizeState = VideoSizeState.BIGRIGHT;
                model.coverState = CoverState.TWOCOVERS;
            }
            else if(sizeRight == 0)
            {
                model.videoSizeState = VideoSizeState.ONLYLEFT;
                model.coverState = CoverState.ONECOVER;
                if( model.coverURLTwo == '' )
                {
                    model.coverURLTwo == model.coverURLOne;
                }
                model.coverURLSingle = model.coverURLOne;
            }
            else if(sizeLeft == 0)
            {
                model.videoSizeState = VideoSizeState.ONLYRIGHT;
                model.coverState = CoverState.ONECOVER;
                model.coverURLSingle = model.coverURLTwo;
            }
		}
		
		/**
         * setMediaResolution
         *
         * Set the resolution
         */
        public function setMediaResolution(newWidthMediaOne:Number, newHeightMediaOne:Number, newWidthMediaTwo:Number, newHeightMediaTwo:Number, multiMediaContainerLeft:Number):void
        {
            if(newWidthMediaOne == 0 && newHeightMediaOne == 0 && newWidthMediaTwo == 0 && newHeightMediaTwo == 0 && multiMediaContainerLeft == 0 )
            {
	             model.previewPlayer = true;
            }
            else
            {
                model.mediaOneWidth = parseInt( newWidthMediaOne.toString() );
			    model.mediaOneHeight = parseInt( newHeightMediaOne.toString() );
			    model.mediaTwoWidth = parseInt( newWidthMediaTwo.toString() );
			    model.mediaTwoHeight = parseInt( newHeightMediaTwo.toString() );
			    model.mediaWidth = parseInt( ( newWidthMediaOne + newWidthMediaTwo ).toString() );
			    model.multiMediaContainerLeft = parseInt( multiMediaContainerLeft.toString() );
			    model.multiMediaContainerRight = 0;
			    model.formatMediaOne = model.mediaOneWidth / model.mediaOneHeight;
			    model.formatMediaTwo = model.mediaTwoWidth / model.mediaTwoHeight;
			   
                if(model.videoSizeState == VideoSizeState.ONLYLEFT || model.videoSizeState == VideoSizeState.BIGLEFT )
			    {
                    model.multiMediaContainerRight = multiMediaContainerLeft;
			        model.multiMediaContainerLeft = 0;
			    }	
            }
        }
		
        /**
         * getViewState
         *
         * The return value is available in the calling javascript.
         */
        public function getViewState():String
        {
        	return model.mediaState;
        }
        
        /**
         * getViewState
         *
         * The return value is available in the calling javascript.
         */
        public function getMediaHeight():Number
        {
          return model.mediaContainer.measuredHeight;
        }
        
        /**
         * reportKeyDown
         *
         * @eventType event:KeyboardEvent
         */
        public function reportKeyUp( event:KeyboardEvent ):void
        {
            if( event.altKey && event.ctrlKey )
            {
                passCharCode( event.keyCode );
            }
        }

        /**
         * passCharCode
         *
         * When the learner press any key for the mediaplayer
         * 
         * @param int charCode
         */
        public function passCharCode( charCode:int ):void
        {
            // Play or pause the video
            if( charCode == 80 || charCode == 112 ) // P or p
            {
                if( model.mediaPlayer.playing() )
                {
                    Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.PAUSE ) );
                }
                else
                {
                    Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.PLAY ) );
                }
            }

            // Mute the video
            if( charCode == 83 || charCode == 115 ) // S or s
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.STOP ) );
            }

            // Mute the video
            if( charCode == 77 || charCode == 109 ) // M or m
            {
               ExternalInterface.call( ExternalFunction.MUTE, '' )
            }

            // Volume up
            if ( charCode == 85 || charCode == 117 ) // U or u
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.VOLUMEUP ) );
            }

            // Volume down
            if( charCode == 68 || charCode == 100 ) // D or d
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.VOLUMEDOWN ) );
            }

            // Seek 0
            if( charCode == 48 ) // 0
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKZERO ) );
            }

            // Seek 1
            if( charCode == 49 ) // 1
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKONE ) );
            }

            // Seek 2
            if( charCode == 50 ) // 2
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKTWO ) );
            }

            // Seek 3
            if( charCode == 51 ) // 3
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKTHREE ) );
            }

            // Seek 4
            if( charCode == 52 ) // 4
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKFOUR ) );
            }

            // Seek 5
            if( charCode == 53 ) // 5
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKFIVE ) );
            }

            // Seek 6
            if( charCode == 54 ) // 6
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKSIX ) );
            }

            // Seek 7
            if( charCode == 55 ) // 7
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKSEVEN ) );
            }

            // Seek 8
            if( charCode == 56 ) // 8
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKEIGHT ) );
            }

            // Seek 9
            if( charCode == 57 ) // 9
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKNINE ) );
            }

            // Closed Caption
            if( charCode == 67 || charCode == 99 ) // C or c
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.CLOSEDCAPTIONS ) );
            }

            // rewind
            if( charCode == 82 || charCode == 114 ) // R or r
            {
                var playRewind:Boolean = model.mediaPlayer.playing();
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.REWIND ) );
                if(playRewind)
                {
                    model.mediaPlayer.play();
                }
            }

            // Fast forward
            if( charCode == 70 || charCode == 102 ) // F or f
            {
                var playForward:Boolean = model.mediaPlayer.playing();
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.FASTFORWARD ) );
                if(playForward)
                {
                    model.mediaPlayer.play();
                }
            }

            // time
            if( charCode == 84 || charCode == 116 ) // T or t
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.HEARTIMEINFO ) );
            }

            // Information
            if( charCode == 73 || charCode == 105 ) // I or i
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SHORTCUTS ) );
            }
        }
        
        /**
         * onBridgeReady
         *
         * When the birdge ist ready
         */
        public function onBridgeReady():void
        {
            ExternalInterface.call( ExternalFunction.ONPLAYERREADY );
        }
    }
}