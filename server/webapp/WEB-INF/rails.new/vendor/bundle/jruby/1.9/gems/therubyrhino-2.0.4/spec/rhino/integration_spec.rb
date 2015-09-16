require 'bundler/setup'

require 'rhino'
require 'pathname'
require 'stringio'

puts "Rhino #{Rhino::VERSION} (#{Rhino::JAR_PATH})"

describe 'integration' do
  
  it "loads LESS" do
    require 'less'
  end
  
  it "require foo" do # CommonJS
    environment = new_environment(:console => Console)
    environment.native 'util', Util.new(out = StringIO.new)
    exports = environment.require 'foo'
    out.string.should == "Hello Bar!\n"
    
    exports.should_not be nil
    exports.foo.should respond_to(:'[]')
    exports.foo['Bar'].should respond_to(:'[]')
    exports.foo['Bar'][:puts].should be_a Rhino::JS::Function
  end

  it "require index/loop" do # CommonJS
    environment = new_environment(:console => Console)
    environment.require 'index'
    environment.context['Loop'].should_not be nil
  end
  
  private
  
  def new_environment(globals = {})
    context = Rhino::Context.new
    #context.optimization_level = -1
    globals.each { |key, obj| context[key] = obj }
    path = Pathname(__FILE__).dirname.join('integration')
    Env.new(context, :path => path.to_s)
  end
  
  class Env # a CommonJS like environment (inspired by commonjs.rb)
  
    attr_reader :context, :modules
    
    def initialize(context, options = {})
      @context = context
      @paths = [ options[:path] ].flatten.map { |path| Pathname(path) }
      @modules = {}
    end
    
    def require(module_id)
      unless mod = modules[module_id]
        filepath = find(module_id) or fail LoadError, "no such module '#{module_id}'"
        js = "( function(module, require, exports) {\n#{File.read(filepath)}\n} )"
        load = context.eval(js, filepath.expand_path.to_s)
        modules[module_id] = mod = Module.new(module_id, self)
        load.call(mod, mod.require_function, mod.exports)
      end
      return mod.exports
    end
    
    def native(module_id, impl)
      modules[module_id] = Module::Native.new(impl)
    end
    
    def new_object
      context['Object'].new
    end

    private

    def find(module_id)
      if loadpath = @paths.find { |path| path.join("#{module_id}.js").exist? }
        loadpath.join("#{module_id}.js")
      end
    end
    
    class Module

      attr_reader :id, :exports

      def initialize(id, env)
        @id, @env = id, env
        @exports = env.new_object
        @segments = id.split('/')
      end

      def require_function
        @require_function ||= lambda do |*args|
          this, module_id = *args
          module_id ||= this #backwards compatibility with TRR < 0.10
          @env.require(expand(module_id))
        end
      end

      private

      def expand(module_id)
        return module_id unless module_id =~ /(\.|\..)/
        module_id.split('/').inject(@segments[0..-2]) do |path, element|
          path.tap do
            if element == '.'
              #do nothing
            elsif element == '..'
              path.pop
            else
              path.push element
            end
          end
        end.join('/')
      end
      
      class Native
        
        def initialize(impl); @impl = impl; end
        def exports; @impl; end
        
      end
      
    end

  end
  
  class Util
    
    def initialize(io = STDOUT)
      @io = io
    end
    
    def puts(*args)
      args.each { |arg| @io.puts(arg) }
    end
    
  end
  
  class Console
    
    def self.log(*msgs)
      puts msgs.join(', ')
    end
    
  end
  
end