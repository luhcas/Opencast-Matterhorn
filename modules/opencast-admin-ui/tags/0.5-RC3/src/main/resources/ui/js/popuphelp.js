var popupHelp = popupHelp || {};

popupHelp.displayHelp = function( element, event ) {
  var id = element[0].id.split("_")[1];
  var title,text;
  if (popupHelp.helpTexts[id]) {
    title = popupHelp.helpTexts[id][0];
    text = popupHelp.helpTexts[id][1];
  } else {
    title = 'Sorry';
    text = "No help defined for field " + id;
  }
  $('#helpTitle').text(title);
  $('#helpText').text(text);
  var help = $('#helpBox');
  help.css({
           left:event.pageX,
           top:event.pageY
           });
  help.fadeIn('fast');
}

popupHelp.resetHelp = function() {
  $('#helpBox').fadeOut('fast');
}
