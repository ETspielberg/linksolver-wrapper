# Linksolver Wrapper

The Linksolver-Wrapper takes the OpenURL parameters and tries to receive the resource URLs from the OVID linksolver or Crossref.

The obtained resource link is then checked for corresponding Shibboleth accounts and a WAYFless URL is constructed.

The wrapper is included into the libintel-plattform by addressing the local Eureka-Server and being accessible through the Gateway.

Configuration lies within a secured Spring Boot config server, providing data for the linksolver

```
libintel.linksolver.url=<Linksolver URL>
```

for general Shibboleth data:
```
libintel.shibboleth.idp.url=<the address of your local Shbboleth endpoint>
libintel.shibboleth.entity.id=<the Shibboleth entity id of your institution>
```

for the data, containing the Shibboleth data for publisher platforms
```
spring.datasource.url=<database url, e.g. jdbc:postgresql://localhost:11111/myDatabase>
spring.datasource.data-username=<database username
spring.datasource.password=<database password>
```
