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
package com.thoughtworks.go.api.pluginimages

import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo
import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo
import com.thoughtworks.go.plugin.domain.common.Image
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SparkController
import com.thoughtworks.go.spark.util.SecureRandom
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static java.nio.charset.StandardCharsets.UTF_8
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class PluginImagesControllerTest implements ControllerTrait<SparkController> {

  @Mock
  DefaultPluginInfoFinder defaultPluginInfoFinder

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  PluginImagesController createControllerInstance() {
    return new PluginImagesController(defaultPluginInfoFinder)
  }

  @Nested
  class Show {
    @Test
    void 'should render an image with a hash and a long lived cache header'() {
      def image = new Image('image/foo', Base64.encoder.encodeToString('some-image-data'.getBytes(UTF_8)), SecureRandom.hex(32))
      def pluginInfo = new CombinedPluginInfo(new AuthorizationPluginInfo(null, null, null, image, null))
      when(defaultPluginInfoFinder.pluginInfoFor('foo')).thenReturn(pluginInfo)

      get(controller.controllerPath("/foo/${image.getHash()}"))

      assertThatResponse()
        .isOk()
        .hasContentType('image/foo')
        .hasCacheControl('max-age=31557600, public')
        .hasBody(image.getDataAsBytes())
    }

    @Test
    void 'should render 304 when etag matches'() {
      def image = new Image('image/foo', Base64.getEncoder().encodeToString('some-image-data'.getBytes(UTF_8)), SecureRandom.hex(32))
      def pluginInfo = new CombinedPluginInfo(new AuthorizationPluginInfo(null, null, null, image, null))
      when(defaultPluginInfoFinder.pluginInfoFor('foo')).thenReturn(pluginInfo)

      get(controller.controllerPath("/foo/${image.getHash()}"), ['if-none-match': $/"${image.getHash()}"/$])

      assertThatResponse()
        .isNotModified()
        .hasCacheControl('max-age=31557600, public')
        .hasNoBody()
    }

    @Test
    void 'renders 404 when plugin does not match'() {
      when(defaultPluginInfoFinder.pluginInfoFor('foo')).thenReturn(null)

      get(controller.controllerPath("/foo/random-hash"))

      assertThatResponse()
        .isNotFound()
    }

    @Test
    void 'renders 404 when hash does not match'() {
      def image = new Image('image/foo', Base64.getEncoder().encodeToString('some-image-data'.getBytes(UTF_8)), SecureRandom.hex(32))
      def pluginInfo = new CombinedPluginInfo(new AuthorizationPluginInfo(null, null, null, image, null))
      when(defaultPluginInfoFinder.pluginInfoFor('foo')).thenReturn(pluginInfo)

      get(controller.controllerPath("/foo/random-hash"))

      assertThatResponse()
        .isNotFound()
    }
  }
}
