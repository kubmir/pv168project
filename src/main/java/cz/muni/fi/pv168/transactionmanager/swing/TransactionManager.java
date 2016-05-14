package cz.muni.fi.pv168.transactionmanager.swing;

import cz.muni.fi.pv168.transactionmanager.Account;
import cz.muni.fi.pv168.transactionmanager.AccountManagerImpl;
import cz.muni.fi.pv168.transactionmanager.Payment;
import cz.muni.fi.pv168.transactionmanager.PaymentManagerImpl;
import cz.muni.fi.pv168.utils.DBUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 * Desktop application in swing for transaction manager.
 * @author Miroslav Kubus, Viktória Tóthová
 */
public class TransactionManager extends javax.swing.JFrame {
    
    private AccountManagerImpl accountManager;
    private final DataSource dataSource;
    private PaymentManagerImpl paymentManager;
    private Account account;
    private final AccountTableModel accountModel;
    private PaymentTableModel paymentModel;
    
    /**
     * Creates new form TransactionManager
     */
    public TransactionManager() {
        dataSource = DBUtils.createMemoryDatabase();
        accountManager = new AccountManagerImpl(dataSource);
        paymentManager = new PaymentManagerImpl(dataSource);
        initComponents();
        accountModel = (AccountTableModel) jAccountTable.getModel();
        accountModel.updateAccounts();
        paymentModel = (PaymentTableModel) jPaymentTable.getModel();
    }
        
    private class CreateAccountSwingWorker extends SwingWorker<Void, Void> {

        @Override
        protected Void doInBackground() throws Exception {
            accountManager.createAccount(account);
            accountModel.addAccount(account);
            return null;
        }
        
        @Override    
        protected void done() {
            try {
                this.get();
                JOptionPane.showMessageDialog(null, java.util.ResourceBundle.getBundle("cz/muni/fi/pv168/transactionmanager/swing/Bundle").getString("ACCOUNT CREATED"), "Message", JOptionPane.INFORMATION_MESSAGE);
            } catch (InterruptedException | ExecutionException ex) {
                JOptionPane.showMessageDialog(null, ex.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
        
    private class CreatePaymentSwingWorker extends SwingWorker<Void, Void> {
        
        @Override
        protected Void doInBackground() throws Exception {
            Payment payment = new Payment();
            payment.setDate(jCalendar.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            payment.setFrom(accountManager.getAccountByNumber((String) makePaymentFromAccJComboBox.getSelectedItem()));
            payment.setTo(accountManager.getAccountByNumber((String) makePaymentToAccJComboBox.getSelectedItem()));
            payment.setAmount(new BigDecimal(makePaymentAmountJTextField.getText()));
        
            paymentManager.createPayment(payment);
            accountModel.updateAccounts();
            
            return null;
        }
        
        @Override    
        protected void done() {
            try {
                this.get();
                JOptionPane.showMessageDialog(null, java.util.ResourceBundle.getBundle("cz/muni/fi/pv168/transactionmanager/swing/Bundle").getString("PAYMENT CREATED"), "Message", JOptionPane.INFORMATION_MESSAGE);
            } catch(InterruptedException | ExecutionException ex) {
                JOptionPane.showMessageDialog(null, ex.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }   
    }
    
    private class DeleteAccountSwingWorker extends SwingWorker<Void, Void> {
        private int index;
        
        public DeleteAccountSwingWorker(int index) {
            this.index = index;
        }
        @Override
        protected Void doInBackground() throws Exception {
            account = accountManager.getAccountById(account.getId());
            accountManager.deleteAccount(account);
            return null;
        }
        
        @Override    
        protected void done() {
            try {
                this.get();
                JOptionPane.showMessageDialog(null, "Account deleted", "Message", JOptionPane.INFORMATION_MESSAGE);
                accountModel.removeRow(index);
            } catch (InterruptedException | ExecutionException ex) {
                JOptionPane.showMessageDialog(null, ex.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }      
    }
    
    private class UpdateAccountSwingWorker extends SwingWorker<Void, Void> {
        private Account acc;
        private int index;
        
        public UpdateAccountSwingWorker(Account acc, int i) {
            this.acc = acc;
            index = i;
        }
        
        @Override
        protected Void doInBackground() throws Exception {
            accountManager.updateAccount(acc);
            return null;
        }
        
        @Override    
        protected void done() {
            try {
                this.get();
                accountModel.setValueAt(acc.getNumber(), index, 1);
                accountModel.setValueAt(acc.getHolder(), index, 2);
                accountModel.setValueAt(acc.getBalance(), index, 3);
//                JOptionPane.showMessageDialog(null, "Account updated", "Message", JOptionPane.INFORMATION_MESSAGE);
            } catch (InterruptedException | ExecutionException ex) {
                JOptionPane.showMessageDialog(null, ex.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }      
    }
        
    private class GetAllAccountsSwingWorker extends SwingWorker<Void, Void> {
        private List<Account> accounts = new ArrayList<>();
        private final JComboBox comboBox;
        
        public GetAllAccountsSwingWorker(JComboBox combo) {
            this.comboBox = combo;
        }
        
        @Override
        protected Void doInBackground() throws Exception {
            accounts = accountManager.getAllAccounts();
            return null;
        }
        
        @Override    
        protected void done() {
            try {
                this.get();
                comboBox.removeAllItems();
            
                for(Account account : accounts) {
                    comboBox.addItem(account.getNumber());
                }            
            } catch (InterruptedException | ExecutionException ex) {
              //TODO  
            }
        }
    }
    
    private class ChooseAccountSwingWorker extends SwingWorker<Void, Void> {

        private Account selected;
        private final String number;
        
        public ChooseAccountSwingWorker(String number) {
            this.number = number;
        }
        
        @Override
        protected Void doInBackground() throws Exception {
            selected = accountManager.getAccountByNumber(number);
            
            return null;
        }
        
        @Override    
        protected void done() {
            try {
                this.get();
                paymentAccountHolder.setText(selected.getHolder());
                paymentAccountBalanceLabel.setText(String.valueOf(selected.getBalance()));
                paymentAccountNumberLabel.setText((String)chooseAccountInPaymentComboBox.getSelectedItem());
            } catch (InterruptedException | ExecutionException ex) {
                JOptionPane.showMessageDialog(null, ex.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private class GetTransactionsSwingWorker extends SwingWorker<Void, Void>  {
        private final String number;
        private List<Payment> payments;
        private final boolean incoming;
        
        public GetTransactionsSwingWorker(String number, boolean incoming) {
            this.number = number;
            this.incoming = incoming;
        }

        @Override
        protected Void doInBackground() throws Exception {
            Account selected = accountManager.getAccountByNumber(number);
            
            if(incoming) {
                payments = paymentManager.getPaymentsToAcoount(selected);
            } else {
                payments = paymentManager.getPaymentsFromAccount(selected);
            }
            
            return null;
        }
        
        @Override    
        protected void done() {
            try {
                this.get();
                
                for(Payment pat : payments) {
                    paymentModel.addPayment(pat);
                }
            } catch (InterruptedException | ExecutionException  ex) {
                JOptionPane.showMessageDialog(null, ex.getCause().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } 
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTransactionManagerTabbedPane = new javax.swing.JTabbedPane();
        jPanel4 = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        createAccountHolderTextField = new javax.swing.JTextField();
        createAccountButton = new javax.swing.JButton();
        jLabel17 = new javax.swing.JLabel();
        createAccountNumberTextField = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        createAccountBalanceTextField = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jAccountTable = new javax.swing.JTable();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        deleteAccountButton = new javax.swing.JButton();
        accountNumberTextField = new javax.swing.JTextField();
        accountHolderTextField = new javax.swing.JTextField();
        accountBalanceTextField = new javax.swing.JTextField();
        updateAccountButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        makePaymentFromAccJComboBox = new javax.swing.JComboBox<>();
        jLabel9 = new javax.swing.JLabel();
        makePaymentToAccJComboBox = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        makePaymentAmountJTextField = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        createPaymentButton = new javax.swing.JButton();
        jCalendar = new com.toedter.calendar.JCalendar();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        chooseAccountInPaymentComboBox = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPaymentTable = new javax.swing.JTable();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel32 = new javax.swing.JLabel();
        incomingTransactionCheckBox = new javax.swing.JCheckBox();
        outgoingTransactionCheckBox = new javax.swing.JCheckBox();
        paymentAccountNumberLabel = new javax.swing.JLabel();
        paymentAccountHolder = new javax.swing.JLabel();
        paymentAccountBalanceLabel = new javax.swing.JLabel();
        MainJMenuBar = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jAccountsSummaryButtonInMenu = new javax.swing.JMenuItem();
        jTransactionsSummaryButtonInMenu = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jExitButtonInMenu = new javax.swing.JMenuItem();
        jMenuNew = new javax.swing.JMenu();
        jMakePaymentButtonInMenu = new javax.swing.JMenuItem();
        jMenuCreateNewAccountButtonInMenu = new javax.swing.JMenuItem();
        jMenuAbout = new javax.swing.JMenu();
        jAboutButtonInMenu = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("cz/muni/fi/pv168/transactionmanager/swing/Bundle"); // NOI18N
        jLabel16.setText(bundle.getString("TransactionManager.jLabel16.text")); // NOI18N

        createAccountHolderTextField.setText(bundle.getString("TransactionManager.createAccountHolderTextField.text")); // NOI18N

        createAccountButton.setText(bundle.getString("TransactionManager.createAccountButton.text")); // NOI18N
        createAccountButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createAccountButtonActionPerformed(evt);
            }
        });

        jLabel17.setText(bundle.getString("TransactionManager.jLabel17.text")); // NOI18N

        createAccountNumberTextField.setText(bundle.getString("TransactionManager.createAccountNumberTextField.text")); // NOI18N

        jLabel12.setText(bundle.getString("TransactionManager.jLabel12.text")); // NOI18N

        createAccountBalanceTextField.setText(bundle.getString("TransactionManager.createAccountBalanceTextField.text")); // NOI18N
        createAccountBalanceTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createAccountBalanceTextFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(createAccountButton))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel16)
                            .addComponent(jLabel17)
                            .addComponent(jLabel12))
                        .addGap(99, 99, 99)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(createAccountHolderTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
                                .addComponent(createAccountBalanceTextField))
                            .addComponent(createAccountNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 475, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(createAccountHolderTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(createAccountNumberTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(createAccountBalanceTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 393, Short.MAX_VALUE)
                .addComponent(createAccountButton)
                .addContainerGap())
        );

        jTransactionManagerTabbedPane.addTab(bundle.getString("TransactionManager.jPanel4.TabConstraints.tabTitle"), jPanel4); // NOI18N

        jAccountTable.setModel(new AccountTableModel(accountManager));
        jAccountTable.setDefaultRenderer(BigDecimal.class, new BigDecimalRenderer());
        jAccountTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jAccountTableMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(jAccountTable);

        jLabel20.setText(bundle.getString("TransactionManager.jLabel20.text")); // NOI18N

        jLabel21.setText(bundle.getString("TransactionManager.jLabel21.text")); // NOI18N

        jLabel22.setText(bundle.getString("TransactionManager.jLabel22.text")); // NOI18N

        jLabel23.setText(bundle.getString("TransactionManager.jLabel23.text")); // NOI18N

        deleteAccountButton.setText(bundle.getString("TransactionManager.deleteAccountButton.text")); // NOI18N
        deleteAccountButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteAccountButtonActionPerformed(evt);
            }
        });

        accountNumberTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        accountHolderTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        accountBalanceTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        updateAccountButton.setText(bundle.getString("TransactionManager.updateAccountButton.text")); // NOI18N
        updateAccountButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateAccountButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 801, Short.MAX_VALUE)
                            .addComponent(jSeparator4)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel20)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel21)
                            .addComponent(jLabel23)
                            .addComponent(jLabel22))
                        .addGap(58, 58, 58)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(accountHolderTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)
                            .addComponent(accountNumberTextField, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(accountBalanceTextField))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(updateAccountButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteAccountButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(28, 28, 28))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel20)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(accountNumberTextField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel21))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel22)
                            .addComponent(accountHolderTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel23)
                            .addComponent(accountBalanceTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 351, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(57, 57, 57)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(deleteAccountButton)
                            .addComponent(updateAccountButton))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        jTransactionManagerTabbedPane.addTab(bundle.getString("TransactionManager.jPanel1.TabConstraints.tabTitle"), jPanel1); // NOI18N

        jLabel2.setText(bundle.getString("TransactionManager.jLabel2.text")); // NOI18N

        makePaymentFromAccJComboBox.setMaximumRowCount(10);
        makePaymentFromAccJComboBox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                makePaymentFromAccJComboBoxMouseClicked(evt);
            }
        });

        jLabel9.setText(bundle.getString("TransactionManager.jLabel9.text")); // NOI18N

        makePaymentToAccJComboBox.setMaximumRowCount(10);
        makePaymentToAccJComboBox.setToolTipText(bundle.getString("TransactionManager.makePaymentToAccJComboBox.toolTipText")); // NOI18N
        makePaymentToAccJComboBox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                makePaymentToAccJComboBoxMouseClicked(evt);
            }
        });

        jLabel10.setText(bundle.getString("TransactionManager.jLabel10.text")); // NOI18N

        makePaymentAmountJTextField.setText(bundle.getString("TransactionManager.makePaymentAmountJTextField.text")); // NOI18N

        jLabel11.setText(bundle.getString("TransactionManager.jLabel11.text")); // NOI18N

        createPaymentButton.setText(bundle.getString("TransactionManager.createPaymentButton.text")); // NOI18N
        createPaymentButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createPaymentButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel9)
                            .addComponent(jLabel2))
                        .addGap(59, 59, 59)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(makePaymentFromAccJComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(makePaymentAmountJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 75, Short.MAX_VALUE))
                            .addComponent(makePaymentToAccJComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(415, 415, 415))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(createPaymentButton, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(12, 12, 12))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel10)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel11)
                                .addGap(30, 30, 30)
                                .addComponent(jCalendar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(makePaymentFromAccJComboBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(makePaymentToAccJComboBox)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(makePaymentAmountJTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(53, 53, 53)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel11)
                    .addComponent(jCalendar, javax.swing.GroupLayout.PREFERRED_SIZE, 133, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 204, Short.MAX_VALUE)
                .addComponent(createPaymentButton)
                .addContainerGap())
        );

        jTransactionManagerTabbedPane.addTab(bundle.getString("TransactionManager.jPanel3.TabConstraints.tabTitle"), jPanel3); // NOI18N

        jLabel1.setText(bundle.getString("TransactionManager.jLabel1.text")); // NOI18N

        chooseAccountInPaymentComboBox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chooseAccountInPaymentComboBoxMouseClicked(evt);
            }
        });
        chooseAccountInPaymentComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chooseAccountInPaymentComboBoxActionPerformed(evt);
            }
        });

        jPaymentTable.setModel(new PaymentTableModel());
        jPaymentTable.setDefaultRenderer(BigDecimal.class, new BigDecimalRenderer());
        jPaymentTable.setDefaultRenderer(LocalDate.class, new DateRenderer());
        jPaymentTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(jPaymentTable);

        jLabel3.setText(bundle.getString("TransactionManager.jLabel3.text")); // NOI18N

        jLabel4.setText(bundle.getString("TransactionManager.jLabel4.text")); // NOI18N

        jLabel5.setText(bundle.getString("TransactionManager.jLabel5.text")); // NOI18N

        jLabel32.setText(bundle.getString("TransactionManager.jLabel32.text")); // NOI18N

        incomingTransactionCheckBox.setText(bundle.getString("TransactionManager.incomingTransactionCheckBox.text")); // NOI18N
        incomingTransactionCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                incomingTransactionCheckBoxActionPerformed(evt);
            }
        });

        outgoingTransactionCheckBox.setText(bundle.getString("TransactionManager.outgoingTransactionCheckBox.text")); // NOI18N
        outgoingTransactionCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                outgoingTransactionCheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(outgoingTransactionCheckBox)
                    .addComponent(incomingTransactionCheckBox)
                    .addComponent(jLabel32))
                .addContainerGap(228, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel32)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(incomingTransactionCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(outgoingTransactionCheckBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        paymentAccountNumberLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        paymentAccountNumberLabel.setText(bundle.getString("TransactionManager.paymentAccountNumberLabel.text")); // NOI18N
        paymentAccountNumberLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        paymentAccountHolder.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        paymentAccountHolder.setText(bundle.getString("TransactionManager.paymentAccountHolder.text")); // NOI18N

        paymentAccountBalanceLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        paymentAccountBalanceLabel.setText(bundle.getString("TransactionManager.paymentAccountBalanceLabel.text")); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(jSeparator1)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5))
                        .addGap(11, 11, 11)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(paymentAccountNumberLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(paymentAccountHolder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(paymentAccountBalanceLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 128, Short.MAX_VALUE)
                            .addComponent(chooseAccountInPaymentComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 173, Short.MAX_VALUE)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(chooseAccountInPaymentComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(paymentAccountNumberLabel))
                        .addGap(13, 13, 13)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(paymentAccountHolder))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(paymentAccountBalanceLabel)))
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 4, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 363, Short.MAX_VALUE)
                .addContainerGap())
        );

        paymentAccountNumberLabel.getAccessibleContext().setAccessibleName(bundle.getString("TransactionManager.paymentAccountNumberLabel.AccessibleContext.accessibleName")); // NOI18N
        paymentAccountHolder.getAccessibleContext().setAccessibleName(bundle.getString("TransactionManager.paymentAccountHolder.AccessibleContext.accessibleName")); // NOI18N

        jTransactionManagerTabbedPane.addTab(bundle.getString("TransactionManager.jPanel2.TabConstraints.tabTitle"), jPanel2); // NOI18N

        jMenuFile.setText(bundle.getString("TransactionManager.jMenuFile.text")); // NOI18N

        jAccountsSummaryButtonInMenu.setText(bundle.getString("TransactionManager.jAccountsSummaryButtonInMenu.text")); // NOI18N
        jAccountsSummaryButtonInMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAccountsSummaryButtonInMenuActionPerformed(evt);
            }
        });
        jMenuFile.add(jAccountsSummaryButtonInMenu);

        jTransactionsSummaryButtonInMenu.setText(bundle.getString("TransactionManager.jTransactionsSummaryButtonInMenu.text")); // NOI18N
        jTransactionsSummaryButtonInMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTransactionsSummaryButtonInMenuActionPerformed(evt);
            }
        });
        jMenuFile.add(jTransactionsSummaryButtonInMenu);
        jMenuFile.add(jSeparator2);

        jExitButtonInMenu.setText(bundle.getString("TransactionManager.jExitButtonInMenu.text")); // NOI18N
        jExitButtonInMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jExitButtonInMenuActionPerformed(evt);
            }
        });
        jMenuFile.add(jExitButtonInMenu);

        MainJMenuBar.add(jMenuFile);

        jMenuNew.setText(bundle.getString("TransactionManager.jMenuNew.text")); // NOI18N

        jMakePaymentButtonInMenu.setText(bundle.getString("TransactionManager.jMakePaymentButtonInMenu.text")); // NOI18N
        jMakePaymentButtonInMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMakePaymentButtonInMenuActionPerformed(evt);
            }
        });
        jMenuNew.add(jMakePaymentButtonInMenu);

        jMenuCreateNewAccountButtonInMenu.setText(bundle.getString("TransactionManager.jMenuCreateNewAccountButtonInMenu.text")); // NOI18N
        jMenuCreateNewAccountButtonInMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuCreateNewAccountButtonInMenuActionPerformed(evt);
            }
        });
        jMenuNew.add(jMenuCreateNewAccountButtonInMenu);

        MainJMenuBar.add(jMenuNew);

        jMenuAbout.setText(bundle.getString("TransactionManager.jMenuAbout.text")); // NOI18N

        jAboutButtonInMenu.setText(bundle.getString("TransactionManager.jAboutButtonInMenu.text")); // NOI18N
        jAboutButtonInMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jAboutButtonInMenuActionPerformed(evt);
            }
        });
        jMenuAbout.add(jAboutButtonInMenu);

        MainJMenuBar.add(jMenuAbout);

        setJMenuBar(MainJMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTransactionManagerTabbedPane)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTransactionManagerTabbedPane)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void chooseAccountInPaymentComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chooseAccountInPaymentComboBoxActionPerformed
        if(chooseAccountInPaymentComboBox.getSelectedItem() != null) {
            ChooseAccountSwingWorker chooseAccountSwingWorker = new ChooseAccountSwingWorker((String)chooseAccountInPaymentComboBox.getSelectedItem());
            chooseAccountSwingWorker.execute();
        }
    }//GEN-LAST:event_chooseAccountInPaymentComboBoxActionPerformed

    private void jExitButtonInMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jExitButtonInMenuActionPerformed
        System.exit(0);
    }//GEN-LAST:event_jExitButtonInMenuActionPerformed

    private void createPaymentButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createPaymentButtonActionPerformed
        CreatePaymentSwingWorker createPaymentSwingWorker = new CreatePaymentSwingWorker();
        createPaymentSwingWorker.execute();
    }//GEN-LAST:event_createPaymentButtonActionPerformed

    private void jMenuCreateNewAccountButtonInMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuCreateNewAccountButtonInMenuActionPerformed
        jTransactionManagerTabbedPane.setSelectedIndex(0);
    }//GEN-LAST:event_jMenuCreateNewAccountButtonInMenuActionPerformed

    private void jAboutButtonInMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jAboutButtonInMenuActionPerformed
        JFrame frame = new JFrame();
        frame.setTitle("About transaction manager");
            
        JLabel info = new JLabel();
//        info.setFont((new java.awt.Font("Times New Roman", 4, 20)));
        info.setText("<html>Application transaction manager allows you to create <br> new accounts "
                + "and payments from and to these accounts. <br> You can also modify and delete accounts <br> <br> <html>");
           
        frame.add(info, BorderLayout.NORTH);
              
        JLabel credits = new JLabel();
//        credits.setFont((new java.awt.Font("Arial", 3, 18)));
        credits.setText("Credits: Viktória Tóthova and Miroslav Kubus");
        frame.add(credits, BorderLayout.SOUTH);
            
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setVisible(true);
    }//GEN-LAST:event_jAboutButtonInMenuActionPerformed

    private void createAccountBalanceTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createAccountBalanceTextFieldActionPerformed
        createAccount();
    }//GEN-LAST:event_createAccountBalanceTextFieldActionPerformed

    private void createAccountButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createAccountButtonActionPerformed
        createAccount();
    }//GEN-LAST:event_createAccountButtonActionPerformed

    private void jMakePaymentButtonInMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMakePaymentButtonInMenuActionPerformed
        jTransactionManagerTabbedPane.setSelectedIndex(2);
    }//GEN-LAST:event_jMakePaymentButtonInMenuActionPerformed

    private void jAccountsSummaryButtonInMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jAccountsSummaryButtonInMenuActionPerformed
        jTransactionManagerTabbedPane.setSelectedIndex(1);
    }//GEN-LAST:event_jAccountsSummaryButtonInMenuActionPerformed

    private void jTransactionsSummaryButtonInMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTransactionsSummaryButtonInMenuActionPerformed
        jTransactionManagerTabbedPane.setSelectedIndex(3);
    }//GEN-LAST:event_jTransactionsSummaryButtonInMenuActionPerformed

    private void makePaymentFromAccJComboBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_makePaymentFromAccJComboBoxMouseClicked
        GetAllAccountsSwingWorker getAllFromAccSwingWorker = new GetAllAccountsSwingWorker(makePaymentFromAccJComboBox);
        getAllFromAccSwingWorker.execute();
        //makePaymentFromAccJComboBox.removeItem(makePaymentToAccJComboBox.getSelectedItem());//PROBLEM, NEZMAZE TUTO MOZNOST

    }//GEN-LAST:event_makePaymentFromAccJComboBoxMouseClicked

    private void makePaymentToAccJComboBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_makePaymentToAccJComboBoxMouseClicked
        GetAllAccountsSwingWorker getAllAccSwingWorker = new GetAllAccountsSwingWorker(makePaymentToAccJComboBox);
        getAllAccSwingWorker.execute();
        //makePaymentToAccJComboBox.removeItem(makePaymentFromAccJComboBox.getSelectedItem());//PROBLEM, NEZMAZE TUTO MOZNOST
    }//GEN-LAST:event_makePaymentToAccJComboBoxMouseClicked

    private void deleteAccountButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteAccountButtonActionPerformed
        int i = jAccountTable.getSelectedRow();
        
        if(i >= 0) {
            //account.setId((Long) jAccountTable.getValueAt(i, 0));
            DeleteAccountSwingWorker deleteAccountSwingWorker = new DeleteAccountSwingWorker(i);
            deleteAccountSwingWorker.execute();
        }
    }//GEN-LAST:event_deleteAccountButtonActionPerformed

    private void jAccountTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jAccountTableMouseClicked
        accountNumberTextField.setText((String) jAccountTable.getValueAt(jAccountTable.getSelectedRow(), 1));
        accountHolderTextField.setText((String) jAccountTable.getValueAt(jAccountTable.getSelectedRow(), 2));
        accountBalanceTextField.setText(String.valueOf(jAccountTable.getValueAt(jAccountTable.getSelectedRow(), 3)));
    }//GEN-LAST:event_jAccountTableMouseClicked

    private void updateAccountButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateAccountButtonActionPerformed
        Account acc = new Account();
        int index = jAccountTable.getSelectedRow();
        
        if(index >= 0) {
            acc.setId((Long) accountModel.getValueAt(index, 0));
            acc.setNumber(accountNumberTextField.getText());
            acc.setHolder(accountHolderTextField.getText());
            acc.setBalance(new BigDecimal(accountBalanceTextField.getText()));

            UpdateAccountSwingWorker updateAccount = new UpdateAccountSwingWorker(acc, index);
            updateAccount.execute();
        } else {
            JOptionPane.showMessageDialog(null, java.util.ResourceBundle.getBundle("cz/muni/fi/pv168/transactionmanager/swing/Bundle").getString("CHOOSE ACCOUNT TO BE UPDATE FROM TABLE"), "Message", JOptionPane.INFORMATION_MESSAGE);
        }
    }//GEN-LAST:event_updateAccountButtonActionPerformed

    private void chooseAccountInPaymentComboBoxMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_chooseAccountInPaymentComboBoxMouseClicked
        GetAllAccountsSwingWorker getAllAccSwingWorker = new GetAllAccountsSwingWorker(chooseAccountInPaymentComboBox);
        getAllAccSwingWorker.execute();
        paymentModel = new PaymentTableModel();
        jPaymentTable.setModel(paymentModel);
        if(incomingTransactionCheckBox.isSelected()) {                     
            incomingTransactionCheckBox.setSelected(false);
        }
        
        if(outgoingTransactionCheckBox.isSelected()) {                     
            outgoingTransactionCheckBox.setSelected(false);
        }
    }//GEN-LAST:event_chooseAccountInPaymentComboBoxMouseClicked

    private void incomingTransactionCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_incomingTransactionCheckBoxActionPerformed
        GetTransactionsSwingWorker getTransactionsSW;
        if(incomingTransactionCheckBox.isSelected()) {
            getTransactionsSW = new GetTransactionsSwingWorker((String) chooseAccountInPaymentComboBox.getSelectedItem(), true);
            getTransactionsSW.execute();
        } else {
            while(paymentModel.getRowCount() > 0) {
                paymentModel.removeRow(0);
            }
            
            if(outgoingTransactionCheckBox.isSelected()) {
                getTransactionsSW = new GetTransactionsSwingWorker((String) chooseAccountInPaymentComboBox.getSelectedItem(), false);
                getTransactionsSW.execute();
            }
        }
    }//GEN-LAST:event_incomingTransactionCheckBoxActionPerformed

    private void outgoingTransactionCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_outgoingTransactionCheckBoxActionPerformed
        GetTransactionsSwingWorker getTransactionsSW;
        if(outgoingTransactionCheckBox.isSelected()) {
            getTransactionsSW = new GetTransactionsSwingWorker((String) chooseAccountInPaymentComboBox.getSelectedItem(), false);
            getTransactionsSW.execute();
        } else {
            
            while(paymentModel.getRowCount() > 0) {
                paymentModel.removeRow(0);
            }
            
            if(incomingTransactionCheckBox.isSelected()) {
                getTransactionsSW = new GetTransactionsSwingWorker((String) chooseAccountInPaymentComboBox.getSelectedItem(), true);
                getTransactionsSW.execute();
            }
        }
    }//GEN-LAST:event_outgoingTransactionCheckBoxActionPerformed
    
    private void createAccount() {
        String balanceText = createAccountBalanceTextField.getText();
        String number = createAccountNumberTextField.getText();
        String holder = createAccountHolderTextField.getText();

        try {
            BigDecimal balance = new BigDecimal(balanceText);
            
            account = new Account();
            account.setBalance(balance);
            account.setHolder(holder);
            account.setNumber(number);
            
            CreateAccountSwingWorker createAccountSwingWorker = new CreateAccountSwingWorker();
            createAccountSwingWorker.execute();
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, java.util.ResourceBundle.getBundle("cz/muni/fi/pv168/transactionmanager/swing/Bundle").getString("BAD FORMAT OF BALANCE OF ACCOUNT TO BE CREATED"), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(TransactionManager.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(TransactionManager.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(TransactionManager.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TransactionManager.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold
        
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            JFrame frame = new TransactionManager();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setTitle(java.util.ResourceBundle.getBundle("cz/muni/fi/pv168/transactionmanager/swing/Bundle").getString("TRANSACTION MANAGER"));
            frame.setVisible(true);
         
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuBar MainJMenuBar;
    private javax.swing.JTextField accountBalanceTextField;
    private javax.swing.JTextField accountHolderTextField;
    private javax.swing.JTextField accountNumberTextField;
    private javax.swing.JComboBox<String> chooseAccountInPaymentComboBox;
    private javax.swing.JTextField createAccountBalanceTextField;
    private javax.swing.JButton createAccountButton;
    private javax.swing.JTextField createAccountHolderTextField;
    private javax.swing.JTextField createAccountNumberTextField;
    private javax.swing.JButton createPaymentButton;
    private javax.swing.JButton deleteAccountButton;
    private javax.swing.JCheckBox incomingTransactionCheckBox;
    private javax.swing.JMenuItem jAboutButtonInMenu;
    private javax.swing.JTable jAccountTable;
    private javax.swing.JMenuItem jAccountsSummaryButtonInMenu;
    private com.toedter.calendar.JCalendar jCalendar;
    private javax.swing.JMenuItem jExitButtonInMenu;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenuItem jMakePaymentButtonInMenu;
    private javax.swing.JMenu jMenuAbout;
    private javax.swing.JMenuItem jMenuCreateNewAccountButtonInMenu;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenu jMenuNew;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JTable jPaymentTable;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JTabbedPane jTransactionManagerTabbedPane;
    private javax.swing.JMenuItem jTransactionsSummaryButtonInMenu;
    private javax.swing.JTextField makePaymentAmountJTextField;
    private javax.swing.JComboBox<String> makePaymentFromAccJComboBox;
    private javax.swing.JComboBox<String> makePaymentToAccJComboBox;
    private javax.swing.JCheckBox outgoingTransactionCheckBox;
    private javax.swing.JLabel paymentAccountBalanceLabel;
    private javax.swing.JLabel paymentAccountHolder;
    private javax.swing.JLabel paymentAccountNumberLabel;
    private javax.swing.JButton updateAccountButton;
    // End of variables declaration//GEN-END:variables
}
