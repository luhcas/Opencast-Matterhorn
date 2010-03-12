package org.opencast.engage.videodisplay.business
{
    import bridge.ExternalFunction;
    
    import flash.events.KeyboardEvent;
    import flash.external.ExternalInterface;
    
    import org.opencast.engage.videodisplay.control.event.ClosedCaptionsEvent;
    import org.opencast.engage.videodisplay.control.event.InitPlayerEvent;
    import org.opencast.engage.videodisplay.control.event.LoadDFXPXMLEvent;
    import org.opencast.engage.videodisplay.control.event.SetCurrentCaptionsEvent;
    import org.opencast.engage.videodisplay.control.event.SetVolumeEvent;
    import org.opencast.engage.videodisplay.control.event.VideoControlEvent;
    import org.opencast.engage.videodisplay.model.VideodisplayModel;
    import org.swizframework.Swiz;

    public class FlexAjaxBridge
    {
        [Autowire]
        [Bindable]
        public var model:VideodisplayModel;
        
        public var pressedKey:String = '';

        /** Constructor */
        public function FlexAjaxBridge()
        {
            Swiz.autowire( this );
        }

        /**
         * play
         *
         * When the learnder click on the play button
         * */
        public function play():void
        {
            Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.PLAY ) );
        }

        /**
         * pause
         *
         * When the learnder click on the pause button
         * */
        public function pause():void
        {
            Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.PAUSE ) );
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
         * Wehn the learner seek the video
         * */
        public function seek( time:Number ):void
        {
            model.player.seek( time );
        }
        
        /**
         * setLanguage
         *
         * Set the language of the captions
         * */
        public function setLanguage( language:String ):void
        {
            Swiz.dispatchEvent( new SetCurrentCaptionsEvent( language ) );
        }

        /**
         * setClosedCaptions
         *
         * To see the captions
         * */
        public function closedCaptions( bool:Boolean ):void
        {
            Swiz.dispatchEvent( new ClosedCaptionsEvent( bool ) );
        }

        /**
         * setVolume
         *
         * Expects value between 0 and 1
         */
        public function setVolume( volume:Number ):void
        {
            Swiz.dispatchEvent( new SetVolumeEvent( volume ) );
        }

        /**
         * getVolume
         *
         * Get the volume from the media player.
         */
        public function getVolume():Number
        {
            return model.player.volume;
        }

        /**
         * setccBool
         *
         * Set true when the cc Button is press
         */
        public function setccBool( ccBool:Boolean ):void
        {
            model.ccButtonBool = ccBool;
        }

        /**
         * setCaptionsURL
         *
         * Set captions URL and load the file.
         */
        public function setCaptionsURL( captionsURL:String ):void
        {
            if ( captionsURL != model.captionsURL )
            {
                var position:int = model.mediaURL.lastIndexOf( '/' );
                var mediaFile:String = model.mediaURL.substring( position + 1 );

                if ( mediaFile == 'matterhorn.mp4' )
                {
                    model.captionsURL = captionsURL;
                    Swiz.dispatchEvent( new LoadDFXPXMLEvent( model.captionsURL ) );
                }
            }
        }
        
        /**
         * setMediaURL
         *
         * Set media URL and init the player.
         */
        public function setMediaURL( mediaURL:String ):void
        {
            if ( mediaURL != model.mediaURL )
            {
                model.mediaPlayerWrapper.visible = true;
                model.mediaURL = mediaURL;
                Swiz.dispatchEvent( new InitPlayerEvent() );
            }
        }
        
        /**
         * setPlayerId
         *
         * Set player Id.
         */
        public function setPlayerId( playerId:String ):void
        {
           model.playerId = playerId;
        }

        /**
         * reportKeyDown
         *
         *
         */
        public function reportKeyUp( event:KeyboardEvent ):void
        {
            if ( event.altKey && event.ctrlKey )
            {
                passCharCode( event.keyCode );
            }
        }

        /**
         * passCharCode
         *
         * When the learner press any key for the mediaplayer
         */
        public function passCharCode( charCode:int ):void
        {
            // Play or pause the video
            if ( charCode == 80 || charCode == 112 ) // P or p
            {
                if ( model.player.playing )
                {
                    Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.PAUSE ) );
                	pressedKey = "PLAYPAUSE";
                }
                else
                {
                    Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.PLAY ) );
                	 pressedKey = "PLAYPAUSE";
                }
               
            }

            // Stop the video
            if ( charCode == 83 || charCode == 115 ) // S or s
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.STOP ) );
            	pressedKey = "STOP";
            }

            // Mute the video
            if ( charCode == 77 || charCode == 109 ) // M or m
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.MUTE ) );
            	pressedKey = "MUTE";
            }

            // Volume up
            if ( charCode == 85 || charCode == 117 ) // U or u
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.VOLUMEUP ) );
            	pressedKey = "VOLUMEUP";
            }

            // Volume down
            if ( charCode == 68 || charCode == 100 ) // D or d
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.VOLUMEDOWN ) );
            	pressedKey = "VOLUMEDOWN";
            }

            // Seek 0
            if ( charCode == 48 ) // 0
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKZERO ) );
            	pressedKey = "SEEKZERO";
            }

            // Seek 1
            if ( charCode == 49 ) // 1
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKONE ) );
            	pressedKey = "SEEKONE";
            }

            // Seek 2
            if ( charCode == 50 ) // 2
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKTWO ) );
            	pressedKey = "SEEKTWO";
            }

            // Seek 3
            if ( charCode == 51 ) // 3
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKTHREE ) );
            	pressedKey = "SEEKTHREE";
            }

            // Seek 4
            if ( charCode == 52 ) // 4
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKFOUR ) );
            	pressedKey = "SEEKFOUR";
            }

            // Seek 5
            if ( charCode == 53 ) // 5
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKFIVE ) );
            	pressedKey = "SEEKFIVE";
            }

            // Seek 6
            if ( charCode == 54 ) // 6
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKSIX ) );
            	pressedKey = "SEEKSIX";
            }

            // Seek 7
            if ( charCode == 55 ) // 7
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKSEVEN ) );
            	pressedKey = "SEEKSEVEN";
            }

            // Seek 8
            if ( charCode == 56 ) // 8
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKEIGHT ) );
            	pressedKey = "SEEKEIGHT";
            }

            // Seek 9
            if ( charCode == 57 ) // 9
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.SEEKNINE ) );
            	pressedKey = "SEEKNINE";
            }

            // Closed Caption
            if ( charCode == 67 || charCode == 99 ) // C or c
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.CLOSEDCAPTIONS ) );
            	pressedKey = "CLOSEDCAPTIONS";
            }

            // rewind
            if ( charCode == 82 || charCode == 114 ) // R or r
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.REWIND ) );
            	pressedKey = "REWIND";
            }

            // Fast forward
            if ( charCode == 70 || charCode == 102 ) // F or f
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.FASTFORWARD ) );
            	pressedKey = "FASTFORWARD";
            }

            // time
            if ( charCode == 84 || charCode == 116 ) // T or t
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.HEARTIMEINFO ) );
            	pressedKey = "HEARTIMEINFO";
            }

            // Information
            if ( charCode == 73 || charCode == 105 ) // I or i
            {
                Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.INFORMATION ) );
            	pressedKey = "INFORMATION";
            }
        }
        
        /**
         * onBridgeReady
         *
         * Set player Id.
         */
        public function onBridgeReady():void
        {
            ExternalInterface.call( ExternalFunction.ONPLAYERREADY, model.playerId );
            
            if( model.SECONDPLAYER == model.playerId )
            {
                model.player.volume = 0;
            }
        }
    }
}