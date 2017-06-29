package Util;

/**
 * Created by wjw_w on 2017/6/24.
 */
public class Xcr3TProtocol {

    public static final String REQUEST_ADD="POST /register HTTP/1.1";
    public static final String REQUEST_HANDSHAKE ="POST /handshake HTTP/1.1";
    public static final String REQUEST_GET_KEY="GET /key HTTP/1.1";
    public static final String REQUEST_FIND="POST /find HTTP/1.1";
    public static final String REQUEST_GOODBYE ="POST /goodbye HTTP/1.1";

    public static final String REQUEST_CHAT="POST /chat HTTP/1.1";

    public static final String RESPONSE_200_OK="HTTP/1.1 200 OK";
    public static final String RESPONSE_400_BAD_REQUEST="HTTP/1.1 400 Bad Request";

}
