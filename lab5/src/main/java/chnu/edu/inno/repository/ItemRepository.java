package chnu.edu.inno.repository;

/**
 * @author Rout
 * @version 1.0.0
 * @project lab5
 * @class src\main\java\chnu\edu\inno\repository\ItemRepository
 * @since 13.11.2026 - 23.55
 */

import chnu.edu.inno.model.Item;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends MongoRepository<Item, String> {
}
