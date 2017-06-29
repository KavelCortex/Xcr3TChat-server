package clientKit;

/**
 * Created by wjw_w on 2017/6/26.
 */
public interface Chattable {
    void incoming(ChatHandler handler);
    void disconnecting(ChatHandler handler);
    void showChat(String chat);
    void printLog(String message);
    void updateUI();
}
