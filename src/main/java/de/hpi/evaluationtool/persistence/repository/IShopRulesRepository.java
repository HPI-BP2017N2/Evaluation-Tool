package de.hpi.evaluationtool.persistence.repository;

import de.hpi.evaluationtool.persistence.ShopRules;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IShopRulesRepository extends MongoRepository<ShopRules, Long> {


}
