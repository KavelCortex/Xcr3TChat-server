package serverKit;

import Util.CryptorUtil;
import Util.RequestParser;
import Util.Response;
import Util.Xcr3TProtocol;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wjw_w on 2017/4/7.
 */
public class SocketHandler implements Runnable {

    private static ConcurrentHashMap<UUID, Socket> clientSocketList = new ConcurrentHashMap<>();

    private UUID mUUID;
    private List<Socket> mSockets = new ArrayList<>();

    SocketHandler(List<Socket> sockets) {
        mSockets = sockets;
    }

    public static Socket getClientSocket(UUID clientUUID) {
        return clientSocketList.get(clientUUID);
    }

    static int getClientServingCount() {
        if (clientSocketList != null)
            return clientSocketList.size();
        else
            return 0;
    }

    int getSocketCount() {
        return mSockets.size();
    }

    public UUID getUUID() {
        return mUUID;
    }

    @Override
    public void run() {
        for (Socket socket : mSockets) {
            try {
                try {
                    mUUID = UUID.randomUUID();
                    clientSocketList.put(mUUID, socket);
                    System.out.println("\nADD:" + getUUID() + "/ " + getClientServingCount() + " serving");
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String line;
                    StringBuilder identity = new StringBuilder();
                    //Identity
                    while ((line = bufferedReader.readLine()) != null) {
                        identity.append(line);
                        identity.append("\r\n");
                        if (line.isEmpty())
                            break;
                    }
                    RequestParser requestParser;
                    requestParser = new RequestParser(identity.toString());
                    boolean doneFlag = false;


                    JSONObject clientJSON = requestParser.getJSON();

                    if (!doneFlag && requestParser.isProtocolHeader(Xcr3TProtocol.REQUEST_ADD)) {
                        try {

                            System.out.println(clientJSON.toString());
                            String uid = CryptorUtil.unpack(Xcr3TServer.SERVER_PRIVATE_KEY, clientJSON.getString("uid"));
                            String pswMD5 = CryptorUtil.unpack(Xcr3TServer.SERVER_PRIVATE_KEY, clientJSON.getString("identity"));
                            //TODO: 添加用户资料进数据库
                            Statement statement = Xcr3TServer.dbConn.createStatement();
                            ResultSet rs = statement.executeQuery("SELECT * FROM ClientInfo WHERE `uid`='" + uid + "';");
                            if (rs.next())
                                throw new IllegalStateException("username is already exist");


                            int affected = statement.executeUpdate("INSERT INTO ClientInfo (uid,identity,status) values('" + uid + "','" + pswMD5 + "','OFFLINE');");
                            if (affected == 0)
                                throw new IllegalStateException("Cannot add user.");
                            rs = statement.executeQuery("SELECT * FROM ClientInfo WHERE `uid`='" + uid + "' AND `identity`='" + pswMD5 + "';");
                            if (!rs.next())
                                throw new IllegalStateException("Cannot read user.");

                            Response response = new Response.Builder(Xcr3TProtocol.RESPONSE_200_OK)
                                    .setDestinationPublicKey(clientJSON.getString("publicKey"))
                                    .put("status", "OK")
                                    .put("id", rs.getString("id"))
                                    .put("uid", rs.getString("uid"))
                                    .build();

                            doneFlag = sendResponse(socket,response);


                        } catch (JSONException e) {
                            throw new IllegalStateException("JSON Error");
                        } catch (SQLException e) {
                            e.printStackTrace();
                            throw new IllegalStateException("SQL Server Unavailable");
                        }
                    }

                    if (!doneFlag && requestParser.isProtocolHeader(Xcr3TProtocol.REQUEST_HANDSHAKE)) {
                        try {

                            System.out.println(clientJSON.toString());
                            String uid = CryptorUtil.unpack(Xcr3TServer.SERVER_PRIVATE_KEY, clientJSON.getString("uid"));
                            String psw = CryptorUtil.unpack(Xcr3TServer.SERVER_PRIVATE_KEY, clientJSON.getString("identity"));
                            String port = CryptorUtil.unpack(Xcr3TServer.SERVER_PRIVATE_KEY,clientJSON.getString("port"));

                            //TODO: 查找数据库匹配身份并返回TOKEN
                            Statement statement = Xcr3TServer.dbConn.createStatement();
                            String clientQueryStr = "WHERE `uid` = '" + uid + "'";

                            ResultSet rs = statement.executeQuery("SELECT * FROM ClientInfo " + clientQueryStr + ";");
                            if (!rs.next())
                                throw new IllegalStateException("Unknown identity");

                            if(rs.getString("status").equals("ONLINE"))
                                throw new IllegalStateException("You have logged in. If it isn't you, please contact the server.");

                            String md5DB = rs.getString("identity");

                            if (!CryptorUtil.equalsSaltedMD5(psw, md5DB))
                                throw new IllegalStateException("Unknown identity");


                            statement.execute("UPDATE ClientInfo SET `status`='ONLINE', `token`= '" + getUUID() + "' " +
                                    ", `location`= '" + socket.getInetAddress() + "', `port`= '"+port+"' " + clientQueryStr + ";");

                            Response response = new Response.Builder(Xcr3TProtocol.RESPONSE_200_OK)
                                    .setDestinationPublicKey(clientJSON.getString("publicKey"))
                                    .put("status", "OK")
                                    .put("token", getUUID().toString())
                                    .build();

                            doneFlag = sendResponse(socket,response);


                        } catch (JSONException e) {
                            throw new IllegalStateException("JSON Error");
                        } catch (SQLException e) {
                            e.printStackTrace();
                            throw new IllegalStateException("SQL Error");
                        }
                    }
                    if (!doneFlag && requestParser.isProtocolHeader(Xcr3TProtocol.REQUEST_FIND)) {
                        try {

                            System.out.println(clientJSON.toString());
                            String destUID = CryptorUtil.unpack(Xcr3TServer.SERVER_PRIVATE_KEY, clientJSON.getString("destUID"));
                            String token = CryptorUtil.unpack(Xcr3TServer.SERVER_PRIVATE_KEY, clientJSON.getString("token"));

                            //TODO: 服务器比较token
                            Statement statement = Xcr3TServer.dbConn.createStatement();
                            ResultSet rs = statement.executeQuery("SELECT `uid` FROM ClientInfo WHERE `token`='" + token + "';");
                            if (!rs.next())
                                throw new IllegalStateException("Bad token");
                            if(rs.getString("uid").equals(destUID))
                                throw new IllegalStateException("Please don't find yourself :(");

                            rs = statement.executeQuery("SELECT * FROM ClientInfo WHERE `uid`='" + destUID + "';");
                            if (!rs.next())
                                throw new IllegalStateException("Invalid user");

                            //TODO:服务器连接对方并获取ready值
                            boolean isReady;
                            String status = rs.getString("status");
                            if(isReady=status.equals("ONLINE")) {
                                Response response = new Response.Builder(Xcr3TProtocol.RESPONSE_200_OK)
                                        .setDestinationPublicKey(clientJSON.getString("publicKey"))
                                        .put("status", "OK")
                                        .put("valid", "true")
                                        .put("ready", String.valueOf(isReady))
                                        .put("ip", rs.getString("location").substring(1))
                                        .put("port", rs.getString("port"))
                                        .build();
                                doneFlag = sendResponse(socket,response);
                            }else{
                                if(status.equals("OFFLINE"))
                                    throw new IllegalStateException(destUID+" is OFFLINE.");
                                throw new IllegalStateException(destUID+"is UNKNOWN.");
                            }



                        } catch (JSONException e) {
                            throw new IllegalStateException("JSON Error");
                        } catch (SQLException e) {
                            e.printStackTrace();
                            throw new IllegalStateException("SQL Error");
                        }
                    }
                    if (!doneFlag && requestParser.isProtocolHeader(Xcr3TProtocol.REQUEST_GOODBYE)) {
                        try {
                            System.out.println(clientJSON.toString());
                            String uid = CryptorUtil.unpack(Xcr3TServer.SERVER_PRIVATE_KEY, clientJSON.getString("uid"));
                            String token = CryptorUtil.unpack(Xcr3TServer.SERVER_PRIVATE_KEY, clientJSON.getString("token"));
                            String clientQueryStr = "WHERE `uid` = '" + uid + "' AND `token`='" + token + "'";
                            //TODO: 更改服务器上的用户状态
                            Statement statement = Xcr3TServer.dbConn.createStatement();
                            statement.execute("UPDATE ClientInfo SET `location`= null, `port`= null, `status`='OFFLINE', `token`= '' " + clientQueryStr + ";");

                            Response response = new Response.Builder(Xcr3TProtocol.RESPONSE_200_OK)
                                    .setDestinationPublicKey(clientJSON.getString("publicKey"))
                                    .put("status", "OK")
                                    .put("goodbye","true")
                                    .build();

                            doneFlag = sendResponse(socket,response);

                        } catch (JSONException e) {
                            throw new IllegalStateException("JSON Error");
                        } catch (SQLException e) {
                            e.printStackTrace();
                            throw new IllegalStateException("SQL Error");
                        }
                    }
                    if (!doneFlag) {
                        throw new IllegalStateException("Unsupported Function");
                    }
                    bufferedReader.close();
                    closeSocket(socket);

                } catch (IllegalStateException e) {
                    String err = "Received a bad/empty request: " + e.getMessage();
                    Response response = new Response.Builder(Xcr3TProtocol.RESPONSE_400_BAD_REQUEST)
                            .put("status", "error")
                            .put("error", e.getMessage())
                            .build();

                    sendResponse(socket,response);
                    System.out.println(err);
                    closeSocket(socket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean sendResponse(Socket socket,Response response)throws IOException{
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        bufferedWriter.write(response.toString());
        bufferedWriter.flush();
        bufferedWriter.close();
        return true;
    }

    private void closeSocket(Socket socket) throws IOException {
        socket.close();
        clientSocketList.remove(mUUID);
        //System.out.println("DEL:" + getUUID() + "/ " + getClientServingCount() + " left");
    }
}
