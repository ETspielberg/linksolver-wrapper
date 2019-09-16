package org.unidue.ub.libintel.linksolverwrapper.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.unidue.ub.libintel.linksolverwrapper.model.ShibbolethData;

/**
 * simple repository to manage the creation and retrieval of shibboleth information
 */
@RepositoryRestResource(collectionResourceRel = "shibbolethData", path = "shibbolethData")
public interface ShibbolethDataRepository extends CrudRepository<ShibbolethData, String> {
}
