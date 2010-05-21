package org.opencast.engage.videodisplay.control.util
{
	import org.opencast.engage.videodisplay.state.DefaultPlayerState;
	import org.osmf.events.MediaPlayerStateChangeEvent;
	import org.osmf.media.MediaElement;
	import org.osmf.media.MediaPlayer;
	
	public class OpencastMediaPlayer
	{
		 // mediaPlayerOne
        private var mediaPlayerOne:MediaPlayer;
        
         // mediaPlayerTwo
        private var mediaPlayerTwo:MediaPlayer;
        
        private var defaultPlayer:String;
        
		public function OpencastMediaPlayer()
		{
			//
			mediaPlayerOne = new MediaElement();
			mediaPlayerTwo = new MediaElement();
			
			setDefaultPlayer(DefaultPlayerState.PLAYERONE);
			
			//
			// Add MediaPlayerOne event handlers..
			
			mediaPlayerOne.addEventListener(MediaPlayerStateChangeEvent.MEDIA_PLAYER_STATE_CHANGE, playerOneOnStateChange);
			mediaPlayerOne.addEventListener( TimeEvent.DURATION_CHANGE, playerOneOnDurationChange);
            mediaPlayerOne.addEventListener( AudioEvent.MUTED_CHANGE, playerOneMuteChange );
            mediaPlayerOne.addEventListener( AudioEvent.VOLUME_CHANGE, playerOneVolumeChange );
            mediaPlayerOne.addEventListener( TimeEvent.CURRENT_TIME_CHANGE, playerOneOnCurrentTimeChange );
            mediaPlayerOne.addEventListener( TimeEvent.DURATION_CHANGE, playerOneOnDurationReached);
            mediaPlayerOne.addEventListener( MediaErrorEvent.MEDIA_ERROR, playerOneOnMediaError);
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
            mediaPlayerTwo.addEventListener( MediaErrorEvent.MEDIA_ERROR, playerTwoOnMediaError);
            mediaPlayerTwo.addEventListener( LoadEvent.BYTES_TOTAL_CHANGE, playerTwoOnBytesTotalChange );
            mediaPlayerTwo.addEventListener( LoadEvent.BYTES_LOADED_CHANGE, playerTwoOnBytesLoadedChange);
            mediaPlayerTwo.addEventListener( BufferEvent.BUFFERING_CHANGE, playerTwoOnBufferingChange);  
            mediaPlayerTwo.addEventListener( BufferEvent.BUFFER_TIME_CHANGE, playerTwoOnBufferTimeChange);
			
        }
        
	    public function setMedia(valueOne:MediaElement, valueTwo:MediaElement)
        {
            mediaPlayerOne.media = valueOne;
            mediaPlayerTwo.media = valueTwo;
	    }
		
		public function setDefaultPlayer(value):void
		{
		    defaultPlayer = value;
		    
		    if( value == DefaultPlayerState.PLAYERONE )
		    {
		    	mediaPlayerTwo.muted;
		    }
		    else if( value == DefaultPlayerState.PLAYERTWO )
            {
                mediaPlayerOne.muted;
            }
		}

        public function play():void
        {
            mediaPlayerOne.play();
            mediaPlayerTwo.play();
        }
        
        public function pause():void
        {
            mediaPlayerOne.pause();
            mediaPlayerTwo.pause();
        }
        
        public function seek(value:Number):void
        {
            mediaPlayerOne.seek(value);
            mediaPlayerTwo.seek(value);
        }
        
        public function muted():void
        {
            if( defaultPlayer == DefaultPlayerState.PLAYERONE )
            {
            	mediaPlayerOne.muted();
            }
            else if( defaultPlayer == DefaultPlayerState.PLAYERTWO )
            {
                mediaPlayerTwo.muted();
            }
        }
        
        public function volume(value:Number):void
        {
            if( defaultPlayer == DefaultPlayerState.PLAYERONE )
            {
                mediaPlayerOne.volume(value);
            }
            else if( defaultPlayer == DefaultPlayerState.PLAYERTWO )
            {
                mediaPlayerTwo.volume(value);
            }
        }
        
        // Player One
        
        private function playerOneOnStateChange(event:MediaPlayerStateChangeEvent)void
        {
        }
        
        private function playerOneOnDurationChange(event:TimeEvent)void
        {
        }
        
        private function playerOneMuteChange( event:AudioEvent ):void
        {
        	
        }
        
        private function playerOneVolumeChange( event:AudioEvent ):void
        {
        	
        }
        
        private function playerOneOnCurrentTimeChange( event:TimeEvent ):void
        {
        	
        }
        
        private function playerOneOnDurationReached( event:TimeEvent ):void
        {
           // do nothing
        }
        
        private function playerOneOnMediaError( event:MediaErrorEvent ):void
        {
        	
        }
        
        private function playerOneOnBytesTotalChange( event:LoadEvent ):void
        {
        	
        }
        
        private function playerOneOnBytesLoadedChange( event:LoadEvent ):void
        {
        	
        }
        
        private function playerOneOnBufferingChange( event:BufferEvent ):void
        {
            
        }
        
        private function playerOneOnBufferTimeChange(event:BufferEvent):void
        {
        	
        }
        
        // Player Two
        
        private function playerTwoOnStateChange(event:MediaPlayerStateChangeEvent)void
        {
        }
        
        private function playerTwoOnDurationChange(event:TimeEvent)void
        {
        }
        
        private function playerTwoMuteChange( event:AudioEvent ):void
        {
            
        }
        
        private function playerTwoVolumeChange( event:AudioEvent ):void
        {
            
        }
        
        private function playerTwoOnCurrentTimeChange( event:TimeEvent ):void
        {
            
        }
        
        private function playerTwoOnDurationReached( event:TimeEvent ):void
        {
           // do nothing
        }
        
        private function playerTwoOnMediaError( event:MediaErrorEvent ):void
        {
            
        }
        
        private function playerTwoOnBytesTotalChange( event:LoadEvent ):void
        {
            
        }
        
        private function playerTwoOnBytesLoadedChange( event:LoadEvent ):void
        {
            
        }
        
        private function playerTwoOnBufferingChange( event:BufferEvent ):void
        {
            
        }
        
        private function playerTwoOnBufferTimeChange(event:BufferEvent):void
        {
            
        }
    }
}