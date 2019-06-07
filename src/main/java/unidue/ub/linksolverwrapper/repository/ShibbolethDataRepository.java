package unidue.ub.linksolverwrapper.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import unidue.ub.linksolverwrapper.model.ShibbolethData;

/**
 * simple repository to manage the creation and retrieval of shibboleth information
 */
@RepositoryRestResource(collectionResourceRel = "shibbolethData", path = "shibbolethData")
public interface ShibbolethDataRepository extends CrudRepository<ShibbolethData, String> {
}
