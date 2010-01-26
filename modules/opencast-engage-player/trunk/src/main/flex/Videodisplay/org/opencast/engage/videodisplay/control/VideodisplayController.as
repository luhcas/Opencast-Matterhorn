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
package org.opencast.engage.videodisplay.control
{
	import bridge.ExternalFunction;
	
	import flash.events.TimerEvent;
	import flash.external.ExternalInterface;
	import flash.utils.Timer;
	
	import mx.controls.Alert;
	import mx.core.Application;
	import mx.rpc.AsyncToken;
	import mx.rpc.IResponder;
	import mx.rpc.http.HTTPService;
	
	import org.opencast.engage.videodisplay.business.VideodisplayDelegate;
	import org.opencast.engage.videodisplay.control.event.ClosedCaptionsEvent;
	import org.opencast.engage.videodisplay.control.event.DisplayCaptionEvent;
	import org.opencast.engage.videodisplay.control.event.LoadDFXPXMLEvent;
	import org.opencast.engage.videodisplay.control.event.ResizeVideodisplayEvent;
	import org.opencast.engage.videodisplay.control.event.SetCurrentCaptionsEvent;
	import org.opencast.engage.videodisplay.control.event.SetVolumeEvent;
	import org.opencast.engage.videodisplay.control.event.VideoControlEvent;
	import org.opencast.engage.videodisplay.control.responder.LoadDFXPXMLResponder;
	import org.opencast.engage.videodisplay.model.VideodisplayModel;
	import org.opencast.engage.videodisplay.state.MediaState;
	import org.opencast.engage.videodisplay.state.PlayerState;
	import org.opencast.engage.videodisplay.vo.CaptionSetVO;
	import org.opencast.engage.videodisplay.vo.CaptionVO;
	import org.opencast.engage.videodisplay.vo.LanguageVO;
	import org.swizframework.Swiz;
	import org.swizframework.controller.AbstractController;
	public class VideodisplayController extends AbstractController
	{
		[Autowire]
		public var model : VideodisplayModel;
		
		[Autowire]
		public var delegate : VideodisplayDelegate;

		private var volume:Number = 1;
		private var skipVolume:Number = 0.1;
		private var percent:int = 100;

		/** Constructor */
		public function VideodisplayController()
		{
			Swiz.addEventListener( LoadDFXPXMLEvent.EVENT_NAME , loadDFXPXML );
			Swiz.addEventListener( SetVolumeEvent.EVENT_NAME , setVolume );
			Swiz.addEventListener( VideoControlEvent.EVENT_NAME , videoControl );
			Swiz.addEventListener( DisplayCaptionEvent.EVENT_NAME , displayCaption );
			Swiz.addEventListener( ResizeVideodisplayEvent.EVENT_NAME , resizeVideodisplay );
			Swiz.addEventListener( SetCurrentCaptionsEvent.EVENT_NAME , setCurrentCaptions );
			Swiz.addEventListener( ClosedCaptionsEvent.EVENT_NAME , closedCaptions );
		}

		/** videoControl 
		* 
		* @eventType event:VideoControlEvent
		* */
		public function videoControl( event : VideoControlEvent ) : void
		{
		  	var currentPlayPauseState:String;

		  	switch(event.videoControlType)
            {
				case VideoControlEvent.PLAY:			if( !model.player.playing)
				                                        {
						    								model.player.play();
						    								
						    							}
						    							model.currentPlayerState = PlayerState.PLAYING;
                                                        currentPlayPauseState = PlayerState.PAUSING;
                                                        ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
						  								break;
												
				case VideoControlEvent.PAUSE: 			if(model.player.playing)
		  				  								{
		  				  									model.player.pause();
		  				  									
		  				  								}
		  				  								model.currentPlayerState = PlayerState.PAUSING;
                                                        currentPlayPauseState = PlayerState.PLAYING;
                                                        ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
									  					break;
												
				case VideoControlEvent.STOP: 			if(model.player.playing)
                                                        {
															model.player.pause();
															model.currentPlayerState = PlayerState.PAUSING;
                                                            currentPlayPauseState = PlayerState.PLAYING;
                                                            ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
													    }
													  	model.player.seek(0);
													  	break;
												
				case VideoControlEvent.SKIPBACKWARD: 	model.player.seek( 0 );
				                                        break;
														
				case VideoControlEvent.REWIND: 			if( model.currentPlayhead +1 > model.rewindTime )
				                                        {
				                                            if(model.player.playing)
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
								
				case VideoControlEvent.FASTFORWARD: 	model.player.seek( model.currentPlayhead + model.fastForwardTime );
										    			break;
								
				case VideoControlEvent.SKIPFORWARD: 	
														if(model.player.playing)
                                                        {
		  				  									model.player.seek( model.currentDuration - 1);
		  				  									model.player.pause();
		  				  									model.currentPlayerState = PlayerState.PAUSING;
                                                            currentPlayPauseState = PlayerState.PLAYING;
                                                            ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
									  			        }
									  			        else
									  			        {
									  			          model.player.seek( model.currentDuration - 1);
									  			        }
									  					
									  					break;
									  					
				case VideoControlEvent.MUTE:            ExternalInterface.call(ExternalFunction.MUTE, '');
                                                        break;
                                                        
                 case VideoControlEvent.VOLUMEUP:		if( model.player.volume != 1 )
														{
															model.player.volume = model.player.volume + skipVolume;
														}
														ExternalInterface.call(ExternalFunction.SETVOLUME, model.player.volume * percent);
														
														if(!model.ccButtonBool)
														{
														    model.ccBoolean = false;
                                                            ExternalInterface.call(ExternalFunction.SETCAPTIONSBUTTON, false);
														}
														
														break;
														
				case VideoControlEvent.VOLUMEDOWN:		if( model.player.volume != 0 )
														{
															model.player.volume = model.player.volume - skipVolume;
														}
														ExternalInterface.call(ExternalFunction.SETVOLUME, model.player.volume * percent);
														break;
														
				case VideoControlEvent.SEEKZERO:        model.player.seek((model.currentDuration / 10) * 0);
                                                        break;
                                                        
                case VideoControlEvent.SEEKONE:         model.player.seek((model.currentDuration / 10) * 1);
                                                        break;
                
                case VideoControlEvent.SEEKTWO:         model.player.seek((model.currentDuration / 10) * 2);
                                                        break;                                                       

                case VideoControlEvent.SEEKTHREE:       model.player.seek((model.currentDuration / 10) * 3);
                                                        break;
                
                case VideoControlEvent.SEEKFOUR:        model.player.seek((model.currentDuration / 10) * 4);
                                                        break;                                                        
                                                        
                case VideoControlEvent.SEEKFIVE:        model.player.seek((model.currentDuration / 10) * 5);
                                                        break;                                                        
                                                        
                case VideoControlEvent.SEEKSIX:         model.player.seek((model.currentDuration / 10) * 6);
                                                        break;
                                                        
                case VideoControlEvent.SEEKSEVEN:       model.player.seek((model.currentDuration / 10) * 7);
                                                        break;                                                        
                                                        
                case VideoControlEvent.SEEKEIGHT:       model.player.seek((model.currentDuration / 10) * 8);
                                                        break;                                                        
                                                        
                case VideoControlEvent.SEEKNINE:        model.player.seek((model.currentDuration / 10) * 9);
                                                        break;                                                     
                
                case VideoControlEvent.CLOSEDCAPTIONS:  if(model.ccBoolean)
                                                        {
                                                        	Swiz.dispatchEvent( new ClosedCaptionsEvent(false) );
                                                        	model.ccButtonBool = false;
                                                        }
                                                        else
                                                        {
                                                        	Swiz.dispatchEvent( new ClosedCaptionsEvent(true) );
                                                        	model.ccButtonBool = true;
                                                        }
                                                        ExternalInterface.call(ExternalFunction.SETCAPTIONSBUTTON, model.ccBoolean);
                                                        break;         
                                                   
                case VideoControlEvent.HEARTIMEINFO:    Swiz.dispatchEvent( new VideoControlEvent(VideoControlEvent.PAUSE ));
                                                        ExternalInterface.call(ExternalFunction.HEARTIMEINFO, model.timeCode.getTC( model.currentPlayhead) );
                                                        break;  
                                                        
                case VideoControlEvent.INFORMATION:     ExternalInterface.call(ExternalFunction.TOGGLEINFO, '' );           
                                                        break;   
                                                        
                default:                                break;
               
             }
		}
		
		/** setVolume 
        * 
        * When the timer ist at the end, play the video.
        * 
        * @eventType event:TimerEvent
        * */
		public function timerComplete( event:TimerEvent ):void 
        {
            Swiz.dispatchEvent( new VideoControlEvent(VideoControlEvent.PLAY));
            
        }

		/** setVolume 
		* 
		* @eventType event:SetVolumeEvent
		* */
		public function setVolume( event : SetVolumeEvent ) : void
		{
			model.player.volume = event.volume;
		}

		/** loadDFXP.XML 
		* 
		* @eventType event:LoadDFXPXMLEvent
		* */
		public function loadDFXPXML( event : LoadDFXPXMLEvent ) : void
		{
			var responder : IResponder = new LoadDFXPXMLResponder();
			var service : HTTPService = new HTTPService();
			service.resultFormat = "e4x";
			service.url = event.source;
			var token : AsyncToken = service.send();
			token.addResponder(responder);
		}

		/** displayCaption 
		* 
		* The event give the new Position in the Video. Find the right captions in the currentCaptionSet an display the captions with the ExternalInterface.
		* 
		* @eventType event:DisplayCaptionEvent
		* */
		public function displayCaption( event : DisplayCaptionEvent ) : void
		{
			var time : Number = event.position * 1000;
			var tmpCaption : CaptionVO = new CaptionVO();
			var lastPos : int = 0;
			var subtitle : String = '';
			// Find the captions
			if( model.currentCaptionSet != null)
			{
				for( var i : int = 0; i < model.currentCaptionSet.length; i++)
				{
					tmpCaption = CaptionVO(model.currentCaptionSet[(lastPos + i) % model.currentCaptionSet.length]);
					if(tmpCaption.begin < time && time < tmpCaption.end)
					{
						lastPos += i;
						
						subtitle = tmpCaption.text;
						
						break;
					}
				}
				
				// When the learner will see the captions	
				if( model.ccBoolean )
				{
					// When the capions are different, than send the new captions
					if( model.oldSubtitle != subtitle )
					{
						model.currentSubtitle = '';
						ExternalInterface.call(ExternalFunction.SETCAPTIONS , subtitle);
						model.currentSubtitle = subtitle;
						model.oldSubtitle = subtitle;
						
					}
				}
				else
				{
					model.currentSubtitle = '';
					model.oldSubtitle = 'default';
					ExternalInterface.call(ExternalFunction.SETCAPTIONS , '');
				}
			}
		}

		/** resizeVideodisplay 
		* 
		* When the learner resize the Videodisplay in the Browser
		*  
		* */
		public function resizeVideodisplay( event : ResizeVideodisplayEvent ) : void
		{
			/**
			* Application max width: 1194px, max Font Size ?, 1194/33 = 36px ( 36 > 20 ) = 20px
			* Application min widht: 231px, min Font Size ?, 231/33 = 7px
			* 
			* */
			var divisor : int = 33;
			
			if( Application.application.width == 400 )
			{
				model.fontSizeCaptions = 12; 
			}
			else
			{
			    if( Application.application.width / divisor < 20 )
	            {
	                model.fontSizeCaptions = Application.application.width / divisor;
	            }
	            else
	            {
	                model.fontSizeCaptions = 20;
	            }
			}
		}

		/** setCurrentCaptions 
		* 
		* When the learner change the subtitltes from the ComboBox
		*  
		* */
		public function setCurrentCaptions( event : SetCurrentCaptionsEvent ) : void
		{
			for(var i : int; i < model.languages.length; i++)
			{
				if( LanguageVO( model.languages.getItemAt(i)).long_name == event.language  )
				{
					for(var j : int = 0; j < model.captionSets.length; j++)
					{
						if( CaptionSetVO( model.captionSets.getItemAt(j) ).lang == LanguageVO( model.languages.getItemAt(i)).short_name )
						{
							// set current capitons
							model.currentCaptionSet = CaptionSetVO( model.captionSets.getItemAt(j) ).captions.toArray();
						}
					}
				}
			}
		}

		/** closedCaptions 
		* 
		* When the learner toggle the cc button
		*  
		* */
		public function closedCaptions( event : ClosedCaptionsEvent ) : void
		{
			model.ccBoolean = event.ccBoolean;
		}
	}
}
