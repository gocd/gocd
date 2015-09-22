
class Object
  
  unless method_defined?(:tap)
    def tap # :nodoc:
      yield self
      self
    end
  end
  
  def eval_js(source, options = {})
    Rhino::Context.open(options.merge(:with => self)) do |cxt|
      cxt.eval(source)
    end
  end
  
end
