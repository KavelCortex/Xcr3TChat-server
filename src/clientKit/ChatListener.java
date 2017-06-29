package clientKit;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by wjw_w on 2017/6/25.
 */
public class ChatListener implements Runnable {

    private ServerSocket mServerSocket;
    private Chattable mChatter;
    private String mSelfUsername;

    public ChatListener(ServerSocket s, Chattable chatter,String selfUsername) {
        mServerSocket = s;
        mChatter = chatter;
        mSelfUsername =selfUsername;
    }

    @Override
    public void run() {
        while (!mServerSocket.isClosed()) {
            try {
                Socket socket = mServerSocket.accept();
                new ChatHandler(socket, mChatter, mSelfUsername);
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
    }
}
