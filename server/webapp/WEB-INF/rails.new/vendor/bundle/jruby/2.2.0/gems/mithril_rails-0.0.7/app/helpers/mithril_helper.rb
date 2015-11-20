module MithrilHelper
  def mithril_component(name, args = {}, options = {}, &block)
    options = {:tag => options} if options.is_a?(Symbol)

    html_options = options.reverse_merge(:data => {})
    html_options[:data].tap do |data|
      data[:mithril_class] = name
      data[:mithril_props] = args.to_json unless args.empty?
    end
    html_tag = html_options.delete(:tag) || :div

    content_tag(html_tag, '', html_options, &block)
  end
end
