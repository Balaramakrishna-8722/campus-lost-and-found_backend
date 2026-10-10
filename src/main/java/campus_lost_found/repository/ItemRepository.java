package campus_lost_found.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import campus_lost_found.model.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByItemType(String itemType);

    List<Item> findByItemNameContainingIgnoreCaseOrLocationContainingIgnoreCase(
            String itemName,
            String location);

    List<Item> findByItemTypeAndItemNameContainingIgnoreCaseOrItemTypeAndLocationContainingIgnoreCase(
            String itemType1,
            String itemName,
            String itemType2,
            String location);
}