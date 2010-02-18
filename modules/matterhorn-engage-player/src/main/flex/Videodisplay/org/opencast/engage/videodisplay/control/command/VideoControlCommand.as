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
                    if ( !model.player.playing )
                    {
                       model.player.play();
                       if( model.SECONDPLAYER == model.playerId )
                       {
                          model.player.volume = 0;
                       }
                    }
                    model.currentPlayerState = PlayerState.PLAYING;
                    currentPlayPauseState = PlayerState.PAUSING;
                    ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState );
                    break;

                case VideoControlEvent.PAUSE:
                    if ( model.player.playing )
                    {
                       model.player.pause();
                    }
                    model.currentPlayerState = PlayerState.PAUSING;
                    currentPlayPauseState = PlayerState.PLAYING;
                    ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState );
                    break;

                case VideoControlEvent.STOP:
                    if ( model.player.playing )
                    {
                        model.player.pause();
                        model.currentPlayerState = PlayerState.PAUSING;
                        currentPlayPauseState = PlayerState.PLAYING;
                        ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState );
                    }
                    model.player.seek( 0 );
                    break;

                case VideoControlEvent.SKIPBACKWARD:
                    model.player.seek( 0 );
                    break;

                case VideoControlEvent.REWIND:
                    if ( model.currentPlayhead + 1 > model.rewindTime )
                    {
                        if ( model.player.playing )
                        {
                            model.player.pause();
                            model.player.seek( model.currentPlayhead - model.rewindTime );
                            model.player.play();
                        }
                        else
                        {
                            model.player.seek( model.currentPlayhead - model.rewindTime );
                        }
                    }
                    break;

                case VideoControlEvent.FASTFORWARD:
                    model.player.seek( model.currentPlayhead + model.fastForwardTime );
                    break;

                case VideoControlEvent.SKIPFORWARD:
                    if ( model.player.playing )
                    {
                        model.player.seek( model.currentDuration - 1 );
                        model.player.pause();
                        model.currentPlayerState = PlayerState.PAUSING;
                        currentPlayPauseState = PlayerState.PLAYING;
                        ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState );
                    }
                    else
                    {
                        model.player.seek( model.currentDuration - 1 );
                    }
                    break;

                case VideoControlEvent.MUTE:
                    ExternalInterface.call( ExternalFunction.MUTE, model.playerId );
                    break;

                case VideoControlEvent.VOLUMEUP:
                    if ( model.player.volume != 1 )
                    {
                        model.player.volume = model.player.volume + skipVolume;
                    }
                    ExternalInterface.call( ExternalFunction.SETVOLUME, model.player.volume * percent, model.playerId );

                    if ( !model.ccButtonBool )
                    {
                        model.ccBoolean = false;
                        ExternalInterface.call( ExternalFunction.SETCAPTIONSBUTTON, false, model.playerId );
                    }
                    break;

                case VideoControlEvent.VOLUMEDOWN:
                    if ( model.player.volume != 0 )
                    {
                        model.player.volume = model.player.volume - skipVolume;
                    }
                    ExternalInterface.call( ExternalFunction.SETVOLUME, model.player.volume * percent, model.playerId );
                    break;

                case VideoControlEvent.SEEKZERO:
                    model.player.seek( ( model.currentDuration / 10 ) * 0 );
                    break;

                case VideoControlEvent.SEEKONE:
                    model.player.seek( ( model.currentDuration / 10 ) * 1 );
                    break;

                case VideoControlEvent.SEEKTWO:
                    model.player.seek( ( model.currentDuration / 10 ) * 2 );
                    break;

                case VideoControlEvent.SEEKTHREE:
                    model.player.seek( ( model.currentDuration / 10 ) * 3 );
                    break;

                case VideoControlEvent.SEEKFOUR:
                    model.player.seek( ( model.currentDuration / 10 ) * 4 );
                    break;

                case VideoControlEvent.SEEKFIVE:
                    model.player.seek( ( model.currentDuration / 10 ) * 5 );
                    break;

                case VideoControlEvent.SEEKSIX:
                    model.player.seek( ( model.currentDuration / 10 ) * 6 );
                    break;

                case VideoControlEvent.SEEKSEVEN:
                    model.player.seek( ( model.currentDuration / 10 ) * 7 );
                    break;

                case VideoControlEvent.SEEKEIGHT:
                    model.player.seek( ( model.currentDuration / 10 ) * 8 );
                    break;

                case VideoControlEvent.SEEKNINE:
                    model.player.seek( ( model.currentDuration / 10 ) * 9 );
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
                    ExternalInterface.call( ExternalFunction.SETCAPTIONSBUTTON, model.ccBoolean, model.playerId );
                    break;

                case VideoControlEvent.HEARTIMEINFO:
                    Swiz.dispatchEvent( new VideoControlEvent( VideoControlEvent.PAUSE ) );
                    ExternalInterface.call( ExternalFunction.HEARTIMEINFO, model.timeCode.getTC( model.currentPlayhead ), model.playerId );
                    break;

                case VideoControlEvent.INFORMATION:
                    ExternalInterface.call( ExternalFunction.TOGGLEINFO, model.playerId );
                    break;

                default:
                    break;
            }
        }
    }
}