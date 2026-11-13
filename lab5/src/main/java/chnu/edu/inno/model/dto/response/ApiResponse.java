package chnu.edu.inno.model.dto.response;

/**
 * @author Rout
 * @version 1.0.0
 * @project lab5
 * @class src\main\java\chnu\edu\inno\model\dto\response\ApiResponse
 * @since 13.11.2026 - 23.55
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ApiResponse<M extends BaseMetaData, D> {
    private M meta;
    private List<D> data;

    public ApiResponse(M meta, D data) {
        this.meta = meta;
        this.data = new ArrayList<>();
        this.data.add(data);
    }

    public ApiResponse(M meta) {
        this.meta = meta;
    }
}
