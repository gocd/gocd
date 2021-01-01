/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.web;

import com.thoughtworks.go.server.util.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;


public class AnnotatedExceptionResolver extends SimpleMappingExceptionResolver {
    private static final Logger LOG = LoggerFactory.getLogger(AnnotatedExceptionResolver.class);

    @Override
    public ModelAndView resolveException(final HttpServletRequest request, final HttpServletResponse response,
                                         final Object controller,
                                         final Exception ex) {
        if (controller == null) {
            LOG.error("Error handling {}", request.getRequestURI(), ex);
            throw bomb(ex);
        }

        Class handlerType = controller.getClass();
        class CallBack implements ReflectionUtils.MethodCallback {
            private ModelAndView mov;

            public ModelAndView getReturnValue() {
                return mov;
            }

            @Override
            public void doWith(Method method) {
                if (method.getAnnotation(ErrorHandler.class) != null && method.getReturnType()
                        .equals(ModelAndView.class)) {
                    mov = (ModelAndView) ReflectionUtils
                            .invokeMethod(method, controller, new Object[]{request, response, ex});
                }
            }
        }
        CallBack back = new CallBack();
        ReflectionUtils.doWithMethods(handlerType, back);
        if (back.getReturnValue() != null) {
            return back.getReturnValue();
        }
        return super.resolveException(request, response, controller, ex);
    }


}
