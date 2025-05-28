package org.javaprojects.onlinestore.api;

import org.javaprojects.onlinestore.client.ApiClient;

import org.javaprojects.onlinestore.models.GetBalanceResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.12.0")
public class BalanceApi {
    private ApiClient apiClient;

    public BalanceApi() {
        this(new ApiClient());
    }

    public BalanceApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    
    /**
     * Get the balance on the account
     * 
     * <p><b>200</b> - Balance information
     * @return GetBalanceResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getBalanceRequestCreation() throws WebClientResponseException {
        Object postBody = null;
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

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

        String[] localVarAuthNames = new String[] { "shop-auth" };

        ParameterizedTypeReference<GetBalanceResponse> localVarReturnType = new ParameterizedTypeReference<GetBalanceResponse>() {};
        return apiClient.invokeAPI("/balance", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Get the balance on the account
     * 
     * <p><b>200</b> - Balance information
     * @return GetBalanceResponse
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<GetBalanceResponse> getBalance() throws WebClientResponseException {
        ParameterizedTypeReference<GetBalanceResponse> localVarReturnType = new ParameterizedTypeReference<GetBalanceResponse>() {};
        return getBalanceRequestCreation().bodyToMono(localVarReturnType);
    }

    /**
     * Get the balance on the account
     * 
     * <p><b>200</b> - Balance information
     * @return ResponseEntity&lt;GetBalanceResponse&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<GetBalanceResponse>> getBalanceWithHttpInfo() throws WebClientResponseException {
        ParameterizedTypeReference<GetBalanceResponse> localVarReturnType = new ParameterizedTypeReference<GetBalanceResponse>() {};
        return getBalanceRequestCreation().toEntity(localVarReturnType);
    }

    /**
     * Get the balance on the account
     * 
     * <p><b>200</b> - Balance information
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getBalanceWithResponseSpec() throws WebClientResponseException {
        return getBalanceRequestCreation();
    }
}
