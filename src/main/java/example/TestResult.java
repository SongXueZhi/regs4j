package example;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @Author: sxz
 * @Date: 2023/02/03/18:14
 * @Description:
 */
public class TestResult {
    private String errCode;
    private STATUS status;
    private String message;

    public TestResult(String errCode, String message) {
        this.errCode = errCode;
        this.message = message;
    }
    public TestResult() {
    }

    public String getErrCode() {
        return errCode;
    }

    public void setErrCode(String errCode) {
        this.errCode = errCode;
    }

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public enum STATUS {
        PASS,
        FAIL,
        NOTEST,
        CE;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("errCode", errCode)
                .append("status", status)
                .append("message", message)
                .toString();
    }
}
