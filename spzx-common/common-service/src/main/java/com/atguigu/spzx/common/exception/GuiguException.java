package com.atguigu.spzx.common.exception;

import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import lombok.Data;
/**
 * @author Sunk
 * @version 1.0
 * @description:
 * @date 2024/5/24 21:07
 */
@Data
public class GuiguException extends RuntimeException{
    private Integer code;
    private String msg;
    private ResultCodeEnum resultCodeEnum;

    public GuiguException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
        this.msg = resultCodeEnum.getMessage();
        this.resultCodeEnum = resultCodeEnum;
    }

}
