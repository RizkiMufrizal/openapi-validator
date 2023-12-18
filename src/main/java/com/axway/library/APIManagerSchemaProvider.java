package com.axway.library;

import com.axway.library.client.APIRepoRestClient;
import com.axway.library.common.TraceLevel;
import com.axway.library.common.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

public class APIManagerSchemaProvider {

    private final APIRepoRestClient apiRepoRestClient;
    ObjectMapper mapper = new ObjectMapper();

    @Setter
    @Getter
    private boolean useOriginalAPISpec = false;

    public APIManagerSchemaProvider(String apiManagerUrl, String version, String username, String password) {
        super();
        this.apiRepoRestClient = new APIRepoRestClient(apiManagerUrl, version, username, password);
    }

    public String getSchema(String apiId) throws Exception {
        Utils.traceMessage("Getschema debug seb", TraceLevel.INFO);
        if (!useOriginalAPISpec) {

            Utils.traceMessage("call getFrontendAPISpec debug seb", TraceLevel.INFO);
            return getFrontendAPISpec(apiId);
        } else {
            Utils.traceMessage("call getBackendAPIID debug seb", TraceLevel.INFO);
            String backendApiID = getBackendAPIID(apiId);
            Utils.traceMessage("Loading the original imported API specification from the API Manager using backend API-ID: " + backendApiID, TraceLevel.INFO);
            return getBackendAPISpec(backendApiID);
        }
    }

    private String getFrontendAPISpec(String apiId) {
        Utils.traceMessage("getFrontendAPISpec debug seb", TraceLevel.INFO);
        Utils.traceMessage("getFrontendAPISpec debug seb inside try", TraceLevel.INFO);
        var httpResponse = apiRepoRestClient.getSwagger("/discovery/swagger/api/id/" + apiId + "?swaggerVersion=2.0");
        int statusCode = httpResponse.getHttpCode();

        Utils.traceMessage("getFrontendAPISpec debug seb response from 2.0 call" + httpResponse.getBody() + " code : " + statusCode, TraceLevel.INFO);
        if (statusCode != 200) {
            Utils.traceMessage("getFrontendAPISpec debug seb not 200", TraceLevel.INFO);
            if (statusCode == 500) {
                Utils.traceMessage("getFrontendAPISpec debug seb code = 500", TraceLevel.INFO);
                if (httpResponse.getBody() != null) {
                    Utils.traceMessage("getFrontendAPISpec debug seb response != null", TraceLevel.INFO);
                    if (httpResponse.getBody().contains("Failed to download API")) {
                        Utils.traceMessage("getFrontendAPISpec debug seb response.contains", TraceLevel.INFO);
                        Utils.traceMessage("No SwaggerVersion 2.0 API-Specification found, trying to download OpenAPI 3.0 specification.", TraceLevel.DEBUG);
                        httpResponse = apiRepoRestClient.getSwagger("/discovery/swagger/api/id/" + apiId + "?swaggerVersion=3.0");
                        statusCode = httpResponse.getHttpCode();
                        if (statusCode == 200) {
                            return httpResponse.getBody();
                        }
                    }
                }
            }
            Utils.traceMessage("Error getting API-Specification from API-Manager. Received Status-Code: " + statusCode + ", Response: '" + httpResponse.getBody() + "'", TraceLevel.ERROR);
            throw new IllegalArgumentException("Error getting API-Specification from API-Manager.");
        }
        return httpResponse.getBody();
    }

    private String getBackendAPISpec(String backendApiId) throws Exception {
        var httpResponse = apiRepoRestClient.getSwagger("/apirepo/" + backendApiId + "/download?original=true");
        Utils.traceMessage("getBackendAPISpec debug seb", TraceLevel.INFO);
        int statusCode = httpResponse.getHttpCode();
        if (statusCode != 200) {
            Utils.traceMessage("Error getting originally imported API-Specification from API-Manager. Received Status-Code: " + statusCode + ", Response: '" + httpResponse.getBody() + "'", TraceLevel.ERROR);
            throw new IllegalArgumentException("Error getting originally imported API-Specification from API-Manager.");
        }
        return httpResponse.getBody();
    }

    private String getBackendAPIID(String apiId) throws Exception {
        Utils.traceMessage("getBackendAPIID debug seb", TraceLevel.INFO);
        var httpResponse = apiRepoRestClient.getSwagger("/proxies/" + apiId);
        int statusCode = httpResponse.getHttpCode();
        if (statusCode != 200) {
            if (statusCode == 403 && httpResponse.getBody() != null && httpResponse.getBody().contains("Forbidden")) {
                Utils.traceMessage("Please check the given API-ID: '" + apiId + "' is correct, as the API-Manager REST-API returns forbidden for an incorrect API-ID.", TraceLevel.INFO);
            }
            Utils.traceMessage("Error loading Backend-API-ID from API Manager for API: " + apiId + ". Received Status-Code: " + statusCode + ", Response: '" + httpResponse.getBody() + "'", TraceLevel.ERROR);
            throw new IllegalArgumentException("Error loading Backend-API-ID from API Manager for API: " + apiId);
        }
        JsonNode node = mapper.readTree(httpResponse.getBody());
        return node.get("apiId").asText();
    }

}
