package org.opencast.engage.videodisplay.command
{
   import com.adobe.cairngorm.commands.Command;
   import com.adobe.cairngorm.control.CairngormEvent;
    import mx.controls.Alert;

      public class MyCommand implements Command
   {
      public function MyCommand()
      {
      }
 
      public function execute( event : CairngormEvent ) : void
      {
         // TODO Auto-generated add your code here
          Alert.show("Do you want to save your changes?");
      }
   }
}
