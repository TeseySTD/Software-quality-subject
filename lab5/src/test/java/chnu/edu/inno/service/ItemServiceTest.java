package chnu.edu.inno.service;

/**
 * @author Rout
 * @version 1.0.0
 * @project lab5
 * @class ItemServiceTest
 * @since 13.11.2026 - 12.00
 */

import chnu.edu.inno.model.Item;
import chnu.edu.inno.repository.ItemRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemServiceTest {

    private final ItemRepository repo = mock(ItemRepository.class);
    private final ItemService service = new ItemService(repo);

    @Test
    void getByIdReturns404WhenMissing() {
        when(repo.findById("x")).thenReturn(Optional.empty());
        assertEquals(404, service.getItemById("x").getMeta().getCode());
    }

    @Test
    void createReturns201AndIgnoresClientId() {
        when(repo.save(any(Item.class))).thenAnswer(i -> i.getArgument(0));
        var response = service.createItem(new Item("1", "Milk", "001", 2));
        assertEquals(201, response.getMeta().getCode());
        assertNull(response.getData().get(0).getId());
    }

    @Test
    void createRejectsBlankNameAndNegativeQuantity() {
        assertEquals(400, service.createItem(new Item(" ", "1", 1)).getMeta().getCode());
        assertEquals(400, service.createItem(new Item("A", "1", -1)).getMeta().getCode());
        verify(repo, never()).save(any());
    }

    @Test
    void updateReturns400WithoutIdAnd404WhenMissing() {
        assertEquals(400, service.updateItem(new Item("A", "1", 1)).getMeta().getCode());
        when(repo.existsById("9")).thenReturn(false);
        assertEquals(404, service.updateItem(new Item("9", "A", "1", 1)).getMeta().getCode());
    }

    @Test
    void deleteReturns200WhenExists() {
        when(repo.existsById("1")).thenReturn(true);
        assertEquals(200, service.deleteItemById("1").getMeta().getCode());
        verify(repo).deleteById("1");
    }

    @Test
    void pageRejectsInvalidParams() {
        var response = service.getPage(-1, 5);
        assertEquals(400, response.getMeta().getCode());
        assertFalse(response.getMeta().isSuccess());
    }
}
