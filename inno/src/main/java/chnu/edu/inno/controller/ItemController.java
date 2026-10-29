package chnu.edu.inno.controller;


/*
  @author   george
  @project   inno
  @class  ItemController
  @version  1.0.0 
  @since 27/09/2026 - 19.09
*/

import chnu.edu.inno.model.Item;
import chnu.edu.inno.model.dto.request.ItemCreateRequest;
import chnu.edu.inno.model.dto.request.ItemUpdateRequest;
import chnu.edu.inno.model.dto.response.ApiResponse;
import chnu.edu.inno.model.dto.response.BaseMetaData;
import chnu.edu.inno.model.dto.response.PaginationMetaData;
import chnu.edu.inno.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("api/v1/items")
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/{id}")
    public Item getItemById(@PathVariable String id) {
        return itemService.getItemById(id);
    }

    @GetMapping()
    public List<Item> getAllItems() {
        return itemService.getAllItems();
    }

    @PostMapping
    public Item createItem(@RequestBody ItemCreateRequest request) {
        return itemService.createItem(request.toItem());
    }

    @PutMapping
    public Item updateItem(@RequestBody ItemUpdateRequest request) {
        return itemService.updateItem(request.toItem());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        Item item = itemService.getItemById(id);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found");
        }
        itemService.deleteItem(item);
        return ResponseEntity.noContent().build();
    }

    //----------------  More ---------------------------

    @GetMapping("/response/{id}")
    public ApiResponse<BaseMetaData, Item> getItemByIdAsApiResponse(@PathVariable String id) {
        return itemService.getItemByIdAsApiResponse(id);
    }

    @GetMapping("/page")
    @Operation(
            method = "GET",
            summary = "Get  ALL items, ASC, PAGEABLE. Default 5 items per page",
            description = "Fetch a PAGE of items  by page number and size "
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Success. Return page of items",
                    content = {
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            type = "object",
                                            properties = {
                                                    @StringToClassMapItem(key = "meta", value = PaginationMetaData.class),
                                                    @StringToClassMapItem(key = "data", value = Item.class)
                                            }
                                    )
                            )
                    }
            )
    })
    public ApiResponse<PaginationMetaData, Item> getItemsPage(
            @Parameter(description = "Request parameter. Page of paginated result. Cannot be less than zero")
            @RequestParam(name = "page", defaultValue = "0", required = false) Integer page,
            @Parameter(description = "Request parameter. Size of paginated page result. Cannot be less than zero")
            @RequestParam(name = "size", defaultValue = "5", required = false) Integer size
    ) {
        return itemService.getPage(page, size);
    }





}
