package chnu.edu.inno.model;

/**
 * @author Rout
 * @version 1.0.0
 * @project lab5
 * @class src\main\java\chnu\edu\inno\model\Item
 * @since 13.11.2026 - 23.55
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Item {
    @Id
    private String id;
    private String name;
    private String code;
    private int quantity;

    public Item(String name, String code, int quantity) {
        this.name = name;
        this.code = code;
        this.quantity = quantity;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof Item item)) return false;

        return java.util.Objects.equals(getId(), item.getId());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(getId());
    }
}
