package clientKit;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by wjw_w on 2017/6/29.
 */
public class Xcr3TAdapter {

    Chattable mChatter;
    private Xcr3TClient mCurrentIdentity;
    private ChatHandler mCurrentChatHandler;
    private Map<String, Xcr3TClient> mIdentityMap = new HashMap<>();
    private Map<String, Map<String, ChatHandler>> mChatHandlerMap = new HashMap<>();
    private Options mCliOptions;
    private HelpFormatter mHelpFormatter;

    public Xcr3TAdapter(Chattable chatter) {

        mChatter = chatter;
        initCli();

    }

    public Xcr3TClient getCurrentIdentity() {
        return mCurrentIdentity;
    }

    public ChatHandler getCurrentChatHandler() {
        return mCurrentChatHandler;
    }

    public Map<String, Xcr3TClient> getIdentityMap() {
        return mIdentityMap;
    }

    public Map<String, ChatHandler> getChatHandlerMap(String identity) {
        if (!mChatHandlerMap.containsKey(identity))
            mChatHandlerMap.put(identity, new HashMap<>());
        return mChatHandlerMap.get(identity);
    }

    public void login(String uid, String psw) {
        if (mIdentityMap.containsKey(uid))
            throw new IllegalStateException("already logged in.");
        addIdentity(uid, psw);
    }

    public void logout() {
        if (mCurrentIdentity == null)
            throw new IllegalStateException("please select an identity to log out.");
        logout(mCurrentIdentity.getUsername());
    }

    public void logout(String identity) throws IllegalStateException {
        if (!mIdentityMap.containsKey(identity))
            throw new IllegalStateException("Invalid identity");
        Xcr3TClient client = mIdentityMap.get(identity);
        try {
            rmIdentity(client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void register(String uid, String psw1, String psw2) {
        if (!psw1.equals(psw2))
            throw new IllegalStateException("password must be the same.");
        Xcr3TClient client = new Xcr3TClient(uid, psw2,mChatter);
        try {
            if (client.register())
                addIdentity(client);
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't connect to server.");
        }
    }

    public void switchIdentity(String identity) {
        if (mIdentityMap.containsKey(identity)) {
            setCurrentClient(identity);
        } else
            throw new IllegalStateException("Invalid Identity.");
    }

    public void connect(String uid) {
        try {
            mCurrentIdentity.find(uid);
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't connect to server.");
        }
    }

    public void disconnect() {
        if (mCurrentChatHandler == null)
            throw new IllegalStateException("please select an contact to disconnect.");
        disconnect(mCurrentChatHandler.getLink());
    }

    public void disconnect(String link) {
        if (!link.matches("[\\s\\S]->[\\s\\S]"))
            throw new IllegalStateException("Invalid link");
        String identity = link.split("->")[0].trim();
        String contact = link.split("->")[1].trim();

        if (!getIdentityMap().containsKey(identity)
                || !getChatHandlerMap(identity).containsKey(contact))
            throw new IllegalStateException("Invalid link");
        ChatHandler handler = getChatHandlerMap(identity).get(contact);
        try {
            handler.disconnect();
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't connect to server.");
        }

    }

    public void forward(String contact) {
        String identity = mCurrentIdentity.getUsername();
        String link = identity + " -> " + contact;
        if (mChatHandlerMap.containsKey(identity) && mChatHandlerMap.get(identity).containsKey(contact)) {
            mCurrentChatHandler = mChatHandlerMap.get(identity).get(contact);
            setCurrentHandler(link);
        } else
            throw new IllegalStateException("Invalid forward");
    }

    public void cmd(String[] args) {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine line = parser.parse(mCliOptions, args);

            if (line.hasOption("h")) {
                StringWriter sw = new StringWriter();
                mHelpFormatter.printHelp(new PrintWriter(sw), 100, "[command] [message]", "", mCliOptions, 0, 0, "");
                mChatter.printLog(sw.toString());
            }
            if (line.hasOption("l")) {
                String uid = line.getOptionValues("l")[0];
                String psw = line.getOptionValues("l")[1];
                login(uid, psw);
            }
            if (line.hasOption("o")) {
                String identity = line.getOptionValue("o");
                if (identity == null)
                    logout();
                else
                    logout(identity);
            }
            if (line.hasOption("r")) {
                String uid = line.getOptionValues("r")[0];
                String psw1 = line.getOptionValues("r")[1];
                String psw2 = line.getOptionValues("r")[2];
                register(uid, psw1, psw2);
            }
            if (line.hasOption("s")) {
                String identity = line.getOptionValue("s");
                switchIdentity(identity);

            }
            if (line.hasOption("c")) {
                String uid = line.getOptionValue("c");
                connect(uid);
            }
            if (line.hasOption("d")) {
                String link = line.getOptionValue("d");
                if (link == null)
                    disconnect();
                else
                    disconnect(link);
            }
            if (line.hasOption("f")) {
                String contact = line.getOptionValue("f");
                forward(contact);
            }

            String chatText = String.join(" ", line.getArgs());

            if (chatText.isEmpty()) {
                mChatter.updateUI();
                return;
            }

            if (mCurrentChatHandler == null)
                throw new IllegalStateException("Oops! we don't know who to be sent to! Please select a contact!");

            if (mCurrentIdentity == null)
                throw new IllegalStateException("Hi stranger! Please log in or select your identity!");

            mCurrentChatHandler.sendChat(chatText);
            mChatter.showChat(mCurrentChatHandler.getSelfUsername() + " -> " + mCurrentChatHandler.getOppositeUsername() + ": " + chatText);
            mChatter.updateUI();


        } catch (ParseException e) {
            StringWriter sw = new StringWriter();
            mHelpFormatter.printHelp(new PrintWriter(sw), 100, "[command] [message]", "", mCliOptions, 0, 0, "");
            mChatter.printLog(sw.toString());
        } catch (IllegalStateException e) {
            mChatter.printLog(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initCli() {
        mHelpFormatter = new HelpFormatter();
        mCliOptions = new Options();
        mCliOptions.addOption("h", "help", false, "print help for the command.");
        mCliOptions.addOption(
                Option.builder("l").longOpt("login")
                        .numberOfArgs(2)
                        .valueSeparator(' ')
                        .argName("uid password")
                        .desc("login using uid and password")
                        .build());
        mCliOptions.addOption(
                Option.builder("o").longOpt("logout")
                        .hasArg()
                        .optionalArg(true)
                        .argName("identity")
                        .desc("logout current identity, or specified ideneity")
                        .build());
        mCliOptions.addOption(
                Option.builder("r").longOpt("register")
                        .numberOfArgs(3)
                        .argName("uid password re-enter-password")
                        .desc("register using uid and password")
                        .build());
        mCliOptions.addOption(
                Option.builder("s").longOpt("switch")
                        .numberOfArgs(1)
                        .argName("identity")
                        .desc("switch to available identity")
                        .build());
        mCliOptions.addOption(
                Option.builder("c").longOpt("connect")
                        .numberOfArgs(1)
                        .argName("uid")
                        .desc("connect to a contact")
                        .build());
        mCliOptions.addOption(
                Option.builder("d").longOpt("disconnect")
                        .hasArg()
                        .optionalArg(true)
                        .argName("link")
                        .desc("disconnect current link, or specified link")
                        .build());
        mCliOptions.addOption(
                Option.builder("f").longOpt("forward")
                        .numberOfArgs(1)
                        .argName("uid")
                        .desc("send chat to specified contact")
                        .build());
    }

    private void addIdentity(String uid, String psw) {
        Xcr3TClient client = new Xcr3TClient(uid, psw,mChatter);
        addIdentity(client);
    }

    public void addIdentity(Xcr3TClient client) {
        try {
            if (client.login()) {
                mIdentityMap.put(client.getUsername(), client);
                mCurrentIdentity = client;
                mChatter.updateUI();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot log Identity: " + client.getUsername());
        }
    }

    public void rmIdentity(Xcr3TClient client) throws IOException {
        mCurrentIdentity = client;
        String identity = mCurrentIdentity.getUsername();
        Map<String, ChatHandler> handlerMap = mChatHandlerMap.get(identity);
        for (ChatHandler handler : handlerMap.values()) {
            handler.disconnect();
        }
        handlerMap.clear();
        mCurrentIdentity.logout();
        mIdentityMap.remove(identity);
        mCurrentIdentity = null;
        mChatter.updateUI();
    }

    public void addContact(ChatHandler handler) {
        String identity = handler.getSelfUsername();
        String contact = handler.getOppositeUsername();
        getChatHandlerMap(identity).put(contact, handler);
        mCurrentChatHandler = handler;
        mChatter.printLog("contact connected:" + handler.getLink());
        mChatter.updateUI();
    }

    public void rmContact(ChatHandler handler) {
        mCurrentChatHandler = handler;
        String identity = mCurrentChatHandler.getSelfUsername();
        String contact = mCurrentChatHandler.getOppositeUsername();
        getChatHandlerMap(identity).remove(contact);
        mCurrentChatHandler = null;
        mChatter.updateUI();
    }

    public void setCurrentClient(String identity) {
        if (identity != null) {
            if (mCurrentIdentity == null || (!identity.equals(mCurrentIdentity.getUsername()) && getIdentityMap().containsKey(identity))) {
                mChatter.printLog("switching to Identity: " + identity);
                mCurrentIdentity = mIdentityMap.get(identity);
                mChatter.updateUI();
            }
        }
    }

    public void setCurrentHandler(String link) {
        if (link != null) {
            String identity = link.split(" -> ")[0];
            String contact = link.split(" -> ")[1];
            if (!link.equals(mCurrentChatHandler.getLink())
                    && getIdentityMap().containsKey(identity)
                    && getChatHandlerMap(identity).containsKey(contact)) {
                mChatter.printLog("switching to connection: " + link);
                mCurrentChatHandler = mChatHandlerMap.get(identity).get(contact);
                mChatter.updateUI();
            }
        }
    }

}
