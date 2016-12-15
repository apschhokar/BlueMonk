package edu.buffalo.rms.bluemountain.translator;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Process;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Created by aniruddh on 10/18/16.
 */

//Sync adapter will take care of logging each and every statement and then sending file to networked server
public class OpLogsSyncAdapter {
    private static String filename;
    private static File file;
    private static FileOutputStream fileOutputStream;
    private static final String TAG = "OpLogsSyncAdapter";
    private static final String upLoadServerUrl = "http://192.168.0.12:3000/uploadDbTableFileToServer";
    private static final String postServerUrl = "http://192.168.0.12:3000";
    private static final String streamingServerUrl = "http://192.168.0.12:5000";
    private static final String streamingServerHost = "192.168.0.12";
    private static final int streamingServerPort = 5000;
    private static ConcurrentLinkedQueue<HashMap<String, String>> queue;
    private static final int BLOCKING_QUEUE_LIMIT = 10;
    private final String TAG_TIME = Globals.TAG_TIME;

    private static String loggingMode = Globals.CURRENT_LOGGING_MODE;

    private static OpLogsSyncAdapter adapter;

    private OpLogsSyncAdapter(Context context, String databasName) {
        Log.d(TAG, "OpLogsSyncAdapter() constructor called");
        filename = "db"+ ".log";
        file = new File(context.getFilesDir(), filename);
        try {
            if (!file.exists()) {
                file.createNewFile();
                Log.d(TAG, "File created " + filename);
            }
            fileOutputStream = new FileOutputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //new StreamingSocketTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, null);
        //new FileWriterTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, fileOutputStream);
    }

    public static OpLogsSyncAdapter getAdapter(Context context, String databseName) {
        //Lazy initialization
        if (null == adapter) {
            synchronized (OpLogsSyncAdapter.class) {
                if (null == adapter)
                    adapter = new OpLogsSyncAdapter(context, databseName);
                queue = new ConcurrentLinkedQueue<HashMap<String, String>>();

            }
        }
        return adapter;
    }

//    public void log(String content) {
//        try {
//            content = String.valueOf(System.nanoTime()) + ":" + content + "\n";
//            //fileOutputStream.write(content.getBytes());
//            final String data = content;
//
//
//            Thread sendDataTask = new Thread(new Runnable() {
//                public void run() {
//                    //sendData(data);
////                    sendPostRequest(data);
////                    queue.add(data);
//                }
//            });
//            sendDataTask.start();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public void log(String content, long start, String method) {
        try {
            content = String.valueOf(System.nanoTime()) + ":" + content + "\n";
            //fileOutputStream.write(content.getBytes());
            final String data = content;
//            HashMap val = new HashMap<String, String>();
//            val.put(method + ":" + String.valueOf(start), content);
//            queue.add(val);

            fileOutputStream.write(content.getBytes());
            fileOutputStream.getChannel().force(Boolean.FALSE);

//            if(loggingMode.equals(Globals.FRAMEWORK_WITH_BLOCKING_FILE_LOGGING)) {
//                long end = System.nanoTime();
//                Log.d(TAG_TIME+method, String.valueOf(end-start));
//            }

//            final String finalMethodName = method;
//            final long startTime = start;
//
//            Thread sendDataTask = new Thread(new Runnable() {
//                public void run() {
//                    //sendData(data);
//                    sendPostRequest(data, startTime, finalMethodName);
////                    queue.add(data);
//                }
//            });
//            sendDataTask.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void close() {
//        try {
//            if(null != fileOutputStream) {
//                fileOutputStream.flush();
//                fileOutputStream.close();
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        if(file != null
//        && fileOutputStream != null){
//            Thread myUploadTask = new Thread(new Runnable(){
//                public void run(){
//                    //... do your uploadDbTableFileToServer task
//                    uploadFile(file.getPath(),upLoadServerUrl);
//                }
//            });
//            myUploadTask.start();
//
//        }

    }

    private static void uploadFile(String filePath, String uploadUrl) {
        int responseCode = -1;

        try {
            String sourceFileUri = filePath;

            HttpURLConnection connection;
            DataOutputStream dataOutputStream;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;
            File sourceFile = new File(sourceFileUri);

            if (sourceFile.isFile()) {

                try {
                    String upLoadServerUrl = uploadUrl;

                    // open a URL connection to the Servlet
                    FileInputStream fileInputStream = new FileInputStream(
                            sourceFile);
                    URL url = new URL(upLoadServerUrl);

                    // Open a HTTP connection to the URL
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setUseCaches(false);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Connection", "Keep-Alive");
                    connection.setRequestProperty("ENCTYPE",
                            "multipart/form-data");
                    connection.setRequestProperty("Content-Type",
                            "multipart/form-data;boundary=" + boundary);
                    connection.setRequestProperty("sampleFile", sourceFileUri);

                    dataOutputStream = new DataOutputStream(connection.getOutputStream());

                    dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
                    dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"sampleFile\";filename=\""
                            + sourceFileUri + "\"" + lineEnd);

                    dataOutputStream.writeBytes(lineEnd);

                    // create a buffer of maximum size
                    bytesAvailable = fileInputStream.available();

                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];

                    // read file and write it into form...
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    while (bytesRead > 0) {

                        dataOutputStream.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math
                                .min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0,
                                bufferSize);

                    }

                    // send multipart form data necesssary after file
                    // data...
                    dataOutputStream.writeBytes(lineEnd);
                    dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens
                            + lineEnd);

                    // Responses from the server (code and message)
                    responseCode = connection.getResponseCode();
                    String serverResponseMessage = connection
                            .getResponseMessage();

                    if (responseCode == 200) {
                        Log.d(TAG, "Upload success");
                    } else {
                        Log.d(TAG, "Upload ERROR");
                    }

                    // close the streams //
                    fileInputStream.close();
                    dataOutputStream.flush();
                    dataOutputStream.close();

                } catch (Exception e) {

                    // dialog.dismiss();
                    e.printStackTrace();

                }
                // dialog.dismiss();

            } // End else block


        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    public void sendPostRequest(String data, long start, String methodName) {
        Log.d(TAG,"inside sendPostRequest");
        StringBuffer responseString = new StringBuffer();
        try {
            //make JSONObject for POST request
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("sql", data);
            String payload = jsonParam.toString();

            URL url = new URL(postServerUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            writer.write(payload);
            writer.close();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String line;
            while ((line = br.readLine()) != null) {
                responseString.append(line);
            }
            br.close();
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (loggingMode.equals(Globals.FRAMEWORK_WITH_REMOTE_SQL)) {
            boolean toLog = true;
            if (data.contains("Count")) {
                toLog = false;
            }
            if (toLog) {
                long end = System.nanoTime();
                Log.d(TAG_TIME + "_" + methodName, String.valueOf(end - start));
            }
        }

        Log.d(TAG, responseString.toString());
    }

    public void sendPostRequest(String data) {
        StringBuffer responseString = new StringBuffer();
        try {
            //make JSONObject for POST request
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("sql", data);
            String payload = jsonParam.toString();

            URL url = new URL(postServerUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            writer.write(payload);
            writer.close();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String line;
            while ((line = br.readLine()) != null) {
                responseString.append(line);
            }
            br.close();
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, responseString.toString());
    }

    //TODO not working
//    private class StreamingSocketTask extends AsyncTask<ServerSocket, String, Void> {
//
//        @Override
//        protected Void doInBackground(ServerSocket... params) {
//            Log.i(TAG, "Inside StreamingSocket doInBackground");
//
//            try {
//                Socket socket = new Socket(streamingServerHost, streamingServerPort);
//                socket.setKeepAlive(true);
//
//                while (true) {
//                    if (queue.size() > 0) {
//                        String data = queue.take();
//                        JSONObject jsonParam = new JSONObject();
//                        jsonParam.put("sql", data);
//                        String payload = jsonParam.toString();
//
//                        OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
//                        out.write("POST " + "/ HTTP/1.0\\r\\n");
//                        out.write("Content-Length: " + payload.length() + "\r\n");
//                        out.write("Content-Type: application/json");
//                        out.write("\r\n");
//
//                        out.write(payload);
//                        out.flush();
//                        //out.close();
//                    } else {
//                        //sleep for 1 sec
//                        Thread.sleep(1000);
//                    }
//
//                }
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return null;
//
//        }
//    }

    private class FileWriterTask extends AsyncTask<FileOutputStream, String, Void> {


        @Override
        protected Void doInBackground(FileOutputStream... params) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
            Log.i(TAG, "Inside FileWriterTask doInBackground");

            while (true) {
                try {
                    HashMap<String, String> hashMap = queue.poll();
                    if (hashMap != null) {
                        Set<String> keys = hashMap.keySet();
                        String[] keysArray = keys.toArray(new String[keys.size()]);
                        String key = keysArray[0];
                        String data = hashMap.get(key);
//
//                        fileOutputStream.write(data.getBytes());
//                        fileOutputStream.flush();

                        String[] splitArray = key.split(":");
                        String methodName = splitArray[0];
                        long start = Long.valueOf(splitArray[1]);
//                        long end = System.nanoTime();

                        sendPostRequest(data, start, methodName);
//                        if (loggingMode.equals("frameworkWithLocalFileWrite")) {
//                            boolean toLog = true;
//                            if (data.contains("Count")) {
//                                toLog = false;
//                            }
//                            if (toLog) {
//                                Log.d(TAG_TIME + "_" + methodName, String.valueOf(end - start));
//                            }
//                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }
}

