package chnu.edu.inno.model.dto.request;

/**
 * @author Rout
 * @version 1.0.0
 * @project inno
 * @class ItemCreateRequest
 * @since 29.10.2026 - 23.09
 */

import chnu.edu.inno.model.Item;

public record ItemCreateRequest(String name, String code, int quantity, String description) {

    public Item toItem() {
        Item item = new Item();
        item.setName(name);
        item.setCode(code);
        item.setQuantity(quantity);
        item.setDescription(description);
        return item;
    }
}
