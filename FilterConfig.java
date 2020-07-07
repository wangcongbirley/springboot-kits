import com.huawei.it.sso.filter.SsoFilter;
import com.huawei.mda.filter.SupportAuthenticationFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 指定filter的配置信息
 * 
 * @author wangcongbirley
 * @since 2020-07-07
 */
@Configuration
public class FilterConfig {
    @Autowired
    private SupportAuthenticationFilter supportAuthenticationFilter;

    @Bean
    public FilterRegistrationBean<SsoFilter> setSsoFilter() {
        FilterRegistrationBean<SsoFilter> filterRegistrationBean = new FilterRegistrationBean<SsoFilter>();
        filterRegistrationBean.setFilter(new SsoFilter());
        filterRegistrationBean.setName("SsoFilter");
        filterRegistrationBean.addUrlPatterns("/*");
        Map<String, String> initParameters = new HashMap<String, String>();
        initParameters.put("userscope", "INTERNET_USER");
        initParameters.put("debug", "false");
        initParameters.put("serverscope", "inter");
        initParameters.put("exclusions", "*\\.htm,*\\.html,*\\.jpg,*\\.png,*\\.gif,*\\.js,*\\.css");
        filterRegistrationBean.setInitParameters(initParameters);
        filterRegistrationBean.setOrder(1); // order的数值越小，在所有的filter中优先级越高
        return filterRegistrationBean;
    }

    @Bean
    public FilterRegistrationBean<SupportAuthenticationFilter> setSupportAuthenticationFilter() {
        FilterRegistrationBean<SupportAuthenticationFilter> filterRegistrationBean =
            new FilterRegistrationBean<SupportAuthenticationFilter>();
        filterRegistrationBean.setFilter(supportAuthenticationFilter);
        filterRegistrationBean.setName("supportAuthenticationFilter");
        filterRegistrationBean.addUrlPatterns("/*");
        Map<String, String> initParameters = new HashMap<String, String>();
        initParameters.put("exclusions", "*\\.htm,*\\.html,*\\.jpg,*\\.png,*\\.gif,*\\.js,*\\.css");
        filterRegistrationBean.setInitParameters(initParameters);
        filterRegistrationBean.setOrder(2); // order的数值越小，在所有的filter中优先级越高
        return filterRegistrationBean;
    }
}
