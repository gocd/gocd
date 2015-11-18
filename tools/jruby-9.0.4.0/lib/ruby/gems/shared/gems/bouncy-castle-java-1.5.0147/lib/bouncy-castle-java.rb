Dir[File.expand_path('bc*.jar', File.dirname(__FILE__))].each do |file|
  require File.basename(file)
end
