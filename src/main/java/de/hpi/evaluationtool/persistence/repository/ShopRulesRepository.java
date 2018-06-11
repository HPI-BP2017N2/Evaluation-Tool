package de.hpi.evaluationtool.persistence.repository;

import de.hpi.evaluationtool.persistence.ShopRules;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Getter(AccessLevel.PRIVATE)
@Repository
@RequiredArgsConstructor
public class ShopRulesRepository {

    private final MongoTemplate mongoTemplate;

    public List<ShopRules> getAllRulesOfCollection(String collectionName){
        return getMongoTemplate().findAll(ShopRules.class, collectionName);
    }

    public Set<String> getCollectionNames(){
        return getMongoTemplate().getCollectionNames();
    }
}
