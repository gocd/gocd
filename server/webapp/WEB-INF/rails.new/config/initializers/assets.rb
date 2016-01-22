Rails.application.config.assets.precompile += %w( lib/d3-3.1.5.min.js css/*.css)
Rails.application.config.assets.precompile += %w(pipeline_configs/application.js pipeline_configs/application.css)
Rails.application.config.assets.precompile += %w(*.svg *.eot *.woff *.ttf *.gif *.png *.ico)
