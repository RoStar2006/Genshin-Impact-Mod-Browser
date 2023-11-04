
import java.io.File;
import java.util.ArrayList;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Romeo
 */
public class Folder {
    private ArrayList<File> files = new ArrayList<>();
    private ArrayList<Folder> folders = new ArrayList<>();
    private File folderData;
    private String section; // Is it a character or a bow, etc
    private String category; // is it a skin or mod
    private String affects; // What it affects, character name / weapon name
    private String modName;
    private File configFile;
    private File preview;
    private String checksum;
    private boolean installed;
    private boolean partial;
    private boolean cached;
    private boolean shaderFixOnly;
    private ArrayList<File> alShaderFixes = new ArrayList<>();
    private int index = -1; // place in al in data
    
    public Folder(File folderData) {
        this.folderData = folderData;
        installed = false;
        cached = false;
        shaderFixOnly = false;
        partial = false;
    }

    public ArrayList<File> getFiles() {
        return files;
    }

    public ArrayList<Folder> getFolders() {
        return folders;
    }

    public ArrayList<File> getAlShaderFixes() {
        return alShaderFixes;
    }

    public File getFolderData() {
        return folderData;
    }

    public String getSection() {
        return section;
    }

    public String getCategory() {
        return category;
    }

    public String getAffects() {
        return affects;
    }

    public String getModName() {
        return modName;
    }

    public File getConfigFile() {
        return configFile;
    }

    public File getPreview() {
        return preview;
    }

    public int getIndex() {
        return index;
    }

    public String getChecksum() {
        return checksum;
    }

    public boolean isInstalled() {
        return installed;
    }

    public boolean isPartial() {
        return partial;
    }

    public boolean isCached() {
        return cached;
    }

    public boolean isShaderFixOnly() {
        return shaderFixOnly;
    }
    
    public boolean hasShaderFixes() {
        return !alShaderFixes.isEmpty();
    }
    
    public void setFolderData(File folderData) {
        this.folderData = folderData;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setAffects(String affects) {
        this.affects = affects;
    }

    public void setModName(String modName) {
        this.modName = modName;
    }

    public void setConfigFile(File configFile) {
        this.configFile = configFile;
    }

    public void setPreview(File preview) {
        this.preview = preview;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setAlShaderFixes(ArrayList<File> alShaderFixes) {
        this.alShaderFixes = new ArrayList<>(alShaderFixes);
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    public void setPartial(boolean partial) {
        this.partial = partial;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public void setShaderFixOnly(boolean shaderFixOnly) {
        this.shaderFixOnly = shaderFixOnly;
    }
    
    public void setFolder(int index, Folder folder) {
        folders.set(index, folder);
    }
    
    public void addFile(File file) {
        files.add(file);
    }
    
    public void removeFile(File file) {
        files.remove(file);
    }
    
    public void addShaderFix(File shaderFix) {
        alShaderFixes.add(shaderFix);
    }
    
    public void removeShaderFix(File shaderFix) {
        alShaderFixes.remove(shaderFix);
    }
    
    public void addDirectory(Folder directory) {
        folders.add(directory);
    }
    
    public void removeDirectory(Folder directory) {
        folders.remove(directory);
    }
    
    public String toString() {
        return "Mod Name: " + modName + "\nAffects: " + affects + "\nCategory: " + category + "\nSection: " + section + "\nIndex: " + index + "\nConfig file: " + configFile + "\nChecksum: " + checksum + "\nShader Fixes: " + alShaderFixes.size() + "\nShader Fixes only: " + shaderFixOnly;
    }
}
