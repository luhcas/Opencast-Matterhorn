var playing = "playing";
var pausing = "pausing";
var currentPlayPauseState = pausing;

function doPlay() {
  Videodisplay.play();
}

function doPause() {
  Videodisplay.pause();
}

function doSkipBackward() {
  Videodisplay.skipBackward();
}

function doRewind() {
  Videodisplay.rewind();
}

function doFastForward() {
  Videodisplay.fastForward();
}

function doSkipForward() {
  Videodisplay.skipForward();
}

function doTogglePlayPause() { //Checking if btn_play_pause is "play"
  if (currentPlayPauseState == pausing) { //Changing the volume to 1.0 and the value of the button of btn_volume to "unmute"
    setPlayPauseState(playing);
    doPlay();
    return;
  } else { //Changing the volume to 0.0 and the value of the button of btn_volume to "mute"
    setPlayPauseState(pausing);
    doPause();
    return;
  }
}

function setPlayPauseState(state) {
  if (state == playing) {
    document.getElementById("btn_play_pause").value = "Play";
    document.getElementById("btn_play_pause").alt = "Play";
    document.getElementById("btn_play_pause").src = "./icons/play---green.png";
    currentPlayPauseState = pausing;
  } else {
    document.getElementById("btn_play_pause").value = "Pause";
    document.getElementById("btn_play_pause").alt = "Pause";
    document.getElementById("btn_play_pause").src = "./icons/pause---green.png";
    currentPlayPauseState = playing;
  }
}

function doStop() {
  Videodisplay.stop();
}

function doSeek(time) {
  $('#slider').slider('value', time);
  Videodisplay.seek(time);
}

function setPlayhead(newPosition) {
  $('#slider').slider('value', newPosition);
}

function doSetVolume(value) {
  Videodisplay.setVolume(value);
}

function setVolume(newVolume) { //      $('#volume_slider').slider('value', newVolume);
}

function doToggleVolume() {
  var mute = "Mute";
  var umute = "Unmute"; //Checking if btn_volume is "mute"
  if (document.getElementById("btn_volume").value == mute) { //Changing the volume to 1.0 and the value of the button of btn_volume to "unmute"
    document.getElementById("btn_volume").value = "Unmute";
    document.getElementById("btn_volume").alt = "Unmute";
    document.getElementById("btn_volume").src = "./icons/volume---mute.png";
    doSetVolume(0.0);
    return
  } else { //Changing the volume to 0.0 and the value of the button of btn_volume to "mute"
    document.getElementById("btn_volume").value = "Mute";
    document.getElementById("btn_volume").alt = "Mute";
    document.getElementById("btn_volume").src = "./icons/volume---high.png";
    doSetVolume(1.0);
    return
  }
}

function doToogleClosedCaptions() {
  var on = "cc on";
  var off = "cc off"; //Checking if btn_cc is "CC off"
  if (document.getElementById("btn_cc").value == off) {
    document.getElementById("btn_cc").value = "cc on";
    document.getElementById("btn_cc").alt = "cc on";
    document.getElementById("btn_cc").src = "./icons/cc_on.png";
    doClosedCaptions(true);
    return;
  } else {
    document.getElementById("btn_cc").value = "cc off";
    document.getElementById("btn_cc").alt = "cc off";
    document.getElementById("btn_cc").src = "./icons/cc_off.png";
    doClosedCaptions(false);
    return;
  }
}

function setCurrentTime(text) {
  document.getElementById("time-current").innerHTML = text;
}

function setTotalTime(text) {
  document.getElementById("time-total").innerHTML = text;
}

function setDuration(time) {
  $('#slider').slider('option', 'max', time);
}

function setProgress(value) {
  $('.matterhorn-progress-bar').css("width", value + "%");
}

function doClosedCaptions(cc) {
  Videodisplay.closedCaptions(cc);
}

function setCaptions(text) {
  document.getElementById("captions").innerHTML = text;
}

function setLangugageComboBox(languageComboBox) {
  for (var i = 0; i < languageComboBox.length; i++) {
    var option = document.createElement('option');
    option.text = languageComboBox[i];
    var cb_item = document.getElementById("cb_lang");
    try {
      cb_item.add(option, null); // standards compliant
    } catch(ex) {
      cb_item.add(option); // IE only
    }
  }
}

function doSetLanguage(value) {
  Videodisplay.setLanguage(value);
}

$(document).ready(function () {
  $("#slider").slider();
  $('#slider').slider('option', 'animate', false);
  $('#slider').slider('option', 'min', 0);
  $('#slider').bind('slide', function (event, ui) {
    Videodisplay.seek(ui.value);
  });
  $('#volume_slider').slider();
  $('#volume_slider').slider('option', 'min', 0);
  $('#volume_slider').slider('option', 'max', 100);
  $('#volume_slider').slider({
    steps: 10
  });
  $('#volume_slider').slider('value', 100);
  $('#volume_slider').bind('slide', function (event, ui) {
    doSetVolume(ui.value / 100);
  });
});