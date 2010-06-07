package org.opencast.engage.videodisplay.control.util
{
    import bridge.ExternalFunction;
    
    import flash.external.ExternalInterface;
    
    import mx.controls.Alert;
    
    import org.opencast.engage.videodisplay.control.event.DisplayCaptionEvent;
    import org.opencast.engage.videodisplay.model.VideodisplayModel;
    import org.opencast.engage.videodisplay.state.DefaultPlayerState;
    import org.opencast.engage.videodisplay.state.MediaState;
    import org.opencast.engage.videodisplay.state.PlayerState;
    import org.opencast.engage.videodisplay.state.SoundState;
    import org.opencast.engage.videodisplay.state.VideoState;
    import org.osmf.events.AudioEvent;
    import org.osmf.events.BufferEvent;
    import org.osmf.events.LoadEvent;
    import org.osmf.events.MediaErrorEvent;
    import org.osmf.events.MediaPlayerStateChangeEvent;
    import org.osmf.events.TimeEvent;
    import org.osmf.layout.HorizontalAlign;
    import org.osmf.layout.LayoutMetadata;
    import org.osmf.layout.ScaleMode;
    import org.osmf.layout.VerticalAlign;
    import org.osmf.media.MediaElement;
    import org.osmf.media.MediaPlayer;
    import org.osmf.metadata.MetadataWatcher;
    import org.swizframework.Swiz;
    
    
    public class OpencastMediaPlayer
    {
        [Autowire]
        public var model:VideodisplayModel;
        
        private var mediaPlayerOne:MediaPlayer;
        private var mediaPlayerTwo:MediaPlayer;
        private var defaultPlayer:String;
        private var mediaPlayerSingle:MediaPlayer;
        private var videoState:String;
        private var recommendationsWatcher:MetadataWatcher;
        private var _time:TimeCode;
        private var currentDurationString:String = "00:00:00";
        private var lastNewPositionString:String = "00:00:00";
        private var startPlay:Boolean = true;
        private var startPlayOne:Boolean = true;
        private var startPlayTwo:Boolean = true;
        private var maxDurationPlayer:String = '';
        private var formatMediaOne:Number = 0;
        private var formatMediaTwo:Number = 0;
      
       
        
        public function OpencastMediaPlayer(value:String)
        {
            Swiz.autowire( this );
            
            videoState = value;
            
            _time = new TimeCode();   
            
            if( videoState == VideoState.SINGLE )
            {
                mediaPlayerSingle = new MediaPlayer();
                mediaPlayerSingle.autoRewind = false;
                mediaPlayerSingle.autoPlay = false;
                mediaPlayerSingle.volume = 1;
                
                // Add MediaPlayerSingle event handlers..
                mediaPlayerSingle.addEventListener(MediaPlayerStateChangeEvent.MEDIA_PLAYER_STATE_CHANGE, onStateChange);
                mediaPlayerSingle.addEventListener( TimeEvent.DURATION_CHANGE, onDurationChange);
                mediaPlayerSingle.addEventListener( AudioEvent.MUTED_CHANGE, muteChange );
                mediaPlayerSingle.addEventListener( AudioEvent.VOLUME_CHANGE, volumeChange );
                mediaPlayerSingle.addEventListener( TimeEvent.CURRENT_TIME_CHANGE, onCurrentTimeChange );
                mediaPlayerSingle.addEventListener( TimeEvent.DURATION_CHANGE, onDurationReached);
                mediaPlayerSingle.addEventListener( MediaErrorEvent.MEDIA_ERROR, onMediaError);
                mediaPlayerSingle.addEventListener( LoadEvent.BYTES_TOTAL_CHANGE, onBytesTotalChange );
                mediaPlayerSingle.addEventListener( LoadEvent.BYTES_LOADED_CHANGE, onBytesLoadedChange);
                mediaPlayerSingle.addEventListener( BufferEvent.BUFFERING_CHANGE, onBufferingChange);  
                mediaPlayerSingle.addEventListener( BufferEvent.BUFFER_TIME_CHANGE, onBufferTimeChange);   
            }
            else if( videoState == VideoState.MULTI )
            {
                mediaPlayerOne = new MediaPlayer();
                mediaPlayerOne.autoRewind = false;
                mediaPlayerOne.autoPlay = false;
                
                mediaPlayerTwo = new MediaPlayer();
                mediaPlayerTwo.autoRewind = false;
                mediaPlayerTwo.autoPlay = false;
                
                defaultPlayer = DefaultPlayerState.PLAYERONE;
               
	            if( defaultPlayer == DefaultPlayerState.PLAYERONE )
	            {
	               mediaPlayerOne.volume = 1;
	               mediaPlayerTwo.volume = 0;
	               
	            }
	            else if( defaultPlayer == DefaultPlayerState.PLAYERTWO )
	            {
	               mediaPlayerOne.volume = 0;
	               mediaPlayerTwo.volume = 1;
	              
	            }
                
                                
                //
                // Add MediaPlayerOne event handlers..
                mediaPlayerOne.addEventListener(MediaPlayerStateChangeEvent.MEDIA_PLAYER_STATE_CHANGE, playerOneOnStateChange);
                mediaPlayerOne.addEventListener( TimeEvent.DURATION_CHANGE, playerOneOnDurationChange);
                mediaPlayerOne.addEventListener( AudioEvent.MUTED_CHANGE, playerOneMuteChange );
                mediaPlayerOne.addEventListener( AudioEvent.VOLUME_CHANGE, playerOneVolumeChange );
                mediaPlayerOne.addEventListener( TimeEvent.CURRENT_TIME_CHANGE, playerOneOnCurrentTimeChange );
                mediaPlayerOne.addEventListener( TimeEvent.DURATION_CHANGE, playerOneOnDurationReached);
                mediaPlayerOne.addEventListener( MediaErrorEvent.MEDIA_ERROR, onMediaError);
                mediaPlayerOne.addEventListener( LoadEvent.BYTES_TOTAL_CHANGE, playerOneOnBytesTotalChange );
                mediaPlayerOne.addEventListener( LoadEvent.BYTES_LOADED_CHANGE, playerOneOnBytesLoadedChange);
                mediaPlayerOne.addEventListener( BufferEvent.BUFFERING_CHANGE, playerOneOnBufferingChange);
                mediaPlayerOne.addEventListener( BufferEvent.BUFFER_TIME_CHANGE, playerOneOnBufferTimeChange);  
                
                // Add MediaPlayerTwo event handlers..
                mediaPlayerTwo.addEventListener(MediaPlayerStateChangeEvent.MEDIA_PLAYER_STATE_CHANGE, playerTwoOnStateChange);
                mediaPlayerTwo.addEventListener( TimeEvent.DURATION_CHANGE, playerTwoOnDurationChange);
                mediaPlayerTwo.addEventListener( AudioEvent.MUTED_CHANGE, playerTwoMuteChange );
                mediaPlayerTwo.addEventListener( AudioEvent.VOLUME_CHANGE, playerTwoVolumeChange );
                mediaPlayerTwo.addEventListener( TimeEvent.CURRENT_TIME_CHANGE, playerTwoOnCurrentTimeChange );
                mediaPlayerTwo.addEventListener( TimeEvent.DURATION_CHANGE, playerTwoOnDurationReached);
                mediaPlayerTwo.addEventListener( MediaErrorEvent.MEDIA_ERROR, onMediaError);
                mediaPlayerTwo.addEventListener( LoadEvent.BYTES_TOTAL_CHANGE, playerTwoOnBytesTotalChange );
                mediaPlayerTwo.addEventListener( LoadEvent.BYTES_LOADED_CHANGE, playerTwoOnBytesLoadedChange);
                mediaPlayerTwo.addEventListener( BufferEvent.BUFFERING_CHANGE, playerTwoOnBufferingChange);  
                mediaPlayerTwo.addEventListener( BufferEvent.BUFFER_TIME_CHANGE, playerTwoOnBufferTimeChange);
                
                // Set the default Player
                setDefaultPlayer(DefaultPlayerState.PLAYERONE);
            }
        }
        
        public function getVideoState():String
        {
            return videoState;
        }
        
        
        /**
         * setSingleMediaElement
         * 
         * Set the media element.
         *
         * @eventType event:MediaElement
         *
         * */
        public function setSingleMediaElement( value:MediaElement ):void
        {
            if( mediaPlayerSingle.media != null )
            {
                recommendationsWatcher.unwatch();
                model.mediaContainer.removeMediaElement( mediaPlayerSingle.media );
            }
            
            if (value != null)
            {
                // If there's no explicit layout metadata, center the content. 
                var layoutMetadata:LayoutMetadata = value.getMetadata(LayoutMetadata.LAYOUT_NAMESPACE) as LayoutMetadata;
                if (layoutMetadata == null)
                {
                    layoutMetadata = new LayoutMetadata();
                    layoutMetadata.scaleMode = ScaleMode.LETTERBOX;
                    layoutMetadata.percentHeight = 100;
                    layoutMetadata.percentWidth = 100;
                    layoutMetadata.horizontalAlign = HorizontalAlign.CENTER;
                    layoutMetadata.verticalAlign = VerticalAlign.MIDDLE;
                    value.addMetadata(LayoutMetadata.LAYOUT_NAMESPACE, layoutMetadata);
                }
                model.mediaContainer.addMediaElement(value);
            }
            mediaPlayerSingle.media = value;
            ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, 100 );
            
          
        }
        
        
         /**
         * setMediaElementOne
         * 
         * Set the media element.
         *
         * @eventType event:MediaElement
         *
         * */
        public function setMediaElementOne(value:MediaElement):void
        {
           if( mediaPlayerOne.media != null )
            {
                recommendationsWatcher.unwatch();
                model.mediaContainer.removeMediaElement( mediaPlayerOne.media );
            }
            
            if (value != null)
            {
                // If there's no explicit layout metadata, center the content. 
                model.layoutMetadataOne = value.getMetadata(LayoutMetadata.LAYOUT_NAMESPACE) as LayoutMetadata;
                if (model.layoutMetadataOne == null)
                {
                    model.layoutMetadataOne = new LayoutMetadata();
                    model.layoutMetadataOne.scaleMode = ScaleMode.LETTERBOX;
                    model.layoutMetadataOne.percentHeight = 100;
                    model.layoutMetadataOne.percentWidth = 100;
                    model.layoutMetadataOne.horizontalAlign = HorizontalAlign.RIGHT;
                    model.layoutMetadataOne.verticalAlign = VerticalAlign.BOTTOM;
                    value.addMetadata(LayoutMetadata.LAYOUT_NAMESPACE, model.layoutMetadataOne);
                }
                model.mediaContainerOne.addMediaElement(value);
            }
            mediaPlayerOne.media = value;
            //
            if( defaultPlayer == DefaultPlayerState.PLAYERONE )
            {
                ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, 100 );
            }
            
        }
        
        
        
        
        /**
         * setMediaElementTwo
         * 
         * Set the media element.
         *
         * @eventType event:MediaElement
         *
         * */
        public function setMediaElementTwo(value:MediaElement):void
        {
            if( mediaPlayerTwo.media != null )
            {
                recommendationsWatcher.unwatch();
                model.mediaContainerTwo.removeMediaElement( mediaPlayerTwo.media );
            }
            
            if (value != null)
            {
                // If there's no explicit layout metadata, center the content. 
                model.layoutMetadataTwo = value.getMetadata(LayoutMetadata.LAYOUT_NAMESPACE) as LayoutMetadata;
                if (model.layoutMetadataTwo == null)
                {
                    model.layoutMetadataTwo = new LayoutMetadata();
                    model.layoutMetadataTwo.scaleMode = ScaleMode.LETTERBOX;
                    model.layoutMetadataTwo.percentHeight = 100;
                    model.layoutMetadataTwo.percentWidth = 100;
                    model.layoutMetadataTwo.horizontalAlign = HorizontalAlign.LEFT;
                    model.layoutMetadataTwo.verticalAlign = VerticalAlign.BOTTOM;
                    value.addMetadata(LayoutMetadata.LAYOUT_NAMESPACE, model.layoutMetadataTwo);
                }
                model.mediaContainerTwo.addMediaElement( value );
               
            }
            
            mediaPlayerTwo.media = value ;
            
            //
            if( defaultPlayer == DefaultPlayerState.PLAYERTWO )
            {
                ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, 100 );
            }
           
            
        }
        
        public function getFormatMediaOne():Number
        {
            return formatMediaOne;
        
        }
        
        public function getFormatMediaTwo():Number
        {
            return formatMediaTwo;
        
        }
        
        
        public function setDefaultPlayer( value:String ):void
        {
            defaultPlayer = value;
            
            if( value == DefaultPlayerState.PLAYERONE )
            {
                mediaPlayerTwo.muted
            }
            else if( value == DefaultPlayerState.PLAYERTWO )
            {
                mediaPlayerOne.muted;
            }
        }
        
        

        public function play():void
        {
            
            
            if( videoState == VideoState.SINGLE )
            {
               
                mediaPlayerSingle.play();
                
                
            
            }
            else if( videoState == VideoState.MULTI )
            {
                
               
                    mediaPlayerOne.play();
               
                    mediaPlayerTwo.play();
               
                
                
               
            }
        }
        
        public function playing():Boolean
        {
            var playing:Boolean;
            
            if( videoState == VideoState.SINGLE )
            {
                playing = mediaPlayerSingle.playing;
            
            }
            else if( videoState == VideoState.MULTI )
            {
                playing = mediaPlayerOne.playing;
                
            }
            
            return playing;
        }
        
        public function pause():void
        {
            if( videoState == VideoState.SINGLE )
            {
                mediaPlayerSingle.pause();
            
            }
            else if( videoState == VideoState.MULTI )
            {
                mediaPlayerOne.pause();
                mediaPlayerTwo.pause();
            }
        }
        
        public function seek(value:Number):void
        {
            if( videoState == VideoState.SINGLE )
            {
                mediaPlayerSingle.seek(value);
            }
            else if( videoState == VideoState.MULTI )
            {
                
                if( mediaPlayerOne.duration < value )
                {
                    mediaPlayerOne.pause();
                    mediaPlayerOne.seek( mediaPlayerOne.duration - 0.1 );
                }
                else
                {
                    mediaPlayerOne.seek(value);
                    if( mediaPlayerOne.paused && mediaPlayerTwo.playing)
                    {
                       mediaPlayerOne.play();
                    }
                }
           
                
                if( mediaPlayerTwo.duration < value )
                {
                    mediaPlayerTwo.pause();
                    mediaPlayerTwo.seek( mediaPlayerTwo.duration - 0.1);
                }
                else
                {
                    mediaPlayerTwo.seek(value);
                    if( mediaPlayerOne.playing && mediaPlayerTwo.paused)
                    {
                       mediaPlayerTwo.play();
                    }
                }
            }
        }
        
        public function seeking():Boolean
        {
           var seeking:Boolean;
            if( videoState == VideoState.SINGLE )
            {
                seeking = mediaPlayerSingle.seeking;
            
            }
            else if( videoState == VideoState.MULTI )
            {
                seeking = mediaPlayerOne.seeking;
                
            }
        
            return seeking;
        
        }
        
        
        public function setMuted( muted:Boolean ):void
        {
            if( videoState == VideoState.SINGLE )
            {
               mediaPlayerSingle.muted = muted;
            
            }
            else if( videoState == VideoState.MULTI )
            {
               if( defaultPlayer == DefaultPlayerState.PLAYERONE )
                {
                    mediaPlayerOne.muted = muted;
                }
                else if( defaultPlayer == DefaultPlayerState.PLAYERTWO )
                {
                   mediaPlayerTwo.muted = muted;
                }
                
            }
        }
        
        
        public function getMuted():Boolean
        {
            var muted:Boolean;
            
            if( videoState == VideoState.SINGLE )
            {
                muted = mediaPlayerSingle.muted;
            }
            else if( videoState == VideoState.MULTI )
            {
               if( defaultPlayer == DefaultPlayerState.PLAYERONE )
                {
                    muted = mediaPlayerOne.muted;
                }
                else if( defaultPlayer == DefaultPlayerState.PLAYERTWO )
                {
                   muted = mediaPlayerTwo.muted;
                }
            }
            
            return muted;
        }
        
        public function setVolume(value:Number):void
        {
            if( videoState == VideoState.SINGLE )
            {
                mediaPlayerSingle.volume = value;
            }
            else if( videoState == VideoState.MULTI )
            {
                if( defaultPlayer == DefaultPlayerState.PLAYERONE )
                {
                    mediaPlayerOne.volume = value;
                }
                else if( defaultPlayer == DefaultPlayerState.PLAYERTWO )
                {
                    mediaPlayerTwo.volume = value;
                }
            }
        }
        
        public function getVolume():Number
        {
            var volume:Number;
            
            if( videoState == VideoState.SINGLE  )
            {
                volume = mediaPlayerSingle.volume;
            }
            else if( videoState == VideoState.MULTI )
            {
                if( defaultPlayer == DefaultPlayerState.PLAYERONE )
                {
                    volume = mediaPlayerOne.volume;
                }
                else if( defaultPlayer == DefaultPlayerState.PLAYERTWO )
                {
                    volume = mediaPlayerTwo.volume;
                }
            }
            return volume;
        
        }
        
        
        
        
        // Player Single
        
        
        
        private function onStateChange(event:MediaPlayerStateChangeEvent):void
        {
        }
        
        
        
        /**
         * onDurationChange
         *
         * When the duration is change
         * 
         * @eventType event:TimeEvent
         * */
        private function onDurationChange( event:TimeEvent ):void
        {
            // Store new duration as current duration in the videodisplay model
            model.currentDuration = event.time;
            model.currentDurationString = _time.getTC( event.time );
            ExternalInterface.call( ExternalFunction.SETDURATION, event.time );
            ExternalInterface.call( ExternalFunction.SETTOTALTIME, model.currentDurationString );
            
            if( event.time * 0.1 > 10)
            {
                model.rewindTime = event.time * 0.1;
                model.fastForwardTime = event.time * 0.1;
            }
            else
            {
                model.rewindTime = 10;
                model.fastForwardTime = 10;
            }
        }
        
        
        
        
        
        /**
         * muteChange
         *
         * When the player is mute or unmute
         * 
         * @eventType event:AudioEvent
         * */
        private function muteChange( event:AudioEvent ):void
        {
            if( event.muted )
            {
                ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, 0 );
                model.playerVolume = 0;
                ExternalInterface.call( ExternalFunction.MUTESOUND, '' );
                model.soundState = SoundState.VOLUMEMUTE;
                if( model.ccButtonBoolean == false )
                {
                    model.ccBoolean = true;
                    ExternalInterface.call( ExternalFunction.SETCCICONON, '' );
                }
            }
            else
            {
                ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, mediaPlayerSingle.volume * 100 ); 
                model.playerVolume = mediaPlayerSingle.volume;
                
                if( mediaPlayerSingle.volume > 0.50 )
                {
                    ExternalInterface.call( ExternalFunction.HIGHSOUND, '' );
                    model.soundState = SoundState.VOLUMEMAX;
                }
           
                if( mediaPlayerSingle.volume <= 0.50 )
                {
                    ExternalInterface.call( ExternalFunction.LOWSOUND, '' );
                    model.soundState = SoundState.VOLUMEMED;
                }
           
                if( mediaPlayerSingle.volume == 0 )
                {
                    ExternalInterface.call( ExternalFunction.NONESOUND, '' );
                    model.soundState = SoundState.VOLUMEMIN;
                }
                
                if( model.ccButtonBoolean == false )
                {
                    model.ccBoolean = false;
                    ExternalInterface.call( ExternalFunction.SETCCICONOFF, '' );
                }
                
            }
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
           if( mediaPlayerSingle.muted == true )
           {
                mediaPlayerSingle.muted = false;
           }
           if( mediaPlayerSingle.volume > 0.50 )
           {
                ExternalInterface.call( ExternalFunction.HIGHSOUND, '' );
                model.soundState = SoundState.VOLUMEMAX;
           }
           
           if( mediaPlayerSingle.volume <= 0.50 )
           {
                ExternalInterface.call( ExternalFunction.LOWSOUND, '' );
                model.soundState = SoundState.VOLUMEMED;
           }
           
           if( mediaPlayerSingle.volume == 0 )
           {
                ExternalInterface.call( ExternalFunction.NONESOUND, '' );
                model.soundState = SoundState.VOLUMEMIN;
           }
           
           if( model.ccButtonBoolean == false && model.ccBoolean == true )
           {
                model.ccBoolean = false;
                ExternalInterface.call( ExternalFunction.SETCCICONOFF, '' );
                model.soundState = SoundState.VOLUMEMUTE;
           }
        }
        
        /**
         * onCurrentTimeChange
         * 
         * When the current time is change
         *
         * @eventType event:TimeEvent
         * */
        private function onCurrentTimeChange( event:TimeEvent ):void
        {
            var newPositionString:String = _time.getTC( event.time );
            
            
            if ( newPositionString != lastNewPositionString )
            {
                ExternalInterface.call( ExternalFunction.SETCURRENTTIME, newPositionString );
                lastNewPositionString = newPositionString;
            }

            if ( !mediaPlayerSingle.seeking )
            {
               ExternalInterface.call( ExternalFunction.SETPLAYHEAD, event.time );
            }
            
            if ( model.captionsURL != null )
            {
                Swiz.dispatchEvent( new DisplayCaptionEvent( event.time ) );
            }
            
            if( lastNewPositionString == model.currentDurationString )
            {
                model.currentPlayerState = PlayerState.PAUSING;
                ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, PlayerState.PLAYING );
               
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
           // do nothing
        }
        
        /**
         * onMediaError
         *
         * When the media file ist not available.
         * 
         * @eventType event:MediaErrorEvent
         *
         * */
        private function onMediaError( event:MediaErrorEvent ):void
        {
            model.mediaState = MediaState.ERROR;
           
            model.errorId =  event.error.errorID.toString();
            model.errorMessage = event.error.message;
            model.errorDetail = event.error.detail;
        }
        
        /**
         * onBytesTotalChange
         *
         * Save the total bytes of the video
         * 
         * @eventType event:LoadEvent
         * */
        private function onBytesTotalChange( event:LoadEvent ):void
        {
            model.bytesTotal = event.bytes;
        }
        
        /**
         * onBytesLoadedChange
         *
         * When the loaded bytes change
         * 
         * @eventType event:LoadEvent
         *
         * */
        private function onBytesLoadedChange( event:LoadEvent ):void
        {
            var progress:Number = 0;
            model.bytesLoaded = event.bytes;
            
            try
            {
                progress = Math.round( event.bytes / model.bytesTotal * 100 );
                ExternalInterface.call( ExternalFunction.SETPROGRESS, progress );
                model.progressBar.setProgress( progress, 100 );
                model.progress = progress;
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
        
        private function onBufferTimeChange( event:BufferEvent ):void
        {
            
        }
        
        
        
        // Player One
        
        private function playerOneOnStateChange( event:MediaPlayerStateChangeEvent ):void
        {
        }
        
        private function playerOneOnDurationChange(event:TimeEvent):void
        {
        	// Store new duration as current duration in the videodisplay model
        	
        	if( model.currentDuration < event.time )
        	{
	        	onDurationChange( event );
	        	maxDurationPlayer = DefaultPlayerState.PLAYERONE;

        	}
        }
        
        private function playerOneMuteChange( event:AudioEvent ):void
        {
            if( defaultPlayer == DefaultPlayerState.PLAYERONE )
            {
            
	            if( event.muted )
	            {
	                ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, 0 );
	                model.playerVolume = 0;
	                ExternalInterface.call( ExternalFunction.MUTESOUND, '' );
	                model.soundState = SoundState.VOLUMEMUTE;
	                if( model.ccButtonBoolean == false )
	                {
	                    model.ccBoolean = true;
	                    ExternalInterface.call( ExternalFunction.SETCCICONON, '' );
	                }
	            }
	            else
	            {
	                ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, mediaPlayerOne.volume * 100 ); 
	                model.playerVolume = mediaPlayerOne.volume;
	                
	                if( mediaPlayerOne.volume > 0.50 )
	                {
	                    ExternalInterface.call( ExternalFunction.HIGHSOUND, '' );
	                    model.soundState = SoundState.VOLUMEMAX;
	                }
	           
	                if( mediaPlayerOne.volume <= 0.50 )
	                {
	                    ExternalInterface.call( ExternalFunction.LOWSOUND, '' );
	                    model.soundState = SoundState.VOLUMEMED;
	                }
	           
	                if( mediaPlayerOne.volume == 0 )
	                {
	                    ExternalInterface.call( ExternalFunction.NONESOUND, '' );
	                    model.soundState = SoundState.VOLUMEMIN;
	                }
	                
	                if( model.ccButtonBoolean == false )
	                {
	                    model.ccBoolean = false;
	                    ExternalInterface.call( ExternalFunction.SETCCICONOFF, '' );
	                }
	                
	            }
            }
        }
        
        private function playerOneVolumeChange( event:AudioEvent ):void
        {
	       if( defaultPlayer == DefaultPlayerState.PLAYERONE )
	       {
	           if( mediaPlayerOne.muted == true )
	           {
	                mediaPlayerOne.muted = false;
	           }
	           if( mediaPlayerOne.volume > 0.50 )
	           {
	                ExternalInterface.call( ExternalFunction.HIGHSOUND, '' );
	                model.soundState = SoundState.VOLUMEMAX;
	           }
	           
	           if( mediaPlayerOne.volume <= 0.50 )
	           {
	                ExternalInterface.call( ExternalFunction.LOWSOUND, '' );
	                model.soundState = SoundState.VOLUMEMED;
	           }
	           
	           if( mediaPlayerOne.volume == 0 )
	           {
	                ExternalInterface.call( ExternalFunction.NONESOUND, '' );
	                model.soundState = SoundState.VOLUMEMIN;
	           }
	           
	           if( model.ccButtonBoolean == false && model.ccBoolean == true )
	           {
	                model.ccBoolean = false;
	                ExternalInterface.call( ExternalFunction.SETCCICONOFF, '' );
	                model.soundState = SoundState.VOLUMEMUTE;
	           }
	       }
        }
        
        private function playerOneOnCurrentTimeChange( event:TimeEvent ):void
        {
            if( maxDurationPlayer == DefaultPlayerState.PLAYERONE )
            {
                var newPositionString:String = _time.getTC( event.time );
               
                if ( newPositionString != lastNewPositionString )
                {
                    ExternalInterface.call( ExternalFunction.SETCURRENTTIME, newPositionString );
                    lastNewPositionString = newPositionString;
                }
    
                if ( !mediaPlayerOne.seeking )
                {
                   ExternalInterface.call( ExternalFunction.SETPLAYHEAD, event.time );
                }
                
                if ( model.captionsURL != null )
                {
                    Swiz.dispatchEvent( new DisplayCaptionEvent( event.time ) );
                }
                
                if( lastNewPositionString == model.currentDurationString )
                {
                   // model.currentPlayerState = PlayerState.PAUSING;
                    //ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, PlayerState.PLAYING );
                   
                }
                
                model.currentPlayhead = event.time;
            }
        }
        
        private function playerOneOnDurationReached( event:TimeEvent ):void
        {
           // do nothing
        }
        
        
        
        private function playerOneOnBytesTotalChange( event:LoadEvent ):void
        {
            model.bytesTotalOne = event.bytes;
        }
        
        private function playerOneOnBytesLoadedChange( event:LoadEvent ):void
        {
              
            if( model.mediaTypeTwo == model.RTMP )
            {
                onBytesLoadedChange(event);
            }
            else 
            {
                var progress:Number = 0;
                model.bytesLoadedOne = event.bytes;
           
                if( model.bytesLoadedTwo > event.bytes )
                { 
                	onBytesLoadedChange(event);
                }
            }
        }
        
        
        
        
        
        private function playerOneOnBufferingChange( event:BufferEvent ):void
        {
            
        }
        
        private function playerOneOnBufferTimeChange(event:BufferEvent):void
        {
            
        }
        
        // Player Two
        
        private function playerTwoOnStateChange( event:MediaPlayerStateChangeEvent ):void
        {
        }
        
        private function playerTwoOnDurationChange( event:TimeEvent ):void
        {
        	if( model.currentDuration < event.time )
            {
                onDurationChange( event );
                maxDurationPlayer = DefaultPlayerState.PLAYERTWO;

            }
        }
        
        private function playerTwoMuteChange( event:AudioEvent ):void
        {
            if( defaultPlayer == DefaultPlayerState.PLAYERTWO )
            {
            
                if( event.muted )
                {
                    ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, 0 );
                    model.playerVolume = 0;
                    ExternalInterface.call( ExternalFunction.MUTESOUND, '' );
                    model.soundState = SoundState.VOLUMEMUTE;
                    if( model.ccButtonBoolean == false )
                    {
                        model.ccBoolean = true;
                        ExternalInterface.call( ExternalFunction.SETCCICONON, '' );
                    }
                }
                else
                {
                    ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, mediaPlayerTwo.volume * 100 ); 
                    model.playerVolume = mediaPlayerTwo.volume;
                    
                    if( mediaPlayerTwo.volume > 0.50 )
                    {
                        ExternalInterface.call( ExternalFunction.HIGHSOUND, '' );
                        model.soundState = SoundState.VOLUMEMAX;
                    }
               
                    if( mediaPlayerTwo.volume <= 0.50 )
                    {
                        ExternalInterface.call( ExternalFunction.LOWSOUND, '' );
                        model.soundState = SoundState.VOLUMEMED;
                    }
               
                    if( mediaPlayerTwo.volume == 0 )
                    {
                        ExternalInterface.call( ExternalFunction.NONESOUND, '' );
                        model.soundState = SoundState.VOLUMEMIN;
                    }
                    
                    if( model.ccButtonBoolean == false )
                    {
                        model.ccBoolean = false;
                        ExternalInterface.call( ExternalFunction.SETCCICONOFF, '' );
                    }
                    
                }
            }
        }
        
        private function playerTwoVolumeChange( event:AudioEvent ):void
        {
            if( defaultPlayer == DefaultPlayerState.PLAYERTWO )
            {
               if( mediaPlayerTwo.muted == true )
               {
                    mediaPlayerTwo.muted = false;
               }
               if( mediaPlayerTwo.volume > 0.50 )
               {
                    ExternalInterface.call( ExternalFunction.HIGHSOUND, '' );
                    model.soundState = SoundState.VOLUMEMAX;
               }
               
               if( mediaPlayerTwo.volume <= 0.50 )
               {
                    ExternalInterface.call( ExternalFunction.LOWSOUND, '' );
                    model.soundState = SoundState.VOLUMEMED;
               }
               
               if( mediaPlayerTwo.volume == 0 )
               {
                    ExternalInterface.call( ExternalFunction.NONESOUND, '' );
                    model.soundState = SoundState.VOLUMEMIN;
               }
               
               if( model.ccButtonBoolean == false && model.ccBoolean == true )
               {
                    model.ccBoolean = false;
                    ExternalInterface.call( ExternalFunction.SETCCICONOFF, '' );
                    model.soundState = SoundState.VOLUMEMUTE;
               }
           }
        }
        
        private function playerTwoOnCurrentTimeChange( event:TimeEvent ):void
        {
            
            
            if( maxDurationPlayer == DefaultPlayerState.PLAYERTWO )
            {
                var newPositionString:String = _time.getTC( event.time );
                
                if ( newPositionString != lastNewPositionString )
                {
                    ExternalInterface.call( ExternalFunction.SETCURRENTTIME, newPositionString );
                    lastNewPositionString = newPositionString;
                }
    
                if ( !mediaPlayerTwo.seeking )
                {
                   ExternalInterface.call( ExternalFunction.SETPLAYHEAD, event.time );
                }
                
                if ( model.captionsURL != null )
                {
                    Swiz.dispatchEvent( new DisplayCaptionEvent( event.time ) );
                }
                
                if( lastNewPositionString == model.currentDurationString )
                {
                    //model.currentPlayerState = PlayerState.PAUSING;
                    //ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, PlayerState.PLAYING );
                   
                }
                
                model.currentPlayhead = event.time;
            }
        }
        
        private function playerTwoOnDurationReached( event:TimeEvent ):void
        {
           // do nothing
        }
        
        
        
        private function playerTwoOnBytesTotalChange( event:LoadEvent ):void
        {
             model.bytesTotalTwo = event.bytes;
        }
        
        private function playerTwoOnBytesLoadedChange( event:LoadEvent ):void
        {
        	 if( model.mediaTypeOne == model.RTMP )
            {
                onBytesLoadedChange(event);
            }
            else 
            {
                var progress:Number = 0;
                model.bytesLoadedTwo = event.bytes;
           
                if( model.bytesLoadedOne > event.bytes )
                { 
                    onBytesLoadedChange(event);
                }
            }
        }
        
        private function playerTwoOnBufferingChange( event:BufferEvent ):void
        {
            
        }
        
        private function playerTwoOnBufferTimeChange(event:BufferEvent):void
        {
            
        }
    }
}