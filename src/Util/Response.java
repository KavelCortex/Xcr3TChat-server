package Util;

import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by wjw_w on 2017/4/8.
 */
public class Response {

    private static final String SERVER_NAME = "Kavel's Xcr3Tchat Server/0.1";
    private String mResponseName;
    private String mProtocolHeader;
    private int mContentLength = 0;
    private JSONObject mJSON;
    private String mEncryptedResponse;

    private Response(){}

    private Response(Builder builder) {
        mResponseName = builder.mResponseName;
        mProtocolHeader = builder.mProtocolHeader;
        mEncryptedResponse = builder.mEncryptedResponse;
        mContentLength = mEncryptedResponse.length();
        mJSON = builder.mJSON;
    }

    public String getServerName() {
        return mResponseName;
    }

    public String getProtocolHeader() {
        return mProtocolHeader;
    }

    public JSONObject getJSON() {
        return mJSON;
    }

    public int getContentLength() {
        return mContentLength;
    }

    @Override
    public String toString() {
        StringBuilder response = new StringBuilder();
        response.append(mProtocolHeader + "\r\n");
        response.append("Server: " + mResponseName + "\r\n");
        response.append("Content-Length: " + mContentLength + "\r\n");
        response.append("\r\n");
        response.append(mEncryptedResponse);
        response.append("\r\n\r\n");
        return response.toString();
    }

    public static class Builder {

        private String mResponseName;
        private String mProtocolHeader;
        private String mDestPublicKey;
        private JSONObject mJSON;
        private String mEncryptedResponse;
        private boolean needEncrypt;

        public Builder(String protocolHeader) {
            mProtocolHeader = protocolHeader;
            mJSON = new JSONObject();
            mResponseName=SERVER_NAME;
        }

        public Builder setResponseName(String name){
            mResponseName=name;
            return this;
        }

        public Builder setDestinationPublicKey(String BASE64PubKey) {
            needEncrypt = true;
            mDestPublicKey = BASE64PubKey;
            return this;
        }

        public Builder put(String key, String value) throws IOException {
            mJSON.put(key, value);
            return this;
        }

        public Response build() throws IOException {
            if (needEncrypt)
                mEncryptedResponse = CryptorUtil.pack(mDestPublicKey, mJSON.toString());
            else
                mEncryptedResponse = mJSON.toString();
            return new Response(this);
        }

    }

}
