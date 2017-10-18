
module Api
  module PipelinesHelper
    include JavaImports

    def url(params={})#FIXME: we should not have such common method names(we work with all helpers mixed in each response, which means we need to be have more specific method names to avoid conflicts)
      api_pipeline_stage_feed_url(params)
    end

    def resource_url(params={})#FIXME: we should not have such common method names(we work with all helpers mixed in each response, which means we need to be have more specific method names to avoid conflicts)
      stage_url(params)
    end

    def page_url(stage_locator, options = {})#FIXME: we should not have such common method names(we work with all helpers mixed in each response, which means we need to be have more specific method names to avoid conflicts)
      stage_identifier = StageIdentifier.new(stage_locator)
      stage_detail_tab_url({:pipeline_name => stage_identifier.getPipelineName(),
                        :pipeline_counter => stage_identifier.getPipelineCounter().to_s,
                        :stage_name => stage_identifier.getStageName(),
                        :stage_counter => stage_identifier.getStageCounter()}.merge(options))
    end

  end
end