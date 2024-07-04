package com.example.edrsystem.static_analysis;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class VirusTotalClient {
    private static final Logger logger = Logger.getLogger(VirusTotalClient.class.getName());
    private static String API_KEY ="ec5215b7e247f59169e4778e6105156b478885e9ce948ae65ed1ffdc8a5d14de";
    private final HttpClient httpClient;

    public VirusTotalClient() {
        this.httpClient = HttpClients.createDefault();
    }

    public boolean checkFileHash(String hash) {
        String url = "https://www.virustotal.com/vtapi/v2/file/report";
        HttpPost request = new HttpPost(url);

        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("apikey", API_KEY));
            params.add(new BasicNameValuePair("resource", hash));
            request.setEntity(new UrlEncodedFormEntity(params));

            HttpResponse response = httpClient.execute(request);
            String result = EntityUtils.toString(response.getEntity());
            JSONObject jsonResponse = new JSONObject(result);

            if (jsonResponse.has("positives") && jsonResponse.has("total")) {
                int positives = jsonResponse.getInt("positives");
                int total = jsonResponse.getInt("total");
                logger.info(String.format("VirusTotal result for %s: %d/%d", hash, positives, total));
                return positives > 0;
            } else if (jsonResponse.has("response_code")) {
                int responseCode = jsonResponse.getInt("response_code");
                if (responseCode == 0) {
                    logger.info("File not found in VirusTotal database: " + hash);
                    return false;
                } else {
                    logger.warning("Unexpected response from VirusTotal: " + result);
                    return false;
                }
            } else {
                logger.warning("Unexpected response format from VirusTotal: " + result);
                return false;
            }
        } catch (Exception e) {
            logger.severe("Error checking file hash with VirusTotal: " + e.getMessage());
            return false;
        }
    }
}