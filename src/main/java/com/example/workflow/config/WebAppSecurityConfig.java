package com.example.workflow.config;

import com.example.workflow.filter.WebAppAuthenticationProvider;
import org.camunda.bpm.webapp.impl.security.auth.ContainerBasedAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;


@EnableWebSecurity
public class WebAppSecurityConfig {

    @Autowired
    ClientRegistrationRepository clientRegistrationRepository;

    OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler successHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        return successHandler;
    }

    @Value("${sso.enable.singlelogout}")
    private boolean singleLogout;
    private final Logger logger = LoggerFactory.getLogger(WebAppSecurityConfig.class.getName());

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf().ignoringAntMatchers("/engine-rest/**", "/camunda/api/**")
                .and()
                .authorizeRequests()
                .antMatchers("/logout/callback/**","/swaggerui/**", "/engine-rest/**", "/error/**")
                .permitAll()
                .and()
                .antMatcher("/**")
                .authorizeRequests()
                .antMatchers("/camunda/**")
                .authenticated()
                .anyRequest()
                .permitAll()
                .and()
                .oauth2Login();

        if (singleLogout) {
            http
                    .logout().logoutSuccessHandler(oidcLogoutSuccessHandler());
        }

        return http.build();
    }


    @Bean
    @Order(SecurityProperties.BASIC_AUTH_ORDER - 15)
    public FilterRegistrationBean containerBasedAuthenticationFilter() {

        logger.info("++++++++ WebAppSecurityConfig.containerBasedAuthenticationFilter()....");
        FilterRegistrationBean filterRegistration
                = new FilterRegistrationBean();
        filterRegistration.setFilter(new ContainerBasedAuthenticationFilter());
        filterRegistration.setInitParameters(Collections.singletonMap("authentication-provider", WebAppAuthenticationProvider.class.getName()));
        filterRegistration.setOrder(101); // make sure the filter is registered after the Spring Security Filter Chain
        filterRegistration.addUrlPatterns("/camunda/app/*");
        return filterRegistration;

    }


}
