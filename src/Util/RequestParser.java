package Util;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by wjw_w on 2017/4/8.
 */
public class RequestParser {
    private final String mRawRequest;
    private String mMethod;
    private String mProtocolHeader;
    private JSONObject mJSON;
    private HashMap<String, String> mQueryMap = new HashMap<>();
    private boolean isBadRequest;

    public RequestParser(String rawRequest) {
        mRawRequest = rawRequest;
        if (mRawRequest.isEmpty()) {
            isBadRequest = true;
            throw new IllegalStateException("Empty Request");
        }
        parseRequest();
    }

    private RequestParser(String rawRequest, String method, String func) {
        mRawRequest = rawRequest;
        mMethod = method;
    }

    public String getRawRequest() {
        return mRawRequest;
    }

    public String getRequestMethod() {
        return mMethod;
    }

    public boolean isRequestMethod(String methodType) {
        return mMethod.equals(methodType);
    }

    public String getProtocolHeader() {
        return mProtocolHeader;
    }

    public boolean isProtocolHeader(String protocolHeader) {
        return mProtocolHeader.startsWith(protocolHeader);
    }

    public boolean isChat() {
        return isProtocolHeader(Xcr3TProtocol.REQUEST_CHAT)
                || isProtocolHeader(Xcr3TProtocol.REQUEST_HANDSHAKE)
                || isProtocolHeader(Xcr3TProtocol.REQUEST_GET_KEY)
                ||isProtocolHeader(Xcr3TProtocol.REQUEST_GOODBYE)
                ||isProtocolHeader(Xcr3TProtocol.RESPONSE_200_OK);
    }

    public JSONObject getJSON() {
        return mJSON;
    }

    public HashMap<String, String> getQueryMap() {
        return mQueryMap;
    }

    public String getQueryValue(String key) {
        return mQueryMap.get(key);
    }

    public boolean isBadRequest() {
        return isBadRequest;
    }

    public static RequestParser generateBadRequest() {
        return new RequestParser("", "", "/bad.request");
    }

    private void parseRequest() {

        mProtocolHeader = mRawRequest.substring(0, mRawRequest.indexOf("\r\n")).trim();
        mMethod = mProtocolHeader.substring(0, mProtocolHeader.indexOf("/")).trim();

        try {
            if (mRawRequest.contains("{")) {
                String jsonRaw = mRawRequest.substring(mRawRequest.indexOf("{"), mRawRequest.indexOf("}") + 1);
                mJSON = new JSONObject(jsonRaw);
            }
        } catch (Exception e) {
            throw new IllegalStateException("No JSON Found");
        }

    }

    private void fillQueryMap(String queryString) {
        for (String querySet : queryString.split("&")) {
            String queryKey = querySet.split("=")[0];
            String queryValue = "";
            try {
                queryValue = querySet.split("=")[1];
            } catch (ArrayIndexOutOfBoundsException e) {
                queryValue = "";
            }
            mQueryMap.put(queryKey, queryValue);
        }
    }
}
