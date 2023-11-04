
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
public class GBFileGetter {
    
    class GBFileDataArray {

        public ArrayList<GBFileData> _aFiles;
    }

    private final String gameBananaAPIURL = "https://gamebanana.com/apiv11";
    private final String section = "Mod";
    private final String queryParams = "?_csvProperties=_aFiles";
    private int status;
    
    public ArrayList<GBFileData> getFileData(String modURL) {
        ArrayList<GBFileData> alResult = new ArrayList<>();

        String modID = modURL.substring(modURL.lastIndexOf("/") + 1);

        String urlString = gameBananaAPIURL;
        urlString += "/" + section;
        urlString += "/" + modID;
        urlString += queryParams;

        try {
            URL filesListURL = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) filesListURL.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);

            status = con.getResponseCode();

            Reader streamReader = null;

            if(status > 299) {
                streamReader = new InputStreamReader(con.getErrorStream());
            } else {
                streamReader = new InputStreamReader(con.getInputStream());
            }

            StringBuilder content;

            try(BufferedReader in = new BufferedReader(streamReader)) {
                String inputLine;
                content = new StringBuilder();
                while((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
            }
            con.disconnect();

            if(status <= 299) {
                Gson gson = new Gson();
                GBFileDataArray fileArr = gson.fromJson(content.toString(), GBFileDataArray.class);

                Collections.sort(fileArr._aFiles, Comparator.comparing(GBFileData::getDateAdded));
                Collections.reverse(fileArr._aFiles);
                alResult.addAll(fileArr._aFiles);
            } else {
                return null;
            }
            
        } catch(MalformedURLException ex) {
            Logger.getLogger(GBFileGetter.class.getName()).log(Level.SEVERE, null, ex);
        } catch(ProtocolException ex) {
            Logger.getLogger(GBFileGetter.class.getName()).log(Level.SEVERE, null, ex);
        } catch(IOException ex) {
            Logger.getLogger(GBFileGetter.class.getName()).log(Level.SEVERE, null, ex);
        }

        return alResult;
    }

    public int getStatus() {
        return status;
    }

}
