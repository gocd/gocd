# Sets visibility of a class method
class YARD::Handlers::Ruby::PrivateClassMethodHandler < YARD::Handlers::Ruby::Base
  handles method_call(:private_class_method)
  namespace_only

  process do
    errors = []
    statement.parameters.each do |param|
      next unless AstNode === param
      begin
        privatize_class_method(param)
      rescue UndocumentableError => err
        errors << err.message
      end
    end
    if errors.size > 0
      msg = errors.size == 1 ? ": #{errors[0]}" : "s: #{errors.join(", ")}"
      raise UndocumentableError, "private class_method#{msg} for #{namespace.path}"
    end
  end

  private

  def privatize_class_method(node)
    if node.literal?
      method = Proxy.new(namespace, node[0][0][0], :method)
      
      # Proxy will not have a #visibility method when handling inherited class methods
      # like :new, yet "private_class_method :new" is valid Ruby syntax. Therefore
      # if Proxy doesn't respond to #visibility, the object should be skipped.
      # 
      # However, it is important to note that classes can be reopened, and
      # private_class_method can be called inside these reopened classes.
      # Therefore when encountering private_class_method, all of the files need
      # to be parsed before checking if Proxy responds to #visibility. If this
      # is not done, it is possible that class methods may be incorrectly marked
      # public/private.
      parser.parse_remaining_files
      method.visibility = :private if method.respond_to? :visibility
    else
      raise UndocumentableError, "invalid argument to private_class_method: #{node.source}"
    end
  rescue NamespaceMissingError
    raise UndocumentableError, "private visibility set on unrecognized method: #{node[0]}"
  end
end