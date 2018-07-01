package com.daubajee.dheba;

import io.vertx.core.json.JsonObject;

public class Block {

    private int index;

    private String hash;

    private String previousHash;

    private long timestamp;

    private long nonce;

    private long difficulty;

    private String data;

    public Block(int index, String hash, String previousHash, long timestamp, long nonce, long difficulty, String data) {
        this.index = index;
        this.hash = hash;
        this.previousHash = previousHash;
        this.timestamp = timestamp;
        this.nonce = nonce;
        this.difficulty = difficulty;
        this.data = data;
    }


    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public long getNonce() {
        return nonce;
    }

    public void setNonce(long nonce) {
        this.nonce = nonce;
    }

    public long getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(long difficulty) {
        this.difficulty = difficulty;
    }

    public JsonObject toJson() {
        return new JsonObject()
            .put("index", index)
            .put("hash", hash)
            .put("previousHash", previousHash)
            .put("timestamp", timestamp)
            .put("nonce", nonce)
            .put("difficulty", difficulty)
            .put("data", data);
    }

    public static Block fromJson(JsonObject jsonObject) {
        Integer index = jsonObject.getInteger("index");
        String hash = jsonObject.getString("hash");
        String previousHash = jsonObject.getString("previousHash");
        Long timestamp = jsonObject.getLong("timestamp");
        String data = jsonObject.getString("data");
        Long nonce = jsonObject.getLong("nonce");
        Long difficulty = jsonObject.getLong("difficulty");
        return new Block(index, hash, previousHash, timestamp, nonce, difficulty, data);
    }

}
