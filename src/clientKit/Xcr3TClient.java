package clientKit;

import Util.CryptorUtil;
import Util.Request;
import Util.ResponseParser;
import Util.Xcr3TProtocol;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wjw_w on 2017/6/22.
 */
public class Xcr3TClient {

    public static final String CLIENT_NAME = "Kavel's Xcr3Tchat Client/0.1";

    private String mUID;
    private String mPassword;
    private ServerSocket mServerSocket;
    private Chattable mChatter;
    private ChatHandler mChatHandler;

    private int mPort;
    protected static KeyPairGenerator mKeyPairGenerator;
    private String mPrivateKey; //己方PrK
    private String mToken = "";
    public final static String SERVERNAME = "localhost";
    public final static int SERVERPORT = 54213;
    private final static String SERVER_PUBLIC_KEY =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCUC8HZmsk2fdHBYTaucuqkSN2EeeKUmcnqrPMg\n" +
                    "9RXxr3QaY8xwseP625eMS70rfgaz/0LmAHenm6rvKkWlGE1M3dr6RwOTXlNAbEW0c1fpfMqY9dd6\n" +
                    "PNw5jn7JxjIrVakscO+eDTRsRq1OX9LHW+qkswjt2RkSIo9ffvTL96n3SQIDAQAB";

    public Xcr3TClient(String uid, String psw,Chattable chatter) {
        mUID = uid;
        mPassword = psw;
        mChatter=chatter;
        try {
            mKeyPairGenerator = KeyPairGenerator.getInstance("RSA");
            mKeyPairGenerator.initialize(1024, new SecureRandom());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void setChatter(Chattable chatter) {
        mChatter = chatter;
    }

    public String getUsername() {
        return mUID;
    }

    public ChatHandler getChatHandler() {
        return mChatHandler;
    }


    private String generatePublicKey() throws IOException {
        KeyPair keyPair = mKeyPairGenerator.generateKeyPair();
        mPrivateKey = CryptorUtil.encryptBASE64(keyPair.getPrivate().getEncoded()); //每次生成新KeyPair后将覆盖旧PrK
        return CryptorUtil.encryptBASE64(keyPair.getPublic().getEncoded());   //己方PuK本地不保存，使用一次后交由GC处理
    }


    /**
     * 添加当前用户
     *
     * @return 服务器返回添加结果
     * @throws IOException
     */
    public boolean register() throws IOException {

        String pswMD5 = CryptorUtil.getRandomSaltedMD5(mPassword);

        Request request = new Request.Builder(Xcr3TProtocol.REQUEST_ADD)
                .setDestinationPublicKey(SERVER_PUBLIC_KEY)
                .put("uid", mUID)
                .put("identity", pswMD5)
                .putSelfPublicKey(generatePublicKey())
                .build();

        ResponseParser parser = sendToServerAndParse(request);

        if (parser.isStatusOK()) {
            mChatter.printLog("You have successfully registered as: " + getUsername());
            return true;
        } else {
            if (parser.getJSON().has("error"))
                mChatter.printLog(parser.getJSON().getString("error"));
            return false;

        }

    }

    /**
     * 登录操作
     *
     * @return 服务器返回握手结果
     * @throws IOException
     */
    public boolean login() throws IOException {
        mServerSocket = new ServerSocket(0);
        mPort = mServerSocket.getLocalPort();
        Request request = new Request.Builder(Xcr3TProtocol.REQUEST_HANDSHAKE)
                .setDestinationPublicKey(SERVER_PUBLIC_KEY)
                .put("uid", mUID)
                .put("identity", mPassword)
                .put("port", "" + mPort)
                .putSelfPublicKey(generatePublicKey())
                .build();

        ResponseParser parser = sendToServerAndParse(request);

        if (parser.isStatusOK()) {
            mToken = parser.getJSON().getString("token");
            new Thread(new ChatListener(mServerSocket, mChatter,getUsername())).start();
            mChatter.printLog("You have logged in as: " + getUsername());
            return true;
        } else {
            if (parser.getJSON().has("error"))
                mChatter.printLog(parser.getJSON().getString("error"));
            return false;
        }
    }

    /**
     * 查询某用户在线状态，接通以后将使用Chattable.incoming()回调进行通知
     *
     * @param uid 要查询的用户ID
     * @return 查询结果
     * @throws IllegalStateException
     * @throws IOException
     */
    public boolean find(String uid) throws IllegalStateException, IOException {
        if (mToken.isEmpty())
            throw new IllegalStateException("Please login first!");

        Request request = new Request.Builder(Xcr3TProtocol.REQUEST_FIND)
                .setDestinationPublicKey(SERVER_PUBLIC_KEY)
                .put("destUID", uid)
                .put("token", mToken)
                .putSelfPublicKey(generatePublicKey())
                .build();

        ResponseParser parser = sendToServerAndParse(request);
        if (parser.isStatusERROR())
            throw new IllegalStateException(parser.getJSON().getString("error"));
        if (parser.isStatusOK() && parser.getJSON().getString("ready").equals("true")) {
            String host = parser.getJSON().getString("ip");
            int port = Integer.parseInt(parser.getJSON().getString("port"));
            Socket s = new Socket(host, port);
            new ChatHandler(s, mChatter,getUsername());
            return true;
        } else
            return false;
    }


    public void logout() throws IOException {
        Request request = new Request.Builder(Xcr3TProtocol.REQUEST_GOODBYE)
                .setDestinationPublicKey(SERVER_PUBLIC_KEY)
                .put("uid", mUID)
                .put("token", mToken)
                .putSelfPublicKey(generatePublicKey())
                .build();
        ResponseParser parser = sendToServerAndParse(request);
        if (parser.isStatusOK()) {
            mToken = "";
            mServerSocket.close();
            mPort = 0;

            mChatter.printLog("You (" + getUsername() + ") have logged out.");
        }
    }

    /**
     * 向服务器发送请求并返回结果
     *
     * @param request 经Request类包装后的请求对象
     * @return 经ResponseParser类包装后的返回结果对象
     * @throws IOException
     */
    private ResponseParser sendToServerAndParse(Request request) throws IOException {

        Socket socket = new Socket(SERVERNAME, SERVERPORT);
        send(socket, request);
        ResponseParser parser = parse(socket, mPrivateKey);
        mPrivateKey = null; //使用一次后清除该私钥
        socket.close();
        return parser;
    }


    protected static void send(Socket socket, Object request) throws IOException {

        System.out.println(request.toString());
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        bufferedWriter.write(request.toString());
        bufferedWriter.flush();
    }

    protected static ResponseParser parse(Socket socket, String BASE64PriKey) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line;
        StringBuilder header = new StringBuilder();

        while ((line = bufferedReader.readLine()) != null) {
            header.append(line);
            header.append("\r\n");
            if (line.isEmpty())
                break;
        }
        StringBuilder msg = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null) {
            msg.append(line);
            msg.append("\r\n");
            if (line.isEmpty())
                break;
        }

        ResponseParser parser = new ResponseParser(header.toString(), msg.toString(), BASE64PriKey);

        System.out.println(parser.getJSON().toString());
        return parser;
    }
}
