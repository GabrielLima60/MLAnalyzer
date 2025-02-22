import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.nio.file.*;

import java.util.Arrays;
import java.util.List;
import javax.swing.text.NumberFormatter;
import java.text.NumberFormat;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

public class MLAnalyzer extends JFrame implements ActionListener {
    private JFileChooser fileChooser;
    private JPanel currentPage;
    private JPanel csvSelectionPage;
    private JPanel analysisConfigPage;
    private JPanel loadingPage;
    private JButton selectCSVButton;
    private JButton analyzeButton;
    private List<JCheckBox> dataCleaningCheckBoxes;
    private List<JCheckBox> modelCheckBoxes;
    private List<JCheckBox> techniqueCheckBoxes;
    private List<JCheckBox> parameterCheckBoxes;
    private JLabel dataCleaningLabel;
    private JLabel techniquesLabel;
    private JLabel modelsLabel;
    private JLabel topLoadingLabel;
    private JLabel bottomLoadingLabel;
    private StringBuilder pythonOutput = new StringBuilder();
    private StringBuilder errorLog = new StringBuilder();
    private static final String LOCK_FILE_PATH = "program.lock";
    private static RandomAccessFile lockFile;
    private static FileLock lock;

    private List<String> dataCleaning = Arrays.asList("Normalize", "Mean Imputation for Missing Values", "Remove Duplicate Data", "Collinearity Removal");
    private List<String> techniques = Arrays.asList("No Technique", "PCA", "IncPCA", "ICA", "LDA");
    private List<String> models = Arrays.asList("Naive Bayes", "SVM", "MLP", "DecisionTree", "RandomForest", "KNN", "LogReg", "GradientBoost", "XGBoost", "Custom AI Model");
    private String selectedOptimization;
    private String selectedCrossValidation;
    private JFormattedTextField iterationsField;
    private int numberOfIterations;
    private List<String> parameters = Arrays.asList("F1-Score", "Processing Time", "ROC AUC", "Memory Usage", "Precision", "Accuracy","Recall");

    public MLAnalyzer() {
        super("MLAnalyzer");

        // Set look and feel to system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize file chooser
        fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".csv") || f.isDirectory();
            }

            public String getDescription() {
                return "CSV files (*.csv)";
            }
        });

        // Create CSV selection page
        createCSVSelectionPage();

        // Create analysis configuration page
        createAnalysisConfigPage();

        //Create loading page
        createLoadingPage();

        // Set initial page to CSV selection page
        currentPage = csvSelectionPage;

        // Set up main frame
        ImageIcon icon = new ImageIcon("resources\\icon.png");
        setIconImage(icon.getImage());
        setContentPane(currentPage);
        setPreferredSize(new Dimension(1280, 720));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClosing();
            }
        });
    }

    private void handleWindowClosing() {
        releaseLock();

        Path filePath = Paths.get("resources\\cleaned_data.csv");
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error deleting cleaned data file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        // Dispose of the frame and exit
        dispose();
        System.exit(0);
    }
    
    private void createCSVSelectionPage() {
        csvSelectionPage = new JPanel();
        csvSelectionPage.setLayout(new BorderLayout());
        ImageIcon backgroundImage = new ImageIcon("resources/background.gif");
        JLabel backgroundLabel = new JLabel(backgroundImage);
        backgroundLabel.setLayout(new BorderLayout());

        selectCSVButton = new JButton("Choose CSV File");
        selectCSVButton.addActionListener(this);
        selectCSVButton.setFont(new Font("Lucida Sans Unicode", Font.BOLD, 40));
        selectCSVButton.setPreferredSize(new Dimension(400, 100));
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(selectCSVButton);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(300, 0, 0, 0));

        backgroundLabel.add(buttonPanel, BorderLayout.CENTER);
        csvSelectionPage.add(backgroundLabel, BorderLayout.CENTER);
    }

    private void createAnalysisConfigPage() {
        analysisConfigPage = new JPanel(new BorderLayout());
        analysisConfigPage.setBackground(Color.WHITE);

        // Adding background image
        ImageIcon backgroundImage = new ImageIcon("resources/background.gif");
        JLabel backgroundLabel = new JLabel(backgroundImage);
        backgroundLabel.setLayout(new BorderLayout());

        JPanel configPanel = new JPanel(new GridLayout(0, 1));
        configPanel.setOpaque(false);
        configPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel cleaningPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dataCleaningLabel = new JLabel("Data Cleaning:");
        dataCleaningLabel.setFont(new Font("Lucida Sans Unicode", Font.BOLD, 26));
        dataCleaningLabel.setForeground(new Color(0, 0, 139));
        cleaningPanel.add(dataCleaningLabel);

        dataCleaningCheckBoxes = new ArrayList<>();
        for (String method : dataCleaning) {
            JCheckBox checkBox = new JCheckBox(method);
            checkBox.setFont(new Font("Lucida Sans Unicode", Font.PLAIN, 20));
            dataCleaningCheckBoxes.add(checkBox);
            cleaningPanel.add(checkBox);
        }
        configPanel.add(cleaningPanel);

        // Models selection panel
        JPanel modelsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        modelsLabel = new JLabel("Select Models:");
        modelsLabel.setFont(new Font("Lucida Sans Unicode", Font.BOLD, 26));
        modelsLabel.setForeground(new Color(0, 0, 139));
        modelsPanel.add(modelsLabel);
        modelCheckBoxes = new ArrayList<>();
        for (String model : models) {
            JCheckBox checkBox = new JCheckBox(model);
            checkBox.setFont(new Font("Lucida Sans Unicode", Font.PLAIN, 20));
            modelCheckBoxes.add(checkBox);
            modelsPanel.add(checkBox);
        }
        configPanel.add(modelsPanel);

        // Techniques selection panel
        JPanel techniquesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        techniquesLabel = new JLabel("Select Techniques:");
        techniquesLabel.setFont(new Font("Lucida Sans Unicode", Font.BOLD, 26));
        techniquesLabel.setForeground(new Color(0, 0, 139));
        techniquesPanel.add(techniquesLabel);
        techniqueCheckBoxes = new ArrayList<>();
        for (String technique : techniques) {
            JCheckBox checkBox = new JCheckBox(technique);
            checkBox.setFont(new Font("Lucida Sans Unicode", Font.PLAIN, 20));
            techniqueCheckBoxes.add(checkBox);
            techniquesPanel.add(checkBox);
        }
        configPanel.add(techniquesPanel);

        // Optimization selection panel
        JPanel optimizationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel optimizationLabel = new JLabel("Optimization:");
        optimizationLabel.setFont(new Font("Lucida Sans Unicode", Font.BOLD, 26));
        optimizationLabel.setForeground(new Color(0, 0, 139));
        optimizationPanel.add(optimizationLabel);
        JRadioButton gridSearchRadioButton = new JRadioButton("Grid Search");
        gridSearchRadioButton.setFont(new Font("Lucida Sans Unicode", Font.PLAIN, 20));
        JRadioButton randomSearchRadioButton = new JRadioButton("Random Search");
        randomSearchRadioButton.setFont(new Font("Lucida Sans Unicode", Font.PLAIN, 20));
        JRadioButton NoSearchRadioButton = new JRadioButton("None");
        NoSearchRadioButton.setFont(new Font("Lucida Sans Unicode", Font.PLAIN, 20));

        ButtonGroup optimizationGroup = new ButtonGroup();
        optimizationGroup.add(gridSearchRadioButton);
        optimizationGroup.add(randomSearchRadioButton);
        optimizationGroup.add(NoSearchRadioButton);


        optimizationPanel.add(gridSearchRadioButton);
        optimizationPanel.add(randomSearchRadioButton);
        optimizationPanel.add(NoSearchRadioButton);

        gridSearchRadioButton.addActionListener(e -> selectedOptimization = "Grid Search");
        randomSearchRadioButton.addActionListener(e -> selectedOptimization = "Random Search");
        NoSearchRadioButton.addActionListener(e -> selectedOptimization = "None");

        gridSearchRadioButton.setSelected(true);
        selectedOptimization = "None";
        
        configPanel.add(optimizationPanel);

        // Cross Validation selection panel
        JPanel crossValidationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel crossValidationLabel = new JLabel("Cross Validation:");
        crossValidationLabel.setFont(new Font("Lucida Sans Unicode", Font.BOLD, 26));
        crossValidationLabel.setForeground(new Color(0, 0, 139));
        crossValidationPanel.add(crossValidationLabel);
        JRadioButton kFoldRadioButton = new JRadioButton("K-Fold");
        kFoldRadioButton.setFont(new Font("Lucida Sans Unicode", Font.PLAIN, 20));
        JRadioButton holdOutRadioButton = new JRadioButton("Hold-Out");
        holdOutRadioButton.setFont(new Font("Lucida Sans Unicode", Font.PLAIN, 20));
        ButtonGroup crossValidationGroup = new ButtonGroup();
        crossValidationGroup.add(kFoldRadioButton);
        crossValidationGroup.add(holdOutRadioButton);
        crossValidationPanel.add(kFoldRadioButton);
        crossValidationPanel.add(holdOutRadioButton);

        kFoldRadioButton.addActionListener(e -> selectedCrossValidation = "K-Fold");
        holdOutRadioButton.addActionListener(e -> selectedCrossValidation = "Hold-Out");

        kFoldRadioButton.setSelected(true);
        selectedCrossValidation = "Hold-Out";

        configPanel.add(crossValidationPanel);

        // Number of iterections selection panel
        JPanel iterationsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel iterationsLabel = new JLabel("Number of Iterations:");
        iterationsLabel.setFont(new Font("Lucida Sans Unicode", Font.BOLD, 26));
        iterationsLabel.setForeground(new Color(0, 0, 139));
        NumberFormat integerFormat = NumberFormat.getIntegerInstance();
        integerFormat.setGroupingUsed(false);
        NumberFormatter numberFormatter = new NumberFormatter(integerFormat);
        numberFormatter.setValueClass(Integer.class);
        numberFormatter.setMinimum(0);
        numberFormatter.setMaximum(999);
        numberFormatter.setAllowsInvalid(false);
        iterationsField = new JFormattedTextField(numberFormatter);
        iterationsField.setFont(new Font("Lucida Sans Unicode", Font.PLAIN, 20));
        iterationsField.setColumns(10);
        iterationsField.setValue(10); // Default value
        iterationsPanel.add(iterationsLabel);
        iterationsPanel.add(iterationsField);
        configPanel.add(iterationsPanel);



        // Parameters Analysed selection panel
        JPanel parametersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel parametersLabel = new JLabel("Parameters Analysed:");
        parametersLabel.setFont(new Font("Lucida Sans Unicode", Font.BOLD, 26));
        parametersLabel.setForeground(new Color(0, 0, 139));
        parametersPanel.add(parametersLabel);
        parameterCheckBoxes = new ArrayList<>();
        for (String parameter : parameters) {
            JCheckBox checkBox = new JCheckBox(parameter);
            checkBox.setFont(new Font("Lucida Sans Unicode", Font.PLAIN, 20));
            parameterCheckBoxes.add(checkBox);
            parametersPanel.add(checkBox);
        }
        configPanel.add(parametersPanel);

        // Analyze button panel
        analyzeButton = new JButton("Start Analysis");
        analyzeButton.addActionListener(this);
        analyzeButton.setFont(new Font("Lucida Sans Unicode", Font.BOLD, 40));
        analyzeButton.setPreferredSize(new Dimension(400, 100));
        JPanel analyzePanel = new JPanel();
        analyzePanel.setOpaque(false);
        analyzePanel.add(analyzeButton);
        configPanel.add(analyzePanel);

        backgroundLabel.add(configPanel, BorderLayout.CENTER);
        analysisConfigPage.add(backgroundLabel, BorderLayout.CENTER);
    }

    private void createLoadingPage() {
        loadingPage = new JPanel(new BorderLayout());
        loadingPage.setBackground(Color.WHITE);

        // Adding background image
        ImageIcon backgroundImage = new ImageIcon("resources/background.gif");
        JLabel backgroundLabel = new JLabel(backgroundImage);
        backgroundLabel.setLayout(new BorderLayout());

        JPanel loadingPanel = new JPanel(new GridLayout(0, 1));
        loadingPanel.setOpaque(false);
        loadingPanel.setBorder(BorderFactory.createEmptyBorder(160, 80, 160, 80));

        JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topLoadingLabel = new JLabel("Loading...");
        topLoadingLabel.setFont(new Font("Lucida Sans Unicode", Font.BOLD, 80));
        textPanel.add(topLoadingLabel);

        //loadingPanel.add(textPanel);

        JPanel bottomTextPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomTextPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        bottomLoadingLabel = new JLabel("<html>Loading... <br/><br/><br/>This analysis program may take hours to finish<br/>Leave it running in the background<br/><br/>The results will be available at MLAnalyzer/results table<br/>The graphs will be available at MLAnalyzer/results image </html>");
        bottomLoadingLabel.setFont(new Font("Lucida Sans Unicode", Font.BOLD, 26));
        bottomLoadingLabel.setForeground(new Color(0, 0, 0));
        bottomTextPanel.add(bottomLoadingLabel);

        loadingPanel.add(bottomTextPanel, BorderLayout.NORTH);

        backgroundLabel.add(loadingPanel, BorderLayout.CENTER);
        loadingPage.add(backgroundLabel, BorderLayout.CENTER);
    }

    private void updateLoadingLabel(int currentIteration, int totalIterations) {
        String progressText = String.format("Iterations done: %d of %d", currentIteration, totalIterations);
        bottomLoadingLabel.setText("<html>Loading...<br/>" + progressText + "<br/><br/>This analysis program may take hours to finish<br/>Leave it running in the background<br/><br/>The results will be available at MLAnalyzer/results table<br/>The graphs will be available at MLAnalyzer/results image </html>");
        revalidate();
        repaint(); 
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == selectCSVButton) {
            openCSVSelection();
        } else if (e.getSource() == analyzeButton) {

            currentPage = loadingPage;
            setContentPane(currentPage);
            revalidate();
            repaint();

            // Start analysis in a background thread
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    performAnalysis();
                    return null;
                }
            };
            worker.execute();
        }
    }

    private void openCSVSelection() {
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            currentPage.setVisible(false);
            currentPage = analysisConfigPage;
            setContentPane(currentPage);
            revalidate();
            repaint();
        }
    }

    private void performAnalysis() {

        // Get selected data cleaning
        List<String> selectedDataCleaning = new ArrayList<>();
        for (JCheckBox checkBox : dataCleaningCheckBoxes) {
            if (checkBox.isSelected()) {
                selectedDataCleaning.add(checkBox.getText());
            }
        }
        String stringDataCleaning = String.join(", ", selectedDataCleaning);

        // Get selected models
        List<String> selectedModels = new ArrayList<>();
        for (JCheckBox checkBox : modelCheckBoxes) {
            if (checkBox.isSelected()) {
                selectedModels.add(checkBox.getText());
            }
        }

        if (selectedModels.isEmpty()){
            selectedModels.add("Naive Bayes");
        }

        // Get selected techniques
        List<String> selectedTechniques = new ArrayList<>();
        for (JCheckBox checkBox : techniqueCheckBoxes) {
            if (checkBox.isSelected()) {
                selectedTechniques.add(checkBox.getText());
            }
        }

        if (selectedTechniques.isEmpty()){
            selectedTechniques.add("No Technique");
        }
        
        // Get selected number of iteractions
        Object value = iterationsField.getValue();
        if (value instanceof Number) {
            numberOfIterations = ((Number) value).intValue();
        }
        else {
            numberOfIterations = 10;
        }

        // Get selected parameters
        List<String> selectedParameters = new ArrayList<>();
        for (JCheckBox checkBox : parameterCheckBoxes) {
            if (checkBox.isSelected()) {
                selectedParameters.add(checkBox.getText());
            }
        }
        if (selectedParameters.isEmpty()){
            selectedParameters.add("F1-Score");
        }
        String parameters = String.join(",", selectedParameters);

        // Get selected file
        File selectedFile = fileChooser.getSelectedFile();
        
        try {
            pythonOutput.append("technique,model," + parameters).append("\n");
            ProcessBuilder pb_cleaning = new ProcessBuilder("python", "code\\program_data_cleaning.py", selectedFile.getAbsolutePath(), stringDataCleaning);
            pb_cleaning.redirectErrorStream(true);
            Process process_cleaning = pb_cleaning.start();
            int exitCode_cleaning = process_cleaning.waitFor();
            if (exitCode_cleaning != 0) {
                Path logFilePath = Path.of("error_log.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process_cleaning.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    errorLog.append(line).append("\n");
                }  
                Files.writeString(logFilePath, errorLog, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);          
                JOptionPane.showMessageDialog(this, "Error on the plot script\nDetails are on the file 'error_log.txt' at the main folder", "Error", JOptionPane.ERROR_MESSAGE);

                releaseLock();
                System.exit(0);
            }

            int numberOfAnalysDone = -1;
            for (String technique : selectedTechniques) {
                for (String model : selectedModels) {
                    for (int i = 0; i < numberOfIterations; i++) {

                        numberOfAnalysDone++;

                        updateLoadingLabel(numberOfAnalysDone, numberOfIterations * selectedModels.size() * selectedTechniques.size());

                        ProcessBuilder pb = new ProcessBuilder("python", "code\\program_analysis.py", technique, model, selectedOptimization, selectedCrossValidation, parameters);
                        pb.redirectErrorStream(true);
                        Process process = pb.start();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;

                        int exitCode = process.waitFor();
                        if (exitCode != 0) {
                            Path filePath = Paths.get("resources\\cleaned_data.csv");
                            Files.delete(filePath);

                            Path logFilePath = Path.of("error_log.txt");
                            while ((line = reader.readLine()) != null) {
                                errorLog.append(line).append("\n");
                            }  
                            Files.writeString(logFilePath, errorLog, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);          
                            JOptionPane.showMessageDialog(this, "Error on the plot script\nDetails are on the file 'error_log.txt' at the main folder", "Error", JOptionPane.ERROR_MESSAGE);

                            releaseLock();
                            System.exit(0);
                        }
                        else {
                            while ((line = reader.readLine()) != null) {
                                pythonOutput.append(line).append("\n");
                        }
                        }
                    }
                }
            }

            // Delete the cleaned data file
            Path filePath = Paths.get("resources\\cleaned_data.csv");
            Files.delete(filePath);
            
            String returnedString = pythonOutput.toString();

            // Save the returned string as a CSV file
            saveStringAsCSV(returnedString, "results table\\results.csv");
             
             revalidate();
             repaint();

             // Generate image
             ProcessBuilder pbImage = new ProcessBuilder("python", "code\\program_plot.py");
             pbImage.redirectErrorStream(true);
             Process processImage = pbImage.start();

             int exitCodeImage = processImage.waitFor();
            if (exitCodeImage != 0) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(processImage.getInputStream()));
                String line;

                Path logFilePath = Path.of("error_log.txt");
                while ((line = reader.readLine()) != null) {
                    errorLog.append(line).append("\n");
                }  
                Files.writeString(logFilePath, errorLog, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);          
                JOptionPane.showMessageDialog(this, "Error on the plot script\nDetails are on the file 'error_log.txt' at the main folder", "Error", JOptionPane.ERROR_MESSAGE);

                releaseLock();
                System.exit(0);
            }

            // Save results to xlsx and graphs to pdf
            ProcessBuilder pbPDF= new ProcessBuilder("python", "code\\program_xlsx_and_pdf.py");
            pbPDF.redirectErrorStream(true);
             Process processPDF = pbPDF.start();

             int exitCodePDF = processPDF.waitFor();
            if (exitCodePDF != 0) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(processPDF.getInputStream()));
                String line;

                Path logFilePath = Path.of("error_log.txt");
                while ((line = reader.readLine()) != null) {
                    errorLog.append(line).append("\n");
                }  
                Files.writeString(logFilePath, errorLog, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);          
                JOptionPane.showMessageDialog(this, "Error on the plot script\nDetails are on the file 'error_log.txt' at the main folder", "Error", JOptionPane.ERROR_MESSAGE);

                releaseLock();
                System.exit(0);
            }

            // Show returned image
            ImageIcon returnedImage = new ImageIcon("results image\\graphs.png");
            JLabel imageLabel = new JLabel(returnedImage);
            imageLabel.setIcon(returnedImage);
            JScrollPane scrollPane = new JScrollPane(imageLabel);
            setContentPane(scrollPane);

            pack();
            setLocationRelativeTo(null);
            setVisible(true);

            // Revalidate and repaint the frame to reflect the changes
            revalidate();
            repaint();

        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error running Python script: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private static void saveStringAsCSV(String content, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
        }
    }

    public static void main(String[] args) {
        if (!acquireLock()) {
            JOptionPane.showMessageDialog(null, 
                "The application is already running.", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        SwingUtilities.invokeLater(MLAnalyzer::new);
    }

    private static boolean acquireLock() {
        try {
            lockFile = new RandomAccessFile(LOCK_FILE_PATH, "rw");
            FileChannel channel = lockFile.getChannel();
            lock = channel.tryLock();
            return lock != null;
        } catch (OverlappingFileLockException | IOException e) {
            return false;
        }
    }

    private static void releaseLock() {
        try {
            if (lock != null) lock.release();
            if (lockFile != null) lockFile.close();
            new File(LOCK_FILE_PATH).delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}