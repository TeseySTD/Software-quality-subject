package chnu.edu.inno.service;


/*
  @author   george
  @project   inno
  @class  ItemService
  @version  1.0.0 
  @since 27/09/2026 - 19.05
*/

import chnu.edu.inno.model.Item;
import chnu.edu.inno.model.dto.response.ApiResponse;
import chnu.edu.inno.model.dto.response.BaseMetaData;
import chnu.edu.inno.model.dto.response.PaginationMetaData;
import chnu.edu.inno.repository.ItemRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.javers.spring.annotation.JaversAuditable;
import org.javers.spring.annotation.JaversAuditableDelete;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@AllArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    @PostConstruct
    public void init() {
        itemRepository.deleteAll();
     List<Item> items = List.of(
             new Item("Milk", "000001", 1),
             new Item("Bread", "00002", 2),
             new Item("Sugar", "00003", 3),
             new Item("Salt", "00004", 1),
             new Item("Meat", "00005", 1),
             new Item("Chips", "00006", 3),
             new Item("Coca-Cola", "00007", 1),
             new Item("Hot dog", "00008", 1)
     );
     itemRepository.saveAll(items);
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public Item getItemById(String id) {
        return itemRepository.findById(id).orElse(null);
    }

    @JaversAuditable
    public Item createItem(Item item) {
        return itemRepository.save(item);
    }

    @JaversAuditableDelete
    public void deleteItem(Item item) {
        itemRepository.delete(item);
    }

    @JaversAuditable
    public Item updateItem(Item item) {
        Item old = itemRepository.findById(item.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        item.setCreatedDate(old.getCreatedDate());
        item.setCreatedBy(old.getCreatedBy());
        return itemRepository.save(item);
    }

    public ApiResponse<BaseMetaData, Item> getItemByIdAsApiResponse(String id) {
        Item item = itemRepository.findById(id).orElse(null);
        if (item == null) {
            return new ApiResponse<>(new BaseMetaData(404, false,"Not found"), null);
        }
        return new ApiResponse<>(new BaseMetaData(200, true), item);
    }

    public ApiResponse<PaginationMetaData, Item> getPage(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<Item> items = itemRepository.findAll(pageable);

        PaginationMetaData metaData = new PaginationMetaData();

        metaData.setNumber(items.getNumber());
        metaData.setSize(items.getSize());
        metaData.setTotalElements(items.getTotalElements());
        metaData.setTotalPages(items.getTotalPages());
        metaData.setFirst(items.isFirst());
        metaData.setLast(items.isLast());
        List<Item> list = items.getContent();
        ApiResponse<PaginationMetaData, Item> response = new ApiResponse<>(metaData,list);
        return response;
    }
}
