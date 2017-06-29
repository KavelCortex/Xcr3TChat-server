package Util;

import org.json.JSONObject;

import javax.crypto.Cipher;
import java.io.IOException;

/**
 * Created by wjw_w on 2017/6/25.
 */
public class ResponseParser {
    private String mHeader;
    private String mMsg;
    private JSONObject mJSON;
    private String mBASE64PriKey;
    private boolean isOK;

    public ResponseParser(String header, String msg, String BASE64PriKey) {
        mHeader = header.split("\n")[0].trim();
        mMsg = msg;
        mBASE64PriKey = BASE64PriKey;
        if (mHeader.isEmpty())
            throw new IllegalStateException("Empty Response");
        parseResponse();
    }

    public JSONObject getJSON() {
        return mJSON;
    }

    public boolean isResponse(String protocolHeader) {
        return mHeader.equals(protocolHeader);
    }

    public boolean isResponseOK() {
        return isOK;
    }

    public boolean isStatusOK() {
        return isOK && mJSON.get("status").equals("OK");
    }

    public boolean isStatusERROR() {
        return mJSON.has("status") && mJSON.get("status").equals("error");
    }

    private void parseResponse() {
        String decryptedMsg = "";

        if (mHeader.equals(Xcr3TProtocol.RESPONSE_200_OK)) {
            isOK = true;
            try {
                byte[] data = CryptorUtil.cryptData(mBASE64PriKey, CryptorUtil.decryptBASE64(mMsg), Cipher.DECRYPT_MODE);
                decryptedMsg = new String(data, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (mHeader.equals(Xcr3TProtocol.RESPONSE_400_BAD_REQUEST)) {
            isOK = false;
            decryptedMsg = mMsg;

        }
        mJSON = new JSONObject(decryptedMsg);
    }
}
