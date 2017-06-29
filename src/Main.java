import Util.CryptorUtil;
import serverKit.ServerSocketRunnable;
import serverKit.Xcr3TServer;

/**
 * Created by wjw_w on 2017/6/22.
 */
public class Main {
    public static void main(String[] args){
        Xcr3TServer server = new Xcr3TServer(54213);
        //new Thread(new Tester()).start();
        /*String md5 = CryptorUtil.getRandomSaltedMD5("123");
        System.out.println(md5);
        boolean check = CryptorUtil.equalsSaltedMD5("123",md5);
        System.out.println(check);*/
        /*Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:mariadb://kavel.cn:3306/xcr3tdb?user=xcr3tserver&password=2014180065");
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM ClientInfo;");
            while(rs.next()){
                System.out.println(rs.getString("uid"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }*/

        /*try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            KeyPair keyPair = generator.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey)keyPair.getPrivate();
            System.out.println("Public Key:\n"+CryptorUtil.decryptBASE64(publicKey.getEncoded()));
            System.out.println("Private Key:\n"+CryptorUtil.decryptBASE64(privateKey.getEncoded()));
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
        String t = "123";
        KeyPairGenerator generator = null;
        try {
            generator = KeyPairGenerator.getInstance("RSA");
            KeyPair keyPair = generator.generateKeyPair();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey)keyPair.getPrivate();
            String base64puk = CryptorUtil.decryptBASE64(publicKey.getEncoded());
            byte[] rawe=CryptorUtil.encryptData(base64puk,t.getBytes());
            String b64=CryptorUtil.decryptBASE64(rawe);
            System.out.println(b64);
        } catch (Exception e) {
            e.printStackTrace();
        }*/


    }
}
