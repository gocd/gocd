package com.thoughtworks.go.apiv1.adminsconfig

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.adminsconfig.representers.AdminsRepresenter
import com.thoughtworks.go.config.AdminUser
import com.thoughtworks.go.config.AdminsConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.domain.config.Admin
import com.thoughtworks.go.server.service.AdminsConfigService
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.doNothing
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class AdminControllerV1DelegateTest implements ControllerTrait<AdminControllerV1Delegate>, SecurityServiceTrait {

    @BeforeEach
    void setUp() {
        initMocks(this)
    }

    @Mock
    private AdminsConfigService adminsConfigService

    @Mock
    private EntityHashingService entityHashingService


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
                List<Routes.Admins> admins = new ArrayList<Routes.Admins>()
                Admin admin = new AdminUser(new CaseInsensitiveString("admin"))
                admins.add(admin)
                AdminsConfig config = new AdminsConfig(admins)
                when(adminsConfigService.findAdmins()).thenReturn(config)
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
                List<Routes.Admins> admins = new ArrayList<Routes.Admins>()
                Admin admin = new AdminUser(new CaseInsensitiveString("admin"))
                admins.add(admin)
                AdminsConfig config = new AdminsConfig(admins)
                when(entityHashingService.md5ForEntity(config)).thenReturn('md5')
                when(adminsConfigService.findAdmins()).thenReturn(config)

                getWithApiHeader(controller.controllerPath())

                assertThatResponse()
                        .isOk()
                        .hasEtag('"md5"')
                        .hasContentType(controller.mimeType)
                        .hasBodyWithJsonObject(config, AdminsRepresenter)
            }

            @Test
            void 'should return 404 if the security auth admins does not exist'() {
                when(adminsConfigService.findAdmins()).thenReturn(null)
                getWithApiHeader(controller.controllerPath())
                assertThatResponse()
                        .isNotFound()
                        .hasContentType(controller.mimeType)
            }

            @Test
            void 'should render 304 if etag matches'() {
                List<Routes.Admins> admins = new ArrayList<Routes.Admins>()
                Admin admin = new AdminUser(new CaseInsensitiveString("admin"))
                admins.add(admin)
                def config = new AdminsConfig(admins)
                when(entityHashingService.md5ForEntity(config)).thenReturn('md5')
                when(adminsConfigService.findAdmins()).thenReturn(config)
                getWithApiHeader(controller.controllerPath(), ['if-none-match': '"md5"'])

                assertThatResponse()
                        .isNotModified()
                        .hasContentType(controller.mimeType)
            }

            @Test
            void 'should render 200 if etag does not match'() {
                List<Routes.Admins> admins = new ArrayList<Routes.Admins>()
                Admin admin = new AdminUser(new CaseInsensitiveString("admin-new"))
                admins.add(admin)
                def config = new AdminsConfig(admins)
                when(entityHashingService.md5ForEntity(config)).thenReturn('md5')
                when(adminsConfigService.findAdmins()).thenReturn(config)
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
    class Replace {
        @Nested
        class Security implements SecurityTestTrait, AdminUserSecurity {
            AdminsConfig config


            @BeforeEach
            void setUp() {
                List<Routes.Admins> admins = new ArrayList<Routes.Admins>()
                Admin admin = new AdminUser(new CaseInsensitiveString("admin"))
                admins.add(admin)
                config = new AdminsConfig(admins)
                when(adminsConfigService.findAdmins()).thenReturn(config)
                when(entityHashingService.md5ForEntity(config)).thenReturn('cached-md5')
            }

            @Override
            String getControllerMethodUnderTest() {
                return "replaceAndUpdateAdmins"
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
            void 'should replace the admin config'() {
                List<Routes.Admins> admins = new ArrayList<Routes.Admins>()
                Admin admin = new AdminUser(new CaseInsensitiveString("admin"))
                admins.add(admin)
                AdminsConfig config = new AdminsConfig(admins)

                doNothing().when(adminsConfigService).replace(any(), any(), any())
                when(entityHashingService.md5ForEntity(config)).thenReturn("cached-md5")
                when(adminsConfigService.findAdmins()).thenReturn(config)

                putWithApiHeader(controller.controllerPath(), toObjectString({ AdminsRepresenter.toJSON(it, config) }))


                assertThatResponse()
                        .isOk()
                        .hasContentType(controller.mimeType)
                        .hasBodyWithJsonObject(config, AdminsRepresenter)
            }

            @Test
            void 'should fail to save if there are validation errors'() {
                List<Routes.Admins> admins = new ArrayList<Routes.Admins>()
                Admin admin = new AdminUser(new CaseInsensitiveString("admin"))
                admins.add(admin)
                AdminsConfig config = new AdminsConfig(admins)

                when(adminsConfigService.replace(any(), any(), any())).then({ InvocationOnMock invocation ->
                    HttpLocalizedOperationResult result = invocation.getArguments().last()
                    result.unprocessableEntity("validation failed")
                })
                when(adminsConfigService.findAdmins()).thenReturn(config)
                when(entityHashingService.md5ForEntity(config)).thenReturn("cached-md5")

                putWithApiHeader(controller.controllerPath(), toObjectString({ AdminsRepresenter.toJSON(it, config) }))

                assertThatResponse()
                        .isUnprocessableEntity()
                        .hasContentType(controller.mimeType)
                        .hasJsonMessage("validation failed")
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
                List<Routes.Admins> admins = new ArrayList<Routes.Admins>()
                Admin admin = new AdminUser(new CaseInsensitiveString("admin"))
                admins.add(admin)
                config = new AdminsConfig(admins)
                when(adminsConfigService.findAdmins()).thenReturn(config)
                when(entityHashingService.md5ForEntity(config)).thenReturn('cached-md5')
            }

            @Override
            String getControllerMethodUnderTest() {
                return "replaceAndUpdateAdmins"
            }

            @Override
            void makeHttpCall() {
                sendRequest('patch', controller.controllerPath(), [
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
            void 'should replace the admin config'() {
                List<Routes.Admins> admins = new ArrayList<Routes.Admins>()
                Admin admin = new AdminUser(new CaseInsensitiveString("admin"))
                admins.add(admin)
                AdminsConfig config = new AdminsConfig(admins)
                def headers = [
                        'accept'      : controller.mimeType,
                        'If-Match'    : 'cached-md5',
                        'content-type': 'application/json'
                ]
                def body = [
                        'roles' : ['add': ['Dev', 'est'], 'remove': ['Production']],
                        'users' : ['add': ['Linux', 'Firefox'], 'remove': ['Chrome']]
                ]
                doNothing().when(adminsConfigService).replace(any(), any(), any())
                when(entityHashingService.md5ForEntity(config)).thenReturn("cached-md5")
                when(adminsConfigService.findAdmins()).thenReturn(config)

                patchWithApiHeader(controller.controllerPath(), headers, body)


                assertThatResponse()
                        .isOk()
                        .hasContentType(controller.mimeType)
                        .hasBodyWithJsonObject(config, AdminsRepresenter)
            }

            @Test
            void 'should fail to save if there are validation errors'() {
                List<Routes.Admins> admins = new ArrayList<Routes.Admins>()
                Admin admin = new AdminUser(new CaseInsensitiveString("admin"))
                admins.add(admin)
                AdminsConfig config = new AdminsConfig(admins)
                def headers = [
                        'accept'      : controller.mimeType,
                        'If-Match'    : 'cached-md5',
                        'content-type': 'application/json'
                ]
                def body = [
                        'roles' : ['add': ['Dev', 'est'], 'remove': ['Production']],
                        'users' : ['add': ['Linux', 'Firefox'], 'remove': ['Chrome']]
                ]
                when(adminsConfigService.replace(any(), any(), any())).then({ InvocationOnMock invocation ->
                    HttpLocalizedOperationResult result = invocation.getArguments().last()
                    result.unprocessableEntity("validation failed")
                })
                when(adminsConfigService.findAdmins()).thenReturn(config)
                when(entityHashingService.md5ForEntity(config)).thenReturn("cached-md5")

                patchWithApiHeader(controller.controllerPath(), headers, body)


                assertThatResponse()
                        .isUnprocessableEntity()
                        .hasContentType(controller.mimeType)
                        .hasJsonMessage("validation failed")
            }
        }
    }
}
