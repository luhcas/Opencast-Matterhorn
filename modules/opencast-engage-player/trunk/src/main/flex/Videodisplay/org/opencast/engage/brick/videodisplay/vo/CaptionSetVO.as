package org.opencast.engage.brick.videodisplay.vo
{
	import mx.collections.ArrayCollection;
	/**
	* 	CaptionSetVO
	* 
	*	@author jemden
	*	@version 1.0
	*/
	[Bindable]
	public class CaptionSetVO
	{
		public function CaptionSetVO()
		{
			super();
			this.captions = new ArrayCollection();
		}

		public var captions : ArrayCollection;
		public var lang : String;
		public var style : String;
		public function toString() : String
		{
			var result : String = "";
			for(var i : int = 0;i < captions.length; i++)
			{
				result += lang + " " + captions.getItemAt(i).toString();
			}

			return result;
		}
	}
}
