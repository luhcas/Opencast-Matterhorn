package org.opencast.engage.brick.videodisplay.control.responder
{
	import mx.collections.ArrayCollection;
	import mx.rpc.IResponder;
	import org.opencast.engage.brick.videodisplay.model.VideodisplayModel;
	import org.opencast.engage.brick.videodisplay.vo.CaptionSetVO;
	import org.opencast.engage.brick.videodisplay.vo.CaptionVO;
	import org.swizframework.Swiz;
	public class LoadDFXPXMLResponder implements IResponder
	{
		/**  */
		[Autowire]
		public var model : VideodisplayModel;
		/** Constructor */
		public function LoadDFXPXMLResponder()
		{
			Swiz.autowire( this );
		}

		public function result(data : Object) : void
		{
			var captionSets : ArrayCollection = new ArrayCollection();
			var xData : XMLList = new XMLList(data.result);
			var divs : XMLList = xData.children().children();
			var div : XML;
			for each(div in divs)
			{
				var lang : String = div.attributes()[0];
				//div.attribute("xml:lang");  // Funktioniert nicht
				var style : String = div.attributes()[1];
				//div.attribute("style");
				if(lang != null)
				{
					var captionSet : CaptionSetVO = new CaptionSetVO();
					captionSet.lang = lang;
					captionSet.style = style;
					//  model.log += lang +" " + style + "\n";  
					var ps : XMLList = div.children();
					var p : XML;
					for each(p in ps)
					{
						var begin : String = p.attribute("begin");
						var end : String = p.attribute("end");
						var text : String;
						if (p.hasSimpleContent())
							text = p.toString();
						else
							text = p.children().toString();
						text = text.replace(new RegExp("\n" , "g") , "");
						// Replace All line breaks
						var caption : CaptionVO = new CaptionVO();
						caption.begin = stringToNumber(begin);
						caption.end = stringToNumber(end);
						caption.text = text;
						captionSet.captions.addItem(caption);
						//  model.log += caption.begin + " " + caption.end + " " + caption.text +"\n";
					}

					captionSets.addItem(captionSet);
					if(captionSet.lang == "de")                     // Make this CaptionSet the default set
						model.currentCaptionSet = captionSet.captions.toArray();
					// TODO: Refactor this to CaptionSetVO
				}
			}
		}

		/*
		*  TODO: Test this function by various Unit Tests
		*/
		public function stringToNumber(timestamp : String) : Number
		{
			var result : Number = 0.0;
			var parts : Array = timestamp.split(":");
			for(var i : int = parts.length - 1; i >= 0; i--)
			{
				switch (i)
				{
					case parts.length - 1 : var secondParts : Array = String(parts[i]).split(".");
					result += secondParts[0] * 1000;
					if(secondParts[1] != null)
						if(secondParts[1] > 10)
						result += secondParts[1] * 10;
					else if(secondParts[1] > 0)
						result += secondParts[1] * 100;
					break;
					case parts.length - 2 : result += parts[i] * 60000;
					break;
					case parts.length - 3 : result += parts[i] * 360000;
					break;
				}
			}

			return result;
		}

		public function fault(info : Object) : void
		{
		}
	}
}
