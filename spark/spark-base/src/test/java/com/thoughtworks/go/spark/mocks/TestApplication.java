/*
 * Copyright 2020 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.spark.mocks;

import com.thoughtworks.go.spark.RoutesHelper;
import com.thoughtworks.go.spark.SparkController;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;
import spark.ExceptionMapper;
import spark.Service;
import spark.Spark;
import spark.route.Routes;
import spark.servlet.SparkApplication;
import spark.staticfiles.StaticFilesConfiguration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TestApplication implements SparkApplication {

    private final RoutesHelper routesHelper;

    public TestApplication(SparkController... sparkControllers) {
        routesHelper = new RoutesHelper(sparkControllers);
    }

    @Override
    public void init() {
        routesHelper.init();
    }

    @Override
    public void destroy() {
        //hacky way to reset the state of the routes between tests
        //see Service.stop(), (not invoked directly because it spawns a thread)
        try {
            Method getInstance = Spark.class.getDeclaredMethod("getInstance");
            getInstance.setAccessible(true);
            Service service = (Service) getInstance.invoke(null);
            //Dependent on current version of Spark
            //This is likely to fail in case of upgrades
            clear(service, "routes", Routes.class);
            clear(service, "exceptionMapper", ExceptionMapper.class);
            clear(service, "staticFilesConfiguration", StaticFilesConfiguration.class);
            ReflectionTestUtils.setField(service, "initialized", false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void clear(Service service, String fieldName, Class<?> fieldType) {
        Field field = ReflectionUtils.findField(Service.class, fieldName);
        field.setAccessible(true);
        Object routes = ReflectionUtils.getField(field, service);
        ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(fieldType, "clear"), routes);
    }
}
