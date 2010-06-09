/* @namespace Holds functions and properites related to all Admin UIs. */
var AdminUI = {} || AdminUI;
/* @namespace Holds functions and properites related to forms used in Admin UIs. */
var AdminForm = {} || AdminForm;

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

$(document).ready(
  AdminUI.log = function(){
    if(window.console){
      try{
        window.console && console.log.apply(console,Array.prototype.slice.call(arguments));
      }catch(e){
        console.log(e);
      }
    }
  }
);

AdminForm.components = {};

AdminForm.Manager = function(rootElm, rootNs){

};

$.extend(AdminForm.Manager.prototype, {
  rootElm: "",
  rootNs: "",
  serialize: function(){
    if(this.validate()){
      var doc = this.createDoc();
      var mdlist = doc.createElement('metadata_list');
      for(var c in AdminForm.components){
        AdminForm.components[c].toNode(mdlist);
      }
    }
  },
  populate: function(){

  },
  validate: function(){
    return true;
  },
  createDoc: function(){
    var doc = null;
    //Create a DOM Document, methods vary between browsers, e.g. IE and Firefox
    if(document.implementation && document.implementation.createDocument) { //Firefox, Opera, Safari, Chrome, etc.
      doc = document.implementation.createDocument(this.rootNS, this.rootEl, null);
    } else { // IE
      doc = new ActiveXObject('MSXML2.DOMDocument');
      doc.loadXML('<' + this.rootEl + ' xmlns="' + this.rootNS + '"></' + this.rootEl + '>');
    }
    return doc;
  }
});

/* @class The Component class is a collection of form elements and associated functions for use
 * with the AdminForm.Manager. It provides basic implementations for setting, getting, displaying,
 * and XMLifying the form elements.
 */
AdminForm.Component = function Component(fields, props, funcs){
  this.setFields(fields);
  this.setFunctions(funcs);
  this.setProperties(props);
};

$.extend(AdminForm.Component.prototype, {
  /* @lends AdminForm.Component.prototype */
  fields:     [],
  errorField: "",
  label:      "",
  properties: [],
  required:   false,
  value:      null,
  /** 
   *  Sets the fields from an array of element ids.
   *  @param {String[]} Array of element ids
   */
  setFields:  function(f){
    if(typeof f == 'string') { //If a single field is specified, wrap in an array.
      f = [f];
    }
    AdminUI.log(f);
    for(var k in f) {
      var e = $('#' + f[k]);
      AdminUI.log(f[k],e);
      if(e[0]){
        this.fields[f[k]] = e;
      }
    }
  },
  /** 
   *  Extends Component with additional methods and/or properties
   *  @param {Object} An object literal or instance with which to extend Component
   */
  setFunctions: function(funcs){
    AdminUI.log(funcs);
    if(funcs && typeof funcs == 'object'){
      AdminUI.log('test');
      $.extend(this, funcs);
    }
  },
  /** 
   *  Sets the Component properties, arbitrary properties are added to properties array
   *  @param {Object} Key/Value pair of properties
   */
  setProperties: function(props){
    if(typeof props == 'object') {
      for(var f in props) {
        switch(f){
          case 'errorField':
            this.errorField = props[f];
            break;
          case 'label':
            this.label = props[f];
            break;
          case 'required':
            this.required = props[f];
            break;
          default:
            this.properties[f] = props[f];
        }
      }
    }
  },
  /** 
   *  Default getValue function
   *  @return A comma seperated string of all element values.
   *  @type String
   */
  getValue: function(){
    if(this.validate()){
      var values = [];
      for(var el in this.fields){
        var e = this.fields[el];
        if(e.length){
          values.push(e.val());
        }
        this.value = values.join(',');
      } 
    }
    return this.value;
  },
  /** 
   *  Default setValue function
   *  Sets all elements to specified value
   *  @param {String}
   */
  setValue: function(val){
    for(var el in this.fields){
      if(this.fields[el].length){
        this.fields[el].val(val);
      }
    }
  },
  /** 
   *  Default toString function
   *  @return A string of the Components value.
   *	@type String
   */
  toString:  function(){
    return this.getValue();
  },
  /** 
   *  Default toNode function
   *  @param {DOM Node} Node to which to attach this Components value
   *  @return DOM Node created from this Component.
   *	@type DOM Node
   */
  toNode: function(parent){
    for(var el in this.fields){
      var container = document.createElement('metadata');
      var value = document.createElement('value');
      var key = document.createElement('key');
      value.appendChild(document.createTextNode(this.getValue()));
      key.appendChild(document.createTextNode(el));
      container.appendChild(value);
      container.appendChild(key);
    }
    if(parent && parent.nodeType){
      parent.appendChild(container);
    }
    return container;
  },
  /** 
   *  Default validation function, displays Component's error message
   *  @return True if Component is required and valid, otherwise false.
   *	@type Boolean
   */
  validate: function(){
    if(!this.required){
      return true;
    }else{
      var oneIsValid = false;
      for(var e in this.fields){
        if(this.fields[e][0].type == 'checkbox' || this.fields[e][0].type == 'radio'){
          if(this.fields[e][0].checked){
            oneIsValid = true;
            break;
          }
        }else{
          if(this.fields[e].val()){
            oneIsValid = true;
            break;
          }
        }
      }
      if(oneIsValid){
        return true;
      }
    }
    return false;
  }
});

/*
TODO: Create a container for components to handle those components that can repeat

AdminUI.ComponentSet = function ComponentSet(){

};

$.extend(AdminUI.ComponentSet.prototype, {
  components: []
});

*/