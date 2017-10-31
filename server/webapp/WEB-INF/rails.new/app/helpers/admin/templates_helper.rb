module Admin
  module TemplatesHelper
    def default_url_options(options = nil)
      super.reverse_merge(params.only(:allow_pipeline_selection).symbolize_keys)
    end

    TRUE = true.to_s

    def allow_pipeline_selection?
      params[:allow_pipeline_selection] == TRUE
    end
  end
end