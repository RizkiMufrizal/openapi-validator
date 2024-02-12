package com.axway.library;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.OpenApiInteractionValidator.ApiLoadException;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReport.Message;
import com.axway.library.common.CacheInstance;
import com.axway.library.common.Constant;
import com.axway.library.common.TraceLevel;
import com.axway.library.common.Utils;
import com.axway.library.object.MessageValidation;
import com.axway.library.object.MessageValidationError;
import com.vordel.mime.HeaderSet;
import com.vordel.mime.QueryStringHeaderSet;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * OpenAPIValidator
 */
public class OpenAPIValidator implements Serializable {

    private static final CacheInstance cacheInstance = new CacheInstance();

    private OpenApiInteractionValidator validator;

    @Setter
    @Getter
    private int payloadLogMaxLength = 40;

    @Setter
    private boolean decodeQueryParams = true;

    public static synchronized OpenAPIValidator getInstance(String openAPISpec) {
        int hashCode = openAPISpec.hashCode();
        var openAPIValidator = cacheInstance.getCache("Cache-Swagger", hashCode, OpenAPIValidator.class);
        Utils.traceMessage("Cache Key : " + hashCode, TraceLevel.INFO);
        if (openAPIValidator != null) {
            Utils.traceMessage("Using existing OpenAPIValidator instance for API-Specification.", TraceLevel.INFO);
            return openAPIValidator;
        } else {
            Utils.traceMessage("Creating new OpenAPI validator instance for given API-Specification.", TraceLevel.INFO);
            OpenAPIValidator validator = new OpenAPIValidator(openAPISpec);
            cacheInstance.addCache("Cache-Swagger", hashCode, validator);
            return validator;
        }
    }

    public static synchronized OpenAPIValidator getInstance(String apiId, String username, String password, boolean useOriginalAPISpec) throws Exception {
        return getInstance(apiId, username, password, Constant.apiManagerURL, Constant.apiManagerVersion, useOriginalAPISpec);
    }

    public static synchronized OpenAPIValidator getInstance(String apiId, String username, String password, String apiManagerUrl, String version) throws Exception {
        return getInstance(apiId, username, password, apiManagerUrl, version, false);
    }

    public static synchronized OpenAPIValidator getInstance(String apiId, String username, String password) throws Exception {
        return getInstance(apiId, username, password, Constant.apiManagerURL, Constant.apiManagerVersion, false);
    }

    public static synchronized OpenAPIValidator getInstance(String apiId, String username, String password, String apiManagerUrl, String version, boolean useOriginalAPISpec) throws Exception {
        var openAPIValidator = cacheInstance.getCache("Cache-Swagger", apiId, OpenAPIValidator.class);
        Utils.traceMessage("Cache Key : " + apiId, TraceLevel.INFO);
        if (openAPIValidator != null) {
            Utils.traceMessage("Using cached instance of OpenAPI validator for API-ID: " + apiId, TraceLevel.DEBUG);
            return openAPIValidator;
        } else {
            OpenAPIValidator validator = new OpenAPIValidator(apiId, username, password, apiManagerUrl, version, useOriginalAPISpec);
            cacheInstance.addCache("Cache-Swagger", apiId, validator);
            Utils.traceMessage("Returning created OpenAPI validator for API-ID: " + apiId, TraceLevel.DEBUG);
            return validator;
        }
    }

    private OpenAPIValidator(String openAPISpec) {
        super();
        try {
            // Check if openAPISpec is a valid URL
            new URI(openAPISpec);
            // If it does not fail, load it from the URL
            Utils.traceMessage("Creating OpenAPIValidator from URL specification: " + openAPISpec, TraceLevel.INFO);
            this.validator = OpenApiInteractionValidator.createForSpecificationUrl(openAPISpec).build();
        } catch (Exception e) {
            Utils.traceMessage("Creating OpenAPIValidator from inline specification", TraceLevel.INFO);
            this.validator = OpenApiInteractionValidator.createForInlineApiSpecification(openAPISpec).build();
        }
    }

    private OpenAPIValidator(String apiId, String username, String password, String apiManagerUrl, String version, boolean useOriginalAPISpec) throws Exception {
        super();
        try {
            Utils.traceMessage("Creating OpenAPIValidator for: [apiId: '" + apiId + "', username: '" + username + "', password: '*******', apiManagerUrl: '" + apiManagerUrl + "']", TraceLevel.INFO);
            APIManagerSchemaProvider schemaProvider = new APIManagerSchemaProvider(apiManagerUrl, version, username, password);
            schemaProvider.setUseOriginalAPISpec(useOriginalAPISpec);
            String apiSpecification = schemaProvider.getSchema(apiId);
            /* debug seb */
            Utils.traceMessage("Api specification debug seb: " + apiSpecification, TraceLevel.DEBUG);
            this.validator = OpenApiInteractionValidator.createForInlineApiSpecification(apiSpecification).withResolveCombinators(true).build();
        } catch (ApiLoadException e) {
            Utils.traceMessage("The obtained API-Specification is not compatible with the OpenAPI validator.", e, TraceLevel.ERROR);
        } catch (Exception e) {
            Utils.traceMessage("Error creating OpenAPIValidator for given API-ID: " + apiId, e, TraceLevel.ERROR);
            throw e;
        }
    }

    public boolean isValidRequest(String payload, String verb, String path, QueryStringHeaderSet queryParams, HeaderSet headers) {
        var validationReport = validateRequest(payload, verb, path, queryParams, headers);
        return !validationReport.getIsErrorFormat() && !validationReport.getIsErrorRequired();
    }

    public MessageValidationError validateRequest(String payload, String verb, String path, QueryStringHeaderSet queryParams, HeaderSet headers) {
        Utils.traceMessage("Validate request: [verb: " + verb + ", path: '" + path + "', payload: '" + Utils.getContentStart(payload, payloadLogMaxLength, true) + "']", TraceLevel.INFO);
        ValidationReport validationReport = null;
        String originalPath = path;
        // The given path might be the FE-API exposure path which is not guaranteed to be part of the API-Specification.
        // If for example the Petstore is exposed with /petstore/v3 the path we get might be /petstore/v3/store/order/78787
        // and with that, the validation fails, as the specification doesn't contain this path.
        // The following code nevertheless tries to find the matching API path by removing the first part of the path and
        // repeating the process max. 5 times.

        boolean cachePath = false;
        // Perhaps the path has already looked up and matched to the specified path (e.g. /petstore/v3/store/order/78787 --> /store/order/{orderId}
        var cachePathExposure = cacheInstance.getCache("Cache-Swagger", path, Object.class);
        if (cachePathExposure != null) {
            if (cachePathExposure instanceof ValidationReport) {
                // Previously with the given we got a validation error, just return the same again
                validationReport = (ValidationReport) cachePathExposure;
            } else {
                // In that case perform the validation based on the cached path
                validationReport = _validateRequest(payload, verb, (String) cachePathExposure, queryParams, headers);
            }
        } else {
            // Otherwise try to find the belonging specified path
            for (int i = 0; i < 5; i++) {
                if (cachePath) {
                    Utils.traceMessage("Retrying validation with reduced path: '" + path + "']' (" + i + "/5)", TraceLevel.INFO);
                }
                validationReport = _validateRequest(payload, verb, path, queryParams, headers);
                if (validationReport.hasErrors()) {
                    if (validationReport.getMessages().toString().contains("No API path found that matches request")) {
                        // Only cache the path, if a direct hit fails
                        cachePath = true;
                        /*
                         * If no match was found in the API-Spec for the given path, then we remove the first part of the
                         * path because the API might be exposed with a different path by the API-Manager than defined in the spec.
                         * For example: /great-petstore/pet/31233 will not find anything in the first attempt, because the
                         * API does not exist with /great-petstore in the API-Spec. This process is repeated at most 5 times.
                         */
                        if (path.indexOf("/", 1) == -1) {
                            break; // No need to continue as there is only a single path left (/petstore)
                        } else {
                            path = path.substring(path.indexOf("/", 1));
                        }
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            if (cachePath) {
                if (validationReport.hasErrors() && validationReport.getMessages().toString().contains("No API path found that matches request")) {
                    // Finally no match found, cache the returned validation report
                    cacheInstance.addCache("Cache-Swagger", originalPath, validationReport);
                } else {
                    cacheInstance.addCache("Cache-Swagger", originalPath, path);
                }
            }
        }

        var messageValidationError = new MessageValidationError();
        if (validationReport.hasErrors()) {
            List<MessageValidation> messageValidationRequired = new ArrayList<>();
            List<MessageValidation> messageValidationFormatError = new ArrayList<>();
            for (Message message : validationReport.getMessages()) {
                Utils.traceMessage(message.getMessage(), TraceLevel.valueOf(message.getLevel().name()));
                var messageValidation = new MessageValidation(message.getKey(), message.getMessage().replaceAll("\"", ""));
                if (message.getKey().contains("validation.request.body.schema.required")) {
                    messageValidationRequired.add(messageValidation);
                    messageValidationError.setMessageValidationRequired(messageValidationRequired);
                    messageValidationError.setIsErrorRequired(true);
                } else {
                    messageValidationFormatError.add(messageValidation);
                    messageValidationError.setMessageValidationFormatError(messageValidationFormatError);
                    messageValidationError.setIsErrorFormat(true);
                }
            }
        }

        return messageValidationError;
    }

    public boolean isValidResponse(String payload, String verb, String path, int status, HeaderSet headers) {
        Utils.traceMessage("Validate response: [verb: " + verb + ", path: '" + path + "', status: " + status + ", payload: '" + Utils.getContentStart(payload, payloadLogMaxLength, true) + "']", TraceLevel.INFO);
        ValidationReport validationReport = validateResponse(payload, verb, path, status, headers);
        return !validationReport.hasErrors();
    }

    public ValidationReport validateResponse(final String payload, String verb, String path, final int status, final HeaderSet headers) {
        return _validateResponse(payload, verb, path, status, headers);
    }

    private ValidationReport _validateRequest(final String payload, final String verb, final String path, final QueryStringHeaderSet queryParams, final HeaderSet headers) {
        Request request = new Request() {
            @Override
            public String getPath() {
                return path;
            }

            @Override
            public Method getMethod() {
                return Request.Method.valueOf(verb);
            }

            @Override
            public Optional<String> getBody() {
                return Optional.ofNullable(payload);
            }

            @Override
            public Collection<String> getQueryParameters() {
                if (queryParams == null) return Collections.emptyList();
                return (queryParams.size() == 0) ? Collections.emptyList() : queryParams.getHeaderSet();
            }

            @Override
            public Collection<String> getQueryParameterValues(String name) {
                if (queryParams == null) return Collections.emptyList();
                ArrayList<String> values = queryParams.getHeaderValues(name);
                if (values == null) return Collections.emptyList();
                if (decodeQueryParams) {
                    values.replaceAll(headerValue -> URLDecoder.decode(headerValue, StandardCharsets.UTF_8));
                }
                return values;
            }

            @Override
            public Map<String, Collection<String>> getHeaders() {
                // Not used for the validation
                return null;
            }

            @Override
            public Collection<String> getHeaderValues(String name) {
                return Utils.getHeaderValues(headers, name);
            }
        };

        var validationReport = validator.validateRequest(request);
        // Finally remove the Content-Type header if exists, as it exists additionally in http.content.headers
        Utils.removeContentTypeHeader(headers);
        return validationReport;
    }

    private ValidationReport _validateResponse(final String payload, String verb, String path, final int status, final HeaderSet headers) {
        Response response = new Response() {
            @Override
            public int getStatus() {
                return status;
            }

            @Override
            public Collection<String> getHeaderValues(String name) {
                return Utils.getHeaderValues(headers, name);
            }

            @Override
            public Optional<String> getBody() {
                return Optional.ofNullable(payload);
            }
        };

        ValidationReport validationReport = validator.validateResponse(path, Request.Method.valueOf(verb), response);
        if (validationReport.hasErrors()) {
            for (Message message : validationReport.getMessages()) {
                Utils.traceMessage(message.getMessage(), TraceLevel.valueOf(message.getLevel().name()));
            }
        }
        return validationReport;
    }

}
