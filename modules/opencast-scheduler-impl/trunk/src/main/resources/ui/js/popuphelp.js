
function displayHelp( element, evt ) {
  resetHelp();
  // put title and text into help box
  var helpName = element.getAttribute("id");
  helpName = helpName.substring(5);
  document.getElementById("helpTitle").innerHTML = helpTexts[helpName][0];
  document.getElementById("helpText").innerHTML = helpTexts[helpName][1];
  
  // display helpbox
  var helpBox = document.getElementById("helpBox");
  
  helpBox.style.position = "absolute";
  
  if ( evt.pageX + helpBox.offsetWidth > window.innerWidth ) {
    helpBox.style.left = evt.pageX - (evt.pageX + helpBox.offsetWidth - window.innerWidth);
  } else {
    helpBox.style.left = evt.pageX;
  }
  
  if ( evt.pageY + helpBox.offsetHeight > window.innerHeight ) {
    helpBox.style.top = evt.pageY - (evt.pageY + helpBox.offsetHeight - window.innerHeight);
  } else {
    helpBox.style.top = evt.pageY;
  }
  helpBox.style.display="block";
  
}

function resetHelp() {
  var helpBox = document.getElementById("helpBox");
  helpBox.style.left = -10 - helpBox.offsetWidth;
  helpBox.style.top = -10 - helpBox.offsetHeight;
  helpBox.style.display = "none";
}