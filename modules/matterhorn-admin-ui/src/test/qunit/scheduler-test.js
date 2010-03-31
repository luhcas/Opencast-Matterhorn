module( "FormField", {
  setup: function(){ 
    var doc = $(document.body);
    doc.append('<input type="text" id="textbox" value="some text here" />');
    doc.append('<input type="checkbox" id="input1" value="true" />');
    doc.append('<input type="checkbox" id="input2" value="true" />');
    doc.append('<input type="checkbox" id="input3" value="true" />');
    doc.append('<input type="text" id="attendees" value="agent1" />');
    doc.append('<input type="text" id="durationHour" value="1" />');
    doc.append('<input type="text" id="durationMin" value="5" />');
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

test("FormField get/set/disp/check", function(){
     var field = new FormField('textbox');
     same(field.getValue(), 'some text here' , "Run getValue");
     field.setValue('test');
     same(field.getValue(), 'test', "Run setValue");
     same(field.dispValue(), 'test', "Run dispValue");
});


test("FormField custom get/set/disp/check", function(){
     field.setFormFieldOpts({
                            getValue: function(){ return this.value },
                            setValue: function(val){ this.value = val; },
                            dispValue: function(){ return this.value }
                            });
     field.setValue('test');
     same(field.value, 'test', "Run custom setValue");
     same(field.getValue(), 'test', "Run custom getValue");
     same(field.dispValue(), 'test', "Run custom dispValue");
});

test("FormField agent get/set/disp/check", function(){
     var field = new FormField('attendees', getValue: getAgent, setValue: setAgent, checkValue: checkAgent);
     field.setValue('test');
     ok(field.checkValue(), "Run agent checkValue");
     same(field.attendees.val(), 'test', "Run agent setValue");
     same(field.getValue(), 'test', "Run agent getValue");
     same(field.dispValue(), 'test', "Run agent dispValue");
});

test("FormField duration get/set/disp/check", function(){
     var field = new FormField(['durationHour','durationMin'], getValue: getAgent, setValue: setAgent, checkValue: checkAgent, dispValue: getDurationDisplay);
     field.setValue('3660000');
     ok(field.checkValue(), "Run duration checkValue");
     same(field.durationHour.val(), '1', "Run duration hour setValue");
     same(field.durationMin.val(), '1', "Run duration min setValue");
     same(field.getValue(), 3660000 , "Run duration getValue");
     same(field.dispValue(), '1 hours, 1 minutes', "Run duration dispValue");
});

test("FormField input get/set/disp/check", function(){
     var field = new FormField(['input1', 'input2', 'input3'], getValue: getAgent, setValue: setAgent, checkValue: checkAgent);
     field.setValue('input1,input2,input3');
     ok(field.checkValue(), "Run input checkValue");
     same(field.input1.val(), 'true', "Run input1 setValue");
     same(field.input2.val(), 'true', "Run input2 setValue");
     same(field.input3.val(), 'true', "Run input3 setValue");
     same(field.getValue(), 'test', "Run input getValue");
});

/*
test("FormField startdate get/set/disp/check", function(){
     var field = new FormField('attendees', getValue: getAgent, setValue: setAgent, checkValue: checkAgent);
     ok(field.checkValue(), "Run startdate checkValue");
     same(field.value, 'test', "Run startdate setValue");
     same(field.getValue(), 'test', "Run startdate getValue");
     same(field.dispValue(), 'test', "Run startdate dispValue");
     ok(true, "TODO: startdate tests");
});*/