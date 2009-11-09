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
package org.opencast.engage.brick.videodisplay.control
{
	import bridge.ExternalFunction;
	
	import flash.external.ExternalInterface;
	
	import mx.core.Application;
	import mx.rpc.AsyncToken;
	import mx.rpc.IResponder;
	import mx.rpc.http.HTTPService;
	
	import org.opencast.engage.brick.videodisplay.business.VideodisplayDelegate;
	import org.opencast.engage.brick.videodisplay.control.event.ClosedCaptionsEvent;
	import org.opencast.engage.brick.videodisplay.control.event.DisplayCaptionEvent;
	import org.opencast.engage.brick.videodisplay.control.event.LoadDFXPXMLEvent;
	import org.opencast.engage.brick.videodisplay.control.event.ResizeVideodisplayEvent;
	import org.opencast.engage.brick.videodisplay.control.event.SetCurrentCaptionsEvent;
	import org.opencast.engage.brick.videodisplay.control.event.SetVolumeEvent;
	import org.opencast.engage.brick.videodisplay.control.event.VideoControlEvent;
	import org.opencast.engage.brick.videodisplay.control.responder.LoadDFXPXMLResponder;
	import org.opencast.engage.brick.videodisplay.model.VideodisplayModel;
	import org.opencast.engage.brick.videodisplay.state.PlayerState;
	import org.opencast.engage.brick.videodisplay.vo.CaptionSetVO;
	import org.opencast.engage.brick.videodisplay.vo.CaptionVO;
	import org.opencast.engage.brick.videodisplay.vo.LanguageVO;
	import org.swizframework.Swiz;
	import org.swizframework.controller.AbstractController;
	public class VideodisplayController extends AbstractController
	{
		/**  */
		[Autowire]
		public var model : VideodisplayModel;
		/**  */
		[Autowire]
		public var delegate : VideodisplayDelegate;
		
		private var lastPlayPauseState:String = "";
		
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
						    								model.player.play();
						  								model.currentPlayerState = PlayerState.PLAYING;
													   	currentPlayPauseState = PlayerState.PAUSING;
													   	break;
												
				case VideoControlEvent.PAUSE: 			if(model.player.playing)
		  				  									model.player.pause();
									  					model.currentPlayerState = PlayerState.PAUSING;
									  					currentPlayPauseState = PlayerState.PLAYING;
														break;
												
				case VideoControlEvent.STOP: 			if(model.player.playing)
												    		model.player.pause();
													  	model.player.seek(0);
													  	model.currentPlayerState = PlayerState.PAUSING;
						  								currentPlayPauseState = PlayerState.PLAYING;
														break;
												
				case VideoControlEvent.SKIPBACKWARD: 	model.player.seek( model.skipBackwardTime );
										    			break;
														
				case VideoControlEvent.REWIND: 			model.player.seek( model.currentPlayhead - model.rewindTime );
										    		
														break;
								
				case VideoControlEvent.FASTFORWARD: 	model.player.seek( model.currentPlayhead + model.fastForwardTime );
										    			
														break;
								
				case VideoControlEvent.SKIPFORWARD: 	model.player.seek( model.currentDuration - 1);
														model.player.pause();
										    			break;
			}
			
	      	try 
	      	{
	      		if(currentPlayPauseState != lastPlayPauseState)
	        	{
	        		ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
	          		lastPlayPauseState = currentPlayPauseState;
	        	}
	      	}	 
	      	catch (e:TypeError) {}
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
		* 
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
				if( model.ccBoolean == true )
				{
					// When the capions are differently, than send the new captiopns
					if(model.oldSubtitle != subtitle)
					{
						model.currentSubtitle = '';
						//ExternalInterface.call('setCaptions' , subtitle);
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
			* Application max width: 1080px, max Font Size ?, 1080/26 = 41px
			* Application min widht: 109px, min Font Size ?, 109/26 = 4px
			* 
			* */
			var divisor : int = 26;
			
			if( Application.application.width / divisor < 28 )
			{
				model.fontSizeCaptions = Application.application.width / divisor;
			}
			else
			{
				model.fontSizeCaptions = 28;
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
			if( model.ccBoolean == true )
			{
				model.ccBoolean = false;
			}else
			{
				model.ccBoolean = true;
			}
		}
	}
}
