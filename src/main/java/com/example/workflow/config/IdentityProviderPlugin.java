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
import org.camunda.bpm.engine.impl.persistence.entity.GroupEntity;
import org.camunda.bpm.engine.impl.persistence.entity.UserEntity;
import org.camunda.bpm.engine.spring.SpringProcessEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class IdentityProviderPlugin extends SpringProcessEnginePlugin {

    private final Logger logger = LoggerFactory.getLogger(IdentityProviderPlugin.class);
    @Value("${oauth2.groupNameAttribute:groups}")
    private String groupNameAttribute;

    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        processEngineConfiguration.setIdentityProviderSessionFactory(new SessionFactory() {
            @Override
            public Class<?> getSessionType() {
                return WritableIdentityProvider.class;
            }

            @Override
            public Session openSession() {
                return new DbIdentityServiceProvider() {
                    //Here I am fetching Groups from Camunda Database First and then in Spring security context. You can change the logic based on your need.
                    @Override
                    public List<Group> findGroupByQueryCriteria(DbGroupQueryImpl query) {
                        List<Group> groups = super.findGroupByQueryCriteria(query);
                        if (!groups.isEmpty()) {
                            return groups;
                        }
                        return Collections.emptyList();
                    }
//                    Searching User in Database first and then in Spring security context. You can tweak the sequence of lookup based on need .
//                    or you can keep the lookup you need.
                    @Override
                    public List<User> findUserByQueryCriteria(DbUserQueryImpl query) {
                        List<User> users = super.findUserByQueryCriteria(query);
                        if (!users.isEmpty()) {
                            return users;
                        }

                        String userId = query.getId();
                        if (userId != null) {
                            Authentication authentication = SecurityContextHolder.getContext()
                                    .getAuthentication();
                            if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
                                UserEntity userEntity = new UserEntity();
                                userEntity.setId(oidcUser.getEmail());
                                userEntity.setFirstName(oidcUser.getGivenName());
                                userEntity.setLastName(oidcUser.getFamilyName());
                                userEntity.setEmail(oidcUser.getEmail());
                                return Collections.singletonList(userEntity);
                            }
                        }

                        return Collections.emptyList();
                    }

//                    private List<String> extractGroupsFromUserAttribute(Authentication authentication) {
//                        return ((OidcUser) authentication.getPrincipal()).getAttribute(groupNameAttribute);
//                    }
//
//                    private List<Group> convertToGroupList(List<String> groupIds) {
//                        return groupIds.stream()
//                                .map(groupId -> {
//                                    Group group = new GroupEntity();
//                                    group.setId(groupId);
//                                    group.setName(groupId);
//                                    return group;
//                                })
//                                .collect(Collectors.toList());
//                    }

                };
            }
        });
    }

}