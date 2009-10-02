package org.opencast.engage.brick.videodisplay.control.event
{
	import flash.events.Event;
	/**
	* 	LoadDFXPXMLEvent
	* 
	*	@author jemden
	*	@version 1.0
	*/
	public class LoadDFXPXMLEvent extends Event
	{
		public static var EVENT_NAME : String = 'LoadDFXPXML';
		private var _source : String;
		public function LoadDFXPXMLEvent(source : String)
		{
			super(EVENT_NAME);
			_source = source;
		}

		public function get source() : String
		{
			return _source;
		}
	}
}
