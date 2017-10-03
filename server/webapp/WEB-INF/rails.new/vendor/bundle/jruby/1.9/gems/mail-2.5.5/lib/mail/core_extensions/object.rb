# encoding: utf-8

unless Object.method_defined? :blank?
  class Object
    def blank?
      if respond_to?(:empty?)
        empty?
      else
        !self
      end
    end
  end
end
