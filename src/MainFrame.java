
import com.formdev.flatlaf.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
/**
 *
 * @author Romeo
 */
public class MainFrame extends javax.swing.JFrame {

    private Main main;
    private boolean initFinished = false;
    private int prevSectionIndex;
    private String prevSelectedItem;
    private ArrayList<String> savedListString = new ArrayList<>();

    private ArrayList<String> filteredListString = new ArrayList<>();
    private int page;
    private int maxPages;
    private ArrayList<Folder> selectedItem = new ArrayList<>();
    private ArrayList<JPanel> alModItem = new ArrayList<>();

    private Color installedColor = new Color(44, 238, 144);
    private Color partialColor = new Color(255, 153, 19);
    private Color normalColor = new Color(255, 109, 106);
    
    private ArrayList<GBFileData> alGBFileData = new ArrayList<>();

    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        initComponents();

        InitUI();
        Init();
    }

    private void InitUI() {
        categorySelector.addItem("Skin");
        categorySelector.addItem("Mod");
        categorySelector.addItem("Installed");
        categorySelector.setSelectedIndex(0);

        sectionSelector.addItem("Character");
        sectionSelector.addItem("Weapon");
        sectionSelector.addItem("All");
        prevSectionIndex = 0;
        sectionSelector.setSelectedIndex(prevSectionIndex);

        prevSelectedItem = "";

        alModItem.add(modItem1);
        modItem1.setVisible(false);
        alModItem.add(modItem2);
        modItem2.setVisible(false);
        alModItem.add(modItem3);
        modItem3.setVisible(false);
        alModItem.add(modItem4);
        modItem4.setVisible(false);

        this.setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    ArrayList<File> alFiles = new ArrayList<>();
                    for(File file : droppedFiles) {
                        if(file.isDirectory()) {
                            alFiles.add(file);
                        } else {
                            String name = file.getName();
                            if(name.endsWith(".zip") || name.endsWith(".7z") || name.endsWith(".rar")) {
                                alFiles.add(file);
                            }
                        }
                    }
                    if(!alFiles.isEmpty()) {
                        copy(alFiles, evt);
                    }
                } catch(UnsupportedFlavorException | IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    private void Init() {
        main = new Main(loadingBar, loadingLabel, reloadLabel, this);

        autoInstallSetting.setSelected(main.getAutoInstall());
        overwriteInstallSetting.setSelected(main.getOverwriteInstall());
        overwriteInstallSetting.setEnabled(main.getAutoInstall());

        if(main.getModDir() != null && main.getModDir().isDirectory()) {
            cachedFolderLabel.setText(main.getModDir().getPath());
        } else {
            cachedFolderLabel.setText("No folder selected");
        }
        if(main.getMigotoModDir() != null && main.getMigotoModDir().isDirectory()) {
            installedFolderLabel.setText(main.getMigotoModDir().getPath());
        } else {
            installedFolderLabel.setText("No folder selected");
        }
        if(main.getMigotoShaderFixDir() != null && main.getMigotoShaderFixDir().isDirectory()) {
            installedShaderFixLabel.setText(main.getMigotoShaderFixDir().getPath());
        } else {
            installedShaderFixLabel.setText("No folder selected");
        }


        /*System.out.println("# Print begin =========");
        // main.printFolder(main.getModFolderStructure());
        // main.printFolder(main.getMigotoModFolderStructure());
        // main.printFolder(main.getMigotoShaderFixFolderStructure());
        System.out.println("# Print end =========");*/
        // System.out.println("Init reload");
        
        reload(true);
        
        /*ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            reload(false);
            this.setEnabled(false);
            
            String modID = "https://gamebanana.com/mods/476816";
            String location = "C:\\Users\\Admin\\Desktop\\Genshin Mod Manager\\temp";
            ArrayList<GBFileData> alGBFileData = new GBFileGetter().getFileData(modID);
            
            GBFileData selectedFile = alGBFileData.get(0);
            
            loadingLabel.setText("Downloading...");
            Download download = new Download(location, selectedFile.getFileName(), selectedFile.getDownloadURL(), loadingBar, loadingLabel);
            loadingLabel.setText("Done downloading");
            
            this.setEnabled(true);
        });
        executor.shutdown();*/
    }

    public void reload(boolean useThreading) {
        if(useThreading) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                reloadHelper();
            });
            executor.shutdown();
        } else {
            reloadHelper();
        }
    }

    private void reloadHelper() {
        this.setEnabled(false);
        String reloadStr = main.fullReload();
        updateUI();
        initFinished = true;
        this.setEnabled(true);

        if(!reloadStr.isEmpty()) { // If error returned make popup
            reloadStr += "\nPlease assign them in the settings menu!";
            JOptionPane.showMessageDialog(null, reloadStr, "Reload error!", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void install(Folder folder) {
        long installStartTime = System.nanoTime();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            this.setEnabled(false);
            main.installMod(folder, loadingBar, loadingLabel);
            // System.out.println("Install reload");
            reload(false);
            this.setEnabled(true);

            long installEndTime = System.nanoTime();
            loadingLabel.setText("Installing took: " + (double) (Math.round((installEndTime - installStartTime) / 10000.0) / 100.0) + " ms");
        });
        executor.shutdown();
    }

    private void copy(ArrayList<File> alFiles, DropTargetDropEvent evt) {
        long copyStartTime = System.nanoTime();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            copyHelper(alFiles);
            evt.dropComplete(true);

            // System.out.println("Copy reload");
            reload(false);
            long copyEndTime = System.nanoTime();
            String text = "Importing took: ";
            if(main.getAutoInstall()) {
                text = "Importing and Installing took: ";
            }
            loadingLabel.setText(text + (double) (Math.round((copyEndTime - copyStartTime) / 10000.0) / 100.0) + " ms");
        });
        executor.shutdown();
    }

    private void copy(ArrayList<File> alFiles) {
        long copyStartTime = System.nanoTime();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            copyHelper(alFiles);

            // System.out.println("Copy reload");
            reload(false);
            long copyEndTime = System.nanoTime();
            String text = "Importing took: ";
            if(main.getAutoInstall()) {
                text = "Importing and Installing took: ";
            }
            loadingLabel.setText(text + (double) (Math.round((copyEndTime - copyStartTime) / 10000.0) / 100.0) + " ms");
        });
        executor.shutdown();
    }
    
    private void copyDownloaded(File file) {
        long copyStartTime = System.nanoTime();
        
        ArrayList<File> alFiles = new ArrayList<>();
        alFiles.add(file);
        copyHelper(alFiles);

        // System.out.println("Copy reload");
        reload(false);
        long copyEndTime = System.nanoTime();
        String text = "Downloading, Importing took: ";
        if(main.getAutoInstall()) {
            text = "Downloading, Importing and Installing took: ";
        }
        loadingLabel.setText(text + (double) (Math.round((copyEndTime - copyStartTime) / 10000.0) / 100.0) + " ms");
    }

    private void copyHelper(ArrayList<File> alFiles) {
        this.setEnabled(false);
        boolean install = categorySelector.getSelectedItem().equals("Installed");

        for(File file : alFiles) {
            if(file.isFile()) {
                loadingLabel.setText("Extracting file... (Sorry no progress bar)");
                loadingBar.setValue(0);
            }
            main.copyMod(file, loadingBar, jLabel1);
        }

        this.setEnabled(true);
    }

    private void download(GBFileData selectedFile) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            this.setEnabled(false);
            
            String location = main.getTempPath().getPath();
            
            loadingLabel.setText("Download pending...");
            Download download = new Download(location, selectedFile.getFileName(), selectedFile.getDownloadURL(), loadingBar, loadingLabel);
            
            if(download.getFile().exists()) {
                loadingLabel.setText("Done downloading");
                copyDownloaded(download.getFile());
                download.getFile().delete();
            } else {
                loadingLabel.setText(download.getErrorMsg());
            }
            this.setEnabled(true);
        });
        executor.shutdown();
    }
    
    public void updateUI() {
        String catString = "";
        String secString = "";
        if(categorySelector.getSelectedItem() != null) {
            catString = categorySelector.getSelectedItem().toString();
        }
        if(sectionSelector.getSelectedItem() != null) {
            secString = sectionSelector.getSelectedItem().toString();
        }

        savedListString = main.filterFolderStructure(catString, secString);

        prevSelectedItem = modList.getSelectedValue();
        if(prevSelectedItem == null) {
            prevSelectedItem = "";
        } else if(catString.equals("Skin")) {
            prevSelectedItem = prevSelectedItem.replaceAll("\\s*\\([^\\)]*\\)\\s*", "");
        }

        if(searchField.getText().isEmpty()) {
            modList.setListData(savedListString.toArray(new String[0]));
            filteredListString = savedListString;
        } else {
            searchList(searchField.getText());
        }

        if(!prevSelectedItem.isEmpty()) {
            for(int i = 0; i < filteredListString.size(); i++) {
                if(filteredListString.get(i).contains(prevSelectedItem)) {
                    modList.setSelectedValue(filteredListString.get(i), true);
                    break;
                }
            }
        }

        selectItemFromList();
    }

    private void searchList(String keyword) {
        filteredListString = main.searchKeyword(savedListString, keyword);
        modList.setListData(filteredListString.toArray(new String[0]));
    }

    private void selectItemFromList() {
        String name = modList.getSelectedValue();
        selectedItem = new ArrayList<>();

        if(name != null) {
            if(categorySelector.getSelectedItem().equals("Skin")) {
                name = name.replaceAll("\\s*\\([^\\)]*\\)\\s*", "");
                selectedItem = main.getFoldersFromCachedHashMapWithName(name);
            } else if(categorySelector.getSelectedItem().equals("Mod")) {
                selectedItem.add(main.getFolderFromModAl(name));
            } else if(categorySelector.getSelectedItem().equals("Installed")) {
                selectedItem.add(main.getFolderFromInstalledHashMapWithName(name));
            }

            maxPages = (int) Math.ceil(selectedItem.size() / 4.0);
            if(page > maxPages) {
                page = maxPages;
            } else if(page < 1) {
                page = 1;
            }
        } else {
            page = 1;
            maxPages = 1;
        }

        scrollPage(0);
    }

    private void scrollPage(int direction) {
        if(direction == 1) {
            if(page < maxPages) {
                page++;
            }
        } else if(direction == -1) {
            if(page > 1) {
                page--;
            }
        }

        pageLabel.setText(page + "/" + maxPages);

        if(!selectedItem.isEmpty()) {
            for(int i = 0; i < 4; i++) {
                Folder folder = null;

                int index = i + (page - 1) * 4;
                if(selectedItem.size() > index && index >= 0 && !selectedItem.isEmpty()) {
                    folder = selectedItem.get(index);
                } else {
                    alModItem.get(i).setVisible(false);

                    continue;
                }
                if(folder == null) {
                    System.err.println(modList.getSelectedValue() + " folder was null (scrollPage)");
                    alModItem.get(i).setVisible(false);

                    continue;
                }

                String modName = folder.getModName();

                File configFile = folder.getConfigFile();
                String configName = "";
                if(configFile != null && configFile.exists()) {
                    configName = configFile.getName();
                } else if(folder.isShaderFixOnly()) {
                    configName = "Only shader fixes";
                }

                File previewFile = folder.getPreview();
                BufferedImage bufferedImage = null;
                Image previewImage = null;
                if(previewFile != null && previewFile.exists()) {
                    try {
                        bufferedImage = ImageIO.read(previewFile);
                        int imageWidth = ((double) bufferedImage.getWidth() / imageLabel1.getWidth() > (double) bufferedImage.getHeight() / imageLabel1.getHeight()) ? imageLabel1.getWidth() : -1;
                        int imageHeight = (imageWidth == -1) ? imageLabel1.getHeight() : -1;
                        previewImage = bufferedImage.getScaledInstance(imageWidth, imageHeight, BufferedImage.SCALE_AREA_AVERAGING);
                    } catch(IOException ex) {
                        Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                boolean cached = folder.isCached();
                boolean installed = folder.isInstalled();
                boolean partial = folder.isPartial();
                Color loadColor = normalColor;
                String loadText = "Disabled";

                if(installed) {
                    loadColor = installedColor;
                    loadText = "Enabled";
                }
                if(partial) {
                    loadColor = partialColor;
                    loadText = "Partial";
                }

                Font labelFont = new Font("Sogoe UI", Font.PLAIN, 12);

                switch(i) {
                    case 0 -> {
                        JLabel label = modLabel1;
                        modLabel1.setFont(calculateMaxFontSize(label, labelFont, modName));

                        modLabel1.setText(modName);
                        iniLabel1.setText(configName);
                        shaderFixesBox1.setSelected(folder.hasShaderFixes());

                        loadModButton1.setText(loadText);
                        loadModButton1.setBackground(loadColor);

                        loadModButton1.setSelected(installed);
                        cachedBox1.setSelected(cached);

                        if(previewImage != null) {
                            imageLabel1.setIcon(new ImageIcon(previewImage));
                            imageLabel1.setText("");
                        } else {
                            imageLabel1.setIcon(null);
                            imageLabel1.setText("No preview image!");
                        }
                    }
                    case 1 -> {
                        JLabel label = modLabel2;
                        modLabel2.setFont(calculateMaxFontSize(label, labelFont, modName));

                        modLabel2.setText(modName);
                        iniLabel2.setText(configName);
                        shaderFixesBox2.setSelected(folder.hasShaderFixes());

                        loadModButton2.setText(loadText);
                        loadModButton2.setBackground(loadColor);

                        loadModButton2.setSelected(installed);
                        cachedBox2.setSelected(cached);

                        if(previewImage != null) {
                            imageLabel2.setIcon(new ImageIcon(previewImage));
                            imageLabel2.setText("");
                        } else {
                            imageLabel2.setIcon(null);
                            imageLabel2.setText("No preview image!");
                        }
                    }
                    case 2 -> {
                        JLabel label = modLabel3;
                        modLabel3.setFont(calculateMaxFontSize(label, labelFont, modName));

                        modLabel3.setText(modName);
                        iniLabel3.setText(configName);
                        shaderFixesBox3.setSelected(folder.hasShaderFixes());

                        loadModButton3.setText(loadText);
                        loadModButton3.setBackground(loadColor);

                        loadModButton3.setSelected(installed);
                        cachedBox3.setSelected(cached);

                        if(previewImage != null) {
                            imageLabel3.setIcon(new ImageIcon(previewImage));
                            imageLabel3.setText("");
                        } else {
                            imageLabel3.setIcon(null);
                            imageLabel3.setText("No preview image!");
                        }
                    }
                    case 3 -> {
                        JLabel label = modLabel4;
                        modLabel4.setFont(calculateMaxFontSize(label, labelFont, modName));

                        modLabel4.setText(modName);
                        iniLabel4.setText(configName);
                        shaderFixesBox4.setSelected(folder.hasShaderFixes());

                        loadModButton4.setText(loadText);
                        loadModButton4.setBackground(loadColor);

                        loadModButton4.setSelected(installed);
                        cachedBox4.setSelected(cached);

                        if(previewImage != null) {
                            imageLabel4.setIcon(new ImageIcon(previewImage));
                            imageLabel4.setText("");
                        } else {
                            imageLabel4.setIcon(null);
                            imageLabel4.setText("No preview image!");
                        }
                    }
                    default -> {
                    }
                }

                alModItem.get(i).setVisible(true);
            }
        } else {
            for(int i = 0; i < alModItem.size(); i++) {
                alModItem.get(i).setVisible(false);
            }
        }
    }

    private Font calculateMaxFontSize(JLabel label, Font labelFont, String text) {
        int stringWidth = label.getFontMetrics(labelFont).stringWidth(text);
        int componentWidth = label.getWidth();

        // Find out how much the font can grow in width.
        double widthRatio = (double) componentWidth / (double) stringWidth;

        int newFontSize = (int) (labelFont.getSize() * widthRatio);
        int componentHeight = label.getHeight() - 3;

        // Pick a new font size so it will not be larger than the height of label.
        int fontSizeToUse = Math.min(newFontSize, componentHeight);

        // Set the label's font size to the newly determined size.
        return new Font(labelFont.getName(), Font.PLAIN, fontSizeToUse);
    }

    private void openFolderHelper(int buttonIndex) {
        Folder folder = null;
        int index = buttonIndex + (page - 1) * 4;
        if(selectedItem.size() > index && index >= 0 && !selectedItem.isEmpty()) {
            folder = selectedItem.get(index);
        }

        if(folder != null) {
            main.openFolderInExplorer(folder);
        }
    }

    private void loadButtonPress(int buttonIndex, boolean state) {
        Folder folder = null;
        int index = buttonIndex + (page - 1) * 4;
        if(selectedItem.size() > index && index >= 0 && !selectedItem.isEmpty()) {
            folder = selectedItem.get(index);
        }

        if(folder != null) {
            if(!state) { // delete mod
                long deleteStartTime = System.nanoTime();
                loadingBar.setValue(0);
                
                main.deleteMod(folder);
                
                long deleteEndTime = System.nanoTime();
                loadingBar.setValue(100);
                loadingLabel.setText("Deleting took: " + (double) (Math.round((deleteEndTime - deleteStartTime) / 10000.0) / 100.0) + " ms");
                // System.out.println("Delete reload");
                reload(true);
            } else { // delete other same mods and install instead 
                for(int i = 0; i < selectedItem.size(); i++) {
                    if(selectedItem.get(i).isInstalled()) {
                        main.deleteMod(selectedItem.get(i));
                    }
                }

                install(folder);
            }
        }
    }

    private File openFolderChooser(String title, File directory) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(directory);
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if(chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        } else {
            return null;
        }
    }

    private File openFileChooser(String title, File directory) {
        FileNameExtensionFilter zipFilter = new FileNameExtensionFilter("Compressed archives (zip, 7z, rar)", "zip", "7z", "rar");
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(directory);
        chooser.setDialogTitle(title);
        chooser.setFileFilter(zipFilter);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setAcceptAllFileFilterUsed(false);

        if(chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        } else {
            return null;
        }
    }

    private void installFromGameBanana() {
        gbFileSelector.removeAllItems();
        gbFileSelector.setEnabled(false);
        selectButton.setEnabled(false);
        
        if(isValidURL(urlTextField.getText())) {
            urlStateLabel.setText("URL is valid!");
            
            GBFileGetter fileGetter = new GBFileGetter();
            alGBFileData = fileGetter.getFileData(urlTextField.getText());
            
            if(alGBFileData == null) {
                urlStateLabel.setText("Error happened while getting data!");
                JOptionPane.showMessageDialog(null, "An error happened while requesting data.\nHTTP status: " + fileGetter.getStatus(), "HTTP error!", JOptionPane.ERROR_MESSAGE);
            } else if(alGBFileData.isEmpty()) {
                urlStateLabel.setText("Given data was flawed or empty!");
            } else {
                gbFileSelector.setEnabled(true);
                selectButton.setEnabled(true);
        
                for(GBFileData fileData : alGBFileData) {
                    if(alGBFileData.indexOf(fileData) == 0) {
                        gbFileSelector.addItem(fileData.getFileName() + " (" + fileData.getSize() + ") ~ newest");
                    } else {   
                        gbFileSelector.addItem(fileData.getFileName() + " (" + fileData.getSize() + ")");
                    }
                }
            }
        } else {
            urlStateLabel.setText("URL is invalid!");
        }
    }
    
    private boolean isValidURL(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch(MalformedURLException | URISyntaxException ex) {
            return false;
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

        settingsFrame = new javax.swing.JFrame();
        jPanel1 = new javax.swing.JPanel();
        autoInstallSetting = new javax.swing.JCheckBox();
        overwriteInstallSetting = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        pathPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        searchCachedFolderBtn = new javax.swing.JButton();
        cachedFolderLabel = new javax.swing.JLabel();
        searchInstalledFolderBtn = new javax.swing.JButton();
        installedFolderLabel = new javax.swing.JLabel();
        searchInstalledShaderFixBtn = new javax.swing.JButton();
        installedShaderFixLabel = new javax.swing.JLabel();
        importFrame = new javax.swing.JFrame();
        jLabel5 = new javax.swing.JLabel();
        urlTextField = new javax.swing.JTextField();
        fromDiskButton = new javax.swing.JButton();
        fromURLButton = new javax.swing.JButton();
        urlStateLabel = new javax.swing.JLabel();
        gbFileSelector = new javax.swing.JComboBox<>();
        selectButton = new javax.swing.JButton();
        leftPanel = new javax.swing.JPanel();
        categorySelector = new javax.swing.JComboBox<>();
        sectionSelector = new javax.swing.JComboBox<>();
        searchField = new javax.swing.JTextField();
        reloadButton = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        modList = new javax.swing.JList<>();
        mainPanel = new javax.swing.JPanel();
        modItem1 = new javax.swing.JPanel();
        previewImagePanel1 = new javax.swing.JPanel();
        imageLabel1 = new javax.swing.JLabel();
        loadModButton1 = new javax.swing.JToggleButton();
        openModFolderButton1 = new javax.swing.JButton();
        modLabel1 = new javax.swing.JLabel();
        iniLabel1 = new javax.swing.JLabel();
        shaderFixesBox1 = new javax.swing.JCheckBox();
        cachedBox1 = new javax.swing.JCheckBox();
        modItem2 = new javax.swing.JPanel();
        previewImagePanel2 = new javax.swing.JPanel();
        imageLabel2 = new javax.swing.JLabel();
        loadModButton2 = new javax.swing.JToggleButton();
        openModFolderButton2 = new javax.swing.JButton();
        modLabel2 = new javax.swing.JLabel();
        iniLabel2 = new javax.swing.JLabel();
        shaderFixesBox2 = new javax.swing.JCheckBox();
        cachedBox2 = new javax.swing.JCheckBox();
        modItem3 = new javax.swing.JPanel();
        previewImagePanel3 = new javax.swing.JPanel();
        imageLabel3 = new javax.swing.JLabel();
        loadModButton3 = new javax.swing.JToggleButton();
        openModFolderButton3 = new javax.swing.JButton();
        modLabel3 = new javax.swing.JLabel();
        iniLabel3 = new javax.swing.JLabel();
        shaderFixesBox3 = new javax.swing.JCheckBox();
        cachedBox3 = new javax.swing.JCheckBox();
        modItem4 = new javax.swing.JPanel();
        previewImagePanel4 = new javax.swing.JPanel();
        imageLabel4 = new javax.swing.JLabel();
        loadModButton4 = new javax.swing.JToggleButton();
        openModFolderButton4 = new javax.swing.JButton();
        modLabel4 = new javax.swing.JLabel();
        iniLabel4 = new javax.swing.JLabel();
        shaderFixesBox4 = new javax.swing.JCheckBox();
        cachedBox4 = new javax.swing.JCheckBox();
        pagePanel = new javax.swing.JPanel();
        previousPageButton = new javax.swing.JButton();
        pageLabel = new javax.swing.JLabel();
        nextPageButton = new javax.swing.JButton();
        otherPanel = new javax.swing.JPanel();
        loadingBar = new javax.swing.JProgressBar();
        loadingLabel = new javax.swing.JLabel();
        settingsButton = new javax.swing.JButton();
        importButton = new javax.swing.JButton();
        reloadLabel = new javax.swing.JLabel();

        settingsFrame.setTitle("Settings");
        settingsFrame.setAlwaysOnTop(true);
        settingsFrame.setResizable(false);
        settingsFrame.setSize(new java.awt.Dimension(583, 153));

        autoInstallSetting.setText("Automatically Install");
        autoInstallSetting.setFocusable(false);
        autoInstallSetting.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoInstallSettingActionPerformed(evt);
            }
        });

        overwriteInstallSetting.setText("Overwrite Install");
        overwriteInstallSetting.setEnabled(false);
        overwriteInstallSetting.setFocusable(false);
        overwriteInstallSetting.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                overwriteInstallSettingActionPerformed(evt);
            }
        });

        jLabel2.setText("Automatically install new imported mods (only if no mod affecting the same is installed");

        jLabel4.setText("Allows the auto-install to deactivate old mods to activate new mods");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 599, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(autoInstallSetting)
                            .addComponent(overwriteInstallSetting))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(autoInstallSetting)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(overwriteInstallSetting)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel1.setText("Folder locations:");

        searchCachedFolderBtn.setText("Cached Mods");
        searchCachedFolderBtn.setFocusable(false);
        searchCachedFolderBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchCachedFolderBtnActionPerformed(evt);
            }
        });

        searchInstalledFolderBtn.setText("Installed Mods");
        searchInstalledFolderBtn.setFocusable(false);
        searchInstalledFolderBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchInstalledFolderBtnActionPerformed(evt);
            }
        });

        searchInstalledShaderFixBtn.setText("Installed Shader Fixes");
        searchInstalledShaderFixBtn.setFocusable(false);
        searchInstalledShaderFixBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchInstalledShaderFixBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pathPanelLayout = new javax.swing.GroupLayout(pathPanel);
        pathPanel.setLayout(pathPanelLayout);
        pathPanelLayout.setHorizontalGroup(
            pathPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pathPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pathPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pathPanelLayout.createSequentialGroup()
                        .addGroup(pathPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(searchCachedFolderBtn)
                            .addComponent(searchInstalledFolderBtn))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pathPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(installedFolderLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cachedFolderLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(jLabel1)
                    .addGroup(pathPanelLayout.createSequentialGroup()
                        .addComponent(searchInstalledShaderFixBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(installedShaderFixLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        pathPanelLayout.setVerticalGroup(
            pathPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pathPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pathPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(searchCachedFolderBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cachedFolderLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pathPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(installedFolderLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(searchInstalledFolderBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pathPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(installedShaderFixLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(searchInstalledShaderFixBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout settingsFrameLayout = new javax.swing.GroupLayout(settingsFrame.getContentPane());
        settingsFrame.getContentPane().setLayout(settingsFrameLayout);
        settingsFrameLayout.setHorizontalGroup(
            settingsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(settingsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pathPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        settingsFrameLayout.setVerticalGroup(
            settingsFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pathPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        importFrame.setTitle("Import");
        importFrame.setAlwaysOnTop(true);
        importFrame.setResizable(false);

        jLabel5.setText("GameBanana URL:");

        urlTextField.setToolTipText("GameBanana link from mod to download");
        urlTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                urlTextFieldActionPerformed(evt);
            }
        });

        fromDiskButton.setText("From disk");
        fromDiskButton.setFocusable(false);
        fromDiskButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fromDiskButtonActionPerformed(evt);
            }
        });

        fromURLButton.setText("From GameBanana");
        fromURLButton.setFocusable(false);
        fromURLButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fromURLButtonActionPerformed(evt);
            }
        });

        gbFileSelector.setEnabled(false);

        selectButton.setText("Select");
        selectButton.setEnabled(false);
        selectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout importFrameLayout = new javax.swing.GroupLayout(importFrame.getContentPane());
        importFrame.getContentPane().setLayout(importFrameLayout);
        importFrameLayout.setHorizontalGroup(
            importFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(importFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(importFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(importFrameLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(urlTextField))
                    .addGroup(importFrameLayout.createSequentialGroup()
                        .addComponent(fromDiskButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fromURLButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(urlStateLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 309, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(importFrameLayout.createSequentialGroup()
                        .addComponent(gbFileSelector, javax.swing.GroupLayout.PREFERRED_SIZE, 450, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(selectButton)))
                .addContainerGap())
        );
        importFrameLayout.setVerticalGroup(
            importFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(importFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(importFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(urlTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(importFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(urlStateLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(importFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(fromDiskButton)
                        .addComponent(fromURLButton)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(importFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(selectButton)
                    .addComponent(gbFileSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Genshin Mod Browser");
        setMinimumSize(new java.awt.Dimension(960, 545));
        setResizable(false);
        setSize(new java.awt.Dimension(960, 545));

        leftPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(99, 102, 106), 2, true));

        categorySelector.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                categorySelectorActionPerformed(evt);
            }
        });

        sectionSelector.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sectionSelectorActionPerformed(evt);
            }
        });

        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                searchFieldKeyReleased(evt);
            }
        });

        reloadButton.setText("Reload");
        reloadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reloadButtonActionPerformed(evt);
            }
        });

        modList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        modList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                modListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(modList);

        javax.swing.GroupLayout leftPanelLayout = new javax.swing.GroupLayout(leftPanel);
        leftPanel.setLayout(leftPanelLayout);
        leftPanelLayout.setHorizontalGroup(
            leftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(leftPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(leftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(leftPanelLayout.createSequentialGroup()
                        .addComponent(categorySelector, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(sectionSelector, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(reloadButton, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 282, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 282, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        leftPanelLayout.setVerticalGroup(
            leftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(leftPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(leftPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(reloadButton, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sectionSelector, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(categorySelector, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 427, Short.MAX_VALUE)
                .addContainerGap())
        );

        mainPanel.setBorder(leftPanel.getBorder());
        mainPanel.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                mainPanelMouseWheelMoved(evt);
            }
        });
        mainPanel.setLayout(new java.awt.GridLayout(2, 2, 5, 5));

        previewImagePanel1.setBackground(new java.awt.Color(90, 96, 97));
        previewImagePanel1.setPreferredSize(new java.awt.Dimension(200, 200));

        imageLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        imageLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout previewImagePanel1Layout = new javax.swing.GroupLayout(previewImagePanel1);
        previewImagePanel1.setLayout(previewImagePanel1Layout);
        previewImagePanel1Layout.setHorizontalGroup(
            previewImagePanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(imageLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        previewImagePanel1Layout.setVerticalGroup(
            previewImagePanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(previewImagePanel1Layout.createSequentialGroup()
                .addComponent(imageLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        loadModButton1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        loadModButton1.setForeground(new java.awt.Color(99, 102, 106));
        loadModButton1.setText("Disabled");
        loadModButton1.setFocusPainted(false);
        loadModButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadModButton1ActionPerformed(evt);
            }
        });

        openModFolderButton1.setText("Open Folder");
        openModFolderButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openModFolderButton1ActionPerformed(evt);
            }
        });

        modLabel1.setText("Mod Name");
        modLabel1.setFocusable(false);

        iniLabel1.setText("ignore.ini");

        shaderFixesBox1.setText("Shaderfixes");
        shaderFixesBox1.setEnabled(false);

        cachedBox1.setText("Cached");
        cachedBox1.setEnabled(false);

        javax.swing.GroupLayout modItem1Layout = new javax.swing.GroupLayout(modItem1);
        modItem1.setLayout(modItem1Layout);
        modItem1Layout.setHorizontalGroup(
            modItem1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(modItem1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(modItem1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(modItem1Layout.createSequentialGroup()
                        .addComponent(iniLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 313, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(modItem1Layout.createSequentialGroup()
                        .addGroup(modItem1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(modLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 315, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(modItem1Layout.createSequentialGroup()
                                .addComponent(previewImagePanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(modItem1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(openModFolderButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(loadModButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(shaderFixesBox1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(cachedBox1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        modItem1Layout.setVerticalGroup(
            modItem1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, modItem1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(modLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(iniLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(modItem1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, modItem1Layout.createSequentialGroup()
                        .addComponent(cachedBox1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(shaderFixesBox1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(openModFolderButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(loadModButton1))
                    .addComponent(previewImagePanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        mainPanel.add(modItem1);

        previewImagePanel2.setBackground(new java.awt.Color(90, 96, 97));
        previewImagePanel2.setPreferredSize(new java.awt.Dimension(200, 200));

        imageLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        imageLabel2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout previewImagePanel2Layout = new javax.swing.GroupLayout(previewImagePanel2);
        previewImagePanel2.setLayout(previewImagePanel2Layout);
        previewImagePanel2Layout.setHorizontalGroup(
            previewImagePanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(imageLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        previewImagePanel2Layout.setVerticalGroup(
            previewImagePanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(previewImagePanel2Layout.createSequentialGroup()
                .addComponent(imageLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        loadModButton2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        loadModButton2.setForeground(new java.awt.Color(99, 102, 106));
        loadModButton2.setText("Disabled");
        loadModButton2.setFocusPainted(false);
        loadModButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadModButton2ActionPerformed(evt);
            }
        });

        openModFolderButton2.setText("Open Folder");
        openModFolderButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openModFolderButton2ActionPerformed(evt);
            }
        });

        modLabel2.setText("Mod Name");
        modLabel2.setFocusable(false);

        iniLabel2.setText("ignore.ini");

        shaderFixesBox2.setText("Shaderfixes");
        shaderFixesBox2.setEnabled(false);

        cachedBox2.setText("Cached");
        cachedBox2.setEnabled(false);

        javax.swing.GroupLayout modItem2Layout = new javax.swing.GroupLayout(modItem2);
        modItem2.setLayout(modItem2Layout);
        modItem2Layout.setHorizontalGroup(
            modItem2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(modItem2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(modItem2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, modItem2Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(modItem2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(openModFolderButton2, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(loadModButton2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, modItem2Layout.createSequentialGroup()
                                .addComponent(previewImagePanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(modItem2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(shaderFixesBox2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(cachedBox2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(modItem2Layout.createSequentialGroup()
                        .addGroup(modItem2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(modLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 315, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(iniLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 315, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        modItem2Layout.setVerticalGroup(
            modItem2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, modItem2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(modLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(iniLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(modItem2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, modItem2Layout.createSequentialGroup()
                        .addComponent(cachedBox2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(shaderFixesBox2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(openModFolderButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(loadModButton2))
                    .addComponent(previewImagePanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        mainPanel.add(modItem2);

        previewImagePanel3.setBackground(new java.awt.Color(90, 96, 97));
        previewImagePanel3.setPreferredSize(new java.awt.Dimension(200, 200));

        imageLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        imageLabel3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout previewImagePanel3Layout = new javax.swing.GroupLayout(previewImagePanel3);
        previewImagePanel3.setLayout(previewImagePanel3Layout);
        previewImagePanel3Layout.setHorizontalGroup(
            previewImagePanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(imageLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        previewImagePanel3Layout.setVerticalGroup(
            previewImagePanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(previewImagePanel3Layout.createSequentialGroup()
                .addComponent(imageLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        loadModButton3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        loadModButton3.setForeground(new java.awt.Color(99, 102, 106));
        loadModButton3.setText("Disabled");
        loadModButton3.setFocusPainted(false);
        loadModButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadModButton3ActionPerformed(evt);
            }
        });

        openModFolderButton3.setText("Open Folder");
        openModFolderButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openModFolderButton3ActionPerformed(evt);
            }
        });

        modLabel3.setText("Mod Name");
        modLabel3.setFocusable(false);

        iniLabel3.setText("ignore.ini");

        shaderFixesBox3.setText("Shaderfixes");
        shaderFixesBox3.setEnabled(false);

        cachedBox3.setText("Cached");
        cachedBox3.setEnabled(false);

        javax.swing.GroupLayout modItem3Layout = new javax.swing.GroupLayout(modItem3);
        modItem3.setLayout(modItem3Layout);
        modItem3Layout.setHorizontalGroup(
            modItem3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(modItem3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(modItem3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(modLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(modItem3Layout.createSequentialGroup()
                        .addComponent(previewImagePanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(modItem3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(openModFolderButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(loadModButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(shaderFixesBox3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cachedBox3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(iniLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 315, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        modItem3Layout.setVerticalGroup(
            modItem3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, modItem3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(modLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(iniLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(modItem3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, modItem3Layout.createSequentialGroup()
                        .addComponent(cachedBox3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(shaderFixesBox3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(openModFolderButton3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(loadModButton3))
                    .addComponent(previewImagePanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        mainPanel.add(modItem3);

        previewImagePanel4.setBackground(new java.awt.Color(90, 96, 97));
        previewImagePanel4.setPreferredSize(new java.awt.Dimension(200, 200));

        imageLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        imageLabel4.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout previewImagePanel4Layout = new javax.swing.GroupLayout(previewImagePanel4);
        previewImagePanel4.setLayout(previewImagePanel4Layout);
        previewImagePanel4Layout.setHorizontalGroup(
            previewImagePanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(imageLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        previewImagePanel4Layout.setVerticalGroup(
            previewImagePanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(previewImagePanel4Layout.createSequentialGroup()
                .addComponent(imageLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        loadModButton4.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        loadModButton4.setForeground(new java.awt.Color(99, 102, 106));
        loadModButton4.setText("Disabled");
        loadModButton4.setFocusPainted(false);
        loadModButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadModButton4ActionPerformed(evt);
            }
        });

        openModFolderButton4.setText("Open Folder");
        openModFolderButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openModFolderButton4ActionPerformed(evt);
            }
        });

        modLabel4.setText("Mod Name");
        modLabel4.setFocusable(false);

        iniLabel4.setText("ignore.ini");

        shaderFixesBox4.setText("Shaderfixes");
        shaderFixesBox4.setEnabled(false);

        cachedBox4.setText("Cached");
        cachedBox4.setEnabled(false);

        javax.swing.GroupLayout modItem4Layout = new javax.swing.GroupLayout(modItem4);
        modItem4.setLayout(modItem4Layout);
        modItem4Layout.setHorizontalGroup(
            modItem4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(modItem4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(modItem4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(modLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(modItem4Layout.createSequentialGroup()
                        .addComponent(previewImagePanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(modItem4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(openModFolderButton4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(loadModButton4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(shaderFixesBox4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cachedBox4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addComponent(iniLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 315, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        modItem4Layout.setVerticalGroup(
            modItem4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, modItem4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(modLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(iniLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(modItem4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, modItem4Layout.createSequentialGroup()
                        .addComponent(cachedBox4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(shaderFixesBox4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(openModFolderButton4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(loadModButton4))
                    .addComponent(previewImagePanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        mainPanel.add(modItem4);

        pagePanel.setBorder(leftPanel.getBorder());

        previousPageButton.setText("Previous Page");
        previousPageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previousPageButtonActionPerformed(evt);
            }
        });

        pageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pageLabel.setText("1/1");

        nextPageButton.setText("Next Page");
        nextPageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextPageButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pagePanelLayout = new javax.swing.GroupLayout(pagePanel);
        pagePanel.setLayout(pagePanelLayout);
        pagePanelLayout.setHorizontalGroup(
            pagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pagePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(previousPageButton, javax.swing.GroupLayout.PREFERRED_SIZE, 205, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(nextPageButton, javax.swing.GroupLayout.PREFERRED_SIZE, 205, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        pagePanelLayout.setVerticalGroup(
            pagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pagePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(previousPageButton)
                    .addComponent(pageLabel)
                    .addComponent(nextPageButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        otherPanel.setBorder(leftPanel.getBorder());

        settingsButton.setText("Settings");
        settingsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                settingsButtonActionPerformed(evt);
            }
        });

        importButton.setText("Import");
        importButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout otherPanelLayout = new javax.swing.GroupLayout(otherPanel);
        otherPanel.setLayout(otherPanelLayout);
        otherPanelLayout.setHorizontalGroup(
            otherPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(otherPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(otherPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(loadingBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(otherPanelLayout.createSequentialGroup()
                        .addGroup(otherPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(loadingLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 282, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(otherPanelLayout.createSequentialGroup()
                                .addComponent(settingsButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(importButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(reloadLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        otherPanelLayout.setVerticalGroup(
            otherPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(otherPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(loadingBar, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(loadingLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(otherPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(settingsButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(importButton)
                    .addComponent(reloadLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(leftPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(otherPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(6, 6, 6))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pagePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(leftPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(otherPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void categorySelectorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_categorySelectorActionPerformed
        if(!initFinished) {
            return;
        }

        if(categorySelector.getSelectedItem().equals("Skin")) {
            sectionSelector.setEnabled(true);
            sectionSelector.setSelectedIndex(prevSectionIndex);
            prevSectionIndex = -1;
        } else if(categorySelector.getSelectedItem().equals("Mod")) {
            sectionSelector.setEnabled(false);
            if(prevSectionIndex == -1) {
                prevSectionIndex = sectionSelector.getSelectedIndex();
            }
            sectionSelector.setSelectedIndex(-1);
            updateUI();
        } else if(categorySelector.getSelectedItem().equals("Installed")) {
            sectionSelector.setEnabled(false);
            if(prevSectionIndex == -1) {
                prevSectionIndex = sectionSelector.getSelectedIndex();
            }
            sectionSelector.setSelectedIndex(-1);
            updateUI();
        }
    }//GEN-LAST:event_categorySelectorActionPerformed

    private void reloadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadButtonActionPerformed
        reload(true);
        updateUI();
    }//GEN-LAST:event_reloadButtonActionPerformed

    private void sectionSelectorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sectionSelectorActionPerformed
        if(!initFinished) {
            return;
        }

        if(sectionSelector.getSelectedItem() != null)
            updateUI();
    }//GEN-LAST:event_sectionSelectorActionPerformed

    private void searchFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchFieldKeyReleased
        searchList(searchField.getText());
    }//GEN-LAST:event_searchFieldKeyReleased

    private void previousPageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_previousPageButtonActionPerformed
        scrollPage(-1);
    }//GEN-LAST:event_previousPageButtonActionPerformed

    private void nextPageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextPageButtonActionPerformed
        scrollPage(1);
    }//GEN-LAST:event_nextPageButtonActionPerformed

    private void mainPanelMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_mainPanelMouseWheelMoved
        scrollPage(evt.getWheelRotation());
    }//GEN-LAST:event_mainPanelMouseWheelMoved

    private void modListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_modListValueChanged
        if(!initFinished) {
            return;
        }

        if(evt.getValueIsAdjusting()) {
            selectItemFromList();
        }
    }//GEN-LAST:event_modListValueChanged

    private void openModFolderButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openModFolderButton1ActionPerformed
        openFolderHelper(0);
    }//GEN-LAST:event_openModFolderButton1ActionPerformed

    private void openModFolderButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openModFolderButton3ActionPerformed
        openFolderHelper(2);
    }//GEN-LAST:event_openModFolderButton3ActionPerformed

    private void openModFolderButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openModFolderButton4ActionPerformed
        openFolderHelper(3);
    }//GEN-LAST:event_openModFolderButton4ActionPerformed

    private void loadModButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadModButton1ActionPerformed
        loadButtonPress(0, loadModButton1.isSelected());
    }//GEN-LAST:event_loadModButton1ActionPerformed

    private void loadModButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadModButton3ActionPerformed
        loadButtonPress(2, loadModButton3.isSelected());
    }//GEN-LAST:event_loadModButton3ActionPerformed

    private void loadModButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadModButton4ActionPerformed
        loadButtonPress(3, loadModButton4.isSelected());
    }//GEN-LAST:event_loadModButton4ActionPerformed

    private void openModFolderButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openModFolderButton2ActionPerformed
        openFolderHelper(1);
    }//GEN-LAST:event_openModFolderButton2ActionPerformed

    private void loadModButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadModButton2ActionPerformed
        loadButtonPress(1, loadModButton2.isSelected());
    }//GEN-LAST:event_loadModButton2ActionPerformed

    private void settingsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settingsButtonActionPerformed
        settingsFrame.setLocationRelativeTo(this);
        settingsFrame.pack();
        settingsFrame.setVisible(!settingsFrame.isVisible());
    }//GEN-LAST:event_settingsButtonActionPerformed

    private void searchCachedFolderBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchCachedFolderBtnActionPerformed
        File newDir = openFolderChooser("Choose cached mods folder", main.getModDir());
        if(newDir != null) {
            main.setModDir(newDir);
            cachedFolderLabel.setText(newDir.getPath());
            reload(true);
        }
    }//GEN-LAST:event_searchCachedFolderBtnActionPerformed

    private void searchInstalledFolderBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchInstalledFolderBtnActionPerformed
        File newDir = openFolderChooser("Choose 3DMigoto mods folder", main.getModDir());
        if(newDir != null) {
            main.setMigotoModDir(newDir);
            installedFolderLabel.setText(newDir.getPath());
            reload(true);
        }
    }//GEN-LAST:event_searchInstalledFolderBtnActionPerformed

    private void searchInstalledShaderFixBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchInstalledShaderFixBtnActionPerformed
        File newDir = openFolderChooser("Choose 3DMigoto shader fixes folder", main.getModDir());
        if(newDir != null) {
            main.setMigotoShaderFixDir(newDir);
            installedShaderFixLabel.setText(newDir.getPath());
            reload(true);
        }
    }//GEN-LAST:event_searchInstalledShaderFixBtnActionPerformed

    private void autoInstallSettingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoInstallSettingActionPerformed
        main.setAutoInstall(autoInstallSetting.isSelected());
        overwriteInstallSetting.setEnabled(autoInstallSetting.isSelected());
    }//GEN-LAST:event_autoInstallSettingActionPerformed

    private void overwriteInstallSettingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_overwriteInstallSettingActionPerformed
        main.setOverwriteInstall(overwriteInstallSetting.isSelected());
    }//GEN-LAST:event_overwriteInstallSettingActionPerformed

    private void importButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importButtonActionPerformed
        importFrame.setLocationRelativeTo(this);
        
        urlStateLabel.setText("");
        urlTextField.setText("");
        
        gbFileSelector.removeAllItems();
        gbFileSelector.setEnabled(false);
        selectButton.setEnabled(false);
        
        importFrame.pack();
        importFrame.setVisible(!importFrame.isVisible());
    }//GEN-LAST:event_importButtonActionPerformed

    private void fromDiskButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fromDiskButtonActionPerformed
        File selectedFile = openFileChooser("Import from disk", new File(System.getProperty("user.home") + "\\Downloads"));

        if(selectedFile != null) {
            importFrame.setVisible(false);
            copy(new ArrayList<File>(List.of(selectedFile)));
        }
    }//GEN-LAST:event_fromDiskButtonActionPerformed

    private void fromURLButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fromURLButtonActionPerformed
        installFromGameBanana();
    }//GEN-LAST:event_fromURLButtonActionPerformed

    private void urlTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_urlTextFieldActionPerformed
        installFromGameBanana();
    }//GEN-LAST:event_urlTextFieldActionPerformed

    private void selectButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectButtonActionPerformed
        importFrame.setVisible(false);
        download(alGBFileData.get(gbFileSelector.getSelectedIndex()));
    }//GEN-LAST:event_selectButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            /* Create and display the form */
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    new MainFrame().setVisible(true);
                }
            });
        } catch(UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox autoInstallSetting;
    private javax.swing.JCheckBox cachedBox1;
    private javax.swing.JCheckBox cachedBox2;
    private javax.swing.JCheckBox cachedBox3;
    private javax.swing.JCheckBox cachedBox4;
    private javax.swing.JLabel cachedFolderLabel;
    private javax.swing.JComboBox<String> categorySelector;
    private javax.swing.JButton fromDiskButton;
    private javax.swing.JButton fromURLButton;
    private javax.swing.JComboBox<String> gbFileSelector;
    private javax.swing.JLabel imageLabel1;
    private javax.swing.JLabel imageLabel2;
    private javax.swing.JLabel imageLabel3;
    private javax.swing.JLabel imageLabel4;
    private javax.swing.JButton importButton;
    private javax.swing.JFrame importFrame;
    private javax.swing.JLabel iniLabel1;
    private javax.swing.JLabel iniLabel2;
    private javax.swing.JLabel iniLabel3;
    private javax.swing.JLabel iniLabel4;
    private javax.swing.JLabel installedFolderLabel;
    private javax.swing.JLabel installedShaderFixLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel leftPanel;
    private javax.swing.JToggleButton loadModButton1;
    private javax.swing.JToggleButton loadModButton2;
    private javax.swing.JToggleButton loadModButton3;
    private javax.swing.JToggleButton loadModButton4;
    private javax.swing.JProgressBar loadingBar;
    private javax.swing.JLabel loadingLabel;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel modItem1;
    private javax.swing.JPanel modItem2;
    private javax.swing.JPanel modItem3;
    private javax.swing.JPanel modItem4;
    private javax.swing.JLabel modLabel1;
    private javax.swing.JLabel modLabel2;
    private javax.swing.JLabel modLabel3;
    private javax.swing.JLabel modLabel4;
    private javax.swing.JList<String> modList;
    private javax.swing.JButton nextPageButton;
    private javax.swing.JButton openModFolderButton1;
    private javax.swing.JButton openModFolderButton2;
    private javax.swing.JButton openModFolderButton3;
    private javax.swing.JButton openModFolderButton4;
    private javax.swing.JPanel otherPanel;
    private javax.swing.JCheckBox overwriteInstallSetting;
    private javax.swing.JLabel pageLabel;
    private javax.swing.JPanel pagePanel;
    private javax.swing.JPanel pathPanel;
    private javax.swing.JPanel previewImagePanel1;
    private javax.swing.JPanel previewImagePanel2;
    private javax.swing.JPanel previewImagePanel3;
    private javax.swing.JPanel previewImagePanel4;
    private javax.swing.JButton previousPageButton;
    private javax.swing.JButton reloadButton;
    private javax.swing.JLabel reloadLabel;
    private javax.swing.JButton searchCachedFolderBtn;
    private javax.swing.JTextField searchField;
    private javax.swing.JButton searchInstalledFolderBtn;
    private javax.swing.JButton searchInstalledShaderFixBtn;
    private javax.swing.JComboBox<String> sectionSelector;
    private javax.swing.JButton selectButton;
    private javax.swing.JButton settingsButton;
    private javax.swing.JFrame settingsFrame;
    private javax.swing.JCheckBox shaderFixesBox1;
    private javax.swing.JCheckBox shaderFixesBox2;
    private javax.swing.JCheckBox shaderFixesBox3;
    private javax.swing.JCheckBox shaderFixesBox4;
    private javax.swing.JLabel urlStateLabel;
    private javax.swing.JTextField urlTextField;
    // End of variables declaration//GEN-END:variables
}
