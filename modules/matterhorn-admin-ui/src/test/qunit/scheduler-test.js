module( "FormField", {
        setup: function(){ 
           $(document).append('<input type="text" id="textbox" />');
           $(document).append('<input type="checkbox" id="checkbox" value="true" />');
           $(document).append('<input type="radio" id="radio" value="true" />');
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
     var field = new FormField('textbox');
     same(field.getValue(), 'some text here' , "Run getValue");
});