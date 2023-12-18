package com.axway.library.common;

import com.axway.library.configuration.HttpComponentConfiguration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.HttpHostConnectException;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class HttpComponentExecution extends HttpUriRequestBase {

    public HttpComponentExecution(String method, String url) {
        super(method, URI.create(url));
    }

    @SneakyThrows
    public HttpResponse executeJson(int connectTimeout, int activeTimeout, Object requestBody, Map<String, String> headers) {
        headers.forEach(this::addHeader);

        if (requestBody != null) {
            this.setEntity(new StringEntity(JacksonObject.objectMapper().writeValueAsString(requestBody), ContentType.APPLICATION_JSON));
        }

        return this.execute(requestBody, connectTimeout, activeTimeout);
    }


    @SneakyThrows
    private HttpResponse execute(Object requestBody, int connectTimeout, int activeTimeout) {
        Map<String, Object> mapLog = new HashMap<>();
        Map<String, Object> mapLogRequest = new HashMap<>();

        mapLogRequest.put("requestUrl", this.getUri());
        mapLogRequest.put("requestMethod", this.getMethod());
        mapLogRequest.put("requestHeader", this.getHeaders());
        mapLogRequest.put("requestBody", requestBody);
        mapLog.put("request", mapLogRequest);

        try (CloseableHttpClient closeableHttpClient = HttpComponentConfiguration.config(connectTimeout, activeTimeout)) {
            long t1 = System.nanoTime();
            return closeableHttpClient.execute(this, response -> {
                long t2 = System.nanoTime();

                Map<String, Object> mapLogResponse = new HashMap<>();
                mapLogResponse.put("responseTime", this.getRequestUri() + " in " + ((t2 - t1) / 1e6d) + "ms");
                mapLogResponse.put("responseHeader", response.getHeaders());
                mapLogResponse.put("responseStatus", response.getCode());
                mapLogResponse.put("responseMessage", response.getReasonPhrase());
                mapLogResponse.put("responseVersion", response.getVersion().getMajor() + "/" + response.getVersion().getMinor());

                var httpResponse = new HttpResponse();
                httpResponse.setHttpCode(response.getCode());
                httpResponse.setHttpMessage(response.getReasonPhrase());

                if (response.getEntity() != null) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    if (responseBody.isEmpty()) {
                        httpResponse.setBody("");
                        mapLogResponse.put("responseBody", "");
                    } else {
                        httpResponse.setBody(responseBody);
                        mapLogResponse.put("responseBody", responseBody);
                    }
                } else {
                    httpResponse.setBody("");
                    mapLogResponse.put("responseBody", "");
                }
                mapLog.put("response", mapLogResponse);
                Utils.traceMessage(JacksonObject.objectMapper().writeValueAsString(mapLog), TraceLevel.INFO);
                return httpResponse;
            });
        } catch (SocketTimeoutException e) {
            log.error(JacksonObject.objectMapper().writeValueAsString(mapLog));
            Utils.traceMessage("SocketTimeoutException", e, TraceLevel.ERROR);
            throw new SocketTimeoutException();
        } catch (HttpHostConnectException e) {
            log.error(JacksonObject.objectMapper().writeValueAsString(mapLog));
            Utils.traceMessage("HttpHostConnectException", e, TraceLevel.ERROR);
            throw new HttpHostConnectException("connect timeout");
        } catch (Exception e) {
            log.error(JacksonObject.objectMapper().writeValueAsString(mapLog));
            Utils.traceMessage("Exception", e, TraceLevel.ERROR);
            throw new Exception(e);
        }
    }
}
