package com.example.workflow.filter;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.rest.security.auth.AuthenticationResult;
import org.camunda.bpm.engine.rest.security.auth.impl.ContainerBasedAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.stream.Collectors;

public class WebAppAuthenticationProvider extends ContainerBasedAuthenticationProvider {

    private final Logger logger = LoggerFactory.getLogger(WebAppAuthenticationProvider.class);

    @Override
    public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request, ProcessEngine engine) {
        logger.info("++ WebAppAuthenticationProvider.extractAuthenticatedUser()....");

        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication == null) {
            logger.debug("++ authentication == null...return unsuccessful.");
            return AuthenticationResult.unsuccessful();
        }

        logger.debug("++ authentication IS NOT NULL");

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String name = oidcUser.getName();

        if (name == null || name.isEmpty()) {
            return AuthenticationResult.unsuccessful();
        }

        String emailID = oidcUser.getEmail();
        return new AuthenticationResult(emailID, true);
    }
}