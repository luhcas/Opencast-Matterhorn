package org.opencast.engage.brick.scrubber.control
{
	import org.opencast.engage.brick.scrubber.business.ScrubberDelegate;
	import org.opencast.engage.brick.scrubber.model.ScrubberModel;
	import org.swizframework.Swiz;
	import org.swizframework.controller.AbstractController;
	public class ScrubberController extends AbstractController
	{
		/**  */
		[Autowire]
		public var model : ScrubberModel;
		/**  */
		[Autowire]
		public var delegate : ScrubberDelegate;
		/** Constructor */
		public function ScrubberController()
		{
		}
	}
}
