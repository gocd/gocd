class JavaInterfaceExtender
  def initialize(java_class_name, &block)
    # don't really need @java_class here any more, keeping around
    # in case any users use this class directly
    @java_class = Java::JavaClass.for_name(java_class_name)
    @block = block
  end
  
  def extend_proxy(proxy_class)
    proxy_class.class_eval(&@block)
  end
end

class InterfaceJavaProxy < JavaProxy
  class << self  
    def new(*outer_args, &block)
      proxy = allocate
      JavaUtilities.set_java_object(proxy, Java.new_proxy_instance(proxy.class.java_class) { |proxy2, method, *args|
        args.collect! { |arg| Java.java_to_ruby(arg) }
        Java.ruby_to_java(proxy.send(method.name, *args))
      })
      proxy.send(:initialize,*outer_args,&block)
      proxy
    end
  end
    
  def self.impl(*meths, &block)
    block = lambda {|*args| send(:method_missing, *args) } unless block

    Class.new(self) do
      define_method(:method_missing) do |name, *args|
        return block.call(name, *args) if meths.empty? || meths.include?(name)
        super
      end
    end.new
  end
end
