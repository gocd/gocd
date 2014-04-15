/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.web;

import java.lang.reflect.Method;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.server.util.ErrorHandler;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;

import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import org.apache.log4j.Logger;


public class AnnotatedExceptionResolver extends SimpleMappingExceptionResolver {
    private static final Logger LOG = Logger.getLogger(AnnotatedExceptionResolver.class);

    public ModelAndView resolveException(final HttpServletRequest request, final HttpServletResponse response,
                                         final Object controller,
                                         final Exception ex) {
        if (controller == null) {
            LOG.error("Error handling " + request.getRequestURI(), ex);
            throw bomb(ex);
        }

        Class handlerType = controller.getClass();
        class CallBack implements ReflectionUtils.MethodCallback {
            private ModelAndView mov;

            public ModelAndView getReturnValue() {
                return mov;
            }

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
