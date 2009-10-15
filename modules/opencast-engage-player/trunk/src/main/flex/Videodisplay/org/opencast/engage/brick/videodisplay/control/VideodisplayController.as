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
	import flash.external.ExternalInterface;
	import mx.rpc.AsyncToken;
	import mx.rpc.IResponder;
	import mx.rpc.http.HTTPService;
	import org.opencast.engage.brick.videodisplay.business.VideodisplayDelegate;
	import org.opencast.engage.brick.videodisplay.control.event.DisplayCaptionEvent;
	import org.opencast.engage.brick.videodisplay.control.event.LoadDFXPXMLEvent;
	import org.opencast.engage.brick.videodisplay.control.event.SetVolumeEvent;
	import org.opencast.engage.brick.videodisplay.control.event.VideoControlEvent;
	import org.opencast.engage.brick.videodisplay.control.responder.LoadDFXPXMLResponder;
	import org.opencast.engage.brick.videodisplay.model.VideodisplayModel;
	import org.opencast.engage.brick.videodisplay.state.PlayerState;
	import org.opencast.engage.brick.videodisplay.vo.CaptionVO;
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
		/** Constructor */
		public function VideodisplayController()
		{
			Swiz.addEventListener( LoadDFXPXMLEvent.EVENT_NAME , loadDFXPXML );
			Swiz.addEventListener( SetVolumeEvent.EVENT_NAME , setVolume );
			Swiz.addEventListener( VideoControlEvent.EVENT_NAME , videoControl );
			Swiz.addEventListener( DisplayCaptionEvent.EVENT_NAME , displayCaption );
		}

		/** videoControl 
		* 
		* @eventType event:VideoControlEvent
		* */
		public function videoControl( event : VideoControlEvent ) : void
		{
			switch(event.videoControlType)
			{
				case VideoControlEvent.PLAY : if( !model.player.playing)
				model.player.play();
				model.currentPlayerState = PlayerState.PLAYING;
				break;
				case VideoControlEvent.PAUSE : if(model.player.playing)
				model.player.pause();
				model.currentPlayerState = PlayerState.PAUSING;
				break;
				case VideoControlEvent.STOP : if(model.player.playing)
				model.player.pause();
				model.player.seek(0);
				model.currentPlayerState = PlayerState.PAUSING;
				break;
			}
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

				// When the capions are differently, than send the new captiopns
				if(model.oldSubtitle != subtitle)
				{
					ExternalInterface.call('setCaptions' , subtitle);
					model.currentSubtitle = subtitle;
					model.oldSubtitle = subtitle;
				}
			}
		}
	}
}
