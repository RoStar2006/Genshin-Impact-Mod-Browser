
import java.util.ArrayList;
import java.util.List;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author Romeo
 */
public class GBFileData {

    private int _idRow;
    private String _sFile;
    private int _nFilesize;
    private String _sDescription;
    private long _tsDateAdded;
    private int _nDownloadCount;
    private String _sAnalysisState;
    private String _sDownloadUrl;
    private String _sMd5Checksum;
    private String _sClamAvResult;
    private String _sAnalysisResult;
    private boolean _bContainsExe;

    public long getDateAdded() {
        return _tsDateAdded;
    }

    public String getSize() {
        double size = _nFilesize;
        ArrayList<String> alSizeScale = new ArrayList<>(List.of("B", "KB", "MB", "GB", "TB", "PB")); // Who the heck download petabytes worth of files?
        int counter = 0;

        while(size > 1000) {
            counter++;
            size /= 1000; // 1024 for kibi, mebi, gibi
        }

        String sizeStr = Math.round(size * 10.0) / 10.0 + " " + alSizeScale.get(counter);

        return sizeStr;
    }

    public String getDownloadURL() {
        return _sDownloadUrl;
    }

    public String getFileName() {
        return _sFile;
    }
}
