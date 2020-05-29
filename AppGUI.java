
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.View;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AppGUI extends JFrame {
    JTextField txtPractitionerId = new JTextField(20);
    JButton btnLoadPatients = new JButton("Load Patients");

    JTable patientsTable = new JTable();
    JTable monitorTable = new JTable();
    JButton btnUnmonitorPatient = new JButton("Remove Selected");

    JTextField txtInterval = new JTextField(10);
    JButton btnUpdateInterval = new JButton("Apply");

    ArrayList<Patient> patients = new ArrayList<>();
    ArrayList<String> monitoredPatientIDs = new ArrayList<>();

    boolean isRunning = false;
    int updateInterval = Config.DEFUALT_UPDATE_INTERVAL;

    ScheduledExecutorService executorService;

    public AppGUI() {
        setLayout(new BorderLayout());

        add(authPanel(), BorderLayout.PAGE_START);

        add(splitPanel(), BorderLayout.CENTER);

        add(settingsPanel(), BorderLayout.PAGE_END);

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    JPanel authPanel() {
        JPanel panel = new JPanel();
        JLabel labelPractitionerId = new JLabel("Practioner ID: ");
        panel.add(labelPractitionerId);
        labelPractitionerId.setBounds(0, 0, 0, 0);
        panel.add(txtPractitionerId);
        txtPractitionerId.setBounds(10, 0, 0, 0);
        panel.add(btnLoadPatients);
        btnLoadPatients.setBounds(20, 00, 0, 0);
        btnLoadPatients.addActionListener(new EventHandler());

        return panel;
    }


    JSplitPane splitPanel() {
        String[] patientsColumns = {"Patients"};
        String[] monitorColumns = {"Name", "Cholesterol", "Effective", "Updated"};

        patientsTable.setModel(new DefaultTableModel(patientsColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        patientsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    addPatientToMonitor();
                }
            }
        });
        JScrollPane patientPane = new JScrollPane(patientsTable);
        patientsTable.setFillsViewportHeight(true);

        JPanel monitorPane = new JPanel(new BorderLayout());
        monitorTable.setModel(new DefaultTableModel(monitorColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        monitorTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    viewPatient();
                }
            }
        });

        JScrollPane monitorTablePane = new JScrollPane(monitorTable);
        monitorTable.setFillsViewportHeight(true);
        monitorPane.add(monitorTablePane, BorderLayout.CENTER);
        monitorPane.add(btnUnmonitorPatient, BorderLayout.PAGE_END);
        btnUnmonitorPatient.addActionListener(new EventHandler());

        JSplitPane splitPane = new JSplitPane(SwingConstants.VERTICAL, patientPane, monitorPane);
        splitPane.setDividerLocation(200);

        return splitPane;
    }

    JPanel settingsPanel() {
        JPanel panel = new JPanel();
        JLabel settingsLabel = new JLabel("Interval (secs)");
        panel.add(settingsLabel);
        settingsLabel.setBounds(0, 0, 0, 0);

        panel.add(txtInterval);
        txtInterval.setBounds(10, 0, 0, 0);
        txtInterval.setText(Integer.toString(Config.DEFUALT_UPDATE_INTERVAL));

        panel.add(btnUpdateInterval);
        btnUpdateInterval.setBounds(20, 0, 0, 0);

        btnUpdateInterval.addActionListener(new EventHandler());

        return panel;
    }

    void loadPatients() {
        if(isRunning) {
            isRunning = false;
            executorService.shutdownNow();
        }
        monitoredPatientIDs.clear();
        patients.clear();
        String practitionerId = txtPractitionerId.getText();
        if(practitionerId.length() > 0) {
            btnLoadPatients.setText("Loading...");
            btnLoadPatients.setEnabled(false);
            DefaultTableModel patientsTableModel = (DefaultTableModel) patientsTable.getModel();
            DefaultTableModel monitorTableModel = (DefaultTableModel) monitorTable.getModel();
            patientsTableModel.setRowCount(0);
            monitorTableModel.setRowCount(0);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try{
                        patients = ApiService.getPatients(practitionerId);

                        for(Patient patient: patients) {
                            patientsTableModel.addRow(new String[]{patient.getName()});
                        }

                        btnLoadPatients.setText("Load Patients");
                        btnLoadPatients.setEnabled(true);

                    }
                    catch (PractitionerNotFoundException e) {
                        JOptionPane.showMessageDialog(getContentPane(), "Practitioner not found");
                        btnLoadPatients.setText("Load Patients");
                        btnLoadPatients.setEnabled(true);
                    }
                }
            });
        }
    }

    void addPatientToMonitor() {
        int selectedPatientIndex = patientsTable.getSelectedRow();
        String patientId = patients.get(selectedPatientIndex).getId();
        if(!monitoredPatientIDs.contains(patientId)) {
            monitoredPatientIDs.add(patientId);
            DefaultTableModel tableModel = (DefaultTableModel) monitorTable.getModel();
            try {
                Map<String, String> data = ApiService.getCholesterol(patients.get(selectedPatientIndex).getId());
                tableModel.addRow(new String[]{
                        patients.get(selectedPatientIndex).getName(),
                        data.get("totalCholesterol") + " " + data.get("cholesterolUnit"),
                        data.get("effectiveDateTime"),
                        data.get("updated")
                });

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if(!isRunning) {
                            isRunning = true;
                            runUpdates();
                        }
                    }
                });
            }
            catch (ObservationNotFoundException e) {
                JOptionPane.showMessageDialog(getContentPane(), "Patient has no cholesterol observation");
            }
        }
        else {
            JOptionPane.showMessageDialog(getContentPane(), "Patient is already monitored");
        }
    }

    void removePatientFromMonitor() {
        int selectedPatientIndex = monitorTable.getSelectedRow();
        monitoredPatientIDs.remove(selectedPatientIndex);

        DefaultTableModel tableModel = (DefaultTableModel) monitorTable.getModel();
        tableModel.removeRow(selectedPatientIndex);

        if(monitoredPatientIDs.size() == 0) {
            isRunning = false;
        }
    }

    void updateMonitoredPatient() {
        patientsTable.setEnabled(false);
        monitorTable.setEnabled(false);
        btnUnmonitorPatient.setEnabled(false);
        if(monitoredPatientIDs.size() > 0) {
            DefaultTableModel tableModel = (DefaultTableModel) monitorTable.getModel();
            for(int i = 0; i < monitoredPatientIDs.size(); i++) {
                try {
                    Map<String, String> data = ApiService.getCholesterol(monitoredPatientIDs.get(i));
                    tableModel.setValueAt(
                            data.get("totalCholesterol") + " " + data.get("cholesterolUnit"),
                            i,
                            1
                    );
                    tableModel.setValueAt(
                            data.get("effectiveDateTime"),
                            i,
                            2
                    );
                    tableModel.setValueAt(
                            data.get("updated"),
                            i,
                            3
                    );

                } catch (ObservationNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        patientsTable.setEnabled(true);
        monitorTable.setEnabled(true);
        btnUnmonitorPatient.setEnabled(true);

    }

    void changeInterval() {
        if(isRunning) {
            executorService.shutdownNow();
            updateInterval = Integer.parseInt(txtInterval.getText());
            runUpdates();
        }
    }

    void runUpdates() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(AppGUI.this::updateMonitoredPatient, updateInterval, updateInterval, TimeUnit.SECONDS);
    }

    void viewPatient() {
        int patientIndex = monitorTable.getSelectedRow();
        Patient patient = ApiService.getPatientDetails(monitoredPatientIDs.get(patientIndex));
        if(patient != null) {
            new ViewPatient(patient);
        }
    }

    public static void main(String[] args) {
        new AppGUI();
    }

    public class EventHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if(e.getSource() == btnLoadPatients) {
                loadPatients();
            }
            else if(e.getSource() == btnUnmonitorPatient) {
                removePatientFromMonitor();
            }
            else if(e.getSource() == btnUpdateInterval) {
                changeInterval();
            }
        }
    }

    public class ViewPatient extends JFrame {
        public ViewPatient(Patient patient){
            setLayout(new GridLayout(0, 2));
            add(new JLabel("ID"));
            add(new JLabel(patient.getId()));
            add(new JLabel("Name"));
            add(new JLabel(patient.getName()));
            add(new JLabel("Gender"));
            add(new JLabel(patient.getGender()));
            add(new JLabel("Address"));
            add(new JLabel(patient.getAddress()));
            add(new JLabel("City"));
            add(new JLabel(patient.getCity()));
            add(new JLabel("State"));
            add(new JLabel(patient.getState()));
            add(new JLabel("Country"));
            add(new JLabel(patient.getCountry()));

            setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            setSize(400, 300);
            setTitle("Patient Details");
            setVisible(true);
        }
        @Override
        public Insets getInsets() {
            return new Insets(20, 10, 10, 10);
        }
    }
}
