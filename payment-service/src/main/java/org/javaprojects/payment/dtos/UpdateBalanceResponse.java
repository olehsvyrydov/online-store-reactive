package org.javaprojects.payment.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * UpdateBalanceResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", comments = "Generator version: 7.12.0")
public class UpdateBalanceResponse implements Serializable {

  private static final long serialVersionUID = 1L;

  private @Nullable Boolean success;

  private @Nullable Float currentBalance;

  private @Nullable String error;

  public UpdateBalanceResponse(@Nullable Boolean success, @Nullable Float currentBalance, @Nullable String error)
  {
    this.success = success;
    this.currentBalance = currentBalance;
    this.error = error;
  }

  public UpdateBalanceResponse success(Boolean success) {
    this.success = success;
    return this;
  }

  /**
   * Get success
   * @return success
   */
  
  @Schema(name = "success", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("success")
  public Boolean getSuccess() {
    return success;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }

  public UpdateBalanceResponse currentBalance(Float currentBalance) {
    this.currentBalance = currentBalance;
    return this;
  }

  /**
   * Get currentBalance
   * @return currentBalance
   */
  
  @Schema(name = "currentBalance", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("currentBalance")
  public Float getCurrentBalance() {
    return currentBalance;
  }

  public void setCurrentBalance(Float currentBalance) {
    this.currentBalance = currentBalance;
  }

  public UpdateBalanceResponse error(String error) {
    this.error = error;
    return this;
  }

  /**
   * Get error
   * @return error
   */
  
  @Schema(name = "error", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("error")
  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateBalanceResponse updateBalanceResponse = (UpdateBalanceResponse) o;
    return Objects.equals(this.success, updateBalanceResponse.success) &&
        Objects.equals(this.currentBalance, updateBalanceResponse.currentBalance) &&
        Objects.equals(this.error, updateBalanceResponse.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(success, currentBalance, error);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateBalanceResponse {\n");
    sb.append("    success: ").append(toIndentedString(success)).append("\n");
    sb.append("    currentBalance: ").append(toIndentedString(currentBalance)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

