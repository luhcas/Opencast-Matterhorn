var AdminUI = {} || AdminUI;

AdminUI.internationalize = function(obj, prefix){
  for(var i in obj){
    if(typeof obj[i] == 'object'){
      AdminUI.internationalize(obj[i], prefix + '_' + i);
    }else if(typeof obj[i] == 'string'){
      var id = '#' + prefix + '_' + i;
      if($(id).length){
        $(id).text(obj[i]);
      }
    }
  }
}

AdminUI.log = function(){
  if(window.console){
    try{
      window.console && console.log.apply(console,Array.prototype.slice.call(arguments));
    }catch(e){
      console.log(e);
    }
  }
}
