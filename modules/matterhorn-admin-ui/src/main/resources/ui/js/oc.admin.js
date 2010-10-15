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

/* @namespace Holds functions and properites related to all Admin UIs. */
var ocAdmin = {} || ocAdmin;

ocAdmin.components = {};

ocAdmin.Manager = function(rootElm, rootNs, components, workflowComponents){
  this.rootElm = rootElm;
  this.rootNs = rootNs;
  this.components = components;
  this.workflowComponents = workflowComponents || {};
};

$.extend(ocAdmin.Manager.prototype, {
  serialize: function(){
    if(this.validate()){
      var doc = this.createDoc();
      var mdlist = doc.createElement('metadataList');
      for(var c in this.components){
        if(c === 'recurrence' || c === 'eventId'){
          this.components[c].toNode(doc.documentElement)
        } else {
          this.components[c].toNode(mdlist);
        }
      }
      //handle OC Workflow specialness
      for(var wc in this.workflowComponents){
        this.workflowComponents[wc].toNode(mdlist);
      }
      doc.documentElement.appendChild(mdlist);
      return ocUtils.xmlToString(doc);
    }
    return false;
  },
  populate: function(values){
    for(var e in this.components){
      if(values[e] != undefined){
        this.components[e].setValue(values[e]);
      }
    }
  },
  validate: function(){
    var error = false;
    $('#missingFieldsContainer').hide();
    $('.missing-fields-item').hide();
    for(var k in this.components){
      if(this.components[k].required && !this.components[k].validate()){
        $('#' + this.components[k].errorField).show();
        $('#' + this.components[k].label).addClass('error');
        error = true;
      }else{
        if(this.components[k].errorField && this.components[k].label){
          $('#' + this.components[k].errorField).hide();
          $('#' + this.components[k].label).removeClass('error');
        }
      }
    }
    if(error){
      $('#missingFieldsContainer').show();
    }
    return !error;
  },
  createDoc: function(){
    var doc = null;
    //Create a DOM Document, methods vary between browsers, e.g. IE and Firefox
    if(document.implementation && document.implementation.createDocument) { //Firefox, Opera, Safari, Chrome, etc.
      doc = document.implementation.createDocument(this.rootNs, this.rootElm, null);
    } else { // IE
      doc = new ActiveXObject('MSXML2.DOMDocument');
      doc.loadXML('<' + this.rootElm + ' xmlns="' + this.rootNs + '"></' + this.rootElm + '>');
    }
    return doc;
  }
});

/* @class The Component class is a collection of form elements and associated functions for use
 * with the ocAdmin.Manager. It provides basic implementations for setting, getting, displaying,
 * and XMLifying the form elements.
 */
ocAdmin.Component = function Component(fields, props, funcs){
  this.fields = [];
  this.errorField = "";
  this.label = "";
  this.properties = [];
  this.required = false;
  this.value = null;
  this.nodeKey = null;
  
  this.setFields(fields);
  this.setFunctions(funcs);
  this.setProperties(props);
};

$.extend(ocAdmin.Component.prototype, {
  /* @lends ocAdmin.Component.prototype */
  /** 
   *  Sets the fields from an array of element ids.
   *  @param {String[]} Array of element ids
   */
  setFields:  function(fields, append){
    append = append || false;
    if(!append){
      this.fields = [];
    }
    if(typeof fields == 'string') { //If a single field is specified, wrap in an array.
      fields = [fields];
    }
    for(var k in fields) {
      var e = $('#' + fields[k]);
      if(e[0]){
        this.fields[fields[k]] = e;
        this.label = e[0].id + 'Label';
        this.errorField = 'missing' + e[0].id;
      }
    }
  },
  /** 
   *  Extends Component with additional methods and/or properties
   *  @param {Object} An object literal or instance with which to extend Component
   */
  setFunctions: function(funcs){
    if(funcs && typeof funcs == 'object'){
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
          case 'nodeKey':
            this.nodeKey = props[f];
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
          switch(e[0].type){
            case 'checkbox':
            case 'radio':
              if(e.is(":checked")){
                values.push('true');
              }else{
                values.push('false');
              }
              break;
            case 'select-multiple':
              values.concat(e.val());
            default:
              values.push(e.val());
          }
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
        switch(this.fields[el][0].type){
          case 'checkbox':
          case 'radio':
            if(val == 'true'){
              this.fields[el][0].checked = true;
            }
            break;
          case 'select-multiple':
            break;
          default:
            this.fields[el].val(val);
        }
      }
    }
  },
  /** 
   *  Add this because IE seems to use the default toString() of Object instead of the definition above (MH-5097)
   *  Default toString function
   *  @return A string of the Components value.
   *	@type String
   */
  asString: function() {
    return this.getValue();
  },
  /** 
   *  Default toNode function
   *  @param {DOM Node} Node to which to attach this Components value
   *  @return DOM Node created from this Component.
   *	@type DOM Node
   */
  toNode: function(parent){
    var doc, container, value, key;
    for(var el in this.fields){
      if(parent){
        doc = parent.ownerDocument;
      }else{
        doc = document;
      }
      container = doc.createElement('metadata');
      value = doc.createElement('value');
      key = doc.createElement('key');
      value.appendChild(doc.createTextNode(this.getValue()));
      if(this.nodeKey !== null){
         key.appendChild(doc.createTextNode(this.nodeKey));
      }else{
         key.appendChild(doc.createTextNode(el));
      }
      container.appendChild(value);
      container.appendChild(key);
    }
    if(parent && parent.nodeType && container){
      parent.appendChild(container); //license bug
    }else{
      ocUtils.log('Unable to append node to document. ', parent, container);
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

ocAdmin.ComponentSet = function ComponentSet(){

};

$.extend(ocAdmin.ComponentSet.prototype, {
  components: []
});

*/