package com.example.workflow.config;

import com.example.workflow.filter.WebAppAuthenticationProvider;
import jakarta.inject.Inject;
import org.camunda.bpm.spring.boot.starter.property.CamundaBpmProperties;
import org.camunda.bpm.webapp.impl.security.auth.ContainerBasedAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;


@EnableWebSecurity
@Configuration
public class WebAppSecurityConfig {

    @Autowired
    ClientRegistrationRepository clientRegistrationRepository;
    OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler successHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        return successHandler;
    }

    @Value("${sso.enable.singlelogout:false}")
    private boolean singleLogout;
    private final Logger logger = LoggerFactory.getLogger(WebAppSecurityConfig.class.getName());
    @Inject
    private CamundaBpmProperties camundaBpmProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String webAppPath = camundaBpmProperties.getWebapp()
                .getApplicationPath(); //default value for root-context is /camunda
        http.csrf(csrf -> csrf.ignoringRequestMatchers(antMatcher(webAppPath + "/api/**")))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(antMatcher("/swaggerui/**"))
                        .permitAll()
                        .requestMatchers(antMatcher(webAppPath + "/**"))
                        .authenticated()
                        .anyRequest()
                        .permitAll())
                .oauth2Login(Customizer.withDefaults());
        if (singleLogout) {
            http
                    .logout((logout) -> logout
                            .logoutSuccessHandler(oidcLogoutSuccessHandler())
                    );
        }
        return http.build();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Bean
    public FilterRegistrationBean containerBasedAuthenticationFilter() {

        FilterRegistrationBean filterRegistration = new FilterRegistrationBean();
        filterRegistration.setFilter(new ContainerBasedAuthenticationFilter());
        filterRegistration.setInitParameters(Collections.singletonMap("authentication-provider", WebAppAuthenticationProvider.class.getName()));
        filterRegistration.setOrder(101); // make sure the filter is registered after the Spring Security Filter Chain
        filterRegistration.addUrlPatterns("/camunda/app/*");
        return filterRegistration;
    }

}