import com.huawei.it.sso.filter.util.SsoConstants;
import com.huawei.it.sso.filter.util.SsoFilterUtil;
import com.huawei.it.support.usermanage.helper.UserInfoBean;
import com.huawei.mda.service.ThisToolService;
import com.huawei.support.onlinetoolservice.service.OnlineToolService;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 根据Support系统的用户权限进行鉴权
 * 
 */
@Component
public class SupportAuthenticationFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(SupportAuthenticationFilter.class);

    private static final String FILTER_EXCLUSIONS = "exclusions";

    private static final String CARRIER_ANONYMOUS_ROLE = "ROLE1001000773";

    private static final String ENTERPRISE_ANONYMOUS_ROLE = "Erole_id_1";

    private static final String SESSION_KEY_SUPPORT_USER_ROLES = "supportUserRoles";

    private static final String SESSION_KEY_SUPPORTE_USER_ROLES = "supporteUserRoles";

    private static String[] exclusions;

    private static ThisToolService thisToolService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String exclusionsStr = filterConfig.getInitParameter(FILTER_EXCLUSIONS);
        if (StringUtils.isNotBlank(exclusionsStr)) {
            exclusions = exclusionsStr.split(",");
        }
        ServletContext servletContext = filterConfig.getServletContext();
        ApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        if (springContext != null) {
            try {
                thisToolService = springContext.getBean(ThisToolService.class);
            } catch (BeansException e) {
                LOG.error("Get " + ThisToolService.class + " from spring context failed!");
            }
        } else {
            LOG.error("No spring context.");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // 无需鉴权的请求直接走到下一个filter
        if ((req.getRequestURL() != null) && isexclusion(req.getRequestURL().toString())) {
            LOG.info("current url is excluded from filter = " + req.getRequestURL());
            chain.doFilter(request, response);
            return;
        }

        // 工具未配置权限信息，直接返回无权限
        List<String> roles = thisToolService.getRoles();
        if (CollectionUtils.isEmpty(roles)) {
            res.sendError(401);
            return;
        }

        // 获取用户登录信息
        HttpSession httpSessn = req.getSession(false);
        if ((httpSessn != null) && (httpSessn.getAttribute(SsoConstants.SESSION_USER_INFO_KEY) != null)) {
            // 从session中获取sso登录的用户信息
            UserInfoBean uiBean = (UserInfoBean) httpSessn.getAttribute(SsoConstants.SESSION_USER_INFO_KEY);

            if (!valiate(roles, uiBean.getUid(), httpSessn)) {
                res.sendError(401);
            }
        } else if (!roles.contains(CARRIER_ANONYMOUS_ROLE) && !roles.contains(ENTERPRISE_ANONYMOUS_ROLE)) {
            // 工具无匿名用户权限，跳转登录
            try {
                SsoFilterUtil.loginAndRedirect2AppCurrentURL(req, res);
            } catch (Exception e) {
                LOG.error("loginAndRedirect2AppCurrentURL exception, current url:" + req.getRequestURL());
            }
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean valiate(List<String> roles, String userId, HttpSession httpSessn) throws IOException {
        Object supportRolesObj = httpSessn.getAttribute(SESSION_KEY_SUPPORT_USER_ROLES);
        String supportRoles = StringUtils.EMPTY;
        if (supportRolesObj != null && supportRolesObj instanceof String) {
            supportRoles = (String) supportRolesObj;
        } else {
            supportRoles = OnlineToolService.getSupportUserRoles(userId);
            httpSessn.setAttribute(SESSION_KEY_SUPPORT_USER_ROLES, supportRoles);
        }

        for (String role : roles) {
            if (StringUtils.contains(supportRoles, role)) {
                return true;
            }
        }

        Object supporteRolesObj = httpSessn.getAttribute(SESSION_KEY_SUPPORTE_USER_ROLES);
        String supporteRoles = StringUtils.EMPTY;
        if (supporteRolesObj != null && supporteRolesObj instanceof String) {
            supporteRoles = (String) supporteRolesObj;
        } else {
            supporteRoles = OnlineToolService.getSupportEUserRoles(userId);
            httpSessn.setAttribute(SESSION_KEY_SUPPORTE_USER_ROLES, supporteRoles);
        }

        for (String role : roles) {
            if (StringUtils.contains(supporteRoles, role)) {
                return true;
            }
        }

        return false;
    }

    private boolean isexclusion(String currentURL) {
        if ((exclusions == null) || (exclusions.length < 1) || StringUtils.isBlank(currentURL)) {
            return false;
        }

        for (String exclusion : exclusions) {
            if (currentURL.matches(exclusion.replaceAll("\\*", "\\.\\*"))) {
                return true;
            }
        }
        return false;
    }
}
