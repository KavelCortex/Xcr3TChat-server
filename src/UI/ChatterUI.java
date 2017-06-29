package UI;

import clientKit.ChatHandler;
import clientKit.Chattable;
import clientKit.Xcr3TAdapter;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Created by wjw_w on 2017/6/26.
 */
public class ChatterUI implements Chattable {
    private JTextField cmdField;
    private JPanel panel;
    private JButton SENDButton;
    private JTextPane chatPane;
    private JList lsIdentity;
    private JList lsContact;
    private JScrollPane scrollPane;
    private DefaultListModel<String> mListModelIdentity;
    private DefaultListModel<String> mListModelContact;


    private Xcr3TAdapter adapter;


    public ChatterUI() {
        adapter = new Xcr3TAdapter(this);

        mListModelIdentity = new DefaultListModel<>();
        mListModelContact = new DefaultListModel<>();
        lsIdentity.setModel(mListModelIdentity);
        lsContact.setModel(mListModelContact);

        SENDButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String command = cmdField.getText();
                cmdField.setText("");
                adapter.cmd(command.split(" "));
            }
        });
        lsIdentity.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting())
                    adapter.setCurrentClient((String) lsIdentity.getSelectedValue());
            }
        });
        lsContact.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    adapter.setCurrentHandler((String) lsContact.getSelectedValue());
                }
            }
        });
        cmdField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    SENDButton.doClick();
            }
        });

    }

    public static void main(String[] args) throws IllegalStateException {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
        JFrame frame = new JFrame("ChatterUI");
        frame.setContentPane(new ChatterUI().panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    @Override
    public void incoming(ChatHandler handler) {

        adapter.addContact(handler);
    }

    @Override
    public void disconnecting(ChatHandler handler) {

        adapter.rmContact(handler);
    }

    @Override
    public void showChat(String chat) {
        Document doc = chatPane.getDocument();
        try {
            doc.insertString(doc.getLength(), chat + "\r\n", new SimpleAttributeSet());
            chatPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void printLog(String message) {
        Document doc = chatPane.getDocument();
        try {
            doc.insertString(doc.getLength(), "System:" + message + "\r\n", new SimpleAttributeSet());
            chatPane.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateUI() {

        mListModelIdentity.clear();
        mListModelContact.clear();

        for (String identity : adapter.getIdentityMap().keySet()) {
            mListModelIdentity.addElement(identity);
            for (ChatHandler handler : adapter.getChatHandlerMap(identity).values()) {
                mListModelContact.addElement(handler.getLink());
            }
        }

        if (adapter.getCurrentIdentity() != null)
            lsIdentity.setSelectedValue(adapter.getCurrentIdentity().getUsername(), true);

        if (adapter.getCurrentChatHandler() != null)
            lsContact.setSelectedValue(adapter.getCurrentChatHandler().getLink(), true);

    }


}
