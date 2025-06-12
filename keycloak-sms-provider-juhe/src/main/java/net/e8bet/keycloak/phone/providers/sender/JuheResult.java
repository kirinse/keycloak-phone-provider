package net.e8bet.keycloak.phone.providers.sender;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class JuheResult {
    @JsonProperty("error_code")
    private int errorCode;
    @JsonProperty("reason")
    private String reason;
    @JsonProperty("result")
    private JsonNode result;

    public JuheResult() {
    }

    public int getErrorCode() {
        return this.errorCode;
    }

    public String getReason() {
        return this.reason;
    }

    public JsonNode getResult() {
        return this.result;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setResult(JsonNode result) {
        this.result = result;
    }
}
