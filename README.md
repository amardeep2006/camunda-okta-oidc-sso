# Camunda Okta OIDC SSO 

# Based on Camunda 7.21 and Springboot 3.3 

The example to implement okta sso in Camunda for Oauth2.0 Authorization Code flow.
I have implemented Single logout as well in this.

Note : It only implements authentication and Login via Okta. It still makes use of DB 
based authorizations.

Prerequisites
* Java 17
* Okta Developer Account
* Git

1. If you do not have free developer account on Okta then you can create one.
[Okta Developer Account](https://developer.okta.com/)
Create one User under Directory menu. 
2. Login to Okta and register your app by clicking on Applications : Create App Integration
![img.png](img/img.png)
3Select OIDC and Application type as Web
![img_2.png](img/img_2.png)
4. Provide App Details

   App integration name (Whatever you like) : camunda-sso-oidc-demo
   
   Sign-in redirect URIs : http://localhost:8080/login/oauth2/code/okta

   You can add multiple for different environments.

   Controlled Access : Everyone (You may change later)   

   Leave Every thing else default and Submit.

   Your app will look like this , Note down the Client ID and Client Secret

![img_4.png](img/img_4.png)

5. Modify the application.yml and update following

First time boot user details. It must exist on your okta tenant.
```yaml 
admin-user:
  id : amar.deep.singh@xyz.com
  firstName: Amar Deep
  lastName: Singh
  email: amar.deep.singh@xyz.com
```
Change the Okta Tenant details

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          okta:
            client-id: change it
            client-secret: change it
            scope: openid,profile,email
        provider:
          okta:
            authorization-uri: https://CHANGEIT.okta.com/oauth2/default/v1/authorize
            token-uri: https://CHANGEIT.okta.com/oauth2/default/v1/token
            user-info-uri: https://CHANGEIT.okta.com/oauth2/default/v1/userinfo
            jwk-set-uri: https://CHANGEIT.okta.com/oauth2/default/v1/keys
```

9. Run the Project with following commands

`./mvnw clean package`

`./mvnw spring-boot:run`

Open http://localhost:8080 in your favorite browser.

Now you will be taken to okta for login.

If you face some trouble, raise and issue on this repo.

10. To disable sso , delete all the okta configurations and configs listed below. 

```yaml
oauth2:
  enabled: true
#  groupNameAttribute matters a lot, it should be same as scope name and claim name. Defaults to groups
  groupNameAttribute: groups
  singlelogout:
    enabled: false
```

# Future plans 

I am working on securing REST API via client credentials flow and JWT based on okta . Will update soon.
