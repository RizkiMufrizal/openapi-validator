package com.axway.library.client;

import com.axway.library.common.Constant;
import com.axway.library.common.HttpClientHeader;
import com.axway.library.common.HttpComponentExecution;
import com.axway.library.common.HttpResponse;

import java.util.Map;

public class APIRepoRestClient {

    private final String version;
    private final String username;
    private final String password;
    private final String apiManagerUrl;

    public APIRepoRestClient(String apiManagerUrl, String version, String username, String password) {
        this.apiManagerUrl = apiManagerUrl;
        this.version = version;
        this.username = username;
        this.password = password;
    }

    public HttpResponse getSwagger(String path) {
        Map<String, String> headers = HttpClientHeader.setAuthorization(this.username, this.password);
        HttpComponentExecution httpComponentExecution = new HttpComponentExecution("GET", this.apiManagerUrl + Constant.basePortalUrl + this.version + path);
        return httpComponentExecution.executeJson(30000, 30000, null, headers);
    }
}
