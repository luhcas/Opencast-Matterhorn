package org.opencast.engage.videodisplay.model
{
	import com.adobe.cairngorm.model.ModelLocator;
	
	import org.opencast.engage.videodisplay.util.ParallelMedia;
	import org.osmf.containers.MediaContainer;
	import org.osmf.media.MediaPlayer;
	
	[Bindable]
	public class VideodisplayModel implements com.adobe.cairngorm.model.ModelLocator
	{
		private static var videodisplayModel : VideodisplayModel;
		
		public static function getInstance() : VideodisplayModel
        {
            if ( videodisplayModel == null )
                videodisplayModel = new VideodisplayModel();
                
            return videodisplayModel;
       }
		
		public function VideodisplayModel()
		{
			if ( videodisplayModel != null )
                throw new Error( "Only one ModelLocator instance should be instantiated." );    
		}
		
		public var testvariable : String = "LMAA";
		
		public var videoURLOne : String = "";
		public var videoURLTwo : String = "";
		
		public var parallelMedia : ParallelMedia;
		public var parallelMediaContainer: MediaContainer;
		public var player : MediaPlayer;

	}
}