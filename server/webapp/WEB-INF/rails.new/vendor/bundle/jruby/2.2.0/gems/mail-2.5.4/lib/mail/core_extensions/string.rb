# encoding: utf-8
class String #:nodoc:
  def to_crlf
    to_str.gsub(/\n|\r\n|\r/) { "\r\n" }
  end

  def to_lf
    to_str.gsub(/\n|\r\n|\r/) { "\n" }
  end

  unless String.instance_methods(false).map {|m| m.to_sym}.include?(:blank?)
    def blank?
      self !~ /\S/
    end
  end

  unless method_defined?(:ascii_only?)
    # Backport from Ruby 1.9 checks for non-us-ascii characters.
    def ascii_only?
      self !~ MATCH_NON_US_ASCII
    end

    MATCH_NON_US_ASCII = /[^\x00-\x7f]/
  end

  def not_ascii_only?
    !ascii_only?
  end

  unless method_defined?(:bytesize)
    alias :bytesize :length
  end
end
