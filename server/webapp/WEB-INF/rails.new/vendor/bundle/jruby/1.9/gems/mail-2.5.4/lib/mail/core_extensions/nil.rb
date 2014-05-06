# encoding: utf-8

# This is not loaded if ActiveSupport is already loaded

class NilClass #:nodoc:
  unless nil.respond_to? :blank?
    def blank?
      true
    end
  end

  def to_crlf
    ''
  end

  def to_lf
    ''
  end
end
