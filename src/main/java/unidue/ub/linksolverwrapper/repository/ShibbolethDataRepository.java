package unidue.ub.linksolverwrapper.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import unidue.ub.linksolverwrapper.model.ShibbolethData;

@RepositoryRestResource(collectionResourceRel = "shibbolethData", path = "shibbolethData")
public interface ShibbolethDataRepository extends CrudRepository<ShibbolethData, String> {
}
