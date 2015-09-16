require File.expand_path('../spec_helper', File.dirname(__FILE__))

module RhinoHelpers
  
  module_function
  
  def add_prototype_key(hash, recurse = false)
    hash['prototype'] ||= {}
    hash.keys.each do |key|
      val = hash[key] unless key == 'prototype'
      add_prototype_key(val, recurse) if val.is_a?(Hash)
    end if recurse
  end  
  
end

shared_examples_for 'ScriptableObject', :shared => true do

  it "acts like a hash" do
    @object['foo'] = 'bar'
    @object['foo'].should == 'bar'
  end

  it "might be converted to a hash with string keys" do
    @object[42] = '42'
    @object[:foo] = 'bar'
    expect = @object.respond_to?(:to_h_properties) ? @object.to_h_properties : {}
    @object.to_h.should == expect.merge('42' => '42', 'foo' => 'bar')
  end
  
  it "yields properties with each" do
    @object['1'] = 1
    @object['3'] = 3
    @object['2'] = 2
    @object.each do |key, val|
      case key
        when '1' then val.should == 1
        when '2' then val.should == 2
        when '3' then val.should == 3
      end
    end
  end
  
end

describe "NativeObject" do
  
  before do
    @object = Rhino::JS::NativeObject.new
  end
  
  it_should_behave_like 'ScriptableObject'
  
end

describe "FunctionObject" do
  
  before do
    factory = Rhino::JS::ContextFactory.new
    context, scope = nil, nil
    factory.call do |ctx|
      context = ctx
      scope = context.initStandardObjects(nil, false)
    end
    factory.enterContext(context)
    
    to_string = java.lang.Object.new.getClass.getMethod(:toString)
    @object = Rhino::JS::FunctionObject.new('to_string', to_string, scope)
    @object.instance_eval do
      def to_h_properties
        h = { "arguments"=> nil, "name"=> "to_string", "arity"=> 0, "length"=> 0 }
        RhinoHelpers.add_prototype_key(h) if Rhino.implementation_version < '1.7R4'
        h
      end
    end
  end

  after do
    Rhino::JS::Context.exit
  end
  
  it_should_behave_like 'ScriptableObject'
  
end

describe "NativeObject (scoped)" do
  
  before do
    factory = Rhino::JS::ContextFactory.new
    @context, @scope = nil, nil
    factory.call do |ctx|
      @context = ctx
      @scope = @context.initStandardObjects(nil, false)
    end
    factory.enterContext(@context)
    
    @object = @context.newObject(@scope)
  end
  
  after do
    Rhino::JS::Context.exit
  end
  
  it_should_behave_like 'ScriptableObject'  
  
  it 'routes rhino methods' do
    @object.prototype.should_not be nil
    @object.getTypeOf.should == 'object'
  end
  
  it 'raises on missing method' do
    lambda { @object.aMissingMethod }.should raise_error(NoMethodError)
  end
  
  it 'invokes JS function' do
    @object.hasOwnProperty('foo').should == false
    @object.toLocaleString.should == '[object Object]'
  end

  it 'puts JS property' do
    @object.has('foo', @object).should == false
    @object.foo = 'bar'
    @object.has('foo', @object).should == true
  end

  it 'gets JS property' do
    @object.put('foo', @object, 42)
    @object.foo.should == 42
  end
  
  it 'is == to an empty Hash / Map' do
    ( @object == {} ).should be true
    ( @object == java.util.HashMap.new ).should be true
  end

  it 'is === to an empty Hash' do
    ( @object === {} ).should be true
  end

  it 'is not eql? to an empty Hash / Map' do
    ( @object.eql?( {} ) ).should be false
    ( @object.eql?( java.util.HashMap.new ) ).should be false
  end

  it 'is eql? to another native object' do
    object = @context.newObject(scope)
    ( @object.eql?( object ) ).should be true
    ( object.eql?( @object ) ).should be true
    ( @object == object ).should be true
    ( object === @object ).should be true
  end

  it 'native objects with same values are equal' do
    obj1 = @context.evaluateString @scope, "( { ferko: 'suska', answer: 42 } )", '<eval>', 0, nil
    obj2 = @context.evaluateString @scope, "var obj = {}; obj['answer'] = 42; obj.ferko = 'suska'; obj", '<eval>', 0, nil
    ( obj1 == obj2 ).should be true
    ( obj1.eql?( obj2 ) ).should be true
  end

end

describe "NativeFunction" do
  
  before do
    factory = Rhino::JS::ContextFactory.new
    @context, @scope = nil, nil
    factory.call do |ctx|
      @context = ctx
      @scope = @context.initStandardObjects(nil, false)
    end
    factory.enterContext(@context)
    
    object = @context.newObject(@scope)
    @object = Rhino::JS::ScriptableObject.getProperty(object, 'toString')
    @object.instance_eval do
      def to_h_properties
        h = { "arguments"=> nil, "name"=> "toString", "arity"=> 0, "length"=> 0 }
        RhinoHelpers.add_prototype_key(h) if Rhino.implementation_version < '1.7R4'
        h
      end
    end
  end

  after do
    Rhino::JS::Context.exit
  end
  
  it_should_behave_like 'ScriptableObject'
  
  it 'is (Ruby) callable' do
    # NOTE: no implicit or bound this thus this === global
    @object.call.should == '[object global]'
  end

  it 'is (Ruby) callable passing arguments' do
    js = "( function foo(arg) { return 'foo' + arg; } )"
    foo = @context.evaluateString(@scope, js, '<eval>', 0, nil)
    foo.call(42).should == 'foo42'
  end

  it 'is (Ruby) callable converting result' do
    js = "( function foo(arg) { return [ 1, 2, arg ]; } )"
    foo = @context.evaluateString(@scope, js, '<eval>', 0, nil)
    foo.call('x').should == [ 1, 2, 'x' ]
  end
  
  it 'might be bind and called' do
    @object.bind(@object).should be_a(Rhino::JS::Function)
    @object.bind(@object).call.should == '[object Function]'
  end
  
  it 'might be bind to a this context' do
    @object.bind(@object).should be_a(Rhino::JS::Function)
    @object.bind(@object).call.should == '[object Function]'
  end

  it 'might be bind to a this with args' do
    array = @context.newArray(@scope, [].to_java)
    push = Rhino::JS::ScriptableObject.getProperty(array, 'push')
    
    this = @context.newArray(@scope, [ 0 ].to_java)
    push.bind(this, 1, 2).call(3, 4)
    
    this.length.should == 5
    5.times { |i| this.get(i, this).should == i }
  end
  
  it 'might be applied' do
    an_obj = @context.newObject(@scope)
    @object.apply(an_obj).should == '[object Object]'
    
    array = @context.newArray(@scope, [].to_java)
    push = Rhino::JS::ScriptableObject.getProperty(array, 'splice')
    
    this = @context.newArray(@scope, [ 0, 3, 4 ].to_java)
    push.apply(this, 1, 0, 1, 2)
    
    this.length.should == 5
    5.times { |i| this.get(i, this).should == i }
  end
  
  it 'might get method-called' do
    @object.method(:apply).should == @object.method(:methodcall)
  end
  
end

describe "NativeFunction (constructor)" do
  
  before do
    factory = Rhino::JS::ContextFactory.new
    @context, @scope = nil, nil
    factory.call do |ctx|
      @context = ctx
      @scope = @context.initStandardObjects(nil, false)
    end
    factory.enterContext(@context)
    
    @object = Rhino::JS::ScriptableObject.getProperty(@context.newObject(@scope), 'constructor')
    @object.instance_eval do
      def to_h_properties
        h = {
          "arguments"=>nil, "prototype"=>{}, "name"=>"Object", "arity"=>1, "length"=>1,
          
          "getPrototypeOf"=> { "arguments"=>nil, "name"=>"getPrototypeOf", "arity"=>1, "length"=>1}, 
          "keys"=>{"arguments"=>nil, "name"=>"keys", "arity"=>1, "length"=>1}, 
          "getOwnPropertyNames"=>{"arguments"=>nil, "name"=>"getOwnPropertyNames", "arity"=>1, "length"=>1}, 
          "getOwnPropertyDescriptor"=>{"arguments"=>nil, "name"=>"getOwnPropertyDescriptor", "arity"=>2, "length"=>2}, 
          "defineProperty"=>{"arguments"=>nil, "name"=>"defineProperty", "arity"=>3, "length"=>3}, 
          "isExtensible"=>{"arguments"=>nil, "name"=>"isExtensible", "arity"=>1, "length"=>1}, 
          "preventExtensions"=>{"arguments"=>nil, "name"=>"preventExtensions", "arity"=>1, "length"=>1}, 
          "defineProperties"=>{"arguments"=>nil, "name"=>"defineProperties", "arity"=>2, "length"=>2}, 
          "create"=>{"arguments"=>nil, "name"=>"create", "arity"=>2, "length"=>2}, 
          "isSealed"=>{"arguments"=>nil, "name"=>"isSealed", "arity"=>1, "length"=>1}, 
          "isFrozen"=>{"arguments"=>nil, "name"=>"isFrozen", "arity"=>1, "length"=>1}, 
          "seal"=>{"arguments"=>nil, "name"=>"seal", "arity"=>1, "length"=>1}, 
          "freeze"=>{"arguments"=>nil, "name"=>"freeze", "arity"=>1, "length"=>1}
        }
        RhinoHelpers.add_prototype_key(h, :recurse) if Rhino.implementation_version < '1.7R4'
        h
      end
    end
  end

  after do
    Rhino::JS::Context.exit
  end
  
  it_should_behave_like 'ScriptableObject'
  
  it 'is constructable' do
    @object.new.should be_a Rhino::JS::NativeObject
  end
  
  it 'is not equal to an empty Hash' do
    ( @object == {} ).should be false
    ( @object === {} ).should be false
    ( @object.eql?( {} ) ).should be false
  end

  it 'empty functions are not considered equal' do
    fn1 = @context.evaluateString @scope, "( function() {} )", '<eval>', 0, nil
    fn2 = @context.evaluateString @scope, "var f = function() {}", '<eval>', 0, nil
    ( fn1 == fn2 ).should be false
    ( fn1 === fn2 ).should be false
    ( fn1.eql?( fn2 ) ).should be false
  end

end
