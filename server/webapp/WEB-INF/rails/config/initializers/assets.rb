# Be sure to restart your server when you modify this file.

# Version of your assets, change this if you want to expire all your assets.
Rails.application.config.assets.version = '1.0'

# Add Yarn node_modules folder to the asset load path.
Rails.application.config.assets.paths << Rails.root.join('node_modules')

Rails.application.config.assets.precompile += %w(lib/d3-3.1.5.min.js css/*.css)
Rails.application.config.assets.precompile += %w(frameworks.css single_page_apps/agents.css single_page_apps/elastic_profiles.css single_page_apps/artifact_stores.css single_page_apps/preferences.css single_page_apps/analytics.css single_page_apps/auth_configs.css single_page_apps/roles.css single_page_apps/plugins.css single_page_apps/new_dashboard.css single_page_apps/data_sharing_settings.css)
Rails.application.config.assets.precompile += %w(*.svg *.eot *.woff *.ttf *.gif *.png *.ico)
Rails.application.config.assets.precompile += %w( new-theme.css )

Rails.application.config.assets.paths << Rails.root.join("webpack", "rails-shared")
