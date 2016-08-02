Rails.application.config.assets.precompile += %w( lib/d3-3.1.5.min.js css/*.css)
Rails.application.config.assets.precompile += %w(single_page_apps/*.js frameworks.css single_page_apps/pipeline_configs.css)
Rails.application.config.assets.precompile += %w(single_page_apps/*.js frameworks.css single_page_apps/agents.css)
Rails.application.config.assets.precompile += %w(*.svg *.eot *.woff *.ttf *.gif *.png *.ico)
