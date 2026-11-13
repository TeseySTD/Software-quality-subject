package chnu.edu.inno.model.dto.response;

/**
 * @author Rout
 * @version 1.0.0
 * @project lab5
 * @class src\main\java\chnu\edu\inno\model\dto\response\PaginationMetaData
 * @since 13.11.2026 - 23.55
 */

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class PaginationMetaData extends BaseMetaData {
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean isFirst;
    private boolean isLast;

}
