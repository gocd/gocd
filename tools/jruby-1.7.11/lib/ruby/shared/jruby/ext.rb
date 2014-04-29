require 'java'
require 'jruby'

module JRuby
  MethodArgs = org.jruby.internal.runtime.methods.MethodArgs
  MethodArgs2 = org.jruby.internal.runtime.methods.MethodArgs2
  IRMethodArgs = org.jruby.internal.runtime.methods.IRMethodArgs
  Helpers = org.jruby.runtime.Helpers
  MultipleAsgn19Node = org.jruby.ast.MultipleAsgn19Node
  UnnamedRestArgNode = org.jruby.ast.UnnamedRestArgNode
  Arity = org.jruby.runtime.Arity
  
  # Extensions only provides one feature right now: stealing methods from one
  # class/module and inserting them into another.
  module Extensions
    
    # Transplant the named method from the given type into self. If self is a
    # module/class, it will gain the method. If self is not a module/class, then
    # the self object's singleton class will be used.
    def steal_method(type, method_name)
      if self.kind_of? Module
        to_add = self
      else
        to_add = JRuby.reference0(self).singleton_class
      end
      
      method_name = method_name.to_str
      
      raise TypeError, "first argument must be a module/class" unless type.kind_of? Module
      
      method = JRuby.reference0(type).search_method(method_name)
      
      if !method || method.undefined?
        raise ArgumentError, "no such method `#{method_name}' on type #{type}"
      end
      
      JRuby.reference0(to_add).add_method(method)
      
      nil
    end
    module_function :steal_method
    
    # Transplant all named methods from the given type into self. See
    # JRuby::Extensions.steal_method
    def steal_methods(type, *method_names)
      for method_name in method_names do
        steal_method(type, method_name)
      end
    end
  end
  
  class ::Method
    def args
      self_r = JRuby.reference0(self)
      method = self_r.get_method
      args_ary = []
      
      case method
      when MethodArgs2
        return Helpers.parameter_list_to_parameters(JRuby.runtime, method.parameter_list, true)
      when IRMethodArgs
        arg_desc = method.parameter_list
        for a in arg_desc
          args_ary << (a[1] == "" ? [a[0].to_sym] : [a[0].to_sym, a[1].to_sym])
        end
      when MethodArgs
        args_node = method.args_node
        
        # "pre" required args
        required_pre = args_node.pre
        if required_pre
          for req_pre_arg in required_pre.child_nodes
            if req_pre_arg.kind_of? MultipleAsgn19Node
              args_ary << [:req]
            else
              args_ary << [:req, req_pre_arg ? req_pre_arg.name.intern : nil]
            end
          end
        end
        
        # optional args in middle
        optional = args_node.opt_args
        if optional
          for opt_arg in optional.child_nodes
            args_ary << [:opt, opt_arg ? opt_arg.name.intern : nil]
          end
        end
        
        # rest arg
        if args_node.rest_arg >= 0
          rest = args_node.rest_arg_node
          
          if rest.kind_of? UnnamedRestArgNode
            if rest.star?
              args_ary << [:rest]
            end
          else
            args_ary << [:rest, rest ? rest.name.intern : nil]
          end
        end
        
        # "post" required args
        required_post = args_node.post
        if required_post
          for req_post_arg in required_post.child_nodes
            if req_post_arg.kind_of? MultipleAsgn19Node
              args_ary << [:req]
            else
              args_ary << [:req, req_post_arg ? req_post_arg.name.intern : nil]
            end
          end
        end
        
        # block arg
        block = args_node.block
        if block
          args_ary << [:block, block.name.intern]
        end
      else
        if method.arity == Arity::OPTIONAL
          args_ary << [:rest]
        end
      end
      
      args_ary
    end
  end
end
