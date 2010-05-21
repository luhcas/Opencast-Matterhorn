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
package org.opencast.engage.videodisplay.control.command
{
    import mx.controls.Alert;
    import mx.core.Application;
    
    import org.opencast.engage.videodisplay.control.event.ResizeVideodisplayEvent;
    import org.opencast.engage.videodisplay.model.VideodisplayModel;
    import org.swizframework.Swiz;

    public class ResizeVideodisplayCommand
    {
        [Autowire]
        public var model:VideodisplayModel;

        /** Constructor */
        public function ResizeVideodisplayCommand()
        {
            Swiz.autowire( this );
        }

        /** execute
         *
         * When the learner resize the Videodisplay in the browser
         *
         * @eventType event:ResizeVideodisplayEvent
         * */
        public function execute( event:ResizeVideodisplayEvent ):void
        {
             if( model.mediaContainer != null)
             {
             	 model.mediaContainer.height = Application.application.height;
                 model.mediaContainer.width = Application.application.width;
                 
             }
            
            
            
            /**
             * Application max width: 1194px, max Font Size ?, 1194/33 = 36px ( 36 > 20 ) = 20px
             * Application min widht: 231px, min Font Size ?, 231/33 = 7px
             *
             * */
            var divisor:int = 50;
            
            if ( Application.application.width == 400 )
            {
                model.fontSizeCaptions = 12;
            }
            else
            {
                if ( Application.application.width / divisor < 14 )
                {
                    model.fontSizeCaptions = 14;
                }
                else if( Application.application.width / divisor >= 14 && Application.application.width / divisor <= 20 )
                {
                    model.fontSizeCaptions = (Application.application.width / divisor ) -1;
                }
                else if( Application.application.width / divisor > 20 )
                {
                	model.fontSizeCaptions = 20;
                }
            }
            
        }
    }
}