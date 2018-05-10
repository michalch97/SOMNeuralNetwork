import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class PropertiesGUI extends JDialog {
    private final JPanel contentPanel = new JPanel();
    private final JPanel buttonPanel = new JPanel();
    private final JPanel pointsPanel = new JPanel();
    private JComboBox quantizationBox;
    private JComboBox typeBox;
    private JComboBox figureBox;
    private JTextField epochLimit;
    private JTextField numberOfGroups;
    private JTextField pathField;
    private JTextField startRadius;
    private JTextField startLearningRate;
    private final JFileChooser fileChooser = new JFileChooser();

    public PropertiesGUI() {
        super((JDialog) null, "Konfiguracja sieci", false);

        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 500, 350);
        setMinimumSize(new Dimension(600, 400));
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(new GridLayout(14,0));

        JLabel quantizationLabel = new JLabel("Kwantyzacja: ");
        quantizationLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
        contentPanel.add(quantizationLabel);

        List<String> functionTypeList = new ArrayList<String>();
        for (QuantizationTypeManager.QuantizationType type : QuantizationTypeManager.QuantizationType.values()) {
            functionTypeList.add(QuantizationTypeManager.toString(type));
        }
        quantizationBox = new JComboBox(functionTypeList.toArray());
        contentPanel.add(quantizationBox);

        JLabel epochLabel = new JLabel("Liczba epok:");
        epochLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
        contentPanel.add(epochLabel);

        epochLimit = new JTextField();
        epochLimit.setText("100");
        epochLimit.setFont(new Font("Tahoma", Font.PLAIN, 14));
        contentPanel.add(epochLimit);

        JLabel groupLabel = new JLabel("Ilosc grup:");
        groupLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
        contentPanel.add(groupLabel);

        numberOfGroups = new JTextField();
        numberOfGroups.setText("5");
        numberOfGroups.setFont(new Font("Tahoma", Font.PLAIN, 14));
        contentPanel.add(numberOfGroups);

        JLabel startRadiusLabel = new JLabel("Startowy promień:");
        startRadiusLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
        contentPanel.add(startRadiusLabel);

        startRadius = new JTextField();
        startRadius.setText("2.3");
        startRadius.setFont(new Font("Tahoma", Font.PLAIN, 14));
        contentPanel.add(startRadius);

        JLabel startLearningRateLabel = new JLabel("Startowy współczynnik nauki:");
        startLearningRateLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
        contentPanel.add(startLearningRateLabel);

        startLearningRate = new JTextField();
        startLearningRate.setText("0.2");
        startLearningRate.setFont(new Font("Tahoma", Font.PLAIN, 14));
        contentPanel.add(startLearningRate);

        JLabel fileOrRandomLabel = new JLabel("Typ punktów: ");
        fileOrRandomLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
        contentPanel.add(fileOrRandomLabel);

        List<String> pointsType = new ArrayList<String>();
        pointsType.add("Wybierz typ...");
        pointsType.add("Wczytaj z pliku");
        pointsType.add("Losuj punkty pokrywające figurę geometryczną");
        typeBox = new JComboBox(pointsType.toArray());
        contentPanel.add(typeBox);

        pointsPanel.setLayout(new GridLayout(0,3));
        {
            typeBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    pointsPanel.removeAll();
                    if(typeBox.getSelectedIndex() == 1){
                        JLabel imagePathLabel = new JLabel("Ścieżka do pliku:");
                        imagePathLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
                        pointsPanel.add(imagePathLabel);

                        pathField = new JTextField();
                        pointsPanel.add(pathField);

                        JButton pathButton = new JButton("Wybierz plik");
                        pathButton.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                onFilePathButtonClicked(e);
                            }
                        });
                        pointsPanel.add(pathButton);
                    } else if (typeBox.getSelectedIndex() == 2){
                        JLabel figureLabel = new JLabel("Wybierz figurę:");
                        figureLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
                        pointsPanel.add(figureLabel);

                        List<String> figureType = new ArrayList<String>();
                        for (ShapeGenerationType type : ShapeGenerationType.values()) {
                            figureType.add(ShapeGenerationType.toString(type));
                        }
                        figureBox = new JComboBox(figureType.toArray());
                        pointsPanel.add(figureBox);
                    } else {
                        pointsPanel.removeAll();
                    }
                    revalidate();
                    repaint();
                }
            });
        }
        contentPanel.add(pointsPanel);

        buttonPanel.setLayout(new GridLayout(0,2));
        {
            JButton learnButton = new JButton("Ucz sieć");
            learnButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    onLearnButtonClicked(e);
                }
            });
            buttonPanel.add(learnButton);

            JButton cancelButton = new JButton("Anuluj");
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    onCancelButtonClicked(e);
                }
            });
            buttonPanel.add(cancelButton);
        }
        contentPanel.add(buttonPanel);

    }

    private void onCancelButtonClicked(ActionEvent e) {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void onLearnButtonClicked(ActionEvent e) {
        NetworkConfiguration configuration = readConfiguration();
        if (configuration == null) {
            return;
        }

        Main.setupAndStartNetwork(configuration);
    }

    private NetworkConfiguration readConfiguration() {

        String filePath = "";
        if(typeBox.getSelectedIndex() == 1)
            filePath = pathField.getText();
        int groups = Integer.parseInt(numberOfGroups.getText());
        int epoch = Integer.parseInt(epochLimit.getText());
        double startR = Double.parseDouble(startRadius.getText());
        double startLR = Double.parseDouble(startLearningRate.getText());
        QuantizationTypeManager.QuantizationType quantizationType = QuantizationTypeManager.QuantizationType.values()[quantizationBox.getSelectedIndex()];
        ShapeGenerationType shapeType = null;
        if(figureBox != null)
            shapeType = ShapeGenerationType.values()[figureBox.getSelectedIndex()];

        NetworkConfiguration configuration = new NetworkConfiguration(filePath,groups,epoch,startR,startLR,quantizationType,shapeType);
        return configuration;
    }

    private void onFilePathButtonClicked(ActionEvent e) {
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(fileChooser.getSelectedFile().getPath());
        }
    }
}
