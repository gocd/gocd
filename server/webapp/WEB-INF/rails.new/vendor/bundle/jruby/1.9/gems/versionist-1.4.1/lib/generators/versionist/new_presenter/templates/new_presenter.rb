class <%= module_name %>::<%= class_name%>Presenter < <%= module_name %>::BasePresenter

  def initialize(<%= file_name %>)
    @<%= file_name %> = <%= file_name %>
  end

  def as_json(options={})
    # fill me in...
  end

  def to_xml(options={}, &block)
    xml = options[:builder] ||= Builder::XmlMarkup.new
    # fill me in...
  end
end
