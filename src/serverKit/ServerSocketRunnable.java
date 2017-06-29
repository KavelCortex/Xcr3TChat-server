package serverKit;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by wjw_w on 2017/4/7.
 */
public class ServerSocketRunnable implements Runnable {
    private final UUID uuid;
    private static ServerSocket mServerSocket;
    private boolean isWorking;
    private int mPort;
    private SocketThreadLimiter mSocketThreadLimiter;
    private static List<Socket> mSocketList = new CopyOnWriteArrayList<>();
    public static final int mSocketPerThread = 10;
    public static int mThreadPerRunningMaximum = 2000;
    public static int mThreadPerRunningStep = 50;
    private Object mSocketListLock = new Object();

    public ServerSocketRunnable(int port) {
        uuid = UUID.randomUUID();
        mPort = port;
        isWorking = true;
        mSocketThreadLimiter = new SocketThreadLimiter();
    }

    public UUID getUUID() {
        return uuid;
    }

    public void stop() {
        isWorking = false;
    }

    public static ServerSocket getServerSocket() {
        return mServerSocket;
    }

    public static int getSocketAwaiting() {
        return mSocketList.size();
    }

    @Override
    public void run() {
        new Thread(new AddSocketThread()).start();
        while (isWorking) {
            synchronized (mSocketListLock) {
                while (mSocketList.isEmpty()) {
                    try {
                        mSocketListLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                List<Socket> socketWaitingList = new ArrayList<>();
                for (Socket s : mSocketList) {
                    socketWaitingList.add(s);
                    if (socketWaitingList.size() == mSocketPerThread)
                        break;
                }
                if (!socketWaitingList.isEmpty()) {
                    mSocketThreadLimiter.addNewHandlerToWaitList(new SocketHandler(socketWaitingList));
                    mSocketList.removeIf(socketWaitingList::contains);
                }
            }
        }
    }

    class AddSocketThread implements Runnable {
        @Override
        public void run() {
            try {
                mServerSocket = new ServerSocket(mPort, 1000000);
                System.out.println("Running server at port " + mPort + "...");
                while (true) {
                    Socket socket = mServerSocket.accept();
                    synchronized (mSocketListLock) {
                        mSocketList.add(socket);
                        mSocketListLock.notify();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
