#responsible for pretty printing hal+json responses for API clients

%w(v1 v2 v3 v4 v5).each do |version|
  mime_type = "application/vnd.go.cd.#{version}+json"
  symbol    = "json_hal_#{version}".to_sym

  Mime::Type.register mime_type, symbol

  ActionController::Renderers.add symbol do |json, options|
    json = JSON.pretty_generate(json.as_json, options) << "\n" unless json.kind_of?(String)
    json = "#{options[:callback]}(#{json})" unless options[:callback].blank?
    render body: json, content_type: mime_type, layout: false
  end
end
