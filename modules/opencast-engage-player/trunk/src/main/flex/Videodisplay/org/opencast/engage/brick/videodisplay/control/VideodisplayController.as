package org.opencast.engage.brick.videodisplay.control
{
	import mx.rpc.AsyncToken;
	import mx.rpc.IResponder;
	import mx.rpc.http.HTTPService;
	import org.opencast.engage.brick.videodisplay.business.VideodisplayDelegate;
	import org.opencast.engage.brick.videodisplay.control.event.LoadDFXPXMLEvent;
	import org.opencast.engage.brick.videodisplay.control.event.SetVolumeEvent;
	import org.opencast.engage.brick.videodisplay.control.event.VideoControlEvent;
	import org.opencast.engage.brick.videodisplay.control.responder.LoadDFXPXMLResponder;
	import org.opencast.engage.brick.videodisplay.model.VideodisplayModel;
	import org.opencast.engage.brick.videodisplay.state.PlayerState;
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
			Swiz.addEventListener(LoadDFXPXMLEvent.EVENT_NAME , loadDFXPXML );
			Swiz.addEventListener(SetVolumeEvent.EVENT_NAME , setVolume );
			Swiz.addEventListener(VideoControlEvent.EVENT_NAME , videoControl );
		}

		/** videoControl */
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

		/** setVolume */
		public function setVolume( event : SetVolumeEvent ) : void
		{
			model.player.volume = event.volume;
		}

		/** loadDFXP.XML */
		public function loadDFXPXML( event : LoadDFXPXMLEvent ) : void
		{
			var responder : IResponder = new LoadDFXPXMLResponder();
			var service : HTTPService = new HTTPService();
			service.resultFormat = "e4x";
			service.url = event.source;
			var token : AsyncToken = service.send();
			token.addResponder(responder);
		}
	}
}
