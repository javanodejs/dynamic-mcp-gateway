package com.xeno.mcpg.common;

/**
 * Standard API response wrapper.
 *
 * @param <T> the type of data in the response
 */
public class BaseResponse<T> {

    private int code;
    private String msg;
    private T data;
    private Long count;

    public BaseResponse() {
    }

    public BaseResponse(T data) {
        this(ResponseCode.OK_REQUEST, data);
    }

    public BaseResponse(ResponseCode responseCode, String msg, T data) {
        this.code = responseCode.getCode();
        this.msg = msg;
        this.data = data;
    }

    public BaseResponse(ResponseCode responseCode, T data) {
        this.code = responseCode.getCode();
        this.msg = responseCode.getMessage();
        this.data = data;
    }

    public BaseResponse(ResponseCode responseCode) {
        this.code = responseCode.getCode();
        this.msg = responseCode.getMessage();
    }

    public BaseResponse(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> BaseResponse<T> ok(T data) {
        return new BaseResponse<>(data);
    }

    public static <T> BaseResponse<T> ok() {
        return new BaseResponse<>(ResponseCode.OK_REQUEST);
    }

    public static <T> BaseResponse<T> error(ResponseCode responseCode, String msg) {
        return new BaseResponse<>(responseCode, msg, null);
    }

    public static <T> BaseResponse<T> error(int code, String msg) {
        return new BaseResponse<>(code, msg, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}