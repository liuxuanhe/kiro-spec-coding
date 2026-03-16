package com.parking.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 请求上下文工具类，用于获取当前请求的 requestId
 */
public final class RequestContext {

    private static final String REQUEST_ID_KEY = "requestId";

    private RequestContext() {
    }

    public static String getRequestId() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                Object requestId = request.getAttribute(REQUEST_ID_KEY);
                if (requestId != null) {
                    return requestId.toString();
                }
            }
        } catch (Exception ignored) {
            // outside of request scope
        }
        return "N/A";
    }
}
