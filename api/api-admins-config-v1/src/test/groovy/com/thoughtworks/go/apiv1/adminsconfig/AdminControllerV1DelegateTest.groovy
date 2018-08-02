package com.thoughtworks.go.apiv1.adminsconfig

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.adminsconfig.representers.AdminsRepresenter
import com.thoughtworks.go.config.AdminRole
import com.thoughtworks.go.config.AdminUser
import com.thoughtworks.go.config.AdminsConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.server.service.AdminsConfigService
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.api.util.HaltApiMessages.etagDoesNotMatch
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class AdminControllerV1DelegateTest implements ControllerTrait<AdminControllerV1Delegate>, SecurityServiceTrait {
    @Mock
    private AdminsConfigService adminsConfigService
    @Mock
    private EntityHashingService entityHashingService

    @BeforeEach
    void setUp() {
        initMocks(this)
    }

    @Override
    AdminControllerV1Delegate createControllerInstance() {
        return new AdminControllerV1Delegate(new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService, adminsConfigService);
    }

    @Nested
    class Show {
        @Nested
        class Security implements SecurityTestTrait, AdminUserSecurity {
            @BeforeEach
            void setUp() {
                AdminsConfig config = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))
                when(adminsConfigService.systemAdmins()).thenReturn(config)
            }

            @Override
            String getControllerMethodUnderTest() {
                return "show"
            }

            @Override
            void makeHttpCall() {
                getWithApiHeader(controller.controllerPath())
            }
        }

        @Nested
        class AsAdmin {
            HttpLocalizedOperationResult result

            @BeforeEach
            void setUp() {
                enableSecurity()
                loginAsAdmin()
                this.result = new HttpLocalizedOperationResult()
            }

            @Test
            void 'should render the security admins config'() {
                AdminsConfig config = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))
                when(entityHashingService.md5ForEntity(config)).thenReturn('md5')
                when(adminsConfigService.systemAdmins()).thenReturn(config)

                getWithApiHeader(controller.controllerPath())

                assertThatResponse()
                        .isOk()
                        .hasEtag('"md5"')
                        .hasContentType(controller.mimeType)
                        .hasBodyWithJsonObject(config, AdminsRepresenter)
            }

            @Test
            void 'should render 304 if etag matches'() {
                def config = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))
                when(entityHashingService.md5ForEntity(config)).thenReturn('md5')
                when(adminsConfigService.systemAdmins()).thenReturn(config)
                getWithApiHeader(controller.controllerPath(), ['if-none-match': '"md5"'])

                assertThatResponse()
                        .isNotModified()
                        .hasContentType(controller.mimeType)
            }

            @Test
            void 'should render 200 if etag does not match'() {
                def config = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin-new")))
                when(entityHashingService.md5ForEntity(config)).thenReturn('md5')
                when(adminsConfigService.systemAdmins()).thenReturn(config)
                getWithApiHeader(controller.controllerPath(), ['if-none-match': '"junk"'])

                assertThatResponse()
                        .isOk()
                        .hasEtag('"md5"')
                        .hasContentType(controller.mimeType)
                        .hasBodyWithJsonObject(config, AdminsRepresenter)
            }
        }
    }


    @Nested
    class Update {
        @Nested
        class Security implements SecurityTestTrait, AdminUserSecurity {
            AdminsConfig config

            @BeforeEach
            void setUp() {
                config = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))
                when(adminsConfigService.systemAdmins()).thenReturn(config)
                when(entityHashingService.md5ForEntity(config)).thenReturn('cached-md5')
            }

            @Override
            String getControllerMethodUnderTest() {
                return "update"
            }

            @Override
            void makeHttpCall() {
                sendRequest('put', controller.controllerPath(), [
                        'accept'      : controller.mimeType,
                        'If-Match'    : 'cached-md5',
                        'content-type': 'application/json'
                ], toObjectString({ AdminsRepresenter.toJSON(it, this.config) }))
            }
        }

        @Nested
        class AsAdmin {
            @BeforeEach
            void setUp() {
                enableSecurity()
                loginAsAdmin()
            }

            @Test
            void 'should update the system admins'() {
                AdminsConfig configInServer = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))
                AdminsConfig configFromRequest = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")),
                  new AdminUser(new CaseInsensitiveString("new_admin")))

                when(adminsConfigService.systemAdmins()).thenReturn(configInServer)
                when(entityHashingService.md5ForEntity(configInServer)).thenReturn("cached-md5")

                putWithApiHeader(controller.controllerPath(), ['if-match': 'cached-md5'], toObjectString({ AdminsRepresenter.toJSON(it, configFromRequest) }))

                verify(adminsConfigService).update(any(), eq(configFromRequest), eq("cached-md5"), any(HttpLocalizedOperationResult.class));
                assertThatResponse()
                        .isOk()
                        .hasContentType(controller.mimeType)
                        .hasBodyWithJsonObject(configFromRequest, AdminsRepresenter)
            }

            @Test
            void 'should return a response with errors if update fails'() {
                AdminsConfig configInServer = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")))
                AdminsConfig configFromRequest = new AdminsConfig(new AdminUser(new CaseInsensitiveString("admin")),
                  new AdminUser(new CaseInsensitiveString("new_admin")))


                when(adminsConfigService.systemAdmins()).thenReturn(configInServer)
                when(entityHashingService.md5ForEntity(configInServer)).thenReturn("cached-md5")
                when(adminsConfigService.update(any(), eq(configFromRequest), eq("cached-md5"), any(HttpLocalizedOperationResult.class))).then({ InvocationOnMock invocation ->
                    HttpLocalizedOperationResult result = invocation.getArguments().last()
                    result.unprocessableEntity("validation failed")
                })

                putWithApiHeader(controller.controllerPath(), ['if-match': 'cached-md5'], toObjectString({ AdminsRepresenter.toJSON(it, configFromRequest) }))

                assertThatResponse()
                        .isUnprocessableEntity()
                        .hasContentType(controller.mimeType)
                        .hasJsonMessage("validation failed")
            }

            @Test
            void 'should not update a stale system admins request'() {
                AdminsConfig systemAdminsRequest = new AdminsConfig(new AdminRole(new CaseInsensitiveString("admin")))
                AdminsConfig systemAdminsInServer = new AdminsConfig(new AdminRole(new CaseInsensitiveString("role1")))

                when(adminsConfigService.systemAdmins()).thenReturn(systemAdminsInServer)
                when(entityHashingService.md5ForEntity(systemAdminsInServer)).thenReturn('cached-md5')

                putWithApiHeader(controller.controllerPath(), ['if-match': 'some-string'], toObjectString({
                    AdminsRepresenter.toJSON(it, systemAdminsRequest)
                }))

                verify(adminsConfigService, Mockito.never()).update(any(), any(), any(), any())
                assertThatResponse().isPreconditionFailed()
                        .hasContentType(controller.mimeType)
                        .hasJsonMessage(etagDoesNotMatch("system_admins", ""))
            }
        }
    }
}
