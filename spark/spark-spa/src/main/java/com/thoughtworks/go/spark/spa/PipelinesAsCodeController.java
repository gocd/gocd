package com.thoughtworks.go.spark.spa;

import com.thoughtworks.go.spark.SparkController;
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.TemplateEngine;

import java.util.HashMap;

import static com.thoughtworks.go.spark.Routes.PipelineConfig.SPA_AS_CODE;
import static com.thoughtworks.go.spark.Routes.PipelineConfig.SPA_BASE;
import static spark.Spark.*;

public class PipelinesAsCodeController implements SparkController {
    private SPAAuthenticationHelper authenticationHelper;
    private TemplateEngine engine;

    public PipelinesAsCodeController(SPAAuthenticationHelper authenticationHelper, TemplateEngine engine) {
        this.authenticationHelper = authenticationHelper;
        this.engine = engine;
    }

    @Override
    public String controllerBasePath() {
        return SPA_BASE;
    }

    @Override
    public void setupRoutes() {
        path(controllerBasePath(), () -> {
            before(SPA_AS_CODE, authenticationHelper::checkAdminUserAnd403);
            get(SPA_AS_CODE, this::asCode, engine);
        });
    }

    public ModelAndView asCode(Request req, Response res) {
        HashMap<Object, Object> object = new HashMap<Object, Object>() {{
            put("viewTitle", "New Pipeline as Code");
        }};

        return new ModelAndView(object, null);
    }
}
