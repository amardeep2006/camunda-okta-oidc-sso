package com.example.workflow.config;

import com.example.workflow.filter.WebAppAuthenticationProvider;
import jakarta.inject.Inject;
import jakarta.servlet.DispatcherType;
import org.camunda.bpm.spring.boot.starter.property.CamundaBpmProperties;
import org.camunda.bpm.spring.boot.starter.property.WebappProperty;
import org.camunda.bpm.webapp.impl.security.auth.ContainerBasedAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@ConditionalOnProperty(prefix = "oauth2", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableWebSecurity
@Configuration
public class WebAppSecurityConfig {
    private final String webappPath;
    public WebAppSecurityConfig(CamundaBpmProperties properties) {

        WebappProperty webapp = properties.getWebapp();
        this.webappPath = webapp.getApplicationPath();
        logger.debug("webappPath = {} ", webappPath);
    }

    @Autowired
    ClientRegistrationRepository clientRegistrationRepository;
    OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
        return new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
    }

    @Value("${oauth2.singlelogout.enabled:false}")
    private boolean singleLogout;
    private final Logger logger = LoggerFactory.getLogger(WebAppSecurityConfig.class.getName());
    @Inject
    private CamundaBpmProperties camundaBpmProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.ignoringRequestMatchers(antMatcher(webappPath + "/api/**")))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(antMatcher("/swaggerui/**"))
                        .permitAll()
                        .requestMatchers(antMatcher(webappPath + "/**"))
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

    @ConditionalOnProperty(prefix = "oauth2", name = "enabled", havingValue = "true", matchIfMissing = false)
    @Bean
    public FilterRegistrationBean containerBasedAuthenticationFilter() {

        FilterRegistrationBean filterRegistration = new FilterRegistrationBean();
        filterRegistration.setFilter(new ContainerBasedAuthenticationFilter());
        filterRegistration.setInitParameters(Collections.singletonMap(
                "authentication-provider", WebAppAuthenticationProvider.class.getName()));
        // make sure the filter is registered after the Spring Security Filter Chain
        filterRegistration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER + 1);
        filterRegistration.addUrlPatterns(webappPath + "/app/*");
        filterRegistration.setDispatcherTypes(DispatcherType.REQUEST);
        return filterRegistration;
    }

}