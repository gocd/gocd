# -*- ruby encoding: utf-8 -*-

# The namespace for MIME applications, tools, and libraries.
module MIME # :nodoc:
end

class << MIME
  # Used to mark a method as deprecated in the mime-types interface.
  def deprecated(klass, sym, message = nil, &block) # :nodoc:
    level = case klass
            when Class, Module
              '.'
            else
              klass = klass.class
              '#'
            end
    unless defined?(@__deprecated) and @__deprecated["#{klass}#{level}#{sym}"]
      message = case message
                when :private, :protected
                  "and will be #{message}"
                when nil
                  "and will be removed"
                else
                  message
                end
      warn "#{klass}#{level}#{sym} is deprecated #{message}."
      (@__deprecated ||= {})["#{klass}#{level}#{sym}"] = true
      block.call if block
    end
  end

  # MIME::InvalidContentType was moved to MIME::Type::InvalidContentType.
  # Provide a single warning about this fact in the interim.
  def const_missing(name) # :nodoc:
    case name.to_s
    when "InvalidContentType"
      warn_about_moved_constants(name)
      MIME::Type.const_get(name.to_sym)
    else
      super
    end
  end

  private
  def warn_about_moved_constants(name) # :nodoc:
    unless defined?(@__warned_constants) and @__warned_constants[name]
      warn "MIME::#{name} is deprecated. Use MIME::Type::#{name} instead."
      (@__warned_constants ||= {})[name] = true
    end
  end
end
