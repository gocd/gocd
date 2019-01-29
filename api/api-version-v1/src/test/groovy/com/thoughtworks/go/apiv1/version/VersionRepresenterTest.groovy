package com.thoughtworks.go.apiv1.version

import com.thoughtworks.go.CurrentGoCDVersion
import com.thoughtworks.go.spark.Routes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static java.lang.String.format
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class VersionRepresenterTest {

  private CurrentGoCDVersion currentGoCDVersion

  @BeforeEach
  void setUp() {
    currentGoCDVersion = CurrentGoCDVersion.getInstance()
  }


  @Test
  void 'should render a version with hal representation'() {
    def versionHash = [
      "_links"      : [
        "self": [
          "href": "http://test.host/go/api/version"
        ],
        "doc" : [
          "href": apiDocsUrl('#version')
        ]
      ],
      "version"     : currentGoCDVersion.goVersion(),
      "build_number": currentGoCDVersion.distVersion(),
      "git_sha"     : currentGoCDVersion.gitRevision(),
      "full_version": currentGoCDVersion.formatted(),
      "commit_url"  : format("%s%s", Routes.Version.COMMIT_URL, currentGoCDVersion.gitRevision())
    ]

    def actualJson = toObjectString({ VersionRepresenter.toJSON(it, currentGoCDVersion) })
    assertThatJson(actualJson).isEqualTo(versionHash)
  }
}
