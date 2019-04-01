# Linksolver Wrapper

The linksolver-wrapper microservice offers tools to optimize the linksolver behavior and includes a Shibboleth WAYFless-URL generator.
As a [Spring Boot](https://spring.io/projects/spring-boot) microservice, it can easily be integrated into the [Lib:Intel](https://github.com/ETspielberg/lib-intel) platform.


## WAYFless-URL generator

When using shibboleth for authentication, users usually have to navigate through an extended navigational structure to answer the "Where Are You From?" (WAYF) question and to determine the appropriate identity provider.
In order to facilitate the access, the information about the identity provider can be provided in a special url, the so called WAYFless URL (find some useful description and advice [here](https://www.ukfederation.org.uk/library/uploads/Documents/WAYFlessGuidance.pdf)).

The linksolver-wrapper includes a WAYFless-URL generator, which generates both SP-side and IdP-side WAYFless URLs with the help of data from a central Shibboleth data repository.

### Shibboleth data repository
  
This repository holds platform specific information such as the base host address, the name of the entity-ID parameter and the name of the target-parameter.
Data are stored in a database ([PostgreSQL](https://www.postgresql.org/) by default) and are delivered through the usual [Spring Data](https://docs.spring.io/spring-data/rest/docs/current/reference/html/) endpoints under "/shibbolethData". 

As these are openly available data, GET access to this endpoints is always allowed, whereas authentication is needed to modify the settings via POST or PUT requests.    


### Linksolver-wrapper
The Linksolver-Wrapper offers two endpoints: `/resolve` accepts OpenURL parameters and then tries to receive the resource URLs from the OVID linksolver and/or Crossref.
The obtained resource link is then checked for corresponding Shibboleth accounts and a WAYFless URL is constructed, if the request originates from a non-excluded IP range.

The other one (`/useShibboleth?target=<target-URL>`) accepts an url as target parameter and transforms it into a WAYFless URL, if a corresponding Shibboleth profile is defined.

The wrapper is included into the [Lib:Intel](https://github.com/ETspielberg/lib-intel) platform by registering at the local Eureka-Server and being accessible through the Zuul-Gateway. 

## Configuration
The service is usually configured via a seccured Spring Boot config server, providing data for the linksolver

    libintel.linksolver.url=<Linksolver URL>


for general Shibboleth data:

    libintel.shibboleth.idp.url=<the address of your local Shbboleth endpoint>
    libintel.shibboleth.entity.id=<the Shibboleth entity id of your institution>

for the data, containing the Shibboleth data for publisher platforms

    spring.datasource.url=<database url, e.g. jdbc:postgresql://localhost:11111/myDatabase>
    spring.datasource.data-username=<database username>
    spring.datasource.password=<database password>

Providing an appropriate keystore, these data can also be stored encrypted.

## Local execution
Configuration can also be provided locally by editing the application.properties file. 
However, in this case, the service discovery features should be switched off by removing the eureka dependencies in the POM file. 
The server port can be provided by the environemnt variable SERVER_PORT.

## Installation, building, and running

As a Spring Boot application, the program requires Java 11 and Maven to be installed on your system.
In addition the database described in the configuration must be accessible to the program. 
The source code can be cloned using
 
    git clone https://github.com/ETspielberg/linksolver-wrapper.git
    
After completion, the application can be build using

    mvn clean install
    
The executable jar-file can be found under `target/linksolver-wrapper.jar`.

