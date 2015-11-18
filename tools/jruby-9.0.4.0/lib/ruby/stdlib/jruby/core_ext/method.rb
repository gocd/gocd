require 'jruby'

class Method
  def parameters
    self_r = JRuby.reference0(self)
    method = self_r.get_method
    args_ary = []

    case method
    when MethodArgs2
      return Helpers.parameter_list_to_parameters(JRuby.runtime, method.parameter_list, true)
    else
      if method.arity == Arity::OPTIONAL
        args_ary << [:rest]
      end
    end

    args_ary
  end
end
