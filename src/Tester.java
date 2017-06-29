import clientKit.Chattable;
import clientKit.Xcr3TAdapter;
import clientKit.Xcr3TClient;

import java.io.IOException;

/**
 * Created by wjw_w on 2017/6/22.
 */
public class Tester implements Runnable {
    @Override
    public void run() {
        /*try {
            Thread.sleep(1000);
            Xcr3TClient client1 = new Xcr3TClient("kavel", "2014180065");
            client1.login();

            Xcr3TClient client2 = new Xcr3TClient("Test", "1231");
            client2.login();
            client2.find("kavel");

            System.out.print("Waiting");
            while(true){
                System.out.print(".");
                if (client2.getChatHandler()!=null)
                    break;
            }
            System.out.print("Done!\r\n");
            client2.getChatHandler().sendChat("hello kavel!");
            client1.getChatHandler().sendChat("hola test!");
            client2.getChatHandler().sendChat("how are you!");
            client1.getChatHandler().sendChat("fine!");



        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        /*while (true){
            try {
                client.login();
                Thread.sleep(1000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/
    }
}
