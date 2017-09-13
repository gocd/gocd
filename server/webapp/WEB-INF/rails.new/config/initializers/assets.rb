Rails.application.config.assets.precompile += %w( lib/d3-3.1.5.min.js css/*.css)
Rails.application.config.assets.precompile += %w(frameworks.css single_page_apps/pipeline_configs.css single_page_apps/agents.css single_page_apps/elastic_profiles.css single_page_apps/preferences.css single_page_apps/auth_configs.css single_page_apps/roles.css single_page_apps/plugins.css)
Rails.application.config.assets.precompile += %w(*.svg *.eot *.woff *.ttf *.gif *.png *.ico)
Rails.application.config.assets.precompile += %w( new-theme.css )
