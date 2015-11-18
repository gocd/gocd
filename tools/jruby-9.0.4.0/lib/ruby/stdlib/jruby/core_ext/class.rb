require 'java'
require 'jruby'
require 'jruby/compiler/java_signature'

##
# Convenience methods added to class when you want to be able to
# reify a Ruby class into a honest to goodness type.  Typically, it would look
# like:
# :call-seq:
#
# class Foo
#   java_signature '@org.foo.EventHandler(@org.foo.Priority.High) void foo(int)'
#   def foo(number)
#   end
#   become_java!
# end 
#
# Although this will still just be an instance of a IRubyObject versus a 
# specific type, you can still use this facility for:
#   1. Adding runtime annotations to methods
#   2. Making a class still reflectable from Java reflection APIs.
#
# Case #1 above is also a useful combination with implementing Java interfaces.
#
class Class
  JClass = java.lang.Class

  ##
  # Get an array of all known subclasses of this class. If recursive == true,
  # include all descendants.
  def subclasses(recursive = false)
    JRuby.reference0(self).subclasses(recursive).to_a.freeze
  end

  ##
  # java_signature will take the argument and annotate the method of the
  # same name with the Java type and annotation signatures.
  # 
  # :call-seq:
  #   java_signature '@Override void foo(int)'
  #   java_signature '@Override void foo(int foo, org.foo.Bar bar)'
  def java_signature(signature_source)
    signature = JRuby::JavaSignature.parse signature_source.to_s
    add_method_signature signature.name, signature.types

    annotations = signature.annotations
    add_method_annotation signature.name, annotations if annotations
  end
  
  ##
  # Generate a native Java class for this Ruby class. If dump_dir is specified,
  # dump the JVM bytecode there for inspection. If child_loader is false, do not
  # generate the class into its own classloader (use the parent's loader).
  # 
  # :call-seq:
  #   become_java!
  #   become_java!(dump_dir)
  #   become_java!(dump_dir, child_loader)
  #   become_java!(child_loader)
  #   become_java!(child_loader, dump_dir)
  def become_java!(*args)
    self_r = JRuby.reference0(self)

    if args.size > 0
      dump_dir = nil
      child_loader = true
      if args[0].kind_of? String
        dump_dir = args[0].to_s
        if args.size > 1
          child_loader = args[1]
        end
      else
        child_loader = args[0]
        if args.size > 1
          dump_dir = args[1].to_s
        end
      end
      
      self_r.reify_with_ancestors(dump_dir, child_loader)
    else
      self_r.reify_with_ancestors
    end

    generate_java_fields

    self_r.reified_class
  end
  
  ##
  # Get the native or reified (a la become_java!) class for this Ruby class.
  def java_class
    self_r = JRuby.reference0(self)
    current = self_r
    while current
      reified = current.reified_class
      return reified if reified
      current = JRuby.reference0(current.super_class)
    end
    
    nil
  end
  
  def _anno_class(cls)
    if cls.kind_of? JClass
      cls
    elsif cls.respond_to? :java_class
      cls.java_class.to_java :object
    else
      raise TypeError, "expected a Java class, got #{cls}"
    end
  end
  private :_anno_class
  
  ##
  # Add annotations to the named method. Annotations are specified as a Hash
  # from the annotation classes to Hashes of name/value pairs for their 
  # parameters. Please refrain from using this in favor of java_signature.
  # :call-seq:
  #
  #  add_method_annotation :foo, {java.lang.Override => {}}
  #
  def add_method_annotation(name, annotations = {})
    name = name.to_s
    self_r = JRuby.reference0(self)
    
    for cls, params in annotations
      params ||= {}
      self_r.add_method_annotation(name, _anno_class(cls), params)
    end
    
    nil
  end
  
  ##
  # Add annotations to the parameters of the named method. Annotations are
  # specified as a parameter-list-length Array of Hashes from annotation classes
  # to Hashes of name/value pairs for their parameters.
  def add_parameter_annotation(name, annotations = [])
    name = name.to_s
    self_r = JRuby.reference0(self)
    
    annotations.each_with_index do |param_annos, i|
      for cls, params in param_annos
        params ||= {}
        self_r.add_parameter_annotation(name, i, _anno_class(cls), params)
      end
    end
    
    nil
  end
  
  ##
  # Add annotations to this class. Annotations are specified as a Hash of
  # annotation classes to Hashes of name/value pairs for their parameters.
  # :call-seq:
  #
  # add_class_annotation java.lang.TypeAnno => {"type" => @com.foo.GoodOne}
  #
  def add_class_annotations(annotations = {})
    self_r = JRuby.reference0(self)
    
    for cls, params in annotations
      params ||= {}
      
      self_r.add_class_annotation(_anno_class(cls), params)
    end
    
    nil
  end

  ##
  # Add a Java signaturefor the named method. The signature is specified as
  # an array of Java classes where the first class specifies the return
  # type.
  # :call-seq:
  #
  # add_method_signature :foo, [:void, :int, java.lang.Thread]
  #
  def add_method_signature(name, classes)
    name, self_r = name.to_s, JRuby.reference0(self)
    types = classes.inject([]) {|arr, cls| arr << _anno_class(cls) }
    
    self_r.add_method_signature(name, types.to_java(JClass))
    
    nil
  end

  def java_field(signature)
    signature = signature.to_s

    signature = signature.split(/\s/)

    raise "Java Field must be specified as a string with the format <Type Name>" if signature.size != 2

    type, name = signature
    java_fields << name
    add_field_signature(name, type)
  end


  def add_field_signature(name, type)
    self_r = JRuby.reference0(self)

    signature = JRuby::JavaSignature.new(nil, nil)
    java_return_type = signature.as_java_type(type)

    self_r.add_field_signature(name, java_return_type.to_java(JClass))
  end

  def add_field_annotation(name, annotations = {})
    name = name.to_s
    self_r = JRuby.reference0(self)

    for cls, params in annotations
      params ||= {}
      self_r.add_field_annotation(name, _anno_class(cls), params)
    end

    nil
  end

  def generate_java_fields
    java_fields.each do |field_name|
      field = java_class.get_declared_field(field_name)
      define_method(field_name) { field.get(self) }
      define_method(:"#{field_name}=") { |v| field.set(self, v) }
    end
  end

  def java_fields
    @java_fields ||= []
  end
end
