package clientKit;


import Util.*;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

/**
 * Created by wjw_w on 2017/6/25.
 */
public class ChatHandler {

    private Socket mSocket;
    private Chattable mChatter;
    private Map<String, String> mPrivateKeyChain;
    private Map<String, String> mPublicKeyChain;
    private Stack<String> mKeyIDList;
    private String mSelfUsername;
    private String mOppositeUsername;
    private Object keyListLock = new Object();

    public ChatHandler(Socket s, Chattable chatter,String selfUsername) throws IOException {
        mSelfUsername =selfUsername;
        mSocket = s;
        mChatter = chatter;
        mPrivateKeyChain = new HashMap<>();
        mPublicKeyChain = new HashMap<>();
        mKeyIDList = new Stack<>();
        new Thread(new CharReceiver()).start();
        sendHandshakeMsg();

    }

    public String getSelfUsername(){
        return mSelfUsername;
    }

    public String getOppositeUsername() {
        return mOppositeUsername;
    }

    public String getLink(){
        return mSelfUsername+" -> "+mOppositeUsername;
    }

    public void sendHandshakeMsg() throws IOException {
        String selfKeyID = UUID.randomUUID().toString();
        String selfKey = generateSelfPublicKey(selfKeyID);
        Request request = new Request.Builder(Xcr3TProtocol.REQUEST_HANDSHAKE)
                .put("uid", mSelfUsername)
                .putEncryptID(selfKeyID)
                .putSelfPublicKey(selfKey)
                .build();
        Xcr3TClient.send(mSocket, request);
    }

    public void sendChat(String chat) throws IOException {

        //STEP1: Request key
        Request keyRequest = new Request.Builder(Xcr3TProtocol.REQUEST_GET_KEY)
                .build();
        Xcr3TClient.send(mSocket, keyRequest);

        //STEP2: Check keyList
        synchronized (keyListLock) {

            while (mKeyIDList.empty()) {
                try {
                    keyListLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        //STEP3: Send message using receivedKey
        String destKeyID = mKeyIDList.pop();
        String destKey = mPublicKeyChain.remove(destKeyID);


        Request request = new Request.Builder(Xcr3TProtocol.REQUEST_CHAT)
                .setDestinationPublicKey(destKey)
                .put("chat", chat)
                .putDecryptID(destKeyID)
                .build();
        Xcr3TClient.send(mSocket, request);
    }

    public void disconnect()throws IOException{
        Request request = new Request.Builder(Xcr3TProtocol.REQUEST_GOODBYE)
                .build();
        Xcr3TClient.send(mSocket, request);
    }


    private String generateSelfPublicKey(String keyID) throws IOException {
        KeyPair keyPair = Xcr3TClient.mKeyPairGenerator.generateKeyPair();
        mPrivateKeyChain.put(keyID, CryptorUtil.encryptBASE64(keyPair.getPrivate().getEncoded()));
        return CryptorUtil.encryptBASE64(keyPair.getPublic().getEncoded());   //己方PuK本地不保存，使用一次后交由GC处理
    }

    private class CharReceiver implements Runnable {

        @Override
        public void run() {
            try {
                while (mSocket.isConnected()) {
                    RequestParser parser = parseChat(mSocket);
                    JSONObject chatJSON = parser.getJSON();
                    //System.out.println(parser.getProtocolHeader() + "\r\n" + chatJSON.toString());

                    if (!parser.isChat())
                        throw new IllegalStateException("It's not a chat");

                    if (parser.isProtocolHeader(Xcr3TProtocol.REQUEST_GET_KEY)) {
                        String selfKeyID = UUID.randomUUID().toString();
                        String selfKey = generateSelfPublicKey(selfKeyID);
                        Response keyResponse = new Response.Builder(Xcr3TProtocol.RESPONSE_200_OK)
                                .setResponseName(Xcr3TClient.CLIENT_NAME)
                                .put("encryptID", selfKeyID)
                                .put("publicKey", selfKey)
                                .build();
                        Xcr3TClient.send(mSocket, keyResponse);
                    }

                    if (parser.isProtocolHeader(Xcr3TProtocol.REQUEST_CHAT)) {
                        String decryptID = chatJSON.getString("decryptID");
                        String decryptKey = mPrivateKeyChain.remove(decryptID);
                        String chatText = CryptorUtil.unpack(decryptKey, chatJSON.getString("chat"));
                        //TODO: 输出内容
                        mChatter.showChat(getSelfUsername()+" <- "+getOppositeUsername()+": "+chatText);
                    }

                    if (parser.isProtocolHeader(Xcr3TProtocol.RESPONSE_200_OK) && chatJSON.has("publicKey")) {
                        String destKey = chatJSON.getString("publicKey");
                        String destKeyID = chatJSON.getString("encryptID");
                        synchronized (keyListLock) {
                            mKeyIDList.push(destKeyID);
                            mPublicKeyChain.put(destKeyID, destKey);
                            keyListLock.notify();
                        }
                    }

                    if (parser.isProtocolHeader(Xcr3TProtocol.REQUEST_HANDSHAKE)) {
                        mOppositeUsername = chatJSON.getString("uid");
                        mChatter.incoming(ChatHandler.this);
                    }

                    if(parser.isProtocolHeader(Xcr3TProtocol.REQUEST_GOODBYE)){
                        Response keyResponse = new Response.Builder(Xcr3TProtocol.RESPONSE_200_OK)
                                .setResponseName(Xcr3TClient.CLIENT_NAME)
                                .put("goodbye","true")
                                .build();
                        Xcr3TClient.send(mSocket, keyResponse);
                        mChatter.disconnecting(ChatHandler.this);
                        mSocket.close();
                    }
                    if (parser.isProtocolHeader(Xcr3TProtocol.RESPONSE_200_OK) && chatJSON.has("goodbye")) {
                        if(chatJSON.getString("goodbye").equals("true"))
                            mChatter.disconnecting(ChatHandler.this);
                            mSocket.close();

                    }
                }
            } catch (IOException e) {
                mChatter.printLog("Connection Closed: "+getLink());
                mChatter.disconnecting(ChatHandler.this);
            }
        }

        private RequestParser parseChat(Socket socket) throws IOException {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            StringBuilder chatContent = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                chatContent.append(line);
                chatContent.append("\r\n");
                if (line.isEmpty())
                    break;
            }

            if (chatContent.toString().startsWith(Xcr3TProtocol.RESPONSE_200_OK)) {
                while ((line = bufferedReader.readLine()) != null) {
                    chatContent.append(line);
                    chatContent.append("\r\n");
                    if (line.isEmpty())
                        break;
                }
            }

            return new RequestParser(chatContent.toString());
        }
    }

}
