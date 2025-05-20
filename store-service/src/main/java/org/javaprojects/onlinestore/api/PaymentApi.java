package org.javaprojects.onlinestore.api;

import org.javaprojects.onlinestore.client.ApiClient;

import org.javaprojects.onlinestore.models.UpdateBalanceResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.12.0")
@Component
public class PaymentApi {
    private ApiClient apiClient;

    public PaymentApi() {
        this(new ApiClient());
    }

    @Autowired
    public PaymentApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    
    /**
     * Making a payment
     * This is request to subtract the amount from the account.  If the amount is greater than the balance, it will return false. 
     * <p><b>200</b> - Payment information This will return true if the payment was successful, false otherwise. 
     * <p><b>400</b> - Bad request in case of invalid amount
     * @param amount Amount to be paid
     * @return UpdateBalanceResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec makePaymentRequestCreation(Float amount) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'amount' is set
        if (amount == null) {
            throw new WebClientResponseException("Missing the required parameter 'amount' when calling makePayment", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("amount", amount);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] {  };

        ParameterizedTypeReference<UpdateBalanceResponse> localVarReturnType = new ParameterizedTypeReference<UpdateBalanceResponse>() {};
        return apiClient.invokeAPI("/pay/{amount}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Making a payment
     * This is request to subtract the amount from the account.  If the amount is greater than the balance, it will return false. 
     * <p><b>200</b> - Payment information This will return true if the payment was successful, false otherwise. 
     * <p><b>400</b> - Bad request in case of invalid amount
     * @param amount Amount to be paid
     * @return UpdateBalanceResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<UpdateBalanceResponse> makePayment(Float amount) throws WebClientResponseException {
        ParameterizedTypeReference<UpdateBalanceResponse> localVarReturnType = new ParameterizedTypeReference<UpdateBalanceResponse>() {};
        return makePaymentRequestCreation(amount).bodyToMono(localVarReturnType);
    }

    /**
     * Making a payment
     * This is request to subtract the amount from the account.  If the amount is greater than the balance, it will return false. 
     * <p><b>200</b> - Payment information This will return true if the payment was successful, false otherwise. 
     * <p><b>400</b> - Bad request in case of invalid amount
     * @param amount Amount to be paid
     * @return ResponseEntity&lt;UpdateBalanceResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<UpdateBalanceResponse>> makePaymentWithHttpInfo(Float amount) throws WebClientResponseException {
        ParameterizedTypeReference<UpdateBalanceResponse> localVarReturnType = new ParameterizedTypeReference<UpdateBalanceResponse>() {};
        return makePaymentRequestCreation(amount).toEntity(localVarReturnType);
    }

    /**
     * Making a payment
     * This is request to subtract the amount from the account.  If the amount is greater than the balance, it will return false. 
     * <p><b>200</b> - Payment information This will return true if the payment was successful, false otherwise. 
     * <p><b>400</b> - Bad request in case of invalid amount
     * @param amount Amount to be paid
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec makePaymentWithResponseSpec(Float amount) throws WebClientResponseException {
        return makePaymentRequestCreation(amount);
    }
}
