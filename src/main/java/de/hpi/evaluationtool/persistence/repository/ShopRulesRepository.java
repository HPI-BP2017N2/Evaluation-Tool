package de.hpi.evaluationtool.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import de.hpi.evaluationtool.persistence.ShopRules;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Getter(AccessLevel.PRIVATE)
@Repository
@RequiredArgsConstructor
public class ShopRulesRepository {

    private final MongoTemplate mongoTemplate;

    private final ObjectMapper objectMapper;

    public List<ShopRules> getAllRulesOfCollection(String collectionName){
        List<ShopRules> rules = new LinkedList<>();
        MongoCollection<Document> collection = getMongoTemplate().getCollection(collectionName);
        for (Document document : collection.find()) {
            try {
                rules.add(getObjectMapper().readValue(JSON.serialize(document), ShopRules.class));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return rules;
    }

    public Set<String> getCollectionNames(){
        return getMongoTemplate().getCollectionNames();
    }
}
