package com.example.bhatt.__backend.model;

import java.util.List;

/**
 * Request body for the POST /api/load endpoint.
 */
public class LoadRequest {

    private int startAddress;
    private List<Integer> hexCodes;

    public LoadRequest() {}

    public int getStartAddress() { return startAddress; }
    public void setStartAddress(int startAddress) { this.startAddress = startAddress; }

    public List<Integer> getHexCodes() { return hexCodes; }
    public void setHexCodes(List<Integer> hexCodes) { this.hexCodes = hexCodes; }
}
