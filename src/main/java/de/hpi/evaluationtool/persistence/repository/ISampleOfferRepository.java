package de.hpi.evaluationtool.persistence.repository;

import de.hpi.evaluationtool.persistence.SampleOffer;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ISampleOfferRepository extends MongoRepository<SampleOffer, Long> {

    Optional<SampleOffer> findByShopID(long shopID);
}
