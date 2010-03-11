module( "FormField", {
        setup: function(){ 
          var doc = $(document.body);
          doc.append('<input type="text" id="textbox" value="true" />');
          doc.append('<input type="checkbox" id="checkbox" checked="checked" value="true" />');
        },
        teardown: function(){
          $(document.body).empty();
        }
});

test("FormField creation", function(){
     var field = new FormField();
     ok((field && typeof field === 'object' && field instanceof FormField), "Instantiate empty FormField");
     field.setFormFields('textbox');
     ok((field.fields.textbox && field.fields.textbox[0].type === 'text'), "Add Field");
     field.setFormFieldOpts({required: true, foo: true});
     ok((field.required && field.foo), "FormField Opts set");
});

test("FormField get/set/disp", function(){
     var field = new FormField('textbox', false, {getValue: function(){ return true; }});
     ok(field.getValue(), "Run getValue");
});