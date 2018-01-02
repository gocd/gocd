module AnalyticsSupport
  include JavaImports

  def with_pipeline_analytics_support(&block)
    return unless block_given?

    default_plugin_info_finder.allPluginInfos(PluginConstants.ANALYTICS_EXTENSION).each do |plugin|
      if plugin.getCapabilities().supportsPipelineAnalytics()
        yield plugin.getDescriptor().id()
      end
    end
  end
end
