module JavaArrayUtilities
  class << self
    def java_to_ruby(java_array)
      unless java_array.kind_of?(ArrayJavaProxy)
        raise ArgumentError,"not a Java array: #{java_array}"
      end
      length = java_array.length
      ruby_array = Array.new(length)
      if length > 0
        if java_array[0].kind_of?ArrayJavaProxy
          length.times do |i|
            ruby_array[i] = java_to_ruby(java_array[i])      
          end
        else
          length.times do |i|
            ruby_array[i] = java_array[i]
          end
        end
      end
      ruby_array
    end
  end #self
end #JavaArrayUtilities
