package com.example.jacob.androidvirtualassist;

/**
 * Created by Jacob on 23/04/2016.
 */
public class CloudSettings {

       // For Cloud connections, hard-coded the Cloud address for the Server
    private static String SERVER_CLOUD_ADDRESS = "kfyDAbVS4bqjykeWAF3.kfyPXqGvFtXcti6UnvF.devEX1Sg2Txs1CgVuW4.kfyPRoQjMLuGh9eZ1Yf";

    // Hard-coded the Cloud password associated with this Cloud address
    private static String SERVER_CLOUD_PASSWORD = "JAZztCRMwnARauw31n2p";

    // For Cloud connections, hard-coded the Cloud address for the Viewer.
    private static String VIEWER_CLOUD_ADDRESS = "kfyDAbVS4bqjykeWAF3.kfyPXqGvFtXcti6UnvF.devEX1Sg2Txs1CgVuW4.kfyPRAVFPBM7EYheTsr";

    // Hard-code the Cloud password for the viewer associated with this Cloud address
    private static String VIEWER_CLOUD_PASSWORD = "VJhLFj90iyrtJ3b0ujeo";

    // Hard-code the Cloud address of the Server (peer) to connect to.
    private static String PEER_CLOUD_ADDRESS = "kfyDAbVS4bqjykeWAF3.kfyPXqGvFtXcti6UnvF.devEX1Sg2Txs1CgVuW4.kfyPRoQjMLuGh9eZ1Yf";

    public CloudSettings(){
        //Required for use with firebase and Jackson
    }

    public CloudSettings(String server_cloud_address, String server_cloud_password,
                         String viewer_cloud_address, String viewer_cloud_password, String peer_cloud_address){
        SERVER_CLOUD_ADDRESS = server_cloud_address;
        SERVER_CLOUD_PASSWORD = server_cloud_password;
        VIEWER_CLOUD_ADDRESS = viewer_cloud_address;
        VIEWER_CLOUD_PASSWORD = viewer_cloud_password;
        PEER_CLOUD_ADDRESS = peer_cloud_address;

    }


    public static String getServerCloudAddress() {
        return SERVER_CLOUD_ADDRESS;
    }

    public static void setServerCloudAddress(String serverCloudAddress) {
        SERVER_CLOUD_ADDRESS = serverCloudAddress;
    }

    public static String getServerCloudPassword() {
        return SERVER_CLOUD_PASSWORD;
    }

    public static void setServerCloudPassword(String serverCloudPassword) {
        SERVER_CLOUD_PASSWORD = serverCloudPassword;
    }

    public static String getViewerCloudAddress() {
        return VIEWER_CLOUD_ADDRESS;
    }

    public static void setViewerCloudAddress(String viewerCloudAddress) {
        VIEWER_CLOUD_ADDRESS = viewerCloudAddress;
    }

    public static String getViewerCloudPassword() {
        return VIEWER_CLOUD_PASSWORD;
    }

    public static void setViewerCloudPassword(String viewerCloudPassword) {
        VIEWER_CLOUD_PASSWORD = viewerCloudPassword;
    }

    public static String getPeerCloudAddress() {
        return PEER_CLOUD_ADDRESS;
    }

    public static void setPeerCloudAddress(String peerCloudAddress) {
        PEER_CLOUD_ADDRESS = peerCloudAddress;
    }
}
