package com.social.query;

import com.social.common.model.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author ycshang
 */
@Data
@Schema(description = "字典查询")
public class DictQuery extends Query {
    @Schema(description = "字典名称")
    private String title;
}
