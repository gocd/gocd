require 'jruby'

class Method
  def parameters
    self_r = JRuby.reference0(self)
    method = self_r.get_method
    args_ary = []

    case method
    when MethodArgs2
      return Helpers.parameter_list_to_parameters(JRuby.runtime, method.parameter_list, true)
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