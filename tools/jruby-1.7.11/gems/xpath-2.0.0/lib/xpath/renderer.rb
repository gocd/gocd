module XPath
  class Renderer
    def self.render(node, type)
      new(type).render(node)
    end

    def initialize(type)
      @type = type
    end

    def render(node)
      arguments = node.arguments.map { |argument| convert_argument(argument) }
      send(node.expression, *arguments)
    end

    def convert_argument(argument)
      case argument
        when Expression, Union then render(argument)
        when Array then argument.map { |element| convert_argument(element) }
        when String then string_literal(argument)
        when Literal then argument.value
        else argument.to_s
      end
    end

    def string_literal(string)
      if string.include?("'")
        string = string.split("'", -1).map do |substr|
          "'#{substr}'"
        end.join(%q{,"'",})
        "concat(#{string})"
      else
        "'#{string}'"
      end
    end

    def this_node
      '.'
    end

    def descendant(parent, element_names)
      if element_names.length == 1
        "#{parent}//#{element_names.first}"
      elsif element_names.length > 1
        "#{parent}//*[#{element_names.map { |e| "self::#{e}" }.join(" | ")}]"
      else
        "#{parent}//*"
      end
    end

    def child(parent, element_names)
      if element_names.length == 1
        "#{parent}/#{element_names.first}"
      elsif element_names.length > 1
        "#{parent}/*[#{element_names.map { |e| "self::#{e}" }.join(" | ")}]"
      else
        "#{parent}/*"
      end
    end

    def axis(parent, name, tag_name)
      "#{parent}/#{name}::#{tag_name}"
    end

    def node_name(current)
      "name(#{current})"
    end

    def where(on, condition)
      "#{on}[#{condition}]"
    end

    def attribute(current, name)
      "#{current}/@#{name}"
    end

    def equality(one, two)
      "#{one} = #{two}"
    end

    def is(one, two)
      if @type == :exact
        equality(one, two)
      else
        contains(one, two)
      end
    end

    def variable(name)
      "%{#{name}}"
    end

    def text(current)
      "#{current}/text()"
    end

    def normalized_space(current)
      "normalize-space(#{current})"
    end

    def literal(node)
      node
    end

    def css(current, selector)
      paths = Nokogiri::CSS.xpath_for(selector).map do |xpath_selector|
        "#{current}#{xpath_selector}"
      end
      union(paths)
    end

    def union(*expressions)
      expressions.join(' | ')
    end

    def anywhere(element_names)
      if element_names.length == 1
        "//#{element_names.first}"
      elsif element_names.length > 1
        "//*[#{element_names.map { |e| "self::#{e}" }.join(" | ")}]"
      else
        "//*"
      end
    end

    def contains(current, value)
      "contains(#{current}, #{value})"
    end

    def starts_with(current, value)
      "starts-with(#{current}, #{value})"
    end

    def and(one, two)
      "(#{one} and #{two})"
    end

    def or(one, two)
      "(#{one} or #{two})"
    end

    def one_of(current, values)
      values.map { |value| "#{current} = #{value}" }.join(' or ')
    end

    def next_sibling(current, element_names)
      if element_names.length == 1
        "#{current}/following-sibling::*[1]/self::#{element_names.first}"
      elsif element_names.length > 1
        "#{current}/following-sibling::*[1]/self::*[#{element_names.map { |e| "self::#{e}" }.join(" | ")}]"
      else
        "#{current}/following-sibling::*[1]/self::*"
      end
    end

    def previous_sibling(current, element_names)
      if element_names.length == 1
        "#{current}/preceding-sibling::*[1]/self::#{element_names.first}"
      elsif element_names.length > 1
        "#{current}/preceding-sibling::*[1]/self::*[#{element_names.map { |e| "self::#{e}" }.join(" | ")}]"
      else
        "#{current}/preceding-sibling::*[1]/self::*"
      end
    end

    def inverse(current)
      "not(#{current})"
    end

    def string_function(current)
      "string(#{current})"
    end
  end
end
