Rails.application.config.assets.precompile += %w( lib/d3-3.1.5.min.js css/*.css)
Rails.application.config.assets.precompile += %w(frameworks.css single_page_apps/pipeline_configs.css single_page_apps/agents.css single_page_apps/elastic_profiles.css single_page_apps/auth_configs.css single_page_apps/roles.css)
Rails.application.config.assets.precompile += %w(*.svg *.eot *.woff *.ttf *.gif *.png *.ico)
Rails.application.config.assets.precompile += %w( new-theme.css )

if defined?(MithrilRails)
  Rails.application.config.assets.precompile += %w(single_page_apps/pipeline_configs.js)
else
  raise 'You probably ought to remove this file, along with the corresponding JS'
end
