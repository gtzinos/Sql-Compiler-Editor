
import cryptography.RSA;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Giwrgos
 */
public class SqlDeveloper extends javax.swing.JFrame {

    /*ALL VARIABLES */
    //all database types
    String[] tableTypes = {"TABLE", "TRIGGER", "TYPE"};

    //result set variable
    static ResultSet result = null;

    //ORACLE DATABASE CONNECTION VARIABLES
    static String OdriverClassName = "oracle.jdbc.OracleDriver";
    static String Ourl = "";
    static Connection OdbConnection = null;
    static String Ousername = "";
    static String Opasswd = "";
    static Statement Ostatement = null;

    //image icon object
    ImageIcon img;
    //opened file path
    String fileName = "";
    //epeita ftiaxnw enan pinaka me osa sosta queries iparxoyn
    String queries[][];
    //query counter
    int queryCounter;
    //configs 
    String configs[];

    //DB TREE HELP VARIABLES
    //DATABASE METADATA
    DatabaseMetaData dbmd;
    //JTREE MODEL
    DefaultTreeModel model;
    //ROOT NODE
    DefaultMutableTreeNode root;
    //TABLE NODES
    DefaultMutableTreeNode tablesTree;
    //PROCEDURES NODES
    DefaultMutableTreeNode proceduresTree;
    //FUNCTIONS NODES
    DefaultMutableTreeNode functionsTree;
    //TRIGGERS NODES
    DefaultMutableTreeNode triggersTree;
    //TYPES NODES
    DefaultMutableTreeNode typesTree;
    //last opened tree node
    String openedTreeNode;
    //last selected tree node
    String selectedTreeNode;
    /*
     Confirm connection methods
     */

    //AN EXOUME SUMPLIRWTHEI OLA TA PEDIA ENERGOPOIEITAI TO CONNECT TO DATABASE
    public void checkConfigs() {
        if (login_username.getText().length() > 0 && login_password.getText().length() > 0 && service_oracle.getText().length() > 0 && server_oracle.getText().length() > 0 && port_oracle.getText().length() > 0) {
            oracle_configs_button.setForeground(Color.BLUE);
        } else {
            oracle_configs_button.setForeground(Color.RED);
        }

        if (oracle_configs_button.getForeground() == Color.BLUE) {
            connect_database_button.setForeground(Color.BLUE);
        } else {
            connect_database_button.setForeground(Color.RED);
        }
    }

    /*
     parsarei diabazei ousiastika to xml file kai to kataxwrei sta config menu
     */
    public void customXMLParser(String file_loc) {
        try {
            File file = new File(file_loc);
            if (file.exists() && file.getTotalSpace() > 0) {
                BufferedReader br = new BufferedReader(new FileReader(file));

                int index = 0;
                String temp = br.readLine();
                configs = new String[4];
                while (temp != null) {
                    temp = temp.substring(temp.indexOf('=') + 1, temp.length());
                    configs[index] = temp;
                    temp = br.readLine();
                    index++;
                }
                br.close();

                login_username.setText(configs[0].toUpperCase());
                user_oracle.setText(configs[0].toUpperCase());

                RSA rsa = new RSA();

                login_password.setText(rsa.getDecrypted());
                pass_oracle.setText(rsa.getDecrypted());

                service_oracle.setText(configs[1]);
                server_oracle.setText(configs[2]);
                port_oracle.setText(configs[3]);

                checkConfigs();
            }

        } catch (Exception e) {

        }
    }

    /*
     diabazei ta config menu kai ta apothikeuei se ena xml file gia thn epomenh fora poy tha anoiksei h efarmogh
     */
    public void customXMLMaker(String file_loc) {
        try {
            File file = new File(file_loc);
            if (file.exists()) {
                file.delete();
            }
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));

            //kriptografisi tou kwdikou
            RSA rsa = new RSA(pass_oracle.getText());

            bw.write("Username=" + user_oracle.getText().toUpperCase());
            bw.newLine();
            bw.write("Service=" + service_oracle.getText());
            bw.newLine();
            bw.write("Server=" + server_oracle.getText());
            bw.newLine();
            bw.write("Port=" + port_oracle.getText());

            bw.close();

            customXMLParser(file_loc);
            checkConfigs();

        } catch (Exception e) {

        }
    }

    /*
     Database connection and  queries results methods
     */
    // h methodos h opoia tha rithmisei katallila oles tis metavlites simfwna me ta config files
    public boolean connectToDatabase() {

        try {

            //oracle rithmiseis
            Ourl = "jdbc:oracle:thin:@" + server_oracle.getText() + ":" + port_oracle.getText() + ":" + service_oracle.getText();
            Ousername = login_username.getText();
            Opasswd = login_password.getText();
            //connect to oracle
            Class.forName(OdriverClassName);
            OdbConnection = DriverManager.getConnection(Ourl, Ousername, Opasswd);
            Ostatement = OdbConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /*
     DESIGN HELP METHODS KAI QUERY TOYS
     */
    //methodos poy deixnei ta history data
    public void showHistory() {
        try {
            //ftiaxnw ena jtable model
            DefaultTableModel model = new DefaultTableModel();
            //ftiaxnw vector ids opou ennow ta columns names
            Vector ids = new Vector();
            //vazw 2 column  me onoma query kai correct
            ids.addElement("Query");
            ids.addElement("Correct");
            //kai ta stelnw sto model
            model.setColumnIdentifiers(ids);
            //thetw to modelo sto jtable
            result_history.setModel(model);
            //data vector
            Vector data = new Vector();
            //an de uparxei history.dll minima kai termatismos methodou
            if (!new File("history.dll").exists()) {
                errors.showMessageDialog(null, "You don't have history data !!!");
                return;
            }
            //open read stream
            BufferedReader br = new BufferedReader(new FileReader(new File("history.dll")));
            //diabazw ta data
            String temp = br.readLine();
            String columns[];
            while (temp != null) {
                columns = temp.split("###");
                data = new Vector();
                //add sto data vector
                data.addElement(columns[0]);
                data.addElement(columns[1]);
                //add sto modelo opou tha pane kai sto j table
                model.addRow(data);
                temp = br.readLine();
            }
            br.close();

            //emfanizw thn forma
            history_data.setVisible(true);
            history_data.setSize(800, 350);

        } catch (Exception e) {

        }
    }

    public void runQueries() {
        error_area.setText(error_area.getText() + "-- SQL Normal Messages --- \n");

        //ektelesi twn queries
        for (int j = 0; j < queryCounter; j++) {
            try {
                //an einai select trexw thn execute query
                if (queries[j][1].compareTo("S") == 0) {
                    result = Ostatement.executeQuery(queries[j][0]);
                    if (result == null || !result.isBeforeFirst()) {
                        error_area.setText(error_area.getText() + "\n " + "Query executed without problems.");
                        error_area.setText(error_area.getText() + "\n " + "No data found.");
                    }
                } //alliws thn execute update
                else {
                    Ostatement.executeUpdate(queries[j][0]);
                    error_area.setText(error_area.getText() + "\n " + Ostatement.getUpdateCount() + " row(s) affected.");
                }

                //epeita emfanizw ta result toy query
                if (result != null && result.isBeforeFirst()) {

                    error_area.setText(error_area.getText() + "\n " + "Query executed without problems.");

                    results.setVisible(true);
                    results.setSize(800, 350);

                    //metadata tou result set
                    ResultSetMetaData rsmd = result.getMetaData();
                    //columns count
                    int columns = rsmd.getColumnCount();

                    //ftiaxnw ena jtable model
                    DefaultTableModel model = new DefaultTableModel();
                    //vector array gia na ilopoihso to model logo oti einai eukolo kai dinamiko
                    Vector columnsName = new Vector();
                    //data types
                    Vector dataTypes = new Vector();
                    //loop gia na parw ta column names
                    for (int i = 1; i <= columns; i++) {
                        columnsName.addElement(rsmd.getColumnName(i));
                    }
                    //set model me xrisi vector
                    model.setColumnIdentifiers(columnsName);

                    result_table.setModel(model);

                    while (result.next()) {
                        dataTypes = new Vector();
                        for (int i = 1; i <= columns; i++) {
                            dataTypes.addElement(result.getString(i));
                        }
                        model.addRow(dataTypes);
                    }
                }
                queries[j][1] = "True";
            } catch (SQLException e) {
                queries[j][1] = "False";
                error_area.setText(error_area.getText() + "\n \n -- SQL Exception --- \n");
                while (e != null) {
                    error_area.setText(error_area.getText() + "\n Message: " + e.getMessage());
                    error_area.setText(error_area.getText() + "\n SQLState: " + e.getSQLState());
                    error_area.setText(error_area.getText() + "\n ErrorCode: " + e.getErrorCode());
                    e = e.getNextException();
                    error_area.setText(error_area.getText() + "\n");
                }
            }

        }
    }

    public void seperateQueries(String query) {

        queryCounter = countChars(';', query);

        //prostasia oste mhpvw to query htane xwris erotimatika
        if (queryCounter == 0) {
            queryCounter++;
        }

        //epeita ftiaxnw enan pinaka me osa sosta queries iparxoyn
        queries = new String[queryCounter][2];

        //index gia kathe fora poy tha mpainei sto loop na kserw o kersoras pou brisketai
        int from = 0;
        //index gia kathe fora poy tha mpainei sto loop na kserw o kersoras pou tha stamathsei otan brei ousiastika to ;
        int to = 0;

        for (int i = 0; i < queryCounter; i++) {
            //psaxnw na brw to prwto erotimatiko
            to = query.indexOf(";", from);
            //se periptwsh pou ksexastike to erwtimatiko tote to kanw na parei to megethos tou text
            if (to == -1) {
                to = query.length();
            }
            //epeita pairnei to kommati pou epilexthike simfwna me thn index of
            queries[i][0] = query.substring(from, to);
            //an iparxei sto kommati auto h leksi select tote milame gia ena execute query 
            if (queries[i][0].contains("select")) {
                queries[i][1] = "S";
            } //alliws gia ena  execute update
            else {
                queries[i][1] = "U";
            }
            //epeita proxoraw to from = to + 1 gia na mhn piasw to erotimatiko
            from = to + 1;
        }
    }

    public void addToHistory() {
        try {
            BufferedWriter br;
            if (!new File("./history.dll").exists()) {
                br = new BufferedWriter(new FileWriter(new File("./history.dll")));
            } else {
                br = new BufferedWriter(new FileWriter(new File("./history.dll"), true));
            }
            for (int i = 0; i < queryCounter; i++) {
                if (queries[i][1].compareTo("True") == 0 || queries[i][1].compareTo("False") == 0) {
                    br.write(queries[i][0] + "###" + queries[i][1]);
                    br.newLine();
                }
            }
            br.close();
        } catch (Exception e) {
        }
    }

    public void runQueryManager(String query) {
        error_area.setText("");

        if (query.length() == 0) {
            if (sql_area.getSelectedText() != null) {
                query = sql_area.getSelectedText();
            } else {
                query = sql_area.getText();
            }
        }
        //gia na ta elenksw xwris 100 sindiasmous kanw thn entolh sthn mnhmh se lower case
        query = query.toLowerCase();
        query = query.replaceAll("\n", "");
        //kalw thn seperate opou tha diaxwrisei ta queries sto pinaka mesa
        seperateQueries(query);

        //kalw thn run queries gia na ta ektelesei
        runQueries();
        addToHistory();
    }

    //methodos poy diaxeirizetai to save as
    public void saveAs() {
        FileWriter fw;
        try {
            //arxika thetw pleon se save to text approve button
            file_chooser.setApproveButtonText("Save");
            //anoigw file chooser
            file_chooser.showOpenDialog(null);
            //an de einai null h epilogh
            if (file_chooser.getSelectedFile() != null) {
                //pairnw to file kai to path
                File file = file_chooser.getSelectedFile();
                String path = file.getAbsolutePath();
                //an to arxeio iparxei proidopoiw ton xristi
                if (file.exists()) {
                    int answer = errors.showConfirmDialog(null, "Υπαρχει ηδη αρχειο με αυτο το ονομα.Θέλετε να συνεχίσετε ? ");
                    // an de einai simfwnow akirwnw thn diadikasia
                    if (answer != errors.YES_OPTION) {
                        return;
                    }
                }

                //alliws proxoraw sthn apothikeysh toy arxeiou me antikatastash paliou
                fw = new FileWriter(path);
                fw.write(sql_area.getText());
                fw.close();
                //kai thetw pleon os neo opened file to path tou save as apo to file chooser
                fileName = path;
            }
        } catch (IOException exc) {
        }
    }

    //methodos pou diaxeirizetai to aplo save opou stelnei an xreiastei sto save as
    public void saveFileManager() {
        FileWriter fw;
        try {
            //an to fileName opoy einai to opened file einai okei iparxei kai de einai empty
            if (fileName.length() > 0 && new File(fileName).exists()) {
                //to apothikeuw
                fw = new FileWriter(fileName);
                fw.write(sql_area.getText());
                fw.close();
            } //alliws kalw thn save as
            else {
                saveAs();
            }

        } catch (IOException exc) {
        }
    }

    /*
     TREE FUNCTIONS HELP ME
     */
    //epistrefei poses fores vrethike enas xaraktiras se ena string
    public int countChars(char character, String text) {
        int counter = 0;
        char array[] = text.toCharArray();
        for (int i = 0; i < array.length; i++) {
            if (array[i] == character) {
                counter++;
            }
        }
        return counter;
    }

    public boolean isTableName(String path) {
        if (path.contains("[" + configs[0] + ", Tables, " + db_tree.getLastSelectedPathComponent())) {
            return true;
        }
        return false;
    }

    //elenxei an enas komvos einai protou epipedou
    public boolean isFirstStageTree(String path) {
        if (countChars(',', path) > 1) {
            return false;
        }
        return true;
    }

    //DIAXIRISI REFRESH ALL NODES
    public void refreshAllNodes() {
        try {

            progress.setValue(0);

            new Thread(new Runnable() {
                public void run() {
                    addNodes(proceduresTree.toString());
                    progress.setValue(progress.getValue() + 20);
                }
            }).start();

            new Thread(new Runnable() {
                public void run() {
                    addNodes(functionsTree.toString());
                    progress.setValue(progress.getValue() + 20);
                }
            }).start();

            new Thread(new Runnable() {
                public void run() {
                    addNodes(triggersTree.toString());
                    progress.setValue(progress.getValue() + 20);
                }
            }).start();

            new Thread(new Runnable() {
                public void run() {
                    addNodes(typesTree.toString());
                    progress.setValue(progress.getValue() + 20);

                }
            }).start();

            new Thread(new Runnable() {
                public void run() {
                    addNodes(tablesTree.toString());
                    progress.setValue(progress.getValue() + 20);
                    messages.showMessageDialog(null, " Ολοκληρο το δεντρο της βασης δεδομενων ενημερωθηκε με επιτυχεια !!!");
                }
            }).start();

        } catch (Exception e) {
            errors.showMessageDialog(null, "Κατι πηγε στραβα. Παρακαλω αναφέρεται το προβλημα στον admin !!!");
            System.out.println(e);
        }

    }

    //DIAXIRISI REFRESH ONE NODE
    public void refreshOneNode() {
        try {
            progress.setValue(0);
            selectedTreeNode = String.valueOf(db_tree.getSelectionPath().getLastPathComponent());
            if (selectedTreeNode != null && selectedTreeNode != "") {
                progress.setValue(progress.getValue() + 50);
                new Thread(new Runnable() {
                    public void run() {
                        if (addNodes(selectedTreeNode)) {
                            progress.setValue(progress.getValue() + 50);
                            messages.showMessageDialog(null, "Το δεντρο του κομβου " + selectedTreeNode + " της βασης δεδομενων ενημερωθηκε με επιτυχεια !!!");
                        }

                    }
                }).start();

            }

        } catch (Exception e) {
            errors.showMessageDialog(null, "Κατι πηγε στραβα. Παρακαλω αναφέρεται το προβλημα στον admin !!!");
            System.out.println(e);
        }
    }

    //add nodes sto tree me bash to result set
    public void addToTree(ResultSet result, DefaultMutableTreeNode treeNode, String arrayIndexName) {
        try {
            treeNode.removeAllChildren();

            while (result.next()) {
                treeNode.add(new DefaultMutableTreeNode(result.getString(arrayIndexName)));
                //System.out.println(result.getString(arrayIndexName));
            }
            model.reload();
        } catch (Exception e) {

        }
    }

    //add nodes sto tree twn procedures me bash to result set
    public void addProcedures(ResultSet result, DefaultMutableTreeNode treeNode, String arrayIndexName) {
        try {
            treeNode.removeAllChildren();

            while (result.next()) {
                if (result.getShort("PROCEDURE_TYPE") == 1) {
                    treeNode.add(new DefaultMutableTreeNode(result.getString(arrayIndexName)));
                }
            }
            model.reload();
        } catch (Exception e) {

        }
    }

    //ADD A TABLE NODE
    public void addTables(ResultSet result, DefaultMutableTreeNode tablesTree) throws Exception {
        try {
            tablesTree.removeAllChildren();
            while (result.next()) {
                String tableName = result.getString("TABLE_NAME");
                DefaultMutableTreeNode tableNode = new DefaultMutableTreeNode(tableName);
                DefaultMutableTreeNode ipofakeloiNode;
                //add onoma pinaka sthn lista
                tablesTree.add(tableNode);

                //add columns node ston kathe pinaka
                ipofakeloiNode = new DefaultMutableTreeNode("Columns");
                tableNode.add(ipofakeloiNode);

                ResultSet tempSet = dbmd.getColumns("%", Ousername, tableName, "%");
                String columnName;
                String columnTN;
                //add columns node ston kathe pinaka
                while (tempSet.next()) {
                    columnName = tempSet.getString("COLUMN_NAME");
                    columnTN = tempSet.getString("TYPE_NAME");
                    ipofakeloiNode.add(new DefaultMutableTreeNode(columnName + " " + columnTN));
                }

                //add primary key node ston kathe pinaka
                ipofakeloiNode = new DefaultMutableTreeNode("Primary Key");
                tableNode.add(ipofakeloiNode);

                tempSet = dbmd.getPrimaryKeys(null, Ousername, tableName);
                //add primary keys nodes ston komvo
                String keyName;
                while (tempSet.next()) {
                    keyName = tempSet.getString("COLUMN_NAME");
                    ipofakeloiNode.add(new DefaultMutableTreeNode(keyName));
                }
                //add primary key node ston kathe pinaka
                ipofakeloiNode = new DefaultMutableTreeNode("Foreign Key");
                tableNode.add(ipofakeloiNode);

                //add primary keys nodes ston komvo
                tempSet = dbmd.getImportedKeys("", Ousername, tableName);
                String fkeyName;
                String refTable;
                while (tempSet.next()) {
                    refTable = tempSet.getString(3);
                    fkeyName = tempSet.getString("FKCOLUMN_NAME");
                    ipofakeloiNode.add(new DefaultMutableTreeNode(fkeyName + " Ref " + refTable));
                }

                //add on update node ston kathe pinaka
                DefaultMutableTreeNode onUpdateNode = new DefaultMutableTreeNode("On Update");
                tableNode.add(onUpdateNode);
                //add on delete node ston kathe pinaka
                DefaultMutableTreeNode onDeleteNode = new DefaultMutableTreeNode("On Delete");
                tableNode.add(onDeleteNode);
                /*
                 ADD ON UPDATE/DELETE NODES
                 */
                tempSet = dbmd.getImportedKeys("%", Ousername, tableName);

                while (tempSet.next()) {

                    if (tempSet.getString("UPDATE_RULE") != null) {
                        onUpdateNode.add(new DefaultMutableTreeNode(tempSet.getString("FKCOLUMN_NAME")));
                    }
                    if (tempSet.getString("DELETE_RULE") != null) {
                        onDeleteNode.add(new DefaultMutableTreeNode(tempSet.getString("FKCOLUMN_NAME")));
                    }
                    /*
                        //  ALL AVAILABLE COLUMNS
                        System.out.println("PKTABLE_CAT " + tempSet.getString("PKTABLE_CAT"));
                        System.out.println("PKTABLE_SCHEM " + tempSet.getString("PKTABLE_SCHEM"));
                        System.out.println("PKTABLE_NAME " + tempSet.getString("PKTABLE_NAME"));
                        System.out.println("PKCOLUMN_NAME " + tempSet.getString("PKCOLUMN_NAME"));
                        System.out.println("FKTABLE_CAT " + tempSet.getString("FKTABLE_CAT"));
                        System.out.println("FKTABLE_SCHEM " + tempSet.getString("FKTABLE_SCHEM"));
                        System.out.println("FKTABLE_NAME " + tempSet.getString("FKTABLE_NAME"));
                        System.out.println("FKCOLUMN_NAME " + tempSet.getString("FKCOLUMN_NAME"));
                        System.out.println("KEY_SEQ " + tempSet.getString("KEY_SEQ"));
                        System.out.println("UPDATE_RULE " + tempSet.getString("UPDATE_RULE"));
                        System.out.println("DELETE_RULE " + tempSet.getString("DELETE_RULE"));
                        System.out.println("FK_NAME " + tempSet.getString("FK_NAME"));
                        System.out.println("PK_NAME " + tempSet.getString("PK_NAME"));
                        System.out.println("DEFERRABILITY " + tempSet.getString("DEFERRABILITY"));
                    */
                }

            }
            model.reload();
        } catch (Exception e) {

        }

    }

    ////KEDRIKI DIAXEIRISI ADD ENA NODE STO TREE ANALOGA TO openedTreeNode
    public boolean addNodes(String openedTreeNode) {
        try {

            switch (openedTreeNode) {
                case "Tables":
                    //vazoume arxika tous pinakes
                    result = dbmd.getTables("%", Ousername, "%", new String[]{tableTypes[0]});
                    addTables(result, tablesTree);
                    break;
                case "Procedures":
                    result = dbmd.getProcedures(null, Ousername, null);
                    addProcedures(result, proceduresTree, "PROCEDURE_NAME");
                    break;
                case "Functions":
                    result = dbmd.getFunctions(null, Ousername, null);
                    addToTree(result, functionsTree, "FUNCTION_NAME");
                    break;
                case "Triggers":
                    result = dbmd.getTables("%", Ousername, "%", new String[]{tableTypes[1]});
                    addToTree(result, triggersTree, "TABLE_NAME");
                    break;
                case "Types":
                    result = dbmd.getTables("%", Ousername, "%", new String[]{tableTypes[2]});
                    addToTree(result, typesTree, "TABLE_NAME");
                    break;
                case "Root":
                    refreshAllNodes();
                    break;
                default:
                    errors.showMessageDialog(null, "Δεν μπορειται να κανετε refresh σε αυτον τον κομβο !!!");
                    return false;
            }

        } catch (Exception e) {

        }
        return true;
    }

    /**
     * Creates new form SqlDeveloper
     */
    public SqlDeveloper() {
        initComponents();

        customXMLParser("./configs/oracle.xml");

        setSize(600, 270);
        setResizable(false);

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        errors = new javax.swing.JOptionPane();
        oracle_configs = new javax.swing.JFrame();
        save_oracle_configs_button = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        user_oracle = new javax.swing.JTextField();
        service_oracle = new javax.swing.JTextField();
        pass_oracle = new javax.swing.JPasswordField();
        jLabel7 = new javax.swing.JLabel();
        server_oracle = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        port_oracle = new javax.swing.JTextField();
        messages = new javax.swing.JOptionPane();
        home_page = new javax.swing.JFrame();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        db_tree = new javax.swing.JTree();
        progress = new javax.swing.JProgressBar();
        jScrollPane3 = new javax.swing.JScrollPane();
        sql_area = new javax.swing.JTextArea();
        jScrollPane4 = new javax.swing.JScrollPane();
        error_area = new javax.swing.JTextArea();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();
        refresh_menu = new javax.swing.JPopupMenu();
        refresh_menu1 = new javax.swing.JMenuItem();
        refresh_all_menu1 = new javax.swing.JMenuItem();
        refresh_all_only = new javax.swing.JPopupMenu();
        refresh_all_menu2 = new javax.swing.JMenuItem();
        aboutDialog = new javax.swing.JDialog();
        jScrollPane2 = new javax.swing.JScrollPane();
        aboutTextArea = new javax.swing.JTextArea();
        results = new javax.swing.JFrame();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        result_table = new javax.swing.JTable();
        jLabel10 = new javax.swing.JLabel();
        file_chooser = new javax.swing.JFileChooser();
        displayTableMenu = new javax.swing.JPopupMenu();
        viewData = new javax.swing.JMenuItem();
        history_data = new javax.swing.JFrame();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        result_history = new javax.swing.JTable();
        jLabel14 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        connect_database_button = new javax.swing.JButton();
        oraclelogo = new javax.swing.JLabel();
        oracle_configs_button = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        login_username = new javax.swing.JTextField();
        login_password = new javax.swing.JPasswordField();

        oracle_configs.setTitle("oracle configs");
        oracle_configs.setResizable(false);

        save_oracle_configs_button.setText("Save");
        save_oracle_configs_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                save_oracle_configs_buttonActionPerformed(evt);
            }
        });

        jLabel1.setText("Username");

        jLabel2.setText("Password");

        jLabel3.setText("Schema");

        service_oracle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                service_oracleActionPerformed(evt);
            }
        });

        jLabel7.setText("Server");

        jLabel8.setText("Port");

        javax.swing.GroupLayout oracle_configsLayout = new javax.swing.GroupLayout(oracle_configs.getContentPane());
        oracle_configs.getContentPane().setLayout(oracle_configsLayout);
        oracle_configsLayout.setHorizontalGroup(
            oracle_configsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(oracle_configsLayout.createSequentialGroup()
                .addGroup(oracle_configsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(oracle_configsLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(save_oracle_configs_button, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(oracle_configsLayout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addGroup(oracle_configsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(oracle_configsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 56, Short.MAX_VALUE)
                                .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(63, 63, 63)
                        .addGroup(oracle_configsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(user_oracle, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(service_oracle)
                            .addComponent(pass_oracle)
                            .addComponent(server_oracle)
                            .addComponent(port_oracle, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE))))
                .addContainerGap(30, Short.MAX_VALUE))
        );
        oracle_configsLayout.setVerticalGroup(
            oracle_configsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, oracle_configsLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(oracle_configsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(user_oracle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(oracle_configsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(pass_oracle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(oracle_configsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(service_oracle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(oracle_configsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(server_oracle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(oracle_configsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(port_oracle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(save_oracle_configs_button)
                .addContainerGap(27, Short.MAX_VALUE))
        );

        home_page.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        home_page.setTitle("Sql Admin Editor");
        home_page.setResizable(false);

        jPanel3.setBackground(new java.awt.Color(255, 255, 153));

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        javax.swing.tree.DefaultMutableTreeNode treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Tables");
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Procedures");
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Functions");
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Triggers");
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Types");
        treeNode1.add(treeNode2);
        db_tree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        db_tree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                db_treeMouseReleased(evt);
            }
        });
        db_tree.addTreeExpansionListener(new javax.swing.event.TreeExpansionListener() {
            public void treeCollapsed(javax.swing.event.TreeExpansionEvent evt) {
            }
            public void treeExpanded(javax.swing.event.TreeExpansionEvent evt) {
                db_treeTreeExpanded(evt);
            }
        });
        db_tree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                db_treeValueChanged(evt);
            }
        });
        db_tree.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                db_treeFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                db_treeFocusLost(evt);
            }
        });
        jScrollPane1.setViewportView(db_tree);

        progress.setStringPainted(true);

        sql_area.setColumns(20);
        sql_area.setRows(5);
        jScrollPane3.setViewportView(sql_area);

        error_area.setColumns(20);
        error_area.setRows(5);
        error_area.setEnabled(false);
        jScrollPane4.setViewportView(error_area);

        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Knob Play.png"))); // NOI18N
        jLabel4.setToolTipText("Run Statement");
        jLabel4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel4MouseClicked(evt);
            }
        });
        jLabel4.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                jLabel4AncestorAdded(evt);
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });

        jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Knob Loop Off.png"))); // NOI18N
        jLabel5.setToolTipText("Refresh");
        jLabel5.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel5MouseClicked(evt);
            }
        });
        jLabel5.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                jLabel5AncestorAdded(evt);
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });

        jLabel11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Knob Shuffle Off.png"))); // NOI18N
        jLabel11.setToolTipText("Clean Messages");
        jLabel11.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel11MouseClicked(evt);
            }
        });
        jLabel11.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                jLabel11AncestorAdded(evt);
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });

        jLabel12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/cd_blue.png"))); // NOI18N
        jLabel12.setToolTipText("History");
        jLabel12.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel12MouseClicked(evt);
            }
        });
        jLabel12.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                jLabel12AncestorAdded(evt);
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });

        jLabel13.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Knob Cancel.png"))); // NOI18N
        jLabel13.setToolTipText("Clean History");
        jLabel13.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel13MouseClicked(evt);
            }
        });
        jLabel13.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                jLabel13AncestorAdded(evt);
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(progress, javax.swing.GroupLayout.PREFERRED_SIZE, 309, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 307, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 590, Short.MAX_VALUE)
                    .addComponent(jScrollPane4)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel13)))
                .addGap(0, 47, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel11, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel13, javax.swing.GroupLayout.Alignment.TRAILING))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 434, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(34, 34, 34))
        );

        jMenu1.setText("File");

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem1.setText("Open");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem4.setText("Close");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem4);

        jSeparator1.setToolTipText("");
        jMenu1.add(jSeparator1);

        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem2.setText("Save");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuItem3.setText("Save As");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem3);

        jSeparator2.setToolTipText("");
        jMenu1.add(jSeparator2);

        jMenuItem5.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem5.setText("Exit");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem5);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Help");

        jMenuItem6.setText("About");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem6);

        jMenuBar1.add(jMenu2);

        home_page.setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout home_pageLayout = new javax.swing.GroupLayout(home_page.getContentPane());
        home_page.getContentPane().setLayout(home_pageLayout);
        home_pageLayout.setHorizontalGroup(
            home_pageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(home_pageLayout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        home_pageLayout.setVerticalGroup(
            home_pageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(home_pageLayout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 634, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 30, Short.MAX_VALUE))
        );

        refresh_menu1.setText("Refresh");
        refresh_menu1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refresh_menu1ActionPerformed(evt);
            }
        });
        refresh_menu.add(refresh_menu1);

        refresh_all_menu1.setText("Refresh All");
        refresh_all_menu1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refresh_all_menu1ActionPerformed(evt);
            }
        });
        refresh_menu.add(refresh_all_menu1);

        refresh_all_menu2.setText("Refresh All");
        refresh_all_menu2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refresh_all_menu2ActionPerformed(evt);
            }
        });
        refresh_all_only.add(refresh_all_menu2);

        aboutDialog.setTitle("About");
        aboutDialog.setResizable(false);

        aboutTextArea.setEditable(false);
        aboutTextArea.setBackground(new java.awt.Color(255, 255, 153));
        aboutTextArea.setColumns(20);
        aboutTextArea.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        aboutTextArea.setLineWrap(true);
        aboutTextArea.setRows(5);
        aboutTextArea.setText("Name : George\nSurname : Tzinos\nBorn : 1994\nA.M : 123896\nContact : gtzinos@it.teithe.gr\nWebSite : aetos.it.teithe.gr/~gtzinos");
        jScrollPane2.setViewportView(aboutTextArea);

        javax.swing.GroupLayout aboutDialogLayout = new javax.swing.GroupLayout(aboutDialog.getContentPane());
        aboutDialog.getContentPane().setLayout(aboutDialogLayout);
        aboutDialogLayout.setHorizontalGroup(
            aboutDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        aboutDialogLayout.setVerticalGroup(
            aboutDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );

        results.setTitle("Sql Results");
        results.setResizable(false);

        jPanel1.setBackground(new java.awt.Color(255, 255, 153));
        jPanel1.setForeground(new java.awt.Color(255, 255, 153));

        result_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane5.setViewportView(result_table);

        jLabel10.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel10.setText("Query Results");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(312, 312, 312)
                        .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(34, 34, 34)
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 722, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(73, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(49, 49, 49))
        );

        javax.swing.GroupLayout resultsLayout = new javax.swing.GroupLayout(results.getContentPane());
        results.getContentPane().setLayout(resultsLayout);
        resultsLayout.setHorizontalGroup(
            resultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(resultsLayout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 54, Short.MAX_VALUE))
        );
        resultsLayout.setVerticalGroup(
            resultsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(resultsLayout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 174, Short.MAX_VALUE))
        );

        viewData.setText("View Data");
        viewData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewDataActionPerformed(evt);
            }
        });
        displayTableMenu.add(viewData);

        history_data.setTitle("History Data");
        history_data.setResizable(false);

        jPanel4.setBackground(new java.awt.Color(255, 255, 153));
        jPanel4.setForeground(new java.awt.Color(255, 255, 153));

        result_history.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane6.setViewportView(result_history);

        jLabel14.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel14.setText("History Data");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(312, 312, 312)
                        .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(34, 34, 34)
                        .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 722, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(73, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(49, 49, 49))
        );

        javax.swing.GroupLayout history_dataLayout = new javax.swing.GroupLayout(history_data.getContentPane());
        history_data.getContentPane().setLayout(history_dataLayout);
        history_dataLayout.setHorizontalGroup(
            history_dataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(history_dataLayout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 54, Short.MAX_VALUE))
        );
        history_dataLayout.setVerticalGroup(
            history_dataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(history_dataLayout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 174, Short.MAX_VALUE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Sql Admin Editor");

        jPanel2.setBackground(new java.awt.Color(255, 255, 102));

        connect_database_button.setForeground(new java.awt.Color(255, 0, 0));
        connect_database_button.setText("Login");
        connect_database_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connect_database_buttonActionPerformed(evt);
            }
        });

        oraclelogo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/oracle.png"))); // NOI18N

        oracle_configs_button.setForeground(new java.awt.Color(255, 0, 0));
        oracle_configs_button.setText("Configs");
        oracle_configs_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                oracle_configs_buttonActionPerformed(evt);
            }
        });

        jLabel6.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel6.setText("Username :");

        jLabel9.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jLabel9.setText("Password : ");

        login_username.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                login_usernameCaretUpdate(evt);
            }
        });

        login_password.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                login_passwordCaretUpdate(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addGap(18, 18, 18)
                                .addComponent(login_username, javax.swing.GroupLayout.PREFERRED_SIZE, 194, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel9)
                                .addGap(18, 18, 18)
                                .addComponent(login_password))
                            .addComponent(connect_database_button, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(oraclelogo, javax.swing.GroupLayout.PREFERRED_SIZE, 324, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 178, Short.MAX_VALUE)
                        .addComponent(oracle_configs_button, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(50, 50, 50))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(41, 41, 41)
                        .addComponent(oracle_configs_button))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(oraclelogo, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(34, 34, 34)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(login_username, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(login_password, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(connect_database_button)
                .addContainerGap(56, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void oracle_configs_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_oracle_configs_buttonActionPerformed
        // TODO add your handling code here:
        customXMLParser("./configs/oracle.xml");
        oracle_configs.setVisible(true);
        oracle_configs.setSize(340, 310);
    }//GEN-LAST:event_oracle_configs_buttonActionPerformed

    private void save_oracle_configs_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_save_oracle_configs_buttonActionPerformed
        // TODO add your handling code here:
        customXMLMaker("./configs/oracle.xml");
        oracle_configs.setVisible(false);
    }//GEN-LAST:event_save_oracle_configs_buttonActionPerformed

    private void service_oracleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_service_oracleActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_service_oracleActionPerformed

    private void connect_database_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connect_database_buttonActionPerformed
        // TODO add your handling code here:
        if (oracle_configs_button.getForeground() == Color.BLUE) {
            if (connectToDatabase()) {
                //prin kleisei kentriko frame an kai logo koinou thread tha treksei prwtos o kwdikas kai meta oi allages sta frame
                String username = user_oracle.getText();
                setVisible(false);
                home_page.setSize(930, 680);
                home_page.setVisible(true);
                try {

                    dbmd = OdbConnection.getMetaData();

                    /*
                     initialize tree variables
                     */
                    model = (DefaultTreeModel) db_tree.getModel();

                    root = (DefaultMutableTreeNode) model.getRoot();
                    root.setUserObject(username);

                    tablesTree = (DefaultMutableTreeNode) root.getFirstChild();

                    proceduresTree = (DefaultMutableTreeNode) tablesTree.getNextNode();

                    functionsTree = (DefaultMutableTreeNode) proceduresTree.getNextNode();

                    triggersTree = (DefaultMutableTreeNode) functionsTree.getNextNode();

                    typesTree = (DefaultMutableTreeNode) triggersTree.getNextNode();

                    /*
                     refresh all tree
                     */
                    messages.showMessageDialog(null, "Connected Succefully !!! Welcome to Sql Admin Editor !!!");
                    refreshAllNodes();
                } catch (Exception e) {
                }

            } else {
                errors.showMessageDialog(null, "Something going wrong with your configs.Check them and try again !!!");
            }
        } else {
            errors.showMessageDialog(null, "You need to make your configs for oracle and postgresql first !!!");
        }
    }//GEN-LAST:event_connect_database_buttonActionPerformed

    private void db_treeTreeExpanded(javax.swing.event.TreeExpansionEvent evt) {//GEN-FIRST:event_db_treeTreeExpanded
        // TODO add your handling code here:

        //kata to anoigma enow tree node ekane kai automata refresh alla to metaniwsa
        /*
        
         try {
         openedTreeNode = String.valueOf(evt.getPath().getLastPathComponent());
         if (openedTreeNode != null && openedTreeNode != "") {

         addNodes(openedTreeNode);
         } else {
         errors.showMessageDialog(null, "Κατι πηγε στραβα. Παρακαλω αναφέρεται το προβλημα στον admin !!!");
         }
         } catch (Exception e) {
         errors.showMessageDialog(null, "Κατι πηγε στραβα. Παρακαλω αναφέρεται το προβλημα στον admin !!!");
         System.out.println(e);
         }
         */
    }//GEN-LAST:event_db_treeTreeExpanded

    private void db_treeMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_db_treeMouseReleased
        // TODO add your handling code here:
        try {

            if (evt.isPopupTrigger()) {

                //an de exei epilegei tipota apo to menu tote tha emfanisei mono to refresh all menu
                String node = String.valueOf(db_tree.getSelectionPath());

                if (db_tree.isSelectionEmpty() || (!isTableName(node) && !isFirstStageTree(node))) {
                    refresh_all_only.show(evt.getComponent(), evt.getX(), evt.getY());
                } //alliws kai thn dinatothta refresh enos pediou
                else if (!db_tree.isSelectionEmpty() && isTableName(node)) {
                    displayTableMenu.show(evt.getComponent(), evt.getX(), evt.getY());
                } else if (!db_tree.isSelectionEmpty() && isFirstStageTree(node)) {
                    refresh_menu.show(evt.getComponent(), evt.getX(), evt.getY());
                }

            }
        } catch (Exception e) {
            errors.showMessageDialog(null, "Κατι πηγε στραβα. Παρακαλω αναφέρεται το προβλημα στον admin !!!");
            System.out.println(e);
        }

    }//GEN-LAST:event_db_treeMouseReleased

    private void refresh_menu1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refresh_menu1ActionPerformed
        // TODO add your handling code here:
        refreshOneNode();

    }//GEN-LAST:event_refresh_menu1ActionPerformed

    private void refresh_all_menu1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refresh_all_menu1ActionPerformed
        // TODO add your handling code here:'
        refreshAllNodes();
    }//GEN-LAST:event_refresh_all_menu1ActionPerformed

    private void refresh_all_menu2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refresh_all_menu2ActionPerformed
        // TODO add your handling code here:
        refreshAllNodes();
    }//GEN-LAST:event_refresh_all_menu2ActionPerformed

    private void db_treeFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_db_treeFocusGained

    }//GEN-LAST:event_db_treeFocusGained

    private void db_treeFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_db_treeFocusLost

    }//GEN-LAST:event_db_treeFocusLost

    private void db_treeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_db_treeValueChanged
        // TODO add your handling code here:

    }//GEN-LAST:event_db_treeValueChanged

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:

        //open file chooser
        file_chooser.setApproveButtonText("Open");
        file_chooser.showOpenDialog(null);
        //an de einai null h epilogh
        if (file_chooser.getSelectedFile() != null) {
            File file = file_chooser.getSelectedFile();
            String path = file.getAbsolutePath();

            try {

                FileReader fw = new FileReader(path);
                sql_area.read(fw, null);
                fw.close();

                fileName = path;
            } catch (IOException exc) {
                errors.showMessageDialog(null, "Κατι πηγε στραβα με το αρχειο !!!");
            }
        }
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        // TODO add your handling code here:
        aboutDialog.setVisible(true);
        aboutDialog.setSize(270, 150);
    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        // TODO add your handling code here:
        if (sql_area.getText().length() == 0) {
            fileName = "";
        } else {
            saveFileManager();
        }

    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        // TODO add your handling code here:
        saveFileManager();

    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        saveAs();
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        System.exit(0);
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jLabel4AncestorAdded(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_jLabel4AncestorAdded
        // TODO add your handling code here:

    }//GEN-LAST:event_jLabel4AncestorAdded

    private void jLabel5AncestorAdded(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_jLabel5AncestorAdded
        // TODO add your handling code here:

    }//GEN-LAST:event_jLabel5AncestorAdded

    private void jLabel5MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel5MouseClicked
        // TODO add your handling code here:
        try {

            jLabel5.setBorder(BorderFactory.createLineBorder(Color.black));

            //an de exei epilegei tipota apo to menu tote tha emfanisei mono to refresh all menu
            new Thread(new Runnable() {
                public void run() {
                    String node = String.valueOf(db_tree.getSelectionPath());
                    if (db_tree.isSelectionEmpty() || !isFirstStageTree(node)) {
                        refreshAllNodes();
                    } //alliws kai thn dinatothta refresh enos pediou
                    else if (isFirstStageTree(node)) {
                        refreshOneNode();
                    }
                    jLabel5.setBorder(BorderFactory.createEmptyBorder());
                }
            }).start();

        } catch (Exception e) {
            errors.showMessageDialog(null, "Κατι πηγε στραβα. Παρακαλω αναφέρεται το προβλημα στον admin !!!");
            System.out.println(e);
        }
    }//GEN-LAST:event_jLabel5MouseClicked

    private void jLabel4MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel4MouseClicked
        // TODO add your handling code here:
        runQueryManager("");


    }//GEN-LAST:event_jLabel4MouseClicked

    private void viewDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewDataActionPerformed
        // TODO add your handling code here:
        if (!db_tree.isSelectionEmpty()) {
            String query = "select * from " + db_tree.getLastSelectedPathComponent();
            runQueryManager(query);
        }
    }//GEN-LAST:event_viewDataActionPerformed

    private void jLabel11MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel11MouseClicked
        // TODO add your handling code here:
        error_area.setText("");
    }//GEN-LAST:event_jLabel11MouseClicked

    private void jLabel11AncestorAdded(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_jLabel11AncestorAdded
        // TODO add your handling code here:
    }//GEN-LAST:event_jLabel11AncestorAdded

    private void jLabel12MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel12MouseClicked
        // TODO add your handling code here:
        showHistory();
    }//GEN-LAST:event_jLabel12MouseClicked

    private void jLabel12AncestorAdded(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_jLabel12AncestorAdded
        // TODO add your handling code here:
    }//GEN-LAST:event_jLabel12AncestorAdded

    private void jLabel13MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel13MouseClicked
        // TODO add your handling code here:

        int anwser = messages.showConfirmDialog(null, "You want to delete all history data ?");
        if (anwser != messages.YES_OPTION) {
            return;
        }
        if (new File("history.dll").exists()) {
            new File("history.dll").delete();
        }
        messages.showMessageDialog(null, "History deleted successfully !!!");


    }//GEN-LAST:event_jLabel13MouseClicked

    private void jLabel13AncestorAdded(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_jLabel13AncestorAdded
        // TODO add your handling code here:
    }//GEN-LAST:event_jLabel13AncestorAdded

    private void login_usernameCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_login_usernameCaretUpdate
        // TODO add your handling code here:
        checkConfigs();
    }//GEN-LAST:event_login_usernameCaretUpdate

    private void login_passwordCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_login_passwordCaretUpdate
        // TODO add your handling code here
        checkConfigs();
    }//GEN-LAST:event_login_passwordCaretUpdate

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
            java.util.logging.Logger.getLogger(SqlDeveloper.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(SqlDeveloper.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(SqlDeveloper.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(SqlDeveloper.class
                    .getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new SqlDeveloper().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JDialog aboutDialog;
    private javax.swing.JTextArea aboutTextArea;
    private javax.swing.JButton connect_database_button;
    private javax.swing.JTree db_tree;
    private javax.swing.JPopupMenu displayTableMenu;
    private javax.swing.JTextArea error_area;
    private javax.swing.JOptionPane errors;
    private javax.swing.JFileChooser file_chooser;
    private javax.swing.JFrame history_data;
    private javax.swing.JFrame home_page;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPasswordField login_password;
    private javax.swing.JTextField login_username;
    private javax.swing.JOptionPane messages;
    private javax.swing.JFrame oracle_configs;
    private javax.swing.JButton oracle_configs_button;
    private javax.swing.JLabel oraclelogo;
    private javax.swing.JPasswordField pass_oracle;
    private javax.swing.JTextField port_oracle;
    private javax.swing.JProgressBar progress;
    private javax.swing.JMenuItem refresh_all_menu1;
    private javax.swing.JMenuItem refresh_all_menu2;
    private javax.swing.JPopupMenu refresh_all_only;
    private javax.swing.JPopupMenu refresh_menu;
    private javax.swing.JMenuItem refresh_menu1;
    private javax.swing.JTable result_history;
    private javax.swing.JTable result_table;
    private javax.swing.JFrame results;
    private javax.swing.JButton save_oracle_configs_button;
    private javax.swing.JTextField server_oracle;
    private javax.swing.JTextField service_oracle;
    private javax.swing.JTextArea sql_area;
    private javax.swing.JTextField user_oracle;
    private javax.swing.JMenuItem viewData;
    // End of variables declaration//GEN-END:variables
}
