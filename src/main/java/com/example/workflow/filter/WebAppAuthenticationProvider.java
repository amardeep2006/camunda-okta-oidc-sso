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
        AuthenticationResult authenticationResult = new AuthenticationResult(emailID, true);
//        Here I am fetching OKTA Groups from Spring Security. If you want to just use DB Groups You can delete the code for setting groups
        authenticationResult.setGroups(getUserGroups(authentication));

        return authenticationResult;
    }

    /*
     * Extract groups from User Authorities in Principal Object
     */
    private List<String> getUserGroups(Authentication authentication) {


        logger.info("++ WebAppAuthenticationProvider.getUserGroups()....");
        List<String> groupIds;
        groupIds = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        logger.debug("++ groupIds = " + groupIds);
        return groupIds;
    }
}