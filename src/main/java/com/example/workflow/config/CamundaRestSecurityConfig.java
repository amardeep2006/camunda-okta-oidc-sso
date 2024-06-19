package com.example.workflow.config;

import jakarta.inject.Inject;
import jakarta.servlet.DispatcherType;
import org.camunda.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter;
import org.camunda.bpm.engine.rest.security.auth.impl.HttpBasicAuthenticationProvider;
import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamundaRestSecurityConfig {

    private final JerseyApplicationPath applicationPath;

    @Inject
    public CamundaRestSecurityConfig(JerseyApplicationPath applicationPath) {
        this.applicationPath = applicationPath;
    }

    @Bean
    public FilterRegistrationBean<ProcessEngineAuthenticationFilter> processEngineAuthenticationFilter() {
        String restApiPath = applicationPath.getUrlMapping();

        FilterRegistrationBean<ProcessEngineAuthenticationFilter> filterRegistration = new FilterRegistrationBean<>();
        filterRegistration.setName("camunda-auth");
        filterRegistration.setFilter(new ProcessEngineAuthenticationFilter());
        filterRegistration.addInitParameter(
                "authentication-provider", HttpBasicAuthenticationProvider.class.getName());
        filterRegistration.addUrlPatterns(restApiPath);
        filterRegistration.setDispatcherTypes(DispatcherType.REQUEST);

        return filterRegistration;
    }
}
