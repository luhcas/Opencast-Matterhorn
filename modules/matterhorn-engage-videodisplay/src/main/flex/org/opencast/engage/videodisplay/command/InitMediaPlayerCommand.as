package org.opencast.engage.videodisplay.command
{
    import com.adobe.cairngorm.commands.Command;
    import com.adobe.cairngorm.control.CairngormEvent;
    
    import org.opencast.engage.videodisplay.model.VideodisplayModel;
    import org.opencast.engage.videodisplay.util.*;
    import mx.controls.Alert;
    

    public class InitMediaPlayerCommand implements Command
    {
    	private var model : VideodisplayModel = VideodisplayModel.getInstance();
    	
        public function InitMediaPlayerCommand()
        {
        	Alert.show("init?0");
        	model.parallelMedia = new ParallelMedia();
        }

        public function execute( event:CairngormEvent ):void
        {
        	Alert.show("init?1");
            // TODO Auto-generated add your code here
            model.parallelMedia = new ParallelMedia();
            
        }
    }
}
