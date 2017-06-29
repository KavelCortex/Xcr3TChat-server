package serverKit;

import java.security.interfaces.RSAPrivateKey;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by wjw_w on 2017/6/24.
 */
public class Xcr3TServer{
    private int mPort;
    protected static Connection dbConn;
    protected final static String databaseURL="jdbc:mariadb://kavel.cn:3306/xcr3tdb?user=xcr3tserver&password=2014180065";
    protected final static String SERVER_PRIVATE_KEY =
            "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAJQLwdmayTZ90cFhNq5y6qRI3YR5\n" +
            "4pSZyeqs8yD1FfGvdBpjzHCx4/rbl4xLvSt+BrP/QuYAd6ebqu8qRaUYTUzd2vpHA5NeU0BsRbRz\n" +
            "V+l8ypj113o83DmOfsnGMitVqSxw754NNGxGrU5f0sdb6qSzCO3ZGRIij19+9Mv3qfdJAgMBAAEC\n" +
            "gYAfC4QgDKxrJ+FHiwo7dM+tmbYSJLkV7lYARzpIy/xJDUDsk8b4TuV+4nOaMPu/VhMzxbCSqMBu\n" +
            "vl8O/i9SmpEC3pOHoa2fYX1OZwUWa89VuiumDMftwIFjnRIzhQf++7GKMcVzRVSSuHlcIG7AcG34\n" +
            "u5Gg8XtrI/vHOpornERZMQJBANIyT6h6lZzKiVy7DDuYowEU1A7LG3Ers7vt4W19VAdjGr+xO792\n" +
            "Svv+2DdaiDWPM1P4dG6d+Wmr6JLqw9kS/QUCQQC0Tmlc2WfgLrCI8pdclB+nXcSYs2UHILcHOtr8\n" +
            "c94SySw2XRTkJTbvqkvgjPHEYvvp8gu3Ls3/yXTUIeiOW0R1AkAhgoPQiDpx1JgxgGBi3+KcuYVV\n" +
            "Fmw5jo4I19OocOKEivgot0ifLWym3+n4aSZt43Z7XJCzUdwBTLa3NVYjtTNBAkA3M/6cN8/O2lyg\n" +
            "QS3IYW1jj5jea6ZVzVVcOE/NlSf7tm374vm/dAlizU/X2y82QlwAX2Po3MKjOqmzPQJ3e0f1AkEA\n" +
            "rQNV91xRhLOKQ3uSfzrAjSv3+mg5vQ3B4VG+hoNmoVmh9V/PJcP2pAv3Zx6yCZCjhv4RuWQSFDro\n" +
            "PEvmPCQgPQ==";

    public Xcr3TServer(int port){
        System.out.println("Initializing Xcr3TServer...");
        mPort=port;
        try {
            dbConn = DriverManager.getConnection(Xcr3TServer.databaseURL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        new Thread(new ServerSocketRunnable(mPort)).start();
    }
}
