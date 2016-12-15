package edu.buffalo.rms.bluemountain.shim;

/**
 * Created by sharath on 9/13/16.
 */
public class BmChunk {
        byte[] data;

        /* Location of the chunk */
        String server;
        String header;
        String headerVal;
        String accessTkn;

        /* Object Id to which the chunk belongs and its own ID */
        String objId;
        String chunkId;

        public BmChunk(String chunkId, String objId, byte[] data) {
            this.chunkId = chunkId;
            this.data = data;
            this.objId = objId;
        }

        public void setServer(String server, String accessTkn) {
            this.server = server;
            this.accessTkn = accessTkn;
        }

        public void setHeader(String header, String val) {
            this.header = header;
            this.headerVal = val;
        }

        public byte[] getData() {
                return data;
        }

        public String getServerUrl() {
                return server;
        }

        public String getChunkId() {
            return chunkId;
        }

        public String getObjId() {
            return objId;
        }


        public String getHeader() {
                return header;
        }

        public String getHeaderVal() {
                return headerVal;
        }

        public String getAccessTkn() {
                return accessTkn;
        }
}
