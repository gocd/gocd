module JRuby::Compiler
  module JavaGenerator
    module_function

    def generate_java(node, script_name = nil)
      walker = ClassNodeWalker.new(script_name)

      node.accept(walker)

      walker.script
    end

    def generate_javac(files, options)
      files_string = files.join(' ')
      jruby_jar, = ['jruby.jar', 'jruby-complete.jar'].select do |jar|
        File.exist? "#{ENV_JAVA['jruby.home']}/lib/#{jar}"
      end
      separator = File::PATH_SEPARATOR
      classpath_string = options[:classpath].size > 0 ? options[:classpath].join(separator) : "."
      javac_opts = options[:javac_options].join(' ')
      target = options[:target]
      java_home = ENV_JAVA['jruby.home']

      compile_string = "javac #{javac_opts} -d #{target} -cp #{java_home}/lib/#{jruby_jar}#{separator}#{classpath_string} #{files_string}"

      compile_string
    end
  end

  module VisitorBuilder
    def visit(name, &block)
      define_method :"visit_#{name}_node" do |node|
        log "entering: #{node.node_type}"
        with_node(node) do
          instance_eval(&block)
        end
      end
    end

    def visit_default(&block)
      define_method :method_missing do |name, node|
        super unless name.to_s =~ /^visit/

        with_node(node) do
          block.call
        end
      end
    end
  end

  class ClassNodeWalker
    AST = org.jruby.ast

    include AST::visitor::NodeVisitor

    java_import AST::NodeType
    java_import org.jruby.parser.JavaSignatureParser
    java_import java.io.ByteArrayInputStream

    extend VisitorBuilder

    attr_accessor :class_stack, :method_stack, :signature, :script, :annotations, :node

    def initialize(script_name = nil)
      @script = RubyScript.new(script_name)
      @class_stack = []
      @method_stack = []
      @signature = nil
      @annotations = []
      @name = nil
      @node = nil
    end

    def add_imports(nodes)
      nodes.each do |n|
        @script.add_import(name_or_value(n))
      end
    end

    def set_signature(name)
      @signature = name
    end

    def add_annotation(nodes)
      nodes.each do
        name = name_or_value(nodes[0])
        @annotations << name
      end
    end

    def add_interface(*ifc_nodes)
      ifc_nodes.
        map {|ifc| defined?(ifc.name) ? ifc.name : ifc.value}.
        each {|ifc| current_class.add_interface(ifc)}
    end

    def new_class(name)
      cls = @script.new_class(name, @annotations)
      @annotations = []

      class_stack.push(cls)
    end

    def current_class
      class_stack[0]
    end

    def pop_class
      class_stack.pop
      @signature = nil
      @annotations = []
    end

    def new_method(name)
      method = current_class.new_method(name, @signature, @annotations)
      @signature = nil
      @annotations = []

      method_stack.push(method)
    end

    def new_static_method(name)
      method = current_class.new_method(name, @signature, @annotations)
      method.static = true
      @signature = nil
      @annotations = []

      method_stack.push(method)
    end

    def current_method
      method_stack[0]
    end

    def pop_method
      method_stack.pop
    end

    def build_signature(signature)
      if signature.kind_of? String
        bytes = signature.to_java_bytes
        return JavaSignatureParser.parse(ByteArrayInputStream.new(bytes))
      else
        raise "java_signature must take a literal string"
      end
    end

    def build_args_signature(params)
      sig = ["Object"]
      param_strings = params.child_nodes.map do |param|
        if param.respond_to? :type_node
          type_node = param.type_node
          next name_or_value(type_node)
        end
        raise 'unknown signature element: ' + param.to_s
      end
      sig.concat(param_strings)

      sig
    end

    def add_requires(*requires)
      requires.each {|r| @script.add_require(name_or_value(r))}
    end

    def set_package(package)
      @script.package = name_or_value(package)
    end

    def name_or_value(node)
      return node.name if defined? node.name
      return node.value if defined? node.value
      raise "unknown node :" + node.to_s
    end

    def with_node(node)
      begin
        old, @node = @node, node
        yield
      ensure
        @node = old
      end
    end

    def error(message)
      long_message =  "#{node.position}: #{message}"
      raise long_message
    end

    def log(str)
      puts "[jrubyc] #{str}" if $VERBOSE
    end

    visit :args do
      # Duby-style arg specification, only pre supported for now
      if node.pre && node.pre.child_nodes.find {|pre_arg| pre_arg.respond_to? :type_node}
        current_method.java_signature = build_args_signature(node.pre)
      end
      node.pre && node.pre.child_nodes.each do |pre_arg|
        current_method.args << pre_arg.name
      end
      node.opt_args && node.opt_args.child_nodes.each do |pre_arg|
        current_method.args << pre_arg.name
      end
      node.post && node.post.child_nodes.each do |post_arg|
        current_method.args << post_arg.name
      end
      if node.rest_arg >= 0
        current_method.args << node.rest_arg_node.name
      end
      if node.block
        current_method.args << node.block.name
      end

      # if method still has no signature, generate one
      unless current_method.java_signature
        args_string = current_method.args.map{|a| "Object #{a}"}.join(",")
        sig_string = "Object #{current_method.name}(#{args_string})"
        current_method.java_signature = build_signature(sig_string)
      end
    end

    visit :class do
      new_class(node.cpath.name)
      node.body_node.accept(self)
      pop_class
    end

    visit :defn do
      new_method(node.name)
      node.args_node.accept(self)
      pop_method
    end

    visit :defs do
      new_static_method(node.name)
      node.args_node.accept(self)
      pop_method
    end

    visit :fcall do
      case node.name
      when 'java_import'
        add_imports node.args_node.child_nodes
      when 'java_signature'
        set_signature build_signature(node.args_node.child_nodes[0].value)
      when 'java_annotation'
        add_annotation(node.args_node.child_nodes)
      when 'java_implements'
        add_interface(*node.args_node.child_nodes)
      when "java_require"
        add_requires(*node.args_node.child_nodes)
      when "java_package"
        set_package(*node.args_node.child_nodes)
      end
    end

    visit :block do
      node.child_nodes.each {|n| n.accept self}
    end

    visit :newline do
      node.next_node.accept(self)
    end

    visit :nil do
    end

    visit :root do
      node.body_node.accept(self)
    end

    visit_default do |node|
      # ignore other nodes
    end
  end
  
  class RubyScript
    BASE_IMPORTS = [
      "org.jruby.Ruby",
      "org.jruby.RubyObject",
      "org.jruby.javasupport.util.RuntimeHelpers",
      "org.jruby.runtime.builtin.IRubyObject",
      "org.jruby.javasupport.JavaUtil",
      "org.jruby.RubyClass"
    ]

    def initialize(script_name, imports = BASE_IMPORTS)
      @classes = []
      @script_name = script_name
      @imports = imports
      @requires = []
      @package = ""
    end

    attr_accessor :classes, :imports, :script_name, :requires, :package

    def add_import(name)
      @imports << name
    end

    def add_require(require)
      @requires << require
    end

    def new_class(name, annotations = [])
      cls = RubyClass.new(name, imports, script_name, annotations, requires, package)
      @classes << cls
      cls
    end

    def to_s
      str = ""
      @classes.each do |cls|
        str << cls.to_s
      end
      str
    end
  end

  class RubyClass
    def initialize(name, imports = [], script_name = nil, annotations = [], requires = [], package = "")
      @name = name
      @imports = imports
      @script_name = script_name
      @methods = []
      @annotations = annotations
      @interfaces = []
      @requires = requires
      @package = package
      @has_constructor = false;
    end

    attr_accessor :methods, :name, :script_name, :annotations, :interfaces, :requires, :package, :sourcefile

    def constructor?
      @has_constructor
    end

    def new_method(name, java_signature = nil, annotations = [])
      is_constructor = name == "initialize"
      @has_constructor ||= is_constructor

      if is_constructor
        method = RubyConstructor.new(self, java_signature, annotations)
      else
        method = RubyMethod.new(self, name, java_signature, annotations)
      end

      methods << method
      method
    end

    def add_interface(ifc)
      @interfaces << ifc
    end

    def interface_string
      if @interfaces.size > 0
        "implements " + @interfaces.join('.')
      else
        ""
      end
    end

    def static_init
      return <<JAVA
    static {
#{requires_string}
        RubyClass metaclass = __ruby__.getClass(\"#{name}\");
        metaclass.setRubyStaticAllocator(#{name}.class);
        if (metaclass == null) throw new NoClassDefFoundError(\"Could not load Ruby class: #{name}\");
        __metaclass__ = metaclass;
    }
JAVA
    end

    def annotations_string
      annotations.map do |a|
        "@" + a
      end.join("\n")
    end

    def methods_string
      methods.map(&:to_s).join("\n")
    end

    def requires_string
      if requires.size == 0
        source = File.read script_name
        source_chunks = source.unpack("a32000" * (source.size / 32000 + 1))
        source_chunks.each do |chunk|
          chunk.gsub!(/([\\"])/, '\\\\\1')
          chunk.gsub!("\n", "\\n\" +\n            \"")
        end
        source_line = source_chunks.join("\")\n          .append(\"");

        "        String source = new StringBuilder(\"#{source_line}\").toString();\n        __ruby__.executeScript(source, \"#{script_name}\");"
      else
        requires.map do |r|
          "        __ruby__.getLoadService().lockAndRequire(\"#{r}\");"
        end.join("\n")
      end
    end

    def package_string
      if package.empty?
        ""
      else
        "package #{package};"
      end
    end

    def constructor_string
      str = <<JAVA
    /**
     * Standard Ruby object constructor, for construction-from-Ruby purposes.
     * Generally not for user consumption.
     *
     * @param ruby The JRuby instance this object will belong to
     * @param metaclass The RubyClass representing the Ruby class of this object
     */
    private #{name}(Ruby ruby, RubyClass metaclass) {
        super(ruby, metaclass);
    }

    /**
     * A static method used by JRuby for allocating instances of this object
     * from Ruby. Generally not for user comsumption.
     *
     * @param ruby The JRuby instance this object will belong to
     * @param metaclass The RubyClass representing the Ruby class of this object
     */
    public static IRubyObject __allocate__(Ruby ruby, RubyClass metaClass) {
        return new #{name}(ruby, metaClass);
    }
JAVA

      unless @has_constructor
        str << <<JAVA
        
    /**
     * Default constructor. Invokes this(Ruby, RubyClass) with the classloader-static
     * Ruby and RubyClass instances assocated with this class, and then invokes the
     * no-argument 'initialize' method in Ruby.
     */
    public #{name}() {
        this(__ruby__, __metaclass__);
        RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, "initialize");
    }
JAVA
      end

      str
    end

    def to_s
      class_string = <<JAVA
#{package_string}

#{imports_string}

#{annotations_string}
public class #{name} extends RubyObject #{interface_string} {
    private static final Ruby __ruby__ = Ruby.getGlobalRuntime();
    private static final RubyClass __metaclass__;

#{static_init}
#{constructor_string}
#{methods_string}
}
JAVA

      class_string
    end

    def imports_string
      @imports.map do |import|
        "import #{import};"
      end.join("\n")
    end
  end

  class RubyMethod
    # How many arguments we can invoke without needing to box arguments
    MAX_UNBOXED_ARITY_LENGTH = 3

    def initialize(ruby_class, name, java_signature = nil, annotations = [])
      @ruby_class = ruby_class
      @name = name
      @java_signature = java_signature
      @static = false
      @args = []
      @annotations = annotations
    end

    attr_accessor :args, :name, :java_signature, :static, :annotations

    def constructor?
      false
    end

    def arity
      typed_args.size
    end

    def to_s
      declarator_string do
        <<-JAVA
#{conversion_string(var_names)}
        IRubyObject ruby_result = RuntimeHelpers.invoke(__ruby__.getCurrentContext(), #{static ? '__metaclass__' : 'this'}, \"#{name}\"#{passed_args});
        #{return_string}
        JAVA
      end
    end

    def declarator_string(&body)
      <<JAVA
    #{annotations_string}
    #{modifier_string} #{return_type} #{java_name}(#{declared_args}) #{throws_exceptions}{
#{body.call}
    }
JAVA
    end

    def annotations_string
      annotations.map { |a| "@" + a }.join("\n")
    end

    def conversion_string(var_names)
      if arity <= MAX_UNBOXED_ARITY_LENGTH
        var_names.map { |a| "        IRubyObject ruby_#{a} = JavaUtil.convertJavaToRuby(__ruby__, #{a});"}.join("\n")
      else
        str =  "        IRubyObject ruby_args[] = new IRubyObject[#{arity}];\n"
        var_names.each_with_index { |a, i| str += "        ruby_args[#{i}] = JavaUtil.convertJavaToRuby(__ruby__, #{a});\n" }
        str
      end
    end

    # FIXME: We should allow all valid modifiers
    def modifier_string
      modifiers = {}
      java_signature.modifiers.reject(&:annotation?).each {|m| modifiers[m.to_s] = m.to_s}
      is_static = static || modifiers["static"]
      static_str = is_static ? ' static' : ''
      abstract_str = modifiers["abstract"] ? ' abstract' : ''
      final_str = modifiers["final"] ? ' final' : ''
      native_str = modifiers["native"] ? ' native' : ''
      synchronized_str = modifiers["synchronized"] ? ' synchronized' : ''
      # only make sense for fields
      #is_transient = modifiers["transient"]
      #is_volatile = modifiers["volatile"]
      strictfp_str = modifiers["strictfp"] ? ' strictfp' : ''
      visibilities = modifiers.keys.to_a.grep(/public|private|protected/)
      if visibilities.size > 0
        visibility_str = "#{visibilities[0]}"
      else
        visibility_str = 'public'
      end

      annotations = java_signature.modifiers.select(&:annotation?).map(&:to_s).join(" ")
      
      "#{annotations}#{visibility_str}#{static_str}#{final_str}#{abstract_str}#{strictfp_str}#{native_str}#{synchronized_str}"
    end

    def typed_args
      return @typed_args if @typed_args

      i = 0;
      @typed_args = java_signature.parameters.map do |a|
        type = a.type.name
        if a.variable_name
          var_name = a.variable_name
        else
          var_name = args[i]
          i+=1
        end

        {:name => var_name, :type => type}
      end
    end

    def throws_exceptions
      if java_signature.throws && !java_signature.throws.empty?
        'throws ' + java_signature.throws.join(', ') + ' '
      else
        ''
      end
    end

    def declared_args
      @declared_args ||= typed_args.map { |a| "#{a[:type]} #{a[:name]}" }.join(', ')
    end

    def var_names
      @var_names ||= typed_args.map {|a| a[:name]}
    end

    def passed_args
      return @passed_args if @passed_args

      if arity <= MAX_UNBOXED_ARITY_LENGTH
        @passed_args = var_names.map {|a| "ruby_#{a}"}.join(', ')
        @passed_args = ', ' + @passed_args if args.size > 0
      else
        @passed_args = ", ruby_args";
      end
    end

    def return_type
      if java_signature
        java_signature.return_type
      else
        raise "no java_signature has been set for method #{name}"
      end
    end

    def return_string
      if java_signature
        if return_type.void?
          "return;"
        else
          # Can't return wrapped array as primitive array
          cast_to = return_type.is_array ? return_type.fully_typed_name : return_type.wrapper_name
          "return (#{cast_to})ruby_result.toJava(#{return_type.name}.class);"
        end
      else
        raise "no java_signature has been set for method #{name}"
      end
    end

    def java_name
      if java_signature
        java_signature.name
      else
        raise "no java_signature has been set for method #{name}"
      end
    end
  end

  class RubyConstructor < RubyMethod
    def initialize(ruby_class, java_signature = nil, annotations = [])
      super(ruby_class, 'initialize', java_signature, annotations)
    end

    def constructor?
      true
    end

    def java_name
      @ruby_class.name
    end

    def return_type
      ''
    end

    def to_s
      declarator_string do
        <<-JAVA
        this(__ruby__, __metaclass__);
#{conversion_string(var_names)}
        RuntimeHelpers.invoke(__ruby__.getCurrentContext(), this, \"initialize\"#{passed_args});
        JAVA
      end
    end
  end
end
