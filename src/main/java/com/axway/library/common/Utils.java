package com.axway.library.common;

import com.vordel.mime.HeaderSet;
import com.vordel.mime.HeaderSet.Header;
import com.vordel.mime.HeaderSet.HeaderEntry;
import com.vordel.trace.Trace;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;

@Slf4j
public class Utils {

    private static boolean useAPIGatewayTrace = true;

    private static boolean initTraceMode = true;

    public static void traceMessage(String message, TraceLevel level) {
        traceMessage(message, null, level);
    }

    public static void traceMessage(String message, Throwable t, TraceLevel level) {
        setupTraceMode();
        if (level == null) level = TraceLevel.ERROR;
        String LOG_PREFIX = "OpenAPIValidator: ";
        switch (level) {
            case FATAL:
                if (useAPIGatewayTrace) {
                    Trace.fatal(LOG_PREFIX + message, t);
                } else {
                    log.error(message, t);
                }
                break;
            case ALWAYS:
                if (useAPIGatewayTrace) {
                    Trace.always(LOG_PREFIX + message, t);
                } else {
                    log.info(message, t);
                }
                break;
            case ERROR:
                if (useAPIGatewayTrace) {
                    Trace.error(LOG_PREFIX + message, t);
                } else {
                    log.error(message, t);
                }
                break;
            case INFO:
                if (useAPIGatewayTrace) {
                    Trace.info(LOG_PREFIX + message, t);
                } else {
                    log.info(message, t);
                }
                break;
            case MIN:
                if (useAPIGatewayTrace) {
                    Trace.min(LOG_PREFIX + message, t);
                } else {
                    log.warn(message, t);
                }
                break;
            case DEBUG:
                if (useAPIGatewayTrace) {
                    Trace.debug(LOG_PREFIX + message, t);
                } else {
                    log.debug(message, t);
                }
                break;
            case DATA:
                if (useAPIGatewayTrace) {
                    Trace.trace(LOG_PREFIX + message, Trace.TRACE_DATA);
                } else {
                    log.trace(message, t);
                }
                break;
            default:
                throw new IllegalArgumentException("Unexpected value: " + level);
        }
    }

    private static void setupTraceMode() {
        if (!initTraceMode) return;
        try {
            Trace.trace("Test OpenAPI validator Trace-Mode", Trace.TRACE_DATA);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError ex) {
            useAPIGatewayTrace = false;
        } catch (Exception ignore) {
        }
        initTraceMode = false;
    }

    public static String getContentStart(String someContent, int maxLength, boolean removeNewLines) {
        try {
            if (someContent == null) return "N/A";
            if (removeNewLines) someContent = someContent.replaceAll("\\r?\\n", "");
            return someContent.length() < maxLength ? someContent : someContent.substring(0, maxLength) + "...(truncated)";
        } catch (Exception e) {
            return "Cannot get content from API-Specification. " + e.getMessage();
        }
    }

    public static Collection<String> getHeaderValues(HeaderSet headers, String name) {
        Collection<String> result = new ArrayList<>();
        if (headers == null) return result;
        HeaderEntry headerValues = headers.getHeaderEntry(name);
        if (headerValues == null) {
            if (name.equalsIgnoreCase("content-type")) {
                traceMessage("WARN - Header: Content-Type not found. Default to application/json.", TraceLevel.DEBUG);
                result.add("application/json");
            } else {
                traceMessage("Header: " + name + " not found.", TraceLevel.DEBUG);
            }
            return result;
        }
        for (Header header : headerValues) {
            result.add(header.toString());
        }
        return result;
    }

    /**
     * The content type may have been merged into http.headers by the policy scripting filter,
     * but additionally exists in http.content.headers.
     * This leads to a duplication of the Content-Type header in the backend request,
     * because the API gateway appends the http.content.header additionally.
     * Therefore, the Content-Type header is removed here again after it was
     * used for validation.
     *
     * @param headers that might contain the Content-Type header
     */
    public static void removeContentTypeHeader(HeaderSet headers) {
        HeaderEntry contentTypeHeader = headers.getHeaderEntry("Content-Type");
        if (contentTypeHeader != null) {
            traceMessage("INFO - Remove Content-Type header as it exists in the http.content.headers attribute and this would result in a duplicate.", TraceLevel.DEBUG);
            headers.remove("Content-Type");
        }
    }
}
