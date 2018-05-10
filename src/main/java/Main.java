import javax.swing.JDialog;
import javax.swing.JOptionPane;

public class Main {
    public static void main(String[] args){
        PropertiesGUI dialog = new PropertiesGUI();
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }

    public static void setupAndStartNetwork(NetworkConfiguration configuration) {
        configuration.start();
    }
}
