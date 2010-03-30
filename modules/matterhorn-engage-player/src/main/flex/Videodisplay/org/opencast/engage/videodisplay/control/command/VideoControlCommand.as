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
    
    import org.opencast.engage.videodisplay.control.event.ClosedCaptionsEvent;
    import org.opencast.engage.videodisplay.control.event.VideoControlEvent;
    import org.opencast.engage.videodisplay.model.VideodisplayModel;
    import org.opencast.engage.videodisplay.state.MediaState;
    import org.opencast.engage.videodisplay.state.PlayerState;
    import org.swizframework.Swiz;

    public class VideoControlCommand
    {
        [Autowire]
        public var model:VideodisplayModel;

        /** Constructor */
        public function VideoControlCommand()
        {
            Swiz.autowire( this );
        }

        /** execute
         *
         * When the learner press a button, or use the keyboard shurtcuts.
         *
         * @eventType event:VideoControlEvent
         * */
        public function execute( event:VideoControlEvent ):void
        {
            var currentPlayPauseState:String;
            var percent:int = 100;
            var skipVolume:Number = 0.1;

            switch ( event.videoControlType )
            {
                case VideoControlEvent.PLAY:
                	if( model.mediaState == MediaState.MULTI)
                	{
                		if( !model.mediaPlayerOne.playing && !model.mediaPlayerTwo.playing)
                		{
                			model.mediaPlayerOne.play();
                			model.mediaPlayerTwo.play();
                		}
                	}
                	else
                	{
                		if ( !model.mediaPlayerSingle.playing )
	                    {
	                    	model.mediaPlayerSingle.play();
	                    }
                	}
                	model.currentPlayerState = PlayerState.PLAYING;
                    currentPlayPauseState = PlayerState.PAUSING;
                    ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState );
                    break;

                case VideoControlEvent.PAUSE:
                	if( model.mediaState == MediaState.MULTI)
                	{
                		if( model.mediaPlayerOne.playing && model.mediaPlayerTwo.playing)
                		{
                			model.mediaPlayerOne.pause();
                			model.mediaPlayerTwo.pause();
                		
                		}
                	}
                	else
                	{
                		if ( model.mediaPlayerSingle.playing )
	                    {
	                    	model.mediaPlayerSingle.pause();
	                    }
                	}
                	model.currentPlayerState = PlayerState.PAUSING;
                    currentPlayPauseState = PlayerState.PLAYING;
                    ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState );
                    break;

                case VideoControlEvent.STOP:
                
                	if( model.mediaState == MediaState.MULTI)
                	{
                		if( model.mediaPlayerOne.playing && model.mediaPlayerTwo.playing)
                		{
                			model.mediaPlayerOne.pause();
                			model.mediaPlayerOne.seek( 0 );
                			model.mediaPlayerTwo.pause();
                			model.mediaPlayerTwo.seek( 0 );
                		
                		}
                	}
                	else
                	{
                		if ( model.mediaPlayerSingle.playing )
	                    {
	                    	model.mediaPlayerSingle.pause();
	                    	model.mediaPlayerSingle.seek( 0 );
	                    }
                	}
                
             		model.currentPlayerState = PlayerState.PAUSING;
                    currentPlayPauseState = PlayerState.PLAYING;
                    ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState );
                
                case VideoControlEvent.SKIPBACKWARD:
                	
                	if( model.mediaState == MediaState.MULTI)
                	{
                		model.mediaPlayerOne.seek( 0 );
                		model.mediaPlayerTwo.seek( 0 );
                	}
                	else
                	{
                		model.mediaPlayerSingle.seek( 0 );
                	}
                	break;

                case VideoControlEvent.REWIND:
                
                    if ( model.currentPlayhead + 1 > model.rewindTime )
                    {
                    	
                    	if( model.mediaState == MediaState.MULTI)
	                	{
	                		if( model.mediaPlayerOne.playing && model.mediaPlayerTwo.playing)
	                		{
	                			model.mediaPlayerOne.pause();
	                			model.mediaPlayerTwo.pause();
                            	model.mediaPlayerOne.seek( model.currentPlayhead - model.rewindTime );
                            	model.mediaPlayerTwo.seek( model.currentPlayhead - model.rewindTime );
                            	model.mediaPlayerOne.play();
                            	model.mediaPlayerTwo.play();
	                		
	                		}
	                		else
                        	{
                        		model.mediaPlayerOne.seek( model.currentPlayhead - model.rewindTime );
                            	model.mediaPlayerTwo.seek( model.currentPlayhead - model.rewindTime );
                        	}
	                	}
	                	else
	                	{
	                		if ( model.mediaPlayerSingle.playing )
		                    {
		                    	model.mediaPlayerSingle.pause();
                            	model.mediaPlayerSingle.seek( model.currentPlayhead - model.rewindTime );
                            	model.mediaPlayerSingle.play();
		                    }
		                    else
		                    {
		                    	model.mediaPlayerSingle.seek( model.currentPlayhead - model.rewindTime );
		                    }
	                	}
                    }
                	break;

                case VideoControlEvent.FASTFORWARD:
                
                	if( model.mediaState == MediaState.MULTI)
	                {
	                	model.mediaPlayerOne.seek( model.currentPlayhead + model.fastForwardTime );
	                	model.mediaPlayerTwo.seek( model.currentPlayhead + model.fastForwardTime );	
	                }
	                else
	                {
	                	model.mediaPlayerSingle.seek( model.currentPlayhead + model.fastForwardTime );
	                
	                }
                	break;

                case VideoControlEvent.SKIPFORWARD:
                
                
                	if( model.mediaState == MediaState.MULTI)
                	{
                		if( model.mediaPlayerOne.playing && model.mediaPlayerTwo.playing)
                		{
                			model.mediaPlayerOne.seek( model.currentDuration - 1 );
                			model.mediaPlayerTwo.seek( model.currentDuration - 1 );
                        	model.mediaPlayerOne.pause();
                        	model.mediaPlayerTwo.pause();
                        	
                        	model.currentPlayerState = PlayerState.PAUSING;
                        	currentPlayPauseState = PlayerState.PLAYING;
                        	ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState );
                		}
                		else
                		{
                			model.mediaPlayerOne.seek( model.currentDuration - 1 );
                			model.mediaPlayerTwo.seek( model.currentDuration - 1 );
                		}
                		
                	}
                	else
                	{
                		if ( model.mediaPlayerSingle.playing )
	                    {
	                    	model.mediaPlayerSingle.seek( model.currentDuration - 1 );
                        	model.mediaPlayerSingle.pause();
                        	
                        	model.currentPlayerState = PlayerState.PAUSING;
                        	currentPlayPauseState = PlayerState.PLAYING;
                        	ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState );
                        }
	                    else
	                    {
	                    	model.mediaPlayerSingle.seek( model.currentDuration - 1 );
	                    }
                	
                	}
                	break;

                case VideoControlEvent.MUTE:
                
                	if( model.mediaState == MediaState.MULTI )
                	{
                		if( model.mediaPlayerOne.muted == true)
	                    {
	                    	model.mediaPlayerOne.muted = false;
	                    }
	                    else
	                    {
	                    	model.mediaPlayerOne.muted = true;
	                    }
                	}
                	else
                	{
                		if( model.mediaPlayerSingle.muted == true)
	                    {
	                    	model.mediaPlayerSingle.muted = false;
	                    }
	                    else
	                    {
	                    	model.mediaPlayerSingle.muted = true;
	                    }
                	
                	}
                	break;

                case VideoControlEvent.VOLUMEUP:
                
                	if( model.mediaState == MediaState.MULTI )
                	{
                		
                		if ( model.mediaPlayerOne.volume != 1 )
	                    {
	                        model.mediaPlayerOne.volume = model.mediaPlayerOne.volume + skipVolume;
	                    }
	                    ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, Math.round(model.mediaPlayerOne.volume * percent) );
                	}
                	else
                	{
                		if ( model.mediaPlayerSingle.volume != 1 )
	                    {
	                        model.mediaPlayerSingle.volume = model.mediaPlayerSingle.volume + skipVolume;
	                    }
	                    ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, Math.round(model.mediaPlayerSingle.volume * percent) );
                	}
                	
					break;

                case VideoControlEvent.VOLUMEDOWN:
                
                	if( model.mediaState == MediaState.MULTI )
                	{
                		if ( model.mediaPlayerOne.volume != 0 )
	                    {
	                        model.mediaPlayerOne.volume = model.mediaPlayerOne.volume - skipVolume;
	                        if( model.mediaPlayerOne.volume < 0 )
	                        {
	                        	model.mediaPlayerOne.volume = 0;
	                        }
	                    }
                		ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, Math.round(model.mediaPlayerOne.volume * percent) );
                		
                	}
                	else
                	{
                		if ( model.mediaPlayerSingle.volume != 0 )
	                    {
	                        model.mediaPlayerSingle.volume = model.mediaPlayerOne.volume - skipVolume;
	                        if( model.mediaPlayerSingle.volume < 0 )
	                        {
	                        	model.mediaPlayerSingle.volume = 0;
	                        }
	                    }
                		ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, Math.round(model.mediaPlayerSingle.volume * percent) );
                	}
                	
                    break;

                case VideoControlEvent.SEEKZERO:
                	if( model.mediaState == MediaState.MULTI )
                    {
                    	model.mediaPlayerOne.seek( ( model.currentDuration / 10 ) * 0 );
                    	model.mediaPlayerTwo.seek( ( model.currentDuration / 10 ) * 0 );
                    }
                    else
                    {
                    	model.mediaPlayerSingle.seek( ( model.currentDuration / 10 ) * 0 );
                    }
                	break;

                case VideoControlEvent.SEEKONE:
                    if( model.mediaState == MediaState.MULTI )
                    {
                    	model.mediaPlayerOne.seek( ( model.currentDuration / 10 ) * 1 );
                    	model.mediaPlayerTwo.seek( ( model.currentDuration / 10 ) * 1 );
                    }
                    else
                    {
                    	model.mediaPlayerSingle.seek( ( model.currentDuration / 10 ) * 1 );
                    }
                    break;

                case VideoControlEvent.SEEKTWO:
                    if( model.mediaState == MediaState.MULTI )
                    {
                    	model.mediaPlayerOne.seek( ( model.currentDuration / 10 ) * 2 );
                    	model.mediaPlayerTwo.seek( ( model.currentDuration / 10 ) * 2 );
                    }
                    else
                    {
                    	model.mediaPlayerSingle.seek( ( model.currentDuration / 10 ) * 2 );
                    }
                    break;

                case VideoControlEvent.SEEKTHREE:
                    if( model.mediaState == MediaState.MULTI )
                    {
                    	model.mediaPlayerOne.seek( ( model.currentDuration / 10 ) * 3 );
                    	model.mediaPlayerTwo.seek( ( model.currentDuration / 10 ) * 3 );
                    }
                    else
                    {
                    	model.mediaPlayerSingle.seek( ( model.currentDuration / 10 ) * 3 );
                    }
                    break;

                case VideoControlEvent.SEEKFOUR:
                    if( model.mediaState == MediaState.MULTI )
                    {
                    	model.mediaPlayerOne.seek( ( model.currentDuration / 10 ) * 4 );
                    	model.mediaPlayerTwo.seek( ( model.currentDuration / 10 ) * 4 );
                    }
                    else
                    {
                    	model.mediaPlayerSingle.seek( ( model.currentDuration / 10 ) * 4 );
                    }
                    break;

                case VideoControlEvent.SEEKFIVE:
                    if( model.mediaState == MediaState.MULTI )
                    {
                    	model.mediaPlayerOne.seek( ( model.currentDuration / 10 ) * 5 );
                    	model.mediaPlayerTwo.seek( ( model.currentDuration / 10 ) * 5 );
                    }
                    else
                    {
                    	model.mediaPlayerSingle.seek( ( model.currentDuration / 10 ) * 5 );
                    }
                    break;

                case VideoControlEvent.SEEKSIX:
                    if( model.mediaState == MediaState.MULTI )
                    {
                    	model.mediaPlayerOne.seek( ( model.currentDuration / 10 ) * 6 );
                    	model.mediaPlayerTwo.seek( ( model.currentDuration / 10 ) * 6 );
                    }
                    else
                    {
                    	model.mediaPlayerSingle.seek( ( model.currentDuration / 10 ) * 6 );
                    }
                    break;

                case VideoControlEvent.SEEKSEVEN:
                    if( model.mediaState == MediaState.MULTI )
                    {
                    	model.mediaPlayerOne.seek( ( model.currentDuration / 10 ) * 7 );
                    	model.mediaPlayerTwo.seek( ( model.currentDuration / 10 ) * 7 );
                    }
                    else
                    {
                    	model.mediaPlayerSingle.seek( ( model.currentDuration / 10 ) * 7 );
                    }
                    break;

                case VideoControlEvent.SEEKEIGHT:
                    if( model.mediaState == MediaState.MULTI )
                    {
                    	model.mediaPlayerOne.seek( ( model.currentDuration / 10 ) * 8 );
                    	model.mediaPlayerTwo.seek( ( model.currentDuration / 10 ) * 8 );
                    }
                    else
                    {
                    	model.mediaPlayerSingle.seek( ( model.currentDuration / 10 ) * 8 );
                    }
                    break;

                case VideoControlEvent.SEEKNINE:
                    if( model.mediaState == MediaState.MULTI )
                    {
                    	model.mediaPlayerOne.seek( ( model.currentDuration / 10 ) * 9 );
                    	model.mediaPlayerTwo.seek( ( model.currentDuration / 10 ) * 9 );
                    }
                    else
                    {
                    	model.mediaPlayerSingle.seek( ( model.currentDuration / 10 ) * 9 );
                    }
                    break;

                case VideoControlEvent.CLOSEDCAPTIONS:
                    if ( model.ccBoolean )
                    {
                        Swiz.dispatchEvent( new ClosedCaptionsEvent( false ) );
                        model.ccButtonBool = false;
                    }
                    else
                    {
                        Swiz.dispatchEvent( new ClosedCaptionsEvent( true ) );
                        model.ccButtonBool = true;
                    }

                    break;

                case VideoControlEvent.HEARTIMEINFO:
                    Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.PAUSE ) );
                    ExternalInterface.call( ExternalFunction.CURRENTTIME, model.timeCode.getTC( model.currentPlayhead ) );
                    break;

                case VideoControlEvent.SHORTCUTS:
                    ExternalInterface.call( ExternalFunction.TOGGLESHORTCUTS );
                    break;

                default:
                    break;
            }
        }
    }
}