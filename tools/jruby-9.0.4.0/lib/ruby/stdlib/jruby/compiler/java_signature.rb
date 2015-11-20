require 'java'

module JRuby
  class JavaSignature
    def initialize(string, ast)
      @string, @ast = string, ast
    end

    def name
      @ast.name
    end

    # FIXME: Can make this accept whole list too if that is actual contract
    # FIXME: Can be literals too
    def as_java_type(string)
      type = primitive? string
      return type if type

      if string.is_a?(Java::OrgJrubyAstJava_signature::ReferenceTypeNode)
        return eval make_class_jiable(string.getFullyTypedName())
      end

      # If annotation makes it in strip @ before we try and match it.
      string = string[1..-1] if string.start_with? '@'

      eval make_class_jiable(string)
    end


    ##
    # return live JI proxy for return type
    def return_type
      as_java_type(@ast.return_type)
    end

    ##
    # {org.bukkit.event.EventHandler => {}, }
    def annotations
      annotations = @ast.modifiers.select(&:is_annotation?)
      annotations.inject({}) do |hash, anno_node|
        hash[as_java_type(anno_node.name)] = process_annotation_params(anno_node)
        hash
      end
    end

    def modifiers
      @ast.modifiers.reject(&:is_annotation?)
    end

    ##
    # return JI proxies for all parameters (excluding return_type)
    def parameters
      @ast.parameters.map { |p| as_java_type(p.type) }
    end

    ##
    # {return_type, *parameters} tuple (JI proxies)
    def types
      [return_type, *parameters]
    end

    def to_s
      @string
    end

    def to_s_ast
      @ast
    end

    def inspect
      <<-EOS
name       : #{name}
modifiers  : #{modifiers.join(', ')}
annotations: #{annotations.join(', ')}
parameters : #{parameters.join(', ')}
return_type: #{return_type}
    EOS
    end

    def self.parse(signature)
      stream = java.io.ByteArrayInputStream.new(signature.to_s.to_java_bytes)
      ast = org.jruby.parser.JavaSignatureParser.parse(stream)
      new signature, ast
    end

    def process_annotation_params(anno_node)
      anno_node.parameters.inject({}) do |hash, param|
        # ??? default will be nil/null name here.
        hash[param.name] = as_java_type(expr)
        hash
      end
    end
    private :process_annotation_params

    # FIXME: Somewhere must have this already?
    PRIMITIVES = {
      "void" => Java::java.lang.Void::TYPE,
       "int" => Java::java.lang.Integer::TYPE, 
      "long" => Java::java.lang.Long::TYPE,
      "double" => Java::java.lang.Double::TYPE,
      "float" => Java::java.lang.Float::TYPE,
      "boolean" => Java::java.lang.Boolean::TYPE
    }

    def primitive?(string)
      PRIMITIVES[string.to_s]
    end

    def make_class_jiable(string)
      new_list = []
      string.split(/\./).inject(false) do |last_cap, segment|
        if segment =~ /[A-Z]/
          if last_cap
            new_list << "::" + segment
          else
            new_list << "." + segment
          end
          last_cap = true
        else
          new_list << "." + segment
          last_cap = false
        end
      end
      "Java::#{new_list.join("")[1..-1]}"
    end
  end
end
