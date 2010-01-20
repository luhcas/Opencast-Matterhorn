<?xml version="1.0" encoding="UTF-8"?>


<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns2="http://searchui.opencastproject.org/">
	<xsl:template match="/">
	  <h2><xsl:value-of select="ns2:episode/dcTitle" /> by <xsl:value-of select="ns2:episode/dcCreator" /></h2>
	  
		<div id="title-bar" class="oc-title-bar">
			<p>Progressive Download Hybrid Player</p>
		</div>
		<div id="controls" class="oc-controls-tmp">

			<div id="oc-slider">
				<div class="matterhorn-progress-bar-background"></div>
				<div class="matterhorn-progress-bar"></div>
				<label id="seekLabel" for="slider_seek_Rail" class="fl-offScreen-hidden">Time</label>
				<div id="slider_seek_Rail" class="oc-slider-seek-Rail" title="Time 00:00:00"
					name="Time" value="Time" alt="Time">
					<button id="slider_seek_Thumb" class="oc-slider-seek-Thumb"></button>
				</div>
			</div>

			<table id="videocontrols-holder" class="oc-videocontrols-holder"
				cellspacing="0" cellpadding="0">
				<tr>
					<td align="left" valign="middle">
						<div id="simpleEdit">
							<span id="time-current" role="timer" class="editableText"
								name="Current Time" title="Current Time" value="00:00:00">00:00:00</span>
							<span id="editorContainer" name="Edit Current Time" title="Edit Current Time"
								value="Edit Current Time">
							 <input id="editField" type="text" maxlength="9" size="9" name="Edit Current Time HH:MM:SS" title="Edit Current Time HH:MM:SS" value="Edit Current Time HH:MM:SS" onKeyPress="Opencast.Player.editTimeKeyListener(event);" onchange="Opencast.Player.editTime();" onFocus="Opencast.Player.doPause();" onBlur="Opencast.Player.doPlay();" />
							</span>
							of
							<span id="time-total" role="timer" name="Total Time" title="Total Time"
								tabIndex="0">00:00:00</span>
						</div>
					</td>

					<td align="center" valign="middle" style="padding-left:0">
					    <button id="btn_skip_backward"  class="btn_skip_backward"  onmouseover="this.className='btn_skip_backward_over'"   onmouseout="this.className='btn_skip_backward_out'"  type="submit"  onclick="Opencast.Player.doSkipBackward();"     name="Skip Backward"  value="Skip Backward"   alt="Skip Backward"  title="Skip Backward" ></button>
          <button id="btn_rewind"         class="btn_rewind"         onmouseover="this.className='btn_rewind_over'"          onmouseout="this.className='btn_rewind_out'"         type="submit"  onclick="Opencast.Player.doRewind();"           name="Rewind"         value="Rewind"          alt="Rewind"         title="Rewind"        ></button>
          <button id="btn_play_pause"     class="btn_play"           onmouseover="Opencast.Player.PlayPauseMouseOver();"     onmouseout="Opencast.Player.PlayPauseMouseOut();"             type="submit"  onclick="Opencast.Player.doTogglePlayPause();"  name="Play"           value="Play"            alt="Play"           title="Play"          ></button>
          <button id="btn_fast_forward"   class="btn_fast_forward"   onmouseover="this.className='btn_fastForward_over'"     onmouseout="this.className='btn_fastForward_out'"    type="submit"  onclick="Opencast.Player.doFastForward();"      name="Fast Forward"   value="Fast Forward"    alt="Fast Forward"   title="Fast Forward"  ></button>
          <button id="btn_skip_forward"   class="btn_skip_forward"   onmouseover="this.className='btn_skip_forward_over'"    onmouseout="this.className='btn_skip_forward_out'"   type="submit"  onclick="Opencast.Player.doSkipForward();"      name="Skip Forward"   value="Skip Forward"    alt="Skip Forward"   title="Skip Forward"  ></button>
					</td>

        <td align="center" valign="middle"   style="padding-right:0" >  
          <button id="btn_volume"        class="oc-btn-volume-high"          type="submit"  onclick="Opencast.Player.doToggleVolume();"          name="Unmute"               value="Unmute"               alt="Unmute"              title="Unmute"              ></button>
        </td>
        
        <td align="center" valign="middle" style="padding-right:0">
          <!-- http://accessify.com/tools-and-wizards/accessibility-tools/aria/slider-generator/ -->
          <label id="volumeLabel" for="slider_volume_Rail" class="fl-offScreen-hidden">Volume</label>
          <div id="slider_volume_Rail" class="oc-slider-volume-Rail" title="Volume">
            <button id="slider_volume_Thumb" class="oc-slider-volume-Thumb"></button>
          </div>
        </td>
       
        <td align="center" valign="middle"   style="padding-right:5" >       
          <button id="btn_cc"      class="oc-btn-cc-off"          type="submit"  onclick="Opencast.Player.doToogleClosedCaptions();"          name="Closed Caption Off"               value="Closed Caption Off"               alt="Closed Caption Off"              title="Closed Caption Off"              ></button>
        </td>
				</tr>
			</table>
		</div>

    <div id="oc-video-display-container">
      <object codeBase="http://fpdownload.macromedia.com/get/flashplayer/current/swflash.cab" height="400" width="320" classid="clsid:D27CDB6E-AE6D-11cf-96B8-444553540000"> 
        <param name ="movie" value="engage-hybrid-player/Videodisplay.swf"/>
        <param name="quality" value="high"/>
        <param name="allowScriptAccess" value="always"/>
        <param name="bgcolor" value="#FFFFFF"/>
        <param name="allowFullScreen" value="true"/>
        <param name="FlashVars">
          <xsl:attribute name="value">bridgeName=b_Videodisplay&amp;video_url=<xsl:value-of select="ns2:episode/videoUrl"/>&amp;autoplay=false&amp;captions=dfxp/car.dfxp.xml</xsl:attribute>
        </param>
        <embed 
          width="400"
          height="320" 
          allowfullscreen="true" 
          type="application/x-shockwave-flash" 
          pluginspage="http://www.macromedia.com/go/getflashplayer" 
          quality="high" 
          allowScriptAccess="always" 
          src="engage-hybrid-player/Videodisplay.swf">
            <xsl:attribute name="flashvars">bridgeName=b_Videodisplay&amp;video_url=<xsl:value-of select="ns2:episode/videoUrl"/>&amp;autoplay=false&amp;captions=dfxp/car.dfxp.xml</xsl:attribute>
          </embed>
      </object>
    </div>
      
    <div id="info" >
      <button id="btn_info" class="oc_btn_info" onClick="Opencast.Player.toggleInfo();" type="submit" name="Keyboard Shortcuts Information"  value="Keyboard Shortcuts Information"   alt="Keyboard Shortcuts Information"  title="Keyboard Shortcuts Information" >Keyboard Shortcuts Information</button>
      <div id="infoBlock" class="oc_infoDisplayNone" >
          Press Control + Alt + I   = Toggle the keyboard shortcuts information between visible or unvisible.<br/>
          Press Control + Alt + P   = Toggle the video between pause or play.<br/>
          Press Control + Alt + S   = Stop the video.<br/>
          Press Control + Alt + M   = Toggle between mute or unmute the video.<br/>
          Press Control + Alt + U   = Volume up<br/>
          Press Control + Alt + D   = Volume down<br/>
          Press Control + Alt 0 - 9 = Seek the time slider<br/>
          Press Control + Alt + C   = Toggle between captions on or off.<br/>
          Press Control + Alt + F   = Forward the video.<br/>
          Press Control + Alt + R   = Rewind the video.<br/>
          Press Control + Alt + T   = the current time for the screen reader<br/>
          Press on Mac cmd + = to zoom in the player<br/>
          Press on Mac cmd - = to minimize the player<br/>
          Press on Windows strg + = to zoom in the player<br/>
          Press on Windows strg - = to minimize the player<br/>
      </div>
    </div>
		<br/>
          <xsl:value-of select="ns2:episode/dcAbstract" />
		<ul id="captions" aria-channel="main" aria-relevant="additions"
			aria-atomic="true" aria-live="polite" role="log" class="fl-offScreen-hidden">
		</ul>
	</xsl:template>
</xsl:stylesheet>