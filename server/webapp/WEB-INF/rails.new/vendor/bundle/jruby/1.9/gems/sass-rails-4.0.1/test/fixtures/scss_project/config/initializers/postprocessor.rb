Rails.application.assets.register_postprocessor 'text/css', :postprocessor do |context, css|
  css.gsub /@import/, 'fail engine'
end
