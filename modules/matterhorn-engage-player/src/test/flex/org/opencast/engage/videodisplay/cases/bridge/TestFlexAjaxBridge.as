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
package org.opencast.engage.videodisplay.cases.bridge
{
	import org.flexunit.Assert;
	import org.opencast.engage.videodisplay.business.FlexAjaxBridge;
	import org.opencast.engage.videodisplay.model.VideodisplayModel;
	
	public class TestFlexAjaxBridge
	{
		private var videoDisplayModel:VideodisplayModel;
		private var flexAjaxBridge:FlexAjaxBridge;
		
		[Before]
        public function setUp():void
        {
            this.videoDisplayModel = new VideodisplayModel();
            this.flexAjaxBridge = new FlexAjaxBridge();
        }

        [After]
        public function tearDown():void
        {
            this.videoDisplayModel = null;
            this.flexAjaxBridge = null;
        }

		[Test]
		public function testFlexAjaxBridge_passCharCode():void
		{
			flexAjaxBridge.passCharCode(83);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'STOP');
			flexAjaxBridge.passCharCode(77);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'MUTE');
			flexAjaxBridge.passCharCode(85);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'VOLUMEUP');
			flexAjaxBridge.passCharCode(68);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'VOLUMEDOWN');
			flexAjaxBridge.passCharCode(48);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'SEEKZERO');
			flexAjaxBridge.passCharCode(49);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'SEEKONE');
			flexAjaxBridge.passCharCode(50);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'SEEKTWO');
			flexAjaxBridge.passCharCode(51);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'SEEKTHREE');
			flexAjaxBridge.passCharCode(52);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'SEEKFOUR');
			flexAjaxBridge.passCharCode(53);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'SEEKFIVE');
			flexAjaxBridge.passCharCode(54);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'SEEKSIX');
			flexAjaxBridge.passCharCode(55);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'SEEKSEVEN');
			flexAjaxBridge.passCharCode(56);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'SEEKEIGHT');
			flexAjaxBridge.passCharCode(57);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'SEEKNINE');
			flexAjaxBridge.passCharCode(67);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'CLOSEDCAPTIONS');
			flexAjaxBridge.passCharCode(82);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'REWIND');
			flexAjaxBridge.passCharCode(70);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'FASTFORWARD');
			flexAjaxBridge.passCharCode(84);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'HEARTIMEINFO');
			flexAjaxBridge.passCharCode(73);
			Assert.assertEquals(flexAjaxBridge.pressedKey, 'INFORMATION');
		}
		

	}
}
