
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import java.io.File;
import java.nio.file.Paths;

public class Download {

    private File file;
    private Downloader downloader;
    
    public Download(String localPath, String fileName, String remoteURL, JProgressBar progressBar, JLabel label) {
        file = new File(getUnusedPath(localPath + "\\" + fileName));
        downloader = new Downloader(file.getPath(), remoteURL, progressBar, label);
    }

    public File getFile() {
        return file;
    }
    
    public String getErrorMsg() {
        if(downloader != null) {
            return downloader.getErrorMsg();
        }
        
        return "No download initialized!";
    }
    
    private String getUnusedPath(String filePath) {
        File file = new File(filePath);
        
        if(file.exists()) {
            String newPath = filePath;
            
            if(newPath.contains(".")) {
                newPath = filePath.substring(0, filePath.lastIndexOf("."));
                newPath += "_copy";
                newPath += filePath.substring(filePath.lastIndexOf("."), filePath.length());
            }
            
            filePath = getUnusedPath(newPath);
        }

        return filePath;
    }

    private interface RBCWrapperDelegate {

        // The RBCWrapperDelegate receives rbcProgressCallback() messages
        // from the read loop.  It is passed the progress as a percentage
        // if known, or -1.0 to indicate indeterminate progress.
        // 
        // This callback hangs the read loop so a smart implementation will
        // spend the least amount of time possible here before returning.
        // 
        // One possible implementation is to push the progress message
        // atomically onto a queue managed by a secondary thread then
        // wake that thread up.  The queue manager thread then updates
        // the user interface progress bar.  This lets the read loop
        // continue as fast as possible.
        public void rbcProgressCallback(RBCWrapper rbc, double progress);
    }

    private static final class Downloader implements RBCWrapperDelegate {

        private JProgressBar progressBar;
        private JLabel label;
        private long expected;
        private String totalStr;
        
        private String errorMsg;
        
        public Downloader(String localPath, String remoteURL, JProgressBar progressBar, JLabel label) {
            this.progressBar = progressBar;
            this.label = label;
            
            FileOutputStream fos;
            ReadableByteChannel rbc;
            URL url;

            try {
                url = new URL(remoteURL);
                
                expected = contentLength(url);
                totalStr = getSize(expected);
                
                rbc = new RBCWrapper(Channels.newChannel(url.openStream()), expected, this);
                fos = new FileOutputStream(localPath);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch(Exception e) {
                errorMsg = "Uh oh: " + e.getMessage();
                System.err.println(errorMsg);
            }
        }

        public String getErrorMsg() {
            return errorMsg;
        }

        public void rbcProgressCallback(RBCWrapper rbc, double progress) {
            if(expected != -1) {
                progressBar.setValue((int)Math.round(progress));

                double lProgress = Math.round(progress * 10.0) / 10.0;
                String readStr = getSize(rbc.getReadSoFar());

                label.setText("Downloading: " + readStr + "/" + totalStr + " ~ (" + lProgress + "%)");
            } else {
                progressBar.setValue(0);
                label.setText("Downloading, couldn't estimate size");
            }
            // System.out.println(String.format("download progress %d bytes received, %.02f%%", rbc.getReadSoFar(), progress));
        }
        
        public String getSize(long pSize) {
            double size = pSize;
            ArrayList<String> alSizeScale = new ArrayList<>(List.of("B", "KB", "MB", "GB", "TB", "PB")); // Who the heck download petabytes worth of files?
            int counter = 0;

            while(size > 1000) {
                counter++;
                size /= 1000; // 1024 for kibi, mebi, gibi
            }

            String sizeStr = Math.round(size * 10.0) / 10.0 + " " + alSizeScale.get(counter);

            return sizeStr;
        }

        private int contentLength(URL url) {
            HttpURLConnection connection;
            int contentLength = -1;

            try {
                HttpURLConnection.setFollowRedirects(true);

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");

                contentLength = connection.getContentLength();
            } catch(Exception e) {
            }

            return contentLength;
        }
    }

    private static final class RBCWrapper implements ReadableByteChannel {

        private RBCWrapperDelegate delegate;
        private long expectedSize;
        private ReadableByteChannel rbc;
        private long readSoFar;

        RBCWrapper(ReadableByteChannel rbc, long expectedSize, RBCWrapperDelegate delegate) {
            this.delegate = delegate;
            this.expectedSize = expectedSize;
            this.rbc = rbc;
        }

        public void close() throws IOException {
            rbc.close();
        }

        public long getReadSoFar() {
            return readSoFar;
        }

        public boolean isOpen() {
            return rbc.isOpen();
        }

        public int read(ByteBuffer bb) throws IOException {
            int n;
            double progress;

            if((n = rbc.read(bb)) > 0) {
                readSoFar += n;
                progress = expectedSize > 0 ? (double) readSoFar / (double) expectedSize * 100.0 : -1.0;
                delegate.rbcProgressCallback(this, progress);
            }

            return n;
        }
    }
}
