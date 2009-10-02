package org.opencast.engage.brick.scrubber.control.responder
{
	import mx.rpc.events.ResultEvent;
	public interface IResponder
	{
		/**  */
		function result( result : ResultEvent ) : void;
		/**  */
		function fault( info : Object ) : void;
	}
}
