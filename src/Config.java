
import java.io.FileInputStream;
import java.util.Properties;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author Romeo
 */
public class Config {

    private Properties appProps;
    private String rootPath;
    private String configPath;
    private File configFile;

    public Config() {
        appProps = new Properties();
        rootPath = System.getProperty("user.dir");
        configPath = rootPath + "\\app.properties";

        configFile = new File(configPath);

        loadConfigFile();
    }

    public void loadConfigFile() {
        try {
            if(!configFile.isFile()) {
                configFile.createNewFile();
            }

            appProps.load(new FileInputStream(configFile));
        } catch(FileNotFoundException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        } catch(IOException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void storeProperties() {
        try {
            appProps.store(new FileWriter(configPath), "");
        } catch(IOException ex) {
            Logger.getLogger(Config.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public File getModPath() {
        String pathStr = appProps.getProperty("ModPath");

        if(pathStr == null) {
            pathStr = rootPath + "\\Mods";
            setModPath(pathStr);
            System.err.println("No mod path in properties");
        }

        File pathFile = new File(pathStr);
        if(!pathFile.isDirectory() && pathFile.exists()) {
            pathFile.delete();
            pathFile.mkdir();
        } else if(!pathFile.exists()) {
            pathFile.mkdir();
        }

        return pathFile;
    }

    public void setModPath(String path) {
        appProps.setProperty("ModPath", path);
        storeProperties();
    }

    public File get3DMigotoModPath() {
        String pathStr = appProps.getProperty("3DMigotoModPath");

        if(pathStr == null) {
            set3DMigotoModPath("");
            pathStr = "";
            System.err.println("No 3DMigoto mod path in properties");
        }

        File pathFile = new File(pathStr);
        if(!pathFile.isDirectory() && pathFile.exists()) {
            pathFile.delete();
            pathFile.mkdir();
        } else if(!pathFile.exists()) {
            pathFile.mkdir();
        }

        return pathFile;
    }

    public void set3DMigotoModPath(String path) {
        appProps.setProperty("3DMigotoModPath", path);
        storeProperties();
    }

    public File get3DMigotoShaderFixPath() {
        String pathStr = appProps.getProperty("3DMigotoShaderFixPath");

        if(pathStr == null) {
            set3DMigotoShaderFixPath("");
            pathStr = "";
            System.err.println("No 3DMigoto shader fix path in properties");
        }

        File pathFile = new File(pathStr);
        if(!pathFile.isDirectory() && pathFile.exists()) {
            pathFile.delete();
            pathFile.mkdir();
        } else if(!pathFile.exists()) {
            pathFile.mkdir();
        }

        return pathFile;
    }

    public void set3DMigotoShaderFixPath(String path) {
        appProps.setProperty("3DMigotoShaderFixPath", path);
        storeProperties();
    }
    
    public boolean getAutoInstall() {
        String resultString = appProps.getProperty("AutoInstall");
        boolean defaultState = false;

        if(resultString == null) {
            setAutoInstall(defaultState);
            storeProperties();

            return defaultState;
        } else {
            return Boolean.parseBoolean(resultString);
        }
    }

    public void setAutoInstall(boolean state) {
        appProps.setProperty("AutoInstall", String.valueOf(state));
        storeProperties();
    }
    
    public boolean getOverwriteInstall() {
        String resultString = appProps.getProperty("OverwriteInstall");
        boolean defaultState = false;

        if(resultString == null) {
            setOverwriteInstall(defaultState);
            storeProperties();

            return defaultState;
        } else {
            return Boolean.parseBoolean(resultString);
        }
    }

    public void setOverwriteInstall(boolean state) {
        appProps.setProperty("OverwriteInstall", String.valueOf(state));
        storeProperties();
    }
}
