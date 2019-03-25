package unidue.ub.linksolverwrapper.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import unidue.ub.linksolverwrapper.model.ShibbolethData;

@Repository
public interface ShibbolethDataRepository extends CrudRepository<ShibbolethData, String> {
}
