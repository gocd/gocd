# This class enables the syntax arr = MyClass[n][n]...[n].new
class ArrayJavaProxyCreator
  def initialize(java_class,*args)
    @java_class = java_class
    @dimensions = []
    extract_dimensions(args)
  end
  
  def [](*args)
    extract_dimensions(args)
    self
  end
  
  private
  def extract_dimensions(args)
    unless args.length > 0
      raise ArgumentError,"empty array dimensions specified"    
    end
    args.each do |arg|
      unless arg.kind_of?(Fixnum)
        raise ArgumentError,"array dimension length must be Fixnum"    
      end
      @dimensions << arg
    end  
  end
  public
  
  def new()
    array = @java_class.new_array(@dimensions)
    array_class = @java_class.array_class
    (@dimensions.length-1).times do
      array_class = array_class.array_class    
    end
    proxy_class = JavaUtilities.get_proxy_class(array_class)
    proxy_class.new(array)
  end
end