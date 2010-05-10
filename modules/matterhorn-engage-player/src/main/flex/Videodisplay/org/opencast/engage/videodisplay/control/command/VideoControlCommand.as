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
    
    import org.opencast.engage.videodisplay.control.event.ClosedCaptionsEvent;
    import org.opencast.engage.videodisplay.control.event.VideoControlEvent;
    import org.opencast.engage.videodisplay.model.VideodisplayModel;
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
            var playState:Boolean = false;
           

            switch ( event.videoControlType )
            {
                case VideoControlEvent.PLAY:
                	if( !model.mediaPlayer.playing )
                	{
                		model.mediaPlayer.play();
                	}
                	model.currentPlayerState = PlayerState.PLAYING;
                    currentPlayPauseState = PlayerState.PAUSING;
                    ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState );
                	break;

                case VideoControlEvent.PAUSE:
                    if( model.mediaPlayer.playing )
                    {
                        model.mediaPlayer.pause();
                    }
                    model.currentPlayerState = PlayerState.PAUSING;
                    currentPlayPauseState = PlayerState.PLAYING;
                    ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState );
                    break;

                case VideoControlEvent.STOP:
                    if( model.mediaPlayer.playing )
                    {
                        model.mediaPlayer.pause();
                        model.mediaPlayer.seek( 0 );
                        model.currentPlayerState = PlayerState.PAUSING;
                        currentPlayPauseState = PlayerState.PLAYING;
                        ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState );
                    }
                    else
                    {
                        model.mediaPlayer.seek( 0 );
                    }
                    break;
                    
                case VideoControlEvent.SKIPBACKWARD:
                	
                	playState = model.mediaPlayer.playing;
                	
                	if( model.mediaPlayer.playing )
                	{
                	   model.mediaPlayer.pause();
                	}
                	
                    model.mediaPlayer.seek(0);
                	
                	if( playState )
                	{
                	   model.mediaPlayer.play();
                	}
                	break;

                case VideoControlEvent.REWIND:
                    
                    if( model.mediaPlayer.playing )
                    {
                        model.mediaPlayer.pause();
                    }
                    
                    if( model.currentPlayhead - model.rewindTime >= 0 )
                    {
                        model.mediaPlayer.seek( model.currentPlayhead - model.rewindTime );
                    }
                    else
                    {
                        model.mediaPlayer.seek( 0 );
                    }
                    break;

                case VideoControlEvent.FASTFORWARD:
                
                	if( model.mediaPlayer.playing )
                    {
                        model.mediaPlayer.pause();
                    }
                    if( model.currentPlayhead + model.fastForwardTime > model.currentDuration )
                    {
                        model.mediaPlayer.seek( model.currentDuration );
                    }
                    else
                    {
                        model.mediaPlayer.seek( model.currentPlayhead + model.fastForwardTime );
                    }
                    break;

                case VideoControlEvent.SKIPFORWARD:
                    playState = model.mediaPlayer.playing;
                    
                    if( model.mediaPlayer.playing )
                    {
                       model.mediaPlayer.pause();
                    }
                    
                    model.mediaPlayer.seek(0);
                    
                    if( playState )
                    {
                       model.mediaPlayer.play();
                    }
                    break;
                    
                case VideoControlEvent.MUTE:
                
                	if( model.mediaPlayer.muted )
                	{
                	   model.mediaPlayer.muted = false;
                	}
                	else
                	{
                	    model.mediaPlayer.muted = true;
                	}
                	break;

                case VideoControlEvent.VOLUMEUP:
                
                	if ( model.mediaPlayer.volume != 1 )
                    {
                        model.mediaPlayer.volume = model.mediaPlayer.volume + skipVolume;
                    }
                    ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, Math.round(model.mediaPlayer.volume * percent) );
            	    break;

                case VideoControlEvent.VOLUMEDOWN:
                
                	if ( model.mediaPlayer.volume != 0 )
                    {
                        model.mediaPlayer.volume = model.mediaPlayer.volume - skipVolume;
                        if( model.mediaPlayer.volume < 0 )
                        {
                            model.mediaPlayer.volume = 0;
                        }
                    }
                    ExternalInterface.call( ExternalFunction.SETVOLUMESLIDER, Math.round(model.mediaPlayer.volume * percent) );
            	    break;

                case VideoControlEvent.SEEKZERO:
                    model.mediaPlayer.seek( ( model.currentDuration / 10 ) * 0 );
                    break;

                case VideoControlEvent.SEEKONE:
                    model.mediaPlayer.seek( ( model.currentDuration / 10 ) * 1 );
                    break;

                case VideoControlEvent.SEEKTWO:
                    
                    model.mediaPlayer.seek( ( model.currentDuration / 10 ) * 2 );
                    break;

                case VideoControlEvent.SEEKTHREE:
                    
                    model.mediaPlayer.seek( ( model.currentDuration / 10 ) * 3 );
                    break;

                case VideoControlEvent.SEEKFOUR:
                    model.mediaPlayer.seek( ( model.currentDuration / 10 ) * 4 );
                    break;

                case VideoControlEvent.SEEKFIVE:
                    
                    model.mediaPlayer.seek( ( model.currentDuration / 10 ) * 5 );
                    break;

                case VideoControlEvent.SEEKSIX:
                    model.mediaPlayer.seek( ( model.currentDuration / 10 ) * 6 );
                    break;

                case VideoControlEvent.SEEKSEVEN:
                   
                    model.mediaPlayer.seek( ( model.currentDuration / 10 ) * 7 );
                    break;

                case VideoControlEvent.SEEKEIGHT:
                    
                    model.mediaPlayer.seek( ( model.currentDuration / 10 ) * 8 );
                    break;

                case VideoControlEvent.SEEKNINE:
                    model.mediaPlayer.seek( ( model.currentDuration / 10 ) * 9 );
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