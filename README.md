# springboot-kits
后端通用脚手架。

文档说明。

Demo工程使用springboot开发，使用maven对jar包进行管理，对于不使用maven进行管理，直接使用jar包放在工程下的，出现类找不到的情况，需要各工具自己去找缺少的jar包

maven私服库为云龙仓库

http://szxy1.artifactory.cd-cloud-artifact.tools.huawei.com/artifactory/sz-maven-public

maven配置文件可以参考



 

主要提供的功能：

1.       连接数据库

2.       日志打印

3.       调用Support的接口

4.       文件上传

5.       根据Support角色进行鉴权

 

目录结构



 

1.       连接数据库

数据库连接使用dbcp2连接池，需要在database.properties中配置数据库的url、用户名、密码信息，密码需要配置密文，

对明文使用HwEncryptUtil.encryptAESWithHWSF方法进行加密，生产环境数据库无法获取密码的明文，需要提交变更，让安控办远程在如下页面输入明文进行加密，然后复制密文，将密文配置到database.properties中。

http://supportops.huawei.com/opsweb/pages/encrypt/aesencrypt.jsp?type=1

dbcp2的配置在DatabaseConfig.java文件中完成，目前demo中只进行了最基本的配置，如需要配置dbcp2数据库连接池的其它自定义设置，请在此处设置



 

2.       日志打印

默认日志级别为error，单个日志文件50M，保留30天，请务必修改logback-spring.xml中的<property name="log.path" value="/applog/demo" />

，HIS申请的机器都有/applog目录，将demo修改为自己工具的名称即可。本地开发时，日志打印在eclipse工作空间所在磁盘的/applog目录下

 

3.       调用Support接口

引入了工具平台的SDK包，该包中仅提供使用http的方式对Support的接口进行调用， 使用的是OkHttp

<dependency>

                     <groupId>com.huawei.support</groupId>

                     <artifactId>onlinetoolservice-sdk</artifactId>

                     <version>20.0.1-SNAPSHOT</version>

            </dependency>

 

对Support的接口进行调用，需要在apicenter中订阅需要调用的接口，搜索onlinetoolservice可以找到所有工具平台对外暴露的接口

http://kwe-beta.huawei.com/sgovernance/servicestore/#/



 

进入到特定的接口后，点击立即订阅，提供方处”选择API”可以一次订阅多个接口，消费方选择工具产品线在HIS中注册的应用，比如传送接入资料在线工具，英文名：tainfoonlinetools，应用APPID：com.huawei.tainfo.onlinetools



 

订阅完成后，在HIS的应用中心中，查看应用的token



在onlinetool.properties中配置三个配置项，测试环境的默认使用的appId=com.huawei.support.hue，订阅了所有接口，在测试环境开发验证时可以不进行修改，但生产环境肯定是要各个产品线自己订阅接口的。

env=uat（测试环境uat，生产环境pro）

appId=工具的appid

appToken=工具应用的token

 

然后就可以使用如下方法调用Support在线工具平台的接口，在线工具平台的接口都已在OnlineToolService中进行了定义，可直接调用。

String supportRoles = OnlineToolService.getSupportUserRoles(userId);

如果需要调用OnlineToolService中未定义的其它服务提供的apicenter中的接口，则可以使用OpenApiClient的doGet和doPost方法

 计划对外暴露的接口列表，除已发布到生产的接口外，如需使用其它接口请在使用前联系w00314669进行接口联调和确认上线时间：

OnlineToolService.getSupportUserRoles("w00314669"); 已发布到生产

OnlineToolService.getSupportEUserRoles("w00314669"); 已发布到生产

OnlineToolService.getSysUserIdListByUserId("[\"swx303584\"]"); 已发布到生产
OnlineToolService.getUserIdListBySysUserId("[\"SU1001731356\"]"); 已发布到生产
OnlineToolService.getUserGroupSpaces("{\"groupType\":\"4\",\"sysUserId\":\"w00314669\",\"status\":\"1\"}"); 已发布到生产

OnlineToolService.getCarrierResourcesPermission("DOC1100099993", "SU1001731356"); 已发布到生产
OnlineToolService.getEnterpriseResourcesPermission("DOC2000055900,PSW1000000540", "SU1001731356"); 已发布到生产

OnlineToolService.getCarrierResources("SU1001835606", "PBI1-13627", "NL-zh"); 已发布到生产
OnlineToolService.getEnterpriseResources("SU1001835606", "PBI2-13627", "ENL-zh"); 已发布到生产

----------------------------------------------------------------------------------------------------------------------------------------------------------

OnlineToolService.getHdxInfo("DOC1100099993");

OnlineToolService.getSupportEHdxInfo("EDOC1000088205");
OnlineToolService.getSupportEHdxInfoList("DOC1100099966,EDOC1000088205,NEWS1100027879");
OnlineToolService.getHdxInfoList("DOC1100099966,DOC1100099969");
OnlineToolService.getLatestDownloadURL("DOC1000045754", "6001", "SUP_DOC");
OnlineToolService.getNodeAttributeByNodeId("EDOC2000119843");
OnlineToolService
.getNodeListByPBI("{\"pageIndex\":0,\"pageSize\":2,\"tid\":\"PBI1-7586384\",\"fileType\":\"FT-hdx\"}");
OnlineToolService.getNodeListCountByPBI("{\"tid\":\"PBI1-7639525\",\"fileType\":\"FT-hdx\"}");
OnlineToolService.getNodesForTool(
"{\"Name\":\"\",\"mid\":\"SUP_DOC\",\"pbiId\":\"PBI1-16265\",\"order\":\"ASC\",\"lang\":\"NL-zh\"}",
"[pageIndex=0,pageSize=10]");
OnlineToolService.getNodesCountForTool(
"{\"Name\":\"\",\"mid\":\"SUP_DOC\",\"pbiId\":\"PBI1-16265\",\"order\":\"ASC\",\"lang\":\"NL-zh\"}");



4.       文件上传

Demo工程实现了FileUploadServlet的文件上传servlet，在常量中定义了文件保存的路径（默认值是/appfile/demo，需修改），上传文件大小的管控，允许上传的文件类型。

目前代码中仅对登录做了校验，工具可根据登录用户的权限控制是否允许用户的上传操作。目前上传servlet实现了保存文件到服务器上，但文件的信息需要各工具保存到各自的数据库中，请实现下列代码中注释掉的部分。





文件上传页面：http://localhost.huawei.com:8080/file/index

5.       根据Support角色进行鉴权

Demo工程已经集成了sso登录的SsoFilter和Support鉴权的SupportAuthenticationFilter，配置信息见OnlineToolFilterConfig.java

需要工具配置的也就是exclusions，哪些请求不走filter

由于原先是在线工具平台在后台进行配置的，一个工具需要用户有哪些角色才能够访问，切换到HIS后则需要各工具配置允许访问工具的用户角色信息，工具需要实现如下方法，返回有权限访问工具的用户角色id（困难点，工具需要配置Support中的用户角色id，没啥好方法获取）

List<String> roles = thisToolService.getRoles();

鉴权的逻辑：若工具未配置权限信息，直接返回无权限，如果用户没有登录，且工具没有配置允许匿名用户访问（即没有配置权限AnonymousRole），则跳转登录页面让用户登录，登录后调用Support接口获取登录用户的所有角色信息，是否包含工具允许访问的角色id，若包含则允许访问，否则跳转到401无权限页面（此页面工具可按自己的UI风格美化下）
