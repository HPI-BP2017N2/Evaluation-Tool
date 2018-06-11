package de.hpi.evaluationtool.persistence.repository;

import de.hpi.evaluationtool.persistence.SamplePage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ISamplePageRepository extends MongoRepository<SamplePage, String> {

    Optional<SamplePage> findById(String id);
}
