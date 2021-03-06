package com.example.jacob.androidvirtualassist;

/**
 * Created by Jacob on 24/04/2016.
 */
public class FirebaseCloudSettings {

    private String SERVER_CLOUD_ADDRESS = "kfyDAbVS4bqjykeWAF3.kfyPXqGvFtXcti6UnvF.devEX1Sg2Txs1CgVuW4.kfyPRoQjMLuGh9eZ1Yf";

    // Hard-coded the Cloud password associated with this Cloud address
    private String SERVER_CLOUD_PASSWORD = "JAZztCRMwnARauw31n2p";

    // For Cloud connections, hard-coded the Cloud address for the Viewer.
    private String VIEWER_CLOUD_ADDRESS = "kfyDAbVS4bqjykeWAF3.kfyPXqGvFtXcti6UnvF.devEX1Sg2Txs1CgVuW4.kfyPRAVFPBM7EYheTsr";

    // Hard-code the Cloud password for the viewer associated with this Cloud address
    private String VIEWER_CLOUD_PASSWORD = "VJhLFj90iyrtJ3b0ujeo";

    // Hard-code the Cloud address of the Server (peer) to connect to.
    private String PEER_CLOUD_ADDRESS = "kfyDAbVS4bqjykeWAF3.kfyPXqGvFtXcti6UnvF.devEX1Sg2Txs1CgVuW4.kfyPRoQjMLuGh9eZ1Yf";


    private String key;

    public FirebaseCloudSettings(){
        //Required for use with firebase and Jackson}
    }

    public FirebaseCloudSettings(String server_cloud_address, String server_cloud_password,
                         String viewer_cloud_address, String viewer_cloud_password, String peer_cloud_address){
        this.SERVER_CLOUD_ADDRESS = server_cloud_address;
        this.SERVER_CLOUD_PASSWORD = server_cloud_password;
        this.VIEWER_CLOUD_ADDRESS = viewer_cloud_address;
        this.VIEWER_CLOUD_PASSWORD = viewer_cloud_password;
        this.PEER_CLOUD_ADDRESS = peer_cloud_address;

    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSERVER_CLOUD_ADDRESS() {
        return SERVER_CLOUD_ADDRESS;
    }

    public void setSERVER_CLOUD_ADDRESS(String SERVER_CLOUD_ADDRESS) {
        this.SERVER_CLOUD_ADDRESS = SERVER_CLOUD_ADDRESS;
    }

    public String getSERVER_CLOUD_PASSWORD() {
        return SERVER_CLOUD_PASSWORD;
    }

    public void setSERVER_CLOUD_PASSWORD(String SERVER_CLOUD_PASSWORD) {
        this.SERVER_CLOUD_PASSWORD = SERVER_CLOUD_PASSWORD;
    }

    public String getVIEWER_CLOUD_ADDRESS() {
        return VIEWER_CLOUD_ADDRESS;
    }

    public void setVIEWER_CLOUD_ADDRESS(String VIEWER_CLOUD_ADDRESS) {
        this.VIEWER_CLOUD_ADDRESS = VIEWER_CLOUD_ADDRESS;
    }

    public String getVIEWER_CLOUD_PASSWORD() {
        return VIEWER_CLOUD_PASSWORD;
    }

    public void setVIEWER_CLOUD_PASSWORD(String VIEWER_CLOUD_PASSWORD) {
        this.VIEWER_CLOUD_PASSWORD = VIEWER_CLOUD_PASSWORD;
    }

    public String getPEER_CLOUD_ADDRESS() {
        return PEER_CLOUD_ADDRESS;
    }

    public void setPEER_CLOUD_ADDRESS(String PEER_CLOUD_ADDRESS) {
        this.PEER_CLOUD_ADDRESS = PEER_CLOUD_ADDRESS;
    }
}
