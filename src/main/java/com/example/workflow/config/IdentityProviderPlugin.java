package com.example.workflow.config;

import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.identity.WritableIdentityProvider;
import org.camunda.bpm.engine.impl.identity.db.DbGroupQueryImpl;
import org.camunda.bpm.engine.impl.identity.db.DbIdentityServiceProvider;
import org.camunda.bpm.engine.impl.identity.db.DbUserQueryImpl;
import org.camunda.bpm.engine.impl.interceptor.Session;
import org.camunda.bpm.engine.impl.interceptor.SessionFactory;
import org.camunda.bpm.engine.impl.persistence.entity.UserEntity;
import org.camunda.bpm.engine.spring.SpringProcessEnginePlugin;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class IdentityProviderPlugin extends SpringProcessEnginePlugin {
    /*
     * IdentityProviderPlugin is required since Camunda 7.19. Camunda implemented a security feature to cache the auth info
     * for finite time only. Refer below links for more info
     * https://docs.camunda.org/security/notices/#notice-85
     * https://docs.camunda.org/manual/7.19/user-guide/security/#authentication-cache
     * https://github.com/camunda-consulting/camunda-7-code-examples/tree/main/snippets/springboot-security-sso
     * https://github.com/camunda/camunda-bpm-platform/issues/3475
     * https://github.com/camunda/camunda-bpm-platform/issues/3689
     * In this implementation, I am querying groups and users against DB first and then SecurityContextHolder
     * */

    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        processEngineConfiguration.setIdentityProviderSessionFactory(new SessionFactory() {
            @Override
            public Class<?> getSessionType() {
                return WritableIdentityProvider.class;
            }

            @Override
            public Session openSession() {
                return new DbIdentityServiceProvider() {

                    @Override
                    public List<Group> findGroupByQueryCriteria(DbGroupQueryImpl query) {
                        List<Group> groups = super.findGroupByQueryCriteria(query);
                        if (!groups.isEmpty()) {
                            return groups;

                        } else {
                            /*
                            * Fetch user details and Groups from SpringSecurityContext
                            * */
                            String userId = query.getUserId();
                            if (userId != null) {
                                UserDetails userDetails = null;

                                Authentication authentication;
                                try {
                                    authentication = SecurityContextHolder.getContext()
                                            .getAuthentication();
                                    if (authentication != null) {
                                        String name = ((OidcUser) authentication.getPrincipal()).getName();
                                        String emailID = ((OidcUser) authentication.getPrincipal()).getEmail();
                                    }
                                } catch (UsernameNotFoundException e) {
                                    return Collections.emptyList();
                                }
                                List<String> groupIds = null;
                                if (authentication != null) {
                                    groupIds = authentication.getAuthorities()
                                            .stream()
                                            .map(GrantedAuthority::getAuthority)
//                                        .map(res -> res.substring(5)) // Strip "ROLE_"
                                            .collect(Collectors.toList());
                                }
                                if (!groupIds.isEmpty()) {
                                    return groupIds.stream()
                                            .map(groupId -> {
                                                Group group = createNewGroup(groupId);
                                                group.setName(groupId);
                                                return group;
                                            })
                                            .collect(Collectors.toList());

                                } else {
                                    return Collections.emptyList();

                                }
                            } else {
                                return Collections.emptyList();

                            }
                        }
                    }

                    @Override
                    public List<User> findUserByQueryCriteria(DbUserQueryImpl query) {
                        List<User> users = super.findUserByQueryCriteria(query);
                        if (!users.isEmpty()) {
                            return users;

                        } else {
                            String userId = query.getId();
                            if (userId != null) {
                                UserDetails userDetails = null;
                                Authentication authentication = null;
                                String name = null;
                                String emailID = null;
                                String firstName= null;
                                String lastName= null;
                                try {
//                                    userDetails = userDetailsService.loadUserByUsername(userId);
                                    authentication = SecurityContextHolder.getContext()
                                            .getAuthentication();
                                    if (authentication != null) {
                                        name = ((OidcUser) authentication.getPrincipal()).getName();
                                        emailID = ((OidcUser) authentication.getPrincipal()).getEmail();
                                        firstName = ((OidcUser) authentication.getPrincipal()).getFullName();
                                        lastName = ((OidcUser) authentication.getPrincipal()).getFamilyName();
                                    }
                                } catch (UsernameNotFoundException e) {
                                    return Collections.emptyList();
                                }

                                if (emailID != null) {
                                    UserEntity userEntity = new UserEntity();
                                    userEntity.setId(emailID);
                                    userEntity.setFirstName(firstName);
                                    userEntity.setLastName(lastName);

                                    return Collections.singletonList(userEntity);

                                } else {
                                    return Collections.emptyList();

                                }

                            } else {
                                return Collections.emptyList();

                            }
                        }
                    }
                };
            }
        });
    }

}