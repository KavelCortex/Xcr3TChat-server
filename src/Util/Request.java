package Util;

import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by wjw_w on 2017/6/22.
 */
public class Request {
    private String mProtocolHeader;
    private JSONObject mJSON;

    private Request(){}

    private Request(Builder builder) {
        mProtocolHeader = builder.mProtocolHeader;
        mJSON = builder.mJSON;
    }

    @Override
    public String toString() {
        StringBuilder request = new StringBuilder();
        request.append(mProtocolHeader);
        request.append("\r\n");
        request.append(mJSON.length()>0?mJSON.toString():"");
        request.append("\r\n\r\n");
        return request.toString();
    }

    public static class Builder {
        private String mProtocolHeader;
        private String mDestPublicKey;
        private JSONObject mJSON;
        private boolean needEncrypt;

        public Builder(String protocolHeader) {
            mProtocolHeader = protocolHeader;
            mJSON = new JSONObject();
        }

        public Builder setDestinationPublicKey(String BASE64PubKey) {
            needEncrypt = true;
            mDestPublicKey = BASE64PubKey;
            return this;
        }

        public Builder put(String key, String value) throws IOException {
            String encryptedValue;
            if (needEncrypt)
                encryptedValue = CryptorUtil.pack(mDestPublicKey, value);
            else
                encryptedValue = value;
            mJSON.put(key, encryptedValue);
            return this;
        }

        public Builder putEncryptID(String id){
            mJSON.put("encryptID",id);
            return this;
        }
        public Builder putDecryptID(String id){
            mJSON.put("decryptID",id);
            return this;
        }

        public Builder putSelfPublicKey(String BASE64PubKey) {
            mJSON.put("publicKey", BASE64PubKey);
            return this;
        }

        public Request build() {
            return new Request(this);
        }
    }

}
