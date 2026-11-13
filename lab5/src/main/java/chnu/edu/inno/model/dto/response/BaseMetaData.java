package chnu.edu.inno.model.dto.response;

/**
 * @author Rout
 * @version 1.0.0
 * @project lab5
 * @class src\main\java\chnu\edu\inno\model\dto\response\BaseMetaData
 * @since 13.11.2026 - 23.55
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class BaseMetaData {
    @Builder.Default
    private int code = 200;
    @Builder.Default
    private boolean success = true;
    @Builder.Default
    private String errorMessage = null;

    public BaseMetaData(int code, boolean success) {
        this.code = code;
        this.success = success;
    }

}
