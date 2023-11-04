
import java.awt.Desktop;
import java.io.*;
import java.nio.file.*;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author Romeo
 */
public class Main {

    private File modDir;
    private Folder modFolderStructure;

    private File migotoModDir;
    private Folder migotoModFolderStructure;

    private File migotoShaderFixDir;
    private Folder migotoShaderFixFolderStructure;
    
    private File tempPath;

    private Data data;
    private Config config;
    private Desktop desktop;
    private final boolean postTimings = false;
    private boolean autoInstall;
    private boolean overwriteInstall;

    private HashMap<String, ArrayList<Folder>> characterMap = new HashMap<>();
    private HashMap<String, String> characterNameAffectsMap = new HashMap<>();
    private HashMap<String, ArrayList<Folder>> weaponMap = new HashMap<>();
    private HashMap<String, String> weaponNameAffectsMap = new HashMap<>();
    private ArrayList<Folder> modList = new ArrayList<>();

    private HashMap<String, Folder> checksumInstalledFolderMap = new HashMap<>();
    private HashMap<Folder, ArrayList<File>> folderInstalledShaderFixMap = new HashMap<>();
    private HashMap<String, File> checksumInstalledShaderFixMap = new HashMap<>();

    private MainFrame mainFrame;
    private JProgressBar loadingBar;
    private JLabel loadingLabel;
    private JLabel reloadingLabel;

    private boolean suppressFileWatcher;

    public Main(JProgressBar progressBar, JLabel loadlLabel, JLabel reloadLabel, MainFrame pMainFrame) {
        Init(progressBar, loadlLabel, reloadLabel, pMainFrame);
    }

    private void Init(JProgressBar progressBar, JLabel loadlLabel, JLabel reloadLabel, MainFrame pMainFrame) {
        long initStartTime = System.nanoTime();
        if(postTimings) {
            System.out.println("###### Init begin ============");
        }
        
        loadingBar = progressBar;
        loadingLabel = loadlLabel;
        reloadingLabel = reloadLabel;
        mainFrame = pMainFrame;

        config = new Config();
        loadConfig();
        
        tempPath = new File(System.getProperty("user.dir") + "\\Temp");
        
        if(!tempPath.isDirectory() && tempPath.exists()) {
            tempPath.delete();
            tempPath.mkdir();
        } else if(!tempPath.exists()) {
            tempPath.mkdir();
        }

        suppressFileWatcher = false;
        WatchService watchService;

        try {
            watchService = FileSystems.getDefault().newWatchService();
            modDir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
            migotoModDir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
            migotoShaderFixDir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
            
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                WatchKey key;
                try {
                    while(true) {
                        key = watchService.take();
                        
                        if(!key.pollEvents().isEmpty() && !suppressFileWatcher) {
                            // System.out.println("File Watcher reload");
                            mainFrame.reload(false);
                        }
                        
                        key.reset();
                    }
                } catch(InterruptedException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            executor.shutdown();
        } catch(IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        data = new Data();
        desktop = Desktop.getDesktop();

        long initEndTime = System.nanoTime();

        if(postTimings) {
            System.out.println("* Init took: " + (initEndTime - initStartTime) / 1000 + " mircoseconds\n###### Init end ============");
        }
    }

    private void loadConfig() {
        try {
            config.loadConfigFile();
            modDir = config.getModPath();
            migotoModDir = config.get3DMigotoModPath();
            migotoShaderFixDir = config.get3DMigotoShaderFixPath();
            autoInstall = config.getAutoInstall();
            overwriteInstall = config.getOverwriteInstall();
        } catch(Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Folder analyzeFolderStructure(File path) {
        long analyzeFolderStartTime = System.nanoTime();

        Folder folder = recursiveAnalyzeFolderStructure(path, -1);

        long analyzeFolderEndTime = System.nanoTime();
        if(postTimings) {
            System.out.println("    Analyzing folder took: " + (analyzeFolderEndTime - analyzeFolderStartTime) / 1000 + " mircoseconds");
        }

        return folder;
    }

    public Folder analyzeFolderStructure(File path, int depth) {
        long analyzeFolderStartTime = System.nanoTime();

        Folder folder = recursiveAnalyzeFolderStructure(path, depth);

        long analyzeFolderEndTime = System.nanoTime();
        if(postTimings) {
            System.out.println("    Analyzing folder took: " + (analyzeFolderEndTime - analyzeFolderStartTime) / 1000 + " mircoseconds");
        }

        return folder;
    }

    private Folder recursiveAnalyzeFolderStructure(File path, int depth) {
        if(!path.exists() || !path.isDirectory()) {
            System.err.println("Path doesn't exist or isn't a directory (recursiveAnalyzeFolderStructure)");
            return null;
        }

        File[] files = path.listFiles();
        Folder fileDir = new Folder(path);

        for(File file : files) {
            if(file.isFile()) {
                fileDir.addFile(file);
            } else if(file.isDirectory()) {
                fileDir.addDirectory(new Folder(file));
            }
        }

        if(depth > 0 || depth == -1) {
            int threadCount = (int) Math.ceil(Runtime.getRuntime().availableProcessors() / 2.0);
            if(threadCount <= 0) {
                threadCount = 1;
            }

            final int foldersPerThread = fileDir.getFolders().size() / threadCount;
            int finalThreadFolders = fileDir.getFolders().size() % threadCount;
            int lastIndex = 0;

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            ArrayList<CompletableFuture> alFutures = new ArrayList<>();

            for(int t = 0; t < threadCount; t++) {
                int jobs = foldersPerThread;
                if(finalThreadFolders > 0) {
                    jobs++;
                    finalThreadFolders--;
                }

                if(jobs == 0) {
                    continue;
                }
                // System.out.println("Thread " + t + " has " + jobs + " jobs, previous thread began at " + lastIndex);

                final int finalLastIndex = lastIndex;
                final int finalJobs = jobs;

                alFutures.add(CompletableFuture.runAsync(() -> {
                    for(int i = finalLastIndex; i < finalLastIndex + finalJobs; i++) {
                        fileDir.setFolder(i, recursiveAnalyzeFolderStructure(fileDir.getFolders().get(i).getFolderData(), depth, 1));
                    }
                }, executor));

                lastIndex += jobs;
            }

            CompletableFuture.allOf(alFutures.toArray(CompletableFuture[]::new)).join();
            executor.shutdown();
        }

        return fileDir;
    }

    private Folder recursiveAnalyzeFolderStructure(File path, int depth, int iteration) {
        if(!path.exists() || !path.isDirectory()) {
            System.err.println("Path doesn't exist or isn't a directory");
            return null;
        }

        File[] files = path.listFiles();
        Folder fileDir = new Folder(path);

        for(int i = 0; i < files.length; i++) {
            if(files[i].isFile()) {
                fileDir.addFile(files[i]);
            } else if(files[i].isDirectory()) {
                if(depth <= iteration && depth != -1) {
                    fileDir.addDirectory(new Folder(files[i]));
                } else {
                    fileDir.addDirectory(recursiveAnalyzeFolderStructure(files[i], depth, iteration + 1));
                }
            }
        }

        return fileDir;
    }

    public void printFolder(Folder folder) {
        printFolder(folder, -1, true, 0);
    }

    public void printFolder(Folder folder, boolean showFiles) {
        printFolder(folder, -1, showFiles, 0);
    }

    public void printFolder(Folder folder, int depth) {
        printFolder(folder, depth, true, 0);
    }

    public void printFolder(Folder folder, int depth, boolean showFiles) {
        printFolder(folder, depth, showFiles, 0);
    }

    private void printFolder(Folder folder, int depth, boolean showFiles, int iteration) {
        if(depth != -1 && depth < iteration || folder == null) {
            return;
        }

        String space = "";
        for(int i = 0; i < iteration; i++) {
            space += "  ";
        }

        if(showFiles) {
            for(int i = 0; i < folder.getFiles().size(); i++) {
                System.out.println(space + "- " + folder.getFiles().get(i).getName());
            }
            for(int i = 0; i < folder.getAlShaderFixes().size(); i++) {
                System.out.println(space + "* " + folder.getAlShaderFixes().get(i).getName());
            }
        }

        for(int i = 0; i < folder.getFolders().size(); i++) {
            if(iteration == 0) {
                System.out.println(space + "[] " + folder.getFolders().get(i).getFolderData().getName());
            } else {
                System.out.println(space + "+ " + folder.getFolders().get(i).getFolderData().getName());
            }
            printFolder(folder.getFolders().get(i), depth, showFiles, iteration + 1);
        }
    }

    public boolean getAutoInstall() {
        return autoInstall;
    }

    public void setAutoInstall(boolean state) {
        autoInstall = state;
        config.setAutoInstall(state);
    }

    public boolean getOverwriteInstall() {
        return overwriteInstall;
    }

    public void setOverwriteInstall(boolean state) {
        overwriteInstall = state;
        config.setOverwriteInstall(state);
    }

    public File getTempPath() {
        return tempPath;
    }
    
    public File getModDir() {
        return modDir;
    }

    public void setModDir(File file) {
        modDir = file;
        config.setModPath(file.getPath());
    }

    public Folder getModFolderStructure() {
        return modFolderStructure;
    }

    public File getMigotoModDir() {
        return migotoModDir;
    }

    public void setMigotoModDir(File file) {
        migotoModDir = file;
        config.set3DMigotoModPath(file.getPath());
    }

    public Folder getMigotoModFolderStructure() {
        return migotoModFolderStructure;
    }

    public File getMigotoShaderFixDir() {
        return migotoShaderFixDir;
    }

    public void setMigotoShaderFixDir(File file) {
        migotoShaderFixDir = file;
        config.set3DMigotoShaderFixPath(file.getPath());
    }

    public Folder getMigotoShaderFixFolderStructure() {
        return migotoShaderFixFolderStructure;
    }

    public Data getData() {
        return data;
    }

    public HashMap<String, ArrayList<Folder>> getCharacterMap() {
        return characterMap;
    }

    public HashMap<String, ArrayList<Folder>> getWeaponMap() {
        return weaponMap;
    }

    public ArrayList<Folder> getModList() {
        return modList;
    }

    private ArrayList<File> searchIniInFolder(Folder folder) {
        ArrayList<File> alFiles = new ArrayList<>();

        for(int i = 0; i < folder.getFiles().size(); i++) {
            File file = folder.getFiles().get(i);
            if(file.getName().contains(".ini")) {
                if(file.getName().equals("merged.ini")) {
                    // Search for a normal .ini file
                    boolean found = false;
                    for(int j = 0; j < folder.getFolders().size(); j++) {
                        if(found) {
                            break;
                        }
                        for(int o = 0; o < folder.getFolders().get(j).getFiles().size(); o++) {
                            if(folder.getFolders().get(j).getFiles().get(o).getName().contains(".ini")) {
                                alFiles.add(folder.getFolders().get(j).getFiles().get(o));
                                found = true;
                                break;
                            }
                        }
                    }
                }
                alFiles.add(file);
            }
        }

        return alFiles;
    }

    private ArrayList<File> recursiveSearchIniInFolder(Folder folder) {
        ArrayList<File> alFiles = searchIniInFolder(folder);

        if(alFiles.isEmpty()) {
            for(int i = 0; i < folder.getFolders().size(); i++) {
                alFiles.addAll(searchIniInFolder(folder.getFolders().get(i)));
            }
        }

        if(alFiles.isEmpty()) {
            for(int i = 0; i < folder.getFolders().size(); i++) {
                alFiles.addAll(recursiveSearchIniInFolder(folder.getFolders().get(i)));
            }
        }

        return alFiles;
    }

    private File searchImageInFolder(Folder folder) {
        for(int i = 0; i < folder.getFiles().size(); i++) {
            File file = folder.getFiles().get(i);

            if(file.getName().contains("preview")) {
                if(file.getName().endsWith(".png") || file.getName().endsWith(".jpg") || file.getName().endsWith(".jpeg")) {
                    return file;
                }
            }
        }

        return null;
    }

    private File recursiveSearchImageInFolder(Folder folder) {
        File file = searchImageInFolder(folder);

        if(file == null) {
            for(int i = 0; i < folder.getFolders().size(); i++) {
                return searchImageInFolder(folder.getFolders().get(i));
            }
        }

        return file;
    }

    private ArrayList<File> searchShaderFixes(Folder folder) {
        ArrayList<File> alFiles = new ArrayList<>();

        for(int i = 0; i < folder.getFiles().size(); i++) {
            File file = folder.getFiles().get(i);
            if(file.getName().contains("_replace.txt")) {
                alFiles.add(file);
            }
        }

        return alFiles;
    }

    private ArrayList<File> recursiveSearchShaderFixes(Folder folder) {
        ArrayList<File> alFiles = searchShaderFixes(folder);

        if(alFiles.isEmpty()) {
            for(int i = 0; i < folder.getFolders().size(); i++) {
                alFiles.addAll(searchShaderFixes(folder.getFolders().get(i)));
            }
        }

        if(alFiles.isEmpty()) {
            for(int i = 0; i < folder.getFolders().size(); i++) {
                alFiles.addAll(recursiveSearchShaderFixes(folder.getFolders().get(i)));
            }
        }

        return alFiles;
    }

    private Folder evaluateFolderStructure(Folder folderStructure) {
        long evaluateStartTime = System.nanoTime();

        if(folderStructure == null) {
            System.err.println("Give folder structure was null! (evaluateFolderStructure)");
            return null;
        }
        
        int threadCount = (int) Math.ceil(Runtime.getRuntime().availableProcessors() / 2.0);
        if(threadCount <= 0) {
            threadCount = 1;
        }

        final int foldersPerThread = folderStructure.getFolders().size() / threadCount;
        int finalThreadFolders = folderStructure.getFolders().size() % threadCount;
        int lastIndex = 0;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        ArrayList<CompletableFuture> alFutures = new ArrayList<>();

        for(int t = 0; t < threadCount; t++) {
            int jobs = foldersPerThread;
            if(finalThreadFolders > 0) {
                jobs++;
                finalThreadFolders--;
            }

            if(jobs == 0) {
                continue;
            }

            // System.out.println("Thread " + t + " has " + jobs + " jobs, previous thread began at " + lastIndex);
            final int finalLastIndex = lastIndex;
            final int finalJobs = jobs;

            alFutures.add(CompletableFuture.runAsync(() -> {
                for(int q = finalLastIndex; q < finalLastIndex + finalJobs; q++) {
                    folderEvaluationHandler(folderStructure.getFolders().get(q));
                }
            }, executor));

            lastIndex += jobs;
        }

        CompletableFuture.allOf(alFutures.toArray(CompletableFuture[]::new)).join();
        executor.shutdown();

        long evaluateEndTime = System.nanoTime();
        if(postTimings) {
            System.out.println("    Evaluation took: " + (evaluateEndTime - evaluateStartTime) / 1000 + " mircoseconds");
        }

        return folderStructure;
    }

    private Folder folderEvaluationHandler(Folder folder) {
        ArrayList<File> alFiles = recursiveSearchIniInFolder(folder);
        File previewImage = recursiveSearchImageInFolder(folder);
        File configFile = null;

        folder.setPreview(previewImage);

        // No .ini files were found, output error and continue
        if(alFiles.isEmpty()) {
            folder.setAlShaderFixes(recursiveSearchShaderFixes(folder));
            if(!folder.getAlShaderFixes().isEmpty()) {
                folder.setShaderFixOnly(true);
                folder.setAffects("");
                folder.setModName(folder.getFolderData().getName());
                folder.setCategory("ShaderFix");
                folder.setSection("");
                folder.setIndex(-1);

            } else {
                System.err.println(folder.getFolderData().getName() + " contains no .ini or shader fixes (evaluateFolderStructure)");
            }

            return folder;
        }

        // Search for merged.ini files in found files, is one is present, set it as config file
        for(int i = 0; i < alFiles.size(); i++) {
            File file = alFiles.get(i);

            if(file.getName().equals("merged.ini")) {
                configFile = file;
                alFiles.remove(file);
                break;
            }
        }

        File file = alFiles.get(0);

        // Check if selected file isn't disabled
        if(file.getName().contains("DISABLED") && configFile == null) {
            for(File tempFile : alFiles) {
                if(!tempFile.getName().contains("DISABLED")) {
                    file = tempFile;
                    break;
                }
            }
        }

        // If multiple .ini files were found, find the important one using the namespace
        if(alFiles.size() > 1) {
            String namespace = "";
            for(int i = 0; i < alFiles.size(); i++) {
                try(BufferedReader br = new BufferedReader(new FileReader(alFiles.get(i)))) {
                    String line = br.readLine();

                    for(int j = 0; j < 4; j++) {
                        if(line == null || line.startsWith(";")) {
                            line = br.readLine();
                            continue;
                        }

                        line = line.replaceAll("\\s", "");

                        if(line.contains("namespace")) {
                            line = line.replace("namespace=", "");
                            if(i == 0 || line.length() < namespace.length()) {
                                namespace = line;
                                file = alFiles.get(i);
                                break;
                            }
                        }

                        line = br.readLine();
                    }
                } catch(FileNotFoundException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                } catch(IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }

        // If no config file ist set, then set it as the selected file
        if(configFile == null) {
            configFile = file;
        }

        // Find all shader fixes in folder
        folder.setAlShaderFixes(recursiveSearchShaderFixes(folder));

        // Set folder parameters
        folder.setConfigFile(configFile);

        String checksum = getChecksum(configFile);

        folder.setChecksum(checksum);

        String affects = file.getName().replace("DISABLED", "").replace(".ini", "").toLowerCase().replaceAll("\\s", "");
        folder.setAffects(affects);
        folder.setModName(folder.getFolderData().getName());

        // Check what this folder affects and attribute the corresponding parameters
        String folderData = data.isInAnyAl(affects);

        if(folderData.equals("0")) {
            folder.setCategory("Mod");
            folder.setSection("");
            folder.setIndex(-1);
        } else {
            folder.setCategory("Skin");
            folder.setSection(data.getAlNameFromNumber(Integer.valueOf(folderData.charAt(0)) - '0'));
            folder.setIndex(Integer.parseInt(folderData.substring(1)));
        }

        return folder;
    }

    public String getChecksum(File file) {
        byte[] fileData = new byte[0];
        byte[] hash = new byte[0];

        try {
            fileData = Files.readAllBytes(file.toPath());
            hash = MessageDigest.getInstance("SHA-256").digest(fileData);
        } catch(IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch(NoSuchAlgorithmException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        return new BigInteger(1, hash).toString(16);
    }

    public String getShaderFixChecksum(File file) {
        return file.getName() + getChecksum(file);
    }

    private String getType(String section) {
        if(section.equals("Character")) {
            return "Character";
        } else if(section.equals("Bow") || section.equals("Catalyst")
                || section.equals("Claymore") || section.equals("Polearm")
                || section.equals("Sword")) {
            return "Weapon";
        }

        return "";
    }

    private String getAffectsFromName(String name) {
        String result = characterNameAffectsMap.getOrDefault(name, "");
        if(result.equals("")) {
            result = weaponNameAffectsMap.getOrDefault(name, "");
        }

        return result;
    }

    private void sortModFolderStructureByAffects() {
        long sortStartTime = System.nanoTime();

        characterMap = new HashMap<>();
        weaponMap = new HashMap<>();
        modList = new ArrayList<>();

        for(int i = 0; i < modFolderStructure.getFolders().size(); i++) {
            Folder folder = modFolderStructure.getFolders().get(i);

            if(folder == null) {
                System.err.println("ERROR! Folder was null in sortModFolderStructureByAffects");
                continue;
            }

            if(folder.getConfigFile() == null && !folder.isShaderFixOnly()) {
                System.err.println(folder.getFolderData().getName() + " has no config file and isn't shader fixes only either! (sortModFolderStructureByAffects)");

                continue;
            }

            if(folder.getCategory() == null) {
                System.err.println(folder.getFolderData().getName() + " has no category! (sortModFolderStructureByAffects)");
            }

            if(folder.getCategory().equals("ShaderFix") || folder.getCategory().equals("Mod")) {
                modList.add(folder);
            } else if(folder.getCategory().equals("Skin")) {
                if(getType(folder.getSection()).equals("Character")) {
                    ArrayList<Folder> alFolders = characterMap.getOrDefault(folder.getAffects(), new ArrayList<>());

                    alFolders.add(folder);
                    characterMap.putIfAbsent(folder.getAffects(), alFolders);
                } else if(getType(folder.getSection()).equals("Weapon")) {
                    ArrayList<Folder> alFolders = weaponMap.getOrDefault(folder.getAffects(), new ArrayList<>());

                    alFolders.add(folder);
                    weaponMap.putIfAbsent(folder.getAffects(), alFolders);
                }
            }

            folder.setCached(true);
        }

        long sortEndTime = System.nanoTime();
        if(postTimings) {
            System.out.println("    Sorting took: " + (sortEndTime - sortStartTime) / 1000 + " mircoseconds");
        }
    }

    private void reloadModFolderStructure(int percentagePerStage) {
        long reloadStartTime = System.nanoTime();
        if(postTimings) {
            System.out.println("# Reload Mod begin ======");
        }

        loadingBar.setValue(loadingBar.getValue() + percentagePerStage);
        reloadingLabel.setText("Analyzing Cached Mods");
        Folder folderStructure = analyzeFolderStructure(modDir);

        loadingBar.setValue(loadingBar.getValue() + percentagePerStage);
        reloadingLabel.setText("Evaluating Cached Mods");
        modFolderStructure = evaluateFolderStructure(folderStructure);

        loadingBar.setValue(loadingBar.getValue() + percentagePerStage);
        reloadingLabel.setText("Sorting Cached Mods");
        sortModFolderStructureByAffects();

        long reloadEndTime = System.nanoTime();
        if(postTimings) {
            System.out.println("    Mod Reload took: " + (reloadEndTime - reloadStartTime) / 1000 + " mircoseconds\n  # Reload Mod end ======");
        }
    }

    private void reloadMigotoModFolderStructure(int percentagePerStage) {
        long reloadStartTime = System.nanoTime();
        if(postTimings) {
            System.out.println("# Reload Migoto Mod begin ======");
        }

        loadingBar.setValue(loadingBar.getValue() + percentagePerStage);
        reloadingLabel.setText("Analyzing Installed Mods");
        Folder folderStructure = analyzeFolderStructure(migotoModDir);

        loadingBar.setValue(loadingBar.getValue() + percentagePerStage);
        reloadingLabel.setText("Evaluating Installed Mods");
        migotoModFolderStructure = evaluateFolderStructure(folderStructure);

        long reloadEndTime = System.nanoTime();
        if(postTimings) {
            System.out.println("    Migoto Mod Reload took: " + (reloadEndTime - reloadStartTime) / 1000 + " mircoseconds\n  # Reload Migoto Mod end ======");
        }
    }

    private void reloadMigotoShaderFixFolderStructure(int percentagePerStage) {
        long reloadStartTime = System.nanoTime();
        if(postTimings) {
            System.out.println("# Reload Migoto Shader Fix begin ======");
        }

        loadingBar.setValue(loadingBar.getValue() + percentagePerStage);
        reloadingLabel.setText("Analyzing Installed Shader Fixes");
        Folder folderStructure = analyzeFolderStructure(migotoShaderFixDir);

        loadingBar.setValue(loadingBar.getValue() + percentagePerStage);
        reloadingLabel.setText("Evaluating Installed Shader Fixes");
        migotoShaderFixFolderStructure = evaluateFolderStructure(folderStructure);

        long reloadEndTime = System.nanoTime();
        if(postTimings) {
            System.out.println("    Migoto Shader Fix Reload took: " + (reloadEndTime - reloadStartTime) / 1000 + " mircoseconds\n  # Reload Migoto Shader Fix end ======");
        }
    }

    public String fullReload() {
        long reloadStartTime = System.nanoTime();
        if(postTimings) {
            System.out.println("### Reload Migoto Shader Fix begin ======");
        }

        loadConfig();
        loadingBar.setValue(0);

        int errorCount = 0;
        String error = "Missing folders: ";

        if(modDir == null || !modDir.isDirectory()) {
            System.err.println("No cached mod folder is selected! (fullReload)");
            error += "cached mods";
            errorCount++;
        }
        if(migotoModDir == null || !migotoModDir.isDirectory()) {
            System.err.println("No 3DMigoto mod folder is selected! (fullReload)");
            if(errorCount != 0) {
                error += ", ";
            }
            error += "3DMigoto mods";
            errorCount++;
        }
        if(migotoShaderFixDir == null || !migotoShaderFixDir.isDirectory()) {
            System.err.println("No 3DMigoto shader fixes folder is selected! (fullReload)");
            if(errorCount != 0) {
                error += ", ";
            }
            error += "3DMigoto shader fixes";
            errorCount++;
        }

        if(errorCount != 0) {
            System.err.println(errorCount + " missing folders! (fullReload)");
            loadingLabel.setText(error);
            return error;
        }

        int percentagePerStage = 100 / 8;

        reloadingLabel.setText("Loading Cached Mods");
        reloadModFolderStructure(percentagePerStage);

        reloadingLabel.setText("Loading Installed Mods");
        reloadMigotoModFolderStructure(percentagePerStage);

        reloadingLabel.setText("Loading Shader Fixes Mods");
        reloadMigotoShaderFixFolderStructure(percentagePerStage);

        reloadingLabel.setText("Matching Installed Mods");
        checkIfInstalled();

        long reloadEndTime = System.nanoTime();

        loadingBar.setValue(100);
        reloadingLabel.setText("Reloaded in " + (double) (Math.round((reloadEndTime - reloadStartTime) / 10000.0) / 100.0) + " ms");

        if(postTimings) {
            System.out.println("  * Full Reload took: " + (reloadEndTime - reloadStartTime) / 1000 + " mircoseconds\n### Reload Migoto Shader Fix end ======");
        }

        return "";
    }

    public ArrayList<String> filterFolderStructure(String category, String section) {
        long filterStartTime = System.nanoTime();

        ArrayList<String> listData = new ArrayList<>();
        if(section == null) {
            section = "";
        }
        if(category == null) {
            category = "";
        }

        if(category.equals("Skin")) {
            if(section.equals("Character") || section.equals("All")) {
                ArrayList<String> keys = new ArrayList<>(List.of(characterMap.keySet().toArray(new String[0])));
                ArrayList<String> tempData = new ArrayList<>();
                characterNameAffectsMap = new HashMap<>();

                for(int i = 0; i < keys.size(); i++) {
                    ArrayList<Folder> alFolders = characterMap.get(keys.get(i));
                    String name = data.getAlCharacters().get(alFolders.get(0).getIndex());
                    name = name.replaceAll("(\\p{Ll})(\\p{Lu})", "$1 $2");
                    tempData.add(name + " (" + (characterMap.getOrDefault(keys.get(i), new ArrayList<>()).size()) + ")");
                    characterNameAffectsMap.putIfAbsent(name, keys.get(i));
                }

                Collections.sort(tempData);
                listData.addAll(tempData);
            }

            if(section.equals("Weapon") || section.equals("All")) {
                ArrayList<String> keys = new ArrayList<>(List.of(weaponMap.keySet().toArray(new String[0])));
                ArrayList<String> tempData = new ArrayList<>();
                weaponNameAffectsMap = new HashMap<>();

                for(int i = 0; i < keys.size(); i++) {
                    ArrayList<Folder> alFolders = weaponMap.get(keys.get(i));
                    String name = "";
                    switch(alFolders.get(0).getSection()) {
                        case "Bow" ->
                            name = data.getAlBows().get(alFolders.get(0).getIndex());
                        case "Catalyst" ->
                            name = data.getAlCatalysts().get(alFolders.get(0).getIndex());
                        case "Claymore" ->
                            name = data.getAlClaymores().get(alFolders.get(0).getIndex());
                        case "Polearm" ->
                            name = data.getAlPolearms().get(alFolders.get(0).getIndex());
                        case "Sword" ->
                            name = data.getAlSwords().get(alFolders.get(0).getIndex());
                    }
                    name = name.replaceAll("(\\p{Ll})(\\p{Lu})", "$1 $2");
                    tempData.add(name + " (" + (weaponMap.getOrDefault(keys.get(i), new ArrayList<>()).size()) + ")");
                    weaponNameAffectsMap.putIfAbsent(name, keys.get(i));
                }

                Collections.sort(tempData);
                listData.addAll(tempData);
            }
        } else if(category.equals("Mod")) {
            ArrayList<Folder> mods = new ArrayList<>(modList);

            for(int i = 0; i < mods.size(); i++) {
                listData.add(mods.get(i).getModName());
            }

            Collections.sort(listData);
        } else if(category.equals("Installed")) {
            for(String key : checksumInstalledFolderMap.keySet()) {
                listData.add(checksumInstalledFolderMap.get(key).getModName());
            }

            for(Folder folder : folderInstalledShaderFixMap.keySet()) {
                listData.add(folder.getModName());
            }

            Collections.sort(listData);
        }

        long filterEndTime = System.nanoTime();
        if(postTimings) {
            System.out.println("Filtering took: " + (filterEndTime - filterStartTime) / 1000 + " mircoseconds\n------------");
        }

        return listData;
    }

    public ArrayList<String> searchKeyword(ArrayList<String> alString, String keyword) {
        ArrayList<String> listData = new ArrayList<>(alString);

        for(int i = listData.size() - 1; i >= 0; i--) {
            if(!listData.get(i).toLowerCase().contains(keyword.toLowerCase())) {
                listData.remove(i);
            }
        }

        Collections.sort(listData);

        return listData;
    }

    public ArrayList<Folder> getFoldersFromCachedHashMapWithName(String name) {
        return getFoldersFromCachedHashMapWithAffects(getAffectsFromName(name));
    }

    public ArrayList<Folder> getFoldersFromCachedHashMapWithAffects(String affects) {
        if(affects.equals("")) {
            return new ArrayList<>();
        }
        ArrayList<Folder> alFolder = characterMap.getOrDefault(affects, new ArrayList<>());

        if(alFolder.isEmpty()) {
            alFolder = weaponMap.getOrDefault(affects, new ArrayList<>());
        }

        return alFolder;
    }

    public Folder getFolderFromModAl(String name) {
        for(Folder folder : modList) {
            if(folder.getModName().equals(name)) {
                return folder;
            }
        }

        return null;
    }

    public Folder getFolderFromInstalledHashMapWithName(String name) {
        for(String key : checksumInstalledFolderMap.keySet()) {
            if(checksumInstalledFolderMap.get(key).getModName().equals(name)) {
                return checksumInstalledFolderMap.get(key);
            }
        }

        // If didn't return then it's possible that it's shader only
        for(Folder key : folderInstalledShaderFixMap.keySet()) {
            if(key.getModName().equals(name)) {
                return key;
            }
        }

        return null;
    }

    public Folder getFolderFromInstalledHashMapWithChecksum(String checksum) {
        for(String key : checksumInstalledFolderMap.keySet()) {
            if(key.equals(checksum)) {
                return checksumInstalledFolderMap.get(key);
            }
        }

        return null;
    }

    public boolean isShaderFixInstalled(File file) {
        String checksum = getShaderFixChecksum(file);
        return checksumInstalledShaderFixMap.containsKey(checksum) && file.getName().equals(checksumInstalledShaderFixMap.get(checksum).getName());
    }

    public void openFolderInExplorer(Folder folder) {
        try {
            desktop.open(folder.getFolderData());
        } catch(IllegalArgumentException iae) {
            System.out.println("File Not Found");
        } catch(IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void checkIfInstalled() {
        long checkStartTime = System.nanoTime();

        if(migotoModFolderStructure == null || migotoShaderFixFolderStructure == null || modFolderStructure == null) {
            System.err.println("Couldn't check if installed, because one or more folder structures are null! (checkIfInstalled)");
            return;
        }

        ArrayList<String> alChecksum = new ArrayList<>();
        HashMap<String, ArrayList<Folder>> checksumAlMap = new HashMap<>();
        checksumInstalledFolderMap = new HashMap<>();
        folderInstalledShaderFixMap = new HashMap<>();
        checksumInstalledShaderFixMap = new HashMap<>();

        // Get the checksums of all installed mods and add register the checksum in aL and the checksum, folder in a hashmap
        for(Folder installedFolder : migotoModFolderStructure.getFolders()) {
            alChecksum.add(installedFolder.getChecksum());
            checksumInstalledFolderMap.putIfAbsent(installedFolder.getChecksum(), installedFolder);
            installedFolder.setInstalled(true);
        }

        // Create a hashmap containing every installed shader fix 
        for(File shaderFix : migotoShaderFixFolderStructure.getFiles()) {
            checksumInstalledShaderFixMap.putIfAbsent(getShaderFixChecksum(shaderFix), shaderFix);
        }

        // Check every cached folder if own checksum matches up with some installed one and put it in the checksumAlMap or check if shader fixes are installed
        for(Folder cachedFolder : modFolderStructure.getFolders()) {
            if(cachedFolder.isShaderFixOnly()) {
                double fileCount = 0;

                for(File file : cachedFolder.getAlShaderFixes()) {
                    if(isShaderFixInstalled(file)) {
                        fileCount++;
                    }
                }

                if(fileCount != 0 && fileCount != cachedFolder.getAlShaderFixes().size()) {
                    System.out.println(cachedFolder.getFolderData().getName() + " has not every shader fix installed! (checkIfInstalled)");
                    cachedFolder.setPartial(true);
                } else if(fileCount != 0) {
                    cachedFolder.setInstalled(true);
                    folderInstalledShaderFixMap.putIfAbsent(cachedFolder, cachedFolder.getAlShaderFixes());
                }
            } else if(alChecksum.contains(cachedFolder.getChecksum())) {
                ArrayList<Folder> alFolder = checksumAlMap.getOrDefault(cachedFolder.getChecksum(), new ArrayList<>());
                alFolder.add(cachedFolder);
                checksumAlMap.putIfAbsent(cachedFolder.getChecksum(), alFolder);
            }
        }

        // Check for any duplicates and tag after tag mod as installed
        for(String key : checksumAlMap.keySet()) {
            if(key == null) {
                System.err.println("Checksum in checksumAlMap was null while checking installed folders (checkIfInstalled)");
                continue;
            }

            Folder folder = null;
            if(checksumAlMap.get(key).size() > 1) {
                int bestDiff = Integer.MAX_VALUE;

                for(Folder installedFolder : checksumAlMap.get(key)) {
                    if(installedFolder.getModName() == null || checksumInstalledFolderMap.get(key).getModName() == null) {
                        continue;
                    }

                    int difference = Math.abs(installedFolder.getModName().compareTo(checksumInstalledFolderMap.get(key).getModName()));
                    // System.out.println(installedFolder.getModName() + " (" + difference + ") - " + key);

                    if(difference < bestDiff) {
                        folder = installedFolder;
                        bestDiff = difference;
                    }
                    if(difference == 0) {
                        break;
                    }
                }
            } else {
                folder = checksumAlMap.get(key).get(0);
            }

            if(folder != null) {
                Folder installedFolder = checksumInstalledFolderMap.get(key);
                installedFolder.setCached(true);
                installedFolder.setAlShaderFixes(folder.getAlShaderFixes());

                for(File shaderFix : folder.getAlShaderFixes()) {
                    if(!isShaderFixInstalled(shaderFix)) {
                        folder.setPartial(true);
                        System.out.println(folder.getFolderData().getName() + " has not every shader fix installed! (checkIfInstalled)");
                        break;
                    }
                }
                folder.setInstalled(!folder.isPartial());

            } else {
                System.err.println("No folder was found for checksum: " + key);
            }
            // System.out.println("+ Installed: " + folder.getModName());
        }

        long checkEndTime = System.nanoTime();
        if(postTimings) {
            System.out.println("    Installed check took: " + (checkEndTime - checkStartTime) / 1000 + " mircoseconds");
        }
    }

    public void deleteMod(Folder folder) {
        suppressFileWatcher = true;

        if(!folder.isShaderFixOnly()) { // Only delete mod files if folder isn't only shader fixes
            String key = folder.getChecksum();
            Folder tempFolder = checksumInstalledFolderMap.get(key);

            if(tempFolder == null) {
                System.err.println("There was no matching installed folder found for checksum: " + key);
                suppressFileWatcher = false;
                return;
            }

            File toDelete = tempFolder.getFolderData();
            // System.out.println("Deleting: " + toDelete.getPath());
            deleteDirectory(toDelete);
        }

        File migotoShaderFixDestination = new File(migotoShaderFixDir.getPath() + "\\");
        if(!folder.getAlShaderFixes().isEmpty()) {
            // System.out.println("Deleting Shader Fixes");

            for(File file : folder.getAlShaderFixes()) {
                File newFile = new File(migotoShaderFixDestination + "\\" + file.getName());
                newFile.delete();
            }
        }

        suppressFileWatcher = false;
    }

    private void deleteDirectory(File pFile) {
        for(File file : pFile.listFiles()) {
            if(file.isDirectory()) {
                deleteDirectory(file);
            }

            file.delete();
        }

        pFile.delete();
    }

    public void installMod(Folder folder, JProgressBar progressBar, JLabel label) {
        // System.out.println("Installing: " + folder.getModName());
        suppressFileWatcher = true;

        if(folder.getConfigFile() == null && !folder.isShaderFixOnly()) {
            System.out.println(folder.getFolderData().getName() + " has no config file and isn't a shader fix only folder! (installMod)");

            suppressFileWatcher = false;
            return;
        }

        loadingBar = progressBar;
        loadingLabel = label;

        File migotoModDestination = new File(migotoModDir.getPath() + "\\" + folder.getFolderData().getName());
        File migotoShaderFixDestination = new File(migotoShaderFixDir.getPath() + "\\");

        // System.out.println("Mod Source: " + modSource.getPath());
        // System.out.println("Mod Destination: " + migotoModDestination.getPath());
        // System.out.println("Shader Fix Destination: " + migotoShaderFixDestination.toPath());
        try {
            if(folder.getConfigFile() != null) {
                File modSource = new File(folder.getConfigFile().getParent());
                copyDirectory(modSource, migotoModDestination);
            }

            for(File file : folder.getAlShaderFixes()) {
                File newFile = new File(migotoShaderFixDestination + "\\" + file.getName());
                copyFile(file, newFile);
            }
        } catch(IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        suppressFileWatcher = false;
    }

    public void copyMod(File file, JProgressBar progressBar, JLabel label) {
        boolean wasArchive = false;
        suppressFileWatcher = true;
        
        if(!file.isDirectory()) {
            String sourceFileStr = file.getPath();
            String destinationStr = modDir.getPath();
            String newPath = "";
            wasArchive = true;

            try {
                newPath = new ArchiveExtractor(sourceFileStr, destinationStr).extract();
            } catch(ArchiveExtractor.ExtractionException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }

            file = new File(newPath);
            if(!file.exists() || !file.isDirectory()) {
                System.err.println("Error happened while extracting (copyMod)");
                suppressFileWatcher = false;
                return;
            }
        }

        Folder importedFolder = analyzeFolderStructure(file);
        importedFolder = folderEvaluationHandler(importedFolder);

        if(importedFolder.getConfigFile() == null && !importedFolder.isShaderFixOnly()) {
            System.err.println("Doesn't contain any .ini files and is no shader fixes only either! (copyMod)");
            suppressFileWatcher = false;
            return;
        }

        String folderName = importedFolder.getFolderData().getName();
        File destination = new File(modDir.getPath() + "\\" + folderName);

        if(!wasArchive) {
            folderName = getUnusedName(destination.getPath());

            destination = new File(modDir.getPath() + "\\" + folderName);

            try {
                // System.out.println("Import: " + file.getName() + " | " + destination);
                copyDirectory(file, destination);
            } catch(IOException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if(autoInstall) {
            boolean foundMatching = false;
            
            if(!importedFolder.isShaderFixOnly()) {
                for(String checksum : checksumInstalledFolderMap.keySet()) {
                    if(checksumInstalledFolderMap.get(checksum).getAffects().equals(importedFolder.getAffects())) {
                        if(overwriteInstall) {
                            deleteMod(checksumInstalledFolderMap.get(checksum));
                        } else {
                            foundMatching = true;
                            break;
                        }
                    }
                }
            } else {
                for(File shaderFix : importedFolder.getAlShaderFixes()) {
                    if(isShaderFixInstalled(shaderFix)) {
                        if(overwriteInstall) {
                            checksumInstalledShaderFixMap.get(getShaderFixChecksum(shaderFix)).delete();
                        } else {
                            foundMatching = true;
                            break;
                        }
                    }
                }

            }
            
            if(!foundMatching) {
                Folder toInstall = analyzeFolderStructure(destination);
                toInstall = folderEvaluationHandler(toInstall);
                installMod(toInstall, progressBar, label);
            }
        }
        
        suppressFileWatcher = false;
    }

    private String getUnusedName(String filePath) {
        File file = new File(filePath);
        
        if(file.exists()) {
            filePath = getUnusedName(filePath + "_copy");
        }

        return Paths.get(filePath).getFileName().toString();
    }

    private void copyDirectory(File sourceDirectory, File destinationDirectory) throws IOException {
        if(!destinationDirectory.exists()) {
            destinationDirectory.mkdir();
        }
        for(String f : sourceDirectory.list()) {
            copyDirectoryCompatibityMode(new File(sourceDirectory, f), new File(destinationDirectory, f));
        }
    }

    private void copyDirectoryCompatibityMode(File source, File destination) throws IOException {
        if(source.isDirectory()) {
            copyDirectory(source, destination);
        } else {
            copyFile(source, destination);
        }
    }

    private void copyFile(File sourceFile, File destinationFile) throws IOException {
        // Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        try {
            FileInputStream inFile = new FileInputStream(sourceFile);
            FileOutputStream outFile = new FileOutputStream(destinationFile);

            FileChannel inChannel = inFile.getChannel();
            FileChannel outChannel = outFile.getChannel();

            double inputSize = (double) inChannel.size();

            byte[] buffer = new byte[1024 * 4];
            int lengthRead;

            int counter = 0;
            boolean updateProgress = false;
            if(loadingBar != null) {
                updateProgress = true;
            }

            if(loadingLabel != null) {
                loadingLabel.setText("Copying " + sourceFile.getName());
            }

            while((lengthRead = inFile.read(buffer)) > 0) {
                outFile.write(buffer, 0, lengthRead);
                outFile.flush();

                counter++;
                if(counter == 1024 * 4) {
                    counter = 0;
                    if(updateProgress) {
                        // System.out.println(Math.round(outChannel.size() / inputSize * 100.0) + "%");
                        loadingBar.setValue((int) Math.round(outChannel.size() / inputSize * 100.0));
                    }
                }
            }

            inChannel.close();
            outChannel.close();
            inFile.close();
            outFile.close();

            if(updateProgress) {
                // System.out.println("Finished copying " + sourceFile.getName());
                loadingBar.setValue(100);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

}
