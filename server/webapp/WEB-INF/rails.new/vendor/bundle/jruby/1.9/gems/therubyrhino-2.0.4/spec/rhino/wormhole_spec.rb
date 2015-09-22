require File.expand_path('../spec_helper', File.dirname(__FILE__))

describe Rhino::To do
  
  describe "ruby translation" do
    
    it "converts javascript NOT_FOUND to ruby nil" do
      Rhino.to_ruby(Rhino::JS::Scriptable::NOT_FOUND).should be nil
    end
  
    it "converts javascript undefined into nil" do
      Rhino.to_ruby(Rhino::JS::Undefined.instance).should be nil
    end
    
    it "does return javascript object" do
      Rhino::JS::NativeObject.new.tap do |js_obj|
        Rhino.to_ruby(js_obj).should be(js_obj)
      end
    end
    
    it "wraps native javascript arrays into a ruby NativeArray wrapper" do
      js_array = Rhino::JS::NativeArray.new([1,2,4].to_java)
      Rhino.to_ruby(js_array).should == [1,2,4]
    end
    
    it "returns a javascript function" do
      klass = Class.new(Rhino::JS::BaseFunction)
      function = klass.new
      Rhino.to_ruby(function).should be(function)
    end
    
    it "leaves native ruby objects alone" do
      obj = Object.new
      Rhino.to_ruby(obj).should be(obj)
    end
    
    it "it unwraps wrapped java objects" do
      Rhino::Context.open do |cx|
        scope = cx.scope
        j_str = java.lang.String.new("Hello World")
        native_obj = Rhino::JS::NativeJavaObject.new(scope, j_str, j_str.getClass())
        Rhino.to_ruby(native_obj).should == "Hello World"
      end
    end
    
    if (Java::JavaClass.for_name('org.mozilla.javascript.ConsString') rescue nil)
      it "converts a cons string" do
        cons_string = org.mozilla.javascript.ConsString.new('1', '2')
        Rhino.to_ruby(cons_string).should == '12'
      end
    end
    
  end
  
  describe  "javascript translation" do
    
    it "passes primitives through to the js layer to let jruby and rhino do he thunking" do
      Rhino.to_javascript(1).should be(1)
      Rhino.to_javascript(2.5).should == 2.5
      Rhino.to_javascript("foo").should == "foo"
      Rhino.to_javascript(true).should be(true)
      Rhino.to_javascript(false).should be(false)
      Rhino.to_javascript(nil).should be_nil
    end
    
    it "leaves native javascript objects alone" do
      Rhino::JS::NativeObject.new.tap do |o|
        Rhino.to_javascript(o).should be(o)
      end
    end
    
    it "converts ruby arrays into javascript arrays" do
      Rhino.to_javascript([1,2,3,4,5]).tap do |a|
        a.should be_kind_of(Rhino::JS::NativeArray)
        a.get(0,a).should be(1)
        a.get(1,a).should be(2)
        a.get(2,a).should be(3)
        a.get(3,a).should be(4)
        a.prototype.should be_nil # this is how Rhino works !
      end
    end
    
    it "converts ruby hashes into native objects" do
      Rhino.to_javascript({ :bare => true }).tap do |h|
        h.should be_kind_of(Rhino::JS::NativeObject)
        h.get("bare", h).should be(true)
        h.prototype.should be_nil # this is how Rhino works !
      end
    end

    it "converts deeply nested ruby hashes into native objects" do
      hash = {
          :array => [
              {
                  :breed => "Pug"
              },
              {
                  :breed => "English Bulldog"
              },
              {
                  :breed => [
                      "Pug",
                      "Beagle"
                  ]
              }
          ]
      }

      Rhino.to_javascript(hash).tap do |h|
        h.should be_kind_of(Rhino::JS::NativeObject)

        a = h.get("array", h)
        a.should be_kind_of(Rhino::JS::NativeArray)

        element0 = a.get(0,a)
        element0.should be_kind_of(Rhino::JS::NativeObject)
        element0.get("breed", element0).should == "Pug"

        element2 = a.get(2,a)
        element2.should be_kind_of(Rhino::JS::NativeObject)

        nested_array = element2.get("breed", element2)
        nested_array.should be_kind_of(Rhino::JS::NativeArray)
        nested_array.get(0,nested_array).should == "Pug"
        nested_array.get(1,nested_array).should == "Beagle"

        h.prototype.should be_nil # this is how Rhino works !
      end
    end
    
    describe "with a scope" do
      
      before do
        factory = Rhino::JS::ContextFactory.new
        context = nil
        factory.call do |ctx|
          context = ctx
          @scope = context.initStandardObjects(nil, false)
        end
        factory.enterContext(context)
      end

      after do
        Rhino::JS::Context.exit
      end
      
      it "converts ruby arrays into javascript arrays" do
        Rhino.to_javascript([1,2,3,4,5], @scope).tap do |a|
          a.should be_kind_of(Rhino::JS::NativeArray)
          a.get(0,a).should be(1)
          a.get(1,a).should be(2)
          a.get(2,a).should be(3)
          a.get(3,a).should be(4)
          a.prototype.should_not be_nil
        end
      end

      it "converts ruby hashes into native objects" do
        Rhino.to_javascript({ :bare => true }, @scope).tap do |h|
          h.should be_kind_of(Rhino::JS::NativeObject)
          h.get("bare", h).should be(true)
          h.prototype.should_not be_nil
        end
      end
      
    end
    
    it "converts procs and methods into native functions" do
      Rhino.to_javascript(lambda {|lhs,rhs| lhs * rhs}).tap do |f|
        f.should be_kind_of(Rhino::JS::Function)
        f.call(context, scope, nil, [7, 6].to_java).should be(42)
      end
      
      Rhino.to_javascript("foo,bar,baz".method(:split)).tap do |f|
        f.should be_kind_of(Rhino::JS::Function)
        Rhino.to_ruby(f.call(context, scope, nil, [','].to_java)).should == ['foo', 'bar', 'baz']
      end
    end

#    it "creates a prototype for the object based on its class" do
#      klass = Class.new do
#        def foo(one, two)
#          "1: #{one}, 2: #{two}"
#        end        
#      end
#
#      Rhino.to_javascript(klass.new).tap do |o|
#        o.should be_kind_of(Rhino::RubyObject)
#        o.prototype.tap do |p|
#          p.should_not be_nil
#          p.get("foo", p).should_not be_nil
#          p.get("toString", p).should_not be_nil
#        end
#      end
#    end
    
  end
  
end
