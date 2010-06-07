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
    
    import mx.core.Application;
    
    import org.opencast.engage.videodisplay.control.event.InitMediaPlayerEvent;
    import org.opencast.engage.videodisplay.control.util.OpencastMediaPlayer;
    import org.opencast.engage.videodisplay.model.VideodisplayModel;
    import org.opencast.engage.videodisplay.state.MediaState;
    import org.opencast.engage.videodisplay.state.PlayerState;
    import org.opencast.engage.videodisplay.state.VideoState;
    import org.osmf.elements.AudioElement;
    import org.osmf.elements.VideoElement;
    import org.osmf.media.MediaElement;
    import org.osmf.media.URLResource;
    import org.swizframework.Swiz;
    
    public class InitMediaPlayerCommand
    {
        [Autowire]
        public var model:VideodisplayModel;

        /** Constructor */
        public function InitMediaPlayerCommand()
        {
            Swiz.autowire( this );
        }

        /** execute
         *
         * init the video player.
         *
         * @eventType event:InitPlayerEvent
         * */
        public function execute( event:InitMediaPlayerEvent ):void
        {
			model.currentPlayerState = PlayerState.PAUSING;
            ExternalInterface.call( ExternalFunction.SETPLAYPAUSESTATE, PlayerState.PLAYING );
            
            // set the cover URL
            model.coverURL = event.coverURL;
           
            // Single Video/Audio
            if( event.mediaURLOne != '' && event.mediaURLTwo == '' )
            {
                
                
                //
                model.mediaPlayer = new OpencastMediaPlayer( VideoState.SINGLE );
                
                var pos:int = event.mediaURLOne.lastIndexOf( "." );
                var fileType:String = event.mediaURLOne.substring( pos + 1 );
                
                switch ( fileType )
                {
                    case "flv":
                    case "mp4":
                        var mediaElementVideo:MediaElement =  new VideoElement ( new URLResource( event.mediaURLOne ) );
                        model.mediaPlayer.setSingleMediaElement( mediaElementVideo );
                        break;

                    case "mp3":
                        var mediaElementAudio:MediaElement = new AudioElement( new URLResource( event.mediaURLOne ) );
                        model.mediaPlayer.setSingleMediaElement( mediaElementVideo );
                        var position:int = event.mediaURLOne.lastIndexOf( '/' );
                        model.audioURL = event.mediaURLOne.substring( position + 1 );
                        Application.application.bx_audio.startVisualization();
                        break;

                    default:
                        errorMessage( "Error", "TRACK COULD NOT BE FOUND" );
                        break;
                }
            }
            else if( event.mediaURLOne != '' && event.mediaURLTwo != '')
            {
            	if( event.mediaURLOne.charAt(0) == 'h' || event.mediaURLOne.charAt(0) == 'H' )
            	{
            	   model.mediaTypeOne = model.HTML;
            	}
            	else if( event.mediaURLOne.charAt(0) == 'r' || event.mediaURLOne.charAt(0) == 'R' )
            	{
            	    model.mediaTypeOne = model.RTMP;
            	}
            	
            	if( event.mediaURLTwo.charAt(0) == 'h' || event.mediaURLTwo.charAt(0) == 'H'  )
                {
                   model.mediaTypeTwo = model.HTML;
                }
                else if( event.mediaURLTwo.charAt(0) == 'r' || event.mediaURLTwo.charAt(0) == 'R')
                {
                    model.mediaTypeTwo = model.RTMP;
                }
            	
            	model.mediaPlayer = new OpencastMediaPlayer( VideoState.MULTI );
            	
            	//
            	var mediaElementVideoOne:MediaElement =  new VideoElement ( new URLResource( event.mediaURLOne ) );
                model.mediaPlayer.setMediaElementOne( mediaElementVideoOne );
            	
            	//
            	var mediaElementVideoTwo:MediaElement =  new VideoElement ( new URLResource( event.mediaURLTwo ) );
                model.mediaPlayer.setMediaElementTwo( mediaElementVideoTwo );
            }
            else
            {
                errorMessage( "Error", "TRACK COULD NOT BE FOUND" );
            }
        }
        
        /**
         * errorMessage
         *
         * Set the error Message and switch the stage.
         * 
         * @param String:name, String:message
         * */
        private function errorMessage( name:String, message:String ):void
        {
            model.mediaState = MediaState.ERROR;
            model.errorMessage = name;
            model.errorDetail = message;
        }
    }
}